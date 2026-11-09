# ALPHA UI and feature notes

## UI

The Alpha dashboard uses the supplied headphone-app screenshot as general layout inspiration: a device/battery summary, an editable equalizer graph, and mode/feature cards. Branding and icon artwork are original LenovoBuds assets.

## Five-band EQ

The custom graph supports five draggable control points at 60 Hz, 230 Hz, 910 Hz, 3.6 kHz and 14 kHz, with a range of -8 to +8 dB. Profiles are phone-side presets. A drag saves a Custom curve locally; Apply EQ attempts the Android output-mix Equalizer effect.

## Noise control

Noise Control is intentionally locked. GM2 Pro sources conflict about ANC: some specification tables say ANC, while manual descriptions talk about ENC call-microphone noise reduction; another GM2 Pro listing states ANC is absent. For LE258, the available manual lists ENC and low-latency gaming, but not ANC listening modes. ENC and ANC are different features. No mode is exposed until hardware support and a corresponding phone command are confirmed.

## Bluetooth control model

Pairing for audio uses Android Bluetooth Settings. This app's BLE scanner discovers advertisers and inspects GATT services. The standard 0x180F / 0x2A19 Battery Service is optional. A BLE scan alone does not prove that an earbud accepts vendor controls: many devices use classic Bluetooth for audio and expose no BLE control service.

A genuine phone-side toggle needs a known remote command. The GM2 Pro manual documents touch operation for Game/Music switching, not an app API. Protocol investigation should capture a short, controlled toggle through any legitimate companion app for the exact hardware revision, if one exists. Compare only the relevant transactions and test both directions before implementing a command. Do not send guessed writes.
