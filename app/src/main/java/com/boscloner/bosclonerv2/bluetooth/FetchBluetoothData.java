package com.boscloner.bosclonerv2.bluetooth;

import android.arch.lifecycle.MediatorLiveData;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.MainThread;
import android.text.TextUtils;

import com.boscloner.bosclonerv2.util.ActionWithDataStatus;
import com.boscloner.bosclonerv2.util.AppExecutors;
import com.boscloner.bosclonerv2.util.SampleGattAttributes;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class FetchBluetoothData extends MediatorLiveData<ActionWithDataStatus<FetchBluetoothDataStatus, FetchBluetoothDataValue>> {

    private DeviceLiveData deviceLiveData;
    private BluetoothGattCharacteristic writeCharacteristic;
    private AppExecutors appExecutors;
    String messageFromBoscloner = "";
    String autoCloneDefault = "1";
    boolean customWriteGlith = true;
    boolean firstRun = true;
    boolean writeFromHistoryFile = false;

    @Inject
    public FetchBluetoothData(DeviceLiveData deviceLiveData, AppExecutors appExecutors) {
        this.appExecutors = appExecutors;
        this.deviceLiveData = deviceLiveData;
        addSource(deviceLiveData, this::processBluetoothData);
    }

    private void processBluetoothData(ActionWithDataStatus<BluetoothDeviceStatus, BluetoothDeviceData> s) {
        if (s != null) {
            Timber.d("" + s.status + " " + s.message_title + " " + s.message_body);
            switch (s.status) {
                case INITIALIZATION_SUCCESS:
                    Timber.d("Initialization success");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Initialization bluetooth device success", "Device is ready for use"));
                    break;
                case CONNECTING:
                    Timber.d("Connecting");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communicationg with the device", "Trying to connect to device"));
                    break;
                case CONNECTED:
                    Timber.d("Connected");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communication with device", "Connection success"));
                    deviceLiveData.startServiceDiscovery();
                    break;
                case STARTING_SERVICE_DISCOVERY:
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communication with device", "Starting service discovery"));
                    break;
                case SERVICES_DISCOVERY_SUCCESS:
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communicationg with device", "Service discovery success"));
                    if (s.data != null) {
                        List<BluetoothGattService> gattServiceList = s.data.bluetoothGattServices;
                        for (BluetoothGattService gattService : gattServiceList) {
                            Timber.d("GattService UUID %s", gattService.getUuid());
                            for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                                Timber.d("Gatt characteristics: %s", characteristic.getUuid());
                                if (characteristic.getUuid().equals(SampleGattAttributes.BOSCLONER_READ_UUID)) {
                                    Timber.d("We found the notify characteristic, and we are trying to enable it");
                                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communication with device", "Enabling characteristic notification"));
                                    deviceLiveData.setCharacteristicNotification(characteristic, true);
                                } else if (characteristic.getUuid().equals(SampleGattAttributes.BOSCLONER_WRITE_UUID)) {
                                    Timber.d("We found the write characteristic, and we can use it to write data");
                                    writeCharacteristic = characteristic;
                                } else {
                                    Timber.d("Characteristic is not known");
                                }
                            }
                        }
                    }
                    //TODO report error here is we don't find write and read boscloner characteristics.
                    break;
                case ON_CHARACTERISTIC_WRITE: {
                    if (s.data != null) {
                        Timber.d("write char UUID: ");
                    }
                }
                break;
                case ON_CHARACTERISTIC_READ: {
                    if (s.data != null) {
                        Timber.d("read char UUID: ");
                    }
                }
                break;
                case ON_CHARACTERISTIC_CHANGED:
                    if (s.data != null) {
                        Timber.d("on characteristic change");
                        if (s.data.value != null) {
                            String messagePart = new String(s.data.value);
                            Timber.d("we got the: %s", messagePart);
                            if (!messagePart.isEmpty()) {
                                messageFromBoscloner += messagePart;
                                if (messageFromBoscloner.contains(DeviceCommands.SCAN.getValue()) && messageFromBoscloner.contains(DeviceCommands.END_DELIMITER.getValue())) {
                                    Timber.d("We got a SCAN message from the boscloner");
                                    String scanDeviceAddress = messageFromBoscloner.substring(7, messageFromBoscloner.length() - 2);
                                    Timber.d("SCAN: clone device scan address: %s", scanDeviceAddress);
                                    autoCloneDefault = "0";
                                    messageFromBoscloner = "";
                                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.SCAN, new FetchBluetoothDataValue(scanDeviceAddress)));
                                } else if (messageFromBoscloner.contains(DeviceCommands.CLONE.getValue()) && messageFromBoscloner.contains(DeviceCommands.END_DELIMITER.getValue())) {
                                    Timber.d("We got a CLONE message from the bosclone");
                                    String cloneDeviceAddress = messageFromBoscloner.substring(8, messageFromBoscloner.length() - 2);
                                    Timber.d("CLONE: clone device clone address: %S", cloneDeviceAddress);
                                    autoCloneDefault = "1";
                                    messageFromBoscloner = "";
                                } else if (messageFromBoscloner.contains(DeviceCommands.STATUS_MCU.getValue()) && customWriteGlith) {
                                    if (autoCloneDefault.equals("1") && firstRun) {

                                    } else if (autoCloneDefault.equals("0") && firstRun) {

                                    } else if (autoCloneDefault.equals("1") && !firstRun && !writeFromHistoryFile) {

                                    } else if (autoCloneDefault.equals("0") && !firstRun && !writeFromHistoryFile) {

                                    } else if ((autoCloneDefault.equals("0") || autoCloneDefault.equals("1")) && !firstRun && writeFromHistoryFile) {

                                    }
                                } else if (messageFromBoscloner.contains(DeviceCommands.STATUS_MCU.getValue()) && !customWriteGlith) {

                                }
                            } else {
                                Timber.d("Data set not complete. Nothing to print just yet");
                            }
                        }
                    }
                    break;
                case ON_DESCRIPTOR_WRITE:
                    if (s.data != null) {
                        if (s.data.bluetoothGattDescriptor != null) {
                            if (s.data.bluetoothGattDescriptor.getUuid().equals(UUID
                                    .fromString(SampleGattAttributes.NOTIFY_CHARACTERISTIC_CONFIG))) {
                                Timber.d("Notify characteristic enabled with success");
                                setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTED));
                            }
                        }
                    }
                    break;
                case DISCONNECTED:
                    Timber.d("Device disconnected");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.DISCONNECTED, "Device disconnected"));
                    //since we are over with this connection, clear the value, so when we next attach we don't get disconnect right away.
                    setValue(null);
                    break;
                case ERROR:
                    Timber.d("Device error");
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

    @MainThread
    public void connect(String DeviceMacAddress) {
        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CONNECTING, "Communicating with the device", "Please be patient"));
        if (DeviceMacAddress == null || TextUtils.isEmpty(DeviceMacAddress)) {
            Timber.d("Blueotooth problem, device mac not set");
            setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.ERROR, "Bluetooth problem error", "Device deviceMac address not set"));
        } else {
            deviceLiveData.connect(DeviceMacAddress);
        }
    }

    public void disconnect() {
        deviceLiveData.disconnect();
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