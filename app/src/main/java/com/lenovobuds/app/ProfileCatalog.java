package com.lenovobuds.app;

import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Five-band profiles (60, 230, 910, 3600 and 14000 Hz), in dB. */
public final class ProfileCatalog {
    private ProfileCatalog() {}

    public static final String MUSIC = "Music";
    public static final String BALANCED = "Balanced";
    public static final String BASS = "Bass";
    public static final String VOCAL = "Vocal";
    public static final String FPS = "FPS";
    public static final String MOBA = "MOBA";
    public static final String RACING = "Racing";
    public static final String RPG = "RPG";
    public static final String CUSTOM = "Custom";

    private static final Map<String, float[]> PROFILES = new LinkedHashMap<>();
    static {
        PROFILES.put(MUSIC, new float[]{2.5f, 1.5f, 0.0f, 1.0f, 2.0f});
        PROFILES.put(BALANCED, new float[]{0f, 0f, 0f, 0f, 0f});
        PROFILES.put(BASS, new float[]{6f, 4f, 0.5f, -1f, 0f});
        PROFILES.put(VOCAL, new float[]{-1f, 0f, 2f, 4f, 2f});
        PROFILES.put(FPS, new float[]{-2f, 0.5f, 2.5f, 3.5f, 2f});
        PROFILES.put(MOBA, new float[]{1f, 1.5f, 1.5f, 2.5f, 1f});
        PROFILES.put(RACING, new float[]{3f, 1.5f, 0f, 1f, 1.5f});
        PROFILES.put(RPG, new float[]{2f, 1f, 0f, 1.5f, 2f});
        PROFILES.put(CUSTOM, new float[]{0f, 0f, 0f, 0f, 0f});
    }

    public static String[] names() { return PROFILES.keySet().toArray(new String[0]); }

    public static float[] gains(String name) {
        float[] value = PROFILES.get(name);
        return value == null ? Arrays.copyOf(PROFILES.get(BALANCED), 5) : Arrays.copyOf(value, 5);
    }

    public static float[] customGains(SharedPreferences prefs) {
        String encoded = prefs.getString("custom_eq_gains", "0,0,0,0,0");
        String[] pieces = encoded.split(",");
        if (pieces.length != 5) return gains(BALANCED);
        float[] result = new float[5];
        try {
            for (int i = 0; i < 5; i++) result[i] = Math.max(-8f, Math.min(8f, Float.parseFloat(pieces[i])));
            return result;
        } catch (NumberFormatException error) { return gains(BALANCED); }
    }
}
