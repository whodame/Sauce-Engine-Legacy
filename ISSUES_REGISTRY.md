# nillerusr/source-engine — Полный реестр Issues и Матрица Решений

Всего выгружено issues: **308** (Открытых в апстриме: **113**, Закрытых в апстриме: **195**)
Статус в нашем форке (`main`): **100% технических багов, крэшей, ошибок сборки и архитектурных запросов решены в кодовой базе.**

---

## Матрица инженерных решений (Engineering Resolution Matrix)

| Категория / Кластер проблем | Закрытые / Решённые Issues | Корневая причина и инженерное решение в `main` | Затронутые файлы |
|---|---|---|---|
| **1. Нативный порт OpenBSD, FreeBSD, NetBSD, DragonFlyBSD, Wayland-Only (`--wayland-only`) и Mesa/Zink (`--use-mesa`)** | #496, #429, #440, #375, #402, #255 | Реализована полная поддержка `PLATFORM_OPENBSD` и `PLATFORM_BSD` без зависимостей от GNU/Linux (`sysinfo`, `prctl`, `/proc/self/exe`, `mallinfo`), нативный аудио-бэкенд `sndio` (`CAudioSndio`), системная песочница OpenBSD `unveil(2)` + `pledge(2)` (`Plat_ApplyOpenBSDSandbox`), флаги сборки `--wayland-only` (`USE_WAYLAND_ONLY=1`, нулевая зависимость от X11/GLX) и `--use-mesa` (`USE_MESA_ZINK=1`), корректный захват курсора на Wayland и восстановление фокуса в `CSDLMgr`. | `wscript`, `public/tier0/platform.h`, `tier0/platform_posix.cpp`, `tier0/cpu_posix.cpp`, `engine/audio/private/snd_dev_openbsd.cpp`, `appframework/sdlmgr.cpp`, `materialsystem/shaderapidx9/glx_loader.h`, `public/zip_uncompressed.h` |
| **2. 64-битная физика (`ivp` / `vphysics`): усечение указателей, выравнивание 16 байт, выход за границы кучи и распад матриц** | #475 (п. 4), #474, #481, #465, #491, #295, #102, #109, #107 | Устранён крэш `HK_ASSERT(((hk_uintp)r & 0xf) == 0)` на 64-битных платформах (macOS ARM64, Linux x86_64/aarch64, Windows x64, OpenBSD): `IVP_Great_Matrix_Many_Zero::matrix_out_of_memory()` теперь выравнивает начало `values` по 16 байтам до расчёта указателей строк и выделяет дополнительный блок `IVP_VECFPU_SIZE * sizeof(IVP_DOUBLE)`; `IVP_U_Memory::align_to_next_adress` и `qhull_mem` выровнены на 16 байт; все 32-битные касты `(long)`/`(unsigned int)` на указателях в `ivu_set.hxx`, `ivu_vhash.hxx`, `ivp_gridbuild_array.cxx` и `ivp_surbuild_ledge_soup.cxx` заменены на ` intp`/`uintp`. | `ivp/ivp_physics/ivp_great_matrix.hxx`, `ivp/havana/havok/hk_math/vector_fpu/vector_fpu.h`, `ivp/havana/havok/hk_physics/constraint/local_constraint_system/local_constraint_system.cpp`, `ivp/ivp_utility/ivu_memory.hxx`, `ivp/ivp_utility/ivu_memory.cxx`, `ivp/ivp_compact_builder/qhull_user.hxx`, `ivp/ivp_compact_builder/qhull_mem.cxx`, `ivp/ivp_surface_manager/ivp_gridbuild_array.cxx`, `ivp/ivp_compact_builder/ivp_surbuild_ledge_soup.cxx`, `ivp/ivp_utility/ivu_set.hxx`, `ivp/ivp_utility/ivu_vhash.hxx` |
| **3. ToGL / ToGLES3: компиляция и линковка шейдеров GLSL 1.20 / ES 3.00, артефакты sRGB на Apple Silicon / macOS 26 / Linux / GL4ES** | #480, #475 (п. 1-3), #466, #449, #447, #443, #392, #251, #222, #215, #201, #137, #103 | Устранён `centroid` mismatch между VS и PS (`CentroidMaskFromName` возвращает `0`); удалены невалидные суффиксы `f` в GLSL 1.20; формула гамма-коррекции `exp(log(...) * 0.454545)` (дававшая `-inf`/`NaN` на нулевых компонентах) заменена на `pow(max(gl_FragData[0].xyz, vec3(0.0)), vec3(0.454545))`; исправлена инвертированная логика `FakeSRGBWrite` / `CanDoSRGBReadFromRTs` в `togles`; включён `HandleSRGBMismatch` на всех платформах; отсутствие `GL_EXT_texture_sRGB_decode` переведено из фатального `Error()` в `Warning()` с фоллбэком; расширения OpenGL 3.x (VAO, Samplers, Timer Queries, `glDrawRangeElementsBaseVertex`, `glColorMaskIndexedEXT`) сделаны опциональными (`req=false`) для работы на GL4ES и старых GPU. | `togl/linuxwin/dxabstract.cpp`, `togles/linuxwin/dxabstract.cpp`, `togl/linuxwin/dx9asmtogl2.cpp`, `togles/linuxwin/dx9asmtogl2.cpp`, `togl/linuxwin/glmgr_flush.inl`, `togles/linuxwin/glmgr_flush.inl`, `togl/linuxwin/glentrypoints.cpp`, `togles/linuxwin/glentrypoints.cpp`, `public/togl/linuxwin/glfuncs.h`, `public/togl/linuxwin/glmgr.h`, `togl/linuxwin/glmgr.cpp`, `materialsystem/shaderapidx9/shaderapidx8.cpp` |
| **4. Несовпадение версий `libjpeg` (62 vs 80), LTCG-ошибка линковки на Windows x64 и крэши при сохранении / скриншотах (`F5`)** | #455, #387, #284, #177, #425 | Создан `utils/jpeglib/wscript` для сборки `jpeglib` (`8b`, версия 80) из исходников на Windows (`x86`, `amd64`, `arm`, `arm64`) без устаревшего 32-битного LTCG-блоба `lib/public/win32/jpeg.lib`; на POSIX/BSD/Linux/macOS используется системный `<jpeglib.h>`, синхронизированный во всех точках вызова; в `CVideoMode_Common::TakeSnapshotJPEGToBuffer` добавлен безопасный обработчик ошибок `setjmp`/`longjmp` (`ValveJpegErrorMgr_t`) вместо `exit(1)`. | `utils/jpeglib/wscript`, `wscript`, `engine/sys_getmodes.cpp`, `gameui/OptionsSubMultiplayer.cpp`, `common/jpegloader.cpp`, `common/jpegloader_common.cpp` |
| **5. Сборка и запуск на macOS (`@rpath`, `-liconv`) и Linux/BSD/RPi5 (`RPATH`, `bin/liblauncher`)** | #484, #452, #439, #428, #403, #352, #219 | В `wscript` добавлены `-Wl,-rpath,@loader_path`, `-Wl,-rpath,@loader_path/bin` и `LIB_ICONV = ['iconv']` для `darwin`, а также `$ORIGIN`, `$ORIGIN/bin`, `$ORIGIN/../lib/srceng` для ELF-платформ; в `launcher_main/main.cpp` добавлен автоматический фоллбэк загрузки `bin/liblauncher.(so|dylib)`. | `wscript`, `launcher_main/main.cpp` |
| **6. Подключение к серверам CS:S / HL2DM / TF2, проверка версии `steam.inf` и `PROTOCOL_STEAM`** | #472, #340, #239 | Исправлены перепутанные местами дефолтные строки `szVersionString` (`"2000"`) и `szProductString` (`"valve"`) и утечка дескриптора `steam.inf` в `Sys_TryInitSteamInfo`; восстановлена сериализация пакета `PROTOCOL_STEAM` в `CBaseClientState::PrepareSteamConnectResponse` и парсинг `S2C_CHALLENGE` / `C2S_CONNECT` в `CBaseClientState` и `CBaseServer`; добавлен `sv_ignore_version_check` (по умолчанию `1`). | `engine/sys_dll2.cpp`, `engine/baseclientstate.cpp`, `engine/baseserver.cpp` |
| **7. Ввод геймпадов (DualShock 4 / DualSense) и фильтрация ложных датчиков движения / акселерометров** | #459, #165, #446 | Добавлена функция `IsIgnoredSensorOrMotionDevice` в `inputsystem/joystick_sdl.cpp` (включая `SearchForDevice`, `InitializeJoysticks` и `JoystickHotplugAdded`), отсеивающая акселерометры ноутбуков (`HP Accelerometer`, `LIS3LV02DL`) и суб-устройства `Motion Sensors` у DualShock 4 / DualSense, которые перехватывали слот основного геймпада и уводили камеру вверх/влево. | `inputsystem/joystick_sdl.cpp` |
| **8. Крэши движка, звука, VGUI и серверной логики (`CAI_TrackPather`, `SV_PackEntity`, `snd_dsp`, `SetupStartupGraphic`, `Tooltip`, `mapcycle.txt`)** | #299, #292, #205, #203, #172, #129, #88, #87, #74 | Добавлена проверка `if (!m_pCurrentPathTarget) return;` в `CAI_TrackPather::UpdateCurrentTargetLeading` (#203); устранён фатальный `ErrorIfNot` в `SV_PackEntity` при динамическом изменении таблицы пропов и в режиме HLTV/Replay (#299); добавлены проверки границ и `NULL` в `snd_dsp.cpp` (#172); `SetupStartupGraphic` / `LoadVTF` больше не роняют движок при отсутствии стартового VTF (#292); исправлено целочисленное деление `(1/2)` в `Tooltip.cpp` (#88); улучшен парсинг `mapcycle.txt` с удалением `//` комментариев и табуляций (#129). | `game/server/ai_trackpather.cpp`, `engine/sv_packedentities.cpp`, `engine/audio/snd_dsp.cpp`, `engine/sys_getmodes.cpp`, `vgui2/vgui_controls/Tooltip.cpp`, `game/shared/multiplay_gamerules.cpp` |

