package com.boscloner.bosclonerv2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.LifecycleService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;

import com.boscloner.bosclonerv2.bluetooth.FetchBluetoothData;
import com.boscloner.bosclonerv2.bluetooth.ScanBluetoothDevice;
import com.boscloner.bosclonerv2.bluetooth.SearchBluetoothDeviceLiveData;
import com.boscloner.bosclonerv2.room.BosclonerDatabase;
import com.boscloner.bosclonerv2.room.Event;
import com.boscloner.bosclonerv2.room.EventType;
import com.boscloner.bosclonerv2.room.HistoryItem;
import com.boscloner.bosclonerv2.util.AppExecutors;

import org.threeten.bp.LocalDateTime;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import timber.log.Timber;

public class ForegroundService extends LifecycleService {

    public static final String NO_PERMISSION_BROADCAST = "com.boscloner.bosclonerv2.ForgroundService.NO_PERMISSION_BROADCAST";
    public static final String STOP_SELF = "com.boscloer.bosclonerv2.ForgroundService.STOP_SELF";
    public static final String UI_UPDATE_BROADCAST = "com.boscloner.bosclonerv2.ForgroundService.UI_UPDATE_BROADCAST";
    public static final String UI_UPDATE_BROADCAST_KEY = "ui_update_state";
    private static Intent noPermissionBroadcast = new Intent(NO_PERMISSION_BROADCAST);
    private static Intent stopSelfIntent = new Intent(STOP_SELF);
    private static Intent uiUpdateBroadcast = new Intent(UI_UPDATE_BROADCAST);
    @Inject
    public FetchBluetoothData fetchBluetoothData;
    @Inject
    BosclonerDatabase database;
    @Inject
    AppExecutors appExecutors;
    private ConnectionState connectionState;
    private String notificationTitle;
    private String notificationContentText;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private SearchBluetoothDeviceLiveData searchBluetoothDeviceLiveData;
    private RFIDBadgeType badgeType;
    private boolean autoCloneSetting;
    private boolean autoClone;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
        connectionState = ConnectionState.LOADING;
        prepareNotification();
        updateTheUi();
        readStatusAndBadgeType();
        clearTheDatabase();
        searchBluetoothDeviceLiveData = new SearchBluetoothDeviceLiveData(this);
        this.searchBluetoothDeviceLiveData.observe(this, status -> {
            Timber.d("Scan data stats %s", status);
            if (status != null) {
                Timber.d("status: " + status.status + " " + status.message_title + status.message_body);
                switch (status.status) {
                    case NO_PERMISSION:
                        noPermission();
                        break;
                    case LOADING: {
                        List<ScanBluetoothDevice> foundDevices = status.data;
                        if (foundDevices != null && !foundDevices.isEmpty()) {
                            searchBluetoothDeviceLiveData.stopScan();
                            Timber.d("Found device %s", foundDevices.size());
                            ScanBluetoothDevice bosclonerDevice = foundDevices.get(0);
                            fetchBluetoothData.connect(bosclonerDevice.deviceMacAddress);
                        }
                    }
                    break;
                    case DONE: {
                        List<ScanBluetoothDevice> foundDevices = status.data;
                        if (foundDevices != null && !foundDevices.isEmpty()) {
                            searchBluetoothDeviceLiveData.stopScan();
                            Timber.d("Found device %s", foundDevices.size());
                            ScanBluetoothDevice bosclonerDevice = foundDevices.get(0);
                            if (connectionState == ConnectionState.CONNECTION_LOST) {
                                connectionState = ConnectionState.ATTEMPTING_TO_RECONNECT;
                            } else {
                                connectionState = ConnectionState.ATTEMPTING_TO_CONNECT;
                            }
                            updateTheUi();
                            fetchBluetoothData.connect(bosclonerDevice.deviceMacAddress);
                        } else {
                            if (connectionState != ConnectionState.CONNECTION_LOST) {
                                connectionState = ConnectionState.SCANNING;
                            }
                            updateTheUi();
                            searchBluetoothDeviceLiveData.startScanning();
                        }
                    }
                    break;
                    case BLUETOOTH_OFF:
                        //TODO ask user to turn on the bluetooth
                        connectionState = ConnectionState.DISCONNECTED;
                        updateTheUi();
                        break;
                    case ERROR:
                        Timber.d("Error while scanning please try again");
                        connectionState = ConnectionState.SCANNING;
                        updateTheUi();
                        startScanning();
                        break;
                    case ADAPTER_ERROR:
                    case BLE_NOT_SUPPORTED:
                    case DEVICE_DOES_NOT_HAVE_BLUETOOTH_ERROR:
                        //TODO display error to the user, both as snackbar in activity, and inside notification
                        connectionState = ConnectionState.DISCONNECTED;
                        updateTheUi();
                        break;
                }
            }
        });

