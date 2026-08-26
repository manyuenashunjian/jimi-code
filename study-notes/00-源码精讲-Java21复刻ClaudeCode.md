# JimiCode 源码精讲：用 Java 21 复刻一个 Claude Code

> 讲解对象：`d:/code/others/jimi-code`，103 个 Java 源文件、25 个包、纯 Java 21 + Maven 构建。
> 本讲按「主干 → 精华 → 支撑」顺序展开，目标是让你能看懂整个项目的运行脉络，并学到可以直接写进简历/面试话术的设计点。

---

## 目录

1. [项目定位：这是什么](#一项目定位这是什么)
2. [架构全景：模块地图](#二架构全景模块地图)
3. [主干：Agent Loop（核心主线）](#三主干agent-loop核心主线)
4. [精华①：上下文工程（两层压缩 + 工具结果预算）](#四精华上下文工程两层压缩--工具结果预算)
5. [精华②：LLM 协议抽象层](#五精华llm-协议抽象层)
6. [精华③：权限体系](#六精华权限体系)
7. [进阶：子代理与多智能体团队](#七进阶子代理与多智能体团队)
8. [工具体系与延迟加载](#八工具体系与延迟加载)
9. [Java 21 特性运用一览](#九java-21-特性运用一览)
10. [学习路径建议](#十学习路径建议)

---

## 一、项目定位：这是什么

一句话：**一个跑在终端里的 AI 编程助手，相当于用 Java 21 重写的 Claude Code**。

它具备这些能力：

- **对话式编程**：你在终端里跟它说话，它自己会读文件、改文件、跑命令，像 Claude Code / Cursor 那样"干活"而不是"聊天"。
- **多模型支持**：同一套代码接 Anthropic（Claude）、OpenAI（GPT）、以及任何 OpenAI 兼容接口（vLLM、DeepSeek、Ollama 等都算）。
- **工程化完整**：有权限审批、上下文压缩、会话恢复、多智能体团队、git worktree 隔离、MCP 工具、TUI 界面，不是玩具 demo。
- **技术栈前沿**：`record`、`sealed interface`、switch 模式匹配、虚拟线程、text block 全用上了，是 Java 21 特性的"活教材"。

入口在 `JimiCode.java`（`src/main/java/com/jimicode/JimiCode.java`）：加载 YAML 配置 → 解析权限模式 → 构造 TUI 模型 → 用 `tui4j`（一个 Java 的 BubbleTea 风格终端 UI 库）启动界面。

```java
var model = new JimiCodeModel(config.getProviders(), mcpServers, hooks, permissionMode);
var program = new Program(model);   // tui4j 终端程序
model.setProgram(program);
program.run();
```

## 二、架构全景：模块地图

按包划分，全项目 25 个包如下（括号内为文件数）：

| 包 | 职责 |
|---|---|
| `agent` (3) | **核心**：Agent 主循环 + 事件模型 |
| `conversation` (5) | 对话消息管理（内存中的"会话"） |
| `llm` (7) | 三家协议的流式客户端 |
| `tool` (5) + `tool/impl` (11) | 工具抽象 + 11 个内置工具实现 |
| `toolresult` (6) | **精华**：工具结果预算 / 内容替换（省 token 的工程） |
| `compact` (2) | **精华**：两层上下文压缩 |
| `permission` (3) | **精华**：权限五层检查 |
| `subagent` (6) | 子代理机制（fork/同步/异步） |
| `teams` (11) | 多智能体团队（文件信箱通信） |
| `worktree` (8) | git worktree 隔离 |
| `mcp` (1) | MCP 工具桥接 |
| `skill` (4) | 技能（SOP）加载与注入 |
| `memory` (4) | 长期记忆 |
| `hook` (1) | 生命周期钩子 |
| `plan` (1) | Plan 模式 |
| `session` (1) | 会话持久化（磁盘 transcript） |
| `filehistory` (1) + `history` (1) | 快照 / 历史 |
| `config` (5) | 配置加载 |
| `prompt` (3) | 系统提示词组装 |
| `tui` (6) + `tui/dialog` (2) | 终端 UI |
| `task` (2) | 后台任务 |
| `command` (3) | 斜杠命令 |

整体数据流一句话：**用户在 TUI 输入 → ConversationManager 存消息 → LLM 流式返回（含工具调用请求）→ Agent 执行工具 → 结果塞回对话 → 再问 LLM → 循环，直到 LLM 不再要求调用工具为止。**

## 三、主干：Agent Loop（核心主线）

### 3.1 三层结构

`agent` 包只有 3 个文件，是全部复杂度的集散地：

- `Agent.java`（434 行）——主循环，下面重点讲。
- `StreamingExecutor.java`——工具执行器，负责执行本轮的多个工具调用（可并行）。
- `AgentEvent.java`——事件模型，Agent 通过一个 `BlockingQueue<AgentEvent>` 把"过程"推给 TUI 展示。

关键设计：**Agent 跑在虚拟线程里，通过有界队列（容量 64）对外发事件**。

```java
public BlockingQueue<AgentEvent> run(ConversationManager conv) {
    var queue = new LinkedBlockingQueue<AgentEvent>(64);
    Thread.startVirtualThread(() -> {          // Java 21 虚拟线程，一个对话一个线程
        try { agentLoop(conv, queue); }
        catch (Exception e) { putSafe(queue, new AgentEvent.ErrorEvent(...)); }
    });
    return queue;
}
```

这保证了 TUI 不会被 Agent 阻塞；TUI 只管 `poll` 事件、刷新界面。

### 3.2 循环体（`agentLoop`）逐步拆解

一次迭代（iteration）做的事，按代码顺序（`Agent.java:130-410`）：

1. **注入长期记忆**（`:131`）：把 instructions + memory 混入系统提示。
2. **注入后台通知**（`:151-155`）：异步子代理完成的通知，以 system reminder 形式追加。
3. **准备工具 schema**（`:160-168`）：每轮从 `ToolRegistry` 取全量 schema；若有 `toolNameFilter`（子代理场景）则过滤。**关键点：schemas 每轮只算一次，供压缩与请求共用，保证字节一致**。
4. **自动压缩检查**（`:171-186`）：`ContextCompactor.manage(...)` 判断是否需要压缩；若压缩过（消息数变少），丢弃旧的 token 锚点。
5. **注入延迟工具提示**（`:189-199`）：那些 `shouldDefer()` 的工具（如 MCP 工具），schema 不加载，只通知模型"你要用的话先调 ToolSearch"。
6. **Plan 模式提示**（`:202-209`）：若处于 plan 模式，注入结构化的计划工作流 reminder。
7. **应用工具结果预算**（`:222-230`）：`ToolResultBudget.apply(...)`——这就是省 token 的核心，见第四节。
8. **流式调用 LLM**（`:231`）：`client.stream(conv, tools)` 返回 `BlockingQueue<StreamEvent>`，然后 `poll(30s)` 消费。
9. **消费流事件**（`:242-290`）：`switch` 模式匹配处理 8 种事件（文本增量、思考、工具调用开始/增量/完成、流结束、错误）。注意 `ToolCallDelta` 是空实现（`:269`）——参数增量不逐字转发，只在完成时整体解析。
10. **错误恢复**（`:293-322`）：上下文过长 → 最多重试 3 次强制压缩；限流 → 睡 5 秒重试。
11. **max_tokens 恢复**（`:329-350`）：若 stopReason 是 `max_tokens`，先升级输出上限到 64_000（`MAX_TOKENS_CEILING`），再不行就注入"接着上次继续"的消息重试，最多 3 次（`MAX_OUTPUT_RECOVERIES`）。
12. **保存 assistant 消息**（`:353-356`）：把文本 + thinking + 工具调用块写回对话。
13. **重新锚定用量**（`:365-369`）：拿本轮真实 token 用量更新 `usageAnchor`，让压缩判断更准。
14. **终止判断**（`:372-385`）：**没有工具调用 → 任务完成**，发 `LoopComplete` 退出。这是 Agent 循环的退出条件，也是最容易理解的"AI 会自己停下来"的原理。
15. **执行工具**（`:388-398`）：`StreamingExecutor.executeAll(...)` 并行执行所有工具调用，把结果 `ToolResultBlock` 加进对话。
16. **Plan 模式退出**（`:400-407`）：调用了 `ExitPlanMode` 就结束循环。

### 3.3 你会学到的核心心智模型

> **Agent = while(模型还想调工具) { 调工具; 把结果放回对话; }**

"AI 自主干活"在代码层面就这一句话。模型是否结束，完全由它自己的 stop_reason 和是否产出工具调用决定。剩下的工程复杂度，全在"把对话喂得又便宜又不溢出的上下文工程"上。

---

## 四、精华：上下文工程（两层压缩 + 工具结果预算）

这是整个项目**技术含量最高的部分**，也是面试时最能讲出深度的点。它要解决的问题是：Agent 对话会无限变长，但模型上下文窗口有限、按 token 计费。两条防线：

- **第一层：工具结果预算**（`toolresult` 包）——在**本地**把占 token 的大块工具结果替换成小标签，零成本。
- **第二层：上下文压缩**（`compact` 包）——超过阈值时，**用 LLM 自己总结旧对话**，把历史折叠成一段 summary。

### 4.1 工具结果预算：ToolResultBudget（`ToolResultBudget.java`）

问题背景：一个 `ReadFile` 可能返回 5 万字符，`grep -r` 可能更大。这些东西塞回对话，一轮就撑爆窗口。做法是**不真正删，而是"落盘替换 + 预览串"**：

关键预算常量（`:40-52`）：

| 常量 | 值 | 含义 |
|---|---|---|
| `SINGLE_RESULT_LIMIT` | 50_000 字符 | 单条结果超过就落盘替换 |
| `MESSAGE_AGGREGATE_LIMIT` | 200_000 字符 | 单条消息聚合超过，把最大的几条落盘 |
| `OLD_RESULT_SNIP_CHARS` | 2_000 字符 | 陈旧结果超过就裁剪成短标签 |
| `KEEP_RECENT_TURNS` | 10 轮 | 最近 10 轮的结果不裁剪 |

算法核心 `apply()`（`:59-180`），**输入会话永不被修改**，返回一个新的 `ConversationManager`：

- **Pass 0 复用决策**（`:84-93`）：`ContentReplacementState` 里记录每个工具调用 id 的决策，之前决定过"替换成某预览串"就原样复用。这是为了 **prompt cache 字节稳定**——后面细说。
- **Pass 1 单条落盘**（`:105-122`）：超 50k 的结果写入 `.JimiCode/tool_results/<toolUseId>` 文件，对话里替换成 `[Result of N chars saved to ... — read with ReadFile if needed]`。
- **Pass 2 聚合落盘**（`:124-154`）：整条消息累计超 200k，按内容长度降序落盘，直到压回限制内。
- **Pass 3 陈旧裁剪**（`:205-243`）：超过最近 10 轮（`KEEP_RECENT_TURNS`）的旧工具结果，只要超 2k 就替换成 `[Stale output snipped: N chars]`。

**为什么强调"byte-stable prefix / prompt cache"？**

这是全项目最值得品的设计（`ContentReplacementState.java:20-22`）。LLM 提供商的 prompt cache 以"前缀逐字节相同"为命中条件。如果每次循环把工具结果替换成**不同**的预览串，前缀一变，缓存全失效，等于每次都全量重新计费。所以：

- `seenIds`：一旦某个工具调用 id 被决策过，**永久冻结**，不再改判。
- `replacements`：保存字节精确的预览串，后续轮次**原样复用**。
- fork 子代理时用 `copy()` 继承父的决策（`AgentTool` 里传入），让子代理共享同一前缀。

`ReplacementRecordsIO` 把每次决策以 JSONL 追加到 `.JimiCode/session/replacement_records.jsonl`，配合 `ContentReplacementLifecycle.reconstruct()` 在 resume/恢复会话时重建决策状态。

### 4.2 上下文压缩：ContextCompactor（`ContextCompactor.java`）

触发公式对齐了 Claude Code 的 `autoCompact.ts`：**已用 token ≥ 有效窗口 − 安全边际**。

关键常量（`:47-70`）：

| 常量 | 值 | 含义 |
|---|---|---|
| `SUMMARY_OUTPUT_RESERVE` | 20_000 | 给总结输出预留的空间 |
| `AUTO_COMPACT_SAFETY_MARGIN` | 13_000 | 软触发线 |
| `MANUAL_COMPACT_SAFETY_MARGIN` | 3_000 | 硬触发线，越过强制压缩 |
| `KEEP_RECENT_TOKENS` | 10_000 | 保留最近原文的 token 预算 |
| `MIN_KEEP_MESSAGES` | 5 | 最少保留的消息数 |
| `KEEP_MAX_TOKENS` | 40_000 | 保留原文上限 |
| `MAX_CONSECUTIVE_FAILURES` | 3 | 熔断器：连续失败 3 次不再自动压缩 |

两个入口：

- `manage(...)`：每轮迭代前调用的自动压缩。判断依据是 `usageAnchor`（上一轮真实的 token 用量）+ 字符估算。
- `forceCompact(...)`：上下文超长报错时的兜底强制压缩。

**Layer 2 压缩策略**（`:62-70`）：不是把整个对话折叠成孤零零一段 summary，而是 **保留最近原文尾巴 + 只总结老前缀**——`KEEP_RECENT_TOKENS` 保底、`MIN_KEEP_MESSAGES` 保证至少 5 条原文。这比"全量总结"聪明得多：模型对最近的上下文记忆最强，丢的都是老历史。

**压缩后的恢复附件（Recovery）**：压缩会丢失"最近读过哪些文件、哪些技能 SOP 在用"这些工作状态，所以 `Agent.java` 里维护一个 `RecoveryState`，每次 ReadFile / 调 skill 时记录快照；压缩发生后，把这些快照作为附件追加到总结后面（`RECOVERY_FILE_LIMIT=5` 个文件、每个 5000 token、技能预算 25k）。这样压缩后模型还能"接得上"。

**熔断器**：`AutoCompactTrackingState` 记录连续失败次数，达到 3 次就停掉自动压缩，避免"压缩请求本身反复失败 → 无限重试"的恶性循环。

### 4.3 两条防线的配合

`Agent.java` 每轮：先 `ContextCompactor.manage()`（第二层判断）→ 再 `ToolResultBudget.apply()`（第一层本地替换）→ 才把 `conv` 送进 `client.stream()`。第一层省钱，第二层防爆，两层叠加把上下文控制得非常紧。

---

## 五、精华：LLM 协议抽象层

### 5.1 统一事件模型：StreamEvent（`StreamEvent.java:10-34`）

用 `sealed interface` 定义 8 种事件，把三家协议的流式响应统一成一个"翻译层"：

```java
public sealed interface StreamEvent {
    record TextDelta(String text) {}
    record ThinkingDelta(String text) {}
    record ThinkingComplete(String thinking, String signature) {}
    record ToolCallStart(String toolId, String toolName) {}
    record ToolCallDelta(String text) {}
    record ToolCallComplete(String toolId, String toolName, Map<String,Object> arguments) {}
    record StreamEnd(String stopReason, int inputTokens, int outputTokens,
                     int cacheReadTokens, int cacheCreationTokens) {}
    record Error(String message) {}
}
```

`Agent` 只依赖这套事件，完全不知道背后是 Anthropic 还是 OpenAI。

### 5.2 三个客户端（`AnthropicClient` / `OpenAiClient` / `OpenAiCompatClient`）

由 `LlmClient.create(cfg, systemPrompt)` 按 `protocol` 配置分派：

| 维度 | Anthropic | OpenAI | OpenAI Compat |
|---|---|---|---|
| API | Anthropic Messages SDK | OpenAI **Responses** API SDK | **手写** `java.net.http.HttpClient` 调 `/chat/completions` |
| 系统提示 | `system` 字段（block，可加 cache） | `instructions` 字段 | messages 里的 `role:"system"` |
| 思考 | `ThinkingConfigAdaptive/Enabled` | `Reasoning` + `ReasoningEffort.HIGH` | 不支持（注释明确 skip thinking） |
| prompt cache | 三处 `cache_control`（system/tools/user-tail） | 无（仅读用量） | 无 |

**注意 OpenAiCompatClient 是手写 HTTP 的**——这说明作者对协议细节掌握得很透，也是学"如何不用 SDK 调 LLM"的好教材。它把 SSE 解析、tool_calls 组装、错误分类全自己实现了一遍。

三个客户端都用**虚拟线程**驱动流式解析，`LinkedBlockingQueue<>(64)` 队列向上抛事件。

### 5.3 工具调用的协议映射

对话消息（`ConversationManager.Message`）到各家请求体的映射是统一的：

- **assistant 文本 + 工具调用** → Anthropic 的 `ContentBlockParam.ofToolUse` / OpenAI 的 `ofFunctionCall`（带 `callId/name/arguments`）。
- **工具结果** → Anthropic 的 `ofToolResult`（`toolUseId/content/isError`）/ OpenAI 的 `ofFunctionCallOutput`（`callId/output`）。
- **纯文本** → 各家文本消息，并 `mergeConsecutiveSameRole` 合并相邻同角色消息（很多 API 不允许连续同角色消息）。

这套映射是"如何把统一消息模型适配到各家 API"的标准答案。

---

## 六、精华：权限体系

权限是"AI 自主执行代码"的护栏。`PermissionChecker.java` 实现了多层检查，核心是 `check(toolName, content)` 返回三种决策：**ALLOW / DENY / ASK**（`PermissionMode.Decision`）。

### 6.1 五种权限模式（`PermissionMode`）

- `acceptEdits`：改文件前问，但跑命令不问。
- `bypassPermissions`：全放行（最危险）。
- `default`：跑命令前问，改文件不问。
- `plan`：只准做规划（只读 + 写 plan 文件），所有改动都禁。
- `dontAsk`：只读只写，命令一概不执行。

### 6.2 检查器的分层策略（`PermissionChecker.java`）

1. **危险命令黑名单**（`:76-85`）：`rm -rf /`、`mkfs.*`、`dd if=... of=/dev/`、`chmod -R 777 /`、fork bomb、`curl|sh`、`wget|sh`、`>/dev/sd`——正则命中直接 DENY。
2. **安全命令白名单**（`:62-74`）：`ls/pwd/cat/grep/git status/git log/...` 这类只读命令，`default` 模式下直接放行不弹窗。
3. **文件模式规则**（`:38-52`）：从 `permissions.yaml` 加载 `ALLOW`/`DENY` 规则，用 `PathMatcher` 的 **glob 匹配**文件路径。rule 是一个 `record PermissionRule(String toolName, String pattern, RuleEffect effect)`。
4. **内容字段映射**（`:87-94`）：不同工具要检查的内容字段不同——Bash 查 `command`、ReadFile/WriteFile/EditFile 查 `file_path`、Glob/Grep 查 `pattern`。
5. **Plan 模式工具白名单**（`:58-60`）：plan 模式下只允许 `Agent/ToolSearch/AskUserQuestion/ExitPlanMode`。

**RuleEffect 的 ALLOW/DENY 可以覆盖默认行为**——即"我明确允许你读某个目录"或"你永远不许动某个目录"，比纯问询更精细。

### 6.3 为什么值得讲

"AI Agent 的执行安全"是行业热门话题。这个模块展示了完整的分层策略：黑名单兜底 + 白名单免打扰 + 文件级规则 + 模式开关，四种手段叠加，比只做"弹窗问用户"成熟得多。

---

## 七、进阶：子代理与多智能体团队

### 7.1 子代理（`subagent` 包）

父 Agent 通过标准工具调用 **`Agent`** 工具来 fork 子代理（`AgentTool.java`，560 行）。`execute()` 按参数分派 4 条路径（`:243-272`）：

| 路径 | 触发条件 | 语义 |
|---|---|---|
| `runFork` | `subagent_type` 为空 | 克隆父对话上下文，后台跑（`:287-316`） |
| `runAsync` | `run_in_background=true` | 后台跑，不阻塞父 Agent（`:276`） |
| `runSync` | 默认 | 前台阻塞，事件逐条回传（`:344-447`） |
| `runAsTeammate` | `team_name` + `name` 同时给 | 变成长驻团队成员（`:461`） |

**Fork 的细节**（`:318-342`）：克隆父消息时，对"有 tool_use 但没有 tool_result"的 assistant 消息补占位结果 `"(tool execution interrupted by fork)"`，防止对话结构不合法；最后追加一段 `FORK_BOILERPLATE` + 任务作为 user 消息。还禁止嵌套 fork（检测到 `<fork_boilerplate>` 标签就报错）。

**内置子代理类型**（`SubAgentSpec.java:61-90`）：
- `general-purpose`：全工具，maxTurns=200。
- `plan`：禁 EditFile/WriteFile，只读架构师，maxTurns=15。
- `explore`：禁改文件，用 Haiku 便宜模型，maxTurns=30。

**子代理也可从 Markdown 加载**（`AgentLoader.java`）：`~/.JimiCode/agents/*.md` 和项目 `.JimiCode/agents/*.md`，YAML frontmatter 定义 `name/description/tools/disallowedTools/model/maxTurns`，正文作为系统提示覆盖。

**权限隔离的关键——`ToolFilter` 的 6 层过滤**（`ToolFilter.java:82-138`）：
1. `mcp__` 前缀工具一律放行；
2. 全局禁用 `{TaskOutput, ExitPlanMode, EnterPlanMode, Agent, AskUserQuestion, TaskStop, Workflow}`——**子代理不能再 fork 子代理**；
3. 自定义代理额外禁用同集合；
4. 异步模式只允许 `ASYNC_ALLOWED` 白名单；
5. 按 spec 的 `disallowedTools` 排除；
6. 按 spec 的 `tools` 白名单取交集（`*` 为全放行）。

### 7.2 多智能体团队（`teams` 包，11 个文件）

团队的本质是**一群长驻、可被消息唤醒的 Agent**。与普通 subagent 的判定就一个开关：`Agent` 工具是否同时传 `team_name` + `name`（`TeamTools.java:129-137`）。

**通信靠文件信箱 `FileMailBox`**：每个成员一个 JSON 收件箱 `.JimiCode/teams/<team>/inboxes/<agentId>.json` + `.lock` 锁文件。
- 写锁采用**随机退避**（`MAX_RETRIES=10`，退避 5–100ms，检测到超过 10s 的陈旧锁自动删）——一个可讲的并发控制范式。
- 成员循环每 500ms（`IDLE_POLL_MS`）轮询一次 mailbox。
- 消息用**文本前缀**约定语义：普通任务、`[idle]` 空闲通知、`[shutdown]` 关闭指令。
- 成员完成一轮自动给 lead 发 `[idle]`；lead 通过 `drainLeadMailbox` 聚合为 `<team-notification>` XML 块注入上下文。

**调度两种后端**（`SpawnDispatcher`）：
- `IN_PROCESS`：`Thread.startVirtualThread` 起虚拟线程跑成员，一个团队就几个虚拟线程，非常轻。
- `TMUX`：`tmux new-window` 启动独立子进程（可真正跨终端并行）。

**Lead 的协调者模式**（`Coordinator.java`）：coordinator 模式下 Lead 只能用白名单工具 `{TeamCreate, TeamDelete, SendMessage, Task*, Agent, ReadFile, Glob, Grep, Bash}`——**Lead 只协调、不出手**，靠 `SendMessage` + 任务依赖图（`SharedTask` 的 `blocks/blockedBy`）驱动成员干活，分四阶段：Research → Synthesis（分解任务）→ Implementation（spawn 成员）→ Verification。

**团队与 git worktree 解耦**：worktree 模块完全不知道 teams 存在，teams 只把 `workdir` 传给成员——"先给每个成员建一个 worktree，再把 worktree 路径当 workdir 传给 spawn"由上层（父 agent）编排。

### 7.3 Worktree 隔离（`worktree` 包）

- **为什么**：多个 Agent 并行改同一仓库，用 `git worktree add -B <branch> <path> HEAD` 建独立分支 + 独立目录（`.JimiCode/worktrees/<slug>`），`node_modules` 等大目录**软链接**回主项目共享。
- **建树后初始化**（`PostCreationSetup`）：拷贝 `settings.local.json`、配置 `core.hooksPath`（husky）、软链目录、按 `.worktreeinclude` 清单拷贝忽略文件。
- **slug 校验**（`SlugValidator`）：`MAX_LENGTH=64`，每段只能 `[a-zA-Z0-9._-]+`，禁 `.`/`..`。
- **清理的安全性（fail-closed）**：`StaleCleanup` 三层过滤才敢删：① 名字匹配临时模式（`agent-aXXXXXXX`、`wf_*` 等）② 超过 24h ③ `git status --porcelain` 干净且**没有未推送的提交**。拿不准就当它有改动，绝不删。
- **没有自动 merge**：改动留在各分支，由用户/父 agent 审阅后决定合并——这是保守但安全的设计。

---

## 八、工具体系与延迟加载

### 8.1 工具抽象

```java
public interface Tool {
    String name();
    String description();
    ToolCategory category();
    Map<String, Object> schema();          // JSON Schema，交给 LLM
    ToolResult execute(Map<String, Object> args);
    default boolean shouldDefer() { return false; }
}
```

`schema()` 返回的 JSON Schema 会被 `ToolRegistry.getAllSchemas(protocol)` 转成各家协议格式——OpenAI 用 `type:function/name/parameters`，Anthropic 用 `input_schema`（`ToolRegistry.java:44-61`）。

内置 11 个工具实现（`tool/impl` 包）：ReadFile / WriteFile / EditFile / Bash / Glob / Grep / Agent / Task / AskUserQuestion / PlanMode / MCP 桥接等。输出统一 `ToolResult`，且有 `MAX_OUTPUT_CHARS = 10_000` 的输出截断上限（`ToolRegistry.java:12`）。

### 8.2 延迟工具（deferred）机制

不是所有工具都值得把 schema 塞进每一轮请求——schema 本身也占 token。所以：
- `shouldDefer() = true` 的工具（如 MCP 工具）默认**不加载 schema**。
- Agent 循环每轮注入一条 system reminder："以下工具通过 ToolSearch 按需加载"（`Agent.java:189-199`）。
- 模型想用时先调 `ToolSearch`（`searchDeferred` / `findDeferredByNames`），把 schema 拉进来（`markDiscovered`），之后轮次 `getDeferredToolNames()` 就不再提示它。

这是一个"**schema 按需加载**"的 token 优化手段，和 tool-result 预算一脉相承：能省就省。

### 8.3 工具执行器 StreamingExecutor

`executeAll` 可以并行执行多轮收集到的工具调用，通过事件队列回传结果。每条工具结果最后被包成 `ToolResultBlock` 塞回对话，作为下一轮请求的一部分（`Agent.java:388-398`）。

---

## 九、Java 21 特性运用一览

这个项目本身是 Java 21 的"使用范例"，边读边对照：

| Java 21 特性 | 用在哪 |
|---|---|
| `record` | `StreamEvent` 的 8 种事件、`ToolCallInfo`、`ContentReplacementRecord`、`WorktreeSession`、`PermissionRule`…… 大量不可变数据载体 |
| `sealed interface` + 模式匹配 `switch` | `StreamEvent`（8 变体）+ `Agent` 里 `switch (event) { case StreamEvent.TextDelta td -> ... }` |
| 虚拟线程 `Thread.startVirtualThread` | Agent 主循环、三个 LLM 客户端流式解析、子代理后台任务、teammate 进程内运行——**一 Agent 一虚拟线程** |
| `String.formatted()` / text block | `"Agent reached maximum iterations (%d)".formatted(max)`、压缩用的 SUMMARY_SYSTEM_PROMPT 大段文本 |
| `Path.of()` / NIO | 文件读取、worktree 路径处理 |
| `List.copyOf` / `Stream.toList()` | 不可变集合贯穿全项目 |
| 本地变量 `var` | 全项目风格 |

**注意**：`record` 里嵌套 `record`、`sealed` 分层、`switch` 穷尽性保证——这些都是编译器替你保证"处理了所有分支"的地方，读 `StreamEvent` + `Agent` 的 switch 就能体会穷尽检查的威力。

---

## 十、学习路径建议

按你自己的目标（学习 Agent 架构）推荐阅读顺序：

1. **第一遍（1 小时，搭骨架）**：`JimiCode.java` → `Agent.java` 的 `agentLoop` → `StreamingExecutor` → `AgentEvent`。看懂"循环 + 事件 + 工具执行"这条主线。
2. **第二遍（2 小时，学精华）**：`ToolResultBudget` → `ContentReplacementState` → `ContextCompactor`。重点理解"省 token / 保前缀 / 防溢出"的工程思路。
3. **第三遍（按需，学细节）**：`llm` 包三个客户端对照读——同一套接口怎么适配 Anthropic 和 OpenAI；再看 `permission` 的分层检查。
4. **进阶**：`subagent`（fork/工具过滤）→ `teams`（文件信箱通信）→ `worktree`（并行隔离）。这三个组合起来就是一个完整的"多 Agent 并行编程"架构。
5. **动手**：项目自带了 `study-notes/sourcebook/`（小林coding 的源码教材）和 `docs/PROJECT_GUIDE.md`，可以配合阅读。改点小功能（比如加一个自定义工具、调预算阈值）最能加深理解。

### 可讲的面试点总结

- **上下文预算**：本地落盘替换 + LLM 总结压缩两层防线，以及"byte-stable prefix 保 prompt cache"。
- **统一协议层**：sealed interface 事件模型 + 三客户端适配。
- **权限护栏**：黑名单/白名单/文件 glob 规则/模式开关四层。
- **并发模型**：虚拟线程一个对话一个线程，团队进程内/进程外两种调度。
- **工具生态**：deferred schema 按需加载、MCP 桥接、skill SOP。

---

*本文基于对 `jimi-code` 源码的直接阅读整理，行号对应仓库当前版本。祝你学得愉快。*
