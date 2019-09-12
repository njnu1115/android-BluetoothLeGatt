/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.byton.dt.blegattsample

import android.app.Activity
import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.util.*


/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
class DeviceScanActivity : ListActivity() {
    private var mLeDeviceListAdapter: LeDeviceListAdapter? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning: Boolean = false
    private var mHandler: Handler? = null

    // Device scan callback.
    private val mLeScanCallback: ScanCallback = object : ScanCallback() {

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            runOnUiThread {
                mLeDeviceListAdapter?.addDevice(result.device)
                mLeDeviceListAdapter?.notifyDataSetChanged()
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            println("BLE// onBatchScanResults")
            for (sr in results) {
                Log.i("ScanResult - Results", sr.toString())
            }
        }

        override fun onScanFailed(errorCode: Int) {

        }

    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar!!.setTitle(R.string.title_devices)
        mHandler = Handler()

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (mBluetoothAdapter?.isMultipleAdvertisementSupported == false ) {
            Toast.makeText(this, "MultipleAdvertisementSupported not supported", Toast.LENGTH_SHORT).show()
        }

        if (mBluetoothAdapter?.isOffloadedFilteringSupported == false) {
            Toast.makeText(this, "OffloadedFilteringSupported not supported", Toast.LENGTH_SHORT).show()
        }

        if (mBluetoothAdapter?.isOffloadedScanBatchingSupported == false) {
            Toast.makeText(this, "OffloadedScanBatchingSupported not supported", Toast.LENGTH_SHORT).show()
        }

        if(mBluetoothAdapter?.isLeExtendedAdvertisingSupported == false){
            Toast.makeText(this, "isLeExtendedAdvertisingSupported not supported", Toast.LENGTH_SHORT).show()
        }

        if(mBluetoothAdapter?.isLePeriodicAdvertisingSupported == false){
            Toast.makeText(this, "isLePeriodicAdvertisingSupported not supported", Toast.LENGTH_SHORT).show()
        }

        if(mBluetoothAdapter?.isLe2MPhySupported == false){
            Toast.makeText(this, "isLe2MPhySupported not supported", Toast.LENGTH_SHORT).show()
        }

        if(mBluetoothAdapter?.isLeCodedPhySupported == false){
            Toast.makeText(this, "isLeCodedPhySupported not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(
                R.layout.actionbar_indeterminate_progress
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                mLeDeviceListAdapter!!.clear()
                scanLeDevice(true)
            }
            R.id.menu_stop -> scanLeDevice(false)
        }
        return true
    }

    override fun onResume() {
        super.onResume()

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter!!.isEnabled) {
            if (!mBluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        listAdapter = mLeDeviceListAdapter
        scanLeDevice(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
        mLeDeviceListAdapter!!.clear()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = mLeDeviceListAdapter!!.getDevice(position) ?: return
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        if (mScanning) {
            mBluetoothAdapter!!.bluetoothLeScanner.stopScan(mLeScanCallback)
            mScanning = false
        }
        startActivity(intent)
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                mScanning = false
                mBluetoothAdapter!!.bluetoothLeScanner.stopScan(mLeScanCallback)
                invalidateOptionsMenu()
            }, SCAN_PERIOD)

            mScanning = true
            mBluetoothAdapter!!.bluetoothLeScanner.startScan(mLeScanCallback)
        } else {
            mScanning = false
            mBluetoothAdapter!!.bluetoothLeScanner.stopScan(mLeScanCallback)
        }
        invalidateOptionsMenu()
    }

    // Adapter for holding devices found through scanning.
    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList()
        private val mInflater: LayoutInflater = this@DeviceScanActivity.layoutInflater

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice? {
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

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view: View? = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflater.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view!!.findViewById(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }

            val device = mLeDevices[i]
            val deviceName: String = device.name
            if (deviceName.isNotEmpty())
                viewHolder.deviceName!!.text = deviceName
            else
                viewHolder.deviceName!!.setText(R.string.unknown_device)
            viewHolder.deviceAddress!!.text = device.address

            return view
        }
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }

    companion object {

        private const val REQUEST_ENABLE_BT = 1
        // Stops scanning after 10 seconds.
        private const val SCAN_PERIOD: Long = 10000
    }
}