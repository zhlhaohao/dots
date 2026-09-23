# Scientific Agent Skills — 全量技能摘要

> 中文总结，按主题分组。每个 skill 一节，5 个角度：**用途 / 技能设计 / 典型工作流 / 特色 / 涉及资源**。
> 信息均来自对 `skills/<name>/SKILL.md`（及 references/）的通读，非搬运 frontmatter。
> 旧版本（搬运式英文版）见 git 历史；当前为重写版。
> 已收录桶：**Wet-lab automation & robotics**、**Neuroscience & behavior**、**Reinforcement learning**。

---

## Wet-lab automation & robotics

### `opentrons-integration`

1. **用途**：面向 Opentrons Flex 和 OT-2 两台移液机器人，编写、审查、迁移、仿真和排障官方 Python Protocol API v2 协议，覆盖移液动作、deck/耗材布局、移液器与模块控制、运行时参数，以及 Opentrons App 分析——属于对单一厂商生态的深度集成。
2. **技能设计**：按主题拆为多个 `references/`（API 参考、协议编写、液体处理、模块与 deck、验证运维、2.19→2.29 迁移指南、来源）；`scripts/` 给 6 个可运行模板（Flex/OT-2 基础协议、梯度稀释、PCR、运行时参数、读板仪）；并明确两套版本锁定环境（Flex 用 opentrons 9.1.1、OT-2 用 9.0.0），避免双发布线串混。
3. **典型工作流**：先按"必需信息采集"清单收齐（机型、API 级别、移液器、耗材 load name、液体量、tip 策略等），再走七步：选机型与 API 级别 → 显式构建 deck → 选移液器/tip → 选液体操作层 → 写 setup 信息与运行参数 → 预算 tip/体积/时间 → 分层验证（编译 → 锁版本仿真 → App 分析 → 操作员复核 → 慢速空跑）。
4. **特色**：安全边界极强——仿真通过绝不等于可上机，上机前要走六步人工核查；提供 API 版本门控清单（2.20 CSV 参数到 2.29 步骤分组）防止误用新特性；明确划线 2.29 仅 Flex 可用；专设"常见失败模式"清单（如旧版移液器命名、部分吸头危险孔位致撞机）。
5. **涉及资源**：Opentrons Python Protocol API v2、`opentrons` PyPI 包（9.1.1/9.0.0）、`opentrons_simulate` CLI、Opentrons App、官方 Labware Library、Protocol Designer（JSON）、机器人 HTTP API（OpenAPI 文档）、Python 3.10+ 与 uv。

### `pylabrobot`

1. **用途**：基于 PyLabRobot——硬件无关的开源实验室自动化框架——做跨厂商移液计划开发与设备集成审查，适用一个工作流需同时涉及 Hamilton STAR/Vantage、Tecan EVO、Opentrons OT-2 等多家机器人品牌的场景。默认只做离线清单校验与软件仿真，不直接驱动硬件。
2. **技能设计**：`references/` 按域分（液体处理、资源树、硬件后端、分析设备、物料处理、可视化），`scripts/` 给 5 个离线优先 CLI（清单校验、deck 几何检查、转移规划、仿真计划生成、后端检查），另有 JSON Schema 资产和合成测试夹具；整体不走"路由表"，而是靠"必需信息采集"清单驱动。
3. **典型工作流**：先收集设备型号、deck 坐标、耗材容量、tip/通道等事实（信息不全只出离线草案）；用 uv 建隔离环境装 PyLabRobot 0.2.1；写协议清单 JSON + 转移 CSV；依次跑 `validate_manifest` → `check_deck_geometry` → `plan_transfers` → `generate_simulation_plan`，全程使用 chatterbox 软件后端，不碰真实硬件。
4. **特色**：不可协商的硬件边界——禁止自动连接/初始化/移动任何物理设备，实机运行必须由受训人员通过六步确认并单独授权；明确 tracker 状态只是记账而非传感、chatterbox 不证明碰撞自由；严格区分新旧 API 命名以防过期代码（如 `STARBackend` 而非 `STAR`）。
5. **涉及资源**：PyLabRobot 0.2.1（PyPI）、chatterbox 与 Visualizer 后端（localhost HTTP/WebSocket）、Hamilton/Tecan/Opentrons 设备后端及 serial/usb extras、`docs.pylabrobot.org` 稳定版文档、GitHub 源码标签与 changelog、Python 3.11 + uv。

### `simpy`

1. **用途**：用 SimPy 构建、检查、测试和分析基于进程的离散事件仿真（DES），面向队列、生产/物流系统、网络、服务运营、库存等事件驱动系统的建模与统计推断。技能本身只承担"如何正确用 SimPy"的工程指引，不替代仿真研究方法论本身。
2. **技能设计**：`references/` 八篇（事件、进程交互、资源、监控、实时、方法论、CLI 指南、来源）覆盖 API 语义与统计方法两层；`scripts/` 给四个安全 CLI（内置队列场景、重复实验运行器、事件轨迹摘要、配置校验器）和 `resource_monitor`；核心语义用表格与规则清单组织（如七种共享资源类型对照表）。
3. **典型工作流**：九步方法论——定义目的与估计量 → 先写概念模型 → 用生成器实现进程并用 `env.process` 注册 → 给每次生产运行设时间/实体/事件/复制上限 → 分离随机流并留种子清单 → 刻意插桩监控 → 验证与确认（守恒律、解析基准）→ 独立重复实验算置信区间 → 报告局限（不把相关性说成因果）。
4. **特色**：强调"有界执行"——绝不 `env.run()` 无界进程；详述 `env.run(until=数值)` 与 `Event` 的半开区间差异、`ConditionValue` 按事件对象取值、中断不取消目标事件等易错语义；CLI 拒绝 URL/符号链接/非有限数/无界输入且不执行用户代码；时间加权统计要求面积法而非事件平均，置信区间拒绝单次复制。
5. **涉及资源**：SimPy 4.1.2（PyPI，无运行时依赖）、`simpy.rt` 实时环境、Python 3.10+/3.13 与 uv、版本化官方文档链接；全程无网络调用。

---

## Neuroscience & behavior

### `neurokit2`

- **用途**：面向使用 NeuroKit2（锁定 0.2.13 版本）进行生理信号（ECG/EDA/EEG/EMG/EOG/PPG/RSP 等）研究的可复现工作流构建与审计。当用户代码 `import neurokit2`、需要其当前 API/输出模式（schema）或方法感知校验时触发，明确不用于医疗诊断或设备验证。

- **技能设计**：SKILL.md 本身是主控文档，内含"证据截止日期"（2026-07-23 快照）、明确的安全边界、必填数据契约（8 项采集前必须记录的信息，相当于信息收集清单）和 10 步固定预处理顺序。`references/` 下按模态拆成 12 个小文档（hrv、eda、complexity、bio_module 等）形成路由表，要求"只读当前需要的文件"；`scripts/` 提供 6 个有防护的命令行工具，全部拒绝 URL/路径穿越/符号链接、限制字节与行数、不使用 pickle、输出确定性 JSON/CSV。

- **典型工作流**：先用 `generate_synthetic.py` 生成合成数据或 `--deidentified` 标记真实数据 → `inspect_signal.py` 先检查后处理（时间单调性、缺口、平线等）→ 按固定顺序预处理（保原始、核单位、分段、清洗、检峰、质量检查、带日志的峰校正、派生特征、对齐、分 epoch）→ 将运行时观察到的 schema 连同版本、参数一起持久化 → 用 `ecg_hrv_pipeline.py` / `eda_pipeline.py` 等管线或 `validate_multimodal.py` 做多模态校验。

- **特色**：①"schema 是运行时观察而非承诺"——强调输出列随版本/方法/数据变化，必须实测记录；②强解释学约束（如 LF/HF 不等于交感-迷走平衡、短记录禁用 VLF、PRV≠HRV）；③版本双轨制——稳定 wheel 与 dev 文档版本分开标注；④隐私边界——PHI 不得进入 prompt/日志/fixtures；⑤专门提示静态扫描器可能把 `eeg_*` / `events_*` 误报为 eval/exec 的误报处理方式。

- **涉及到的资源**：PyPI 的 NeuroKit2 0.2.13、GitHub 官方仓库、官方文档站（neuropsychology.github.io/NeuroKit）、Makowski/Pham 等论文 DOI、SPR 指南；可选依赖 MNE、cvxopt、Plotly、PyEMD、pyRQA、Pillow、OpenCV 等；工具链为 Python 3.10+ 与 uv。

### `neuropixels-analysis`

- **用途**：对 Neuropixels 1.0/2.0 高密度细胞外神经记录做端到端分析——从 SpikeGLX/Open Ephys/NWB 原始数据加载，经预处理、漂移校正、锋电位排序（Kilosort4 等），到质量指标计算与单元筛选（阈值、模型、AI 辅助三层），最终导出为 Phy/NWB，产出可发表级别的 curated units。

- **技能设计**：SKILL.md 提供 8 步标准工作流 + 常见陷阱清单 + 可调参数表；`references/` 下 10 个专题文档（预处理、锋电位排序、运动校正、质量指标、自动/模型/AI 筛选、绘图、API 参考等）构成路由表；`scripts/` 有 6 个可单独运行的脚本（explore/preprocess/run_sorting/compute_metrics/export_to_phy）加一个 `neuropixels_pipeline.py` 全流程封装；`assets/analysis_template.py` 是可复制修改的完整分析模板。API 设计与 Allen Institute、IBL 的最佳实践对齐。

- **典型工作流**：加载（先用 `get_neo_streams` 查流，选 AP 流，可切片 60 秒快速迭代）→ 预处理（400Hz 高通 → 检测并移除坏通道 → NP1.0 的 `phase_shift` ADC 相位校正 → 全局中位数共参考 → 保存为 binary）→ 检查漂移（峰检测+定位+drift raster 图，>10 μm 需校正，用 `correct_motion` 选 preset）→ 排序（GPU 上 Kilosort4 推荐；CPU 备选 SpykingCircus2/Mountainsort5/Tridesclous2）→ 后处理建 SortingAnalyzer（波形、模板、自相关图、单元定位、质量指标）→ 三层筛选 → 导出 Phy/报告/NWB/CSV。

- **特色**：①三层递进筛选体系——Allen 风格指标阈值（含 allen/ibl/strict 预设）处理明确案例，Hugging Face 上的 UnitRefine 预训练分类器（噪声/神经、单单元/多单元，带概率）处理中间地带，视觉模型/AI 只看不确定单元的波形-自相关图，且强调"自动结果是起点而非结论"；②agent 原生设计——在 Cursor/Claude Code 里无需 API key，agent 直接看图给出专家级判读；③凭据安全——API key 一律从环境变量读取、绝不硬编码；④`trust_model=True` 信任门槛提醒（模型跨脑区迁移性需人工验证子集校验）；⑤硬件感知（NP1.0 需 ADC 相位校正、NP2.0 四 shank 多区域）。

- **涉及到的资源**：SpikeInterface（含 `spikeinterface.curation`、`spikeinterface.exporters`）、Neo/probeinterface；排序器 Kilosort4（需 CUDA GPU）、SpykingCircus、Mountainsort5；Hugging Face 的 SpikeInterface/UnitRefine 模型仓库（需 huggingface_hub + skops）；可选 Anthropic Python SDK（Claude 视觉模型，环境变量 ANTHROPIC_API_KEY）；IBL 工具（ibl-neuropixel/ibllib）、Bombcell；文档站 spikeinterface.readthedocs.io 及 Neuropixels 教程、Allen Institute ecephys、awesome_neuropixels 等 GitHub 资源；数据格式 SpikeGLX（.ap.bin/.lf.bin/.meta）、Open Ephys（.continuous/.oebin）、NWB。

---
## Reinforcement learning

### `pufferlib`
- **用途**：指导在 PufferLib 上做强化学习开发——环境适配（Gymnasium/PettingZoo）、向量化训练、策略构建、训练/评估与检查点审查。核心问题是上游存在两条互不兼容的版本线（PyPI 稳定版 3.0.0 与重写后的 4.0 源码），skill 帮助按明确的"版本档案"选择正确的 API。
- **技能设计**：SKILL.md 内置 3.0/4.0 双档案对照表和安全默认规则清单；`references/` 按 workflow 领域分 5 篇（environments、vectorization、policies、training、integration 含迁移矩阵）；`scripts/` 全部零依赖、仅标准库、输出严格 JSON，且默认 dry-run 不真正执行训练。
- **典型工作流**：先跑本地合成、无网络的 CLI 自检 → 验证环境契约（reset/step 五元组、seed、清理）→ 审查后再包装环境 → 向量化（先 Serial 后 Multiprocessing）→ 用 `train_template` 生成训练计划并用 `validate_plan` 校验（拒绝含密钥、无界资源、混版配置）→ 检查点先哈希审查再沙箱加载。
- **特色**：极强的供应链与安全边界——锁定 PyPI SHA-256 与 4.0 commit、禁止混用版本、禁止管道执行远程安装器、凭据只允许命名环境变量（`WANDB_API_KEY` 等）且绝不打印、检查点只做哈希/分类绝不反序列化（不调用 `torch.load`）。默认一切本地、CPU、无网、有界。
- **涉及到的资源**：PyPI（pufferlib 3.0.0）、GitHub PufferAI/PufferLib 与 PufferTank、PyTorch >=2.9/CUDA 工具链（4.0）、uv 包管理、可选 W&B/Neptune 日志服务、官方文档 puffer.ai、OpenReview/arXiv 论文。

### `stable-baselines3`
- **用途**：使用 Stable Baselines3（SB3 2.8.0）做标准单智能体强化学习：训练 PPO/SAC/DQN/TD3/DDPG/A2C 等算法、自定义环境、回调监控、评估与录像，适合快速原型与标准实验；明确把高性能并行/多智能体场景让位给 pufferlib。
- **技能设计**：单一 SKILL.md 按七大能力分节（训练、自定义环境、向量化、回调、持久化、评估、高级特性），近似"路由表"式按需引用 4 篇 `references/`（algorithms、custom_environments、callbacks、vectorized_envs）和 3 个可直接改用的 `scripts/` 模板（训练、评估、环境模板）。无严格阶段划分，属于实用教程型结构。
- **典型工作流**：定义观测/动作空间与奖励 → 按 `references/algorithms.md` 选算法 → 用模板创建/适配环境 → `check_env()` 验证 → 用 `train_rl_agent.py` 起训并挂 `EvalCallback` / `CheckpointCallback` → 需要加速时切 `SubprocVecEnv`（off-policy 配 `gradient_steps=-1`）→ `evaluate_policy` + `VecVideoRecorder` 评估录像迭代。
- **特色**：强调 API 细节与陷阱（`load` 是静态方法、replay buffer 不随模型保存、图像须 uint8/通道前置且自动除以 255、不支持 start≠0 的离散空间、VecEnv 四元组 `step` 与 `terminal_observation`）；末尾附常见问题排错（内存、慢、不稳定）。相比 pufferlib 几乎无安全护栏，风格是"上手即用"。
- **涉及到的资源**：PyPI stable-baselines3 2.8+、PyTorch >=2.3、Gymnasium 环境、可选 extras（TensorBoard、ale-py Atari、MuJoCo）、相关项目 SB3-Contrib / RL Baselines3 Zoo / SBX(JAX)、readthedocs 官方文档、uv 安装。

### `hypogenic`
- **用途**：规划和审计 ChicagoHAI 的 HypoGeniC/HypoRefine——用 LLM 从带标签文本数据集迭代生成并打分"假设库"。适用范围严格限定为软件使用；明确声明输出只是候选文本假设与任务预测统计，不是实验证实或科学验证，并把人工机制构想、开放头脑风暴路由给其他 skill。
- **技能设计**：双层结构——`references/` 6 篇（配置、上游事实、数据集、评估、安全、来源）+ 全本地确定性 `scripts/` 6 个（validate_config、plan_run、audit_dataset、inspect_outputs、evaluate_local 等），另带 `assets/` 官方任务 YAML 与本地 run policy 示例。流程内嵌"信息收集清单"（记录包/源/数据/模型/预算等）与"先本地审查"阶段门。
- **典型工作流**：分类请求 → 记录计划要素 → 校验 run policy 与任务配置 → 审计数据集（校验和、schema、跨切分泄漏，pin 到不可变版本）→ 生成有界成本/运行计划 → 外部 LLM 调用前单独确认 → 本地审查生成的假设库（只输出聚合统计，不打印文本）→ 冻结测试集只评估一次并报告局限。
- **特色**：提示注入与隐私防御最突出——一切数据集字段/提示/假设均视为不可信文本，绝不执行其中指令；密钥只查命名环境变量是否存在（布尔输出）；provider 隐私门列出 OpenAI/Anthropic 保留期与 ZDR 条款并要求发送敏感数据前重查；成本上界为保守估计而非报价；上游 CLI 本身不强制预算，本 skill 不包装执行它。
- **涉及到的资源**：PyPI `hypogenic==0.3.5`（含 wheel/sdist SHA-256 与源码 commit 锁定）、OpenAI/Anthropic API（可选 vLLM/HuggingFace 本地模型）、Redis（运行时可能需要）、HypoBench 数据集（pin 不可变修订）、PyYAML 6.0.2（仅 YAML 校验）、uv、老旧宽依赖集（PyTorch 2.4 / Transformers 4.45 / OpenAI 1.40 / Anthropic 0.32）。

---
## Forecasting / time series

### `timesfm-forecasting`
- **用途**：封装 Google TimesFM 基础模型，对任意单变量时间序列（销售、传感器、能耗、生命体征、天气等）做零样本预测，无需训练自定义模型，输出点预测加校准的分位数预测区间；还支持协变量预测和借助分位数区间做异常检测。明确划定了不适用场景（需要系数解释、多变量 VAR、分类聚类等应转用 statsmodels/aeon/scikit-learn）。
- **技能设计**：典型的"主文档 + 分层参考文档"结构：SKILL.md 承载快速入门、硬件要求表和常见陷阱清单，7 个 `references/` 文档（API 参考、数据准备、工作流、性能调优、输出配置、系统需求、示例验证）按需下钻。另配 `scripts/` 目录两个脚本：强制的 preflight 系统检查器 `check_system.py`（检查 RAM/GPU/磁盘/Python 版本/已装依赖，带 Mermaid 决策流程图）和端到端 `forecast_csv.py`。
- **典型工作流**：先跑 `check_system.py` 验证资源（RAM 低于 2GB 直接阻断，2-4GB 警告），再用 `uv pip install timesfm[torch]` 按硬件安装 PyTorch；然后 `from_pretrained` 加载模型、必须调用 `model.compile()` 配置 `ForecastConfig`（normalize_inputs、fix_quantile_crossing 等），最后把 1-D 数组列表传入 `forecast()`，从分位数输出推导置信区间。
- **特色**：核心亮点是"mandatory preflight"安全设计——在加载模型前就验证资源，防止 agent 搞垮用户机器；模型权重不入库，首次用时从 HuggingFace 按需下载并缓存。SKILL.md 还列了 10 条常见陷阱（忘记 compile、传 2-D 数组、正数推断误用于可负序列等），并给出各版本（2.5/2.0/1.0）的参数量/内存/上下文长度对照，推荐 200M 的 2.5 版（16K 上下文，远小于旧版 500M 模型的 32GB 内存需求）。
- **涉及资源**：Google Research 的 TimesFM 模型（HuggingFace checkpoints `google/timesfm-2.5-200m-pytorch` 等）；Python 库 timesfm[torch]/[flax]/[xreg]、PyTorch、numpy/pandas、statsmodels（对比基线）；模型下载依赖 HuggingFace Hub；另有 BigQuery 集成文档和 ICML 2024 论文引用。

---

## Local / host introspection

### `get-available-resources`
- **用途**：在资源敏感型本地任务运行前，探测当前进程**实际可用**的 CPU、内存、磁盘、调度器（Slurm）配额、容器 cgroup 限制和加速器情况，产出一份脱敏 JSON 快照，供 agent 做资源感知规划——核心原则是"宿主机可见 ≠ 可用"。
- **技能设计**：SKILL.md 顶部即"安全契约"（不做压力测试、不 dump 环境、不报主机名/路径/UUID、未知不等于无限），正文按 CPU/内存/加速器/磁盘/调度器/容器六个域给出语义解释规则，配套 3 个 `references/`（平台语义规则、schema 1.1 契约、带日期的官方来源台账）和 4 个 `scripts/`：`detect_resources.py`（快照采集）、`plan_workload.py`（确定性规划器）、`snapshot_tools.py`（校验/对比）、`accelerator_diagnostics.py`（只生成计划不执行的诊断）。
- **典型工作流**：运行 `detect_resources.py` 得到 stdout 快照（或显式写入私有权限的本地 JSON）；阅读快照时区分"宿主机总量 / cgroup 限制 / 调度器分配 / 有效容量"等不同事实层；再喂给 `plan_workload.py` 得到保守的 worker 数和内存建议；可用 `snapshot_tools.py` 前后对比资源状态变化。
- **特色**：极度保守与隐私优先的设计哲学——只读、固定参数元组、无 shell、短超时、有界输出；缺失观测一律记为 unknown，绝不推成 unlimited；GPU 只标记为"candidate"且 `runtime_usable_devices` 保持 null（可见性不证明权限/驱动/框架兼容）；部分探测失败不抹掉成功结果，warnings/provenance 记录来源；快照脱敏（无主机名、绝对路径、job ID、设备 UUID）。
- **涉及资源**：纯本地系统信息源（/proc、cgroup v2、sysctl、system_profiler、Slurm 环境变量、nvidia-smi/rocm-smi 等管理 CLI）；可选 psutil 7.2.2 增强；跨 Linux/macOS/Windows，无需网络服务。

### `autoskill`
- **用途**：把用户自己的工作流历史（由本地 screenpipe 守护进程被动录屏捕获）转化为新 skill——发现重复的研究工作流模式，与仓库中现有 135 个 skill 匹配，对未覆盖的模式起草新 skill 或"组合配方"（串联现有 skill）。仅响应用户显式请求（如"分析我最近一天并提议新 skill"）。
- **技能设计**：五段流水线脚本架构：`fetch_window.py`（从 screenpipe API 拉取并规范化时间线）→ `redact.py`（正则脱敏邮箱/API key/token/电话）→ `cluster.py`（按空闲间隙切分会话并聚类，本地完成）→ `match_skills.py`（读全部 SKILL.md frontmatter，用本地 sentence-transformers 嵌入做 top-k 余弦相似度匹配）→ `synthesize.py`（LLM 判官将每个簇分类为 reuse/compose/novel）。统一 CLI `autoskill.py` 提供 doctor/run/promote 三个子命令；`references/` 含 screenpipe 配置模板和 TLS 代理方案。
- **典型工作流**：① `doctor` 一键预检（config/skills 目录/screenpipe 可达/LLM 后端）；② `run --start --end` 跑流水线，产出到 `~/.autoskill/proposed/<ts>/`（报告 + 草稿，与 skills 仓库隔离，可用 `--dry-run` 停在聚类后不调 LLM）；③ 用户人工审查编辑 `report.md` 和草稿；④ `promote` 把确认的提案移入 `skills/<name>/`，拒绝覆盖同名 skill。
- **特色**：隐私设计是灵魂——敏感应用（密码管理器、银行、聊天）在 screenpipe 采集层就被 deny-list 排除，原始 OCR 永不出本机，只有脱敏后的簇摘要到达 LLM；LLM 后端默认本地 LM Studio（Gemma-4-31B-it），云端 Claude/Foundry 是显式 opt-in；提案必须经用户审查 + 显式 promote 才入库。"compose vs novel" 二分也很巧妙：能靠串联现有 skill 覆盖的就只生成薄配方文档，不从零造轮子。
- **涉及资源**：本地 screenpipe 守护进程（localhost:3030 HTTP API，需 SCREENPIPE_TOKEN，从 mediar-ai/screenpipe 源码构建）；LM Studio 本地 LLM 服务（localhost:1234）；sentence-transformers 的 all-MiniLM-L6-v2 嵌入模型（~80MB，首用下载）；可选 Anthropic API / Foundry 网关；Python 依赖 httpx/pyyaml。

---
## Mathematical biology & PKPD

### `pkpd-modeling`
- **用途**：面向药代动力学/药效学（PK/PD）建模与模拟的完整技能，覆盖非房室分析（NCA）、房室拟合、群体 PK、暴露-效应、生物等效性、异速放大与首次人体剂量（FIH）、药物相互作用（DDI）及贝叶斯治疗药物监测（TDM）。适用于分析浓度-时间数据、推导暴露参数、拟合模型或评估给药方案的场景。
- **技能设计**：采用"9 个即用脚本 + 15 份深度参考文档 + 2 份清单资产"的三层结构。每个脚本对应一个明确的分析问题（如 `nca.py` 回答"暴露参数是什么、末端相是否可信"），共享 `_models.py`（线性房室解析解及 MM/TMDD/间接效应模型）与 `_common.py` 两个私有模块。参考文档按主题分类（NCA 约定、结构模型、群体 PK、PBPK、监管指南账本、来源账本等），强调版本和来源溯源。
- **典型工作流**：按分析类型调用对应脚本：NCA 提取暴露参数并检查末端相拟合质量；房室拟合用 AIC/BIC/F 检验比较 1-3 房室模型并标记不可辨识参数；群体 PK 先用 `check_popk_dataset.py` 查数据集缺陷（如 NM-TRAN 把 BLQ 读作 0）；模拟给药方案并计算群体达标比例；最后是 BE、异速放大、DDI、贝叶斯 TDM 等专项分析。数据走 stdout、发现走 stderr，退出码 0/1/2 可用于流水线门控。
- **特色**：鲜明的"脚本只报告、不下结论"安全边界——不做生物等效性判定、不选剂量、不替代定量药理学专家和监管审评；TDM 明确为建模辅助，改药属临床医生决策。方法论上强调"收敛≠可辨识"（强制报告 RSE 和参数相关性）、区分结构错误 vs 权重错误、区分稳态 AUC(0-tau) 与 AUCinf 等。结尾列出"该技能旨在防止的 10 类错误"清单。
- **资源**：仅需 Python 3.11+ 与 numpy/scipy，无网络、无专有软件；生态文档指引 Pharmpy 2.1.1、NONMEM 7.6、nlmixr2、PK-Sim/MoBi 等（均由用户单独持证、脚本从不调用）。监管口径对齐 ICH M12/M13/E14、FDA 2005 MRSD 指南、EMA ABEL 规则等。

### `arbor`
- **用途**：基于 Arbor 论文的"自主优化（AO）"技能，在给定工件（代码/训练配方/agent harness/提示词）+ 可评分目标下，通过大量"实验-评估"循环迭代改进，且不过拟合反馈信号。典型场景：提模型评测分、优化 agent 通过率、MLE-bench/Kaggle 式提交优化、可自动打分的提示词优化。
- **技能设计**：核心是把研究状态持久化在一棵**假设树**（hypothesis tree）中而非对话历史里；每个节点绑定一个假设、其提炼的洞见、以及实现它的工件版本指针。架构上由长期存活的**协调者**（Claude 本体）持有树并决策搜索方向，短命的**执行者**子代理在隔离 git worktree 中各测一个假设。配套 `scripts/tree.py` 状态管理器负责建节点、写证据、传播洞见、剪枝、合并门和 Observe 投影。
- **典型工作流**：先钉死任务四元组 (M_0 初始工件, O 目标, E_dev 开发评估器, E_test 留出测试评估器)——dev/test 分离是防过拟合的核心机制；然后循环执行六步：Observe（重新从树而非记忆取上下文）→ Ideate（基于树的证据提出可证伪假设）→ Select（选择信息量大的实验）→ Dispatch（并行派发子代理到隔离 worktree）→ Backpropagate（把叶子观察抽象为方向级/全局约束向上传播）→ Decide（剪枝或过合并门）。
- **特色**：**held-out 合并门**只允许在从未参与搜索优化的 test 评估器上获胜的改进进入最终工件；执行者"假设绑定"契约——指标卡住时只许修代码不许改假设，否则证据语义崩坏。洞见传播被消融实验证明是收益主要来源（无洞见反馈的树比平铺队列还差）。失败被视为约束而非噪声，报告需诚实披露 dev 赢/test 输的差距。
- **资源**：无外部网络/云依赖，全靠本地 git worktree 隔离与 Agent 工具并行派发；方法学引用 Arbor 论文（Jin et al., 2026），另附独立 `arbor` CLI（RUC-NLPIR/Arbor）的安装运行说明。执行者简报、报告模板在 `references/` 中。

### `arboreto`
- **用途**：使用 Arboreto 库（Aerts Lab 出品）从基因表达数据推断基因调控网络（GRN），识别转录因子-靶基因调控关系。适用于 bulk RNA-seq 或单细胞 RNA-seq 数据分析，支持从本地多核到远程集群的大规模分布式计算。
- **技能设计**：结构精简——SKILL.md 承载快速上手、核心能力、常见用例与排错；3 份参考文档按能力划分：`basic_inference.md`（数据准备与标准推断流程）、`algorithms.md`（GRNBoost2 vs GENIE3 算法选择）、`distributed_computing.md`（Dask 本地/集群扩展）。另有一个即用脚本 `scripts/basic_grn_inference.py` 封装标准推断。相比其他技能，它是典型的"薄包装型"技能：内容围绕上游库 API 组织，不做领域再创造。
- **典型工作流**：uv/conda 安装 arboreto → 读入表达矩阵（DataFrame/ndarray/稀疏 csc，行=观测、列=基因）→ 可选加载 TF 名单做过滤 → 调 `grnboost2()`（推荐）或 `genie3()` 推断 → 输出 TF/target/importance 三列表 → 按 `limit=N` 或重要性阈值（如 >0.5）或按靶基因取 top 做过滤 → 与 pySCENIC 衔接做下游 ctx 剪枝和 AUCell。多条件比较时逐条件推断网络。
- **特色**：突出的工程性提醒——Dask 会 spawn 新进程，脚本**必须**加 `if __name__ == '__main__':` 保护；强调设 seed 保证可复现，并给出多 seed 共识网络（按 TF-target 对取均值重要性）的做法；记录了 pySCENIC 0.12+ 默认改用无 Dask 的多进程变体这一兼容性细节；排错表覆盖内存不足、速度慢、Dask 报错、空结果、稀疏数据五类。
- **资源**：依赖 PyPI 的 arboreto 0.1.6（2021 年后未更新）、dask[complete]/distributed/numpy/pandas/scikit-learn/scipy，或 Bioconda 渠道；主要下游消费者是 pySCENIC；文档在 arboreto.readthedocs.io，代码在 github.com/aertslab。

### `pyhealth`
- **用途**：用 PyHealth 构建临床/医疗深度学习流水线：加载 EHR/生理信号/医学影像数据集（MIMIC-III/IV、eICU、OMOP、SleepEDF、ChestX-ray14、EHRShot 等），定义预测任务（死亡、再入院、住院时长、用药推荐、睡眠分期、ICD 编码等），实例化模型并训练评估。也覆盖 ICD/ATC/NDC/RxNorm 医学编码查询与交叉映射。
- **技能设计**：围绕 PyHealth 的 **5 段式流水线** Dataset → Task → Model → Trainer → Metrics 组织，各段可替换、段间接口稳定。参考文档按流水线阶段路由：installation（环境与 MIMIC 访问）、datasets、tasks（任务-数据集映射）、models（架构选择）、medcode（编码工具）、examples（端到端配方），SKILL.md 内嵌一张"用户问什么 → 读哪个 reference"路由表。另附 `assets/starter_pipeline.py` 可复制启动模板（完整流水线 <20 行）。
- **典型工作流**：uv 创建项目并锁 Python 3.12 → 构造数据集（如 `MIMIC3Dataset`）→ `set_task(具体任务)` 得到 SampleDataset → `split_by_patient` 切分并建 DataLoader → 把 SampleDataset 传给模型（Transformer/RETAIN/GAMENet/SafeDrug 等）→ Trainer 训练（按任务类型选对 monitor，如二分类用 pr_auc）→ 用 `binary_metrics_fn` 等计算临床指标。
- **特色**：总结 6 条最易踩坑的"关键正确事项"——模型收 SampleDataset 而非 BaseDataset、必须按患者而非样本切分防泄漏、任务与数据集必须匹配（MIMIC3 任务不能用于 MIMIC-IV）、monitor 与任务类型对齐、MIMIC-IV 用 `ehr_root=`、设 `cache_dir=` 避免重复解析。风格上强调"最小惯用 PyHealth"，能用 Trainer 就别手写训练循环；演示可用官方合成 MIMIC-III 桶（GCS 公开，免凭证）。
- **资源**：依赖 PyHealth 2.0（Python ≥3.12,<3.14，uv 管理，自动带 PyTorch），旧线 1.16 支持 Python 3.9+；数据集涉及 MIMIC-III/IV（需 PhysioNet 凭证）、eICU、OMOP-CDM、SleepEDF/SHHS/ISRUC、TUEV/TUAB、COVID19-CXR、ChestX-ray14、EHRShot；合成数据托管在 storage.googleapis.com/pyhealth；文档 pyhealth.dev。

### `etetoolkit`
- **用途**：用 ETE 4 操作既有的系统发生树或其他层级树：Newick/Nexus 读写、拓扑编辑与模式匹配、Robinson-Foulds 比较、基因树进化事件（复制/物种形成）与调和、NCBI/GTDB 分类查询、SmartView 交互探索及出版级渲染。明确不用于从原始序列推断树——需先用 MAFFT/IQ-TREE 2 等比对推断后再载入 ETE。
- **技能设计**：锁定 ETE 4.4.0（2025-09-03 发布）为目标版本，结构为 SKILL.md（范围+快速上手+核心工作流+质量检查清单）+ 5 份按需加载的参考文档（api_reference、workflows、visualization、taxonomy、ete3→ete4 迁移）+ 2 个打包脚本（`tree_operations.py` 提供 stats/ascii/convert/reroot/prune/compare 子命令，`quick_visualize.py` 负责可视化）。强调"只为当前任务加载所需 reference"，不一次性灌入全部 API。
- **典型工作流**：`uv pip install ete4==4.4.0`（按需加 render-sm 或 treeview 可视化 extra）→ 打开文件对象读入 Newick（字符串留给 Newick 文本），**刻意选择 parser**（parser=1 保留内部节点名）→ 遍历/注释/剪枝/定根 → `robinson_foulds()` 比较拓扑或用 PhyloTree 检测复制/物种形成事件 → NCBITaxa/GTDBTaxa 查分类 → `explore()` 交互或 `render_sm()` 出 PNG。复杂操作可走脚本子命令，经 `uv run --with` 隔离运行。
- **特色**：专设"ETE 3 → ETE 4 破坏性变更"防护清单（`ete4` 包名、`parser=` 替代 `format=`、`props` 元数据、迭代器返回、`tree["name"]` 查找等），防止静默回退到 ETE 3 语法；并提醒 etetoolkit.org/docs/latest 实为 ETE 3 旧文档。7 条质量检查强调科学严谨：报告前确认 parser 保留预期标签、RF 比较需有意义的叶子名、声明有根/无根、多分枝解析仅是展示便利而非进化证据、记录版本与分类快照以复现。
- **资源**：Python 3.10+ 与 ete4 4.4.0（GPL-3.0）；NCBI/GTDB 分类设置和 SmartView 探索需网络访问且分类库占大量磁盘；静态 PNG 需 ete4[render-sm]、Qt PDF/SVG 需 ete4[treeview]；上游为 github.com/etetoolkit/ete 与 etetoolkit.github.io/ete 文档，无需凭证。

---
## Workflow & pipelines

### `nextflow`
- **用途**：用来端到端地构建、运行和调试 Nextflow 数据管线及 nf-core 社区工作流（如 rnaseq、sarek），覆盖从编写 `.nf` 脚本、DSL2 语法、samplesheet 输入，到配置执行器/容器、扩展到 HPC（SLURM）或云（AWS Batch、GCP、Azure、K8s）、失败恢复（`-resume`）的全链路场景，即使没明说 "Nextflow" 的可复现生物信息工作流也适用。
- **技能设计**：SKILL.md 本身是"概览 + 路由表"结构：先用一张 "Two Modes of Work" 表格按目标（运行/开发/配置/测试）分发到 7 个自包含的 `references/` 文件（language、configuration、containers、running-pipelines、nf-core-tools、developing、testing），无 `scripts/` 目录、无分阶段清单，全靠按需深读单个参考文件。
- **典型工作流**：安装 Nextflow（需 Java 17+）与 nf-core tools → 先用 `-profile test,docker` 小数据冒烟测试 → 真实运行时用 `-r` 锁定版本、选容器引擎、传 samplesheet CSV 和参数、加 `-resume` 复用缓存；开发路径则是 `nf-core pipelines create` 脚手架 → 复用/新建模块（meta.yml + nf-test）→ lint 后提交。
- **特色**：强调"test profile 优先、一切钉版本、每进程一容器"的科研复现纪律；区分单/双横线参数语义、`-resume` 缓存失效原理、meta map 元数据约定，并提醒写向前兼容的严格语法（Nextflow 26.04 起默认）。结尾要求引用 K-Dense 的 arXiv 论文。
- **涉及资源**：Nextflow 引擎与官网文档、nf-core 社区及模块库、nf-test、nf-core tools CLI、Docker/Singularity(Apptainer)/Podman/Conda/Wave 容器生态、SLURM 等调度器、AWS/GCP/Azure/K8s 云执行器、Seqera Platform、iGenomes 参考基因组。

