package com.boscloner.bosclonerv2.bluetooth;

import android.arch.lifecycle.MutableLiveData;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import com.boscloner.bosclonerv2.util.ActionWithDataStatus;
import com.boscloner.bosclonerv2.util.SampleGattAttributes;

import java.util.UUID;

import javax.inject.Inject;

import timber.log.Timber;

class DeviceLiveData extends MutableLiveData<ActionWithDataStatus<BluetoothDeviceStatus, BluetoothDeviceData>> {

    private static int counter = 0;
    public BluetoothGatt mBluetoothGatt;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private Handler handler;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Timber.d("On state change: %s", newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.CONNECTED));
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                close();
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.DISCONNECTED));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.SERVICES_DISCOVERY_SUCCESS, BluetoothDeviceData.newInstance(gatt.getServices())));
            } else {
                Timber.e("onServicesDiscovered received: %s", status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ON_CHARACTERISTIC_READ));
            } else {
                Timber.e("onCharacteristicRead received: %s", status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Timber.d("On characteristic write");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ON_CHARACTERISTIC_WRITE));
            } else {
                Timber.e("onCharacteristic write problem: %s", status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Timber.d("Before characteristic change");
            counter++;
            Timber.d("Counter %s", counter);
            Timber.d("DATA LENGTH: %s", characteristic.getValue().length);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < characteristic.getValue().length; i++) {
                stringBuilder.append(characteristic.getValue()[i]).append(" ");
            }
            Timber.d(" %s", stringBuilder.toString());
            Timber.d("____________________________________");
            sendCharacteristic(characteristic);
            Timber.d("After characteristic change");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ON_DESCRIPTOR_WRITE, BluetoothDeviceData.newInstance(descriptor)));
            } else {
                Timber.e("onDescriptor write: %s", status);
            }
        }

    };
    private Context context;

    @Inject
    public DeviceLiveData(Context context) {
        this.context = context;
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    //noinspection unchecked
                    setValue((ActionWithDataStatus<BluetoothDeviceStatus, BluetoothDeviceData>) msg.obj);
                }
                if (msg.what == 2) {
                    setValue(new ActionWithDataStatus<>(BluetoothDeviceStatus.ON_CHARACTERISTIC_CHANGED, BluetoothDeviceData.newInstance((byte[]) msg.obj, counter)));
                } else {
                    super.handleMessage(msg);
                }
            }
        };
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        //TODO possible we will need to check if we are connected
        //TODO check if we are disconnected :/
        Timber.d("onInactive observer");
        if (mBluetoothGatt != null) {
            disconnect();
        }
    }

    private void connectToDevice(@Nullable final String address) {

        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Timber.e("Unable to initialize BluetoothManager.");
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth Error", "This device does not have bluetooth capabilities"));
                return;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Timber.e("Unable to obtain a BluetoothAdapter.");
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth initialization Error", "Unable to obtain BluetoothAdapter"));
            return;
        }
        if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
            setValue(new ActionWithDataStatus<>(BluetoothDeviceStatus.BLUETOOTH_OFF,
                    "Bluetooth seems to be off",
                    "Please check bluetooth connection and try the process again"));
            return;
        }
        setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.INITIALIZATION_SUCCESS));

        if (address == null) {
            Timber.w("Unspecified device address.");
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth Error", "Unspecified device address."));
            return;
        }
        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Timber.d("Trying to use an existing mBluetoothGatt for connection.");
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.TRYING_TO_USE_EXISTING_CONNECTION));
            if (mBluetoothGatt.connect()) {
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.CONNECTING));
                return;
            } else {
                setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth error", "We could not establish the connection with the device"));
                return;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth error", "Device not found.  Unable to get device."));
            return;
        }

        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.CONNECTING));
    }

    /**
     * This method should be only called when there is succesfull connection to the device.
     */
    public void startServiceDiscovery() {
        if (mBluetoothGatt == null) {
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR_STARTING_SERVICE_DISCOVERY));
            return;
        }
        setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.STARTING_SERVICE_DISCOVERY));
        boolean serviceDiscoveryStarted = mBluetoothGatt.discoverServices();
        if (!serviceDiscoveryStarted) {
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR_STARTING_SERVICE_DISCOVERY));
        }
    }

    public void connect(@Nullable String macAddress) {
        connectToDevice(macAddress);
    }

    public boolean disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.w("BluetoothAdapter not initialized");
            close();
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR));
            return false;
        }
        mBluetoothGatt.disconnect();
        setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.DISCONNECTING));
        return true;
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.e("BluetoothAdapter not initialized");
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth readCharacteristic Error", "Bluetooth not initialized"));
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Timber.e("BluetoothAdapter not initialized");
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth setCharacteristicNotification Error", "Bluetooth not initialized"));
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (SampleGattAttributes.BOSCLONER_READ_UUID.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID
                    .fromString(SampleGattAttributes.NOTIFY_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }


    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null || characteristic == null) {
            Timber.e("BluetoothAdapter not initialized");
            setMessage(new ActionWithDataStatus<>(BluetoothDeviceStatus.ERROR, "Bluetooth setCharacteristicNotification Error", "Bluetooth not initialized"));
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void setMessage(ActionWithDataStatus<BluetoothDeviceStatus, BluetoothDeviceData> actionResult) {
        Message msg = handler.obtainMessage(1, actionResult);
        handler.sendMessage(msg);
    }

    public void sendCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        byte[] copy = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            copy[i] = value[i];
        }
        Message msg = handler.obtainMessage(2, copy);
        handler.sendMessage(msg);
    }
}