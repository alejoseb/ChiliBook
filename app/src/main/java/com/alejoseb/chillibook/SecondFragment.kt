package com.alejoseb.chillibook

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.alejoseb.chillibook.databinding.FragmentSecondBinding
import java.util.*
import kotlin.collections.ArrayList


import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.LimitLine.LimitLabelPosition;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    private var mBluetoothLeService: BluetoothLeService? = null
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    private val TAG = SecondFragment::class.java.simpleName
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var  tvtemperature:  TextView? = null
    private var  tvhumidity:  TextView? = null
    private var queueCharacteristics: Queue<BluetoothGattCharacteristic> = LinkedList()
    private  var blename: String = "empty"
    private  var bleaddress: String = "empty"
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"

    private var linechartTemp: LineChart? = null
    private var linechartHumidity: LineChart? = null
    var valuesTemp: ArrayList<Entry> = ArrayList()
    var valuesHumidity: ArrayList<Entry> = ArrayList()





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

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService!!.supportedGattServices)
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData( intent.getStringExtra("UUID"),  intent.getStringExtra(BluetoothLeService.EXTRA_DATA) )
            }else if (BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN.equals(action)){
                   writeDescriptor()
            }

        }
    }


    private fun displayData(uuidval: String?,  data: String?) {

        if (uuidval == "beb5483e-36e1-4688-b7f5-ea07361b26a4") { //temperature
            //var fahrenheit: Float = data!!.toFloat()
            //var celsius: Float = (fahrenheit - 32)*5/9
            tvtemperature!!.text = getString(R.string.default_temperature, data)

            var i = valuesTemp.count()
            valuesTemp.add(Entry(i.toFloat(),data!!.toFloat() ))
            var set1: LineDataSet = LineDataSet(valuesTemp, "Temperature")
            val dataSets: ArrayList<ILineDataSet> = ArrayList()
            set1.setDrawValues(false)
            set1.setColor(Color.rgb(240, 99, 99))
            set1.setCircleColor(Color.rgb(240, 99, 99))
            dataSets.add(set1)
            val data = LineData(dataSets)
            linechartTemp!!.data = data
            linechartTemp!!.notifyDataSetChanged()
            linechartTemp!!.invalidate()

        }
        else if  (uuidval == "beb5483e-36e1-4688-b7f5-ea07361b26a8") { //humidity
            tvhumidity!!.text =   getString(R.string.default_humidity, data)

            var i = valuesTemp.count()
            valuesHumidity.add(Entry(i.toFloat(),data!!.toFloat() ))
            var set2: LineDataSet = LineDataSet(valuesHumidity, "Humidity")
            val dataSets2: ArrayList<ILineDataSet> = ArrayList()
            set2.setDrawValues(false)
            dataSets2.add(set2)
            val data = LineData(dataSets2)
            linechartHumidity!!.data = data
            linechartHumidity!!.notifyDataSetChanged()
            linechartHumidity!!.invalidate()


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
            if (uuid ==  "4fafc201-1fb5-459e-8fcc-c5c9c331914b")
            {
                currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)
                currentServiceData[LIST_UUID] = uuid
                gattServiceData.add(currentServiceData)
                val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
                val gattCharacteristics = gattService.characteristics
                val charas = ArrayList<BluetoothGattCharacteristic>()

                // Loops through available Characteristics.
                for (gattCharacteristic in gattCharacteristics) {

                    uuid = gattCharacteristic.uuid.toString()
                    if (uuid == "beb5483e-36e1-4688-b7f5-ea07361b26a8" || uuid ==  "beb5483e-36e1-4688-b7f5-ea07361b26a4" )
                    {
                        charas.add(gattCharacteristic)
                        val currentCharaData = HashMap<String, String?>()

                        currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownCharaString)
                        currentCharaData[LIST_UUID] = uuid
                        gattCharacteristicGroupData.add(currentCharaData)
                    }

                }
                mGattCharacteristics.add(charas)
                gattCharacteristicData.add(gattCharacteristicGroupData)
            }

        }
        for(characteristic in mGattCharacteristics[0])
        {
            queueCharacteristics.add(characteristic)
        }

        writeDescriptor()



    }


    private fun writeDescriptor()
    {

        if( queueCharacteristics.count()>0)
        {
            val characteristic: BluetoothGattCharacteristic = queueCharacteristics.remove()
            val charaProp: Int = characteristic.properties
            if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0)
            {
                mNotifyCharacteristic = characteristic
                mBluetoothLeService!!.setCharacteristicNotification(
                characteristic, true
                )

            }
        }

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

        var textViewsecond = _binding!!.root.findViewById<TextView>(R.id.textview_second)
        textViewsecond.setText( String.format("Name: %s, Address: %s ",blename , bleaddress ))

        tvhumidity =  _binding!!.root.findViewById<TextView>(R.id.textViewHumidity)
        tvtemperature =  _binding!!.root.findViewById<TextView>(R.id.textViewTemp)

        // BLE service initilization
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

        // chart initialization
        linechartTemp =  _binding!!.root.findViewById<LineChart>(R.id.linechartTemp)
        linechartTemp!!.description.isEnabled = false

        linechartHumidity = _binding!!.root.findViewById<LineChart>(R.id.linechartHumidity)
        linechartHumidity!!.description.isEnabled = false

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.buttonSecond.setOnClickListener {
            mBluetoothLeService!!.disconnect()
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)

        }

        binding.buttonClear.setOnClickListener {
            valuesTemp.clear()
            linechartTemp!!.notifyDataSetChanged()
            linechartTemp!!.invalidate()

            valuesHumidity.clear()
            linechartHumidity!!.notifyDataSetChanged()
            linechartHumidity!!.invalidate()

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
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DESCRIPTOR_WRITTEN)
        return intentFilter
    }


}