### `datalad`
- **用途**：解决科研大数据的获取、版本化、来源记录（provenance）与发布问题：克隆/取回 OpenNeuro、DANDI、datasets.datalad.org 等超大数据集而无需下载全部字节，用 `datalad run` 记录每条产出的可复算命令，并发布到 GitHub + 存储远端；也指导何时该用 DataLad 而非普通 Git。
- **技能设计**：无路由表但按主题组织成 3 个 `references/`（data-access 数据访问、provenance 来源记录、publishing 发布），SKILL.md 主体是"核心心智模型 + 高频故障模式表"——把"指针不是数据"、"git-annex 分管大文件内容、Git 管结构与历史"这两个关键概念放在最前面反复强调。
- **典型工作流**：安装 datalad + git-annex → `datalad clone` → `datalad get` 取回所需文件内容 → `datalad run --input/--output "命令"` 生成带 JSON 运行记录的提交 → `datalad rerun`（可 `-b` 到新分支验证可复现性）→ `create-sibling-github` + `git annex initremote`（S3 等）+ `--publish-depends` 双远端发布。
- **特色**：大量篇幅给安全边界与故障诊断：7 行"症状—原因—修复"表（如文件读为空/损坏符号链接 → 需 `get`；写输出权限拒绝 → 需 `--output`；drop 拒绝执行以防止无第二副本丢数据）；`containers-run` 让容器镜像随数据集一起被追踪；明确区分 Git sibling 与 git-annex special remote 的发布语义陷阱。
- **涉及资源**：DataLad（Python 1.6.x）与 git-annex 10.x、datalad-container 扩展、Singularity/Apptainer 或 Docker、OpenNeuro/DANDI/datasets.datalad.org/registry.datalad.org 数据集源、GitHub 及 S3/WebDAV/SSH 存储远端、W3C PROV 导出、YODA/STAMPED 规范。

### `lamindb`
- **用途**：面向 LaminDB——一个"血缘原生"的生物学数据湖仓：让 scRNA-seq、空间转录组、流式细胞等数据集与模型变得可查询、可追溯、经验证、FAIR 合规，覆盖 artifact 注册、注释校验、本体标注、血缘追踪与工作流/ML 集成的完整场景。
- **技能设计**：SKILL.md 采用"六大能力域"结构，每个域（核心概念与血缘、数据管理与查询、注释与校验、本体、集成、安装部署）各对应一个 `references/` 文件并标注"Read this for..."，形成清晰的能力→文档路由；另设"Getting Started Checklist"分步入门清单（安装→概念→查询→校验→本体→集成）和 10 条 Key Principles。
- **典型工作流**：`uv pip install lamindb==2.5.1` 钉版本 → `lamin login` + `lamin init --storage` 初始化实例 → 代码中 `ln.track()` 开始追踪 → 用 `AnnDataCurator` 等 curator 校验/标准化数据（Bionty 本体映射）→ `save_artifact` 注册并 `features.set_values()` 注释 → 按 key/tissue/condition 过滤查询、流式加载 → `ln.finish()` 收尾。
- **特色**：独有的 Bionty 本体体系（Ensembl、CL、Uberon、Mondo、HPO 等 12+ 公共本体）驱动注释标准化；三档 schema 严格度（flexible/minimal/strict）；明确的安全默认：绝不输出 API 密钥/连接串，只检查环境变量是否存在、存疑数据必须先过 curator；推荐"本地 SQLite 起步、云端 Postgres 生产"的迁移路径。
- **涉及资源**：lamindb/bionty Python 库及 docs.lamin.ai、lamin.ai 账号体系、SQLite/Postgres/DuckDB、AWS S3/GCS/MinIO/Cloudflare R2/HuggingFace 存储、TileDB-SOMA/cellxgene、AnnData/MuData/SpatialData 生态、Nextflow/Snakemake/Redun 工作流、W&B/MLflow/Lightning/scVI-tools、Vitessce 可视化。

---## Knowledge & data services (external APIs)

### `exa-search`
- **用途**：基于 Exa 搜索 API 的网络研究工具包，面向科研/技术类查询，提供高质量网络搜索（关键词+语义混合检索）和 URL 内容提取（网页、文章、学术 PDF 批量抓取），适合需要学术信源过滤的文献调研场景。
- **技能设计**：典型的"路由表"结构——SKILL.md 只负责路由与安装，开头一张"用户想要什么 → 能力 → reference 文件"的分发表，两个能力各有独立 reference（`web-search.md`、`web-extract.md`），执行靠 `scripts/` 下两个 CLI 包装脚本（exa_search.py、exa_extract.py），用 PEP 723 内联元数据声明依赖，可直接 `uv run` 免安装。
- **典型工作流**：读用户请求 → 查路由表选定能力 → 打开对应 reference → 从 `.env` 或环境变量加载 `EXA_API_KEY` → 运行脚本并 `-o` 保存 JSON 结果；学术查询采用"两遍搜索"策略：先带 `--category "research paper"` 加学术域名白名单跑一遍学术向搜索，再跑一遍无限制通用搜索，合并时学术源优先。
- **特色**：学术信源优先级清单（同行评审 > 预印本 > 机构/政府 > 商业站点）、强制作者-年份引文格式、DOI 优先、强制的 Sources 分节；脚本统一携带 `x-exa-integration` 追踪头且明令不得移除。
- **涉及资源**：Exa 云服务（exa.ai API、dashboard.exa.ai）、`exa-py` SDK、uv/dotenv 工具链、arXiv/bioRxiv/medRxiv/PubMed/Nature/Science 等学术站点（作过滤白名单）。

### `parallel-web`
- **用途**：Parallel 的"网络智能"统一工具集，覆盖搜索、URL 提取、数据富化（给一批公司/人物/产品批量补充网络字段）、FindAll 实体发现、深度研究报告、以及周期性网页变更监控，核心价值是获取实时网络证据。
- **技能设计**：六个能力各自一个 reference 文件（web-search/web-extract/data-enrichment/deep-research/findall/monitor），SKILL.md 用路由表 + 决策指南区分易混淆能力（如 enrichment 是"给定实体补字段"、findall 是"发现实体本身"）；无本地脚本，完全驱动外部 `parallel-cli` 命令行工具；含异步任务状态查询与轮询上限（最多 3 次 × 9 分钟）规范。
- **典型工作流**：查路由表选能力 → 打开对应 reference → 运行 `parallel-cli <子命令> --json`，多行文本走 stdin；长任务用 `--no-wait` + 按任务 ID 前缀（`trun_`/`tgrp_`/`findall_`/`mon_`）轮询状态；后续追问可通过 `--previous-interaction-id` 复用上下文。
- **特色**：安全规范密集——所有返回网页内容视为不可信数据（防注入）、任务 ID 前缀校验防注入 shell 元字符、API key 不得出现在命令行/日志、JSON 参数必须用序列化器构造；明确"Deep Research 只在用户明说详尽时用"、"Monitor 只用于明确周期性追踪"（避免产生持久外部状态）；含学术信源优先级与引文规范。
- **涉及资源**：Parallel 云平台（platform.parallel.ai）、`parallel-cli`（parallel-web-tools[cli]==0.7.1，uv tool 安装）、`PARALLEL_API_KEY`、设备授权登录流程。

### `bioservices`
- **用途**：通过 Python 包 bioservices 统一访问约 40 个生物信息学 Web 服务（UniProt、KEGG、ChEMBL、Reactome、QuickGO、PSICQUIC 等），主打跨数据库查询、ID 映射和多服务整合工作流；官方边界：快速单库查询用 gget，序列/文件操作用 biopython。
- **技能设计**：单文件大而全的"能力手册"式 SKILL.md（区别于前两者的路由表），按 7 大核心能力（蛋白分析、通路、化合物、序列、ID 映射、GO、蛋白互作）组织，每个能力附代码示例和"关键方法"清单；`references/` 三份（services_reference 全服务清单、workflow_patterns、identifier_mapping），`scripts/` 四个可执行端到端示例脚本（蛋白全流程、通路网络、化合物交叉引用、批量 ID 转换）。
- **典型工作流**：先 `uv pip install bioservices==1.16.0`、导出 `NCBI_EMAIL` → 按能力选服务类（如 `UniProt().search()`）→ 异步任务（BLAST）需轮询 `getStatus` 再 `getResult` → 复杂整合直接跑或改造 `scripts/` 里的示例脚本（如 `protein_analysis_workflow.py`）。
- **特色**：大量版本陷阱标注（1.16.0 中 UniChem 旧辅助方法已删、PSICQUIC 缺失需防御性导入并回退 IntactComplex/OmniPath/STRING、ChEMBL 1.6 后方法改名），并给出 hasattr 探测式降级写法；强调外部分子先标准化、解析失败要检查 None；组织代码表（hsa/mmu/dme/sce）等实操约定。
- **涉及资源**：40+ 生信 Web API（UniProt、KEGG、Reactome、ChEBI/ChEMBL/PubChem/UniChem、PDB/Pfam、QuickGO、BioMart/ArrayExpress/ENA 等），NCBI BLAST（需联系邮箱），配套 BioPython/Pandas/PyMOL/NetworkX/Galaxy 生态。

### `gget`
- **用途**：命令行 + Python 双形态的基因组数据库快速查询工具，通过统一接口访问 20+ 数据库：基因信息、BLAST/BLAT、AlphaFold 结构、单细胞表达、富集分析、OpenTargets 疾病关联、COSMIC、病毒序列、小鼠组织特异性等，定位是"交互式探索和简单查询"。
- **技能设计**：SKILL.md 采用"六类 23 模块总目录表"+ 按需加载的分层 references——module_catalog（参数/返回形状目录）、module_reference（逐参数详解）、database_info（各库更新频率）、common_workflows/workflows（多模块流水线）；无自带脚本，直接教 agent 驱动 gget CLI。CLI 与 Python 参数命名有明确映射规律（`--census_version` ↔ `census_version=`）。
- **典型工作流**：建干净 venv 并锁版本 `gget==0.30.5` → 查模块目录表定位模块与类别 → 部分模块先 `gget setup`（alphafold/cellxgene/elm）→ 命令行跑（默认 JSON，`-csv` 转格式）或 Python 调用（返回 DataFrame/dict）→ 大查询用 `--limit`、`-o` 落盘保证可复现。
- **特色**：数据库持续变动的现实应对策略（适配器坏了查上游 release notes 后显式升级，可复现环境则锁死版本）；丰富实操细节——cellxgene 基因符号大小写敏感、AlphaFold 多聚体用 `-mr 20`、先查 PDB 再做预测、病毒下载支持 `--baseline`/`--merge-results` 断点续传、`gget info` 单次最多约 1000 个 Ensembl ID；凭据不写入示例/日志。
- **涉及资源**：Ensembl、Enrichr、ARCHS4、Bgee、CELLxGENE、OpenTargets、cBioPortal、COSMIC（需凭据）、AlphaFold DB、PDB/ELM/DIAMOND/MUSCLE、NCBI BLAST/BLAT，Python 3.8+（setup 模块建议 3.9/3.10）。

### `datamol`
- **用途**：RDKit 之上的 Pythonic 轻量封装层，面向标准药物发现化学信息学任务：SMILES 解析与标准化、分子描述符/指纹、聚类与多样性选择、3D 构象、骨架/片段分析、化学反应应用和并行批处理；返回原生 `rdkit.Chem.Mol` 对象保证生态兼容，需要精细控制时才直接用 RDKit。
- **技能设计**：SKILL.md 是"十区工作域总览表"（基础处理、文件读写、描述符、指纹相似性、聚类、骨架、片段、3D 构象、可视化、反应），每区详细代码在 `core_workflows.md`，另有六个按模块划分的 API reference（core_api/io/conformers/descriptors_viz/fragments_scaffolds/reactions_data）和三条端到端流水线（加载-过滤-分析、骨架 SAR、虚拟筛选）；无脚本，全部是 Python 库用法教学。
- **典型工作流**：`uv pip install datamol`（RDKit 自动装）→ `import datamol as dm` → 外源分子先 `dm.standardize_mol()` 标准化、`dm.to_mol()` 后检查 None → 批量操作加 `n_jobs=-1, progress=True` → 按需读对应 reference 里的工作域示例拼装流水线。
- **特色**：默认值"省心"哲学 + 全生态兼容（对象就是 RDKit Mol）；明确的规模边界提示（Butina 聚类约 1000 分子上限，更大用多样性选择）；ML 集成模式（指纹/描述符 → scikit-learn）；云 I/O 仅在用户要求时启用并确认远端写路径；指纹选型指南（ECFP 通用、MACCS 快、原子对考虑距离）。
- **涉及资源**：datamol（0.12.x）+ RDKit（自动依赖）、scipy/scikit-learn、fsspec + 可选 s3fs/gcsfs（S3/GCS 云存储 I/O）、本地计算为主（无外部 API）。

### `ncats-arax`
- **用途**：查询 NCATS Translator 的 ARAX 生产 API，做有界、强类型、带出处的生物医学知识图谱关系查询（一跳查询、两端固定的两跳查询），用于 Biolink 约束下的 RTX-KG2 查找和实体规范化；明确不用于推理、排序、开放式寻路或临床建议。
- **技能设计**：契约驱动设计——SKILL.md 主文件 + 两个 reference（`query-contract.md` 查询契约：查询形状/校验正则/固定操作/限额重试/端点策略；`output-schema.md` 输出解读），一个纯标准库的 `arax_client.py` 客户端脚本；本地只做"形状校验"，语义权威归 ARAX，不维护本地 Biolink 模型或 provider 注册表。
- **典型工作流**：preflight 验证生产端点身份与 TRAPI 版本 → `normalize` 独立规范化自由文本并人工审查 CURIE/类别（规范化永不自动链到查询）→ `one-hop`/`two-hop` 提交带类型和限定符的查询 → 检查 `summary.json`（有界绑定+出处）与 `response.json`（原始 TRAPI 载荷）→ 对重要路径在 ARAX 之外用文献和权威数据库验证。
- **特色**：安全边界极其严格——每次网络请求强制 `--acknowledge-public-query`（查询和调用方元数据可能公开，禁提交患者信息/未公开化合物/专有靶点假设）；"零结果 = 该约束下未返回，而非关系不存在"；结果顺序是未打分的响应顺序而非排名；禁止失败后静默改 provider 或展开顺序、禁止重试 POST；客户端刻意砍掉 raw query/workflow/overlay/Pathfinder/MCP 等所有"逃生舱口"。
- **涉及资源**：ARAX 生产 API（arax.transltr.io/api/arax/v1.4，ARAX 1.5.4 / TRAPI 1.5.0）、RTX-KG2 知识图谱、联邦模式可选 2-5 个 provider（如 molepro）、Biolink 模型、Translator Reasoner API 规范；仅需 Python 3.10+ 标准库，无 API key。

### `primekg`
- **用途**：查询 PrimeKG 精准医学知识图谱（整合 20+ 数据库、约 12.9 万节点、400 万边、29 种关系类型），支持基因/药物/疾病/表型的关联检索，服务于药物重定位、靶点发现、表型分析和网络药理学等场景。
- **技能设计**：极简单文件 + 单脚本结构——SKILL.md 本身就是全部说明，只有一个 `scripts/query_primekg.py`（基于 pandas 读 CSV 实现 `search_nodes`、`get_neighbors`、`get_disease_context` 三个函数），无 `references/` 路由表。
- **典型工作流**：先按名称+节点类型搜索实体拿到 ID → 用 ID 获取全部邻居（可按关系类型过滤）→ 或直接用高层函数 `get_disease_context` 一步拿到某疾病相关的基因、药物、表型汇总；最佳实践是"先看整体上下文再下钻"。
- **特色**：把哈佛的医学知识图谱封装成三函数式极简查询接口，降低了图查询门槛；数据路径通过 `PRIMEKG_DATA` 环境变量可配置；文档明确建议与 OpenTargets、Semantic Scholar 组合做多尺度证据整合。
- **涉及资源**：PrimeKG `kg.csv`（从 Harvard Dataverse 下载，本地 CSV 存储）、pandas 库；外部建议搭配 OpenTargets 与 Semantic Scholar。

### `database-lookup`
- **用途**：当一个科学、监管、金融等领域的事实必须从**具名的权威数据库**可复现地检索（而非凭模型知识推断）时使用，覆盖 80 个公共数据库 API，横跨物理天文、地学、化学药物、材料、基因组学、临床、专利、经济金融、社科人口九大领域。
- **技能设计**：路由型大技能——SKILL.md 是核心协议层（定义"检索契约"七步工作流、标识符格式速查表、POST-only API 清单、API key 表、限速表），`references/` 下 82 个文件 = 每库一个独立参考文件 + 一份 `database_selection_guide.md` 总路由表（按领域分组）+ `retrieval-contract.md` 检索契约模板。
- **典型工作流**：定义检索契约（实体、标识符、约束、是否穷尽）→ 按选择指南挑主库（不滥用多库扇出）→ 读对应参考文件 → 先规划服务端/本地过滤语义 → 有界、限速地调用 API → 穷尽式检索要先 count、翻页到数目对账 → 按"检索摘要 + 结果 + 溯源"固定格式输出（含端点、参数、日期、标识符转换、计数核对）。
- **特色**：把"可审计、可复现"作为第一原则（计数对账、fail-visible 而非 fail-plausible）；安全边界非常突出——外部响应当不可信数据、API key 只探测不外泄、查询构造防注入（拒绝字符串拼接、过滤控制字符/元字符）、超过 1 万条记录或 100 次调用需用户确认、付费库自动降级到免费替代。
- **涉及资源**：80 个公共数据库 REST/GraphQL API（PubChem、ChEMBL、UniProt、ClinVar、FRED、SEC EDGAR、NASA、USGS 等），约 18 个免费 API key，多平台 HTTP 工具（WebFetch/web_fetch/curl），`.env` 凭证管理。

### `usfiscaldata`
- **用途**：查询美国财政部 Fiscal Data REST API，获取联邦财务数据：国债（Debt to the Penny）、每日/每月财政报表、国债拍卖、利率、汇率、储蓄债券、政府收支等，共 54 个数据集、179 张表。
- **技能设计**：单一 API 的深度文档型技能——SKILL.md 精炼核心（参数表、关键端点表、响应结构），8 个 `references/` 文件按主题分层：api-basics / parameters / response-format 是 API 层，datasets-debt / fiscal / interest-rates / securities 是数据集层，examples 汇总 Python/R/pandas 代码。
- **典型工作流**：确定数据集端点（提醒端点路径会变，需在数据集页面核对）→ 用 `fields`/`filter`/`sort`/`page[size]` 组装查询 → 小结果单页取回（total-pages 为 1 时直接拉），多页则用 parameters.md 里的 `fetch_all()` 有界翻页辅助函数 → 结果转 DataFrame 分析。
- **特色**：完全免费开放、无需 API key；文档强调几个坑：所有值返回字符串需自行转换、空值是字符串 `"null"`、省略分组字段会触发自动聚合求和；过滤运算符简洁（`gte`/`in` 等，逗号分隔多条件）。
- **涉及资源**：`api.fiscaldata.treasury.gov` REST API、fiscaldata.treasury.gov 数据集浏览页、requests 和 pandas 库（uv pip 安装）。

### `adaptyv`
- **用途**：对接 Adaptyv Bio 云实验室（Foundry），把蛋白质序列提交给自动化湿实验平台做结合测定（BLI/SPR 亲和力/筛选）、热稳定性、表达量、荧光等实验，约 21 天返回实验数据——覆盖实验设计、成本估算、提交、状态跟踪和结果取回全流程。
- **技能设计**：API + SDK 双轨文档——SKILL.md 覆盖认证、SDK 两种用法（装饰器 `@lab.experiment` 与 `FoundryClient` 客户端）、实验类型表、九态生命周期状态机、序列格式、s-expression 过滤语法；一个 `references/api-endpoints.md` 收录全部 32 个端点的请求/响应 schema。
- **典型工作流**：搜索靶点（如 EGFR，可限自助服务）→ `cost_estimate` 预估费用 → `create` 建草稿实验（此时可加序列）→ `submit` 提交审阅 → 收到报价后确认 → 轮询或 webhook 跟踪状态直至 Done → `get_results` 取结果；自动化流水线可用 `skip_draft` + `auto_accept_quote` + webhook 一步到位。
- **特色**：商业实验平台的花费意识设计（草稿零成本、报价确认环节、成本预估先行）；Biscuit 加密衰减令牌支持按组织/资源/动作/有效期签发受限 token 且可级联吊销；API key 强制走环境变量 `ADAPTYV_API_KEY`，严禁硬编码；SDK 尚未上 PyPI，需从 GitHub 安装。
- **涉及资源**：Adaptyv Foundry API（foundry-api-public.adaptyvbio.com）、docs.adaptyvbio.com 官方文档与 OpenAPI spec、foundry.adaptyvbio.com 控制台账号、GitHub 上的 adaptyv-sdk（beta 0.1.0）、Python 3.10+、webhook 回调服务。

### `pi-agent`
- **用途**：安装、配置和二次开发 Pi——一个极简终端编码代理框架；既服务日常使用（CLI、provider 配置、本地模型），也服务在 Pi 上构建扩展、技能、包、主题，以及通过 SDK/RPC/JSON 流嵌入到其他应用，覆盖 pi-subagents（子代理编排）、pi-mcp-adapter（MCP 接入）、pi-interview（交互表单）、pi-web-access（网络访问）等生态包。
- **技能设计**：教科书式的"意图→参考文件"路由表——SKILL.md 开头就是 25+ 行的 First Decision 决策表，把每类用户意图映射到 32 个 `references/` 文件之一；SKILL.md 只保留路由、集成模式选型默认值、安全默认值和常用命令，细节全部下放参考文件。
- **典型工作流**：按用户意图查决策表锁定参考文件 → 读取该文件 → 按"Build-On-Pi Defaults"选集成模式：Node/TS 应用用 SDK（`createAgentSession()`）、非 Node 或需进程隔离用 RPC 模式（`pi --mode rpc --no-session`，注意只能按 `\n` 分行）、一次性管道用 JSON 模式、Pi 原生行为用扩展、跨机器复用用包。
- **特色**：明确的安全边界声明——Pi 本地运行且默认无沙箱，扩展/包/技能/shell 命令都拥有 Pi 进程权限，项目信任只管加载不管隔离，不受信仓库需用 Docker/VM/远程沙箱；秘密不入项目文件，走环境变量、`~/.pi/agent/auth.json` 或 OAuth；文档声明基于 Pi 0.84.2 全量官方文档页并注明精确行为以源码类型定义为准。
- **涉及资源**：Node.js ≥22.19 + npm、npm 包 `@earendil-works/pi-coding-agent`、GitHub 源码仓库（earendil-works/pi）、pi.dev 官方文档、各生态 npm 包（pi-subagents、pi-mcp-adapter、pi-interview、pi-web-access）、llama.cpp 本地模型路由、各家 LLM provider。

---## Lab platforms & ELN/LIMS

### `benchling-integration`
- **用途**：对接 Benchling 生命科学研发云平台，用官方 Python SDK（benchling-sdk 1.25.0）或 v2 REST API 编程管理生物序列（DNA/RNA/蛋白）、注册实体、库存、电子实验记录本（ELN）、工作流、Benchling Apps 及数据仓库查询，适合实验室数据自动化与外部系统同步场景。
- **技能设计**：采用"SKILL.md 总览 + references 按专题分层"的结构——core_capabilities.md 覆盖七大能力域（认证、注册实体、库存、笔记本、工作流、事件集成、数据仓库），另有 authentication/sdk_reference/api_endpoints/eventbridge 四份深度参考；SKILL.md 内嵌最佳实践代码片段（重试、分页、fields 助手）与常见用例，无 scripts 目录，是典型的"文档型"技能。
- **典型工作流**：配置认证（API Key 或 OAuth）→ 建立客户端（可自定义 RetryStrategy）→ 用生成器式分页遍历对象 → 通过 `fields()` 助手处理自定义 schema 字段 → 批量导入/导出、库存盘点或工作流任务自动更新，最后落盘 CSV 或数据库。
- **特色**：强调 SDK 的前向兼容（未知枚举值与多态类型被保留而非报错）、内存高效的生成器分页与 estimated_count 预估；安全边界包括绝不提交密钥、只读指定环境变量、网络请求仅路由到本租户 URL、最小权限原则，支持多环境（生产/预发）凭据分离。
- **涉及资源**：Benchling 云平台（租户 URL + API Key/OAuth 凭据）、benchling-sdk、Benchling Data Warehouse（SQL）、AWS EventBridge（事件订阅）、docs.benchling.com 官方文档。

### `labarchive-integration`
- **用途**：安全对接 LabArchives 电子实验记录本的类 REST ELN API 与 Inventory API v1，覆盖区域端点选择、HMAC 签名请求构造、用户授权 UID 流程，以及本地检查 LabArchives 容器归档文件；面向拥有 Enterprise 许可、需要自动化笔记/库存操作的研究机构。
- **技能设计**：SKILL.md 明确"不要混用接口"并区分三块（遗留 ELN API / Inventory v1 / 产品级集成），references 四份（api_reference、authentication_guide、integrations、sources）；scripts 三个工具均**刻意不联网**——setup_config.py 仅校验端点与命名环境变量，entry_operations.py 生成脱敏的签名请求计划（JSON plan），notebook_operations.py 不解压即可检查 LA 容器 ZIP。
- **典型工作流**：先用 `setup_config.py regions/check` 校验配置 → 写 API 代码前读 api_reference.md → 用 entry_operations.py 生成 ELN 或 Inventory 的脱敏干跑计划 → 远程写入前必须：核对官方方法页、生成干跑计划、确认目标区域/笔记本/可见影响、获显式批准、事后重读响应体确认（HTTP 200 不代表成功）。
- **特色**：详尽实现了两套不同的 HMAC-SHA-512 签名算法（ELN 走查询参数、Inventory 走 X-LabArchives-* 请求头，二者不可互换）；强调官方文档是"共享笔记本而非版本化 SDK"，任何操作前须立即核验当前页面；限速纪律（串行调用、批量间隔≥1 秒、4xx 不自动重试）、ZIP 安全检查（限制成员数/路径）。
- **涉及资源**：LabArchives ELN 与 Inventory API（五个区域 `*api.labarchives.com` 主机）、机构 Enterprise 许可与 API 权限、Python 3.11+ 标准库（无官方 SDK，明确警示勿默认安装过时的社区 `mcmero/labarchives-py`）。

### `lab-hardware-cad`
- **用途**：用 build123d 参数化 Python 代码设计自制实验室硬件（微流控芯片/模具、光机支架、酶标板/比色皿/离心管座、动物行为学装置、3D 打印夹具等），导出可加工的 STEP/STL/DXF，核心解决"零件必须与固定标准的设备精确配合"这一难题——0.5 mm 误差就装不进仪器。
- **技能设计**：四向路由表（microfluidics/optomechanics/labware-adapters/behavior-rigs 四个器件族参考，一次只加载一个）+ fabrication-limits、build123d-patterns、validation 等参考；assets/standards.json 内置 12 个标准 ID（SLAS 微孔板、比色皿、光学面包板、笼式系统、SM1 螺纹等）；scripts 提供 gen.py（生成+校验）、check.py（8 种检查子命令）、snapshot.py（六视图渲染）。
- **典型工作流**：8 步强制流程——路由到器件族 → 写代码前先确立每个配合界面的尺寸来源与公差（"绝不凭记忆写界面尺寸"，查 standards.json）→ 先选加工工艺再定几何 → 编写带 INTERFACE/DESIGN 参数块、`interfaces()` 与 `checks()` 声明的模型文件 → gen.py 生成并跑数值检查（facts/interfaces/geometry）→ **必须看快照 PNG 并报告看到什么** → 失败只改源码不补丁 STEP → 加工前按 validation.md 输出含工艺、材料、界面尺寸来源的完整报告。
- **特色**：最强安全与验证设计——模型声明式"通规/止规"量具（clear/material 区域从实际实体测量而非声明数字）；MMC（最大实体条件）配合理念（口袋按名义值+公差+间隙取尺寸，否则一半合格件装不进）；快照必看且承认渲染分辨率极限（0.3 mm 细节看不清就明说，改用 check.py bores/probe 量化）；模型文件会被执行，警示只跑可信来源的代码；单位统一毫米/度，警惕 25 mm 与 1 英寸网格差 1.6 mm 的累积错误。
- **涉及资源**：build123d 0.11.1（含 OpenCascade 内核）、matplotlib、ANSI/SLAS 微孔板标准、光学面包板/笼式系统/SM1 螺纹标准、本地 standards.json 数据库，全程无需联网。

### `omero-integration`
- **用途**：安全地检查和自动化 OMERO 显微成像数据服务器的工作流——用 omero-py/BlitzGateway、OMERO CLI 做受限的图像清单盘点、元数据导出、导入/导出规划，以及经过审查的写入操作；面向持有未发表图像等敏感数据的显微镜用户。
- **技能设计**：以"操作契约"（8 条硬规则）开局，之后按能力域分 9 份 references（connection、data_access、metadata、image_processing、rois、tables、scripts、advanced、sources）；scripts 四个本地规划器默认干跑、远程操作需 `--execute`，其中 validate_config 纯本地、inventory 有界只读、export_image_metadata 默认脱敏且不下载像素、plan_transfer 纯本地规划。
- **典型工作流**：先做本地校验/干跑（用户确认主机、组、对象类型、ID、结果上限后才允许连接）→ 搭建可复现 Python 3.12 环境（精确匹配安装 zeroc-ice 3.6.5 wheel 再装 omero-py 5.22.1）→ 凭据只从命名 OMERO_* 环境变量读 → 用异常安全的 BlitzGateway 模式（finally 关闭连接）做有界分页查询 → 远程操作前按"最终审查清单"确认服务器版本、目标 ID、数据能否离开服务器、输出路径不覆盖。
- **特色**：对凭据极严——密码/会话密钥绝不进命令行参数、代码、日志、输出 JSON，会话密钥视为 bearer 凭据；默认 secure=True 加密传输；所有列表/分页/ROI/注释/表格行都有硬上限，不得把单对象请求扩为全组导出；版本基线明确（OMERO.server 5.6.18 + omero-py 5.22.1 + IcePy 3.6.5，明确不能用 Ice 3.7），并提醒该固定仅为快照而非承诺。
- **涉及资源**：OMERO.server、omero-py/BlitzGateway、OMERO CLI（导入需 OMERODIR 指向服务器 Java 库）、OMERO.web api/webgateway API、ZeroC IcePy 3.6.5（GPL-2.0+）、OME 官方文档。

### `dnanexus-integration`
- **用途**：在 DNAnexus 基因组云平台上构建、运行和运维可复现的计算负载——涵盖 dx CLI 与 dxpy 自动化、文件/记录/项目管理、dxapp.json 定义的 app/applet、任务监控与成本控制、原生工作流及 WDL/CWL（dxCompiler）与 Nextflow 导入，适合生信流水线开发与项目自动化。
- **技能设计**："目标→先读哪份参考→首选接口"三列路由表（7 个任务路径映射到 app-development、configuration、data-operations、python-sdk、job-execution、workflow-languages、operations-and-troubleshooting 等参考，另有 authentication 与 sources）；scripts 含离线工具 validate_dxapp.py（结构/弃用项/过宽权限校验）和 inspect_dxpy.py（SDK 符号签名检查，不联网不认证）。
- **典型工作流**：隔离环境安装 dxpy 0.410.0 → `dx login` 认证 → 安全预检（dx whoami/pwd/ls，把项目名解析为不可变 project ID、检查文件 open/closed 与归档状态、用 `dx run -h` 看输入）→ 数据传输（小量用 dx upload/download，>50 MB 用 Upload Agent）→ dx-app-wizard 建 applet、离线校验 dxapp.json、dx build → 带 --cost-limit 的受控启动 → dx find/watch 监控，用 `get_output_ref()` 作业链避免轮询。
- **特色**：面向"可能暴露受监管数据、删除不可变对象、产生费用"的 8 条操作契约——先只读后变更、计费/删除/权限变更须逐项确认、绝不打印 DX_SECURITY_CONTEXT 或跑 `dx env`（会泄露令牌）、不凭非唯一名推断删除目标、尊重 PHI/TRE 限制；还沉淀了平台新知：Ubuntu 24.04 执行环境、弃用的 systemRequirements 位置、`find_data_objects` 默认精确名匹配需显式 `name_mode="glob"`、job- 与 analysis- ID 区分等。
- **涉及资源**：DNAnexus 云平台、dx CLI 与 dxpy 0.410.0、dxCompiler 2.17.0、Upload/Download Agent、Nextflow、WDL/CWL，部分功能需组织许可。

### `latchbio-integration`
- **用途**：在 Latch 生信云平台上创建、注册、调试和运行工作流——包括 Python SDK 任务图、Nextflow/Snakemake 打包、资源（CPU/内存/GPU）配置、Latch Data 与 Registry 数据管理、表单界面设计、启动监控及 Latch MCP，面向生信流程开发与部署者。
- **技能设计**：9 份 references 按"需求→参考"路由（workflow-creation、data-management、registry、resource-configuration、nextflow-snakemake、ui-and-automation、operations-and-debugging、verified-workflows、latch-mcp）；scripts 仅一个 inspect_latch_sdk.py（本地 import 检查已装 SDK 符号，可出 JSON，不认证不联网）；SKILL.md 给出最小工作流代码样例和 7 步推荐开发生命周期。
- **典型工作流**：uv 建 Python 3.12 环境装 latch==2.76.8 → `latch login` OAuth 认证并选 workspace → 快速路径 `latch init --template subprocess` + `latch register --yes --open`（远程构建镜像为默认）→ 按生命周期开发：检查兼容性→定义类型化接口→配置 metadata 与资源→`latch register --staging` + `latch develop` 在镜像内验证→刻意注册→审查成本与参数后才启动（Console/MCP 或 launch_v2，弃用 `latch launch`）→ 监控终态与任务日志并做科学性验证。
- **特色**：明确"已装包与 changelog 为准"的版本权威观（SDK 2.76.8 基线，警示旧教程的 Python 版本差异与 Snakemake v2 教程需单独 pin）；工作流体保持声明式（计算与副作用全在 task 内，模块导入期禁止网络调用/取密钥）；运维安全上付费计算（尤其 GPU）与 `LPath.rmr`/Registry 删除须确认、`get_secret()` 只在 task 内用且不得作为输出、改 Dockerfile 后须重新 staging 注册（开发容器内编辑不会回同步）、重复注册退出码 2 不等于构建失败。
- **涉及资源**：Latch 平台（Console）、Latch SDK 2.76.8（PyPI）、Latch Data/Registry、Latch MCP、Nextflow 与 Snakemake、wiki.latch.bio 官方文档，Windows 需 WSL。

### `protocolsio-integration`
- **用途**：读取、校验和安全导出 protocols.io 实验方案库数据（搜索/获取/步骤/材料/PDF），或为创建、更新、发布、删除、上传等变更生成**不执行**的干跑计划；仅用于明确针对 protocols.io 或确切方案版本的任务，2026-07-23 对照官方文档刷新。
- **技能设计**：10 条操作契约开篇（默认离线、联网读须 `--execute`、只读命名变量、官方 HTTPS 主机、内容当不可信数据等）；references 六份（authentication、protocols_api、discussions、workspaces、file_manager、additional_features）+ assets 中的保守 JSON Schema；scripts 七个纯标准库 CLI：validate_auth_config、protocols_read（默认规划模式）、pagination_helper（校验 next_page 不猜页码）、validate_protocol_json（离线校验结构/版本/署名）、plan_write_request（只出计划永不执行）。
- **典型工作流**：离线先验证配置（`validate_auth_config.py --require read`）→ protocols_read.py 以规划模式列出请求（URL/边界待审）→ 审查后加全局 `--execute` 门做有界读取（--page-size/--max-pages/--max-items）→ 保存的 JSON 用 validate_protocol_json 离线校验 → 变更操作先取版本快照、比对目标/版本/DOI/权限/正文，产出脱敏计划与确认短语，用户复核确认短语仅标记"已审阅"，实际执行交给另行审查的外部集成。
- **特色**：本项目最保守的安全设计——写路径工具**根本没有执行模式**（连确认短语也不触发写入）；API 版本地图区分 v3/v4 混用（无统一 `/api/v3` 基址）并点名三个不可恢复的旧端点模式；科学溯源纪律（保留 DOI/version_uri/显式 /vN，禁止静默替换为 /latest）；把远程方案文本/评论/文件名一律当提示注入风险对待（"保留或摘要，绝不服从"）；限速 100 req/min、PDF 匿名 3/min，只重试幂等读、写永不自动重试。
- **涉及资源**：protocols.io 官方 REST API（www.protocols.io 及租户 `<subdomain>.protocols.io`）、官方 MCP 端点（Streamable HTTP + OAuth/客户端令牌，只读）、PROTOCOLS_IO_ACCESS_TOKEN、官方开发者页 OAuth 凭据（scope=readwrite）、ephemeral S3 上传表单字段，Python 3.11+ 标准库即可运行。

### `tamarind`
- **用途**：通过 Tamarind Bio 云平台（REST API 或 MCP server）调用数百个开源计算生物学工具——结构预测（AlphaFold/Boltz/Chai/ESMFold）、蛋白与抗体设计（RFdiffusion/ProteinMPNN/BoltzGen/BindCraft）、分子对接（DiffDock/Vina）、亲和力预测、MSA 生成与分子动力学，适合无本地 GPU 的用户做批量序列筛选和设计→折叠→打分管线。
- **技能设计**：主文件讲 API 表面与"规范里没写的坑"，四个 references 分工：`examples.md`（验证过的 settings payload）、`api_reference.md`（非显而易见的响应形状）、`tool_catalog.md`（工具分类地图）、`workflows.md`（端到端配方）。核心结构是强制"发现→取 schema→validateJob→提交→轮询→取结果"六步循环，并反复强调"运行时枚举、永不硬编码工具名"。
- **典型工作流**：认证（x-api-key）→ `GET /tools` 或 MCP `getAvailableTools` 发现工具 → `getJobSchema` 拿参数模式 → `validateJob` 干跑校验 → `submitJob`/`submitBatch` → 轮询 `JobStatus`/`batchStatus` → 两步式下载结果 zip；链式任务用 `JobName/path` 引用上游输出，或 `submitBatch(fromJob=...)` 一步完成设计→折叠。
- **特色**：强调活体规范源（llms.txt、openapi.yaml、live discovery）优于任何静态拷贝；明确无官方 Python SDK（PyPI 的 `tamarind` 是无关的 Neo4j 包）；大量 foot-gun 提示（文件引用必须用裸文件名不能带 email 前缀、批任务要轮询父任务的 batchStatus 而非子任务）；要求提交前向用户呈现影响成本/结果的选择而非静默默认。
- **涉及资源**：app.tamarind.bio（REST API + UI）、mcp.tamarind.bio（MCP server）、S3 预签名上传、docs.tamarind.bio；Python `requests` + `python-dotenv`；需 `TAMARIND_API_KEY`，每用户 10 次免费任务。

