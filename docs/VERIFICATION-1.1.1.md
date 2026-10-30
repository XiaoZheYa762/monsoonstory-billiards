# 1.1.1 licensing and packaging verification

Date: **2026-10-30**. This release changes licensing, version metadata, legal
file packaging, source-package exclusions, official project links and publishing documentation.
No gameplay Java code or gameplay resources were changed.

## Builds

All four local Gradle builds passed:

| Module | Gradle JVM | Command |
|---|---|---|
| Fabric 1.20.1 | Java 21 | `fabric/gradlew.bat -p fabric --no-daemon build` |
| Forge 1.20.1 | Java 21 | `forge/gradlew.bat -p forge --no-daemon --offline build` |
| NeoForge 1.21.1 | Java 21 | `neoforge/gradlew.bat -p neoforge --no-daemon build` |
| NeoForge 26.1 | Java 25 | `neoforge-26.1/gradlew.bat -p neoforge-26.1 --no-daemon build` |

Forge's first online attempt stalled during dependency metadata lookup. That
build's processes were stopped and the build passed using the existing local
dependency cache in offline mode. This is not a clean-network dependency
download test. Existing build outputs and dependency caches were reused.

## Artifact and source checks

- `tools/verify_public.py` passed **2,346** shared public-resource checks.
- Validation of the four exact 1.1.1 installable JARs passed **4,525** checks
  in total (including those shared checks). The existing checks cover loader
  identity, version, exact Minecraft target, bytecode level, resources and
  exclusion of verification/runtime content.
- Each installable JAR contains exactly one current copy of `LICENSE`,
  `ASSET-LICENSE.md`, `ASSETS_LICENSE.md`, `NOTICE` and
  `THIRD_PARTY_NOTICES.md`. Their bytes match the repository files. Each
  platform's packaged metadata reports the custom source license and
  separate asset licensing.
- All four Gradle `-sources.jar` files were separately inspected for those
  same legal files and byte-for-byte consistency.
- Both modified Python tools passed Python syntax compilation.
- Git's upload candidate list, generated with a separate audit Git directory
  under ignored `build/`, was checked against the source-package allowlist.
  No build/runtime/cache directories or files outside that allowlist appeared.
  This initial audit preceded initialization of the project root as a Git repository.
- `tools/package_release.py` validates the four JARs, packages allowlisted
  source, verifies every archived file against `SOURCE_MANIFEST.json`, and
  writes `SHA256SUMS.txt` under `dist/1.1.1/`.
- Every file listed in the existing `dist/1.0.0/SHA256SUMS.txt` and
  `dist/1.1.0/SHA256SUMS.txt` still matches its original checksum.

## Scope

No new Minecraft client, dedicated-server, multiplayer, shader, modpack or
long-running gameplay acceptance test was performed for 1.1.1. The gameplay
and screenshot records in [1.1.0 verification](VERIFICATION-1.1.0.md) and the
older sections of [VERIFICATION.md](VERIFICATION.md) apply only to those older
artifacts, not to new 1.1.1 binaries.

## GitHub publication

The official repository is
[XiaoZheYa762/monsoonstory-billiards](https://github.com/XiaoZheYa762/monsoonstory-billiards).
All four loader metadata files include the official source/homepage and issue
URLs. The public source includes the existing four-target
[Build workflow](https://github.com/XiaoZheYa762/monsoonstory-billiards/actions/workflows/build.yml).
Consult the workflow run for the exact tagged commit for online CI results;
the local checks above are not evidence that an online run passed.

The `v1.1.1` release is prepared from the final committed source, with the four
installation JARs, a complete source ZIP, source manifest and SHA-256 list.
No mod-platform publication is included in this GitHub release work.

The custom license has not undergone legal review. The modification cannot
revoke permissions already granted for previously distributed MIT copies.
