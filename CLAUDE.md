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
  - `plugin/` — 插件目录。现有 9 个插件：`JsGetScreenInfo`（同步）、`JsGetUserInfo`（异步，测试数据勿用于生产）、`JsCloseHtmlPage`（同步）、`JsCanGoBack`（异步，控制返回键关页）、`JsOpenHtmlPage`（同步，启动新一层 WebViewActivity）、`JsTakePhoto`（异步，拍照压缩 base64 回传）、`JsPickPhotos`（异步，相册多选 base64 数组）、`JsPickAndUploadFiles`（异步，多选文件 multipart 上传）、`JsDownloadFile`（异步，OkHttp 流式下载到 Downloads）+ `PhotoCodecUtil`（图片压缩工具类）+ `IntranetTrust`（内网自签名证书 trust-all）
- `app/src/main/assets/webpage/` — `jsbridge.js`（ES5 IIFE，注入 `window.jsbridge`）+ `jsbridge_demo.html`（演示页）
- `app/src/main/res/` — 布局/主题（Theme.MyApp / Theme.MyApp.Splash）/字符串/图标（mipmap-anydpi-v26 + PNG 密度图）

## JSBridge 约定（与 ../news 保持一致，详见其 docs/jsbridge-dev-guide.md）

- **新增插件**：`jsbridge/plugin/` 下建类继承 `BaseJSPlugin`（异步）或 `BaseJSPluginSync`（同步），到 `WebViewActivity.registerJSApi()` 加一行 `registerJSPlugin`；JS 侧在 `jsbridge.js` 导出便捷方法，演示页加按钮。
- **硬约束**：异步插件恰好回调一次（四态选一）；回调 JSON 内禁止单引号（字符串拼接执行，会静默丢回调）；同步插件跑在 JS 线程禁碰 UI、返回必须是 JSON 字符串；参数解析必须 try-catch。
- **零初始化**：宿主生命周期保证 注入→注册→loadUrl 顺序，H5 引入 `jsbridge.js` 后随处可调，无 ready 握手。
- **UA 标识**：`NexBox/1.0`（有意与 news 共用，见 docs/adr/0001）。

## 移植自 ../news 的事项（重要）

JSBridge 框架及插件**事实来源是 ../news 仓库**（`C:\Users\lianghao\github\news`，纯 Java + X5 WebView 项目）。移植与后续同步须遵守：

