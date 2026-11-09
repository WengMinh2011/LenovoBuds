package com.lenovobuds.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQ_BLUETOOTH = 301;
    private static final int REQ_NOTIFICATIONS = 302;
    private static final UUID BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    private static final int BG = Color.rgb(247, 245, 249);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(39, 28, 48);
    private static final int MUTED = Color.rgb(132, 123, 141);
    private static final int PURPLE = Color.rgb(133, 63, 178);
    private static final int PURPLE_LIGHT = Color.rgb(246, 235, 252);
    private static final int PINK = Color.rgb(235, 104, 165);
    private static final int LINE = Color.rgb(235, 229, 239);
    private static final int GREEN = Color.rgb(45, 153, 122);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private SharedPreferences prefs;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private ScanCallback scanCallback;
    private BluetoothGatt gatt;
    private boolean scanning = false;
    private boolean pendingUsageSettings = false;
    private boolean pendingBluetoothScan = false;
    private String selectedProfile = ProfileCatalog.MUSIC;
    private float[] currentGains = ProfileCatalog.gains(ProfileCatalog.MUSIC);

    private LinearLayout page;
    private TextView deviceStatus;
    private TextView batteryStatus;
    private TextView modelStatus;
    private TextView eqStatus;
    private TextView modeStatus;
    private TextView ancStatus;
    private LinearLayout profileChips;
    private EqualizerGraphView graphView;
    private Switch autoSwitch;
    private Button musicButton;
    private Button gamingButton;
    private Button ancButton;
    private Button ambientButton;
    private Button normalButton;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("lenovo_buds", MODE_PRIVATE);
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (manager != null) bluetoothAdapter = manager.getAdapter();
        selectedProfile = prefs.getString("active_profile", ProfileCatalog.MUSIC);
        currentGains = loadCurrentGains();
        buildScreen();
        refreshUi();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(34));
        scroll.addView(page);
        setContentView(scroll);

        buildHeader();
        buildDeviceCard();
        buildModeCard();
        buildNoiseCard();
        buildEqualizerCard();
        buildGameCard();

        TextView foot = text("LenovoBuds is an unofficial community project. ALPHA controls marked “not verified” are deliberately locked until the exact earbud hardware and Bluetooth command are confirmed.", 11, MUTED, false);
        foot.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams footLp = params(-1, -2);
        footLp.topMargin = dp(18);
        page.addView(foot, footLp);
    }

    private void buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(com.lenovobuds.app.R.drawable.lenovobuds_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView name = text("LenovoBuds", 20, TEXT, true);
        names.addView(name);
        TextView sub = text("YOUR AUDIO, YOUR WAY", 9, MUTED, true);
        sub.setLetterSpacing(0.13f);
        LinearLayout.LayoutParams subLp = params(-1, -2);
        subLp.topMargin = dp(2);
        names.addView(sub, subLp);
        LinearLayout.LayoutParams namesLp = new LinearLayout.LayoutParams(0, -2, 1f);
        namesLp.leftMargin = dp(10);
        row.addView(names, namesLp);

        TextView alpha = text("ALPHA 0.1", 10, PURPLE, true);
        alpha.setPadding(dp(11), dp(7), dp(11), dp(7));
        alpha.setBackground(pill(PURPLE_LIGHT, dp(18), Color.TRANSPARENT));
        row.addView(alpha);
        page.addView(row);

        TextView title = text("Sound, tuned to you.", 26, TEXT, true);
        LinearLayout.LayoutParams titleLp = params(-1, -2);
        titleLp.topMargin = dp(21);
        page.addView(title, titleLp);
        TextView intro = text("Connect your earbuds and shape every listening session.", 13, MUTED, false);
        LinearLayout.LayoutParams introLp = params(-1, -2);
        introLp.topMargin = dp(5);
        page.addView(intro, introLp);
    }

    private void buildDeviceCard() {
        LinearLayout card = card();
        addCardHeading(card, "YOUR DEVICE", "Earbuds");
        LinearLayout deviceRow = new LinearLayout(this);
        deviceRow.setOrientation(LinearLayout.HORIZONTAL);
        deviceRow.setGravity(Gravity.CENTER_VERTICAL);
        ImageView image = new ImageView(this);
        image.setImageResource(com.lenovobuds.app.R.drawable.lenovobuds_logo);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        image.setPadding(dp(8), dp(8), dp(8), dp(8));
        image.setBackground(pill(PURPLE_LIGHT, dp(16), Color.TRANSPARENT));
        deviceRow.addView(image, new LinearLayout.LayoutParams(dp(72), dp(72)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        TextView model = text("thinkplus GM2 Pro", 17, TEXT, true);
        details.addView(model);
        deviceStatus = text("Not connected", 12, MUTED, false);
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(4);
        details.addView(deviceStatus, statusLp);
        modelStatus = text("Target profile · GM2 Pro", 11, MUTED, false);
        LinearLayout.LayoutParams modelLp = params(-1, -2);
        modelLp.topMargin = dp(3);
        details.addView(modelStatus, modelLp);
        LinearLayout.LayoutParams detailsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        detailsLp.leftMargin = dp(13);
        deviceRow.addView(details, detailsLp);
        card.addView(deviceRow);

        LinearLayout batteryRow = new LinearLayout(this);
        batteryRow.setOrientation(LinearLayout.HORIZONTAL);
        batteryRow.setGravity(Gravity.CENTER_VERTICAL);
        batteryStatus = text("L  —%     R  —%     CASE  —%", 11, MUTED, true);
        LinearLayout.LayoutParams batteryLp = new LinearLayout.LayoutParams(0, -2, 1f);
        batteryLp.topMargin = dp(14);
        batteryRow.addView(batteryStatus, batteryLp);
        TextView batteryLabel = text("BATTERY", 9, MUTED, true);
        batteryLabel.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams batteryLabelLp = params(-2, -2);
        batteryLabelLp.topMargin = dp(14);
        batteryRow.addView(batteryLabel, batteryLabelLp);
        card.addView(batteryRow);

        Button connect = primaryButton("Connect earbuds");
        connect.setOnClickListener(v -> beginScan());
        LinearLayout.LayoutParams connectLp = params(-1, dp(47));
        connectLp.topMargin = dp(15);
        card.addView(connect, connectLp);
        TextView note = text("Pair first in Android Bluetooth settings. Battery is shown only when the earbuds expose a readable standard BLE service.", 10, MUTED, false);
        note.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams noteLp = params(-1, -2);
        noteLp.topMargin = dp(8);
        card.addView(note, noteLp);
        page.addView(card, cardMargins());
    }

    private void buildModeCard() {
        LinearLayout card = card();
        addCardHeading(card, "LISTENING MODE", "Choose your session");
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        musicButton = modeButton("Music");
        gamingButton = modeButton("Gaming");
        LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, dp(46), 1f);
        LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, dp(46), 1f);
        second.leftMargin = dp(9);
        buttons.addView(musicButton, first);
        buttons.addView(gamingButton, second);
        LinearLayout.LayoutParams buttonsLp = params(-1, dp(46));
        buttonsLp.topMargin = dp(13);
        card.addView(buttons, buttonsLp);
        modeStatus = text("Phone EQ: Music · Earbud low-latency control not verified", 11, MUTED, false);
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(10);
        card.addView(modeStatus, statusLp);
        page.addView(card, cardMargins());
    }

    private void buildNoiseCard() {
        LinearLayout card = card();
        addCardHeading(card, "NOISE CONTROL", "ANC & ambient sound");
        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        normalButton = modeButton("Off");
        ancButton = modeButton("ANC");
        ambientButton = modeButton("Ambient");
        normalButton.setOnClickListener(v -> {
            prefs.edit().putString("noise_profile", "Off").apply();
            updateNoiseUi();
        });
        ancButton.setOnClickListener(v -> showNoiseUnavailable("ANC"));
        ambientButton.setOnClickListener(v -> showNoiseUnavailable("Ambient sound"));
        ancButton.setEnabled(false);
        ambientButton.setEnabled(false);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(42), 1f);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(42), 1f);
        LinearLayout.LayoutParams c = new LinearLayout.LayoutParams(0, dp(42), 1f);
        b.leftMargin = dp(7); c.leftMargin = dp(7);
        modes.addView(normalButton, a);
        modes.addView(ancButton, b);
        modes.addView(ambientButton, c);
        LinearLayout.LayoutParams modesLp = params(-1, dp(42));
        modesLp.topMargin = dp(14);
        card.addView(modes, modesLp);
        ancStatus = text("ANC is locked in this ALPHA: the exact GM2 Pro revision's listening-mode ANC support and phone command have not been verified.", 11, MUTED, false);
        ancStatus.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(10);
        card.addView(ancStatus, statusLp);
        Button details = secondaryButton("Why are ANC controls locked?");
        details.setOnClickListener(v -> showNoiseUnavailable("ANC / Ambient"));
        LinearLayout.LayoutParams detailsLp = params(-1, dp(40));
        detailsLp.topMargin = dp(10);
        card.addView(details, detailsLp);
        page.addView(card, cardMargins());
    }

    private void buildEqualizerCard() {
        LinearLayout card = card();
        addCardHeading(card, "EQUALIZER", "Make it sound right");
        profileChips = new LinearLayout(this);
        profileChips.setOrientation(LinearLayout.HORIZONTAL);
        profileChips.setGravity(Gravity.CENTER_VERTICAL);
        profileChips.setPadding(0, dp(5), 0, dp(4));
        ScrollView horizontal = new ScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setVerticalScrollBarEnabled(false);
        horizontal.setFillViewport(false);
        horizontal.setHorizontalFadingEdgeEnabled(false);
        horizontal.addView(profileChips);
        LinearLayout.LayoutParams horizontalLp = params(-1, dp(50));
        horizontalLp.topMargin = dp(8);
        card.addView(horizontal, horizontalLp);
        rebuildProfileChips();

        graphView = new EqualizerGraphView(this);
        graphView.setGains(currentGains);
        graphView.setOnGainsChanged(gains -> {
            currentGains = gains;
            selectedProfile = ProfileCatalog.CUSTOM;
            prefs.edit().putString("active_profile", selectedProfile)
                    .putString("custom_eq_gains", encodeGains(gains)).apply();
            if (prefs.getBoolean("gaming_mode", false)) {
                eqStatus.setText(AudioProfileEngine.applyGains(selectedProfile, gains));
            } else {
                eqStatus.setText("Custom curve saved · select Gaming or press Apply to preview");
            }
            rebuildProfileChips();
        });
        LinearLayout.LayoutParams graphLp = params(-1, dp(190));
        graphLp.topMargin = dp(3);
        card.addView(graphView, graphLp);

        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        String[] labels = {"BASS", "LOW", "MID", "HIGH", "TREBLE"};
        for (String label : labels) {
            TextView t = text(label, 8, MUTED, true);
            t.setGravity(Gravity.CENTER);
            legend.addView(t, new LinearLayout.LayoutParams(0, dp(20), 1f));
        }
        card.addView(legend);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        Button reset = secondaryButton("Reset");
        reset.setOnClickListener(v -> selectProfile(selectedProfile.equals(ProfileCatalog.CUSTOM) ? ProfileCatalog.BALANCED : selectedProfile));
        Button apply = primaryButton("Apply EQ");
        apply.setOnClickListener(v -> {
            String status = AudioProfileEngine.applyGains(selectedProfile, currentGains);
            eqStatus.setText(status);
            Toast.makeText(this, "Applied " + selectedProfile + " phone EQ", Toast.LENGTH_SHORT).show();
        });
        controls.addView(reset, new LinearLayout.LayoutParams(0, dp(43), 1f));
        LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(0, dp(43), 1f);
        applyLp.leftMargin = dp(8);
        controls.addView(apply, applyLp);
        LinearLayout.LayoutParams controlsLp = params(-1, dp(43));
        controlsLp.topMargin = dp(12);
        card.addView(controls, controlsLp);
        eqStatus = text("Drag the five points to shape your curve.", 11, MUTED, false);
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(10);
        card.addView(eqStatus, statusLp);
        TextView limitation = text("Phone-side EQ is best-effort. Android may block global EQ or leave Bluetooth output unchanged.", 10, MUTED, false);
        LinearLayout.LayoutParams limitationLp = params(-1, -2);
        limitationLp.topMargin = dp(5);
        card.addView(limitation, limitationLp);
        page.addView(card, cardMargins());
    }

    private void buildGameCard() {
        LinearLayout card = card();
        addCardHeading(card, "GAME AUDIO", "Automatic game profiles");
        autoSwitch = new Switch(this);
        autoSwitch.setText("Switch when a game opens");
        autoSwitch.setTextColor(TEXT);
        autoSwitch.setTextSize(13);
        autoSwitch.setChecked(prefs.getBoolean("auto_game_mode", false));
        LinearLayout.LayoutParams switchLp = params(-1, -2);
        switchLp.topMargin = dp(9);
        card.addView(autoSwitch, switchLp);
        autoSwitch.setOnCheckedChangeListener((button, checked) -> onAutoSwitchChanged(checked));

        Button manage = secondaryButton("Assign profiles to installed games");
        manage.setOnClickListener(v -> showGameProfiles());
        LinearLayout.LayoutParams manageLp = params(-1, dp(42));
        manageLp.topMargin = dp(10);
        card.addView(manage, manageLp);
        TextView tip = text("FPS · MOBA · Racing · RPG · Music. Auto detection requires Android Usage Access.", 10, MUTED, false);
        LinearLayout.LayoutParams tipLp = params(-1, -2);
        tipLp.topMargin = dp(8);
        card.addView(tip, tipLp);
        page.addView(card, cardMargins());
    }

    private void onAutoSwitchChanged(boolean checked) {
        if (!checked) {
            prefs.edit().putBoolean("auto_game_mode", false).apply();
            stopService(new Intent(this, GameDetectionService.class));
            return;
        }
        if (!GameUtils.hasUsageAccess(this)) {
            pendingUsageSettings = true;
            setAutoSwitchChecked(false);
            Toast.makeText(this, "Enable Usage Access for LenovoBuds, then return here.", Toast.LENGTH_LONG).show();
            try { startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            catch (Throwable error) { Toast.makeText(this, "Could not open Usage Access settings", Toast.LENGTH_SHORT).show(); }
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
        startAutoService();
    }

    private void setAutoSwitchChecked(boolean checked) {
        if (autoSwitch == null) return;
        autoSwitch.setOnCheckedChangeListener(null);
        autoSwitch.setChecked(checked);
        autoSwitch.setOnCheckedChangeListener((button, value) -> onAutoSwitchChanged(value));
    }

    private void startAutoService() {
        prefs.edit().putBoolean("auto_game_mode", true).apply();
        Intent service = new Intent(this, GameDetectionService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
            setAutoSwitchChecked(true);
            Toast.makeText(this, "Automatic game profiles enabled", Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            prefs.edit().putBoolean("auto_game_mode", false).apply();
            setAutoSwitchChecked(false);
            Toast.makeText(this, "Android blocked the background game monitor", Toast.LENGTH_LONG).show();
        }
    }

    private void selectProfile(String profile) {
        selectedProfile = profile;
        currentGains = profile.equals(ProfileCatalog.CUSTOM)
                ? ProfileCatalog.customGains(prefs) : ProfileCatalog.gains(profile);
        prefs.edit().putString("active_profile", profile).apply();
        if (graphView != null) graphView.setGains(currentGains);
        rebuildProfileChips();
        if (eqStatus != null) eqStatus.setText("Selected " + profile + " · press Apply EQ to preview");
        if (profile.equals(ProfileCatalog.MUSIC)) {
            prefs.edit().putBoolean("gaming_mode", false).apply();
        } else {
            prefs.edit().putBoolean("gaming_mode", true).apply();
        }
        refreshModeButtons();
    }

    private void setListeningMode(boolean gaming) {
        if (gaming) {
            if (selectedProfile.equals(ProfileCatalog.MUSIC)) selectProfile(ProfileCatalog.FPS);
            prefs.edit().putBoolean("gaming_mode", true).apply();
            eqStatus.setText(AudioProfileEngine.applyGains(selectedProfile, currentGains));
            modeStatus.setText("Gaming EQ selected · earbud low-latency command not yet verified");
        } else {
            prefs.edit().putBoolean("gaming_mode", false).apply();
            selectProfile(ProfileCatalog.MUSIC);
            modeStatus.setText("Music EQ selected · phone-side audio profile");
        }
        refreshModeButtons();
    }

    private void refreshModeButtons() {
        boolean gaming = prefs.getBoolean("gaming_mode", false);
        if (musicButton != null) styleModeButton(musicButton, !gaming);
        if (gamingButton != null) styleModeButton(gamingButton, gaming);
    }

    private void updateNoiseUi() {
        String value = prefs.getString("noise_profile", "Off");
        styleModeButton(normalButton, "Off".equals(value));
        ancStatus.setText("ANC / ambient controls remain locked until supported by this exact earbud revision and a real control command is verified.");
    }

    private void showNoiseUnavailable(String feature) {
        new AlertDialog.Builder(this)
                .setTitle(feature + " control unavailable")
                .setMessage("This ALPHA does not yet have a verified ANC or transparency command for the GM2 Pro. GM2 Pro documentation is inconsistent: one specification table claims ANC, while other text describes ENC for call microphones. ENC is not the same as listening-mode ANC. The app will not pretend this switch changes the earbuds.")
                .setPositiveButton("Got it", null).show();
    }

    private void rebuildProfileChips() {
        if (profileChips == null) return;
        profileChips.removeAllViews();
        for (String name : ProfileCatalog.names()) {
            Button chip = chipButton(name, name.equals(selectedProfile));
            chip.setOnClickListener(v -> selectProfile(name));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(36));
            lp.rightMargin = dp(7);
            profileChips.addView(chip, lp);
        }
    }

    private float[] loadCurrentGains() {
        String profile = prefs.getString("active_profile", ProfileCatalog.MUSIC);
        return profile.equals(ProfileCatalog.CUSTOM) ? ProfileCatalog.customGains(prefs) : ProfileCatalog.gains(profile);
    }

    private String encodeGains(float[] gains) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < gains.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(gains[i]);
        }
        return builder.toString();
    }

    private void refreshUi() {
        refreshModeButtons();
        if (ancStatus != null) updateNoiseUi();
        if (autoSwitch != null) setAutoSwitchChecked(prefs.getBoolean("auto_game_mode", false));
    }

    private void showGameProfiles() {
        List<ResolveInfo> launchables;
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchables = getPackageManager().queryIntentActivities(intent, 0);
        } catch (Throwable error) {
            Toast.makeText(this, "Could not read installed apps", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, String> games = new LinkedHashMap<>();
        for (ResolveInfo info : launchables) {
            if (info.activityInfo == null || info.activityInfo.applicationInfo == null) continue;
            ApplicationInfo app = info.activityInfo.applicationInfo;
            String pkg = app.packageName;
            boolean categoryGame = Build.VERSION.SDK_INT >= 26 && app.category == ApplicationInfo.CATEGORY_GAME;
            if (!categoryGame && !GameUtils.isKnownGamePackage(pkg)) continue;
            try { games.put(pkg, getPackageManager().getApplicationLabel(app).toString()); }
            catch (Throwable ignored) { games.put(pkg, pkg); }
        }
        if (games.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Game profiles")
                    .setMessage("No games were identified. Install a game or use a phone that labels games in Android's app list.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        List<String> packages = new ArrayList<>(games.keySet());
        String[] labels = new String[packages.size()];
        for (int i = 0; i < packages.size(); i++) labels[i] = games.get(packages.get(i));
        new AlertDialog.Builder(this).setTitle("Choose a game")
                .setItems(labels, (dialog, which) -> chooseGameProfile(packages.get(which), labels[which])).show();
    }

    private void chooseGameProfile(String packageName, String gameName) {
        String[] profiles = {ProfileCatalog.FPS, ProfileCatalog.MOBA, ProfileCatalog.RACING, ProfileCatalog.RPG, ProfileCatalog.MUSIC, ProfileCatalog.BALANCED};
        String current = prefs.getString("game_profile_" + packageName, ProfileCatalog.FPS);
        int selected = 0;
        for (int i = 0; i < profiles.length; i++) if (profiles[i].equals(current)) selected = i;
        new AlertDialog.Builder(this).setTitle(gameName + " EQ")
                .setSingleChoiceItems(profiles, selected, (dialog, which) -> {
                    String profile = profiles[which];
                    prefs.edit().putString("game_profile_" + packageName, profile).apply();
                    Toast.makeText(this, gameName + " → " + profile, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }).setNegativeButton("Close", null).show();
    }

    private void beginScan() {
        if (!hasBluetoothPermissions()) {
            pendingBluetoothScan = true;
            requestBluetoothPermissions();
            return;
        }
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This phone has no Bluetooth adapter", Toast.LENGTH_LONG).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            try { startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); }
            catch (Throwable ignored) { Toast.makeText(this, "Enable Bluetooth in Settings", Toast.LENGTH_SHORT).show(); }
            return;
        }
        devices.clear();
        addPairedDevices();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            showDevicePicker("BLE scanning is unavailable. You can still try a paired device.");
            return;
        }
        scanCallback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                if (device != null) devices.put(device.getAddress(), device);
            }
            @Override public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) if (result.getDevice() != null)
                    devices.put(result.getDevice().getAddress(), result.getDevice());
            }
            @Override public void onScanFailed(int errorCode) {
                scanning = false;
                runOnUiThread(() -> showDevicePicker("BLE scan failed (" + errorCode + "). Paired devices are still listed."));
            }
        };
        try {
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            bleScanner.startScan(null, settings, scanCallback);
            scanning = true;
            deviceStatus.setText("Scanning nearby Bluetooth LE devices…");
            handler.postDelayed(() -> {
                stopScan();
                showDevicePicker("Select your Lenovo earbuds from nearby or paired devices.");
            }, 8000L);
        } catch (SecurityException error) {
            Toast.makeText(this, "Bluetooth permission was not granted", Toast.LENGTH_LONG).show();
        } catch (Throwable error) {
            showDevicePicker("Could not start BLE scan. Check Bluetooth settings.");
        }
    }

    private void addPairedDevices() {
        try {
            if (bluetoothAdapter != null)
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                    devices.put(device.getAddress(), device);
        } catch (SecurityException ignored) {}
    }

    private void stopScan() {
        if (scanning && bleScanner != null && scanCallback != null) {
            try { bleScanner.stopScan(scanCallback); } catch (Throwable ignored) {}
        }
        scanning = false;
    }

    private void showDevicePicker(String message) {
        addPairedDevices();
        if (devices.isEmpty()) {
            deviceStatus.setText("No BLE or paired device found");
            new AlertDialog.Builder(this).setTitle("No earbuds found").setMessage(message + "\n\nPair the earbuds in Android Bluetooth Settings, then try again.")
                    .setPositiveButton("Bluetooth settings", (d, w) -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                    .setNegativeButton("Close", null).show();
            return;
        }
        List<BluetoothDevice> list = new ArrayList<>(devices.values());
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            BluetoothDevice d = list.get(i);
            labels[i] = safeDeviceName(d) + "\n" + d.getAddress();
        }
        new AlertDialog.Builder(this).setTitle("Found Bluetooth devices")
                .setMessage(message)
                .setItems(labels, (dialog, which) -> connectGatt(list.get(which))).show();
    }

    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null || name.trim().isEmpty() ? "Unnamed Bluetooth device" : name;
        } catch (SecurityException ignored) { return "Bluetooth device"; }
    }

    private void connectGatt(BluetoothDevice device) {
        if (!hasBluetoothPermissions()) { requestBluetoothPermissions(); return; }
        try {
            if (gatt != null) { gatt.disconnect(); gatt.close(); gatt = null; }
            String name = safeDeviceName(device);
            deviceStatus.setText("Connecting to " + name + "…");
            modelStatus.setText(detectModel(name) + " · name match only");
            gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } catch (Throwable error) {
            deviceStatus.setText("Could not open a BLE connection");
            Toast.makeText(this, "This device may use classic Bluetooth only", Toast.LENGTH_LONG).show();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            runOnUiThread(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    deviceStatus.setText("BLE connected · discovering services…");
                    modelStatus.setText(detectModel(safeDeviceName(bluetoothGatt.getDevice())) + " · checking services");
                    try { bluetoothGatt.discoverServices(); } catch (Throwable ignored) {}
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    deviceStatus.setText("Bluetooth audio may still be connected; BLE link disconnected");
                    if (gatt == bluetoothGatt) gatt = null;
                    try { bluetoothGatt.close(); } catch (Throwable ignored) {}
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    deviceStatus.setText("BLE connection error · " + status);
                }
            });
        }

        @Override public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            BluetoothGattService battery = bluetoothGatt.getService(BATTERY_SERVICE);
            BluetoothGattCharacteristic level = battery == null ? null : battery.getCharacteristic(BATTERY_LEVEL);
            runOnUiThread(() -> {
                deviceStatus.setText("BLE link connected");
                if (level == null) batteryStatus.setText("L —%     R —%     CASE —%  ·  service not exposed");
            });
            if (level != null) {
                try { bluetoothGatt.readCharacteristic(level); } catch (Throwable ignored) {}
            }
        }

        @Override public void onCharacteristicRead(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, int status) {
            updateBatteryRead(characteristic, characteristic.getValue(), status);
        }

        @Override public void onCharacteristicRead(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
            updateBatteryRead(characteristic, value, status);
        }
    };

    private void updateBatteryRead(BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if (BATTERY_LEVEL.equals(characteristic.getUuid()) && status == BluetoothGatt.GATT_SUCCESS && value != null && value.length > 0) {
            int percent = value[0] & 0xff;
            runOnUiThread(() -> batteryStatus.setText("L/R —%     BATTERY " + percent + "%     CASE —%"));
        }
    }

    private String detectModel(String name) {
        String normalized = name == null ? "" : name.toUpperCase(Locale.ROOT);
        if (normalized.contains("LE258")) return "Possible Lenovo LE258";
        if (normalized.contains("GM2") || normalized.contains("GM 2")) return "Possible thinkplus GM2 Pro";
        if (normalized.contains("LENOVO") || normalized.contains("THINKPLUS")) return "Lenovo/thinkplus device · model not confirmed";
        return "Device: " + (name == null ? "unknown name" : name);
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQ_BLUETOOTH);
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_BLUETOOTH);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_BLUETOOTH && pendingBluetoothScan) {
            pendingBluetoothScan = false;
            if (hasBluetoothPermissions()) beginScan();
            else Toast.makeText(this, "Bluetooth permissions are required to scan", Toast.LENGTH_LONG).show();
        } else if (requestCode == REQ_NOTIFICATIONS) {
            if (GameUtils.hasUsageAccess(this) && prefs.getBoolean("auto_game_mode", false)) startAutoService();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (pendingUsageSettings) {
            pendingUsageSettings = false;
            if (GameUtils.hasUsageAccess(this)) startAutoService();
            else Toast.makeText(this, "Usage Access was not enabled", Toast.LENGTH_SHORT).show();
        }
        refreshUi();
    }

    @Override protected void onDestroy() {
        stopScan();
        if (gatt != null) {
            try { gatt.disconnect(); gatt.close(); } catch (Throwable ignored) {}
            gatt = null;
        }
        AudioProfileEngine.release();
        super.onDestroy();
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(21));
        layout.setBackground(bg);
        layout.setElevation(dp(2));
        return layout;
    }

    private void addCardHeading(LinearLayout card, String eyebrow, String heading) {
        TextView small = text(eyebrow, 9, PURPLE, true);
        small.setLetterSpacing(0.13f);
        card.addView(small);
        TextView title = text(heading, 18, TEXT, true);
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(4);
        card.addView(title, lp);
    }

    private Button modeButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setPadding(dp(9), 0, dp(9), 0);
        styleModeButton(button, false);
        return button;
    }

    private void styleModeButton(Button button, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(selected ? PURPLE_LIGHT : Color.rgb(249, 247, 250));
        bg.setCornerRadius(dp(13));
        bg.setStroke(dp(1), selected ? PURPLE : LINE);
        button.setBackground(bg);
        button.setTextColor(selected ? PURPLE : MUTED);
        button.setAlpha(button.isEnabled() ? 1f : 0.42f);
    }

    private Button chipButton(String label, boolean selected) {
        Button button = modeButton(label);
        button.setTextSize(10);
        button.setPadding(dp(11), 0, dp(11), 0);
        styleModeButton(button, selected);
        return button;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{PURPLE, PINK});
        bg.setCornerRadius(dp(14));
        button.setBackground(bg);
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(PURPLE);
        button.setTextSize(11);
        button.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PURPLE_LIGHT);
        bg.setCornerRadius(dp(13));
        button.setBackground(bg);
        return button;
    }

    private GradientDrawable pill(int color, int radius, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(radius);
        if (stroke != Color.TRANSPARENT) bg.setStroke(dp(1), stroke);
        return bg;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams cardMargins() {
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(15);
        return lp;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
