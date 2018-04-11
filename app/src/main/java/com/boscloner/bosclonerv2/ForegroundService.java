package com.boscloner.bosclonerv2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat.Builder;

import timber.log.Timber;

public class ForegroundService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals(Constants.Action.STARTFOREGROUND_ACTION)) {
                Timber.i("Received Start Foreground Intent ");
                showNotification();
            } else if (intent.getAction().equals(
                    Constants.Action.STOPFOREGROUND_ACTION)) {
                Timber.i("Received Stop Foreground Intent");
                stopForeground(true);
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void showNotification() {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            NotificationChannel channel = new NotificationChannel(Constants.NotificationId.CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        Notification notification = new Builder(this, Constants.NotificationId.CHANNEL_ID)
                .setContentTitle("Boscloner")
                .setTicker("Boscloner")
                .setContentText("Boscloner running")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop",
                        closePendingIntent)
                .build();
        startForeground(Constants.NotificationId.FOREGROUND_SERVICE,
                notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // in case we bind to the service, we don't do that right now.
        return null;
    }
}
