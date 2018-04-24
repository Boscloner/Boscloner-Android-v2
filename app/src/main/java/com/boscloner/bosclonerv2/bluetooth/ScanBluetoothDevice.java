package com.boscloner.bosclonerv2.bluetooth;

public class ScanBluetoothDevice {
    public String deviceMacAddress;
    public int deviceRssi;

    public ScanBluetoothDevice(String deviceMacAddress, int deviceRssi) {
        this.deviceMacAddress = deviceMacAddress;
        this.deviceRssi = deviceRssi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScanBluetoothDevice that = (ScanBluetoothDevice) o;

        if (deviceRssi != that.deviceRssi) return false;
        return deviceMacAddress != null ? deviceMacAddress.equals(that.deviceMacAddress) : that.deviceMacAddress == null;
    }

    @Override
    public int hashCode() {
        int result = deviceMacAddress != null ? deviceMacAddress.hashCode() : 0;
        result = 31 * result + deviceRssi;
        return result;
    }
}