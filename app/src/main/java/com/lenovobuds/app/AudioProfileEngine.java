package com.lenovobuds.app;

import android.media.audiofx.Equalizer;

/**
 * Best-effort Android output-mix EQ. Android/OEM audio policy may reject session 0 or
 * may not route this effect to Bluetooth. The UI must report that limitation honestly.
 */
public final class AudioProfileEngine {
    private static Equalizer equalizer;
    private static String lastStatus = "EQ has not been initialised";

    private AudioProfileEngine() {}

    public static synchronized String apply(String profileName) {
        try {
            if (equalizer == null) {
                equalizer = new Equalizer(0, 0); // session 0 = global output mix, when supported
            }
            if (equalizer == null || equalizer.getNumberOfBands() <= 0) {
                lastStatus = "This phone does not expose a global EQ effect";
                return lastStatus;
            }
            short[] range = equalizer.getBandLevelRange(); // millibels
            float[] targetHz = new float[]{60f, 230f, 910f, 3600f, 14000f};
            float[] gains = ProfileCatalog.gains(profileName);
            short count = equalizer.getNumberOfBands();
            for (short band = 0; band < count; band++) {
                float hz = equalizer.getCenterFreq(band) / 1000f;
                int nearest = 0;
                float distance = Float.MAX_VALUE;
                for (int i = 0; i < targetHz.length; i++) {
                    float d = Math.abs((float) Math.log(Math.max(1f, hz) / targetHz[i]));
                    if (d < distance) { distance = d; nearest = i; }
                }
                int millibels = Math.round(gains[nearest] * 100f);
                millibels = Math.max(range[0], Math.min(range[1], millibels));
                equalizer.setBandLevel(band, (short) millibels);
            }
            equalizer.setEnabled(true);
            lastStatus = "Android EQ enabled · " + profileName;
            return lastStatus;
        } catch (Throwable error) {
            lastStatus = "Android EQ unavailable on this phone";
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
