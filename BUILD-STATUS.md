# Build status — 0.1.3 — 2026-10-25

- Target: GTNH 2.8.4 / Minecraft 1.7.10 / Forge 10.13.4.1614 / AE2 rv3-beta-695-GTNH.
- Fluid integration: AE2 Fluid Crafting 1.4.120-gtnh, already included in GTNH 2.8.4.
- `gradlew.bat spotlessApply test build --console=plain`: PASS.
- 22 JUnit tests: PASS (0 failures, 0 errors).
- New sorting coverage: mixed item/fluid names, multiple configured names, empty configurations last, color-code/case handling, Chinese collation, current-dimension priority, other-dimension order and stable equal-name ties.
- Existing network identity, item/fluid search, locale, face bounds, fluid compatibility/NBT and packet tests: PASS.
- Spotless formatting and Checkstyle: PASS.
- Production JAR isolated pinyin checks without external libraries: 11 cases PASS, including fluid names.
- Production JAR sorting class present, Java 8 class version 52: PASS.
- No AE2, AE2FC, Minecraft, unrelocated embedded-library or test classes bundled: PASS.

The GUI defaults to localized name sorting and offers a button to switch to the original server-provided distance order. Both modes retain dimension grouping. Equal names retain server distance/position order. Multiple filter slots use the alphabetically first configured name; unconfigured buses appear last within their dimension. The selection survives refreshes and reopened screens during the same game run; restarting restores name sorting.

Network packet layout and existing tool bindings are unchanged from 0.1.2. Install 0.1.3 on client and server and remove the previous JAR.

Not performed by the agent: in-game rendering, button interaction and multiplayer acceptance in the full GTNH instance. See TESTING.md steps 18-19 for sorting checks and the preceding checks for existing behavior.
