package com.lenovobuds.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BluetoothDiagnosticsActivity extends Activity {
    private static final int BG = Color.rgb(244, 248, 250);
    private static final int TEXT = Color.rgb(31, 38, 43);
    private static final int MUTED = Color.rgb(112, 125, 132);
    private LinearLayout log;
    private TextView status;
    private BluetoothAdapter adapter;
    private BluetoothA2dp a2dp;
    private boolean receiverRegistered;

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = (BluetoothA2dp) proxy;
                refreshConnection();
                addLog("Android A2DP profile proxy connected");
            }
        }
        @Override public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = null;
                status.setText("A2DP profile unavailable");
                addLog("Android A2DP profile proxy disconnected");
            }
        }
    };

    private final BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) return;
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
            BluetoothDevice device = getDeviceExtra(intent);
            String name = safeName(device);
            addLog(name + " · " + stateLabel(state));
            refreshConnection();
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
        if (!hasConnectPermission()) {
            status.setText("Nearby devices permission is required");
            addLog("Grant Nearby devices permission in Android App info, then reopen diagnostics");
            return;
        }
        if (adapter == null || !adapter.isEnabled()) {
            status.setText("Bluetooth is turned off");
            addLog("Bluetooth adapter is unavailable or disabled");
            return;
        }
        registerConnectionReceiver();
        boolean requested = adapter.getProfileProxy(this, profileListener, BluetoothProfile.A2DP);
        addLog(requested ? "Requested Android A2DP profile" : "Could not request Android A2DP profile");
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(page);
        setContentView(scroll);

        Button back = new Button(this);
        back.setText("‹  Back");
        back.setAllCaps(false);
        back.setTextColor(TEXT);
        back.setOnClickListener(v -> finish());
        page.addView(back, new LinearLayout.LayoutParams(-2, dp(44)));

        TextView title = text("Bluetooth diagnostics", 23, TEXT, true);
        LinearLayout.LayoutParams titleLp = params(-1, -2);
        titleLp.topMargin = dp(12);
        page.addView(title, titleLp);
        page.addView(text("GM2 Pro · Android audio connection", 12, MUTED, false), params(-1, -2));

        status = text("Checking Android Bluetooth…", 15, TEXT, true);
        status.setPadding(dp(15), dp(17), dp(15), dp(17));
        status.setBackground(round(Color.WHITE));
        LinearLayout.LayoutParams statusLp = params(-1, -2);
        statusLp.topMargin = dp(18);
        page.addView(status, statusLp);

        Button refresh = new Button(this);
        refresh.setText("Refresh connection");
        refresh.setAllCaps(false);
        refresh.setOnClickListener(v -> refreshConnection());
        LinearLayout.LayoutParams refreshLp = params(-1, dp(48));
        refreshLp.topMargin = dp(12);
        page.addView(refresh, refreshLp);

        TextView explanation = text(
                "This checks the standard Bluetooth audio (A2DP) connection managed by Android. It cannot read the earbuds’ private Game/Music mode, ANC state, or proprietary commands. Use the earbud touch control to change Game/Music mode.",
                12, MUTED, false);
        explanation.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams expLp = params(-1, -2);
        expLp.topMargin = dp(17);
        page.addView(explanation, expLp);

        TextView logTitle = text("CONNECTION EVENT LOG", 10, MUTED, true);
        logTitle.setLetterSpacing(.12f);
        LinearLayout.LayoutParams logTitleLp = params(-1, -2);
        logTitleLp.topMargin = dp(24);
        page.addView(logTitle, logTitleLp);

        log = new LinearLayout(this);
        log.setOrientation(LinearLayout.VERTICAL);
        log.setPadding(dp(14), dp(10), dp(14), dp(10));
        log.setBackground(round(Color.WHITE));
        LinearLayout.LayoutParams logLp = params(-1, -2);
        logLp.topMargin = dp(9);
        page.addView(log, logLp);
    }

    private void refreshConnection() {
        if (status == null) return;
        if (!hasConnectPermission()) {
            status.setText("Nearby devices permission is required");
            return;
        }
        if (a2dp == null) {
            status.setText("Waiting for Android A2DP profile…");
            return;
        }
        try {
            List<BluetoothDevice> connected = a2dp.getConnectedDevices();
            List<String> names = new ArrayList<>();
            for (BluetoothDevice device : connected) names.add(safeName(device));
            if (names.isEmpty()) {
                status.setText("No Bluetooth audio device connected");
            } else {
                StringBuilder value = new StringBuilder("A2DP audio connected");
                for (String name : names) value.append("\n• ").append(name);
                status.setText(value.toString());
            }
        } catch (SecurityException error) {
            status.setText("Bluetooth permission is missing");
            addLog("Android denied access to connected-device information");
        }
    }

    private void registerConnectionReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(connectionReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(connectionReceiver, filter);
        }
        receiverRegistered = true;
    }

    private BluetoothDevice getDeviceExtra(Intent intent) {
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        }
        return intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    }

    private String safeName(BluetoothDevice device) {
        if (device == null) return "Unknown Bluetooth device";
        try {
            String name = device.getName();
            return name == null || name.trim().isEmpty() ? "Unnamed Bluetooth device" : name;
        } catch (SecurityException ignored) {
            return "Bluetooth device";
        }
    }

    private String stateLabel(int state) {
        if (state == BluetoothProfile.STATE_CONNECTED) return "audio connected";
        if (state == BluetoothProfile.STATE_CONNECTING) return "connecting";
        if (state == BluetoothProfile.STATE_DISCONNECTING) return "disconnecting";
        if (state == BluetoothProfile.STATE_DISCONNECTED) return "audio disconnected";
        return "state changed";
    }

    private void addLog(String message) {
        if (log == null) return;
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        TextView item = text(time + "  " + message, 11, MUTED, false);
        item.setPadding(0, dp(6), 0, dp(6));
        log.addView(item, 0);
        while (log.getChildCount() > 30) log.removeViewAt(log.getChildCount() - 1);
    }

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < 31
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    @Override protected void onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(connectionReceiver); } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
        if (adapter != null && a2dp != null) {
            try { adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dp); } catch (Throwable ignored) {}
            a2dp = null;
        }
        super.onDestroy();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return view;
    }

    private android.graphics.drawable.GradientDrawable round(int color) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(18));
        return bg;
    }

    private LinearLayout.LayoutParams params(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
