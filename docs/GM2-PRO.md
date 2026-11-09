# GM2 Pro device notes

## Identification

Some manual listings use the Bluetooth display name `thinkplus GM2 pro`. LenovoBuds uses a name match as a hint only; product names and hardware revisions can be reused, so the match does not authenticate a physical device.

## Mode control

Manual listings commonly describe a built-in Music/Game toggle using the right earbud touch area, held for approximately two seconds. At least one manual listing describes the gesture differently, so check the leaflet shipped with the exact unit. LenovoBuds does not send a proprietary packet for this control and cannot query the current hardware mode.

References:

- https://manuals.plus/ae/1005007424603622
- https://manuals.plus/ae/1005007495084810
- https://manuals.plus/ae/1005006195112746

## What the app can do today

- Scan for BLE advertisers and inspect visible GATT services.
- Read battery level only if the standard Battery Service is advertised and readable.
- Save app-side Music/Gaming EQ presets and game assignments.
- Attempt an Android output-mix equalizer, subject to phone firmware/audio policy.

## What is not implemented

- GM2 Pro hardware Game/Music mode control from the phone.
- Earbud touch remapping, ANC changes, onboard EQ, or charging-case battery.
- A verified custom BLE GATT service or vendor command characteristic.

## Protocol investigation plan

1. Pair the GM2 Pro in Android Bluetooth Settings.
2. Record its exact displayed name and firmware information if the phone exposes any.
3. Note the supplied unit's mode-switch gesture and confirmation sound.
4. If collecting an HCI snoop capture, capture only the short interval around the physical gesture and inspect it before sharing.
5. Compare whether the phone sends any packets when the gesture is performed. If the mode change is entirely internal to the earbuds, phone-side control may not be possible.

Do not write guessed data to arbitrary GATT characteristics.
