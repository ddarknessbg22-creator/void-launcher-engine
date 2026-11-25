# VOID Engine

This repository is a modified fork of [ZalithLauncher2](https://github.com/ZalithLauncher/ZalithLauncher2)
by MovTery, licensed under **GPL-3.0-or-later**. The original copyright and license are preserved.

## What VOID changes

- Application label: **VOID Engine** (`launcher_name` in `ZalithLauncher/gradle.properties`).
- Version: `1.0.0-void` (`launcher_version_code/name`).
- AMOLED black theme forced in both light and dark system modes.
- VOID monochrome portal launcher icon.
- A prominent "Unofficial modified version of ZalithLauncher (GPL-3.0)" notice on the welcome screen.

## What VOID does NOT change

The real Minecraft engine: JVM launching, LWJGL, MobileGlues, Mods, version download and
controls remain those of ZalithLauncher2. This is intentional — it is a proven native engine.

## Building

GitHub Actions builds the ARM64 debug APK on every push to `main`.
Download it from the `debug apk (arm64)` artifact of a successful "Build CI" run.

## License

GPL-3.0-or-later. See `LICENSE`. This modified version is distributed under the same license.
