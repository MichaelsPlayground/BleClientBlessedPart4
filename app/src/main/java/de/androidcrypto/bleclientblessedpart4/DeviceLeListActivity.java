/*
 * Copyright (C) 2014 The Android Open Source Project
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

package de.androidcrypto.bleclientblessedpart4;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */

public class DeviceLeListActivity extends AppCompatActivity {

    /**
     * Tag for Log
     */
    private static final String TAG = "DeviceListActivity";

    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    /**
     * Member fields
     */
    private BluetoothAdapter mBtAdapter;
    private BluetoothLeScanner mBtLeScanner;
    private boolean scanning;
    private Handler handler = new Handler();
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000; // 5000 = 5 seconds
    List<String> subject_list; // for temporary list
    SwitchMaterial scanFilterEnabled;
    // this is the UUID for filtering
    private static final UUID HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");

    /**
     * Newly discovered devices
     */
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    ProgressBar progressBar;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_le_device_list);

        scanFilterEnabled = findViewById(R.id.swDeviceLeListScanFilter);
        progressBar = findViewById(R.id.pbList);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.VISIBLE);
                subject_list = new ArrayList<String>();
                scanLeDevice();
                v.setVisibility(View.GONE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // and initialize the LE Scanner
        mBtLeScanner  = mBtAdapter.getBluetoothLeScanner();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = "none device paired";
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            // Make sure we're not doing discovery anymore
            mBtAdapter.cancelDiscovery();
        }
    }

    /**
     * Start device discover with the BluetoothAdapter
     */

    @SuppressLint("MissingPermission")
    private void scanLeDevice() {
        Log.i("DeviceLeList", "scanLeDevice");
        setProgressBarIndeterminateVisibility(true);
        setTitle("scanning");
        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    scanning = false;
                    mBtLeScanner.stopScan(leScanCallback);
                    progressBar.setIndeterminate(true);
                    progressBar.setVisibility(View.GONE);
                }
            }, SCAN_PERIOD);

            scanning = true;

            /**
             * notice on using a ScanFilter: testing with a real device (Samsung Galaxy A4 with
             * Android 5.0.1 does not run a filtered service uuid so you need to run the scan with
             * UNCHECKED switch
             */

            if (scanFilterEnabled.isChecked()) {
                // using a scan filter - here for fixed Heart Rate Service UUID
                List<ScanFilter> leScanFilter = new ArrayList<ScanFilter>();
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(HEART_RATE_SERVICE_UUID))
                        .build();
                leScanFilter.add(scanFilter);
                ScanSettings leScanSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                mBtLeScanner.startScan(leScanFilter, leScanSettings, leScanCallback);
            } else {
                // no scan filter
                mBtLeScanner.startScan(leScanCallback);
            }

        } else {
            scanning = false;
            mBtLeScanner.stopScan(leScanCallback);
        }
    }

    @SuppressLint("MissingPermission")
    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    String deviceInfos =
                            "name: " + result.getDevice().getName()
                                    + "\ntype: " + getBTDeviceType(result.getDevice())
                                    + "\naddress: " + result.getDevice().getAddress();
                    // this code is for avoiding duplicates in the listview
                    subject_list.add(deviceInfos);
                    HashSet<String> hashSet = new HashSet<String>();
                    hashSet.addAll(subject_list);
                    subject_list.clear();
                    subject_list.addAll(hashSet);
                    mNewDevicesArrayAdapter.clear();
                    mNewDevicesArrayAdapter.addAll(hashSet);
                    mNewDevicesArrayAdapter.notifyDataSetChanged();
                }
            };

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        @SuppressLint("MissingPermission")
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the Intent and include the MAC address
            Intent intent = new Intent(DeviceLeListActivity.this, MainActivity.class);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            startActivity(intent);
            finish();
        }
    };

    /**
     * get the type of Bluetooth device
     */

    @SuppressLint("MissingPermission")
    private String getBTDeviceType(BluetoothDevice d){
        String type = "";
        switch (d.getType()){
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "unknown...";
        }
        return type;
    }
}
