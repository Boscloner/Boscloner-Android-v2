package com.boscloner.bosclonerv2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.LifecycleService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;

import com.boscloner.bosclonerv2.bluetooth.FetchBluetoothData;
import com.boscloner.bosclonerv2.bluetooth.ScanBluetoothDevice;
import com.boscloner.bosclonerv2.bluetooth.SearchBluetoothDeviceLiveData;

import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import timber.log.Timber;

public class ForegroundService extends LifecycleService {

    public static final String NO_PERMISSION_BROADCAST = "com.boscloner.bosclonerv2.ForgroundService.NO_PERMISSION_BROADCAST";
    private static Intent noPermissionBroadcast = new Intent(NO_PERMISSION_BROADCAST);
    @Inject
    public FetchBluetoothData fetchBluetoothData;
    private String notificationTitle;
    private String notificationContentText;
    private NotificationCompat.Builder notificationBuilder;
    private SearchBluetoothDeviceLiveData searchBluetoothDeviceLiveData;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
        prepareNotificationBuilder();
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
                            //TODO add a code to connect to the device here
                            Timber.d("Found device %s", foundDevices.size());
                            ScanBluetoothDevice bosclonerDevice = foundDevices.get(0);
                            fetchBluetoothData.connect(bosclonerDevice.deviceMacAddress);
                        }
                    }
                    break;
                    case DONE: {
                        List<ScanBluetoothDevice> foundDevices = status.data;
                        if (foundDevices != null && !foundDevices.isEmpty()) {

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
                    case ERROR:
                    case DISCONNECTED:
                        //we start a scan again in case of some error. Need to test this more.
                        searchBluetoothDeviceLiveData.startScanning();
                        break;
                    case SCAN:
                        notificationTitle = "Badge Captured";
                        if (status.data != null) {
                            notificationContentText = status.data.value;
                        }
                        updateNotification();
                        break;
                }
            }
        });

        showNotification();
        searchBluetoothDeviceLiveData.startScanning();
    }

    private void prepareNotificationBuilder() {
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
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case Constants.Action.STOPFOREGROUND_ACTION: {
                    Timber.i("Received Stop Foreground Intent");
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
        showNotification();
    }

    private void showNotification() {
        createNotificationChannel();
        startForeground(Constants.NotificationId.FOREGROUND_SERVICE,
                notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String channelName = getString(R.string.channel_name);
            if (notificationManager != null && notificationManager.getNotificationChannel(channelName) != null) {
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