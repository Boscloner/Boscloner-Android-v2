package com.boscloner.bosclonerv2.bluetooth;

import android.arch.lifecycle.MediatorLiveData;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.MainThread;
import android.text.TextUtils;

import com.boscloner.bosclonerv2.Constants;
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
    private BluetoothGattCharacteristic readCharacteristic;
    private AppExecutors appExecutors;
    private String messageFromBoscloner = "";
    boolean autoCloneDefault = true;
    boolean customWriteGlitch = true;
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
                    boolean readCharFound = false;
                    boolean writeCharFound = false;
                    if (s.data != null) {
                        List<BluetoothGattService> gattServiceList = s.data.bluetoothGattServices;
                        for (BluetoothGattService gattService : gattServiceList) {
                            Timber.d("GattService UUID %s", gattService.getUuid());
                            for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                                Timber.d("Gatt characteristics: %s", characteristic.getUuid());
                                if (characteristic.getUuid().equals(SampleGattAttributes.BOSCLONER_READ_UUID)) {
                                    Timber.d("We found the notify characteristic, and we are trying to enable it");
                                    readCharacteristic = characteristic;
                                    readCharFound = true;
                                }
                                if (characteristic.getUuid().equals(SampleGattAttributes.BOSCLONER_WRITE_UUID)) {
                                    Timber.d("We found the write characteristic, and we can use it to write data");
                                    writeCharacteristic = characteristic;
                                    writeCharFound = true;
                                }
                            }
                        }
                    }
                    if (!readCharFound || !writeCharFound) {
                        readCharacteristic = null;
                        writeCharacteristic = null;
                        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.ERROR, "Communicating with the device", "This device does not have proper read/write characteristics"));
                    } else {
                        deviceLiveData.setCharacteristicNotification(readCharacteristic, true);
                    }
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
                                    String scanDeviceAddress = messageFromBoscloner.substring(7, messageFromBoscloner.length() - 3);
                                    Timber.d("SCAN: clone device scan address: %s", scanDeviceAddress);
                                    autoCloneDefault = false;
                                    messageFromBoscloner = "";
                                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.SCAN, new FetchBluetoothDataValue(scanDeviceAddress)));
                                } else if (messageFromBoscloner.contains(DeviceCommands.CLONE.getValue()) && messageFromBoscloner.contains(DeviceCommands.END_DELIMITER.getValue())) {
                                    Timber.d("We got a CLONE message from the bosclone");
                                    String cloneDeviceAddress = messageFromBoscloner.substring(8, messageFromBoscloner.length() - 3);
                                    Timber.d("CLONE: clone device clone address: %S", cloneDeviceAddress);
                                    autoCloneDefault = true;
                                    messageFromBoscloner = "";
                                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CLONE, new FetchBluetoothDataValue(cloneDeviceAddress)));
                                } else if (messageFromBoscloner.contains(DeviceCommands.STATUS_MCU.getValue()) && customWriteGlitch) {
                                    messageFromBoscloner = "";
                                    if (autoCloneDefault && firstRun) {
                                        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.STATUS_MCU_ENABLED, new FetchBluetoothDataValue("")));
                                        firstRun = false;
                                    } else if (!autoCloneDefault && firstRun) {
                                        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.STATUS_MCU_DISABLED, new FetchBluetoothDataValue("")));
                                        firstRun = false;
                                    } else if (autoCloneDefault && !firstRun && !writeFromHistoryFile) {
                                        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.AUTO_CLONE_ENABLED, new FetchBluetoothDataValue("")));
                                    } else if (!autoCloneDefault && !firstRun && !writeFromHistoryFile) {
                                        setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.AUTO_CLONE_DISABLED, new FetchBluetoothDataValue("")));
                                    } else {
                                        writeFromHistoryFile = false;
                                        Timber.d("Write operation executed from History Log File");
                                    }
                                } else if (messageFromBoscloner.contains(DeviceCommands.STATUS_MCU.getValue()) && !customWriteGlitch) {
                                    messageFromBoscloner = "";
                                    Timber.d("Custom Data Written. Ignoring STATUS,MCU Signal");
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
        //writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
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

    public void onAutoCloneChanged(boolean isChecked) {
        if (isChecked && !autoCloneDefault) {
            customWriteGlitch = true;
            sendData(Constants.ENABLE_CLONE.getBytes());
            autoCloneDefault = true;
            setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.AUTO_CLONE_ENABLED, new FetchBluetoothDataValue("")));
        } else if (!isChecked && autoCloneDefault) {
            customWriteGlitch = true;
            sendData(Constants.DISABLE_CLONE.getBytes());
            autoCloneDefault = false;
            setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.AUTO_CLONE_DISABLED, new FetchBluetoothDataValue("")));
        } else {
            Timber.d("Auto clone is already on the proper value");
        }
    }

    public void writeDataToTheDevice(String macAddress) {
        customWriteGlitch = false;
        String command = String.format(Constants.CLONE, macAddress);
        Timber.d("Command to write " + command + " " + command.getBytes().length);
        sendData(command.getBytes());
    }
}