        fetchBluetoothData.observe(this, status -> {
            Timber.d("Fetch data status %s", status);
            if (status != null) {
                switch (status.status) {
                    case CONNECTED:
                        fetchBluetoothData.onAutoCloneChanged(autoClone);
                        if (connectionState == ConnectionState.ATTEMPTING_TO_RECONNECT) {
                            connectionState = ConnectionState.RECONNECTED;
                        } else {
                            connectionState = ConnectionState.CONNECTED;
                        }
                        updateTheUi();
                        break;
                    case ERROR:
                    case DISCONNECTED:
                        //we start a scan again in case of some error. Need to test this more.
                        if (connectionState == ConnectionState.CONNECTED) {
                            connectionState = ConnectionState.CONNECTION_LOST;
                        } else {
                            connectionState = ConnectionState.SCANNING;
                        }
                        updateTheUi();
                        searchBluetoothDeviceLiveData.startScanning();
                        break;
                    case BLUETOOTH_OFF:
                        //TODO ask user to turn on the bluetooth
                        connectionState = ConnectionState.DISCONNECTED;
                        updateTheUi();
                        break;
                    case SCAN:
                        if (status.data != null) {
                            notificationTitle = "Badge Captured";
                            notificationContentText = status.data.value;
                            updateNotification();
                            appExecutors.diskIO().execute(() -> {
                                Event event = new Event();
                                event.type = EventType.SCAN;
                                event.value = "Boscloner$ " + status.data.value;
                                database.eventDao().addEvent(event);
                                HistoryItem historyItem = new HistoryItem();
                                historyItem.localDateTime = LocalDateTime.now();
                                historyItem.deviceMacAddress = status.data.value;
                                database.historyItemDao().add(historyItem);
                            });
                        }
                        break;
                    case CLONE:
                        if (status.data != null) {
                            notificationTitle = "Badge Cloned";
                            notificationContentText = status.data.value;
                            updateNotification();
                            appExecutors.diskIO().execute(() -> {
                                Event event = new Event();
                                event.type = EventType.CLONE;
                                event.value = "Boscloner$ " + status.data.value;
                                database.eventDao().addEvent(event);
                                HistoryItem historyItem = new HistoryItem();
                                historyItem.localDateTime = LocalDateTime.now();
                                historyItem.deviceMacAddress = status.data.value;
                                database.historyItemDao().add(historyItem);
                            });
                        }
                        break;
                    case STATUS_MCU_ENABLED:
                        appExecutors.diskIO().execute(() -> {
                            Event event = new Event();
                            event.type = EventType.STATUS_MCU_ENABLED;
                            event.value = "**RFID Badge Type: " + "RFID badge type" + "\n" +
                                    "----------------------------\n" +
                                    "Boscloner$ (Ready to Receive Data)";
                            database.eventDao().addEvent(event);
                        });
                        break;
                    case STATUS_MCU_DISABLED:
                        appExecutors.diskIO().execute(() -> {
                            Event event = new Event();
                            event.type = EventType.STATUS_MCU_DISABLED;
                            event.value = "**RFID Badge Type: " + "RFID badge type" + "\n" +
                                    "----------------------------\n" +
                                    "Boscloner$ (Ready to Receive Data)";
                            database.eventDao().addEvent(event);
                        });
                        break;
                    case AUTO_CLONE_ENABLED:
                        appExecutors.diskIO().execute(() -> {
                            Event event = new Event();
                            event.type = EventType.AUTO_CLONE_ENABLED;
                            event.value = "**AutoClone Status: Enabled**";
                            database.eventDao().addEvent(event);
                        });
                        break;
                    case AUTO_CLONE_DISABLED:
                        appExecutors.diskIO().execute(() -> {
                            Event event = new Event();
                            event.type = EventType.AUTO_CLONE_DISABLED;
                            event.value = "**AutoClone Status: Disabled**";
                            database.eventDao().addEvent(event);
                        });
                        break;
                }
            }
        });
        showNotification();
    }

    private void clearTheDatabase() {
        appExecutors.diskIO().execute(() -> {
            database.eventDao().clearTable();
            appExecutors.mainThread().execute(this::startScanning);
        });
    }

    private void startScanning() {
        connectionState = ConnectionState.SCANNING;
        updateTheUi();
        searchBluetoothDeviceLiveData.startScanning();
    }

    private void readStatusAndBadgeType() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String badgeString = preferences.getString("rfid_badge_type", "");
        badgeType = RFIDBadgeType.findValueByName(badgeString);
        autoCloneSetting = preferences.getBoolean("pref_key_enable_autoclone", false);
    }

    private void prepareNotification() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.Action.MAIN_ACTION);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent closeIntent = new Intent(this, ForegroundService.class);
        closeIntent.setAction(Constants.Action.STOPFOREGROUND_ACTION);
        PendingIntent closePendingIntent = PendingIntent.getService(this, 0,
                closeIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.bos_cloner_logo);

        notificationBuilder = new Builder(this, Constants.NotificationId.CHANNEL_ID)
                .setContentTitle("Boscloner")
                .setTicker("Boscloner")
                .setContentText("Boscloner running")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop",
                        closePendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case Constants.Action.STOPFOREGROUND_ACTION: {
                        Timber.i("Received Stop Foreground Intent");
                        searchBluetoothDeviceLiveData.stopScan();
                        fetchBluetoothData.disconnect();
                        LocalBroadcastManager.getInstance(this).sendBroadcast(stopSelfIntent);
                        stopForeground(true);
                        stopSelf();
                        break;
                    }
                    case Constants.Action.PERMISSION_RESULT_ACTION: {
                        boolean permission_granted = intent.getBooleanExtra(Constants.Action.PERMISSION_RESULT_DATA, false);
                        if (permission_granted) {
                            searchBluetoothDeviceLiveData.startScanning();
                        } else {
                            notificationTitle = "No permission";
                            notificationContentText = "Boscloner requires permission to run";
                            updateNotification();
                        }
                        break;
                    }
                    case Constants.Action.AUTO_CLONE_ACTION: {
                        boolean isChecked = intent.getBooleanExtra(Constants.Action.AUTO_CLONE_DATA, false);
                        autoClone = isChecked;
                        fetchBluetoothData.onAutoCloneChanged(isChecked);
                        break;
                    }
                    case Constants.Action.WRITE_MAC_ADDRESS: {
                        Timber.d("Mac Address %s", intent.getStringExtra(Constants.Action.WRITE_MAC_ADDRESS_DATA));
                        String macAddress = intent.getStringExtra(Constants.Action.WRITE_MAC_ADDRESS_DATA);
                        boolean fromHistory = intent.getBooleanExtra(Constants.Action.WRITE_MAC_ADDRESS_HISTORY, false);
                        fetchBluetoothData.writeDataToTheDevice(macAddress);
                        String source = fromHistory ? "Written from History: " : "Custom ID Written: ";
                        appExecutors.diskIO().execute(() -> {
                            Event event = new Event();
                            event.type = EventType.VALUE_WRITE;
                            event.value = source + macAddress;
                            database.eventDao().addEvent(event);
                            HistoryItem historyItem = new HistoryItem();
                            historyItem.localDateTime = LocalDateTime.now();
                            historyItem.deviceMacAddress = macAddress;
                            database.historyItemDao().add(historyItem);
                        });
                        break;
                    }
                }
            }
        }
        updateTheUi();
        return START_STICKY;
    }

    private void updateTheUi() {
        switch (connectionState) {
            case DISCONNECTED:
                notificationTitle = "Disconnected";
                notificationContentText = "Boscloner device Disconnected";
                break;
            case SCANNING:
                notificationTitle = "Scanning";
                notificationContentText = "Scanning for a Boslconer device";
                break;
            case ATTEMPTING_TO_CONNECT:
                notificationTitle = "Connecting";
                notificationContentText = "Attempting to Connect to the Boscloner device";
                break;
            case ATTEMPTING_TO_RECONNECT:
                notificationTitle = "Reconnecting";
                notificationContentText = "Attempting to Reconnect to the Boscloner device";
                break;
            case CONNECTED:
                notificationTitle = "Connected";
                notificationContentText = "Boscloner Device Connected";
                break;
            case RECONNECTED:
                notificationTitle = "Connection Restored";
                notificationContentText = "Connection with the Device Restored";
            case CONNECTION_LOST:
                notificationTitle = "Connection Lost";
                notificationContentText = "Connection with the Device has been Lost";
                break;
        }
        uiUpdateBroadcast.putExtra(UI_UPDATE_BROADCAST_KEY, connectionState);
        if (!LocalBroadcastManager.getInstance(this).sendBroadcast(uiUpdateBroadcast)) {
            updateNotification();
        }
    }

    private void noPermission() {
        if (!LocalBroadcastManager.getInstance(this).sendBroadcast(noPermissionBroadcast)) {
            notificationTitle = "No Permission";
            notificationContentText = "Boscloner Requires Permission to Run";
            updateNotification();
        }
    }

    private void updateNotification() {
        notificationBuilder.setContentTitle(notificationTitle)
                .setTicker(notificationTitle)
                .setContentText(notificationContentText);
        startForeground(Constants.NotificationId.FOREGROUND_SERVICE, notificationBuilder.build());
    }

    private void showNotification() {
        createNotificationChannel();
        startForeground(Constants.NotificationId.FOREGROUND_SERVICE,
                notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = getString(R.string.channel_name);
            if (notificationManager != null && notificationManager.getNotificationChannel(channelName) == null) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                CharSequence name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                NotificationChannel channel = new NotificationChannel(Constants.NotificationId.CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(description);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                // Register the channel with the system
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        // in case we bind to the service, we don't do that right now.
        return null;
    }

    public enum ConnectionState {
        LOADING,
        DISCONNECTED,
        SCANNING,
        ATTEMPTING_TO_CONNECT,
        ATTEMPTING_TO_RECONNECT,
        CONNECTED,
        CONNECTION_LOST,
        RECONNECTED,
    }
}