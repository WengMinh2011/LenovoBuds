package com.lenovobuds.app;

import android.media.audiofx.Equalizer;

/**
 * Best-effort Android output-mix EQ. This does not write EQ into the earbuds.
 * Android/OEM audio policy may reject session 0 or may not route the effect to Bluetooth.
 */
public final class AudioProfileEngine {
    private static Equalizer equalizer;
    private static String lastStatus = "EQ ready";

    private AudioProfileEngine() {}

    public static synchronized String apply(String profileName) {
        return applyGains(profileName, ProfileCatalog.gains(profileName));
    }

    public static synchronized String applyGains(String profileName, float[] gains) {
        try {
            if (equalizer == null) equalizer = new Equalizer(0, 0);
            if (equalizer == null || equalizer.getNumberOfBands() <= 0) {
                lastStatus = "This phone does not expose a global EQ effect";
                return lastStatus;
            }
            short[] range = equalizer.getBandLevelRange();
            float[] targetHz = new float[]{60f, 230f, 910f, 3600f, 14000f};
            short count = equalizer.getNumberOfBands();
            for (short band = 0; band < count; band++) {
                float hz = equalizer.getCenterFreq(band) / 1000f;
                int nearest = 0;
                float distance = Float.MAX_VALUE;
                for (int i = 0; i < targetHz.length; i++) {
                    float d = Math.abs((float) Math.log(Math.max(1f, hz) / targetHz[i]));
                    if (d < distance) { distance = d; nearest = i; }
                }
                float gain = gains != null && gains.length == 5 ? gains[nearest] : 0f;
                int millibels = Math.round(gain * 100f);
                millibels = Math.max(range[0], Math.min(range[1], millibels));
                equalizer.setBandLevel(band, (short) millibels);
            }
            equalizer.setEnabled(true);
            lastStatus = "Phone EQ applied · " + profileName;
            return lastStatus;
        } catch (Throwable error) {
            lastStatus = "Phone EQ unavailable on this device";
            return lastStatus;
        }
    }

    public static synchronized String status() { return lastStatus; }

    public static synchronized void release() {
        if (equalizer != null) {
            try { equalizer.release(); } catch (Throwable ignored) {}
            equalizer = null;
        }
    }
}
