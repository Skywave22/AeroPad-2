# AeroPad

Turn your Android phone into a **Bluetooth keyboard, mouse, media remote, presenter and gamepad** for any PC, laptop, TV or device that accepts Bluetooth input.

Built 100% in Kotlin + Jetpack Compose, MVVM + Clean Architecture, Hilt, DataStore. No account, no cloud, no ads — input goes straight over Android's Bluetooth HID Device API. Nothing to install on the PC.

## Features

- **Mouse** — trackpad with tap-click, double-tap, long-press right-click, **real two-finger scroll**, scroll strip, L/M/R buttons, precision mode, drag lock; sensitivity, smoothing, pen mode, tap-to-click, invert scroll all tunable.
- **Keyboard** — text-send bar with history, **quick snippets** (long-press send to save a phrase, one tap to type it), shortcuts (copy/paste/cut/select-all/save/undo/redo), F1–F12, arrows **with hold-to-repeat**, navigation cluster, extended combo pack (Alt+Tab, Win+Shift+S screenshot, browser tabs, task view…), plus a full on-screen keyboard.
- **Multimedia** — play/pause, stop, tracks, **hold-to-repeat volume ramp**, mute, brightness, and **PC power controls** (sleep/wake).
- **Presenter** — big prev/next, start/from-here, black/white screen, end, and a **built-in presentation timer**.
- **Gamepad** — virtual stick + D-pad + ABXY + shoulders; three modes: real HID gamepad, keyboard fallback, mouse+keyboard hybrid; sensitivity + dead-zone tuning.
- **Home quick-connect** — saved PCs appear as one-tap connect chips the moment you open the app.
- **Quick actions** — Lock PC, Show desktop, Play/Pause and Mute chips on Home while connected.
- **Pinch-to-zoom** — pinch the trackpad to zoom on the PC (Ctrl+scroll).
- **Modifier lock** — arm Ctrl/Shift/Alt/Win as sticky toggles, tap any key to fire the combo.
- **Phone volume buttons control the PC** while connected (toggleable).
- **Notification media controls** — prev/play/next buttons for the PC right in the notification shade.
- **Air Mouse** — wave the phone like a TV magic remote to steer the PC pointer (gyroscope, hold-to-move clutch, sensitivity slider).
- **Scanner** — scan QR codes & barcodes with the camera and type them straight on the PC (auto-send mode for inventory work).
- **Voice input** — dictate with the system speech recognizer, review, then send to the PC.
- **Clipboard sync** — one tap types the phone's clipboard on the PC.
- **Keep PC awake (jiggler)** — invisible 1-px pointer nudge every 50 s so the PC never sleeps, locks, or marks you away.

### Connection
- Real Bluetooth HID Device — works with Windows, macOS, Linux, Android/Google TV, most smart TVs.
- Multi-host profiles: save every machine you use, one-tap switch.
- Auto-reconnect with backoff after unexpected drops; optional reconnect on launch.
- Connect timeout with clear error messages (no endless "Connecting…").
- Stuck-key protection: all inputs are released automatically on reconnect.

### Settings
- Theme (Light/Dark/System), fullscreen, keep screen on, touch vibrations, secure screen (blocks screenshots).
- Mouse, keyboard and gamepad tuning applied live everywhere.
- Accessibility: spoken connection alerts, reduce motion.

## Requirements
- **Android 10 (API 29) or newer.**
- A phone/ROM that supports **Bluetooth HID Device mode** (most do; some ROMs block it — the app detects and reports this).
- A host that accepts Bluetooth keyboards/mice: Windows, macOS, Linux, Android/Google TV, most smart TVs. iOS/iPadOS: keyboard yes; mouse depends on OS version + AssistiveTouch settings.

## Pairing with a PC (Windows)
1. Open the app → **Connect** → grant the Bluetooth permissions.
2. On Windows: Settings → Bluetooth & devices → **Add device** → Bluetooth.
3. Pick your phone from the list and confirm the pairing code on both sides.
4. Back in the app, tap your PC in the device list — done. The phone now acts as a keyboard/mouse/gamepad.

## Build
```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # unsigned release APK
```

CI builds run on every push (see `.github/workflows/main.yml`).

## License
See [LICENSE](LICENSE).
