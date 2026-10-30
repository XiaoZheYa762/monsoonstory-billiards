# Verification

## 1.1.1 — 2026-10-30

Current licensing, build and artifact checks are recorded in
[1.1.1 verification](VERIFICATION-1.1.1.md). This update does not repeat
in-game or multiplayer acceptance testing. Earlier reports below remain
evidence only for their own release artifacts.

## 1.1.0 — 2026-10-25

The 1.1.0 review and port verification is recorded in [1.1.0 verification](VERIFICATION-1.1.0.md). The checks in the following 1.0.0 section are historical evidence for the **1.0.0 artifacts only**; they do not establish client or multiplayer coverage for the newer ports.

## 1.0.0 — 2026-10-24

Checks use isolated development worlds and test accounts, without modifying the original personal-edition project or player saves.

## Shared code and resources

- Both platform builds pass the shared rule, physics, spin, nine-ball, guide, prediction and power checks: 85,602 logic checks, 116,057 prediction checks and 6,111 power checks per platform.
- Music protocol/storage integration: **46 checks passed**, including real asynchronous client/server uploads and downloads, multiple chunks, SHA-256, Ogg CRC, size/duration limits, permission changes, interrupted transfers, cache recovery, reconnect races, pause/resume and paused persistence after restart.
- Native Vorbis decoder: **5 checks passed**, covering PCM decoding, sample-position seek, looping and invalid metadata rejection. This test requires no window or audio device.
- Resource checks verify JSON, matching English/Chinese keys, eleven recipes and recipe-book unlocks, referenced models/textures/sounds, excluded assets, loader metadata and Java bytecode versions.

## Fabric 1.20.1

- Installable release JAR built successfully, targeting Java 17.
- Real Fabric GameTest server: **14 public-safety checks passed**, including malformed saved tables/cleanup entries and spectator strike packets during charge and before contact.
- Dedicated server with **three actual remote clients** passed practice/room controls, cue-ball placement and repositioning, aiming and contact-point camera, charged shots, observer motion, called-black victory, GUI scales, resource-pack overrides, disconnect/reconnect, timeout, rematch and save roundtrip.
- The same isolated server process was stopped and restarted. Cross-chunk structures, angular/linear ball state and paused match recovery passed the restart checks.
- An operator uploaded an OGG file through the production protocol. Two ordinary clients downloaded it and opened the custom stream in the actual Minecraft sound engine. The fixture was silent; this verifies the playback pipeline, not a subjective listening evaluation.
- Screenshots under `docs/images/fabric-*.png` are unmodified captures from this acceptance run.

Tested Fabric JAR SHA-256:

```text
4af5cc9cd78622784f62688f952696f3768934ddd5ae43b4d7d444cdc9a037f2
```

## NeoForge 1.21.1

- Installable release JAR built successfully, targeting Java 21.
- **40 native GameTest checks passed while loading the actual release JAR**: three table styles, lamps, preserved held-item interaction, scoreboard drops and scores, rack inventory/components, white-ball token/epoch checks, malformed save isolation, public reset and spectator-strike protection.
- Real client without Better Combat: **41 checks passed**, including all public item models, tables and furniture, controls registration, menus, charging, shot cameras, uploaded OGG streaming and screenshots.
- The same JAR with Better Combat 2.4.0, Player Animation 2.0.4 and Cloth Config 15.0.140 passed another **40 native server checks**.
- With those optional mods installed, **two actual remote clients** passed **45 operator checks and 3 ordinary-listener checks**. The listener observed the other player's cue pose and shot, had no operator permission or music-management entry, was refused an upload request, and downloaded/played the same song hash through the Minecraft sound engine.
- Eleven operator screenshots and two listener screenshots were captured. Menu, gallery, music and return-to-settings screenshots were visually checked after fixing the 1.21 background-blur difference.

Tested NeoForge JAR SHA-256:

```text
e1718761db7531c59afb2aeb0c4b74d1b6674b13a845f679c25746884fe575d0
```

## Clean-source rebuild and packaging

Both builds were repeated from clean, allowlisted source copies in separate directories, using their committed Gradle Wrapper versions and standard dependency caches. Both rebuilt JARs are **byte-for-byte identical** to their tested release JARs, with the SHA-256 values above.

The source ZIP is checked against `SOURCE_MANIFEST.json`; each archived file matches the working source byte-for-byte. Installable JAR checks reject verification code, old namespaces, excluded assets and runtime directories. Neither artifact includes a user's uploaded music or private world/account data.

## Repeating the checks

Run from the repository root:

```powershell
python tools/verify_public.py
.\fabric\gradlew.bat -p fabric verifyLogic verifyPrediction verifyPower verifyVorbis verifyPublicSafety
.\neoforge\gradlew.bat -p neoforge verifyLogic verifyPrediction verifyPower runGameTestServer
pwsh -NoProfile -File tools/test_music.ps1
```

For Fabric's production-JAR three-client test, build `remapVerificationJar exportVerificationClasspath`, then run `python tools/run_multiplayer.py`. It creates an isolated localhost server and test clients under `fabric/run`, and writes its report under `fabric/build`. It starts real game windows and closes only the processes it created.

The test reports and runtime directories are excluded from the source archive. Test-only classes and verification-mod metadata are excluded from the installable JARs. These checks do not claim coverage of arbitrary modpacks, shader packs, public-network latency or every operating system.
