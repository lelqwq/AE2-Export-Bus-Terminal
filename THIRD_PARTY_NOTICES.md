# Third-party notices

The Gradle Wrapper (`gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`)
is from the GTNewHorizons ExampleMod1.7.10 project and the Gradle project. It is
provided under the Apache License, Version 2.0, as indicated by the wrapper script
headers. See <https://www.apache.org/licenses/LICENSE-2.0>.

GTNH's Gradle convention plugin is fetched at build time under its upstream
license. Minecraft, Forge, Applied Energistics 2 and their dependencies are not
included in the release JAR or the source ZIP; they are resolved separately by
the build or provided by the user's GTNH installation.

The mod implementation, resources and documentation in this repository are
provided under the MIT License in `LICENSE`.

## Embedded pinyin matching

PinIn 1.6.0 by Juntong Liu (Towdium) is embedded under its MIT License:
<https://github.com/Towdium/PinIn/tree/1.6.0>. The full license text is included
in `src/main/resources/META-INF/licenses/PinIn-LICENSE.txt` and the release JAR.
PinIn's pinyin data is credited upstream to rime-terra-pinyin and pinyin-data.

The needed Fastutil 8.3.0 classes are embedded under the Apache License 2.0,
Copyright (c) 2002-2020 Sebastiano Vigna. A full copy of the license is included
in `src/main/resources/META-INF/licenses/Fastutil-Apache-2.0.txt`.

The embedded packages are relocated into this mod's namespace to avoid clashes
with other installed mods. Neither PinIn nor Fastutil needs a separate mod JAR.
