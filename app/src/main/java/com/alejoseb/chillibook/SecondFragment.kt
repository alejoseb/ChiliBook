package com.alejoseb.chillibook

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.Intent.getIntent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import com.alejoseb.chillibook.databinding.FragmentSecondBinding
import com.alejoseb.chillibook.BluetoothLeService
import com.alejoseb.chillibook.SampleGattAttributes
import java.util.ArrayList
import java.util.HashMap

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    private var mBluetoothLeService: BluetoothLeService? = null
    private var mConnected = false
    private val mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    private val TAG = SecondFragment::class.java.simpleName

    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null


    private  var blename: String = "empty"
    private  var bleaddress: String = "empty"

    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Code to manage Service lifecycle.
    private var mServiceConnection: ServiceConnection?= null


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true
                updateConnectionState(R.string.connected)
                //invalidateOptionsMenu()
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false
                updateConnectionState(R.string.disconnected)
                //invalidateOptionsMenu()
                //clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService!!.supportedGattServices)
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }


    private fun displayData(data: String?) {
        if (data != null) {
            //mDataField.setText(data)
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val unknownCharaString = resources.getString(R.string.unknown_characteristic)
        val gattServiceData = ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
        mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownCharaString)
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
            }
            mGattCharacteristics.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }
        /*
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this,
            gattServiceData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2),
            gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2,
            arrayOf(LIST_NAME, LIST_UUID),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )*/
        //mGattServicesList.setAdapter(gattServiceAdapter)
    }


    private fun updateConnectionState(resourceId: Int) {
       // runOnUiThread(Runnable { mConnectionState.setText(resourceId) })
        requireActivity().runOnUiThread(
            Runnable {
              var tv:TextView =  requireActivity().findViewById<TextView>(R.id.textview_second)
                tv.setText(resourceId)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)


        mServiceConnection=  object : ServiceConnection {
            override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
                mBluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
                if (!mBluetoothLeService!!.initialize()) {
                    Log.e(
                        TAG,
                        "Unable to initialize Bluetooth"
                    )
                    requireActivity().finish()
                }
                // Automatically connects to the device upon successful start-up initialization.
                mBluetoothLeService!!.connect(mDeviceAddress)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                mBluetoothLeService = null
            }
        }

    }


    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.clear()
    }


    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(
            mGattUpdateReceiver,
        makeGattUpdateIntentFilter()
        )
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.d(
              TAG,
                "Connect request result=$result"
            )
        }
    }


    override fun onCreateView( inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?  ): View? {


        _binding = FragmentSecondBinding.inflate(inflater, container, false)


        val intent: Intent = requireActivity().getIntent()
        val bundle = arguments
        if(bundle != null) {
            if (bundle!!.containsKey("blename")) {
                mDeviceName =  bundle!!.getString("blename").toString()
            }
            if (bundle!!.containsKey("bleaddress")) {
                mDeviceAddress =  bundle!!.getString("bleaddress").toString()
            }
        }

        val gattServiceIntent = Intent(requireContext(), BluetoothLeService::class.java)
        requireActivity().bindService(gattServiceIntent, mServiceConnection!!, Context.BIND_AUTO_CREATE)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        var textViewsecond =  view.findViewById<TextView>(R.id.textview_second)
        textViewsecond.setText( String.format("Name: %s, Address: %s ",blename , bleaddress ))

        binding.buttonSecond.setOnClickListener {
            //findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)


        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(mGattUpdateReceiver)
    }


    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }


}