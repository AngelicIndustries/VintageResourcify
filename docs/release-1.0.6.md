# VintageResourcify 1.0.6 release checklist

Matching addon release: **VRAP 1.0.2**. This is local release preparation, not a record of publication.

## Compatibility

- VR packages and Maven group: `industries.angelic.vintageresourcify`.
- VRAP packages and Maven group: `industries.angelic.vrap`.
- Addons using `dev.dediamondpro.resourcify` must be rebuilt. VRAP 1.0.1 is for older VR; 1.0.2 requires VR 1.0.6+.
- Mod IDs, `config/vintageresourcify`, `config/vrap.cfg` and `.resourcify-index.json` remain unchanged. Do not delete these when upgrading.
- AngelLib 1.0.0 replaces FentLib for VR. Other mods may still need FentLib; do not remove it indiscriminately.
- Tested with ModularUI2 2.3.92 and Forgelin `2.4.3-2.4.10`. The Kotlin compiler and test stdlib use 2.4.10, matching Forgelin's runtime. The user's 2.5.0-Beta1 test stdlib compiled, but was aligned to the shipped runtime rather than used to validate an older runtime.
- Existing default 67Minecraft URLs resolve to `https://67minecraft.com/api/v2`; custom endpoints are preserved. Existing platform configuration is not rewritten.
- Packed Packs organization is now separate. VPP remains in development and needs a matching VR bridge update before integration testing; its current source still refers to the old VR package. It was not changed during this release preparation.

## Repeatable build checks

From VintageResourcify:

```sh
VERSION=1.0.6 ./gradlew spotlessApply build generatePomFileForMavenPublication --console=plain
python tools/verify_release_artifacts.py --version 1.0.6
```

From the sibling `vrap` checkout:

```sh
VERSION=1.0.2 ./gradlew spotlessApply build generatePomFileForMavenPublication \
  -PresourcifyDevJar="$(realpath ../VintageResourcify/build/libs/vintage-resourcify-1.0.6-dev.jar)" \
  --console=plain
```

The local jar override is only for testing before Maven publication. Normal VRAP builds resolve VR 1.0.6 from Angelic Maven.

## Verification performed (2026-10-19)

- Regular build: 20 tests passed, including provider API, downloads/checksums, update-index filename changes and release/config migration.
- CurseForge build: 14 provider/download tests passed against its replacement policy classes.
- VRAP build and new API linkage passed.
- Artifact inspection: both VR distributions have renamed classes, mixin configurations/refmap/core plugin, relocated Java-8-compatible CommonMark, correct metadata, and distinct distribution policies. Neither bundles Kotlin or AngelLib. Maven POM uses the new group and AngelLib dependency.
- Linux Xvfb, Java 25/lwjgl3ify: real mouse/keyboard browsing, project Markdown/images, resource-pack download and enable, installed markers and update checking passed.
- Angelica 2.2.35: downloaded and enabled BSL 10.1.7, then updated it to 10.1.8 through VR. The old file was replaced, the update index followed the new filename, and the active shader setting became `BSL_v10.1.8.zip`.
- Java 8 client: restarted with the downloaded resource pack and shader retained; browsing/project rendering and installed markers passed. Clean shutdown passed.
- Java 8 dedicated server: loaded VR, AngelLib and VRAP, reached `Done`, then saved and stopped cleanly. Client-only dependencies were excluded by the run task.
- VRAP fetched a fake token from a localhost fixture and applied it on client and server startup. This tests its integration, not live CurseForge authorization.
- Actual CurseForge development jar on Java 8: startup with VRAP, live Modrinth browsing and project rendering passed. Its platform dropdown contained Modrinth and CurseForge, without the regular build's configured 67Minecraft platform. Clean shutdown passed.

The isolated test instances are `run/release-client`, `run/release-server` and `../vrap/run/release-curseforge`. Screenshots are under `run/release-evidence`; launch logs are under each project's `run/`. No personal game instance or real API token was used. `tools/release_ui_smoke.py` contains input helpers restricted to X display `:179` and the localhost token fixture.

The dev stack emits unrelated NEI missing-resource/texture warnings and BSL warnings about newer biome constants. Passing these checks does not mean the entire log is warning-free or that BSL was visually verified in a world.

## Final manual checks before a stable tag

1. Back up an existing instance, then replace VR/VRAP with the matching jars and install AngelLib plus the tested Kotlin/MUI dependencies. Confirm settings, enabled resource-pack order and update tracking survive the upgrade.
2. On the normal desktop/OS, test the resource-pack and shader file pickers, external drag-and-drop, browser links and closing/reopening each screen. Xvfb checks do not cover native desktop dialogs or macOS integration.
3. Test SwanSong separately if it remains part of the advertised release support. Automated shader testing used Angelica only.
4. With an authorized CurseForge token, test a real CurseForge search/download/update. The fixture does not validate service credentials or approval.
5. Review both changelogs and confirm repository publishing secrets/permissions are available in the new organization. Public endpoint/workflow checks do not validate account permissions.

## Publication order

1. Commit the reviewed VR changes and tag/release **1.0.6** when approved. Its shared workflow is pinned to `AngelicIndustries/67minecraft-gradle-publish@1.0.3`; CurseForge receives the restricted jar, while the regular jar goes to the other configured destinations.
2. Confirm the new Maven coordinate `industries.angelic.vintageresourcify:VintageResourcify:1.0.6` and its `dev` classifier are available, along with all platform artifacts.
3. Commit/tag/release **VRAP 1.0.2**. Its CI depends on the preceding VR Maven publication.
4. Verify download links and dependency relations on the published pages. Update any external addon examples/wiki imports to the new VR package.

Do not resume VPP changes as part of this release. Resume its namespace/bridge work and remaining UX tests after VR is stable.