- **源码对应关系**：本仓 `com.lianghao.dots.jsbridge.*` ←→ news 仓 `com.tencent.tbs.jsbridge.*`；宿主 `WebViewActivity` ←→ news `BaseWebViewActivity`（其 `registerJSApi()` 约 252-271 行是插件注册事实来源）。
- **本仓已做的适配**（向 news 回移或对照时注意差异）：① WebView 由 X5 `com.tencent.smtt.sdk.WebView` 改为系统 `android.webkit.WebView`；② 宿主依赖由 `BaseWebViewActivity` 具体类改为 `WebViewHost` 接口；③ `JSBridge` 移除了 X5 专属直暴露接口（`openDebugX5`/`openWebkit`/zxing `openQRCodeScan`），新增 `hybrid(callbackId)` 空实现兜底；④ 插件状态内聚（如 `JsCanGoBack` 的 allowClose 收进插件，news 版是宿主 public 字段）；⑤ **`jsbridge.js` 的 `callBackFromNative` 已修复 news 潜伏 bug**（空 params 时 `JSON.parse(undefined)` 抛异常丢回调，兜底为 `{}`）——news 侧同款文件未修，回移时勿覆盖本仓版本；⑥ **takePhoto 移植适配**（2026-09-17）：权限申请 EasyPermissions → 宿主 `requestRuntimePermissions`（ActivityResult API），拉起相机 `startActivityForResult` → 宿主 `startPluginActivityForResult`，FileProvider authorities 用本仓 `com.lianghao.dots.fileprovider`；结果码 0x0309 / 权限码 0x0019 与 news 对齐；接口契约（参数/错误码/cancel 态）与 news `docs/camera_bridge.md` 完全一致。⑦ **openHtmlPage 多层栈实现差异**（2026-09-23）：本仓 `JsOpenHtmlPage` 启动**当前 Activity**（`WebViewActivity`，与 Dashboard 单击格子走同一入口），news 侧启动独立 `PureX5WebViewActivity` + `singleTask`+`taskAffinity=:pure_x5_task`+返回键 `moveTaskToBack`。本仓选择 standard launchMode + 返回键直接 finish，行为是"九宫格 → 第一层 → 第二层 → … → 返回键逐层回退到九宫格"，栈深度由系统 Activity 任务栈托管，无独立 taskAffinity；JS 调用参数 `EXTRA_URL` 与 Dashboard 入口统一。⑧ **导航策略有意分叉**（2026-09-23，见 ADR-0004）：http/https 主框架导航本仓 `shouldOverrideUrlLoading` **开新层**，news 侧为 `return false` 层内加载；属有意分叉，勿按 news 改回。
- **未移植的插件**（news 共 18 个注册插件，本仓已移 9 个）：其余 9 个依赖 zxing/EasyPermissions/系统 GPS/锁屏方向等宿主设施，移植时按需引入。完整清单见 news `docs/jsbridge-dev-guide.md` §2。
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
# 产物: app/build/outputs/apk/debug/Dots-v1.0.apk
```

- SDK 路径已写入 `local.properties`（`C:/Users/lianghao/AppData/Local/Android/Sdk`）
- AGP 7 必须在 `app/build.gradle.kts` 中声明 `namespace`（Manifest 中不再写 package 属性）
- **包名**：`com.lianghao.dots`（2026-09-22 由 `com.lianghao.myapp` 改名；FileProvider authority 同步改为 `com.lianghao.dots.fileprovider`；App 名 Dots，APK 命名 `Dots-v<version>.apk`，debug/release 共用 `keystores/dots-release.jks` 签名，签名信息在 local.properties）

## 模拟器测试

已有 AVD：`Pixel_3a_API_35_extension_level_13_x86_64`

```bash
SDK=/c/Users/lianghao/AppData/Local/Android/Sdk
$SDK/emulator/emulator.exe -avd Pixel_3a_API_35_extension_level_13_x86_64
$SDK/platform-tools/adb.exe install -r app/build/outputs/apk/debug/Dots-v1.0.apk
$SDK/platform-tools/adb.exe shell am start -n com.lianghao.dots/.SplashActivity
# 验证前台 Activity: adb shell dumpsys activity activities | grep topResumedActivity
# 注意包名是 com.lianghao.dots；DashboardActivity 未导出，只能从 SplashActivity 进入
```

注意：Git Bash 会把 `/sdcard` 误转为本地路径，adb shell 内的设备路径需用 `//sdcard` 或禁用路径转换（`MSYS_NO_PATHCONV=1`）。

已实测通过（2026-09-15）：构建成功 → 模拟器安装 → Splash 1.2s 跳转 Dashboard，无崩溃。

### JSBridge 演示页装机验证（已实测通过 2026-09-16）

```bash
$SDK/platform-tools/adb.exe shell am start -n com.lianghao.dots/.SplashActivity
# Dashboard → 点「打开 JSBridge 演示页」；演示页路径 file:///android_asset/webpage/jsbridge_demo.html
$SDK/platform-tools/adb.exe logcat -s JsGetUserInfo:I JsGetScreenInfo:I JsCloseHtmlPage:I JsCanGoBack:I
```

验证点：同步通道（getScreenInfo 返回 JSON 字符串）、异步通道（getUserInfo success 回调）、fail 兜底（调用未注册函数）、closeHtmlPage 关页、canGoBack=false 时返回键拦截页面不关闭。

### openHtmlPage 多层 WebView 验证（已实测通过 2026-09-23，ADR-0004）

```bash
$SDK/platform-tools/adb.exe logcat -s JsOpenHtmlPage:I
# 路径：九宫格 → 第一层演示页 → 点「打开新一层 WebView(同步 openHtmlPage)」→ 第二层演示页
# 验证返回键逐层回退（点几次开几层，返回键一下退一层，最终回到九宫格）
# 验证 <a href> 自然导航：主框架 http/https 链接由 shouldOverrideUrlLoading 自动开新层（无需调 openHtmlPage）
```

验证点：JS 主动调 `jsbridge.openHtmlPage({url})` → 原生日志 `JsOpenHtmlPage: open url: ...` → Activity 栈叠加新一层 WebViewActivity → 返回键按开层顺序逆序 finish 回到 Dashboard。已知环境坑：注入 JS 调试可用 `adb forward tcp:9222 localabstract:webview_devtools_remote_<pid>` + chrome devtools protocol 的 `Runtime.evaluate`（pid 取自 `adb shell cat /proc/net/unix | grep webview_devtools_remote_ | grep -v Zygote`）。

### 分层导航装机验证（已实测通过 2026-09-23，ADR-0004）

