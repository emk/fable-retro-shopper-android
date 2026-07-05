# Design Notes

An Android shopping app loosely inspired by the classic PalmPilot HandyShopper. For historic background, see [PalmDB](https://palmdb.net/app/handyshopper) (has a single low-res screenshot) and [the author's docs](https://www.ggaub.com/hs/readme.html). For our purposes, we are interested in the core feature set:

- One view, used at home, for marking items as needed.
    - Items have a name, and an indicator showing if they are needed.
    - Basic search and add functionality would also be useful.
- Another view, used for shopping. This has per-store profiles. For each item, profiles allow indicating:
    - Whether this item can be found at this store. For items which have not been previously bought at this store, there should be some way to indicate that they can be. But it should also be easy to focus only on items available in this store.
    - What aisle of this store the item is found in.
    - A way to check off the item when bought.

Out of scope:

- Price and quantity.
- Multiple databases. Just one shopping database is fine.
- Complex multi-column sorting.
- Per-item alarms.

## Audience & Implications

This app is intended for developer side-loading and should at least initially use on-device storage. At some point in the future, it might be modified to use a backend like Firestore to support use within a single family. The suggests encapsulating data access behind an application-level API that can later potentially be reimplemented using Firestore.

As a secondary audience, the code should be written to clear, simple and based on modern Android best practices. This is half a pedagogical exercise for me, because I haven't touched Android seriously in over a decade.

Google Play is not a target.

## Suggestions

Some suggestions to consider:

- Definitely Kotlin.
- Jetpack Compose & Gradle.
- Use the [new Android CLI tools](https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html) designed for agents.
- We're going to want to find and install the new official Android skills for guidance.
- Test as much as possible using pure Kotlin and the CLI for speed and convenience.
- I am told that "Android CLI itself can render Compose previews and run UI tests headlessly." If so, we should use that for as much testing as we can.
- We might want a final testing stage with a full emulator?

Let's also try to be tasteful about mocking and dependency injection. We should follow platform conventions where reasonable, but when it is straightforward to test (say) both the "business model" layer and the database at once, there's no need to do elaborate dependency injection to test each part in artificial isolation. Essentially, mocking and DI are a price to be paid in exchange for some payoff (testing things we can't otherwise, adhering to major conventions), not a goal in themselves. (Yes, this is very high-level guidance for generic projects in arbitrary environments, and you'll have to decide how it actually applies tastefully in this specific case.)

## Workflow

We'll plan, refine and answer questions where needed, and write a permanent copy of the plan as PLAN.md. Use git init to set up a local git repo and commit as you go. When you're done, see if the tools allow you capture 2-3 screenshots for inclusion in a README.md.
