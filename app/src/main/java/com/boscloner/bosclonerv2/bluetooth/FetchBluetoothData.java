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

    boolean firstRun = true;
    boolean multipart = false;
    DeviceCommands lastCommand;
    int multipartIndex = 0;
    byte[] multipartData;
    private DeviceLiveData deviceLiveData;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;
    private AppExecutors appExecutors;
    private String messageFromBoscloner = "";

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
                        Timber.d("Characteristic write: %s", s.data.bluetoothGattCharacteristic);
                        if (s.data.bluetoothGattCharacteristic.getUuid().equals(SampleGattAttributes.BOSCLONER_WRITE_UUID)) {
                            if (multipart) {
                                multipartIndex += 20;
                                sendMultipartData();
                            }
                        }
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
                                if (messageFromBoscloner.contains(DeviceResponses.SCAN.getValue())
                                        && messageFromBoscloner.contains(DeviceResponses.END_DELIMITER.getValue())) {
                                    Timber.d("We got a SCAN message from the boscloner");
                                    String scanDeviceAddress = messageFromBoscloner.substring(7, messageFromBoscloner.length() - 3);
                                    Timber.d("SCAN: clone device scan address: %s", scanDeviceAddress);
                                    messageFromBoscloner = "";
                                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.SCAN, new FetchBluetoothDataValue(scanDeviceAddress)));
                                } else if (messageFromBoscloner.contains(DeviceResponses.CLONE.getValue())
                                        && messageFromBoscloner.contains(DeviceResponses.END_DELIMITER.getValue())) {
                                    Timber.d("We got a CLONE message from the bosclone");
                                    String cloneDeviceAddress = messageFromBoscloner.substring(8, messageFromBoscloner.length() - 3);
                                    Timber.d("CLONE: clone device clone address: %S", cloneDeviceAddress);
                                    messageFromBoscloner = "";
                                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.CLONE, new FetchBluetoothDataValue(cloneDeviceAddress)));
                                } else if (messageFromBoscloner.contains(DeviceResponses.STATUS_MCU.getValue())) {
                                    messageFromBoscloner = "";
                                    switch (lastCommand) {
                                        case ENABLE_CLONE:
                                            setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.AUTO_CLONE_ENABLED,
                                                    new FetchBluetoothDataValue("")));
                                            if (firstRun) {
                                                setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.STATUS_MCU_ENABLED,
                                                        new FetchBluetoothDataValue("")));
                                                firstRun = false;
                                            }
                                            break;
                                        case DISABLE_CLONE:
                                            setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.AUTO_CLONE_DISABLED,
                                                    new FetchBluetoothDataValue("")));
                                            if (firstRun) {
                                                setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.STATUS_MCU_DISABLED,
                                                        new FetchBluetoothDataValue("")));
                                                firstRun = false;
                                            }
                                            break;
                                        default:
                                            Timber.d("Custom Data Written. Ignoring STATUS,MCU Signal");
                                            break;
                                    }
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
                    writeCharacteristic = null;
                    readCharacteristic = null;
                    setValue(null);
                    break;
                case ERROR:
                    writeCharacteristic = null;
                    readCharacteristic = null;
                    Timber.d("Device error");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.ERROR, s.message_title, s.message_body));
                case BLUETOOTH_OFF:
                    writeCharacteristic = null;
                    readCharacteristic = null;
                    Timber.d("Blutooth off");
                    setValue(new ActionWithDataStatus<>(FetchBluetoothDataStatus.BLUETOOTH_OFF, s.message_title, s.message_body));
                    setValue(null);
                default:
                    break;
            }
        }
    }

    private void sendData(byte[] data) {
        if (data.length == 0) {
            Timber.d("Empty data, we should not send to the boscloner");
            return;
        }
        if (data.length < 20) {
            multipart = false;
            multipartIndex = 0;
            if (writeCharacteristic != null) {
                writeCharacteristic.setValue(data);
                writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                deviceLiveData.writeCharacteristic(writeCharacteristic);
            }
        } else {
            multipart = true;
            multipartIndex = 0;
            multipartData = data;
            sendMultipartData();
        }
    }

    private void sendMultipartData() {
        if (multipartIndex >= multipartData.length) {
            Timber.d("Everything ok, lets try to notify that the message has been sent");
            multipartData = null;
            multipart = false;
            multipartIndex = 0;
            return;
        }
        int partSize = 20;
        if (multipartIndex + partSize > multipartData.length) {
            partSize = multipartData.length - multipartIndex;
        }
        byte[] part = new byte[partSize];
        System.arraycopy(multipartData, multipartIndex, part, 0, partSize);
        if (writeCharacteristic != null) {
            writeCharacteristic.setValue(part);
            writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            deviceLiveData.writeCharacteristic(writeCharacteristic);
        }
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
        firstRun = true;
        lastCommand = null;
        writeCharacteristic = null;
        readCharacteristic = null;
        messageFromBoscloner = "";
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
        if (writeCharacteristic != null) {
            if (isChecked) {
                lastCommand = DeviceCommands.ENABLE_CLONE;
                sendData(DeviceCommands.ENABLE_CLONE.getValue().getBytes());
            } else if (!isChecked) {
                lastCommand = DeviceCommands.DISABLE_CLONE;
                sendData(DeviceCommands.DISABLE_CLONE.getValue().getBytes());
            } else {
                Timber.d("Auto clone is already on the proper value");
            }
        }
    }

    public void writeDataToTheDevice(String macAddress) {
        if (writeCharacteristic != null) {
            String stiped = macAddress.replaceAll(":", "");
            if (stiped.length() < 10) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(stiped);
                int count = 10 - stiped.length();
                for (int i = 0; i < count; i++) {
                    stringBuilder.append("0");
                }
                stiped = stringBuilder.toString();
            }
            lastCommand = DeviceCommands.CLONE;
            String command = String.format(DeviceCommands.CLONE.getValue(), stiped);
            Timber.d("Command to write " + command + " " + command.getBytes().length);
            sendData(command.getBytes());
        }
    }
}