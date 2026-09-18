# CLAUDE.md

本项目为 Android 应用（见 readme.md）：Kotlin + Splash Screen + Dashboard（显示「欢迎访问」）+ JSBridge WebView 容器（自 ../news 移植）。

## 项目结构

- `app/src/main/java/com/lianghao/myapp/`
  - `SplashActivity.kt` — LAUNCHER 入口，`core-splashscreen` 兼容库，1.2s 后跳转 Dashboard
  - `DashboardActivity.kt` — 九宫格启动台（3×3，见 docs/adr/0002 与下文「九宫格」节）：单击开格、长按静止松手弹「格子编辑」、长按拖动排序；配置经 `grid/GridConfigStore` 读写
  - `grid/` — 九宫格支持包：`GridItem`（格子条目，空=灰显占位）/`GridConfigJson`（纯编解码，损坏返回 null）/`GridConfigStore`（filesDir 用户配置 + assets 出厂配置，首启拷贝落盘）/`GridAdapter`（自管手势状态机 + ItemTouchHelper）/`GridCellEditDialog`（编辑+恢复默认入口）
  - `WebViewActivity.kt` — JSBridge 宿主：系统 WebView + 注册插件 + 加载 URL（缺省加载内置演示页）
- `app/src/main/java/com/lianghao/myapp/jsbridge/` — JSBridge 核心框架（Java，自 ../news 的 com.tencent.tbs.jsbridge 移植，WebView 由 X5 改为系统 android.webkit.WebView，宿主依赖抽象为 `WebViewHost` 接口）
  - `JSBridge.java`（分发器，注入对象名 `Android`）/ `BaseJSPlugin.java`（异步基类）/ `BaseJSPluginSync.java`（同步基类）/ `JSCallbackType.java`（四态）/ `HybridConstant.java` / `WebViewHost.java`（宿主接口：含 `startPluginActivityForResult`/`requestRuntimePermissions`，为本仓新增）
  - `plugin/` — 插件目录。现有 5 个插件：`JsGetScreenInfo`（同步）、`JsGetUserInfo`（异步，测试数据勿用于生产）、`JsCloseHtmlPage`（同步）、`JsCanGoBack`（异步，控制返回键关页）、`JsTakePhoto`（异步，拍照压缩 base64 回传）+ `PhotoCodecUtil`（图片压缩工具类）
- `app/src/main/assets/webpage/` — `jsbridge.js`（ES5 IIFE，注入 `window.jsbridge`）+ `jsbridge_demo.html`（演示页）
- `app/src/main/res/` — 布局/主题（Theme.MyApp / Theme.MyApp.Splash）/字符串/图标（mipmap-anydpi-v26 + PNG 密度图）

## JSBridge 约定（与 ../news 保持一致，详见其 docs/jsbridge-dev-guide.md）

- **新增插件**：`jsbridge/plugin/` 下建类继承 `BaseJSPlugin`（异步）或 `BaseJSPluginSync`（同步），到 `WebViewActivity.registerJSApi()` 加一行 `registerJSPlugin`；JS 侧在 `jsbridge.js` 导出便捷方法，演示页加按钮。
- **硬约束**：异步插件恰好回调一次（四态选一）；回调 JSON 内禁止单引号（字符串拼接执行，会静默丢回调）；同步插件跑在 JS 线程禁碰 UI、返回必须是 JSON 字符串；参数解析必须 try-catch。
- **零初始化**：宿主生命周期保证 注入→注册→loadUrl 顺序，H5 引入 `jsbridge.js` 后随处可调，无 ready 握手。
- **UA 标识**：`NexBox/1.0`（有意与 news 共用，见 docs/adr/0001）。

## 移植自 ../news 的事项（重要）

JSBridge 框架及插件**事实来源是 ../news 仓库**（`C:\Users\lianghao\github\news`，纯 Java + X5 WebView 项目）。移植与后续同步须遵守：

