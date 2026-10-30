# Monsoon Story: Billiards

[中文说明](README.zh-CN.md)

[Downloads](https://github.com/XiaoZheYa762/monsoonstory-billiards/releases) · [Source](https://github.com/XiaoZheYa762/monsoonstory-billiards) · [Issues and licensing requests](https://github.com/XiaoZheYa762/monsoonstory-billiards/issues)

Play casual eight-ball, nine-ball, and solo practice in your Minecraft world, with two-player matches, spectators, three table and ordinary cue styles, spin, aiming prediction, continuous ball-following cameras, and billiard-room furniture.

## Installation

Install the same mod version for the matching Minecraft version and loader on both the client and server.

| Minecraft version and loader | Java | Required dependencies |
|---|---|---|
| Fabric 1.20.1 | 17 or later | Fabric Loader 0.16.9 or later, Fabric API |
| Forge 1.20.1 | 17 | Forge 47.4.28 |
| NeoForge 1.21.1 | 21 | NeoForge 21.1.264 or later for the same Minecraft version |
| NeoForge 26.1 | 25 | NeoForge 26.1.0.19-beta |

Better Combat is optional. Without it, ordinary cues still support vanilla left-click attacks and charged right-click thrusts. No other Monsoon Story mod is required.

## Getting started

Find the items in the **Monsoon Story: Billiards** creative tab, or craft them in survival. A table needs a flat, supported, clear **10 × 6** area. After placing it, right-click the table and choose practice or create a match. The match starts once a second player has joined and readied up.

Hold a cue and right-click the table to aim. First release the right-click used to enter aiming, then hold right-click to charge and release to strike.

Default controls:

| Key | Action |
|---|---|
| <kbd>G</kbd> | Toggle the aiming guide |
| <kbd>Alt</kbd> | Adjust the cue-ball contact point |
| <kbd>R</kbd> | Center the cue-ball contact point |
| <kbd>Shift</kbd> | Fine aiming |
| <kbd>O</kbd> | Open settings |
| <kbd>V</kbd> | Change the overview camera position |
| <kbd>Esc</kbd> | Cancel aiming |

Key bindings can be changed in Minecraft's controls settings.

Eight-ball uses the casual house rules described in the interface, with a called pocket required for the black eight. In nine-ball, the cue ball must contact the lowest-numbered object ball on the table first. Snooker is not currently included. Any nearby player can reset a table; dismantling is managed by its placer and operators.

| Crafted item | Materials for one item |
|---|---|
| Table in any of the three colors | 3 matching carpets, 4 planks, 1 iron ingot |
| Ordinary cue in any of the three colors | 2 sticks, 1 iron nugget, 1 matching carpet |
| Cue rack | 4 sticks, 3 planks |
| Floor scoreboard | 6 planks, 1 red dye, 1 blue dye |
| Lamp in any of the three colors | 3 planks, 2 iron ingots, 1 matching carpet, 3 lanterns |

The deep blue style uses blue carpet; check the recipe book for the crafting patterns. Ordinary cues have a maximum shot power of 100% and 128 durability. Playing pool does not consume durability; successful combat hits do. Cue racks preserve items in all six slots, including their item data. Right-click the corresponding side of a scoreboard to add a point, or sneak-right-click to reset it. Lamps require support above them. Souvenir balls retain the match-win reward mechanic, while the temporary cue ball is only a server-issued tool for placing balls.

## Administrator-uploaded music

Only operators with permission level 2 or higher can select local **OGG Vorbis** files in the table's music interface, upload them, select tracks, and control playback. Other players listen nearby and adjust their own volume, which defaults to 25%. No background songs are bundled.

Players begin listening within 8 blocks and stop, with a fade-out, beyond 10 blocks. When several tables are playing, only one nearby table is heard at a time. After a download finishes, playback starts at the current shared position. Muting music stops downloads. Track selections are saved, but playback pauses after a server restart until an administrator resumes it.

The defaults are 20 MiB and 10 minutes per track, a 1 GiB library, and a total transfer limit of 1 MiB/s. Server owners can adjust these limits. Music is stored in the world's `monsoonstory_billiards/music` directory, and client downloads are cached locally.

## License and copyright

Original code uses the custom [Jeefeng Source License v1.0](LICENSE): personal, non-commercial use and modification are allowed. Distributing a modified version requires the complete corresponding source code and build scripts, the same license, retained attribution, and clearly identified changes. Commercial use, sale, or paid distribution requires prior written permission from the rightsholder. Private modifications do not need to be published if they are not distributed.

Original textures, models, GUIs, icons, and other creative assets use the separate [asset license](ASSET-LICENSE.md), with rights reserved. Only personal, non-commercial gameplay and local building and running are expressly permitted. Extracting and reusing assets, distributing them with modified versions, or repackaging them in free modpacks requires separate written permission. Code forks may replace the assets with materials they are entitled to distribute. Third-party CC0 recordings and their derived sound effects retain CC0 licensing.
