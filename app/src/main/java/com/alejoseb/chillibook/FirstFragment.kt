package com.alejoseb.chillibook

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.fragment.findNavController
import com.alejoseb.chillibook.databinding.FragmentFirstBinding
import java.util.ArrayList


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {


    private var mLeDeviceListAdapter: FirstFragment.LeDeviceListAdapter? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning = false
    private var mHandler: Handler? = null
    private val SCAN_PERIOD: Long = 10000
    private val REQUEST_ENABLE_BT = 1

    private var buttonConnect: Button? = null
    private var textViewFirst: TextView? = null



    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val bluetoothManager = requireActivity().getSystemService(Context.BLUETOOTH_SERVICE)  as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter


    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        mHandler = Handler()

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        buttonConnect =_binding!!.root.findViewById<Button>(R.id.button_connect)
        textViewFirst = _binding!!.root.findViewById<Button>(R.id.textview_first)


        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.buttonConnect.setOnClickListener {

            val bundle = Bundle()

             if(!mLeDeviceListAdapter!!.isEmpty)
            {
                var devicechilli = mLeDeviceListAdapter!!.getDevice(0)
                bundle.putString("blename",devicechilli.name)
                bundle.putString("bleaddress", devicechilli.address)

            }
            else{
                bundle.putString("blename", "prueba name")
                bundle.putString("bleaddress", "prueba address")
             }
            mLeDeviceListAdapter!!.clear()
            buttonConnect!!.isEnabled =false

            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    override fun onResume() {
        super.onResume()

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(
                enableBtIntent,
                REQUEST_ENABLE_BT
            )
        }


        //setListAdapter(mLeDeviceListAdapter)


        buttonConnect!!.isEnabled = !mLeDeviceListAdapter!!.isEmpty


    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*
        return when (item.itemId) {
            R.id.action_scan -> true
            else -> super.onOptionsItemSelected(item)
        }*/
        when (item.itemId) {


            R.id.action_scan -> {

                buttonConnect!!.isEnabled = false
                mLeDeviceListAdapter!!.clear()
                scanLeDevice(true)
                return  true

            }
            R.id.action_stop ->
            {

                scanLeDevice(false)
                return  true

            }
            R.id.home ->
            {
                //onBackPressed();
                return  true
            }

        }

        return super.onOptionsItemSelected(item);


    }




    // Device scan callback.
    // Device scan callback.
    private var mLeScanCallback: LeScanCallback? =
        LeScanCallback { device, rssi, scanRecord ->
            requireActivity().runOnUiThread(Runnable {

                if(device.name =="ESP32") {
                   mLeDeviceListAdapter!!.addDevice(device)
                }
                //mLeDeviceListAdapter!!.notifyDataSetChanged()
            })
        }

    private fun scanLeDevice(enable: Boolean) {


        if (enable) {
            // Stops scanning after a pre-defined scan period.

            mHandler!!.postDelayed({
                mScanning = false
                textViewFirst!!.setText(R.string.not_scanning)
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)

                if(!mLeDeviceListAdapter!!.isEmpty)
                {
                    Toast.makeText(getActivity(), R.string.ble_device_found, Toast.LENGTH_SHORT).show()
                    buttonConnect!!.isEnabled = true
                }
                else
                {
                    Toast.makeText(getActivity(), R.string.ble_device_not_found, Toast.LENGTH_SHORT).show()
                    buttonConnect!!.isEnabled = false
                }

                invalidateOptionsMenu(getActivity());

            }, SCAN_PERIOD)

            textViewFirst!!.setText(R.string.scanning)
            mScanning = true
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)

        } else {
            mScanning = false
            textViewFirst!!.setText(R.string.not_scanning)
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)

            if(!mLeDeviceListAdapter!!.isEmpty)
            {
                Toast.makeText(getActivity(), R.string.ble_device_found, Toast.LENGTH_SHORT).show()
                buttonConnect!!.isEnabled = true
            }
            else
            {
                Toast.makeText(getActivity(), R.string.ble_device_not_found, Toast.LENGTH_SHORT).show()
                buttonConnect!!.isEnabled = false
            }


        }
        invalidateOptionsMenu(getActivity());
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)  {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu)

        if (!mScanning) {
            menu.findItem(R.id.action_stop).isVisible = false
            menu.findItem(R.id.action_scan).isVisible = true

        } else {
            menu.findItem(R.id.action_stop).isVisible = true
            menu.findItem(R.id.action_scan).isVisible = false
        }


    }

    private class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList()

        // private val mInflator: LayoutInflater
        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
            var viewlocal = view

            return viewlocal

        }

        /*
        override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: com.example.android.bluetoothlegatt.DeviceScanActivity.ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = com.example.android.bluetoothlegatt.DeviceScanActivity.ViewHolder()
                viewHolder.deviceAddress = view.findViewById<View>(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById<View>(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder =
                    view.tag as com.example.android.bluetoothlegatt.DeviceScanActivity.ViewHolder
            }
            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0) viewHolder.deviceName.setText(
                deviceName
            ) else viewHolder.deviceName.setText(R.string.unknown_device)
            viewHolder.deviceAddress.setText(device.address)
            return view
        }*/

        init {
            // mInflator =    LayoutInflater.from();
        }
    }

}