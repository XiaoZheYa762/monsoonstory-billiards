# Changelog

## 1.1.1 — 2026-10-30

- Replace the original-code MIT notice with Jeefeng Source License v1.0: non-commercial use and same-license source publication when distributing modifications.
- Separately license original creative assets under Jeefeng Story Asset License; retain third-party CC0 and build-tool notices.
- Include source/asset licenses, NOTICE and third-party notices in all platform JARs and source archives.
- Expand GitHub upload and release instructions. Preserve older release artifacts and any permissions already granted in MIT-distributed copies.
- No gameplay code changes. Verification for earlier releases remains historical evidence for those artifacts only.

## 1.1.0

- Add separate builds for Forge 1.20.1 and NeoForge 26.1 while retaining Fabric 1.20.1 and NeoForge 1.21.1.
- Rotate pending table cleanup so an unloaded table cannot indefinitely block cleanup of other loaded tables.
- Preserve the stored music library and table assignments when the server stops before asynchronous initialization reaches the main thread.
- Extend build, release validation and source packaging to all four targets; add regression checks for both reviewed bugs.

## 1.0.0

- Introduce the independent `monsoonstory_billiards` public edition for Fabric 1.20.1 and NeoForge 1.21.1.
- Retain eight-ball, nine-ball, practice, multiplayer, spectators, three ordinary cue/table styles, furniture, souvenirs, achievements, shot settings, prediction and cameras.
- Add a creative tab, survival recipes and recipe-book unlocks.
- Replace bundled background music with operator-managed OGG Vorbis uploads and nearby shared playback.
- Exclude themed special cues, the former background song, the former logo and the personal-server confetti integration.
- Reject strike packets that would cancel another player's aiming state; validate saved table fields and isolate malformed cleanup records.
- Add independent reproducible builds, source packaging, asset notices and release checks.
