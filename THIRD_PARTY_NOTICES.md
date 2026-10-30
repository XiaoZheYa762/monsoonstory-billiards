# Third-party notices

The custom project license does not replace the licenses below. Original
project creative assets are separately governed by `ASSET-LICENSE.md`.

## Billiard sound recordings — CC0 1.0

| Author | Recording and source | Included source files | Processing / use |
|---|---|---|---|
| Za-Games | [billiard ball clack, 539854](https://freesound.org/people/Za-Games/sounds/539854/) | `art/audio/539854.mp3` | Filtering, gain and pitch adjustments; ball collisions, cue and cushion interactions |
| csaszi | [BILLIARD pool shots- CsG.wav, 350918](https://freesound.org/people/csaszi/sounds/350918/) | `art/audio/350918.mp3` | Trimmed and level-adjusted; pocket, rack and rolling sounds |

License: [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/). Derived OGG files are under `common/src/main/resources/assets/monsoonstory_billiards/sounds/`. Match start, turn, foul and result notification tones are synthesized project sounds. The public source includes the CC0 recordings and source links, without redistributing the source website's HTML.

## Build and runtime dependencies

Dependencies are obtained from their official Maven repositories. Their binaries are not copied into the source archive or shaded into the mod, except for the standard Gradle Wrapper used to bootstrap each build.

- [Gradle](https://github.com/gradle/gradle): Apache-2.0. The wrapper JARs and launcher scripts are part of the build tool distribution.
- [Fabric Loader](https://github.com/FabricMC/fabric-loader): Apache-2.0.
- [Fabric API](https://github.com/FabricMC/fabric): Apache-2.0.
- [NeoForge](https://github.com/neoforged/NeoForge): LGPL-2.1-only; see its distribution for component notices.
- [LWJGL](https://github.com/LWJGL/lwjgl3): BSD-3-Clause. The client uses Minecraft's existing LWJGL/STB Vorbis decoder; no extra decoder binary is bundled.
- [Better Combat](https://github.com/ZsoltMolnarrr/BetterCombat): optional external mod. The project refers to its `bettercombat:battlestaff` preset and compatible API; no Better Combat code or animations are redistributed here.

Minecraft is a separate proprietary product and is required to run the mod. This project does not distribute Minecraft game files or claim endorsement by Mojang or Microsoft.
