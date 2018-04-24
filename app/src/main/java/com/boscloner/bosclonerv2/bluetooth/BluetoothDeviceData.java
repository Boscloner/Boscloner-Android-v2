package com.boscloner.bosclonerv2.bluetooth;

import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.List;

public class BluetoothDeviceData {
    public List<BluetoothGattService> bluetoothGattServices;
    public byte[] value;
    public BluetoothGattDescriptor bluetoothGattDescriptor;
    public int counter;

    public BluetoothDeviceData() {

    }

    public static BluetoothDeviceData newInstance(List<BluetoothGattService> bluetoothGattServices) {
        BluetoothDeviceData bluetoothDeviceData = new BluetoothDeviceData();
        bluetoothDeviceData.bluetoothGattServices = bluetoothGattServices;
        return bluetoothDeviceData;
    }

    public static BluetoothDeviceData newInstance(byte[] value, int counter) {
        BluetoothDeviceData bluetoothDeviceData = new BluetoothDeviceData();
        bluetoothDeviceData.value = value;
        bluetoothDeviceData.counter = counter;
        return bluetoothDeviceData;
    }

    public static BluetoothDeviceData newInstance(BluetoothGattDescriptor bluetoothGattDescriptor) {
        BluetoothDeviceData bluetoothDeviceData = new BluetoothDeviceData();
        bluetoothDeviceData.bluetoothGattDescriptor = bluetoothGattDescriptor;
        return bluetoothDeviceData;
    }
}