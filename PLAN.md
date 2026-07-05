# Retro Shopper — Implementation Plan

A single-database Android shopping app inspired by HandyShopper. See
[DESIGN_NOTES.md](DESIGN_NOTES.md) for goals and scope. This plan records the
decisions made while refining those notes, plus the concrete build strategy.

## Decisions log

These were discussed and settled before implementation:

- **Check-off semantics**: checking an item while shopping puts it *in the
  cart* (a distinct, visible state). A separate **checkout** action ends the
  trip: every in-cart item has its "needed" flag cleared and leaves the cart.
  Accidental taps are therefore trivially reversible.
- **No prices or quantities** (out of scope, per revised design notes).
- **Aisle** is free text per item per store (`"7"`, `"produce"`, …).
- **Availability is two-state**: a store either has a record for an item
  (available, with optional aisle) or it doesn't. We do not distinguish
  "not available here" from "never recorded". Needed items with no record at
  the current store appear dimmed in a separate section at the bottom of the
  shopping view, where a tap can mark them available.
- **Item renaming and deleting** are in scope (delete cascades to per-store
  records).
- **Modern devices only**: use the template's default `minSdk`.
- **Package / application ID**: `net.randomhacks.retroshopper`.
- **Navigation**: two bottom tabs — **Home** (needed list) and **Shop**
  (per-store view with a store selector at the top).

## Data model

Room database, three tables. Room entities double as domain models — the app
is small enough that a parallel set of "pure" domain classes would be ceremony
without payoff.

```
Item        id, name, needed: Boolean, inCart: Boolean
Store       id, name
StoreItem   itemId + storeId (composite PK, FKs with CASCADE), aisle: String
```

- A `StoreItem` row existing *means* "this item is available at this store".
- `inCart` lives on `Item` (not per-store): you physically shop one store at
  a time. Checkout = one transaction: `needed=false, inCart=false` for all
  in-cart items.
- Item names are unique case-insensitively (enforce via index + repository
  check) so search-and-add can't create near-duplicates.

## Architecture

Single Gradle module. Kotlin, Jetpack Compose, Material 3.

- **`data`** — Room entities, DAOs, and `ShoppingRepository`, the
  application-level API from the design notes. The repository is a plain
  concrete class (no interface yet): it exposes `Flow`s for observation and
  `suspend` functions for mutation, and nothing outside `data` touches a DAO.
  That keeps the future-Firestore seam real; extracting an interface later is
  mechanical. (A real Firestore move also changes sync semantics — the seam
  makes it possible, not free.)
- **`ui`** — one Compose screen per tab plus dialogs; `HomeViewModel` and
  `ShopViewModel` exposing `StateFlow<UiState>`; Navigation 3 for the two
  bottom-tab destinations.
- **DI**: hand-rolled. An `AppContainer` on the `Application` class builds the
  database and repository; ViewModels get the repository via constructor
  (through a ViewModel factory). No Hilt — the object graph is three nodes.

### Screen behavior

**Home tab** — all items, alphabetical. Each row: name + needed indicator;
tap toggles needed. A search field filters the list; when the query matches
nothing exactly, an "Add «query»" row creates the item (marked needed).
Long-press → item details sheet.

**Shop tab** — store selector (dropdown) at top; the dropdown ends with an
"Add store…" entry (name dialog). The tab's overflow menu has
"Rename store…" and "Delete store…" for the *currently selected* store;
delete confirms and warns that this store's aisle/availability records go
with it (items themselves are untouched — the FK cascade). With no stores
yet, the tab shows an empty state with an "Add store" button. No dedicated
store-management screen: at a personal store count, menus suffice.
- *Main section*: needed items available at this store, sorted by aisle then
  name, aisle shown on the row. Tap = into the cart (styled distinctly,
  e.g. strikethrough + moved to an "In cart" group). Tap again = back out.
  Tap on these rows is reserved for the cart toggle — the high-frequency
  shopping gesture never opens UI. Long-press → item details sheet.
- *Dimmed bottom section*: needed items with no record at this store. Tap
  opens the item details sheet (the only reason to touch these rows is to
  mark them available and type an aisle).
- *Checkout* button (with confirmation) finishes the trip as described above.

**Item details sheet** — one Material 3 modal bottom sheet consolidating all
low-frequency item actions; opened by long-press anywhere, or by tapping a
dimmed Shop row.
- Always: editable name (rename), Needed switch, Delete item (with confirm).
- From the Shop tab, additionally a store section: an "Available at «store»"
  switch plus an aisle text field (enabled while available). Switching it off
  *is* "remove from this store" — it deletes the `StoreItem` row, so the item
  drops to the dimmed section if still needed. Consequence: the aisle text
  lives on that row, so un-marking and re-marking forgets the aisle. That's
  fine: removal doesn't mean "out of stock this week", it means "gone from
  this store" — an item coming *back* is a marginal case not worth extra
  state.
- The available switch is never pre-flipped when the sheet opens from a
  dimmed row: dismissing a sheet you opened to peek must not write anything.

## Tooling

- **Android CLI** (installed, v1.0): `android create` for scaffolding
  (`empty-activity` template: Compose + AGP 9), `android sdk install` for a
  lean SDK at `~/Android/Sdk`, `android emulator` / `android run` /
  `android screen capture` for the final stage.
- **Skills**: run `android init`, then `android skills add` for
  `testing-setup` (and `navigation-3` if Navigation 3 setup proves fiddly).
- **JDK**: system default is still Java 8; JDK 21 is installed at
  `/usr/lib/jvm/java-21-openjdk-amd64`. Pin Gradle to it (Gradle toolchain /
  `org.gradle.java.home`) rather than changing the system default.

## Testing strategy

Tastefully mock-free, per the design notes — three tiers, fastest first:

1. **Pure-JVM unit tests (the bulk).** Room 2.7+ runs on the JVM via the
   bundled SQLite driver, so repository and ViewModel tests exercise the
   *real* database in ordinary fast unit tests: build an in-memory
   `ShoppingDatabase`, wire the real repository, drive the ViewModel, assert
   on emitted state. No fakes, no Robolectric, no emulator. Checkout
   semantics, search/add, availability sections, cascade deletes all live
   here.
2. **Headless Compose UI tests.** Compose rule tests running on the JVM
   (Robolectric-backed) for screen-level behavior: tap toggles, section
   membership, dialogs. During development, use the CLI's
   `android studio render-compose-preview` to eyeball composables without a
   device. Consult the `testing-setup` skill before wiring this tier.
3. **Emulator smoke test (final).** `android emulator create/start`, install
   with `android run`, walk the core flow by hand (need → shop → cart →
   checkout), and grab 2–3 screenshots with `android screen capture` for
   README.md.

## Milestones

Each milestone ends green (build + tests) and committed.

1. **Scaffold** — `android create` with package `net.randomhacks.retroshopper`
   into this repo; SDK install; JDK pinning; verify `gradlew build` and an
   example test pass headlessly. Install skills.
2. **Data layer** — entities, DAOs, `ShoppingRepository`, JVM tests
   (including checkout transaction and cascades).
3. **Home tab** — list / needed toggle / search / add, ViewModel + Compose
   tests.
4. **Shop tab** — store selector + add-store, availability sections, cart
   & checkout, ViewModel + Compose tests.
5. **Item details sheet & management** — the details sheet (rename, needed,
   delete, per-store availability + aisle), store rename/delete menu items.
6. **Polish** — Material 3 theming, edge-to-edge, app icon, empty states.
7. **Verification & docs** — emulator smoke test, screenshots, README.md.
