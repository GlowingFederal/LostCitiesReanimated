# Lost Cities Reanimated

A Java 8 backport of Lost Cities for Forge **1.7.10** and **1.12.2**, with modern building assets, city planning, hierarchical streets, intercity highways, configurable railways, multibuildings, scattered structures, interiors, loot, damage, and overgrowth. This is a development backport; see [the architecture and compatibility notes](docs/backport.md) for the migration boundaries.

Install the JAR matching your Minecraft version. Lost Cities runs independently. Universal Adaptive Backporter is optional: available blocks and supported states are detected from the actual registry. Native materials, optional providers, and functional substitutes are resolved centrally. No optional block provider is a mandatory dependency of Lost Cities.

Create a world using the **lostcities** world type. Servers can set `level-type=lostcities` and select a profile with `generator-settings={"profile":"default"}`. Configuration is under `config/lostcities/`: `general.cfg` controls providers, assets, dimensions and world types; `profile_<name>.cfg` contains generation options, including the `modern_<name>` category. Existing profile identifiers and legacy settings are retained. The default extra dimension is 111; a bed on two diamond blocks, surrounded by six skulls, provides travel between it and the overworld. Additional dimensions are configured separately.

Modern asset overrides use `config/lostcities/modern/<category>/<name>.json`. Namespaced packs can use `config/lostcities/modern/data/<namespace>/lostcities/<category>/<name>.json` and reference those assets as `<namespace>:<name>`. Custom loot tables can use `modern/data/<namespace>/loot_tables/<path>.json`; supported formats and target-specific functions are described in the compatibility notes. Legacy asset libraries remain supported. New worlds record their generation mode, material choices, and highway hub decisions in saved data. Existing worlds without that marker retain legacy generation. Completed chunks are not regenerated. Back up worlds before changing generation settings or removing a block provider; modern city chunks save native fallback snapshots for selected optional materials and supported inventories. Save with the provider installed before removing it; older chunks without snapshots cannot be reconstructed. See the compatibility notes for the scope and Forge missing-mod confirmation.

To add cities to a normal overworld, opt in with `overlayDimensions` in the general configuration, such as `0:realistic`. This affects future chunks and preserves the saved world type. Native BOP overworlds are also supported. Custom dimension generators require the native terrain adapter API; see the [profile options](docs/profile_options.md).

The `spheres` and `cavernspheres` profiles enclose cities in modern glass shells over native terrain. Editor operators can use `lc_buildpart <part>`, `lc_exportpart <file> <slices>`, `lc_exportbuilding <file> <floors>` and `lc_debug`. Exports contain native materials, block-entity data and self-contained palettes; each building floor is six blocks high.

Build with **JDK 25** as the Gradle launcher, with JDK 8 and JDK 21 available as toolchains:

```text
./gradlew release
# Windows:
gradlew.bat release
```

The root `version.properties` controls the release version for both targets, their Forge mod metadata, and all artifact filenames. Its initial value preserves the backport's `0.1.0-dev` version. Change that file to set the next release version; Minecraft, Forge, mappings and toolchain settings stay in the target builds.

`release` builds each target independently through its production reobfuscation/remapping pipeline, clears the previous `build/release/` output, and collects exactly these two distribution JARs:

```text
build/release/lostcities-reanimated-<version>+mc1.7.10.jar
build/release/lostcities-reanimated-<version>+mc1.12.2.jar
```

Development, API, sources and intermediate JARs are excluded. A failed target or missing distribution JAR fails the release command. Ordinary `build`, `buildAll`, target-specific compilation and run tasks remain available; see [building and releases](docs/building.md).

Native Minecraft/Forge code lives in each version directory. `common/` contains portable planners, asset processing and material decisions. The root `src/` and `modern-reference/` retain the original 1.20.1 implementation as a reference and are excluded from both legacy production builds. Upstream authorship and MIT licensing are retained; see [attribution](THIRD_PARTY_NOTICES.md).

Developer documentation:

- [Asset system and custom expansions](https://mcjty.eu/docs/mods/lost-cities/asset_structure)
- [City generation](https://mcjty.eu/docs/mods/lost-cities/city_generation)
- [Native legacy profile options](docs/profile_options.md)
- [Upstream profile reference](https://mcjty.eu/docs/mods/lost-cities/profile_options)
- [Legacy backport architecture and compatibility](docs/backport.md)
- [Building and releases](docs/building.md)
- [World-generation thread safety](https://mcjty.eu/docs/mods/lost-cities/thread_safety)
- [Changelog](CHANGELOG.md)