- **源码对应关系**：本仓 `com.lianghao.myapp.jsbridge.*` ←→ news 仓 `com.tencent.tbs.jsbridge.*`；宿主 `WebViewActivity` ←→ news `BaseWebViewActivity`（其 `registerJSApi()` 约 252-271 行是插件注册事实来源）。
- **本仓已做的适配**（向 news 回移或对照时注意差异）：① WebView 由 X5 `com.tencent.smtt.sdk.WebView` 改为系统 `android.webkit.WebView`；② 宿主依赖由 `BaseWebViewActivity` 具体类改为 `WebViewHost` 接口；③ `JSBridge` 移除了 X5 专属直暴露接口（`openDebugX5`/`openWebkit`/zxing `openQRCodeScan`），新增 `hybrid(callbackId)` 空实现兜底；④ 插件状态内聚（如 `JsCanGoBack` 的 allowClose 收进插件，news 版是宿主 public 字段）；⑤ **`jsbridge.js` 的 `callBackFromNative` 已修复 news 潜伏 bug**（空 params 时 `JSON.parse(undefined)` 抛异常丢回调，兜底为 `{}`）——news 侧同款文件未修，回移时勿覆盖本仓版本；⑥ **takePhoto 移植适配**（2026-09-17）：权限申请 EasyPermissions → 宿主 `requestRuntimePermissions`（ActivityResult API），拉起相机 `startActivityForResult` → 宿主 `startPluginActivityForResult`，FileProvider authorities 用本仓 `com.lianghao.myapp.fileprovider`；结果码 0x0309 / 权限码 0x0019 与 news 对齐；接口契约（参数/错误码/cancel 态）与 news `docs/camera_bridge.md` 完全一致。
- **未移植的插件**（news 共 18 个注册插件，本仓已移 5 个）：其余 13 个依赖 zxing/EasyPermissions/FileProvider/CacheWebView 等宿主设施，移植时按需引入（FileProvider 本仓已随 takePhoto 引入）；`pickAndUploadFiles` 在 news 侧本身未实现（仅契约冻结）。完整清单见 news `docs/jsbridge-dev-guide.md` §2。
- **文档同步**：news 侧 `docs/jsbridge-dev-guide.md` 是接口契约的权威文档（含硬约束、调用时序、调试对照表）；本仓 `docs/web/jsbridge.js` 未维护副本，以本仓 `assets/webpage/jsbridge.js` 为准，与 news 版差异见上一条 ⑤。
- **UA/演示页**：UA 标识两仓共用 `NexBox/1.0`（决策见 docs/adr/0001）；H5 环境检测、`jsbridge.js` 引用方式与 news §6 完全一致，同一页面可无差别跑在两仓 App 内。

## 技术栈与版本

- Kotlin 1.6.21 / AGP 7.0.2 / Gradle 7.4 wrapper
- compileSdk 31，minSdk 23，targetSdk 31
- AndroidX：appcompat、core-ktx、material、constraintlayout、core-splashscreen 1.0.0、exifinterface 1.3.7（takePhoto EXIF 旋转转正）

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
$SDK/platform-tools/adb.exe shell am start -n com.lianghao.myapp/.SplashActivity
# 验证前台 Activity: adb shell dumpsys activity activities | grep topResumedActivity
# 注意包名是 com.lianghao.myapp；DashboardActivity 未导出，只能从 SplashActivity 进入
```

注意：Git Bash 会把 `/sdcard` 误转为本地路径，adb shell 内的设备路径需用 `//sdcard` 或禁用路径转换（`MSYS_NO_PATHCONV=1`）。

已实测通过（2026-09-15）：构建成功 → 模拟器安装 → Splash 1.2s 跳转 Dashboard，无崩溃。

### JSBridge 演示页装机验证（已实测通过 2026-09-16）

