# Build status — 0.1.1 — 2026-10-25

- Target: GTNH 2.8.4 / Minecraft 1.7.10 / Forge 10.13.4.1614 / AE2 rv3-beta-695-GTNH.
- Pre-change 0.1.0 source pushed to GitHub: 36c7efdccae7317ee8894e81a3e87ed850288f37.
- `gradlew.bat test build`: PASS.
- 14 JUnit tests: PASS (0 failures, 0 errors).
- New coverage: persistent network ID binding/lookup/NBT reload, valid network ID zero, legacy/corrupt ID detection, full Pinyin, initials, mixed queries, alternate readings, formatting codes.
- Existing packet, face-bounds and locale tests: PASS.
- Spotless formatting and Checkstyle: PASS.
- Production JAR pinyin verification with an isolated classloader and no external dependencies: 8 cases PASS.
- Embedded PinIn/Fastutil packages relocated into the mod namespace; dictionary and licenses included: PASS.
- Reobfuscated production JAR generated, Java 8 bytecode (major 52): PASS.

Behavior: the tool now stores AE2's persistent grid-storage ID instead of a cable position or dimension. Every query still checks the current network's BUILD permission. Legacy tools migrate once through their loaded old anchor or can be rebound to any node in the desired network. No chunks or dimensions are force-loaded.

Not yet performed by the agent: in-game cross-dimension/quantum-bridge, legacy-tool migration, GUI and multiplayer acceptance in the user's full GTNH instance. A fully unloaded or dissolved/reformed network may need loading or rebinding. See TESTING.md.
