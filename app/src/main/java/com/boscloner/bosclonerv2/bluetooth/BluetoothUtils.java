package com.boscloner.bosclonerv2.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class BluetoothUtils {
    public static boolean isBluetoothAvailable() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return (bluetoothAdapter != null && bluetoothAdapter.isEnabled());
    }

    @NonNull
    public static String getNameWithOutSemicolons(@Nullable String macAddress) {
        if (macAddress == null)
            return "";
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < macAddress.length(); i++) {
            if (macAddress.charAt(i) != ':') {
                stringBuilder.append(macAddress.charAt(i));
            }
        }
        return stringBuilder.toString();
    }
}