### `rowan`
- **用途**：Rowan 是云原生分子建模与药物化学工作流平台（Python API），用于 pKa/macropKa 预测、构象/互变异构体搜索、对接与类似物对接、蛋白-配体共折叠、MSA 生成、分子动力学、ADMET 性质预测，面向需要可扩展基础设施、不想自建 HPC/GPU 的批量药物筛选和多步化学管线。
- **技能设计**：主文件覆盖快速上手、输入格式规范（SMILES vs Molecule 对象的分工作流差异）、结果类型系统、项目/文件夹组织，以及三棵决策树（pKa vs macropKa、构象 vs 互变异构、docking vs analogue vs cofolding）。五个 references：workflow_catalog（全部工作流类型）、batch_and_webhooks、access_and_pricing、end_to_end_example（完整先导优化战役）、troubleshooting。
- **典型工作流**：`pip install rowan-python` → 设置 `ROWAN_API_KEY` → `rowan.Molecule.from_smiles()` 构造分子 → 用专用函数（如 `submit_descriptors_workflow`）提交 → `result()` 阻塞等待（<5 分钟任务）或 `stream_result()` 流式获取（长任务）→ 用类型化属性（`result.descriptors`）取结果，`.data` 兜底；非平凡战役用 project/folder 组织，>50 个工作流用 webhook。
- **特色**：强调"能跑得通的三步模式"和结果对象的双访问模式（便捷属性 vs 原始 dict）；给出经过实测验证的具体陷阱（如 DescriptorsResult 没有 molecular_weight 属性、MW 是精确质量、MW/TPSA 与 TopoPSA 的区别）；明确不适用场景（简单 I/O 用 RDKit、post-HF 量子化学不做）；示例对 rowan-python 3.1.13 逐值验证。
- **涉及资源**：Rowan 云平台（rowan.ai）、`rowan-python` SDK（Python 3.12+）、RDKit（用于 SMILES 预校验）、FastAPI（webhook 示例）；需 `ROWAN_API_KEY`，按 credit 计费。

### `ginkgo-cloud-lab`
- **用途**：在 Ginkgo Bioworks 云实验室（cloud.ginkgo.bio）提交湿实验协议——蛋白表达与纯化（无细胞/E. coli/Pichia）、HiBiT/A280/LabChip 定量、IVT mRNA/circRNA 合成、热位移/可开发性 assay、Echo-MS 酶活检测、SPR 靶点接入、荧光像素画——面向想远程执行自动化实验但无自建设施的科研用户，覆盖选型、输入准备、定价到下单。
- **技能设计**：主文件是一张"路由表"：按表达系统/检测类型分四大类，17 个协议各一行表格（名称→readout→价格→周期→认证状态），每行链接到独立 references 文件（一个协议一个文件）；附"选协议决策指引"（按目标倒推协议）。frontmatter 限定 `allowed-tools: Read`，纯读型技能。
- **典型工作流**：按需求在目录中选协议（如快速可表达性筛选选 $39 的 cell-free HiBiT）→ 点开对应 reference 读输入要求 → 到 cloud.ginkgo.bio/protocols 配置参数 → 下载输入模板并上传 FASTA/CSV/XLSX → 填特殊要求 → 提交邮箱并同意条款 → 加入购物车获得可行性报告与报价；非目录内协议用 EstiMate AI 聊天（自然语言描述→评估定价）。
- **特色**：把"实验室即服务"整个产品目录结构化成可导航的决策文档，价格/周期/认证状态（Certified/Beta）直接内联，使代理能替用户做成本权衡；EstiMate 是独特出口——自然语言定义自定义协议；不涉及任何编程，纯 Web 下单流程。
- **涉及资源**：Ginkgo Cloud Lab（cloud.ginkgo.bio）、RACs 自动化机器人+磁悬浮传输+Nebula 波士顿设施、70+ 集成仪器（Agilent Bravo、Beckman Echo、Revvity LabChip、Nicoya Alto SPR、SciEx Echo-MS 等）、Catalyst 编排软件；账号或机构访问需联系 cloud@ginkgo.bio。

### `waypoint-bio`
- **用途**：使用 Outpost Bio 开源的微生物组基础模型——Waypoint 检查点（6M/45M/170M GPT-2 架构）、Atlas 预训练语料（53.9 万 MGnify 样本）、Compass 八任务基准——做样本嵌入、表型微调、基准评测、预训练，以及把 MetaPhlAn/Kraken2/QIIME 2 丰度表转成 waypoint 格式。核心思想："微生物组样本是一个句子，每个分类单元是一个 token"。
- **技能设计**：主文件按六步工作流组织（格式转换→词表覆盖检查→嵌入→微调→基准→预训练），并设重量级"Scientific caveats"一节。references 四件（cli-reference/compass-benchmark/data-preparation/python-api），scripts 两个实用工具：`profiler_to_waypoint.py`（profiler 输出转换）和 `vocab_coverage.py`（词表覆盖诊断）——"先查覆盖再信任何下游数字"是设计上的强制关卡。
- **典型工作流**：`pip install waypoint-bio` → HF 逐仓库点开访问授权 + `HF_TOKEN` → 用转换脚本或 `prepare-dataset` 生成 waypoint 格式 parquet（Taxa 列须完整谱系）→ `vocab_coverage.py` 检查（中位丰度加权覆盖 <0.8 要重新审视）→ `embed` 出固定维度向量，或 `finetune`（分类/回归，相关样本必须设 `split_column` 防泄漏）→ `benchmark` 或 `pretrain`。
- **特色**：科学边界写得极硬：低于 ~1000 标注样本不如随机森林（论文交叉点在 1 万）、词表外分类单元被静默丢弃、45M 而非 170M 是最佳基准模型、属级 tokenization 折叠种级差异、成分数据与批次效应警示、明确"非临床诊断工具"；默认 pooling=last_token 与预训练一致的细节也讲清。
- **涉及资源**：HuggingFace gated 仓库（outpost-bio/Waypoint-*、Atlas、Compass，需 `HF_TOKEN`）、GitHub Outpost-Bio/waypoint、bioRxiv 论文、Slack 社区；`waypoint-bio` 包拉取 torch/transformers/datasets/peft/sklearn；预训练与基准强烈建议 GPU。

### `modal`
- **用途**：在 Modal 无服务器云平台上按需运行 Python（含 GPU T4→B200），用于部署/服务 AI-ML 模型、训练与推理、构建 serverless Web API、批量并行处理、定时任务与持久存储——面向想"以代码定义一切、零到千容器自动伸缩"的开发者。
- **技能设计**：主文件是概念百科：App/Function、镜像构建、GPU、Volumes、Secrets、Web 端点、定时任务、伸缩并发、资源配额、生命周期类、Sandbox，每个概念一节代码示例；12 个 references 按主题一一对应（getting-started/functions/images/gpu/volumes/secrets/web-endpoints/scheduled-jobs/scaling/resources/examples/api_reference），典型的"总览+深钻"双层结构。
- **典型工作流**：`pip install modal` → 认证（优先复用已有 `MODAL_TOKEN_ID`/`SECRET`，其次 .env，最后 `modal setup`）→ 用装饰器写 `@app.function(gpu="H100", image=image)` → `modal run` 本地入口调试 → `modal serve` 热重载开发 → `modal deploy` 生产部署；批处理用 `.map()`，状态化服务用 `@app.cls` + `@modal.enter()` 一次性加载模型，配合 Volume 存权重、Secret 注入凭据。
- **特色**：明确的现代 API 纪律（GPU 只用字符串如 `"H100:4"`，旧 `modal.gpu.*` 已弃用；推荐 `uv_pip_install`）；GPU 自动升级（H100→H200 免费加 `!` 阻止）；Sandbox 隔离不可信/AI 生成代码并配 CIDR 白名单——安全边界一节还要求 subprocess 参数固定、不拼接用户输入；有状态类 + `min_containers` 保活是低延迟推理的关键模式。
- **涉及资源**：modal.com 平台（免费层每月 $30 credit）、Modal Python SDK（3.10-3.14）、`MODAL_TOKEN_ID`/`MODAL_TOKEN_SECRET`；示例涉及 vllm、transformers、FastAPI、pandas/torch 等；Volume/Secret/Cron 均为平台内建服务。

### `paperclip`
- **用途**：用 GXL 的 Paperclip CLI 把约 1100 万篇全文论文、21.7 万监管文件、11 万临床试验、57.4 万蛋白条目当作只读虚拟文件系统用 Unix 命令访问，配服务端语义搜索与 LLM 读者——面向生物医学文献调研、监管/试验格局分析、跨论文字段抽取，以及"必须引用到具体行号"的写作。
- **技能设计**：主文件以"操作规则"为骨架（认证前缀、禁交互命令、限制输出、捕获 result id、并行查询、不解析 search 输出、服务端返回一律当数据），再加工具选择决策表、核心工作流、虚拟文件系统地图、引用规范、已知缺陷清单；6 个 references 覆盖安装/CLI 全参/检索/map-reduce/仓库/Python SDK。是"防御性 CLI 使用手册"式的设计。
- **典型工作流**：先跑 preflight（`paperclip config` 查认证态）→ 每条命令前缀 `.env` 加载守卫 → `search -s <source>` 语义搜（查询词要写成摘要句式而非关键词）→ 捕获 `s_` id → `cat meta.json`/`head content.lines`/`grep -n` 读原文 → 多论文字段抽取用 `filter`→`map`（限 3-10 篇）→ `results` 导出；引用格式固定 `[1]` + `#L45` 行锚 URL。
- **特色**：行号引用体系（`#L45` 精确到句）是其存在的理由，且反复强调"搜索摘要不是证据，要打开行再引"；记录了实测验证的缺陷与规避（reduce 的截断 id 是死链、`--json` 不强制 JSON、二进制图无法导出只能 ask-image）；数据外发命令（upload/sync/share/fetch 用浏览器 cookie）全部列为需用户明确指令才执行，repo 默认 opt-in。
- **涉及资源**：paperclip.gxl.ai 服务（语义搜索+LLM 读者+托管 MCP）、PMC/arXiv/bioRxiv/medRxiv、FDA/PMDA/EMA、ClinicalTrials.gov 等多区注册库、UniProt/PDB/ChEMBL；CLI 装在 ~/.paperclip（仅 macOS/Linux，Windows 用 MCP）；`PAPERCLIP_API_KEY` 认证；`gxl_paperclip` Python SDK。

### `paperzilla`
- **用途**：让代理直接对话 Paperzilla 平台上的科研项目、论文推荐与经典论文：拉取某项目最新推荐、解释推荐理由、把经典论文取回为 markdown 并总结、反馈推荐（赞/踩/星）、导出 JSON 或 Atom 订阅源——面向个人化文献追踪与阅读场景。
- **技能设计**：七个 skill 中最轻的一个——单文件 SKILL.md，无 references/scripts，自述"核心技能，只给数据访问能力，不强加工作流"。结构为：能问什么 → 访问方式（`pz` CLI）→ 各平台安装 → 更新 → 认证 → 命令参考 → 输出与自动化提示 → 配置，本质是一份紧凑的 CLI 备忘录。
- **典型工作流**：安装 `pz`（brew/scoop/Linux 官方指南）→ `pz login` → `pz project list` 找项目 → `pz feed <project-id>` 浏览推荐（可加 `--must-read --since --limit`）→ `pz paper <id> --markdown` 取论文全文 → `pz feedback <id> upvote|downvote --reason` 反馈 → `--json` 机器解析或 `--atom` 生成订阅 URL。
- **特色**：反馈闭环（upvote/downvote/star 带原因码）会回显在 feed 中（`[↑][↓][★]` 标记）；markdown 是惰性生成——`pz paper --markdown` 只在已准备好时返回，`pz rec --markdown` 会排队并提示重试；`pz update` 能按安装方式自动检测并升级，歧义时可显式指定安装来源。
- **涉及资源**：Paperzilla 云服务（paperzilla.ai，可用 `PZ_API_URL` 覆盖）、`pz` CLI（Go 1.23+，GitHub paperzilla-ai/pz）、brew/scoop 分发渠道、docs.paperzilla.ai 文档与快速上手指南。

---## Data formats & I/O & output

### `docx`
- **用途**：创建、读取、编辑 Word 文档（.docx/.dotx），覆盖生成带格式的报告/信函/模板、查找替换、插图、修订追踪（redline）与批注，以及从 Word 文件中抽取重组内容。
- **技能设计**：核心是一张"任务→方法"路由表（创建用 docx-js、编辑用解包 XML、读取用 pandoc），正文浓缩为大量"footgun 清单"（页面尺寸、表格双宽度、列表编号、TOC 等易错点）；`scripts/` 提供 merge_runs、comment、accept_changes、validate、soffice 封装等工具，无独立 `references/` 目录，全部知识内联在单文件里。
- **典型工作流**：新建时直接写 Node 脚本调用预装的 docx 库；编辑时 unzip → 清除符号链接 → merge_runs 合并碎片 run → 原地改 document.xml → 重新 zip → XSD 校验；生成后用 LibreOffice 转 PDF 再 pdftoppm 转图目检；批注用 comment.py 生成六个关联文件并插入锚点标记。
- **特色**：深刻理解 OOXML 内部结构（Word 把文本拆成多个 run 导致字符串不可直接查找，用 merge_runs 解决）；修订追踪校验用 --author 检测未标记的改动；把外部来件 docx 视为不可信（删符号链接）；明确 pandoc 与自研脚本在"接受删除段落标记"上的不同缺陷。
- **涉及资源**：docx（npm）、pandoc、LibreOffice、Poppler（pdftoppm），均为本地工具，无云服务。

### `pdf`
- **用途**：PDF 全生命周期操作：读取/抽取文本与表格、合并拆分、旋转、加水印、新建 PDF、填表单、加解密、抽图、扫描件 OCR。
- **技能设计**：单文件教程式结构，按工具库分章节（pypdf 基础操作、pdfplumber 文本表格、reportlab 创建、命令行 qpdf/pdftk/pdftotext），末尾附"任务→最佳工具"速查表；深度内容外置到 reference.md（pypdfium2、pdf-lib JS 库）和 forms.md（表单填写专用指引），主文件只留快速上手路径。
- **典型工作流**：先按速查表选工具——提取文本用 pdfplumber、合并拆分用 pypdf 或 qpdf、建新 PDF 用 reportlab 的 Canvas/Platypus；扫描件走 pdf2image 转图 + pytesseract OCR；表格抽取结果可直接转 pandas DataFrame 再导出 Excel。
- **特色**：强调"同一任务多条路径"（Python 库 vs 命令行），并对 reportlab 给出关键陷阱：禁止用 Unicode 上下标字符（内置字体缺字形会渲染成黑块），必须用 `<sub>`/`<super>` 标签；无云依赖、全部本地完成。
- **涉及资源**：pypdf、pdfplumber、reportlab、pandas、pytesseract、pdf2image（Python 库），poppler-utils、qpdf、pdftk（CLI），以及 reference.md 中的 pypdfium2 和 pdf-lib。

### `pptx`
- **用途**：涉及 .pptx/.potx 的一切操作——创建演示文稿、解析提取内容、编辑既有 deck、基于模板填充、增删重排幻灯片、演讲者备注与批注。
- **技能设计**：任务路由表 + 三层脚本体系（thumbnail 网格选版式、add_slide 复制幻灯片并处理包注册、clean 清理孤儿部件、office/validate 做 XSD 校验）；大篇幅 pptxgenjs footgun 清单（颜色格式、图表轴声明、负阴影会损坏文件等）+ 一整节设计美学指南（配色表、版式、字体安全清单、反 AI 味清单）；QA 分内容/文件/视觉三道关卡且为必做项。
- **典型工作流**：新建时写 pptxgenjs 脚本（预设 layout、安全字体、原生图表）；模板流程为 thumbnail 选版式 → markitdown 提内容映射 → 解包后先做结构操作再改内容 → clean → 重打包 → validate（带 --original 基线化模板固有错误）→ 转 PDF/图片逐页视觉 QA。
- **特色**：明确列出"会产出 PowerPoint 拒开但其他工具都接受"的损坏文件模式（次轴缺 catAxes、堆叠图 outEnd 标签等）；字体按"QA 可信度"分级，指出 LibreOffice 预览与真实 PowerPoint 渲染差异；反 AI 生成痕迹清单（禁止标题下划线、装饰色条）；视觉 QA 建议用子代理以新鲜眼光看图。
- **涉及资源**：pptxgenjs、react-icons/react/react-dom/sharp（npm，用于图标渲染），markitdown、Pillow、defusedxml、lxml（pip），LibreOffice、Poppler。

### `xlsx`
- **用途**：以 Excel 工作簿（.xlsx/.xlsm/.xltx，含 csv/tsv 输出）为交付物的创建、编辑、分析——公式、格式化、财务模型、多 sheet 工作簿。
- **技能设计**：任务路由表（创建编辑用 openpyxl、批量数据用 pandas、快速浏览用 markitdown、读模型需两次加载）；核心是"产出硬性要求"清单（零公式错误、公式不硬编码、假设必须显性记录出处）+ 强制重算机制 + 公式兼容性分级表（Excel 2007 函数 / 需 _xlfn 前缀的六个 / 绝对禁用的动态数组函数）；附金融建模颜色与数字格式惯例。
- **典型工作流**：写 openpyxl 脚本生成文件（公式写表达式而非计算结果）→ 运行 scripts/recalc.py 让 LibreOffice 就地重算并返回 JSON 错误报告 → 修复错误单元格直至 status 为 success → 先写 2-3 条公式验证取值正确再铺满网格；编辑既有文件时严格匹配原文件惯例、只在指定输入单元格写入。
- **特色**：解决 openpyxl 写公式无缓存值导致一切读取器看到 None 的核心痛点；揭示 LibreOffice 与 Excel 函数集差异（外链公式被重算销毁、动态数组函数截断且报零错误）；强调"重算通过只证明公式可求值，不证明公式正确"；错误归属判定方法（对比原始文件）。
- **涉及资源**：openpyxl、pandas、markitdown（pip），LibreOffice（soffice，经脚本封装适配沙箱），纯本地无云服务。

### `markdown-mermaid-writing`
- **用途**：为科研与技术文档建立"Markdown + 内嵌 Mermaid 图"这一默认规范——报告、分析、README、决策记录等任何要进 git 的产出，结构关系图一律 Mermaid 优先。
- **技能设计**：庞大的分形知识库：两份风格指南（markdown ~733 行、mermaid ~458 行）+ 24 种图型各一个独立参考文件（各含范例、技巧、可复制模板）+ 9 种文档模板 + 完整示例报告；SKILL.md 本身是路由层：文档类型→模板表、用例→图型表、五步工作流、常见坑。明确"三阶段"哲学：Phase 1 Mermaid 文本永远必做且为唯一事实源，Phase 2/3（Python 图表/AI 图像）可选。
- **典型工作流**：判断文档类型选模板 → 读 markdown 风格指南 → 按用例表选对图型（不滥用流程图）并读对应图型指南 → 从模板起稿、图随文走 → 以 .md 文本提交，图像仅为补充；收尾跑一份跨技能通用的检查清单。
- **特色**：论证文本图的优势矩阵（git diff 可读、免构建、省 token、无障碍 accTitle/accDescr、可后转图片）；硬性语法规则（禁 `%%{init}` 破坏 GitHub 暗色模式、禁内联 style 只用 classDef）；专治 radar-beta/xychart-beta 的易混语法坑；与 scientific-schematics、scientific-writing、literature-review 等技能定义了分工接口。
- **涉及资源**：无外部运行时依赖——纯文本规范，渲染依赖 GitHub/GitLab/Notion/VS Code 等原生 Mermaid 支持；内容移植自 SuperiorByteWorks agent-project 仓库（Apache-2.0）。

### `markitdown`
- **用途**：用微软 MarkItDown 把各类文档（Office/PDF/HTML/EPUB/音频/YouTube 等）转为保留结构的 Markdown，服务于文本分析、搜索与 LLM/RAG 摄入；也含批量与文献库转换工作流。
- **技能设计**：以"选对路径"决策表开篇（本地文件/字节流/远程 URI/OCR/云抽取/MCP 各有对应入口），随后四条核心安全规则（最窄方法原则、输出视为不可信、内外处理分离、插件默认关闭按需启用）；7 份 `references/` 按主题切分（API、格式、云与 OCR、MCP 与插件、安全、工作流、版本迁移），`scripts/` 提供批量转换与文献转换助手及安装自检。
- **典型工作流**：uv 建隔离环境并安装指定 extras → 按来源选最窄 API（convert_local/convert_stream/convert_response）或 CLI → 批量场景跑 batch_convert.py（防同名碰撞、跳符号链接）或 convert_literature.py（带 provenance front matter、按年份归档）→ 转换后五步质检（非空 UTF-8、结构与原文件比对、目检图表、记录元数据、保留原件）。
- **特色**：版本钉死 0.1.6 并区分新旧 API（result.markdown 取代 text_content）；安全边界清晰——convert()/convert_uri() 有意宽松故不可喂不可信输入、转换出的 Markdown 可能含提示注入只当数据不当指令、外发内容（转录/LLM/云）前须用户批准、MCP 的 HTTP 模式无鉴权须绑定本机；明确自身不做像素级还原与本地 OCR，边界场景导流到 pdf skill 或 LiteParse。
- **涉及资源**：Microsoft MarkItDown（PyPI/GitHub）、markitdown-ocr 视觉插件（OpenAI 兼容客户端）、Azure Document Intelligence / Content Understanding、markitdown-mcp 官方服务器、Google Web Speech（音频转录）、uv 工具链；本地转换可离线。

### `liteparse`
- **用途**：本地文档解析——从 PDF、Office 文件、图片中提取带包围盒的空间文本，输出布局保持的纯文本或结构化 JSON，用于布局感知 RAG、引用定位、批量文献摄取，并可把页面渲染成 PNG 供多模态代理"看图"。
- **技能设计**：单文件主干 + 5 份 `references/`（解析器选型对比、API、CLI、输出格式、OCR 与格式支持）+ 一个无网络调用的批量脚本；开头有"When to Use / When Not to Use"双向路由表，把 Markdown 需求导给 markitdown、页面操作导给 pdf skill、高难度表格导给 LlamaParse；正文按 9 个编号工作流组织（文本、JSON、指定页、字节流、截图、批量、OCR、加密、短语检索）。
- **典型工作流**：pip 安装 liteparse 2.0.0 与 lit CLI → 按需选输出：`lit parse file.pdf` 得布局文本，`--format json` 得逐项坐标/字体/置信度；截图用 screenshot 子命令配合 DPI 控制；批量用 batch-parse 并行 OCR；性能上对原生数字 PDF 加 --no-ocr、用 target_pages 只解析方法章节、num_workers 扩并行。
- **特色**：三大差异点全部围绕"本地 + 空间信息"：逐 token 包围盒、页面栅格输出、零云 API（内置 Tesseract，支持离线 TESSDATA_PREFIX 与自建 HTTP OCR 服务器）；search_items 可按短语合并相邻项返回联合包围盒，便于坐标与截图配对；Office/图片输入统一先内部转 PDF 再解析。
- **涉及资源**：liteparse（Rust 核心，Python/Node 绑定，GitHub run-llama、PyPI、npm @llamaindex/liteparse）、捆绑 Tesseract、可选 LibreOffice 与 ImageMagick；文档链向 LlamaParse 云服务但本技能不依赖它。

### `latex-posters`
- **用途**：用 LaTeX 制作学术会议研究海报（beamerposter/tikzposter/baposter 三包），覆盖 A0 等尺寸规格、多栏布局、图表整合与打印交付，也用于把论文转成海报格式。
- **技能设计**：SKILL.md 是六阶段主工作流骨架，深度内容全部下沉到 8 份 `references/`（AI 图形规则、LaTeX 包对比、排版质检、内容模式与演讲日指南、布局、设计原则、内容指南）；`scripts/` 含 generate_schematic.py（AI 示意图生成）与 review_poster.sh（校验）；`assets/` 提供三包多套现成模板与配色/机构模板。frontmatter 声明可选的 OpenRouter API key。
- **典型工作流**：阶段一定需求与内容大纲（1-3 条核心信息、300-800 词）选包 → 阶段二用 AI 先生成全部主要视觉元素（每图一个信息、巨字号、大量留白）并过两道审查门 → 阶段三选模板定分栏与字号层级 → 阶段四组装页眉、插图与极简文字、加 QR 码 → 阶段五缩小打印模拟远距离可读性测试 → 阶段六 pdflatex/lualatex 编译、grep overfull 查溢出、准备印刷与补充材料。
- **特色**：核心理念是"AI 视觉优先"——海报面积 60-70% 应为 AI 生成图、文字仅 30-40%；给出一整套硬性数值上限（每图 3-4 元素、≤10 词、≥50% 留白、正文 ≥24pt、关键数字 ≥120pt）；两道强制审查门（生成前检查拆分必要性、生成后 25% 缩放两秒可懂测试）；把 overfull 溢出定义为错误而非警告。
- **涉及资源**：LaTeX 发行版与 beamerposter/tikzposter/baposter 等包（tlmgr/MiKTeX）、scientific-schematics 或 Nano Banana Pro（AI 图形，经 OpenRouter）、pdflatex/lualatex 编译器；与 scientific-writing、literature-review、data-analysis 等技能协同。

### `infographics`
- **用途**：用 AI 生成专业信息图——数据统计、时间线、流程、对比、列表、地理、层级、解剖、简历、社媒等 10 类，面向报告/营销/演示等需要快速清晰传达复杂内容的场景。
- **技能设计**：以"生成-评审-精炼"闭环为核心架构：Nano Banana Pro 生成 → Gemini 3.6 Flash 按文档类型阈值评分 → 达标即停（省 API 调用）、不达标则依据批评改进提示词重生成；SKILL.md 提供类型/行业风格/色盲安全调色板的速查表与提示词工程建议，细节下沉到 `references/`（类型目录、设计原则、调色板、迭代精炼机制），单脚本 generate_infographic.py 承载全部 CLI 能力。
- **典型工作流**：可选 --research 先用 Perplexity Sonar 收集 2023-2026 的新近事实与统计（落盘 research.json 并注入提示词）→ 写具体、含数据点、指明视觉元素的提示词 → 指定 --type/--style/--palette 与 --doc-type 阈值运行脚本 → 产出带版本号的图片与含评分、批评、提前停止信息的评审日志 → 按 checklist 复查、必要时加迭代次数重生成。
- **特色**：按用途分级质量阈值（营销 8.5 最高、草稿 6.5 最低）而非一刀切；智能迭代只在低于阈值时才重新生成；内置 Wong/IBM/Tol 色盲安全调色板与 8 种行业风格；清晰的边界划分——技术流程图、生物通路、神经网络架构、CONSORT/PRISMA 图导流给 scientific-schematics。
- **涉及资源**：Nano Banana Pro（图像生成）、Gemini 3.6 Flash（质量评审）、Perplexity Sonar Pro（研究检索），三者经 OpenRouter API key（OPENROUTER_API_KEY）访问；Python 脚本运行环境。

### `generate-image`
- **用途**：通过 OpenRouter Image API 一个统一入口调用 Gemini、Seedream、Recraft、GPT-Image、Riverflow 等约三十个图像模型，生成/编辑照片、插画、概念图、logo，并支持基于参考图的合成；技术类示意图明确分流给 scientific-schematics。
- **技能设计**：单脚本 `generate_image.py`（纯标准库）+ `references/models.md` 模型目录；SKILL.md 内置"按需求选模型"的路由表、逐模型参数差异速查表和完整 flag 表，结构扁平、以表格为主要知识载体。
- **典型工作流**：解析 API key（参数→环境变量→.env）→（可选）`--list-models`/`--model-info` 查目录 → 写五要素 prompt（主体/媒介/光色/构图/规避项）→ `--dry-run` 校验 → 生成 → 回读检查图像；编辑用 `-i` 传参考图描述增量修改。
- **特色**：计费前"预检"——脚本先对照实时目录校验参数，不支持的参数本地秒级报错并打印合法值，不花钱；计费全有或全无；反复强调"生成图不是证据"、模型写不可靠文字、敏感数据不要上传、迭代先用便宜低分辨率模型。
- **涉及资源**：openrouter.ai（API key、按请求计费）、各上游图像模型、可选 `.env` 本地配置。

### `scientific-slides`
- **用途**：制作科研口头报告（会议 5-20 分钟、研讨会 45-60 分钟、答辩、基金路演、journal club 等），产出 PDF 幻灯片或 PowerPoint，兼顾结构、设计、时间与视觉校验。
- **技能设计**：核心脚本 `generate_slide_image.py`（AI 生成整页幻灯）+ `slides_to_pdf.py`；大量 `references/` 分主题文档（结构、设计原则、数据可视化、Beamer 指南、prompt 写法、脚本参考、常见坑）+ `assets/` 三套 Beamer 模板与设计/计时指南；强调与其他 skill（research-lookup、PPTX、schematics）的集成。
- **典型工作流**：先用文献检索定引用 → 逐页写详细计划（标题/要点/视觉元素）→ 用 AI 逐页生成：每条 prompt 附 FORMATTING GOAL、引用、并 `--attach` 上一页保持风格连贯，结果页强制挂真实数据图 → 合成 PDF → 人工审查迭代 → 计时演练。
- **特色**："格式一致性协议"（风格目标进每个 prompt + 附加前一页作风格锚点）是关键设计；视觉优先、反对满页文字；引用直接烙进生成的幻灯片；PPT 路线则用 `--visual-only` 只生成图、文字由 PPTX skill 另行排版。
- **涉及资源**：OpenRouter API（Nano Banana Pro 等生成模型，可选 key）、LaTeX Beamer、PPTX skill、research-lookup skill、Python/Pillow 等 PDF 组装工具。

### `scientific-schematics`
- **用途**：用自然语言生成出版级科研示意图——神经网络结构、系统架构、CONSORT/PRISMA 流程图、电路图、生物信号通路等；无需写代码、模板或手工绘制。
- **技能设计**：单脚本 `generate_schematic.py` 实现"生成-评审-精炼"回路：Nano Banana 2 出图，Gemini 3.6 Flash 按文档类型阈值打分（journal 8.5 → presentation 6.5），达标即停，不达标改 prompt 重生成（上限 2 轮）；`references/` 收录回路细节、prompt 工程与最佳实践。
- **典型工作流**：设 OPENROUTER_API_KEY → 用具体、含组件/流向/标签的 prompt 调脚本并指定 `--doc-type` → 后台自动生成、评审、迭代 → 产出版本化图片（`_v1`/`_v2`）、胜者副本和 `*_review_log.json`（分数、批评意见、早停原因）→ 人工按 checklist 核对标签拼写、科学正确性、无障碍性。
- **特色**：按文档类型分级质量阈值 + "达标即停"省 API 费；评审失败不编造分数（记 `score: null` 并明示未验证）；诚实声明局限：仅 PNG 位图、无矢量/DPI 控制，期刊需 PDF/TIFF 要自行转换；prompt 和数据会离开本机，禁止描述未发表数据。
- **涉及资源**：OpenRouter（Nano Banana 2 生成 + Gemini 3.6 Flash 评审）、Python requests 库、Nature/Science/CONSORT 等外部出版规范链接。

### `scientific-visualization`
- **用途**：用 Matplotlib/Seaborn/Plotly 制作与审查"真实、无障碍、可发表"的科研图表——图设计、多面板布局、不确定性与缺失数据展示、色彩对比审查、图像元数据校验、期刊导出规划。
- **技能设计**：六步工作流主线（定义证据与去向→选诚实编码→无障碍内建→受作用域样式实现→显式导出并记溯源→检查评审）+ 五个网络无关的 CLI（image_metadata、palette_audit、export_plan、style_preview、style_presets/figure_export）+ `assets/` 样式文件与色板、publisher_profiles.json 期刊快照；`references/` 按主题分层。
- **典型工作流**：先记录受众/期刊/变量语义/变换/缺失值等"证据清单"→ 选择基线/不确定性/缺失/对数轴等诚实编码 → 用 style_context 作用域样式写图 → export_figure 显式导出（dpi、格式、溯源 manifest）→ 用 CLI 审元数据、对比度、出版商规则 → 最终尺寸人工复核。
- **特色**：以"科研诚信护栏"开篇——禁止改动/隐藏/发明数据、禁止臆测期刊要求、禁止把自动报告当成合规证明；编码诚实性规则细致（条形须含零基线、双轴慎用、面积按面积缩放等）；全部工具确定性、离线、拒符号链接、不隐式覆盖。
- **涉及资源**：Matplotlib 3.11.1、Seaborn 0.13.2、Plotly 6.9.0、Kaleido 1.3.0（需本机 Chrome）、Pillow、pypdf、uv（全部版本钉死、日期化快照）；无网络依赖。

### `venue-templates`
- **用途**：准备期刊论文、会议论文、科研海报、基金申请书时，提供按 venue 的格式指引与捆绑 LaTeX 脚手架——选官方模板、核对页数/匿名规则、按 venue 调整文风、检查投稿 PDF。
- **技能设计**：核心是"验证优先"工作流 + 按需求路由的 `references/` 路由表（期刊/会议/海报/基金/各文风/审稿人期望共 11 个文档）+ 有意只捆绑少量资产的 scaffolds（Nature/PLOS/NeurIPS/Elsevier、NSF/NIH、beamerposter）+ 三个 helper 脚本（query_template、customize_template、validate_format）。
- **典型工作流**：先精确解析目标（venue/年份/track/类型/阶段）→ 查对应 reference → 写"合规笔记"记录官方来源 URL 与核查日期 → 从官方模板起步（捆绑 scaffold 仅作草稿）→ 机械+人工双重验证页数、字体、匿名、必需声明 → 提交前再看官方页面。
- **特色**："强制时效性规则"是灵魂——绝不靠改旧文件名年份猜当前样式文件、绝不把通用 scaffold 说成官方模板；区分初始投稿/修改/camera-ready 三阶段规则；PDF 校验脚本只能查页数与嵌入字体，明确不证明边距合规。
- **涉及资源**：各期刊/会议/资助机构官网（NeurIPS、Elsevier、NSF、NIH 等）、LaTeX 与 Poppler 命令行工具（可选）、SciENcv 等官方表单系统；helper 脚本本身本地运行。

### `market-research-reports`
- **用途**：构建"证据可追溯"的市场研究报告与假设驱动的市场规模测算/预测——市场定义、行业与客户证据、竞争格局、TAM/SAM/SOM 对账、预测敏感性、可审计报告脚手架；面向需要可审计决策依据的场景。
- **技能设计**：十步工作流 + 双台账核心（来源台账 S-xxx 与声明台账 C-xxx，每条声明映射到确切来源 ID）+ 七个纯标准库、离线、有界的 CLI（验证台账、审计声明引用、确定性测算、预测敏感性、竞争矩阵、单位一致性、生成报告工作区）+ 模板资产与七个 `references/`（报告结构、证据模型、官方数据源路由、方法与伦理等）。
- **典型工作流**：先立"研究契约"（口径、地域、期间、货币、分类体系）→ 按八级证据优先级路由来源 → 建来源台账 → 建声明台账并跑引用审计 → 自上而下与自下而上独立算 TAM、按情景推 SAM/SOM → 带显式不确定性的预测与敏感性 → 单位归一 → 起草并过发布门（release gate）。
- **特色**：反"咨询腔"红线明确——禁止冒充分析机构、禁止编造引用/市场份额、禁止把 TAM/预测说成唯一真相、禁止投资建议；测量护栏防重复计数（制造商收入不与终端支出相加、存量和流量不混）；不确定就写 `unknown` 而不是硬凑；脚本零网络零 LLM 调用。
- **涉及资源**：官方统计机构/监管披露/公司申报等一手来源（World Bank、IMF、OECD、Eurostat 等，需用户批准的网络访问）、各公共 API（条款为日期化快照）、可选 XeLaTeX/LuaLaTeX 模板。

### `pptx-posters`
- **用途**：仅当交付物是"可编辑的 PowerPoint 科研海报"时使用——从作者批准的本地内容与资产生成单页无宏 .pptx，并做物理尺寸、打印、无障碍、溯源、包安全五类检查。
- **技能设计**：严格的"清单驱动"架构：先谈需求（裁切/出血/安全边距/画布/打印尺度）→ 填写故意留无效的 manifest 模板（每个元素/资产都要源 ID、作者核验+批准、本地路径+SHA-256、alt 文本）→ 审资产/色板/导出计划 → 生成 → 双重终审（包安全+布局）→ 人工 PowerPoint 无障碍门 → 导出打印。七个 CLI 分管验证、生成、包检查、布局、资产盘点、色板、导出计划。
- **典型工作流**：钉死 python-pptx==1.0.2/Pillow==12.3.0/lxml==6.1.1 → 记录会议与打印要求 → 建 manifest 并让作者按内容哈希绑定批准 → `inventory_images`/`check_palette`/`plan_export` 预检 → `generate_poster.py` 生成 → `inspect_pptx.py`+`check_layout.py` 终审 → PowerPoint 内跑 Check Accessibility、验 QR、作者签发 → 导 PDF 并打印小样。
- **特色**：八条"硬门"fail-closed——内容未定、宏/外部关系、远程资产、未经哈希批准等任一不满足即停，绝不编造或留占位符；生成器全新空白演示、不加载用户模板、不嵌字体/媒体/链接；包检查器只读 ZIP/XML 绝不执行；有效 DPI 按像素÷实际英寸而非元数据。
- **涉及资源**：python-pptx、Pillow、lxml（精确版本钉死）、uv；全程离线无 API key；PowerPoint 本体与打印机校样为人工环节。

