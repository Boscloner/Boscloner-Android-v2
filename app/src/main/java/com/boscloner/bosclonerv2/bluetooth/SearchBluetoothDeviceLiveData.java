package com.boscloner.bosclonerv2.bluetooth;

import android.Manifest;
import android.arch.lifecycle.LiveData;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;

import com.boscloner.bosclonerv2.Constants;
import com.boscloner.bosclonerv2.util.ActionWithDataStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

public class SearchBluetoothDeviceLiveData extends LiveData<ActionWithDataStatus<SearchingStatus, List<ScanBluetoothDevice>>> {

    private final boolean useBLEScan;
    private Context context;
    private boolean bleScanning;
    private BluetoothAdapter.LeScanCallback leScanCallback;
    private BluetoothAdapter bluetoothAdapter;
    private ScanCallback scanCallback;
    private Handler handler;
    private Runnable stopLeScanRunnable;
    private BluetoothManager bluetoothManager;
    private Set<String> foundMacAddresses;
    private Set<ScanBluetoothDevice> foundDevices;

    public SearchBluetoothDeviceLiveData(Context context) {
        useBLEScan = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        this.context = context;
        foundMacAddresses = new HashSet<>();
        foundDevices = new HashSet<>();

        Timber.d("Creating live data");

        bluetoothManager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);

        leScanCallback = (bluetoothDevice, signalStrengthRSSI, scanRecord) -> checkDevice(bluetoothDevice, signalStrengthRSSI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    Timber.d("On scan result");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        checkDevice(result.getDevice(), result.getRssi());
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    Timber.d("Scan error");
                    stopScan();
                    setValue(new ActionWithDataStatus<>(SearchingStatus.ERROR, "Error scanning devices", "Please try again"));
                }
            };
        }

        handler = new Handler(context.getMainLooper());
        stopLeScanRunnable = () -> {
            Timber.d("stop le scann");
            if (bluetoothAdapter == null) {
                bleScanning = false;
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            } else {
                bluetoothAdapter.stopLeScan(leScanCallback);
            }
            bleScanning = false;
            setValue(new ActionWithDataStatus<>(SearchingStatus.DONE, new ArrayList<>(foundDevices), "Devices", "Devices"));
        };
    }

    public void startScanning() {
        setValue(new ActionWithDataStatus<>(SearchingStatus.LOADING, "Searching for your device, please wait.."));
        foundMacAddresses.clear();
        foundDevices.clear();
        if (!isPermissionGranted()) {
            setValue(new ActionWithDataStatus<>(SearchingStatus.NO_PERMISSION, "No permission", "Please grant location permission"));
            return;
        }
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    setValue(new ActionWithDataStatus<>(SearchingStatus.ERROR,
                            "Bluetooth seems to be off",
                            "Please check bluetooth connection and start the process again"));
                }
                if (useBLEScan) {
                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                        bleScanning = true;
                        Timber.d("Start le scan");
                        handler.postDelayed(stopLeScanRunnable, 10000);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Timber.d("Starting Lollipop scan");
                            Timber.d("!!!!!!!!!!!!!!!!!");
                            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
                            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED); // LOW_LATENCY
                            ScanSettings scanSettings = scanSettingsBuilder.build();
                            bluetoothAdapter.getBluetoothLeScanner().startScan(new ArrayList<>(), scanSettings, scanCallback);
                        } else {
                            Timber.d("Starting pre Lolipop scan");
                            bluetoothAdapter.startLeScan(leScanCallback);
                        }
                    } else {
                        setValue(new ActionWithDataStatus<>(SearchingStatus.ERROR, "Bluetooth error", "Make sure that the bluetooth is turned on"));
                    }
                } else {
                    setValue(new ActionWithDataStatus<>(SearchingStatus.ERROR,
                            "Bluetooth low energy problem", "It seems that this device does not support BLE protocol"));
                }
            } else {
                setValue(new ActionWithDataStatus<>(SearchingStatus.ERROR, "Bluetooth error", "Could not get bluetooth adapter, please try to scan again"));
            }
        } else {
            setValue(new ActionWithDataStatus<>(SearchingStatus.ERROR, "Bluetooth error", "This device does not have bluetooth capabilities"));
        }
    }

    @Override
    protected void onInactive() {
        stopScan();
        setValue(new ActionWithDataStatus<>(SearchingStatus.DONE, new ArrayList<>(foundDevices), "Devices", "Devices"));
    }

    public void stopScan() {
        if (bluetoothManager != null) {
            if (bluetoothAdapter != null) {
                if (bleScanning) {
                    handler.removeCallbacks(stopLeScanRunnable);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (bluetoothAdapter.getBluetoothLeScanner() != null) {
                            bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                        }
                    } else {
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    }
                    bleScanning = false;
                }
            }
        }
    }

    public void checkDevice(BluetoothDevice device, int signalStrengthRSSI) {
        Timber.d("Device " + device.getName() + " " + device.getAddress() + " device rssi: " + signalStrengthRSSI);
        String deviceName = device.getName();
        if (deviceName != null && deviceName.equals(Constants.DEVICE_NAME) && foundMacAddresses.add(device.getAddress())) {
            foundDevices.add(new ScanBluetoothDevice(device.getAddress(), signalStrengthRSSI));
            setValue(new ActionWithDataStatus<>(SearchingStatus.LOADING, new ArrayList<>(foundDevices), "Devices", "Devices"));
        }
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}