# LenovoBuds

An unofficial Android companion-app research project for Lenovo/thinkplus earbuds, starting with the **GM2 Pro** and **LE258**. This is a community prototype, not an official Lenovo app.

> **Core goal:** control the earbuds' built-in Game/Music mode from the phone app. That hardware command is **not implemented yet** for either model. The current Gaming/Music selector changes only the phone-side audio profile. No undocumented vendor commands are sent.

## Current preview (v0.1.0)

- Native Android app written in Java, with no third-party runtime libraries.
- Minimum Android 8.0 (API 26); compile/target SDK 35.
- BLE discovery and GATT inspection. Either model may use classic Bluetooth for audio and may not appear in a BLE scan.
- Reads the standard Bluetooth Battery Service (`180F` / `2A19`) only if the earbuds expose it. Battery reporting is not guaranteed.
- Experimental name hints for `thinkplus GM2 pro` and `LE258`; a name match does not authenticate the hardware.
- Local Music / Gaming EQ profiles: Music, Balanced, FPS, MOBA, Racing and RPG.
- Per-game profile assignments and optional automatic switching using Android Usage Access.
- Best-effort Android output-mix EQ. Phone firmware may reject it or may not route it to Bluetooth audio.
- GitHub Actions workflow to build a debug APK and upload it as a workflow artifact.

## Device compatibility status

| Capability | GM2 Pro | LE258 |
|---|---|---|
| Bluetooth audio pairing | Android Bluetooth Settings | Android Bluetooth Settings |
| BLE discovery / GATT inspection | Implemented; may not advertise over BLE | Implemented; may not advertise over BLE |
| Standard BLE battery reading | Only if service is exposed | Only if service is exposed |
| Local app EQ / game profiles | Available in preview | Available in preview |
| Built-in low-latency / Game mode | Hardware mode documented; app command unknown | Gaming mode advertised; app command unknown |
| Remote Game/Music switch from this app | Not implemented | Not implemented |
| Case battery / touch remapping / onboard EQ | Not implemented | Not implemented |

### GM2 Pro notes

Several manual listings describe a built-in Music/Game toggle using the right earbud touch area held for about two seconds. Manual variants are inconsistent, so check the leaflet supplied with the exact unit. This project has no verified phone-to-earbud command for that toggle and cannot query the current internal mode. References: [manual A](https://manuals.plus/ae/1005007424603622), [manual B](https://manuals.plus/ae/1005007495084810), [manual C](https://manuals.plus/ae/1005006195112746).

### LE258 notes

The available LE258 manual describes a low-latency gaming feature but does not document a companion-app workflow or a phone command for switching it. Treat app compatibility as unconfirmed until the exact hardware revision is tested. See [LE258 manual](https://manuals.plus/ae/3256811711037133).

Lenovo's Smart Wearable support instructions found during research refer to a different Lenovo Smart Wireless Earbuds product family (PS-1551B), so they are not evidence that the GM2 Pro or LE258 works with that app: [Lenovo support FAQ](https://support.lenovo.com/in/en/solutions/ht512473).

## What the app's Gaming/Music selector does today

It selects a local phone-side audio profile and can trigger game-specific preset selection. It does **not** change the earbuds' internal low-latency DSP mode. The app applies a best-effort Android output-mix `Equalizer` effect (audio session 0); Android manufacturers may disable it or prevent it from affecting Bluetooth output. If that happens, profile assignment and game detection still work, but the EQ may not be audible.

## Requirements for real app-to-earbud control

Before implementing a hardware mode switch, we need to identify a real, reproducible command for each model/revision. Evidence could come from a Lenovo-provided companion app that explicitly supports the exact model, or a verified Bluetooth capture showing the phone sending a command while the mode changes. A physical touch gesture may be handled entirely inside the earbuds and produce no phone-side packet.

The project will not send guessed data to arbitrary GATT characteristics. Until a command has been captured and tested, the UI must label the hardware control as unavailable rather than imply that the earbud changed modes.

## Build an APK with GitHub Actions

1. Open the repository's **Actions** tab.
2. Select **Build LenovoBuds APK** (or push a commit to trigger it).
3. Open the completed run and download the `LenovoBuds-debug-apk` artifact.
4. Extract the artifact ZIP and install `app-debug.apk` on the phone.

## Build locally

Open this folder in Android Studio, install Android SDK Platform 35, use JDK 17 and Gradle 8.9, then run:

```bash
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## First use

1. Pair the GM2 Pro or LE258 from Android **Settings → Bluetooth** first.
2. Open LenovoBuds and grant the requested Bluetooth permissions.
3. Tap **Find / connect earbuds**. The app scans for BLE devices; classic-only earbuds may not appear.
4. If the standard BLE Battery Service exists, the app may show a percentage.
5. Choose Music or Gaming to change the **phone-side EQ profile**. This preview does not remotely change the earbuds' built-in mode.
6. For automatic game profiles, enable **Auto-switch when a game opens** and grant Usage Access.

## Project layout

- `app/src/main/java/com/lenovobuds/app/MainActivity.java` — UI, BLE scanning, GATT connection, battery reading and model-name hints.
- `AudioProfileEngine.java` — best-effort Android global EQ.
- `GameDetectionService.java` / `GameUtils.java` — active-game detection and profile switching.
- `ProfileCatalog.java` — local EQ curves.
- `docs/GM2-PRO.md` and `docs/LE258.md` — device notes and protocol status.
- `.github/workflows/build-apk.yml` — cloud APK build.

## Disclaimer

LenovoBuds is an unofficial community project and is not affiliated with Lenovo. Product names are used only to identify intended test devices.
