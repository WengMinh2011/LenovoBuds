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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
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
    private static final int COLOR_BG = Color.rgb(11, 17, 24);
    private static final int COLOR_CARD = Color.rgb(20, 29, 39);
    private static final int COLOR_TEXT = Color.rgb(239, 245, 250);
    private static final int COLOR_MUTED = Color.rgb(157, 174, 190);
    private static final int COLOR_ACCENT = Color.rgb(114, 224, 186);

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

    private LinearLayout page;
    private TextView deviceStatus;
    private TextView batteryStatus;
    private TextView modelStatus;
    private TextView eqStatus;
    private TextView modeStatus;
    private RadioGroup modeGroup;
    private Switch autoSwitch;
    private Spinner profileSpinner;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        prefs = getSharedPreferences("lenovo_buds", MODE_PRIVATE);
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (manager != null) bluetoothAdapter = manager.getAdapter();
        buildScreen();
        refreshModeUi();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BG);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(22), dp(18), dp(34));
        scroll.addView(page);
        setContentView(scroll);

        TextView brand = text("LENOVOBUDS", 13, COLOR_ACCENT, true);
        brand.setLetterSpacing(0.13f);
        page.addView(brand);
        TextView title = text("Your earbuds,\nyour settings.", 30, COLOR_TEXT, true);
        LinearLayout.LayoutParams titleLp = params(-1, -2);
        titleLp.topMargin = dp(6);
        page.addView(title, titleLp);
        TextView intro = text("Unofficial Lenovo earbuds companion · early preview", 13, COLOR_MUTED, false);
        LinearLayout.LayoutParams introLp = params(-1, -2);
        introLp.topMargin = dp(8);
        page.addView(intro, introLp);

        buildDeviceCard();
        buildModeCard();
        buildEqCard();
        buildSupportCard();

        TextView footer = text("Pair your Lenovo earbuds in Android Settings. This preview changes phone-side EQ only; app control of the earbuds’ built-in Game/Music mode is not verified yet.", 12, COLOR_MUTED, false);
        footer.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams footerLp = params(-1, -2);
        footerLp.topMargin = dp(10);
        page.addView(footer, footerLp);
    }

    private void buildDeviceCard() {
        LinearLayout card = card();
        addSectionHeading(card, "DEVICE", "Connect and inspect");
        deviceStatus = text("No earbud BLE link", 17, COLOR_TEXT, true);
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(12);
        card.addView(deviceStatus, statusLp);
        modelStatus = text("Targets: thinkplus GM2 Pro + LE258 · experimental name detection", 12, COLOR_MUTED, false);
        LinearLayout.LayoutParams modelLp = params(-1, -2);
        modelLp.topMargin = dp(5);
        card.addView(modelStatus, modelLp);
        batteryStatus = text("Battery: not available", 13, COLOR_MUTED, false);
        LinearLayout.LayoutParams batteryLp = params(-1, -2);
        batteryLp.topMargin = dp(9);
        card.addView(batteryStatus, batteryLp);
        Button scan = button("Find / connect earbuds");
        scan.setOnClickListener(v -> beginScan());
        LinearLayout.LayoutParams scanLp = params(-1, dp(48));
        scanLp.topMargin = dp(14);
        card.addView(scan, scanLp);
        TextView note = text("Scan looks for BLE advertisements. Some earbuds expose only classic Bluetooth audio and may not appear here.", 11, COLOR_MUTED, false);
        note.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams noteLp = params(-1, -2);
        noteLp.topMargin = dp(9);
        card.addView(note, noteLp);
        page.addView(card, cardMargins());
    }

    private void buildModeCard() {
        LinearLayout card = card();
        addSectionHeading(card, "APP AUDIO PROFILE", "Choose this phone’s audio profile");
        modeGroup = new RadioGroup(this);
        modeGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton music = radio("Music", 1);
        RadioButton gaming = radio("Gaming", 2);
        modeGroup.addView(music, new RadioGroup.LayoutParams(0, dp(48), 1f));
        modeGroup.addView(gaming, new RadioGroup.LayoutParams(0, dp(48), 1f));
        LinearLayout.LayoutParams groupLp = params(-1, dp(52));
        groupLp.topMargin = dp(14);
        card.addView(modeGroup, groupLp);
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == 1) setMode(false, null);
            else if (checkedId == 2) setMode(true, selectedProfile());
        });
        modeStatus = text("Local app profile · Music", 12, COLOR_MUTED, false);
        LinearLayout.LayoutParams modeLp = params(-1, -2);
        modeLp.topMargin = dp(7);
        card.addView(modeStatus, modeLp);
        TextView hardwareNote = text("Earbud Game/Music control from this app is not verified for GM2 Pro or LE258. This selector changes only the phone-side audio profile.", 11, COLOR_MUTED, false);
        hardwareNote.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams hardwareLp = params(-1, -2);
        hardwareLp.topMargin = dp(7);
        card.addView(hardwareNote, hardwareLp);

        autoSwitch = new Switch(this);
        autoSwitch.setText("Auto-switch when a game opens");
        autoSwitch.setTextColor(COLOR_TEXT);
        autoSwitch.setTextSize(14);
        autoSwitch.setPadding(0, dp(5), 0, dp(5));
        autoSwitch.setChecked(prefs.getBoolean("auto_game_mode", false));
        LinearLayout.LayoutParams autoLp = params(-1, -2);
        autoLp.topMargin = dp(12);
        card.addView(autoSwitch, autoLp);
        autoSwitch.setOnCheckedChangeListener((buttonView, checked) -> onAutoSwitchChanged(checked));
        TextView helper = text("Automatic detection uses Android Usage Access. You can revoke it any time in system settings.", 11, COLOR_MUTED, false);
        LinearLayout.LayoutParams helperLp = params(-1, -2);
        helperLp.topMargin = dp(4);
        card.addView(helper, helperLp);
        page.addView(card, cardMargins());
    }

    private void buildEqCard() {
        LinearLayout card = card();
        addSectionHeading(card, "EQUALIZER", "Choose a starting curve");
        profileSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ProfileCatalog.names());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        profileSpinner.setAdapter(adapter);
        profileSpinner.setSelection(indexOf(ProfileCatalog.names(), prefs.getString("active_profile", ProfileCatalog.FPS)));
        LinearLayout.LayoutParams spinnerLp = params(-1, dp(48));
        spinnerLp.topMargin = dp(12);
        card.addView(profileSpinner, spinnerLp);
        profileSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String profile = ProfileCatalog.names()[position];
                prefs.edit().putString("active_profile", profile).apply();
                if (prefs.getBoolean("gaming_mode", false)) {
                    AudioProfileEngine.apply(profile);
                    updateEqStatus();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        Button manage = button("Assign EQ profiles to installed games");
        manage.setOnClickListener(v -> showGameProfiles());
        LinearLayout.LayoutParams manageLp = params(-1, dp(46));
        manageLp.topMargin = dp(12);
        card.addView(manage, manageLp);
        eqStatus = text("EQ: waiting for selection", 12, COLOR_MUTED, false);
        LinearLayout.LayoutParams eqLp = params(-1, -2);
        eqLp.topMargin = dp(10);
        card.addView(eqStatus, eqLp);
        TextView warning = text("Android global EQ is best-effort. Some phones disable output-mix effects or do not route them to Bluetooth.", 11, COLOR_MUTED, false);
        warning.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams warningLp = params(-1, -2);
        warningLp.topMargin = dp(6);
        card.addView(warning, warningLp);
        page.addView(card, cardMargins());
    }

    private void buildSupportCard() {
        LinearLayout card = card();
        addSectionHeading(card, "PROTOCOL STATUS", "What works in this preview");
        addBullet(card, "BLE discovery and GATT service inspection");
        addBullet(card, "Standard Bluetooth Battery Service (if the buds expose it)");
        addBullet(card, "Local EQ presets and game-to-profile assignments");
        addBullet(card, "Automatic Music / Gaming selection via Usage Access");
        TextView pending = text("Remote Game/Music control, touch remapping, case battery and onboard EQ are not implemented for GM2 Pro or LE258. No guessed vendor packets are sent.", 12, COLOR_ACCENT, false);
        pending.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams pendingLp = params(-1, -2);
        pendingLp.topMargin = dp(12);
        card.addView(pending, pendingLp);
        page.addView(card, cardMargins());
    }

    private void onAutoSwitchChanged(boolean checked) {
        if (!checked) {
            prefs.edit().putBoolean("auto_game_mode", false).apply();
            stopService(new Intent(this, GameDetectionService.class));
            refreshModeUi();
            return;
        }
        if (!GameUtils.hasUsageAccess(this)) {
            pendingUsageSettings = true;
            autoSwitch.setOnCheckedChangeListener(null);
            autoSwitch.setChecked(false);
            autoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onAutoSwitchChanged(isChecked));
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

    private void startAutoService() {
        prefs.edit().putBoolean("auto_game_mode", true).apply();
        Intent service = new Intent(this, GameDetectionService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
            Toast.makeText(this, "Automatic game switching enabled", Toast.LENGTH_SHORT).show();
        } catch (Throwable error) {
            prefs.edit().putBoolean("auto_game_mode", false).apply();
            if (autoSwitch != null) autoSwitch.setChecked(false);
            Toast.makeText(this, "Android blocked the background monitor on this device", Toast.LENGTH_LONG).show();
        }
        refreshModeUi();
    }

    private void setMode(boolean gaming, String profile) {
        String selected = gaming ? (profile == null ? selectedProfile() : profile) : ProfileCatalog.MUSIC;
        prefs.edit().putBoolean("gaming_mode", gaming).putString("active_profile", selected).apply();
        AudioProfileEngine.apply(selected);
        updateEqStatus();
        modeStatus.setText("Local app profile · " + (gaming ? "Gaming" : "Music") + " · " + selected + " (earbud mode unchanged)");
        refreshModeUi();
    }

    private void refreshModeUi() {
        if (modeGroup != null) {
            int selected = prefs.getBoolean("gaming_mode", false) ? 2 : 1;
            if (modeGroup.getCheckedRadioButtonId() != selected) modeGroup.check(selected);
        }
        if (autoSwitch != null && autoSwitch.isChecked() != prefs.getBoolean("auto_game_mode", false)) {
            autoSwitch.setOnCheckedChangeListener(null);
            autoSwitch.setChecked(prefs.getBoolean("auto_game_mode", false));
            autoSwitch.setOnCheckedChangeListener((buttonView, checked) -> onAutoSwitchChanged(checked));
        }
        if (modeStatus != null) {
            String mode = prefs.getBoolean("gaming_mode", false) ? "Gaming" : "Music";
            String profile = prefs.getString("active_profile", ProfileCatalog.MUSIC);
            modeStatus.setText("Local app profile · " + mode + " · " + profile + " (earbud mode unchanged)");
        }
        updateEqStatus();
    }

    private void updateEqStatus() {
        if (eqStatus != null) eqStatus.setText(AudioProfileEngine.status());
    }

    private String selectedProfile() {
        if (profileSpinner != null && profileSpinner.getSelectedItem() != null)
            return String.valueOf(profileSpinner.getSelectedItem());
        return prefs.getString("active_profile", ProfileCatalog.FPS);
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
        String[] profiles = ProfileCatalog.names();
        String current = prefs.getString("game_profile_" + packageName, ProfileCatalog.FPS);
        int selected = indexOf(profiles, current);
        new AlertDialog.Builder(this).setTitle(gameName + " EQ")
                .setSingleChoiceItems(profiles, selected, (dialog, which) -> {
                    String profile = profiles[which];
                    prefs.edit().putString("game_profile_" + packageName, profile).apply();
                    Toast.makeText(this, gameName + " → " + profile, Toast.LENGTH_SHORT).show();
                    if (packageName.equals(prefs.getString("active_game", ""))) {
                        prefs.edit().putString("active_profile", profile).apply();
                        AudioProfileEngine.apply(profile);
                    }
                    dialog.dismiss();
                    refreshModeUi();
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
            showDevicePicker("Could not start BLE scan. Check Bluetooth and location settings.");
        }
    }

    private void addPairedDevices() {
        try {
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
            String name = safeDeviceName(d);
            labels[i] = name + "\n" + d.getAddress();
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
            modelStatus.setText("Checking advertised name and GATT services…");
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
                    String label = safeDeviceName(bluetoothGatt.getDevice());
                    modelStatus.setText(detectModel(label) + " · name match only; protocol not verified");
                    try { bluetoothGatt.discoverServices(); } catch (Throwable ignored) {}
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    deviceStatus.setText("BLE disconnected");
                    batteryStatus.setText("Battery: unavailable");
                    if (gatt == bluetoothGatt) gatt = null;
                    try { bluetoothGatt.close(); } catch (Throwable ignored) {}
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    deviceStatus.setText("BLE connection error · " + status);
                }
            });
        }

        @Override public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> modelStatus.setText("GATT service discovery failed"));
                return;
            }
            BluetoothGattService battery = bluetoothGatt.getService(BATTERY_SERVICE);
            BluetoothGattCharacteristic level = battery == null ? null : battery.getCharacteristic(BATTERY_LEVEL);
            runOnUiThread(() -> {
                deviceStatus.setText("BLE link connected");
                if (level == null) batteryStatus.setText("Battery: standard battery service not exposed");
                else batteryStatus.setText("Battery service found · reading…");
            });
            if (level != null) {
                try { bluetoothGatt.readCharacteristic(level); } catch (Throwable ignored) {}
            }
        }

        @Override public void onCharacteristicRead(BluetoothGatt bluetoothGatt,
                BluetoothGattCharacteristic characteristic, int status) {
            updateBatteryRead(characteristic, characteristic.getValue(), status);
        }

        @Override public void onCharacteristicRead(BluetoothGatt bluetoothGatt,
                BluetoothGattCharacteristic characteristic, byte[] value, int status) {
            updateBatteryRead(characteristic, value, status);
        }
    };

    private void updateBatteryRead(BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if (BATTERY_LEVEL.equals(characteristic.getUuid()) && status == BluetoothGatt.GATT_SUCCESS) {
            if (value != null && value.length > 0) {
                int percent = value[0] & 0xff;
                runOnUiThread(() -> batteryStatus.setText("Battery: " + percent + "% · standard BLE reading"));
            }
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
            if (autoSwitch != null && autoSwitch.isChecked() && GameUtils.hasUsageAccess(this)) startAutoService();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (pendingUsageSettings) {
            pendingUsageSettings = false;
            if (GameUtils.hasUsageAccess(this)) {
                if (autoSwitch != null) {
                    autoSwitch.setOnCheckedChangeListener(null);
                    autoSwitch.setChecked(true);
                    autoSwitch.setOnCheckedChangeListener((buttonView, checked) -> onAutoSwitchChanged(checked));
                }
                startAutoService();
            } else {
                Toast.makeText(this, "Usage Access was not enabled", Toast.LENGTH_SHORT).show();
            }
        }
        refreshModeUi();
    }

    @Override protected void onDestroy() {
        stopScan();
        if (gatt != null) {
            try { gatt.disconnect(); gatt.close(); } catch (Throwable ignored) {}
            gatt = null;
        }
        super.onDestroy();
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_CARD);
        bg.setCornerRadius(dp(18));
        layout.setBackground(bg);
        return layout;
    }

    private void addSectionHeading(LinearLayout card, String eyebrow, String heading) {
        TextView small = text(eyebrow, 10, COLOR_ACCENT, true);
        small.setLetterSpacing(0.12f);
        card.addView(small);
        TextView title = text(heading, 18, COLOR_TEXT, true);
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(5);
        card.addView(title, lp);
    }

    private void addBullet(LinearLayout card, String line) {
        TextView item = text("•  " + line, 12, COLOR_MUTED, false);
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(8);
        card.addView(item, lp);
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.rgb(8, 28, 23));
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_ACCENT);
        bg.setCornerRadius(dp(13));
        button.setBackground(bg);
        return button;
    }

    private RadioButton radio(String label, int id) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(label);
        button.setTextColor(COLOR_TEXT);
        button.setTextSize(14);
        button.setButtonTintList(android.content.res.ColorStateList.valueOf(COLOR_ACCENT));
        button.setGravity(Gravity.CENTER_VERTICAL);
        return button;
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
        lp.topMargin = dp(16);
        return lp;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        return 0;
    }
}
