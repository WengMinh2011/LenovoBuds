# GM2 Pro Game/Music mode protocol investigation

This document records a controlled way to collect evidence before implementing any app-side Game/Music command.

## Current evidence

Available GM2 Pro manual copies describe switching Game/Music mode by holding the right earbud touch area for about two seconds. They do not document a phone-app command, BLE GATT service, UUID, characteristic, or vendor command. Manual copies differ on some other touch gestures, so the exact unit and hardware revision matter.

**Working hypothesis:** the right-earbud gesture may change an internal earbud DSP/latency mode without sending a command from the phone. This is not confirmed. Do not send guessed bytes to random GATT characteristics.

## Capture method

Use a short Android Bluetooth HCI snoop capture while the GM2 Pro is paired and connected to the phone.

1. On the phone, pair/connect the GM2 Pro normally. Do not start music playback, calls, or screen recording during this capture.
2. Enable Developer options if needed. On Xiaomi/POCO HyperOS, Xiaomi's support route is Settings → About phone → Detailed info and specs → tap OS/MIUI version several times. Then open Settings → Additional settings → Developer options. Some builds show OS version directly on the About phone page.
3. In Developer options, enable **Bluetooth HCI snoop log** (use Settings search if the list is long).
4. Toggle Bluetooth off and back on so logging takes effect, then reconnect the GM2 Pro. Android's documentation says HCI snoop captures Bluetooth HCI packets; the file may be available as `/sdcard/btsnoop_hci.log`, or in the Bluetooth logs contained in a bug report.
5. Keep other Bluetooth accessories disconnected for this short test if practical.
6. Wait about 10 seconds with the earbuds connected.
7. Hold the **right** earbud touch area for approximately two seconds once. Wait five seconds.
8. Hold the right earbud touch area for approximately two seconds a second time. Wait five seconds.
9. Stop the capture by turning Bluetooth off, then disable HCI snoop logging.
10. Generate an Android bug report using the Developer options entry if the direct log file is not accessible. Extract only the Bluetooth snoop file from the report (often under `FS/data/misc/bluetooth/logs/` or a similar Bluetooth-log folder). If the file is not present, keep the bug report private and tell us the phone's HyperOS version; the capture location can vary by build.

Do not share the whole bug report. It can contain unrelated phone diagnostics and personal information. Share only the extracted `btsnoop_hci.log` (or corresponding `.cfa`/snoop file). Bluetooth captures can still reveal device names, addresses, and connection metadata; inspect before sharing and redact unrelated identifiers where possible.

## Comparison and interpretation

The trace should be examined around the two marked right-earbud gestures, compared with the idle connection interval, and checked for:

- Any phone-to-earbud HCI ACL data around the gesture.
- Any BLE connection or GATT activity (if the device exposes BLE).
- Any disconnect/reconnect, profile change, or AVRCP/A2DP signaling.
- Whether the only packets are ordinary background keepalives/transport traffic.

A short trace with no new phone-to-earbud traffic around the gesture would support (but not prove) the hypothesis that mode switching is handled internally by the earbuds. HCI snoop observes the phone's Bluetooth controller traffic; it cannot see internal earbud firmware events that never cross the radio link.

## Implementation gate

Do not add a Game/Music hardware command until a repeatable packet sequence is identified, associated with a known action, and validated on the same earbud revision. Any eventual command should be sent only to the identified service/characteristic or transport and should have a read-back or observable state change where available.

## Sources

- Xiaomi Support: enable Developer options on Xiaomi phones/tablets — https://www.mi.com/global/support/faq/details/KA-168765/
- Android Developers: on-device Developer options and HCI snoop log location — https://developer.android.com/studio/debug/dev-options
- Android Open Source Project: Bluetooth packet capture and bug-report extraction — https://source.android.com/docs/core/connect/bluetooth/verifying_debugging
- GM2 Pro manual copy describing right-earbud long-press mode switching — https://manuals.plus/m/b2e1fe37e445096387b88eb09954118ef7323cd18ac87f185a6bd5abfe69a606
