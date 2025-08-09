# LuaDecompiler (Android)

Android app to inspect Lua files, detect bytecode versions, and decompile when possible.

## Current status
- Detects Lua bytecode (5.1/5.2/5.3) and LuaJIT
- Shows plain text `.lua` files
- Engines scaffolded:
  - unluac (Lua 5.1): add `app/libs/unluac.jar` to enable
  - luadec (Lua 5.1–5.3, native): not yet integrated

## Quick start
1. Open this project in Android Studio (Giraffe+ recommended)
2. Ensure JDK 17 is configured for the project
3. Optional: add `unluac.jar` to `app/libs/` to enable 5.1 decompilation
4. Build and run on a device/emulator (minSdk 24)

## unluac integration
- Download `unluac.jar` from its official distribution and place it at `app/libs/unluac.jar`
- The Gradle script will include it automatically if present
- The `UnluacEngine` currently detects the presence of the jar but does not call a public API; you may:
  - Expose a programmatic API in the jar, or
  - Replace `UnluacEngine` to call into the library if it provides stable entry points

## Roadmap
- Wire unluac API to actually decompile 5.1 chunks on-device
- Add native `luadec` via Android NDK for 5.1–5.3
- Graceful, partial handling for LuaJIT
- Batch folder selection and APK scanning

## License
This repo includes no third-party binaries. Add your own decompiler artifacts as needed.