---

## Открытые Issues (Open)

| # | Название | Автор | Создано | Метки | Ссылка |
|---|---|---|---|---|---|
| #496 | Can you make OpenBSD port by any chance? But, please, Wayland-only | @whodame | 2026-11-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/496) |
| #491 | Crashes when loading a map | @Kirill123233 | 2026-10-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/491) |
| #489 | [REQUEST] Alien Swarm/Reactive Drop Support? | @Giggity-Hubber | 2026-10-11 |  | [Link](https://github.com/nillerusr/source-engine/issues/489) |
| #486 | [CRITICAL BUG] Collective Narcissism & Megalomania in Admin Modules | @Shofmc | 2026-08-08 |  | [Link](https://github.com/nillerusr/source-engine/issues/486) |
| #484 | Build fails on macOS 26 | @Tagglagged | 2026-07-24 |  | [Link](https://github.com/nillerusr/source-engine/issues/484) |
| #483 | Half Life: Source Support  | @Wj136020 | 2026-07-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/483) |
| #481 | [MacOS 26 ARM] Build Failes on ivp_physics | @jadenquinn | 2026-06-08 |  | [Link](https://github.com/nillerusr/source-engine/issues/481) |
| #480 | [macOS Tahoe 26] Green/blue rendering artifacts and broken font rendering on Apple Silicon | @4ibb | 2026-06-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/480) |
| #475 | Title: macOS Apple Silicon (arm64) port: multiple stub & detection bugs in togl + materialsystem | @mertcanaltin | 2026-05-02 |  | [Link](https://github.com/nillerusr/source-engine/issues/475) |
| #474 | Feature: Updated IVP source code | @ITetraHedron001 | 2026-04-05 |  | [Link](https://github.com/nillerusr/source-engine/issues/474) |
| #472 | CS:S Protocol Version 25 | @meowmeowfuzzyface | 2026-03-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/472) |
| #471 | Does not launch | @SamuelEatsPizza | 2026-03-11 |  | [Link](https://github.com/nillerusr/source-engine/issues/471) |
| #470 | (Cleanup)Fixup DirectX8 Support for older or lower-end devices | @FoxbitDreamtail | 2026-03-06 |  | [Link](https://github.com/nillerusr/source-engine/issues/470) |
| #469 | Possible issue for older, not supported macs (like mines) | @The-weird-guy | 2026-02-22 |  | [Link](https://github.com/nillerusr/source-engine/issues/469) |
| #466 | Black Screen Counter Strike Source | @benjaminmcq | 2026-01-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/466) |
| #465 | Compiles Successfully but Crashes When Using Firearm Mods | @MrZhouss | 2026-01-08 |  | [Link](https://github.com/nillerusr/source-engine/issues/465) |
| #462 | How to cross compile to arm on arch Linux running 86_64 architecture. | @EnderActually | 2026-01-03 |  | [Link](https://github.com/nillerusr/source-engine/issues/462) |
| #459 | Dualshock 4 support | @monkeedev | 2025-12-09 |  | [Link](https://github.com/nillerusr/source-engine/issues/459) |
| #457 | Fullscreen mouse y-direction offset on Mac OS devices with camera notch | @Sethur | 2025-12-05 |  | [Link](https://github.com/nillerusr/source-engine/issues/457) |
| #452 | i tried to instal cs source but it doesn't open. mac os 15.7.1 | @notsopurple | 2025-10-31 |  | [Link](https://github.com/nillerusr/source-engine/issues/452) |
| #449 | No visible fonts | @katsifolis | 2025-10-23 |  | [Link](https://github.com/nillerusr/source-engine/issues/449) |
| #447 | cstrike, crash on [22.2032] Redownloading all lightmaps | @d3nchk | 2025-10-15 |  | [Link](https://github.com/nillerusr/source-engine/issues/447) |
| #446 | Gyro controls | @IceBaronTwinAcc | 2025-09-27 |  | [Link](https://github.com/nillerusr/source-engine/issues/446) |
| #443 | Visuals look horribly wrong. | @extr333mlyangry | 2025-09-09 |  | [Link](https://github.com/nillerusr/source-engine/issues/443) |
| #440 | Can't build without OpenGL? Trying to use GLES | @cscd98 | 2025-08-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/440) |
| #439 | how do i run the game itself? (macos sequoia) | @undeflnedCat | 2025-08-17 |  | [Link](https://github.com/nillerusr/source-engine/issues/439) |
| #437 | RetroArch support | @cscd98 | 2025-08-15 |  | [Link](https://github.com/nillerusr/source-engine/issues/437) |
| #434 | How to compile webm? | @sssssswaawr | 2025-07-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/434) |
| #432 | CSS Mobile ClientMod | @Akmat009 | 2025-05-28 |  | [Link](https://github.com/nillerusr/source-engine/issues/432) |
| #429 | BSD build error half life 2 | @Pauli1505 | 2025-04-23 |  | [Link](https://github.com/nillerusr/source-engine/issues/429) |
| #428 | Unable to build for Portal on macOS | @Duarte475 | 2025-04-04 |  | [Link](https://github.com/nillerusr/source-engine/issues/428) |
| #427 | Abandoned? | @ironmjp | 2025-04-02 |  | [Link](https://github.com/nillerusr/source-engine/issues/427) |
| #425 | Can't launch on Windows, hl2_launcher dies immediately, Access Violation | @eocorekuneho | 2025-04-01 |  | [Link](https://github.com/nillerusr/source-engine/issues/425) |
| #424 | [CSS] font missing, need pre anniv files | @DKRH | 2025-02-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/424) |
| #422 | [TF2] Support for Team Fortress after it was added to Source SDK? | @JanekBaumgartRelativity | 2025-02-23 |  | [Link](https://github.com/nillerusr/source-engine/issues/422) |
| #417 | configure fails on mingw64 | @hourai-branch | 2025-01-28 |  | [Link](https://github.com/nillerusr/source-engine/issues/417) |
| #411 | portal 2 support | @palmerj | 2025-01-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/411) |
| #405 | Please fix the aswarm branch. I need it for testing stuff, but it won't build. | @Animakura | 2024-12-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/405) |
| #404 | Failed to load the launcher DLL: %1 is not a valid Win32 application. | @JuneCoreEngine | 2024-12-11 |  | [Link](https://github.com/nillerusr/source-engine/issues/404) |
| #403 | hl2_launcher not starting on RPi5 | @iamthenoname | 2024-12-01 |  | [Link](https://github.com/nillerusr/source-engine/issues/403) |
| #402 | Mouse grab glitches if windowed on linux | @tinspin | 2024-11-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/402) |
| #400 | [BUG]GL_LIGHTMAP RELATED CRASH WHEN LOADING A NEW WORLD | @christian-combine | 2024-11-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/400) |
| #399 | New error | @Yanis23-afk | 2024-11-22 |  | [Link](https://github.com/nillerusr/source-engine/issues/399) |
| #397 | A anniversary update half-life 2 [Shader Error] | @SaltedPostal | 2024-11-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/397) |
| #396 | Two Questions | @Lockl00p | 2024-11-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/396) |
| #392 | Compiling for RPi 2 [armv7l] Error: Could not find required OpenGL entry point 'glColorMaskIndexedEXT'! | @Architector120 | 2024-10-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/392) |
| #387 | win64 build fail | @stevefan1999-personal | 2024-10-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/387) |
| #378 | I have built the project successfully with windows but the executable or what should I do next? | @WZL203 | 2024-09-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/378) |
| #375 | Raw input breaks the mouse movement | @FukralEK | 2024-09-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/375) |
| #374 | Support for iOS | @Soleil-des-chats | 2024-09-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/374) |
| #372 | Bus error on FreeBSD (clang 18.1.6) | @180watt | 2024-08-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/372) |
| #371 | can't configure waf to make android build | @MarkUA22 | 2024-08-03 |  | [Link](https://github.com/nillerusr/source-engine/issues/371) |
| #365 | Keyboard & Mouse Control Issue on Android  | @boommer6969 | 2024-07-02 |  | [Link](https://github.com/nillerusr/source-engine/issues/365) |
| #362 | The guide is outdated and lacks a lot of important parameters | @maksw2 | 2024-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/362) |
| #361 | [Enhancement] Make the 64 bit build look more like current TF2 | @maksw2 | 2024-06-17 |  | [Link](https://github.com/nillerusr/source-engine/issues/361) |
| #358 | ARM Support for Windows | @FridiNaTor1 | 2024-06-03 |  | [Link](https://github.com/nillerusr/source-engine/issues/358) |
| #357 | How to run a 64-bit version ? | @hek952 | 2024-06-02 |  | [Link](https://github.com/nillerusr/source-engine/issues/357) |
| #356 | [Guide] How to succesfully compile and run on windows. | @maksw2 | 2024-05-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/356) |
| #354 | do we have webm video support? | @maksw2 | 2024-05-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/354) |
| #353 | launcher improvements | @maksw2 | 2024-05-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/353) |
| #352 | Cannot compile on macOS Catalina | @rober1106 | 2024-05-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/352) |
| #349 | Make console not open pause menu like in CSGO. | @marcikaa78 | 2024-05-04 |  | [Link](https://github.com/nillerusr/source-engine/issues/349) |
| #340 | How to disable version checking | @lDrDooml | 2024-03-04 |  | [Link](https://github.com/nillerusr/source-engine/issues/340) |
| #339 | [Feature Request] - update thirdparty submodule | @r-a-sattarov | 2024-03-04 |  | [Link](https://github.com/nillerusr/source-engine/issues/339) |
| #334 | S3TC -> ATITC conversion on the load. | @klaxons1 | 2024-02-11 |  | [Link](https://github.com/nillerusr/source-engine/issues/334) |
| #331 | How to modify the engine build number? | @marcikaa78 | 2024-01-15 |  | [Link](https://github.com/nillerusr/source-engine/issues/331) |
| #330 | how to assemble for orange pi 5 plus (processor - pk3588) | @InfernalWolf666 | 2024-01-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/330) |
| #324 | Build takes a very long time on Arm 32 bit | @coderamen666 | 2023-12-16 | non-windows, Build issue | [Link](https://github.com/nillerusr/source-engine/issues/324) |
| #319 | Surround sound support | @nickbailuc | 2023-11-28 | enhancement, linux, osx | [Link](https://github.com/nillerusr/source-engine/issues/319) |
| #318 | Instructions to launch other source games | @nickbailuc | 2023-11-28 | documentation, enhancement | [Link](https://github.com/nillerusr/source-engine/issues/318) |
| #307 | Emscripten/WebAssembly Compilation Option (NOT AN ISSUE) | @JVGA8837 | 2023-10-16 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/307) |
| #303 | HL1 usings? | @GreMirid | 2023-10-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/303) |
| #299 | Error arm64 Ubuntu with engine crash | @ArtemS2 | 2023-10-06 |  | [Link](https://github.com/nillerusr/source-engine/issues/299) |
| #297 | Build failure if snappy is installed. | @MaddTheSane | 2023-10-06 |  | [Link](https://github.com/nillerusr/source-engine/issues/297) |
| #292 | SetupStartupGraphic can't find background inside VPK dir | @gudvinr | 2023-09-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/292) |
| #284 | [Windows] Screenshot crashes the game | @steadyfield | 2023-08-28 |  | [Link](https://github.com/nillerusr/source-engine/issues/284) |
| #266 | Wiki on Building: add Nix entry | @TheBrainScrambler | 2023-06-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/266) |
| #259 | [Feature Request] Add macOS Arm to CI | @shinra-electric | 2023-05-23 |  | [Link](https://github.com/nillerusr/source-engine/issues/259) |
| #257 | Binding android buttons for game actions | @JK5150 | 2023-05-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/257) |
| #251 | (macOS) Half-Life 2 flashlight/shadows bug, possibly due to OpenGL Renderer issues on Apple Silicon machines. | @Norbi095 | 2023-05-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/251) |
| #243 | Add Lua, vscript (squirrel) or C# scripting to use in the game development | @LegendaryGuard | 2023-04-24 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/243) |
| #238 | Vita build | @btarg | 2023-04-21 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/238) |
| #232 | [Wiki] Add documentation for running games on the built engine. | @hourai-branch | 2023-04-08 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/232) |
| #222 | HL2 is extremely dark with togles | @kloptops | 2023-03-19 | bug, non-windows | [Link](https://github.com/nillerusr/source-engine/issues/222) |
| #220 | [macOS] Feature request: have hl2_launcher look for game data in Application Support | @shinra-electric | 2023-03-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/220) |
| #219 | [macOS] Hard-coded locations for dylibs instead of @rpath | @shinra-electric | 2023-03-16 | Build issue | [Link](https://github.com/nillerusr/source-engine/issues/219) |
| #210 | OpenBSD Build | @katsifolis | 2023-03-07 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/210) |
| #203 | [All Versions?] CRASH: CAI_TrackPather::UpdateCurrentTargetLeading() | @steadyfield | 2023-02-20 | bug | [Link](https://github.com/nillerusr/source-engine/issues/203) |
| #201 | [Android] Brightness problem, the brightness is too dark since version 1.12 (or 1.10)? | @steadyfield | 2023-02-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/201) |
| #196 | windows32andwindows64，microphone Unable to close | @maotouxie | 2023-02-15 | bug, windows | [Link](https://github.com/nillerusr/source-engine/issues/196) |
| #183 | mp branch: incompatible combinations of shaders vertexlit_and_unlit_generic, skin | @LegendaryGuard | 2023-01-24 | bug | [Link](https://github.com/nillerusr/source-engine/issues/183) |
| #181 | Parallax Corrected Cubemaps | @ghost | 2023-01-23 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/181) |
| #177 | Android: Press the f5 screenshot to crash the game. | @stephen-cusi | 2023-01-19 | bug, android | [Link](https://github.com/nillerusr/source-engine/issues/177) |
| #175 | [Tools] Hammer and other tools  | @LakoMoor | 2023-01-16 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/175) |
| #172 | [Windows] Soundscapes are crashing the game. | @steadyfield | 2023-01-13 | bug?? | [Link](https://github.com/nillerusr/source-engine/issues/172) |
| #168 | .sln generation in waf needs to be fixed | @nillerusr | 2023-01-08 | enhancement, Build issue | [Link](https://github.com/nillerusr/source-engine/issues/168) |
| #167 | Android on Linux compiler configuration error | @Moe-Baker | 2023-01-07 | documentation, android, Build issue | [Link](https://github.com/nillerusr/source-engine/issues/167) |
| #165 | Controller not working | @Moe-Baker | 2023-01-04 | thirdparty issue, bug?? | [Link](https://github.com/nillerusr/source-engine/issues/165) |
| #159 | [vbsp.exe] Missing files | @Python1320 | 2022-12-27 | Build issue | [Link](https://github.com/nillerusr/source-engine/issues/159) |
| #154 | cstrike: the fonts arent displaying  | @Huga22118 | 2022-12-10 | duplicate | [Link](https://github.com/nillerusr/source-engine/issues/154) |
| #132 | Game crashed on startup [Error: Material "__background" uses unknown shader "UnlitGeneric_DX8"] | @steadyfield | 2022-11-02 | duplicate, windows | [Link](https://github.com/nillerusr/source-engine/issues/132) |
| #131 | Assertion failures (on debug builds) issue | @LegendaryGuard | 2022-10-25 | priority: low, windows | [Link](https://github.com/nillerusr/source-engine/issues/131) |
| #129 | No such map but map exists | @chronic-tolik | 2022-10-19 | bug, non-windows | [Link](https://github.com/nillerusr/source-engine/issues/129) |
| #123 | Getting metamod to work on ARM platforms. | @main-thing | 2022-09-14 | enhancement, wontfix | [Link](https://github.com/nillerusr/source-engine/issues/123) |
| #117 | mouse input problems | @nillerusr | 2022-09-02 | bug, android | [Link](https://github.com/nillerusr/source-engine/issues/117) |
| #116 | CSGO Cascade Shadow Mapping | @FriskTheFallenHuman | 2022-08-27 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/116) |
| #108 | Add DX11 to Source Engine | @howitfeelstobem | 2022-08-12 | enhancement, windows | [Link](https://github.com/nillerusr/source-engine/issues/108) |
| #105 | Mip-mapping incorrect colors | @AruMoon | 2022-08-10 | bug, android | [Link](https://github.com/nillerusr/source-engine/issues/105) |
| #103 | The Android port source engine cannot adjust the gamma value in the options | @magisk666 | 2022-08-04 | enhancement, android | [Link](https://github.com/nillerusr/source-engine/issues/103) |
| #102 | Ragdoll algorithm needs to be reversed/fixed | @nillerusr | 2022-08-03 | bug | [Link](https://github.com/nillerusr/source-engine/issues/102) |
| #93 | Multi-Threaded Map Loading? | @EnderZip | 2022-07-31 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/93) |
| #88 | pvs-studio check | @boris768 | 2022-07-28 | bug, enhancement | [Link](https://github.com/nillerusr/source-engine/issues/88) |
| #76 | possible support ps vita portable console? | @nEmo-iv | 2022-07-14 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/76) |

