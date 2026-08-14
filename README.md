# AeroPad

Turn your Android phone into a **Bluetooth keyboard, mouse, media remote, presenter and gamepad** for any PC, laptop, TV or device that accepts Bluetooth input.

Built 100% in Kotlin + Jetpack Compose, MVVM + Clean Architecture, Hilt, DataStore. No account, no cloud, no ads — input goes straight over Android's Bluetooth HID Device API. Nothing to install on the PC.

## Features

- **Mouse** — trackpad with tap-click, double-tap, long-press right-click, scroll strip, L/M/R buttons; sensitivity, smoothing, pen mode, tap-to-click, invert scroll all tunable.
- **Keyboard** — text-send bar with history, shortcuts (copy/paste/cut/select-all/save/undo/redo), F1–F12, arrows + navigation cluster, combos (Alt+Tab, Alt+F4, Win+D, Win+L, Ctrl+Shift+Esc), plus a full on-screen keyboard.
- **Multimedia** — play/pause, stop, tracks, volume, mute, brightness.
- **Presenter** — big prev/next, start/from-here, black/white screen, end.
- **Gamepad** — virtual stick + D-pad + ABXY + shoulders; three modes: real HID gamepad, keyboard fallback, mouse+keyboard hybrid; sensitivity + dead-zone tuning.

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
