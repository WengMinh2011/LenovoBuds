package com.lenovobuds.app;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;

public final class GameUtils {
    private GameUtils() {}

    public static boolean hasUsageAccess(Context context) {
        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode;
            if (Build.VERSION.SDK_INT >= 29) {
                mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), context.getPackageName());
            } else {
                mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), context.getPackageName());
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable ignored) { return false; }
    }

    public static String latestForegroundPackage(Context context) {
        if (!hasUsageAccess(context)) return null;
        try {
            UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (manager == null) return null;
            long now = System.currentTimeMillis();
            UsageEvents events = manager.queryEvents(now - 10 * 60 * 1000L, now);
            UsageEvents.Event event = new UsageEvents.Event();
            String current = null;
            long latest = 0L;
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event);
                int type = event.getEventType();
                boolean foreground = type == UsageEvents.Event.MOVE_TO_FOREGROUND
                        || (Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_RESUMED);
                boolean background = type == UsageEvents.Event.MOVE_TO_BACKGROUND
                        || (Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.ACTIVITY_PAUSED);
                if (foreground || background) {
                    if (event.getTimeStamp() >= latest) {
                        latest = event.getTimeStamp();
                        current = foreground ? event.getPackageName() : null;
                    }
                }
            }
            return current;
        } catch (Throwable ignored) { return null; }
    }

    public static boolean isGamePackage(Context context, String packageName) {
        if (packageName == null || packageName.equals(context.getPackageName())) return false;
        if (isKnownGamePackage(packageName)) return true;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, 0);
            return Build.VERSION.SDK_INT >= 26 && info.category == ApplicationInfo.CATEGORY_GAME;
        } catch (Throwable ignored) { return false; }
    }

    public static boolean isKnownGamePackage(String packageName) {
        if (packageName == null) return false;
        String p = packageName.toLowerCase();
        return p.contains("freefire") || p.contains("mobile.legends") || p.contains("pubg")
                || p.contains("callofduty") || p.contains("wildrift") || p.contains("genshin")
                || p.contains("hkrpgoversea") || p.contains("honkaistarrail")
                || p.contains("roblox") || p.contains("arenaofvalor") || p.contains("asphalt")
                || p.contains("clashofclans") || p.contains("clashroyale") || p.contains("brawlstars")
                || p.contains("minecraft") || p.contains("fortnite") || p.contains("codm")
                || p.contains("deltaforce") || p.contains("zenless") || p.contains("wuthering")
                || p.contains("honkaiimpact") || p.contains("nikke") || p.contains("snowbreak")
                || p.contains("toweroffantasy") || p.contains("punishing") || p.contains("arknights")
                || p.contains("genshinimpact") || p.contains("mihoyo") || p.contains("hoyoverse")
                || p.contains("tencent") && (p.contains("game") || p.contains("sgame") || p.contains("igame"))
                || p.contains("com.garena") || p.contains("com.vng") || p.contains("com.riotgames")
                || p.contains("com.supercell") || p.contains("com.mojang") || p.contains("com.epicgames");
    }
}
