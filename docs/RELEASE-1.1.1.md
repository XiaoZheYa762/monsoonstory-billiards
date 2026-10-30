# Monsoon Story: Billiards 1.1.1 / 季风物语：桌球

This release updates licensing and official project links. No gameplay code
or gameplay resources were changed.

## Changes / 更新内容

- Original source code uses **Jeefeng Source License v1.0**: personal,
  non-commercial use; distribution of modifications requires complete
  corresponding source under the same license, attribution and a clear
  identification of changes. Commercial use or sale requires written permission.
- Original creative assets use the separate **Jeefeng Story Asset License**.
  Reuse or redistribution, including in free modpacks and modified mods,
  requires separate permission. Listed third-party CC0 material keeps its terms.
- Every platform JAR includes the current source/asset licenses and notices.
- Official source, homepage and issue links are included in loader metadata.
- Earlier MIT-distributed copies retain the permissions already granted.

源码采用“非商业、修改版分发须提供同协议完整源码”的自定义源码可见许可；原创
贴图、模型等素材单独授权。新协议不能撤回旧 MIT 副本已经授予的权限。

## Downloads and installation / 下载与安装

Choose **one** matching installation JAR and install it on both client and
server. Different loaders and game versions are separate builds.

| File | Minecraft / loader | Java | Required loader/dependencies |
|---|---|---|---|
| `monsoonstory_billiards-fabric-1.20.1-1.1.1.jar` | Fabric 1.20.1 | 17+ | Fabric Loader 0.16.9+ and Fabric API for 1.20.1 |
| `monsoonstory_billiards-forge-1.20.1-1.1.1.jar` | Forge 1.20.1 | 17+ | Forge 47.4.28+ |
| `monsoonstory_billiards-neoforge-1.21.1-1.1.1.jar` | NeoForge 1.21.1 | 21+ | NeoForge 21.1.264+ |
| `monsoonstory_billiards-neoforge-26.1-1.1.1.jar` | NeoForge exactly 26.1 | 25+ | NeoForge 26.1.0.19-beta or newer, below 26.1.1 |

Better Combat is optional. Do not select 26.1.1 or 26.1.2 as compatible game
versions. Player-uploaded music is external content and is not bundled.

`monsoonstory_billiards-1.1.1-github-source.zip` contains the complete source,
build scripts, resources and documentation. It is for development, not player
installation. `SOURCE_MANIFEST.json` lists source-file hashes;
`SHA256SUMS.txt` lists the four JAR, source ZIP and manifest hashes.

## Verification / 验证范围

All four local builds and installation-JAR/public-resource checks passed.
The GitHub Build workflow checks all four targets and runs shared rule,
prediction and power regressions, plus Fabric Vorbis and music-transfer checks.
See the Actions run for this release's exact commit for the online result.

No new real-client, dedicated-server, multiplayer, shader, modpack or
long-running gameplay acceptance test was performed for 1.1.1. Historical
gameplay reports and screenshots apply to their stated older releases.

Source: https://github.com/XiaoZheYa762/monsoonstory-billiards

Issues and licensing requests: https://github.com/XiaoZheYa762/monsoonstory-billiards/issues