### `matplotlib`
- **用途**：Python 底层绘图库的完整使用指引——各类图表（线/散点/柱/直方/热图/等高线/箱线/小提琴）、多面板、3D、样式定制、多格式导出；适合需要细粒度控制、自创图型或集成到科研工作流的场景，与 seaborn（快速统计图）、plotly（交互）、scientific-visualization（期刊级成图）形成分工。
- **技能设计**：经典教程式结构——概念（Figure/Axes/Artist/Axis 层级、pyplot vs OO 双接口）→ 六个常见工作流 → 最佳实践 → 常见坑；`references/` 四份文档（plot_types、styling_guide、api_reference、common_issues）+ `scripts/` 两个助手（plot_template 模板、style_configurator 交互式样式生成）。
- **典型工作流**：`uv add matplotlib` 安装 → 用 OO 接口 `fig, ax = plt.subplots(figsize=...)` → 按数据类型选 plot 函数 → rcParams/样式表定制 → 按媒介选 DPI 导出 PNG/PDF/SVG（出版 300 dpi、web 150）；Jupyter 用 `%matplotlib widget`（ipympl）。
- **特色**：明确推荐 OO 接口替代有状态 pyplot（避免状态机混乱）；colormap 选择讲感知均匀性（禁 rainbow/jet）、无障碍（viridis/cividis、加纹理冗余编码）；性能提示（rasterized、blitting、显式 close figure）；无自己的安全边界设计，属常规库技能。
- **涉及资源**：Matplotlib 3.10.x（Python 3.10+、NumPy 1.23+）、uv、NumPy/Pandas、ipympl/pyside6（可选交互）、matplotlib.org 官方文档/画廊/cheatsheet（联网查阅）。

### `seaborn`
- **用途**：基于 matplotlib+pandas 的统计可视化——快速探索分布、变量关系、分类比较，自带聚合/置信区间等统计估计与出版级默认美观主题；定位为"快速统计图"，交互图转 plotly、期刊级排版转 scientific-visualization。
- **技能设计**：文档式结构——设计哲学（面向数据集/语义映射/统计感知）→ 双接口（传统函数接口 vs 0.13 的 seaborn.objects 声明式接口，后者标注为实验性但可用）→ 0.12/0.13 API 变更要点（errorbar 取代 ci、关键词传参、palette 需配 hue 等）→ 长表/宽表数据结构要求；`references/` 七份文档覆盖函数总表、网格、色板、排错、objects 接口、示例。
- **典型工作流**：`uv pip install seaborn==0.13.2`（需高级统计再装 `[stats]`）→ pandas 整理成长表 tidy 数据 → 按变量类型选图（连续×连续用 scatter/line/kde/reg，连续×分类用 violin/box，相关用 heatmap/clustermap）→ hue/size/style 语义映射 → figure-level 函数做分面 → 与 matplotlib 无缝微调 → 300 dpi 导出。
- **特色**：对 0.12→0.13 破坏性变更的迁移提示是该 skill 最实用的部分；明确私密/离线数据应本地 pandas 加载而非 `sns.load_dataset()`（其会联网下载公开示例数据）；生产代码建议保守用函数接口，objects 接口仅在组合式 API 明显简化时使用。
- **涉及资源**：seaborn 0.13.2（Python 3.8+）、必依赖 NumPy/pandas/matplotlib、可选 scipy/statsmodels/fastcluster、uv；`sns.load_dataset()` 涉及在线示例数据集下载。

---## Geospatial, earth & astronomy

### `geopandas`
- **用途**：面向直接使用 GeoPandas `GeoSeries`/`GeoDataFrame` 的 Python 平面矢量数据工作流（空间运算、矢量 I/O），重点是保证结果正确、可复现、可审计。目标是稳定版 1.1.4，并包含从 0.14 迁移的检查清单。
- **技能设计**：SKILL.md 本身承担"方法论 + 正确性门禁"的角色，下设 6 篇 references（数据结构、CRS、几何操作、空间分析、I/O、可视化）按 API 主题路由；scripts/ 提供 6 个本地 CLI（盘点、CRS 重投影计划、有效性审计、空间连接审计、导出契约、隐私发布门禁）。核心是一个 8 步"正确性门禁"（来源/几何状态/CRS 语义/单位/变换质量/拓扑精度/基数/输出契约），每个 reference 文件与之对应。
- **典型工作流**：先用 `vector_inventory.py` 做脱敏技术盘点，经 8 道正确性门禁核查（如 CRS 必须显式、地理坐标不得直接算距离/面积），再按需调用审计 CLI（如 `spatial_join_audit.py` 验证谓词语义与连接基数），最后用 `export_plan.py` 生成非执行的导出契约并重新打开验证输出。
- **特色**：安全/隐私契约非常突出——把精确坐标、地址、轨迹视为敏感数据，禁止自动加载 URL/云路径/压缩包（扩展名白名单），PostGIS 密码只走命名环境变量；明确反蛙跳式（antiloomidian）与反日期变更线陷阱（区分 `set_crs` 与 `to_crs`）；所有 CLI 无网络、确定性、输出 JSON 不含坐标。原生 GDAL/GEOS/PROJ 依赖被定义为信任边界。
- **涉及资源**：GeoPandas 1.1.4 + 全套钉版依赖（NumPy、pandas、Shapely、pyproj、pyogrio、pyarrow），GeoParquet/GeoPackage/PostGIS（SQLAlchemy），PROJ 网格默认禁网，引用 PyPI/GitHub/geopandas.org 官方源。

### `geomaster`
- **用途**：全领域地球空间科学"百科全书式"技能，覆盖遥感、GIS、空间分析、对地观测 ML、30+ 科学领域（海洋、水文、农业等），从基础矢量/栅格操作到 SAR、点云、网络分析、云原生工作流。
- **技能设计**：SKILL.md 是自足的速查手册（安装、Quick Start、核心概念、常见操作、性能/最佳实践），再路由到 14 篇 references（坐标系、核心库、遥感、ML、GIS 软件、科学领域、高级 GIS、大数据、行业应用、8 种编程语言、数据源、排错、500+ 代码示例）。没有分阶段流程，也没有信息收集清单，是"目录 + 示例驱动"结构。
- **典型工作流**：conda/uv 装好栈后，按需抄示例：如 Sentinel-2 算 NDVI、GeoPandas 空间连接统计、Earth Engine 时间序列、DEM 地形分析、OSMnx 路网最短路、随机森林影像分类、STAC + Planetary Computer 云原生取数。
- **特色**：广度即特色（8 语言、70+ 主题、500+ 示例），并强调通用最佳实践（CRS 检查、投影 CRS 测量、GeoPackage 优先、云掩膜、保留 lineage）。相比 geopandas，几乎无安全/审计门禁，风格更像教程型知识库而非流程管控。
- **涉及资源**：GDAL/rasterio/geopandas/shapely/pyproj/fiona、rsgislib、torchgeo、earthengine-api、scikit-learn/xgboost/torch-geometric、osmnx/networkx/folium/keplergl、xarray/rioxarray/dask、pystac-client/planetary-computer、laspy/pdal/open3d、PostGIS/SpatiaLite；数据侧涉及 Sentinel、Landsat、MODIS、Google Earth Engine、Microsoft Planetary Computer、AWS S3、OpenStreetMap、OGC WMS/WFS/WCS/STAC。

### `astropy`
- **用途**：天文学与天体物理 Python 核心库 Astropy（7.2.0）的使用技能，覆盖单位/量、天球坐标系、FITS I/O、表格、时间系统、WCS、宇宙学，用于实现或调试天文数据分析代码。
- **技能设计**：SKILL.md 按 7 大能力模块组织（每节"关键操作 + 对应 reference"的轻量路由表），references/ 下 7 篇与模块一一对应。无脚本、无分阶段流程，是"API 速查 + 最佳实践"型技能，但附有较详细的"当前版本注意事项"（7.2 与 8.0rc 差异、弃用项）。
- **典型工作流**：先按任务类型（坐标转换、FITS 读取、宇宙学距离、星表交叉匹配）选模块，抄 Quick Start，遵循最佳实践（永远带单位、FITS 用上下文管理器、数组化处理、显式时间尺度、QTable 单位感知列），必要时跳到对应 reference 查细节。
- **特色**：版本意识强（明确区分 7.2 稳定与 8.0 RC 的 API 变化，如 cosmology 子模块扁平化、CODATA 2022、NumPy 2.0 底线）；网络访问透明化——明确列出哪些调用会触网（`from_name`、`of_site`、远程 FITS、IERS 更新），要求敏感目标名/地址先征得用户同意。
- **涉及资源**：astropy 7.2.0（uv 安装，钉版）及 NumPy/PyERFA/PyYAML 依赖；网络侧涉及对象名解析（Simbad 类）、台站查询、远程 FITS（S3/HTTP）、IERS 地球自转数据服务；文档站 docs/learn.astropy.org 与 GitHub。

### `aeon`
- **用途**：时间序列机器学习工具包 aeon（1.x）技能，覆盖分类、回归、聚类、预测、异常检测、变点分割、相似性/motif 搜索、距离度量、特征变换与基准测试，面向需要超越常规 ML 的专用时序算法场景。
- **技能设计**：SKILL.md 按任务域组织（每个能力一节：简介 + Quick Start + 指向 reference 的链接），references/ 11 篇按任务域精确对应（classification、regression、clustering、forecasting、anomaly_detection、segmentation、similarity_search、transformations、distances、networks、datasets_benchmarking）——是很干净的"任务→算法目录"路由。
- **典型工作流**：数据格式统一为 `(n_cases, n_channels, n_timepoints)`；先归一化/插补，再按选择指南挑算法（快速原型用 MiniRocket/ROCKET，最高精度用 HIVECOTEV2/InceptionTime，可解释性用 Shapelet/Catch22，小数据集用 DTW+KNN），可与 sklearn Pipeline 组合，或用 ROCKET/Catch22 提特征后接传统 ML，最后可用 `get_estimator_results` 对比公开基准。
- **特色**：明确区分稳定模块与"实验性"模块（forecasting、anomaly_detection、segmentation、similarity_search、visualisation 上游标为实验性，接口可能小版本间变动）；强调先简单后深度学习、必须和简单基线对比；记录了 1.0 对 forecasting/transformations 的重构导致的与 0.x/sktime 时代不同的导入路径陷阱。
- **涉及资源**：aeon-toolkit（aeon 1.x，`aeon[all_extras]` 可选深度学习）、scikit-learn 兼容生态、内置基准数据集（UCR GunPoint 等）与 aeon 官网/GitHub 文档。

### `fluidsim`
- **用途**：规划、配置、检查、重启和分析周期性笛卡尔伪谱 CFD 仿真（FluidSim 0.9.0），重点是求解器选择、参数审查、FFT/MPI 配置、输出诊断与重启兼容性。
- **技能设计**：最"重流程"的一个——SKILL.md 开头即给 9 步强制工作流（先声明方程/单位/边界/验收标准，否则停下），6 篇 references 按生命周期组织（安装与 FFT/MPI 后端、求解器注册表、仿真与重启工作流、参数面、输出分析、高级特性），scripts/ 有 8 个严格 JSON CLI（配置校验、资源估算、dry-run 生成、输出盘点、收支摘要、重启兼容性）。
- **典型工作流**：声明物理设定 → 选验证过的求解器并检视生成的默认参数 → 写显式资源/时间步/CFL 边界的 JSON 计划 → 跑校验器和资源估算器 → 生成 dry-run 脚本（需显式 config-ID 确认才执行）→ 跑一个微型串行试点 → 独立细化网格与时间步 → 才准备站点 MPI 作业（绝不自动提交）→ 保留完整溯源。
- **特色**：科学验收门禁极严——明确"跑完、稳定、图平滑、程序正常退出都不是收敛/物理有效的证据"，禁止仅凭参数或图就打上 DNS/converged/validated 标签；安全边界包括：不自动提交 MPI 作业、生成的脚本绝不使用 `--modify-params`（上游 CLI 会执行任意 Python 代码）、不宣称 GPU 支持（CUDA 集成视为源码级实验）；CLI 全部无网络、无子进程、拒绝 URL/软链接。`ParamContainer` 拒绝未声明属性，须从 `Simul` 类生成默认值再改。
- **涉及资源**：fluidsim 0.9.0、fluidfft 0.4.5、pyFFTW 0.15.1（uv 锁定）；可选 mpi4py 4.1.2 与 fluidfft 原生插件（需站点 MPI 实现、FFTW/PFFT/P3DFFT 开发库、编译器、审批过的 HPC 调度器）；分析用 h5py/netCDF4（惰性加载）；上游为 PyPI、readthedocs、GitHub 及两篇 JOSS 论文（DOI 10.5334/jors.239/238）。

---

## Bayesian, stats & epidemiology

### `pymc`
- **用途**：用 PyMC 做贝叶斯建模与概率编程——构建线性/逻辑回归、分层模型、时间序列等模型，运行 MCMC（NUTS）或变分推断，进行先验/后验预测检查、收敛诊断（R-hat、ESS、发散）和模型比较（LOO/WAIC），适用于需要不确定性量化和分层/缺失数据处理的科研场景。
- **技能设计**：SKILL.md 内嵌"八步标准贝叶斯工作流"和分布选择速查表（先验/似然按参数类型分类），references/ 有 5 份文档（标准工作流、分布目录、采样与推断、工作流示例、模型模式），scripts/ 提供自动诊断报告和基于 PSIS-LOO 的模型比较工具，assets/ 提供线性回归和分层模型两个完整模板。没有路由表，靠正文的"See"链接分发到 references。
- **典型工作流**：数据准备并标准化预测变量 → 在 `pm.Model` 中写先验与似然 → 先验预测检查（拟合前确认合理）→ `pm.sample()` 带种子采样 → 检查诊断（发散必须改模型或重参数化，而不是盲目调 target_accept）→ 后验预测检查 → 结果分析 → 用 `pm.set_data` 对新数据做预测。
- **特色**：强调"先验预测检查优先、绝不先采样后检查"的纪律性流程；对常见问题（发散、低 ESS、高 R-hat、慢采样）给出症状-方案对照；诊断脚本可一键生成 trace/rank/能量图等完整报告；文档绑定 PyMC 6.0.1 + ArviZ 1.x 的最新 API 变化。
- **涉及资源**：Python 3.12+，PyMC 6.0.1（含 nutpie 可选采样器）、PyTensor、ArviZ、NumPyro/BlackJAX 可选、xarray/NetCDF 存储；无云服务或外部数据集。

### `statsmodels`
- **用途**：Python 统计建模与计量经济学库的深度指南，用于拟合 OLS/WLS/GLS、GLM、离散选择（Logit/Probit/计数模型）、混合效应模型、ARIMA/SARIMAX/VAR 等时间序列模型，重点在推断（标准误、置信区间、假设检验、系数表）而非预测。
- **技能设计**：纯 references 型 skill（无 scripts），8 份主题文档组成"入口-专题"两级结构：quick_start_guide.md 是最小可运行示例入口，modeling_capabilities.md 和 model_selection.md 是总览路由，另有线性模型、GLM、离散选择、时间序列、统计诊断五份专题详解；文档教用户用 `rg` 命令在 references/ 里检索。
- **典型工作流**：四条内置工作流——线性回归（探索→OLS→残差诊断→异方差/自相关/多重共线性/影响点→稳健标准误→验证）、二分类（Logit→收敛→odds 比→边际效应→AUC→与 Probit 比较）、计数数据（Poisson→过散检验→负二项→零膨胀→AIC 比较）、时间序列（平稳性检验→差分→ACF/PACF 定阶→ARIMA→Ljung-Box→预测）。
- **特色**：明确划定与 scikit-learn 的边界（推断 vs 预测），并与其他统计 skill 互相引流；正文列了 15 条"常见陷阱"清单（忘加常数项、过散用 Poisson、非嵌套模型用 LR 检验等），这是它区别于通用文档的核心价值。
- **涉及资源**：Python 3.9+，statsmodels 0.14.6（uv 固定版本安装），可选 scikit-learn；官方文档站 statsmodels.org 作为延伸参考。

### `statistical-analysis`
- **用途**：科研数据的"引导式"统计分析总入口——当用户想比较组间差异、检验假设、分析实验/问卷数据但未必说出具体检验名时使用；覆盖 t 检验、ANOVA、卡方、相关、回归、非参数和贝叶斯方法，贯穿假设检查、效应量和 APA 格式报告，目标是产出"审稿人挑不出毛病"的完整分析。
- **技能设计**：分层的"选择-检查-运行-报告"架构：正文含快速检验选择表和假设违反补救表（相当于内置路由），references/ 有 5 份深度文档（检验选择决策树、假设与诊断、效应量与功效、贝叶斯统计、APA 报告标准），scripts/ 提供自动化假设检查模块（正态性、方差齐性、回归四联诊断、异常值检测，带可视化）。
- **典型工作流**：六步流程——先在碰数据前框定问题并预定检验（防 p-hacking）→ 逐组检查描述统计与原始数据图 → 按选择表/决策树选检验 → 用脚本查假设、失败则换补救检验并报告变更 → 运行检验同时必算效应量 → 按 APA 模板写报告（描述统计、精确 p 值、效应量+CI、假设检查结果、全部预定分析含阴性结果）。
- **特色**："统计诚信"守则是灵魂——区分验证性/探索性分析、禁止凑显著性、非显著≠无效应、多重比较校正必须声明、缺失数据不能随手删；文档还详细标注了 pingouin 0.6 列名变更、ArviZ 1.x 默认 89% 区间等版本兼容陷阱。
- **涉及资源**：Python 3.10+，pingouin、scipy、statsmodels、pandas、matplotlib、seaborn、pymc、arviz；与 statsmodels/pymc skill 互为上下游。

### `statistical-power`
- **用途**：研究设计阶段的样本量与统计功效计算——回答"我需要多少被试/样本/重复"，覆盖先验功效分析、最小可检测效应（MDE）、功效曲线，以及为资助申请、IRB 伦理审查、预注册撰写样本量论证；闭式公式覆盖标准检验，蒙特卡洛模拟覆盖无公式的复杂设计（混合模型、logistic 回归、整群随机试验、生存分析）。
- **技能设计**：双轨制架构——scripts/power.py 把 statsmodels/pingouin 封装成统一接口（`sample_size`/`power`/`mde`/`power_curve` 四个函数支持 9 类检验），scripts/simulate_power.py 提供蒙特卡洛通用框架（`simulate_power`/`find_sample_size`）；references/ 三份文档分别对应闭式配方、模拟模式和效应量选择，另与 experimental-design、statistical-analysis 等 skill 显式分工。
- **典型工作流**：七步——先声明设计与计划分析（混合模型/GLM 直接走模拟）→ 按可信度顺序选效应量（最小重要效应 SESOI > 缩水的先导估计 > Cohen 惯例）并写明理由 → 设定 α 和目标功效 → 用脚本计算 → 敏感性分析（效应量区间+功效曲线，"这是交付物而非单个数字"）→ 叠加脱落/聚类/多重比较校正 → 按模板写报告。
- **特色**：把"效应量是核心决策"放在最前并坚决反对事后观测功效（指出它是 p 值的确定性函数，被要求也需警示）；专设"人们忘记的调整"一节（多重比较、脱落膨胀、设计效应 DEFF 防伪重复、单双侧、非 1:1 分配），这是通用工具不会主动提醒的。
- **涉及资源**：Python 3.10+，statsmodels、scipy、pingouin、numpy、matplotlib、pandas，可选 lifelines（生存）；文献引用 Cohen 1988、Lakens 2022 等。

### `uncertainty-and-units`
- **用途**：科学计算中的物理单位跟踪与测量不确定度传播——单位换算与量纲检查、GUM 不确定度预算、A/B 类评定、包含因子与扩展不确定度、蒙特卡洛传播、±报告格式、曲线拟合参数不确定度，以及"这个数字物理上合理吗"的量级合理性审计（雷诺数等 14 个无量纲群、特征尺度、观测量级带）。
- **技能设计**：SKILL.md 主体是"不可协商的工作流"（11 条铁律）+"本 skill 要防的六类静默失败"（各配反例代码：`.magnitude` 丢单位、offset 温度、dB 相加、相关被序列化摧毁、curve_fit 协方差被缩放、线性化未验证）；references/ 6 份文档覆盖 GUM 方法论、pint/uncertainties 配方、领域换算、报告规则、合理性尺度；scripts/ 是 6 个离线 CLI 工具（传播、预算、格式化、换算、代码审计、合理性检查）。
- **典型工作流**：输入端挂单位、只在输出端剥离 → 显式写出测量模型 → 每个输入给估计值+标准不确定度+分布+自由度 → 用正确除数换算 B 类陈述 → 识别相关性 → 算灵敏度系数并读预算 → GUM 与蒙特卡洛并行跑 JCGM 101 第 8 条验证 → 按有效自由度选 k → 先舍入不确定度再对齐数值 → 明确 ± 的含义 → 报告前做量级合理性检查。
- **特色**：全技能最"防错工程化"的一个——audit_units.py 是纯 AST 静态审计器（9 条规则 UNIT001-CONST001，可设 fail-on 阈值当 CI 门禁，支持指令注释豁免）；check_plausibility.py 坚持"量纲一致≠物理可能"，会在算数前拒绝动态/运动粘度混用这类错误；所有 CLI 拒绝 URL/符号链接、原子写入、离线运行。
- **涉及资源**：Python 3.12+，pint 0.25.3、uncertainties 3.2.3、NumPy、SciPy（scipy.constants 提供 CODATA 2022 常数）；标准文档来自 BIPM JCGM 100/101、NIST TN 1297、CODATA，均为引用不联网。

### `treatment-plans`
- **用途**：在临床决策已由授权执业医师做出并本地验证之后，对这些决策文档做格式化与结构性验证——来源溯源、医师撰写的干预记录、目标与检查点、共同决策记录、交接核对、放行门禁。明确"不做临床决策"，只管文档。
- **技能设计**：安全边界优先的架构：SKILL.md 开头即"硬安全边界"（一长串 Never 清单）+ 强制可见声明（DRAFT — NOT MEDICAL ADVICE…）+ 数据门禁（真实患者数据的六条本地处理规则）；assets/ 提供 6 份不含任何临床内容的通用 JSON 模板，scripts/ 是 8 个纯标准库的确定性校验器（计划验证、溯源、完整性、隐私流程、一致性、时间线生成），references/ 8 份文档含安全范围、隐私治理、来源账本。
- **典型工作流**：五步——确立权限与用途（记录责任人、辖区、政策、放行门保持 blocked）→ 用 generate_template.py 生成通用空包 → 逐字转录已验证的医师决策（保留来源定位符/版本/验证角色，缺字段留空、绝不推断填充）→ 跑六个确定性本地检查 → 人工审查放行（对照签字来源核对、药物重整、FDA 标签核实、多方签字后才解除门禁）。
- **特色**：这是"反 LLM 能力"的极致设计——转录零推断、脚本零网络零动态执行、时间线只排布已提供日期绝不推导周期、报告只输出规则代码和字段路径不含临床值；脚本通过≠放行授权，结构性成功也移除不了 DRAFT 标记。
- **涉及资源**：Python 3.11+ 纯标准库，本地 JSON 文件，零网络/零第三方依赖；FDA 标签数据库、Medication Guide、REMS、WHO/Joint Commission、AHRQ/NICE、CMS 仅作为"由人核实的权威来源记录"，skill 本身不解读。

### `relsa-severity-assessment`
- **用途**：实验动物研究的严重程度评估与人道终点预测——把体重、体温、临床评分、生物标志物、活动等多项福利指标合成每只动物每天一个 RELSA 严重度分数（0=基线，1=达到参照集最大负荷），用 ARIMA 预测人道终点何时到达，用 KDE 在分数尺度上划定注意区/危险区，服务于 3Rs 改良、动物福利报告和 EU Directive 2010/63/EU 申请。
- **技能设计**：围绕"四个决定结果的决策"（变量方向性、参照集、零基线评分映射、变量全程齐备）组织知识；scripts/ 四个模块对应三步法（relsa_score.py 打分、forecast_relsa.py 含 auto_arima 的预测、kde_thresholds.py 阈值分区、_common.py 公共 I/O 与指标），references/ 三份文档分别讲方法、预测和阈值，assets/ 提供一个 6 只小鼠的合成队列 CSV 让所有命令可直接跑通。
- **典型工作流**：第一步算 RELSA 分（声明 turned 变量、基线时间、参照组，参照模型可存 JSON 供后续队列复用同尺度）→ 第二步预测终点（训练到终点前一个时点、预测终点分数并报告 RMSE+PICP+MPIW 三指标，或滚动单步预测做实时监测）→ 第三步 KDE 定严重度分区（阈值取密度极小值，必须带带宽敏感性扫描）→ 按 8 条报告清单写方法。
- **特色**：处处"诚实的边界"声明——RELSA 只是辅助评估不是决策参数、KDE 阈值不等于法规严重度分级、ARIMA 预测不了断崖式恶化且"低估才是危险的错误"；明确标注发表证据仅 13 只动物的 proof-of-concept；对方向性搞错会静默归零、零基线 0/0、变量中途增删等陷阱有详细剖析，且 Python 实现与 R 包发表示例核对到小数点后两位。
- **涉及资源**：Python 3.10+，numpy/pandas/scipy（打分）、statsmodels 0.14（ARIMA）、matplotlib（图）；方法源自 Talbot 2022 RELSA 与 Lutscher 2026 foRcast 两篇论文及 R 包，无需网络访问。

### `clinical-decision-support`
- **用途**：仅限科研用途的临床决策支持（CDS）评估与治理制品准备——预期用途声明、聚合队列表壳、统计分析计划、生存分析计划、模型/生物标志物聚合性能评估、GRADE 证据档案、去标识化流程清单、决策逻辑溯源等，明确排除患者诊疗和实时临床运行。
- **技能设计**：典型"模板+校验器"配对架构：7 份 assets JSON 模板与 7 个 scripts 校验器一一对应（有一张"需求→模板→脚本"路由表）；每份制品强制携带可见头部（类型/版本/状态、禁止用途、数据级别、人工审查边界、"Not for patient care"声明）；references/ 13 份文档含 FDA/ONC/ICH 监管语境、GRADE 人工流程、各分析专题和带日期的权威来源账本。
- **典型工作流**：先框定研究问题（在看结果前定义估计目标、区分描述/预后/预测/诊断/因果问题、预注册分析）→ 按需求表选制品类型 → 本地跑对应校验脚本 → 按制品类型配置比例化人工审查（方法学家、领域专家、隐私官、法务、人因专家、治理责任人），脚本通过仅代表字段齐全和内部一致，不代表可放行。
- **特色**：安全设计极严——数据门禁只接受合成/聚合输入并主动拒绝行级患者数据、URL 路径和常见行级键；GRADE 确定性评级必须由人工小组做且禁止从文章文本或 p 值推断；模型评估只接受聚合混淆计数、明确否认去标识化/HIPAA 合规的判定能力；给出 FDA 2026 年 1 月 CDS 指引、ONC HTI-1、ICH E6(R3) 等带日期的监管语境。
- **涉及资源**：Python 3.11+ 纯标准库，本地文件零网络零凭据；概念上引用 GRADE、STROBE/TRIPOD+AI/PROBAST+AI/REMARK/STARD-AI/SPIRIT-AI/CONSORT-AI/DECIDE-AI 报告规范、FDA/ONC/ICH/HHS 框架，均作为文档语境而非服务依赖。

### `clinical-reports`
- **用途**：为临床病例报告、诊断报告（影像/病理/检验）、临床试验结果与 CSR、安全性报告、聚合研究摘要准备"安全受限的草稿结构"并运行本地确定性检查——只做结构脚手架和溯源，不做诊断、解释或提交，所有输出需合格人工审查。
- **技能设计**：核心是一张"先路由后起草"表：11 类制品各自映射到 CARE 2013、ACR 2025、CAP 癌症协议、42 CFR 493.1291、CONSORT 2025、SPIRIT 2025、ICH E3/E2/E6(R3) 等指南及对应边界；15 份 assets 模板全部是合成 schema 且"起始即 blocked"；scripts/ 9 个标准库校验器（病例报告验证、试验报告验证、不良事件表格化、术语/溯源/一致性/去标识检查）；sources.md 是 2026-07-23 核对的官方来源账本。
- **典型工作流**：五步——先建溯源清单（只记本地记录定位符+SHA-256 哈希，不复制内容）→ 用生成器产出 fail-closed JSON 模板 → 只在有验证事实 ID 支持时填字段，null/missing 保持原样绝不填合理文本 → 跑确定性结构检查 → 按专业配置审查（临床、统计、安全、隐私/法务、监管、期刊），最后交班时声明指南版本、未解决项和"草稿勿提交"警告。
- **特色**：禁令清单最细的一个——禁止发明/推断/"补全"观察结果、禁止把原始观察翻译成诊断/分级/因果、禁止代替任何人签字提交、禁止调用外部 LLM/API/其他 skill；曾有的 SOAP/H&P/会诊/出院摘要接口已被主动移除；七个禁用词（"compliant"、"HIPAA-safe"、"validated" 等）永远不能说。
- **涉及资源**：Python 3.11+ 纯标准库可选脚本，零网络/凭据/外部模型；引用 CARE/CONSORT/SPIRIT/ICH E3/E6(R3)/E2A/E2B(R3)/E2D(R1)、ACR/CAP/CLIA、HHS 45 CFR 164.514(b) 等标准文本作为路由与校验依据。

---## ML / deep learning / optimization

### `pytorch-lightning`
- **用途**：用 Lightning 框架组织 PyTorch 深度学习工程——把训练循环、设备管理、分布式、日志等样板代码自动化，面向从单卡到多 GPU/TPU 的可扩展神经网络训练与部署场景。
- **技能设计**：SKILL.md 按"组件→详情"两级组织：主文档给六大能力概览（LightningModule、Trainer、DataModule、Callbacks、Logging、分布式），每个能力配一个 `references/` 专题文档（共 7 个，含 best_practices），另配 3 个可执行 scripts 模板（模型、数据、Trainer 配置）作为快速起步样板。
- **典型工作流**：先按六段式（init/train/val/test/predict/optimizers）编写 LightningModule 模板 → 用 DataModule 或直接 DataLoader 准备数据 → 配置 Trainer（策略、精度、回调）→ `trainer.fit()` 训练并经 `self.log()` 记录指标。
- **特色**：按模型规模给出分布式选型决策（<500M 参数用 DDP、500M+ 用 FSDP、精细控制用 DeepSpeed）；强调设备无关代码、`save_hyperparameters`、`fast_dev_run` 调试等最佳实践；明确标注版本变化（如 2.6.4 移除 NeptuneLogger）。
- **涉及资源**：PyPI 的 `lightning` 包（2.6+），CUDA 版 PyTorch；可选 W&B、MLflow、comet-ml、DeepSpeed 等扩展；官方文档站 lightning.ai。

### `torch-geometric`
- **用途**：基于 PyG 做图神经网络开发：节点/图分类、链接预测、异构图建模、消息传递自定义层、邻居采样扩规模、自定义数据集加载与 GNN 可解释性，明确不覆盖普通 NetworkX 分析。
- **技能设计**：主文档本身即是一份长教程（数据结构、卷积层选型表、三类任务模式、异构图三种建模方式），不设 scripts/，把深度内容下沉到 6 个 `references/`（message_passing、link_prediction、scaling、heterogeneous、custom_datasets、explainability），按需加载。
- **典型工作流**：把图数据组装成 `Data`/`HeteroData`（注意 edge_index 的 [2, num_edges] 转置）→ 从 `torch_geometric.nn` 选层或子类化 MessagePassing 自定义 → 按任务套用训练模式（mask 全批 / DataLoader+global_pool / RandomLinkSplit）→ 大图改用 NeighborLoader 或 DDP。
- **特色**：突出常见坑清单（edge_index 形状、卷积层不含激活、异构二部图禁用自环、seed 节点切片、num_neighbors 对齐层数）；`_i/_j` 自动索引约定与 lazy 初始化（-1 通道）；卷积层选型对照表。
- **涉及资源**：PyTorch 2.6+、torch-geometric 2.7.x；可选加速轮子 pyg-lib/torch-scatter/torch-sparse/torch-cluster（需从 data.pyg.org wheel 索引匹配 CUDA 版本）；内置 Planetoid、TUDataset、OGB、QM9、ShapeNet 等数据集；conda pyg 渠道已停维。

### `transformers`
- **用途**：用 Hugging Face Transformers 加载 Hub 预训练模型、pipeline 快速推理、文本生成参数控制，以及 Trainer 微调，覆盖 NLP/视觉/音频/多模态任务；限定于 Transformers 库本身而非通用 ML。
- **技能设计**：轻量主文档 + 5 个 `references/`（pipelines、models、generation、training、tokenizers），每个能力都标了"何时用"，像一张能力路由表；给出三种使用模式（pipeline / 手动加载模型 / Trainer 微调）作渐进路径；无 scripts/。
- **典型工作流**：装库并 `hf auth login` 认证 → 简单任务直接 `pipeline("task", model=...)` 推理 → 需要控制时分开加载 AutoTokenizer/AutoModel 并调 `generate`（max_new_tokens、temperature 等）→ 定制任务用 Trainer + TrainingArguments 微调。
- **特色**：针对 v5 的重大变化（仅支持 PyTorch、弃 TF/JAX）与迁移指南；明确的 token 安全纪律（最窄权限、不硬编码、HF_HUB_DISABLE_IMPLICIT_TOKEN、HF_HOME/HF_HUB_OFFLINE 缓存策略）；版本依赖钉死以保可复现。
- **涉及资源**：PyPI 的 transformers 5.x、huggingface_hub 1.x、datasets、evaluate、accelerate；视觉任务加 timm/pillow，音频加 librosa/soundfile；Hugging Face Hub 云端模型仓库（gated 模型需 token）。

### `shap`
- **用途**：对已训练好的预测模型做 SHAP 归因解释与审计：选 explainer/masker、计算并验证特征归因、处理多输出、产出局部或全局可视化，适用于模型行为描述与合规审计场景。
- **技能设计**：以"操作规则"开篇（9 条铁律），主体是七步标准工作流 + explainer 选型决策表 + "问题→图形"对照表；8 个 `references/` 采用"Load when"参考地图路由；另有 1 个确定性示范脚本（不联网、不反序列化模型）。
- **典型工作流**：先声明解释目标（模型方法、输出单位、背景人群、版本）→ 选 explainer 与 masker → 用 `shap.Explainer(model, masker)` 计算现代 `Explanation` 对象 → 多输出先切片 `explanation[..., idx]` → 用 base_values+values 之和对照模型真实输出做可加性校验 → 按"要回答的问题"选图 → 随结果报告局限。
- **特色**：强科学纪律——SHAP 不等于因果/公平性/机制解释；加和性失败未查清前不得静默；禁止加载不可信 pickle/joblib（反序列化可执行代码）；树模型概率/log-loss 输出必须显式 interventional 语义；0.45 前后多输出切片语法差异的迁移提醒。
- **涉及资源**：PyPI `shap[plots]==0.52.0`（需 Python 3.12+）、uv；被解释模型所属库（如 sklearn、深度框架）按需安装；shap.readthedocs.io 与 GitHub 官方源；内置 sklearn 数据集。

### `scikit-learn`
- **用途**：覆盖经典机器学习全流程：监督（分类/回归）、无监督（聚类/降维）、预处理、交叉验证评估、超参搜索和 Pipeline 组合，面向表格与文本数据的生产级建模。
- **技能设计**：主文档含快速起步代码、能力总览与最佳实践/排障清单；知识分两层——`references/` 8 个文档按五大能力域（监督、无监督、评估、预处理、管道）+ quick_reference + common_workflows 组织；`scripts/` 提供 2 个完整可跑示例（分类流水线、聚类分析）。
- **典型工作流**：分层划分数据 → 在 `Pipeline`/`ColumnTransformer` 内做数值（填补+标准化）与类别（填补+独热）分支预处理 → 接估计器 → `fit`/`predict`，或用 GridSearchCV 做嵌套调参与多模型交叉验证比较。
- **特色**：反复强调防数据泄漏（预处理必须放进管道按折重拟合、只在训练折 fit）；哪些算法需要/不需要缩放的对照表；不收敛、过拟合、大内存的对症排障；提醒装 PyPI 包名为 `scikit-learn` 而非弃用的 `sklearn`。
- **涉及资源**：PyPI scikit-learn 1.7+（Python 3.11+）、NumPy/SciPy 必装；可选 matplotlib/seaborn 画图、pandas；scikit-learn.org 官方文档/示例库。

### `scikit-survival`
- **用途**：构建、评估与审计右删失生存分析工作流：Cox/Coxnet/IPC Ridge/生存森林/Boosting/SVM 等模型、删失感知指标（IPCW C-index、动态 AUC、Brier）、非参数竞争风险累积发生率，面向医学与可靠性场景。
- **技能设计**：以"10 条不可协商工作流"为骨架（定义估计目标→校验结局→先切分再预处理→管道内拟合→嵌套调参→训练集拟合删失分布→限评价时间网格→预测与指标匹配→竞争风险显式处理→报告局限）；`references/` 6 个按数据、模型族（Cox/集成/SVM）、指标、竞争风险划分；`scripts/` 提供本地无网 CLI 五件套。
- **典型工作流**：`Surv.from_arrays` 构造 (event, time) 结构化结局 → 分层切分后用 ColumnTransformer+make_pipeline 组装防泄漏管道 → 拟合所选生存模型 → 风险分送 C-index/动态 AUC，`predict_survival_function` 在时间网格上取 (n, n_times) 概率矩阵送 Brier → 或经 CLI 链 validate→train→evaluate→report 产出报告。
- **特色**：对指标契约的严格区分（判别 vs 校准 vs 概率误差、SVM 无生存概率）；删失分布只用训练数据；竞争风险不做 1-KM 近似、无 Fine-Gray；明确"非临床建议"边界与安全纪律（拒绝 URL/符号链接、不加载不可信 pickle、避免脚本名遮蔽包名）。
- **涉及资源**：PyPI scikit-survival 0.28.0 及钉死的 sklearn/numpy/pandas/scipy/ecos/osqp/narwhals 栈；上游 GPL-3.0 许可（skill 本身 MIT）；readthedocs/GitHub 官方源；纯本地合成数据。

