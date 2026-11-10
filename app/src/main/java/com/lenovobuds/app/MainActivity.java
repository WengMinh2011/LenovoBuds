package com.lenovobuds.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.FrameLayout;
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

    private static final int BG = Color.rgb(244, 248, 250);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(31, 38, 43);
    private static final int MUTED = Color.rgb(128, 139, 145);
    private static final int PURPLE = Color.rgb(67, 165, 112);
    private static final int PURPLE_LIGHT = Color.rgb(233, 246, 239);
    private static final int PINK = Color.rgb(111, 190, 146);
    private static final int LINE = Color.rgb(231, 237, 239);
    private static final int GREEN = Color.rgb(45, 153, 122);
    private static final int CYAN = Color.rgb(43, 177, 204);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private SharedPreferences prefs;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
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
    private Button findDeviceButton;
    private Switch gamingModeSwitch;
    private EarbudsIllustrationView controlsIllustration;
    private String selectedEarbudSide = "Left";
    private String currentScreen = "home";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        prefs = getSharedPreferences("lenovo_buds", MODE_PRIVATE);
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) bluetoothAdapter = bluetoothManager.getAdapter();
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
        page.setPadding(dp(18), dp(12), dp(18), dp(30));
        scroll.addView(page);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars()
                                | WindowInsets.Type.displayCutout());
                topInset = bars.top;
                bottomInset = bars.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(view.getPaddingLeft(), topInset, view.getPaddingRight(), bottomInset);
            return insets;
        });
        setContentView(scroll);
        showHomeScreen();
    }

    private void resetPage(String screen) {
        currentScreen = screen;
        page.removeAllViews();
        page.setPadding(dp(18), dp(12), dp(18), dp(30));
        eqStatus = null; modeStatus = null; ancStatus = null;
        profileChips = null; graphView = null; autoSwitch = null;
        musicButton = null; gamingButton = null; ancButton = null;
        ambientButton = null; normalButton = null; gamingModeSwitch = null;
        findDeviceButton = null;
        controlsIllustration = null;
    }

    private void showHomeScreen() {
        resetPage("home");
        buildHeader();
        buildHomeHero();
        buildHomeFeatures();
        TextView foot = text("LenovoBuds ALPHA is an unofficial community project. GM2 Pro hardware controls remain previews until their Bluetooth commands are verified.", 10, MUTED, false);
        foot.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams lp = params(-1, -2);
        lp.topMargin = dp(15);
        page.addView(foot, lp);
        refreshUi();
    }

    private void showEqualizerScreen() {
        resetPage("sound");
        buildSubpageHeader("Sound Effects");
        buildEqualizerCard();
        buildGameCard();
        refreshUi();
    }

    private void showControlsScreen() {
        resetPage("controls");
        buildSubpageHeader("Controls");
        buildControlsContent();
    }

    private void buildSubpageHeader(String title) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = iconButton("‹");
        back.setOnClickListener(v -> showHomeScreen());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView heading = text(title, 19, TEXT, true);
        heading.setGravity(Gravity.CENTER);
        header.addView(heading, new LinearLayout.LayoutParams(0, dp(42), 1f));
        Button settings = iconButton("⚙");
        settings.setOnClickListener(v -> showSettingsSheet());
        header.addView(settings, new LinearLayout.LayoutParams(dp(42), dp(42)));
        page.addView(header);
    }

    private void buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(com.lenovobuds.app.R.drawable.lenovobuds_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        row.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(38)));
        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text("LenovoBuds", 17, TEXT, true));
        TextView sub = text("THINKPLUS COMPANION", 8, MUTED, true);
        sub.setLetterSpacing(0.13f);
        names.addView(sub);
        LinearLayout.LayoutParams namesLp = new LinearLayout.LayoutParams(0, -2, 1f);
        namesLp.leftMargin = dp(9);
        row.addView(names, namesLp);
        findDeviceButton = iconButton("⌕");
        findDeviceButton.setContentDescription("Find paired earbuds");
        findDeviceButton.setOnClickListener(v -> onFindDeviceClicked());
        LinearLayout.LayoutParams findLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        findLp.rightMargin = dp(7);
        row.addView(findDeviceButton, findLp);
        Button settings = iconButton("⚙");
        settings.setOnClickListener(v -> showSettingsSheet());
        row.addView(settings, new LinearLayout.LayoutParams(dp(42), dp(42)));
        page.addView(row);
    }

    private Button iconButton(String glyph) {
        Button b = secondaryButton(glyph);
        b.setTextSize(19);
        b.setPadding(0, 0, 0, dp(2));
        b.setBackground(pill(Color.WHITE, dp(14), Color.TRANSPARENT));
        b.setTextColor(TEXT);
        return b;
    }

    private void buildHomeHero() {
        String oldDevice = deviceStatus == null ? "Not connected" : deviceStatus.getText().toString();
        String oldModel = modelStatus == null ? "Target profile · GM2 Pro" : modelStatus.getText().toString();
        String oldBattery = batteryStatus == null ? "L  —%          R  —%" : batteryStatus.getText().toString();

        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(pill(Color.rgb(237, 245, 248), dp(25), Color.TRANSPARENT));
        hero.addView(new EarbudsIllustrationView(this),
                new FrameLayout.LayoutParams(-1, dp(196), Gravity.CENTER));
        TextView alpha = text("ALPHA 0.1.3", 9, PURPLE, true);
        alpha.setPadding(dp(10), dp(6), dp(10), dp(6));
        alpha.setBackground(pill(Color.WHITE, dp(20), Color.TRANSPARENT));
        FrameLayout.LayoutParams alphaLp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.LEFT);
        alphaLp.setMargins(dp(13), dp(13), 0, 0);
        hero.addView(alpha, alphaLp);
        LinearLayout.LayoutParams heroLp = params(-1, dp(205));
        heroLp.topMargin = dp(12);
        page.addView(hero, heroLp);

        TextView model = text("thinkplus GM2 Pro", 22, TEXT, true);
        model.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams modelLp = params(-1, -2);
        modelLp.topMargin = dp(11);
        page.addView(model, modelLp);
        deviceStatus = text(oldDevice, 11, MUTED, false);
        deviceStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(4);
        page.addView(deviceStatus, statusLp);
        modelStatus = text(oldModel, 10, MUTED, false);
        modelStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams detailLp = params(-1, -2);
        detailLp.topMargin = dp(3);
        page.addView(modelStatus, detailLp);

        batteryStatus = text(oldBattery, 12, TEXT, true);
        batteryStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams batteryLp = params(-1, dp(29));
        batteryLp.topMargin = dp(8);
        page.addView(batteryStatus, batteryLp);
        TextView hint = text("Battery readings depend on what Android exposes to the app.", 9, MUTED, false);
        hint.setGravity(Gravity.CENTER);
        page.addView(hint);
    }

    private void buildHomeFeatures() {
        LinearLayout features = card();
        addFeatureRow(features, "≋", "Sound Effects", "Phone-side EQ · " + selectedProfile, "›", () -> showEqualizerScreen());
        addDivider(features);
        addFeatureRow(features, "☝", "Controls", "Left / right touch actions", "›", () -> showControlsScreen());
        addDivider(features);

        LinearLayout gameRow = new LinearLayout(this);
        gameRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView tile = iconTile("◉");
        gameRow.addView(tile, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("Gaming Mode", 14, TEXT, true));
        TextView detail = text("Phone EQ only · earbud command unverified", 10, MUTED, false);
        LinearLayout.LayoutParams detailLp = params(-1, -2);
        detailLp.topMargin = dp(3);
        labels.addView(detail, detailLp);
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        labelsLp.leftMargin = dp(12);
        gameRow.addView(labels, labelsLp);
        gamingModeSwitch = new Switch(this);
        gamingModeSwitch.setChecked(prefs.getBoolean("gaming_mode", false));
        gamingModeSwitch.setOnCheckedChangeListener((button, checked) -> setListeningMode(checked));
        gameRow.addView(gamingModeSwitch);
        features.addView(gameRow, params(-1, dp(68)));
        addDivider(features);
        addFeatureRow(features, "▣", "Dual Connections", "Not verified for the GM2 Pro", "›", () -> showDualConnectionInfo());
        page.addView(features, cardMargins());
    }

    private void addFeatureRow(LinearLayout parent, String glyph, String title, String subtitle, String trailing, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = iconTile(glyph);
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 14, TEXT, true));
        TextView sub = text(subtitle, 10, MUTED, false);
        LinearLayout.LayoutParams subLp = params(-1, -2);
        subLp.topMargin = dp(3);
        labels.addView(sub, subLp);
        LinearLayout.LayoutParams labelsLp = new LinearLayout.LayoutParams(0, -2, 1f);
        labelsLp.leftMargin = dp(12);
        row.addView(labels, labelsLp);
        TextView arrow = text(trailing, 25, MUTED, false);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(25), -2));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18000000),
                pill(Color.WHITE, dp(12), Color.TRANSPARENT),
                pill(Color.WHITE, dp(12), Color.TRANSPARENT)));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> action.run());
        parent.addView(row, params(-1, dp(68)));
    }

    private TextView iconTile(String glyph) {
        TextView icon = text(glyph, 20, TEXT, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(pill(Color.rgb(242, 247, 248), dp(13), Color.TRANSPARENT));
        return icon;
    }

    private void addDivider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = params(-1, dp(1));
        lp.leftMargin = dp(54);
        parent.addView(line, lp);
    }

    private void showDualConnectionInfo() {
        new AlertDialog.Builder(this)
                .setTitle("Dual Connections")
                .setMessage("The GM2 Pro's dual-device connection support cannot currently be controlled from LenovoBuds. You can still manage Bluetooth pairings through Android settings.")
                .setPositiveButton("Open Bluetooth settings", (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                .setNegativeButton("Close", null).show();
    }

    private void buildControlsContent() {
        selectedEarbudSide = prefs.getString("selected_earbud_side", "Left");
        TextView intro = text("Choose an earbud to plan its touch actions.", 12, MUTED, false);
        LinearLayout.LayoutParams introLp = params(-1, -2);
        introLp.topMargin = dp(8);
        page.addView(intro, introLp);

        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(pill(Color.rgb(237, 245, 248), dp(24), Color.TRANSPARENT));
        controlsIllustration = new EarbudsIllustrationView(this);
        controlsIllustration.setSelectedSide("Left".equals(selectedEarbudSide));
        hero.addView(controlsIllustration, new FrameLayout.LayoutParams(-1, dp(160), Gravity.CENTER));
        LinearLayout.LayoutParams heroLp = params(-1, dp(165));
        heroLp.topMargin = dp(14);
        page.addView(hero, heroLp);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabs.setBackground(pill(Color.rgb(231, 237, 239), dp(26), Color.TRANSPARENT));
        Button left = modeButton("Left"), right = modeButton("Right");
        styleModeButton(left, "Left".equals(selectedEarbudSide));
        styleModeButton(right, "Right".equals(selectedEarbudSide));
        left.setOnClickListener(v -> selectEarbudSide("Left", left, right));
        right.setOnClickListener(v -> selectEarbudSide("Right", left, right));
        tabs.addView(left, new LinearLayout.LayoutParams(0, dp(38), 1f));
        tabs.addView(right, new LinearLayout.LayoutParams(0, dp(38), 1f));
        LinearLayout.LayoutParams tabsLp = params(-1, dp(46));
        tabsLp.topMargin = dp(14);
        page.addView(tabs, tabsLp);

        LinearLayout actions = card();
        renderTouchActionRows(actions);
        page.addView(actions, cardMargins());

        TextView note = text("These are saved preferences on this phone, not earbud remappings. The GM2 Pro's app control protocol has not been verified, so LenovoBuds cannot write these choices to the headset yet.", 10, MUTED, false);
        note.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteLp = params(-1, -2);
        noteLp.topMargin = dp(13);
        page.addView(note, noteLp);
    }

    private void selectEarbudSide(String side, Button left, Button right) {
        selectedEarbudSide = side;
        prefs.edit().putString("selected_earbud_side", side).apply();
        styleModeButton(left, "Left".equals(side));
        styleModeButton(right, "Right".equals(side));
        if (controlsIllustration != null) controlsIllustration.setSelectedSide("Left".equals(side));
        // Rebuild only the gesture card while preserving the selected-side tab.
        showControlsScreen();
    }

    private void renderTouchActionRows(LinearLayout parent) {
        parent.removeAllViews();
        String[] gestures = {"Single Tap", "Double Tap", "Triple Tap", "Tap and Hold"};
        for (int i = 0; i < gestures.length; i++) {
            if (i > 0) addDivider(parent);
            addTouchActionRow(parent, gestures[i]);
        }
    }

    private String gesturePreferenceKey(String gesture) {
        return "gesture_" + selectedEarbudSide.toLowerCase(Locale.ROOT) + "_"
                + gesture.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private void addTouchActionRow(LinearLayout parent, String title) {
        String saved = prefs.getString(gesturePreferenceKey(title), "Not set");
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = text("•", 25, CYAN, true);
        dot.setGravity(Gravity.CENTER);
        row.addView(dot, new LinearLayout.LayoutParams(dp(38), -1));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text(title, 14, TEXT, true));
        TextView subtitle = text(saved.equals("Not set")
                ? "Tap to set a local preference"
                : "Preferred: " + saved + " · local only", 10, MUTED, false);
        LinearLayout.LayoutParams slp = params(-1, -2);
        slp.topMargin = dp(3);
        labels.addView(subtitle, slp);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView arrow = text("›", 23, MUTED, false);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(25), -2));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x18000000),
                pill(Color.WHITE, dp(10), Color.TRANSPARENT),
                pill(Color.WHITE, dp(10), Color.TRANSPARENT)));
        row.setOnClickListener(v -> editGesturePreference(title));
        parent.addView(row, params(-1, dp(68)));
    }

    private void editGesturePreference(String title) {
        if (prefs.getBoolean("app_control_lock", false)) {
            new AlertDialog.Builder(this).setTitle("App Control Lock is on")
                    .setMessage("Turn off App Control Lock in Settings to edit your saved gesture preferences.")
                    .setPositiveButton("Got it", null).show();
            return;
        }
        String[] options = {
                "Not set", "Play / pause", "Next track", "Previous track",
                "Volume up", "Volume down", "Voice assistant", "Game / Music profile"
        };
        String current = prefs.getString(gesturePreferenceKey(title), "Not set");
        int selected = 0;
        for (int i = 0; i < options.length; i++) if (options[i].equals(current)) selected = i;
        new AlertDialog.Builder(this)
                .setTitle(selectedEarbudSide + " · " + title)
                .setSingleChoiceItems(options, selected, (dialog, which) -> {
                    prefs.edit().putString(gesturePreferenceKey(title), options[which]).apply();
                    dialog.dismiss();
                    showControlsScreen();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSettingsSheet() {
        Dialog dialog = new Dialog(this);
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(22), dp(17), dp(22), dp(24));
        sheet.setBackground(pill(Color.WHITE, dp(24), Color.TRANSPARENT));

        View handle = new View(this);
        handle.setBackground(pill(Color.rgb(220, 225, 228), dp(6), Color.TRANSPARENT));
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(dp(38), dp(5));
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        sheet.addView(handle, hlp);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.addView(text("Settings", 18, TEXT, true), new LinearLayout.LayoutParams(0, dp(48), 1f));
        Button close = iconButton("×");
        close.setOnClickListener(v -> dialog.dismiss());
        titleRow.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout.LayoutParams tlp = params(-1, dp(52));
        tlp.topMargin = dp(9);
        sheet.addView(titleRow, tlp);

        LinearLayout lockRow = new LinearLayout(this);
        lockRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.addView(text("App Control Lock", 14, TEXT, true));
        TextView detail = text("Locks gesture preferences inside LenovoBuds only. It does not disable touch controls on the headset.", 10, MUTED, false);
        detail.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams dlp = params(-1, -2);
        dlp.topMargin = dp(4);
        labels.addView(detail, dlp);
        lockRow.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch lockSwitch = new Switch(this);
        lockSwitch.setChecked(prefs.getBoolean("app_control_lock", false));
        lockSwitch.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean("app_control_lock", checked).apply());
        lockRow.addView(lockSwitch);
        LinearLayout.LayoutParams rlp = params(-1, -2);
        rlp.topMargin = dp(17);
        sheet.addView(lockRow, rlp);

        TextView note = text("This is an in-app setting. The GM2 Pro's hardware Control Lock command is not verified.", 10, MUTED, false);
        note.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams noteLp = params(-1, -2);
        noteLp.topMargin = dp(9);
        sheet.addView(note, noteLp);

        Button done = primaryButton("Done");
        done.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams doneLp = params(-1, dp(46));
        doneLp.topMargin = dp(20);
        sheet.addView(done, doneLp);

        dialog.setContentView(sheet);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM;
            lp.dimAmount = 0.48f;
            window.setAttributes(lp);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
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

        findDeviceButton = primaryButton("Find paired earbuds");
        findDeviceButton.setOnClickListener(v -> onFindDeviceClicked());
        LinearLayout.LayoutParams connectLp = params(-1, dp(47));
        connectLp.topMargin = dp(15);
        card.addView(findDeviceButton, connectLp);
        TextView note = text("First pair your earbuds in your phone's Bluetooth settings. LenovoBuds can identify a paired device; this does not guarantee that its private controls are supported.", 11, MUTED, false);
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
        musicButton.setOnClickListener(v -> setListeningMode(false));
        gamingButton.setOnClickListener(v -> setListeningMode(true));
        modeStatus = text("This switch changes the app's sound profile. It does not yet switch the GM2 Pro's built-in gaming mode.", 11, MUTED, false);
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
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        horizontal.setVerticalScrollBarEnabled(false);
        horizontal.setFillViewport(false);
        horizontal.setClipToPadding(false);
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
        eqStatus = text("Drag each point up to boost that frequency range, or down to reduce it. Changes are saved on this phone.", 11, MUTED, false);
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(10);
        card.addView(eqStatus, statusLp);
        TextView limitation = text("Important: this is phone-side EQ, not an earbud firmware setting. Android or your phone manufacturer may not apply it to Bluetooth audio.", 11, MUTED, false);
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
        TextView tip = text("Choose a profile for each game. Automatic switching needs Usage Access so Android can tell which app is in the foreground. Audio changes remain phone-side.", 11, MUTED, false);
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
            if (eqStatus != null) eqStatus.setText(AudioProfileEngine.applyGains(selectedProfile, currentGains));
            if (modeStatus != null) modeStatus.setText("Gaming EQ selected · earbud low-latency command not yet verified");
        } else {
            prefs.edit().putBoolean("gaming_mode", false).apply();
            selectProfile(ProfileCatalog.MUSIC);
            if (modeStatus != null) modeStatus.setText("Music EQ selected · phone-side audio profile");
        }
        refreshModeButtons();
    }

    private void refreshModeButtons() {
        boolean gaming = prefs.getBoolean("gaming_mode", false);
        if (musicButton != null) styleModeButton(musicButton, !gaming);
        if (gamingButton != null) styleModeButton(gamingButton, gaming);
        if (gamingModeSwitch != null) {
            gamingModeSwitch.setOnCheckedChangeListener(null);
            gamingModeSwitch.setChecked(gaming);
            gamingModeSwitch.setOnCheckedChangeListener((button, checked) -> setListeningMode(checked));
        }
    }

    private void updateNoiseUi() {
        String value = prefs.getString("noise_profile", "Off");
        if (normalButton != null) styleModeButton(normalButton, "Off".equals(value));
        if (ancStatus != null) ancStatus.setText("ANC / ambient controls remain locked until supported by this exact earbud revision and a real control command is verified.");
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

    private void onFindDeviceClicked() {
        if (findDeviceButton == null || !findDeviceButton.isEnabled()) return;
        deviceStatus.setText("Checking paired Bluetooth devices…");
        setFindButtonState("Checking Bluetooth…", false);
        try {
            beginScan();
        } catch (SecurityException error) {
            pendingBluetoothScan = false;
            if (deviceStatus != null) deviceStatus.setText("Bluetooth access was denied by Android.");
            setFindButtonState("Find paired earbuds", true);
            Toast.makeText(this, "Allow Nearby devices permission for LenovoBuds in Settings.", Toast.LENGTH_LONG).show();
        } catch (Throwable error) {
            pendingBluetoothScan = false;
            if (deviceStatus != null) deviceStatus.setText("Discovery failed: " + error.getClass().getSimpleName());
            setFindButtonState("Find paired earbuds", true);
            Toast.makeText(this, "Could not check paired devices. See the device status above.", Toast.LENGTH_LONG).show();
        }
    }

    private void setFindButtonState(String label, boolean enabled) {
        if (findDeviceButton == null) return;
        findDeviceButton.setText(label);
        findDeviceButton.setEnabled(enabled);
        findDeviceButton.setAlpha(enabled ? 1f : 0.72f);
    }

    private void beginScan() {
        if (!hasBluetoothPermissions()) {
            pendingBluetoothScan = true;
            if (deviceStatus != null) deviceStatus.setText("Waiting for Android Nearby devices permission…");
            setFindButtonState("Waiting for permission…", false);
            try {
                requestBluetoothPermissions();
            } catch (Throwable error) {
                pendingBluetoothScan = false;
                if (deviceStatus != null) deviceStatus.setText("Could not request Bluetooth permission.");
                setFindButtonState("Find paired earbuds", true);
                Toast.makeText(this, "Open LenovoBuds app settings and allow Nearby devices.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (bluetoothAdapter == null) {
            if (deviceStatus != null) deviceStatus.setText("This phone does not expose a Bluetooth adapter.");
            setFindButtonState("Find paired earbuds", true);
            Toast.makeText(this, "Bluetooth is unavailable on this phone.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            if (deviceStatus != null) deviceStatus.setText("Bluetooth is off. Turn it on, then tap Find paired earbuds.");
            setFindButtonState("Find paired earbuds", true);
            try {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } catch (Throwable error) {
                Toast.makeText(this, "Enable Bluetooth in Settings, then try again.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        devices.clear();
        if (!addPairedDevices()) {
            setFindButtonState("Find paired earbuds", true);
            return;
        }

        // GM2 Pro is normally a classic Bluetooth audio endpoint. Check Android's
        // paired-device list before attempting BLE discovery.
        boolean hasLenovoPair = false;
        for (BluetoothDevice device : devices.values()) {
            String name = safeDeviceName(device).toUpperCase(Locale.ROOT);
            if (name.contains("GM2") || name.contains("GM 2")
                    || name.contains("THINKPLUS") || name.contains("LENOVO")
                    || name.contains("LE258")) {
                hasLenovoPair = true;
                break;
            }
        }
        if (hasLenovoPair || !devices.isEmpty()) {
            showDevicePicker("Paired Bluetooth devices are listed first. Choose your thinkplus GM2 Pro.");
            return;
        }

        if (deviceStatus != null) deviceStatus.setText("No paired devices found. Checking for BLE advertisements…");
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            showDevicePicker("No paired device was found and BLE scanning is unavailable. Pair your earbuds in Android Bluetooth settings first.");
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
                runOnUiThread(() -> {
                    setFindButtonState("Find paired earbuds", true);
                    showDevicePicker("BLE scan failed (" + errorCode + "). Pair the earbuds in Android Bluetooth settings first.");
                });
            }
        };
        try {
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            bleScanner.startScan(null, settings, scanCallback);
            scanning = true;
            deviceStatus.setText("Scanning for BLE devices…");
            setFindButtonState("Scanning…", false);
            handler.postDelayed(() -> {
                stopScan();
                setFindButtonState("Find paired earbuds", true);
                showDevicePicker("No paired Lenovo device was found. The GM2 Pro may not advertise as a BLE device.");
            }, 8000L);
        } catch (SecurityException error) {
            if (deviceStatus != null) deviceStatus.setText("Android denied Bluetooth scanning permission.");
            setFindButtonState("Find paired earbuds", true);
            Toast.makeText(this, "Bluetooth permission was not granted.", Toast.LENGTH_LONG).show();
        } catch (Throwable error) {
            setFindButtonState("Find paired earbuds", true);
            showDevicePicker("Could not start BLE scan. Pair the earbuds in Android Bluetooth settings first.");
        }
    }

    private boolean addPairedDevices() {
        try {
            if (bluetoothAdapter != null) {
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                    devices.put(device.getAddress(), device);
                }
            }
            return true;
        } catch (SecurityException error) {
            deviceStatus.setText("Android blocked access to paired devices. Check Nearby devices permission.");
            Toast.makeText(this, "LenovoBuds needs Nearby devices permission to list paired earbuds.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void stopScan() {
        if (scanning && bleScanner != null && scanCallback != null) {
            try { bleScanner.stopScan(scanCallback); } catch (Throwable ignored) {}
        }
        scanning = false;
    }

    private void showDevicePicker(String message) {
        setFindButtonState("Find paired earbuds", true);
        if (!addPairedDevices()) return;
        if (devices.isEmpty()) {
            if (deviceStatus != null) deviceStatus.setText("No paired devices found");
            new AlertDialog.Builder(this).setTitle("No paired earbuds found")
                    .setMessage(message + "\n\nYour phone already needs to pair the earbuds in Android Bluetooth settings before LenovoBuds can identify them.")
                    .setPositiveButton("Open Bluetooth settings", (d, w) -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                    .setNegativeButton("Close", null).show();
            return;
        }
        List<BluetoothDevice> list = new ArrayList<>(devices.values());
        list.sort((left, right) -> {
            boolean l = isLenovoName(safeDeviceName(left));
            boolean r = isLenovoName(safeDeviceName(right));
            return Boolean.compare(r, l);
        });
        String[] labels = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            BluetoothDevice d = list.get(i);
            labels[i] = "PAIRED · " + safeDeviceName(d) + "\n" + d.getAddress();
        }
        new AlertDialog.Builder(this).setTitle("Select your earbuds")
                .setMessage(message)
                .setItems(labels, (dialog, which) -> connectGatt(list.get(which))).show();
    }

    private boolean isLenovoName(String name) {
        String normalized = name == null ? "" : name.toUpperCase(Locale.ROOT);
        return normalized.contains("GM2") || normalized.contains("GM 2")
                || normalized.contains("THINKPLUS") || normalized.contains("LENOVO")
                || normalized.contains("LE258");
    }

    private String safeDeviceName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null || name.trim().isEmpty() ? "Unnamed Bluetooth device" : name;
        } catch (SecurityException ignored) { return "Bluetooth device"; }
    }

    private void connectGatt(BluetoothDevice device) {
        if (!hasBluetoothPermissions()) { requestBluetoothPermissions(); return; }
        String name = safeDeviceName(device);
        if (isLenovoName(name)) {
            // The GM2 Pro is currently treated as a classic Bluetooth audio
            // device. A2DP connection is managed by Android; a BLE GATT
            // connection is not the same thing as the audio connection.
            if (gatt != null) {
                try { gatt.disconnect(); gatt.close(); } catch (Throwable ignored) {}
                gatt = null;
            }
            if (deviceStatus != null) deviceStatus.setText("Paired · Android manages the Bluetooth audio connection");
            if (modelStatus != null) modelStatus.setText(detectModel(name) + " · device identified; control protocol not verified");
            if (batteryStatus != null) batteryStatus.setText("L  —%          R  —%");
            if (ancStatus != null) ancStatus.setText("ANC / Ambient controls remain locked until a GM2 Pro control protocol is verified.");
            Toast.makeText(this, "GM2 Pro detected. Android manages its audio connection; earbud controls are not verified yet.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            if (gatt != null) { gatt.disconnect(); gatt.close(); gatt = null; }
            if (deviceStatus != null) deviceStatus.setText("Opening BLE control link to " + name + "…");
            if (modelStatus != null) modelStatus.setText(detectModel(name) + " · checking BLE services");
            gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } catch (Throwable error) {
            if (deviceStatus != null) deviceStatus.setText("Could not open a BLE control link");
            Toast.makeText(this, "This device may use classic Bluetooth only", Toast.LENGTH_LONG).show();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int status, int newState) {
            runOnUiThread(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (deviceStatus != null) deviceStatus.setText("BLE connected · discovering services…");
                    if (modelStatus != null) modelStatus.setText(detectModel(safeDeviceName(bluetoothGatt.getDevice())) + " · checking services");
                    try { bluetoothGatt.discoverServices(); } catch (Throwable ignored) {}
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (deviceStatus != null) deviceStatus.setText("Bluetooth audio may still be connected; BLE link disconnected");
                    if (gatt == bluetoothGatt) gatt = null;
                    try { bluetoothGatt.close(); } catch (Throwable ignored) {}
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    if (deviceStatus != null) deviceStatus.setText("BLE connection error · " + status);
                }
            });
        }

        @Override public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            BluetoothGattService battery = bluetoothGatt.getService(BATTERY_SERVICE);
            BluetoothGattCharacteristic level = battery == null ? null : battery.getCharacteristic(BATTERY_LEVEL);
            runOnUiThread(() -> {
                if (deviceStatus != null) deviceStatus.setText("BLE link connected");
                if (level == null && batteryStatus != null) batteryStatus.setText("L  —%          R  —%");
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
            runOnUiThread(() -> { if (batteryStatus != null) batteryStatus.setText("BATTERY " + percent + "% · BLE service"); });
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
            else {
                if (deviceStatus != null) deviceStatus.setText("Bluetooth permission denied. Allow Nearby devices in App info.");
                setFindButtonState("Find paired earbuds", true);
                Toast.makeText(this, "Allow Nearby devices permission for LenovoBuds.", Toast.LENGTH_LONG).show();
            }
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

    private static class EarbudsIllustrationView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF shape = new RectF();

        private boolean highlightLeft = true;

        EarbudsIllustrationView(Activity activity) {
            super(activity);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setSelectedSide(boolean left) {
            highlightLeft = left;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.37f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(229, 240, 244));
            canvas.drawCircle(cx, cy, radius, paint);
            drawEarbud(canvas, cx - radius * 0.43f, cy, true, radius);
            drawEarbud(canvas, cx + radius * 0.43f, cy, false, radius);
        }

        private void drawEarbud(Canvas canvas, float cx, float cy, boolean left, float radius) {
            canvas.save();
            canvas.rotate(left ? -28f : 28f, cx, cy);
            float u = radius / 65f;
            if (left == highlightLeft) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(48, 43, 177, 204));
                canvas.drawCircle(cx + 1f * u, cy + 11f * u, 22f * u, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(7f * u);
            paint.setColor(Color.rgb(34, 42, 47));
            shape.set(cx - 25f*u, cy - 45f*u, cx + 37f*u, cy + 49f*u);
            canvas.drawArc(shape, left ? 205f : -25f, 225f, false, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            shape.set(cx - 25f*u, cy - 7f*u, cx + 27f*u, cy + 29f*u);
            canvas.drawRoundRect(shape, 16f*u, 16f*u, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.4f*u);
            paint.setColor(Color.rgb(195, 205, 209));
            canvas.drawRoundRect(shape, 16f*u, 16f*u, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(52, 64, 69));
            shape.set(cx - 9f*u, cy + 2f*u, cx + 7f*u, cy + 9f*u);
            canvas.drawRoundRect(shape, 4f*u, 4f*u, paint);
            paint.setColor(Color.rgb(43, 177, 204));
            canvas.drawCircle(cx + 17f*u, cy + 11f*u, 2.1f*u, paint);
            canvas.restore();
        }
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
        String explanation;
        switch (eyebrow) {
            case "YOUR DEVICE": explanation = "See what Android can identify and what the earbuds expose."; break;
            case "LISTENING MODE": explanation = "Choose a phone-side sound profile for music or games."; break;
            case "NOISE CONTROL": explanation = "Hardware noise controls stay disabled until verified for your exact model."; break;
            case "EQUALIZER": explanation = "Pick a preset or adjust five frequency bands to your preference."; break;
            case "GAME AUDIO": explanation = "Optionally match installed games with saved EQ profiles."; break;
            default: explanation = "";
        }
        if (!explanation.isEmpty()) {
            TextView description = text(explanation, 11, MUTED, false);
            description.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams descriptionLp = params(-1, -2);
            descriptionLp.topMargin = dp(5);
            card.addView(description, descriptionLp);
        }
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
        button.setBackground(new RippleDrawable(
                ColorStateList.valueOf(0x55FFFFFF),
                bg,
                pill(Color.WHITE, dp(14), Color.TRANSPARENT)));
        button.setStateListAnimator(null);
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
