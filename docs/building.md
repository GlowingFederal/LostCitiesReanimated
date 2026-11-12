# Building and releases

The supported production targets are Forge 1.7.10 and Forge 1.12.2. These are separate Gradle builds under `versions/`, orchestrated by the repository-root wrapper. The root `src/` and `modern-reference/` preserve the original 1.20.1 implementation and upstream version as reference material; they are not production release targets.

## Requirements

Use the checked-in Gradle wrapper (Gradle 9.4.0) with JDK 25 as the launcher. Have JDK 8 and JDK 21 installed and discoverable as Gradle toolchains. Target 1.7.10 compiles with JDK 8; target 1.12.2 compiles with JDK 21 and `--release 8`. Both resulting mods run on Java 8. Existing target-specific Minecraft, Forge, mappings, dependencies and run configurations remain in each target build.

## Central version

Edit only the repository-root `version.properties` to change the mod release version:

```properties
version=0.1.0-dev
```

This preserves the active backport version at migration. The root build and both independent target builds load this property, including when invoked directly with `-p versions/<target>`. It controls Gradle's project version, archive filenames, processed `mcmod.info`, and a generated Java constant used by Forge's `@Mod` annotation. Generated source stays under each target's `build/` directory and is refreshed before compilation. Missing, empty or filename-unsafe versions fail configuration. Values must start with a letter or digit and contain only letters, digits, `.`, `_`, `+` and `-`. Command-line `-Pversion` is not an override; the file is authoritative.

## Release command

Run from the repository root:

```sh
./gradlew release
```

On Windows:

```bat
gradlew.bat release
```

The task first deletes the previous `build/release/` directory, then runs the independent target builds sequentially using the same wrapper, launcher and Gradle user home. Offline mode is forwarded to both builds. Each executes its existing `build` lifecycle and explicitly requests the final production task:

| Minecraft | Build plugin | Production task | Collected file |
| --- | --- | --- | --- |
| 1.7.10 | RetroFuturaGradle 2.0.4 | `reobfJar` | `lostcities-reanimated-<version>+mc1.7.10.jar` |
| 1.12.2 | Unimined 1.4.1 | `remapJar` | `lostcities-reanimated-<version>+mc1.12.2.jar` |

Only the exact standard distribution JAR from each target's `build/libs/` is collected into the root `build/release/`. Development (`-dev.jar`), API-only, source, Javadoc and intermediate artifacts are not collected. Old versions in target `build/libs/` cannot be selected accidentally. Both expected files must exist and be nonempty before either is copied. A nonzero child build exit or missing expected JAR fails the command with the Minecraft target identified. If a target fails, the previous release is already removed and no partial release set is collected. Successful up-to-date production outputs can be reused; a release does not force a full target clean.

## Development commands

The existing commands remain available:

```text
gradlew.bat build
gradlew.bat buildAll
gradlew.bat build1_7_10
gradlew.bat build1_12_2
gradlew.bat compileJava1_7_10
gradlew.bat compileJava1_12_2
gradlew.bat runClient1_7_10
gradlew.bat runServer1_12_2
gradlew.bat -p versions/1.7.10 build
gradlew.bat -p versions/1.12.2 build
```

Use `./gradlew` on other platforms. `build` and `buildAll` build both targets without collecting or clearing root release output. Target builds retain their ordinary production and development JARs under `versions/<target>/build/libs/`. Root `cleanRelease` removes only the collected release directory; root `clean` removes root build output, while target cleanup is available through `-p versions/<target> clean`.