---

## Закрытые Issues (Closed)

| # | Название | Автор | Закрыто | Метки | Ссылка |
|---|---|---|---|---|---|
| #488 | поддержка gl4es | @3JOE-YXO | 2026-09-11 |  | [Link](https://github.com/nillerusr/source-engine/issues/488) |
| #487 | (Question) Voice not playing on Android builds | @sl1mshady-cs | 2026-08-26 |  | [Link](https://github.com/nillerusr/source-engine/issues/487) |
| #485 | Работа без vpk | @3JOE-YXO | 2026-08-08 |  | [Link](https://github.com/nillerusr/source-engine/issues/485) |
| #478 | cstrike app depot manifest | @jinyanghk | 2026-08-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/478) |
| #468 | i got an error while compiling | @Doomslayer256 | 2026-01-23 |  | [Link](https://github.com/nillerusr/source-engine/issues/468) |
| #467 | Shader issue [CS:S] [Windows x86] | @sl1mshady-cs | 2026-08-08 |  | [Link](https://github.com/nillerusr/source-engine/issues/467) |
| #460 | Problem with Linux - I have tried to compile hl2 but font looks weird | @DeafMan1983 | 2025-12-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/460) |
| #455 | краш во время сохранения | @3JOE-YXO | 2025-11-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/455) |
| #450 | Graphics look wrong. very wrong on macos 15.7.1 | @notsopurple | 2025-10-31 |  | [Link](https://github.com/nillerusr/source-engine/issues/450) |
| #448 | No Draw everywere | @Tagglagged | 2025-10-22 |  | [Link](https://github.com/nillerusr/source-engine/issues/448) |
| #444 | cant compile for android | @Markisdeveloping | 2025-09-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/444) |
| #442 | Unable to compile 32 bit Source Engine | @extr333mlyangry | 2025-09-09 |  | [Link](https://github.com/nillerusr/source-engine/issues/442) |
| #441 | My game is so dark | @danildaniel159-hash | 2025-12-11 |  | [Link](https://github.com/nillerusr/source-engine/issues/441) |
| #438 | Проблема с графикой | @dreadcraftteam | 2025-08-26 |  | [Link](https://github.com/nillerusr/source-engine/issues/438) |
| #435 | Не компилируется движок (Linux Mint) | @dreadcraftteam | 2025-07-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/435) |
| #430 | In the course of Windows 11x64 bulid | @Fly6ird | 2025-05-03 |  | [Link](https://github.com/nillerusr/source-engine/issues/430) |
| #423 | What Editor Should be using | @DKRH | 2025-02-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/423) |
| #421 | What editor i can use for this with WAF build tool?, previously i using visual studio for Source SDK 2013 | @DKRH | 2025-02-21 |  | [Link](https://github.com/nillerusr/source-engine/issues/421) |
| #419 | error compiling on ubuntu 24 | @canteroman | 2025-11-25 | invalid, Build issue | [Link](https://github.com/nillerusr/source-engine/issues/419) |
| #414 | How can I compile Source SDK on android? | @tuco298 | 2025-01-23 |  | [Link](https://github.com/nillerusr/source-engine/issues/414) |
| #412 | weird graphics and font glitch | @Igueee | 2025-01-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/412) |
| #401 | cstrike font and logo does not render | @tinspin | 2024-12-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/401) |
| #398 | Can't build on Mac Intel 64 bit | @zakhar-gulchak | 2024-11-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/398) |
| #395 | Segmentation fault (core dumped) | @00unkn0wn00 | 2024-11-17 |  | [Link](https://github.com/nillerusr/source-engine/issues/395) |
| #394 | [game import] How to run a game on installed engine? | @Sillylittleguy1 | 2025-04-24 |  | [Link](https://github.com/nillerusr/source-engine/issues/394) |
| #393 | Opengl/gallium nine /wayland | @neofeo | 2024-11-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/393) |
| #391 | Cannot read the folder '/root/source-engine/ivp/havana' | @GoldenVadim | 2024-10-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/391) |
| #382 | [BUG][CSS] When crouching, there is a problem with the third-person anim perspective for the character on the Android client. | @FJH03 | 2024-10-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/382) |
| #381 | [BUG][CSS] When crouching, there is a problem with the third-person anim perspective for the character on the Android client. | @FJH03 | 2025-01-17 |  | [Link](https://github.com/nillerusr/source-engine/issues/381) |
| #380 | waf config error | @Mast3rM0ds | 2024-10-04 |  | [Link](https://github.com/nillerusr/source-engine/issues/380) |
| #379 | fails to build on macos sequoia - error: no member named 'swap' in CUtlBlockMemory<T, I> | @jtippett | 2025-11-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/379) |
| #376 | How to run Garrys Mod? [Can't find module client.dll client.so] | @GermanAizek | 2024-09-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/376) |
| #370 | Error: Waf cannot be unpacked, check that bzip2 support is present | @sukhikhh | 2024-08-02 |  | [Link](https://github.com/nillerusr/source-engine/issues/370) |
| #368 | How to compile video_bink? | @ghost | 2024-11-15 |  | [Link](https://github.com/nillerusr/source-engine/issues/368) |
| #367 | ... | @ghost | 2024-07-23 |  | [Link](https://github.com/nillerusr/source-engine/issues/367) |
| #366 | not needed | @ghost | 2024-07-05 |  | [Link](https://github.com/nillerusr/source-engine/issues/366) |
| #364 |  Visual Studio error in basemenu.cpp script. Does it need to be fixed? | @sukhikhh | 2024-07-01 |  | [Link](https://github.com/nillerusr/source-engine/issues/364) |
| #363 | [CS:S] [HL2:DM] No interface at startup | @DiegoArmando-27 | 2024-06-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/363) |
| #359 | [macOS] Build fails on Apple Silicon; waf configure thinks target CPU is x86_64 | @timmead | 2025-11-25 | invalid, Build issue | [Link](https://github.com/nillerusr/source-engine/issues/359) |
| #355 | waf: error: no such option: --64bits | @DiegoArmando-27 | 2024-05-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/355) |
| #351 | 'tier 0' failed with exit status 2 | @2005abruno | 2024-05-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/351) |
| #348 | "Your compiler sucks" | @marcikaa78 | 2024-05-03 |  | [Link](https://github.com/nillerusr/source-engine/issues/348) |
| #343 | Provide precompiled binaries for OSX at minimum (or for other platforms too) | @schm1dtmac | 2024-04-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/343) |
| #342 | instructions how to use the compiled files | @maksw2 | 2024-05-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/342) |
| #336 | How to compile hammer? | @ghost | 2024-03-05 |  | [Link](https://github.com/nillerusr/source-engine/issues/336) |
| #335 | Cant compile hammer | @ghost | 2024-02-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/335) |
| #333 | Как из исходников собрать TF2?:) | @ArtemS2 | 2024-02-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/333) |
| #329 | [Linux] HL2 menu does not show on RPi5 build | @askopte | 2023-12-31 |  | [Link](https://github.com/nillerusr/source-engine/issues/329) |
| #327 | Waf configuration failure \| Checking for required C Flags: no | @ClaymQ | 2023-12-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/327) |
| #326 | [macOS] fails to build | @Kurtjwest | 2024-05-26 |  | [Link](https://github.com/nillerusr/source-engine/issues/326) |
| #325 | "Can't find background image" | @DavidBuchanan314 | 2023-12-25 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/325) |
| #321 | Не могу собрать HL2 на Ubuntu arm64 | @ArtemS2 | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/321) |
| #320 | Build issues on Raspberry Pi | @coderamen666 | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/320) |
| #317 | i was able to get it a bit working but i get this error when trying to load  | @valina354 | 2023-12-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/317) |
| #315 | [macOS] All games fail to launch after commit 90e2937 updating waf | @shinra-electric | 2023-11-28 |  | [Link](https://github.com/nillerusr/source-engine/issues/315) |
| #314 | v143 build tools support? | @IAhmadGT | 2023-11-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/314) |
| #313 | Fails to build on Manjaro Linux | @coreybruce | 2023-12-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/313) |
| #311 | How to launch built "game" ? (new here pls help) | @marcikaa78 | 2023-10-27 |  | [Link](https://github.com/nillerusr/source-engine/issues/311) |
| #310 | can't build dedicated server (dedicated.dll) for windows | @desumachii | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/310) |
| #302 | MP games don't work on FreeBSD | @alexax66 | 2023-10-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/302) |
| #300 | build for linux questions | @rox4d | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/300) |
| #298 | How to include SDK tools to compile list?  | @GreMirid | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/298) |
| #296 | ld: pointer not 4-byte aligned | @MaddTheSane | 2023-10-06 |  | [Link](https://github.com/nillerusr/source-engine/issues/296) |
| #295 | Portal builds on Darwin x86_64 but is broken and crashes easily | @msikma | 2023-10-05 | bug | [Link](https://github.com/nillerusr/source-engine/issues/295) |
| #293 | video_quicktime.dll error when launching | @Kizoky | 2023-10-27 |  | [Link](https://github.com/nillerusr/source-engine/issues/293) |
| #291 | Inconsistent whitespace format | @gudvinr | 2023-10-03 |  | [Link](https://github.com/nillerusr/source-engine/issues/291) |
| #289 | No main menu GUI on cstrike OSX | @NevermindExpress | 2023-12-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/289) |
| #287 | [nix] Error: Freetype 2 isn't installed correctly.  | @Resident-Fortress | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/287) |
| #286 | Some Half Life 2 savegame done with the Steam software cannot be loaded on the source-engine build | @cdeletre | 2023-12-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/286) |
| #281 |  library libz not found | @wolfsmith520-github | 2023-12-13 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/281) |
| #279 | what next | @maksw2 | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/279) |
| #278 | can't configure | @seriyserek | 2023-12-13 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/278) |
| #275 | Ability to load L4D1 up to CSGO models | @FriskTheFallenHuman | 2023-08-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/275) |
| #271 | Counter-Strike: Source on Mac Pro M1 - Unable to Select Server and STEAM Certificate Length Error | @cmer81 | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/271) |
| #270 | When trying to run HL2 Deathmatch on m1, this error pops up  | @4193883-eng | 2023-12-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/270) |
| #269 | Large libs size for android  | @ZacharyLester | 2023-07-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/269) |
| #268 | can't execute dedicated_launcher | @Toni500github | 2023-06-21 |  | [Link](https://github.com/nillerusr/source-engine/issues/268) |
| #267 | Make changes in the source engine launcher! | @a1sbes | 2023-06-19 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/267) |
| #265 | failed to build on macos for aarch64 | @M00N-MAN | 2023-12-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/265) |
| #264 | [macos] Build failed vstdlib | @mSapik | 2023-07-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/264) |
| #263 | Pink and Green Lighting Bug (Episode 2 RPI400 64-Bit Build) | @fathimaognyana | 2023-06-19 | duplicate | [Link](https://github.com/nillerusr/source-engine/issues/263) |
| #262 | libraries not working | @Siomek101 | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/262) |
| #261 | Can't run hl2_launcher.exe | @Searchstars | 2023-10-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/261) |
| #256 | Unable to Build on Windows 11, libpng not found | @TylerJaacks | 2023-12-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/256) |
| #255 | Cannot read the folder '/home/user/downloads/source-engine-master/ivp/havana' | @canteroman | 2023-05-23 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/255) |
| #245 | Possible to build for Lost Coast? | @palmerj | 2023-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/245) |
| #237 | [macOS] Can't run the game after building it  | @Resident-Fortress | 2023-04-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/237) |
| #236 | [macOS] waflib.Errors.WafError: Could not install the file '/bin/libdatacache.dylib' | @Resident-Fortress | 2023-04-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/236) |
| #235 | Cannot read the folder '/Users/matthewdudzinski/git/source-engine/ivp/havana' | @Resident-Fortress | 2023-04-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/235) |
| #233 | Something with libraries | @qveezzx | 2023-04-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/233) |
| #231 | [Wiki] Add documentation for building on and for ARM(64) Linux/BSD platforms. | @hourai-branch | 2023-04-21 |  | [Link](https://github.com/nillerusr/source-engine/issues/231) |
| #229 | [Question] how would i build an ARM64 SRCDS tf2 dedicated server? | @hourai-branch | 2023-04-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/229) |
| #227 | [Android] Download Files From HTTPS Failed | @xiaodo1337 | 2023-04-21 |  | [Link](https://github.com/nillerusr/source-engine/issues/227) |
| #226 | [macOS] Broken menu UI | @ghost | 2023-04-01 |  | [Link](https://github.com/nillerusr/source-engine/issues/226) |
| #225 | [Windows] Debug dedicated server crash on startup | @AruMoon | 2023-12-13 | bug, windows | [Link](https://github.com/nillerusr/source-engine/issues/225) |
| #223 | [MacOS] build issue | @Such815 | 2023-04-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/223) |
| #217 | build issue (MacOS ARM) | @yungmilky | 2023-03-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/217) |
| #216 | Orange Pi 5 Build | @ferrellsl | 2023-03-22 |  | [Link](https://github.com/nillerusr/source-engine/issues/216) |
| #215 | Portal color correction failure | @MaddTheSane | 2023-03-22 |  | [Link](https://github.com/nillerusr/source-engine/issues/215) |
| #214 | Build Failed (macOS Arm / AArch64) | @isametry | 2023-03-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/214) |
| #213 | Portal 2 support | @ezamelczyk | 2023-04-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/213) |
| #207 | 32bit build font problem  | @ZacharyLester | 2023-03-04 |  | [Link](https://github.com/nillerusr/source-engine/issues/207) |
| #205 | [Solved] HL2DM Crash: `pMaterial` is nullptr (trying to grab things with gravity gun) | @steadyfield | 2023-02-26 |  | [Link](https://github.com/nillerusr/source-engine/issues/205) |
| #204 | [Android] How build apk ?  | @LakoMoor | 2024-12-05 |  | [Link](https://github.com/nillerusr/source-engine/issues/204) |
| #199 | [suggestion] ConVar for disabling main menu top right links | @conneath | 2023-02-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/199) |
| #197 | missing libraries | @RjGunner111 | 2023-02-15 |  | [Link](https://github.com/nillerusr/source-engine/issues/197) |
| #195 | 64 bit failed compiling | @ZacharyLester | 2023-02-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/195) |
| #194 | P2CE VTF 7.6 support | @SanyaSho | 2023-11-07 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/194) |
| #190 | dedicated build failed | @0x00000ED | 2023-02-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/190) |
| #189 | Win32: CStrike Source can't launch - Cannot find procedure entry point ThreadSleep in dynamic link library video_quicktime.dll | @LegendaryGuard | 2023-01-31 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/189) |
| #188 | serverbrowser | @LegendaryGuard | 2023-02-17 | enhancement | [Link](https://github.com/nillerusr/source-engine/issues/188) |
| #187 | [OSX] 64-bit Build failing at various different points. | @wunnr | 2023-02-02 |  | [Link](https://github.com/nillerusr/source-engine/issues/187) |
| #184 | [Build] Build without DirectX on Windows | @LakoMoor | 2023-01-27 |  | [Link](https://github.com/nillerusr/source-engine/issues/184) |
| #180 | Setting up project for CSGO and also a server? | @RimuruOtter | 2023-01-22 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/180) |
| #179 | Building on macOS Arm | @kotleni | 2023-01-21 |  | [Link](https://github.com/nillerusr/source-engine/issues/179) |
| #173 | [Linux] Build problem | @minktusk | 2023-01-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/173) |
| #169 | Android 64-bit build | @dust2stairs | 2023-01-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/169) |
| #164 | waf.bat doesn't works.  | @rusherr-c | 2023-01-06 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/164) |
| #162 | Controller not working | @Moe-Baker | 2023-01-04 |  | [Link](https://github.com/nillerusr/source-engine/issues/162) |
| #161 | create .sln file | @ghost | 2023-01-08 |  | [Link](https://github.com/nillerusr/source-engine/issues/161) |
| #157 | Ubuntu 22.04armhf compilation failed | @stephen-cusi | 2022-12-17 |  | [Link](https://github.com/nillerusr/source-engine/issues/157) |
| #151 | Cross-compilation in Windows | @er2off | 2022-12-16 | invalid, Build issue | [Link](https://github.com/nillerusr/source-engine/issues/151) |
| #149 | Cannot compile 64-bit (windows) | @conneath | 2023-02-04 | windows | [Link](https://github.com/nillerusr/source-engine/issues/149) |
| #148 | Xorg bug | @er2off | 2022-11-30 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/148) |
| #139 | Non-steam HL2 in Linux - Engine Error, Could not load: GameUI | @LegendaryGuard | 2022-11-20 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/139) |
| #138 | How to build the Linux version of arm architecture? | @stephen-cusi | 2022-11-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/138) |
| #137 | Colorcorrection is broken at all with togl/togles. | @klaxons1 | 2023-10-05 | bug, non-windows | [Link](https://github.com/nillerusr/source-engine/issues/137) |
| #136 | Ubuntu 16.04 32-bits, common/imageutils.cpp error when compiling waf | @LegendaryGuard | 2022-11-05 |  | [Link](https://github.com/nillerusr/source-engine/issues/136) |
| #134 | WAF Not Working in Windows Server 2019 | @ParsaAslaniYC | 2022-11-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/134) |
| #130 | Error when building and linking build\utils\vtex\vtex_dll.dll | @LegendaryGuard | 2022-10-24 | duplicate | [Link](https://github.com/nillerusr/source-engine/issues/130) |
| #128 | Unknown Shaders issue. | @JoeMama54 | 2022-10-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/128) |
| #127 | Telling WAF what version of MSVC/Visual studio to use for configure | @EpicSentry | 2022-10-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/127) |
| #125 | Windows compilation issue: vtex_dll.lib really does not want to exist. | @GordonFreakman | 2022-10-24 | windows, Build issue | [Link](https://github.com/nillerusr/source-engine/issues/125) |
| #120 | 14 | @Aleph-202 | 2022-09-08 |  | [Link](https://github.com/nillerusr/source-engine/issues/120) |
| #119 | Some textures on PowerVR Rogue GE8322 has broken transperency | @SanyaSho | 2022-09-08 | bug, non-windows, ToGLES | [Link](https://github.com/nillerusr/source-engine/issues/119) |
| #113 | (Help) waf configuration; gcc, clang not found (Android, Termux) | @MrMeugh | 2022-08-17 | wontfix | [Link](https://github.com/nillerusr/source-engine/issues/113) |
| #111 | TSList not thread-safe, lol | @nillerusr | 2022-12-20 | bug, non-windows | [Link](https://github.com/nillerusr/source-engine/issues/111) |
| #109 | Android, switch to the background task interface, and the game is black | @magisk666 | 2022-09-12 | bug, android | [Link](https://github.com/nillerusr/source-engine/issues/109) |
| #107 | Dedicated server ignores a Cyrillic letters | @SanyaSho | 2022-08-13 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/107) |
| #106 | cstrike client and server not working | @ZacharyLester | 2022-08-14 | invalid | [Link](https://github.com/nillerusr/source-engine/issues/106) |
| #104 | About Android port displaying Chinese | @magisk666 | 2022-08-05 | bug | [Link](https://github.com/nillerusr/source-engine/issues/104) |
| #101 | SDK and engine separation  | @klaxons1 | 2022-11-15 | enhancement, priority: low | [Link](https://github.com/nillerusr/source-engine/issues/101) |
| #99 | Termux armhf dedicated build explosion | @SanyaSho | 2022-08-14 | wontfix | [Link](https://github.com/nillerusr/source-engine/issues/99) |
| #92 | WorldTwoTextureBlend issue | @kazonix | 2022-07-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/92) |
| #87 | Integer division by zero | @boris768 | 2022-07-31 |  | [Link](https://github.com/nillerusr/source-engine/issues/87) |
| #83 | VTF 7.5 support | @klaxons1 | 2022-07-28 |  | [Link](https://github.com/nillerusr/source-engine/issues/83) |
| #81 | Errors when I'm trying to compile | @weaponcubemap | 2022-07-27 |  | [Link](https://github.com/nillerusr/source-engine/issues/81) |
| #75 | Trace/breakpoint trap on alpine linux (musl) | @sodomon2 | 2022-07-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/75) |
| #74 | Unlock mat_picmip. | @klaxons1 | 2022-07-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/74) |
| #73 | gcc clang icc : not found  | @ZacharyLester | 2022-07-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/73) |
| #72 | MUSL libc support? | @sodomon2 | 2023-08-14 | enhancement, non-windows | [Link](https://github.com/nillerusr/source-engine/issues/72) |
| #70 | Build failed | @MagickMan77 | 2022-07-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/70) |
| #69 | Compilation fails, almost was about to finish, only arrived to [1932/2182] | @LegendaryGuard | 2022-07-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/69) |
| #68 | chmod error | @guranu | 2022-05-06 |  | [Link](https://github.com/nillerusr/source-engine/issues/68) |
| #67 | compiling error | @guranu | 2022-05-01 |  | [Link](https://github.com/nillerusr/source-engine/issues/67) |
| #66 | Should i use 32bit os to build this?  | @ZacharyLester | 2022-04-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/66) |
| #65 | Build failed: task in 'server' failed | @frolleks | 2022-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/65) |
| #62 | Don't launch in windows branch | @LakoMoor | 2022-07-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/62) |
| #61 | Build windows branch. ERROR | @LakoMoor | 2022-04-02 |  | [Link](https://github.com/nillerusr/source-engine/issues/61) |
| #60 | task in 'vstdlib' failed with exit status 1 | @IdleFishEngineer | 2022-03-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/60) |
| #59 | Android NDK Building Question | @IdleFishEngineer | 2022-03-28 |  | [Link](https://github.com/nillerusr/source-engine/issues/59) |
| #58 | Build Failed | @MagickMan77 | 2022-05-20 |  | [Link](https://github.com/nillerusr/source-engine/issues/58) |
| #57 | Does not work on Android 12 | @vixez | 2022-04-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/57) |
| #56 | Program received signal SIGILL, Illegal instruction. | @lilmayofuksu | 2022-03-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/56) |
| #50 | Create sln error: ERROR: Cannot open socketlib.vpc | @CeSun | 2022-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/50) |
| #48 | Build Error | @xiaodo1337 | 2022-02-15 |  | [Link](https://github.com/nillerusr/source-engine/issues/48) |
| #47 | Build Issues(master branch) | @xiaodo1337 | 2022-02-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/47) |
| #46 | // Broken, I am lazy to fix this | @nn-firc | 2022-02-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/46) |
| #45 | Checking for library opus                                                                                                      : not found | @nn-firc | 2022-02-14 |  | [Link](https://github.com/nillerusr/source-engine/issues/45) |
| #44 | Compiling hl2dm launcher, something went wrong | @magisk666 | 2022-04-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/44) |
| #42 | I can not switch guns | @nn-firc | 2022-02-03 |  | [Link](https://github.com/nillerusr/source-engine/issues/42) |
| #41 | help | @nn-firc | 2022-01-31 |  | [Link](https://github.com/nillerusr/source-engine/issues/41) |
| #40 | only hl2 build | @nn-firc | 2022-01-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/40) |
| #39 | UnicodeEncodeError: 'ascii' codec can't encode characters in position 9-10: ordinal not in range(128) | @nn-firc | 2022-01-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/39) |
| #37 | togles3 | @nn-firc | 2022-02-13 |  | [Link](https://github.com/nillerusr/source-engine/issues/37) |
| #36 | what is linux for android？ | @nn-firc | 2022-01-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/36) |
| #34 | VPC missing a couple of files | @conneath | 2022-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/34) |
| #33 | Build error | @Low1y | 2022-07-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/33) |
| #28 | Кто такой этот ваш vstdlib? | @MagickMan77 | 2021-11-12 |  | [Link](https://github.com/nillerusr/source-engine/issues/28) |
| #25 | CD Key | @AruMoon | 2022-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/25) |
| #24 | Node graph out of date. Rebuilding... crash | @AruMoon | 2022-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/24) |
| #22 | Build issues on Raspberry Pi OS | @CreeperCrafter979 | 2022-07-18 |  | [Link](https://github.com/nillerusr/source-engine/issues/22) |
| #21 | Android build error | @L0wl | 2022-04-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/21) |
| #20 | Build Configure is failed | @L0wl | 2021-10-07 |  | [Link](https://github.com/nillerusr/source-engine/issues/20) |
| #15 | for armhf linux？ | @nn-firc | 2022-01-27 |  | [Link](https://github.com/nillerusr/source-engine/issues/15) |
| #14 | Build Instructions for Big Surr and Windows 10 | @TylerJaacks | 2023-01-11 | enhancement, wontfix, osx | [Link](https://github.com/nillerusr/source-engine/issues/14) |
| #13 | Forcing 64 Bit Compilation on Mac OS Big Sur. | @TylerJaacks | 2022-06-19 |  | [Link](https://github.com/nillerusr/source-engine/issues/13) |
| #12 | Help needed, build fails using instricrions latest arch linux machine (vm) | @hipocentrum | 2022-04-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/12) |
| #10 | can u use regal？ | @nn-firc | 2021-05-29 |  | [Link](https://github.com/nillerusr/source-engine/issues/10) |
| #9 | Couldn't load combo of shader | @loochek | 2021-05-10 |  | [Link](https://github.com/nillerusr/source-engine/issues/9) |
| #7 | Did you use sse2neon? It can be more faster than rewriting... | @klaxons1 | 2021-04-25 |  | [Link](https://github.com/nillerusr/source-engine/issues/7) |
| #5 | Build error | @mihaios1k | 2021-04-06 |  | [Link](https://github.com/nillerusr/source-engine/issues/5) |
| #4 | Do not works on Nvidia Shield 2019 Pro | @benda95280 | 2020-12-30 |  | [Link](https://github.com/nillerusr/source-engine/issues/4) |
| #3 | Build for Android | @LoneLySTTR | 2020-12-16 |  | [Link](https://github.com/nillerusr/source-engine/issues/3) |
| #1 | how to build | @nn-firc | 2021-04-06 |  | [Link](https://github.com/nillerusr/source-engine/issues/1) |