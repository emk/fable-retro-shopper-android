# CLAUDE.md

Android shopping-list app (HandyShopper-inspired). Read
[DESIGN_NOTES.md](DESIGN_NOTES.md) for goals/scope and
[PLAN.md](PLAN.md) for the settled design decisions (check-off/cart
semantics, two-state availability, data model) before changing behavior.

## Toolchain

- **Gradle needs JDK 21+** (Robolectric's SDK 36 sandbox requires a Java 21
  runtime, and the module's toolchain is set to 21). If the system `java` is
  older, pin per-machine via `org.gradle.java.home` in the *user-level*
  `~/.gradle/gradle.properties` — never commit a machine path into the repo.
- The project assumes the **Android CLI** (`android`,
  https://developer.android.com/tools/agents) for SDK setup and device work:
  `android sdk install`, `android emulator create/start`,
  `android run --apks=...`, `android screen capture --output=x.png`.
  `local.properties` (untracked) points Gradle at the SDK.
- `adb` lives in `<sdk>/platform-tools/`, typically not on PATH.
- `android studio render-compose-preview` requires a running Android Studio —
  it is not headless. Some `android` help strings are wrong (e.g. the skills
  subcommand is `skills add`, though its usage line says `install`).

## Build & test

```sh
./gradlew build                  # compile + lint + all tests
./gradlew :app:testDebugUnitTest # fast headless suite (~10s warm)
```

Everything runs on the JVM — no emulator needed for tests.

## Architecture (single module)

- `data/` — Room entities (`Item`, `Store`, `StoreItem`), one `ShoppingDao`,
  and `ShoppingRepository`. The repository is the app-level API: nothing
  outside `data` touches a DAO (this is the future-Firestore seam). A
  `StoreItem` row *existing* means "available at this store"; deleting it
  forgets the aisle on purpose.
- `ui/` — `HomeViewModel`/`ShopViewModel` expose `StateFlow<UiState>`;
  `ItemDetailsSheet` is the one place for rename/delete/availability/aisle.
  Navigation 3 with two bottom-tab keys in `Navigation.kt`.
- DI is a hand-wired `AppContainer` on the `Application` class plus
  `AppViewModelProvider`. No Hilt — keep it that way (see the mocking/DI
  guidance in DESIGN_NOTES.md).

## Testing conventions

- **Mock-free by design**: tests build a real in-memory Room database
  (`BundledSQLiteDriver`) and drive the real repository/ViewModels. Don't
  introduce fakes or mocks without a strong reason.
- Robolectric supplies the Android `Context` (Room's context-free builder is
  KMP-target-only) and runs the Compose UI tests. Its SDK 36 sandbox is why
  the toolchain is Java 21. Local tests need the `sqlite-bundled-jvm`
  artifact (the Android one lacks host natives).
- Compose tests: async DB round-trips mean you *wait* for UI state
  (`waitUntil` helpers in the tests), never assert immediately after a click.
- Room schema JSON is exported to `app/schemas/` (committed); bump the DB
  version + add a migration if entities change.

## Workflow

- Commit after each coherent green step (build + tests) — the history so far
  is one commit per milestone.
- Item/store names are unique case-insensitively (NOCASE collation);
  duplicate-name validation happens live in the UI, so repository renames
  just no-op on conflict.