### `pymoo`
- **用途**：多目标/进化优化框架：单目标、多目标（Pareto 前沿）、多目标（4+ 目标）、约束、混合变量优化，配套基准测试问题与多准则决策，面向工程设计与权衡分析。
- **技能设计**：围绕统一的 `minimize(problem, algorithm, termination)` 接口组织；SKILL.md 提供九个工作流路由表、按目标数的算法选型表、算子按变量类型的选型建议与排障速查；`references/` 7 个文档（算法、基准问题、算子、可视化、约束与 MCDM、并行、快速工作流），`scripts/` 5 个可执行示例。
- **典型工作流**：从三种问题定义式中选一种（向量化 `Problem`、逐解 `ElementwiseProblem`、函数式 `FunctionalProblem`）定义目标与约束（g≤0）→ 按目标数选算法（GA/DE/PSO/CMA-ES；NSGA-II/SPEA2/MOEA/D；NSGA-III/RVEA 需参考方向）→ 设置终止条件与种子运行 → 可视化 Pareto 前沿并用标量化/MCDM 挑解。
- **特色**：不收敛、前沿分布差、可行解少、算力贵四类问题的对症排障；强调目标归一化、保存历史分析收敛、混合变量用 `vars` 字典；指出 LLM 友好的文档入口 pymoo.org/llms.txt 与 references 检索 grep 模式。
- **涉及资源**：PyPI pymoo 0.6.1.x（Python 3.10+）、NumPy/SciPy；可选 matplotlib 画图、autograd 梯度特性、joblib/Dask 并行；pymoo.org 文档站。

### `pennylane`
- **用途**：硬件无关的量子机器学习框架：量子电路自动微分、量子-经典混合模型、变分算法（VQE、QAOA）、量子化学模拟，一次编写即可在多家厂商模拟器/真机间移植。
- **技能设计**：主文档给出快速起步与六大能力概览，每项能力对应一个 `references/` 专题（电路、量子 ML、化学、设备后端、优化、高级特性，另含入门指南共 7 个）；提供三个代表性工作流（变分分类器、VQE、跨设备切换）作为模式模板；无 scripts/。
- **典型工作流**：`qml.device()` 建设备 → `@qml.qnode` 装饰电路（门+测量）→ 选优化器训练参数（化学场景则 qchem 构建分子哈密顿量+UCCSD ansatz 做 VQE）→ 需要上真机时换 plugin 设备（如 `qiskit.remote`）复用同一电路。
- **特色**：设备无关性是核心卖点——同一电路在 default.qubit、lightning、IBM、Braket、Rigetti、IonQ 间切换；最佳实践强调先模拟器后硬件、真机只能用 parameter-shift 梯度（backprop 仅限模拟器）、防贫瘠高原的小初值初始化、Catalyst JIT 提速；与其他量子 skill 的分工声明（IBM 专用 qiskit、Google 用 cirq、开放系统用 qutip）。
- **涉及资源**：PyPI pennylane 0.45.0 及全家桶插件（pennylane-qiskit/-cirq/-rigetti/-ionq、amazon-braket-pennylane-plugin、lightning、catalyst）；对应云平台 IBM Quantum、Amazon Braket、IonQ、Rigetti；docs.pennylane.ai 与 Codebook/演示站。

### `cirq`
- **用途**：Google 量子计算框架：面向 NISQ 电路设计、噪声感知建模、电路编译变换，以及在 Google Quantum AI 及 IonQ/Azure/AQT/Pasqal 等合作后端上运行与做量子特性表征实验。
- **技能设计**：主文档按六大能力域路由到 6 个 `references/`（building、simulation、transformation、hardware、noise、experiments），每个域下列出主题清单；附三个通用模板（变分算法、硬件执行、噪声对比研究）可直接套用；含最佳实践与常见故障对照；无 scripts/。
- **典型工作流**：用 LineQubit/GridQubit 与门构建电路（可 sympy 参数化）→ `Simulator` 本地仿真并用 `run_sweep` 做参数扫描 → 经 transformer 流水线优化（门合并、Z 弹出、路由/SWAP、编译到硬件门集）→ 注入噪声模型用 DensityMatrixSimulator 预研 → 再经 Engine/厂商服务提交真机。
- **特色**：噪声建模与表征（去极化/振幅阻尼信道、随机基准、XEB、热噪声、读出噪声热图）是其相对 PennLane 的独门强项；变分算法模板刻意用 scipy COBYLA 而非自动微分；与 qiskit/pennylane/qutip 的分工路由写在开头；硬件执行前"先仿真、按校准选量子比特、及时保存昂贵结果"。
- **涉及资源**：PyPI cirq 1.6.1 及同版本号厂商包（cirq-google/cirq-ionq/cirq-aqt/cirq-pasqal、azure-quantum[cirq]）；Google Quantum Engine 需审批的 GCP 项目、IonQ API key、Azure Quantum 资源；quantumai.google 文档与 ReCirq 实验框架。

### `qutip`
- **用途**：QuTiP 5 的有限维量子系统动力学仿真与审计：闭合/开放系统（Lindblad、量子跳变、Bloch-Redfield）、随机轨迹、稳态、谱与相空间分析，以及 Floquet/HEOM/PIQS 等专门方法；定位是物理仿真而非硬件执行 SDK。
- **技能设计**：以"七条不可协商的模型契约"（单位约定、张量顺序、态有效性、生成元含义、近似声明、数值设置、收敛扫描）为骨架；按物理选求解器的对照表；`references/` 5 个文档按核心概念/时间演化/分析/可视化/高级边界划分；`scripts/` 提供 6 个本地无网 CLI（模型校验、仿真、求解器规划、收敛扫描、结果审计、谱规划）。
- **典型工作流**：记录单位（ħ=1、rad/s）与子系统顺序、检查 Qobj 的 dims（仅看 shape 不够）→ 按物理选 sesolve/mesolve/mcsolve/brmesolve/Floquet/HEOM → 5.3 新式 options 字典运行、报告 `result.stats` → 校验收缩（Fock 维数、时间网格、ODE 容差、轨迹数）→ 用 result_audit.py 审计 JSON 输出。
- **特色**：极强的物理严谨纪律——Lindblad 速率是 `sqrt(gamma)*A`、ptrace 是保留而非求迹、brmesolve 可能破坏正性需查本征值、FFT 谱须验尾衰减/混叠；安全边界：禁动态代码执行、不加载不可信对象文件（可执行代码）、QIP 与最优控制已拆到独立包（qutip-qip/qutip-qtrl，明确各自成熟度）。
- **涉及资源**：PyPI qutip==5.3.0（Python 3.11+）、NumPy/SciPy、可选 graphics extra 画图；可选家族包 qutip-qip/qutip-qtrl/qutip-jax（预发布需谨慎，qutip-cupy 无 PyPI 发布勿用）；纯本地运行、无网络服务与凭据；官方 readthedocs/GitHub/教程源。

### `qiskit`
- **用途**：用 Qiskit 2.x 构建、模拟、转译（transpile）和执行量子线路，覆盖本地态矢量采样/期望值、Aer 噪声模拟、IBM 量子云 QPU 执行、错误缓解及 V2 primitives 工作流。
- **技能设计**：SKILL.md 主体加 11 个 references（setup/circuits/primitives/transpilation/backends/patterns/algorithms/visualization/migration/testing/sources），配"Reference Map"路由表按需加载；另有 3 个 scripts（环境检查、本地 primitives 演示、只读后端能力检查）。核心工作流按 Map→Optimize→Execute→Analyze 四阶段组织，末尾有 8 条返回代码前的 Final Checklist。
- **典型工作流**：先按"目标→接口"选路（本地精确采样用 StatevectorSampler，QPU 用 Runtime SamplerV2/EstimatorV2）；问题映射为线路+可观测量，对选定后端一次性转译出 ISA 线路，对可观测量 apply_layout，经 PUB 提交执行，最后分析寄存器级结果与元数据。
- **特色**：强版本契约（针对 qiskit 2.5.0/ibm-runtime 0.48 验证）；"Non-Negotiable Rules" 硬性禁用 V1 API 和 pulse；明确参数化线路只转译一次；API 密钥永不落盘/日志；inspect 脚本只查后端不提交作业；QPY 而非 pickle 序列化；甚至给出"该用 QuTiP/PennyLane 而非 Qiskit"的边界。
- **涉及资源**：qiskit、qiskit-aer、qiskit-ibm-runtime、IBM Quantum Platform（账号+API key+网络）、qiskit 生态包（Nature/ML/Optimization）、QuTiP/PennyLane（建议替代）、Qiskit 官方文档与发布说明。

### `dask`
- **用途**：把 pandas/NumPy 工作流扩展到超出内存或跨集群的并行/分布式计算，适合多文件批处理、TB 级数组、非结构化日志 ETL 和自定义并行任务图；frontmatter 明确分流：单机 out-of-core 用 vaex，内存内求快用 polars。
- **技能设计**：SKILL.md 是"总纲+组件速查"，按 5 大组件（DataFrame/Array/Bag/Futures/Schedulers）各给 Purpose/When to Use/示例/要点，并各自指向 `references/` 下 6 个专题文档；含组件选择决策指南（按数据类型/操作类型/控制粒度/静态或动态）和"先试更简单方案"的节制原则。
- **典型工作流**：选组件（表格→DataFrame、数组→Array、文本→Bag、自定义→Futures）→ 懒构建任务图 → 选调度器（threads/processes/sync/distributed）→ 单次 compute() 触发 → 用 dashboard 监控；调试时先 synchronous 调度器小数据验证再放大。
- **特色**：强调"先穷尽更好算法/Parquet/Numba/采样再上 Dask"；反模式清单很具体（勿先读入内存再交 Dask、勿循环反复 compute、勿建百万级任务图、chunk 目标约 100MB）；调度器开销数据量化（线程 ~10µs、进程 ~10ms、distributed ~1ms）。
- **涉及资源**：dask（2025.1+，表达式式 DataFrame 已是唯一实现）、dask.distributed、pandas 2+、PyArrow 16+、s3fs/gcsfs（S3/GCS 云存储）、Zarr/HDF5/NetCDF、XArray、Dask-ML、docs.dask.org。

### `polars`
- **用途**：高性能 DataFrame 库，做表达式式 ETL/分析与 pandas 迁移；主打惰性查询优化、默认并行、流式 out-of-core 处理、Arrow 互操作与可选 GPU 执行。
- **技能设计**：SKILL.md 内嵌核心概念（表达式、eager vs lazy、常见操作、join/pivot、pandas 对照表、最佳实践），`references/` 6 个文档（core_concepts/operations/pandas_migration/io_guide/transformations/best_practices）按主题加载；frontmatter 的 allowed-tools 仅 Read，偏纯知识型 skill。
- **典型工作流**：scan_csv 建 LazyFrame → filter/select 链式表达式 → collect() 触发优化执行（大文件用 engine="streaming"）；保持热路径在原生表达式 API 内，map_elements 仅作兜底；尽早做列裁剪、选对数据类型。
- **特色**：以"表达式即一等公民、无 index、严格类型、默认并行"四概念统摄全篇；pandas→polars 逐操作映射表（含 assign 串行 vs with_columns 并行的对比）；明确 warns 别在热路径写 Python 函数破坏并行。
- **涉及资源**：polars 1.41.2（可选 extras：excel/database/fsspec/pandas/numpy）、Apache Arrow、云存储（S3/Azure/GCS）、BigQuery、Parquet/CSV/JSON/Excel 文件格式。

### `vaex`
- **用途**：单机 out-of-core DataFrame，处理超内存的十亿行级表格数据；强项是零拷贝内存映射的快统计聚合、大数据交互可视化（热图/直方图）和大数据 ML 管线。
- **技能设计**：SKILL.md 按 6 大能力域组织（core_dataframes/data_processing/performance/visualization/machine_learning/io_operations），每域一段摘要指向对应 references 文档，并附"按任务选参考"的路由说明；无 scripts，纯参考型。
- **典型工作流**：vaex.open(HDF5/CSV/Arrow/Parquet) → describe() 探查 → 建虚拟列（按需计算零内存开销）→ selections 过滤 → groupby 聚合（delay=True 批量执行）→ df.viz 可视化 → export_hdf5 固化；大 CSV 先转 HDF5 换后续秒开。
- **特色**：与 polars/dask 的三方分工写在开头（内存内速度→polars、分布式集群→dask、单机超内存→vaex）；虚拟列+表达式避免物化、delay=True 批处理、99.7% 分位限幅绘图是标志性用法；提醒 Windows 上 annoy 依赖需 Python dev headers。
- **涉及资源**：vaex 4.19.0 元包（vaex-core/-viz/-hdf5/-ml）、HDF5/Apache Arrow/Parquet/CSV、可选 s3fs/gcsfs/adlfs 云 I/O、scikit-learn/XGBoost/CatBoost 集成、matplotlib 生态。

### `optimize-for-gpu`
- **用途**：把科学 Python（NumPy/SciPy/pandas/sklearn/NetworkX/scikit-image、向量检索、文件 I/O、仿真）加速到 NVIDIA GPU，并"验证结果正确且确实更快"；即使没点名 CUDA，只要大数据并行代码慢就适用。
- **技能设计**：SKILL.md 承担总决策（选最小合适层的路由表 + legacy 项目状态表），`references/` 15 个文档覆盖每个 RAPIDS 库（cupy/cudf/cuml/cugraph/cuvs/cucim/kvikio/numba/warp/raft/cuxfilter/cuspatial）加 decision_framework/installation/code_transformation_patterns 三份横向文档，写码前必读对应文件。
- **典型工作流**：7 步证据驱动流程——定契约与端到端基线并剖析 → 判断 GPU 适配性（禁用固定行数阈值当证据）→ 由轻到重尝试（框架内优化 → 加速器模式 cudf.pandas/cuml.accel/nx-cugraph → 原生 API → 自写 kernel）→ 保持数据常驻显存 → 先验证语义再谈速度 → 正确计时（CUDA events、warmup、nsys/ncu）→ 保留/修改/否决移植。
- **特色**：把 GPU 视为"证据驱动的优化而非自动重写"；显式维护弃用边界（cuxfilter 26.06 终版、cuSpatial 已归档仅限隔离环境、warp.sim→Newton）；近似最近邻要求报 recall@k 而非拿精确 CPU 算法硬比；要求记录硬件/版本/种子以复现。
- **涉及资源**：NVIDIA CUDA GPU（CUDA 12/13）、RAPIDS 26.06 全家桶、Numba-CUDA(-MLIR)、Warp/Newton、PyTorch/JAX/TensorFlow（作为"勿迁出"边界）、Nsight Systems/Compute、S3/HTTP/WebHDFS 与 GPUDirect Storage。

### `umap-learn`
- **用途**：非线性降维 skill，覆盖 2D/3D 可视化嵌入、聚类预处理（配 HDBSCAN）、监督/半监督 UMAP、DensMAP、AlignedUMAP（时序对齐）和 Parametric UMAP（神经网络映射）。
- **技能设计**：结构最轻的之一——SKILL.md 单文件承载参数调优指南、聚类工作流、transform/inverse_transform、进阶特性与排障清单，仅 1 个 `references/api_reference.md` 承接完整类签名；按"参数语义表"组织知识而非分阶段流程。
- **典型工作流**：按 metric 匹配预处理（欧氏则标准化，cosine/二值勿盲目标准化）→ 设 n_neighbors/min_dist/n_components/metric → fit_transform → 可视化或喂 HDBSCAN；聚类与可视化用两套不同参数的嵌入；新数据用 transform()，分布漂移大则改 Parametric UMAP。
- **特色**：四主参数给了"取值→效果→推荐区间"三段式调优法，并区分"聚类专用配置"（n_neighbors=30、min_dist=0、n_components=5-10）与可视化配置；诚实声明 UMAP 不完全保密度、可能制造假簇，需验证；提醒 random_state 权衡确定性与吞吐；连"本地 umap.py 遮蔽包名"这类坑都写进 FAQ。
- **涉及资源**：umap-learn 0.5.12（依赖 scikit-learn≥1.6、numba、pynndescent、numpy、scipy）、hdbscan、TensorFlow/Keras（Parametric UMAP extra）、matplotlib、umap-learn readthedocs 与 GitHub。

### `networkx`
- **用途**：复杂网络/图的创建、分析、生成与可视化——中心性、最短路、社区发现、合成网络模型、多种图文件格式 I/O；面向社交/生物/交通/引文/知识图谱等关系数据。
- **技能设计**：SKILL.md 按 5 大能力域（图基础/算法/生成器/IO/可视化）各给代码速查并挂对应 `references/` 文档（graph-basics/algorithms/generators/io/visualization），尾部附"基本操作/核心算法/文件 IO"三张 Quick Reference 速查表；无 scripts、无路由表，是平铺式手册型。
- **典型工作流**：创建或加载图（edgelist/GraphML/pandas/邻接阵）→ 检查结构（节点数/密度/连通性）→ 算指标与算法（中心性、最短路、社区）→ spring_layout 等布局绘图（随机种子固定）→ 导出 GraphML 及指标 CSV。
- **特色**：明确版本边界（3.x，列出 nx.info/write_gpickle/read_shp/random_tree 等已移除 API 及替代）；内置 NetworkX 3.x 加速后端机制（nx-cugraph/nx-parallel/graphblas-algorithms，backend= 关键字零改码加速）；大图建议稀疏矩阵、子图加载、近似算法；提醒 JSON node-link 在 3.6 后用 "edges" 键。
- **涉及资源**：networkx 3.6（Python≥3.11）、matplotlib、pandas、NumPy/SciPy 稀疏矩阵、Plotly/PyVis（交互可视化）、可选后端 nx-cugraph/nx-parallel/graphblas-algorithms、networkx.org 官方文档/教程/画廊。

---## Imaging & microscopy

### `histolab`
- **用途**：数字病理学中全切片图像（WSI）的轻量级切片（tile）提取与预处理库，覆盖组织检测、掩膜生成、三种切片提取策略（随机/网格/评分）、图像与形态学滤波、H&E 染色归一化（Reinhard/Macenko）和可视化。定位是简单流水线和数据集准备，复杂空间蛋白质组学或深度学习场景明确让位给 pathml。
- **技能设计**：主文件是"总览 + 最佳实践 + 排障"结构，核心细节下放到 7 个 `references/`（含 core_capabilities.md 六大能力汇总、typical_workflows.md 五条端到端工作流，以及按主题分的 slide 管理、组织掩膜、切片提取、滤波预处理、可视化专题文档）；没有 `scripts/`，全部是纯 API 知识。
- **典型工作流**：加载 Slide 并检查属性/金字塔层级 → 生成缩略图确认组织存在 → 预览掩膜（locate_mask）→ 用 locate_tiles 预览切片位置 → 选择合适的 tiler 提取 → 需要时做染色归一化和质量过滤，ScoreTiler 可输出 CSV 评分报告。
- **特色**：强制"先预览再提取"的操作纪律（locate_tiles/locate_mask），按场景给出 tiler 选择指南（RandomTiler 采样、GridTiler 全覆盖、ScoreTiler 质量驱动），性能与 QC 建议具体到 tissue_percent 阈值、金字塔层级、可复现 seed；不支持 Windows，需 OpenSlide 系统库。
- **涉及资源**：histolab 0.7.0（Python 3.8–3.11）、OpenSlide 系统库、pooch（下载内置 TCGA 样例切片）、uv 包管理器。

### `pathml`
- **用途**：面向本地科研用途的计算病理学全流程工具链：加载/切片幻灯片、构建预处理与 QC 流水线、管理 .h5path 数据、多重免疫荧光图像定量、空间图构建、有边界的模型推理规划。明确声明是 beta 研究软件，非医疗器械、不可用于诊断。
- **技能设计**：最重"治理"的一个 skill——开篇即安全边界与去标识化五步要求，随后是版本基线（锁定 3.0.5，解释 PyPI 与 GitHub 版本不一致的坑）、可复现安装、稳定最小工作流。6 个 `references/` 按主题分层（图像加载/预处理/数据管理/多参数/空间图/机器学习），6 个 `scripts/` 是无网络、拒绝 URL 和符号链接的本地 CLI（清单校验、流水线规划、图像 QC、空间模式校验、推理规划）。
- **典型工作流**：8 步研究流程——本地清单盘点 → 按患者冻结数据划分 → 估算 tile 数/内存/输出规模做边界规划 → 少量样本试点预处理（不碰测试集）→ 全量运行并保留坐标/掩膜/QC 记录 → 审慎构建空间数据 → 有界批量推理 → 报告来源与局限。
- **特色**：默认无网络 + 显式同意门禁——任何会下载模型的类（SegmentMIFRemote 从 HuggingFace 拉 ONNX、PanNukeDataModule 连 Warwick）都需用户明确同意后才能实例化；模型代码安全一节强调 pickle/.pt 文件视同可执行代码、校验 SHA-256、ONNX 也非天然可信；先小样本手动跑通再全量的纪律。
- **涉及资源**：PathML 3.0.5（GPL-2.0，Python 3.10–3.12）、OpenSlide、Java/Bio-Formats、Torch/ONNX、HuggingFace、Warwick/Zenodo（数据集，默认关闭）、ReadTheDocs 文档。

### `pydicom`
- **用途**：DICOM 文件读写、检视、变换与安全预检——元数据提取、传输语法与压缩插件、帧级像素解码、私有元素、DICOM JSON，以及有边界的去标识化审查。定位是通用 DICOM 框架而非诊断查看器。
- **技能设计**：开篇"强制性安全边界"（PHI 风险、禁止默认打印 Dataset、去标识化不等于合规声明、假名化密钥按机密管理、解析不可信数据前设输入/帧数/字节上限），然后是"按需选工作流"路由表（7 个场景对应 7 个脚本），主体按 API 主题组织。仅 2 个 `references/`（common_tags、transfer_syntaxes），8 个本地无网络 CLI 脚本。
- **典型工作流**：先跑 extract_metadata/dicom_inventory 做聚合清点 → 需要压缩时先用 transfer_syntax_inspector 查已装编解码器 → pixel_frame_planner 规划帧与内存 → 单帧非诊断渲染需显式 `--acknowledge-pixel-phi` → 去标识化走 9 步流程（定目的/选配置文件/保原件/递归处理/UID 一致替换/日期策略/像素检查/重建文件元/审计+专家复核）。
- **特色**：安全设计最严密——allowlist 输出、UID 结构性标签不可动、假名化密钥从密钥管理器取并锁权限、脚本故意把 PatientIdentityRemoved 设为 NO 因为无法证明去标识成功；附 pydicom 3.0 迁移注意事项（dcmread/enforce_file_format/pydicom.pixels 等）。
- **涉及资源**：pydicom 3.0.2（修 CVE-2026-32711 路径穿越）、pylibjpeg 系列插件、pyjpegls、python-gdcm、NumPy/Pillow、DICOM 标准（PS3.3/3.5/3.6/3.15）、PyPI/GitHub/官方文档。

### `flowio`
- **用途**：流式细胞术 FCS 2.0/3.0/3.1 文件的底层读写库——读取 HEADER/TEXT/ANALYSIS 元数据与通道信息、事件数据转 NumPy 数组、多数据集旧文件、写出 FCS 3.1 文件、导出供 pandas/下游工具用。明确不做补偿、logicle 变换、分群或 FlowJo 工作区（那是 FlowKit 的事）。
- **技能设计**：主文件含"操作工作流"6 步准则 + "关键语义"章节（TEXT 键小写化并去 $、事件的两种表示、通道双编号约定、写接口的刻意限制）+ 3 个 Quick Start；5 个 `references/` 按"只读当前任务需要的"原则分（API 参考/工作流/FCS 语义/排障/上游来源），1 个 inspect_fcs.py 检查器脚本。
- **典型工作流**：先分清是元数据清点还是事件提取 → 用 only_text=True 只读元数据（大文件尤其）→ 显式选择 as_array(preprocess=True/False) 并记录决定 → 默认严格解析、不静默吞偏移错误 → 导出时只导需要的字段（TEXT 可能含受试者/操作员标识）→ 写出后重新打开验证往返一致。
- **特色**：以"非协商检查"清单收尾（不得声称 FlowIO 做补偿/分群、不得把 preprocess=True 当原始采集值、不得传二维数组给 create_fcs、事件加载非流式/分块）；检查器脚本默认拒大文件、可配偏移恢复选项；标量化的语义陷阱（键名规范化、$ 字符被整体移除）讲得很细。
- **涉及资源**：FlowIO 1.4.0（Python 3.9–3.13）、NumPy（随装）、可选 pandas、uv；本地解析无需凭证或网络；FCS 3.1 规范文献。

### `openpiv`
- **用途**：粒子图像测速（PIV）分析——从实验图像对提取二维速度场，覆盖互相关窗口参数调优、伪向量验证与替换、平滑、物理单位缩放，以及涡量/应变率/湍流统计量计算。面向流体力学与流动可视化实验，明确 CFD 模拟不在此范围。
- **技能设计**：主文件按"核心概念（PIV 原理、窗口参数、信噪比）→ 常用操作（动态掩膜、多遍处理）→ 验证与后处理 → 可视化 → 派生分析"组织，重点标注版本陷阱（针对 0.25.4 验证，API 版本间会变）；仅 1 个 `references/`（advanced_algorithms.md，讲相关与亚像素方法、多遍窗口变形、PIVSettings、3D 模块）；`scripts/` 有 runner.py（CLI 全流水线）、analyze.py（PIVAnalyzer 派生量）、run_example.py（官方测试图验证安装）。
- **典型工作流**：读入图像对 → 预处理（可选动态掩膜）→ extended_search_area_piv 互相关 + get_coordinates → sig2noise/global_val/local_median_val 布尔 OR 组合验证 → replace_outliers 插值或 NaN 丢弃（二选一）→ scaling.uniform 缩放到物理单位 → 可视化/计算涡量应变；或直接 `runner.py --image a --image b` 一条命令。
- **特色**：满篇"单位陷阱"警告最突出——验证阈值要按 u/v 的实际单位（px/s vs px/frame）设置、windef 多遍输出的 dt/scaling_factor 字段是摆设需事后换算、replace_outliers 传错方法名会静默返回全零核、smoothn 不接受 NaN、y 轴方向约定会影响导数符号——都是实测踩坑结论而非文档复述。
- **涉及资源**：openpiv 0.25.4（BSD-3）、numpy/scipy/scikit-image/matplotlib（随依赖）、matplotlib Agg 后端、官方 bundled 测试图；安装后无需网络。

### `imaging-data-commons`
- **用途**：查询和下载 NCI 影像数据共享平台（IDC）的公开癌症影像数据（CT/MR/PET 放射学 + 切片显微镜病理），包括按癌症类型/模态/部位筛选队列、下载 DICOM、浏览器可视化、许可证与引用检查。免认证。
- **技能设计**：最复杂的多路径路由设计——开篇即"先选访问路径"决策树（有 MCP 服务器→用它；已装 idc-index→全用它；只读元数据→REST API curl 不装任何东西；要下载数据→装 idc-index 约 77MB）。13 个 `references/` 每个对应一条访问路径或专题（REST/MCP/Parquet/BigQuery/DICOMweb/云存储/许可证/临床数据/数字病理等），并附"何时加载"导航表；1 个 check_version.py 只检测不安装。
- **典型工作流**：核对会话路径与 IDC 版本（当前 v24）→ 枚举过滤值再筛选（猜 Modality 字符串是空结果首因，癌症类型在 collections_index 需 JOIN）→ sql_query 得 DataFrame 提取 UID 列表 → download_from_selection/download_dicom_series 下载（两个方法参数顺序相反，最常见坑）→ get_viewer_URL 浏览器查看 → 查许可证（97% CC BY、3% CC BY-NC 且按 series 附加）并生成引用。
- **特色**：强调"IDC 数据内容问题绝不用网络搜索、只查索引（索引是权威源）"；对每个易错点给出精确防呆（下载文件名是 CRDC UUID 而非 DICOM UID、prior_versions_index 只用于复现不可当版本历史、REST v3 之外的 v1/v2 将停用、mixed-license 队列按最严条款）；Portal 只供人工浏览、禁止写进脚本。
- **涉及资源**：idc-index 0.12.5（本地 DuckDB 索引）、IDC REST/MCP API、AWS S3 与 GCS 公开桶（免流量费）、DICOMweb 代理或 Google Healthcare API、BigQuery（需 GCP 认证）、s5cmd/AWS CLI/gsutil、OHIF/SLIM 查看器、pandas/pydicom/SimpleITK/3D Slicer/QuPath。

### `bids`
- **用途**：BIDS（脑影像数据结构）社区标准的操作指南——把神经科学/生物医学数据（MRI、EEG、MEG、iEEG、PET、显微镜、NIRS、运动捕捉、EMG、波谱、行为数据共 11 种模态）组织成标准目录结构、用 PyBIDS 查询、bids-validator 验证、DICOM 转 BIDS、写元数据 sidecar、建衍生数据集。
- **技能设计**：主文件是"概览 + 12 个工作流领域清单 + 8 个常见问题速查 + 10 条最佳实践 + BEP（扩展提案）状态表 + 工具生态表"；`references/` 里除 core_workflows.md 外还有独特的机器可读资产——bids_schema.json（官方 schema 原样拷贝，实体/命名/后缀的权威源）、beps.yml（BEP 列表）、metadata_fields.md（各模态 sidecar 必填/推荐字段）、conversion_tools.md；`scripts/update_schema.py` 可刷新 schema 和 BEP。
- **典型工作流**：规划命名（最好采集前定好，ReroIn 约定支持自动转换）→ 建目录结构与 dataset_description.json → HeuDiConv/dcm2bids/BIDScoin 从 DICOM 转换 → 用 PyBIDS BIDSLayout 查询（可 SQLite 缓存索引）→ 每次改动后跑 validator（早验证勤验证）→ 加 participants.tsv/scans.tsv/README → 共享前去脸（pydeface）并上传 OpenNeuro/DANDI。
- **特色**：覆盖最广的"生态型" skill——不仅讲标准本身，还整理 BIDS-Apps（fMRIPrep/MRIQC/QSIPrep）、TemplateFlow、DataLad、CuBIDS 等周边工具矩阵和当前全部 BEP 进展（如 BEP032 神经像素探针）；把 bids-examples 仓库定位为各模态的规范参照；常见问题直击实战痛点（相位编码方向 i/j/k、SliceTiming 手工补、TSV 用 n/a 而非 NA）。
- **涉及资源**：PyBIDS、bids-validator-deno（Deno）、HeuDiConv/dcm2bids/BIDScoin、nibabel/pydicom、BIDS Specification（readthedocs）及 schema.json、bids-website/bids-schema/bids-examples GitHub 仓库、OpenNeuro/DANDI 数据库、pydeface 等去脸工具。

---

## Chemistry & molecules

### `rdkit`
- **用途**：化学信息学底层工具库，用于分子读写（SMILES/SDF/InChI）、描述符计算、指纹与相似度、子结构搜索、化学反应处理、2D/3D 坐标生成与可视化，服务于药物发现与计算化学研究。定位是"精细控制"，简单场景应改用 datamol 封装。
- **技能设计**：SKILL.md 主体是一张 12 个能力领域的路由表，正文只放概览和常见陷阱，细节下沉到 `references/` 下五个文档（核心能力、最佳实践、API 索引、描述符清单、SMARTS 模式库），`scripts/` 附三个可运行示例脚本。
- **典型工作流**：安装（conda-forge 或 uv pip）→ 按能力表定位需求 → 查 core_capabilities.md 获取带代码的用法 → 解析分子后先校验非 None、按需手动 sanitize / AddHs → 计算/搜索/绘图；复用 scripts 模板。
- **特色**：强调版本基线核查（RDKit 2026.03.3）与安装陷阱（conda 与 PyPI 混装风险）；明确列出六大常见坑（解析返回 None、sanitize 失败用 DetectChemistryProblems 调试、MolSupplier 线程安全等）；明确区分"本地捆绑资源"与"可安装包名"避免误引用。
- **涉及资源**：RDKit（conda-forge / PyPI）、Avalon 指纹、ETKDG/MMFF 力场；无外部云服务，属纯本地库；引用 arXiv 论文作为出处。

### `medchem`
- **用途**：药物化学化合物库分诊与过滤，把文献中的类药性规则（Lipinski、Veber、CNS、lead-like）、结构警示目录（PAINS、Brenk、ChEMBL、NIBR）、复杂度阈值（ZINC 百分位）和官能团检测打包成可批量并行的 Python API。
- **技能设计**：单文档分八大能力模块（规则、结构警示、命名目录、函数式 API、化学基团、复杂度、骨架约束、查询语言），每个模块配可运行代码段；`references/` 仅两个补充文档（API 指南 + 规则目录），`scripts/` 一个批量过滤 CLI。
- **典型工作流**：读 CSV/SDF → datamol 转 Mol 列表 → RuleFilters 批量跑多条规则得 DataFrame（含 pass_all / 描述符列）→ 用 QueryFilter 组合表达式（如 `MATCHRULE(...) AND NOT HASALERT("pains")`）→ 导出过滤结果并保留 status/reasons 列做审计。
- **特色**：独有 medchem 查询语言，用类 SQL 表达式组合规则/警示/属性/子结构条件；返回类型双轨制（类返回 DataFrame、functional 返回布尔掩码）；明确提醒规则是"情境性指南"而非硬标准（上市药常违反 Ro5）；Lilly demerit 过滤器需额外装原生二进制。
- **涉及资源**：medchem / datamol / RDKit（PyPI）、lilly-medchem-rules（conda-forge 原生包）、ZINC-15 百分位阈值数据、ChEMBL 与 NIBR 警示集、官方文档站 medchem-docs.datamol.io。

### `molfeat`
- **用途**：分子特征化枢纽，统一 100+ 种特征化器（ECFP、MACCS、描述符、预训练 ChemBERTa/GIN/Graphormer 等），把 SMILES 转成 ML 可用向量，面向 QSAR 建模、虚拟筛选、相似度检索与深度学习。
- **技能设计**：以"三层抽象"为骨架——calc（单分子计算器）、trans（sklearn 兼容批量转换器）、trans.pretrained（预训练模型转换器）；`references/` 四份文档形成选型漏斗（选型指南 → 特征化器全目录 → API → 示例），并提示用 grep 在目录中检索。
- **典型工作流**：锁定 Python 3.9–3.10 环境 → 按 extras 安装（transformer/dgl/graphormer 等）→ 用 ModelStore 浏览/搜索/加载预训练模型 → FPCalculator + MoleculeTransformer 批量并行特征化（ignore_errors 处理脏数据）→ to_state_yaml_file 保存配置保证可复现。
- **特色**：严格版本护栏（0.11.0 不支持 Python 3.11+，DGL 需 ≤2.0）；强调可复现性（配置 YAML + 版本记录）；安全意识——禁用 pickle 存嵌入（可执行任意代码），改用 NumPy .npz；提供 featurizer 选型速查表（维度/速度/用途）。
- **涉及资源**：molfeat + datamol + PyTorch（PyPI）、DGL/Graphormer/PyG 生态、外部 MAP4（reymond-group GitHub）、HuggingFace 系预训练模型（ChemBERTa、ChemGPT、MolT5）、官方文档与 Valence Labs 教程。

### `pytdc`
- **用途**：通过 PyTDC 包使用 Therapeutics Data Commons——发现治疗学 ML 任务、加载获批数据集、做任务感知的切分（scaffold/cold/time 等）、用标准评估器打分、跑基准组，以及有界的分子打分 oracle 工作流。
- **技能设计**：全 skill 以"数据与网络政策"为核心架构：五步不可协商流程（先发现→再计划→用户批准→才执行→输出有界）；`references/` 四份（数据集、工具语义、oracle、来源考证），`scripts/` 五个 CLI 全部设计为"计划模式 + 显式 --execute/--download 开关"，外加缓存审计脚本。
- **典型工作流**：建 CPython 3.11 隔离环境并钉死 setuptools 80.9.0 + PyTDC 1.1.15 → discover_metadata.py 无下载发现任务/数据集 → 记录数据集、许可、大小、切分、度量、种子 → 获批后加 --execute 真正加载与切分 → 用 Evaluator 精确名称打分 → 基准组 evaluate_many 聚合。
- **特色**：最重的安全/治理设计——构造函数即触发下载，故强制先批后载；对切分语义"不过度声称"（scaffold 切分不等于无泄漏，random 切分验证集用固定种子）；反对使用未 documented 的 API 拼写；输出只给计数/schema/小预览；代码 MIT 但数据许可异质需逐条核查。
- **涉及资源**：PyTDC 1.1.15（PyPI 源码包）、Harvard Dataverse 文件端点、mims-harvard/TDC 仓库、tdc.readthedocs.io（仅作交叉参考）、BiGG 级模型/remote oracle 等可选网络服务、cellxgene-census 与受限版 RDKit 依赖。