```bash
$SDK/platform-tools/adb.exe shell am start -n com.lianghao.myapp/.SplashActivity
# Dashboard → 点「打开 JSBridge 演示页」；演示页路径 file:///android_asset/webpage/jsbridge_demo.html
$SDK/platform-tools/adb.exe logcat -s JsGetUserInfo:I JsGetScreenInfo:I JsCloseHtmlPage:I JsCanGoBack:I
```

验证点：同步通道（getScreenInfo 返回 JSON 字符串）、异步通道（getUserInfo success 回调）、fail 兜底（调用未注册函数）、closeHtmlPage 关页、canGoBack=false 时返回键拦截页面不关闭。已知环境坑：模拟器冷进程**首次**进入 WebViewActivity 可能因 chromium 初始化阻塞主线程 >5s 触发一次 ANR（非代码问题），force-stop 重开后正常。

### takePhoto 装机验证（已实测通过 2026-09-17）

```bash
# Dashboard → 打开 JSBridge 演示页 → 点「拍照(异步,base64回传)」→ 允许相机权限 → 快门(底栏中心) → 确认
$SDK/platform-tools/adb.exe logcat -d -s JsTakePhoto:V
# 期望: I JsTakePhoto: photo ready: 960x1280, 15749 bytes, base64 len=21000
```

已验证：权限弹窗→相机拉起→拍照→success 回调→页面 `#show` 显示 `takePhoto success: size=15749B, 960x1280, base64len=21000`→`<img id="photoPreview">` dataURL 渲染出照片（截屏比对 virtualscene 场景结构一致）。照片落盘 `cache/jsbridge_photo/photo_<ts>.jpg`。环境坑：①模拟器 `com.android.camera2` 自身偶发 ANR（Input dispatching timed out，virtualscene 保存慢），force-stop 后重跑即恢复，与插件无关；②演示页照片预览在按钮列表下方，验证渲染需滚动到页底。

### 九宫格 Dashboard 装机验证（已实测通过 2026-09-18，工单01–05）

```bash
$SDK/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
$SDK/platform-tools/adb.exe shell am start -n com.lianghao.myapp/.SplashActivity
# 单击格1 → WebViewActivity；长按静止松手 → 编辑框；input draganddrop 起拖排序；恢复默认 → 二次确认
MSYS_NO_PATHCONV=1 $SDK/platform-tools/adb.exe shell "run-as com.lianghao.myapp cat files/grid_items.json"
# 重装恢复实测：
$ADB shell bmgr transport com.android.localtransport/.LocalTransport   # GMS transport 未登录会 Transport error
$ADB shell bmgr backupnow com.lianghao.myapp && $ADB uninstall com.lianghao.myapp && $ADB install app/build/outputs/apk/debug/app-debug.apk
# 期望: 重装首启 files/grid_items.json 恢复用户配置（KeepMe 实测通过）
```

已验证：九宫格渲染（6 实格+3 空格、首字母色块）→ 首启出厂配置落盘 filesDir → 单击开 URL → 长按编辑保存（杀进程重进仍在）→ draganddrop 排序持久化 → 恢复默认（二次确认，UI+落盘回出厂）→ bmgr local transport 备份→卸载→重装→KeepMe 配置自动恢复 → JSBridge 回归（getScreenInfo 1080×2154 / getUserInfo 桥日志正常）。**过程中修 1 个真 bug**：长按弹框泄漏 click（`CellGestureHandler` UP 分支先重置标志后 return false，系统补发 click 打开 WebView 盖住编辑框；修复=UP 先记 `consume` 再重置）。环境坑：①模拟器默认 GMS transport 未登录 → `Transport error`，须 `bmgr transport` 切 local transport；②`bmgr select-transport` 不是合法子命令，用 `bmgr transport WHICH`；③模拟器手势注入用 `input draganddrop x1 y1 x2 y2 3000`（3s 内建长按，恰好触发本仓起拖手势），普通 `input swipe` 时长不足会先触发编辑框。
