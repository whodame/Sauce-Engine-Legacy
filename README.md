<div align="center">

[![Star this repo](https://img.shields.io/badge/⭐_Star-this_repo-yellow?style=for-the-badge)](https://github.com/whodame/Sauce-Engine-Legacy)
[![Fork](https://img.shields.io/badge/🍴_Fork-the_repo-blue?style=for-the-badge)](https://github.com/whodame/Sauce-Engine-Legacy/fork)
[![Watch](https://img.shields.io/badge/👀_Watch-for_updates-success?style=for-the-badge)](https://github.com/whodame/Sauce-Engine-Legacy)

**Original repository (upstream Source Engine):** [nillerusr/source-engine](https://github.com/nillerusr/source-engine)

⭐ If this project is useful to you, please star the repository, and watch it to follow updates!

</div>

---

# Sauce Engine (Legacy)

**Sauce Engine Legacy** is a polished, stable, and highly portable 64-bit fork of the classic Source Engine (based on the 2018 TF2 code leak), modernized for native compilation and seamless execution on contemporary operating systems (Windows x64 / MSVC, Linux x86_64, macOS Apple Silicon / ARM64, FreeBSD, and OpenBSD).

**Sauce Engine Legacy** maintains full backward compatibility with original Valve game assets (HL 2, CS: Source, TF 2, Portal) while resolving hundreds of critical build failures, 64-bit IVP physics alignment crashes, screenshot/save crashes, shader combo lookup mismatches, and obsolete binary dependencies.

---

## Features & Improvements

- **Modern Toolchain Support**: Native builds with MSVC 2022 (v143 / v144), Clang 16+, and GCC 12+.
- **Full 64-bit Portability (x86_64 / ARM64)**:
  - Fixed 64-bit IVP physics crashes.
  - Built-in `jpeglib` (version 8b) compilation from source, removing legacy 32-bit LTCG binary blobs.
  - Fully compatible with modern system SDKs (Windows 10/11 SDK, macOS 14/15/26 SDK).
- **True Cross-Platform Runtime**:
  - Windows x64 (DirectX 9 / OpenGL / ToGL).
  - Linux (glibc, musl) and BSD variants (FreeBSD, OpenBSD with native `sndio` audio backend and `unveil(2)`/`pledge(2)` sandboxing).
  - macOS (Apple Silicon / ARM64 with gamma-correction sRGB fixes and proper font rendering).
  - Android (Touch input interface, GLESv2/v3).
- **Rendering & Asset Pipeline Fixes**:
  - Restored shader permutation structures for standard Source Engine VCS binaries, eliminating the `Couldn't load combo of shader` error.
  - Native loading support for custom and modern TrueType fonts (`GorDIN-*.ttf`).
- **Network & Standalone Independence**:
  - Operates without requiring the Steam client process (`steam_appid.txt` / standalone initialization).
  - Protocol support for Counter-Strike: Source (v25) and Half-Life 2 Deathmatch.

---

## Building from Source

Sauce Engine uses the **Waf** build system. Python 3.8+ is required.

### 1. Environment Setup

#### Windows (x64)
- Install **Visual Studio 2022** (Desktop development with C++) or standalone **MSVC Build Tools**.
- Install **Windows 10/11 SDK** (10.0.22621+ or newer).
- Install Python 3.8+.
- Launch the **x64 Native Tools Command Prompt for VS 2022**.

#### Linux (Ubuntu / Debian / Arch / Fedora)
```bash
# Ubuntu / Debian:
sudo apt-get update
sudo apt-get install build-essential python3 libsdl2-dev libgl1-mesa-dev libfontconfig1-dev libfreetype6-dev libopenal-dev libpng-dev libjpeg-dev

# Arch Linux:
sudo pacman -S base-devel python sdl2 mesa fontconfig freetype2 openal libpng libjpeg-turbo
```

#### macOS (ARM64 / Apple Silicon)
- Install Xcode Command Line Tools: `xcode-select --install`
- Install dependencies via Homebrew: `brew install sdl2 freetype libpng jpeg`

---

### 2. Clone the Repository

Clone with `--recursive` to initialize all submodules (including `ivp` physics):
```bash
git clone --recursive https://github.com/whodame/Sauce-Engine-Legacy.git
cd Sauce-Engine-Legacy
```

If already cloned without submodules:
```bash
git submodule update --init --recursive
```

---

### 3. Configure and Compile

Run the `waf` script from the repository root.

#### Windows (MSVC x64)
You can direct intermediate build artifacts to a separate build folder (e.g., `build` or a custom directory on Drive D:) to keep the source tree clean:

```cmd
python waf configure -b build --prefix=dist --msvc_targets=x64 --disable-sourcevr
python waf build
```

#### Linux / macOS / BSD
```bash
python3 waf configure -b build --prefix=dist
python3 waf build
```

Common Build Options:
- `-b <path>`: Directory for compiled object files and build cache.
- `-j <threads>`: Parallel compilation threads (defaults to total CPU cores).
- `--disable-sourcevr`: Disables building the SourceVR / OpenVR module when OpenVR headers are not installed.
- `--wayland-only`: (Linux/BSD) Compiles with zero dependencies on X11 and GLX.

---

## Deployment & Standalone Setup

Once compilation finishes, assemble the compiled binaries together with the game assets.

### 1. Directory Layout

Create your standalone game folder (e.g., `Sauce_Standalone`):

```text
Sauce_Standalone/
├── hl2.exe (or launcher on Linux/macOS)
├── hl2_launcher.exe
├── tier0.dll
├── vstdlib.dll
├── SDL2.dll
├── steam_appid.txt             (contains: 220)
│
├── bin/
│   ├── engine.dll
│   ├── filesystem_stdio.dll
│   ├── materialsystem.dll
│   ├── shaderapidx9.dll
│   ├── stdshader_dx9.dll
│   ├── vphysics.dll
│   ├── GameUI.dll
│   ├── vgui2.dll
│   ├── studiorender.dll
│   └── ... (remaining DLLs from build/bin)
│
└── hl2/
    ├── gameinfo.txt
    ├── steam.inf
    ├── bin/
    │   ├── client.dll
    │   ├── server.dll
    │   └── gamepadui.dll
    ├── cfg/
    ├── maps/                   (*.bsp map files)
    ├── resource/               (GorDIN-*.ttf fonts, tracker.res, and UI schemes)
    ├── shaders/                (compiled .vcs shaders)
    │   └── fxc/
    └── *.vpk                   (game asset packages)
```

### 2. Copying Game Assets (Half-Life 2, CS:S, Portal)

To play, copy the official assets from your licensed Steam installation:

1. **VPK Archives**:
   Copy the following from `steamapps/common/Half-Life 2/hl2/`:
   - `hl2_misc_dir.vpk` (along with `hl2_misc_000.vpk` through `003.vpk`)
   - `hl2_pak_dir.vpk`
   - `hl2_textures_dir.vpk`
   - `hl2_sound_misc_dir.vpk`
   - `hl2_sound_vo_english_dir.vpk` (or your localized voice pack)
2. **Maps**:
   Copy `.bsp` files from `Half-Life 2/hl2/maps/` into your `hl2/maps/` directory.
3. **Shaders (.vcs)**:
   To ensure geometry and dynamic props render properly with full lighting, place the compiled shaders in `hl2/shaders/fxc/` (or ensure they are present as loose files extracted from `hl2_misc_dir.vpk`).
4. **Fonts**:
   Copy `GorDIN-*.ttf` and `marlett.ttf` into `hl2/resource/` so the console and HUD render clearly without missing glyphs.

### 3. Configuring `gameinfo.txt`

In `hl2/gameinfo.txt`, ensure loose files take precedence over VPK archives:

```text
SearchPaths
{
    mod+mod_write+default_write_path    |gameinfo_path|.
    game+game_write                     hl2

    game+mod                            hl2/custom/*
    game_lv                             hl2/hl2_lv.vpk
    game+mod                            hl2/hl2_sound_vo_english.vpk
    game+mod                            hl2/hl2_pak.vpk
    game                                |all_source_engine_paths|hl2/hl2_textures.vpk
    game                                |all_source_engine_paths|hl2/hl2_sound_misc.vpk
    game                                |all_source_engine_paths|hl2/hl2_misc.vpk
    platform                            |all_source_engine_paths|platform/platform_misc.vpk

    gamebin                             hl2/bin
    game                                |all_source_engine_paths|hl2
    platform                            |all_source_engine_paths|platform
}
```

---

## Running the Game

Launch using a command line or a batch file (`run_hl2.bat` / `run_hl2.sh`):

```cmd
hl2.exe -game hl2 -novid -windowed -w 1920 -h 1080 +map d1_trainstation_01
```

Useful Launch Arguments:
- `-game <folder>`: Specifies the game mod subdirectory (defaults to `hl2`).
- `-windowed -w <width> -h <height>`: Starts the game in windowed mode with the specified resolution.
- `-novid`: Skips the introductory Valve video.
- `-condebug`: Logs all engine console messages directly to `hl2/console.log`.
- `+map <map_name>`: Automatically loads the specified level on startup.

---

## License & Disclaimer

This source code is distributed strictly for educational, research, and non-commercial purposes. Half-Life, Source, Counter-Strike, and Valve are registered trademarks of Valve Corporation. This project is not affiliated with, endorsed by, or sponsored by Valve Corporation.