### `matchms`
- **用途**：串联质谱（MS/MS）的导入、清洗、处理与比对：库搜索打分、元数据归一、峰过滤、十余种相似度度量（余弦/改进余弦/中性丢失/熵等）、打分矩阵与分子相似网络。明确划界：LC-MS 特征检测与蛋白组学应改用 pyopenms。
- **技能设计**：以"修正破坏性 API 变更"为特色的守则式结构——正文先给 8 步操作工作流，再给"当前 API 护栏"清单（旧教程必踩的坑）；`references/` 六份按需加载（导入导出、过滤、相似度、工作流、迁移表、来源），`scripts/` 一个可复现库搜索 CLI。
- **典型工作流**：检查输入（格式/谱数/前体覆盖/离子模式）→ 开元数据归一加载 → 查询与参考谱用完全相同的峰处理流水线（default_filters 先行 + SpectrumProcessor）→ 按科学问题选相似度类 → 预估 N×M 对数再 calculate_scores → 用 scores_by_query 提取 top-k 并保留分数+匹配峰数 → 镜像图与正交证据人工验证。
- **特色**：版本敏感度极高（0.27/0.32 移除了 add_losses、ModifiedCosine 等），提供专门 migration.md 迁移映射；多项"不可协商检查"（不得跨处理差异比对、不得无前体元数据跑改进余弦、不得把高分当确认鉴定、不得反序列化不可信 pickle、不得无预估跑全对全比对）；拒绝设定万能鉴定阈值。
- **涉及资源**：matchms 0.33.1（Python 3.10–3.14，自带 RDKit）、metabolomics-USI 网络加载（需联网）、MGF/MSP/mzML/mzXML/mzSpecLib 等格式生态、官方 matchms 文档与文献。

### `pyopenms`
- **用途**：OpenMS 的 Python 绑定，覆盖完整质谱分析平台：蛋白组学与代谢组学工作流——特征检测、肽/蛋白鉴定、无标记与同位素标记定量、加合物与精确质量注释、端到端 LC-MS/MS 流水线及大量文件格式互转。
- **技能设计**："脚本优先"架构：SKILL.md 主体就是 15 个参数化 CLI 脚本的分类目录（检查转换/特征检测定量/注释/鉴定/化学/靶向可视化），明确指示"先跑脚本、脚本不合适再下钻 Python API 与 references/"；`references/` 六份按主题划分（文件 IO、信号处理、特征检测、鉴定、代谢组学、数据结构）。
- **典型工作流**：inspect_ms_data.py 先摸底 → convert_format.py / process_spectra.py 做格式转换与信号处理（平滑、质心化、归一）→ 代谢组走 MassTraceDetection→ElutionPeakDetection→FeatureFindingMetabo，蛋白组走 FeatureFinderAlgorithmPicked → align_link_quantify.py 多样本对齐联配出定量矩阵 → detect_adducts / accurate_mass_search 注释 → process_identifications.py 做 FDR 过滤。
- **特色**：专门列出 3.5.0 破坏性 API 变更（FeatureFinder("centroided") 已移除、idXML 需 PeptideIdentificationList、加合物语法改为 `H:+:0.4`、get_df 小写列名）；指出 pip wheel 缺 HMDB2StructMapping.tsv 并让脚本自检说明；Param 对象调试模式与 pandas 导出模式开箱即用。
- **涉及资源**：pyopenms 3.5.0（PyPI）、HMDB 数据库、GNPS FBMN 与 SIRIUS 导出格式、Escher（JSON 兼容）、pandas/NumPy/scikit-learn/Matplotlib 生态、readthedocs 官方文档。

### `cobrapy`
- **用途**：约束代谢建模（COBRA）的 Python 实现，用于基因组尺度代谢模型的加载/构建/导出、FBA/pFBA/FVA/通量采样、基因敲除筛选、培养基优化与 gap-filling，面向系统生物学与代谢工程。
- **技能设计**：十大能力区（模型管理、结构、FBA、FVA、敲除、培养基、采样、生产包络、gapfill、从头建模）各配代码块的教程式单文档；`references/` 两份（工作流合集 + API 速查，含 DictList/GPR/EX_ 符号约定等关键概念）；无脚本目录，靠代码示例驱动。
- **典型工作流**：load_model 取教材模型（textbook 本地免网）或远程 BiGG 模型 → slim_optimize 先验模型可行 → 按需做 single/double_gene_deletion 找必需基因 → flux_variability_analysis 看通量范围、sample 采样分布 → minimal_medium / production_envelope 做培养基与产率分析 → with model 上下文管理器内做临时修改自动回滚 → 写回 SBML。
- **特色**：上下文管理器保证状态安全；强调求解器选择（GLPK 默认，大 MILP 用 hybrid/OSQP，osqp 直连将弃用）；通量采样必须 sampler.validate 校验数值稳定；明确 gapfill 所需通用反应库在 0.31+ 不再捆绑需自备；提醒大规模并行从小 n 和单进程起步。
- **涉及资源**：cobra 0.31.1（PyPI）、swiglpk/GLPK 及可选 CPLEX/Gurobi/HIGHS/OSQP 求解器、BiGG 与 BioModels 模型库（联网）、optlang 抽象层、Escher 可视化兼容、cobrapy.readthedocs.io。

---## Chemistry & molecules (续)

### `torchdrug`
- **用途**：围绕 TorchDrug 0.2.1 这套 PyTorch 图学习栈构建和排障代码，覆盖分子图性质预测、自监督预训练、分子生成、逆合成、蛋白质表征学习与知识图谱推理；只要代码 `import torchdrug` 或用到其 datasets/models/tasks/Engine 就该启用。
- **技能设计**：主文档按"版本守卫 → 安装 → 规范工作流 → 7 条可靠代码规则 → 排错 → 参考索引"组织；8 个 references 按任务域切分（核心概念、数据集、模型架构、性质预测、蛋白质、生成、逆合成、知识图谱），SKILL.md 内每个任务方向都路由到对应 reference，形成任务→文档的索引表。
- **典型工作流**：先跑版本守卫检查 Python/PyTorch/torchdrug 三元组是否落在支持矩阵内；再按 datasets → models → tasks → core.Engine 四段式组装（如 ClinTox → GIN → PropertyPrediction → Engine）；复杂任务（预训练微调、双视图逆合成、组合 task）按 reference 中的分步说明执行；出错时对照安装/维度/设备/检查点四类排错小节。
- **特色**：钉死 0.2.1 并明确"官方文档不是滚动最新版站点"，把 Python 3.11+/PyTorch 2.1+ 视为未验证组合而非默认兼容；专治 LLM 臆造 API——点名不存在的 `protein.residue_graph()`、废弃的 feature 别名、"model/task/engine 参数不得混传"；torch-scatter/torch-cluster 轮子必须匹配 PyTorch+CUDA+ABI，Apple Silicon 仅 CPU；生成分子只作候选不得当作可合成化合物。
- **涉及资源**：torchdrug.ai 官方文档/教程/安装页/API、GitHub DeepGraphLearning/torchdrug 0.2.1 release、PyG 轮子索引 data.pyg.org；自动下载数据集 ClinTox/BBBP/Tox21/QM9/ZINC250k/USPTO50k/FB15k237 等。

### `pymatgen`
- **用途**：用当前 pymatgen API 分析、验证、转换和变换材料结构及计算材料学数据——成分、周期结构、对称性、相图、能带/DOS、VASP/Q-Chem 文件 I/O，外加边界清晰的 Materials Project 数据库查询。
- **技能设计**：六者中工程化最重：8 个 `scripts/` CLI（结构验证器、结构分析器、对称性敏感度报告、I/O 转换规划器、转换器、相图生成器、MP 查询器、工件清单）+ 5 个 references；主文档开头即列 12 条"必需工作流"，本质是一份信息收集清单（周期性、单位、坐标模式、无序度、tolerance、来源链）。
- **典型工作流**：先声明对象类型/单位/坐标模式 → 捆绑验证器做本地结构摄入并逐条检查 parser 警告 → 验证后才做对称性，且扫描 symprec 网格出敏感度报告而非只报单值 → 转换先跑不打开文件的 dry-run 规划器、再显式承认损失后只写新路径 → 相图只接受带溯源的严格 JSON 兼容能量 → MP 查询默认 dry-run，加 `--execute` 才联网且单次有界。
- **特色**：网络默认关闭，API key 只认 `MP_API_KEY` 命名环境变量（禁 CLI 传参/翻 .env）；提示恶意 CIF 代码执行 CVE（≤2024.2.8），不可信文件不得在特权进程解析；禁 pickle，用 schema 校验 JSON；pymatgen 与 pymatgen-core 双钉版本防未来 core 漂移；反复强调 MP 数据是方法相关的计算值（PBE 系统性低估带隙），相图 hull 不等于实验稳定性。
- **涉及资源**：PyPI 上 pymatgen 2026.5.4 / pymatgen-core 2026.7.16 / mp-api 0.46.4；pymatgen.org 文档与 changelog；Materials Project API（需 key，CC BY 4.0，有引用要求）；VASP/Q-Chem（商业许可，POTCAR 不可再分发）；可选外部可执行 enumlib/Bader/packmol/ffmpeg/Zeo++。

### `sympy`
- **用途**：在 Python 里做精确符号数学——代数化简、微积分、解方程（含微分方程）、符号线性代数、物理力学/量子、数论几何，以及 lambdify/LaTeX/代码生成。指引明确划分边界：浮点近似够用时改用 NumPy/SciPy。
- **技能设计**：单一主文档 + 5 个能力域 references（core-capabilities、matrices-linear-algebra、physics-mechanics、advanced-topics、code-generation-printing），每个 reference 在主文档里标注"Load when"触发条件，构成按主题的按需加载路由；主文档本身含安装、触发条件、七能力清单、6 条最佳实践、3 个使用模式、速查表和排错。
- **典型工作流**：先 `symbols()` 定义符号并尽量带 assumptions（positive/real 等）→ 全程用 `Rational`/`S` 保持精确、避免 `0.5` 浮点污染 → 按问题类型选求解器（solveset 代数、linsolve 线性、nonlinsolve 非线性、dsolve ODE）→ 解后代回验证 `simplify(...) == 0` → 需要数值时 `evalf(n)` 或批量场景 `lambdify` 转 NumPy 函数 → 输出 LaTeX/pretty 格式。
- **特色**：最佳实践全部围绕"精确性"这一核心红线（假设改善化简、精确算术、循环内禁用 subs/evalf）；排错覆盖五类典型坑（未定义符号、意外数值、性能慢、解不动、化简不动）；目录中同时存在 `core-capabilities.md` 与 `core_capabilities.md` 两种命名的文件，是明显的遗留重复。
- **涉及资源**：SymPy 1.14+（Python 3.9+）；可选 NumPy/SciPy/Matplotlib（lambdify 与绘图）、C/Fortran 编译器（autowrap/codegen）；docs.sympy.org 文档/教程/API 和 GitHub examples；纯本地计算，无云服务或外部数据库。

### `molecular-dynamics`
- **用途**：用 OpenMM 运行分子动力学模拟、用 MDAnalysis 分析轨迹，覆盖体系搭建、力场定义、能量最小化、平衡与生产模拟，以及 RMSD/RMSF/接触分析。面向结构生物学、药物结合、生物物理问题（突变影响、构象采样、膜蛋白、内在无序蛋白等）。
- **技能设计**：主文档以"带 docstring 的可复制函数库"形式组织，模拟侧按四阶段各给一段完整代码（体系制备、能量最小化、NVT 平衡、NPT 生产），另有力场选择表、PDBFixer/OpenFF 体系制备工具节；分析侧仅 1 个 `reference`（mdanalysis_analysis.md，讲 Universe/AtomGroup/原子选择语言）。
- **典型工作流**：原始 PDB 先用 PDBFixer 补缺失残基/原子、加氢 → Modeller 加溶剂盒（10 Å padding、150 mM NaCl）→ createSystem（PME 静电、HBonds 约束支持 2 fs 步长）→ 能量最小化消空间位阻 → NVT 平衡（约 50-100 ps）→ 加 MC barostat 后 NPT 平衡与生产并保存 checkpoint → MDAnalysis 加载轨迹、对齐后计算 RMSD/RMSF/蛋白-配体接触；分析时丢弃前 20-50% 的平衡段。
- **特色**：力场速查表按体系类型推荐组合（标准蛋白 AMBER14+TIP3P-FB、膜蛋白 CHARMM36m、无序蛋白 ff19SB）；平台自动降级链 CUDA→OpenCL→CPU；最佳实践强调"MD 前必最小化""只分析平衡后轨迹""必须存 checkpoint"；小分子配体参数化交给 OpenFF/GAFF2 而非手写。
- **涉及资源**：OpenMM、MDAnalysis、PDBFixer、OpenFF Toolkit、nglview（conda-forge/uv 安装）；openmm.org 与 docs.mdanalysis.org 文档；备选引擎 GROMACS、NAMD；在线建模工具 CHARMM-GUI、AmberTools。

### `glycoengineering`
- **用途**：分析和工程化蛋白质糖基化：扫描 N-糖 sequon（N-X-S/T，X≠P）、启发式预测 O-糖热点、接入糖工程专用工具与糖数据库。面向治疗性抗体优化（ADCC/免疫原性/半衰期）、疫苗糖盾设计、生物类似药表征和靶点分析。
- **技能设计**：主文档内嵌可直接运行的纯 Python 函数（sequon 扫描与报告、糖位点消除/新增突变、O 糖热点打分），外接工具编目（NetOGlyc、GlycoSHIELD、GlycoWorkbench、GlyConnect、UniCarbKB）+ 1 个 reference 汇总糖数据库（GlyTouCan/GlyConnect/UniCarbKB/KEGG Glycan/CAZy 及预测服务器）；另配治疗抗体策略表和常用突变表（N297A/Q、F243L 等）与 IUPAC 糖符号速查。
- **典型工作流**：序列输入 `find_n_glycosylation_sequons` 生成位点报告 → 决定消除（保守 N→Q 替换）或新增糖位点（改 N 并保证 +1≠P、+2=S/T）→ O 糖先用 Ser/Thr 密度启发式初筛 → 上 DTU 的 NetNGlyc/NetOGlyc 网页服务精确预测 → 需评估屏蔽效应时用 GlycoSHIELD 把预模拟糖构象库嫁接到结构上 → GlyConnect 查实验验证位点。
- **特色**：明确启发式只是快速 baseline、不能替代 NetOGlyc；对 GlycoSHIELD 给出罕见的安装陷阱细节（不在 PyPI、须 git clone + 单独下载糖库、GlycoSASA 需 PATH 里的 GROMACS gmx），并诚实标注示例命令未实际运行、仅按 argparse 与教程示意；反复提醒"预测到 sequon ≠ 实际被糖基化"（受可及性/细胞类型/构象影响），抗体工作永远先看 Fc N297。
- **涉及资源**：DTU Health Tech 预测服务（NetNGlyc 1.0/NetOGlyc 4.0）；GlycoSHIELD（MPG GitLab + glycoshield.eu，GPL-3.0，依赖 GROMACS/MDAnalysis）；GlycoWorkbench（EuroCarbDB）；GlyConnect、GlyTouCan、UniCarbKB、KEGG Glycan、CAZy、CFG 等糖数据库；纯 Python（re）即可本地扫描。

### `deepchem`
- **用途**：化学机器学习的"全家桶"——分子/材料/生医数据加载与特征化、性质预测（溶解度/毒性/ADMET）、MoleculeNet 基准、GNN 训练与预训练模型迁移学习。描述里显式划界：图优先的 PyTorch 工作流用 torchdrug，基准数据集用 pytdc。
- **技能设计**：主文档 + 4 个 references（core_capabilities 八大能力、typical_workflows 三个端到端模板、workflows 八个详细工作流、api_reference 含 50+ 模型目录与 featurizer 选择指南）+ 3 个 `scripts/` 生产级脚本（predict_solubility.py、graph_neural_network.py、transfer_learning.py，均支持自定义 CSV），SKILL.md 标明何时查哪个 reference。
- **典型工作流**：加载数据（CSV/SDF/蛋白序列）→ 选 featurizer（指纹配传统 ML、描述符配可解释模型、图特征配 GNN）→ 用 ScaffoldSplitter 划分（分子数据的"诚实默认"）→ NormalizationTransformer 归一目标 → 按"从简单到复杂"升级：RF+指纹起步 → XGBoost → >5K 样本上深度学习 → >10K 样本上 GNN → 小数据/新骨架用迁移学习（ChemBERTa/GROVER/MolFormer）→ 评估并预测。
- **特色**：反数据泄漏立场最强（随机划分会让相似分子同时进训练/测试集，必须 scaffold split）；给出按样本量分档的模型升级路线图；针对懒加载后端讲清 extras 匹配（`deepchem[torch/tensorflow/jax]`，先装后端再装 extra），含 Conda+PyTorch 的 `mkl<2025` undefined symbol 修复；workflows 里保留"预测时要把 transformers 传给 predict() 才能反归一回真实单位"这类易错细节。
- **涉及资源**：DeepChem 2.8.0（Python 3.7-3.11）；RDKit 为核心依赖；可选 PyTorch/TensorFlow/JAX 后端及 dqc 可微调量子化学；预训练模型 ChemBERTa/GROVER/MolFormer；MoleculeNet 基准数据集（Tox21/BBBP/Delaney 等）；deepchem.readthedocs.io 文档与 GitHub 仓库。

---## Single-cell & genomics

### `alphagenome`
- **用途**：查询和计算 DeepMind AlphaGenome 对基因组变异的调控效应预测。分两层：一是 AlphaGenome Atlas（对 GRCh38 全部约 90 亿个 SNV 预计算好的 AVI 排名分数 + 18 项 SHAP 归因 + 各 assay 轨道分数），二是在线调用模型对 indel、小鼠变异、自定义窗口做变异打分、REF-vs-ALT 轨道预测和虚拟突变扫描（ISM）。面向 VCF/credible set 非编码变异优先级排序与机制解释，明确仅供研究、非临床。
- **技能设计**：SKILL.md 内嵌一张"When to use which"路由表，把 Atlas/模型/其他 skill（genomic-coordinates、folklore-variant-evidence 等）的分工讲清。三个 references 分主题（atlas.md 讲数据构成与 API、model-api.md 讲 dna_client 速查、interpretation.md 讲读数规则与报告清单）；`scripts/` 提供三个 CLI 脚本（atlas_query.py、score_variants.py、离线的 atlas_link.py 生成门户深链）。
- **典型工作流**：安装 alphagenome 包并设 API key → 用 `atlas_query.py scorers` 验证连通 → `avi` 子命令按变异/VCF/区间查 AVI 排名 → 按 top feature 归因决定机制方向，再用 `scores` 按组织/轨道细化 → `atlas_link.py` 生成门户链接附在报告中；Atlas 覆盖不了的（indel、小鼠）转 `score_variants.py` 或 Python SDK。
- **特色**：强调"排序而非一刀切阈值"、先看归因再看分数；坐标契约（1-based 闭区间、GRCh38-only、REF 写反会静默返回错误记录）单列成节；明示模型盲区（trans 效应、蛋白后果、非 polyA RNA 等）与访问层级/许可差异；密钥只从环境读取。
- **涉及资源**：alphagenome PyPI 包（0.9.0+）、DeepMind API（gdmscience.googleapis.com，免费非商业 key）、AlphaGenome Atlas 门户与 Tabix 下载站、GCS 上的 GENCODE v46 注释、AnnData/numpy/pandas/grpcio 技术栈。

### `biopython`
- **用途**：Biopython（1.87）综合分子生物学工具箱的操作指南，覆盖序列读写与格式转换（FASTA/GenBank/FASTQ/PDB 等）、比对、NCBI Entrez 数据库编程访问、BLAST 自动化、蛋白结构分析、系统发育树、motif 与群体遗传学。适合批量处理和自定义流水线；快速查询则路由到 gget，多服务集成到 bioservices。
- **技能设计**：典型的"模块地图 + 分域 references"结构——按 Biopython 的 7 个子包（Seq/Align/Entrez/Blast/PDB/Phylo/高级）各配一个独立 reference 文件（共 7 个 .md），SKILL.md 只给每域的用途说明 + 最小代码示例 + rg 搜索模式，让 agent 按域查参考再写代码。无 scripts/。
- **典型工作流**：识别任务所属模块 → Read 对应 reference 文件（或用 rg 在 references/ 里搜函数名）→ 提取代码模式改写 → 需要时组合多个模块（如 BLAST → 取 top hit → Entrez 拉序列）。文中附 4 个常见组合 Pattern 与最佳实践/排错清单。
- **特色**：强调 Entrez 合规（必设 email、API key 只从环境读、用 history/batching 控速）；版本敏感的坑位提示（1.86 移除 Bio.HMM、gap score 默认改 -1、CVE-2025-68463 修复需 1.87+）；面向可复现实验（显式 import、迭代器处理大文件、缓存下载）。
- **涉及资源**：Biopython 库本身、NumPy、NCBI Entrez/PubMed/BLAST 网络服务、可选本地 BLAST/MUSCLE 命令行工具、官方文档与 GitHub。

### `bulk-rnaseq`
- **用途**：bulk RNA-seq 差异表达分析的端到端编排器：从原始 FASTQ 到 QC/修剪、比对定量（STAR/Salmon）、基因级计数矩阵、差异表达（pydeseq2）、通路富集和出版级图表。目标是"defensible"（可复现、有质量门禁、统计上站得住）的完整流程；单细胞数据明确转 scanpy。
- **技能设计**：定位为"路由器而非重实现"——多数阶段复用仓库内已有 skill（nextflow、pydeseq2、pathway-enrichment、scientific-visualization），自己只补一个真空缺环节：quant 输出 → PyDESeq2 就绪的整数计数矩阵。SKILL.md 内含 mermaid 流程图、A/B 双路径决策表、逐阶段章节、9 条常见错误清单；4 个 references 各自独立成篇；`scripts/` 仅两个（build_counts_matrix.py、validate_samplesheet.py）。
- **典型工作流**：设计样本表（≥3 重复、查批次混杂）并校验 → FastQC/MultiQC 原始 QC → fastp/Trim Galore 修剪 → 二选一上游路径：A 路走 nf-core/rnaseq（推荐，自带 tximport 合并基因计数），B 路手动 STAR/Salmon/featureCounts（注意链特异性）→ B 路用 build_counts_matrix.py 组装矩阵 → 交接 pydeseq2 出 DE 表 → pathway-enrichment 做 GSEA/ORA → scientific-visualization 出火山图等。
- **特色**：强工程质量边界——版本全部钉死、9 条"最常见错误"逐条对应修正（TPM 不能喂 DESeq2、链特异性选错会静默丢一半 reads、Ensembl ID 忘映射导致富集全空等）；明确了估计计数取整进 PyDESeq2 的理由与 DESeq2+offset 路线的差异。
- **涉及资源**：nf-core/rnaseq（Nextflow）、FastQC/fastp/Trim Galore/STAR/Salmon/featureCounts/MultiQC、pytximport、下游 skill（pydeseq2、gseapy/gprofiler）、gget/database-lookup 做参考与 ID 映射。

### `cellxgene-census`
- **用途**：程序化查询 CZ CELLxGENE Census——一个版本化、标准化的公共单细胞 + 空间转录组数据集合（2025-11-08 LTS 含 2.17 亿细胞、1845 个数据集、人/鼠/猴/绒/黑猩猩）。用于按细胞类型/组织/疾病切片取表达数据、元数据统计、嵌入与空间数据访问、以及基于数千万细胞的机器学习训练；本地自有数据则转 scanpy/scvi-tools。
- **技能设计**：3 个 references 分层：census_schema.md（数据结构与过滤语法权威）、common_patterns.md 与 core_workflow_patterns.md（8 种核心模式，从开 Census、元数据探索、AnnData 切片、out-of-core 大查询、PyTorch 训练、空间数据到 Scanpy 集成）。SKILL.md 主体是最佳实践 + 元数据字段表 + 4 个用例代码 + 排错。无 scripts/，纯 Python API 模式。
- **典型工作流**：`open_soma(census_version=...)` 固定版本 → 第一步只查元数据（get_obs + value_filter，注意 is_primary_data == True 去重）评估规模 → 大于 ~10 万细胞改用 axis_query 核外处理 → 小切片 get_anndata 转 AnnData → 接 scanpy 常规分析，或 ExperimentDataset + tiledbsoma-ml 进 PyTorch 训练循环。
- **特色**：三条反复强化的纪律——固定 census_version 保证可复现、过滤必带 is_primary_data 防重复计数、先估查询规模再载入防内存爆；tissue vs tissue_general 粗细分级过滤；presence matrix 先确认基因是否被测过。
- **涉及资源**：cellxgene-census 1.17.x（Python 3.10–3.12）、TileDB-SOMA/SOMA-ML、AnnData/Scanpy、spatialdata（空间扩展）、CZ CELLxGENE Discover 云端数据（公开免认证）。

### `deepspot-m`
- **用途**：用 DeepSpot-M 多模态基础模型从 H&E 病理切片生成"虚拟空间转录组"：对每个 224×224、约 20x 的 tile，按 HGNC 基因符号查询约 1.9 万个蛋白编码基因的 log1p-CPM 表达预测值，拼成整张切片的基因表达空间图。适合无匹配检测的切片队列做标记基因图谱或为形态学流水线加表达通道。
- **技能设计**：轻量两层结构——SKILL.md 为主体（安装、模型访问、快速上手、tile 校验、依赖可选化、五种嵌入源选择表、全片流程），`references/` 仅 api.md（完整调用面/批处理/设备/符号处理）和 whole_slide.md（切片级工作循环与 AnnData 组装）。无 scripts/。
- **典型工作流**：装 deepspotm==1.0.0 → 在 Hugging Face 申请受限权重访问并 `huggingface-cli login` → `DeepSpotM.from_pretrained("ratschlab/DeepSpotM", source=...)` 选定基因嵌入源 → 用 histolab 在接近 0.5 µm/像素层切 224×224 tile 并保留坐标 → 批量 `predict_genes(batch, gene_list)` → 拼成 tile×gene 矩阵装入 AnnData 做下游空间分析。
- **特色**：强调把重依赖做成可选导入（ImportError 转成含全部步骤的提示）；tile 尺寸在流水线边界强制校验（重采样会破坏 backbone 读的纹理）；基因以可查询嵌入而非固定输出槽进入模型，能查训练未见基因；代码与权重双非商业许可（PolyForm-NC / CC-BY-NC-SA）需分开检查。
- **涉及资源**：PyPI 的 deepspotm 包、PyTorch（建议 CUDA）、Hugging Face 门控权重 ratschlab/DeepSpotM、五种冻结基因嵌入（Evo 2/Orthrus/ProtT5/scGPT/Apertus）、histolab skill 切片、AnnData；模型曾用于 TCGA 28,664 张切片图谱。

### `depmap`
- **用途**：查询 Broad 的 Cancer Dependency Map——数百条癌细胞系的基因组级 CRISPR 敲除依赖分数（Chronos）、RNAi（DEMETER2）和 PRISM 药物敏感性数据。典型问题：某基因在特定癌症是否选择性必需、什么基因组特征预测敲除敏感性（生物标志物）、合成致死搭档、共必需基因。用于肿瘤药物靶点验证。
- **技能设计**：SKILL.md 是"概念 + 内联代码函数库"风格：核心概念表（分数含义与阈值、细胞系注释字段）、6 段可复用 Python 函数（API 封装、依赖切片、选择性依赖、生物标志物 Mann-Whitney 检验、共必需相关）、3 个查询工作流、数据文件参考表与最佳实践。`references/` 仅 dependency_analysis.md，深入 Chronos 分数解读、阳/阴性对照基因与选择性计算。
- **典型工作流**：小查询走 DepMap REST API（requests + pandas）；大规模分析推荐下载数据文件（CRISPRGeneEffect.csv、sample_info.csv、突变/表达/拷贝/PRISM 等）→ 本地 pandas 分析。例如靶点验证流程：按癌症类型过滤细胞系 → 比较目标基因在癌种内外的平均基因效应 → 交叉参考突变/表达/CNA 作生物标志物 → 注意 FDR 校正。
- **特色**：强调方法论陷阱——区分泛必需（差靶点）与癌症选择性必需、用 DepMap_ID 而非细胞系名、拷贝数扩增造成的假必需、未表达基因必然打不出必需；明确用 Chronos 而非 DEMETER2 作为当前 CRISPR 标准。
- **涉及资源**：DepMap Portal 及其 REST API、DepMap 数据下载页/figshare（24Q4 等版本化数据集）、requests/pandas/scipy、参考论文（Behan 2019、Chronos Dempster 2021）。

### `dhdna-profiler`
- **用途**：从任意文本中提取作者的"认知指纹"（Digital Human DNA 框架）：分析一个人如何推理、决策、价值化和表达。触发场景包括"分析这个人的思维方式""我的思维风格是什么""认知画像"等；也支持两人文本的对比综合与基于会话历史的自我画像。属于本仓库中少数非科研工具类 skill。
- **技能设计**：纯 prompt 驱动（allowed-tools 仅 Read/Write，无代码、无网络）。核心框架是 12 个认知维度（分析深度、创造范围、情绪处理、语言精度、伦理推理、战略思维、记忆整合、社交智力、专业深度、直觉、时间取向、元认知），每维 1–10 打分，外加 6 组"张力对"（如分析↔直觉）——张力差本身即签名。references/advanced-profiling.md 补充分文体预设（学术/创意/商务/技术/日记）、认知熵（分数标准差）和 4D 纵向扩展。
- **典型工作流**：四阶段——证据收集（逐维找直接引文、结构模式、缺失即线索）→ 打分（每维 1–10 + 引证 + 高/中/低置信度）→ 模式综合（主导模式、影子模式、签名张力、推理拓扑 Linear/Spiral/Web/Dialectic/Fractal、决策指纹）→ 按固定 ASCII 模板输出画像（含张力图与 2–3 段叙事综合及关键引文）；多作者时追加对比综合。
- **特色**：伦理边界写得非常重——只分析用户当次带来的文本、不得主动翻找作者其他材料；给第三者（同事、候选人）的画像必须标注为推测；明确拒绝用于招聘/晋升/录取/临床等重大决策；自我画像前须先征得同意再回读会话；一切留在本地会话。另声明非人格测试、非智力评判、非静态定论。
- **涉及资源**：无外部依赖——不联网、不写文件到用户未指定处；仅引用 Zenodo 上的 DHDNA 预印本（DOI 10.5281/zenodo.18736629 等）与 AHK Strategies/themindbook.app 出品方信息。

### `diffdock`
- **用途**：操作 DiffDock / DiffDock-L 扩散模型做蛋白-小分子对接：从 PDB 结构或氨基酸序列 + SMILES/SDF/MOL2 预测配体 3D 结合构象并给置信度，支持批量对接与虚拟筛选。明确边界：只预测 pose 和置信度，不预测结合亲和力（ΔG/Kd）。
- **技能设计**：结构最全的 skill 之一——`scripts/` 三个辅助脚本（setup_check.py 环境体检、prepare_batch_csv.py 批量输入模板与校验、analyze_results.py 解析置信度排名导出 CSV），`references/` 三篇（参数全参考、置信度与局限、完整工作流示例），`assets/` 含批量 CSV 模板和带四种预设（高精度/快速筛选/柔性/刚性配体）的注释 YAML 配置。
- **典型工作流**：先 `setup_check.py` 验环境（conda environment.yml 或官方 Docker）→ 单对接：`python -m inference --protein_path/--protein_sequence --ligand_description --out_dir` 输出 rank1_confidence0.87.sdf 等 10 个采样 pose → 批量/虚拟筛选：准备 CSV（complex_name/protein_path/ligand_description/protein_sequence 四列）后跑 `--protein_ligand_csv`，>100 化合物时先预计算 ESM 蛋白嵌入 → analyze_results.py 按 >0 / −1.5~0 / <−1.5 分高中低置信度并排名。
- **特色**：把"置信度 ≠ 亲和力"贯穿始终，给出与 GNINA/MM-GBSA/FEP 打分函数的接力推荐流程；置信度解释表 + 大配体/多链/新蛋白家族的期望修正；明确不适用范围（蛋白-蛋白对接、>20 残基肽、共价对接、膜蛋白慎用）；提供本地 Gradio Web UI 与 HF 在线 demo 两种图形入口。
- **涉及资源**：DiffDock GitHub 仓库（v1.1.3，DiffDock-L 默认）、conda 环境或 rbgcsail/diffdock Docker 镜像、RDKit/PyTorch/PyG/CUDA、ESMFold 蛋白嵌入、GNINA/AmberTools/OpenMM 等下游打分工具、Hugging Face 在线 demo。

### `folklore-variant-evidence`
- **用途**：通过 Folklore 临床变异解读 MCP（Helena Bioinformatics 托管端点，免密钥）为单个公共胚系变异（GRCh38 SNV/简单 indel）取回结构化证据、自动化 ACMG/AMP 决策支持、溯源链接与关联文献，并可查询 ClinGen 基因-疾病有效性断言。核心价值是让 agent 对六种返回状态做确定性分支、保留证据出处。
- **技能设计**：围绕一份外部 MCP 契约组织（references/mcp-contract.md 是组包与解读前的必读）；SKILL.md 定义严格输入边界（恰好一个公共变异标识、剔除一切患者/表型/家系上下文）、要求实时 `tools/list` 验证目录而非依赖记忆，并给出 curl JSON-RPC 最小连接示例与一个可证伪的冒烟测试（rs80357914 应返回 ambiguous 并强制停下）。无 scripts/。
- **典型工作流**：剥离出唯一公共变异标识（坐标/HGVS/SPDI/rsID）→ 调 `search_variant_evidence`（assembly=GRCh38）→ 按状态分支：resolved 才可继续（复用 canonical_key）；ambiguous 必须列出候选让人显式选择、绝不自动挑；not_found/invalid_request/unsupported/resolution_unavailable 各有规定动作（尤其"服务不可用≠证据不存在"）→ resolved 后用 canonical_key 链 `search_variant_literature`（区分 exact_variant/variant_alias/gene_association 三种匹配）→ PMID 进 `get_publication_details`，或用 `search_literature_corpus` 做语义检索；基因-疾病断言走另两个独立工具。
- **特色**：安全与防过度声明是主轴——患者数据绝不入参、不得把结果包装成诊断/风险/治疗建议、报告必须附带固定边界声明、区分"返回事实"与"agent 综合"、空结果只代表本服务范围内无匹配；文献关联不改变 ACMG 分类；与其他 skill 的分工路由明确（database-lookup 管广查、genomic-coordinates 管坐标清洗）。
- **涉及资源**：api.helena.bio 的 Folklore MCP 端点（Streamable HTTP MCP，无需凭证）、GitHub 上 Apache-2.0 公共适配器与契约、底层 ClinVar/ClinGen/PubMed 等公共数据源、folklore.helena.bio 文档站。

### `genomic-coordinates`
- **用途**：解决基因组坐标在格式、工具、组装版本之间流转时的隐性错误——0-based 半开与 1-based 闭区间的换算、BED/GFF/VCF/SAM 等十余种格式的约定差异、indel 归一化与等价性判断、GRCh37/hg19/GRCh38 组装不匹配检测，以及基因组↔转录本↔蛋白坐标转换。核心信条是"坐标是三个事实：数值、约定、组装版本"，缺一不可解读。
- **技能设计**：4 个独立脚本（convert_coords、normalize_variant、check_contigs、audit_intervals）+ 4 个 references（格式约定表、变异表示、参考构建签名、转录本坐标）。SKILL.md 即速查手册，内置换算规则表、格式分类表、审计发现表。纯标准库、零网络依赖。
- **典型工作流**：查表换算坐标 → 归一化变异前先用 FASTA 校验 REF（MISMATCH 说明组装不符，转 check_contigs）→ 审计文件是否违反自身格式约定（如 BED 出现大量零长度特征即 1-based 数据误写）→ 报告结果时坐标旁必标组装版本。
- **特色**：强调"最安静的 bug"——错一碱基的 BED 能正常排序和取交集，混合组装的 join 照样返回结果而不报错。check_contigs 用主染色体长度签名识别组装（含 GRCh37 与 hg19 仅差线粒体 2bp 这类陷阱）；audit 可作 CI 门禁（致命发现时退出码 1）；零长度 BED 插入点标记为 unrepresentable 而非给出错误结果。
- **涉及资源**：零外部依赖；仅需参考 FASTA（含 .fai 索引）。脚本只用 Python 标准库，无网络访问。

### `genomic-intelligence`
- **用途**：调用 Genomic Intelligence 托管的 DNA 语言模型，从序列直接预测调控特征与基因结构——启动子、剪接位点、增强子活性、染色质状态、序列到表达量（log TPM）、从头基因注释，外加"找基因+预测表达"复合工作流。面向本地无 GPU、不想下模型权重的场景。
- **技能设计**：纯客户端 skill，无 `scripts/`，4 个 references（任务输出结构、REST 与认证、MCP 工具清单、序列获取）。SKILL.md 详尽记录六个任务各自的长度上下限、闭合 options 模式、错误码表，并注明与 OpenAPI 版本的对齐情况。
- **典型工作流**：先获取序列（基因符号/区域 → Ensembl 取号、或本地 FASTA/演示序列）→ MCP 上先拿 sequence_ref 句柄再 predict（序列不进上下文）；REST 上直接 POST /v1/tasks/{task}/predict → annotation 任务用异步提交+轮询 → 校验 meta 里的 `scored_window` 确认打分窗口正确。
- **特色**：对契约细节极度较真——expression 任务必须精确切 9,198 bp TSS 居中窗口且 tss_index 错了不报错只会"自信地答错"，skill 教你断言响应字段自查；hosted MCP 免 key 可用；模型默认 ID 故意不写死，要求运行时发现。
- **涉及资源**：api.genomicintelligence.ai（REST，需 GI_API_KEY）、mcp.genomicintelligence.ai（免 key MCP demo）、docs.genomicintelligence.ai、Ensembl REST（取序列）；Python requests 库。

