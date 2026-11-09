package com.lenovobuds.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public class GameDetectionService extends Service {
    private static final String CHANNEL_ID = "lenovobuds_game_mode";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String previousGame = null;

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            SharedPreferences prefs = getSharedPreferences("lenovo_buds", MODE_PRIVATE);
            if (!prefs.getBoolean("auto_game_mode", false)) {
                stopSelf();
                return;
            }
            String foreground = GameUtils.latestForegroundPackage(GameDetectionService.this);
            boolean game = GameUtils.isGamePackage(GameDetectionService.this, foreground);
            if (game) {
                if (!foreground.equals(previousGame)) {
                    String profile = prefs.getString("game_profile_" + foreground, ProfileCatalog.FPS);
                    prefs.edit().putBoolean("gaming_mode", true)
                            .putString("active_game", foreground)
                            .putString("active_profile", profile).apply();
                    AudioProfileEngine.apply(profile);
                    updateNotification("Gaming · " + profile);
                }
                previousGame = foreground;
            } else if (previousGame != null) {
                previousGame = null;
                prefs.edit().putBoolean("gaming_mode", false)
                        .remove("active_game").putString("active_profile", ProfileCatalog.MUSIC).apply();
                AudioProfileEngine.apply(ProfileCatalog.MUSIC);
                updateNotification("Music mode");
            }
            handler.postDelayed(this, 1800L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(71, buildNotification("Watching for games · Music mode"));
        handler.post(poll);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(poll);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Automatic game audio",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Switch LenovoBuds audio profiles when a game opens or closes.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("LenovoBuds · Auto mode")
                .setContentText(text).setOngoing(true).build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(71, buildNotification(text));
    }
}
