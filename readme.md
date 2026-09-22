# MyApp

## 项目介绍

1. 这是 android app
2. 技术栈：kotlin
3. 包括 splash screen 和 dashboard
4. dashboard页展示以下文字：欢迎访问

## 项目结构

```
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # SplashActivity 为 LAUNCHER 入口
│       ├── java/com/lianghao/dots/
│       │   ├── SplashActivity.kt        # 启动页，1.2s 后跳转 Dashboard
│       │   └── DashboardActivity.kt     # 主页，居中显示「欢迎访问」
│       └── res/
│           ├── layout/                  # activity_splash / activity_dashboard
│           ├── values/                  # strings / colors / themes
│           └── mipmap-*/                # 应用图标（各密度）
├── build.gradle.kts                     # AGP 7.0.2 + Kotlin 1.6.21
├── settings.gradle.kts                  # 含阿里云镜像
└── gradlew                              # Gradle 7.4 wrapper
```

## 技术栈

| 项 | 版本 |
|---|---|
| Kotlin | 1.6.21 |
| AGP / Gradle | 7.0.2 / 7.4 |
| compileSdk / minSdk / targetSdk | 31 / 23 / 31 |
| 关键依赖 | appcompat、material、constraintlayout、core-ktx、core-splashscreen 1.0.0 |

## 构建与运行

```bash
# 本机无独立 java 命令时，先指向 Android Studio 自带 JRE
export JAVA_HOME="C:\Program Files\Android\Android Studio\jre"

./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

## 模拟器测试

```bash
SDK=/c/Users/lianghao/AppData/Local/Android/Sdk
$SDK/emulator/emulator.exe -avd Pixel_3a_API_35_extension_level_13_x86_64
$SDK/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
$SDK/platform-tools/adb.exe shell am start -n com.lianghao.dots/.SplashActivity
```

已实测通过：构建 → 安装 → Splash 自动跳转 Dashboard，正常显示「欢迎访问」。