### `gtars`
- **用途**：本地基因组区间运算——重叠检测与计数、集合代数（并/交/差/最近邻）、consensus 合并、coverage、BED tokenization（为 ML 准备）、片段处理，以及 refget 参考序列存储与 BEDbase 缓存规划。覆盖 Python 绑定、Rust 与 CLI 三种形态。
- **技能设计**：6 个 references（python-api、overlap、coverage、tokenizers、refget、cli）+ 6 个自研"确定性 CLI"审计脚本（bed_validator、execution_plan、tokenizer_manifest 等），全部本地、无网络、不写文件。结构上以"数据契约 → 本地安全工作流 → 网络门禁"分层推进。
- **典型工作流**：先盘点本地文件与组装 → bed_validator 校验（坐标、排序、contig 命名）→ 小合成文件试跑 → 按 Python/CLI/Rust 选调用面（imports 来自子模块）→ 设资源硬限 → 运行后复验排序、行数、校验和。远程功能（from_pretrained、open_remote）需显式审批后才用。
- **特色**：安全边界最重的一个——原生 PyO3 扩展与 Cargo 构建被当作代码执行对待，要求校验所有者、版本、SHA-256 后再装；明确列出上游文档漂移（如 RegionSet 排序行为、strand 默认丢失）和 1.1 已移除的过时 API；强调按患者切分数据防泄漏。
- **涉及资源**：PyPI gtars==0.9.2、crates.io gtars/gtars-cli/gtars-refget、Hugging Face（tokenizer 下载，需审批）、refget 远程服务、api.bedbase.org（BEDbase 缓存，需审批）、uv/cargo 工具链。

### `hugging-science`
- **用途**：面向科学领域（生物、化学、物理、天文、气候、材料等 17 个方向）的 AI/ML 资源发现与使用——找数据集、找模型、找方法论博客、调用交互式 Space demo。本质是一个高信噪比的策展目录，解决"科学 ML 问题前先找对资源"的问题。
- **技能设计**：1 个 fetch_catalog.py 脚本（结构化拉取目录）+ 5 个 references（slug 清单与条目 schema、数据集/模型/Spaces 各自的用法、各领域旗舰资源）。SKILL.md 定义了清晰的五步循环，并对"何时不用"（通用 ML 任务）做了界定。
- **典型工作流**：识别领域映射到 topic slug → fetch_catalog.py 拉 topics/<slug>.md 或搜索 → 按 scale 适配、许可证、模态匹配、新旧版本权衡选资源（拿不准时给用户列 2-3 个候选）→ 按 references 里的模式加载（streaming、Inference API、gradio_client）→ 引用对应的方法论博客。
- **特色**：强调"目录是指针不是 API"——条目最终都通过标准 HF 工具使用；`trust_remote_code=True` 在此生态常见但等于执行任意代码，必须先征得用户同意；目录 404 时重取而非掩盖失败；大科学数据集默认流式加载。
- **涉及资源**：huggingscience.co（llms.txt/llms-full.txt/topics/*.md）、huggingface.co/hugging-science 组织及 HF Hub（datasets/transformers/Inference API/Inference Providers）、gradio_client（调 BoltzGen 等 ~27 个 Space）、HF_TOKEN 认证（.env 加载）。

### `onekgpd`
- **用途**：对 1000 Genomes Project（3,202 个 GRCh38 全基因组测序个体）做个体级查询——哪些个体携带某区域符合条件（AF、ClinVar、AlphaMissense、VEP 后果等）的变异、某位置谁是纯合参考、两个体的亲缘关系（等级+KING 系数），以及离线的群体/家系元数据查询。
- **技能设计**：两个脚本双轨制——onekgpd_api.py（联网查询，统一处理连接/分页/流式）与 onekgpd_meta.py（纯离线，用 skill 自带数据文件答群体问题），两者用同一套样本 ID 可组合。2 个 references（全命令参数表与 JSON schema、受控词表）。SKILL.md 内含"命令选择指南"路由表和 Common Mistakes 章节。
- **典型工作流**：第一步强制做坐标溯源（基因符号 → 权威来源解析为 GRCh38 坐标，因错坐标会无错返回错误位置的结果）→ 先 count 再 select（计数便宜，先估规模）→ 需要时把样本名单喂给 select-variants-in-samples 看具体变异。大 JSON 落盘用 jq 提取，不整读进上下文。
- **特色**："count before you select"纪律与坐标溯源是结构性规则而非建议；明确记录隐蔽语义（AF=0.0 表示"该来源未收录"而非频率为零；am_score=0.0 表示未打分而非良性；count-samples-hom-ref 的 -1/0/>0 哨兵值）；无任何凭据。
- **涉及资源**：公共 1000 Genomes 查询端点（dnaerys.org/online 托管）、国际基因组计划/IGSR 数据使用条款、Ensembl（坐标解析）、uv 运行时；元数据层完全离线。

### `pacsomatic`
- **用途**：nf-core/pacsomatic 配对肿瘤-正常（matched tumor-normal）流程的运维工具箱——从 BAM 输入验证、生成合规 samplesheet、构建可复现的 Nextflow 启动产物，到本地运行或提交 LSF/Slurm/PBS/SGE 调度器，以及启动失败的初步分诊。
- **技能设计**：单一入口 `scripts/run_pacsomatic.py` 承载全部逻辑（验证+产物生成+执行），配 config.yaml 基线配置、3 个 references（agent 剧本、配置与输出说明、流程指南）与单元测试。SKILL.md 明确路由规则：默认 dry-run，禁止绕过助手手拼 nextflow 命令。
- **典型工作流**：收集必需输入（肿瘤/正常 BAM、患者与样本 ID、输出目录、fasta 或 genome 二选一）→ 校验路径与运行时依赖 → 自动生成 samplesheet（patient,sample,status,bam,pbi）与 params YAML + 启动脚本 → dry-run 停在产物生成，或 --run 本地执行/提交调度器并返回 job ID。
- **特色**：定义了"Agent Response Contract"——每次响应必须含确切命令/脚本路径、验证确认、dry-run vs run 类型、job ID、一个具体下一步；失败时按 .nextflow.log → pipeline_info → 任务日志顺序分诊；范围边界清晰（不做深度生物学解读、不动流程内部）。
- **涉及资源**：nf-core/pacsomatic 上游流程（GitHub）、Nextflow 运行时、容器（singularity profile）、LSF/Slurm/PBS/SGE 调度器；纯本地工具无需外部数据服务。

### `pathogen-variant-surveillance`
- **用途**：通过 GenSpectrum LAPIS API 查询活的病原体基因组监测数据——当下流行哪些毒株谱系、增长速度如何、携带什么突变、PCR 引物/检测靶点是否仍匹配。覆盖 SARS-CoV-2、H5N1/流感、RSV、猴痘、麻疹、登革热等十余个实例。
- **技能设计**：4 个脚本（resolve_lineage、lineage_prevalence、mutation_profile、reporting_lag）+ 3 个 references（LAPIS API 与陷阱、谱系命名体系、监测数据注意事项）。SKILL.md 以"陷阱表"为骨架，记录了经实测验证的静默错误。全部标准库、无 key。
- **典型工作流**：先跑 `lineage_prevalence --top 5` 从数据里发现实际流行谱系（而非凭记忆点名）→ resolve_lineage 核对名字是否仍存在/已被撤回/别名展开 → 报告任何近期数据前先跑 reporting_lag 确认可信回溯窗口（如 H5N1 近两个月基本盲区）→ prevalence 带 Wilson 区间与增长斜率，mutation_profile 可对比两个谱系的获得/丢失突变。
- **特色**：铁律是"绝不凭记忆陈述流行情况或写出谱系名"——命名体系是活数据结构（6000+ Pango 名、294 个已撤回、XFG 这类重组需查 alias_key.json）；每周报数据版本；增长斜率明确标注为描述性而非传播力估计；脚本运行时读取各实例 databaseConfig 自适应字段名差异。
- **涉及资源**：lapis.cov-spectrum.org、lapis.genspectrum.org、lapis.pathoplexus.org 三个公共 LAPIS 实例、raw.githubusercontent.com（pango-designation 的 alias_key.json/lineage_notes.txt）；Python 标准库，无 API key。

### `phylogenetics`
- **用途**：构建与分析系统发育树——重建基因/蛋白/基因组的演化历史，用于演化关系推断、病毒谱系动力学（疫情传播与传代时间）、蛋白家族分析、水平基因转移检测、祖先序列重建、分子钟分析、16S/核心基因组物种树等。
- **技能设计**：教程式单文档 skill，无路由表；SKILL.md 主体是六步流水线的完整可复用 Python 函数（run_mafft、trim_alignment_trimal、run_iqtree、run_fasttree、ETE3 分析可视化、full_phylogenetic_analysis 串联）+ 模型选择指南表与最佳实践清单，附 1 个 iqtree 参考。
- **典型工作流**：MAFFT 多序列比对（按规模选方法：<200 序列用 linsi/einsi，>1000 用 fftns）→ TrimAl 可选修整比对列 → IQ-TREE 2 构建最大似然树（-m TEST 自动选模型，-B 1000 超快 bootstrap；时间采样加 --date 做分子钟）或大数据集改用 FastTree → ETE3 中点根化、算统计量、渲染树图。
- **特色**：最"传统"的一个——以代码模板与方法论经验为主（模型选择表、bootstrap 建议、重组检查提示 RDP4/GARD），而非安全门禁或 API 契约；对工具适配给出量化阈值（>5000 序列用 FastTree，快 10-100 倍）；强调比对质量是树可靠性的前提。
- **涉及资源**：MAFFT、IQ-TREE 2、FastTree、TrimAl（bioconda 安装）、ETE3+PyQt5（Python）、FigTree/iTOL（可视化）；另列 MUSCLE、RDP4、GARD 等备选工具，全部本地运行。

### `pydeseq2`
- **用途**：用 PyDESeq2（DESeq2 的 Python 实现）做 bulk RNA-seq 差异表达分析——单/多因素设计（含批次效应校正）、Wald 检验、BH 校正、apeGLM LFC 收缩、火山图/MA 图可视化，以及从 R DESeq2 工作流迁移到 Python。
- **技能设计**：1 个完整命令行脚本 run_deseq2_analysis.py（加载校验、过滤、全流程、导出 CSV+AnnData/H5AD、可选画图）+ 4 个 references（核心六步、分析模式、API 参考、工作流指南）。SKILL.md 采用 Quick Start 速通 + 核心步骤 + 故障排查 + "关键提醒"八条的渐进结构。
- **典型工作流**：载入原始整数计数矩阵（务必转置为样本×基因）+ 元数据 → 过滤低计数基因（总和 <10）→ 显式设定参考水平并声明 design 公式（调整变量在前）→ DeseqDataSet 拟合 → DeseqStats 指定 contrast 做 Wald 检验 → 可选 LFC 收缩仅用于排序/可视化（p 值仍基于未收缩值）→ 按 padj<0.05 筛显著基因并导出。
- **特色**：把统计纪律写进规则——显著性只看 padj 不看原始 p 值；收缩后与未收缩的 LFC 用途严格区分；记录 PyDESeq2 0.5.x 特有的收缩系数格式；常见坑（索引不匹配、设计矩阵不满秩即变量混杂）配有诊断代码（crosstab 检查）；pickle 只信自己生成的。
- **涉及资源**：PyPI pydeseq2==0.5.4 及 pandas/numpy/scipy/scikit-learn/anndata/formulaic 依赖、matplotlib/seaborn 可选；官方文档 pydeseq2.readthedocs.io、scverse/PyDESeq2 GitHub、Muzellec 2023 与 Love 2014 论文；全本地无网络服务。

### `pysam`
- **用途**：面向基于 Python/HTSlib 的基因组文件低层流式读写，覆盖 SAM/BAM/CRAM 比对、VCF/BCF 变异、FASTA/FASTQ 序列及 tabix 索引表（BED/GFF/GTF），包括 pileup、覆盖度、索引和 CRAM 参考序列处理。
- **技能设计**：SKILL.md 主体 + 9 篇 `references/` 按主题拆分（比对、变异、序列文件、坐标系与索引、CRAM 与性能、通用工作流、API、迁移指南、来源），末尾有"需求→文档"路由表。另附 4 个只读/安全脚本（inspect_hts、alignment_qc、variant_summary、filter_alignments），全部拒绝覆盖已有输出。写代码前有"First Decide"六步信息收集清单。
- **典型工作流**：先识别文件真实格式/排序/索引 → 用 inspect_hts.py 做元数据探查 → 按坐标契约（数值坐标 0-based 半开、region 字符串 1-based 闭区间）选 fetch/pileup 方式读 → 需要批量成熟操作时走 pysam.samtools/bcftools 命令分发器 → 按写入规则产出新文件并用 quickcheck 校验。
- **特色**：以"坐标契约"专章强制区分两套坐标系，是最常见的踩坑点；明确列出 11 条常见失败模式和 pileup/count/count_coverage 的默认过滤差异；0.24 迁移要点（CRAM 3.1 默认、HTSlib 不再默认连 EBI 参考服务器）单独成文；安全边界包括不覆盖已有输出、不切分不可信 shell 命令拼参数。
- **涉及资源**：pysam 0.24.0（封装 HTSlib/samtools/bcftools 1.23.1）、Python 3.8–3.14、本地文件与参考 FASTA（REF_PATH/REF_CACHE 可选）、官方安装与规范文档链接。

### `scanpy`
- **用途**：单细胞 RNA-seq 标准分析流水线：QC、归一化、降维（PCA/UMAP/t-SNE）、Leiden 聚类、标志基因、细胞类型注释、伪批量差异分析和出版级可视化；也负责把 Seurat/SingleCellExperiment 等 R 格式转换为 h5ad。
- **技能设计**：最大亮点是 15 个可组合 CLI 脚本（h5ad 进/出、共享 `_common.py` 助手），覆盖全流程并有一键 `run_pipeline.py`；明确"优先跑脚本而非手写代码"。`references/` 五篇（标准工作流、分析工作流、API、绘图指南、R 互操作 runbook），`assets/` 提供分析模板和 JSON 配置/细胞类型映射/基因集模板。
- **典型工作流**：两条路径——一键 `run_pipeline.py raw.h5ad -o processed.h5ad`（支持 `--config` JSON 复现参数），或分步链式：qc_analysis → preprocess（保留 counts 层和 raw）→ reduce_dimensions → （batch_correct harmony/bbknn/combat）→ cluster（多分辨率）→ find_markers → 写映射 JSON → annotate → score/pseudobulk/plot。R 对象先用 Rscript 转换再读入。
- **特色**：脚本优先的"防手滑"设计；强调保存原始计数（`.raw`）、先看 QC 图再定阈值、Leiden 替代已弃用的 louvain；警示 `rank_genes_groups` 的 p 值不能当作严格的条件间 DE（要 pseudobulk + pydeseq2）；与 anndata、scvi-tools 技能有明确的分工互引。
- **涉及资源**：scanpy 1.12.x（Python 3.12+，`[leiden]` extra 装 igraph/leidenalg）、anndata ≥0.10、可选 Dask（大规模）与 rapids-singlecell（GPU）、R + zellkonverter/SeuratDisk 转换链、pydeseq2、Scrublet、官方教程与 Luecken & Theis (2019) 最佳实践。

### `scvi-tools`
- **用途**：用深度生成模型（VAE + 变分推断）做单细胞组学的高级建模：概率式批次校正、跨研究整合、带不确定性的差异表达、多模态联合建模（CITE-seq/multiome）、空间转录组去卷积、细胞注释与迁移学习。
- **技能设计**：`references/` 按数据模态分文件路由（scRNA-seq、ATAC-seq、多模态、空间、专门模态、差异表达、理论基础、工作流）。无脚本，以统一 API 模式为核心：setup_anndata → train → 提取表示。特别强调模型命名空间区分（`scvi.model` vs `scvi.external`）。
- **典型工作流**：加载 AnnData → scanpy 预处理（过滤低计数基因、选 HVG）→ `setup_anndata` 注册原始计数层和批次/协变量 → 创建并训练模型 → `get_latent_representation`/`get_normalized_expression` 存回 obsm/layers → 回到 scanpy 做 neighbors/UMAP/Leiden；DE 用 `differential_expression(mode="change")`；模型可 save/load 持久化。
- **特色**：所有模型共享一致的五步 API，AnnData 为中心与 scverse 无缝衔接；强制使用未归一化原始计数是关键约束；概率式 DE 提供效应量阈值（delta）而非仅 p 值；支持 PyTorch/JAX/MLX 三后端；与 scanpy 技能明确分工（标准流程用 scanpy，深度模型用本技能）。
- **涉及资源**：scvi-tools 1.4.3（Python 3.12+）、PyTorch/PyTorch Lightning（可选 `[cuda]`）、JaxSCVI 与 Apple silicon 实验性 MLX 后端、scanpy/anndata 生态、官方 docs.scvi-tools.org 教程与 API。

### `scvelo`
- **用途**：RNA velocity 分析——利用 unspliced/spliced mRNA 动力学比值推断细胞状态转换方向，在快照数据上重建发育轨迹、预测细胞命运、估计 latent time 并识别驱动基因，作为 Scanpy/scVI 的方向性补充。
- **技能设计**：单一 SKILL.md、无 `references/` 无脚本，但内含完整可复制的分步教程和封装好的 `run_rna_velocity()` 一键函数；附 AnnData 输出字段对照表、三种速度模型（stochastic/deterministic/dynamical）对比表和排错表。兼容性注明 scvelo 0.3.4 的 dynamical 模型需 pandas<3、stochastic 需 numpy<2。
- **典型工作流**：上游先用 velocyto/STARsolo/kallisto|bustools(lamanno)/alevin-fry 生成 spliced/unspliced 计数 → 读 loom 并与已处理的 AnnData merge → filter_and_normalize + scanpy 的 log1p/HVG/neighbors → moments → 先 stochastic 探索、终稿用 dynamical（recover_dynamics 较慢）→ velocity_graph → latent_time/rank_velocity_genes/velocity_confidence → 流线图、相图与 PAGA 图。
- **特色**：明确 0.3 版行为变化（filter_and_normalize 不再做 log/HVG，须由 scanpy 补齐）；给出生物学合理性自检（根细胞 unspliced/spliced 比应高、箭头方向要符合已知生物学）；最低约 2000 细胞、短读长覆盖不足 intron 等实用性边界。
- **涉及资源**：scvelo 0.3.4（Python 3.10+）、scanpy/anndata、velocyto/STARsolo/kallisto-bustubs/alevin-fry（预处理）、loom 文件格式、CellRank 与 dynamo（延伸工具）、官方文档与 Bergen et al. 2020 论文。

### `tiledbvcf`
- **用途**：基于 TileDB 稀疏数组技术对人群基因组 VCF/BCF 变异数据做高效存储与检索：可扩展摄取、增量添加样本（免合并）、并行区域查询和导出，面向队列研究、GWAS 数据准备等群体基因组学场景（开源版定位 <1000 样本）。
- **技能设计**：单文件结构、无 `references/` 无脚本，按"核心能力四分类"（建库摄取/查询过滤/导出互操作/群体基因组工作流）组织；含 Python API、CLI 子命令（create/store/export/list/stat）双轨示例，以及内存管理、坐标系统、云存储等关键概念小节；末尾为向 TileDB-Cloud 迁移的完整章节与清单。
- **典型工作流**：conda/mamba 或 Docker 安装 → `Dataset(uri, mode="w")` 创建数据集 → 摄取带索引（.csi/.tbi）的单样本 VCF（多样本 VCF 不支持）→ `read(regions, samples, attrs)` 做区域+样本并行查询 → 需要时 `export` 回 VCF/TSV；进阶用 read_allele_frequency、sample_qc；规模化后按清单迁移 TileDB-Cloud 做分布式摄取与查询。
- **特色**：增量加样本无需昂贵合并是该技术核心卖点；坐标遵循 VCF 的 1-based 闭区间约定（BED 风格自动换算）；强约束是仅接受带索引的单样本 VCF；明确了内存预算、流式查询、区域合并等性能技巧和"多写者导致损坏"的并发警告；开源/云两段式定位有清晰迁移判断清单（>1000 样本或 >100GB 建议上云）。
- **涉及资源**：TileDB-VCF（C++ 库 + Python/CLI）、tiledb-py、conda-forge/bioconda/tiledb 频道、Docker 镜像、S3/Azure/GCS 云存储、TileDB-Cloud 平台（tiledb-cloud 客户端 + `[life-sciences]`、TILEDB_REST_TOKEN 认证）、TileDB Academy 文档。

### `deeptools`
- **用途**：NGS 命令行分析套件：BAM→bigWig 覆盖度转换与归一化、样本 QC（指纹图、相关性、PCA）、TSS/peak 周边热图与信号剖面，服务 ChIP-seq、RNA-seq、ATAC-seq、MNase-seq 等实验的可视化与比较。
- **技能设计**：SKILL.md + 6 篇 `references/`（核心工作流、工作流、逐工具参考含全部 21 个命令、归一化方法、有效基因组大小、另有 `assets/quick_reference.md` 速查卡）。附 2 个辅助脚本：`validate_files.py` 输入校验器和 `workflow_generator.py` 工作流模板生成器（chipseq_qc/chipseq_analysis/rnaseq_coverage/atacseq 四种）。还有按用户类型（新手/老手/具体任务）分流的应答策略。
- **典型工作流**：先 validate_files.py 校验 BAM 索引/BED 格式 → workflow_generator.py 生成定制 bash 流程或手写命令 → 按"先 QC 后分析"顺序执行（plotFingerprint→correlation→computeMatrix→plotHeatmap/plotProfile）→ 小区域 `--region` 试参数再全基因组跑 → 文档化保存命令以复现。
- **特色**：归一化方法选择是核心知识（RPGC/CPM/RPKM/BPM 按实验类型给速查表）；按实验类型的安全边界非常细——ChIP-seq 必须 extendReads 而 RNA-seq 绝不能（跨剪接位点）、ATAC-seq 要做 Tn5 偏移校正（--ATACshift）、GC 校正后不可再 ignoreDuplicates；内置常见物种（hg38/T2T/mm39 等）有效基因组大小表。
- **涉及资源**：deepTools 3.5.6（Python >3.8，uv 或 conda/bioconda 安装）、samtools（建 BAM 索引）、pyBigWig 等 PyPI 依赖、BAM/bigWig/BED 文件格式、上游 bioconda 渠道与 HPC 文档。

---## Research workflow & meta-skills

### `literature-review`
- **用途**：执行系统性文献综述、荟萃分析、研究综合，覆盖生物医学与理工多领域。面向写论文/学位论文的文献综述章节、调研某领域研究现状、识别研究空白，最终产出带核验引用的 Markdown 与 PDF 专业文档。
- **技能设计**：references/ 存放七阶段核心工作流、数据库检索策略、引用格式指南（APA/Nature/Vancouver 等）和完整示例；scripts/ 含引用核验、检索结果去重格式化、PDF 生成、示意图生成四个脚本；assets/ 提供全章节综述模板。SKILL.md 按阶段组织，附最佳实践与十大常见错误清单。
- **典型工作流**：①规划与界定（纳入/排除标准）→②多数据库系统检索并逐条记录检索式和日期→③题目/摘要/全文三级筛选并记录计数（供 PRISMA 流程图）→④结构化数据提取与质量评估→⑤按主题而非逐篇综合→⑥逐条引用跑脚本核验→⑦汇编带完整文献表的专业文档。
- **特色**：强制要求每篇综述至少 1–2 张 AI 生成示意图（如 PRISMA 流程图），"没有图的综述不完整"；把可复现性提到原则高度——"无法复现自己检索过程的综述不是系统综述"；通过复用 gget/bioservices/datacommons 等技能访问专业数据库。
- **涉及资源**：parallel-cli（parallel-web 技能）做网络搜索与全文提取；PubMed、arXiv、bioRxiv、Semantic Scholar 等数据库；PRISMA、Cochrane Handbook、AMSTAR 2、MeSH Browser 等指南与工具；pandoc + LaTeX 生成 PDF；Python requests。

### `research-lookup`
- **用途**：为科研手稿或研究简报汇编外部学术证据，默认目标是产出含 60 条"已核验且去重"参考文献的手稿级研究包（证据矩阵、论断-来源映射、章节简报），而不是一堆链接。随便的事实问答和 PRISMA 式系统综述都不在其范围内（后者转交 literature-review）。
- **技能设计**：核心是一个路由脚本 research_lookup.py（配 manuscript_packet.py 打包器），SKILL.md 内置一张明确的"Parallel 优先路由表"，把不同需求映射到 Parallel Search/Extract/Research/Chat 或 Perplexity；手稿上下文用 `--context-file` 传入 PICO 式 JSON 字段，缺失信息不编造。
- **典型工作流**：①收集手稿上下文（研究问题、设计、人群、暴露、对照、结局、目标期刊）→②运行学术证据管线：6 轮有界检索（近期原研、系统综述、奠基文献、方法学、矛盾/阴性证据、兜底无限制检索）→③用 Parallel Extract 批量核验候选来源的 DOI/PMID、设计、样本量、撤稿状态等→④写出 9 类 packet 文件（含 references.bib、evidence-matrix、claim-source-map、coverage、search-ledger）→⑤按"引言/方法依据/讨论"安全地使用证据，Results 只用用户自己的数据。
- **特色**：十条硬性参考质量规则——按 DOI/PMID 去重、剔除撤稿文献、保留矛盾与阴性证据、引用数只作次级信号、不足 60 条时报告缺口而非拿弱记录凑数、付费墙后只看摘要就不得声称读过全文；原始 Parallel 响应全部留档可审计；所有返回网页内容一律视为不可信数据而非指令。
- **涉及资源**：api.parallel.ai（需 parallel-cli 0.7.1+；Chat 后端直连需 PARALLEL_API_KEY）；优先检索 PubMed/PMC、Europe PMC、Crossref、OpenAlex、Semantic Scholar、arXiv/bioRxiv/medRxiv；可选 OpenRouter→Perplexity 兜底（OPENROUTER_API_KEY）。

### `research-grants`
- **用途**：撰写面向 NSF、NIH、DOE、DARPA 及台湾国科会（NSTC）的竞争性科研基金申请书，覆盖机构专属格式、评审标准、预算编制、broader impacts、意义与创新叙事及提交合规；也用于重提交时回应评审意见。
- **技能设计**：纯知识型技能，无脚本。references/ 按机构分文件（nsf/nih/doe/darpa/nstc 五份 guidelines）加通用主题（核心章节、评审标准、写作原则、提案类型与重提交、broader impacts、specific aims 指南）；assets/ 提供 NSF 项目摘要、NIH Specific Aims、预算说明三类模板。SKILL.md 本身给出五大机构速览、五阶段时间线和五类常见错误清单。
- **典型工作流**：五阶段推进——①规划准备（截止前 2–6 个月：锁定机会、组队、补预实验数据、拟 aims 大纲）→②起草（先写 specific aims，再展开叙述、配图、时间线、预算、支持信）→③内部评审（同事反馈、模拟评审、修订）→④定稿（表单、biosketch、数据管理计划、校对）→⑤提交（提前 24–48 小时上传并确认编号）。
- **特色**：机构差异化做得很细——NSF"智力价值+更广泛影响"并重与 15 页上限、NIH Aims 1 页+Research Strategy 12 页、DARPA 强调高风险高回报的原型与转化路径、NSTC 要求中英双语摘要与必配研究架构图（CM03 表）；反复强调"截止前 48 小时提交"；配图可选 scientific-schematics（`--doc-type grant`），并明确披露提示词会发送到第三方 OpenRouter。
- **涉及资源**：核心指引完全离线；机构官方政策（如 NSF PAPPG 24-1）为权威依据；可选 scientific-schematics 生成图表（需 OPENROUTER_API_KEY）；与 literature-review、research-lookup、scientific-writing、peer-review 等技能联动。

### `scientific-writing`
- **用途**：起草、修订和审计科研手稿或报告，核心诉求是证据可溯源、报告规范覆盖、作者问责与保密控制。适用于手稿章节、参考文献、声明、图表和投稿准备——凡是"科学准确性与可追溯性重要"的写作场景，且绝不编造证据。
- **技能设计**：以结构化登记表为中心——assets/ 提供来源清单、声明登记（存声明文本哈希）、一致性清单、作者身份、报告覆盖度等模板；scripts/ 有 8 个完全本地、零依赖、零网络的 CLI（脚手架、报告规范选择器、一致性检查、声明审计、参考文献检查、作者校验、lint）；references/ 覆盖证据工作流、IMRAD、报告规范、作者与保密、期刊政策等。工作流为 12 个编号步骤。
- **典型工作流**：①生成 fail-closed 脚手架→②按研究设计选报告规范（CONSORT 2025、SPIRIT 2025、PRISMA 2020、STROBE、ARRIVE 2.0 等）→③建证据记录：给来源/声明/数值/方法/结果分配 E/C/N/M/O/R 编号，正文中标注 `[claim:C001][evidence:E001]`→④仅基于已记录证据列提纲→⑤起草不新增事实→⑥跑一致性脚本核对数值→⑦引用与声明审计→⑧作者身份与 AI 披露校验→⑨声明逐项核验→⑩图表仅在必要时用并绑定溯源→⑪记录规范覆盖→⑫lint 通过后由人类批准 `submission_ready`。
- **特色**：安全设计突出——脚手架的占位符会被 linter 拒绝（fail-closed），未核验就不能通过；只有可问责的人类作者能批准投稿、移除草稿横幅，AI 不是作者、流畅的生成文本不是证据；敏感内容不出本地（全部脚本离线、无密钥）；明确标注政策时效（如 COPE 2017 Core Practices 已于 2024 退役）；甚至主动移除了 LaTeX 模板，防止"看似完整的占位模板"被直接投出去。
- **涉及资源**：无外部依赖（Python 3.11+ 标准库，无网络无 API key）；引用的规范包括 CONSORT/SPIRIT/PRISMA/STROBE/STARD(-AI)/TRIPOD+AI/CARE/SQUIRE/CHEERS、NLM《Citing Medicine》、ICMJE 2026 建议，均以本地参考文件形式提供。

### `scientific-critical-thinking`
- **用途**：系统性评估科学论断与证据质量——审查研究方法与实验设计、识别偏倚与混杂、评估统计有效性、应用 GRADE 和 Cochrane 风险偏倚等分级框架，也可用于批判性分析的教学。正式的同行评审意见写作则交给 peer-review 技能。
- **技能设计**：纯 references 知识库型，无脚本。core_capabilities.md 汇总七大能力域（方法论批评、偏倚检测、统计分析评估、证据质量评估、逻辑谬误识别、研究设计指导、论断评估），另有六份专题参考（科学方法、常见偏倚、统计陷阱、证据等级、逻辑谬误、实验设计）。SKILL.md 提供程序性指导，并说明"何时查哪份参考"、可用 grep 检索参考库。
- **典型工作流**：先按五条通用原则评估（建设性、具体到表格与原句、批评力度与问题重要性成比例、对所有研究用同一标准、考虑领域语境）→把反馈组织成固定结构：摘要→优点（先于缺点，利于可信与学习）→按严重度分三级的担忧（致命/重要/次要）→可操作建议→总体评估；不确定时承认不确定、提澄清性问题、给条件式判断。
- **特色**：反复强调六组概念区分——数据 vs 解释、相关 vs 因果、统计显著 vs 实际重要性、探索性 vs 验证性、已知 vs 不确定、"反对某论断的证据" vs "支持零假设的证据"；判断只依据方法学而非结论，防止对不合己意的发现更苛刻；图表仅在用户明确要求时才经 scientific-schematics 生成（并披露外发 OpenRouter）。
- **涉及资源**：分析指引完全离线；依赖的评估框架为 GRADE、Cochrane Risk of Bias、证据等级体系；可选 scientific-schematics 生成示意图（需 OPENROUTER_API_KEY 和到 OpenRouter 的出站访问）。

### `scientific-brainstorming`
- **用途**：证据感知的早期科研选题头脑风暴——生成、组织、挑战并透明地排序候选研究方向。所有产出都视为"提案"而非"发现"；明确不做假设验证（转 hypothesis-generation）、不做伦理/监管审查、不提供临床建议。
- **技能设计**：references/ 含五份文件（按证据校准的方法选择、个人/小组/异步引导流程、评价标准与决策日志、负责任 AI、带日期的一手文献 sources.md）；scripts/ 三个纯标准库本地工具——session_scaffold 建会话登记、validate_register 校验结构与溯源、evaluate_matrix 从 CSV 算出带不确定性区间和权重敏感性分析的加权评分矩阵（decision 字段刻意留空）。SKILL.md 给出 10 步可复现协议加一份偏倚控制清单。
- **典型工作流**：①界定焦点问题与安全门→②刻意引入多元视角并说明谁缺席→③独立静默生成（每条想法带稳定 ID、贡献者、来源标注 human/AI/literature）→④轮转共享、不即时评价→⑤按显式关系聚类保留原始 ID→⑥评分前先定透明标准与权重→⑦对抗性评审（由非原创者挑刺）→⑧文献核查（记录 not-checked/support-located/mixed 状态）后再补一轮独立生成→⑨过可行性/严谨性/伦理门→⑩决策人写完整决策日志。
- **特色**：大量设计直接来自群体创造力研究的证据——先独立后讨论（防生产阻断）、领导者最后发言、不自动选"赢家"、共识不是真理、票数不是效应量；检索没找到不等于"没人研究过"；针对 AI 幻觉与同质化设防（人优先创意、全程溯源、保留非 AI 视角、警惕可疑的重复框架）。
- **涉及资源**：核心全离线（Python 3.11+ 标准库，脚本不调网络、LLM 或数据库、无凭据）；文献核查环节需要权威学术数据库（由使用者或其他技能提供）；references/sources.md 记录了该版本所依据的带日期一手研究与官方指引。

### `hypothesis-generation`
- **用途**：把观察或初步发现转化为透明、可检验的研究计划——证据有界的科学问题、候选假设与对立解释、判别性预测、测量操作化、可预注册的分析计划。立场鲜明：假设是"待挑战的提案"，不是事实、诊断或建议。
- **技能设计**：结构最重的技能之一。开篇一张对象术语表严格区分观察、研究问题、假设、机制、因果估计量、预测、对立解释、零假设、阴性对照、操作化、分析计划、证据共 12 个概念；references/ 有 10 份专题文件（概念与工作流、质量标准、检索策略、因果推断与论断、实验设计模式、预注册与开放科学、伦理安全与 AI、工具参考、核验至 2026-07-23 的 source_ledger、安全验证记录）；assets/ 提供各类记录模板；scripts/ 是 7 个本地确定性、非评分 CLI（明确不自动给假设打分或取舍）。
- **典型工作流**：①跑范围与安全门→②冻结观察（先记录后解释，用"观察到/相关"而非因果语言）→③选合适的问题框架（PICO/PECO 等，强调不是万能模板）→④建立带日期的证据边界（只能说"未在已记录检索边界内找到"，不能说"无先例"）→⑤先生成多类对立解释再选检验→⑥声明论断类型与因果估计量→⑦推导能区分各假设的判别性预测（含证伪条件与阴性对照）→⑧测量操作化并校验→⑨设计与分析匹配论断类型→⑩防 HARKing（结果数据访问前时间戳全部计划）→⑪复现与更新计划→⑫人类逐项问责核验。
- **特色**：安全边界极强——不给患者个体化诊疗建议、不提供病原体/毒素等有害实验的优化或操作细节、不绕过 IRB/IACUC/IBC 等任何审查门、不编造来源或检索覆盖、不自动排序取舍假设；方法论上强调"阴性对照必须不能经由目标机制起作用"、强推断中淘汰对手不等于幸存者为真；AI 仅作辅助扩展且须记录其使用与影响。
- **涉及资源**：完全本地（Python 3.11+ 标准库，无网络、凭据、模型或外部包）；证据检索建议用一手文献、官方政策与现行报告规范；引用 SPIRIT 2025、CONSORT 2025、FINER、Platt 强推断等方法学框架；内置 source ledger 核验至 2026-07-23，时效敏感内容需用前复查。

### `experimental-design`
- **用途**：在数据采集**之前**设计实验——选择设计类型、随机分组、区组化、排布处理组合，确保结果可解释。适用于规划研究、分配受试者、多因素筛选、响应面优化、交叉/重复测量/裂区/整群随机化等场景。
- **技能设计**：主文档内置一棵"选择设计类型"的决策树（按研究问题分支路由），references/ 按主题拆成 4 个文档（随机化与区组、因子与 DOE、设计类型、序贯/自适应设计）；scripts/ 提供两个种子化（seeded）脚本：`randomization.py`（生成随机分配表）和 `doe_designs.py`（生成真实单位的 DOE 矩阵）。
- **典型工作流**：先明确研究问题、实验单元与响应变量 → 列出干扰因素（批次/日期/位点）→ 用决策树选设计 → 在正确层级确定重复数（样本量交给 statistical-power 技能）→ 用脚本生成可复现的种子化排布 → 随机化运行顺序 → 记录设计、种子与分配表（可预注册）→ 让分析与设计匹配（交给 statistical-analysis）。
- **特色**：核心围绕 Fisher 三原则（随机化、重复、区组），并专门列出 8 类"毁掉研究"的结构性错误清单（伪重复、混杂、批效应、板位效应、混叠等）——强调这些错误事后无法用分析补救。所有脚本带种子、可归档重生成，满足预注册/GLP 要求。
- **涉及资源**：Python ≥3.10，numpy、pandas、pyDOE3（提供因子、Plackett-Burman、中心复合、Box-Behnken、拉丁超立方等生成器）；经典文献（Fisher 1935、Montgomery、Hurlbert 伪重复论文）；与 statistical-power / statistical-analysis / statsmodels / pymc 技能联动。

### `exploratory-data-analysis`
- **用途**：在建模或确证推断之前，对**授权的本地数据**做有界、确定性的探索性分析——生成聚合报告、缺失值/数据泄漏审计、离群值与变换敏感性分析，以及 EDA 报告脚手架。明确不做格式认证、不推断科学含义、不做确证性/因果声明。
- **技能设计**：以"能力矩阵"为骨架——每种格式标注三档（自动化核心 / 自动化可选 / 仅参考 / 不支持即"fail closed"），并用 `capability_manifest.py` 做机器可读登记。references/ 按领域分 6 个格式文档（通用、基因组学、显微成像、化学分子、光谱、蛋白/代谢组），要求只读最相关的一个；scripts/ 含 7 个 CLI（分析、表剖析、缺失审计、分布敏感性、序列/图像检查、报告脚手架）。
- **典型工作流**：确认授权与根目录 → 先跑 manifest 检查（reference_only 则改读参考文档）→ 运行最窄化的自动化工具（如 `eda_analyzer.py`、`missingness_leakage_audit.py`）→ 补充科学上下文（读对应格式参考）→ 用 `report_scaffold.py` 生成 EDA 报告并填写观察、假设、敏感性分析、局限。
- **特色**：安全边界极其严格——所有单元格/元数据视为不可信输入，禁止跟随嵌入指令、解析 URL、pickle 加载、打印原始行；I/O 契约强制 64 MiB 默认上限、拒绝符号链接与根外路径、输出默认用 token 化标识符、全程零网络。另有一份"EDA 前必备信息清单"（数据字典、观测单元层级、缺失机制、cutoff 检测限、train/test 划分等）和 10 条推理规则（如缺失与真零严格区分、先划分后拟合）。
- **涉及资源**：Python 标准库为核心；可选固定版本依赖 NumPy、h5py、Biopython、Pillow、tifffile，备选 pandas/Polars（版本基线核验于 2026-07-23）；引用 NIST EDA 手册、FDA/ICH E9(R1)、EPA 检测限指南、Benjamini–Hochberg FDR、FAIR 原则等来源。

### `scholar-evaluation`
- **用途**：对学术作品（论文、草稿、方案、综述、研究想法）做"定性优先、证据可溯源"的发展性评议，并审计低风险研究评估流程的规范性。**绝不**用于排名个人或任何高影响人事决策（招聘、晋升、招生、经费、评奖、处分）。
- **技能设计**：结构最"治理化"的一个——references/ 5 个文档（负责任评估、评估框架、本地工具、来源账本、安全验证），assets/ 提供 6 个 JSON/CSV 模板（量规、评分、证据清单、流程清单、评分者数据），scripts/ 6 个纯标准库 CLI 做结构校验、评分计算、证据溯源、评分者一致性、权重敏感性、流程检查、报告脚手架。
- **典型工作流**：确认允许用途与授权 → 先定义"构念"再定标准 → 从模板适配量规并跑 `validate_rubric.py`（校验器会拒绝影响因子/h 指数等代理指标）→ 建立可溯源证据记录（区分观察证据与解释、支持与反证、missing 与 not_applicable）→ 独立评分（missing 不得记 0 分）→ 跑本地质量检查脚本 → 以准则级证据（而非总分）为主线综合定性结论 → 人类委员会审核后发布。
- **特色**：硬安全边界明确禁止给人排名、复合打分、推断能力/品格/受保护特征；明确声明其参考的 ScholarEval 是实验性框架而非验证心理测量学；解释规则反复强调"分数是序数汇总不是自然测量""一致性≠效度""总分永不凌驾证据"；所有脚本无网络、无凭证、无子进程，且模板默认 content validity 为 not_established、流程清单故意 fail closed。
- **涉及资源**：Python 3.11+ 仅标准库；输入限严格本地 JSON/CSV（假名 ID）；数据分类只允许 synthetic / public_scholarly_work / deidentified_low_stakes；无任何外部服务，外部依据只有 ScholarEval arXiv 论文（arXiv:2510.16234）及其来源账本。

### `peer-review`
- **用途**：辅助有责任的人类审稿人起草证据有界、建设性的同行评审稿，覆盖报告规范选择、主张–证据核对、方法与统计审查、可复现性、伦理、图表与引文审查、修回回复规划。默认所有未发表稿件保密。
- **技能设计**：核心是"intake gate（进门审查）"——读稿前必须填 `review_intake_template.json` 并跑 `validate_review_intake.py`，只有 `READY_FOR_LOCAL_REVIEW` 才能继续，校验器会阻断未记录授权、未评估利益冲突、未查期刊政策等情形。references/ 6 个文档（伦理实践/COPE/ICMJE、报告规范、统计可复现性、常见问题、工具参考、安全验证），scripts/ 7 个确定性本地 CLI（intake 校验、指南选择、主张证据矩阵、统计审计、引文审计、评审脚手架、lint）。
- **典型工作流**：过 intake gate → 确立范围与可得材料 → 做中立"定向地图"（不写接收/拒稿意见）→ 用脚本选择报告规范（如 CONSORT/STROBE 家族）→ 逐条映射主张到证据并跑矩阵校验 → 按固定顺序审方法与统计 → 审可复现性与伦理 → 审图表与引文 → 生成结构化评论（位置/观察/证据/重要性/请求行动五要素）→ 严格区分"给作者"与"给编辑保密"两个通道 → lint 后交人类定稿。
- **特色**：安全边界针对保密性设计——未授权不得读稿、不得把稿件文本发外部服务、不得冒名或宣布编辑决定、不得捏造细节；明确"报告完整性≠设计质量≠价值"，禁止把缺失项自动折算成分数；脚本输出只含 ID 和计数、不回显稿件文本；lint 检查通道分离、辱骂词表、占位符与角色/决定性措辞。
- **涉及资源**：Python 3.11+ 纯标准库，全部 CLI 确定性、本地运行、零网络/模型/图像调用；参考文件承载 COPE、ICMJE、各类报告规范（已核验 2026-07-23 的来源账本）；与 reporting standards 目录 JSON 配套。

### `pathway-enrichment`
- **用途**：对基因列表或排序基因数据做通路/基因集富集分析并解读结果——回答"我的基因富集了哪些生物学通路"。适用于差异基因、CRISPR 筛选命中、聚类标记基因、蛋白组命中后的 ORA、GSEA、ssGSEA/GSVA 及功能注释。
- **技能设计**：主文档以一张"方法选择表"为核心路由（离散命中列表→ORA；全排序列表→preranked GSEA；表达矩阵+标签→GSEA；单样本打分→ssGSEA/GSVA），references/ 3 个文档分别覆盖 gseapy API、数据库与基因集目录、统计与解读；scripts/ 一个 `run_enrichment.py` 封装端到端 ORA/GSEA 流程。
- **典型工作流**：8 步核心流程——确认输入并选方法 → 基因 ID 转换到正确命名空间（symbol，人大写/小鼠首字母大写）→ 按问题选 2–4 个基因集库（Hallmark→GO:BP→KEGG/Reactome）→ 设定背景全集（ORA 必须，用 g:Profiler custom 或 gp.enrich）→ 运行分析（GSEA 必设 seed）→ 按校正 p 值（BH/FDR q-val）过滤 → 可视化（dotplot、富集图、running-score）→ 去冗余（enrichment map、leading-edge 合并）并产出发表级表格。
- **特色**：把"最容易悄悄出错"的中段步骤（ID 映射、背景全集）提升为核心工作流步骤；列出 9 大常见坑（阈值化后再喂 GSEA、按 log2FC 排序不稳定、跨库 FDR 不可比、小基因集轻易显著等）；强调 GSEA 排序应优先用 DESeq2 的 `stat` 列而非 log2FoldChange；要求记录库版本与日期以保可复现；与 gget（轻量 Enrichr）、database-lookup 技能有明确分工。
- **涉及资源**：gseapy、gprofiler-official（Python 包）；网络服务 Enrichr、g:Profiler、MSigDB 下载；基因集数据库 GO、KEGG、Reactome、WikiPathways、MSigDB 各 collection；上游 pydeseq2/scanpy 技能，下游 scientific-visualization、networkx 等。

### `ontology-term-resolution`
- **用途**：把自由文本科学标签解析为本体术语 ID、并校验已有 CURIE 是否真实/现行。凡是要写出或信任一个本体 ID 的场合都适用：标注组织/细胞类型/疾病/表型/测定法/化合物/物种，准备 GEO/ENA/CELLxGENE 等提交元数据，或审计既有 ID 表。
- **技能设计**：一条核心铁律贯穿全文——"绝不凭记忆写本体 ID，绝不未查就接受 ID"，所有产出的 ID 都来自实时 OLS 查询。scripts/ 4 个纯标准库脚本按问题路由：`resolve_terms.py`（文本→术语，OLS）、`validate_terms.py`（ID 校验，OLS，9 种状态码可当 CI 门禁）、`map_terms.py`（ZOOMA 处理实验行话，提议但 OLS 决定）、`lookup_prefix.py`（前缀/落地页，Bioregistry + Identifiers.org）；references/ 4 个文档（OLS4 API、伴随 API、本体注册表、策展规则）。
- **典型工作流**：先 `resolve_terms.py` 限定本体查文本（exact→token→fulltext 逐级升级并报告命中类型）→ 读 `match_type`，partial 需人工裁决、unresolved 是合法输出 → OLS 漏掉行话时用 `map_terms.py`（ZOOMA）提议，再逐个 `validate_terms.py` 验证 → 审计已有表用 `--input --strict`（可加 `--branch`/`--expect-ontology` 约束列）→ 报告时同时给 ID 和 label 并说明匹配方式。
- **特色**：最大亮点是一张"会误导你的 API 行为陷阱表"——记录了实机验证过的十几个坑（OLS `exact=true` 是 token 匹配、`/search` 不返回 obsolete 状态、Identifiers.org 拒绝同义前缀如 HPO、OxO 已退役返回假 200 等），这是"用脚本而非配方"的存在理由；校验状态区分 ok/警告/失败三档，`--strict` 升级；报告规范要求 ID+label+匹配方式，禁止用最近命中填充未解析项。
- **涉及资源**：Python 3.11+ 纯标准库；网络服务 EBI OLS4、Bioregistry、Identifiers.org resolver、ZOOMA（均公开无需 API key）、Ontobee（仅 URL）；本体体系 MONDO/HP/UBERON/CL/EFO/ChEBI/NCBITaxon/PATO。

### `citation-management`
- **用途**：贯穿研究与写作全流程的引文管理——多库检索论文（OpenAlex、PubMed、Google Scholar）、从 DOI/PMID/arXiv ID/URL 提取完整元数据、校验引文准确性、生成规范 BibTeX。目标是杜绝参考文献错误、保证可复现。
- **技能设计**：按 5 个阶段（+一个强制的"2.5 元数据富化"阶段）组织主文档，每阶段给出典型命令，细节全部下沉到 references/ 的 10 个专题文档（核心工作流、检索策略、BibTeX 格式、校验、最佳实践、端到端示例等）；scripts/ 8 个脚本共享 `_common.py` 中的解析/渲染/引用键方案，保证"同一论文无论从哪个来源提取都得到同一 citation key"从而可跨源去重。
- **典型工作流**：Phase 1 至少搜两个库（OpenAlex ~2.5 亿作品无密钥、PubMed 生物医学权威、Google Scholar 用 scholarly 爬取仅作补充）→ Phase 2 用 `extract_metadata.py`/`doi_to_bibtex.py` 把标识符转元数据（URL 无 DOI 时读页面 citation_doi meta 标签再走 CrossRef）→ Phase 2.5 强制富化：任何缺 volume/pages/doi 的条目必须 WebSearch/WebFetch 补齐并记录来源，找不到则写 note → Phase 3 `format_bibtex.py --rekey --deduplicate` 清洗合并 → Phase 4 `validate_citations.py` 校验完整性/期刊要求/与稿件一致性/DOI 有效性 → Phase 5 接入写作与 Zotero/literature-review 工作流。
- **特色**：明确"元数据当不可信输入"——出版商控制的标题字符串可能含 shell 语法，要求用 subprocess 参数列表传递、引文键须匹配 `^[A-Za-z0-9]+$` 才能进路径；写作是选择性的（默认 stdout 不动原文件）；校验器对高危错误非零退出可当门禁，但把期刊引文数只当编辑经验值（警告不报错）；凭证透明——两个可选环境变量各只发给对应的一个服务。
- **涉及资源**：Python 3.9+ 与 requests（BibTeX 解析/校验全用标准库），可选 scholarly；网络 API：OpenAlex、CrossRef、PubMed E-utilities、arXiv、DataCite（全部无需密钥）；依赖工具 MeSH Browser、DOI 解析器、BibTeX/LaTeX 生态（Overleaf 文档），可导出 Zotero/pyzotero。

### `open-notebook`
- **用途**：用于部署和操作 Open Notebook——一个开源、自托管的 Google NotebookLM 替代品。面向需要在私有基础设施上整理研究材料（PDF、视频、音频、网页、Office 文档）、生成 AI 笔记与摘要、制作多说话人播客、与文档进行带引用的对话式问答的研究者，核心卖点是数据主权。
- **技能设计**：SKILL.md 本身是一份较完整的操作手册（安装、配置 AI 提供商、各功能 API 示例代码），辅以 4 个参考文件（api_reference.md、architecture.md、configuration.md、examples.md）补充完整 API 细节。无路由表、无脚本，主要靠 REST API 端点分组来组织内容（notebooks / sources / notes / chat / search / podcasts / transformations / models / credentials）。
- **典型工作流**：用 Docker Compose 启动服务（前端 8502 端口、API 5055 端口）→ 配置至少一个 AI 提供商的凭据并注册模型 → 创建 notebook → 摄入各类来源（URL 或文件上传，异步处理）→ 进行全文/向量搜索或上下文感知聊天 → 可选地生成播客或执行自定义内容转换。
- **特色**：与 NotebookLM 相比提供完整 REST API、支持 16+ AI 提供商（经 Esperanto 库）不锁定 Google、播客支持 1-4 个可定制说话人（对比 NotebookLM 的 2 个限制）、全数据本地存储。可用 Ollama 实现零 API 费用的本地推理。
- **涉及资源**：GitHub 仓库（lfnovo/open-notebook）、Docker Compose 部署、SurrealDB 数据库、FastAPI 后端、Next.js 前端、LangChain + Esperanto 多提供商 AI 库（OpenAI、Anthropic、Google、Ollama、Groq、Mistral、ElevenLabs 等 16 家）。

### `consciousness-council`
- **用途**：针对任何问题、决策或创意挑战运行"心智议会"式多视角审议。当用户面临两难、权衡、无明确答案的复杂选择，或希望从多个角度（如"不同专家会怎么看"）探索问题时触发。它模拟认知层面的"董事会 + 哲学研讨班 + 作战室"，刻意制造真正的认知多样性而非角色扮演。
- **技能设计**：纯提示工程技能（allowed-tools 仅 Read Write），无脚本无模板。核心结构是 12 个思维原型库（建筑师、反方、实证主义者、伦理学家、未来学家、实用主义者、历史学家、共情者、局外人、战略家、极简主义者、创造者），每个原型定义了思维方式、标志性问题和盲点。配有"问题类型 → 原型组合"的选择启发式表（如商业决策 → 战略家+实用主义者+伦理学家+未来学家+反方）。
- **典型工作流**：三阶段流程——阶段一根据问题召选 4-6 名成员，要求组合能产生真正冲突；阶段二每人按固定格式发言（立场一句话 + 2-4 句推理 + 他人遗漏的关键风险 + 意外洞见）；阶段三输出综合裁决（3 人以上共识点、核心张力、集体盲区、尊重张力的可行建议、置信度、留给用户继续思考的一个问题）。
- **特色**：强制分歧机制——若全员意见一致视为议会失败，须回头强化张力；反方须挑战最流行立场而非泛泛怀疑。支持 8 种定制配置：快速议会（3 人）、深度议会（6 人）、增删特定原型、自定义名单、匿名议会（综合前不揭示身份以减少锚定偏差）、魔鬼代言人模式（全员反驳直觉）、多轮模式（成员互相回应）。还内置适用性判断：对纯事实题、已决而求认可、低风险琐事会主动降级为双视角对比。
- **涉及资源**：无任何外部依赖——不需要网络、数据库、API 或库，完全靠模型自身推理。仅署名引用 AHK Strategies（ahkstrategies.net）和 TheMindBook（themindbook.app）。

### `what-if-oracle`
- **用途**：对不确定的未来进行结构化 What-If 情景分析，通过 4-6 个分支（最优 Ω / 最可能 α / 最差 Δ / 野牌 Ψ / 逆共识 Φ / 二阶 ∞）绘制完整的"可能性空间"。适用场景：投机性 what-if 提问、岔路口决策、应急预案与风险映射、承诺前的压力测试。理论基础是其作者发表的"What-If 范式"研究（将假设视为一种基本的计算操作）。
- **技能设计**：SKILL.md 定义分析框架本体，一个 references/scenario-templates.md 提供按领域的模板（创业、技术架构、投资金融、职业个人等），每个模板给出该领域应测试的变量、分支侧重和提示词格式。核心哲学是 "0·IF·1"：0 为未表达态、1 为现实态、IF 为连接两者的条件键——IF 越精确，分析越有价值，故流程要求先锐化模糊问题（"AI 会接管吗"→"3 年内某行业 40% 知识工作被自动化"）并经用户确认。
- **典型工作流**：四阶段——(1) 锐化问题，分解出变量、幅度、时间框架、当前状态；(2) 生成 4-6 个情景分支；(3) 每个分支按固定卡片格式分析：概率、时间框架、置信度、叙事、关键假设、触发条件（早期信号）、即时/30 天/6 个月后果、所需应对、多数人遗漏点；(4) 综合输出概率分布直方图、跨分支稳健行动（无悔之举）、对冲行动、决策触发线、"1% 洞见"。
- **特色**：黄金比例加权（主情景 61.8% / 备选 38.2% 的注意力分配）防止孤注一掷或过度分散；五种运行模式——快速（3 分支）、深度（全 6 分支）、情景链（递归深挖某分支内部）、逆向（从期望结果倒推必要条件）、竞争式（我方/对方/市场多视角）。明确声明边界：这是可能性地图而非预测，概率是基于证据的估计而非确定性，也不替代后续行动。许可证为 CC BY-NC-SA 4.0（商用需授权）。
- **涉及资源**：无网络/API/库依赖，纯推理技能；仅引用作者两篇 Zenodo 论文（DOI 10.5281/zenodo.18736841、18807387）和上游 GitHub 仓库。

### `iso-standards-readiness`
- **用途**：为四类 ISO/IEC 标准（ISO 13485 医疗器械 QMS、ISO 14971 器械风险管理、ISO/IEC 17025 检测校准实验室、ISO 15189 医学实验室）整备"就绪证据"供授权人类审查：声明范围、受控文件、实施记录、追溯链、CAPA、外部供方控制等。明确不用于认证/认可/合规决定，不含任何条款文本。
- **技能设计**：典型的"路由器 + 参考库"架构——SKILL.md 持有边界、车道纪律、共享工作流和 CLI 契约；每个标准一个 references/ 深度文件（iso-13485.md 等 4 个），另有 5 个共享参考（assurance-lanes 车道表、source-ledger 溯源账本、evidence-architecture、gap-analysis-checklist、quality-manual-guide）。scripts/ 提供 6 个确定性校验 CLI，assets/templates/ 提供 10 个故意 fail-closed 的 JSON/MD 模板（全部为 draft/pending 占位态，须复制到库外填写）。
- **典型工作流**：八步流程——(1) 声明标准、车道、授权责任人并跑 scope intake 校验；(2) 冻结来源/版本证据（不用搜索片段当受控要求）；(3) 建受控文件登记册（不数文件名、不扫关键词）；(4) 评估程序实施并抽样记录；(5) 按车道运行专项检查（追溯、CAPA、供方）；(6) 美国器械道单独跑 QMSR 过渡检查；(7) 组装有界清单并用 validate_evidence_manifest + gap_analyzer 生成差距报告；(8) 人类审查与受控移交，标题必须是"供授权人类评估的草稿证据审查"。
- **特色**：安全边界极其严格——"保证车道"纪律（组织被认证、实验室被认可，混用即范畴错误；证书不能替代监管者），非协商性禁令清单（不得认证、不得判法律适用性、不得从文件名/数量/百分比推断实施情况），CLI 全部仅标准库、零网络、拒绝符号链接/重复 JSON 键/不安全路径、退出码 0 明确标注"非合规结果"。基线账本跟踪 2026 年最新状态（FDA QMSR 已生效、ISO 15189:2022 过渡已关闭、GAI 取代 ILAC/IAF）。
- **涉及资源**：ISO/IEC 标准文本（版权，须从 ISO 官方购买，禁止复制条款）、FDA/CAP/MDSAP 官方方案文件、Python 3.11+ 标准库；刻意做到零网络访问、零凭据。

### `analytical-method-validation`
- **用途**：规划、执行和记录分析程序（HPLC、LC-MS/MS、GC、CE、ICP-MS、溶出、qNMR、qPCR、NIR、结合/细胞活性测定）的验证、确认与转移，回答"程序是否适合其预期用途"。治理框架覆盖 ICH Q2(R2)/Q14、USP <1220>/<1225>/<1226>、ICH M10 生物分析、CLSI EP、ISO/IEC 17025。
- **技能设计**：SKILL.md 承载两条铁律（先定治理框架、先于数据定验收标准）和完整工作流；references/ 有 6 个文件（框架选择、ICH Q2(R2)、ICH M10、USP/CLSI 指南 designation、统计方法、溯源账本）；scripts/ 有 6 个纯标准库统计脚本（规划、响应曲线、准确度精密度、检测限、生物分析批、方法比较），统一支持 `--format table|tsv|json`、数据走 stdout 而出处走 stderr、退出码 0/1/2 可做流水线门控；assets/ 有协议和报告模板。
- **典型工作流**：(1) 用 plan_validation.py 按属性（assay/impurity/limit/identity，Q2(R2) Table 1 由属性而非技术决定要求）确定必需特性与研究布局；(2) 生成协议骨架，刻意不预填验收标准（Q2(R2) 无合理默认值，须来自质量标准或 ATP）；(3) check_response.py 评估校准模型（失拟 F 检验 + 残差模式，支持 1/x² 加权与异方差检测）；(4) check_accuracy_precision.py 用随机效应模型分层报告重复性与中间精密度；(5) check_detection_limits.py 按多种允许方法算 DL/QL 并要求确认；(6) check_bioanalytical_run.py 按 modality（色谱 vs 配体结合，标准强制必填）核查 M10 批；(7) compare_methods.py 用 TOST 等效检验 + Deming/Passing-Bablok 回归做转移比较。
- **特色**：整个技能围绕"防止七个常见错误"设计——用 Q2(R1) 过时结构、事后定验收标准、拿 r² 当线性证据、把重复性当程序精密度（示例中中间精密度是重复性的 23 倍）、DL/QL 不注明方法不确认、色谱标准误用于配体结合（或反之）、把 t 检验不显著当等效。统计分布从第一性原理计算保证可复现。版权边界：ICH 公开可内嵌，USP/CLSI/ISO 付费只给 designation 不给文本。
- **涉及资源**：ICH Q2(R2)/Q14/M10 指南（公开）、USP 通则与 CLSI EP 文件（付费墙，仅指路）、Python 3.11+ 标准库（无 numpy/scipy、无网络）。

### `pyzotero`
- **用途**：通过 pyzotero Python 客户端与 Zotero 文献库的 Web API v3 交互：检索/创建/更新/删除条目、集合、标签、附件，导出引用，构建文献管理自动化工作流。面向需要程序化操作 Zotero 的研究者。
- **技能设计**：SKILL.md 是精炼的快速上手手册（认证、安装、核心概念、常见模式），深度内容全部下放到 14 个按主题切分的参考文件——authentication、read-api、write-api、search-params、collections、tags、files-attachments、exports（BibTeX/CSL-JSON）、pagination、full-text、saved-searches、error-handling，以及两个特殊通道 cli.md 和 mcp.md。本身无脚本，靠示例代码模式传授用法。
- **典型工作流**：从 zotero.org 获取 User ID 和 API Key 存入环境变量（ZOTERO_LIBRARY_ID / ZOTERO_API_KEY / ZOTERO_LIBRARY_TYPE）→ `uv add pyzotero` 安装 → 实例化 Zotero 对象绑定单个库 → 读（top/items/搜索，注意默认 100 条、用 everything() 取全量）→ 改（item_template 建条目、update_item 更新）→ 导出（add_parameters(format='bibtex')）→ 上传附件。本地模式可免 Key 只读访问；本地 Zotero 7 可走 CLI 或 MCP server 做含 PDF 全文的搜索。
- **特色**：明确区分三种接入通道并给出选择逻辑——Web API（全功能，需 Key）、本地只读模式（local=True 免 Key）、本地 Zotero 7 CLI/MCP（免 Key 且支持全 PDF 搜索，面向 LLM 客户端）。API 陷阱教育到位：默认分页 100、写操作返回 True 或抛 ZoteroError、条目数据在 item['data'] 下。
- **涉及资源**：Zotero Web API v3、pyzotero 1.13+（PyPI）、可选 CLI 与 MCP server 扩展（需 Zotero 7 开启本地 API）、bibtexparser（BibTeX 导出）。

### `paper-lookup`
- **用途**：跨 18 个学术 API 检索论文、预印本、引文、开放获取全文、仓储记录和期刊 OA 状态，并返回带可复现溯源的结果。覆盖查文献、DOI/PMID/arXiv 反查、摘要、OA PDF、引文图谱、作者发文、生物医学实体标注、Zenodo/Figshare 沉积记录、机构 ROR ID 等几乎所有学术检索需求。
- **技能设计**：大型路由器架构——SKILL.md 含核心工作流、按用例和跨库查询两张路由表、标识符格式对照表、API Key 说明、调用纪律；每个数据库一个自包含 references/ 文件（18 个，含端点、参数、响应形状和"静默失败方式"专节）。scripts/ 有 4 个仅标准库的解析脚本（paginate 分页对账、jats_to_text 全文提取、arxiv_atom、openalex_abstract），用退出码区分错误类型。设计为可生长：新增数据库只需加参考文件 + 路由行。
- **典型工作流**：七步——(1) 定义检索契约（要什么、有何约束，缺关键约束先问）；(2) 按路由表选库，不无脑发散 18 个；(3) 调用前读对应参考文件；(4) 优先用捆绑脚本而非手搓解析；(5) 有界调用：先看总数、确定性分页、对账，超 ~1000 条或 ~50 次调用先征求同意；(6) 把响应当作不可信第三方数据（防提示注入、不回显 Key）；(7) 按固定格式返回答案 + 溯源（端点、参数、标识符转换、访问日期、数量对账、警告）。
- **特色**：核心洞察是"这些 API 会用 HTTP 200 失败"——PMC eFetch 无 body 的 200、arXiv 名为 Error 的条目、Europe PMC 藏在 200 里的 errCode 等，每个数据库的参考文件都有专门的 hazard 章节。调用纪律极细：URL 编码（含方括号，否则 curl globbing 静默吞请求）、对限速主机串行化、429/503 重试一次。安全设计：paginate.py 自动脱敏 provenance 中的 api_key/email/mailto。明确禁止把元数据冒充全文（jats_to_text 退出码 2 时如实报告不可得）。
- **涉及资源**：PubMed、PMC、Europe PMC、bioRxiv、medRxiv、arXiv、OpenAlex、Crossref、Semantic Scholar、CORE、Unpaywall、OpenCitations、PubTator3、Zenodo、Figshare、ROR、BioStudies、DOAJ 共 18 个 API；可选 Key：NCBI_API_KEY、S2_API_KEY、CORE_API_KEY（全文必需）、OPENALEX_API_KEY；需要 curl 和 Python 3.11+ 标准库。

---## Misc / matrix & ETL

### `scikit-bio`
- **用途**：指导使用 scikit-bio Python 库完成生物信息学分析——序列读写与操作、比对、系统发育树、微生物群落数量生态学（多样性、排序、置换检验），面向宏基因组/微生物组等组学数据处理场景。
- **技能设计**：SKILL.md 主体按 10 项核心能力分节（序列、比对、树、多样性、排序、统计检验、I/O、距离矩阵、表格、蛋白质嵌入），每节都给出"Key operations + 常用代码模式 + Important notes"三段式；仅有一个 references/api_reference.md 作为完整 API 手册，结构相对扁平。
- **典型工作流**：读 BIOM/FASTA → 计算 alpha/beta 多样性 → PCoA 排序 → PERMANOVA 检验；或读序列 → 比对 → 建距离矩阵 → 构树（NJ/UPGMA/GME/BME）→ 分析分支。
- **特色**：大量强调 0.7.0 版本的重大 API 变化（pair_align 取代 SSW、`taxa=` 取代 `otu_ids`、dispatch 系统让函数直接接受 pandas/polars/AnnData/BIOM），避免模型用过时代码；内置性能建议（大文件用生成器、大树用 GME/BME、beta 多样性可分块并行）。
- **涉及资源**：PyPI/conda 的 scikit-bio（需 Python 3.10+、NumPy 2.0+），生态依赖 matplotlib/seaborn/plotly、biom-format、polars/anndata、Biopython、QIIME 2；链接官方文档、GitHub、Nature Methods 论文及 QIIME 2 论坛。

### `matlab`
- **用途**：设计、评审、迁移 MATLAB/GNU Octave 数值计算代码，覆盖数组、表格/时间表、测试、项目、图形导出、MAT 文件及 Python 互操作，并规划"可信执行"流程。
- **技能设计**：SKILL.md 是政策性总纲（license 门禁、安全边界、7 步默认工作流、语言检查清单），8 个 references/ 文件按主题深挖（编程、矩阵、数学、图形、I/O、执行、Python 集成、Octave 兼容），另有 scripts/ 7 个本地无网络 CLI 辅助脚本和 assets/ 中的清单模板 JSON。
- **典型工作流**：先澄清目标（版本、平台、许可）→ 静态扫描 `.m`/MAT 头 → 选择代码形式（优先 arguments 块的函数）→ 显式声明数值语义 → 无隐藏状态测试 → 生成 argv 执行计划待用户批准 → 哈希记录溯源。
- **特色**：最强的安全边界设计——绝不运行不可信的 .m/.mlx/MEX/MAT，列出 eval/system/py.*/load 等危险面；严格区分 MATLAB（专有、许可未知）与 Octave（GPLv3+，仅部分兼容）；所有 bundled 脚本只静态分析或 dry-run，绝不启动 MATLAB/Octave/子进程。
- **涉及资源**：MATLAB R2026a 文档/发行说明/系统要求、MathWorks PyPI 包 `matlabengine`、GNU Octave 11.3.0、可选 scipy/h5py 做 MAT 清点；纯本地运行，无强制网络。

