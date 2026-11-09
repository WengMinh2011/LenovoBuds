package com.lenovobuds.app;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** EQ profiles use five broad bands: 60, 230, 910, 3600 and 14000 Hz. Values are dB. */
public final class ProfileCatalog {
    private ProfileCatalog() {}

    public static final String MUSIC = "Music";
    public static final String BALANCED = "Balanced";
    public static final String FPS = "FPS";
    public static final String MOBA = "MOBA";
    public static final String RACING = "Racing";
    public static final String RPG = "RPG";

    private static final Map<String, float[]> PROFILES = new LinkedHashMap<>();
    static {
        PROFILES.put(MUSIC, new float[]{2.5f, 1.5f, 0.0f, 1.0f, 2.0f});
        PROFILES.put(BALANCED, new float[]{0f, 0f, 0f, 0f, 0f});
        PROFILES.put(FPS, new float[]{-2.0f, 0.5f, 2.5f, 3.5f, 2.0f});
        PROFILES.put(MOBA, new float[]{1.0f, 1.5f, 1.5f, 2.5f, 1.0f});
        PROFILES.put(RACING, new float[]{3.0f, 1.5f, 0.0f, 1.0f, 1.5f});
        PROFILES.put(RPG, new float[]{2.0f, 1.0f, 0.0f, 1.5f, 2.0f});
    }

    public static String[] names() { return PROFILES.keySet().toArray(new String[0]); }
    public static float[] gains(String name) {
        float[] value = PROFILES.get(name);
        return value == null ? Arrays.copyOf(PROFILES.get(BALANCED), 5) : Arrays.copyOf(value, 5);
    }
}
