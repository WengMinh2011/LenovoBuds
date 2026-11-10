# LenovoBuds

**LenovoBuds ALPHA 0.1.2** is an unofficial Android companion-app research project for Lenovo/thinkplus earbuds, beginning with the GM2 Pro. The LE258 is a secondary research target. It is not an official Lenovo app.

![LenovoBuds logo](app/src/main/res/drawable-nodpi/lenovobuds_logo.png)

## Alpha UI and features

- Redesigned device dashboard, dedicated left/right Controls screen, and bottom Settings sheet inspired by the supplied layout reference. Touch remapping and Control Lock remain preview-only until GM2 Pro commands are verified.

- Light, card-based dashboard inspired by modern headphone companion apps, with original LenovoBuds branding and an ALPHA status label.
- Bluetooth LE discovery, GATT service inspection, and best-effort standard battery-service reading (only where exposed).
- Music / Gaming app-side mode selector.
- Editable five-point EQ graph: drag the points to change gain at 60 Hz, 230 Hz, 910 Hz, 3.6 kHz and 14 kHz.
- Presets: Music, Balanced, Bass, Vocal, FPS, MOBA, Racing, RPG and Custom.
- Game-to-profile assignment and optional automatic game detection using Android Usage Access.
- ANC / Ambient control panel in a locked, unverified state for GM2 Pro. The visible controls are not hardware commands yet.
- GitHub Actions workflow to build a debug APK.

## Important device-control truth

The app currently changes **phone-side EQ**, not EQ stored in the earbuds. Android's global Equalizer effect is best-effort: OEM audio policy may reject it or not route it to Bluetooth output.

### Game / Music mode

Several GM2 Pro manual listings describe changing Game/Music mode by holding the right earbud touch area for around two seconds. Those same manuals do not document a phone-app command, a vendor GATT characteristic, or a companion-app API. The UI's Music/Gaming selector therefore changes the phone EQ/profile only; it does not claim the earbud's internal low-latency mode changed. Manual variants also differ on which touch gesture is used, so check the leaflet supplied with the exact hardware revision.

### ANC / ENC

Do not assume ANC exists just because an online listing uses the term. One GM2 Pro manual specification table claims Active Noise-Cancellation, but its operation text describes **ENC** for call microphone noise reduction; another GM2 Pro listing says ANC is absent. These are not equivalent features. The LE258 manual describes spatial audio, gaming low latency and **ENC Noise Cancelling**, but does not document an ANC listening mode or app control. The ALPHA exposes the future Noise Control panel but keeps ANC/Ambient disabled until the exact unit and a real vendor command are verified.

References:
- [GM2 Pro manual/specification listing](https://manuals.plus/ae/1005006195112746)
- [GM2 Pro manual describing ENC call noise reduction and Game/Music touch mode](https://manuals.plus/ae/1005005665135687)
- [Another GM2 Pro manual listing ANC absent](https://manuals.plus/ae/3256811614376757)
- [LE258 manual: gaming low latency and ENC](https://manuals.plus/ae/3256811711037133)

## Why controls need protocol research

A phone app can control an earbud feature only if the earbuds expose a supported Bluetooth control path (for example, a documented BLE GATT characteristic or a vendor-specific Bluetooth command). A feature that is switched entirely inside the earbuds by a touch gesture may not be remotely controllable from Android.

For real GM2 Pro support, the next step is a short, controlled Bluetooth HCI snoop capture around a verified vendor-app action (if an app exists for this exact unit), then identify and validate the command in both directions. Do not send guessed data to arbitrary GATT characteristics. If no host-side packet is emitted, the earbud's touch gesture may be internal-only.

## Build the ALPHA APK

1. Open the repository's Actions tab.
2. Select the latest successful Build LenovoBuds ALPHA APK workflow run.
3. Download the versioned artifact, for example `LenovoBuds-v0.1.2-alpha-build-123.zip`.
4. Extract and install the APK inside. Its filename includes the app version and build number, not the generic `app-debug.apk` name.

The internal Android Gradle output is still named `app-debug.apk`; the workflow copies it to a versioned filename before uploading it.

A successful push to main also creates a GitHub ALPHA pre-release with the debug APK attached. The APK is an unsigned debug build for testing, not a signed Play Store release. The workflow artifact is also retained for 14 days.

## Build locally

Use JDK 17, Gradle 8.9 and Android SDK Platform 35. Open the project in Android Studio or run: gradle assembleDebug.

The output is app/build/outputs/apk/debug/app-debug.apk.

## Compatibility status

| Capability | GM2 Pro | LE258 |
|---|---|---|
| BLE discovery / GATT inspection | Implemented; may not advertise over BLE | Implemented; may not advertise over BLE |
| Standard BLE battery service | Only if exposed | Only if exposed |
| Phone-side EQ and custom curve | ALPHA UI available | ALPHA UI available |
| Auto game profiles | Phone-side only | Phone-side only |
| Earbud low-latency switch from app | Not implemented; manual gesture documented | Gaming feature documented; host command unknown |
| Earbud ANC / transparency control | Not verified; product sources conflict | Not documented in available manual |
| Case battery / touch remapping / onboard EQ | Not implemented | Not implemented |

## Disclaimer

LenovoBuds is an unofficial community project and is not affiliated with Lenovo. Lenovo and thinkplus names are used only to identify intended test devices.
