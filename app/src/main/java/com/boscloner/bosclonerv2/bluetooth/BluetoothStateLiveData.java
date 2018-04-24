package com.boscloner.bosclonerv2.bluetooth;

import android.arch.lifecycle.LiveData;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BluetoothStateLiveData extends LiveData<BluetoothStateLiveData.BluetoothStatus> {

    private Context context;
    private BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    setValue(BluetoothStatus.DISCONNECTED);
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    setValue(BluetoothStatus.DISCONNECTED);
                    break;
                case BluetoothAdapter.STATE_ON:
                    setValue(BluetoothStatus.CONNECTED);
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    setValue(BluetoothStatus.DISCONNECTED);
                    break;
                case BluetoothAdapter.ERROR:
                    setValue(BluetoothStatus.DISCONNECTED);
                    break;
            }
        }
    };

    public BluetoothStateLiveData(Context context) {
        this.context = context;
    }

    @Override
    protected void onActive() {
        context.registerReceiver(connectionReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (BluetoothUtils.isBluetoothAvailable()) {
            setValue(BluetoothStatus.CONNECTED);
        } else {
            setValue(BluetoothStatus.DISCONNECTED);
        }
    }

    @Override
    protected void onInactive() {
        context.unregisterReceiver(connectionReceiver);
    }

    public enum BluetoothStatus {CONNECTED, DISCONNECTED}
}