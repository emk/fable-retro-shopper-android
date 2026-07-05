# Retro Shopper

A small Android shopping-list app loosely inspired by the classic PalmPilot
[HandyShopper](https://palmdb.net/app/handyshopper). One shared item list, with
per-store availability and aisle locations, so the list you see in the store is
sorted in walking order.

| Home: mark what's needed | Shop: aisle order + cart | Item details |
| --- | --- | --- |
| ![Home tab](screenshots/home.png) | ![Shop tab](screenshots/shop.png) | ![Details sheet](screenshots/details.png) |

## How it works

- **Home tab** — every item you ever buy, alphabetical, with a "needed"
  checkbox. The search box filters; typing a new name offers *Add "…"*.
- **Shop tab** — pick a store. Needed items available there appear in aisle
  order; checking one puts it *in the cart* (reversible — no lost items from a
  stray tap). Needed items never recorded at this store sit dimmed under
  *"Not at this store?"* — tap one to record that it's here and which aisle.
- **Check out** — one tap when you're done: everything in the cart stops being
  needed. Nothing else changes.
- **Long-press any item** for details: rename, delete, needed, and per-store
  availability/aisle.

Design history and architecture live in [DESIGN_NOTES.md](DESIGN_NOTES.md) and
[PLAN.md](PLAN.md).

## Building

Requirements: JDK 21 and an Android SDK. The easiest SDK setup is the
[Android CLI](https://developer.android.com/tools/agents): `android sdk
install` puts a lean SDK in `~/Android/Sdk` (or point `local.properties` at an
existing one).

```sh
./gradlew build          # compile + lint + all tests
./gradlew test           # headless test suite only
```

The test suite runs entirely on the JVM — including the Room database (real
SQLite via the bundled driver) and the Compose UI tests (Robolectric). No
emulator needed.

## Installing on your phone (sideloading)

This app is deliberately not on any app store. To put it on a device:

1. Build the debug APK:

   ```sh
   ./gradlew assembleDebug
   # -> app/build/outputs/apk/debug/app-debug.apk
   ```

2. On the phone, enable **Developer options** (tap *Settings → About phone →
   Build number* seven times), then turn on **USB debugging** inside
   *Settings → System → Developer options*.

3. Connect the phone over USB (accept the debugging prompt) and install:

   ```sh
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

   Or, with the Android CLI: `android run --apks=app/build/outputs/apk/debug/app-debug.apk`

No USB cable handy? Copy `app-debug.apk` to the phone (Drive, email,
`adb wifi`, …), open it from the Files app, and allow "install unknown apps"
when prompted. Updates installed over an existing copy keep your data; only
uninstalling removes the database.

## Status

Feature-complete for the original design: needed list, per-store
availability + aisles, cart with checkout, item/store management. Prices,
quantities, and multi-device sync are out of scope for now — though the data
layer is deliberately isolated behind a repository so a Firestore-style
backend could slot in later.
