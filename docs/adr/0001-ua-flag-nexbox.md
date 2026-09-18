# 1. WebView 容器 UA 标识沿用 NexBox/1.0

日期：2026-09-16
状态：已接受

## 背景

从 ../news（NexBox 容器）向本项目移植 JSBridge 框架时，WebView UserAgent 需要追加一个容器标识，供 H5 页面通过 `navigator.userAgent.match(/NexBox\/(\d+\.\d+)/)` 感知运行环境与容器版本（环境检测首选方案）。

候选：沿用 `NexBox/1.0`，或启用本项目专属标识（如 `SampleBox/1.0`）。

## 决策

沿用 `NexBox/1.0`，不引入新标识。

## 后果

- 正面：同一套 H5 页面在两个 App 的 WebView 中环境检测行为一致，H5 侧无需按 App 分支；与 news 侧 jsbridge.js / 检测文档直接互拷。
- 负面：H5 无法区分页面运行在哪个 App；未来两容器行为分叉（同名函数返回不同结构）时，需引入新标识并让 H5 按版本分支。
- 版本升级约定：`HybridConstant.HYBRID_UA_FLAG` 版本号变动时，两仓需同步评估 H5 侧按版本分支的逻辑。

## 备注

news 侧依据：`com.tencent.tbs.jsbridge.HybridConstant` + `docs/jsbridge-dev-guide.md` §6.3（UA 标识自 NexBox 1.0 / 2026-08 起生效）。
