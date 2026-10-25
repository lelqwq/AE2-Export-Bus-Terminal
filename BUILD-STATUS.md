# Build status — 0.1.2 — 2026-10-25

- Target: GTNH 2.8.4 / Minecraft 1.7.10 / Forge 10.13.4.1614 / AE2 rv3-beta-695-GTNH.
- Fluid integration: AE2 Fluid Crafting 1.4.120-gtnh, already included in GTNH 2.8.4.
- `gradlew.bat test build`: PASS.
- 19 JUnit tests: PASS (0 failures, 0 errors).
- New coverage: actual AE2FC export/import class distinction; fluid display-packet NBT name/amount/variant preservation; malformed/empty filter handling; fluid names/Pinyin/initials; bus-type flag packet roundtrip.
- Existing network identity, item search, locale, face bounds and packet tests: PASS.
- Spotless formatting and Checkstyle: PASS.
- Production JAR isolated pinyin checks without external libraries: 11 cases PASS, including fluid names.
- Production JAR includes fluid integration classes, dictionary and license resources; Java 8 class version 52: PASS.
- No AE2, AE2FC, Minecraft or test classes bundled: PASS.

The list now includes both item and AE2FC fluid export buses, with type labels, real fluid icons/colors, localized fluid names and fluid registry-name search. Both reuse the existing network identity, permissions and face-location highlighting. Fluid filters are copied for display without modifying server-side configurations.

The packet layout includes a new fluid-bus flag; update client and server together to 0.1.2 and remove the previous JAR. Existing tool bindings remain usable.

Not performed by the agent: in-game rendering and fluid-bus/multiplayer acceptance in the user's full GTNH instance. See TESTING.md steps 15-17 for the fluid checks.