```bash
$ADB install -r app/build/outputs/apk/debug/Dots-v1.1.apk   # 注意：包名 com.lianghao.dots，APK 已升 v1.1
# 验证路径（CDP 注入替代手点，见上一节环境坑）：
#  1. Dashboard 点演示页格 → 第一层（Hist #1）
#  2. CDP: jsbridge.openHtmlPage({url:'file://...演示页'}) → 第二层（#2），日志 JsOpenHtmlPage: open url
#  3. CDP: jsbridge.openHtmlPage({url:'https://example.com'}) → 第三层（#3）
#  4. CDP 注入 <a href="https://example.org"> 并 click → 第四层（#4）——自然导航自动开层
#  5. 返回键连按：逐层 finish（#4→#3→#2→#1→Dashboard），栈序 hist 数可见每按一次少一层
```

已验证：`openHtmlPage` 开层（file/https 均可）→ `<a>` 自然导航 http 自动开新层（无需 JS 参与）→ 返回键逐层回父页面直至 Dashboard → 多层栈下 `canGoBack=false` 拦截仍生效（误触「禁止返回键关页」按钮后该层按返回不退，恢复 canGoBack=true 后正常退层）。**实测发现的注意点**：演示页按钮排布密集，`input tap` 坐标必须先截图核对（本次 y=1360 误触禁止按钮，导致第一层返回键"失灵"假象）；CDP `Runtime.evaluate` 是替代手点的可靠通道，`jsbridge.openHtmlPage` 同步返回 `{"code":"success"}` 可直接断言。

### takePhoto 装机验证（已实测通过 2026-09-17）

```bash
# Dashboard → 打开 JSBridge 演示页 → 点「拍照(异步,base64回传)」→ 允许相机权限 → 快门(底栏中心) → 确认
$SDK/platform-tools/adb.exe logcat -d -s JsTakePhoto:V
# 期望: I JsTakePhoto: photo ready: 960x1280, 15749 bytes, base64 len=21000
```

已验证：权限弹窗→相机拉起→拍照→success 回调→页面 `#show` 显示 `takePhoto success: size=15749B, 960x1280, base64len=21000`→`<img id="photoPreview">` dataURL 渲染出照片（截屏比对 virtualscene 场景结构一致）。照片落盘 `cache/jsbridge_photo/photo_<ts>.jpg`。环境坑：①模拟器 `com.android.camera2` 自身偶发 ANR（Input dispatching timed out，virtualscene 保存慢），force-stop 后重跑即恢复，与插件无关；②演示页照片预览在按钮列表下方，验证渲染需滚动到页底。

### 九宫格 Dashboard 装机验证（已实测通过 2026-09-18，工单01–05）

```bash
$SDK/platform-tools/adb.exe install -r app/build/outputs/apk/debug/Dots-v1.0.apk
$SDK/platform-tools/adb.exe shell am start -n com.lianghao.dots/.SplashActivity
# 单击格1 → WebViewActivity；长按静止松手 → 编辑框；input draganddrop 起拖排序；恢复默认 → 二次确认
MSYS_NO_PATHCONV=1 $SDK/platform-tools/adb.exe shell "run-as com.lianghao.dots cat files/grid_items.json"
# 重装恢复实测：
$ADB shell bmgr transport com.android.localtransport/.LocalTransport   # GMS transport 未登录会 Transport error
$ADB shell bmgr backupnow com.lianghao.dots && $ADB uninstall com.lianghao.dots && $ADB install app/build/outputs/apk/debug/Dots-v1.0.apk
# 期望: 重装首启 files/grid_items.json 恢复用户配置（KeepMe 实测通过）
```

已验证：九宫格渲染（6 实格+3 空格、首字母色块）→ 首启出厂配置落盘 filesDir → 单击开 URL → 长按编辑保存（杀进程重进仍在）→ draganddrop 排序持久化 → 恢复默认（二次确认，UI+落盘回出厂）→ bmgr local transport 备份→卸载→重装→KeepMe 配置自动恢复 → JSBridge 回归（getScreenInfo 1080×2154 / getUserInfo 桥日志正常）。**过程中修 1 个真 bug**：长按弹框泄漏 click（`CellGestureHandler` UP 分支先重置标志后 return false，系统补发 click 打开 WebView 盖住编辑框；修复=UP 先记 `consume` 再重置）。环境坑：①模拟器默认 GMS transport 未登录 → `Transport error`，须 `bmgr transport` 切 local transport；②`bmgr select-transport` 不是合法子命令，用 `bmgr transport WHICH`；③模拟器手势注入用 `input draganddrop x1 y1 x2 y2 3000`（3s 内建长按，恰好触发本仓起拖手势），普通 `input swipe` 时长不足会先触发编辑框。