### `esm`
- **用途**：使用 EvolutionaryScale 的 `esm` SDK 做蛋白质语言模型任务——ESM3 生成式蛋白质设计（序列/结构/功能多模态）、ESMC 提取蛋白质嵌入、ESMFold2 结构预测，面向蛋白质工程与药物发现。
- **技能设计**：SKILL.md 按六大能力组织（生成、结构预测/逆折叠、嵌入、功能条件化、chain-of-thought 多步生成、Forge 异步批处理），附模型选型指南与安装/认证节；5 个 references/ 分别对应 esm3-api、esm-c-api、forge-api、biohub-platform、workflows 端到端示例，按需加载。
- **典型工作流**：本地加载 `esm3-open`/`esmc_300m` 或经 `ESM_API_KEY` 连 Forge → 构造 `ESMProtein` 提示（`_` 掩码位置）→ `generate` 按 track（sequence/structure/function）迭代生成 → 生成结果转 PDB/嵌入，批处理用 asyncio 并发；复杂设计走"先结构、再序列、后功能"的 chain-of-thought 精炼。
- **特色**：明确区分本地开源权重（1.4B/300M/600M）与 Forge/Biohub 托管的 7B/98B/6B 模型及各自 ID 写法差异；认证只允许读 `ESM_API_KEY`，禁止硬编码令牌或从不可信输入取 API 主机；附"负责任使用"节，要求遵守 Responsible Biodesign 框架并考虑生物安全。
- **涉及资源**：PyPI `esm==3.2.3`（Python 3.12）、可选 flash-attn；Forge（forge.evolutionaryscale.ai）与 Biohub（biohub.ai）云端推理、Hugging Face 模型权重、AWS SageMaker 部署选项、Science 论文与 Slack 社区。

### `geniml`
- **用途**：在基因组区间集合（BED 文件）上做机器学习与统计工作流——验证 BED 与 universe 契约、规划 Region2Vec/scEmbed 训练、检查模型/tokenizer 兼容性、构建共识 peaks，面向 ATAC/ChIP 等区间组学数据。
- **技能设计**：SKILL.md 以"契约"为骨架（坐标/组装契约、模型兼容契约、安全门禁），内嵌 0.8.4 版本 API 迁移说明；scripts/ 含 6 个纯标准库、本地运行的审计/规划 CLI（bed_validator、corpus_auditor、tokenizer_compatibility、model_artifact_inspector、consensus_plan、embedding_plan），references/ 5 个文件按方法（Region2Vec、scEmbed、BEDspace、共识 peaks、工具）拆分。
- **典型工作流**：先过安全门禁（本地文件、BED 结构与组装校验、资源上限、按患者划分数据集）→ 用 bed_validator 产出只报告不改写的归一化计划 → Gtars Tokenizer 从 universe.bed 分词 → 训练 Region2Vec/scEmbed 或走 `geniml build-universe`/`assess-universe` → 用 tokenizer_compatibility 验证模型 config、universe 字节、checkpoint 形状一致后才加载。
- **特色**：把上游文档过时、CLI 名漂移、`__init__` 不再导出等版本陷阱逐条写明并给出具体模块路径；脚本只"规划与审计"不执行训练、不反序列化模型、零网络请求；强调 0-based half-open 坐标、按生物学单元防泄漏、BEDbase/Hugging Face 下载须显式批准。
- **涉及资源**：PyPI `geniml==0.8.4` + `gtars==0.9.2`（ml extra 含 Torch/Gensim/Scanpy/HF Hub）、BEDbase API（api.bedbase.org）、Hugging Face Hub 模型仓库、本地染色质大小文件与 bigWig 覆盖度文件；上游库为 BSD-2-Clause。

---## Single-cell & genomics（补遗）

### `bgpt-paper-search`
- **用途**：通过 BGPT 远程 MCP 服务器检索科研文献。它解决的痛点是传统文献库只返回标题和摘要，而 BGPT 从论文全文中抽取结构化实验数据——每篇论文返回 25+ 个字段（方法、定量结果、样本量、质量评分和结论）。适用于系统性/范围性文献综述、跨研究的效应量对比、构建荟萃分析证据表等需要全文细节的场景。
- **技能设计**：典型的"轻量指引型"技能，SKILL.md 本身就是全部内容，没有 references/ 或 scripts/ 目录，也没有路由表或分阶段流程。它扮演"使用说明 + 环境前置条件说明"的角色：核心是告知 agent 该调用哪个 MCP 工具（`search_papers`）、以及如何正确配置 MCP 服务器。技能本身不实现任何搜索逻辑，完全依赖外部服务器。
- **典型工作流**：第一步先在 agent 宿主的 MCP 配置中添加 BGPT 服务器（如 `npx mcp-remote https://bgpt.pro/mcp/sse` 或 `npx bgpt-mcp`），配置完成后 agent 通过 MCP 接口（而非 Bash）调用 `search_papers` 工具，传入自然语言检索主题（如"CRISPR 基因编辑效率"），最后拿到含标题、作者、DOI、方法、结果、样本量、质量评分、结论的结构化结果。
- **特色**：亮点在于"结构化全文数据"而非摘要级检索，并附带质量评分/证据分级字段，方便证据合成。安全与成本边界清晰：免费层每网络 50 次搜索无需 API key，付费 $0.01/条结果需申请 key；且明确声明该技能不会自行启用 MCP 访问，必须由宿主预先配置。
- **涉及资源**：依赖 bgpt.pro 远程服务及其 MCP 端点（SSE）、bgpt-mcp 开源包（GitHub: connerlambden/bgpt-mcp）、mcp-remote 中转工具、Node.js/npx 运行环境，以及可选的 BGPT API key（付费）。

---

## Misc / matrix & ETL（补遗）

### `zarr-python`
- **用途**：Zarr-Python 3（当前上游版本 3.2.1）的使用技能，面向大规模科学计算中 N 维数组的分块存储需求——支持压缩、并行 I/O、云原生存储，并与 NumPy/Dask/Xarray 无缝集成。适合处理超大数组（如气候数据）且需要高效本地或云端读写的科研流水线。
- **技能设计**：采用"主文档 + 分层引用"的渐进披露架构：SKILL.md 内置 Quick Start 和核心 API 示例（创建/打开/读写数组、Groups 层级、属性元数据），深层细节下沉到 references/ 下的 6 个专题文档（分块与压缩、存储后端、生态集成、性能模式、API 参考、v2→v3 迁移指南），并在主文档中列出带摘要的链接作为"路由表"，让 agent 按需加载，控制上下文占用。
- **典型工作流**：先用 `uv pip install "zarr==3.2.1"` 精确安装（云端另装 `zarr[remote]` 并 pin s3fs/gcsfs 版本）；然后用 `zarr.create_array`/`zarr.zeros` 创建分块数组，用 NumPy 风格切片读写，用 `zarr.group` 组织层级结构，用 `z.blocks`/`vindex`/`oindex` 做块级与高级索引；性能与云存储细节按需查阅对应 reference 文档。
- **特色**：强调版本纪律与安全边界——要求精确 pin 版本（3.2.1）而非范围依赖，除非有 lockfile 和兼容性测试；明确 v3 移除了 h5py 风格 API（`create_dataset` → `create_array`）；凭证安全上建议 IAM 角色/工作负载身份、禁止打印凭证值。此外有独特的学术引用规范：若技能对成果有实质贡献，须引用 K-Dense 的 Scientific Agent Skills 论文（arXiv:2609.00065），且要求在线核实最新作者与版本信息。
- **涉及资源**：Python 3.12+/NumPy 2.0+、zarr 3.2.1 库及 zarr-specs 规范；云端依赖 fsspec、s3fs（S3）、gcsfs（GCS）；生态库 Dask、Xarray、NumCodecs；官方文档与社区渠道包括 zarr.readthedocs.io、zarr-specs.readthedocs.io、GitHub zarr-developers/zarr-python 及 Zulip 开发者频道。

> 说明：`anndata` 在 Single-cell & genomics 桶已详述；`polars-bio` 在 ML / deep learning / optimization 桶已详述；它们不再在 Misc 桶下重复。

---