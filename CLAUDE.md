# CLAUDE.md

本项目为 Android 应用（见 readme.md）：Kotlin + Splash Screen + Dashboard（显示「欢迎访问」）。

## 项目结构

- `app/src/main/java/com/example/myapplication/`
  - `SplashActivity.kt` — LAUNCHER 入口，`core-splashscreen` 兼容库，1.2s 后跳转 Dashboard
  - `DashboardActivity.kt` — 加载 `activity_dashboard.xml`，居中显示 `@string/welcome_text`
- `app/src/main/res/` — 布局/主题（Theme.MyApp / Theme.MyApp.Splash）/字符串/图标（mipmap-anydpi-v26 + PNG 密度图）

## 技术栈与版本

- Kotlin 1.6.21 / AGP 7.0.2 / Gradle 7.4 wrapper
- compileSdk 31，minSdk 23，targetSdk 31
- AndroidX：appcompat、core-ktx、material、constraintlayout、core-splashscreen 1.0.0

## 构建与运行

环境说明：本机无独立 java/gradle 命令，构建前需设置：

```bash
export JAVA_HOME="C:\\Program Files\\Android\\Android Studio\\jre"
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

- SDK 路径已写入 `local.properties`（`C:/Users/lianghao/AppData/Local/Android/Sdk`）
- AGP 7 必须在 `app/build.gradle.kts` 中声明 `namespace`（Manifest 中不再写 package 属性）

## 模拟器测试

已有 AVD：`Pixel_3a_API_35_extension_level_13_x86_64`

```bash
SDK=/c/Users/lianghao/AppData/Local/Android/Sdk
$SDK/emulator/emulator.exe -avd Pixel_3a_API_35_extension_level_13_x86_64
$SDK/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
$SDK/platform-tools/adb.exe shell am start -n com.example.myapplication/.SplashActivity
# 验证前台 Activity: adb shell dumpsys activity activities | grep topResumedActivity
```

注意：Git Bash 会把 `/sdcard` 误转为本地路径，adb shell 内的设备路径需用 `//sdcard` 或禁用路径转换（`MSYS_NO_PATHCONV=1`）。

已实测通过（2026-09-15）：构建成功 → 模拟器安装 → Splash 1.2s 跳转 Dashboard，无崩溃。
