# 4. 分层导航：http/https 主框架导航开新层，返回键逐层回父页面

日期：2026-09-23
状态：已接受

## 背景

格子网单击开宿主页面后，页面内继续点击链接的去向需要定案。news 仓（JSBridge 事实来源）的 `BaseWebViewActivity` 对 http 链接 `return false` 层内加载；dots 需要决定沿用还是分叉。用户诉求：**手机返回键的动作是「回到父页面」**——即点进来的路径，返回键原路退出。

会谈逐项确认（2026-09-23）：

- 层数**无上限**，每点一次主框架导航叠一层，不封顶。
- 返回键**混合语义**：本层 WebView 有内部历史（SPA pushState、30x 重定向、file 层内跳转）先层内退，退空才关层——与浏览器直觉一致，SPA 页面返回先收尾页面内状态。
- iframe 子框架导航**丢弃**：无法区分「iframe 想跳」与「页面想展示 iframe 内容」，放行会让广告疯狂开层。
- `openHtmlPage` 插件保留，与自然导航**殊途同归**：都往栈上叠层（news 侧有同款插件，接口契约一致，保留即两仓 H5 行为对齐）。
- file 等本地协议**层内加载**：规则是「跨入互联网才开层」，本地资源叠层无隔离价值。

候选方案：

1. **无上限叠层 + 混合返回**（选定）：每次主框架 http/https 导航 = 开一层，返回键先层内退再关层。栈托管给系统 Activity 栈，无自定义计数。
2. **封顶两层**（或 N 层）：第二层内点击退化为层内前进；需传层号、按层分派两套行为，「层」心智模型混杂，被否。
3. **news 式层内加载**：整程单 Activity，返回键走 WebView 历史；「返回=父页面」只在历史走完时成立，SPA 深链下返回键行为不可预期，被否。

## 决策

- `shouldOverrideUrlLoading`：主框架 http/https → `WebViewActivity.start()` 开新层；iframe http/https → 丢弃（`return true` 不加载）；file/about/blob/data → 层内加载；其余 scheme → 尝试唤起外部 App，无 App 处理则吞掉导航。
- `onBackPressed`：`webView.canGoBack()` 时层内 `goBack()`；否则（canGoBack 插件未禁止关页时）finish 本层，露出父页面。
- `JsOpenHtmlPage`（同步插件）保留：JS 主动开层与自然导航同栈同语义，启动目标为 `WebViewActivity`（与格子网入口统一走 `EXTRA_URL`）。
- 与 news 的**有意分叉**：news 层内加载、dots 分层。分叉不回移 news；`openHtmlPage` 插件接口契约两仓保持一致。

## 后果

- 正面：返回键语义可预期（原路逐层退出）；每层独立 Activity，内存隔离、层间互不污染；实现零自定义栈管理。
- 负面：深链多的站点连点多次会叠很多层（Activity 栈占用）；同一 SPA 站点的内部路由不叠层（pushState 走层内历史），「层」粒度是站点主框架导航而非页面视图。
- 术语：**层 / 开层 / 父页面 / 分层导航** 已录入 CONTEXT.md（2026-09-23）。
