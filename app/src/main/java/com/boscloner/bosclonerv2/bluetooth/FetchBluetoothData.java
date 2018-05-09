package com.boscloner.bosclonerv2.bluetooth;

import android.arch.lifecycle.MediatorLiveData;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.MainThread;
import android.text.TextUtils;
import android.util.Log;

import com.boscloner.bosclonerv2.util.ActionWithDataStatus;
import com.boscloner.bosclonerv2.util.AppExecutors;
import com.boscloner.bosclonerv2.util.SampleGattAttributes;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
class FetchBluetoothData extends MediatorLiveData<ActionWithDataStatus<FetchBluetoothDataStatus, FetchBluetoothDataInterface>> {

    private static final String TAG = FetchBluetoothData.class.getSimpleName();

    private DeviceLiveData deviceLiveData;
    private BluetoothGattCharacteristic writeCharacteristic;
    private AppExecutors appExecutors;

    @Inject
    public FetchBluetoothData(DeviceLiveData deviceLiveData, AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        this.deviceLiveData = deviceLiveData;
        addSource(deviceLiveData, this::processBluetoothData);
    }

    private void processBluetoothData(ActionWithDataStatus<BluetoothDeviceStatus, BluetoothDeviceData> s) {
        if (s != null) {
            Log.d("TAG", "" + s.status + " " + s.message_title + " " + s.message_body);
            switch (s.status) {
                case INITIALIZATION_SUCCESS:
                    Timber.d("Initialization success");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Initialization bluetooth device success", "Device is ready for use"));
                    break;
                case CONNECTING:
                    Timber.d("Connecting");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communicationg with the device", "Trying to connect to oximeter"));
                    break;
                case CONNECTED:
                    Timber.d("Connected");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communication with oximeter device", "Connection success"));
                    deviceLiveData.startServiceDiscovery();
                    break;
                case STARTING_SERVICE_DISCOVERY:
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communication with oximeter device", "Starting service discovery"));
                    break;
                case SERVICES_DISCOVERY_SUCCESS:
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communicationg with oximeter device", "Service discovery success"));
                    if (s.data != null) {
                        List<BluetoothGattService> gattServiceList = s.data.bluetoothGattServices;
                        for (BluetoothGattService gattService : gattServiceList) {
                            for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                                if (characteristic.getUuid().equals(DeviceLiveData.NOTIFY_UUID)) {
                                    Log.d("TAG", "We found the notify characteristic, and we are trying to enable it");
                                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communication with oximeter device", "Enabling characteristic notification"));
                                    deviceLiveData.setCharacteristicNotification(characteristic, true);
                                } else if (characteristic.getUuid().equals(DeviceLiveData.WRITE_UUID)) {
                                    Log.d("TAG", "We found the write characteristic, and we can use it to write data");
                                    writeCharacteristic = characteristic;
                                } else {
                                    Log.d("TAG", "Characteristic is not known");
                                }
                            }
                        }
                    }
                    break;
                case ON_CHARACTERISTIC_WRITE: {
                    if (s.data != null) {
                        Log.d("TAG", "write char UUID: ");
                    }
                }
                break;
                case ON_CHARACTERISTIC_READ: {
                    if (s.data != null) {
                        Log.d("TAG", "read char UUID: ");
                    }
                }
                break;
                case ON_CHARACTERISTIC_CHANGED:
                    if (s.data != null) {
                        Log.d("TAG", "on characteristic change UUID: ");
                        //TODO see what to do with the value that we get from the boscloner.

                    }
                    break;
                case ON_DESCRIPTOR_WRITE:
                    if (s.data != null) {
                        if (s.data.bluetoothGattDescriptor != null) {
                            if (s.data.bluetoothGattDescriptor.getUuid().equals(UUID
                                    .fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))) {
                                Log.d("TAG", "Notify characteristic enabled with success");
                                setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTED));
                            }
                        }
                    }
                    break;
                case DISCONNECTED:
                    Timber.d("Oximeter disconnected");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.DISCONNECTED, "Oximeter disconnected"));
                    //since we are over with this connection, clear the value, so when we next attach we don't get disconnect right away.
                    setValue(null);
                    break;
                case ERROR:
                    Timber.d("Oximeter error");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.ERROR, s.message_title, s.message_body));
                case BLUETOOTH_OFF:
                    Timber.d("Blutooth off");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.BLUETOOTH_OFF, s.message_title, s.message_body));
                    setValue(null);
                default:
                    break;
            }
        }
    }

    private void sendData(byte[] data) {
        writeCharacteristic.setValue(data);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        deviceLiveData.writeCharacteristic(writeCharacteristic);

    }

    private void logCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT).equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d("TAG", "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d("TAG", "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d("TAG", String.format("Received heart rate: %d", heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d("TAG", new String(data) + "\n" + stringBuilder.toString());
            }
        }
    }
    
    @MainThread
    public void connect(String oximeterMacAddress) {
        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communicating with the oximeter device", "Please be patient"));
        if (oximeterMacAddress == null || TextUtils.isEmpty(oximeterMacAddress)) {
            Timber.d("Blueotooth problem, device mac not set");
            setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.ERROR, "Bluetooth problem error", "Oximeter deviceMac address not set"));
        } else {
            deviceLiveData.connect(oximeterMacAddress);
        }
    }

    @MainThread
    public void getBatteryLevel() {
        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.LOADING, "Communication with oximeter device", "Getting battery level"));
        sendData(DeviceCommand.GET_BATTERY_LEVEL());
    }

    @MainThread
    public void getContiniousPRDataSize() {
        sendData(DeviceCommand.GET_CONTINUOUS_DATA_SIZE(DeviceCommand.ContinuousDataType.PR));
    }

    @MainThread
    public void getContiniousPRData() {
        sendData(DeviceCommand.GET_PR_CONTINUOUS_DATA(DeviceCommand.ContinuousGetDataOption.START));
    }

    @MainThread
    public void getContiniousSPO2DataSize() {
        sendData(DeviceCommand.GET_CONTINUOUS_DATA_SIZE(DeviceCommand.ContinuousDataType.SPO2));
    }

    @MainThread
    public void getContiniousSPO2Data() {
        sendData(DeviceCommand.GET_SPO2_CONTINUOUS_DATA(DeviceCommand.ContinuousGetDataOption.START));
    }

    public void disconnect() {
        deviceLiveData.disconnect();
    }

    public void setTime() {
        sendData(DeviceCommand.SET_TIME());
    }

    public void clearContinuousPrData() {
        sendData(DeviceCommand.DEL_CONTINUOUS_DATA(DeviceCommand.ContinuousDataType.PR));
    }

    public void clearContinuousSPO2Data() {
        sendData(DeviceCommand.DEL_CONTINUOUS_DATA(DeviceCommand.ContinuousDataType.SPO2));
    }

    @Override
    protected void onActive() {
        super.onActive();
        Timber.d("Fetch bluetooth data active");
    }

    @Override
    protected void onInactive() {
        Timber.d("Fetch bluetooth data inactive");
        super.onInactive();
    }
}