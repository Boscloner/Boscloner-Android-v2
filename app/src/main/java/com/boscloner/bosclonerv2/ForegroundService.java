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
import com.boscloner.bosclonerv2.util.AppExecutors;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import timber.log.Timber;

public class ForegroundService extends LifecycleService {

    public static final String NO_PERMISSION_BROADCAST = "com.boscloner.bosclonerv2.ForgroundService.NO_PERMISSION_BROADCAST";
    public static final String STOP_SELF = "com.boscloer.bosclonerv2.ForgroundService.STOP_SELF";
    private static Intent noPermissionBroadcast = new Intent(NO_PERMISSION_BROADCAST);
    private static Intent stopSelfIntent = new Intent(STOP_SELF);

    @Inject
    public FetchBluetoothData fetchBluetoothData;

    @Inject
    BosclonerDatabase database;

    @Inject
    AppExecutors appExecutors;

    private String notificationTitle;
    private String notificationContentText;
    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private SearchBluetoothDeviceLiveData searchBluetoothDeviceLiveData;
    private RFIDBadgeType badgeType;
    private boolean autoCloneSetting;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
        prepareNotification();
        readStatusAndBadgeType();
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
                            //TODO stop scan here.
                        }
                    }
                    break;
                    case DONE: {
                        List<ScanBluetoothDevice> foundDevices = status.data;
                        if (foundDevices != null && !foundDevices.isEmpty()) {
                            //TODO write value to the database saying we are connected.
                            Timber.d("We have the device and we are connected");
                        } else {
                            searchBluetoothDeviceLiveData.startScanning();
                        }
                    }
                    break;
                }
            }
        });

        fetchBluetoothData.observe(this, status -> {
            Timber.d("Fetch data status %s", status);
            if (status != null) {
                switch (status.status) {
                    case CONNECTED:
                        SharedPreferences settings = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this);
                        boolean autoClone = settings.getBoolean(Constants.Preferences.AUTO_CLONE_KEY, false);
                        fetchBluetoothData.onAutoCloneChanged(autoClone);
                        break;
                    case ERROR:
                    case DISCONNECTED:
                        //we start a scan again in case of some error. Need to test this more.
                        searchBluetoothDeviceLiveData.startScanning();
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
                            });
                        }
                        break;
                    case STATUS_MCU_ENABLED:
                        appExecutors.diskIO().execute(() -> {
                            Event event = new Event();
                            event.type = EventType.STATUS_MCU_ENABLED;
                            event.value = "**AutoClone Status: Enabled**\n" +
                                    "**RFID Badge Type: " + "rfid badge type" + "\n" +
                                    "----------------------------" +
                                    "Boscloner$ (Ready to Receive Data)";
                            database.eventDao().addEvent(event);
                        });
                        break;
                    case STATUS_MCU_DISABLED:
                        appExecutors.diskIO().execute(() -> {
                            Event event = new Event();
                            event.type = EventType.STATUS_MCU_DISABLED;
                            event.value = "**AutoClone Status: Disabled**\n" +
                                    "**RFID Badge Type: " + "rfid badge type" + "\n" +
                                    "----------------------------" +
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
                        fetchBluetoothData.onAutoCloneChanged(isChecked);
                    }
                }
            }
        }
        return START_STICKY;
    }

    private void noPermission() {
        if (!LocalBroadcastManager.getInstance(this).sendBroadcast(noPermissionBroadcast)) {
            notificationTitle = "No permission";
            notificationContentText = "Boscloner requires permission to run";
            updateNotification();
        }
    }

    private void updateNotification() {
        notificationBuilder.setContentTitle(notificationTitle)
                .setTicker(notificationTitle)
                .setContentText(notificationContentText);
        notificationManager.notify(Constants.NotificationId.FOREGROUND_SERVICE, notificationBuilder.build());
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
                NotificationChannel channel = new NotificationChannel(Constants.NotificationId.CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
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
}