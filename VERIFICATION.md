# VOID Launcher verification

## Build
- Built by GitHub Actions `Build CI` on branch `void-single-app`, ARM64 debug.
- `:ZalithLauncher:assembleDebug -Darch=arm64` — BUILD SUCCESSFUL (run `45748752788` and later).
- APK verified with `apksigner verify`; `aapt2 dump badging` reports version `1.0.0-void-debug`.

## Device-verified (Android 16, ARM64, CPH2467)
- **Minecraft Java Edition reached its main menu inside the single VOID app.** This is real
  rendering by the bundled native engine, not an external launcher and not a mockup.
- VOID home screen renders in AMOLED black/white with status-bar-safe layout.
- Version browser loads the live Mojang manifest (926 versions at time of test).
- Offline account creation/persistence works.
- Library sub-tabs (Versions / Profiles / Mods / Loaders) render.
- Renderer list comes from the engine's real registry; MobileGlues is detected.
- Java tab lists the four bundled runtimes (8 / 17 / 21 / 25).
- VOID crash screen captures a readable stack trace and saves logs.
- Animated VOID loading overlay appears while the game starts.

## Built, not fully device-verified end-to-end
- Mod loader installation progress (wired to Zalith's task flow).
- Mod download from CurseForge/Modrinth with compatible-version highlighting.
- Foreground download notification with MB progress + cancel action.
- One-time VOID bootstrap with component progress.
- Touch-control editor with optional button images.

## Known limitations
- Minecraft is started from an external engine fork's code compiled into VOID; the combined
  app is GPL-3.0-or-later and retains upstream attribution.
- Physical keyboard/mouse/controller support depends on the engine layer.
- Microsoft entitlement path is implemented but not exercised with a real purchased account.
- Large version installs still depend on network stability; transient failures are retried and
  surfaced in the progress card instead of crashing the app.
