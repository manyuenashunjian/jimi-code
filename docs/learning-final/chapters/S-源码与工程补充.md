# 源码与工程补充

> 返回总稿：[JIMICODE-最终版学习教材.md](../JIMICODE-最终版学习教材.md)

以下段落来自深度学习、deep-dive 和 sourcebook；只有主干没有覆盖的内容才进入这里，重点是源码调用链、行为契约、异常、测试和工程取舍。

### 工程补充来源：`docs/deep-learning/06-核心链路语义讲解.md`

这份文档专门补足“有文件名但没有理解”的问题。`02-逐文件源码精读.md` 是覆盖清单，`05-核心Java文件逐方法讲解.md` 是方法签名与源码片段证据，本章则把这些证据串成可运行的语义：对象在哪里创建，状态怎样变化，方法为什么按这个顺序调用，失败时哪一层负责兜底。学习时不要只背类名，应该把每一条链路追到测试和文件格式。

`jimi-code` 不是 Spring MVC 应用。它没有 Controller、Service、Mapper 这种固定三层，也没有依赖数据库完成主流程。它是 Java 17+ 的本地 CLI/TUI Agent：输入来自终端，模型通过 HTTP/SSE 返回增量，工具直接操作工作区文件或子进程，会话和历史落盘到 `.JimiCode`，多 Agent 通过虚拟线程、文件邮箱和 Git worktree 协作。因此学习它时，应该把“接口”理解成四种接口：`LlmClient` 的 provider SPI、`Tool` 的工具 SPI、斜杠命令的命令接口、本地 JSON/YAML/JSONL 文件协议。

仓库里有 103 个主源码 Java 文件、对应测试、Maven 构建和已有的 study-notes。主包按职责分成：`com.jimicode` 启动；`config` 配置；`agent` 编排；`llm` 模型适配；`conversation` 消息；`prompt` 提示词；`tool` 工具；`permission` 权限；`hook` 钩子；`compact` 上下文压缩；`session/history/filehistory` 会话与审计；`memory` 长期记忆；`worktree` 隔离；`subagent/teams` 并发 Agent；`skill/mcp` 扩展协议；`tui` 用户界面；`toolresult` 工具输出预算与替换生命周期。

入口文件是 `src/main/java/com/jimicode/JimiCode.java`。`main(String[] args)` 做了五件有顺序依赖的事。

第一，解析配置路径。第一个命令行参数优先；没有参数时读取环境变量 `JimiCode_CONFIG`；两者都没有才交给 `ConfigLoader.load(null)` 做默认搜索。这里的设计意味着“配置路径选择”与“配置内容合并”是两个层次，不能把它们混成一个优先级规则。

第二，调用 `ConfigLoader.load`。显式路径只加载单文件并校验；隐式路径按 `~/.JimiCode/config.yaml`、当前目录 `.JimiCode/config.yaml`、当前目录 `.JimiCode/config.local.yaml` 的顺序尝试。后加载层覆盖 providers、permission mode 和同名 MCP server，并追加 hooks。`ConfigLoader` 使用 SnakeYAML 的 `Constructor(AppConfig.class)` 反序列化，再用 `SnakeCasePropertyUtils` 把 `base_url` 映射到 `baseUrl`。学习这里要特别注意：YAML 字段映射不是 Jackson 的 naming strategy，而是 SnakeYAML PropertyUtils 的自定义解析。

第三，`validate` 检查 providers 非空、每个 provider 的 name/protocol/base_url/model 非空，并把协议限制为 `anthropic`、`openai`、`openai-compat`。API key 没有在异常文本中回显，属于基本的秘密保护。配置错误抛出受检的 `ConfigException`，入口打印后退出；这是启动期失败，而不是进入 Agent Loop 后才暴露。

第四，`PermissionMode.fromConfig` 将字符串变成枚举。入口单独捕获 `IllegalArgumentException`，说明权限模式错误与 YAML 语法错误是不同故障类别。第五，创建 `JimiCodeModel` 和 TUI4J `Program`，把 program 反向注入 model，然后隐藏光标、注册 SIGINT、运行程序，finally 中恢复光标。这个 finally 很重要：即使模型或 UI 抛异常，终端也不会永久处于隐藏光标状态。

`AppConfig` 是聚合根，不执行 IO；`ProviderConfig` 描述一个模型提供商；`McpServerConfig` 描述 MCP 进程/命令；`HookConfig` 描述事件钩子。配置类上的 getter/setter 看似简单，但它们定义了整个系统的可配置边界。阅读时要记录每个字段的三个信息：来源（默认/YAML/环境/命令行）、消费方（谁调用 getter）、失败策略（空值是默认、跳过还是报错）。

`ProviderConfig.resolvedContextWindow()` 与 `resolvedMaxOutputTokens()` 不是普通 getter：它们把显式配置、模型默认值和安全上限归一化，`Agent` 构造时把结果冻结到 `contextWindow`/`maxOutput`。这解释了为什么运行中修改 YAML 不会影响已经创建的 Agent，也解释了上下文压缩必须读取 provider 的真实窗口，而不能只看一个常量。

## 四、Agent Loop：一次 turn 的每个阶段

`Agent` 是 jimi-code 的核心编排类。它持有 `LlmClient`、`ToolRegistry`、协议名、上下文窗口、权限检查器、HookEngine、工作目录、会话 id、记忆内容、恢复状态和工具结果替换状态。构造器只接收不可变的模型/注册表/协议/ProviderConfig；权限、Hook、最大迭代数、workDir 等通过 setter 注入，说明它被 `JimiCodeModel`、SubAgent 和 Team 复用组装。

`run(ConversationManager)` 不在调用线程直接执行模型，而是创建容量 64 的 `LinkedBlockingQueue<AgentEvent>`，再启动虚拟线程执行 `agentLoop`。调用方因此得到一个“事件队列”而非未来值。队列满时 `putSafe` 负责处理背压/中断；线程顶层捕获异常并转换成 `ErrorEvent`，避免异常直接丢失。

`agentLoop` 首先把 instructions 和 memoryContent 注入会话。随后是无限迭代，每轮依次执行：最大迭代检查；中断检查；后台 SubAgent 通知注入；计算当前工具 schema；自动压缩；注入延迟工具提醒；PLAN 模式提醒；应用工具结果预算；调用 LLM；消费流事件；执行工具；把结果追加回会话；若模型还需要工具则进入下一轮。

工具 schema 每轮重新计算，因为 Skill 或 ToolSearch 可能改变可见工具集合。`toolNameFilter` 只过滤 schema，不删除注册表对象，避免子 Agent 修改父 Agent 的全局能力目录。延迟工具的设计是先把名称作为系统提醒，模型必须调用 `ToolSearch` 加载 schema，这是一种“能力目录与能力细节分离”的上下文优化。

自动压缩调用 `ContextCompactor.manage`。它接收 conversation、client、contextWindow、maxOutput、workDir、sessionId、恢复状态、工具 schema 和 usageAnchor。`usageAnchor` 优先使用 provider 报告的真实 token；首次没有 usage 时才退回字符估算。压缩后如果消息数量减少，Agent 清除 anchor，下一次流重新锚定。这避免旧 token 统计与新消息列表错位。

工具结果预算是另一层优化。`ToolResultBudget` 根据协议和上下文预算决定哪些大型结果需要替换；`ContentReplacementState` 记录每个 toolUseId 的决定，保证同一会话后续请求的前缀稳定，尤其照顾 Anthropic prompt cache。`ApplyResult` 返回一个新的 API conversation，而不是原地修改 UI conversation，因此“显示历史”和“发送给模型的压缩历史”可以不同。

`LlmClient` 是最小 SPI；`AnthropicClient`、`OpenAiClient`、`OpenAiCompatClient` 负责把 ConversationManager 的消息和 Tool schema 转成 provider 请求，再把 SSE/JSON delta 转成统一 `StreamEvent`。Agent 不应该知道 provider 的字段差异；它只处理文本增量、思考块开始/结束、工具调用开始/参数增量/结束、usage 和错误。

消费流时必须使用 toolUseId 作为关联键，而不能按到达顺序猜测。一个模型响应可能同时包含文本、thinking 和多个 tool use；`ToolUseBlock` 保存名称、id、arguments，`ToolResultBlock` 保存 id、content、isError。工具结束后把结果追加为下一条消息，下一次模型请求才能看到完整闭环。若参数 JSON 只到了一半就结束，解析层应产生错误事件或不完整标记，不能静默调用工具。

`PermissionChecker` 按 `PermissionMode` 决定 ALLOW、ASK 或 DENY。只读搜索工具通常自动允许；Bash、WriteFile、EditFile、EnterWorktree 等具有副作用的工具进入 ASK 或 PLAN。TUI 中 `AskUserDialog`/`PlanApprovalDialog` 完成 HITL，回答再写回 Agent Loop。权限不是 UI 功能，而是工具执行前的安全边界；即使未来换成 HTTP UI，也必须保留同一检查器。

`ToolRegistry` 同时承担注册、按协议生成 schema、延迟工具列表和名称查找。`ReadFileTool` 记录恢复附件；`WriteFileTool` 创建或覆盖文件；`EditFileTool` 做精确替换并校验旧文本；`GrepTool`/`GlobTool`/`ToolSearchTool` 读取和发现；`BashTool` 启动外部进程；`AskUserTool` 把问题转成 UI 事件；Worktree/Plan/Agent 工具把高阶能力挂进同一个 SPI。每个 Tool 都应回答输入 schema、路径规范化、超时、输出上限、权限、错误包装和可重试性。

`HookEngine` 在模型调用前后、工具执行前后触发 HookConfig 对应的命令。Hook 失败应被记录并按配置决定阻断还是继续；不能因为 hook 是“附加功能”就吞掉所有异常，否则审计和安全策略会失效。

`ConversationManager` 维护消息列表与系统提醒；`SessionManager` 负责会话目录、恢复和 resume；`HistoryStore` 写入持久化 transcript；`FileHistory` 记录文件变更快照；`MemoryScanner` 扫描项目记忆，`MemoryRecall` 选择与当前任务相关的内容，`MemoryManager` 管理写入策略。上下文压缩不能只保留摘要：必须保留未完成任务、最近工具调用、工具 id、关键文件快照和技能 SOP，这正是 `RecoveryState` 存在的原因。

## 八、Worktree、SubAgent 与 Teams

`WorktreeManager` 使用 Git 命令创建、列出、删除和清理 worktree；`SlugValidator` 防止分支/目录名注入；`StaleCleanup` 处理过期目录；`WorktreeChanges.detect` 用 `git diff --stat` 判断未提交变化。它是文件级隔离，不是线程同步，合并仍需要 Git 语义。

`SubAgentTaskManager` 以 `TaskStatus` 管理 PENDING/RUNNING/COMPLETED/FAILED/CANCELLED，虚拟线程消费子 Agent 的 AgentEvent，父 Agent 在下一轮通过 notificationFn 注入结果。取消依赖 interrupt；因此子 Agent 内部的阻塞 IO、外部进程和 HTTP 客户端必须正确响应中断。

`TeamManager` 是更高层的成员目录；`FileMailBox` 是跨线程/跨进程通信协议；TMUX backend 负责可视化 pane。Team 的 stopAll 只中断成员线程，不能替代 worktree 清理、子进程回收和邮箱重放，这些是运维层必须补齐的责任。

已有测试覆盖 ConfigLoader、ProviderConfig、Anthropic context window、Usage parsing、PermissionMode、SessionManager、ContextCompactor、ToolSearch、ToolResultBudget 和 ReplacementRecordsIO。学习时建议为每条主链补一个失败测试：配置多层覆盖、SSE 分片断裂、重复 toolUseId、工具超时、权限拒绝、压缩失败回退、worktree 创建失败、子 Agent 取消、邮箱半写文件。

面试可从四个原理展开：为什么用事件队列而不是直接返回字符串（流式与解耦）；为什么 tool result 要做稳定替换（上下文预算与 prompt cache）；为什么 worktree 不能用 synchronized 代替（进程/文件系统隔离）；为什么配置加载要在启动期失败（fail-fast 与秘密保护）。

对 `02` 中每个文件执行固定动作：读类型声明；列出构造器调用者；逐个方法找用法；记录 IO/网络/线程/异常副作用；打开对应测试；运行最小测试；写一句“如果替换该类，必须保持什么契约”。这样才是真正的逐文件学习，而不是把文件名复制到 Markdown。

学习目标不是记住 `new Program(model)`，而是理解启动器为什么只做组装。它不应该把模型请求、工具执行或会话写盘塞进 main，否则测试和复用都会变差。阅读时画出配置失败的两条退出路径、SIGINT 的注册时机、光标恢复的 finally 路径，并思考如果 `Program.run()` 在注册 SIGINT 前就收到 Ctrl-C 会发生什么。

这是一个“分层配置合并器”，不是简单 YAML 读取器。显式 path 走单文件分支；隐式 path 走候选列表；providers 是整体替换，MCP servers 按 name 做 LinkedHashMap 合并，hooks 是追加。面试时应能解释为什么用 LinkedHashMap（保持用户配置顺序）、为什么空 override 不覆盖 base、为什么 parse error 和 validate error 要分开。

把字段按生命周期分组：模型/注册表是构造期依赖；checker、hook、workDir 是运行期注入；usageAnchor、replacementState、recoveryState 是跨迭代状态；queue、virtual thread 是一次 run 的执行资源。任何修改 Agent 的提交都应该说明它属于哪一组，是否需要 copy、清理或并发保护。

### `ConversationManager.java` 与消息块

Message、ThinkingBlock、ToolUseBlock、ToolResultBlock 共同构成 provider-neutral 的中间表示。不要在 UI 层直接拼 provider JSON；应由 LlmClient adapter 完成映射。学习时构造一条包含 system、user、assistant thinking、tool_use、tool_result、assistant final 的消息序列，验证序列化、压缩和 resume 后 toolUseId 是否保持不变。

压缩决策至少有三种输入：真实 usage、字符估算、上下文窗口。压缩动作至少有两层：工具结果替换和消息摘要。失败时应保留原会话并返回可见错误，不能把摘要异常吞掉后继续把超长上下文发给 provider。恢复附件的顺序也有契约：先恢复系统约束，再恢复最近文件快照和未完成任务，最后恢复可选历史。

### `ToolRegistry.java` 与 `ToolSearchTool.java`

注册表是能力目录，ToolSearch 是目录查询器。延迟工具模式的价值是减少每轮 schema token，但它引入了“名称已知、schema 未加载”的中间状态。测试必须覆盖：查询不存在工具、批量 select、重复加载、工具过滤器与 deferred list 不一致、加载后权限仍然生效。

WriteFile 是覆盖语义，EditFile 是匹配/替换语义；两者都应记录修改前状态。`ContentReplacementLifecycle` 负责记录工具结果如何从完整文本变成摘要/占位符，`ReplacementRecordsIO` 负责持久化，`ToolResultBudget` 负责预算决策。三者不能互相代替：预算是策略，生命周期是状态机，IO 是持久化。

Git 命令调用必须有工作目录、合并 stdout/stderr、超时和非零退出码检查。`parsePorcelain` 处理最后一个没有空行的 block，是典型边界条件。cleanupStale 是 best-effort，意味着调用者必须有监控发现删除失败；不能把“忽略异常”误解成“问题不存在”。

### `TeamManager.java` 与 `FileMailBox.java`

TeamManager 管理内存对象，FileMailBox 管理跨成员消息，tmux 只负责显示/进程后端。它们的故障域不同：内存 map 重启即丢，邮箱文件可能半写、重复读或残留，tmux 可能不存在。生产化时要增加原子写、消息 id、ack、重放和清理策略。

1. 使用两个 provider 配置启动，故意把第二个 provider 的 protocol 写错，观察启动期错误是否包含密钥。
2. 注册一个只读 Tool 和一个写文件 Tool，分别在 DEFAULT、PLAN、AUTO 模式执行，记录 PermissionResponse。
3. 将工具返回填充到超过 context window，观察替换记录、compact boundary 和恢复附件。
4. 在 EditFile 匹配文本不存在时检查错误是否回到模型，而不是修改错误文件。
5. 启动 SubAgent 后取消其虚拟线程，确认父 Agent 能收到 FAILED/CANCELLED 通知。
6. 创建 worktree 后人为制造未提交变更，验证 detectChanges、remove 和 cleanupStale 的行为。
7. 让 provider SSE 在 tool call JSON 中间断开，确认不会执行半个参数。

完成这些实验后，再回头阅读每个包中的其余小类（record、enum、dialog、test fixture），它们就不再是孤立文件，而会落到已经验证过的主链路上。

讲解任意一个类时，使用“职责—输入—状态—输出—失败—协作—替换”七句模板。例如讲 `ContextCompactor`：职责是控制上下文预算；输入是消息、窗口、usage 和恢复附件；状态是 anchor 与 replacement records；输出是摘要或新的会话；失败包括 provider 摘要失败和恢复文件缺失；协作对象是 ConversationManager、HistoryStore、ReplacementRecordsIO；替换点是 compactor 策略接口。这个模板能避免只说“它负责压缩上下文”这种空话。

讲解任意一条链路时，使用“触发点—第一跳—循环点—终止条件—持久化—用户可见结果”六段模板。把模板应用到 jimi-code，就能清楚回答：用户输入由 TUI 触发，第一跳是 Agent.run，循环点是 agentLoop 的下一次 iteration，终止条件是无工具调用/最大迭代/错误，中间状态写入 session/history，最终结果通过 AgentEvent 和 MarkdownRenderer 呈现。

最后建立一张自己的契约表：`ConfigLoader` 的契约是“输入路径/候选文件，输出已校验 AppConfig”；`LlmClient` 的契约是“输入 provider-neutral 消息和 schema，输出统一 StreamEvent”；`Tool` 的契约是“输入已校验参数，输出 ToolResult 且不泄漏未授权副作用”；`ConversationManager` 的契约是“保持消息顺序、角色和 toolUseId”；`HistoryStore` 的契约是“可恢复、可审计、写入失败可诊断”；`WorktreeManager` 的契约是“Git 隔离目录可创建、可列出、可回收”。以后修改任何文件，先写清它维护的是哪一条契约，再决定实现和测试。

这张契约表也是代码审查清单：检查输入是否已校验、输出是否可序列化、异常是否有上下文、线程是否可取消、文件是否可恢复、权限是否在副作用前判断、测试是否覆盖失败分支。把这些问题逐条写进评审记录，学习成果才会转化为工程能力。

建议每完成一个包就输出一页自己的笔记：类图、时序图、状态转移、失败注入实验和三个面试追问。这样阅读速度可能慢一些，但能避免“看完很多 Markdown，却无法解释一次真实请求”的问题。

最终验收不是字数，而是能否从任意入口追到模型、工具、状态和用户界面。

也要能指出边界：哪些逻辑在第三方 SDK，哪些逻辑只在本地 TUI，哪些状态可持久化，哪些状态只能在一次调用中存在。

当你能把这些契约讲给别人，并用测试证明它们，才算完成本项目的深入学习。

### 工程补充来源：`docs/deep-dive/01-入口配置消息与Agent主循环.md`

> 本册面向希望真正读懂 JimiCode 执行机制的 Java 开发者。重点不是重复 README，而是把一次用户输入从进程启动一直追到模型流、工具执行、消息回写和下一轮迭代。文中所有类名均来自当前源码；遇到“源码行为”和“工程建议”时会明确区分。

JimiCode 是一个终端里的 coding agent。它没有传统 Web 项目的 Controller/Service/DAO 三层结构，最接近的架构是“事件驱动的状态机 + 模型驱动的工具循环”。用户看到的是 TUI，模型看到的是一组带有文本、思考、工具调用和工具结果的消息，操作系统看到的是进程、文件、Git worktree 和外部模型连接。真正的系统边界如下：

```
命令行参数/环境变量
        |
        v
JimiCode.main
        |
        v
ConfigLoader -> AppConfig -> ProviderConfig/McpServerConfig/HookConfig
        |
        v
JimiCodeModel（Bubble Tea Model，保存 UI 与业务状态）
        |
        +--> ConversationManager（消息序列）
        +--> PromptBuilder（系统提示与环境）
        +--> Agent（迭代控制）
                  |
                  +--> LlmClient.stream
                  +--> ContextCompactor
                  +--> ToolResultBudget
                  +--> StreamingExecutor
                              |
                              +--> PermissionChecker
                              +--> HookEngine
                              +--> ToolRegistry / concrete Tool
        |
        v
AgentEvent -> JimiCodeModel.update/view -> 终端
```

这张图的关键是：Agent 不直接把字符串返回给 UI，而是通过 BlockingQueue 产生 AgentEvent；工具也不直接操作 UI，而是把权限请求、用户问题、工具结果和进度转成事件。这样做让 Agent 线程可以和 TUI 线程解耦，但同时带来队列满、线程中断、生命周期和异常转译问题。

1. 启动时解析配置，并把 provider、MCP、Hook、权限模式传给 JimiCodeModel。
2. 用户输入经过命令识别；普通文本会进入 ConversationManager。
3. JimiCodeModel 组装系统提示、环境信息、记忆内容和历史消息。
4. Agent.run 创建虚拟线程和有限容量队列，进入 agentLoop。
5. 每轮先注入后台通知、检查自动压缩、注入延迟工具名和 Plan reminder。
6. ToolResultBudget 根据过去工具结果的大小和生命周期构造给 API 的视图。
7. LlmClient.stream 将消息和工具 schema 发给 Anthropic、OpenAI 或兼容端点。
8. Agent 消费 StreamEvent，边消费边发 AgentEvent；完成的工具调用进入 StreamingExecutor。
9. StreamingExecutor 做 Hook 和 Permission 检查，再调用 Tool。
10. 工具结果以 ToolResultBlock 写入会话，下一轮模型根据结果继续推理。
11. stop reason 是 end_turn 时结束；tool_use 时继续；context 太长、rate limit、max_tokens 时进入不同恢复路径。
12. JimiCodeModel 消费 LoopComplete/TurnComplete/Usage/Error 等事件，并落盘 session/history。

## 2. JimiCode.java：进程入口的责任边界

文件：`src/main/java/com/jimicode/JimiCode.java`

`main(String[] args)` 做了四件事：确定配置路径、加载配置、构造 UI 模型、启动 TUI 程序。它没有把业务逻辑写进 main，这是一种很重要的边界。第一优先级是 CLI 第一个参数；没有参数时读取 `JimiCode_CONFIG` 环境变量；两者都没有时交给 `ConfigLoader.load(null)` 使用默认路径。源码使用的是大写风格的自定义环境变量名，部署时不要想当然地改成常见的 `JIMICODE_CONFIG`。

配置异常被转换成标准错误输出并以退出码 1 结束。权限模式再次解析时单独捕获 `IllegalArgumentException`，说明“YAML 能反序列化”不等于“语义配置合法”。接着构造 `JimiCodeModel`，对 mcpServers 和 hooks 做 null-to-empty 归一化，再把模型交给 tui4j 的 `Program`。`model.setProgram(program)` 是反向注入：JimiCodeModel 在收到需要退出、重绘或异步命令时需要知道 Program 实例。

这里还有两个容易错过的运行时细节。第一，启动时输出 `ESC[?25l` 隐藏光标，finally 中输出 `ESC[?25h` 恢复光标；如果在初始化阶段直接抛出未捕获错误，终端可能残留隐藏光标，因此生产化版本应该把更多启动异常纳入统一清理。第二，代码用虚拟线程延迟 500ms 后重新注册 SIGINT，目的是覆盖 TUI4J 默认的 Ctrl-C 退出行为，让 `model.handleSigint()` 接管中断。这个方案依赖时序：如果 Program 启动时间变化，500ms 可能过早或过晚；面试时可以把它当成“框架默认信号处理如何被安全替换”的问题。

## 3. AppConfig、ProviderConfig 与 ConfigLoader

`AppConfig` 保存 providers、mcpServers、hooks、permissionMode 等顶层项。它本身应该尽量是数据对象，而不应在 getter 中发起网络或读文件。配置加载的顺序是：文件文本 -> SnakeYAML -> Java 对象 -> 默认值/语义校验 -> 运行时组件。不要把 AppConfig 当成 Spring ConfigurationProperties；这里没有容器，生命周期完全由 main 和 JimiCodeModel 管理。

### 3.2 ConfigLoader 的 YAML 映射

ConfigLoader 使用 SnakeYAML 的 LoaderOptions、Constructor 和自定义 PropertyUtils。自定义的 `SnakeCasePropertyUtils` 解决配置字段命名与 Java bean 属性之间的映射问题。阅读它时要区分三类失败：

- 文件不存在：默认路径不存在时是“未配置”还是启动错误，要看 load 的分支；
- YAML 语法错误：解析阶段失败，应该给用户位置和原因；
- 字段语义错误：比如 provider 类型、协议、权限模式非法，可能要到 ProviderConfig 或 PermissionMode 才暴露。

配置对象常有默认值，因此测试 `ConfigLoaderValidationTest` 和 `ProviderConfigApiKeyTest` 的意义很大：它们锁定环境变量覆盖、临时文件加载、非法配置不应静默启动等行为。配置系统最危险的 bug 不是立即崩溃，而是把拼写错误当成未知字段忽略，最后使用错误默认值。学习时应检查 SnakeYAML 是否允许未知属性、空列表、null、重复键，以及默认配置路径是否位于用户 home。

### 3.3 ProviderConfig 的解析优先级

ProviderConfig 不只是 endpoint、apiKey、modelName 的 DTO，它还负责 resolvedContextWindow、resolvedMaxOutputTokens 等推导。模型调用时，contextWindow 决定何时压缩，maxOutput 决定请求输出上限；如果配置值为零或未给出，resolved 方法会根据 provider/model 的默认规则返回可用值。这里的设计要点是：将“原始配置”与“已解析运行参数”分开，便于诊断；但如果 resolved 方法内部含有较多隐式默认值，文档和测试就必须明确列出每个 provider 的行为。

API key 的优先级通常是配置项、环境变量、provider 专属环境变量之间的组合。源码测试专门验证 key 获取，说明密钥不是简单字段。学习时要特别检查是否将 key 写入异常消息、日志、session 或 Hook 环境变量；任何请求失败时打印完整 URL 和 headers 都可能泄露凭证。

## 4. PromptBuilder 与 PromptSections：系统提示是执行协议

Prompt 不是装饰性文案，而是模型与工具系统之间的协议。PromptBuilder 读取环境、日期、工作目录、Git 信息和可用工具说明，PromptSections 则把这些内容分成稳定段落。稳定前缀非常重要：Anthropic prompt cache 依赖字节稳定的前缀；如果每轮都把动态通知插在最前面，缓存命中和成本都会下降。

- 身份与能力边界：模型是谁，能做什么，不能假装做什么；
- 工作目录：相对路径应该相对于哪个目录，是否允许跳出目录；
- 工具契约：工具名、参数 schema、危险操作说明；
- 权限与 Plan 模式：先计划还是直接执行，什么时候要求用户确认；
- 记忆：长期规则与当前任务背景，避免把旧任务结论伪装成当前事实；
- 动态通知：后台 SubAgent、Team mailbox、任务更新；
- 当前用户消息：应当保持在消息序列的正确 role 中，而不是拼进 system 字符串。

PlanModePrompt 不是普通提示，它把“先读计划文件、更新步骤、经批准后执行”结构化。ExitPlanModeTool 则把这种模式从 Prompt 约束连接到 PermissionChecker 的状态。面试可以问：为什么不只在 UI 中显示“Plan mode”，还要把它注入模型上下文？因为模型的决策空间必须知道当前工具权限和完成条件；UI 标识无法改变模型是否调用写文件工具。

## 5. ConversationManager 与消息块

ConversationManager 保存的是 Message 列表，负责添加用户消息、助手文本、ThinkingBlock、ToolUseBlock、ToolResultBlock 和系统提醒。Message 的设计把一次 assistant 输出拆成多个内容块，而不是把所有内容拼成一个字符串。这是模型协议适配的基础：Anthropic 的 thinking signature、tool_use id、tool_result 对应关系不能通过纯文本可靠恢复。

```
user: 需求
assistant: thinking + tool_use(id=A)
user/tool: tool_result(tool_use_id=A)
assistant: 解释或下一次 tool_use
...
assistant: 最终答案
```

如果工具结果没有引用正确的 tool use id，Anthropic/OpenAI 适配器可能拒绝请求；如果把 thinking signature 丢掉，部分模型的后续请求也会失败。ConversationManager 的 helper 方法应当保证调用者不需要直接操作裸 List，从而集中处理空消息、复制、截断和恢复。

`addSystemReminder` 是一种特殊机制：它把后台通知、Plan reminder、deferred tool notice 以可识别方式插入上下文。通知不是用户输入，也不应被当成最终对话内容永久保存；但如果它影响下一轮模型选择，应该确保会话恢复时能解释其来源。阅读时要追踪这些 reminder 是否写入 session、是否会重复注入、压缩后是否保留。

Agent 是本项目的核心。构造器接收 LlmClient、ToolRegistry、协议和 ProviderConfig，解析上下文窗口与最大输出；随后通过 setter 注入 PermissionChecker、HookEngine、maxIterations、workDir、sessionId、notificationFn、instructions 和 memoryContent。这是一种“手工依赖注入”，优点是无框架依赖、便于子 Agent 复用；缺点是 setter 顺序错误可能产生半初始化对象，建议生产化时增加不可变配置或 builder。

run 方法创建容量为 64 的 LinkedBlockingQueue，并用虚拟线程执行 agentLoop。队列容量是背压设计：UI 消费太慢时，putSafe 需要决定阻塞、丢弃还是失败。AgentEvent 是对 UI 的稳定协议，模型 SDK 的事件不会直接暴露给 UI。

每一轮开始先检查 maxIterations 和线程中断。随后注入 notificationFn 的后台消息，再计算当前工具 schema。toolNameFilter 允许 SubAgent 或 Skill 缩小工具集合；注意它既影响模型看到的 schema，也影响恢复附件使用的 schema，源码注释明确要求两者一致，避免压缩后模型看到一套工具、恢复提示却描述另一套工具。

自动压缩调用 ContextCompactor.manage。压缩前记录 conv.size，压缩后如果消息数减少，就清空 usageAnchor，因为原来的 token 用量锚点对应的是旧消息序列。这个细节体现了“估算值与真实值不能跨状态变换直接复用”。随后注入 deferred tool names，告诉模型这些工具只有名称没有 schema，必须先调用 ToolSearch 的 select 协议加载；Plan 模式则创建或定位计划文件并注入 reminder。

接下来 ToolResultBudget.apply 创建 apiConv。关键点是 conv 不被原地修改，应用层只替换发送给模型的视图；新 records 追加到 replacement records 文件，失败时内存状态仍然作为进程内权威。这让工具完整输出可留在会话审计中，同时让下一次模型请求不超过预算。

LlmClient.stream 返回 BlockingQueue<StreamEvent>。Agent 以 30 秒 poll 读取，TextDelta 累加到 text 并发出 StreamText；ThinkingDelta 发出 ThinkingText；ThinkingComplete 形成 ThinkingBlock；ToolCallStart/Complete 发出 ToolUseEvent；StreamEnd 记录 stop reason 和 token 使用；Error 发出 ErrorEvent 并退出本轮。这里的事件消费是一个小型解析器，必须保证“完成事件”只落盘一次，特别是网络重试和重复 SSE 片段。

错误恢复按原因分流。上下文超长时最多尝试三次 forceCompact；rate limit 等待 5 秒后继续；其他错误结束当前 Agent。max_tokens 首次出现会提升 max output ceiling 并注入“从停止处继续”的用户消息，后续最多 MAX_OUTPUT_RECOVERIES 次把剩余工作拆小。这不是简单重试：重试改变了 conversation，因为模型必须知道上一次输出在哪里中断。若 blindly 重新发相同请求，可能产生重复写文件或重复工具调用。

正常返回后累加 token，发 UsageEvent。若 stopReason 是 tool_use，工具调用必须执行并将结果写回 conversation，再进入下一轮；若是 end_turn，发 LoopComplete/TurnComplete 并结束。实际源码还包含替换状态、恢复状态和 sessionId 边界写入，阅读时一定要把这些副作用与“模型输出解析”分开记录。

## 7. JimiCodeModel 如何把 Agent 接入 TUI

JimiCodeModel 是 2221 行的大型编排类，是理解系统真实行为不可绕过的文件。它同时实现 tui4j Model 生命周期、消息分发、命令处理、Agent 事件消费、会话选择、权限问答、Plan 审批、SubAgent 和 Team 进度显示。不要把它当成“视图类”；它更像应用层 coordinator。

Bubble Tea 风格通常是 `init`、`update`、`view`：update 接收键盘消息、窗口尺寸、异步命令返回消息；命令可以在后台运行并把结果再次送入 update。JimiCodeModel 还必须把 AgentEvent 转成 UI 状态：流文本追加到当前 ChatMessage，thinking 展示或折叠，tool use 创建工具块，tool result 更新工具块，permission request 打开确认，AskUser request 打开问题对话框，usage 更新统计，error 写入可见错误。

这里的并发模型是“单一 UI 状态线程 + 多个后台生产者”。因此不要在 Agent 线程直接修改 UI 的 ArrayList；应该通过消息或队列。后台邮箱轮询同样要通过 MailboxPollMessage。面试题可以问：为什么线程安全集合不能替代单线程 UI 模型？因为线程安全只保证数据结构不崩，不保证多个字段的状态变更原子，也无法保证绘制时观察到一致快照。

```
StreamEvent.ToolCallComplete
  -> AgentEvent.ToolUseEvent
  -> Agent 收集 ToolCallInfo
  -> StreamingExecutor.execute
  -> HookEngine.runPreToolHooks
  -> PermissionChecker.check
  -> PermissionResponse / 用户确认
  -> ToolRegistry.find
  -> BashTool.execute
  -> ToolResult
  -> AgentEvent.ToolResultEvent
  -> ConversationManager.addToolResult
  -> 下一次 LlmClient.stream
```

每一层只负责一件事：模型层负责解析参数，执行层负责生命周期，Hook 负责策略，Permission 负责授权，Registry 负责发现，具体 Tool 负责副作用，Conversation 负责协议回写，UI 负责呈现。任何一层绕过下一层都会制造安全漏洞或消息不一致。例如 BashTool 不能自己决定“用户已同意”，因为同意可能来自规则、模式或异步 UI；Tool 也不能自己把结果写进 Conversation，因为执行器还要处理替换、恢复和 UI 事件。

本项目没有统一的 GlobalExceptionHandler，异常在不同层被分类处理。LlmException 的子类把认证、限流、上下文过长和网络错误映射成 Agent 的不同恢复策略；ConfigException 在入口直接阻止启动；工具异常通常转成 ToolResult 错误文本，让模型决定是否修复；UI 异常转成 ErrorEvent 或可见消息；持久化异常很多是 best effort。

这种策略适合交互式 Agent，但有风险：如果把所有异常都作为工具文本返回，模型可能把权限拒绝或内部错误误认为普通业务失败；如果把所有异常都直接退出，用户无法继续会话。改进方向是给 ToolResult 增加 machine-readable error category，同时保留 human-readable message；给 AgentEvent.ErrorEvent 增加 retryable、userActionRequired 和 correlation id。

普通聊天是一问一答；ReAct 是 Reason → Act → Observe 的闭环。JimiCode 的 thinking/text 对应 Reason，ToolUse 对应 Act，ToolResult 对应 Observe。循环结束条件不是“收到一段文本”，而是模型 stop reason 表示 end_turn，或者达到迭代上限/错误恢复上限。

流式输出降低首 token 延迟，允许 UI 显示思考和工具进度，也能在长任务中及时感知中断。代价是需要处理增量 JSON、工具参数跨片段拼接、连接断开、重复事件、半完成工具调用和最终 usage。StreamEvent 归一化了不同 provider 的差异，让 Agent 不直接依赖某一家 SDK。

旧消息可能包含文件内容、工具约束、用户决策和未完成任务状态。直接删除会让模型失去因果链。ContextCompactor 通过摘要、保留尾部和 RecoveryState 附件重建工作上下文；ToolResultBudget 则按结果生命周期替换，尽可能保留结构而不是所有原文。

JimiCode 的 Agent loop、Shell 进程等待、阻塞式 SDK 和队列 poll 都是阻塞友好型任务，虚拟线程降低了创建大量平台线程的成本。但虚拟线程不等于异步：Thread.sleep、Process.waitFor、InputStream.readAllBytes 仍然会阻塞逻辑；如果调用外部资源没有超时，虚拟线程也会无限占用任务生命周期。

### 10.5 如何回答“这个项目的全局异常处理在哪里”

准确回答是：它没有 Web 意义上的单一全局异常处理器，而是分层恢复协议。入口捕获 ConfigException；Agent 捕获模型流异常并按 context/rate-limit/max_tokens 分类；工具执行把业务失败变成 ToolResult；UI 把 AgentEvent.ErrorEvent 呈现给用户；持久化部分采用 best effort。面试时要继续指出优点是恢复策略贴近故障来源，缺点是错误语义分散、观测和统一告警较弱。

1. 使用错误 provider 配置，观察 ConfigLoader、ModelResolver 和 LlmException 的边界。
2. 设置很小的 contextWindow，触发自动压缩，比较 conv 与 apiConv。
3. 让模型返回 max_tokens，观察 maxOutput escalation 和恢复消息如何进入 ConversationManager。
4. 对 BashTool 配置 pre_tool reject Hook，验证 Hook 与 Permission 的先后顺序。
5. 让 ReadFileTool 读取大文件，观察 ToolResultBudget 是否只压缩发送视图而不破坏文件历史。
6. 启动 SubAgent 并观察父 Agent 的 notificationFn 如何把后台进度变成 system reminder。
7. 在 TUI 中发送 Ctrl-C，验证信号处理、Agent 中断和光标恢复是否一致。

完成这些实验后，学习者应该能从任意一个入口反向定位到模型调用、工具副作用和持久化位置，而不是只记住类名。

### 工程补充来源：`docs/deep-dive/02-工具权限Hook-MCP与文件变更.md`

在 coding agent 中，工具是模型可以触发的副作用边界。一个工具的完整生命周期包括：声明名称和 schema、进入 ToolRegistry、按协议暴露给模型、模型返回 tool call、参数解析、延迟 schema 加载、权限判断、Hook 拦截、具体执行、结果截断、状态缓存、会话写入和下一轮观察。JimiCode 把这些责任拆成 Tool、ToolCategory、ToolRegistry、ToolResult、FileStateCache、impl 子包、PermissionChecker、HookEngine 和 StreamingExecutor，拆分的价值在于安全策略不会被某一个工具硬编码。

```
模型可见 schema
       |
       v
ToolCall(name,args,id)
       |
       +--> ToolRegistry.resolve
       +--> HookEngine.pre_tool
       +--> PermissionChecker
       +--> 用户确认/规则命中
       +--> Tool.execute
       +--> FileHistory/FileStateCache（若有副作用）
       +--> ToolResult
       +--> HookEngine.post_tool
       +--> ConversationManager
```

这里最重要的原则是“模型建议动作，框架决定动作”。模型输出的工具名和参数永远是不可信输入；即使模型说“用户已经同意”，也不能跳过权限层。

## 2. Tool.java、ToolCategory.java、ToolResult.java

`Tool` 是工具抽象。它至少需要给出名称、描述、参数 schema、类别和执行入口。ToolCategory 把工具分为 read、write、execute、search、interaction、system 等语义类别，PermissionMode 和 PermissionChecker 依赖类别做默认决策。类别不是 UI 标签，而是安全策略的输入；新增工具时如果选错类别，可能导致本应询问用户的动作自动放行。

`ToolResult` 是工具到 Agent 的协议。它应该区分 success/error，携带可供模型理解的文本，并允许附带结构化信息。失败结果不能只抛异常，因为模型往往需要观察错误并修正参数；但认证失败、越权、路径越界和系统崩溃也不能全伪装成普通工具失败。工程上建议保留 errorCode、retryable、userActionRequired 和 truncated 字段。

## 3. ToolRegistry：发现、schema 与延迟工具

ToolRegistry 保存已注册工具，负责按协议生成 schema、按名字找到工具、按类别筛选和 deferred tool 管理。注册顺序是否稳定很重要：稳定的 schema 顺序可以改善 prompt cache，也使测试和日志更可比。不要使用无序 HashMap 直接拼工具列表，除非调用方明确排序。

ToolSearchTool 支持延迟工具。延迟模式把工具分为“模型初始可见的轻量集合”和“仅名称可发现的 deferred 集合”。Agent 每轮注入 deferred tool names，并要求模型通过 `select:name[,name]` 加载 schema。这个设计解决工具数过多导致上下文膨胀的问题，但增加了一个协议状态机：

1. 模型发现名称；
2. 调用 ToolSearch；
3. ToolSearch 验证 select 语法；
4. Registry 把目标 schema 标记为 active；
5. 下一次模型调用才能真正执行目标工具。

ToolSearchTest 锁定的不是字符串格式，而是“schema 可见性状态”与“真实执行权限”之间的关系。搜索到一个工具不等于它已经执行，也不等于它绕过了权限。

## 4. 文件读取：ReadFileTool、GrepTool、GlobTool

ReadFileTool 负责路径解析、文件存在性、目录拒绝、编码读取、行范围或输出上限，并把读取状态登记到 FileStateCache 或 Agent 的 RecoveryState。读取文件看似安全，但路径穿越、符号链接、超大文件和二进制内容都可能造成问题。工作目录约束不能仅依赖字符串前缀：`/workspace/app2` 可能错误匹配 `/workspace/app`，应使用 Path.normalize、toRealPath 或 Path.startsWith 的正确语义，并决定是否允许不存在的新路径。

读取结果需要同时考虑三个消费者：模型上下文、UI 展示和压缩恢复。模型需要内容与路径，UI 需要截断提示，RecoveryState 需要最近读取的快照。若只保存一个很大的字符串，会造成内存和 session 膨胀；若只保存摘要，压缩后模型无法恢复关键代码。最好的策略是分层保存：完整内容可按需落盘，当前消息使用预算视图，恢复附件只包含最近且相关的文件片段。

GrepTool 通常调用本机 grep 或 Java 读取目录，涉及正则 Pattern、错误输出、行号、二进制跳过和文件过滤。源码中同时导入 Process/stream、Path、Pattern，说明它可能兼容外部 grep 与 Java 文件遍历。外部命令的好处是性能和功能丰富，代价是平台差异、命令注入、编码和退出码解释。参数必须通过 ProcessBuilder 参数列表传递，不能把用户搜索词拼进 `bash -c`。

正则是另一个边界：PatternSyntaxException 应转成用户可理解的工具错误，而不是让整个 Agent 线程结束。空结果不是异常；权限不足、目录不存在、超时和非法 regex 才需要分类。面试可以问 grep 工具如何避免“搜索整个 node_modules/target 导致上下文爆炸”：应设置忽略目录、文件大小、匹配行上限、总输出预算和明确的 truncated 标记。

GlobTool 通过 PathMatcher 或 Files.walk 处理通配模式。它的输出应稳定排序，避免同一个请求每次返回不同顺序导致模型判断抖动。遍历目录时要处理符号链接循环、隐藏目录、权限异常和极深路径。结果上限必须在遍历过程中生效，而不是先把所有路径放入 ArrayList 再截断，因为大仓库会先耗尽内存。

WriteFileTool 的安全链路应为：解析目标路径 -> 确认工作目录/权限 -> 创建父目录策略 -> 记录旧内容或 hash -> 写入临时文件 -> 原子移动 -> 更新 FileStateCache -> 返回摘要。源码使用 Files 和 PosixFilePermission，说明还要考虑 Unix 权限；Windows 上 PosixFilePermission 可能不支持，异常处理不能阻止所有正常写入。

全量覆盖写入的风险是模型提供空内容、错误路径或部分生成，导致文件丢失。FileHistory 可能在写入前建立备份；若没有备份，至少应使用临时文件和原子替换，避免进程中断留下半个文件。写文件结果不应把完整新文件再次塞回模型上下文，而应返回路径、字节数、hash、变更摘要和必要的前后片段。

EditFileTool 采用 oldString/newString 或类似精确替换时，必须处理匹配次数：0 次说明上下文过期，1 次是理想情况，多次说明定位不唯一。盲目 replaceAll 会把多个区域同时修改，这是 coding agent 中最危险的静默错误之一。工具应该提供 replace_all 的显式开关，并在多匹配时拒绝执行或让模型重新提供更大上下文。

编辑前的 FileStateCache 用于乐观并发检查：ReadFile 时记录 size、mtime 或 hash，EditFile 时验证文件没有被其他进程修改。如果被修改，应返回 stale file 错误，让模型重新读取，而不是覆盖用户的变化。这个机制相当于轻量级 CAS；面试可类比数据库的版本号更新。

BashTool 是风险最高的内置工具。它涉及命令解释器、工作目录、stdin、stdout、stderr、退出码、超时、中断和权限。安全设计至少包含：

- 命令由模型提供，但执行前必须经过 PermissionChecker；
- 工作目录使用明确 Path，不从命令字符串推断；
- ProcessBuilder 不要把未验证参数拼到 shell；
- stdout/stderr 必须有上限，避免 `yes`、日志或二进制输出拖垮内存；
- 超时后 destroy，必要时 destroyForcibly，并等待子进程结束；
- 保留退出码和截断标记，让模型知道命令是否真的成功；
- 中断时恢复线程中断标记，不要吞掉 InterruptedException；
- 生产环境应考虑进程组、网络权限、环境变量白名单和 sandbox。

“执行命令”与“执行 shell 字符串”不是一回事。即使 PermissionChecker 允许 Bash 类别，也要决定允许的工作区、环境和资源。HookEngine 的 command action 也会使用 `bash -c`，因此 Hook 配置本身是高权限输入，不能因为来自配置文件就视为安全。

## 7. PermissionMode 与 PermissionChecker

PermissionMode 定义默认策略，例如允许读取、询问写入、拒绝执行，或者 Plan 模式只允许读和计划文件操作。PermissionResponse 表示用户对一次请求的异步决定。PermissionChecker 除了模式，还加载 PermissionRule，使用 glob/path matcher、工具类别和参数条件进行 allow/deny/ask 判断。

检查顺序应该稳定，否则“更宽松的规则”可能覆盖安全拒绝。常见策略是 deny 优先、显式 allow 次之、默认 ask；但具体源码要以实际实现为准。学习 PermissionChecker 时要读它如何处理：

- 工具类别；
- 工具名；
- 路径参数；
- 规则文件与默认规则；
- 子 Agent 的 inherited permission；
- Plan 模式；
- 用户一次批准是否只对当前调用、当前会话还是同类工具生效；
- 路径不存在时如何做匹配；
- Windows 和 Unix 的路径分隔符。

权限检查不能只依赖模型传来的 JSON 字符串。比如模型把 `../secret` 编码、使用符号链接或把危险动作藏在 Bash 命令里，类别检查和路径检查可能失效。对于 Bash，应把命令视为不可结构化字符串，默认更严格；对于写文件，应解析真实目标路径并检查父目录。

## 8. HookEngine：可编程策略与拒绝语义

HookEngine 定义 SESSION_START、TURN_START、PRE_SEND、POST_RECEIVE、PRE_TOOL_USE、POST_TOOL_USE 等事件，ActionType 包括 command、script、prompt。HookContext 包含 event、toolName、toolArgs、filePath、message、error，条件支持等值和正则。

`runPreToolHooks` 与通用 `runHooks` 的差异非常关键：pre-tool hook 需要能在工具执行前返回 rejected/message，而普通 hook 主要产生通知。这说明 Hook 具有两种语义：观察型和控制型。观察型失败可以记录；控制型失败必须阻止副作用，并把可解释原因传给 UI/Agent。

条件表达式解析不是完整脚本语言。等值条件如 `tool==bash`、正则条件如 `event=~session.*`，未知操作为了前向兼容可能被当作 truthy。这一选择有风险：拼写错误不会拒绝，而可能意外执行。生产改进应对未知操作给出配置警告，或者采用 fail-closed。

命令 Action 通过 ProcessBuilder 设置 JimiCode_EVENT、JimiCode_TOOL 环境变量，读取 stdout/stderr 并等待退出码。这里的 IO 是同步的；Hook 命令卡住会阻塞 Agent。应增加超时、输出上限和环境变量白名单。Hook 的 stderr 是否返回模型也需谨慎，里面可能包含密钥或内部路径。

McpManager 连接 stdio 或 streamable HTTP 服务，把 MCP tool 包装为 JimiCode Tool。MCP 的价值是工具可以独立进程、独立语言和独立发布；JimiCode 只需要处理工具列表、schema、调用和结果。McpToolWrapper 必须解决协议字段到 ToolResult 的映射，包括文本、结构化 JSON、资源链接、错误和连接关闭。

MCP 连接生命周期包括创建 transport、建立 client、列工具、注册 wrapper、调用、关闭。不能只在启动时连接而永不关闭，尤其是 stdio 子进程；也不能每一次 tool call 都创建连接，否则延迟和资源泄漏严重。配置中的 command/args 可能执行任意本地程序，必须当成安装级权限。

安全上要注意供应链：示例中通过 npx 拉包如果不固定版本，会把网络下载和第三方代码执行引入运行时。HTTP MCP 要做 endpoint allowlist、TLS 校验、认证、超时和响应大小限制。MCP 工具还应继承 JimiCode 的 PermissionChecker，而不能因为“外部工具已经注册”就默认可信。

## 10. StreamingExecutor 的编排价值

StreamingExecutor 接收已完成的 ToolCallInfo，处理工具查找、Hook、权限、异步用户确认、执行和结果回传。它把 Agent 的“解析模型事件”和“执行副作用”分开，便于未来并行工具调用。注意并行执行不是默认安全：两个工具可能同时修改同一文件、一个删除目录另一个读取文件，必须按工具类别或资源锁进行协调。

它还负责把最近文件读取或 Skill 调用登记到 RecoveryState，使 ContextCompactor 在上下文缩减后能够附加关键上下文。这个交叉依赖说明恢复能力不是单独的压缩功能，而是由执行器在正常路径不断采集证据。

工具执行异常的策略通常是把异常消息包装成 ToolResult，并让 Agent 继续。为了防止模型把内部堆栈当指令，返回结果应分离安全的摘要、可诊断的日志和机器可识别的错误类别。用户拒绝也应与工具失败区分：拒绝是策略结果，失败是执行结果，重试建议不同。

一次 EditFileTool 成功写入后，至少有四份状态需要一致：

1. 磁盘文件；
2. FileStateCache 中的 hash/mtime；
3. FileHistory 的备份或 snapshot；
4. ConversationManager 中描述变更的 ToolResult。

如果只更新磁盘而缓存仍是旧 hash，下一次编辑会误报 stale；只更新缓存而备份失败，回滚能力丢失；只写 ToolResult 而磁盘写失败，模型会误以为修改成功。工程上应按“先可恢复、后覆盖、再更新缓存、最后报告成功”的顺序设计，并在失败时明确哪个阶段完成了。

Worktree 模式会把这个问题扩大到 Git：AgentWorktree 创建隔离目录，WorktreeSessionStore 保存映射，WorktreeChanges 通过 git diff/status 计算结果，ExitWorktreeTool 负责退出与汇总。工具层需要知道当前 workdir，不能把主仓库路径写死。用户在主分支有未提交改动时，进入和退出 worktree 的行为更需要可解释，否则会造成“看起来写了但用户找不到”的体验问题。

工具注册描述“能做什么”，权限描述“在当前上下文是否允许做”。把两者混在一起会导致每个工具重复实现安全逻辑，且无法统一 Plan、用户确认和规则文件。分离后可以对 MCP、SubAgent 和内置工具采用一致的策略。

Hook 更像生命周期事件观察/拦截，围绕单个阶段触发；Middleware 更像请求链，可以包裹后续执行并改变输入输出。JimiCode 的 HookEngine 是轻量配置型策略，不能直接等同于 AgentScope 的 MiddlewareChain。面试回答时要结合实际代码，不要泛称“都是 AOP”。

模型重试、网络断开和 UI 重绘都可能让调用者看起来重复。工具调用应有 tool call id，持久化时记录状态，具有副作用的操作最好使用幂等键或先检查目标状态。对于写文件，hash/CAS 和 file history 可以减少重复覆盖；对于 Bash，必须假设命令不可逆，重试前要求人工或策略确认。

工具结果往往是最大的单项输出，而且旧结果的价值随时间衰减。按结果生命周期替换可以用摘要保留事实，用完整结果保留最近证据，同时比粗暴截断整个上下文更少破坏消息协议。ToolResultBudget 的核心不是字符串截断，而是维护“原始记录、发送视图、替换状态”三者一致。

- 创建一个只允许 ReadFile 的权限模式，确认 WriteFile、EditFile、Bash 的行为分别是拒绝还是询问。
- 配置 `pre_tool_use` reject Hook，验证它是否在 PermissionChecker 前后执行，以及拒绝原因如何进入 UI。
- 让 EditFile 的 oldString 匹配零次和多次，观察是否安全失败。
- 让 Bash 产生超大 stdout，观察截断、进程终止和 Agent 是否还能继续。
- 启动一个最小 MCP stdio 服务，确认工具 schema、调用结果、连接关闭和权限继承。
- 在用户手动修改文件后再次 Edit，验证 FileStateCache 的 stale 检查。
- 在 worktree 中写入文件并退出，比较主仓库、worktree 和 WorktreeChanges 的结果。

读完本册后，应该能解释“模型为什么不能直接写文件”：因为工具只是意图，真正的副作用必须经过 Registry、Hook、Permission、执行器、文件一致性和会话审计的完整链路。

下面把本册涉及的文件按“声明—编排—执行—安全—持久化”重新串起来。`Tool.java` 是最小能力契约，`ToolCategory.java` 是策略分类，`ToolResult.java` 是结果协议；三者不应该包含具体文件或 Shell 逻辑。`ToolRegistry.java` 承担发现和 schema，`FileStateCache.java` 承担读取版本快照。`ToolSearchTool.java` 解决工具数量和上下文预算问题，但它的 select 语法仍然必须经过 Registry 校验。

`ReadFileTool.java`、`GrepTool.java`、`GlobTool.java` 是只读检索工具，重点看路径正规化、遍历上限和结果截断；`WriteFileTool.java`、`EditFileTool.java` 是写入工具，重点看旧内容备份、匹配次数、缓存更新和失败阶段；`BashTool.java` 是进程工具，重点看命令解释器、工作目录、timeout、stdout/stderr 和中断；`AskUserTool.java` 是交互工具，重点看 CompletableFuture 如何与 TUI 的 AskUserRequestEvent 配对；`EnterWorktreeTool.java`、`ExitWorktreeTool.java` 把工具层连接到 Git 隔离。

`PermissionMode.java` 将高层模式压缩成工具级 Decision，`PermissionResponse.java` 只是异步用户答案，真正复杂的规则匹配和路径处理在 `PermissionChecker.java`。因此不能只修改枚举来增加权限能力；必须检查规则加载、glob matcher、参数提取、子 Agent 继承和 Plan 文件语义。`HookEngine.java` 是另一条策略管道，它支持条件、command/prompt action 和 reject；其失败语义与 PermissionChecker 的 deny/ask 不完全相同，写文档时必须分开。

`StreamingExecutor.java` 是这些能力的汇合点：它把已完成的 ToolCallInfo 转成 ToolExecResult，调用前先做 Hook/Permission，调用中连接队列和用户确认，调用后把结果交回 Agent，并把 RecoveryState 的证据登记进去。若要调试工具“模型看到了但没有执行”，先检查 ToolRegistry schema；若“执行了但不安全”，检查 PermissionChecker/HookEngine；若“执行成功但下一轮不理解”，检查 ToolResult、ConversationManager 和 ToolResultBudget；若“文件已变更但再次编辑报错”，检查 FileStateCache/FileHistory。

假设模型返回 `EditFileTool`，参数包含 path、oldString、newString。首先，provider adapter 把 function call 或 tool_use 转成内部 `ToolCallComplete`，工具参数还是不可信的 Map。Agent 把它放入当前 assistant message，确保 tool call id 与模型协议一致。StreamingExecutor 找到 `EditFileTool`，再调用 HookEngine 的 pre-tool hook；如果 Hook 条件匹配 `tool==edit_file` 且 reject，磁盘完全不会触碰。

如果 Hook 放行，PermissionChecker 根据当前 mode、tool category、path 和规则判断 allow/ask/deny。ask 会发一个 PermissionRequestEvent，调用线程等待 CompletableFuture；用户答复只对当前 tool call 生效。allow 后 EditFileTool 解析工作目录和目标 Path，读取当前文件并核对 FileStateCache 的 hash。如果 hash 变化，返回 stale 错误；没有变化才查找 oldString。零匹配和多匹配都应拒绝，唯一匹配时先建立 FileHistory 备份，再写临时文件并替换。

写入完成后更新 FileStateCache，生成包含 path、match count、bytes、hash 或 diff 摘要的 ToolResult。StreamingExecutor 发出 ToolResultEvent，Agent 将 ToolResultBlock 追加到下一轮消息。ToolResultBudget 可能只把摘要/引用发送给模型，但 session 或 FileHistory 仍保存可追溯证据。这个时序说明：一个工具调用不是一个方法，而是一个跨线程、跨协议、跨持久化边界的事务近似过程。

反例一：只用 `path.toString().startsWith(workDir)`。攻击者可以利用相邻目录前缀、大小写、符号链接和 `..` 绕过。反例二：权限规则只看 tool name，不看参数。所有 Bash 命令都叫 bash，真正风险在 command；所有写文件工具都叫 write，真正风险在 resolved path。反例三：把 Hook 配置当作可信代码。Hook command 本身可以启动任意进程，且环境中可能包含敏感变量。

反例四：工具输出不设上限。模型可能读取二进制、日志或无限流，内存和上下文同时爆炸。反例五：用户拒绝被包装成普通工具失败后立即重试。拒绝是政策，不是参数错误；模型不应通过换一种措辞绕过拒绝。反例六：EditFile 允许多次替换。模型可能误改多个相似函数，测试不一定立刻发现。

问：为什么 PermissionChecker 不能只在 UI 层做？答：工具也可能由 SubAgent、MCP 或后台任务触发，UI 不一定存在；安全判断必须在副作用最近的执行边界，并且要能被非交互模式复用。

问：为什么工具结果要同时有文本和结构化字段？答：文本适合模型自然语言观察，结构化字段适合 UI、日志和策略判断，例如 exitCode、truncated、retryable；只有文本会导致上层再次解析不稳定字符串。

问：一个工具调用跨越了模型重试，怎样避免重复写入？答：用 tool call id、文件版本/hash、执行记录和幂等检查；对不可逆 Bash 不应自动重放，模型请求重试必须知道前一工具是否已完成。

问：MCP 工具注册后是否可以跳过本地权限？答：不可以。MCP 只定义发现和调用协议，不替本地应用承担用户授权、路径边界、租户隔离和审计责任。

读者如果能独立回答以下问题，才算真正读懂工具层：`ToolRegistry.getAllSchemas` 如何影响模型；deferred tool 如何从名称进入 active schema；BashTool 如何处理 timeout 和 stderr；EditFileTool 如何处理零/多匹配；FileStateCache 如何发现并发修改；HookEngine 的 reject 与 PermissionChecker 的 deny 有何差别；MCP wrapper 如何把外部结果变成 ToolResult；ToolResultBudget 为什么不直接删除 live conversation；Worktree 工具如何保证当前 workDir 传到所有具体工具。任何一个问题答不出来，都应继续回到对应文件逐方法阅读。

### 工程补充来源：`docs/deep-dive/03-会话记忆压缩历史与Worktree.md`

传统 CLI 可以把输出打到终端就结束；coding agent 必须在长时间、多轮、可中断、可恢复、可回滚的环境中工作。因此 JimiCode 的“状态”不只有内存里的消息，还包括：

- session JSONL：会话审计、恢复、compact boundary；
- prompt history JSONL：输入历史和循环缓冲；
- memory Markdown/JSON：跨会话规则与事实；
- replacement records：工具结果从完整到摘要的决策日志；
- FileHistory：文件变更前的备份和 snapshot；
- Worktree session：Agent 与隔离目录的映射；
- task/team/mailbox：后台协作状态。

这些状态的生命周期不同。ConversationManager 是短期活跃状态；SessionManager 是会话级恢复状态；MemoryManager 是跨会话知识；FileHistory 是副作用回滚；WorktreeSessionStore 是工作区生命周期。将它们都塞进一个“数据库”并不会自动解决语义，反而可能混淆哪些内容可重放、哪些内容只能审计。

## 2. SessionManager：JSONL 不是简单日志

SessionManager 使用 JSONL 文件保存 SessionMessage。每行一个 JSON 对象，便于追加、损坏跳过、流式写入和人工检查。典型记录至少包含 role、content、timestamp；compact boundary 会使用特殊结构保存摘要、保留消息和恢复信息。

追加式日志的优点是崩溃后大部分记录仍然可读，缺点是一个会话可能包含压缩前的原始前缀、压缩边界、边界后的新消息。恢复不能简单地把所有行逐条 replay，否则会把压缩前的上下文和压缩后的摘要重复发送给模型。

SessionManager 的核心算法是 findLastCompactBoundary：

1. 从文件加载所有可解析消息；
2. 从末尾反向寻找最后一个 compact boundary；
3. 解析 boundary 的 summary 和 keep；
4. boundary 后的普通消息全部保留；
5. 如果 boundary 损坏，回退到完整 replay，而不是静默丢失对话；
6. rebuildConversation 用摘要 user message + keep tail + boundary 后消息构建新的 ConversationManager。

这里的“最后一个边界”很关键。会话可能发生多次压缩；最后一个边界代表当前 live state，早期边界和原始前缀保留作审计但不重放。boundary 后如果又写入新的用户消息和 assistant/tool 消息，必须按原顺序接到恢复序列。

## 3. compact boundary 的数据语义

压缩边界不是普通摘要文本，而是一个状态转换点。可以写成：

```
原始消息 M0...Mn
        |
        | autoCompact / forceCompact
        v
boundary {
  summary: S,
  keep: K1...Kk,
  recovery: R,
  metadata: ...
}
        |
        v
live context = [S, K1...Kk, messages after boundary]
audit file = [M0...Mn, boundary, messages after boundary]
```

summary 负责保留早期事实和决策，keep 负责保留最近的精确消息，recovery 负责重新附加最近读取文件和技能 SOP。恢复时 summary 需要以一致的中文 framing 变成 user message，让模型知道这不是用户新问题，而是之前对话的摘要。

- 语义正确：模型能继续当前任务，不重复已完成工作；
- 协议正确：assistant tool_use 与 tool_result、thinking signature 等结构不被破坏；
- 审计正确：原始 session 文件仍可解释，边界之后不会产生歧义。

## 4. ContextCompactor：两层压缩

ContextCompactor 有自动管理和强制管理。自动管理根据 contextWindow、maxOutput、当前消息规模和 UsageAnchor 判断是否接近上限；强制管理由模型返回 context too long 时立即触发。

冷启动时没有 provider token 用量，代码会用字符/消息估算；第一次真实 stream 结束后，用 provider 报告的 input/output/cache token 建立 UsageAnchor。下一次判断可以在真实锚点附近估算，而不是完全依赖字符数。压缩后消息数量和内容结构改变，旧锚点失效，因此 Agent 会将 usageAnchor 置空。

这是一个一般化的工程问题：任何基于历史样本的估算器，在数据集发生结构突变后都必须重新校准。面试可以把它类比为缓存失效、分页游标失效或数据库统计信息失效。

ToolResultBudget 在请求发送前创建 apiConv，不直接缩短 live conversation。它按结果年龄、大小、tool 类型和替换状态决定保留完整、替换为摘要、替换为文件引用或移除冗余内容。ContentReplacementState 记录某条结果是否已经被替换，ReplacementRecordsIO 将决策追加到 JSONL 文件。

为什么要记录“已替换”？因为每轮都重新决定可能产生不同文本，破坏 prompt cache 前缀，还可能在恢复时不知道某个结果的原始视图。状态机要保证：

```
FULL -> SUMMARIZED -> REFERENCED
  |       |              |
  +-------+--------------+
        幂等，不重复追加记录
```

实际状态可能更丰富，但阅读时要看 transition 是否允许回退、是否按 toolUseId 定位、文件是否存在、摘要失败时是否保留原文。

当整体上下文超过阈值，ContextCompactor 调用 LlmClient 生成摘要，把旧消息折叠为 summary，并保留最近 tail。摘要生成本身又是一次模型调用，需要处理超时、错误、token 费用和递归超限。不能在摘要请求中再次开启同样的自动压缩循环，否则会形成压缩递归。

RecoveryState 在正常执行过程中记录最近 ReadFile 和 Skill 调用。压缩完成后，把这些记录作为恢复附件加到下一次请求，避免模型只剩一句“曾经读过文件”的摘要。RecoveryAttachmentTest 直接锁定这一语义。

## 5. MemoryScanner、MemoryRecall、MemoryManager

### 5.1 MemoryScanner：从文件发现记忆

MemoryScanner 遍历约定目录，解析 Markdown frontmatter，提取 description、path、age、更新时间等元数据。它要处理目录不存在、文件损坏、frontmatter 不完整、隐藏文件、权限异常和重复路径。扫描结果最好是不可变快照，避免 Agent 正在运行时目录变化导致半个列表。

MemoryAge 把记忆按 startup、session、project、user 等生命周期分类。生命周期不是装饰字段，它影响注入频率和淘汰策略。项目级规则可以长期注入，某次 session 的临时笔记不应污染所有未来会话。

### 5.2 MemoryRecall：相关性不是向量检索

MemoryRecall 从记忆文件读取 JSON/Markdown 记录，根据关键词、路径、标签、时间和选择函数找到 relevant memories。它是轻量本地召回，不等同于 RAG 向量库。优点是可解释、无需外部服务，缺点是语义召回能力有限、内容质量依赖文件格式。

召回结果进入 Prompt 时，必须附上来源和可信度，防止模型把过期记忆当事实。对用户明确写入的规则和模型自动生成的总结应分开标记；自动总结可能有错误，最好需要审批或可删除。

### 5.3 MemoryManager：与 Agent 的注入关系

MemoryManager 负责读写长期 memory、通过模型摘要生成新的记忆、把 memory 注入 ConversationManager。Agent 开始 agentLoop 时调用 `conv.injectLongTermMemory(instructions, memoryContent)`，说明长期记忆不是普通 user message，而是一个特殊的上下文前置阶段。

记忆系统的难点是“何时写入”：每轮都写会产生噪声和成本；只在 session end 写又可能在崩溃时丢失。成熟策略是显式保存、重要事件触发、会话结束总结和周期维护组合。写入前应该去重、限制大小、保留来源、记录时间，并允许用户查看和删除。

HistoryStore 将 prompt 以 JSONL 保存，最大 200 条，连续重复去重，append 时整体重写。它不是 session transcript，不能拿来恢复 Agent 状态，因为它没有 assistant/tool 结果和协议块。把两者混淆会造成用户以为“历史里看到过这句话就能继续任务”。

整体重写简单可靠但不是高性能；200 条上限使成本可接受。load 对坏行和空文本采取跳过策略，说明历史是 best effort。时间戳按 append 时统一生成，历史条目的 timestamp 并不代表原始输入发生时间，文档中不要过度解释它。

FileHistory 使用 Path、Files、MessageDigest、Instant、CopyOnWriteArrayList 等实现文件备份、snapshot 和恢复。文件历史应回答三个问题：备份何时建立、如何识别版本、如何恢复到哪个点。

1. Write/Edit 前读取旧文件并计算 hash；
2. 把旧内容复制到 history 目录，记录 path/hash/timestamp；
3. 执行写入；
4. 写入成功后记录新 hash 和变更摘要；
5. 回滚时按 snapshot 复制回原路径；
6. 如果文件已经被用户修改，恢复前再次比较 hash，避免覆盖新变化。

CopyOnWriteArrayList 适合读多写少的监听器/快照列表，不适合高频写文件历史；源码使用它时要关注是否只是保存订阅者，而不是误以为文件操作本身线程安全。

WorktreeManager、AgentWorktree、PostCreationSetup、WorktreeSessionStore、WorktreeSession、WorktreeChanges、SlugValidator 和 StaleCleanup 共同实现隔离工作区。它不是简单的 `git worktree add` 包装，而是把“Agent 会话”和“Git 目录”绑定。

EnterWorktreeTool 接收 slug 或名称，SlugValidator 限制字符集和长度，WorktreeManager 调用 git 创建目录，PostCreationSetup 复制或初始化必要文件，WorktreeSessionStore 写入 session -> path 映射。失败时可能发生半创建：Git worktree 已存在但 session 未写入，或者目录创建成功而依赖安装失败。清理策略必须可重入。

进入后，Agent 的 workDir 被切换，Read/Edit/Write/Bash 都应以这个目录为基准；PromptBuilder 也应告诉模型当前路径，避免模型写主仓库。Session 文件通常仍需关联主会话 id，否则恢复时不知道在哪个 worktree 中。

ExitWorktreeTool 和 WorktreeChanges 计算 git status/diff，向用户报告新增、修改、删除和未跟踪文件。退出不等于自动合并；如果源码没有实现合并，就不能在文档中承诺会自动回主分支。用户可能想保留 worktree、删除 worktree、复制补丁或继续会话，状态机要区分。

StaleCleanup 通过时间、目录属性和 session 记录清理陈旧 worktree。自动删除是破坏性动作，必须确认路径解析在目标根目录内，避免 session 文件被篡改导致清理任意目录。最好先移动到回收站或生成 dry-run 报告；至少保留日志和可恢复信息。

JSONL loader 应跳过损坏行并恢复此前消息；如果损坏行是 compact boundary，则 findLastCompactBoundary 找不到有效边界，回退到完整 replay。优点是尽量不丢数据，缺点是可能重新发送超长上下文，随后再次触发压缩。

不能删除原消息后再请求摘要，否则会丢失上下文。正确顺序是先生成摘要和 boundary 对象，成功后再追加边界记录/更新内存视图。若摘要成功但写 boundary 失败，要区分内存当前状态和磁盘恢复状态，并给用户警告。

这是典型一致性问题。若 FileHistory 是回滚保障，应该在写文件前强制备份，备份失败则拒绝写入；若策略允许 best effort，则成功结果中必须告诉用户没有建立回滚点。不能静默声称可恢复。

WorktreeSessionStore 仍可能保存旧路径。读取时要检查存在性，报告 stale session，允许重新定位或清理记录。不要把不存在路径当作当前工作目录继续调用模型。

它支持追加、逐行恢复、低依赖和人工检查，适合 Agent 的长会话；缺点是查询和事务能力弱，需要 compact boundary、索引或周期重写。数据库适合复杂查询与并发，但会引入 schema、迁移和部署成本。选择 JSONL 是 CLI 本地优先的工程取舍。

摘要模型可能遗漏条件、混淆已完成和未完成、虚构文件状态。解决方法是保留最近精确消息、保存恢复附件、标记摘要来源、对关键事实采用结构化状态而非只靠自然语言。压缩不是无损编码，而是有损压缩加恢复提示。

FileHistory 是应用级即时回滚，能记录单文件前后版本；Git 是仓库级版本控制，支持 diff、分支和提交。二者互补：模型工具需要在每次副作用前快速备份，长期协作需要 Git。只依赖 Git 会遇到用户未提交改动、二进制、生成文件和 worktree 生命周期问题。

### 什么时候应该使用分布式 session store

JimiCode 是本地 CLI，文件最简单；如果 Agent 运行在多实例服务中，需要跨节点恢复、并发租约、查询、过期和对象存储，就应抽象 SessionStore。核心契约应是 append、load、find boundary、list 和 optimistic version，而不是把 Path API 暴露给业务层。

- 手动在 session JSONL 中插入坏行，观察恢复策略。
- 构造多次 compact boundary，验证只重放最后边界后的消息。
- 让工具结果超过预算，检查原始结果、apiConv 和 replacement records 的差异。
- 修改已读取文件后调用 EditFile，确认 stale 检查。
- 在 worktree 创建失败的不同阶段终止进程，再运行 StaleCleanup，检查是否误删。
- 删除 memory 文件 frontmatter 的必填字段，观察 scanner 是否跳过而不是污染 Prompt。
- 在文件写入过程中终止程序，检查临时文件、旧文件和历史备份是否可解释。

JimiCode 的长期可用性依赖的不是某一个“保存聊天记录”的类，而是多种状态的边界：session 用于恢复，history 用于输入回看，memory 用于跨会话知识，replacement records 用于上下文视图稳定，FileHistory 用于副作用回滚，WorktreeSession 用于代码隔离。源码学习时，凡是看到 `Files.write`、`Files.move`、`ObjectMapper`、`ProcessBuilder`、`git` 和 `Thread`，都要追问它的生命周期、故障恢复和安全范围。

`ConversationManager.java` 是内存消息的最小操作面：添加 user/assistant、thinking、tool use、tool result 和 system reminder。它不负责文件落盘，也不应决定何时压缩；这两个职责分别由 SessionManager 和 ContextCompactor 承担。`Message.java`、`ThinkingBlock.java`、`ToolUseBlock.java`、`ToolResultBlock.java` 是协议数据结构，重点检查字段能否无损重建 provider 请求。

`SessionManager.java` 是可恢复 transcript 的聚合器，负责 sessionsDir、append、load、find boundary、rebuild、list 和 search。它同时调用 Git 获取当前 branch，并把文件大小、消息数、首条用户消息和修改时间变成 SessionInfo。`HistoryStore.java` 的边界更窄，只保存最近 200 条 prompt，连续重复去重并整体重写；不能把它当成 Agent session。`FileHistory.java` 保存文件版本，`WorktreeSessionStore.java` 保存 session 与 worktree 的映射，二者都需要显式 Path 注入以便测试。

`ContextCompactor.java` 是最长的状态变换文件之一，既生成摘要，又选择 keep tail、写 compact boundary、管理 UsageAnchor 和恢复附件。`RecoveryState.java` 是采集证据的轻量状态，记录最近文件读取和 Skill 调用；它本身不负责生成摘要。`toolresult/ContentReplacementLifecycle.java`、`ContentReplacementState.java`、`ReplacementRecordsIO.java` 和 `ToolResultBudget.java` 则负责请求视图的预算状态，不能误认为是消息删除器。

Worktree 子包的分工是：`SlugValidator` 先限制用户输入；`WorktreeManager` 执行 Git 操作并维护信息；`AgentWorktree` 表达一次创建/使用结果；`PostCreationSetup` 做创建后的初始化；`WorktreeChanges` 查询 diff/status；`WorktreeSession` 与 Store 做持久化；`StaleCleanup` 做生命周期回收。调试 Worktree 时从工具进入 Manager，再检查 Store 和 Git 命令输出，不要只看目录是否存在。

## 14. Compact boundary 的一致性证明思路

可以把 session 看成事件序列 E，把压缩结果看成函数 C(E) = (S, K, B)，其中 S 是摘要，K 是保留尾部，B 是边界之后的新事件。恢复函数 R(C(E)) 产生 [summary(S), K, B]。它不可能保证与原始 E 在 token 级完全相同，只能要求对当前任务相关状态近似保持：已完成文件、待完成任务、当前路径、工具限制、用户选择和最近 tool call 不能丢。

因此测试不能只断言恢复消息数量。应该断言：最后一个 boundary 生效；旧 boundary 不重复发送；boundary 后消息顺序不变；boundary 损坏时不会静默丢失原始消息；tool result 的 id 仍能配对；summary framing 清楚表明内容来自早期会话；RecoveryState 附件只附加真实记录，不凭空生成文件事实。

如果摘要模型失败，最安全的策略是保留原始 conv，不写一个空 summary 充当成功。若 boundary 写入失败，应该让 session file 的恢复语义显式标为不完整；如果代码采用 best effort，则至少通过通知或日志告诉用户，不能让 UI 显示“已压缩并可恢复”。

启动时 SessionManager.loadSession 读取 JSONL，ConversationManager.rebuildConversation 生成内存消息。用户输入追加到 session；Agent 每轮可能追加 system reminder、assistant output 和 tool result。工具读取文件时 RecoveryState 记录路径和快照，工具输出过大时 ToolResultBudget 在 apiConv 中建立替换记录。模型返回 context too long 时，Agent 调 ContextCompactor.forceCompact，摘要成功后追加 compact boundary，并清空旧 UsageAnchor。

下一轮使用缩小后的 conv；如果程序此时退出，JSONL 中既有旧审计消息，也有 boundary。下次 resume 时，SessionManager 反向找最后 boundary，只恢复 summary、keep tail 和后续消息。若模型继续修改文件，FileHistory 在写前建立新 snapshot，WorktreeChanges 在退出时报告 diff。这个流程横跨内存、磁盘、模型和 Git，任何一步都可能在重启后只完成一半，因此每个状态都要有 version、时间或边界标记。

长期记忆最大的风险不是读取失败，而是错误内容永久注入。模型可能在一次任务中把临时路径、猜测的 API、用户未确认的偏好写入 memory。MemoryManager 应区分用户显式规则、项目事实、模型摘要和临时 session note，使用不同目录或 frontmatter 类型；MemoryRecall 返回时带 source、updatedAt 和 scope，让 PromptBuilder 能提醒模型“这是记忆，不是当前文件事实”。

MemoryScanner 解析 frontmatter 时不能执行其中的脚本或自动加载任意资源。Skill/Memory 文件可能包含 prompt injection，注入系统提示前应做大小限制、来源标记和敏感信息检查。用户需要拥有查看、删除、禁用和清空记忆的入口，否则长时间使用会出现难以解释的行为漂移。

问：压缩后为什么要清空 token anchor？答：anchor 是旧消息序列上的真实 usage 参照，压缩改变了消息集合和前缀，继续使用会把新上下文误判为旧上下文的 token 分布。

问：Session JSONL 为什么不直接覆盖为压缩后的消息？答：追加边界保留审计和故障回退；覆盖会丢失原始证据，摘要失败或摘要错误时无法诊断。代价是恢复算法必须识别 boundary。

问：FileHistory 能否替代 Git？答：不能。FileHistory 面向工具级即时回滚，Git 面向仓库级协作、diff、分支和提交；Worktree 还需要 Git 的隔离语义。

问：如何处理两个进程同时写 session？答：使用文件锁、append-only 原子写、临时文件/rename 或引入带版本的 Store；仅靠 ArrayList 和 `Files.write` 不能保证跨进程一致性。

问：摘要里是否应该保留全部工具输出？答：不应。应保留对未完成任务有用的事实、路径、版本、错误和决策；完整原文放审计/文件系统，最近关键结果通过 RecoveryState 或引用恢复。

学习者应该能画出“原始 session → boundary → resume”的消息序列，解释 Layer 1 工具结果替换与 Layer 2 消息压缩的区别，指出 HistoryStore 为什么不能恢复任务，说明 FileHistory 与 WorktreeChanges 的职责差异，并能在会话最后一行损坏、摘要失败、文件被外部修改、worktree 被删除四种故障下给出不会丢数据的处理方案。

### 工程补充来源：`docs/deep-dive/04-依赖库Maven构建与面试原理.md`

JimiCode 的 pom.xml 采用单模块 Maven，Java release 21，产物最终名称为 jimicode，并通过 Shade 插件把依赖打进一个可执行 jar。依赖列表并不是“装饰”，每个库都对应源码中的一个边界：

| 依赖 | 版本 | 在源码中的作用 | 学习重点 |
|---|---|---|---|
| tui4j | 0.3.3 | Bubble Tea 风格终端程序、Model、Message、Style | 事件循环与 UI 线程 |
| Mordant | 3.0.2 | 终端颜色、布局与 Markdown 渲染 | ANSI 输出与终端兼容 |
| mordant-markdown | 3.0.2 | Markdown 渲染 | 代码块、列表、流式文本 |
| MCP SDK | 1.1.3 | stdio/HTTP MCP client、schema、工具调用 | 外部进程与协议生命周期 |
| slf4j-nop | 2.0.16 | runtime 日志绑定 | 为什么没有默认日志输出 |
| anthropic-java | 2.34.0 | Anthropic Messages SDK 与 streaming | thinking/tool/usage 映射 |
| openai-java | 4.37.0 | OpenAI Responses SDK 与 streaming | response item、reasoning、工具 |
| SnakeYAML | 2.2 | YAML 配置和 Skill/Agent 元数据 | 反序列化安全 |
| jackson-databind | 2.21.3 | JSON、JSONL、schema、持久化 | 版本、树模型与容错 |
| jackson-datatype-jsr310 | 2.21.3 | Instant/Java time 序列化 | 时间字段兼容 |
| JUnit Jupiter | 5.11.4 | 单元测试 | 临时目录、参数、断言 |
| Maven Enforcer | 3.5.0 | Java/Maven 版本门禁 | reproducible build |
| Compiler plugin | 3.14.0 | release 21、参数名、编码 | 编译契约 |
| Surefire | 3.5.3 | 测试执行 | module path、UTF-8 |
| Shade | 3.6.0 | fat jar、Main-Class、SPI 资源 | 服务发现与签名过滤 |

源码使用虚拟线程、record、模式匹配 switch、List.getLast 等 Java 21 能力。maven.compiler.release=21 让编译器同时约束 API 和字节码目标，不能只在本机装 JDK 21 却用更高版本 API。Enforcer 的 requireJavaVersion [21,) 与 requireMavenVersion [3.9,) 在构建早期失败，避免生成“在开发机能跑、在部署机才失败”的 jar。

虚拟线程适合 Agent 的阻塞式网络、ProcessBuilder、BlockingQueue poll，但不自动解决资源上限；每个工具调用仍需超时，外部进程仍需销毁，队列仍需背压。Java 21 的 pattern switch 让 StreamEvent 解析清晰，但如果项目要回移 Java 17，需要重写这些语法并替换 List.getLast/getFirst 等 API。

tui4j 提供 bubbletea 风格的 Model-Update-View。JimiCodeModel 不是被动渲染器，而是整个交互式应用状态机。Program.run 持有事件循环，用户键盘消息和异步 AgentEvent 都会进入 update。模型状态变更应集中在 UI 线程，后台 Agent 只能通过消息/队列投递。

tui4j 的 Command 通常代表异步动作。JimiCode 的启动、Agent 运行、后台 mailbox 轮询、文件读取和用户确认都可能产生 Command。面试时可比较 Swing EDT、JavaFX Application Thread、Redux reducer 和 Bubble Tea：共同点是单向事件流，区别是 tui4j 更接近函数式 update。

Mordant 与 mordant-markdown 负责 ANSI style、颜色和 Markdown。MarkdownRenderer 可能还有轻量正则处理，用于流式文本中未闭合代码块、工具输出和思考内容。终端渲染不能假设 stdout 是真实终端；重定向到文件、Windows Terminal、SSH 和窄窗口都会影响换行、宽字符和颜色。Styles 集中定义样式，避免业务代码到处写 ANSI escape。

AnthropicClient 使用 AnthropicOkHttpClient、Messages API 和 StreamResponse。它需要将内部 Message 结构映射到 Anthropic 的 content blocks，包括 text、thinking、tool_use、tool_result；把 SDK 的增量事件统一成 StreamEvent.TextDelta、ThinkingDelta、ToolCallStart/Delta/Complete、StreamEnd 和 Error。

难点一是工具参数是增量 JSON。ToolCallDelta 到达时不能立即当成完整 Map，必须累计 JSON 字符串，ToolCallComplete 时解析成 Map。难点二是 thinking signature：后续请求可能需要完整 signature，截断、压缩和恢复时不能只保存可见文本。难点三是 usage 字段版本差异，AnthropicClientContextWindowTest 和 UsageParsingTest 说明用量解析需要对缺失字段、cache read、cache creation 和不同 response 形态容错。

Anthropic 的 prompt cache 让稳定前缀有实际成本收益，因此 ToolResultBudget 记录替换决策并保持 prefix byte-stable。新增 system reminder 时应考虑插入位置，不能每轮在缓存前缀开头加入随机内容。

OpenAiClient 使用 OpenAIOkHttpClient 与 Responses API。Responses API 的输出不是只有一个 message 字符串，可能包含 reasoning item、output text、function call、function call output。内部 StreamEvent 抽象将这些差异压平，让 Agent 只处理统一事件。

Reasoning 与 Anthropic thinking 不完全等价：字段、签名、可见性和缓存语义不同。Adapter 应保证 conversation 中的内部块能被同一 provider 正确重建，而不是把 provider A 的块原样发送给 provider B。ModelResolver 根据 protocol/provider/model 选择客户端；配置切换模型时，协议和工具 schema 也可能变化。

OpenAI 错误分类需要映射认证、限流、上下文超长、服务端错误和网络断开。不能只根据 HTTP status 判断，因为兼容服务可能返回 200 + error JSON，或者 429 的 body 结构不同。OpenAiCompatClient 因而使用 Java HttpClient、BufferedReader、ObjectMapper 和 JsonNode 自己解析 SSE/JSON，灵活但要自行处理连接关闭、UTF-8、data:[DONE]、空行和 JSON 片段。

## 6. OpenAiCompatClient：为什么要自己写兼容适配

兼容端点的优点是可以接入 DeepSeek、Qwen、Kimi、OpenRouter 或本地服务；缺点是“兼容”通常只覆盖请求路径，不保证 tool call、reasoning、usage、stream event 完全一致。OpenAiCompatClient 用 ObjectMapper tree model 处理未知字段，适合多供应商，但应有 capability 配置：

- 是否支持 streaming；
- 是否支持 tools；
- 是否支持 reasoning；
- 是否支持 system role；
- tool arguments 是 JSON string 还是 object；
- usage 是否在 stream 末尾；
- stop reason 的取值；
- context window 和 max output。

SSE 解析器必须按事件边界而不是按 TCP read 边界解析；一条 JSON 可能被分成多个 read，也可能一个 read 包含多个事件。正确做法是 BufferedReader 按行读取，以空行结束 event，累积 data 行，再交给 Jackson。网络流异常时要把线程中断、连接关闭和解析错误分别映射。

Jackson Databind 在 AppConfig、HistoryStore、SessionManager、TaskList、FileMailBox、SharedTaskStore、WorktreeSessionStore、MemoryManager 和 ToolSearchTool 中出现。Tree model JsonNode 适合解析 provider 未知字段、JSONL 坏行和动态工具参数；POJO 反序列化适合稳定的 config/record。

JSONL 的常见模式是逐行 readTree，坏行 catch Exception 后跳过。它提高了可恢复性，但如果所有异常都 ignored，用户可能不知道数据损坏。生产建议记录文件路径、行号和一次性警告。写文件时应考虑 UTF-8、临时文件、flush、原子 move 和并发 writer。

Jackson 版本要统一。pom 使用属性 jackson.version 同时管理 databind 与 jsr310，但 transitive jackson-core、annotations 也可能来自其他依赖；用 dependency:tree 检查是否存在版本冲突。Shade fat jar 还要保留 META-INF/services，否则 MCP、ModelProvider 或其他 SPI 可能在打包后找不到实现。

SnakeYAML 2.2 负责 app YAML、hooks、skills、subagents。LoaderOptions 和 Constructor 的配置决定是否允许任意类型、未知属性和递归深度。安全原则是只反序列化到明确的 Java 类型，不启用任意类实例化；对文件大小、别名数量和嵌套深度做限制，防止 YAML bomb。

SkillCatalog、AgentLoader 和 ConfigLoader 都把 YAML 内容变成运行时行为。Skill 的 description、allowed tools、prompt 和 script 路径本质上都可能影响 Agent；来自项目目录的 SKILL.md 不是天然可信，应该检查路径、命令、网络和权限。面试可以问“YAML 配置为什么也需要安全审查”：因为配置不只是静态参数，它会决定执行什么进程、访问哪些文件和调用哪个模型。

MCP SDK 提供 McpClient/McpSyncClient、StdioClientTransport、HttpClientStreamableHttpTransport、ServerParameters、McpSchema。JimiCode 通过 McpManager 将外部服务暴露为内部 Tool。MCP 连接使用同步 client，但外层 Agent 在虚拟线程中运行，需注意同步调用仍可能无限阻塞；应设置 request timeout 和 shutdown hook。

MCP schema 与内部 schema 的转换要保留 required、properties、description、enum 和 nested object。工具返回可能是 text、image、resource 或 structured content，ToolResult 不能只取一个字符串。若外部服务断开，已有工具是否从 Registry 移除、下一轮是否重新连接、错误如何向模型解释，都属于生命周期设计。

项目 runtime 使用 slf4j-nop，意味着 SLF4J API 调用不会输出日志。对一个交互式 CLI 来说，避免库日志污染终端有价值，但也会让网络、MCP、Hook 失败难以诊断。正式部署可替换为 logback-classic 或 slf4j-simple，并把日志写到文件而不是 stdout。

日志中不能打印 API key、完整工具参数、任意文件内容和用户 prompt。建议使用 session id、tool call id、provider、latency、status、token usage、error category 等结构化字段。JimiCode 当前部分类使用 java.util.logging、部分依赖 SDK 日志，统一日志门面可以改善排查。

测试目录的 14 个测试文件覆盖压缩、恢复、配置、provider key/context window、Anthropic usage、权限模式、session、mailbox、tool search、tool result budget。它们体现了本项目的测试优先级：不是追求每个 getter 的覆盖率，而是验证状态转换和边界。

`@TempDir` 用于隔离文件系统；测试构造显式 Path，避免写入真实 user.home。StubSummaryClient 让压缩测试不依赖真实模型。UsageParsingTest 使用 JsonNode 验证外部 response 的多形态解析。FileMailBoxTest 和 ToolResultBudgetTest 关注并发/幂等/持久化边界。

建议补充的测试包括：SIGINT 清理光标、Bash 超时销毁、MCP 连接失败、Windows path、symlink escape、Hook regex 非法、队列满、Agent 线程中断、重复 tool call id、session boundary 多次压缩、Jackson 版本冲突和 fat jar 的 ServiceLoader。

Enforcer 在 validate 阶段检查 JDK/Maven；Compiler 设置 release 21、UTF-8 和 parameters=true。parameters=true 让反射或工具生成 schema 时可以取得 Java 参数名，尤其对工具方法很重要；关闭后参数可能变成 arg0，模型工具 schema 会变差。

Surefire 设置 useModulePath=false、useManifestOnlyJar=false 和 UTF-8，适合当前非 JPMS 项目。Shade 在 package 阶段生成带依赖的 jimicode jar，ManifestResourceTransformer 写入 Main-Class，ServicesResourceTransformer 保留 SPI。过滤 SF/DSA/RSA 签名文件是必要的，因为依赖重打包后原签名通常失效。

```
mvn -q clean test
mvn -q package
java -jar target/jimicode.jar
jar tf target/jimicode.jar | Select-String "META-INF/services|com/jimicode/JimiCode"
```

构建可复现还要固定插件版本、依赖版本、编码、时区和环境变量。Shade 的 createDependencyReducedPom=false 避免修改项目 pom，但会让依赖树和发布行为与普通 jar 不同；学习时要区分“源码依赖”和“最终运行时依赖”。

### Maven dependencyManagement 与 dependencies 的区别

dependencies 表示当前模块实际依赖；dependencyManagement 只统一版本和默认配置，不自动引入依赖。JimiCode 是单模块，直接在 dependencies 写版本较直观；多模块 AgentScope 更需要 BOM/parent 管理。

### fat jar 为什么需要 ServicesResourceTransformer

Java SPI 通过 META-INF/services 文件发现实现。多个依赖各自带这个文件，普通 shade 可能只保留一个；ServicesResourceTransformer 会合并它们。没有它，MCP transport、JDBC driver 或自定义 provider 可能在 IDE 能跑、fat jar 不能跑。

### release 与 source/target 有什么区别

release 同时约束可用 API、语言级别和字节码，避免在高 JDK 编译出低版本无法运行的 API 引用。source/target 只约束语法和字节码，不保证 API 兼容。

### 为什么要把 SDK 事件统一成 StreamEvent

Agent Loop 不应知道 Anthropic 和 OpenAI 的 SDK 类型。统一事件降低业务层分支，便于新增 provider、测试和错误恢复；代价是抽象必须覆盖 thinking/tool/usage/cache/stop reason 等差异，不能只定义 text。

固定版本、检查 dependency:tree、使用 BOM、避免多个 JSON/HTTP 栈随意混用、锁定 transitive 关键版本、对安全公告建立升级窗口。provider SDK 升级要用 adapter 测试验证事件和 usage，不应只看编译通过。

1. 检查 java -version 是否 21+，mvn -version 是否 3.9+。
2. 删除 target 后 clean test，排除旧 class。
3. 用 dependency:tree 检查 Jackson、OkHttp、SLF4J、MCP 冲突。
4. 若 TUI 报类找不到，检查 tui4j/Mordant 版本和 shade。
5. 若 fat jar 找不到 provider，检查 ServiceLoader 资源是否被合并。
6. 若工具参数名为 arg0，检查 compiler parameters=true。
7. 若 YAML 能读但运行行为错误，检查语义默认值和 environment key。
8. 若模型流卡住，检查 HttpClient/SSE timeout、线程 dump、进程是否仍活跃。
9. 若测试在 Windows 失败，检查 shell、路径分隔符、PosixFilePermission 和 SIGINT 差异。

`tui4j` 的核心价值是把终端输入、窗口尺寸、异步命令和退出事件组织成 Model/Update/View；它不是普通的 console print 工具。`mordant` 和 `mordant-markdown` 负责视觉层，Styles/MarkdownRenderer 通过它们把 AgentEvent 变成可读的终端内容。三者版本必须一起验证，因为 ANSI style、Markdown parser 和 Bubble Tea compatibility 层可能互相依赖。

`anthropic-java` 和 `openai-java` 是两个强类型 SDK，但强类型不代表内部协议相同。AnthropicClient 要处理 Messages content block、thinking signature、cache token；OpenAiClient 要处理 Responses item、reasoning 和 function call。它们都通过内部 `StreamEvent` 交给 Agent，新增 provider 时应遵循同样的隔离，而不是把 SDK 类型渗透到 ConversationManager。

`mcp` 把远程/本地工具的发现和调用协议带进来，运行时可能启动 stdio 子进程或建立 HTTP 连接，所以它同时是扩展点和资源边界。`slf4j-nop` 刻意不向终端输出库日志，适合 TUI，但生产调试必须配置文件日志或替换 binding。`snakeyaml` 解析配置、Skill 和 SubAgent 元数据；任何能改变命令、路径或 prompt 的 YAML 都应按可执行配置审查。

Jackson 在项目中承担多种语义：POJO 配置、JsonNode provider response、JSONL session、task/mailbox、worktree session、memory 和 tool schema。不要把所有 JSON 都用同一种容错策略：配置错误应尽早失败，历史坏行可以跳过，tool response 缺字段应分类，session boundary 损坏应回退。`jackson-datatype-jsr310` 让 Instant/时间类型有稳定表示，但仍要明确时区和 epoch 语义。

| 源码区域 | 直接库 | 需要重点验证的边界 |
|---|---|---|
| `JimiCode.java`、`JimiCodeModel.java`、`Styles.java`、dialog | tui4j、Mordant | UI 单线程、ANSI、退出清理、窗口尺寸 |
| `AnthropicClient.java` | anthropic-java、Jackson | 流事件、thinking、usage、上下文窗口 |
| `OpenAiClient.java` | openai-java、Jackson | Responses item、reasoning、工具调用 |
| `OpenAiCompatClient.java` | JDK HttpClient、Jackson | SSE 边界、兼容协议、超时、错误 body |
| `ConfigLoader.java`、`SkillCatalog.java`、`AgentLoader.java` | SnakeYAML | 类型白名单、未知字段、路径和脚本 |
| `SessionManager.java`、`HistoryStore.java`、`TaskList.java` | Jackson | JSONL 追加、坏行、版本、原子写 |
| `McpManager.java` | MCP SDK、SLF4J | transport 生命周期、schema、连接关闭 |
| `FileHistory.java`、Worktree 包 | JDK NIO、ProcessBuilder | Git 状态、备份、路径安全、平台差异 |
| `src/test/java` | JUnit Jupiter | 临时目录、fake model、协议和状态机 |

升级 provider SDK 不能只修改版本号。先读取 release notes，确认 streaming event、usage、tool schema 和异常类型是否改变；再运行 adapter 单元测试和 JSON fixture 测试；最后用真实 provider 做最小 smoke test。升级 Jackson 要检查 SnakeYAML/SDK 的 transitive 版本、JsonNode 行为、Java time module 和 Shade 后的重复类。升级 tui4j 要验证 KeyPressMessage、UpdateResult、Program.run、Style API 和 Windows terminal。

升级 MCP SDK 要检查 stdio transport 是否仍由用户配置的 command/args 启动、同步 client 的 close 是否阻塞、工具结果是否新增 image/resource block，以及 server capability negotiation 是否变化。升级 Maven Shade 要检查 Main-Class、ServiceLoader、签名过滤和最终 jar 文件大小。任何依赖升级都应生成 `dependency:tree` 前后差异，避免“编译通过但运行时 classpath 变了”。

## 18. 运行时 classpath 与 fat jar

IDE 运行时 classpath 是多个 target/classes 和 ~/.m2 jar 的集合，ServiceLoader 能看到各个 jar 的 META-INF/services。Shade 后所有内容合并到一个 jar，服务文件、资源路径、签名、重复 package 和反射配置都可能改变。因而测试不能只在 IDE 里启动；至少要运行 `mvn clean package` 后用 `java -jar` 启动，并测试 provider、MCP、YAML、Markdown 和 TUI 入口。

如果 fat jar 启动报 NoClassDefFoundError，先看依赖是否是 runtime scope、shade 是否包含；若报服务实现找不到，检查 ServicesResourceTransformer；若 Jackson 报模块或重复类，检查 dependency tree 和 shade jar；若终端显示异常，检查编码、ANSI 和终端宽度；若模型流卡住，检查不是编译问题，而是 HTTP timeout、SSE 解析或未关闭的 process。

`ContextCompactorTest` 验证摘要调用和消息折叠，`RecoveryAttachmentTest` 验证压缩后恢复文件/技能证据；`ConfigLoaderValidationTest` 验证错误配置，`ProviderConfigApiKeyTest` 验证密钥优先级，`ProviderConfigContextWindowTest` 验证上下文/输出默认值；`AnthropicClientContextWindowTest` 验证模型上下文策略，`UsageParsingTest` 验证 provider usage 归一化。

`PermissionModeTest` 锁定模式字符串到 Decision 的映射；`SessionManagerTest` 锁定 JSONL、boundary 和重建；`FileMailBoxTest` 锁定文件通信的追加/读取；`ToolSearchTest` 锁定 deferred schema 的选择语法；`ContentReplacementStateTest` 与 `ReplacementRecordsIOTest` 锁定替换状态和落盘；`ToolResultBudgetTest` 锁定预算算法。读测试时要问：它在防哪种回归？如果删掉断言，哪条链路会悄悄变坏？

问：为什么单模块也要 Enforcer？答：单模块仍有开发者 JDK、Maven 和 CI 环境差异；门禁在编译前失败，反馈更早且结果更一致。

问：为什么 compiler parameters=true 会影响 Agent？答：工具 schema 需要 Java 方法参数名；没有它，反射可能只能看到 arg0/arg1，模型难以正确构造参数。

问：为什么不把所有 JSONL 换成数据库？答：本地 CLI 优先考虑零部署、追加、可读和损坏隔离；当并发、多节点查询和事务需求出现，再抽象 Store，而不是先引入重基础设施。

问：为什么 NOP logger 有利有弊？答：它不污染 TUI 输出，但会丢失 SDK/transport/MCP 的诊断；生产应将结构化日志写到文件，并对 prompt/key/tool 输出脱敏。

问：为什么兼容 OpenAI 的服务仍需独立 adapter？答：路径和基本 JSON 兼容不等于 streaming event、tools、reasoning、usage、context window 和错误码兼容；adapter 是能力协商和差异隔离的位置。

读者应该能从 pom.xml 反推出运行时架构，说明每个直接依赖对应哪些源码文件和资源边界；能解释 `mvn clean test`、`mvn package`、`java -jar` 分别验证什么；能定位 SPI 丢失、参数名变 arg0、SSE 卡住、YAML 语义错误、Jackson 冲突和终端日志污染等常见问题；还能说出升级任一 provider SDK 时需要补哪些 adapter 和 fixture 测试。

JimiCode 的依赖选择体现了一个本地优先 Agent 的工程取舍：tui4j/Mordant 解决交互，provider SDK 和 HttpClient 解决模型，MCP 扩展工具，SnakeYAML/Jackson 解决可配置和可恢复状态，JUnit 锁定关键状态机，Maven Enforcer/Compiler/Surefire/Shade 把运行环境和交付物固定下来。真正的面试亮点不是背依赖名称，而是能说清每个库在调用链中的位置、它引入了什么线程/资源/协议边界，以及失败时如何被上层接管。

### 工程补充来源：`docs/deep-dive/05-SubAgent团队Skill任务与TUI.md`

JimiCode 的 Agent Loop 解决的是单个 Agent 如何思考和调用工具；但真实 coding 任务经常需要拆分研究、并行执行、等待用户、跟踪待办、加载 SOP、在终端展示多个进度。这些能力分别位于 subagent、teams、skill、task 和 tui 包，却共享同一套并发与状态问题：任务如何创建、如何完成、如何通知父 Agent、如何让 UI 显示、如何恢复、如何限制工具。

JimiCodeModel
  |
  +--> CommandRegistry / TaskTools / TeamTools
  +--> AgentTool
          |
          +--> SubAgentTaskManager
          +--> child Agent
          +--> ToolFilter
          +--> AgentEvent / SubAgentProgress
  |
  +--> SkillCatalog / SkillExecutor
  +--> TeamManager / FileMailBox / SharedTaskStore
  +--> ChatMessage / Dialog / Styles

不要把 SubAgent 当成“开一个线程”或 Team 当成“多个 SubAgent 的数组”。它们都有身份、工具边界、通知、工作目录、状态和终止语义。

## 2. SubAgentSpec 与 AgentLoader

SubAgentSpec 是子 Agent 配置的领域对象，通常描述 name、description、prompt、tools、model/provider、max iterations、permission 或 mode。AgentLoader 从约定目录读取 YAML/Markdown agent 定义，把文件系统中的声明转换成 SubAgentSpec。它需要处理目录不存在、坏 YAML、重复名称、缺少必填字段、工具名不存在和路径越界。

AgentLoader 的安全问题与 ConfigLoader 类似，但风险更高：SubAgent spec 直接决定模型提示和可用工具。一个配置文件可以把 Bash、WriteFile、EnterWorktree 暴露给子 Agent；因此加载阶段至少要做名称规范化、工具白名单和 inherited permission 检查。父 Agent 的 tool filter 不能只在 Prompt 中描述，必须在 ToolRegistry/schema 和实际执行处同时生效。

AgentTool 是父 Agent 看到的工具。它把 task、prompt、subagent type、run mode 等参数传给 SubAgentTaskManager/child Agent，并把子 Agent 的进度和最终结果转成 ToolResult。父模型应该看到清晰的状态：已启动、正在运行、等待确认、已完成、失败、取消，而不是只等待一个长时间阻塞字符串。

- force sync：父 Agent 等待子 Agent 完成后再继续；
- background：立即返回 task id，后台运行，通过 notificationFn/TaskTools 查询。

同步模式适合需要子 Agent 结果才能决定下一步的研究任务，但会增加父 Agent 延迟；后台模式适合并行扫描、测试或文档生成，需要可靠的任务仓库和通知。SubAgentProgress 用于 UI/父 Agent 的进度快照，不应该把所有子 Agent transcript 塞到父模型上下文。

子 Agent 可以有独立 ConversationManager、LlmClient、ToolRegistry 和 sessionId。是否共享 FileHistory、Memory、Permission 和 Worktree 要明确：共享 workdir 可能产生并发写冲突；共享 permission 可能让子 Agent继承过高权限；共享 memory 可能把临时结论污染长期记忆。AgentTool 中 clone replacement state、继承工具过滤和配置传递的代码是关键证据。

SubAgentTaskManager 维护 task id、状态、队列和通知。它使用 BlockingQueue、AtomicInteger 等并发结构，重点检查状态读写是否原子、task id 是否稳定、完成结果是否只消费一次、后台异常是否被父 Agent 看到。若父 Agent被中断，子 Agent 是取消、继续后台还是转 orphan，应由明确策略决定，不能依赖线程默认行为。

ToolFilter 根据 ToolRegistry 和 Set<String> 形成 allow/deny 视图。它可能用于 SubAgent、Skill、Team member 和 Plan mode。正确做法是同时过滤模型 schema 和执行入口：

visibleTools = registry.schemas().filter(filter)
execute(toolCall) = registry.find(tool).thenCheck(filter)

只过滤 schema 会留下“隐藏工具调用”漏洞，只过滤执行会让模型反复调用永远被拒绝的工具。过滤名称时还要注意工具别名、MCP 前缀、group 和 deferred name；一个工具在 schema 中叫 mcp_x，而执行层用原始名，可能出现错配。

TeamManager 保存 TeamMode、Team、Member 和父 Agent/子 Agent 的关系。TeamMode 可能表示单一协调者、共享任务、并行成员等。TeamCreate/TeamDelete/SendMessage tools 把管理操作暴露给模型，但真正状态应由 TeamManager 统一维护。

成员信息要有稳定 name/id、role、status、current task、last activity 和 mailbox。AgentNameRegistry 负责给成员生成或校验可读名字，避免两个成员在 UI 和消息中混淆。TeammateProgress 保存最近工具活动和状态摘要，不能把成员完整输出实时拼到主 UI 的所有区域，否则会造成终端重绘和内存膨胀。

TeamManager 的关键并发问题是共享 Map 的一致性：创建成员、发送消息、删除成员、更新任务可能同时发生。LinkedHashMap 保证稳定展示顺序，但不提供并发保护；如果所有变更都来自 TUI 单线程则安全边界清晰，如果后台线程也修改就要通过消息回到 UI 或使用锁。

## 6. FileMailBox 与 SharedTaskStore

FileMailBox 使用 JSONL/文件目录作为本地通信介质。它的优势是跨进程、可检查、无需数据库；代价是文件锁、部分写入、重复读取、顺序、清理和跨平台 rename。MailMessage 至少需要 id、from、to、body、timestamp、read/ack 状态。读取后是否删除消息决定了 crash recovery：先标记再删除比读后立即删除更可靠。

MutationFn 之类的原子 mutation 设计，通常是读取、修改、临时写入、原子替换的循环。如果多个进程同时写，必须在冲突时重读并重试；随机 sleep 不是一致性协议，只能降低碰撞概率。

SharedTaskStore 类似 mailbox，但保存团队共享任务、状态和结果。任务状态应有合法迁移，例如 pending → in_progress → completed/failed/cancelled；重复 update 要幂等，完成任务不能被旧消息覆盖。AtomicInteger 只能解决本进程计数，不能解决跨进程 task 文件冲突。

## 7. TeammateRunner、SpawnDispatcher 与 tmux

TeammateRunner 消费成员 AgentEvent 队列，输出 WaitResult 或进度；它要处理 30 秒 poll、队列结束、工具活动、错误和取消。SpawnDispatcher 把 SpawnConfig 转成实际创建策略，ITermBackend 抽象终端后端，TmuxBackend 使用 tmux 进程/窗口/会话来承载成员。

Tmux 是外部系统，不应假设命令一定存在。启动失败要返回可读错误，tmux session 名称要做 slug 校验，命令参数不能拼接到 shell 字符串造成注入；清理要区分本次创建的 session 和用户原有 session。TimeUnit timeout 和进程退出码需要纳入状态。

如果没有 tmux，TeamManager 仍可以使用本地 Agent 线程或文件 mailbox；ITermBackend 的价值是让协作编排和终端呈现解耦。面试可问：为什么不直接把成员输出 print 到 stdout？因为 TUI、多个成员、窗口重绘、输出归属和取消都要求结构化事件。

## 8. SkillCatalog、SkillExecutor 与 SkillHost

SkillCatalog 扫描技能目录，解析 SkillMeta 和 Skill。SkillMeta 一般包括 name、description、path、allowed tools、arguments、resources；Skill 则包含 prompt/SOP 正文和引用。扫描时要按路径排序、过滤不可读文件、限制 Markdown 大小、处理 frontmatter 错误和重复 name。

SkillExecutor 把选中的 Skill 转成工具过滤器、Prompt sections、SkillHost 或 Fork host。Skill 不是一次性 prompt，而可能要求按 SOP 顺序调用工具、读取 references、运行脚本、汇报结果。执行器必须为技能建立单独的 scope，结束时释放/恢复工具过滤和 prompt 状态，不能把一个技能的工具限制带到下一轮普通对话。

SkillHost 与 SkillForkHost 代表两种执行：当前 Agent 内执行和 fork/child Agent 执行。fork 更安全地隔离上下文，但需要复制 provider、tool state、permission 和 replacement state。学习时重点看 run、fork、allowedTools、onComplete 和异常处理。

TaskList 是本地任务状态机和 JSON 持久化。Status/Task/UpdateResult 表示状态、任务数据和更新结果；SecureRandom 生成任务 id 的目的通常是避免时间戳碰撞和可预测 id。任务项需要 subject、description、status、owner、blockedBy、created/updated 时间等。

TaskTools 将 TaskList 包装成 TaskCreateTool、TaskGetTool、TaskListTool、TaskUpdateTool。模型使用任务工具时，工具描述必须告诉它何时创建、何时更新、不要重复创建和如何表达 blocked。任务状态不等于 Agent 状态：Agent 可以继续运行而 task blocked，也可以 task completed 但 Agent 尚未给最终答复。

TaskList 写 JSON 时要考虑两个模型并发更新：当前 UI Agent 和后台 SubAgent 都可能修改任务。至少做读-改-写版本检查；如果项目当前是单进程模型，则文档要明确这是边界，不要声称支持跨进程并发。TaskTools 返回 LinkedHashMap/Joiner 形成稳定文本，注意输出同样受工具结果预算限制。

## 10. ChatMessage、AppState 与对话框

ChatMessage 把内部 AgentEvent 映射成 UI 结构，包括 ToolBlockInfo 和 SubAgentBlockState。它不应保存原始 BlockingQueue 或 CompletableFuture，而应保存可渲染快照：角色、文本片段、thinking 是否展开、tool name、status、result preview、elapsed time 和 error。

AppState 很小但代表 UI 的顶层状态枚举/对象。JimiCodeModel 通常还保存输入框、history index、current assistant message、current tool block、permission pending、ask user pending、plan dialog、session list、team progress 和窗口尺寸。任何后台更新都要通过 Update message 修改这些状态。

AskUserDialog 是一个较大的状态机，包含 Question、Option、选中索引、键盘处理和结果提交。它要处理单选、多选、自由文本、取消、默认值和按键焦点。用户问题可能由 AgentTool 或 AskUserTool 发起；future 完成后工具执行线程继续，UI 只负责收集答案。PlanApprovalDialog 则把 PlanFile 内容/摘要和 approve/reject/edit 等动作变成 DialogResult。

## 11. MarkdownRenderer、Styles、SpinnerVerbs

MarkdownRenderer 处理模型流式文本时的代码块、标题、列表和换行。流式输入可能在中间截断反引号或 UTF-16 surrogate，渲染器不能每个 delta 都假设 Markdown 完整；实际 UI 往往先保留 raw buffer，按可渲染边界更新。

Styles 集中定义 title、assistant、user、tool、error、thinking、selected 等颜色和 padding。样式选择是状态映射，不要让业务代码直接依赖 ANSI 颜色。无色终端、窄窗口和颜色不支持时应退化。SpinnerVerbs 提供等待时随机文案，ThreadLocalRandom 适合并发，但随机变化会让 snapshot 测试不稳定，测试应注入 deterministic source 或只验证非空。

用户要求“研究三个模块并总结”。主 Agent 创建 TaskList 任务，TeamCreateTool 建立团队，SpawnDispatcher 产生三个成员。每个成员继承只读工具过滤器，在自己的 Agent loop 中读取文件并写入 mailbox。TeammateRunner 将子事件变成 TeammateProgress，FileMailBox 发送完成消息，JimiCodeModel 通过 MailboxPollMessage 读取并更新 ChatMessage 的 SubAgentBlockState。

主 Agent 的 notificationFn 将“成员 A 已完成”注入 system reminder，模型通过 TeamTools/TaskTools 读取汇总，更新任务状态为 completed，并输出最终答案。如果一个成员失败，TeamTask 标记 failed，父模型可以重新分派，但必须避免重复创建相同任务。若用户 Ctrl-C，TeammateRunner、child Agent、tmux/backend 和 mailbox poll 都需要收到取消；不能只停止主 UI。

SubAgent 失败应保留 task record 和失败原因，不能只返回空字符串；Team message 投递失败应可重试/标记未送达；Skill 解析失败应回退到普通 Agent 而不是加载半份 SOP；TaskList JSON 损坏应尽量跳过坏任务并告警；Dialog future 取消时必须让等待工具结束，否则 Agent 会永久卡在 pending。

TUI 异常要保证 view 不访问 null pending 对象；用户在 PermissionDialog 关闭时应有明确 reject/cancel；窗口 resize 不应改变业务状态；Agent 队列超时要显示错误或结束状态。tmux 进程崩溃时，TeammateRunner 应转为 failed/orphan，而不是让 UI 永远显示 running。

SubAgent 有模型、消息、工具、权限、session、事件和最终语义；线程池任务只是执行代码。把 SubAgent 当 Runnable 会丢失上下文隔离、工具过滤、停止和恢复。

后台成员完成时父进程可能不在消费；持久化允许重启后恢复、重试、审计和去重。内存队列只能传递在线事件，不能承担任务结果的可靠投递。

Skill SOP 可能要求只允许特定工具，减少模型自由度和风险；过滤必须同时作用于 schema 和执行，且结束时恢复原始工具集合。

Agent 在后台线程/虚拟线程运行，直接读取可变字段会产生竞态和半更新快照；事件消息把并发转换成单线程 reducer 的状态变化。

学习者应该能够画出 SubAgent 同步/后台两条链路，解释 AgentTool、TaskManager、ToolFilter 的配合；说明 FileMailBox 如何处理跨进程消息和重复读取；说清 Team 与 SubAgent 的边界；解释 SkillCatalog 到 SkillExecutor 的生命周期；列出 TaskList 状态迁移；指出 AskUserDialog/PlanApprovalDialog 的 Future 如何恢复执行；并能在父 Agent 中断、子 Agent orphan、tmux 不存在、技能解析失败、任务 JSON 损坏五种场景下设计恢复路径。

### 工程补充来源：`docs/deep-dive/06-系统架构逐模块调用链与异常体系.md`

JimiCode 是一个 Java 21/Maven 实现的终端 coding agent。它的输入是命令行参数、环境变量、配置 YAML 和用户键盘；它的输出是终端 UI、文件变更、Git worktree、会话 JSONL、memory 文件和模型请求。它不是 Web API 服务，因此不存在传统 Controller/Service/Mapper 的业务分层；最合适的架构描述是“终端事件循环 + Agent 状态机 + 工具副作用边界 + 本地持久化”。

源码规模为 117 个 Java 文件：103 个生产文件和 14 个测试文件。包级边界如下：

| 包 | 核心文件 | 架构职责 |
|---|---|---|
| com.jimicode | JimiCode | 进程入口 |
| agent | Agent、StreamingExecutor、AgentEvent | Agent Loop、流和工具编排 |
| tui | JimiCodeModel、ChatMessage、Renderer、Styles | UI 状态和事件消费 |
| config | AppConfig、ProviderConfig、ConfigLoader | 配置加载与默认值 |
| conversation | ConversationManager、Message、内容块 | 消息领域模型 |
| llm | LlmClient、Anthropic/OpenAI clients | provider 适配 |
| tool | Tool、Registry、Result、impl | 工具发现和副作用 |
| permission | Checker、Mode、Response | 安全策略 |
| hook | HookEngine | 生命周期 hook |
| compact/toolresult | 压缩、预算、替换状态 | 上下文治理 |
| memory/session/history | 记忆、会话、历史 | 长期与短期持久化 |
| subagent/teams | 子 Agent 和团队 | 协作 |
| skill/task | SOP 技能和任务清单 | 工作流 |
| worktree | Git 隔离 | 代码变更隔离 |

理想的依赖方向是入口依赖编排，编排依赖领域和基础设施，具体工具依赖文件/进程，UI 依赖事件和状态；领域模型不应该反向依赖 UI。当前 JimiCodeModel 是一个大型 coordinator，直接依赖很多包，这是现实架构中的应用层聚合点。

- JimiCode.main 拥有启动生命周期和 Program；
- JimiCodeModel 拥有 UI 状态、当前 Agent、待确认 Future 和对话显示；
- Agent 拥有一次执行的迭代状态、usage anchor、replacement state、recovery state；
- ConversationManager 拥有当前消息列表；
- ToolRegistry 拥有工具和 deferred schema；
- PermissionChecker 拥有模式、规则和当前判断；
- SessionManager/HistoryStore/FileHistory/WorktreeSessionStore 拥有各自文件；
- 子 Agent/Team manager 拥有协作状态；
- 外部模型 client 拥有 SDK/HTTP 请求资源。

如果两个对象都认为自己拥有同一份可变 List、线程或 Future，就会出现竞态和重复关闭。源码阅读时应把 setter 注入的依赖标成 borrowed，把构造器创建的资源标成 owned，再决定谁负责 close。

JimiCode.main 解析 args[0]、JimiCode_CONFIG 或默认路径，调用 ConfigLoader.load。ConfigLoader 使用 SnakeYAML 把 YAML 转成 AppConfig；ProviderConfig 决定 provider、model、endpoint、key、protocol、contextWindow 和 maxOutput；McpServerConfig 和 HookConfig 留给 JimiCodeModel 初始化扩展。

PermissionMode.fromConfig 将文本模式变成 enum/Decision。失败时 main 输出 Configuration error 并退出，不进入 TUI。JimiCodeModel 构造 provider list、MCP list、Hook list 和 PermissionMode；它创建或准备 HistoryStore、SessionManager、MemoryManager、TaskList、ToolRegistry、AgentTool、CommandRegistry 等对象。Program(model) 启动事件循环，setProgram 让 model 可发出退出/异步命令。

启动异常应按阶段分类：配置文件找不到/解析失败、密钥缺失、provider 未知、MCP 连接失败、终端初始化失败。并非所有错误都应该阻止 UI：一个可选 MCP server 失败可以显示 warning 并继续；主 provider 缺失则不能正常运行。源码文档应明确 required 与 optional，而不是笼统写“启动失败”。

键盘输入首先进入 tui4j 的 KeyPressMessage/update。JimiCodeModel 判断是否是输入编辑、历史导航、斜杠命令、Ctrl-C、窗口尺寸或对话框按键。普通文本写入 HistoryStore，连续重复被抑制，然后加入 ConversationManager 的 user message。命令文本交给 CommandRegistry，命令可以改变 session、memory、permission、plan、worktree 或 UI，而不是进入模型。

普通输入要经过 PromptBuilder，构造 system sections：身份、工作目录、时间、Git、工具规则、权限模式、Plan 约束、memory 和 skill 摘要。动态通知可能追加为 system reminder。JimiCodeModel 读取当前 provider 的协议和工具 schema，配置 Agent，调用 Agent.run 返回 BlockingQueue<AgentEvent>，再由 TUI 命令轮询和投递。

这里的一个重要设计是 command 和 user prompt 分离。若所有以斜杠开头的文本都直接送模型，用户无法可靠控制 session/plan；若所有文本都当命令，又会误伤自然语言。CommandRegistry 的识别规则、别名、参数解析和错误输出应单独测试。

Agent.run 使用虚拟线程进入 agentLoop。每轮：

1. 检查 maxIterations 和 interrupt；
2. 注入后台通知；
3. 获取工具 schema 并应用 toolNameFilter；
4. 调 ContextCompactor.manage；
5. 注入 deferred tool names；
6. Plan 模式注入 reminder；
7. ToolResultBudget.apply 创建 API conversation；
8. client.stream；
9. poll StreamEvent；
10. 发 AgentEvent 给 UI；
11. 记录文本、thinking、tool calls、stop reason、usage；
12. 分类处理错误；
13. 若需要工具，交给 StreamingExecutor；
14. 将 ToolResultBlock 写回 ConversationManager；
15. 继续下一轮或结束。

AgentEvent 是 UI 协议，不要把 provider SDK 类型暴露给 JimiCodeModel。StreamEvent 是模型协议归一化，不要把它直接作为持久化格式。ConversationManager 是模型请求的事实状态，apiConv 是一次请求的预算视图，session file 是可恢复/审计视图；三者可能不同，文档必须讲清楚。

LlmClient 统一提供 protocol、stream、setMaxOutputTokens、contextWindow/usage 相关能力。AnthropicClient 走 Messages SDK，OpenAiClient 走 Responses SDK，OpenAiCompatClient 走 JDK HttpClient/SSE/JsonNode。ModelResolver 根据 provider/protocol/model 选择实现。

流式事件需要处理 text delta、thinking delta/completion、tool call start/delta/complete、stream end、error。工具参数通常跨多个 SSE 片段，必须在 adapter 内累积和解析；Agent 只应该看到完整 ToolCallComplete。usage 在不同 provider/SDK 版本中字段不同，adapter 统一为 input/output/cache tokens。

模型异常由 LlmException 分类：AuthenticationException 不应无限重试；RateLimitException 可以延迟重试；ContextTooLongException 触发 forceCompact；NetworkException 可以有限重试；解析错误通常需要停止并保留诊断。OpenAI-compatible 服务不能假设遵循全部 Responses 语义，Capability/配置应说明是否支持 tool、thinking、stream、usage。

ToolRegistry 为模型生成 schemas，PermissionChecker 与 HookEngine 在执行器前拦截，具体 Tool 产生副作用。文件工具还经过 FileStateCache/FileHistory，Worktree 工具经过 Git Manager，MCP 工具经过外部 transport。工具结果回写 ConversationManager，可能经过 ToolResultBudget 视图压缩。

权限是纵深防御而非一个 if：模式给默认策略，规则按工具/路径/参数细化，Hook 可执行配置策略，具体工具再次做输入校验，工作区路径约束保护文件范围，sandbox/worktree 隔离进程或目录。任意一层放宽都可能导致越权，因此子 Agent/MCP/Skill 必须继承或重新计算权限。

SessionManager 保存完整消息和 compact boundary；HistoryStore 保存 prompt history；MemoryManager/Scanner/Recall 维护长期记忆；FileHistory 保存文件副作用版本；TaskList 保存任务；FileMailBox/SharedTaskStore 保存团队状态；WorktreeSessionStore 保存 Git 隔离会话；ReplacementRecordsIO 保存工具结果替换决策。

这些文件可能同时失败，但恢复优先级不同。会话 boundary 和文件备份直接关系到任务恢复，应尽量强一致；prompt history/Hook notification 可以 best effort；memory 写入失败要警告但不应阻止最终回答；team mailbox 失败要转 task 状态而不能静默丢成员结果。不要在一个统一 catch(Exception ignored) 中吞掉所有持久化异常。

## 9. SubAgent/Team/Skill 作为扩展工作流

AgentTool 创建子 Agent，SubAgentTaskManager 维护生命周期，ToolFilter 限制能力，AgentEvent/SubAgentProgress 回传状态；TeamManager、FileMailBox、SharedTaskStore 提供多成员协作；SkillCatalog/Executor 将文件 SOP 转为 prompt/tool scope；TaskList/TaskTools 让模型显式管理进度。

它们共享 notificationFn：后台完成消息转成 system reminder，避免直接修改 ConversationManager 的用户消息。通知要幂等，不能每轮重复注入同一完成消息。父 Agent 中断时要决定 child 是 cancel、continue background 还是 orphan；Session/Task 记录必须支持恢复。

Agent、MCP、Bash、SubAgent、tmux 可能阻塞或异步，JimiCodeModel 的 UI 状态应只在 tui update 中修改。AgentEvent 通过 BlockingQueue 和 Program command 进入 UI。UI 不应直接读后台可变 List，也不应在 view 中调用文件/网络/ProcessBuilder。

ChatMessage 保存渲染快照，ToolBlockInfo/SubAgentBlockState 保存工具和子任务显示；AskUserDialog/PlanApprovalDialog 负责用户决策，CompletableFuture 让执行线程获得结果。用户关闭窗口、Ctrl-C、网络错误、tool timeout 时，所有 pending dialog/future 都要完成或取消，否则任务会泄漏。

没有单一 GlobalExceptionHandler。入口处理 ConfigException；provider adapter 产生 LlmException；Agent 按 context/rate/max tokens 分类；StreamingExecutor 把工具异常转 ToolResult；Permission/Hook 返回控制结果；TUI 把 AgentEvent.ErrorEvent 显示；持久化分 best effort 与强恢复；Worktree/Git 通过结果对象报告。

错误对象应至少有 domain、message、cause、retryable、userActionRequired、correlationId。当前源码的部分路径只用字符串，这是可以作为改进点的架构审查结论。日志要记录完整 cause，但模型和用户只接收安全摘要，避免 API key、源码全文和内部路径泄露。

Agent：IDLE → RUNNING → WAITING_TOOL/WAITING_USER → RUNNING → COMPLETED/FAILED/INTERRUPTED。

Tool permission：UNDECIDED → ALLOWED/DENIED/ASKING → EXECUTING → SUCCEEDED/FAILED/CANCELLED。

Session compaction：NORMAL → NEAR_LIMIT → SUMMARIZING → BOUNDARY_WRITTEN → RECOVERED；摘要失败回到 NORMAL/ERROR，不能进入假成功。

Task：PENDING → IN_PROGRESS → BLOCKED/WAITING → COMPLETED/FAILED/CANCELLED。

Worktree：ABSENT → CREATING → ACTIVE → EXITING → RETAINED/REMOVED/STALE。

对状态机的每个边都要记录触发事件、持久化动作、UI 显示和取消行为。枚举/record 看似很小，但它们决定了跨文件的协议稳定性。

性能瓶颈主要是模型 token、工具输出、文件扫描、MCP 网络和 TUI 重绘。ToolResultBudget/ContextCompactor 降低 token，ToolSearch 延迟 schema，FileStateCache 减少重复读取，Workspace/Glob/Grep 限制遍历，异步 SubAgent 并行工作。优化不能牺牲协议正确性和安全。

可靠性来自：JSONL 追加与坏行隔离、compact boundary、FileHistory、Git worktree、tool call id、任务状态、future timeout、process kill、MCP close 和 shutdown hook。扩展性来自 LlmClient、Tool、ProviderConfig、HookEngine、SkillHost、ITermBackend 等边界，但大而全的 JimiCodeModel 是未来可以拆分的地方。

第一阶段读 JimiCode、AppConfig、ConfigLoader、ProviderConfig、JimiCodeModel 的初始化；第二阶段读 Message/ConversationManager、PromptBuilder、AgentEvent、Agent；第三阶段读 LlmClient、Anthropic/OpenAI/Compat、StreamEvent；第四阶段读 Tool/Registry/impl、StreamingExecutor、Permission/Hook；第五阶段读 ContextCompactor、ToolResultBudget、SessionManager、Memory、FileHistory；第六阶段读 SubAgent/Team/Skill/Task；第七阶段读 TUI dialog/renderer/styles 和全部测试。

每读一个文件记录：公共入口、字段、调用者、被调用者、线程、IO、副作用、异常、测试、扩展点和一个面试问题。短 record/enum 也要说明协议意义；长 coordinator 要画状态图，不要只按行号机械阅读。

为什么这是 Agent Loop 而不是 CRUD？因为状态由模型输出驱动，工具观察反馈形成闭环，结束条件是 stop reason/状态而非一次 HTTP response。

全局异常在哪里？没有单点，在入口、adapter、Agent、executor、permission、UI 和持久化层分域处理。

如何保证模型不能越权？工具 schema、Permission、Hook、PathPolicy/Worktree、FileStateCache、具体工具校验和 sandbox 形成纵深防御。

上下文为什么需要两层治理？工具结果预算控制单项大输出，消息压缩控制整体窗口；前者产生 API 视图，后者改变可恢复 session 状态。

为什么 TUI 和 Agent 解耦？Agent 可能阻塞/异步/多线程，UI 需要单向事件和一致状态；直接共享可变字段会导致竞态。

读者完成本册后，应能从用户输入定位到 JimiCodeModel.update、Agent.run、LlmClient.stream、ToolRegistry、PermissionChecker、具体 Tool、ConversationManager、SessionManager 和 UI；能指出每一步的输入输出和失败处理；能解释所有 30 个左右核心 package 的层次关系；能用源码中的实际类名说明依赖库为何存在；能设计一个新 provider、一个新工具、一个新 permission rule、一个新 skill 和一个新 TUI dialog 的接入位置；能在模型超限、工具超时、用户拒绝、session 损坏、worktree stale、子 Agent orphan 六种故障下画出恢复路径。

### 工程补充来源：`docs/deep-dive/07-构建部署配置调试测试与常见问题.md`

JimiCode 的 pom 明确要求 Java 21 及以上、Maven 3.9 及以上，源码编码 UTF-8。项目是单模块 Maven，主类是 com.jimicode.JimiCode，打包 finalName 为 jimicode，Shade 后生成可执行 fat jar。它不是 Spring Boot，不读取 application.yml，也没有 HTTP server、数据库 migration 或容器编排。

1. java -version 确认主版本 21；
2. mvn -version 确认 Maven 3.9+ 且使用同一 JDK；
3. 检查当前工作目录是不是 jimi-code；
4. 检查配置文件路径、provider key、模型 endpoint；
5. 检查终端支持 ANSI 和 UTF-8；
6. 若使用 BashTool/Hook/MCP/Worktree，再检查 bash、git、tmux 和外部命令。

JimiCode 在 Windows 与 Unix 的差异很明显：BashTool 和 HookEngine 可能依赖 bash；FileHistory 的 PosixFilePermission 在 Windows 行为不同；tmux 通常不存在；SIGINT 注册使用 sun.misc.Signal；Git 参数、路径分隔符和 shell 退出码也不同。文档不能只给 Linux 命令，然后声称跨平台。

main 的配置路径优先级是命令行第一个参数、环境变量 JimiCode_CONFIG、默认路径。注意环境变量名称大小写和项目源码保持一致。ConfigLoader.load 负责读取文本、SnakeYAML 解析、SnakeCasePropertyUtils 映射和配置异常封装。

- providers：provider name、protocol、model、apiKey/apiKeyEnv、baseUrl、contextWindow、maxOutputTokens；
- mcpServers：name、transport、command/args 或 URL、enabled、tools；
- hooks：id、event、condition、action type、command/message、reject；
- permission：mode、rules、allowed/denied paths；
- agents/skills：名称、描述、提示、允许工具、资源路径；
- workspace/session：工作目录、session id、memory 目录。

API key 推荐只放环境变量。配置文件可以有 apiKeyEnv 字段，让 ProviderConfig 读取指定变量；绝不把真实 key 提交到 config.example.yaml、session、memory 或日志。配置 error message 应隐藏 value，只指出字段和缺失原因。

配置可以在三层失败：YAML 语法、Java 属性映射、运行时语义。YAML 语法错误由 SnakeYAML 抛出；属性名错误可能因为未知字段策略而被忽略；provider/permission/model 非法在 ProviderConfig/PermissionMode 解析时失败。

测试时应该建立最小配置、空配置、缺 provider、未知 provider、非法 contextWindow、缺 API key、未知 permission mode、坏 hook regex、空 MCP list、重复 provider name 和错误路径。每个错误都应检查异常类型/消息是否可行动，而不是只断言抛出 Exception。

ConfigLoaderValidationTest、ProviderConfigApiKeyTest、ProviderConfigContextWindowTest 是源码行为的证据。读这些测试要注意临时目录、环境变量不可随意修改的限制，以及它们是否测试了默认值和显式值的优先级。

mvn validate 触发 Enforcer，检查 Java/Maven。mvn compile 运行 Compiler，release=21、UTF-8、parameters=true。mvn test 由 Surefire 执行，useModulePath=false，文件编码 UTF-8。mvn package 在 test 后执行 Shade，写 Main-Class，合并 SPI services，过滤依赖签名。

mvn -q clean test
mvn -q package
java -jar target/jimicode.jar

mvn dependency:tree
mvn help:effective-pom

不要先用 IDE Run 代替 package 验证。IDE 可能自动提供资源、classpath 和 JDK 参数；fat jar 可能缺 ServiceLoader、资源文件或 runtime dependency。每次改变 pom、provider SDK、MCP SDK、Jackson、tui4j 或 shade 配置，都应重新跑 fat jar smoke test。

启动时若提示 Configuration error，先定位是路径、YAML、provider 还是 permission mode。使用明确的配置绝对路径，避免默认 user.home 不同导致加载了另一份文件。检查 ConfigLoader 是否读取了你编辑的文件，环境变量是否覆盖了命令行选择。

启动后终端光标隐藏/恢复由 JimiCode.main 的 ESC 序列和 finally 负责。若程序异常退出后光标不见，可以在终端执行恢复光标命令，随后修复启动阶段异常。Ctrl-C 由延迟虚拟线程重新注册 SIGINT，可能存在框架启动时序问题；调试时观察 signal handler 是否实际安装，Agent 是否收到 handleSigint。

JimiCodeModel 初始化复杂，若启动后 UI 空白但无异常，检查 Program、model.init/command、窗口尺寸消息和 view 的 null 状态。TUI 代码不能在非交互重定向环境中假设 terminal width、ANSI 和 key events 均存在。

先确认 ProviderConfig 的 resolved protocol/model/baseUrl/contextWindow/maxOutput。再确认 ModelResolver 返回的具体 client。Anthropic/OpenAI SDK 客户端创建成功不代表 key、endpoint、model 可用；第一次 stream 才能暴露认证、限流和模型错误。

调试流式问题要区分：连接未建立、连接建立但无事件、SSE 行解析失败、事件无法映射、工具 JSON 未闭合、StreamEnd 缺失、usage 缺失和 Agent poll timeout。Agent 每 30 秒 poll；streamQueue 没有事件会返回 Stream timeout。不要只增加 timeout，先检查 provider 是否真的发送 data、是否需要 Accept text/event-stream、是否有代理缓冲。

OpenAiCompatClient 的常见问题是 data:[DONE]、空行分隔、JSON 跨行、tool arguments 为字符串/对象、兼容服务返回 200 error。使用本地抓包或保存脱敏 SSE fixture，比盯着终端文本更容易定位。

contextWindow 太小会在第一次请求前触发 ContextCompactor.manage，或者由 provider 返回 context too long，Agent 再 forceCompact。要检查 summary client 是否能调用、keep tail 是否保留、RecoveryState 是否有文件记录、compact boundary 是否写入 session、usageAnchor 是否被清空。

max_tokens 是输出耗尽，不等于上下文超限。Agent 首次会提高 max output ceiling；如果仍耗尽，会注入“从中断处继续/拆小工作”的消息，最多 MAX_OUTPUT_RECOVERIES 次。调试时检查 ConversationManager 是否已追加上一次 assistant 文本，避免重试造成重复答案或重复 tool use。

ToolResultBudget 是请求视图压缩，不代表 live conversation 删除。比较 conv.getMessages、applied.apiConv 和 ReplacementRecordsIO 文件，确认替换状态符合预期。若每轮替换文本不同，可能破坏 prompt cache，也可能是 state key 未稳定。

ToolSearchTest 验证 deferred schema 选择；若模型调用未知工具，先检查 deferred names 是否注入、query 是否符合 select 语法、ToolRegistry 是否 active。若工具 schema 正确但执行失败，检查 tool name、arguments、ToolRegistry lookup、ToolCategory 和具体 Tool。

ReadFile/Glob/Grep 调试先测试工作目录、相对路径、空目录、权限错误、二进制、大文件和符号链接。Write/Edit 调试先看 FileStateCache 的 hash/mtime，Edit 的 oldString 匹配数量和 FileHistory 是否成功建立备份。Bash 调试要记录 command 摘要、cwd、timeout、exit code、stdout/stderr 是否截断和进程是否清理，但日志中不能保存 secrets。

Permission 拒绝时，区分默认 mode、规则命中、Hook reject、用户拒绝和路径越界。PermissionModeTest 只验证模式映射，复杂问题要读 PermissionChecker.check、规则加载和参数提取。Plan mode 下写工具被阻止是预期行为，不能把它当作工具 bug。

McpManager 的 server 连接分 stdio 和 HTTP。stdio 调试先单独在 shell 中运行 command/args，确认版本、工作目录和输出协议；npx/外部包必须固定版本，网络下载失败要和 MCP 协议错误分开。HTTP 调试确认 URL、TLS、认证、timeout、redirect 和 server capability。

连接成功但看不到工具，检查 listTools、enable/disable filters、registry registration 和 deferred schema。工具返回空，检查 MCP result content 类型；它可能是 text、structured data、resource 或 error。退出程序时确认 client close 和 stdio 子进程结束，否则重复启动会留下孤儿进程。

## 10. Session、Memory、Worktree 调试

Session 文件通常位于工作区 .JimiCode/session。listSessions 只扫描 jsonl，展示首条 user message、消息数、文件大小和时间。若 resume 后内容重复，检查最后 compact boundary 是否被识别；若全部历史重新发送，可能 boundary 损坏/缺失；若摘要之后丢 tool result，检查 boundary keep 和 rebuild。

MemoryScanner 找不到记忆时，检查目录、frontmatter、文件命名和 age；MemoryRecall 找到过期内容时，检查相关性与生命周期；MemoryManager 写失败要看目录权限和 JSON/Markdown 序列化。不要在调试输出中直接打印完整长期记忆，可能包含敏感数据。

Worktree 调试执行 git status、git worktree list、查看 WorktreeSessionStore 和实际目录。创建失败要检查 slug、当前仓库、未提交改动、目标目录和 post setup；退出异常要区分仅报告 changes、删除 worktree 和真正合并。StaleCleanup 前必须 dry-run/核对绝对路径，避免误删用户目录。

mvn -q -Dtest=SessionManagerTest test

mvn -q -Dtest=SessionManagerTest#testName test

测试不要访问真实 API。Anthropic/OpenAI 测试应使用 stub/fake response，MCP/ProcessBuilder 应使用 fixture 或可控本地进程，文件测试使用 @TempDir，history/session/worktree 不应写用户 home 或真实仓库。

每次修改核心 Agent 都应回归：ContextCompactorTest、RecoveryAttachmentTest、SessionManagerTest、ToolResultBudgetTest、ToolSearchTest、PermissionModeTest、FileMailBoxTest。修改 provider adapter 加 UsageParsingTest、AnthropicClientContextWindowTest；修改 UI/命令时项目测试较少，应补人工 TUI smoke test和事件序列测试。

| 现象 | 优先检查 |
|---|---|
| 配置找不到 | CLI 参数、JimiCode_CONFIG、默认路径 |
| API key missing | ProviderConfig key/env 优先级 |
| context too long | contextWindow、compact、summary、boundary |
| stream timeout | endpoint、SSE、代理、poll、连接关闭 |
| tool not found | Registry、deferred ToolSearch、name filter |
| permission denied | mode、rule、Hook、Plan、path |
| 文件 stale | FileStateCache、外部修改、hash |
| 编辑改多处 | oldString 匹配次数与 replace_all |
| session 重复 | 最后 boundary、rebuild、坏 JSONL |
| MCP 无工具 | transport、listTools、filters、close |
| UI 卡住 | pending Future、Agent queue、update/view |
| Ctrl-C 无效 | signal handler 时序、Program 默认处理 |
| fat jar 缺类 | scope、Shade、dependency tree |
| SPI 找不到 | ServicesResourceTransformer |
| Windows 失败 | bash/tmux、路径、Posix permissions、SIGINT |

模型成本由 input/output/cache tokens 决定；查看 UsageEvent 和 provider usage，而不是只看字符数。工具结果过大时先用 ToolResultBudget，工具 schema 过多时用 ToolSearch。上下文压缩会产生额外摘要调用，要权衡阈值和摘要成本。

CPU/内存问题常见于全盘 Glob/Grep、大文件 read、无限 Bash 输出、完整 ChatMessage 历史和多个 SubAgent transcript。设置每工具输出预算、队列容量、任务并发上限和子 Agent maxIterations。虚拟线程降低线程成本，但不能消除外部资源泄漏。

TUI 性能关注每个 delta 是否触发完整 Markdown 渲染和整屏重绘；工具块和子 Agent 进度应增量更新。随机 spinner 文案不应导致业务状态更新。JimiCodeModel 过大时可把 session controller、permission coordinator、team coordinator 拆出，但需要保持事件协议。

检查配置文件权限、API key 来源、Hook command、MCP command、Bash、路径规范化、symlink、worktree cleanup、memory prompt injection、Skill SOP 和日志脱敏。任何由模型生成的路径/命令都当作不可信输入。任何由项目文件加载的 Skill/Agent 配置都当作可执行策略审查。

高风险动作包括 shell、写文件、删除文件、Git reset/clean、Worktree remove、启动外部进程、MCP stdio 和 Hook command。权限模式要默认收紧，用户批准应有范围和生命周期，拒绝不能被模型通过重命名工具绕过。测试应包含路径穿越、非法 regex、超时进程、巨大输出和恶意 YAML。

发布前执行 clean test/package，检查 jar 内容、Main-Class、SPI、资源、版本、文件大小、无真实 key、配置示例可运行。使用一个只读 provider smoke test、一条文本请求、一条工具调用、一条权限确认、一条上下文压缩、一条 session resume 和一次 worktree 创建/退出。

记录版本：JDK、Maven、git、模型 SDK、MCP SDK、配置 schema。把依赖树和测试报告作为构建产物。发布时不要提交 .JimiCode session、memory、真实 provider 配置、target 或用户文件快照。

如何定位 Agent 卡住？先找最后一个事件：UI 无事件可能是 Agent queue/stream poll，Agent 有 StreamStart 无 Delta 可能是网络/SSE，ToolUse 无 ToolResult 可能是 permission Future/tool process，ToolResult 有但无下一轮可能是 conversation/Agent loop 异常。

为什么使用 JSONL？追加、可读、坏行隔离；为什么需要 boundary？压缩后不能重放原始前缀；为什么需要 FileHistory？工具副作用前回滚；为什么使用 worktree？隔离主仓库和并行 Agent；为什么用 ToolSearch？降低 schema token；为什么用 virtual thread？让阻塞 SDK/进程等待更廉价，但仍要 timeout。

每次问题记录：发生时间、commit、JDK/Maven、配置 hash、provider/model/protocol、session id、tool call id、最后 AgentEvent、异常 domain、是否 retry、工作目录、文件 hash、是否有 boundary、是否有 pending Future、相关日志路径和恢复动作。不要把 API key、完整 prompt、完整源码和用户隐私直接贴进公开 issue。

读者能够在没有猜测的前提下完成：从空目录创建最小配置；启动 TUI；发送文本；观察 StreamEvent/AgentEvent；批准一个工具；拒绝一个工具；触发压缩；恢复 session；检查工具结果替换；执行测试；打出 fat jar；解释 Windows/Linux 差异；定位五种常见模型/工具/持久化异常。若还需要“重新试试”，说明应回到对应源码分册继续追踪。

### 工程补充来源：`docs/deep-dive/08-测试源码逐文件精读与行为契约.md`

JimiCode 的测试数量不多，但集中在最容易回归的状态机和协议边界。14 个测试文件不是把每个 getter 都覆盖，而是锁定压缩、配置、模型 usage、权限、会话、邮箱、延迟工具和工具结果预算。测试源码应与生产源码成对阅读：测试给出行为契约，生产类给出实现细节，二者合起来才能判断某个 catch、默认值或状态字段是否可以修改。

- compact/ContextCompactorTest.java
- compact/RecoveryAttachmentTest.java
- config/ConfigLoaderValidationTest.java
- config/ProviderConfigApiKeyTest.java
- config/ProviderConfigContextWindowTest.java
- llm/AnthropicClientContextWindowTest.java
- llm/UsageParsingTest.java
- permission/PermissionModeTest.java
- session/SessionManagerTest.java
- teams/FileMailBoxTest.java
- tool/ToolSearchTest.java
- toolresult/ContentReplacementStateTest.java
- toolresult/ReplacementRecordsIOTest.java
- toolresult/ToolResultBudgetTest.java

ContextCompactorTest 使用 JUnit Test、TempDir、ConversationManager、Message、ToolResultBlock、ToolUseBlock 和 StubSummaryClient。它的关键不是“摘要返回了某个字符串”，而是验证压缩如何改变消息集合、哪些消息保留、摘要 client 如何被调用、session/工作目录参数如何参与。

读该测试时记录四个对象：输入消息序列、触发阈值、stub 模型收到的摘要内容、压缩后的 conversation。输入应覆盖普通 user/assistant、tool use/tool result、大输出和最近尾部。若测试只使用简单文本，不能证明工具协议在压缩中安全；应关注源码是否断言 tool result id、thinking 和 keep tail。

StubSummaryClient 的意义是隔离外部模型。摘要测试不应调用真实 API，因为 token、网络和模型输出不稳定。Stub 还应该记录调用次数和 prompt，确保自动压缩不会递归调用或重复摘要。TempDir 让 compact boundary/session 写入临时目录，避免污染项目。

可扩展测试：摘要失败时 conversation 是否保持原样；摘要返回空内容时是否拒绝写 boundary；边界多次存在时是否只使用最新；恢复附件超长时是否截断；usage anchor 与字符估算切换时是否触发正确阈值。

## 3. RecoveryAttachmentTest

RecoveryAttachmentTest 使用反射访问 RecoveryState 或 ContextCompactor 的内部状态，说明被测行为可能不是公开 API，而是压缩后的内部附件构造。反射测试的好处是能锁定内部恢复结构，坏处是重构字段名会导致测试脆弱；如果该行为是长期契约，未来应提取 package-private helper 或公开不可变结果对象。

该测试重点看最近文件读取和 Skill invocation 的记录是否按时间/优先级选择，重复路径是否去重，过期记录是否剔除，附件文本是否包含 path/content/skill SOP 的清楚标签。恢复附件不是普通摘要，它承担让模型重新获得操作所需的证据，因此测试应断言内容来源和顺序。

错误场景包括：文件记录为空、content 为空、同一路径多次读取、时间戳相同、skill 失败、附件总预算不足。一个常见错误是把全部历史 read 记录附加，导致压缩后上下文再次超限；另一个错误是只附加路径没有内容，模型仍然无法继续。

ConfigLoaderValidationTest 使用 TempDir 写入 YAML，再调用 ConfigLoader.load。它验证配置文件语法和语义错误应该在启动前发现。测试文件要关注使用的是 Path 还是字符串、文件不存在的期望、错误消息是否包含字段名、未知字段是否被允许，以及 SnakeCasePropertyUtils 的映射。

ProviderConfigApiKeyTest 验证 key 来源。API key 可能来自显式配置、环境变量或 provider 默认变量；测试构造临时配置并检查返回结果。环境变量测试在 Java 中不容易安全修改，因此源码可能通过临时文件、系统属性或明确的 env name 选择来测试。不要把测试中的 fake key 复制到生产配置。

ProviderConfigContextWindowTest 覆盖 contextWindow/maxOutput 默认和显式设置。它直接影响 ContextCompactor.manage、LlmClient 请求和 max_tokens recovery。测试要检查零、负数、缺省、超大值和 provider-specific default。默认值变化可能导致 token 成本和行为变化，即使编译完全不受影响，因此这个测试属于运行时契约。

建议补充配置测试：重复 provider、未知 protocol、空 model、非法 baseUrl、坏 Hook 条件、MCP command 缺失、权限规则冲突、Windows 路径和 YAML alias/depth 限制。

## 5. AnthropicClientContextWindowTest

AnthropicClientContextWindowTest 只针对 AnthropicClient 的上下文窗口/最大输出解析，说明 adapter 不只是网络包装。测试应验证 ProviderConfig 解析后的 window 如何传进 client，未配置时的 fallback，以及 client 暴露给 Agent 的值是否稳定。

Anthropic adapter 升级时，重点回归：Messages API 参数名、max_tokens 的上限、thinking 参数、usage input/cache 字段和错误类型。测试最好使用 SDK 对象或 JSON fixture，不要依赖真实 streaming，因为 SDK 可能改变 builder API。若 context window 不正确，Agent 可能太晚压缩而收到 400，也可能太早压缩增加成本。

UsageParsingTest 使用 Jackson ObjectMapper/JsonNode 构造不同 JSON，验证 input/output/cache token 的读取。JsonNode tree model 适合 provider response 字段不稳定，但解析方法必须明确缺失字段按 0、null、负数或错误处理。

用量是两个用途：向 UI 发 UsageEvent，累计 Agent totalInput/totalOutput；向 ContextCompactor 提供 UsageAnchor；可能还影响缓存成本展示。测试不能只断言 total=输入+输出，还要看 cache read/cache creation 是否被单独保留，stream end 缺 usage 时是否安全。

常见回归是 provider 改字段名、usage 只在最后一个 SSE、cache 字段嵌套、数值是字符串、对象为 null、异常响应也被当作 usage。建议 fixture 包括完整、部分、空、错误和未知字段，测试 parser 不被未知字段影响。

PermissionModeTest 很小，但它锁定配置字符串到枚举/Decision 的映射。测试内容应包括允许的大小写、空值、未知值和默认模式。PermissionMode 是所有工具安全行为的入口，枚举字符串改名可能使旧 config 进入错误模式。

小测试也要追踪调用者：JimiCode.main 调 fromConfig；Agent 检查 PLAN；PermissionChecker 根据 mode 做 check；ExitPlanModeTool 和 UI dialog 改变计划状态。若新增一个 mode，必须同步默认工具分类、Prompt reminder、permission UI、子 Agent继承和测试。

SessionManagerTest 是持久化测试的中心。它使用 TempDir、ConversationManager、Message 和 SessionManager，覆盖 session 文件创建、追加、读取、重建和格式化。应逐个观察：

- sessionsDir 如何由 workDir/session id 得到；
- id 是否被规范化，能否路径穿越；
- JSONL 每行是否独立；
- 坏行是否跳过；
- assistant/user role 如何 replay；
- compact boundary 是否被识别；
- boundary 后消息如何追加；
- listSessions 是否排序；
- currentGitBranch 失败是否返回空；
- formatRelativeTime/formatFileSize 是否稳定。

Session resume 的关键断言不是文件存在，而是 ConversationManager 的消息语义。对于有 boundary 的 session，旧前缀不能重复 replay；对于无 boundary 的旧 session，所有普通消息应按顺序重放；对于坏 boundary，应回退而不是返回空会话。

建议补充多进程写入、最后一行没有换行、Unicode/emoji、超长消息、tool result block、thinking signature、多个 boundary、损坏 JSON 和未知 role。若 JSONL 文件中包含 API key 或敏感内容，测试还应检查日志/错误不会输出原文。

FileMailBoxTest 验证团队通信的文件实现。TempDir 保证每个测试有独立 mailbox root；测试应关注发送、读取、空 mailbox、多个消息顺序、ack/删除、部分文件和 mutation conflict。文件通信必须在真实文件系统上测，因为 mock Path 无法暴露 rename/encoding/权限问题。

FileMailBox 的一致性通常依赖临时文件 + atomic move；如果生产源码采用 retry 或随机延迟，测试应验证最终状态，而不是固定 sleep 时间。消息 id 必须稳定，重复读取不应产生重复的父 Agent notification。跨进程场景可以用两个 FileMailBox 实例模拟，而非只复用同一对象。

建议增加损坏 JSON、发送进程中断、读进程中断、同名临时文件、目录不存在、权限只读、消息超大、时钟倒退和清理 stale mailbox 的测试。若 mailbox 作为任务结果的唯一存储，还要测试父进程重启后仍能消费未 ack 消息。

ToolSearchTest 创建 ToolSearchTool、ObjectMapper 和工具定义，测试 query 解析、name selection、schema 输出和未知工具。它锁定的是 deferred tool 协议：模型通过搜索获得 schema，之后 Agent 才能调用。

测试要覆盖空 query、普通关键词、多名称 select、空名称、重复名称、未知名称、混合空格、大小写和 JSON 序列化失败。返回内容需要明确哪些工具找到、哪些不存在，不能让模型误以为所有 query 都成功。Schema 中 name、description、input_schema/properties 必须与 ToolRegistry 的真实输出一致。

若 ToolSearchTool 修改了 Registry active/deferred 状态，测试还要确认再次查询幂等、另一 Agent 的 Registry 不被污染、工具过滤器不会被绕过。若只返回 schema 而不修改状态，下一步 execution 是否允许应由源码契约说明。

## 11. ContentReplacementStateTest 与 ReplacementRecordsIOTest

ContentReplacementStateTest 锁定内存状态：某个 tool result/record 是否已替换、重复 apply 是否幂等、不同 session/key 是否隔离、删除/恢复操作是否正确。状态对象使用 Map/Set 时要检查可变性和 copy；如果 getter 返回内部集合，调用者可能绕过 transition。

ReplacementRecordsIOTest 使用 TempDir 验证 JSONL records 追加、读取、坏行、空列表和编码。它是审计日志测试，不只是文件写测试。记录应包含 tool use id、原始/新状态、原因、时间或 version，让后续恢复能知道为什么 API 视图不同。

建议测试写入中途异常、父目录不存在、追加多批记录、重复记录、记录顺序、Unicode、大摘要和并发 writer。若 IO 失败源码选择 best effort，测试应确认内存状态仍可用，同时确认不会把错误伪装成 durable。

## 12. ToolResultBudgetTest

ToolResultBudgetTest 是上下文成本治理的核心。它构造 ConversationManager、ToolResultBlock、临时 session 目录和 replacement state，检查 apply 返回的新 ConversationManager、records 和原始 conv 是否保持不变。

1. apply 不原地修改 live conversation；
2. 最近工具结果优先保留；
3. 大旧结果按策略替换；
4. 替换文本仍包含足够定位/事实；
5. tool use/result 配对不破坏；
6. 记录只对新 transition 追加；
7. 重复 apply 幂等；
8. session 目录写失败不会让 in-memory 结果消失；
9. 不同 tool/category 的预算策略可区分；
10. replacementState 跨 iteration 继续有效。

这个测试对应一个典型架构问题：为了降低 token，不能破坏对话协议和审计。若开发者只断言最终字符串短了，可能已经把 tool call id、文件路径或错误信息截掉；因此测试应同时断言结构化字段。

模型、时间、随机数、线程和外部进程都会让测试不稳定。ContextCompactor 用 StubSummaryClient 是正确方向；History/Session 使用 TempDir；Task id 使用 SecureRandom 时测试不应断言具体 id；SpinnerVerbs 使用随机文案时测试应断言候选集合/非空。

对时间相关逻辑可以注入 Clock；对网络可以用 fake LlmClient 或 fixture；对 ProcessBuilder 可以抽象 ProcessRunner；对 tmux 可以 fake ITermBackend；对用户确认可以直接完成 PermissionResponse/CompletableFuture；对 BlockingQueue 可以放入确定事件序列并设置有限 timeout。

不要在单元测试中调用真实 provider、真实 tmux、真实 git reset、真实用户 home 或真实 MCP server。可以增加单独标记的 integration/smoke profile，但默认 mvn test 必须离线可重复。

当前测试覆盖压缩和预算，说明上下文是活跃演进区域；覆盖 provider usage，说明外部协议不稳定；覆盖 FileMailBox，说明团队协作有跨文件一致性；覆盖 SessionManager，说明 resume 是用户核心能力。相对缺少 UI、Bash、MCP、Worktree 和 Hook 集成测试，这些是未来最值得补齐的风险面。

- AgentLoopTest：文本、tool_use、end_turn、max iteration、interrupt；
- StreamingExecutorTest：allow/deny/ask、Hook reject、tool exception、timeout；
- BashToolTest：cwd、exit code、stderr、timeout、output cap；
- FileToolSecurityTest：path traversal、symlink、stale edit、多匹配；
- McpManagerTest：connect/list/call/close/reconnect；
- HookEngineTest：condition、regex error、command timeout、notifications；
- WorktreeManagerTest：创建失败回滚、changes、stale；
- TuiEventReducerTest：事件顺序、dialog、resize、queue timeout；
- SubAgentLifecycleTest：sync/background/cancel/orphan；
- TaskListConcurrencyTest：状态迁移和文件冲突。

为什么状态机测试比 getter 测试有价值？因为 Agent 的风险在状态转换和副作用，不在简单字段返回。为什么要使用 TempDir？隔离测试副作用，避免测试顺序和用户环境影响。为什么 mock 模型？控制输出和错误，让测试证明 Agent 行为而不是外部服务。为什么还要 integration test？单元测试无法证明真实 SDK/SSE/MCP/终端资源兼容。

如何测试取消？订阅 Flux 后取消，验证工具不再开始、子进程被销毁、状态变成 cancelled、必要的 session/trace 已保存。如何测试背压？让生产者快速发事件而消费者慢，验证队列满时是阻塞、降级还是 ErrorEvent，不能无限丢失。

完整测试命令是 mvn -q clean test；单类使用 -Dtest=ClassName；单方法使用 -Dtest=ClassName#method。执行后检查 Surefire reports，不要只看命令最后一行。失败时先区分编译、测试断言、timeout、外部资源和环境问题。

构建验收还需要 mvn -q package 和 java -jar target/jimicode.jar。测试结果、依赖树、jar 内容和配置示例共同构成发布证据。不要因为单元测试全绿就认为 TUI、MCP、Bash 和 provider 已被真实验证。

读者应该能从每个测试文件反向找到生产代码，并说明测试锁定的行为、输入边界、状态变更和失败路径；能为任意新增工具写出 schema、权限、执行、结果、超时和测试；能为任意新增 provider 写出 adapter、StreamEvent、usage、error 和 fixture；能判断一个修复应该改生产代码、测试契约、文档还是构建配置。

### 工程补充来源：`study-notes/00-源码精讲-Java21复刻ClaudeCode.md`

> 讲解对象：`d:/code/others/jimi-code`，103 个 Java 源文件、25 个包、纯 Java 21 + Maven 构建。
> 本讲按「主干 → 精华 → 支撑」顺序展开，目标是让你能看懂整个项目的运行脉络，并学到可以直接写进简历/面试话术的设计点。

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

一句话：**一个跑在终端里的 AI 编程助手，相当于用 Java 21 重写的 Claude Code**。

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

> **Agent = while(模型还想调工具) { 调工具; 把结果放回对话; }**

"AI 自主干活"在代码层面就这一句话。模型是否结束，完全由它自己的 stop_reason 和是否产出工具调用决定。剩下的工程复杂度，全在"把对话喂得又便宜又不溢出的上下文工程"上。

## 四、精华：上下文工程（两层压缩 + 工具结果预算）

这是整个项目**技术含量最高的部分**，也是面试时最能讲出深度的点。它要解决的问题是：Agent 对话会无限变长，但模型上下文窗口有限、按 token 计费。两条防线：

- **第一层：工具结果预算**（`toolresult` 包）——在**本地**把占 token 的大块工具结果替换成小标签，零成本。
- **第二层：上下文压缩**（`compact` 包）——超过阈值时，**用 LLM 自己总结旧对话**，把历史折叠成一段 summary。

### 4.1 工具结果预算：ToolResultBudget（`ToolResultBudget.java`）

问题背景：一个 `ReadFile` 可能返回 5 万字符，`grep -r` 可能更大。这些东西塞回对话，一轮就撑爆窗口。做法是**不真正删，而是"落盘替换 + 预览串"**：

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

| 常量 | 值 | 含义 |
|---|---|---|
| `SUMMARY_OUTPUT_RESERVE` | 20_000 | 给总结输出预留的空间 |
| `AUTO_COMPACT_SAFETY_MARGIN` | 13_000 | 软触发线 |
| `MANUAL_COMPACT_SAFETY_MARGIN` | 3_000 | 硬触发线，越过强制压缩 |
| `KEEP_RECENT_TOKENS` | 10_000 | 保留最近原文的 token 预算 |
| `MIN_KEEP_MESSAGES` | 5 | 最少保留的消息数 |
| `KEEP_MAX_TOKENS` | 40_000 | 保留原文上限 |
| `MAX_CONSECUTIVE_FAILURES` | 3 | 熔断器：连续失败 3 次不再自动压缩 |

- `manage(...)`：每轮迭代前调用的自动压缩。判断依据是 `usageAnchor`（上一轮真实的 token 用量）+ 字符估算。
- `forceCompact(...)`：上下文超长报错时的兜底强制压缩。

**Layer 2 压缩策略**（`:62-70`）：不是把整个对话折叠成孤零零一段 summary，而是 **保留最近原文尾巴 + 只总结老前缀**——`KEEP_RECENT_TOKENS` 保底、`MIN_KEEP_MESSAGES` 保证至少 5 条原文。这比"全量总结"聪明得多：模型对最近的上下文记忆最强，丢的都是老历史。

**压缩后的恢复附件（Recovery）**：压缩会丢失"最近读过哪些文件、哪些技能 SOP 在用"这些工作状态，所以 `Agent.java` 里维护一个 `RecoveryState`，每次 ReadFile / 调 skill 时记录快照；压缩发生后，把这些快照作为附件追加到总结后面（`RECOVERY_FILE_LIMIT=5` 个文件、每个 5000 token、技能预算 25k）。这样压缩后模型还能"接得上"。

**熔断器**：`AutoCompactTrackingState` 记录连续失败次数，达到 3 次就停掉自动压缩，避免"压缩请求本身反复失败 → 无限重试"的恶性循环。

`Agent.java` 每轮：先 `ContextCompactor.manage()`（第二层判断）→ 再 `ToolResultBudget.apply()`（第一层本地替换）→ 才把 `conv` 送进 `client.stream()`。第一层省钱，第二层防爆，两层叠加把上下文控制得非常紧。

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

对话消息（`ConversationManager.Message`）到各家请求体的映射是统一的：

- **assistant 文本 + 工具调用** → Anthropic 的 `ContentBlockParam.ofToolUse` / OpenAI 的 `ofFunctionCall`（带 `callId/name/arguments`）。
- **工具结果** → Anthropic 的 `ofToolResult`（`toolUseId/content/isError`）/ OpenAI 的 `ofFunctionCallOutput`（`callId/output`）。
- **纯文本** → 各家文本消息，并 `mergeConsecutiveSameRole` 合并相邻同角色消息（很多 API 不允许连续同角色消息）。

这套映射是"如何把统一消息模型适配到各家 API"的标准答案。

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

"AI Agent 的执行安全"是行业热门话题。这个模块展示了完整的分层策略：黑名单兜底 + 白名单免打扰 + 文件级规则 + 模式开关，四种手段叠加，比只做"弹窗问用户"成熟得多。

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

不是所有工具都值得把 schema 塞进每一轮请求——schema 本身也占 token。所以：
- `shouldDefer() = true` 的工具（如 MCP 工具）默认**不加载 schema**。
- Agent 循环每轮注入一条 system reminder："以下工具通过 ToolSearch 按需加载"（`Agent.java:189-199`）。
- 模型想用时先调 `ToolSearch`（`searchDeferred` / `findDeferredByNames`），把 schema 拉进来（`markDiscovered`），之后轮次 `getDeferredToolNames()` 就不再提示它。

这是一个"**schema 按需加载**"的 token 优化手段，和 tool-result 预算一脉相承：能省就省。

### 8.3 工具执行器 StreamingExecutor

`executeAll` 可以并行执行多轮收集到的工具调用，通过事件队列回传结果。每条工具结果最后被包成 `ToolResultBlock` 塞回对话，作为下一轮请求的一部分（`Agent.java:388-398`）。

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

按你自己的目标（学习 Agent 架构）推荐阅读顺序：

1. **第一遍（1 小时，搭骨架）**：`JimiCode.java` → `Agent.java` 的 `agentLoop` → `StreamingExecutor` → `AgentEvent`。看懂"循环 + 事件 + 工具执行"这条主线。
2. **第二遍（2 小时，学精华）**：`ToolResultBudget` → `ContentReplacementState` → `ContextCompactor`。重点理解"省 token / 保前缀 / 防溢出"的工程思路。
3. **第三遍（按需，学细节）**：`llm` 包三个客户端对照读——同一套接口怎么适配 Anthropic 和 OpenAI；再看 `permission` 的分层检查。
4. **进阶**：`subagent`（fork/工具过滤）→ `teams`（文件信箱通信）→ `worktree`（并行隔离）。这三个组合起来就是一个完整的"多 Agent 并行编程"架构。
5. **动手**：项目自带了 `study-notes/sourcebook/`（小林coding 的源码教材）和 `docs/PROJECT_GUIDE.md`，可以配合阅读。改点小功能（比如加一个自定义工具、调预算阈值）最能加深理解。

- **上下文预算**：本地落盘替换 + LLM 总结压缩两层防线，以及"byte-stable prefix 保 prompt cache"。
- **统一协议层**：sealed interface 事件模型 + 三客户端适配。
- **权限护栏**：黑名单/白名单/文件 glob 规则/模式开关四层。
- **并发模型**：虚拟线程一个对话一个线程，团队进程内/进程外两种调度。
- **工具生态**：deferred schema 按需加载、MCP 桥接、skill SOP。

*本文基于对 `jimi-code` 源码的直接阅读整理，行号对应仓库当前版本。祝你学得愉快。*

### 工程补充来源：`study-notes/02-启动流程详解.md`

> 适用版本：Java 21 + Maven（当前仓库唯一实现）
> 阅读建议：配合源码逐段对照，重点看 `JimiCode.java`、`ConfigLoader.java`、`JimiCodeModel.java`、`Agent.java` 四个文件。
> 核心结论：**启动 = 加载配置 → 构建系统提示词 → 创建 LLM 客户端 → 注册工具全家桶 → 连接 MCP / 加载技能 → 起终端聊天界面**，之后进入"调用 LLM → 执行工具 → 再调用 LLM"的 Agent 循环。

```mermaid
flowchart TD
    A[JimiCode.main 入口] --> B[解析配置路径<br/>CLI参数 > 环境变量 > 默认]
    B --> C[ConfigLoader.load<br/>合并多配置文件 + 校验]
    C --> D[解析 permission_mode]
    D --> E[new JimiCodeModel<br/>创建 TUI 模型]
    E --> F{Provider 数量}
    F -- 1个 --> G[自动选中并标记待初始化]
    F -- 多个 --> H[进入 Provider 选择界面]
    G --> I[initializeProvider<br/>核心装配流程]
    H --> I
    I --> J[创建 Agent 对象]
    J --> K[异步连接 MCP Server]
    K --> L[Program.run 启动终端 UI]
    L --> M[用户输入消息]
    M --> N[Agent.run → agentLoop]
    N --> O[上下文压缩检查]
    O --> P[LLM 流式请求]
    P --> Q{有工具调用?}
    Q -- 否 --> R[保存回复, 本轮结束]
    Q -- 是 --> S[执行工具 → 结果写回对话]
    S --> O
```

## 2. 程序入口 `JimiCode.main()`

```java
// 优先级：第一个 CLI 参数 > 环境变量 JimiCode_CONFIG > 默认候选路径
String configPath = null;
if (args.length > 0) {
    configPath = args[0];
} else {
    String envPath = System.getenv("JimiCode_CONFIG");
    if (envPath != null && !envPath.isBlank()) {
        configPath = envPath;
    }
}
```

```java
config = ConfigLoader.load(configPath);          // 失败 → 打印错误并 exit(1)
permissionMode = PermissionMode.fromConfig(...); // 非法模式 → exit(1)
```

```java
var model = new JimiCodeModel(
        config.getProviders(),
        config.getMcpServers() != null ? config.getMcpServers() : List.of(),
        config.getHooks() != null ? config.getHooks() : List.of(),
        permissionMode
);
var program = new Program(model);
model.setProgram(program);   // 让后台线程能向 TUI 发消息
```

- `System.out.print("\033[?25l")` 隐藏光标（退出时恢复）。
- 500ms 后在**虚拟线程**里重注册 SIGINT：流式输出中 Ctrl+C = 打断当前回答；空闲时 Ctrl+C = 退出程序。

```java
Thread.ofVirtual().start(() -> {
    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    try {
        Signal.handle(new Signal("INT"), sig -> model.handleSigint());
    } catch (IllegalArgumentException ignored) {}
});
```

文件：`src/main/java/com/jimicode/config/ConfigLoader.java`

未显式指定路径时，按以下候选顺序查找，**后面的层覆盖前面的层**：

| 优先级 | 路径 |
|---|---|
| 1（最底层） | `~/.JimiCode/config.yaml` |
| 2 | `<当前工作目录>/.JimiCode/config.yaml` |
| 3（最高层） | `<当前工作目录>/.JimiCode/config.local.yaml` |

合并规则：
- `providers`：整体替换
- `mcp_servers`：按 `name` 合并/覆盖
- `hooks`：追加
- `permission_mode`：非空时覆盖

- 用 SnakeYAML + 自定义 `SnakeCasePropertyUtils` 实现 **下划线转驼峰**（`base_url` → `baseUrl`）。
- 校验规则：至少一个 Provider，且每个 Provider 必须包含 `name / protocol / base_url / model`。
- `protocol` 只能是：`anthropic`、`openai`、`openai-compat` 三种。

```yaml
providers:
  - name: anthropic
    protocol: anthropic                 # anthropic / openai / openai-compat
    base_url: https://api.anthropic.com
    model: claude-sonnet-4-6
    context_window: 200000
    max_output_tokens: 8192
    api_key: "sk-ant-..."               # 留空则读环境变量 ANTHROPIC_API_KEY
    thinking: false

permission_mode: default                # default / accept_edits / plan / bypass

mcp_servers:
  - name: context7
    command: npx
    args: ["-y", "@upstash/context7-mcp"]

## 4. TUI 模型创建 `JimiCodeModel`

文件：`src/main/java/com/jimicode/tui/JimiCodeModel.java`

| 项目 | 说明 |
|---|---|
| 状态分支 | 只有一个 Provider → 直接 `CHAT` 态 + 标记 `pendingSingleProviderInit`；多个 → `PROVIDER_SELECT` 态 |
| HookEngine | 把配置里的 hooks 解析成事件/动作/条件，加载进引擎 |
| CommandRegistry | 斜杠命令注册表 |
| HistoryStore | 加载用户输入历史 |
| ConversationManager | 创建空对话管理器 |

## 5. 核心装配 `initializeProvider()`

这是**启动后最重要的初始化**，全部失败都会被 catch 后作为错误消息显示在聊天区。按执行顺序拆解：

```java
memoryManager = new MemoryManager(workDir);                    // 记忆管理器
instructionsContent = MemoryManager.loadInstructions(workDir); // 项目指令(AGENTS.md等)
memoryContentField = buildMemorySection();                     // 长期记忆片段
String systemPrompt = PromptBuilder.buildSystemPrompt(env, options); // 系统提示词
```

```java
client = LlmClient.create(selectedProvider, systemPrompt);     // 按 protocol 创建适配器
String protocol = selectedProvider.getProtocol();
```

### 5.3 注册工具（ToolRegistry 全家桶）

**第一批：基础工具**（`ToolRegistry.createDefault()`）

| 工具 | 类别 | 用途 |
|---|---|---|
| `ReadFile` | read | 读取文件，带行号 |
| `WriteFile` | write | 写文件 |
| `EditFile` | write | 精确字符串替换 |
| `Bash` | command | 执行 `bash -c` 命令 |
| `Glob` | read | 文件通配搜索 |
| `Grep` | read | 正则内容搜索 |

| 工具 | 说明 |
|---|---|
| `ToolSearch` | deferred 工具搜索/精确加载 |
| `ExitPlanMode` | 退出计划模式并触发审批对话框 |
| `AskUserQuestion` | 向用户弹出结构化选择题 |
| `Agent` | 启动子 Agent（同步/后台/fork/teammate） |
| `EnterWorktree` / `ExitWorktree` | Git worktree 隔离 |
| `TaskCreate/Get/List/Update` | 持久化任务列表 |
| `TeamCreate/Delete`、`SendMessage` | 多 Agent 团队 |

```java
var savedSession = WorktreeSessionStore.load(workDir);
if (savedSession != null && 目录存在) {
    WorktreeSessionStore.restoreSession(savedSession);  // 恢复上次崩溃前的隔离工作区
}
```

```java
permChecker = new PermissionChecker(initialPermissionMode, Path.of(workDir));
```

```java
sessionId = SessionManager.newId();                 // 新会话 ID
fileHistory = new FileHistory(workDir, sessionId);  // 编辑前快照/回滚
```

把上面所有组件注入 `Agent`：文件历史、指令、记忆、权限检查器、工作目录、sessionId、通知回调、工具过滤（团队协调模式）。

```java
Thread.ofVirtual().name("mcp-connect").start(() -> {
    mcpManager = new McpManager(mcpServers);
    var result = mcpManager.connectAll();
    for (var t : result.tools()) registryRef.register(t);   // 工具命名 mcp__<server>__<tool>
    // 汇总 server instructions 与工具清单，作为 system reminder 注入
});
```

```java
skillCatalog = new SkillCatalog();
var skillDir = Path.of(workDir, ".JimiCode", "skills");
if (Files.isDirectory(skillDir)) {
    skillCatalog.loadFromDirectory(skillDir);   // 扫描技能并注册为斜杠命令
}
```

```java
agent.setHookEngine(hookEngine);
fireHook(HookEngine.EventName.SESSION_START, null, null);
```

| 序号 | 内容 | 关键类 |
|---|---|---|
| 1 | 记忆/指令加载 | `MemoryManager` |
| 2 | 系统提示词构建 | `PromptBuilder` |
| 3 | LLM 客户端 | `LlmClient.create()` |
| 4 | 6 个基础工具 | `ToolRegistry.createDefault()` |
| 5 | 高级工具(子Agent/任务/团队/worktree) | `AgentTool`、`TaskTools`、`TeamTools` 等 |
| 6 | Worktree 崩溃恢复 | `WorktreeSessionStore` |
| 7 | 权限检查器 | `PermissionChecker` |
| 8 | 会话 + 文件历史 | `SessionManager`、`FileHistory` |
| 9 | Agent 装配 | `Agent` |
| 10 | MCP 异步连接 | `McpManager` |
| 11 | Skill 技能扫描 | `SkillCatalog` |
| 12 | SESSION_START 钩子 | `HookEngine` |

- `Program.run()` 启动 Bubble Tea 风格的终端界面。
- 显示 Provider 选择界面（多 Provider 时）或直接进入聊天界面（单 Provider 时自动触发初始化）。

文件：`src/main/java/com/jimicode/agent/Agent.java`（核心中的核心）

### 7.1 消息入口 `sendUserMessage()`

```java
conversation.addUserMessage(userText);
SessionManager.saveMessage(workDir, sessionId, "user", userText);
historyStore.append(userText);                        // 输入历史
conversation.addSystemReminder(mcpInstructions);      // 首次注入 MCP 说明
prefetchRelevantMemories(userText);                   // 预取相关记忆(虚拟线程, 8s 超时)
fireHook(TURN_START);                                 // 触发 turn_start 钩子
agentQueue = agent.run(conversation);                 // 启动 Agent 循环(虚拟线程)
Command.tick(50ms, AgentEventMessage);                // 定时轮询事件队列
```

1. **注入长期记忆**（仅首轮）：`conv.injectLongTermMemory(instructions, memoryContent)`
2. **收集后台通知**：子 Agent 任务完成、团队邮箱消息 → 注入为 `<system-reminder>`
3. **计算工具 schema**：每轮按协议生成（支持技能/协调过滤）
4. **上下文压缩检查**：`ContextCompactor.manage()` 超限自动压缩
5. **deferred 工具提醒**：提示模型可用 `ToolSearch` 加载未发现工具
6. **计划模式提醒**：注入计划文件路径 + 五阶段工作流
7. **Layer 1 工具结果预算**：`ToolResultBudget.apply()` 生成不含大结果的 API 视图
8. **调用 LLM**：`client.stream()` 流式返回事件队列
9. **处理事件**：文本增量、thinking、工具调用、usage、错误
10. **错误恢复**：
    - `context too long` → 强制压缩重试（最多 3 次）
    - `rate limit` → 等待 5s 重试
    - `max_tokens` → 首次提升上限到 64000，之后最多 3 次"从中断处继续"
11. **执行工具**：`StreamingExecutor.executeAll()` 并行执行同一轮里的工具调用
12. **结果写回**：tool-result 消息加入对话
13. **结束条件**：无工具调用 → 结束；调用了 `ExitPlanMode` → 结束；超迭代上限 → 报错结束

```
LLM 流事件 StreamEvent  ──归一化──▶  Agent 内部处理  ──▶  AgentEvent 队列  ──▶  TUI 渲染
(text/thinking/tool/usage)          (压缩/重试/权限)     (StreamText/ToolUse/...)   (聊天区展示)
```

| 类 | 职责 |
|---|---|
| `JimiCode` | 程序入口，配置加载、TUI 启动、SIGINT |
| `ConfigLoader` | YAML 多文件合并 + 校验 |
| `AppConfig` | 根配置模型（providers/mcp/hooks/permission_mode） |
| `ProviderConfig` | LLM 提供商配置，解析 context_window/max_output_tokens |
| `JimiCodeModel` | TUI 主模型，负责整体装配与事件循环 |
| `Agent` | Agent Loop 核心状态机 |
| `AgentEvent` | 交给 TUI 的运行事件 |
| `StreamingExecutor` | 并行执行一轮内的工具调用 |
| `ToolRegistry` | 工具注册/协议 schema 生成/deferred 管理 |
| `LlmClient` | LLM 统一接口，按协议创建适配器 |
| `AnthropicClient` / `OpenAiClient` / `OpenAiCompatClient` | 三种协议适配器 |
| `ConversationManager` | 消息序列管理 |
| `PermissionChecker` | 权限分层检查 |
| `McpManager` | MCP Server 连接与工具注册 |
| `ContextCompactor` | Layer 2 上下文压缩 |
| `ToolResultBudget` | Layer 1 工具结果预算 |
| `MemoryManager` / `MemoryRecall` | 长期记忆管理与相关记忆召回 |
| `SkillCatalog` | 技能扫描与斜杠命令注册 |
| `SessionManager` | 会话 JSONL 持久化/恢复 |
| `FileHistory` | 编辑前快照与回滚 |
| `TeamManager` | 多 Agent 团队管理 |

| 路径 | 内容 |
|---|---|
| `.JimiCode/config.yaml` | 项目配置 |
| `.JimiCode/config.local.yaml` | 本地覆盖配置 |
| `.JimiCode/sessions/*.jsonl` | 会话记录 + compact boundary |
| `.JimiCode/file-history/<session>/` | 编辑前备份和 rewind 快照 |
| `.JimiCode/memory/` | 项目级记忆 |
| `.JimiCode/skills/` | 项目级技能 |
| `.JimiCode/tasks/` | 任务列表 |
| `.JimiCode/teams/` | 团队邮箱与共享状态 |
| `.JimiCode/worktrees/` | Git worktree |
| `.JimiCode/worktree_session.json` | 当前 worktree session |
| `.JimiCode/plans/` | 计划模式文件 |
| `.JimiCode/tool_results/` | 大工具结果 spill |
| `.JimiCode/replacement_records.jsonl` | 工具结果替换决策 |

> 启动后 JimiCode 依次做了：**加载配置 → 构建系统提示词 → 创建 LLM 客户端 → 注册一整套编码工具（文件/命令/子代理/团队/任务/worktree）→ 连接 MCP 与加载技能 → 起一个终端聊天界面**，等你发消息后进入"调用 LLM → 执行工具 → 再调用 LLM"的 Agent 循环。

### 工程补充来源：`study-notes/09-优化设计方案.md`

> 目标：在不破坏现有功能的前提下，从**架构、健壮性、可观测性、性能、可测试性、可扩展性**六个维度提出可落地的优化方案。
> 适用对象：`jimi-code`（Java 21 复刻 Claude Code 的 AI Coding Agent）。

```
JimiCode.java (入口)
   └─ JimiCodeModel (TUI 主模型, 2220 行) ── 持有几乎所有 UI/业务状态
        ├─ Agent (主循环, 434 行)
        │    ├─ ContextCompactor (上下文压缩, 729 行, 静态)
        │    ├─ ToolResultBudget (工具结果预算, 276 行, 静态)
        │    ├─ StreamingExecutor (工具执行, 212 行)
        │    └─ PromptBuilder (提示词组装, 静态)
        ├─ ToolRegistry (工具注册, 静态工厂)
        ├─ PermissionChecker (权限检查)
        ├─ SessionManager (会话持久化, 静态)
        ├─ MemoryManager (记忆系统)
        ├─ HookEngine (钩子)
        ├─ McpManager (MCP)
        └─ ... (SubAgent/Team/Task/Skill/Worktree/Command)
```

功能完整性很高：Agent Loop、双层压缩、并发工具执行、权限多级模型、MCP、SubAgent、Team、Memory、Skill、Plan Mode、Rewind、会话恢复、成本统计均有实现。

| 编号 | 问题 | 严重度 | 位置 |
|------|------|--------|------|
| P1 | **巨型类**：JimiCodeModel 2220 行，TUI 与业务逻辑强耦合 | 高 | `tui/JimiCodeModel.java` |
| P2 | **静态方法泛滥**：ContextCompactor/SessionManager/ToolResultBudget/PromptBuilder 全静态，无法注入、难测试 | 高 | 多个类 |
| P3 | **Agent 主循环状态复杂**：错误恢复、max_tokens 升级、压缩重试全在 for 循环里手写状态机，分支多、难以单测 | 高 | `agent/Agent.java` |
| P4 | **可观测性缺失**：`slf4j-nop` 无任何日志；错误多被 `catch (Exception ignored)` 静默吞掉 | 高 | 全局 |
| P5 | **Token 估算粗糙**：`字符数/3.5` 启发式，与真实 tokenizer 偏差大 | 中 | `ContextCompactor.estimateTokens` |
| P6 | **硬编码魔法数字/字符串**：protocol 字符串 `"anthropic"/"openai"`、`MAX_OUTPUT_CHARS=10000`、重试次数、sleep 时长散落各处 | 中 | 全局 |
| P7 | **测试覆盖不足**：仅 14 个测试文件，Agent 主循环、权限、工具、TUI 几乎无测试 | 高 | `src/test` |
| P8 | **TUI 状态爆炸**：大量字段 `rewindDialog/permDialog/...` 平铺在一个类，状态迁移无法验证 | 中 | `JimiCodeModel` |
| P9 | **工具注册硬编码**：`ToolRegistry.createDefault()` 写死，无法从配置/插件动态扩展 | 中 | `tool/ToolRegistry.java` |
| P10 | **异常处理模式不统一**：混合返回 `ToolResult.error` 与抛出异常，错误上下文丢失 | 中 | 全局 |

```
┌─────────────────────────────────────────────────┐
│  UI 层  (tui/)  — 纯展示/交互，不依赖业务细节   │
│  JimiCodeModel → 拆分为多个小 Model + View       │
├─────────────────────────────────────────────────┤
│  应用层 (app/)  — 编排：Session, Agent, Compaction│
│  SessionCoordinator（会话编排器）                 │
├─────────────────────────────────────────────────┤
│  领域层 (core/)  — 业务规则                      │
│  AgentLoop, CompactionPolicy, PermissionPolicy   │
├─────────────────────────────────────────────────┤
│  基础设施 (infra/) — LLM/MCP/文件/命令           │
│  LlmClient, McpManager, SessionStore, 工具实现    │
└─────────────────────────────────────────────────┘
```

1. **引入组合根（Composition Root）**：新建 `AppContext` 或直接在主类中组装依赖，所有组件通过构造函数注入，消灭 `new` 硬编码与静态单例。
2. **接口化核心服务**：
   - `interface TokenEstimator`（替换静态 `estimateTokens`）
   - `interface CompactionStrategy`（替换静态 `ContextCompactor` 直接调用）
   - `interface SessionStore`（替换静态 `SessionManager`）
   - `interface ToolRegistry` 保留，但提供可插拔注册。
3. **UI 与业务解耦**：`JimiCodeModel` 不再直接持有 `Agent/PermissionChecker`，改为订阅 `SessionCoordinator` 暴露的**不可变状态快照**与事件流。

- 每个组件可独立单测（mock LLM、mock 文件系统）。
- 新增功能时只需实现接口，无需改动调用方。
- 静态方法类 → 实例对象，生命周期明确。

`Agent.agentLoop` 中手写了一个隐式状态机：

```java
if ("max_tokens".equals(stopReason)) {
    if (!maxTokensEscalated) { ... }           // 升级 max tokens
    else if (outputRecoveries < 3) { ... }      // 恢复重试
    // 否则 fall through 正常结束
}
if (streamError) {
    if (contextRetries < 3) { forceCompact; continue; }
    if (rate limit) { sleep(5s); continue; }
}
```

分支多、嵌套深、重试逻辑与主流程混在一起，无法单独验证"某次 max_tokens 后是否升级"。

```
enum LoopState { STREAM, HANDLE_RESULT, MAX_TOKENS, STREAM_ERROR,
                 EXECUTE_TOOLS, COMPACTING, DONE }

每个状态 = 一个独立的 handler（函数式接口），返回下一状态：
    LoopState handleMaxTokens(LoopContext ctx)
    LoopState handleStreamError(LoopContext ctx)
```

1. **`LoopContext`** 承载本轮所有状态：iteration、totalTokens、recoveries、escalated 标志。与循环解耦，便于快照/回滚。
2. **重试策略提取为 `RetryPolicy`**：
   ```java
   interface RetryPolicy {
       boolean shouldRetry(String errorKind, int attempt);
       Duration backoff(String errorKind, int attempt);
   }
   ```
   内置实现：context-too-long 重试 3 次；rate-limit 指数退避；可配置。
3. **max_tokens 策略提取为 `MaxTokensPolicy`**：第一次升级到 ceiling（64K），之后分片恢复。
4. **副作用外置**：状态处理中不直接 `putSafe(queue, ...)`，而是返回 `List<AgentEvent>`，由外层统一派发 → 状态 handler 可纯函数化，直接单测。

- `AgentLoopTest` 可直接注入 fake `LlmClient`（返回预置事件序列），覆盖所有分支。
- 每个策略（重试/升级/压缩）可独立调参、独立测试。

- 引入 `slf4j + logback`（或保留 slf4j 仅换 binding），替换 `slf4j-nop`。
- 关键埋点：
  - 每次 LLM 请求：provider、model、input/output/cache tokens、耗时、是否命中缓存（**INFO**）
  - 工具执行：toolName、耗时、结果长度、是否报错（**DEBUG/INFO**）
  - 压缩触发：触发原因（Layer1/2、阈值、当前 tokens、压缩前后 tokens）（**INFO**）
  - 异常堆栈：所有 `catch (Exception ignored)` → `logger.warn("...", e)`

- 新建 `UsageTracker`：累加每次 `UsageEvent`，按会话/按天记录 token 消耗与估算成本。
- 在 TUI 状态栏显示：`本轮 tokens / 累计 tokens / 估算成本 $`。
- 会话结束时写入 `{sessionId}.usage.json`，支持按模型汇总。

- `--debug` 参数开启**完整请求/响应转储**（到 `.JimiCode/debug/`），便于排查 prompt 缓存命中问题。
- 为 `ToolResultBudget`/`ContextCompactor` 提供"决策日志"（dry-run 模式输出每条 tool_result 的处理决策），解决压缩行为不可解释的问题。

- 现状：`estimateTokens` 用 `长度/3.5` 估算全部消息，虽有 `UsageAnchor` 校准，但估算误差仍可能触发错误的压缩决策。
- 方案：
  1. 定义 `TokenEstimator` 接口，默认实现保留启发式（零依赖）。
  2. 可选接入真实 tokenizer（如 Anthropic Token Counting API，或本地 `tiktoken` Java 移植）。
  3. **回写作废**：每次 stream 结束拿到真实 usage 后，用该轮实际数据修正估算公式的常量（在线校准）。

- 把 `ContextCompactor` 的魔法常量（阈值、margin、keep 窗口、spill 上限）收敛到 `CompactionConfig`，支持 YAML 配置与命令行覆盖。
- 压缩行为策略化：
  ```java
  interface CompactionStrategy {
      CompactionDecision decide(ContextUsage usage);
      String compact(...);   // 返回压缩结果描述
  }
  ```
  - `AdaptiveCompaction`（现状：anchor + 阈值）
  - `AggressiveCompaction`（低水位触发，适合长任务）
  - 未来可接 `MapReduce` 分块压缩（对超大会话分片摘要再合并）。

- 现状：`ToolResultBudget` 已保证字节稳定前缀。可进一步：
  - 将 `system-reminder`（日期、指令、plan-mode 提示）**固定顺序**，避免每次迭代插入点漂移破坏前缀缓存。
  - 在 `PromptBuilder` 中保证 `addSystemReminder` 的幂等性（同 key 覆盖而非追加）。

`JimiCodeModel` 2220 行承载了：provider 选择、聊天流、权限弹窗、rewind 弹窗、AskUser 弹窗、subagent 进度、task 面板、plan 审批。建议按 Bubble Tea 组合模型拆成独立 `Model`：

```
RootModel
 ├─ ChatModel        (消息列表 + 流式渲染)
 ├─ InputModel       (输入框)
 ├─ StatusModel      (状态栏: tokens/成本/spinner)
 ├─ DialogStack      (权限/AskUser/Rewind/Plan 统一弹窗栈)
 ├─ SubAgentModel    (进度展示)
 └─ PanelModel       (Task/Team 面板)
```

- 新增 `UiState`（不可变 record），`Update()` 返回新实例，替换当前平铺的可变字段。
- 弹窗统一为 `Dialog` 抽象（`title/message/options/onSelect`），替换 `permDialog/rewindDialog/...` 各自为政的字段。

- 状态迁移可测试（输入按键序列 → 断言状态）。
- 新增交互（如 diff 预览）只需加一个 Dialog 类型。

- 定义 `JimiCodeException`（受检）与分类：
  - `ConfigException`（配置错误，已有）
  - `StreamException`（LLM 流中断/超时）
  - `ToolExecutionException`（工具失败）
  - `PermissionDeniedException`
- 规则：
  - **预期失败**（工具返回错误、权限拒绝）→ 返回结果对象，不入异常链；
  - **非预期失败**（IO、网络、Bug）→ 抛异常并带上下文（toolName、args、耗时）；
  - 所有边界统一由 `AgentLoop` 顶层 catch 并转为 `ErrorEvent`，同时打日志。

- 全项目搜索 `catch (Exception ignored)`（约 30+ 处），逐处改为：
  - 可恢复 → 记录 `logger.debug` 并降级；
  - 不可恢复 → 记录 `logger.error` 并向上传播；
  - 明确可忽略（如 best-effort 快照）→ 注释说明理由 + 至少 debug 日志。

- LLM 流已支持 30s poll 超时；建议对**工具执行**增加超时（尤其 Bash），防止死循环命令挂起整个 agent。
- 将 `SIGINT` 处理升级为"当前 turn 可取消、会话可继续"（当前实现是整体退出）。

| 目标 | 现有 | 建议 |
|------|------|------|
| Agent 主循环分支 | 无 | 状态机化后注入 fake LlmClient，覆盖 max_tokens/rate-limit/context-too-long/正常流程 |
| PermissionChecker | 1 个（仅 mode） | 补充规则匹配、危险命令、路径沙箱、allow-always 的用例 |
| 工具实现（Read/Edit/Bash） | 无 | 用临时目录 + mock 进程覆盖 |
| 上下文压缩决策 | 有（compact） | 补充阈值边界（刚好触发/不触发）、keep 窗口配对保护 |
| TUI 状态迁移 | 无 | 拆分后对 DialogStack/ChatModel 输入序列化测试 |

- 用 `WireMock` 或本地 `MockServer` 模拟 Anthropic/OpenAI 流式接口，做端到端冒烟：真实 prompt 构建 → 模拟流 → 断言工具调用被正确解析。
- CI 中运行 `mvn test`，接入 GitHub Actions（当前仓库已有 Gitee/GitHub 双远程）。

- 文件系统访问抽象为 `FileSystemPort`（便于注入内存实现）。
- 时间抽象为 `Clock`（便于测试会话时间格式化、压缩时间戳）。

- 现状：`ToolRegistry.createDefault()` 硬编码 6 个工具。
- 方案：支持通过配置/SPI 加载自定义工具：
  ```yaml
  tools:
    - name: MyTool
      class: com.example.MyTool   # 实现 Tool 接口
      config: {...}
  ```
  或者沿用 MCP 作为统一扩展机制（推荐优先 MCP，减少自建工具 API）。

- 现状：`LlmClient.create()` 用 switch 按 protocol 字符串分发。
- 方案：`ProviderFactory` 注册表（`Map<String, ProviderFactory>`），支持运行时注册新协议（Azure OpenAI、Ollama、vLLM 等），并允许配置 alias（`model: sonnet` → 解析为完整模型名，已有 `ModelResolver` 雏形）。

- 现状：单会话单 Agent。
- 方案：`SessionCoordinator` 支持并行会话（每个会话一个 Agent + 独立 conversation），TUI 用 tab 切换；为 Team 功能复用同一基础设施。

| 阶段 | 内容 | 价值 | 工作量 |
|------|------|------|--------|
| **M1 健壮性地基** | 引入日志（P4）、统一异常（P7.1/P7.2）、工具超时（P7.3）、魔法数字配置化（P6） | 可排查、防挂起 | 小 |
| **M2 可测试性** | Agent 主循环状态机化（P3）、TokenEstimator 接口（P5）、核心服务接口化（P2） | 主循环可单测 | 中 |
| **M3 架构解耦** | 组合根 + 依赖注入、UI/业务解耦（P1）、TUI 拆分子模型（P8） | 长期可维护 | 大 |
| **M4 可观测增强** | UsageTracker 成本统计（P4.2）、--debug 转储、压缩决策日志 | 解释/优化成本 | 小 |
| **M5 扩展性** | 工具插件化（P9.1）、Provider 注册表（P9.2）、多会话（P9.3） | 生态能力 | 中 |

> 建议：**M1 + M2 立即做**（改动小、收益大、风险低）；M3 需配合功能冻结窗口执行；M4/M5 按业务需要排期。

1. **行为兼容**：所有重构保持对外行为不变，以现有 14 个测试 + 新增测试作为回归护栏。
2. **提示词缓存**：任何改动都不应破坏 `ToolResultBudget` 已建立的字节稳定前缀；改动 conversation 构建顺序需回归验证 cache hit。
3. **压缩正确性**：状态机化时保留 Layer1/Layer2 决策逻辑与 `compact_boundary` 持久化，避免 resume 语义漂移。
4. **逐步迁移**：巨型类拆分不要一次完成，先抽出"无业务副作用"的纯工具类（如格式化、token 估算），再迁移有状态组件。

### 工程补充来源：`study-notes/sourcebook/05-核心源码逐文件精读/05-01-入口配置与消息模型.md`

本卷覆盖启动链路、YAML 配置对象和会话消息数据结构。阅读这组文件的价值在于：Agent 并不是凭空出现的，它必须先被入口装配；模型也不能直接接收任意 Java 对象，而要由 `Message`、`ThinkingBlock`、`ToolUseBlock` 和 `ToolResultBlock` 组成稳定的跨供应商表示。

> 路径均相对于 `jimi-code` 根目录。以下“源码事实”来自当前文件；“原理”解释 Java/SnakeYAML/TUI4J 的机制；“建议”不代表项目已经实现。

## 1. 启动文件 `src/main/java/com/jimicode/JimiCode.java`

文件 package 是 `com.jimicode`，唯一 public 类型是 `JimiCode`。它导入 `AppConfig`、`ConfigLoader`、`PermissionMode`、`JimiCodeModel`、TUI4J 的 `Program`、`List` 和 JDK 内部的 `sun.misc.Signal`。入口没有把 LLM SDK、工具类或 MCP 类直接 import 进来，这是一种装配边界：入口只关心配置、权限模式、UI 和进程生命周期，具体能力由 `JimiCodeModel` 再组装。

### 1.2 `main(String[] args)` 的逐步控制流

第一步确定配置路径。`args.length > 0` 时取第一个参数，即 `java -jar target/jimicode.jar D:\\demo\\config.yaml` 的显式配置；否则读取环境变量 `JimiCode_CONFIG`，空字符串视为未提供。注意 README 示例使用的目录名是 `.jimicode`，而 `ConfigLoader` 实际查找的是大小写为 `.JimiCode` 的目录，Windows 通常不敏感，Linux/macOS 会区分大小写，学习时必须以源码为准。

第二步调用 `ConfigLoader.load(configPath)`。它声明抛出受检异常 `ConfigLoader.ConfigException`，入口捕获后把错误写入 `System.err`，调用 `System.exit(1)`，并用 `return` 让静态分析器知道后续不会继续。这里没有把 YAML 解析栈、API Key 或完整配置打印出来，避免秘密泄漏。

第三步把字符串权限模式转换为枚举：`PermissionMode.fromConfig(config.getPermissionMode())`。这一步在配置读取之后、TUI 创建之前发生，因此非法的 `permission_mode` 不会让用户进入一个半初始化界面。`fromConfig` 抛出的 `IllegalArgumentException` 与 YAML 的 `ConfigException` 分开捕获，但用户看到的都是 `Configuration error` 前缀。

第四步构造 `JimiCodeModel`。MCP 和 hooks 可能为 null，因此入口用 `List.of()` 归一化为空列表；providers 不做同样的替换，因为 `ConfigLoader.validate` 已保证至少一个 provider。构造器参数把依赖显式传入，便于测试时注入假的 provider 或空 MCP 配置。

第五步创建 TUI4J `Program`，随后 `model.setProgram(program)` 形成反向引用。反向引用不是循环调用；它允许 Model 在收到 Agent 事件、退出命令或对话框结果时向 TUI 发送命令。创建后打印 ANSI `\033[?25l` 隐藏光标，提升流式输出体验。

第六步启动虚拟线程，延迟 500 毫秒重新注册 SIGINT。TUI4J 默认可能把 Ctrl+C 解释为立即退出；这里把信号转交给 `model.handleSigint()`，使模型在生成中停止当前回答、空闲时再退出。`sun.misc.Signal` 属于 JDK 内部 API，编译时可能给出警告，跨平台行为也不是 Java 标准库长期承诺的一部分。

第七步 `program.run()` 阻塞直到 UI 结束。`finally` 必定执行，重新显示光标并 flush stdout。即使构造 UI 后发生运行时异常，也不会把用户终端留在隐藏光标状态，这是一个典型的资源清理模式。

启动期副作用包括读取配置文件、读取环境变量、创建终端 UI、注册操作系统信号。模型和工具的网络/文件副作用不在 `main` 线程直接发生。虚拟线程只负责延迟安装 signal handler；真正的 Agent 虚拟线程由 `Agent.run` 创建。配置失败不会创建 Program，模型运行期异常则由事件队列转成 UI 可见错误。

在 `ConfigLoader.load` 返回处断点，传入不存在路径，观察 `System.exit` 前的错误；再把 `permission_mode` 改为 `unknown`，观察第二个 catch。启动一个可用配置后，在 `program.run` 与 `finally` 打断点按 Ctrl+C，确认光标恢复。Linux 上可分别测试 `.JimiCode` 和 `.jimicode`，验证大小写差异。

## 2. `config/AppConfig.java`

`AppConfig` 是 SnakeYAML 的根绑定对象，字段只有 `providers`、`permissionMode`、`mcpServers`、`hooks`。它没有构造器逻辑、校验注解或业务方法，全部是 JavaBean getter/setter。SnakeYAML 的 `Constructor(AppConfig.class, loaderOptions)` 需要公开的无参构造器（编译器会生成），并通过 setter 写入 YAML 值。

`providers` 是 `List<ProviderConfig>`，缺失时保持 null，最终由 `ConfigLoader.validate` 拒绝；`permissionMode` 是字符串而不是枚举，原因是配置层要允许大小写、连字符和下划线的用户写法，真正的枚举转换延迟到入口。`mcpServers` 和 `hooks` 允许 null，入口与 `JimiCodeModel` 将 null 视为空列表。

这里的“简单 DTO”仍有重要边界：getter 返回原始可变列表，调用者可以改变配置对象；配置加载完成后项目没有做深拷贝。如果在生产服务中需要热加载，应使用不可变快照或复制列表，避免一个线程遍历 provider 时另一个线程替换列表。

## 3. `config/ProviderConfig.java`

字段 `name`、`protocol`、`baseUrl`、`model`、`apiKey`、`thinking`、`contextWindow`、`maxOutputTokens` 对应 YAML 的 snake_case 属性；`fetchedContextWindow` 是运行期 volatile 缓存，不由 YAML 绑定。`ENV_KEY_MAP` 把三个协议映射到 `ANTHROPIC_API_KEY` 或 `OPENAI_API_KEY`。

方法先判断 `apiKey != null && !apiKey.isEmpty()`，配置文件非空值优先；否则调用私有 `envApiKey()`。环境变量名由 `protocol` 查表，未知协议得到空字符串而不是异常，真正的协议合法性由 `ConfigLoader.validate` 承担。返回空字符串代表调用方应在客户端构造阶段报告认证错误。不要把 API Key 放进日志或 `toString`；该类没有重写 `toString`，这是安全上的好处。

### 3.3 `resolvedContextWindow()` 与 `windowForModel(String)`

上下文窗口有四层优先级：显式 `contextWindow > 0`；Anthropic 客户端探测后调用 `setFetchedContextWindow` 写入的 volatile 值；按模型名称子串匹配的内置表；保守默认值。`windowForModel` 先匹配 `1m`/`-1m`，再匹配 `gpt-4.1`、`gpt-4o`、`gpt-4-turbo`、`o1/o3/o4`、`gpt-3.5`、`claude`，最后返回 128000。顺序很关键，例如 `gpt-4.1-1m` 必须命中百万窗口，而不是先命中通用 GPT 值。

缓存字段使用 `volatile`，保证客户端构造线程写入后，Agent 线程读取时至少看到最新引用；它不保证复杂更新的原子事务，因为这里只写一个 Integer。模型厂商变更窗口后，内置表可能过时，源码明确建议用 `context_window` 覆盖。

### 3.4 `resolvedMaxOutputTokens()`

显式值大于 0 时直接返回；缺失时 thinking 模式返回 64000，普通模式返回 8192。该值是输出上限，不是上下文窗口；`Agent` 在遇到 `max_tokens` 时还会临时提升客户端上限到自己的天花板。新手常把两者相加当作模型可用 token，正确理解是“输入窗口 + 单轮输出约束”。

## 4. `config/McpServerConfig.java`

这个 Bean 描述 MCP 连接，不执行连接。`name` 用于配置层合并和工具名前缀；`command` 与 `args` 用于 stdio 子进程；`url` 与 `headers` 用于 streamable HTTP；`env` 注入子进程环境。当前类没有互斥校验，`command` 和 `url` 同时提供时由 `McpManager` 的分支决定，学习时应继续跟读而不是假设 YAML 校验已经阻止。

## 5. `config/HookConfig.java`

字段 `id`、`event`、`condition`、`type`、`command`、`message`、`reject` 只是序列化模型。`HookEngine` 将字符串事件转换为枚举，将 type 转为动作并解释条件。配置类保留 `reject` 布尔值意味着“钩子既可以观察，也可以拒绝”，但真正拒绝发生在工具前置阶段，不能只看这个 Bean 判断行为。

`load(String path)` 在显式路径非空时只调用一次 `loadSingleFile` 并校验。默认模式构造 home、cwd 两个 `Path`，按固定顺序遍历三个候选文件。`merged == null` 时直接使用第一层对象；后续层调用 `mergeConfig` 原地更新，因此返回对象就是第一层对象的引用，而不是新对象。

`loadSingleFile` 先 `Files.exists`，再 `Files.readString`。使用 SnakeYAML `LoaderOptions` 和 `Constructor(AppConfig.class, loaderOptions)`，替换 `PropertyUtils` 为内部 `SnakeCasePropertyUtils`。该类的 `getProperty` 将含下划线的键转换成驼峰，例如 `max_output_tokens`→`maxOutputTokens`。解析异常被统一包装为 `ConfigException`，空文档变成新的 `AppConfig`。

providers 只有在 override 列表非空时整体替换；permissionMode 只有非空白时覆盖；MCP 使用 `LinkedHashMap<String,Integer>` 按 name 做替换或追加，保留原顺序；hooks 只要 override 列表非 null 就追加，即使它是空列表也不会清除已有 hooks。这个差异是一个常见坑：YAML `hooks: []` 不等于“清空此前钩子”。

校验 provider 列表非空、元素非 null、name/protocol/base_url/model 非空，并把协议限制为 `anthropic`、`openai`、`openai-compat`。API Key 不在此处校验，因为它可以来自环境变量；模型客户端构造时才知道最终 key。`ConfigException` 是受检异常，使启动调用者必须显式处理配置错误。

问“为什么不用 Spring `@ConfigurationProperties`”：因为这是独立 CLI，不启动 Spring 容器；SnakeYAML 直接绑定更轻量。问“多层配置如何保证可预测”：固定顺序、字段级规则、MCP 按名称合并，且不把空列表一律解释为删除。生产建议是把 merge 规则写成表格测试，使用秘密管理器替代明文 `api_key`，并在加载后生成不可变配置快照。

### `conversation/Message.java`

`Message` 保存 `role`、`content` 和三个可选列表：thinking blocks、tool uses、tool results。构造器只设置 role/content，列表由 setter 后置填充。Anthropic 需要 thinking signature 和 tool-use id，OpenAI 需要 function call/output 对应关系，所以不能把所有内容压成一个字符串。当前列表可能为 null，适配器必须按 null 安全处理；生产建议在构造时使用空不可变列表，减少 NPE。

`ThinkingBlock(String thinking, String signature)` 保存推理文本及供应商签名；签名不可随意修改，否则 Anthropic 续写可能拒绝。`ToolUseBlock(String toolUseId, String toolName, Map<String,Object> arguments)` 记录模型发出的调用；id 是后续结果配对的主键。`ToolResultBlock(String toolUseId, String content, boolean isError)` 记录工具回传和错误标志。record 自动生成访问器、equals、hashCode、toString，适合事件和序列化，但 Map 仍可能可变。

### `ConversationManager.java`

内部 `history` 是 `ArrayList`，所有追加方法都按 Agent Loop 的消息顺序写入。`addUserMessage`/`addAssistantMessage` 创建单纯文本；`addAssistantFull` 同时写 thinking 和 tool uses；`addToolResultsMessage` 用 role=`user` 的消息承载结果，这是供应商 API 的兼容表示，并不意味着真实用户输入了工具文本。

`injectLongTermMemory` 用 `ltmInjected` 保证只注入一次，把 instructions、memory、当天日期包装成 `<system-reminder>`，插入列表头部。空 instructions 与空 memories 都不会触发日期注入。`addSystemReminder` 每次追加一个 user-role 包装消息，计划模式和 deferred tool 提示都走此通道。

`getMessages` 返回 `List.copyOf` 的不可变视图，供 LLM 适配器安全读取；`getMessagesMutable` 暴露内部列表，压缩器需要原地重排时使用，调用者必须谨慎。`truncateTo(index)` 只在 index 合法且小于 size 时删除 index 及之后的消息；传入等于 size 或更大值是 no-op，传入负数也是 no-op。

```mermaid
sequenceDiagram
    participant U as 用户
    participant M as JimiCode.main
    participant C as ConfigLoader
    participant T as JimiCodeModel
    participant A as Agent
    participant V as ConversationManager
    U->>M: java -jar + 配置路径
    M->>C: load(path)
    C-->>M: AppConfig
    M->>T: providers/mcp/hooks/permission
    T->>A: 创建并启动
    A->>V: addUserMessage
    A->>V: injectLongTermMemory(一次)
    A-->>T: AgentEvent 队列
```

1. 写一个只读单元测试，给 `ConfigLoader` 三层临时 YAML，验证 provider 替换、MCP 同名覆盖、Hook 追加。
2. 构造一条 assistant tool-use 消息和一条 tool result，验证两个 id 相同且顺序为 assistant→user(result)。
3. 连续调用 `injectLongTermMemory` 两次，确认只出现一条 system-reminder。
4. 在 Linux 上分别创建 `.JimiCode` 与 `.jimicode`，解释哪个能被当前源码发现。
5. 设计一个不可变 `AppConfigSnapshot`，说明它如何解决 getter 暴露可变列表的问题。

入口文件解决生命周期，配置文件解决外部化装配，消息模型解决跨模型协议的内部中间表示。三者共同决定 Agent 后续能否稳定运行：配置错误必须在 UI 前失败；消息必须保留 tool id、thinking signature 和错误位；列表修改与线程边界要有明确约束。读完本卷后，再进入 `05-02-Agent与LLM` 才不会把 Agent Loop 误解成一个没有状态的 while 循环。

`JimiCode.java` 的 `java.util.List` 只用于 null 列表归一化；如果误删，编译器会提示 `List.of()` 无法解析。`sun.misc.Signal` 不是标准业务 API，依赖 JDK 的 `jdk.unsupported` 模块；Shade 打包时它不会被第三方替换。TUI4J `Program` 和 `JimiCodeModel` 的关系是外部框架接口，Program 调用 Model.init/update/view，Model 不能依赖具体终端实现之外的业务类。

配置包的 SnakeYAML import 可以分为解析器（Yaml）、构造器（Constructor）、安全选项（LoaderOptions）和属性映射（Property/PropertyUtils）。这四类依赖组合在一起才完成 snake_case 绑定；只使用 `new Yaml().loadAs` 会绕过自定义 PropertyUtils，`base_url` 可能无法写入 `baseUrl`。Java NIO 的 `Files`/`Path` 负责读取，不负责文件权限提示；IOException 被 ConfigException 包装时应保留 path。

会话包没有 Jackson import，因为 ConversationManager 只维护内存对象；序列化职责放在 SessionManager 和 LLM 适配器。这个分层让 ConversationManager 可以在不引入 JSON 库的情况下被单测，代价是恢复时必须由外部重建消息结构。

|文件|方法|必须验证的边界|
|---|---|---|
|JimiCode|`main`|无参数、显式路径、环境变量、配置异常、非法权限、SIGINT、finally|
|AppConfig|每个 setter|null 列表、SnakeYAML 绑定、外部修改列表后的可见性|
|ProviderConfig|`resolvedApiKey`|配置 key 优先、环境变量 fallback、未知协议为空|
|ProviderConfig|`windowForModel`|null、1m、gpt-4.1、claude、未知模型|
|ProviderConfig|`resolvedMaxOutputTokens`|显式正值、thinking 默认、普通默认|
|ConfigLoader|`load`|候选层顺序、空文件、单层显式路径、无 provider|
|ConfigLoader|`mergeConfig`|provider 整体替换、MCP 同名替换、hook 空列表不清空|
|ConversationManager|`injectLongTermMemory`|两次调用只注入一次、只有日期不应单独注入|
|ConversationManager|`truncateTo`|负数、0、size-1、size、超大值|
|Message|setter|thinking/tool list null 与空列表在各客户端的差异|

假设用户层有 provider `anthropic`、MCP `docs`、Hook `h1`；项目层有 provider `openai`、MCP 同名 `docs` 和新 MCP `db`、空 `hooks: []`；本地层只有 permission_mode。最终 providers 是项目层的 openai 列表而非两者拼接；MCP docs 使用项目层定义、db 被追加且顺序为 docs→db；hooks 仍保留 h1，因为空列表只触发 `override.getHooks()!=null` 的追加；permission_mode 使用本地值。这个例子应写进测试而非依赖口头记忆。

`ConfigLoaderValidationTest` 对应 `validate` 的 provider 缺字段/协议分支；`ProviderConfigApiKeyTest` 对应配置优先和环境 fallback；`ProviderConfigContextWindowTest` 对应窗口表和 fetched cache；`AnthropicClientContextWindowTest` 验证客户端构造后写回 ProviderConfig；`SessionManagerTest`/`ContextCompactorTest` 间接验证消息结构在边界后仍可重建。当前没有专门的 `JimiCode.main` 集成测试，启动失败只能通过手工临时配置验证。

可以只编译配置与消息包，写一个 `ConfigLoaderDemo` 测试：创建临时目录、写三层 YAML、设置 `user.dir`（测试后恢复）、调用 load，再把 providers/mcp/hooks 打印为不含 key 的摘要。然后创建 ConversationManager，加入 user、assistant thinking、assistant tool use、tool result，调用 `getMessages`，用断言检查 role 和 id。这个实验把“外部 YAML→Java Bean→内部消息”闭环起来，不需要网络 API Key，也不会启动 TUI。

配置热刷新时，是否会在 Agent 正在使用 provider 列表时替换 List？多租户时，`System.getProperty("user.home")` 是否会把用户配置误共享？Windows 和 Linux 的 `.JimiCode` 大小写是否统一？API key 是否可能出现在 `ConfigException` 的 YAML parse message？SIGINT 线程是否被重复注册？Program 启动失败时光标是否恢复？Message 的 tool result 是否验证 toolUseId 与最近 assistant tool use 相配对？这些问题不是理论题，均可用断点、临时文件和故障注入复现。

读者应能不看其他章节写出：一个显式配置路径启动命令；一个包含三层覆盖的 YAML；一个 provider key/context window 的解析测试；一个 assistant tool-use/result 消息；一个配置错误退出而光标恢复的解释。若只能复述“ConfigLoader 读取 YAML”，说明还没有掌握本卷的字段级和异常级细节。

### 工程补充来源：`study-notes/sourcebook/05-核心源码逐文件精读/05-02-Agent与LLM适配器.md`

本卷是项目的核心：`Agent` 把会话、提示、模型流和工具执行串成状态机；`LlmClient` 把供应商 SDK 的不同事件统一成 `StreamEvent`。如果只把 Agent 看成“调用一次模型”，就无法解释工具多轮、上下文压缩、权限等待、限流重试和 Ctrl+C。

## 1. 统一事件协议：`llm/StreamEvent.java`

`StreamEvent` 是 sealed interface，当前允许八类 record：

|事件|字段|在 Agent 中的用途|
|---|---|---|
|`TextDelta`|`text`|增量拼接回答并实时发给 TUI|
|`ThinkingDelta`|`text`|显示供应商的思考摘要|
|`ThinkingComplete`|`thinking`,`signature`|保存完整 thinking block|
|`ToolCallStart`|`toolId`,`toolName`|通知 UI 工具开始|
|`ToolCallDelta`|`text`|传递增量 JSON，当前 Agent 不拼接此事件|
|`ToolCallComplete`|id/name/arguments|得到可执行的结构化参数|
|`StreamEnd`|stopReason、token、缓存 token|结束一轮并更新压缩锚点|
|`Error`|`message`|进入重试或终止分支|

`StreamEnd` 有五参数主构造器和三参数便捷构造器，后者把缓存 token 置零。这样兼容没有缓存统计的 provider，同时让 `Agent` 不必用 null 表示“无缓存”。record 的 pattern matching 由 Java 21 的 `switch` 使用，新增事件类型时编译器可帮助发现未处理分支。

接口只有 `stream(ConversationManager,List<Map<String,Object>>)` 和默认空实现 `setMaxOutputTokens`。stream 返回容量为 64 的 `BlockingQueue<StreamEvent>`，调用方先拿到队列，再异步消费；它不会阻塞等待首字节。静态工厂 `create` 按 `ProviderConfig.protocol` 选择 `AnthropicClient`、`OpenAiClient` 或 `OpenAiCompatClient`，未知协议抛 `IllegalArgumentException`。配置层已经校验协议，但工厂仍保留防御性分支，防止测试直接构造非法对象。

为什么用接口而不是让 Agent 直接依赖三个 SDK？这是 Adapter + Dependency Inversion：Agent 只依赖内部事件，供应商变化被限制在 llm 包；单元测试可以提供 fake client，把一串确定性事件放入队列而不消耗真实 API Key。

## 3. `llm/LlmException.java`

基类继承 RuntimeException，子类分别是 `AuthenticationException`、`RateLimitException`（额外保存 `retryAfter`）、`ContextTooLongException` 和 `NetworkException`。这些类型不是为了展示漂亮的错误，而是给 Agent 恢复策略提供分类：认证错误通常立即终止；限流等待后重试；上下文过长触发压缩；网络错误需要考虑请求是否已经被服务端接受，不能盲目重复写操作。

当前异常只在 LLM 客户端线程转成 `StreamEvent.Error`，不直接抛到 TUI 线程。生产建议在错误中保留 provider、HTTP 状态和 request id，但必须过滤 API Key、完整 prompt 和敏感工具参数。

## 4. `llm/ModelResolver.java`

`ALIASES` 把 `haiku`、`sonnet`、`opus` 映射为固定 Claude 模型 ID。`resolve` 对未知值原样返回，便于用户直接填厂商模型名。`supportsAdaptiveThinking` 只对解析后包含 `opus-4-6` 或 `sonnet-4-6` 的模型返回 true；`supportsThinking` 以 `claude` 子串判断。这里是启发式表，不是从 provider 元数据查询，模型重命名会造成误判，配置中的 thinking 仍由 `ProviderConfig` 控制。

## 5. `agent/AgentEvent.java`

Agent 对 UI 的协议同样是 sealed interface。`PermissionRequestEvent` 携带 `CompletableFuture<PermissionResponse>`，工具线程把事件放入队列后等待 UI 完成 future；`AskUserRequestEvent` 携带问题列表和答案 future。`TurnComplete` 表示本轮工具结果已经加入会话，`LoopComplete` 表示整个循环退出；两者不能混用，否则 TUI 可能提前清空最终文本。

事件队列是跨线程边界：生产者是 Agent/StreamingExecutor，消费者是 `JimiCodeModel`。事件对象尽量使用不可变 record，避免 UI 线程读取时被后台修改。`UsageEvent` 的 token 是累计值，不是只读本轮值，适合状态栏显示但不适合直接计算本轮成本。

## 6. `agent/Agent.java`：字段、构造和生命周期

1. **模型与工具**：`client`、`registry`、`protocol`。
2. **预算**：`contextWindow`、`maxOutput`、`maxIterations`、`usageAnchor`。
3. **交互策略**：`checker`、`hookEngine`、`toolNameFilter`、`instructions`、`memoryContent`、`notificationFn`。
4. **恢复状态**：`replacementState`、`recoveryState`、`fileHistory`、`sessionId`、`workDir`。
5. **运行统计**：`lastStreamError`、累计 input/output token。
6. **内部 record**：`ToolCallInfo` 和 `ToolCallResult`。

构造器接收 `LlmClient`、`ToolRegistry`、协议和 `ProviderConfig`，立即解析上下文窗口和输出上限；它不启动线程、不连接 MCP。setter 用于 TUI 装配运行期对象，这也是测试能够替换 PermissionChecker、HookEngine、工作目录的原因。

## 7. `run(ConversationManager)`

`run` 创建容量 64 的 `LinkedBlockingQueue<AgentEvent>`，使用 `Thread.startVirtualThread` 执行 `agentLoop`，立即返回队列。顶层 catch 把未分类异常转成 `ErrorEvent`，避免虚拟线程异常无人消费。注意它没有返回 Future；调用方通过 `LoopComplete` 或 `ErrorEvent` 判断结束，取消需要配合线程中断/模型 client 的关闭策略。

```mermaid
flowchart TD
    A[注入 instructions/memory 一次] --> B[检查 maxIterations/interrupt]
    B --> C[取通知、工具 schema、deferred 提醒]
    C --> D[自动 compaction]
    D --> E[计划模式提醒]
    E --> F[Layer1 ToolResultBudget]
    F --> G[LlmClient.stream]
    G --> H{消费 StreamEvent}
    H -->|text/thinking| I[发送 AgentEvent]
    H -->|tool complete| J[收集 ToolCallInfo]
    H -->|error| K[分类恢复]
    H -->|end| L[保存 usage 与 assistant 消息]
    L --> M{是否有工具?}
    M -->|否| N[LoopComplete]
    M -->|是| O[StreamingExecutor]
    O --> P[追加 ToolResultBlock]
    P --> B
```

`injectLongTermMemory` 只在第一次有效写入；`maxIterations > 0` 且 iteration 超限时发 ErrorEvent 并 break；线程被中断时直接 break。`notificationFn` 的每条消息转成 system reminder。每一轮计算一次 `iterToolSchemas`，并用 `toolNameFilter` 过滤；同一列表同时用于恢复附件和 stream，避免技能在中间修改造成 schema 不一致。

自动压缩调用 `ContextCompactor.manage`。异常被忽略，表示压缩是 best-effort，不应因摘要失败让普通请求立刻崩溃；但这也意味着上下文可能继续膨胀，直到 provider 返回 context error。若消息数量减少，清空 `usageAnchor`，下一轮重新建立真实 token 基线。

Agent 用 `poll(30, TimeUnit.SECONDS)` 读取队列。30 秒没有事件即发 `Stream timeout` 并返回；InterruptedException 恢复中断标志并退出。TextDelta 拼到 `StringBuilder text` 并转为 `AgentEvent.StreamText`；ThinkingComplete 收集到列表；ToolCallComplete 追加内部 record；StreamEnd 保存停止原因、token 和缓存 token。

收到 Error 时设置 `lastStreamError`、发 ErrorEvent，并跳出消费循环。这里的队列 Error 不是 Java 异常，必须看 `lastStreamError` 做分类。

上下文错误且 `contextRetries < 3` 时发 RetryEvent，调用 `forceCompact`，压缩成功后 continue；最多三次防止无限循环。消息含 `rate limit` 时固定等待 5 秒再 continue，没有使用异常对象中的 `retryAfter`，这是当前实现事实。其他错误直接 break，finally 会补发 LoopComplete(0)。生产建议采用指数退避、Retry-After、抖动和可取消 sleep。

首次 `stopReason == max_tokens` 时把 client 输出上限提升到 `MAX_TOKENS_CEILING`，保存已经产生的 assistant 文本和 thinking，然后追加一条用户提醒要求从中断位置继续。后续最多 `MAX_OUTPUT_RECOVERIES` 次以“拆小任务”提示恢复。恢复次数耗尽后落入普通完成，避免模型永远循环。

模型轮次结束后，把文本、thinking、tool calls 组成一个 `Message`。有真实 usage 时建立 `UsageAnchor(baseline, conv.size())`，其中 baseline 是非缓存 input + cacheRead + cacheCreation + output。没有工具调用就做 FileHistory 快照、发 LoopComplete 并结束；有工具调用则创建 `StreamingExecutor`，执行结果转换为 `ToolResultBlock`，若调用 `ExitPlanMode` 则立即发 TurnComplete+LoopComplete，否则只发 TurnComplete，下一轮继续。

如果不是正常 `loopCompleted`，finally 发 `LoopComplete(0)`；这保证 UI 不会永远等待。`putSafe` 捕获队列阻塞时的中断并恢复标志。队列容量有限，UI 停止消费时 Agent 可能阻塞在 put，生产环境需要取消传播和有界丢弃策略。

## 9. `agent/StreamingExecutor.java`

`executeAll` 先按工具类别把调用分为 READ 和 other。多个读调用使用 `Executors.newVirtualThreadPerTaskExecutor()` 并行，按 futures 原列表顺序收集结果；写、命令、未知工具全部串行。并行只改变开始时间，不改变返回顺序。写工具串行是为了避免两个模型调用同时修改同一文件。

找不到工具时立即发错误 ToolResultEvent。然后运行 pre-tool hook；hook reject 不进入权限和执行。权限决策 DENY/ASK/ALLOW 分支中，ASK 发 PermissionRequestEvent 并最多等待五分钟；超时按 DENY；ALLOW_ALWAYS 会从参数提取 command/file_path/pattern 并写入 checker 的持久规则。获得允许后计时执行 `tool.execute`，异常被转为 `ToolResult.error`，结果写入 recovery snapshot，输出超过 `ToolRegistry.MAX_OUTPUT_CHARS` 截断，再发 ToolResultEvent，最后运行 post-tool hook。

只对成功的 `ReadFile` 处理，从参数取 `file_path`，重新 `Files.readString` 保存完整内容和时间。工具输出可能带行号，重新读取保证压缩恢复得到原文件文本。文件在工具返回后消失时静默跳过，模型仍保留原工具输出。

## 10. Anthropic/OpenAI/OpenAI-compatible 的差异

`AnthropicClient` 使用 Messages 流，处理文本增量、thinking 签名、tool input JSON 增量和 usage；构造消息时需要保留连续 role 合并和 cache breakpoint。`OpenAiClient` 使用 Responses API，把 assistant tool use 转为 function call，把 tool result 转为 function call output，并从 `usage.inputTokensDetails.cachedTokens` 拆出缓存 token。`OpenAiCompatClient` 通过 HTTP Chat Completions SSE 读取 `data:` 行，自己拼接增量 tool-call JSON，兼容不支持 reasoning item 的第三方服务。

三个适配器都在虚拟线程中生产容量 64 的队列；任何异常必须转成 `StreamEvent.Error`。这是一种“生产者 SDK → 内部事件总线 → Agent 消费者”的 Adapter/Pipeline 组合。不能把 OpenAI 的 function call id、Anthropic 的 tool_use id 或兼容服务的 index 直接混用，必须在 `ToolUseBlock` 中保存适配器最终确认的字符串 id。

```mermaid
sequenceDiagram
    participant A as Agent
    participant C as ConversationManager
    participant L as LlmClient
    participant X as StreamingExecutor
    participant P as PermissionChecker
    participant T as Tool
    A->>C: ToolResultBudget.apply
    A->>L: stream(messages, schemas)
    L-->>A: TextDelta/ToolCallComplete/StreamEnd
    A->>X: executeAll(calls)
    X->>P: check(tool,args)
    P-->>X: ALLOW/ASK/DENY
    X->>T: execute(args)
    T-->>X: ToolResult
    X-->>A: ToolResultEvent
    A->>C: addToolResultsMessage
    A->>L: 下一轮 stream
```

- `BlockingQueue` 不是消息持久化；进程崩溃后只能靠 Session。
- `StreamEnd` 不是一定代表业务成功，stop reason 可能是 max_tokens。
- 模型 schema 可见不等于工具已授权，权限检查必须在执行前再次发生。
- 读工具并行不意味着所有工具都可并行；写和命令仍串行。
- 压缩失败被忽略不表示上下文无限；下一次 provider 可能拒绝。
- `finally` 的 LoopComplete 是兜底事件，不代表最后一个工具成功。

1. 为什么使用虚拟线程而不是固定线程池？回答：单次网络流和阻塞队列等待适合轻量虚拟线程，但仍要限制并发和外部服务配额。
2. 如何保证工具调用结果按模型给出的顺序返回？读并行 futures 按原列表遍历，写调用按原列表串行。
3. 发生 429 能否无脑重试？不能，模型调用可能已被计费；应使用幂等请求、Retry-After、指数退避和取消。
4. 构造 fake client，让第一次返回工具调用、第二次返回文本，观察 AgentEvent 顺序并断点验证 conversation。
5. 让 fake client 返回三次 context error，确认第三次后退出而不是死循环。
6. 让 ReadFile 成功后删除文件，再触发压缩，观察 recovery snapshot 的 best-effort 行为。

Agent 的难点在状态和边界，不在 while 语法：每一轮都要决定 schema、预算、消息、事件、权限、工具副作用和恢复方式。LLM 适配器的价值是隔离供应商语义，统一事件协议。真正掌握本卷后，读 `ToolResultBudget`、`PermissionChecker` 和 `SessionManager` 才能理解它们为什么嵌在循环的特定位置。

|字段|写入位置|读取位置|生命周期|
|---|---|---|---|
|`client`|构造器|每轮 stream、max_tokens 调整|一次 Agent|
|`registry`|构造器/Model 装配|每轮 schema、Executor|可运行期注册|
|`protocol`|构造器|schema 转换|只读|
|`contextWindow`|ProviderConfig 解析|Compactor|一次 Agent|
|`replacementState`|默认/parent copy|ToolResultBudget|跨 iteration，fork 拷贝|
|`recoveryState`|构造器|ReadFile/Compactor|跨 iteration|
|`usageAnchor`|StreamEnd 后|下一轮 Compactor|压缩后清空|
|`lastStreamError`|Error event|恢复分支|最近一次错误|
|`fileHistory`|TUI setter|terminal turn 快照|session 生命周期|

这张表说明为什么 Agent 不能被复用到另一个 ConversationManager：replacement、recovery、usage anchor 和 sessionId 都绑定一个线程。复用会让第二个用户看到第一个用户的恢复附件，属于严重的数据隔离错误。

对 `AnthropicClient`，重点追 `buildMessages` 的 role 合并、thinking block 序列化、tool input JSON 增量、usage 拆分和错误分类；对 `OpenAiClient`，重点追 `buildInput` 如何把 tool use/output 转成 Responses item、reasoning summary 如何变成 ThinkingComplete；对 `OpenAiCompatClient`，重点追 SSE `data:` 行、`[DONE]`、choices.delta.tool_calls.index 的累积和最终 usage chunk。三个类的 public API 相同，但 private 转换完全不同。

调试时不要只在 `stream` 入口打断点：真正的 bug 常发生在“SDK 事件已经到达、内部队列尚未 put”之间。可以用本地 HTTP mock 返回固定 SSE，依次发送文本、tool call arguments 分片、usage、done；断言 Agent 收到的事件顺序和 arguments Map。JSON 参数解析失败时当前客户端会使用空 Map，工具之后会返回参数缺失错误，模型有机会修正；生产建议把“解析失败”标记成结构化错误而不是静默空参数。

上下文错误重试通常安全，因为请求没有工具副作用；限流重试通常也安全，但可能重复计费；max_tokens 恢复会把已经输出的文本追加到 conversation，再发“继续”消息，不能把上一次 tool call 重复加入，否则会执行两次。工具执行完成后模型流断开，Agent 当前代码可能在下一轮重试而重新生成相同 tool call，必须依赖 tool id、FileHistory、业务幂等 key 或人工确认降低风险。

每次 stream 一个虚拟线程和一个 64 容量队列；读工具多个虚拟线程，写工具串行。队列满时 producer 阻塞，UI 卡死或用户关闭终端都可能造成资源滞留。模型 SDK 的 HTTP connection pool、MCP client、文件 spill 目录和 session JSONL 也会累积。生产优化包括：每 Agent 最大并发 read 数、队列 put 超时、client close、spill TTL、token cost meter 和取消信号传播。

使用 fake LlmClient 构造四轮：第一轮 TextDelta+StreamEnd；第二轮 ToolCallComplete+StreamEnd；第三轮 StreamEnd(stop=max_tokens)；第四轮 Error("context too long")。逐轮记录 ConversationManager 消息数、AgentEvent、usageAnchor 和 contextRetries。验收标准是：普通文本 LoopComplete；有工具调用 TurnComplete 后继续；max_tokens 至少追加 continuation user message；context error 最多三次 forceCompact 后结束。这个实验比直接调用真实模型更适合新手理解状态机。

### 工程补充来源：`study-notes/sourcebook/05-核心源码逐文件精读/05-03-工具权限钩子与MCP.md`

这一卷解释模型“能看见什么、能调用什么、真正被允许做什么”。工具 schema 是提示词层协议；`ToolRegistry` 是发现与适配层；`PermissionChecker` 是安全决策层；`StreamingExecutor` 才在决策后产生文件、进程或网络副作用；MCP 则把外部服务器包装成同一套 Tool 接口。

接口要求每个工具实现 `name()`、`description()`、`category()`、`schema()`、`execute(Map<String,Object>)`。`shouldDefer()` 默认 false，工具可覆写为 true 表示启动时不把完整 schema 发送给模型。这里没有泛型参数校验和异步方法，所有执行都以 Map 输入和 `ToolResult` 输出，优点是与 Anthropic/OpenAI 的 JSON schema 对齐，缺点是编译器不能检查参数名称和类型。

枚举只有 READ、WRITE、COMMAND。它同时影响 `StreamingExecutor` 的并发策略和 `PermissionMode.decide` 的默认授权。分类是粗粒度安全标签，不应被理解为完整能力证明：一个 READ 工具仍可能读取密钥，一个 COMMAND 工具也可能只是查询状态。

record 字段 `output`、`isError`。静态工厂 `success`/`error`（以当前源码为准）让工具少写重复构造。错误结果仍作为正常消息回到模型，Agent 不会因单个工具错误自动抛出 Java 异常；模型可以根据错误继续修正参数。

注册表使用 `ConcurrentHashMap<String,Tool>`，允许 MCP 或 TUI 线程在运行期注册工具；`discoveredTools` 是并发 Set，用于 deferred schema。`register` 按名称覆盖旧工具，意味着同名 MCP 工具会替换内置实现，生产建议在注册时检测冲突并记录来源。

`get` 是 O(1) 查找；`listTools` 返回 `List.copyOf`；`getDeferredToolNames` 只列出 shouldDefer 且尚未发现的工具。`getAllSchemas(protocol)` 遍历工具，跳过未发现的 deferred 工具；OpenAI 需要 `{type:function,name,description,parameters}`，Anthropic/兼容协议直接返回工具自身 schema。这里完成的是协议形状转换，不是 JSON Schema 验证。

`searchDeferred(query,maxResults,protocol)` 以名称或描述的大小写不敏感子串匹配，最多返回 maxResults；`findDeferredByNames` 按名称集合精确选择。两者都没有把命中的工具自动标记 discovered，调用 `ToolSearchTool` 负责写入 discovered 状态。`createDefault` 当前只注册 ReadFile、WriteFile、EditFile、Bash、Glob、Grep；AskUser、Agent、Worktree 等由 `JimiCodeModel` 额外装配。

## 3. `permission/PermissionMode.java`

`fromConfig` 先 trim、小写、移除下划线与连字符，所以 `accept-edits`、`ACCEPT_EDITS`、`acceptedits` 都能转换。未知值抛异常。`decide` 的矩阵：DEFAULT 允许 READ、询问 WRITE/COMMAND；ACCEPT_EDITS 允许 READ/WRITE、询问 COMMAND；PLAN 与 DEFAULT 相同；BYPASS 全部允许。PLAN 的特殊限制不是在此枚举中，而在 Checker 的 Layer 0。

面试中要区分“模式默认决策”和“最终授权”：最终结果还要经过计划例外、safe command、危险命令、路径沙箱、文件规则和 allow-always。

## 4. `permission/PermissionResponse.java`

枚举表示 UI 对 ASK 的回答，当前包含 DENY、ALLOW、ALLOW_ALWAYS。`ALLOW_ALWAYS` 不是永久全局信任，而是把当前工具与内容组合写入 checker 的会话集合，并由 `appendLocalRule` 持久化本地规则（具体是否调用持久化取决于 UI 选择）。

## 5. `PermissionChecker.java`：五层决策链

构造器保存 mode/projectRoot，并从用户级 `~/.JimiCode/permissions.yaml`、项目 `.JimiCode/permissions.yaml`、本地 `.JimiCode/permissions.local.yaml` 加载规则。规则解析为内部 record `PermissionRule(toolName,pattern,effect)`。YAML 每项要求 `rule: "Bash(git *)"` 与 `effect: allow|deny`；坏文件、未知 effect、正则不匹配都静默跳过。

```mermaid
flowchart TD
    A[提取工具内容] --> B{PLAN 例外?}
    B -->|允许 Agent/ToolSearch/Ask/Exit| Z[ALLOW]
    B -->|计划文件写入| Z
    B -->|否| C{Bash safe command?}
    C -->|是| Z
    C -->|否| D{危险模式?}
    D -->|是| X[DENY]
    D -->|否| E{文件路径在 projectRoot 或 /tmp?}
    E -->|否| X
    E -->|是/非路径工具| F[倒序匹配 YAML 规则]
    F -->|ALLOW/DENY| Z/X
    F -->|无匹配| G{allowAlways?}
    G -->|是| Z
    G -->|否| H[PermissionMode 矩阵]
    H -->|ALLOW/ASK/DENY| Z/Y/X
```

Layer 0 允许计划模式的 Agent、ToolSearch、AskUserQuestion、ExitPlanMode；写入 `.JimiCode/plans/` 的 WriteFile/EditFile 也允许。Layer 1 对没有管道、分号、重定向、命令替换的安全命令自动允许。当前安全集合包括 ls/dir/pwd/git status/git diff/java -version 等；删除 Go 后不再包含 Go 命令。Layer 2 对根目录递归删除、mkfs、向设备 dd、chmod 777、fork bomb、curl/wget 管道 shell、写入 `/dev/sd` 拒绝。

Layer 3 只对 ReadFile/WriteFile/EditFile 检查路径；normalize 后要求位于 projectRoot 或 `/tmp`。如果 Path 解析抛异常，当前实现返回 true，这是跨平台容错但有安全风险。Layer 4 倒序匹配文件规则，实现“最后匹配优先”；Layer 4b 检查内存 allow-always；Layer 5 执行模式矩阵。

`appendLocalRule` 创建父目录，读取本地规则，追加 allow，使用 SnakeYAML dump 写回，再重新加载全部规则。写失败静默忽略，调用方看不到持久化错误；生产建议返回结果或记录 warning。`addAllowAlwaysRule` 只改内存 HashSet，线程安全性弱于 ConcurrentHashMap，当前主要由单个工具执行线程调用。

### 5.4 `describeToolAction`

把参数摘要转换成 UI 文案，例如 `Execute: git diff`、`Write: path`。它不执行校验、不截断命令；UI 展示前应避免把超长或秘密参数完整回显。

## 6. `hook/HookEngine.java`

HookEngine 定义 EventName、ActionType、Action、Hook、HookContext、HookResult、PreToolResult。事件包括工具前后、会话/回合等生命周期；条件表达式支持变量解析和简单比较。`runPreToolHooks` 返回 rejected/message，供 StreamingExecutor 在权限前短路；`runHooks` 执行动作。ActionType 的 command 会启动外部进程，message 只输出提醒，reject 影响执行，因此 Hook 本身也属于安全边界。

阅读条件表达式时要注意：这是项目自定义的小语言，不是 JavaScript；未识别变量和异常应看源码的默认布尔值。生产建议使用白名单命令、超时、输出上限和审计，避免把配置文件变成任意代码执行入口。

输入 `file_path`、可选 `offset`/`limit`；读取文本并附行号和截断提示，成功后写入 `FileStateCache`，同时由 StreamingExecutor 再写 RecoveryState。路径校验由 PermissionChecker 和工具自身的 normalize 双重承担。文件不存在、读取权限不足、非 UTF-8 都转为 `ToolResult.error`。实验：先读取后让另一个进程修改，再调用 EditFile，观察 mtime 检测。

输入 `file_path`、`content`。创建父目录并写完整内容；覆盖已有文件前要求 FileStateCache 中存在先前读取且 mtime 未变化，编辑前交给 FileHistory 备份，成功后刷新缓存。首次创建文件没有旧快照。异常不会回滚已创建的父目录，生产建议临时文件写入后原子 move。

输入路径、`old_string`、`new_string`，要求 old_string 在文件中唯一；非唯一、找不到、缓存过期都返回错误。先备份再精确替换，避免模型使用模糊 patch 修改错误位置。练习是构造两个相同 old_string，验证拒绝而不是替换全部。

输入 command、timeout；以 `ProcessBuilder` 启动 shell，合并 stdout/stderr，等待退出并处理超时/中断。PermissionChecker 先做危险检测，但 BashTool 仍应限制工作目录和环境。命令结果包含 exit code，非零通常 `isError=true`。不要把 shell 字符串拼接成新的高权限命令。

### `GlobTool.java` 与 `GrepTool.java`

Glob 在根目录递归匹配 `**`，跳过 `.git`、依赖和虚拟环境目录，排序并限制输出；Grep 编译正则、按文件逐行搜索，支持 include glob，输出 `path:line:content` 并限制字符数。它们属于 READ，多个查询可以并行；正则灾难回溯和符号链接循环是生产风险。

把结构化问题放入 AgentEvent 的 future，阻塞等待 TUI 提交；取消或超时应返回错误。它不是普通命令行 input，因为 TUI 需要在主事件循环显示多问题、多选项对话框。

解析 `select:name1,name2` 的精确语法或关键词，调用 Registry 的 deferred 搜索，命中后标记 discovered 并返回 schema。工具本身通常分类 READ/系统，允许模型按需加载 schema，降低 prompt token。

### `EnterWorktreeTool.java`、`ExitWorktreeTool.java`、`ExitPlanModeTool.java`

三者分别把 WorktreeManager、计划文件和 TUI 回调包装成模型可调用接口。Enter 校验 slug 并创建/恢复 session；ExitWorktree 的 keep/remove 需要检查变更；ExitPlanMode 只有计划文件存在并处于 PLAN 时成功。

## 8. MCP：`mcp/McpManager.java`

构造器按 server name 放入 LinkedHashMap，同名配置后者覆盖。`connectAll` 逐个 `createClient`、initialize、读取 server instructions、listTools；单个失败加入 errors，其他 server 继续。`registerAllTools` 把成功 wrapper 注册到 ToolRegistry 并返回 errors，适合部分可用。

`createClient` 在 command 非空时建立 `StdioClientTransport`，把 args、env（支持 `${VAR}` 替换）传给子进程；url 非空时建立 `HttpClientStreamableHttpTransport`，headers 也支持变量替换；两者都为空抛异常。Windows 上 npx/npm/node/uvx 等命令自动追加 `.cmd`。

`McpToolWrapper.name()` 生成 `mcp__server__tool` 并把非字母数字替换为下划线；category 固定 COMMAND，shouldDefer 固定 true。schema 从 SDK inputSchema 转为内部 `input_schema`；execute 构造 `CallToolRequest`，调用同步 client，合并 TextContent，按 SDK isError 返回 ToolResult。MCP 返回图片或结构化内容时，当前 `extractTextContent` 只保留文本，非文本会丢失，这是已确认限制。

`shutdown` 对 clients 调用 closeGracefully 并清空 map。客户端连接成功但 listTools 失败会落入整个 server error，已建立 client 仍可能留在 map，生产建议失败时显式关闭。MCP 工具是外部网络/进程副作用，schema 不等于安全授权，仍会经过 PermissionChecker；默认 deferred 只降低可见性，不降低危险性。

|阶段|现象|当前处理|学习关注|
|配置|command/url 都空|connectAll errors|部分 server 不影响内置工具|
|initialize|握手超时|记录错误|是否关闭半连接|
|listTools|schema 无效|wrapper 可能调用失败|运行期验证|
|权限|MCP 分类 COMMAND|ASK/拒绝|不能因为 MCP 来自可信服务器而绕过|
|调用|SDK 异常|ToolResult.error|模型可重试但可能重复副作用|
|输出|非文本 content|`(no output)`|当前信息丢失|

1. 为什么 schema 和 permission 必须分离？schema 只是模型可生成的形状，权限是服务端最终信任边界。
2. 为什么读工具并行、写工具串行？读通常无共享可变状态，写会发生冲突；实际仍需考虑两个 ReadFile 后文件被修改。
3. 写一个假的 MCP server command，验证 `ConnectResult.errors` 不会清空内置 Registry。
4. 配置 `${MCP_TOKEN}`，分别设置和不设置环境变量，观察未解析占位符如何进入 header。
5. 添加 `permissions.local.yaml` 的 `Bash(git status)` allow，验证最后匹配和 reload。
6. 设计一个 prompt injection：文件内容要求执行 `rm -rf /`，说明它只能作为 ReadFile 输出，仍要经过工具权限层。

工具系统是从模型到真实世界的“受控执行层”。Registry 管理可见性，Hook 管理策略扩展，PermissionChecker 管理最终授权，Executor 管理并发和副作用，MCP 管理外部能力接入。任何一层缺失都会让“模型想做什么”和“程序实际做什么”混在一起；源码阅读应沿 `Agent → Registry → Hook → Permission → Tool/MCP → Result → Conversation` 这条链逐步打断点。

|工具|类别|核心参数|成功副作用|失败时模型看到的内容|
|---|---|---|---|---|
|ReadFile|READ|file_path,offset,limit|读取文件、更新缓存/恢复状态|文件不存在或编码错误|
|Glob|READ|pattern,path|遍历目录但不修改|匹配为空或越界|
|Grep|READ|pattern,path,include|编译正则并读取文件|正则非法、结果截断|
|WriteFile|WRITE|file_path,content|创建父目录并覆盖文件、历史备份|未先读取、mtime 变化、写失败|
|EditFile|WRITE|file_path,old_string,new_string|唯一字符串替换、历史备份|old_string 不唯一/不存在|
|Bash|COMMAND|command,timeout|启动本地 shell 进程|非零退出、超时、权限拒绝|
|ToolSearch|READ|query|maxResults|标记 deferred 工具 discovered|没有命中 schema|
|AskUserQuestion|READ|questions|阻塞等待用户 future|取消或超时|

schema 的 `required` 只影响模型生成参数；工具实现仍必须对 null、错误类型、空字符串做运行时校验。比如 `WriteFile` 的 content 不是 String 时不能直接 cast 后让 ClassCastException 穿出，应该返回 ToolResult.error 并保留 tool id。

`isSafeCommand` 只接受完整命令或以安全命令加空格开头，并拒绝管道、分号、&&、重定向、命令替换和反引号。这不是完整 shell parser：引号、换行、Unicode 同形字符和 shell alias 仍可能绕过简单字符串判断。因此它适合教学级默认保护，不适合生产级沙箱。生产应在独立容器/沙箱执行，使用 argv 数组、系统调用策略和资源限制。

`isPathAllowed` 只保护三类文件工具，Bash 中的 `cat /etc/passwd` 仍依赖命令策略而非路径沙箱。模型可通过 Bash 读取任意可读文件，说明“工具分类+路径检查”不是完整的数据防泄漏方案。企业改造应让 Bash 使用工作目录、只读 mount、环境白名单，并对输出做 secret scanning。

## 14. Hook 与 Permission 的先后顺序

当前 `StreamingExecutor.executeSingle` 先找工具，再运行 pre-hook，再 PermissionChecker，再 execute，最后 post-hook。这样 Hook 可以实现业务拒绝（例如禁止修改锁定文件），但 Hook command 自身并不经过 PermissionChecker，必须在 HookEngine 内部限制动作。若把 Permission 放在 Hook 之后，恶意 hook 可能成为权限绕过；若把 post-hook 放在结果截断之前，审计看到的输出可能与模型看到的不一致。调试时分别在四个边界断点，记录时间戳和 tool id。

项目直接使用 MCP SDK 的 `McpClient.sync(transport)`、`initialize()`、`getServerInstructions()`、`listTools()`、`callTool()` 和 `closeGracefully()`。这些是已确认的外部 API；SDK 内部的 JSON-RPC 重试、线程模型和连接池没有在仓库中实现，若 source jar 不可用，不应把推断写成事实。MCP server instructions 当前只被收集为 ServerInfo，是否拼入系统 Prompt 取决于 Model 装配代码，学习时要在 `initializeProvider` 断点确认。

1. 让 `ReadFile` 返回 60,000 字符，确认工具原始输出、AgentEvent 截断输出、ToolResultBudget spill 三种视图的差异。
2. 用 `Bash` 参数 `git status; echo x`，确认 safe command 不允许自动通过并进入 ASK。
3. 用 `WriteFile` 写 `..\\outside.txt`，确认 normalize 后被路径规则拒绝。
4. 创建 pre-hook reject，确认 PermissionChecker 没被调用；创建 post-hook 抛异常，确认工具结果已经回传且 UI 不崩溃。
5. 配置一个不可执行 MCP command 和一个正常 HTTP server，确认 ConnectResult 同时返回成功工具与错误列表。

工具执行后如果 JVM 立即崩溃，哪些状态可能落盘？FileHistory 备份可能已经存在，Session 可能尚未 append，MCP 外部副作用无法回滚，Worktree 可能留下未提交分支。权限 ASK 等待期间用户关闭终端，future 是否完成？当前需要依赖 TUI 的 quit/interrupt 路径，单独测试 Executor 才能发现死等。MCP schema 被标记 deferred 后，模型是否知道如何调用 ToolSearch？Agent 每轮会注入 deferred names reminder，若 reminder 被压缩掉则需要 recovery attachment 重新列工具。

### 工程补充来源：`study-notes/sourcebook/05-核心源码逐文件精读/05-04-会话记忆压缩与文件历史.md`

长对话 Agent 的两个不同问题经常被初学者混在一起：会话持久化解决“进程重启后还能不能继续”，上下文压缩解决“本次请求还能不能塞进模型窗口”。Memory 解决“跨会话留下哪些稳定事实”，FileHistory 解决“工具改错后能不能回退”。当前源码把四者放在不同包中，职责边界非常清楚。

## 1. `compact/RecoveryState.java`

RecoveryState 用两个 HashMap 保存最近文件读取和 Skill 调用。`recordFileRead(path,content)`、`recordSkillInvocation(name,body)` 都在 null/空键时 no-op，并在 synchronized(lock) 中覆盖同路径/同名称旧记录；因此“最新快照赢”，不会无限累积同一个文件的每次版本。`snapshotFiles(limit)` 复制 values 后按 timestamp 倒序，limit>0 时截断；`snapshotSkills()` 返回全部倒序。工具执行器可由多个虚拟线程调用，所以锁保护写与复制，排序发生在锁外。

它只保存内存状态，不直接写磁盘。压缩后由 `ContextCompactor.buildRecoveryAttachment` 把快照重新附加到 summary。文件内容可能包含密钥，生产建议对路径和内容脱敏、设置大小预算、按租户隔离。

## 2. `compact/ContextCompactor.java`

Layer 1 `offloadAndSnip` 不调用模型：单个 tool result 超过 50,000 字符写入 `.JimiCode/tool_results`，消息只保留可回读提示；旧轮次结果裁剪为前 2,000 字符。Layer 2 在 token 使用量接近窗口时调用 LLM 生成摘要，保留最近若干原始消息和 recovery 附件。

核心常量包括摘要输出预留 20,000、自动安全边距 13,000、硬阻断边距 3,000、最近 10 轮、连续失败 3 次熔断、最近保留目标 10,000 token（上限 40,000，至少 5 条消息）。这些数字是当前实现事实，不是模型厂商通用标准。

`manage` 先无条件执行 Layer 1，然后用 `currentTokens(messages,anchor)` 估算使用量。低于 `effectiveWindow - 13000` 直接返回 Layer 1 提示；超过硬线 `effectiveWindow - 3000` 强制 `forceCompact`；处于软线区间时，如果 tracking 未熔断，尝试 autoCompact，成功 reset，异常 recordFailure。三次失败后只继续 Layer 1，不再自动调用摘要，直到硬线强制压缩。

`UsageAnchor` 由 Agent 在 provider 返回真实 usage 后创建，baseline 是输入+缓存读+缓存写+输出，anchorCount 是当时消息数量。后续只估算 anchor 之后追加的消息，避免缓存命中时用原始字符重复计算。冷启动或压缩后 anchor 为 null，回退到全量估算。

`estimateTokens` 对每条 content 按长度/3.5 加 4，tool use 参数序列化后额外加 50，tool result 加长度/3.5+10，thinking 按长度/3.5。它不是 tokenizer，中文、代码、JSON 的真实比例会变化；因此显式上下文窗口配置和 provider usage 更可靠。

### 2.4 Layer 1 offload/snipping

遍历可变消息，找到 tool result。单结果大于 50,000 且有 workDir 就写 spill 文件，替换内容为 `[Result of N chars saved to path]`；一条消息聚合超过 200,000 时对较大结果重复 offload。早于最近边界的结果若未处理，则保留前 2,000 字符并追加 `... [snipped]`。已处理内容通过提示前缀识别，避免每轮重复写 spill。

写 spill 使用创建目录、文件名清洗和异常回退（当前实现以 tool id 生成路径）；工具结果不会因为写 spill 失败而删除，只会继续原内容。生产建议限制磁盘总量、定时清理、路径使用随机 ID 防碰撞，并在提示中提供受权限保护的读取工具而不是让模型直接访问任意文件路径。

压缩器选择旧前缀发送给 client，要求输出 `<summary>`，解析失败则使用原始返回或错误；保留最近完整 tool-use/result 配对，避免只保留 function call 而丢结果。摘要后附 recovery attachment：最多 5 个文件、每文件约 5,000 token，Skill 总预算 25,000，并列出当前可用工具 schema。

如果提供 workDir+sessionId，压缩器调用 SessionManager.appendCompactBoundary 写入 summary 和 keep tail；子 Agent/一次性调用传 null，不写主会话边界。压缩是状态替换而非删除审计：旧 JSONL 仍保留，resume 根据最后 boundary 重建有效上下文。

## 3. `session/SessionManager.java`

`SessionMessage(role,type,content,timestamp)` 对应 JSONL 一行；`type=compact_boundary` 表示特殊记录。`KeepMessage` 是 boundary 中保留的消息；`CompactBoundary(summary,keep)` 是 JSON blob；`BoundaryScan(boundary,after,found)` 表示扫描结果；`SessionInfo` 用于列表显示。

`appendMessage(workDir,sessionId,role,content,type)` 创建 `.JimiCode/sessions`，Jackson 写一个 JSON 对象并追加换行，写入失败 best-effort。`loadSession` 逐行解析，坏行跳过，缺 content 的行跳过，返回已成功读取部分。JSONL 的优点是追加和人工审计容易，缺点是并发追加可能交错，当前没有跨进程锁。

`appendCompactBoundary` 把 summary/keep 序列化为 content，type 写 boundary。`findLastCompactBoundary` 只取最后一个 boundary，blob 损坏时 found=false，调用方回退完整重放，优先保证不丢对话。`rebuildConversation` 在有 boundary 时构造“压缩说明 user 消息 + keep tail + boundary 后新消息”，旧前缀只留在文件中；无 boundary 时按原序 replay。

`replay` 将 assistant role 调用 `addAssistantMessage`，其他 role 调用 `addUserMessage`，因此原始 tool-use/thinking 结构在简单恢复路径会丢失；当前 boundary keep 也只保存 role/content。真正需要供应商级 tool call 恢复时必须扩展 JSON schema，这是一个重要限制。

`listSessions` 扫描 `.jsonl`，读取首个 user 消息、文件大小、修改时间和 `currentGitBranch(workDir)`，按时间倒序。Git 分支通过 `ProcessBuilder("git","-C",workDir,"rev-parse",...)` 获取，失败返回空字符串。`formatRelativeTime`、`formatFileSize`、`matchesSearch` 是纯展示辅助，不影响会话语义。

## 4. `history/HistoryStore.java`

这是输入框历史，不是 Agent 会话。默认文件 `~/.JimiCode/prompt_history.jsonl`，上限 200 条；`load` 逐行读 `{text,ts}`，坏行和空文本跳过；`append` 忽略空值、抑制连续重复、超过上限从头删除，并每次重写整个文件；`getEntries` 返回不可变副本。它把每条记录时间写成当前时间，因此重启后无法保留原始时间语义，但 UI 只需要文本导航。

## 5. `filehistory/FileHistory.java`

构造器把 sessionDir 设为 `<baseDir>/.JimiCode/file-history/<sessionId>` 并创建目录。`trackedFiles` 记录绝对路径→版本号，`snapshots` 使用 CopyOnWriteArrayList，最多 100 个。`backupName` 对路径取 SHA-256 前 8 字节并附 `@vN`，避免路径包含斜杠造成非法文件名。

同步方法把路径转绝对路径，版本加一，读取当前文件写入备份；新文件读取失败时不写备份但仍记录版本。这里的语义是“编辑前备份”，所以调用者必须在实际写入前调用。写备份失败被忽略，意味着回退可能不可用，生产建议把备份失败升级为工具错误而不是继续修改。

### 5.3 `makeSnapshot(msgIndex,userText)`

遍历 trackedFiles，为新文件补做当前状态备份，组装 `Snapshot` 并追加；超过 100 删除最旧。Agent 在无工具的 terminal turn 会调用它，因此一个用户回合的文件变更被归档到消息索引和摘要文本。

### 5.4 `rewind(snapshotIndex)`

校验索引后，读取每个 backup 与当前文件比较；不同则写回并把路径加入 changed。备份缺失时把当前文件删除，表示目标快照时文件不存在。然后截断目标之后的 snapshot，重置 tracked version。该操作改变工作区但不创建新的 Git commit，必须经过用户确认；目录创建使用 `Path.of(filePath).getParent()`，极端相对路径可能得到 null，生产应加保护。

纯函数工具，根据文件修改时间计算年龄天数、freshness 文案和过期提醒。没有全局状态，适合单元测试；时间基于 `Instant.now()`，测试应注入 clock 才能避免抖动。

扫描 memory 目录，解析 Markdown frontmatter 的 name、description、type，生成轻量 `MemoryHeader`，启动阶段只读头不加载正文。坏 frontmatter 跳过或使用默认值（以当前方法为准），目的是控制 prompt 体积。

定义 `SelectorFn`，让调用方注入一次轻量模型选择器；`RelevantMemory(path,mtimeMs)` 是选择结果。流程是把未展示 memory 的 manifest 和用户问题发给 selector，解析 JSON 文件名，最多选择有限条，过滤已展示和普通参考噪声；selector 失败返回空列表而不是阻塞主 Agent。

管理用户级与项目级目录，创建 `MEMORY.md` 索引，列出、清除和格式化 `/memory` 命令内容。`MemoryEntry(content,timestamp,type)` 是对单文件的展示对象。它不是向量数据库，也没有 embedding；相关性依赖 `MemoryRecall` 的 selector，生产可替换为向量检索但要保留租户隔离和过期策略。

```mermaid
flowchart LR
    A[ConversationManager] --> B[ContextCompactor]
    B --> C[ToolResult spill]
    B --> D[SessionManager boundary]
    A --> E[FileHistory snapshot]
    F[MemoryManager] --> A
    G[HistoryStore 输入历史] -.不进入模型.-> H[TUI 输入框]
```

|故障|当前行为|可能损失|改进实验|
|---|---|---|---|
|spill 写失败|保留原结果|继续超限|填满只读目录观察日志|
|摘要模型超时|tracking 失败，最多三次|上下文继续增长|fake client 连续 Error|
|boundary 损坏|完整 replay|token 可能再次超限|手工破坏 JSONL|
|session 追加失败|内存继续|重启丢记录|让目录只读|
|备份失败|继续编辑|无法 rewind|模拟权限错误|
|memory selector 失败|不注入相关 memory|回答缺背景|fake selector 抛异常|

1. 为什么要同时保留摘要和最近尾部？摘要保留长期目标与决策，尾部保留精确参数和未完成工具配对。
2. 为什么 usage anchor 能改善压缩？缓存命中时 provider 报告的新增输入少，按全量字符估算会重复计算；anchor 只估算新增消息。
3. JSONL 的原子性问题如何解决？写临时文件+原子替换或文件锁；当前 SessionManager 主要是 best-effort append。
4. FileHistory 与 Git worktree 有何不同？前者是文件级版本快照和回退，后者是目录/分支隔离；两者可以叠加。
5. Memory 为什么不等于 Conversation？Memory 跨会话、可筛选、需要生命周期治理；Conversation 是当前线程的事实序列。

- 写测试验证 `currentTokens` 在 anchorCount 后只估算追加消息。
- 生成 60,000 字符 ToolResult，确认 spill 提示包含原长度和路径。
- 触发一次 compact boundary，重启后 `rebuildConversation` 不重放旧前缀。
- 修改文件后调用 `rewind`，确认只恢复目标 snapshot 中 tracked 文件。
- 创建过期 memory，验证扫描头和年龄文案，确认主 Prompt 没有加载全部正文。

会话是可审计日志，压缩是上下文状态机，Memory 是跨线程知识索引，FileHistory 是可回退副作用记录。它们共同构成“模型可能犯错但系统还能恢复”的基础。源码阅读要特别关注 best-effort 的地方：当前实现为了不中断交互会吞掉若干 IOException/摘要错误，生产系统需要把这些静默降级变成指标、告警和可见状态。

```json
{"role":"user","content":"读取 README","ts":1710000000}
```

`role` 决定 replay 时调用 addUserMessage 还是 addAssistantMessage；`type` 缺失或为空代表普通消息；`content` 是已经格式化的文本，不包含独立的 thinking/tool-use JSON；`ts` 是 epoch second，列表界面用文件 mtime 计算相对时间。压缩边界行的 content 是另一个 JSON 字符串，里面有 summary 和 keep 数组，不能把它当普通 user 内容展示给模型。

当前 replay 会跳过 boundary 行，但不会验证普通消息是否有连续的 user/assistant 合法交替。手工编辑 JSONL 可以制造两个 assistant 连续或空 user；provider 适配器可能接受，也可能返回 400。生产恢复应增加 schema 校验、tool id 配对和最大行大小。

## 13. `ContextCompactor` 的阈值推导

假设 contextWindow=200000、maxOutput=8192：reserve=min(8192,20000)=8192，effectiveWindow=191808，自动阈值=178808，硬阈值=188808。也就是说不是简单的 80% 固定比例；源码保留 `AUTOCOMPACT_THRESHOLD=0.80` 只是旧常量，实时决策走绝对 token 公式。若 maxOutput=64000，reserve=20000，effectiveWindow=180000，自动阈值=167000。读者可用测试验证 maxOutput 越大，留给历史的空间越小。

`UsageAnchor` 例子：provider 报告 baseline=12000、anchorCount=8，之后 ConversationManager 追加两条共估算 3000 token 的 tool result，则 currentTokens=15000；若压缩让消息数从 10 变成 4，anchorCount=8 已失效，Agent 会清空 anchor，下一轮重新估算。这个细节防止“压缩后错误地把旧 baseline 加在新消息上”。

RecoveryState 保存文件原文和 Skill SOP，压缩器把它们放入模型可见的 attachment。文件原文可能包含密码、证书和个人数据，Skill body 也可能包含“执行某命令”的提示。当前实现没有脱敏或签名，依赖工具权限和模型遵循系统提醒。生产建议：读取时做 secret detector；RecoveryState 只保留 hash+必要片段；附件标记来源和时间；跨租户绝不复用同一个 Agent 实例。

## 15. FileHistory 与 EditFile 的竞态

理想时序是 ReadFile(t0,mtime=1)→EditFile 校验缓存→FileHistory.trackEdit 备份旧字节→写新字节→刷新 cache→Agent terminal turn makeSnapshot。若外部进程在校验后、备份前修改，备份的可能已经不是模型看到的版本；若备份写失败，当前代码仍继续写，rewind 无法保证。可以通过在 trackEdit 前再次比较 mtime、使用文件锁或原子 rename 改进。实验：后台 PowerShell 循环写文件，主线程执行 EditFile，观察是否能检测到全部竞态。

创建四种 frontmatter：`type: user`、`feedback`、`project`、`reference`，分别放入用户目录和项目目录；调用 MemoryScanner 只读 headers，调用 MemoryRecall 让 fake selector 选择其中两条，再调用 MemoryManager 格式化 Prompt。把一个文件改成超过最大行数/大小，观察是否被限制；调用 clear 后确认只删除索引允许的 Markdown，不删除源码。验收标准是用户记忆不会出现在另一个项目的 project scope 中。

```mermaid
flowchart TD
    A[启动 resume] --> B{session 文件存在?}
    B -->|否| C[新会话]
    B -->|是| D[逐行解析 JSONL]
    D --> E{最后 boundary 可解析?}
    E -->|否| F[完整 replay + 可能再次压缩]
    E -->|是| G[summary + keep tail + after]
    G --> H{文件当前是否变化?}
    H -->|是| I[ReadFile 重新确认]
    H -->|否| J[继续 Agent]
```

如果 session 读取成功但 worktree 路径不存在，SessionManager 仍可能重建文本；真正执行工具时 Permission/Worktree 层才失败。恢复流程应把“会话可读”和“工作目录可用”分别提示用户。

- Session JSONL：文件锁、临时文件、fsync、最大行/最大文件、加密或租户目录。
- Compaction：真实 tokenizer、摘要质量评估、失败指标、可取消请求、摘要版本号。
- Spill：随机文件名、目录配额、TTL、权限 600、工具专用读取接口。
- FileHistory：备份失败阻断写、原子写、磁盘配额、回退审计。
- Memory：frontmatter schema、敏感信息检测、向量检索隔离、删除合规。
- Resume：provider/model 兼容性检查、tool id 结构恢复、工作目录/分支校验。

“压缩是不是丢数据？”回答：当前代码保留旧 JSONL 审计和最近 tail，模型上下文只看到摘要+tail；是否可完整恢复取决于 boundary 中保存的结构，当前 replay 对 thinking/tool-use 不是完全无损。 “FileHistory 能否替代 Git？”回答：不能，它是 session 级文件快照，Git 负责团队版本、分支和提交；FileHistory 甚至可能恢复未提交的临时状态。 “Memory selector 为什么允许失败静默？”回答：记忆是增强信息，主请求应可继续；但生产需指标和用户可见告警。

最后做一次“从零恢复”验收：删除内存中的 Agent，只保留 `.JimiCode/sessions/<id>.jsonl`、`file-history/<id>` 和 memory 目录，重新启动程序选择 resume。记录哪些内容被恢复、哪些必须重新读取、哪些外部 MCP 连接需要重新初始化；把结果写成当前事实与生产建议两列，这比只看单元测试更能暴露边界。

### 工程补充来源：`study-notes/sourcebook/05-核心源码逐文件精读/05-05-SubAgent团队与Worktree.md`

这一卷回答一个问题：当一个 Agent 把工作交给另一个 Agent 时，哪些状态共享，哪些状态隔离，任务结果如何回来，代码改动如何避免互相覆盖。当前 Java 实现既支持同进程虚拟线程，也保留 tmux/iTerm 后端；它不是分布式微服务，通信主要是内存对象和 `.JimiCode/teams` 文件。

### `subagent/SubAgentSpec.java`

record 字段：`name`、`description`、允许工具列表、禁用工具列表、系统提示覆盖、最大轮数、模型选择。内置 `GENERAL_PURPOSE` 允许所有工具、200 轮；`PLAN` 禁止 EditFile/WriteFile、15 轮，附带只读架构师 Prompt；`EXPLORE` 禁止写工具、30 轮、模型别名 haiku。record 本身不可变，但 List 引用若来自外部可变，生产建议复制为 `List.copyOf`。

`loadAll(projectRoot)` 先加载内置，再用户目录 `~/.JimiCode/agents/*.md`，最后项目 `.JimiCode/agents/*.md`，后层同名覆盖前层。`parseAgentFile` 支持 `---` YAML frontmatter 和 Markdown 正文：name/description 必填，model 只能空、inherit、haiku、sonnet、opus，tools/disallowedTools 为字符串列表，maxTurns 为数字。坏文件静默跳过，启动不会因一个用户 Agent 文档失败而退出；代价是用户可能不知道为什么 Agent 没出现。

ToolFilter 根据 SubAgentSpec 复制或筛选 Registry：先应用 disallowedTools，再应用 tools 白名单，后台/teammate/fork 还有额外限制，MCP 工具按名称前缀过滤。它的职责是减少 child 能见到的 schema，不是代替父级 PermissionChecker；子 Agent 执行时仍然要检查权限。

## 3. `AgentTool.java`：路由入口

AgentTool 实现 Tool，schema 包含 description、prompt、subagent_type、model、run_in_background、isolation 等参数。执行时先解析 spec，再按参数选择：teammate 路由到 TeamManager；fork 创建独立 conversation；async 交给 SubAgentTaskManager；普通 sync 在当前进程阻塞等待结果。互斥参数会被拒绝，例如 isolation=worktree 与已存在的 child worktree 冲突。

父 Agent 的 `ContentReplacementState` 可以 clone 给 fork，使共享历史中的 tool_use_id 具有稳定的 prompt cache 前缀；但 fork 的文件、session、工作目录可以独立。禁止 fork 套 fork 是为了避免指数级任务树和权限传播失控。

## 4. `SubAgentTaskManager.java`

TaskStatus 为 PENDING、RUNNING、COMPLETED、FAILED、CANCELLED。`createTask` 在 synchronized 下生成 `task_1` 风格 id；`setRunning` 保存虚拟线程；完成/失败/取消都追加 TaskNotification；`drainNotifications` 一次取出并清空，Agent 主循环通过 `notificationFn` 将通知注入 system reminder。

方法先创建任务，再 `Thread.startVirtualThread`：按 spec 过滤 registry，创建 child Agent，设置 maxIterations，必要时复制 replacement state，构造 ConversationManager，注入系统 Prompt 和用户 prompt，然后消费 child AgentEvent 队列。Text 累加输出；ErrorEvent 失败；LoopComplete 成功；60 秒没有事件则失败 Timeout。父线程立即得到 task id，不等待 child 完成。

取消只 interrupt 记录的线程；如果 child 正在阻塞 SDK 网络调用，中断能否终止取决于 SDK 和 HTTP client，不能假设一定立即停止。任务状态由 synchronized 方法更新，但 `TaskEntry` 字段也标 volatile，兼顾列表读取和线程可见性。

### `teams/TeamManager.java`

TeamMode 只有 IN_PROCESS、TMUX；Team 包含 name、mode、members 和邮箱根目录；Member 保存 name、Agent、ConversationManager、线程/进度信息。TeamManager 负责 create/delete、add/remove member、send message、聚合进度、shutdown。IN_PROCESS 直接启动 TeammateRunner；TMUX/iTerm 由 SpawnDispatcher 创建外部 pane。

`SendMessageTool` 校验 sender、recipient 和消息内容，把消息写入目标邮箱；`TeamCreateTool` 创建团队并选择 mode；`TeamDeleteTool` 关闭成员并清理目录。它们是 Lead 的协调工具，不是普通写文件工具，因此 Team 存在时 `Coordinator` 可以收窄 Lead 的工具集，避免 Lead 与 teammate 同时编辑同一文件。

### `AgentNameRegistry.java`

工具类负责校验/生成 teammate 名称，避免路径和邮箱文件名注入。名称必须可预测、可安全拼入 `.JimiCode/teams` 子目录；生产建议同一团队内做大小写不敏感唯一约束。

`MailMessage` 包含 from/text/timestamp/read；邮箱以 JSON 文件保存。发送、读取未读、标记已读通过文件锁/临时文件原子替换（具体锁范围以方法为准），避免两个 teammate 同时写丢消息。坏 JSON 行通常跳过，读取到的消息仍可继续消费。

把 Team 任务保存为 JSON，状态、owner、description、result 等字段供多个进程观察。更新采用读—改—写，必须配合锁；并发下最后写入可能覆盖前一更新。它与 SubAgentTaskManager 的区别是跨进程持久化，不依赖父进程内存。

静态工具集中定义 Lead 在团队运行期间可见的协调工具白名单，并解释为什么隐藏 Bash/WriteFile：协调阶段 Lead 应通过 SendMessage 指挥成员，而不是直接造成共享工作区冲突。

长期循环先注入团队身份，启动 Agent 执行任务，消费事件更新 `TeammateProgress`；完成一轮后进入 idle，轮询邮箱等待新 prompt 或 shutdown。`WaitResult` 区分新任务与关闭。轮询周期、邮箱读取失败和 Agent timeout 是运行稳定性的关键，生产建议使用文件系统 watch 或真正消息队列替代忙轮询。

维护最近工具、工具次数、token 用量、spinner 文案和状态。`ToolActivity` 根据工具名生成用户友好描述，例如 ReadFile→读取文件、Bash→执行命令。它是展示状态，不应作为权限来源。

校验成员名、工作目录和 TeamMode，生成带 `--teammate` 参数的命令并按 mode 调用进程内、tmux 或 iTerm。shell quote 是安全重点：不能把用户提供的 team name 直接拼进 shell 字符串。

### `TmuxBackend.java` 与 `ITermBackend.java`

Tmux 使用 `tmux split-window` 创建 pane、`send-keys C-c` 中断、`kill-pane` 清理；iTerm 使用 macOS `osascript`/iTerm CLI。Windows/Linux 没有 iTerm，Dispatcher 应回退进程内。外部 pane 的 stdout 不直接进入父 JVM，主要通过邮箱和状态文件同步。

校验 slug 不能为空、包含路径穿越或危险字符，并提供 flatten 把层级转换成分支安全形式。它是 worktree 路径安全的第一道防线，不能只依赖 Git 自己报错。

构造器保存 projectRoot、symlinkDirs 和 staleCutoffHours（<=0 使用 24 小时）。`create` 默认路径 `<root>/.JimiCode/worktrees/<branch>`，执行 `git worktree add -B branch path`，再调用 `PostCreationSetup` 复制设置、hooks、symlink 和 `.worktreeinclude`，最后登记 WorktreeInfo。`remove` 只允许移除内存登记的分支；`list` 优先解析 `git worktree list --porcelain`，失败回退内存 map；`cleanupStale` 按创建时间强制删除过期登记；`detectChanges` 在指定目录执行 `git diff --stat`，30 秒超时杀进程。

当前 WorktreeInfo 的 createdAt 在解析 porcelain 时使用当前时间，而不是 Git 创建时间，因此 stale 只对本进程内 create 的对象可靠。`removeAll` best-effort，不检查未提交变更；调用前必须得到用户确认。

这是给一次性 SubAgent 的无全局 session 版本。`create(slug,repoRoot,symlinkDirs)` 验证 slug，路径使用 flatten，分支为 `worktree-<slug>`；目录存在时 bump mtime 并快速恢复 HEAD；否则执行 `git worktree add -B branch path HEAD`，设置 GIT_TERMINAL_PROMPT=0/GIT_ASKPASS 空值防止等待凭据，完成后做 PostCreationSetup。`remove` 强制移除 worktree 再删除分支，失败只记录 fine 日志；`buildNotice` 生成告知 child 翻译父路径的系统提示。

`WorktreeSession` record 保存当前 worktree path/branch/root/head；`WorktreeSessionStore` 用 Jackson 写读 session JSON，缺失返回空；`WorktreeChanges` 统计 changedFiles、commits 和未推送提交；`StaleCleanup` 扫描 `.JimiCode/worktrees`，检查命名、年龄、session、脏状态和未推送 commit，任何不确定都跳过；`PostCreationSetup` 复制 `.JimiCode` 设置、共享 Git hooks、创建 symlink，并读取 `.worktreeinclude`；`AgentWorktree` 与 `WorktreeManager` 的区别是前者面向 child 的轻量静态 API，后者面向 TUI 全局管理。

```mermaid
flowchart LR
    L[Lead Agent] --> AT[AgentTool]
    AT -->|sync| C1[子 Agent]
    AT -->|background| SM[SubAgentTaskManager]
    AT -->|teammate| TM[TeamManager]
    TM --> MB[FileMailBox]
    TM --> TR[TeammateRunner]
    AT --> AW[AgentWorktree]
    AW --> G[Git branch/worktree]
    SM --> N[父 Agent notifications]
```

1. 在 Git 仓库根运行 `AgentWorktree.create("demo",root,List.of())`，记录 path/branch/head。
2. 在 child path 修改文件但不提交，调用 `WorktreeChanges`/`git diff --stat`。
3. 父目录检查文件不应变化，证明工作目录隔离。
4. `remove` 后确认 branch 删除；如果有未提交改动，解释 `--force` 的数据风险。
5. 模拟进程中断，留下目录，再调用 `StaleCleanup`，观察它何时保守跳过。

1. SubAgent 与线程池任务有什么区别？SubAgent 有独立消息上下文、工具过滤、最大轮数和事件协议；线程只是执行载体。
2. 为什么 fork 要复制 replacement state？共享历史中的替换决策必须保持字节稳定，避免 prompt cache 前缀变化。
3. Worktree 能隔离 MCP/数据库副作用吗？不能，它只隔离 Git 文件；外部服务仍需租户、session 和幂等设计。
4. 如何避免 Team 两个成员改同一文件？角色分工、Lead 协调白名单、worktree 隔离、提交后合并冲突检测；当前实现不提供自动语义合并。
5. 为什么 tmux 后端不是默认？依赖终端能力和操作系统，失败时应回退同进程以减少部署条件。

SubAgent 解决任务分解，Team 解决持续协作，TaskManager 解决后台生命周期，Mailbox/SharedTaskStore 解决跨线程或跨进程通信，Worktree 解决文件隔离。它们的共同难点是取消、重复、崩溃恢复和副作用边界。源码阅读时要沿“父事件→子上下文→工具过滤→工作目录→结果通知→父会话”逐段跟踪，而不是只看 AgentTool 的 schema。

|入口|关键参数|必须拒绝/处理的异常|
|---|---|---|
|AgentTool|description、prompt、agent type|prompt 为空、未知 agent、fork 套 fork、isolation 冲突|
|AgentLoader|frontmatter path|缺 name/description、非法 model、坏 YAML、同名覆盖|
|SubAgentTaskManager|client/registry/spec/prompt|队列 60s timeout、interrupt、child ErrorEvent|
|TeamCreate|team name/mode|非法名称、重复团队、不可用 tmux/iTerm|
|SendMessage|sender/recipient/text|不存在成员、空文本、邮箱写失败|
|WorktreeManager.create|branch,targetDir|路径穿越、Git 非仓库、分支锁、创建后 setup 失败|
|AgentWorktree.remove|path/branch/gitRoot|root 为空、git remove 失败、分支删除失败|

同步 child 通常只收到父 prompt 和 spec.systemPromptOverride；它不会自动看到父 Conversation 的所有历史，除非 fork 分支显式 seed。后台 child 完成后只把最终文本通过 TaskNotification 注入父 Agent，工具中间事件不进入父上下文。这样可以节省 token，却可能让父 Agent 不知道 child 为什么失败。生产可保留结构化摘要：已读文件、修改文件、命令、测试结果、失败原因和建议，而不是只传一段自然语言。

Team teammate 是另一种边界：它有长期 ConversationManager 和邮箱，可接收多轮消息；Lead 看到的是协调工具和聚合进度。邮箱消息不一定与模型 token 顺序一致，TeammateRunner 每轮 idle 后才读取下一封，多个消息可能合并。需要强一致工作流时应给消息编号、ack、幂等 key 和 dead-letter 文件。

取消父 Agent 不一定取消 child：`SubAgentTaskManager.cancelTask` 只 interrupt child thread，Team 外部 pane 需要 TmuxBackend/ITermBackend 发送 Ctrl+C。若 child 正在执行 Bash，ProcessBuilder 子进程可能继续运行；如果 child 正在 MCP call，SDK timeout 才会结束。重复点击“重试”可能创建两个 task；当前 task id 由 manager 生成但没有业务去重。生产建议为每个任务设置 parent cancellation token、子进程树 kill、MCP request id 和幂等任务键。

`PostCreationSetup` 的职责不是业务合并，而是让 child 环境可运行：复制用户/项目 `.JimiCode` 只读设置、共享 Git hooks、按配置 symlink 大目录（例如 node_modules）、读取 `.worktreeinclude` 复制被 gitignore 的配置。symlink 在 Windows 可能需要开发者权限，失败时应让 child 继续但把 warning 传回。复制本地 config 时必须排除 api_key，当前实现以文件模式和 ignore 规则为准，使用者应实际检查。

在临时仓库创建 `main`，调用 `AgentWorktree.create("feature/ui",root,List.of())`。child 修改 A.java 并提交，父目录修改同一 A.java；使用 `WorktreeChanges` 统计 changedFiles/commits，再尝试 cherry-pick child branch。记录无冲突、文本冲突、删除冲突三种结果。实验说明 Worktree 只减少工作区互相覆盖，不会自动解决语义冲突，也不会自动把 child commit 合并到主分支。

Windows 默认没有 tmux/iTerm，IN_PROCESS 是最稳妥路径；macOS 可选择 iTerm，但需要允许 AppleScript；Linux 常见 tmux，需确认命令在 PATH。`SpawnDispatcher` 应先检测能力再选择，不能把 OS name 当作唯一依据；例如远程 SSH 终端可能没有 `$TERM` 或 pane。部署文档应列出 fallback 和如何查看 teammate 日志。

Agent spec 的 systemPromptOverride 是用户可写自然语言，不能赋予它超出 PermissionChecker 的权限；disallowedTools 只能收窄不能放宽。Worktree branch/slug 必须阻止 `../`、控制字符和超长名；邮箱文件名必须防路径穿越；TeamTools 的 SendMessage 不能让模型伪造 sender；tmux/iTerm 命令必须 argv 化并 shell quote。MCP/数据库/网络副作用不因 worktree 隔离而消失。

“为什么子 Agent 不是简单递归调用 Agent.run？”因为需要独立 token/iteration、工具过滤、取消、进度和结果生命周期。 “同进程 Team 与多进程 Team 如何选？”同进程共享内存简单、延迟低；多进程故障隔离和终端可观察性更好，但要处理邮箱、序列化和进程回收。 “Worktree stale cleanup 为什么保守？”误删未提交代码代价高，无法确认 session、脏状态或未推送 commit 时宁可泄漏目录。

完成一个最小 child Agent：使用 fake LlmClient，只允许 ReadFile，maxTurns=2，返回结果后父 Agent 收到一条通知；再加 Worktree 隔离、FileMailBox 消息和取消测试。验收必须记录 task status、child worktree、父 Conversation 的新增文本和任何未清理资源，证明你理解的不只是接口，而是完整生命周期。

把这个实验运行两次并比较：第一次 child 正常完成，第二次在 Git 命令中途终止。若第二次留下 worktree、branch、邮箱或 task 状态，分别说明应由哪个类负责清理；不要用 `removeAll` 盲目删除整个仓库的 worktree。

### 工程补充来源：`study-notes/sourcebook/05-核心源码逐文件精读/05-06-命令任务Prompt与工具结果稳定性.md`

这一卷覆盖不直接调用模型、但决定模型工作方式的“控制面”：斜杠命令改变会话或模式，PromptBuilder 生成系统约束，Skill 提供可复用 SOP，TaskList 持久化计划，toolresult 包保证大输出裁剪后仍能保持 prompt cache 前缀稳定。

## 1. `command/Command.java` 与 `CommandContext.java`

Command 是不可变 record，字段 name、description、aliases、CommandType、hidden。`matches` 对 canonical name 和 alias 做大小写敏感精确匹配；所以 `/Help` 不会自动等于 `/help`。CommandType：LOCAL 返回文字，LOCAL_UI 触发 TUI 动作，PROMPT 生成发给 Agent 的提示。`CommandContext` record 集中提供 args、workDir、model、permissionMode、toolCount、tokenCount、memoryList/clear、sessionInfo、skillList、mcpInfo，全部以 Supplier/Runnable 延迟读取，避免注册命令时捕获过期状态。

## 2. `command/CommandRegistry.java`

构造器注册 help、mcp、clear、compact、status、memory、plan、session、permission、resume、rewind、skills、review 等命令。`parse` 处理开头 `/`、命令名和参数，`complete` 为输入前缀返回候选，`execute` 根据 CommandType 分派。LOCAL 命令读取 Context 的 supplier；LOCAL_UI 通过返回的特殊结果让 `JimiCodeModel` 清空/切换；PROMPT 返回一段用户消息进入 Agent Loop。

重点看 `/compact`：它不是直接在 CommandRegistry 中压缩，而是生成控制信号，由 TUI 调用 `ContextCompactor.forceCompact`，这样所有压缩都使用同一套 RecoveryState/session boundary。`/permission` 改变 checker mode；`/rewind` 调 FileHistory；命令自身不绕过 PermissionChecker。

### `prompt/PromptSections.java`

静态方法按主题生成 Section：身份、系统规则、做任务、执行动作、工具使用、语气、输出效率、环境。每个 Section 返回 name/priority/content，不直接读取外部文件。内容中包含“先读取再编辑”“危险操作前询问”“不要泄露秘密”等自然语言约束；这些是模型提示，不是安全控制，最终仍由 PermissionChecker 决策。

### `prompt/PromptBuilder.java`

`add` 把 section 放入列表并返回 this，形成轻量 Builder；`build` 按 priority 升序排序，trim 空内容，用双换行拼接。重复调用 build 会继续使用已排序列表，新增 section 后再次排序。`detectEnvironment(model)` 读取 user.dir、os.name、arch、SHELL，默认 shell 为 bash；通过 `git rev-parse` 判断 Git 仓库和分支，异常静默为非 Git。`buildSystemPrompt` 按固定顺序添加八个 PromptSections，skillSection 以 priority=90 追加。

检测环境会启动外部 git 进程，属于启动副作用；在恶意目录中运行应设置超时和输出上限。系统 Prompt 可能包含当前路径和分支名，发送给外部模型前应确认不包含凭据。

### `prompt/PlanModePrompt.java` 与 `plan/PlanFile.java`

PlanFile 为当前工作目录创建 `.JimiCode/plans` 下的计划文件，维护当前路径，提供 exists/read/write/reset；路径名通常由 session id/时间生成。PlanModePrompt 根据是否已有文件和 iteration 生成完整或稀疏提醒，要求先探索、列关键文件、等待 ExitPlanMode。计划模式允许写计划文件而不允许业务文件，这个例外在 PermissionChecker Layer 0 中实现。

### `SkillHost.java` 与 `SkillForkHost.java`

SkillHost 是解耦接口，暴露 activateSkill、setToolFilter、toolRegistry、recordSkillInvocation 等能力，避免 skill 包直接依赖 Agent；SkillForkHost 扩展 `runSubAgent`，让 fork Skill 使用独立 child。

SkillMeta 包含 name、description、allowedTools、model、mode 等元数据；Skill 同时保存 promptBody、sourceDir、bodyLoaded。Catalog 扫描用户/项目 `.JimiCode/skills`，启动时读取 frontmatter，调用时才加载正文，减少初始 prompt。解析支持 `skill.yaml + prompt.md` 或单文件 `SKILL.md`；同名项目 skill 覆盖用户 skill。坏目录跳过并记录可诊断信息（当前实现对部分异常静默）。

根据 mode 选择 none/inline/fork：inline 把 SOP 注入当前 Agent 并通过 ToolFilter 缩小工具；fork 复制 seed messages 后运行 child，最终只返回摘要。缺失 allowed tool 时应 fail-fast，避免模型得到一个声明可用但 Registry 没有的工具。

Task 字段 id、subject、description、activeForm、status、owner、blocks、blockedBy、metadata；JSON 位于 `.JimiCode/tasks/<listId>.json`。Status 只有 pending、in_progress、completed，`fromString` 对未知值抛异常。所有 public CRUD synchronized，每次 mutation 重新 load 再 save，意图是多进程看到较新文件，但没有跨进程锁。

`create` 使用 SecureRandom 生成 t+8 hex id，复制 metadata；`get` 按 id 查找；`list` 隐藏 metadata 中 `_internal` 的任务；`update` 支持 subject/description/activeForm/status/owner、追加 blocks/blockedBy、metadata 合并；status=deleted 物理删除。读取损坏文件返回空列表，保存失败抛 RuntimeException，因此“读坏静默恢复、写坏显式失败”是不对称策略。

把 TaskList 包装成 TaskCreate、TaskGet、TaskList、TaskUpdate 四个 Tool，每个提供模型 schema 和格式化输出。输入状态和依赖列表必须是字符串/数组，工具层负责把 Map 转换成 TaskList 的更新 map。任务工具一般分类 WRITE，但不直接修改业务文件；仍应在模式矩阵中询问。

### `ContentReplacementState.java`

每个 conversation thread 保存 seenIds 和 replacements。第一次遇到 tool_use_id 时决定是否替换；之后 decision 永久冻结。`copy` 给 fork 创建独立副本，继承父历史决策但不会写回父集合。Invariant 是 replacements 的 key 必须是 seenIds 子集。

### `ContentReplacementRecord.java`、`ApplyResult.java`

record 描述 kind、toolUseId、replacement，静态工厂区分 tool result/spill 生命周期；ApplyResult 同时返回重建后的 API Conversation 和本轮新记录，保证调用方知道需要持久化哪些决策。

### `ReplacementRecordsIO.java`

以 JSONL 追加/加载记录；缺失文件返回空，坏行跳过。它是 best-effort 审计，不是唯一真相，进程内 `ContentReplacementState` 才是当前 turn 的权威。生产建议使用文件锁和 fsync，避免两个 Agent 同时追加交错。

### `ContentReplacementLifecycle.java`

集中定义 replacement 的生命周期枚举/分类，区分 fresh、frozen、replaced、spill 等状态。它本身不读取文件，供 `ToolResultBudget` 和审计代码统一字符串，避免不同模块拼写不一致。

`apply(conv,sessionDir,state)` 永不修改输入 ConversationManager，先遍历每条 tool result：已有 replacement 直接复用；seen 但未替换则保留原文；首次遇到的超大结果尝试 spill，失败则冻结原文；消息聚合超过 200,000 时按长度降序继续 spill；剩余 fresh 冻结为原文。之后 `snipStale` 对超过最近 10 轮的旧结果替换为 `[Stale output snipped: N chars]`。最后通过 ConversationManager 的公开追加 API 重建新对象。

设计重点是“冻结”：如果每一轮都重新根据文件是否存在、当前长度做决定，提示词前缀会变化，供应商缓存命中率下降，且模型看到的历史不稳定。冻结也意味着 spill 文件删除后不会自动恢复原文，必须由 ReadFile 工具显式失败。

`JimiCodeModel` 接收斜杠命令，构造 CommandContext，处理 LOCAL/LOCAL_UI/PROMPT；收到 AgentEvent.CompactEvent 显示压缩提示；收到 PermissionRequestEvent 打开 Permission dialog 并完成 future；收到 AskUserRequestEvent 打开 AskUserDialog；收到 LoopComplete 才把最终 assistant 文本提交 Session。命令、Prompt 和事件都在 UI 线程串行更新，Agent 线程不直接修改 Swing/终端组件。

1. 新增一个 `/tokens` LOCAL 命令，使用 tokenCount supplier 显示累计 input/output。
2. 写一个 inline Skill，限制只读工具，验证 Agent 仍无法通过 Bash 变更文件。
3. 创建两个相同 tool_use_id 的结果，运行 ToolResultBudget 两次，比较 replacement 字节完全一致。
4. 让 TaskList JSON 损坏，验证 list 返回空；再让目录只读，验证 save 抛异常并由 ToolResult 转成错误。
5. 解释 Prompt 中“不要执行危险命令”和 PermissionChecker 的 DANGEROUS_PATTERNS 哪一个更可信，为什么。

- Builder 与 Template Method 在 PromptBuilder 中如何体现？Builder 负责组合可选 Section；PromptSections 提供固定模板方法式的片段生产。
- 为什么 ToolResultBudget 返回新 Conversation 而不是原地修改？压缩决策是 API 视图，不应破坏 UI/审计中的原始结果；新对象还便于纯函数式测试。
- Skill allowed_tools 能否当权限？不能，Skill 是模型可见性/工作流限制，Permission 是不可绕过的执行授权。
- TaskList 的 synchronized 能否解决多进程竞争？不能，只保护同 JVM；跨进程要文件锁或数据库事务。

命令、Prompt、Skill 和 Task 是“控制面”；ToolResultBudget 是“上下文稳定性面”。它们不直接产生模型答案，却决定模型在什么规则、什么工具集合、什么历史视图中行动。理解这些文件后，再读 2,000 多行的 `JimiCodeModel` 会更容易，因为 UI 只是把这些控制面映射到键盘和屏幕。

### 工程补充来源：`study-notes/sourcebook/05-核心源码逐文件精读/05-07-TUI事件循环与对话框.md`

JimiCode 没有 Web Controller；`JimiCodeModel` 就是它的交互入口。它实现 TUI4J/Bubble-Tea 风格的 Model：`init` 返回初始命令，`update(Message)` 消费键盘、定时器和 Agent 事件，`view()` 把当前状态渲染为字符串。理解 Model 的状态转换，等价于理解 Web 项目的 Controller→Service→View 链路。

枚举只有 PROVIDER_SELECT、CHAT、RESUME。状态决定 `update` 把 KeyPress 转发给哪个 handler，也决定 `view` 显示 provider 选择、聊天或会话恢复界面。状态转换必须通过 Model 的 update 线程完成，后台 Agent 不能直接修改 state。

## 2. `tui/ChatMessage.java`

ChatMessage 是展示模型，不是发送给 provider 的 Message。role 可以是 user、assistant、system、error、tool、tool_collapsed、tool_group、sub_agent、thinking；`ToolBlockInfo` 保存工具名、参数、输出、错误、耗时、折叠和 loading；`SubAgentBlockState` 保存描述、类型、工具列表、完成状态、工具数和总耗时。把展示对象与 conversation 对象分离，避免 UI 为了折叠一条工具结果而改变模型上下文。

## 3. `JimiCodeModel.java` 的装配字段

Model 持有 provider 列表、MCP 配置、Hook 配置、当前 PermissionMode、Program 引用、AppState、选中 provider、Agent、ConversationManager、ToolRegistry、Session/History/FileHistory、CommandRegistry、Memory/Skill/Team/SubAgent 管理器、输入缓冲、slash/at 补全列表、Permission/AskUser/Plan/Rewind 对话框和消息列表。这个类很大不是偶然：它是 CLI 的 composition root，同时承担 TUI 状态和模块生命周期；生产重构可拆成 ProviderController、ChatController、DialogController，但教学代码把装配集中便于跟踪。

两个构造器最终调用完整版本，保存 providers/mcpServers/hooks/permissionMode，创建 `CommandRegistry`、`HistoryStore`、`TeamManager` 等。`parseEventName`/`parseActionType` 把配置字符串转 HookEngine 枚举，未知值通常回退默认或忽略。`setProgram` 保存 TUI 引用；`handleSigint` 根据 Agent 是否运行决定取消当前请求或发 quit 命令。

加载 HistoryStore，设置 provider selection 初始光标，返回定时轮询 Agent/邮箱的 Command。它不创建模型 client，真正初始化发生在用户选择 provider 后。

先处理 WindowSize、KeyPress、AgentEventMessage、MailboxPollMessage、定时器等类型，再根据 AppState 转发。键盘在 provider 选择状态调用 `handleProviderSelectKey`；CHAT 状态调用 `handleChatKey`；RESUME 调用 `handleResumeKey`；对话框激活时优先由对应 handler 消费。返回 `UpdateResult`，可附带下一条 TUI Command（例如 50ms 后继续轮询）。

按 AppState 调 `viewProviderSelect`/`viewChat`/`viewResume`，再叠加 active Permission、AskUser、PlanApproval、Rewind dialog。视图只读状态，不执行工具。渲染超长 Markdown 时应注意 terminal width，当前 MarkdownRenderer 主要做 ANSI 样式而非真正中文换行。

## 6. Provider 选择到 Agent 初始化链路

`handleProviderSelectKey` 处理上下移动和 Enter；Enter 调 `initializeProvider`。初始化步骤通常包括：选择 ProviderConfig；创建 LlmClient；创建默认 ToolRegistry；连接 MCP 并注册 wrappers；创建 PermissionChecker；装载 Skill/Agent/Team/Task 工具；构建 PromptBuilder 环境 Prompt；创建 ConversationManager、SessionManager、FileHistory；实例化 Agent 并设置 workDir/sessionId/filters/hooks/memory。任何认证或 MCP 错误应转为系统消息而不是让 TUI 线程崩溃。

## 7. Chat 输入处理：`handleChatKey`

普通字符追加到输入缓冲；Enter 调 `sendUserMessage`；Escape 在流式阶段调用 `handleSigint`；上下键读取 HistoryStore；Tab 选择 slash 或 @ 文件补全；Shift+Tab 切换 PermissionMode。`updateSlashMenu` 在输入以 `/` 开头时调用 CommandRegistry，`updateAtMenu` 扫描工作目录并跳过 `.git`、target、node_modules 等目录。补全只改变 UI 输入，不直接读取模型。

## 8. `sendUserMessage()` 完整链路

1. trim 输入，空字符串返回。
2. 如果以 `/` 开头，解析 Command；LOCAL/LOCAL_UI 直接执行，PROMPT 变成 Agent 的 user message。
3. 普通消息追加 ChatMessage(user) 和 ConversationManager.addUserMessage。
4. 通过 MemoryRecall 异步预取相关 memory，避免阻塞首屏；完成后注入下一轮上下文。
5. 触发 `SESSION_START`/`TURN_START` hooks。
6. 调 `agent.run(conversation)`，保存返回的 AgentEvent 队列，启动 50ms AgentEventMessage 轮询。
7. UI 继续可响应 Ctrl+C、权限对话框和 AskUser。

## 9. `handleAgentEvents()`

它从 Agent 队列 poll 事件并更新展示：StreamText 追加当前 assistant stream buffer；ThinkingText/Complete 维护 thinking block；ToolUseEvent 创建 loading ToolBlock；ToolResultEvent 填充 output、elapsed、error；UsageEvent 更新 token；CompactEvent 显示压缩；RetryEvent 显示等待；PermissionRequestEvent 激活权限对话框并完成 future；AskUserRequestEvent 激活 AskUserDialog；LoopComplete 调 `savePartialResponse`/`commitToolBlocks`，写 Session，并触发 TURN_END/MEMORY extraction。

事件处理必须保持顺序。尤其不能在最终 no-tool turn 先发送 TurnComplete 再清空 stream buffer，因为 Agent 注释明确指出这会导致 LoopComplete 看到空缓冲而无法持久化最终回复。

`commitToolBlocks` 把连续工具调用合并为可折叠 group；`renderToolBlockText` 生成单个工具摘要；`renderToolGroupSummary` 显示“读取 3 个文件，耗时 1.2s”；`renderToolBlock` 根据 loading/error/collapsed 使用 Styles。`handleSubAgentProgress` 将后台进度加入队列，`drainSubAgentProgress` 合并到 ChatMessage.SubAgentBlockState；`renderSubAgentBlock` 展开时列出子工具，折叠时只显示总数和耗时。

### `dialog/AskUserDialog.java`

支持 Question(text,header,options,multiSelect) 和 Option(label,description)。状态包括当前问题、光标、选择集合、输入模式和 active。上下键移动，空格选择，多选允许多个答案，Enter 提交当前/全部，Escape 取消。`submitAllAnswers` 完成 future；关闭对话框必须在异常和 Ctrl+C 时也完成，否则 Agent 工具线程会等待五分钟。

### `dialog/PlanApprovalDialog.java`

三项结果 YOLO、MANUAL、FEEDBACK，另有 CANCEL。`activate` 重置 cursor/input；`handleKey` 支持 up/down/enter/shift+tab/backspace/可打印字符；选择 feedback 但输入为空时不关闭。YOLO 会把 PermissionMode 切到 BYPASS，风险极高，当前实现把这一能力作为显式用户选择，而不是模型自行决定。

`render(markdown,width)` 按行处理代码围栏、标题、列表、引用、分隔线和空行；代码块用灰色边框，普通行走 `renderInline`。正则替换粗体、斜体、反引号和 Markdown 链接，链接只显示文字、不创建终端可点击 URL。它不解析嵌套 Markdown、表格或 ANSI 转义，恶意模型输出 ANSI 可能污染终端，生产建议先过滤控制字符。

集中定义品牌紫、灰、绿、红、黄、青等 ANSI256 样式，覆盖 banner、prompt、AI 文本、工具状态、错误、权限模式、provider 选择和 placeholder；静态 helper `cyan/dim/green/red/yellow` 用于内联团队树。样式不是业务状态，禁止用颜色判断权限。

保存运行中动词和过去式转换，随机返回 spinner 文案。随机性只影响展示，不应进入会话或 Prompt，否则同一请求的审计文本会不稳定。

## 13. Resume、Rewind、Memory 与 Hook

`handleResumeKey` 在 RESUME 状态过滤 SessionInfo，Enter 调 SessionManager.rebuildConversation，重新初始化 Agent；`filterResumeSessions` 按 id/firstMessage 搜索；`viewResume` 显示相对时间、分支和大小。`handleRewindKey` 展示 FileHistory snapshots，`executeRewindOption` 调 rewind 并刷新文件状态，不能只清除 UI 消息。

`buildMemorySection` 合并 MemoryManager 的索引和 MemoryRecall 结果；`triggerMemoryExtraction` 启动后台 headless child，完成后向 UI 提示新 memory。`fireHook` 把事件、toolName、args 交给 HookEngine，Hook 错误不应阻塞终端循环。

```mermaid
flowchart LR
    K[键盘/UI线程] --> M[JimiCodeModel.update]
    M -->|Command| P[TUI Program]
    M -->|start| A[Agent虚拟线程]
    A --> Q[AgentEvent BlockingQueue]
    Q --> M
    A --> V[工具虚拟线程]
    V --> Q
    A --> W[MCP/HTTP SDK线程]
    W --> Q
```

只有 UI 线程修改 `ChatMessage` 和对话框状态；后台线程通过 queue/message 回传。`ConcurrentLinkedQueue<SubAgentProgress>`、`CompletableFuture`、`BlockingQueue` 分别承担无锁进度缓存、HITL 回复和有界流事件。

1. 在 `update` 断点输入普通文本，记录 state、inputBuffer。
2. 在 `sendUserMessage` 记录 ConversationManager size、sessionId。
3. 在 `handleAgentEvents` 按事件类型单步，确认 UI 不直接执行工具。
4. 在 Permission dialog 中不点击，观察 Agent 线程等待；五分钟后确认 DENY。
5. 在 LoopComplete 断点检查 stream buffer、ChatMessage 和 session JSONL。
6. 关闭终端模拟 SIGINT，确认 `finally` 光标恢复和 queue 不再增长。

- TUI Model 与 MVC Controller 有何区别？Model 的 update 同时承担事件路由和状态转换，view 是纯字符串渲染；它更接近 Elm/Bubble Tea 的单向数据流。
- 为什么后台线程不能直接改 UI？会造成竞态和终端输出交错；统一消息队列还能记录事件顺序。
- 如何保证权限请求不死锁？future 必须有超时，dialog cancel/quit 必须完成 future，Agent 捕获 InterruptedException。
- 为什么展示 Message 与 Conversation Message 分离？一个保留供应商协议字段，一个支持折叠、耗时和 loading，两者生命周期不同。

`JimiCodeModel` 是项目中最像“控制器”的文件，但它不是业务服务。它把键盘、定时器、AgentEvent、对话框和持久化统一到一个事件循环；其余 TUI 文件提供状态枚举、展示 DTO、ANSI 样式和纯字符串渲染。学习时要把“事件来了如何改变状态”写成表，而不是只看最终屏幕长什么样。

### 工程补充来源：`study-notes/sourcebook/06-会话MemoryWorktree与恢复.md`

﻿# 06 会话、Memory、Worktree 与恢复

`ConversationManager` 管理消息、thinking block、tool use/result block；`SessionManager` 将会话写为可追加记录，提供 session id、消息统计、compact boundary 和 resume 所需扫描。`HistoryStore` 管理历史列表或索引，`FileHistory` 为编辑操作保存备份和快照。学习时要区分“模型上下文”与“磁盘审计记录”：前者可能被压缩，后者用于恢复和追责。

`MemoryManager`、`MemoryScanner` 和 `MemoryRecall` 根据项目路径和时间选择记忆文件，把相关内容注入 Prompt。Memory 是辅助上下文，不是事实数据库；旧记忆可能过时，跨项目复用可能泄露路径或凭证。`/clear` 或新会话时要确认 memory content 是否仍被保留。

`WorktreeManager`/`AgentWorktree` 负责创建、状态、变更摘要、清理和 stale cleanup；`SlugValidator` 防止不安全名称，`PostCreationSetup` 负责初始化。Worktree 隔离 Git 文件，却不能隔离模型 API、MCP Server 或共享外部服务。删除 worktree 前必须检查未提交变更和 active session。

```mermaid
stateDiagram-v2
    [*] --> ACTIVE
    ACTIVE --> COMPACTING: context threshold
    COMPACTING --> ACTIVE: boundary persisted
    ACTIVE --> PAUSED: user interrupt
    PAUSED --> ACTIVE: resume session
    ACTIVE --> WORKTREE_CREATED: subagent/team
    WORKTREE_CREATED --> MERGED: changes reviewed
    WORKTREE_CREATED --> CLEANED: no changes/stale
```

在工具写文件后强制退出，使用 session id 恢复并核对 file history；在压缩 boundary 后退出，确认 summary、keep tail 和 RecoveryState 能重建；创建 worktree 后模拟 stale cleanup，确认未提交变更不会静默丢失；在子 Agent 运行时终止父进程，记录 mailbox、session 和 worktree 的残留。

### 工程补充来源：`study-notes/sourcebook/08-调试实验复刻项目与面试.md`

可以把 JimiCode 复刻为一个更小的 `mini-code-agent`：保留配置加载、一个 LLM client、三个工具（ReadFile、WriteFile、Bash）、一轮 tool loop、确认式权限和 JSONL session。第二阶段加入 MCP；第三阶段加入上下文压缩；第四阶段加入子 Agent 和 worktree。每个阶段都先用 fake LLM 测试，再接真实 provider。

```java
interface ModelClient {
    Stream<Event> stream(List<Message> messages, List<ToolSchema> tools);
}
interface Tool {
    String name();
    Result execute(Map<String, Object> args, Context context);
}
```

核心循环必须明确：模型输出文本则追加并结束；输出 tool call 则做 schema 校验、权限检查、执行工具、追加 tool result；错误则生成结构化事件；达到最大迭代或 token 预算则终止。不要从一开始复制 200 个类。

Java：`JimiCode.main` 配置解析后、`Agent` 每轮开始、`LlmClient.stream` 返回后、`PermissionChecker` 决策、`ToolRegistry` 查找、`ContextCompactor` threshold、`SessionManager` boundary 写入、`McpManager.connectAll` 和 `WorktreeManager` 创建/清理。

Java/Maven：`JimiCode.main`、`Agent`、`LlmClient`、`ToolRegistry`、`PermissionChecker`、`ContextCompactor`、`SessionManager` 和 `WorktreeManager`。

1. 为什么 tool schema 不等于权限？
2. 为什么 LLM 流式调用要用生产者/消费者队列？
3. 为什么上下文压缩必须保存 boundary 和 RecoveryState？
4. 如何避免工具执行成功但模型超时导致重复？
5. MCP stdio 与 HTTP transport 的故障边界是什么？
6. Worktree 能解决什么并发问题，不能解决什么问题？
7. Java 虚拟线程和普通平台线程的取消语义有什么不同？
8. 为什么 API Key 错误、限流、上下文过长和网络错误要分类？

完成复刻后，必须有 fake provider 测试、工具权限测试、上下文压缩测试、会话恢复测试和客户端取消测试。真实 API、MCP Server、Git worktree 未启动时要明确标记“未实测”，不能把静态阅读当成运行证据。

### 工程补充来源：`docs/PROJECT_GUIDE.md`

> 生成日期：2026-08-10  
> 覆盖范围：当前工作区中的 Go、Java 源码，测试，构建配置，内置 Skill，Wrapper/构建产物。  
> 说明：当前目录不包含 `.git` 元数据；`build/` 和二进制 JAR 按生成物说明，不反编译第三方依赖。

JimiCode 是一个运行在终端中的 AI Coding Agent。它把 LLM 的流式输出、工具调用、权限控制、上下文压缩、持久记忆、子 Agent、Agent 团队、MCP 和 Git worktree 组合成一个完整的交互式编程助手。

| 实现 | 位置 | 构建系统 | TUI | 当前定位 |
|---|---|---|---|---|
| Go | `cmd/`、`internal/` | Go Modules | Bubble Tea | 功能更丰富、测试覆盖更广，包含内置 Skill、命令文件、后台记忆提取、外部 teammate 进程等能力 |
| Java | `src/main/java/`、`src/test/java/` | Gradle Kotlin DSL | TUI4J | README 中默认介绍的打包运行路径，可生成单体可执行 JAR |

两套代码共享相同的核心思想和大量同名模块，但不是逐行等价：Go 侧已有一些 Java 侧尚未完全接入的能力，配置细节也存在差异。阅读和修改时应先确定目标运行时，不能把一侧的行为直接假定为另一侧也已实现。

```text
JimiCode-java/
├─ cmd/JimiCode/                  # Go CLI/TUI 入口及 teammate worker 入口
├─ internal/                     # Go 主体实现与 Go 测试
│  ├─ agent/                     # Agent 循环
│  ├─ agents/                    # 子 Agent
│  ├─ compact/                   # 上下文压缩
│  ├─ llm/                       # Anthropic/OpenAI 客户端
│  ├─ tools/                     # 内置工具
│  ├─ skills/                    # Skill 目录、执行、安装和内置 Skill
│  ├─ memory/                    # 长期记忆与后台提取
│  ├─ teams/                     # 多 Agent 团队
│  ├─ worktree/                  # Git worktree 隔离
│  └─ ...                        # 权限、会话、Hook、任务、TUI 等
├─ src/main/java/com/JimiCode/    # Java 实现
├─ src/test/java/com/JimiCode/    # Java JUnit 测试
├─ gradle/wrapper/               # Gradle Wrapper
├─ build/libs/JimiCode.jar        # 已生成的 Java 21 fat JAR
└─ docs/PROJECT_GUIDE.md         # 本文档
```

- JDK 21 或更高版本。`build.gradle.kts` 使用 Java Toolchain 21；现有 JAR 的 class major version 为 65，即 Java 21。
- 首次运行 Gradle Wrapper 时需要访问 `services.gradle.org` 和 Maven Central。
- MCP 的 stdio Server 还需要对应命令，例如 `npx`、`node` 或 `uvx`。
- 文件命令工具会按当前工作目录工作；Bash 工具在 Windows 上仍调用 `bash -c`，因此需要 Git Bash、WSL Bash 或其他可用的 `bash`。

```powershell
.\gradlew.bat clean shadowJar
java -jar build\libs\JimiCode.jar
```

```bash
./gradlew clean shadowJar
java -jar build/libs/JimiCode.jar
```

```powershell
# 第一命令行参数优先
java -jar build\libs\JimiCode.jar D:\path\to\config.yaml

```powershell
.\gradlew.bat test
```

- `go.mod` 声明 Go 1.25.0。
- 首次构建需要下载 Go Modules。
- Bash 工具同样依赖 `bash`。
- tmux/iTerm 团队后端是可选能力；默认团队后端为进程内模式。

```bash
go run ./cmd/JimiCode

Go 的普通入口不接受配置文件路径参数，而是自动合并默认配置层。`--teammate --team-name <team> --agent-name <name>` 是团队框架内部启动 worker 时使用的模式，不是普通交互启动方式。

本次检查环境只有 Java 17，未安装 Go；Gradle 8.13 Wrapper 下载又因本机 Java 信任库的 PKIX 证书链错误失败。因此当前环境无法重新执行 Java/Go 测试。已有 `build/libs/JimiCode.jar` 的 Manifest 指向 `com.JimiCode.JimiCode`，但该 JAR 需要 Java 21，不能用当前 Java 17 启动。

1. `~/.JimiCode/config.yaml`
2. `<当前工作目录>/.JimiCode/config.yaml`
3. `<当前工作目录>/.JimiCode/config.local.yaml`

后面的层覆盖前面的层。`providers` 整体替换；`mcp_servers` 按 `name` 合并/覆盖；`hooks` 追加；`permission_mode` 非空时覆盖。至少需要一个 Provider，且 `name`、`protocol`、`base_url`、`model` 必填。

```yaml
providers:
  - name: anthropic-main
    protocol: anthropic       # anthropic / openai / openai-compat
    base_url: https://api.anthropic.com
    api_key: "..."            # 可省略，改读环境变量
    model: claude-sonnet-4-6
    thinking: true
    context_window: 200000    # 可选，最高优先级
    max_output_tokens: 64000  # 可选
```

API Key 的解析顺序是配置值优先，然后按协议读取 `ANTHROPIC_API_KEY` 或 `OPENAI_API_KEY`。上下文窗口按“显式配置 → Anthropic `/v1/models/{model}` 探测（Go 侧）→ 内置模型表 → 保守默认值”解析；输出上限在显式值缺失时，thinking 模式默认为 64000，否则为 8192。

| `protocol` | API | 思考内容 | 工具调用 |
|---|---|---|---|
| `anthropic` | Anthropic Messages API | 支持 enabled/adaptive thinking，保留 thinking signature | Anthropic `tool_use`/`tool_result` |
| `openai` | OpenAI Responses API | 支持 reasoning summary/encrypted content | Responses function call |
| `openai-compat` | Chat Completions API | 不保留原生 thinking block | Chat Completions tool calls，适合兼容服务 |

```yaml
mcp_servers:
  - name: context7
    command: npx
    args: ["-y", "@upstash/context7-mcp"]
    env:
      TOKEN: "${CONTEXT7_TOKEN}"
```

```yaml
mcp_servers:
  - name: remote
    url: https://example.com/mcp
    transport: http           # Go: http/streamable 或 sse；Java 当前使用 streamable HTTP
    headers:
      Authorization: "Bearer ${MCP_TOKEN}"
```

连接成功后，Server 工具被包装成 `mcp__<server>__<tool>`，非字母数字字符转成下划线。MCP 工具默认延迟加载，模型先通过 `ToolSearch` 发现后，下一轮才会收到完整 schema。

Go Hook 使用嵌套 `action`，支持 command、prompt、http、agent，支持 `once`、`async` 和 `on_error`：

```yaml
hooks:
  - id: format-go
    event: post_tool_use
    if: 'tool == "WriteFile" && file_path =* "*.go"'
    action:
      type: command
      command: gofmt -w "$JimiCode_FILE_PATH"
      timeout: 30s
    once: false
    async: false
    on_error: ignore
```

Java `HookConfig` 当前采用扁平字段，只接入 command/script/prompt、condition 和 reject：

```yaml
hooks:
  - id: notify
    event: post_receive
    condition: 'message =~ /done/'
    type: prompt
    message: "Model response received"
    reject: false
```

事件名包括 `session_start`、`session_end`、`turn_start`、`turn_end`、`pre_send`、`post_receive`、`pre_tool_use`、`post_tool_use`、`shutdown`。Go 启动入口会先校验 Hook；任意配置错误会警告并禁用整组 Hook，避免运行中才失败。

### 4.5 `permission_mode` 的现状

配置对象会读取 `permission_mode`，但当前 Go/Java TUI 构造参数都没有把它传给主 Agent，主 Agent实际仍以 `default` 启动。Go 可用 `/permission mode ...` 或 Shift+Tab 切换；Java 主要通过 Shift+Tab 切换。这是“已解析但入口未接线”的字段。

```mermaid
flowchart TD
    A[CLI main] --> B[加载并合并 config.yaml]
    B --> C[创建 TUI Model]
    C --> D[选择或自动选中 Provider]
    D --> E[加载指令、记忆、Skills，构建 System Prompt]
    E --> F[创建 LLM Client 和 Tool Registry]
    F --> G[接入权限、Hook、会话、任务、团队、Worktree、MCP]
    G --> H[用户输入消息或斜杠命令]
    H --> I[Agent Loop]
    I --> J[上下文预算与必要时压缩]
    J --> K[LLM 流式请求]
    K --> L{有工具调用?}
    L -- 否 --> M[保存最终助手消息并结束本轮]
    L -- 是 --> N[权限/Hook 检查后并发执行工具]
    N --> O[工具结果写回 Conversation]
    O --> I
    M --> P[保存会话、快照，触发后台记忆提取]
```

Java `JimiCode.main` 还负责隐藏/恢复光标、给 TUI 注入 `Program`，并在虚拟线程中重新注册 SIGINT：流式响应期间 Ctrl+C 中断当前回答，空闲时退出。Go `main` 先识别 teammate worker 参数，否则运行 Bubble Tea Model。

1. 首轮把项目指令和长期记忆注入会话头部。
2. 收集后台子任务/团队邮箱通知，作为 `<system-reminder>` 加入上下文。
3. 根据当前 Skill/团队协调模式筛选可见工具 schema。
4. Layer 2 检查上下文 token；必要时只摘要旧前缀并原样保留最近消息。
5. 计划模式下注入计划文件位置和五阶段工作流提醒。
6. 提示模型有哪些 deferred tools 可用。
7. Layer 1 对工具结果做落盘、聚合限额和旧结果裁剪，生成不修改原会话的 API 视图。
8. 调用 LLM 流式接口，把文本、思考、工具 JSON 增量统一映射成 `StreamEvent`。
9. 工具调用一完整就立即异步执行，不必等待 LLM 整条流结束；最终按调用顺序收集结果。
10. 无工具调用时结束；有工具调用时把 assistant tool-use 和 user tool-result 写回会话，进入下一轮。

- `ContextTooLong`：强制压缩后重试，并清除旧 usage anchor。
- 限流：读取 `Retry-After`，等待后重试。
- `max_tokens`：首次把模型输出上限提升到 64000；之后最多再进行三次“从中断处继续”的多轮恢复。
- 连续三个未知工具（Go）：终止循环，防止模型无限调用不存在的名称。
- 流空闲：各 LLM 适配器设置 5 分钟 SSE idle timeout；Java Agent 消费队列另有 30 秒 poll timeout。

```go
type Client interface {
    Stream(ctx context.Context, conv *conversation.Manager,
        tools []map[string]any) (<-chan StreamEvent, <-chan error)
}
```

```java
interface LlmClient {
    BlockingQueue<StreamEvent> stream(
        ConversationManager conv, List<Map<String, Object>> tools);
}
```

适配器必须把厂商事件归一化为文本增量、thinking 增量/完成、工具调用开始/参数增量/完成、usage 和流结束。Agent 因而不依赖具体 SDK。

工具都提供名称、给模型看的描述、权限类别、JSON Schema 和执行函数。类别为 read、write、command，直接影响权限矩阵。Java 的 `shouldDefer()` 和 Go 的 `DeferrableTool` 决定 schema 是否默认发送给模型。

| 工具 | 类别 | 作用与关键约束 |
|---|---|---|
| `ReadFile` | read | 按 offset/limit 读取并带行号返回，同时记录内容和 mtime，为后续编辑建立“先读后改”状态 |
| `WriteFile` | write | 创建目录并写完整文件；已有文件必须先读，写前进入文件历史 |
| `EditFile` | write | 对唯一的 `old_string` 做一次精确替换；零次或多次匹配均报错 |
| `Bash` | command | 通过 `bash -c` 执行；Go 默认 120 秒、最大 600 秒，汇总 stdout/stderr/exit code |
| `Glob` | read | 遍历文件并匹配 glob，跳过 `.git`、`node_modules`、虚拟环境等目录 |
| `Grep` | read | 对文件逐行执行正则搜索，可按文件名 include 过滤 |
| `ToolSearch` | read | `select:A,B` 精确加载或按关键词搜索 deferred tools，并标记为已发现 |
| `AskUserQuestion` | read | 把 1-4 个结构化选择题交给 TUI 对话框，等待用户回答 |
| `ExitPlanMode` | read | 校验当前确在计划模式且计划文件存在，通知 TUI 展示审批对话框 |
| `EnterWorktree` | command | 创建/恢复 JimiCode 管理的隔离 worktree，并保存 session |
| `ExitWorktree` | command | keep 或 remove 当前会话 worktree；有改动时要求显式 `discard_changes` |
| `TaskCreate/Get/List/Update` | read/write | 管理带状态、依赖、owner 和 metadata 的持久任务列表 |
| `Agent` | command | 启动同步、后台、fork、worktree 隔离或团队 teammate 子 Agent |
| `TeamCreate/Delete` | command | 建立/销毁长期 Agent 团队，Lead 有团队时进入协调工具白名单 |
| `SendMessage` | command | 通过共享文件邮箱向 teammate 或 lead 发消息 |
| `LoadSkill` | read/system | Go：按需加载 Skill 正文并固定到每轮环境上下文，注册目录工具 |
| `InstallSkill` | write | Go：从 skills.sh/GitHub URL 安装用户级 Skill 并热刷新 Catalog |
| `parse_resume` | read | Go 内置 backend-interview Skill 的专用工具，提取技术栈、项目和工作年限 |
| `mcp__*` | command | 动态包装 MCP Server 暴露的工具；默认 deferred |

Conversation 的 Message 同时支持普通文本、thinking block、tool-use block 和 tool-result block。工具结果作为 `user` 角色消息保存，以满足 Anthropic/OpenAI 的工具协议。Manager 提供追加、截断、复制读取，以及一次性的长期记忆注入。

LLM `StreamEvent` 是“厂商协议层事件”；Agent `AgentEvent` 是“交给 TUI 的运行事件”。后者额外包含权限请求、工具耗时、重试、压缩、turn/loop 完成等 UI 所需信息。

1. 计划模式例外：只允许计划文件写入和必要的协作/提问工具。
2. 安全只读命令白名单，例如 `git status`、`git diff`、`go version`。
3. 危险命令硬拦截，例如根目录 `rm -rf`、`mkfs`、向磁盘设备写入、远程脚本 pipe 到 shell。
4. 路径沙箱：文件工具仅能访问项目根、临时目录和额外允许的 memory 目录。
5. 用户/项目/本地 YAML 权限规则，按工具名和 glob 内容匹配。
6. 权限模式矩阵；结果为 allow、deny 或 ask。
7. ask 时 TUI 展示 Allow、Deny、Always Allow；Always Allow 会追加本地规则。

| 模式 | read | write | command |
|---|---:|---:|---:|
| `default` | allow | ask | ask |
| `acceptEdits` | allow | allow | ask |
| `plan` | 只读/计划例外 | deny/例外 | deny/少数例外 |
| `bypassPermissions` | allow | allow | allow；危险命令硬拦截仍在前面 |

- 单个工具结果超过 50,000 字符时写入 `.JimiCode/tool_results/`，上下文只保留可回读的稳定提示。
- 同一条消息的工具结果合计超过 200,000 字符时，从最大的结果开始落盘，直到回到预算内。
- 超过最近 10 个 turn 的旧结果，若仍大于 2,000 字符则替换成 stale snip 标记。
- `ContentReplacementState` 冻结每个 tool-use ID 的决定，并把记录写到 JSONL，保证后续请求的 prompt prefix 字节稳定，有利于 Anthropic prompt cache。
- 从 spill 文件再次 `ReadFile` 的结果不会再次 spill，避免“落盘结果的回读又落盘”的循环。

- 触发线基于 `contextWindow - outputReserve - safetyMargin`，而不是简单字符比例。
- 优先使用厂商真实 usage 作为 anchor，只对 anchor 之后新增消息做估算；冷启动才全量按字符估 token。
- 从尾部保留至少约 10,000 tokens 或 5 条消息，最多约 40,000 tokens；只把更旧前缀交给 LLM 总结。
- 压缩后会附加最近读取文件快照、已激活 Skill SOP 和仍可用工具列表，降低摘要丢失工作状态的风险。
- 自动压缩连续失败三次后熔断；手动 `/compact` 和 ContextTooLong 强制压缩仍可使用。
- 会话日志追加 `compact_boundary`，恢复时从最后一个 boundary 的 summary + keep tail + 后续消息重建，而不是重放全部旧消息。

Go Catalog 的优先级是内置 < `~/.JimiCode/skills` < 项目 `.JimiCode/skills`。启动只读 frontmatter，调用 `GetFull` 时再读正文，因此支持热修改。Skill 可声明：

- `mode: inline|fork`
- `allowed_tools`
- `model`
- `fork_context: none|recent|full`
- `tool.json` 目录工具（仅能绑定已编译进程序的 factory，不能动态加载任意 Go 插件）

inline Skill 把 SOP 固定到每轮环境提醒，并可收窄工具集；fork Skill 在隔离子 Agent 中运行，只把最终摘要带回。Java 提供 `SkillCatalog`/`SkillExecutor` 接口，但当前 TUI 只扫描项目 `.JimiCode/skills`，斜杠调用直接返回 Prompt Body；Go 的三层 Catalog、内置 Skill、`LoadSkill`/`InstallSkill` 接线更完整。

内置命令包括 `/help`、`/mcp`、`/clear`、`/compact`（`/c`）、`/status`（`/s`）、`/memory`、`/plan`（`/p`）、`/session`、`/permission`（`/perm`）、`/resume`（`/r`）、`/rewind`、`/skills`、`/review`。Skill 会注册为 `/<skill-name>`。

Go 还从 `~/.JimiCode/commands/**/*.md` 和项目 `.JimiCode/commands/**/*.md` 加载命令。子目录用冒号命名，例如 `git/log.md` 变为 `/git:log`；YAML frontmatter 可定义 description、argument-hint、aliases，正文支持 `$ARGUMENTS`。

- 用户级 `~/.JimiCode/memory/`：`user`、`feedback`
- 项目级 `<repo>/.JimiCode/memory/`：`project`、`reference`

每个目录的 `MEMORY.md` 是索引，最多向 prompt 注入 200 行/25KB；实际记忆按主题存放在独立 Markdown 文件并带 frontmatter。每轮结束后，后台 extractor fork 会在严格 memory 路径沙箱中判断是否更新记忆；如果主 Agent 本轮已主动写 memory，则跳过重复提取。发送新请求时还可以通过一个轻量 LLM selector，从文件名和 description 中最多挑 5 条相关记忆再注入。

## 11. 子 Agent、团队与 Worktree

| 类型 | 用途 | 限制 |
|---|---|---|
| `general-purpose` | 通用研究和多步骤任务 | 默认最多 200 turn |
| `plan` | 只读架构分析和实现计划 | 禁止 WriteFile/EditFile，最多 15 turn |
| `explore` | 快速只读代码搜索 | 禁止写工具，Go 默认别名模型 `haiku` |
| `verification` | Go 可选破坏性验证 Agent | `JimiCode_VERIFICATION_AGENT=true` 才注册 |

自定义 Agent 从用户级/项目级 `.JimiCode/agents/*.md` 加载，项目定义覆盖用户和内置定义。frontmatter 可声明工具白/黑名单、模型、最大 turn、权限模式、后台运行、worktree 隔离、初始 Prompt、memory/MCP/skills 等字段；部分字段目前只解析保留，尚未全部消费。

团队与一次性子 Agent 不同：teammate 完成一轮后进入 idle，继续轮询共享 JSON 邮箱；Lead 可再发消息唤醒。默认同进程运行并把工具/usage 进度显示在 TUI；Go 还实现 tmux 和 iTerm 外部进程后端。团队存在时 Lead 只看到协调工具，减少 Lead 与 teammate 同时改代码的冲突。

Worktree 创建在 `<repo>/.JimiCode/worktrees/<slug>`，临时分支名是 `worktree-<flattened-slug>`。创建后会复制本地设置、共享 git hooks、按配置 symlink 大目录，并根据 `.worktreeinclude` 复制被 gitignore 的必要文件。清理时只自动处理匹配临时命名模式、超过时限、非当前 session、无已跟踪改动且无未推送 commit 的 worktree；任何检查失败都保守跳过。

- Enter：发送消息或执行斜杠命令。
- Ctrl+J：输入换行。
- Shift+Tab：循环切换权限模式。
- Ctrl+O：展开/收起工具组和子 Agent 明细。
- Esc（流式执行中）：Go 将任务收养为后台任务；Java 当前停止前台跟踪。
- Ctrl+C：流式执行时中断并保存部分回答，空闲时退出。
- 上/下：在输入首尾浏览历史；斜杠菜单/Provider/对话框中移动光标。
- `@`：Go TUI 中打开项目文件补全并把路径插入输入。
- `/resume`：搜索并恢复 JSONL 会话；`/rewind`：选择文件快照并恢复代码、会话或两者。

| 路径 | 内容 |
|---|---|
| `.JimiCode/config.yaml` | 项目配置 |
| `.JimiCode/config.local.yaml` | 本地覆盖配置 |
| `.JimiCode/sessions/*.jsonl` | Go/Java 会话记录与 compact boundary |
| `.JimiCode/file-history/<session>/` | 编辑前备份和 rewind 快照 |
| `.JimiCode/tool_results/` | Go 大工具结果 spill；Java 当前常量路径实现有轻微差异 |
| `.JimiCode/replacement_records.jsonl` 或 Java session 目录记录 | 工具结果替换决策 |
| `.JimiCode/memory/` | 项目级记忆 |
| `~/.JimiCode/memory/` | 用户级记忆 |
| `.JimiCode/tasks/` | 任务列表持久化 |
| `.JimiCode/teams/` | 团队邮箱和共享状态 |
| `.JimiCode/worktrees/` | JimiCode 管理的 Git worktree |
| `.JimiCode/worktree_session.json` | 当前 worktree session |
| `.JimiCode/plans/` | 计划模式文件 |
| `.JimiCode/permissions.local.yaml` | Always Allow 等本地规则 |

下面按当前文件树逐个说明。测试文件的描述以其测试函数和断言为依据；生成 JAR 只说明来源和用途。

| 文件 | 说明 |
|---|---|
| `.gitignore` | 忽略 Gradle/IDE/构建输出、`.JimiCode/` 运行数据和本地指令文件，避免 API Key、会话、缓存进入版本控制。 |
| `README.md` | 面向 Java 版本的快速开始，说明 Java 21、Provider/MCP 基本配置、Shadow JAR 构建与 JUnit 测试命令。 |
| `JimiCode.md` | 项目内 Agent 指令：声明技术栈为 Go、commit message 使用英文、变量命名要求；Go memory/instructions 模块会把它注入模型上下文。 |
| `go.mod` | Go Module `JimiCode`，声明 Go 1.25.0；直接依赖 Anthropic/OpenAI SDK、Bubble Tea/Lip Gloss/Glamour、MCP SDK 和 YAML。 |
| `go.sum` | Go 依赖模块内容校验和，用于可重复、可验证的依赖解析。 |
| `settings.gradle.kts` | 将 Gradle 根项目名设为 `JimiCode`。 |
| `build.gradle.kts` | Java 21、application、Shadow JAR、TUI4J、Mordant、MCP、Anthropic/OpenAI、SnakeYAML、Jackson 和 JUnit 的构建定义；主类是 `com.JimiCode.JimiCode`。 |
| `gradlew` | Unix Gradle Wrapper 启动脚本。 |
| `gradlew.bat` | Windows Gradle Wrapper 启动脚本。 |
| `gradle/wrapper/gradle-wrapper.properties` | 锁定 Gradle 8.13 binary distribution 及本地缓存位置。 |
| `gradle/wrapper/gradle-wrapper.jar` | Wrapper 引导二进制；读取 properties、下载对应 Gradle 后执行构建。 |
| `build/libs/JimiCode.jar` | `shadowJar` 生成的 Java 21 fat JAR，包含应用类及运行依赖；Manifest 主类为 `com.JimiCode.JimiCode`。 |
| `docs/PROJECT_GUIDE.md` | 本文档，汇总运行、配置、接口、调用链和逐文件职责。 |

| 文件 | 说明 |
|---|---|
| `cmd/JimiCode/main.go` | Go 进程入口；先解析 teammate worker 模式，否则加载配置、校验 Hook、创建 Bubble Tea TUI 并运行。 |
| `cmd/JimiCode/teammate.go` | 解析 `--teammate` 参数，创建无 TUI 的 LLM/工具/Team Member，处理 SIGINT/SIGTERM，持续从团队邮箱接任务并把 AgentEvent 输出到 stderr。 |
| `cmd/JimiCode/teammate_test.go` | 覆盖 worker 标志不存在、正常参数、缺失值和未知参数忽略行为。 |

### 14.3 Go `internal/agent`

| 文件 | 说明 |
|---|---|
| `internal/agent/agent.go` | Go 核心 Agent 状态机：注入记忆/计划/Skill/通知，管理两层上下文，消费 LLM 流，启动工具，处理权限/Hook、重试、usage、max_tokens 恢复和循环终止。 |
| `internal/agent/events.go` | 定义 TUI 消费的 `AgentEvent` 联合类型，包括文本、thinking、工具开始/结果、usage、压缩、重试、权限请求和 AskUser 请求。 |
| `internal/agent/streaming_executor.go` | 为每个完整工具调用启动 goroutine，线程安全记录 pending 和结果，等待后按原调用顺序返回。 |
| `internal/agent/agent_test.go` | 使用 mock LLM/Tool 广泛验证文本响应、多工具轮次、并发执行、权限、错误、重试、未知工具、max token 恢复、Hook 和上下文行为。 |
| `internal/agent/agent_live_test.go` | 需要真实配置/API 的端到端测试，覆盖简单聊天、frontend-design Skill 和 skill-creator 输出路径；不是普通离线单元测试。 |
| `internal/agent/skills_test.go` | 验证 Skill 系统 Prompt/激活内容如何进入真实 Agent 轮次。 |

### 14.4 Go `internal/agents`

| 文件 | 说明 |
|---|---|
| `internal/agents/agent_tool.go` | `Agent` 工具完整实现；校验参数，选择模型和自定义 Agent spec，支持同步、后台、fork、teammate、cwd/worktree 隔离，并传递进度和父会话 replacement state。 |
| `internal/agents/definition.go` | 解析 `.JimiCode/agents/*.md` YAML frontmatter，校验 permission/memory/isolation 字段并转换成运行时 `SubAgentSpec`。 |
| `internal/agents/loader.go` | 合并内置、用户级、项目级 Agent 定义，记录坏文件；可通过环境变量启用 verification Agent。 |
| `internal/agents/subagent.go` | 定义后台任务状态/通知、内置 general-purpose/plan/explore spec，以及 headless 子 Agent 的启动、取消和结果收集。 |
| `internal/agents/tool_filter.go` | 根据所有 Agent 禁用表、自定义 Agent 限制、async/in-process teammate 白名单和 MCP 名称过滤/克隆 Tool Registry。 |
| `internal/agents/verification_prompt.go` | verification Agent 的系统 Prompt、何时使用说明和 spec，要求主动尝试破坏实现并输出 PASS/FAIL/PARTIAL 证据。 |
| `internal/agents/agent_tool_test.go` | 覆盖 fork 会话 thinking 保留、权限派生、防止 fork 套 fork、registry query source、后台路由和隔离参数互斥。 |
| `internal/agents/loader_test.go` | 覆盖 Agent frontmatter 解析、必填/非法字段、第三方模型、内置加载、坏文件告警、项目覆盖和 MCP 需求匹配。 |
| `internal/agents/tool_filter_test.go` | 验证通用、自定义、后台、teammate、MCP 工具过滤规则及不可绕过的禁用项。 |

### 14.5 Go `internal/commands`

| 文件 | 说明 |
|---|---|
| `internal/commands/commands.go` | 定义斜杠命令类型、上下文、Registry、解析/补全；注册 help、mcp、clear、compact、status、memory、plan、session、permission、resume、rewind、skills、review。 |
| `internal/commands/loader.go` | 递归加载用户/项目 Markdown 命令，解析 YAML frontmatter、冒号命名空间、aliases 和 `$ARGUMENTS` 替换，项目命令覆盖用户命令。 |
| `internal/commands/loader_test.go` | 覆盖空目录、单文件/嵌套命名、缺失 frontmatter、参数追加/替换、alias、跨层覆盖和非 Markdown 忽略。 |

### 14.6 Go `internal/compact`

| 文件 | 说明 |
|---|---|
| `internal/compact/compact.go` | Layer 2 上下文管理：usage anchor、阈值计算、保留最近完整 tool-use/result 配对、调用 LLM 总结旧前缀、熔断和 compact boundary 持久化。 |
| `internal/compact/recovery.go` | 并发安全记录最近文件读取和 Skill 调用；按 token 预算构建压缩后的 recovery attachment，并列出可用工具。 |
| `internal/compact/compact_test.go` | 覆盖 token 估算/anchor、spill/snip 联动、tail 保留、tool 配对、摘要格式、自动/强制压缩和三次失败熔断。 |
| `internal/compact/recovery_test.go` | 覆盖 nil/空状态、完整附件、文件数量与顺序、单文件截断和 Skill 总预算。 |

| 文件 | 说明 |
|---|---|
| `internal/config/config.go` | 定义 App/Provider/MCP 配置，加载三层 YAML，按字段合并并校验 Provider；解析 API Key、上下文窗口和最大输出 token。 |
| `internal/config/context_window_test.go` | 验证显式配置、远端探测、模型表和默认值四层优先级。 |
| `internal/conversation/conversation.go` | Conversation 数据模型和 Manager；追加文本/thinking/tool block、系统提醒、长期记忆，只读复制和截断。 |
| `internal/filehistory/filehistory.go` | 编辑前按路径哈希创建版本备份，生成最多 100 个快照，支持恢复/删除新文件、截断后续快照并保存 metadata。 |
| `internal/history/history.go` | 持久化用户输入历史，去重并限制 200 条，供 TUI 上下键浏览。 |
| `internal/history/history_test.go` | 验证空历史、追加加载、去重、裁剪和自动创建历史文件。 |
| `internal/hooks/hooks.go` | Hook 验证和引擎；支持 9 类事件、条件表达式、once/async、command/prompt/http/agent 动作、pre-tool reject 和通知队列。 |
| `internal/hooks/hooks_test.go` | 覆盖配置聚合校验、条件运算/变量、同步异步动作、once、reject、on_error、命令超时、HTTP 和 AgentRunner。 |

### 14.8 Go `internal/llm` 与 MCP

| 文件 | 说明 |
|---|---|
| `internal/llm/client.go` | 声明统一 `Client`/`MaxTokensSetter`，按 protocol 构造适配器；Anthropic 模型窗口探测失败时静默回退。 |
| `internal/llm/events.go` | 定义厂商无关的流事件和 usage 字段，特别区分常规 input、cache read 和 cache creation tokens。 |
| `internal/llm/errors.go` | 统一 API、认证、限流、网络、上下文过长错误类型，便于 Agent 用 `errors.As` 决定恢复策略。 |
| `internal/llm/model_resolver.go` | 将 haiku/sonnet/opus 短名映射为模型 ID，并基于父 Provider 复制配置创建子 Agent client。 |
| `internal/llm/anthropic.go` | Anthropic Messages 流适配；构造 thinking/tool 消息，设置 system/tool/user-tail prompt-cache breakpoint，探测模型窗口，映射 SSE 和错误。 |
| `internal/llm/openai.go` | OpenAI Responses API 适配；保留 reasoning item、function call/output，解析 cached tokens，统一上下文/限流/认证错误。 |
| `internal/llm/openai_compat.go` | Chat Completions 兼容适配；拼接增量 tool-call JSON，转换消息角色，不携带 thinking block，解析最终 usage chunk。 |
| `internal/llm/context_window_test.go` | 验证客户端/Provider 上下文窗口解析及失败回退。 |
| `internal/llm/thinking_test.go` | 覆盖模型 thinking 支持判断、thinking block 在 Anthropic/OpenAI 消息中的序列化和恢复。 |
| `internal/llm/usage_cache_test.go` | 验证缓存 token 从总 prompt token 中拆分，避免 compaction usage anchor 双重计数。 |
| `internal/mcp/mcp.go` | MCP stdio、streamable HTTP、legacy SSE 客户端和 Manager；注入环境/Headers，连接/列工具/调用/关闭，并包装 deferred Tool。 |
| `internal/mcp/mcp_test.go` | 覆盖 MCP 名称清洗、schema 包装、transport 判断和连接/错误边界。 |

### 14.9 Go `internal/memory`

| 文件 | 说明 |
|---|---|
| `internal/memory/paths.go` | 计算用户/项目 memory 目录和 `MEMORY.md` 路径，判断某绝对路径是否属于 auto-memory。 |
| `internal/memory/memory_types.go` | 定义 user/feedback/project/reference 类型，以及何时保存、何时读取、不要保存什么和 frontmatter 示例 Prompt。 |
| `internal/memory/memory.go` | 双目录 Memory Manager；构建系统提醒、枚举/解析 memory 文件、格式化 `/memory list`、清空 Markdown 记忆。 |
| `internal/memory/memdir.go` | 确保目录存在，限制 `MEMORY.md` 为 200 行/25KB，生成双路径保存说明并把两个索引加入 Prompt。 |
| `internal/memory/instructions.go` | 按用户全局→Git 根到 cwd→legacy→local 顺序发现 JimiCode/AGENTS 指令，支持最多 5 层、循环安全的 `@include`。 |
| `internal/memory/memory_scan.go` | 只读取 memory 文件头/frontmatter，限制文件数量，生成供 selector/extractor 使用的轻量 manifest。 |
| `internal/memory/memory_age.go` | 把 mtime 转成年龄天数、可读 freshness 文本和过期提醒。 |
| `internal/memory/find_relevant_memories.go` | 调用轻量 selector，从未展示 memory 中最多选择 5 个相关文件；校验 JSON/文件名，过滤正在使用工具的普通参考噪音。 |
| `internal/memory/memory_test.go` | 覆盖双目录 Manager、frontmatter、索引 Prompt、列出/清除和路径行为。 |
| `internal/memory/memory_scan_test.go` | 覆盖扫描数量、frontmatter、坏文件、排序/manifest 和 `MEMORY.md` 排除。 |
| `internal/memory/memory_age_test.go` | 覆盖不同时间跨度的年龄与 freshness 文本。 |
| `internal/memory/instructions_test.go` | 覆盖指令优先级、Git 目录层级、include、代码块跳过、循环和深度上限。 |
| `internal/memory/find_relevant_memories_test.go` | 覆盖 selector 输入、JSON 容错、最多 5 条、已展示过滤、无效文件名和失败静默回退。 |
| `internal/memory/extractor/extractor.go` | 后台记忆提取器；合并并发触发，复制父会话，过滤工具并限制 memory 沙箱，运行最多 5 轮的 headless Agent，通知新文件。 |
| `internal/memory/extractor/prompts.go` | 构造提取 Agent Prompt，说明四类 memory 路由、先批量读后批量写、索引更新和禁止调查源码。 |
| `internal/memory/extractor/extractor_test.go` | 覆盖消息游标、并发尾随提取、主 Agent 已写 memory 时跳过、工具过滤、写入路径识别和 Drain。 |
| `internal/memory/extractor/prompts_test.go` | 验证 Prompt 的目录、工具、类型、索引和 skip-index 文案。 |

| 文件 | 说明 |
|---|---|
| `internal/permissions/permissions.go` | 五层权限 Checker：计划例外、安全/危险命令、路径沙箱、YAML 规则、模式矩阵和 HITL；支持追加本地 allow 规则。 |
| `internal/permissions/permissions_test.go` | 覆盖所有权限模式、危险/安全命令、沙箱、规则优先级、内容提取和计划文件例外。 |
| `internal/planfile/planfile.go` | 在 `.JimiCode/plans` 生成本次会话计划路径，维护进程内当前路径并提供存在、读写、重置接口。 |
| `internal/prompt/builder.go` | 按优先级拼接 Prompt Section，探测 cwd/OS/arch/shell/Git branch/model/date。 |
| `internal/prompt/sections.go` | 定义身份、系统、任务执行、风险操作、工具用法、语气、输出效率和环境等核心系统 Prompt。 |
| `internal/prompt/plan_mode.go` | 生成计划模式完整/稀疏提醒、计划文件规则、五阶段流程和退出后的 reminder。 |

### 14.11 Go `internal/session`、`skills` 与内置 Skill

| 文件 | 说明 |
|---|---|
| `internal/session/session.go` | 会话 JSONL 的 ID、追加、读取、列表、搜索和格式化；保存/查找最后一个 compact boundary 以支持压缩后恢复。 |
| `internal/session/session_test.go` | 覆盖 ID、保存加载、列表、搜索、格式化、compact boundary 重建和多次压缩取最后边界。 |
| `internal/skills/skills.go` | Skill/SkillMeta 数据模型，参数渲染，inline/fork 判断和生成委派指令。 |
| `internal/skills/parser.go` | 两阶段解析 `skill.yaml+prompt.md` 或 `SKILL.md` frontmatter；按需热加载正文并填充默认元数据。 |
| `internal/skills/catalog.go` | 合并内置/用户/项目三层 Catalog，后层覆盖前层；支持 GetFull 热加载、Reload 和 eager legacy API。 |
| `internal/skills/builtins.go` | 用 `go:embed` 打包并加载 `builtins/*`，访问内置 `tool.json` 和 reference 文件。 |
| `internal/skills/directory.go` | 解析目录 Skill 的 tool.json，把 schema 绑定到编译期 factory；同名已存在工具优先，未知 factory 记录警告后跳过。 |
| `internal/skills/executor.go` | 定义 `SkillHost`/`SkillForkHost`；inline 激活 SOP 和工具白名单，fork 按 none/recent/full 复制父上下文后运行子 Agent。 |
| `internal/skills/load_skill_tool.go` | `LoadSkill` 系统工具，按需取得正文、固定 SOP、注册目录工具，且不被 Skill 自身 allowed_tools 隐藏。 |
| `internal/skills/install.go` | 解析 skills.sh、GitHub tree/raw URL，通过 GitHub API 递归下载目录，校验名称和 SKILL.md，并返回安装报告。 |
| `internal/skills/install_tool.go` | `InstallSkill` Tool 包装；安装到用户 Skill 根目录，刷新 Catalog 并让 TUI 热注册斜杠命令。 |
| `internal/skills/parse_resume.go` | 将 `parse_resume` schema 注册到内置 factory；用关键字/正则从文本简历提取技术、年限和最多 8 条项目描述，同时承载 factory 的 `init()` 注册和具体 Tool 实现。 |
| `internal/skills/catalog_test.go` | 覆盖多层优先级、frontmatter-only 启动、GetFull 热加载、Reload、内置 Skill 和坏目录跳过。 |
| `internal/skills/executor_test.go` | 覆盖 inline 激活/过滤、缺失工具 fail-fast、fork 上下文策略、模型和 allowed_tools 透传。 |
| `internal/skills/install_test.go` | 覆盖三类 URL 解析、非法 URL/名称、递归安装、报告统计和缺失 SKILL.md 回滚。 |
| `internal/skills/skills_test.go` | 覆盖 frontmatter/default、`$ARGUMENTS`、fork directive、内置加载和目录工具注册。 |
| `internal/skills/builtins/commit/SKILL.md` | 内置 commit SOP：读 status/diff、选择 Conventional Commit 类型、逐文件 stage、创建 commit，不 push。 |
| `internal/skills/builtins/test/SKILL.md` | 内置 test SOP：识别项目语言、运行完整测试、把失败分为代码/测试/环境并报告覆盖盲区。 |
| `internal/skills/builtins/backend-interview/SKILL.md` | 基于简历的三轮后端面试 SOP：基础、项目深挖、按工作年限定难度的系统设计，最后给 hire signal。 |
| `internal/skills/builtins/backend-interview/tool.json` | 声明 `parse_resume(file_path)` JSON Schema，使 backend-interview Skill 可调用编译期工具。 |

### 14.12 Go `internal/teams`

| 文件 | 说明 |
|---|---|
| `internal/teams/backend.go` | 检测当前终端是否位于 tmux 或 iTerm pane，并选择团队外部进程后端；没有可用宿主时回退到进程内模式。 |
| `internal/teams/coordinator.go` | 定义 Lead 在团队存续期间可见的协调工具白名单，避免 Lead 与 teammate 同时直接修改项目。 |
| `internal/teams/filemailbox.go` | 基于 `.JimiCode/teams` JSON 文件实现跨线程/跨进程邮箱；发送、读取未读和标记已读都经锁文件串行化，并采用临时文件原子替换。 |
| `internal/teams/inprocess.go` | 在当前进程的 goroutine 中启动 teammate Agent，把事件、取消函数和活动状态挂到 Team Member。 |
| `internal/teams/iterm.go` | 通过 AppleScript/iTerm CLI 创建外部 teammate 会话，并提供关闭对应 pane/session 的实现。 |
| `internal/teams/progress.go` | 维护 teammate 的状态、最近工具、工具次数、token 用量和 spinner 文案，供 TUI 实时渲染团队树。 |
| `internal/teams/runner.go` | teammate 的长期运行循环：注入团队身份、执行任务、汇报 idle，随后轮询邮箱接收下一轮指令或 shutdown 消息。 |
| `internal/teams/spawn.go` | 校验成员名和工作目录，按 TeamMode 分派进程内/tmux/iTerm 启动，并生成带 `--teammate` 参数且经过 shell quote 的 CLI。 |
| `internal/teams/teams.go` | 定义 TeamMode、Member、Team 和 TeamManager；负责团队 CRUD、成员生命周期、邮箱路由、进度聚合和全量关闭。 |
| `internal/teams/tmux.go` | 用 `tmux split-window` 创建命名 pane 执行 teammate 命令，并用 `kill-pane` 停止外部成员。 |
| `internal/teams/tools.go` | 实现 `SendMessage`、`TeamCreate`、`TeamDelete` 三个协调工具；检查发送者/接收者、选择模式并同步 TeamManager。 |
| `internal/teams/filemailbox_test.go` | 覆盖邮箱发送/读取/已读、空邮箱、Team 消息集成以及待处理消息注入。 |
| `internal/teams/runner_test.go` | 覆盖 teammate 等待新任务或关闭、Lead 邮箱排空、CLI 拼接、spawn 校验、外部成员登记和 shell quote。 |
| `internal/teams/teams_test.go` | 覆盖邮箱并发写、TeamManager CRUD、tmux/iTerm 后端检测以及 `SendMessage` 到 Lead 的路由。 |

### 14.13 Go `internal/todo`

| 文件 | 说明 |
|---|---|
| `internal/todo/store.go` | 把某个任务列表序列化为 JSON 文件，读取不存在文件时返回空列表，保存时创建目录并写临时文件后替换。 |
| `internal/todo/todo.go` | 定义 pending/in_progress/completed 状态和 Task/TaskList；实现创建、查询、排序列出和带状态/依赖/owner/metadata 校验的更新。 |
| `internal/todo/tools.go` | 将 TaskList 包装为 `TaskCreate`、`TaskGet`、`TaskList`、`TaskUpdate` 四个模型工具，并格式化单任务与列表结果。 |

### 14.14 Go `internal/toolresult`

| 文件 | 说明 |
|---|---|
| `internal/toolresult/budget.go` | 实现 Layer 1 工具结果预算：单结果 spill、同消息聚合裁剪、旧结果 snip、spill 回读豁免及 replacement state 冻结。 |
| `internal/toolresult/reconstruct.go` | 根据当前/继承的 replacement records 重放历史消息替换，恢复发送给模型时必须保持稳定的内容。 |
| `internal/toolresult/record.go` | 定义 replacement record JSONL 格式及追加/加载逻辑，记录 tool-use ID、生命周期、替换内容和 spill 元数据。 |
| `internal/toolresult/state.go` | 线程安全保存每个 tool-use ID 的 frozen/replaced 决策，支持父 Agent 状态继承、查询、批量冻结和持久化增量提取。 |
| `internal/toolresult/budget_test.go` | 覆盖不修改原 Conversation、首次冻结、字节稳定、聚合挑选、旧结果处理以及父/子 records 重建。 |
| `internal/toolresult/record_test.go` | 覆盖 JSONL 追加加载、空/坏行容错、字段往返和不存在文件。 |
| `internal/toolresult/state_test.go` | 覆盖状态冻结不可逆、替换记录、并发访问、父状态继承和新增记录排空。 |

### 14.15 Go `internal/tools`

| 文件 | 说明 |
|---|---|
| `internal/tools/tool.go` | 声明 `Tool`、`DeferrableTool`、`SystemTool`、类别和结果接口；Registry 负责注册、schema 协议转换、deferred 搜索/发现与默认工具装配。 |
| `internal/tools/descriptions.go` | 集中保存 Bash、ReadFile、EditFile、WriteFile、Glob、Grep 的模型可见说明，明确参数、限制和典型用途。 |
| `internal/tools/ask_user.go` | 定义结构化问题/选项/回答协议；`AskUserQuestion` 把请求发给 TUI channel 并阻塞等待用户提交或上下文取消。 |
| `internal/tools/bash.go` | 通过 `bash -c` 执行命令，默认 120 秒、上限 600 秒，合并 stdout/stderr、退出码与超时/取消错误。 |
| `internal/tools/edit_file.go` | 要求目标文件已读且 mtime 未变化，对唯一 `old_string` 做精确替换；编辑前建立文件历史，完成后刷新缓存。 |
| `internal/tools/enter_worktree.go` | 包装 worktree session 创建/恢复，校验 slug，并返回新工作目录、分支和原始 HEAD 信息。 |
| `internal/tools/exit_plan_mode.go` | 仅在计划模式且计划文件存在时成功，通过回调通知 TUI 打开计划审批流程。 |
| `internal/tools/exit_worktree.go` | 执行 keep/remove；remove 前检查未提交改动，除非模型显式传入 `discard_changes`。 |
| `internal/tools/file_state_cache.go` | 保存 ReadFile 观察到的绝对路径、内容和 mtime，供写工具执行“先读后写”和并发修改检测。 |
| `internal/tools/glob.go` | 在指定根目录递归匹配 glob，支持 `**`，跳过 `.git`、依赖和虚拟环境目录并排序截断输出。 |
| `internal/tools/grep.go` | 编译正则后逐文件逐行搜索，可用 include glob 过滤文件，输出 `path:line:content` 并限制结果大小。 |
| `internal/tools/read_file.go` | 按 offset/limit 读取文本、附行号和截断提示；成功后写入 FileStateCache 和 compaction recovery state。 |
| `internal/tools/tool_search.go` | 解析 `select:name1,name2` 或关键词查询 deferred tools，将命中项标记 discovered 并返回协议对应 schema。 |
| `internal/tools/write_file.go` | 创建父目录并写完整内容；覆盖已有文件前校验 ReadFile 缓存并建立历史快照，写后更新缓存。 |
| `internal/tools/glob_test.go` | 构造临时目录树，验证 `**` 跨目录匹配和普通 glob 兼容行为。 |
| `internal/tools/tool_search_test.go` | 覆盖 deferred/MCP 工具默认隐藏、精确选择、关键词搜索、发现持久化、token 节省和端到端 schema 暴露。 |

| 文件 | 说明 |
|---|---|
| `internal/tui/styles.go` | 定义 Lip Gloss 的主题色、消息/工具/状态栏/对话框样式和少量内联着色函数。 |
| `internal/tui/verbs.go` | 保存运行中随机动词及过去式转换，为主 Agent 和 teammate 的 spinner/完成状态提供较自然的短文案。 |
| `internal/tui/tui.go` | Bubble Tea 主 Model，承担 Provider 选择、Agent/MCP/Skill/Memory 初始化、输入与补全、流事件消费、权限/提问/计划/恢复/回退对话框、团队进度和全部 View 渲染；也是 Go 版本各模块的主要组装根。 |

### 14.17 Go `internal/worktree`

| 文件 | 说明 |
|---|---|
| `internal/worktree/agent.go` | 为一次性子 Agent 创建独立 worktree，记录 root/branch/HEAD；任务结束后按结果策略安全移除 worktree 和临时分支。 |
| `internal/worktree/changes.go` | 结合 `git status --porcelain` 与相对原始 HEAD 的 diff 统计工作区改动、提交数和未推送提交。 |
| `internal/worktree/cleanup.go` | 扫描 JimiCode 管理的 worktree，按命名、年龄、session、脏状态和未推送提交保守判断并清理过期项。 |
| `internal/worktree/create.go` | 解析 Git 根和默认分支，验证 slug/目标路径，调用 `git worktree add -b`，失败时清理残留并执行创建后设置。 |
| `internal/worktree/env.go` | 统一执行 Git 子进程，注入禁用交互式凭据提示的环境变量，返回 stdout/stderr 并响应 context 取消。 |
| `internal/worktree/filesystem.go` | 不依赖 Git 命令解析 `.git` 文件/目录、commondir、HEAD、loose/packed refs 和默认分支，并严格校验 ref/SHA 防止路径穿越。 |
| `internal/worktree/notice.go` | 生成进入隔离 worktree 后注入 Agent 的环境提示，说明父目录和当前目录的关系与操作边界。 |
| `internal/worktree/session.go` | 管理 `.JimiCode/worktree_session.json` 和进程内当前 session；实现创建、恢复、keep 与 cleanup 的完整会话语义。 |
| `internal/worktree/setup.go` | 复制 local settings、共享 hooks、配置 symlink 目录并按 `.worktreeinclude` 复制被忽略文件；保存清理周期配置并定位 canonical Git root。 |
| `internal/worktree/validate.go` | 校验最大 64 字符的分段 slug，拒绝绝对路径、`.`/`..` 和非法字符，并生成扁平目录名及 `worktree-` 分支名。 |
| `internal/worktree/agent_test.go` | 覆盖子 Agent worktree 创建、恢复、移除以及缺少 Git root 的保守失败。 |
| `internal/worktree/changes_test.go` | 覆盖干净/脏工作区、已提交改动和 ChangeSummary 计数。 |
| `internal/worktree/cleanup_test.go` | 覆盖 stale 筛选、当前 session 保护、脏改动/未推送提交保护和清理失败回退。 |
| `internal/worktree/create_test.go` | 覆盖正常创建、嵌套 slug、已存在路径/分支、非 Git 目录和创建失败清理。 |
| `internal/worktree/env_test.go` | 覆盖 Git 无提示环境、正常命令、非零退出与 context 取消。 |
| `internal/worktree/filesystem_test.go` | 覆盖 ref/SHA 安全校验、普通/linked Git 目录解析、HEAD 读取、loose ref 和缺失 ref。 |
| `internal/worktree/notice_test.go` | 验证隔离环境提示包含父/子工作目录和关键边界说明。 |
| `internal/worktree/session_test.go` | 覆盖 session 单例、JSON 保存加载、创建、keep、cleanup 和测试 Git 仓库初始化。 |
| `internal/worktree/setup_test.go` | 覆盖本地配置复制、hooksPath、目录 symlink、防路径穿越、`.worktreeinclude` 和 Git 根发现。 |
| `internal/worktree/validate_test.go` | 覆盖合法/非法 slug、扁平化、分支名和长度常量边界。 |

| 文件 | 说明 |
|---|---|
| `src/main/java/com/JimiCode/JimiCode.java` | Java `main` 入口；确定配置路径、调用 ConfigLoader、检查 Provider，并把 Provider/MCP/Hook 配置交给 JLine/Lanterna TUI 模型启动。 |
| `src/main/java/com/JimiCode/agent/Agent.java` | Java 核心 Agent Loop；写入用户消息，做工具结果预算与自动压缩，消费 LLM 流，累计 thinking/tool calls，执行权限与 Hook 检查，并持续迭代到纯文本完成或上限。 |
| `src/main/java/com/JimiCode/agent/AgentEvent.java` | sealed 事件接口及其 record 事件，统一表达文本/思考增量、工具开始/结束、usage、权限提问、重试、压缩、错误和轮次完成。 |
| `src/main/java/com/JimiCode/agent/StreamingExecutor.java` | 用线程池并行执行同一模型响应里的工具调用，先走 PermissionChecker/用户授权，再执行 Tool，并按原调用顺序返回 ToolExecResult。 |
| `src/main/java/com/JimiCode/command/Command.java` | 斜杠命令不可变 record；保存名称、别名、说明、参数提示和 command/prompt/skill 类型，并提供名称匹配。 |
| `src/main/java/com/JimiCode/command/CommandContext.java` | 命令处理器上下文 record；汇集参数、Conversation、Agent、MCP/Skill 状态以及 clear/compact/resume/rewind 等 UI 回调。 |
| `src/main/java/com/JimiCode/command/CommandRegistry.java` | 注册并搜索内置斜杠命令，按名称/别名查找和执行 handler，区分可见命令与内部命令并输出未知命令错误。 |

### 14.19 Java 压缩、配置、对话与基础状态

| 文件 | 说明 |
|---|---|
| `src/main/java/com/JimiCode/compact/ContextCompactor.java` | Java Layer 2 压缩器；用 usage anchor 估算当前 token，先执行 Layer 1 offload/snip，再选完整消息尾部、调用 LLM 总结前缀、追加 recovery attachment 并管理三次失败熔断。 |
| `src/main/java/com/JimiCode/compact/RecoveryState.java` | 并发记录最近读取的文件和调用的 Skill，按最近优先生成快照，为压缩后的上下文恢复附件提供数据。 |
| `src/main/java/com/JimiCode/config/AppConfig.java` | YAML 根配置 Bean，包含 Providers、`permission_mode`、MCP Servers 和 Hooks，并提供 SnakeYAML 所需 getter/setter。 |
| `src/main/java/com/JimiCode/config/ConfigLoader.java` | 查找并加载显式/用户/项目/local YAML，做 Provider 替换、MCP 按名合并和 Hook 追加；用自定义 SnakeYAML Constructor 解析下划线字段并校验配置。 |
| `src/main/java/com/JimiCode/config/HookConfig.java` | Java Hook 的扁平 YAML Bean，保存事件、条件、动作类型、command/message、reject 等字段；其结构与 Go 的嵌套 `action` 配置不同。 |
| `src/main/java/com/JimiCode/config/McpServerConfig.java` | MCP Server Bean，描述名称、transport、command/args、URL、环境变量和 HTTP headers。 |
| `src/main/java/com/JimiCode/config/ProviderConfig.java` | Provider Bean；保存 API Key、model、base URL、protocol、thinking、max token 与 context window，并按显式值、远程探测、模型表、默认值解析窗口。 |
| `src/main/java/com/JimiCode/conversation/ConversationManager.java` | 可变消息序列管理器；追加用户/助手/thinking/tool 结果，注入长期记忆与系统提醒，并提供只读访问、可变访问和 truncate。 |
| `src/main/java/com/JimiCode/conversation/Message.java` | 跨 Provider 的消息 DTO，包含 role、文本、thinking blocks、tool-use blocks 和 tool-result blocks。 |
| `src/main/java/com/JimiCode/conversation/ThinkingBlock.java` | 模型 thinking/reasoning 内容块 record，保存正文及厂商恢复所需签名/标识信息。 |
| `src/main/java/com/JimiCode/conversation/ToolUseBlock.java` | 工具调用 record，保存调用 ID、工具名和结构化输入参数。 |
| `src/main/java/com/JimiCode/conversation/ToolResultBlock.java` | 工具结果 record，保存对应 tool-use ID、文本输出和错误标志。 |
| `src/main/java/com/JimiCode/filehistory/FileHistory.java` | 对写操作前的文件版本做哈希命名备份，按消息建立最多 100 个 rewind 快照，并可恢复旧内容或删除当时尚不存在的文件。 |
| `src/main/java/com/JimiCode/history/HistoryStore.java` | 持久化 TUI 输入历史；加载、去空/去重追加、限制 200 条并提供索引读取，供上下键浏览。 |
| `src/main/java/com/JimiCode/hook/HookEngine.java` | 定义 Java Hook 的事件/动作/context/result，按条件运行同步 command/notification 动作，支持 pre-tool reject 和通知排空；能力范围比 Go HookEngine 更窄。 |

### 14.20 Java LLM、MCP、记忆、权限与 Prompt

| 文件 | 说明 |
|---|---|
| `src/main/java/com/JimiCode/llm/LlmClient.java` | 厂商无关客户端接口；接受 Conversation 和工具 schemas，返回 `BlockingQueue<StreamEvent>`，并暴露上下文窗口及可选最大输出 token 调节能力。 |
| `src/main/java/com/JimiCode/llm/StreamEvent.java` | sealed 流事件模型，表达 text/thinking/tool-use 增量、content block 生命周期、usage、完成和错误。 |
| `src/main/java/com/JimiCode/llm/LlmException.java` | 统一 LLM 异常层级与错误类型，使 Agent 能区分认证、限流、网络、API 和上下文过长并选择重试/压缩。 |
| `src/main/java/com/JimiCode/llm/ModelResolver.java` | 将 haiku/sonnet/opus 别名映射成具体模型，并从父 Provider 配置复制/覆盖 model 后创建子 Agent 客户端。 |
| `src/main/java/com/JimiCode/llm/AnthropicClient.java` | Anthropic Messages API 适配器；序列化 thinking/tool blocks，设置 prompt cache，后台消费流式事件，解析 usage/错误并支持 context-window 探测。 |
| `src/main/java/com/JimiCode/llm/OpenAiClient.java` | OpenAI Responses API 适配器；转换 reasoning/function call/output items，解析增量事件、cached tokens 和统一错误。 |
| `src/main/java/com/JimiCode/llm/OpenAiCompatClient.java` | OpenAI-compatible Chat Completions 适配器；构造 messages/tools，拼接分片 tool calls，发出统一事件并读取流尾 usage。 |
| `src/main/java/com/JimiCode/mcp/McpManager.java` | 根据配置连接 stdio/HTTP MCP Server，收集 server instructions 和错误，把远端 tools 包装并注册到 ToolRegistry，退出时关闭 clients。 |
| `src/main/java/com/JimiCode/memory/MemoryAge.java` | 把文件 mtime 转成年龄天数、简短 age 文本和 freshness 提醒。 |
| `src/main/java/com/JimiCode/memory/MemoryManager.java` | 管理用户/项目 memory 目录，读取受限大小的 `MEMORY.md` 索引，构建系统提示，并支持 list/clear 等命令输出。 |
| `src/main/java/com/JimiCode/memory/MemoryScanner.java` | 只扫描 Markdown memory 的 frontmatter/mtime，生成受数量限制的轻量清单，排除索引文件供相关性筛选。 |
| `src/main/java/com/JimiCode/memory/MemoryRecall.java` | 调用轻量 LLM 从 manifest 中选择与当前问题相关的 memory 文件，校验返回 JSON/文件名并读取最多 5 条正文。 |
| `src/main/java/com/JimiCode/permission/PermissionMode.java` | 定义 DEFAULT、ACCEPT_EDITS、PLAN、BYPASS_PERMISSIONS 模式和 ALLOW/DENY/ASK 决策矩阵。 |
| `src/main/java/com/JimiCode/permission/PermissionResponse.java` | TUI 对一次权限请求的响应 record，区分允许、拒绝和 Always Allow，并携带可持久化规则。 |
| `src/main/java/com/JimiCode/permission/PermissionChecker.java` | 依次执行计划模式限制、安全/危险命令、路径沙箱、YAML 规则和模式矩阵；生成操作说明，并可把 Always Allow 追加到本地权限文件。 |
| `src/main/java/com/JimiCode/plan/PlanFile.java` | 在 `.JimiCode/plans` 生成并维护当前计划路径，提供读写/存在/重置和“目标是否为计划文件”的标准化判断。 |
| `src/main/java/com/JimiCode/prompt/PromptBuilder.java` | 按 section 优先级组装 system prompt，并探测 cwd、平台、shell、Git 分支、模型和日期等环境信息。 |
| `src/main/java/com/JimiCode/prompt/PromptSections.java` | 保存身份、工程行为、工具使用、风险操作、输出风格和运行环境等静态 Prompt 片段。 |
| `src/main/java/com/JimiCode/prompt/PlanModePrompt.java` | 生成进入计划模式后的完整/简短提醒、计划文件规范、五阶段流程和退出计划模式后的补充指令。 |
| `src/main/java/com/JimiCode/session/SessionManager.java` | 管理 `.JimiCode/sessions/*.jsonl` 的新建、追加、读取、列出、搜索和恢复；持久化 compact boundary 并从最后边界重建对话。 |

### 14.21 Java Skill、子 Agent、任务与团队

| 文件 | 说明 |
|---|---|
| `src/main/java/com/JimiCode/skill/SkillHost.java` | inline Skill 所需宿主接口，允许激活 SOP、设置工具过滤器并取得 ToolRegistry。 |
| `src/main/java/com/JimiCode/skill/SkillForkHost.java` | 在 SkillHost 上增加父 Conversation 快照和子 Agent 执行接口，供 fork Skill 选择 none/recent/full 上下文。 |
| `src/main/java/com/JimiCode/skill/SkillCatalog.java` | 定义 SkillMeta/Skill，扫描 Skill 目录、解析 frontmatter、按需读取正文、reload 并生成已激活 Skill 上下文；当前 Java TUI 接线主要使用项目目录。 |
| `src/main/java/com/JimiCode/skill/SkillExecutor.java` | 替换 `$ARGUMENTS` 后执行 inline 或 fork Skill；inline 激活宿主和工具过滤，fork 构造父消息 seed 并回收子 Agent 摘要。 |
| `src/main/java/com/JimiCode/subagent/SubAgentSpec.java` | 子 Agent 不可变规格，包含 description、prompt、tools、model、最大轮次、权限、后台与隔离选项，并内置 general-purpose/plan/explore。 |
| `src/main/java/com/JimiCode/subagent/AgentLoader.java` | 从用户和项目 `.JimiCode/agents/*.md` 加载 YAML frontmatter，自定义定义覆盖内置/用户定义，坏文件跳过。 |
| `src/main/java/com/JimiCode/subagent/ToolFilter.java` | 克隆并过滤父 ToolRegistry，叠加全局不可委派工具、自定义 allow/deny、后台模式和 teammate 协调白名单。 |
| `src/main/java/com/JimiCode/subagent/SubAgentProgress.java` | 子 Agent 进度 record，携带任务 ID、名称、状态、最近工具、工具次数、token 和摘要，供 TUI 展示。 |
| `src/main/java/com/JimiCode/subagent/SubAgentTaskManager.java` | 管理后台子 Agent 的 pending/running/completed/failed/cancelled 状态、线程取消、完成通知和任务列表。 |
| `src/main/java/com/JimiCode/subagent/AgentTool.java` | `Agent` 工具实现；选择 spec/model，构造隔离 registry/conversation/replacement state，支持同步/后台、fork、worktree 和 teammate，并向 TUI 报进度。 |
| `src/main/java/com/JimiCode/task/TaskList.java` | JSON 持久化任务列表；定义 Task/Status，提供创建、查询、排序列出和状态/owner/依赖/metadata 更新校验。 |
| `src/main/java/com/JimiCode/task/TaskTools.java` | 把 TaskList 包成 deferred 的 `TaskCreate/Get/List/Update` 工具，定义 schemas 并格式化更新字段和阻塞关系。 |
| `src/main/java/com/JimiCode/teams/AgentNameRegistry.java` | 进程级线程安全单例，维护可读成员名到 Agent ID 的注册、解析、注销和快照。 |
| `src/main/java/com/JimiCode/teams/Coordinator.java` | 保存团队 Lead 的协调工具白名单并提供名称判断。 |
| `src/main/java/com/JimiCode/teams/FileMailBox.java` | 以 JSON 文件和进程内锁实现 Team 邮箱，支持发送、读取 unread 和批量标记已读。 |
| `src/main/java/com/JimiCode/teams/ITermBackend.java` | 通过 AppleScript 在 iTerm 创建/关闭 teammate tab；Java TeamMode 尚未把 iTerm 作为正式枚举模式接入。 |
| `src/main/java/com/JimiCode/teams/SharedTaskStore.java` | 团队共享任务 JSON 存储；用递增 ID 创建任务，并按状态/assignee 查询或更新认领信息。 |
| `src/main/java/com/JimiCode/teams/SpawnDispatcher.java` | 校验 SpawnConfig，按 TeamMode 分派 in-process 或 tmux，生成 `--teammate` CLI 并返回 pane/成员结果。 |
| `src/main/java/com/JimiCode/teams/TeamManager.java` | 管理 Team/Member 生命周期、后端检测、进程内成员线程、邮箱路由和进度聚合；Java 正式模式为 IN_PROCESS/TMUX。 |
| `src/main/java/com/JimiCode/teams/TeamTools.java` | 实现 `SendMessage`、`TeamCreate`、`TeamDelete`，在模型工具层处理成员检查、Lead 路由、模式参数和团队清理。 |
| `src/main/java/com/JimiCode/teams/TeammateProgress.java` | 线程安全记录 teammate 状态、最近工具摘要、工具次数和 token 总量，并提供紧凑格式化。 |
| `src/main/java/com/JimiCode/teams/TeammateRunner.java` | 运行进程内长期 teammate：添加团队身份、执行一轮后进入 idle、轮询新消息/关闭指令，并向 Lead 发送完成通知。 |
| `src/main/java/com/JimiCode/teams/TmuxBackend.java` | 通过 tmux 创建命名 pane 执行 teammate CLI，并关闭对应 pane。 |

| 文件 | 说明 |
|---|---|
| `src/main/java/com/JimiCode/tool/Tool.java` | Java 工具核心接口，约定 name、description、category、JSON schema、execute，并提供 deferred/system 默认能力。 |
| `src/main/java/com/JimiCode/tool/ToolCategory.java` | 工具权限类别枚举：READ、WRITE、COMMAND，PermissionChecker 据此应用模式矩阵。 |
| `src/main/java/com/JimiCode/tool/ToolResult.java` | 工具执行返回 record，封装 output 与 isError，并提供 success/error 工厂方法。 |
| `src/main/java/com/JimiCode/tool/ToolRegistry.java` | 注册/查询工具，按协议生成模型 schemas；隐藏未发现的 deferred 工具，并支持关键词/精确名称检索与 discovered 状态。 |
| `src/main/java/com/JimiCode/tool/FileStateCache.java` | 缓存文件最后读取的内容、mtime 和绝对路径，供 Edit/Write 执行先读后写和外部修改检测。 |
| `src/main/java/com/JimiCode/tool/impl/AskUserTool.java` | `AskUserQuestion` 实现；校验结构化 questions/options，把请求送入 TUI 回调并等待回答，取消时返回错误。 |
| `src/main/java/com/JimiCode/tool/impl/BashTool.java` | 用 `bash -c` 运行命令，执行超时控制并汇总 stdout/stderr/exit code；因此 Windows 环境仍需可用 bash。 |
| `src/main/java/com/JimiCode/tool/impl/EditFileTool.java` | 对唯一 `old_string` 精确替换，检查 FileStateCache 防止未读覆盖或并发修改，并在写入前记录 FileHistory。 |
| `src/main/java/com/JimiCode/tool/impl/EnterWorktreeTool.java` | 调用 WorktreeManager 为当前 session 创建隔离分支/目录，返回新路径信息并建立 WorktreeSession。 |
| `src/main/java/com/JimiCode/tool/impl/ExitPlanModeTool.java` | 确认正处计划模式且计划存在后触发 TUI 审批；自身不直接执行计划。 |
| `src/main/java/com/JimiCode/tool/impl/ExitWorktreeTool.java` | 根据 keep/remove 请求退出隔离目录；删除前检查改动，并要求显式确认丢弃。 |
| `src/main/java/com/JimiCode/tool/impl/GlobTool.java` | 用 NIO FileVisitor 遍历并匹配 glob，跳过重目录，排序后返回相对路径。 |
| `src/main/java/com/JimiCode/tool/impl/GrepTool.java` | 用 Java regex 逐行搜索，可按 include 过滤文件，跳过依赖目录并限制输出长度。 |
| `src/main/java/com/JimiCode/tool/impl/ReadFileTool.java` | 按 offset/limit 读取并添加行号，记录 FileStateCache，向长文件返回截断提示。 |
| `src/main/java/com/JimiCode/tool/impl/ToolSearchTool.java` | deferred 系统工具；支持 `select:` 精确发现或关键词搜索，并把命中 schema 暴露给后续 LLM 请求。 |
| `src/main/java/com/JimiCode/tool/impl/WriteFileTool.java` | 创建目录并完整写文件；覆盖时要求已有读取状态，写前备份、写后刷新 FileStateCache。 |
| `src/main/java/com/JimiCode/toolresult/ApplyResult.java` | Layer 1 预算应用结果 record，返回替换后的 Conversation 视图、新 records 和是否发生变化等信息。 |
| `src/main/java/com/JimiCode/toolresult/ContentReplacementLifecycle.java` | 工具结果生命周期枚举，区分冻结保留与 spill/snip 等已替换状态。 |
| `src/main/java/com/JimiCode/toolresult/ContentReplacementRecord.java` | 单个 tool-use 的稳定替换记录 DTO，保存 ID、生命周期、原始大小、替代文本和落盘路径。 |
| `src/main/java/com/JimiCode/toolresult/ContentReplacementState.java` | 线程安全冻结每个 tool-use 的首次预算决策，支持父状态继承、记录导入和新增记录排空。 |
| `src/main/java/com/JimiCode/toolresult/ReplacementRecordsIO.java` | 以 `replacement_records.jsonl` 追加/加载替换记录，为 session 恢复和子 Agent 重建提供稳定决策。 |
| `src/main/java/com/JimiCode/toolresult/ToolResultBudget.java` | 执行 50k 单结果 spill、200k 消息聚合预算、最近 10 turn 外 2k 旧结果 snip，并避免 spill 文件回读再次落盘。 |

### 14.23 Java TUI 与 Worktree

| 文件 | 说明 |
|---|---|
| `src/main/java/com/JimiCode/tui/AppState.java` | TUI 页面/交互状态枚举，区分 Provider 选择、普通聊天、权限确认、恢复、回退和对话框阶段。 |
| `src/main/java/com/JimiCode/tui/ChatMessage.java` | TUI 层消息模型，保存角色、正文、thinking、工具块、时间和展开状态，不等同于发送给 LLM 的 Conversation Message。 |
| `src/main/java/com/JimiCode/tui/MarkdownRenderer.java` | 使用终端样式把 Markdown 标题、列表、代码块、行内代码等转换为适合当前宽度的 ANSI 文本。 |
| `src/main/java/com/JimiCode/tui/JimiCodeModel.java` | Java TUI 主组装根与事件循环；初始化 Provider/Agent/MCP/工具，处理键盘、斜杠命令、权限/计划/提问/恢复/回退、消费 AgentEvent 并渲染聊天和状态栏。 |
| `src/main/java/com/JimiCode/tui/SpinnerVerbs.java` | 提供随机运行动词、完整词表和过去式转换，用于 spinner 与完成状态。 |
| `src/main/java/com/JimiCode/tui/Styles.java` | 集中定义 Mordant 终端颜色/样式，包括消息、工具、错误、权限框、状态栏和四种权限模式。 |
| `src/main/java/com/JimiCode/tui/dialog/AskUserDialog.java` | 多问题结构化对话框状态机，处理选项、自由输入、问题导航、提交与取消，并返回回答映射。 |
| `src/main/java/com/JimiCode/tui/dialog/PlanApprovalDialog.java` | 展示已生成计划并处理执行、保留计划、反馈修改和取消等审批动作。 |
| `src/main/java/com/JimiCode/worktree/SlugValidator.java` | 校验最长 64 字符的 worktree slug，拒绝路径穿越/非法段，并生成扁平目录名和临时分支名。 |
| `src/main/java/com/JimiCode/worktree/PostCreationSetup.java` | worktree 创建后复制 local 配置、共享 hooks、建立配置目录 symlink 并处理 `.worktreeinclude` 文件。 |
| `src/main/java/com/JimiCode/worktree/WorktreeChanges.java` | 调用 Git 检测未提交变化和相对原始 HEAD 的 commit 数，输出 ChangeSummary。 |
| `src/main/java/com/JimiCode/worktree/AgentWorktree.java` | 为一次性子 Agent 创建/移除独立 worktree，返回路径/分支/HEAD/Git root，并生成隔离环境 notice。 |
| `src/main/java/com/JimiCode/worktree/WorktreeManager.java` | 项目级 worktree CRUD，维护分支与目录映射，列出/查找、检测变化、批量删除和按截止时间清理。 |
| `src/main/java/com/JimiCode/worktree/StaleCleanup.java` | 保守扫描并清理过期临时 worktree，保护脏目录和未推送提交，也可启动周期清理线程。 |
| `src/main/java/com/JimiCode/worktree/WorktreeSession.java` | 当前隔离会话 record，保存 session ID、slug、worktree 路径/分支、父目录、原始 HEAD 和创建时间。 |
| `src/main/java/com/JimiCode/worktree/WorktreeSessionStore.java` | 保存进程内当前 WorktreeSession，并读写仓库 `.JimiCode/worktree_session.json` 以支持重启恢复。 |

| 文件 | 说明 |
|---|---|
| `src/test/java/com/JimiCode/compact/ContextCompactorTest.java` | 覆盖 token 估算/usage anchor、阈值、tail 选择、工具调用配对、spill/snip、强制压缩和失败熔断。 |
| `src/test/java/com/JimiCode/compact/RecoveryAttachmentTest.java` | 覆盖空 recovery、文件/Skill 顺序与数量、单项截断、总预算和可用工具附件。 |
| `src/test/java/com/JimiCode/config/ProviderConfigContextWindowTest.java` | 验证显式窗口、远程探测、已知模型表和默认值的优先顺序。 |
| `src/test/java/com/JimiCode/llm/AnthropicClientContextWindowTest.java` | 用替身 HTTP 响应验证 Anthropic 模型上下文窗口探测、解析及失败回退。 |
| `src/test/java/com/JimiCode/llm/UsageParsingTest.java` | 验证 Anthropic/OpenAI usage 中普通输入、cache read/cache creation/cached tokens 不被重复计数。 |
| `src/test/java/com/JimiCode/session/SessionManagerTest.java` | 覆盖 session 创建、JSONL 追加读取、列出搜索，以及 compact boundary 后的消息恢复。 |
| `src/test/java/com/JimiCode/teams/FileMailBoxTest.java` | 覆盖 Java 文件邮箱发送、未读筛选、已读标记、空邮箱和消息字段往返。 |
| `src/test/java/com/JimiCode/tool/ToolSearchTest.java` | 覆盖 deferred 工具隐藏/发现、MCP 默认 deferred、精确选择、关键词查询和 schema token 节省。 |
| `src/test/java/com/JimiCode/toolresult/ContentReplacementStateTest.java` | 覆盖 Java replacement state 首次冻结、不可逆决策、父状态继承和增量 records。 |
| `src/test/java/com/JimiCode/toolresult/ReplacementRecordsIOTest.java` | 覆盖 replacement JSONL 追加、加载、空/缺失文件和多批记录往返。 |
| `src/test/java/com/JimiCode/toolresult/ToolResultBudgetTest.java` | 覆盖单结果/聚合/旧结果预算、原对话不变、替换字节稳定、spill 回读和 records 重建。 |

1. **当前主线更接近 Go 版本。** Go 已完整接线内置/用户/项目 Skill、安装工具、后台记忆提取、iTerm TeamMode 和更丰富 Hook；Java 保留了不少对应 API，但部分能力尚未从 `JimiCodeModel` 接入完整运行链。
2. **配置格式不能完全互换。** 尤其 Hook：Go 使用嵌套 `action`，Java `HookConfig` 是扁平字段。共享配置前应分别对照第 4 节示例。
3. **`permission_mode` 当前不会自动成为 TUI 初始模式。** 两个入口都解析了该字段，但组装 PermissionChecker 时仍以默认模式启动；可在 TUI 内用 Shift+Tab 或 `/permission` 切换。
4. **Java 配置空 Provider 的错误边界不够稳健。** `ConfigLoader.validate()` 直接遍历 providers，和 Go 版显式拒绝空列表的顺序不同；配置时至少提供一个有效 Provider。
5. **Windows 仍需要 Bash。** Go/Java 的 `Bash` 工具都调用 `bash -c`，只安装 PowerShell/cmd 不足以执行该工具，可使用 Git Bash 或 WSL 提供 bash。
6. **Worktree 功能要求真实 Git 仓库。** 当前交付目录快照没有 `.git` 元数据，因此普通聊天/读写可用，但 `EnterWorktree`、Agent worktree 和 stale cleanup 要在 Git clone 中运行。

- 已按磁盘文件树逐项检查根目录、Go 源码与测试、Java 源码与测试、构建脚本、Markdown 和 JAR；第 14 节给出逐文件职责。
- 当前机器没有安装 Go，`go version` 和 `go test ./...` 无法执行。
- 当前 `java -version` 是 Java 17.0.1，而源码和现有 `build/libs/JimiCode.jar` 都要求 Java 21（class major version 65）。
- 尝试 `gradlew.bat test` 时 Wrapper 下载 Gradle 8.13 遇到 `SSLHandshakeException`/PKIX 证书链失败，因此没有把“测试通过”作为结论。
- 已检查现有 fat JAR Manifest，其 `Main-Class` 为 `com.JimiCode.JimiCode`；安装 Java 21 后可按第 3 节命令运行。

### 工程补充来源：`docs/JAVA_MAVEN_GUIDE.md`

本文只描述当前仓库中实际保留的 Java 21 实现。项目已经统一使用 Maven；不再把其他语言实现或旧构建系统当作当前运行入口。

JimiCode 是运行在终端中的 AI Coding Agent。它把模型流式响应转换为统一事件，允许模型在权限检查后调用文件、搜索、命令、MCP、子 Agent、任务和 Git worktree 工具。程序不是 HTTP 服务，没有 Controller 或 Spring 容器；`JimiCode.main` 创建配置、模型和 TUI，`JimiCodeModel` 负责交互状态，`Agent` 负责循环。

```text
src/main/java/com/jimicode/
├── agent/          Agent 循环、流式执行器、事件类型
├── command/        /help、/compact 等斜杠命令
├── compact/        上下文预算、摘要和恢复附件
├── config/         SnakeYAML 绑定对象与配置合并
├── conversation/   模型消息及 tool-use/result 数据结构
├── filehistory/    编辑前备份和快照
├── history/        JSONL 历史读取写入
├── hook/           工具前后钩子及条件表达式
├── llm/            Anthropic、OpenAI、OpenAI-compatible 适配器
├── mcp/            stdio/streamable HTTP MCP 客户端
├── memory/         项目/用户记忆扫描和选择
├── permission/     模式、危险命令和路径规则
├── plan/ prompt/   计划文件和系统提示词
├── session/        会话、compact boundary、恢复
├── skill/          Skill 元数据、执行宿主和 fork
├── subagent/       一次性或后台子 Agent
├── task/           JSON 任务列表工具
├── teams/          进程内、tmux、iTerm 团队协作
├── tool/           工具接口、注册表和内置工具
├── toolresult/     工具结果裁剪、spill、稳定替换
├── tui/            TUI4J Model、消息、渲染和对话框
└── worktree/       Git worktree 创建、清理和会话
```

要求 JDK 21、Maven 3.9 以上。使用 Wrapper 可避免本机 Maven 版本差异：

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\jimicode.jar
```

```bash
./mvnw -DskipTests package
java -jar target/jimicode.jar
```

测试命令是 `mvn test`。如果离线仓库缺少 Surefire provider，编译仍可用 `-DskipTests package` 验证；联网后再运行完整测试。

`ConfigLoader.load(null)` 依次读取：

1. `~/.JimiCode/config.yaml`；
2. 当前项目 `.JimiCode/config.yaml`；
3. 当前项目 `.JimiCode/config.local.yaml`。

后层只覆盖已提供字段；MCP 按名称替换或追加，Hook 追加。显式传入命令行路径时只读取该文件。API Key 优先使用 `api_key`，为空时按协议读取 `ANTHROPIC_API_KEY` 或 `OPENAI_API_KEY`。

完整逐文件教材位于 [`study-notes/sourcebook/README.md`](../study-notes/sourcebook/README.md)。建议先读配置和消息模型，再读 `Agent`，然后读一个内置工具，最后读 LLM、MCP、Session、SubAgent 和 Worktree。每一章都区分“当前源码事实”“Java/框架原理”和“生产建议”。

### 工程补充来源：`LEARNING_GUIDE.md`

> 基于开源项目 [JimiCode](https://github.com/xiaolincoder/jimi-code)，一个 Java CLI 终端 AI 编程 Agent
> 目标读者：有 Spring Boot 经验、准备把 Agent 写进简历并做出可演示工程的后端开发

> 课程版深度教材入口：[`docs/course/README.md`](docs/course/README.md)。该目录按 0～17 章和附录组织，包含 Mermaid 链路图、源码路径、实验、排障、安全、面试题和 FAQ。

> 如果需要一次性阅读仓库内多套教材，请使用整合主教材：[`docs/JIMICODE-整合学习教材.md`](docs/JIMICODE-整合学习教材.md)。

1. [项目定位与框架对比](#1-项目定位与框架对比)
2. [项目结构与依赖引入](#2-项目结构与依赖引入)
3. [Agent 核心循环：Think→Act→Observe](#3-agent-核心循环thinkactobserve)
4. [模型接入：LlmClient 接口与多协议适配](#4-模型接入llmclient-接口与多协议适配)
5. [Tool / ToolRegistry / Schema 生成](#5-tool--toolregistry--schema-生成)
6. [ToolResultBudget：工具结果预算机制](#6-toolresultbudget工具结果预算机制)
7. [ConfigLoader：三层配置合并与校验](#7-configloader三层配置合并与校验)
8. [ContextCompactor：两层上下文压缩](#8-contextcompactor两层上下文压缩)
9. [SubAgent：AgentTool 与 Worktree 隔离](#9-subagentagenttool-与-worktree-隔离)
10. [Team 模式：多智能体协作](#10-team-模式多智能体协作)
11. [会话持久化、Hook 系统、权限检查](#11-会话持久化hook-系统权限检查)
12. [生产化思路：从 CLI 到服务化](#12-生产化思路从-cli-到服务化)
13. [4 周学习路径与最小里程碑](#13-4-周学习路径与最小里程碑)
14. [简历技术点与面试追问预案](#14-简历技术点与面试追问预案)

JimiCode 是一个**纯 Java CLI 终端 AI 编程 Agent**，像 Claude Code 或 Codex CLI 一样，在终端中通过自然语言驱动代码编写、文件操作、命令执行。它不依赖 Spring 框架，直接通过 `anthropic-java` / `openai-java` 等官方 SDK 调用 LLM，所有组件（Agent 循环、工具系统、上下文管理、子 Agent 调度）均为**手写实现**。

### 1.2 与 AgentScope Java / Spring AI / LangChain4j 的差异

| 维度 | JimiCode | AgentScope Java | Spring AI | LangChain4j |
|------|:---:|:---:|:---:|:---:|
| **定位** | CLI 编程 Agent 应用 | 多智能体运行时平台 | LLM 集成工具包 | LLM 集成工具包 |
| **架构风格** | 手写无框架 | 平台化 SDK | Spring 生态集成 | 抽象层 |
| **容器依赖** | 无（纯 Java） | 可选 Spring Boot | 依赖 Spring | 无 |
| **Agent 循环** | 手写 `agentLoop()` | 内建 ReActAgent | 无 | 有限 |
| **上下文压缩** | 两层：截断+LLM摘要 | 两层：CompactionMiddleware | 无 | 无 |
| **工具结果预算** | ToolResultBudget 写入磁盘 | 无 | 无 | 无 |
| **子 Agent** | AgentTool + Worktree 隔离 | AgentSpawnTool | 无 | 无 |
| **Team 协作** | 原生 TeamManager | A2A 协议 | 无 | 无 |
| **TUI** | 自研终端 UI | 无（Web UI） | 无 | 无 |
| **学习价值** | **极高**：全部手写，透视 Agent 底层 | 高：理解平台化设计 | 中：理解 Spring 集成 | 中：理解抽象层 |

**核心差异一句话**：JimiCode 是"手把手教你写一个 Agent"的教学级项目，AgentScope Java 是"直接用现成的 Agent 平台"的工具级项目。学 JimiCode 让你理解 Agent 的每一行代码为什么这样写，学 AgentScope 让你快速搭建生产级 Agent 系统。

```
jimi-code/
├── pom.xml                          # Maven 构建，Java 21+
├── src/main/java/com/jimicode/
│   ├── JimiCode.java                # 程序入口：加载配置→初始化→启动 TUI
│   ├── agent/
│   │   └── Agent.java               # 核心 Agent 循环（Think→Act→Observe）
│   ├── config/
│   │   ├── AppConfig.java           # 配置 POJO（providers/permission/mcp/hooks）
│   │   └── ConfigLoader.java        # 三层配置合并+校验
│   ├── llm/
│   │   ├── LlmClient.java           # LLM 客户端接口（工厂方法）
│   │   ├── AnthropicClient.java     # Anthropic 协议实现
│   │   ├── OpenAiClient.java        # OpenAI 协议实现
│   │   └── OpenAiCompatClient.java  # OpenAI 兼容协议实现
│   ├── tool/
│   │   ├── Tool.java                # 工具接口
│   │   ├── ToolRegistry.java        # 工具注册表 + Schema 生成
│   │   └── ToolResult.java          # 工具执行结果
│   ├── toolresult/
│   │   ├── ToolResultBudget.java    # 工具结果预算控制（核心机制）
│   │   └── ContentReplacementState.java  # 替换状态追踪
│   ├── compact/
│   │   ├── ContextCompactor.java    # 两层上下文压缩
│   │   └── RecoveryState.java       # 压缩恢复状态
│   ├── subagent/
│   │   ├── AgentTool.java           # 子 Agent 工具（Fork/异步/同步）
│   │   └── SubAgentSpec.java        # 子 Agent 规格定义
│   ├── teams/
│   │   ├── TeamManager.java         # 团队管理
│   │   └── SpawnDispatcher.java     # 队员孵化调度
│   ├── conversation/
│   │   ├── ConversationManager.java # 对话管理
│   │   ├── ToolResultBlock.java     # 工具结果块
│   │   └── ToolUseBlock.java        # 工具调用块
│   ├── permission/
│   │   ├── PermissionChecker.java   # 权限检查器
│   │   └── PermissionMode.java      # 权限模式枚举
│   ├── hook/
│   │   └── HookEngine.java          # Hook 引擎
│   ├── session/
│   │   └── SessionManager.java      # 会话持久化
│   └── worktree/
│       ├── WorktreeManager.java     # Git Worktree 管理
│       └── AgentWorktree.java       # Agent 隔离工作树
```

```xml
<!-- pom.xml 关键依赖 -->
<dependencies>
    <!-- Anthropic 官方 SDK -->
    <dependency>
        <groupId>com.anthropic</groupId>
        <artifactId>anthropic-java</artifactId>
    </dependency>
    <!-- OpenAI 官方 SDK -->
    <dependency>
        <groupId>com.openai</groupId>
        <artifactId>openai-java</artifactId>
    </dependency>
    <!-- MCP 协议 -->
    <dependency>
        <groupId>io.modelcontextprotocol.sdk</groupId>
        <artifactId>mcp</artifactId>
    </dependency>
    <!-- YAML 配置解析 -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
    </dependency>
    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <!-- 终端 UI（JLine/TUI 库） -->
    <!-- 测试 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

```bash
# 使用本机 Maven 构建
mvn clean package

## 3. Agent 核心循环：Think→Act→Observe

JimiCode 的 Agent 循环是**手写的 ReAct 模式**，核心在 `Agent.agentLoop()` 方法中，约 200 行代码，每一行都值得精读：

```mermaid
sequenceDiagram
    participant Loop as Agent.agentLoop()
    participant Conv as ConversationManager
    participant Client as LlmClient
    participant Model as LLM API
    participant Registry as ToolRegistry
    participant Budget as ToolResultBudget

Loop->>Conv: injectLongTermMemory(instructions, memory)
    loop 每轮迭代 (iteration++)
        Loop->>Loop: 检查 maxIterations 是否超限
        Loop->>Loop: 检查 Thread 是否被中断
        Loop->>Conv: 注入后台任务通知
        Loop->>Registry: getAllSchemas(protocol)
        Registry-->>Loop: 工具 Schema 列表
        Loop->>Budget: apply(conversation, replacementState)
        Budget-->>Loop: ApplyResult（替换/裁剪后的对话）
        Loop->>Client: stream(conv, tools)
        Client->>Model: POST /v1/messages
        Model-->>Client: SSE 流式响应
        Client-->>Loop: BlockingQueue<StreamEvent>
        Loop->>Loop: 处理 StreamEvent 流
        alt 收到 ToolUseBlock
            Loop->>Registry: 执行工具
            Registry-->>Loop: ToolResult
            Loop->>Conv: 写入 ToolResultBlock
            Loop->>Loop: 进入下一轮迭代
        else 收到文本内容
            Loop->>Loop: 累积文本
        else 流结束
            Loop->>Loop: 更新 usageAnchor
            Loop->>Loop: 检查是否需要上下文压缩
            Loop->>Loop: break 循环
        end
    end
    Loop-->>Caller: AgentEvent.LoopComplete
```

```java
// Agent.java 核心循环（简化）
private void agentLoop(ConversationManager conv, BlockingQueue<AgentEvent> queue) {
    conv.injectLongTermMemory(instructions, memoryContent);

for (int iteration = 1; ; iteration++) {
        // 防护：最大迭代次数
        if (maxIterations > 0 && iteration > maxIterations) {
            putSafe(queue, new AgentEvent.ErrorEvent("Max iterations reached"));
            break;
        }

// 注入后台任务通知
        if (notificationFn != null) {
            for (String note : notificationFn.get()) {
                conv.addSystemReminder(note);
            }
        }

// 工具 Schema 生成（每轮一致，保证 prompt-cache 稳定）
        var iterToolSchemas = registry.getAllSchemas(protocol);

// 工具结果预算：防止大文件结果撑爆上下文
        ApplyResult budgetResult = ToolResultBudget.apply(
            conv, replacementState, usageAnchor, contextWindow);

// 调用 LLM 流式接口
        BlockingQueue<StreamEvent> stream = client.stream(conv, iterToolSchemas);
        // ... 处理流事件，执行工具调用，写入结果
    }
}
```

1. **循环内 Schema 一致性**：每轮迭代开始时生成一次工具 Schema，保证同一轮内 Schema 不变，利用 LLM 的 prompt-cache
2. **ToolResultBudget 在模型调用前**：先压缩再发送，避免浪费 Token
3. **BlockingQueue 流式处理**：生产-消费模式，虚拟线程中运行 Agent 循环，主线程消费事件
4. **usageAnchor 跟踪**：记录真实 API 返回的 Token 用量，用于精确的压缩决策

- **症状**：Agent 卡住不动，没有输出
- **原因**：`BlockingQueue.poll(60, SECONDS)` 超时返回 null
- **排查**：检查 LLM API 是否可达、API Key 是否有效、网络是否连通
- **修复**：增加 `StreamEvent` 超时处理逻辑，添加重试机制

## 4. 模型接入：LlmClient 接口与多协议适配

JimiCode 不依赖任何 LLM 框架，通过**策略模式**直接封装各厂商 SDK：

```java
public interface LlmClient {
    BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools);

default void setMaxOutputTokens(int tokens) {}

static LlmClient create(ProviderConfig cfg, String systemPrompt) {
        return switch (cfg.getProtocol()) {
            case "anthropic"  -> new AnthropicClient(cfg, systemPrompt);
            case "openai"     -> new OpenAiClient(cfg, systemPrompt);
            case "openai-compat" -> new OpenAiCompatClient(cfg, systemPrompt);
            default -> throw new IllegalArgumentException("Unknown protocol: " + cfg.getProtocol());
        };
    }
}
```

| 协议 | 实现类 | 适用模型 |
|------|--------|---------|
| `anthropic` | `AnthropicClient` | Claude 3.5/4 系列 |
| `openai` | `OpenAiClient` | GPT-4o / GPT-4.1 |
| `openai-compat` | `OpenAiCompatClient` | 通义千问、DeepSeek 等兼容 OpenAI 协议的模型 |

```yaml
# ~/.JimiCode/config.yaml
providers:
  - name: claude
    protocol: anthropic
    base_url: https://api.anthropic.com
    model: claude-sonnet-4-20250514
    api_key: ${ANTHROPIC_API_KEY}

- name: qwen
    protocol: openai-compat
    base_url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model: qwen-max
    api_key: ${DASHSCOPE_API_KEY}
```

- **症状**：`Unknown protocol: xxx`
- **原因**：配置文件中 `protocol` 字段拼写错误或使用了不支持的协议名
- **排查**：检查 `ConfigLoader.validate()` 的 `VALID_PROTOCOLS` 集合
- **修复**：协议名只能是 `anthropic`、`openai`、`openai-compat` 之一

## 5. Tool / ToolRegistry / Schema 生成

```java
public interface Tool {
    String name();
    String description();
    ToolCategory category();
    Map<String, Object> schema();
    boolean shouldDefer();  // 是否延迟加载（大工具集优化）
    ToolResult execute(Map<String, Object> args);
}
```

```java
public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

// 生成符合 LLM 协议的工具 Schema
    public List<Map<String, Object>> getAllSchemas(String protocol) {
        return tools.values().stream()
            .filter(t -> !t.shouldDefer() || isExplicitlyRequested(t))
            .map(Tool::schema)
            .toList();
    }
}
```

与 AgentScope 的 `@Tool` 注解不同，JimiCode 的 Tool Schema 是**手写 Map 结构**：

```java
// AgentTool 的 Schema 生成
@Override
public Map<String, Object> schema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("description", Map.of(
        "type", "string",
        "description", "A short (3-5 word) description of the task"
    ));
    properties.put("prompt", Map.of(
        "type", "string",
        "description", "The task for the agent to perform"
    ));
    properties.put("subagent_type", Map.of(
        "type", "string",
        "enum", List.of("general-purpose", "plan", "explore"),
        "description", "The type of agent to use"
    ));

Map<String, Object> inputSchema = new LinkedHashMap<>();
    inputSchema.put("type", "object");
    inputSchema.put("properties", properties);
    inputSchema.put("required", List.of("description", "prompt"));

return Map.of(
        "name", name(),
        "description", description(),
        "input_schema", inputSchema
    );
}
```

- **症状**：模型不调用某个工具，或调用时参数格式错误
- **原因**：Schema 的 `description` 和 `enum` 不匹配模型理解
- **排查**：检查 `getAllSchemas()` 返回的 Schema JSON，用 LLM Playground 验证
- **修复**：优化 Schema 中的 `description` 字段，确保参数说明清晰、枚举值完整

## 6. ToolResultBudget：工具结果预算机制

这是 JimiCode **最精妙的设计之一**。当工具返回超大结果（如读取 10MB 日志文件），直接将结果放到对话中会撑爆上下文窗口。

```mermaid
graph TD
    A[ToolResultBlock 写入对话] --> B{单结果 > 50KB?}
    B -->|是| C[写入磁盘文件]
    C --> D[替换为提示语:<br/>结果已保存到 /tmp/jimi-xxx,<br/>如需查看请用 ReadFile 工具]
    B -->|否| E{聚合结果 > 200KB?}
    E -->|是| F[裁剪旧结果到 2KB 摘要]
    E -->|否| G[保留原样]
    F --> H[保留最近 10 轮对话]
```

```java
// ToolResultBudget.apply() 核心逻辑
public static ApplyResult apply(
        ConversationManager conv,
        ContentReplacementState state,
        UsageAnchor usageAnchor,
        int contextWindow) {

// 规则 1：单条结果 > 50KB → 写入磁盘，替换为提示语
    // 规则 2：聚合结果 > 200KB → 裁剪旧结果
    // 规则 3：保留最近 10 轮对话
    // 规则 4：文件已存在且大小一致 → 跳过写盘，复用提示（prompt-cache 友好）
}
```

1. **替换而非截断**：原文完整保存在磁盘，模型可通过 `ReadFile` 工具读回
2. **字节稳定性**：`ContentReplacementState` 追踪替换决策，保证同一 `toolUseId` 的替换一致性，维持 prompt-cache 前缀稳定
3. **按需读取**：平时"瘦着发"，按需"胖一次"——只在模型真正需要时才读回完整内容

- **症状**：大文件读取后 Agent "看不到"内容
- **原因**：ToolResultBudget 已将结果替换为提示语
- **排查**：检查 `/tmp/jimi-*` 目录下的文件是否存在，确认提示语格式正确
- **修复**：这是设计行为，Agent 应自动调用 ReadFile 工具读取磁盘文件

## 7. ConfigLoader：三层配置合并与校验

```mermaid
graph TD
    A[CLI 指定路径?] -->|是| B[加载单文件]
    A -->|否| C[三层合并模式]
    C --> D[Layer 1: ~/.JimiCode/config.yaml<br/>全局配置]
    D --> E[Layer 2: .JimiCode/config.yaml<br/>项目配置]
    E --> F[Layer 3: .JimiCode/config.local.yaml<br/>本地覆盖]
    F --> G[合并策略]
    G --> H[validate 校验]
    H --> I[返回 AppConfig]
```

```java
// ConfigLoader.mergeConfig() 合并规则
private static void mergeConfig(AppConfig base, AppConfig override) {
    // providers: 整体替换（不合并）
    if (override.getProviders() != null && !override.getProviders().isEmpty()) {
        base.setProviders(override.getProviders());
    }

// permission_mode: 非空覆盖
    if (override.getPermissionMode() != null && !override.getPermissionMode().isBlank()) {
        base.setPermissionMode(override.getPermissionMode());
    }

// mcp_servers: 同名替换，异名追加
    // hooks: 追加合并
}
```

```java
private static void validate(AppConfig cfg) throws ConfigException {
    // 至少一个 provider
    // 每个 provider 必须有 name/protocol/base_url/model
    // protocol 必须是 anthropic/openai/openai-compat
}
```

- **症状**：`No config file found`
- **原因**：三层路径都没有配置文件
- **排查**：检查 `~/.JimiCode/config.yaml`、`$PWD/.JimiCode/config.yaml`、`$PWD/.JimiCode/config.local.yaml` 是否存在
- **修复**：创建至少一个配置文件，或通过 CLI 参数 `--config` 指定路径

## 8. ContextCompactor：两层上下文压缩

```java
// 两层压缩策略
// Layer 1（轻量）：截断旧消息 + 裁剪冗余 ToolResult
//   - 单结果 > 50,000 chars → 替换为摘要
//   - 聚合结果 > 200,000 chars → 裁剪旧结果到 2,000 chars
//   - 保留最近 10 轮对话

// Layer 2（LLM 摘要）：当 Token 使用量 > 80% 上下文窗口
//   - 触发条件：usedTokens >= effectiveWindow - 13000
//   - 硬限制：usedTokens >= effectiveWindow - 3000（强制压缩）
//   - 生成摘要后注入为系统消息
```

```java
public final class ContextCompactor {
    private static final int SUMMARY_OUTPUT_RESERVE = 20_000;   // 摘要响应预留
    private static final int AUTO_COMPACT_SAFETY_MARGIN = 13_000;  // 软触发线
    private static final int MANUAL_COMPACT_SAFETY_MARGIN = 3_000;   // 硬触发线
    private static final int SINGLE_RESULT_LIMIT = 50_000;       // 单结果阈值
    private static final int MESSAGE_AGGREGATE_LIMIT = 200_000;   // 聚合阈值
    private static final int KEEP_RECENT_TURNS = 10;             // 保留轮数
}
```

```java
// UsageAnchor: 记录真实 API 返回的 Token 用量
// 冷启动时：用字符数估算
// 首轮 API 调用后：切换到真实的 prompt_tokens + completion_tokens
// 目的：精确判断何时触发压缩，避免过早或过晚
```

```java
public class RecoveryState {
    // 最近的文件读取记录
    // 当前激活的 Skill SOP
    // 压缩后注入到对话开头，保证 Agent 不丢失关键信息
}
```

- **症状**：压缩后 Agent 行为异常，重复已完成的操作
- **原因**：压缩丢失了任务进度信息
- **排查**：检查 `RecoveryState` 是否正确保存了最近操作，检查压缩摘要的质量
- **修复**：增大 `KEEP_RECENT_TURNS`，或在关键操作后主动调用 `MemorySave`

## 9. SubAgent：AgentTool 与 Worktree 隔离

```mermaid
graph TD
    A[AgentTool.execute] --> B{参数判断}
    B -->|有 team_name| C[Team Member 模式<br/>长期运行队员]
    B -->|无 subagent_type| D[Fork 模式<br/>复制父对话上下文]
    B -->|有 subagent_type| E{run_in_background?}
    E -->|true| F[异步模式<br/>后台执行，taskManager 管理]
    E -->|false| G[同步模式<br/>阻塞等待结果]

G --> H{isolation=worktree?}
    H -->|是| I[创建 Git Worktree<br/>隔离文件系统]
    H -->|否| J[共享工作目录]
```

```java
// Fork: 复制父对话 + 注入 fork boilerplate
private ToolResult runFork(String description, String prompt, String modelOverride) {
    // 1. 检查嵌套 fork（禁止 fork 中再 fork）
    // 2. 构建 forkedConversation：复制父消息 + placeholder 结果
    // 3. 注入 fork boilerplate（行为约束）
    // 4. 传递 parentReplacementState（保持 prompt-cache 一致）
    // 5. 后台执行，返回 taskId
}
```

```java
// 创建隔离的工作树
AgentWorktree.Result wtResult = AgentWorktree.create(
    slug,                          // 唯一标识
    worktreeManager.getProjectRoot(),  // 项目根目录
    worktreeManager.getSymlinkDirs()   // 软链接目录
);

subAgent.setWorkDir(wtResult.worktreePath());
// 注入工作路径通知
String notice = AgentWorktree.buildNotice(userDir, wtResult.worktreePath());
prompt = notice + "\n\n" + prompt;

// 完成后清理
if (WorktreeChanges.hasChanges(wtResult.worktreePath(), wtResult.headCommit())) {
    // 保留 worktree（有未提交变更）
} else {
    AgentWorktree.remove(wtResult.worktreePath(), wtResult.worktreeBranch(), wtResult.gitRoot());
}
```

- **症状**：子 Agent 超时，`queue.poll(60, SECONDS)` 返回 null
- **原因**：子 Agent 工具调用耗时过长或卡住
- **排查**：检查子 Agent 的 `AgentEvent.ToolResultEvent` 日志，确认哪个工具调用耗时最长
- **修复**：为子 Agent 设置更小的 `maxIterations`，或增加超时时间

JimiCode 的 Team 模式是**Leader-Follower 架构**：

```java
// TeamManager 管理团队
public class TeamManager {
    // teamName → Team
    private final Map<String, Team> teams = new ConcurrentHashMap<>();

public Team createTeam(String name) { ... }
    public Team getTeam(String name) { ... }
}

// Team 管理成员
public class Team {
    private final String name;
    private final Map<String, TeammateRunner> members = new ConcurrentHashMap<>();

public boolean hasMember(String name) { ... }
    public List<String> memberNames() { ... }
}

// 队员间通信
public class SendMessageTool implements Tool {
    // 发送消息给其他队员
    // 接收方通过 notificationFn 收到消息
}
```

```java
// AgentTool.runAsTeammate()
private ToolResult runAsTeammate(SubAgentSpec spec, String teamName, ...) {
    // 1. 验证 Team 存在
    // 2. 去重成员名
    // 3. 构建 subRegistry（过滤工具 + 添加 SendMessageTool）
    // 4. 构建 addendum（团队上下文 + 其他成员名）
    // 5. 可选 worktree 隔离
    // 6. SpawnDispatcher.spawnTeammate()
}
```

- **症状**：队员之间消息丢失
- **原因**：`notificationFn` 未正确注入或 `SendMessageTool` 未注册
- **排查**：检查 `SpawnDispatcher` 日志，确认队员的 `notificationFn` 已设置
- **修复**：确保 `SendMessageTool` 在 `subRegistry` 中注册，且 teamName 参数正确

```java
// SessionManager 管理会话
// - 会话 ID 由 SessionIdUtils 生成
// - 对话记录写入磁盘 transcript
// - 压缩边界记录写入同一 session 文件
// - 支持 resume 恢复：从 transcript 重建压缩后的对话状态

// 关键设计：compaction 时将 compact_boundary 记录写入 session 文件
// 恢复时读取 boundary，重建压缩后的上下文
```

```java
// HookEngine 在 Agent 生命周期各节点触发回调
// 支持：
// - PreToolUse: 工具调用前
// - PostToolUse: 工具调用后
// - Notification: 自定义通知
// - Stop: Agent 停止

// 配置（config.yaml）
hooks:
  - type: command
    command: "echo 'Tool called: {{tool_name}}' >> /tmp/agent.log"
    matcher: "ToolUse"
```

```java
// PermissionMode 枚举
public enum PermissionMode {
    ALLOW_ALL,    // 允许所有操作
    ASK,          // 每次操作前询问用户
    DENY_ALL      // 拒绝所有操作
}

// PermissionChecker 在工具执行前检查
// 支持：
// - 工具名过滤
// - 路径模式匹配
// - 用户确认交互
```

- **症状**：Hook 脚本执行失败但不影响 Agent 运行
- **原因**：Hook 脚本本身有 bug，但 `HookEngine` 默认不阻断 Agent
- **排查**：检查 Hook 脚本的 stdout/stderr 输出
- **修复**：确保 Hook 脚本健壮，或在 Agent 配置中设置 `stop_on_hook_error: true`

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   TUI 终端    │────▶│   Agent 引擎  │────▶│   LLM API    │
│  (Program)   │◀────│  (agentLoop) │◀────│  (stream)    │
└──────────────┘     └──────────────┘     └──────────────┘
```

```mermaid
graph TD
    subgraph "改造后"
        A[WebSocket/REST API] --> B[AgentService]
        B --> C[Agent 引擎]
        C --> D[LLM API]
        B --> E[SessionStore<br/>Redis/DB]
        B --> F[TaskManager<br/>后台任务队列]
    end

subgraph "新增能力"
        G[多用户会话隔离]
        H[会话持久化与恢复]
        I[限流与熔断]
        J[监控与告警]
    end
```

| 改造项 | 现状 | 目标 |
|--------|------|------|
| 入口 | TUI `Program` | Spring Boot REST + WebSocket |
| 会话管理 | 内存 `ConversationManager` | Redis/DB 持久化 `SessionStore` |
| 配置管理 | 文件 `ConfigLoader` | 配置中心 + 环境变量 |
| 子 Agent | 本地进程 | 独立服务 + 消息队列 |
| 监控 | 无 | Micrometer + OpenTelemetry |

```java
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final AgentService agentService;

@PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);

Agent agent = agentService.getOrCreateAgent(request.getSessionId());
        ConversationManager conv = agentService.getOrCreateConversation(request.getSessionId());
        conv.addUserMessage(request.getMessage());

BlockingQueue<AgentEvent> queue = agent.run(conv);
        // 异步消费事件，通过 SSE 推送
        Thread.startVirtualThread(() -> {
            try {
                while (true) {
                    AgentEvent event = queue.poll(60, TimeUnit.SECONDS);
                    if (event instanceof AgentEvent.StreamText st) {
                        emitter.send(SseEmitter.event().data(st.text()));
                    } else if (event instanceof AgentEvent.LoopComplete) {
                        emitter.complete();
                        break;
                    }
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

| 日期 | 内容 | 交付物 |
|------|------|--------|
| Day 1-2 | 配置 API Key，跑通 JimiCode CLI | 终端对话截图 |
| Day 3-4 | 逐行阅读 `Agent.agentLoop()`，画出时序图 | 手绘 ReAct 循环图 |
| Day 5-6 | 理解 `LlmClient` 接口与三种协议实现 | 协议适配对比笔记 |
| Day 7 | 理解 `ConfigLoader` 三层合并 + 校验 | 配置加载流程图 |

| 日期 | 内容 | 交付物 |
|------|------|--------|
| Day 8-9 | 精读 `ToolResultBudget`，理解替换而非截断 | 预算机制分析笔记 |
| Day 10-11 | 精读 `ContextCompactor` 两层压缩 | 压缩策略对比表 |
| Day 12-13 | 实现一个自定义 Tool | 自定义工具 Demo |
| Day 14 | 理解 `RecoveryState` + `UsageAnchor` | 压缩恢复机制笔记 |

| 日期 | 内容 | 交付物 |
|------|------|--------|
| Day 15-16 | 精读 `AgentTool`，理解 Fork/同步/异步三种模式 | 子 Agent 调度流程图 |
| Day 17-18 | 精读 `AgentWorktree` 隔离机制 | Worktree 隔离 Demo |
| Day 19-20 | 精读 `TeamManager` + `SendMessageTool` | 多 Agent 协作 Demo |
| Day 21 | 理解 `HookEngine` + `PermissionChecker` | Hook 脚本示例 |

| 日期 | 内容 | 交付物 |
|------|------|--------|
| Day 22-24 | 将 JimiCode 核心引擎包装为 Spring Boot 服务 | REST API + SSE 流式输出 |
| Day 25-26 | 添加会话持久化（Redis/H2） | 多用户会话管理 |
| Day 27-28 | 文档 + 简历 + 面试准备 | 可演示的完整项目 |

| 技术点 | 简历描述示例 |
|--------|-------------|
| **ReAct 决策循环** | "手写实现 ReAct Agent 决策循环（Think→Act→Observe），支持多轮工具调用与流式响应" |
| **多协议 LLM 适配** | "基于策略模式实现 Anthropic/OpenAI/OpenAI 兼容三种协议的统一适配层" |
| **工具结果预算** | "设计 ToolResultBudget 机制，对大文件结果自动写入磁盘并替换为提示语，保障上下文窗口不溢出" |
| **两层上下文压缩** | "实现两层上下文压缩策略：Layer 1 轻量截断 + Layer 2 LLM 摘要，配合 RecoveryState 保证关键信息不丢失" |
| **多智能体协作** | "实现 AgentTool 子 Agent 调度系统，支持 Fork/同步/异步三种模式，结合 Git Worktree 实现文件系统隔离" |
| **Team 模式** | "设计 Leader-Follower 多 Agent 协作架构，通过 SendMessageTool 实现队员间通信" |
| **三层配置合并** | "实现 ConfigLoader 三层配置合并（全局→项目→本地），支持 SnakeYAML 解析与蛇形命名自动转换" |

**Q1：为什么 ContextCompactor 需要两层设计？**

> 一层不够，因为：
> 1. **Layer 1（轻量截断）**：延迟极低（< 1ms），处理 90% 的 Token 溢出场景——只需截断旧消息、裁剪旧结果
> 2. **Layer 2（LLM 摘要）**：延迟高（需要调用 LLM），但保留语义信息——只在 Layer 1 不够用（Token 使用量 > 80%）时触发
> 3. 如果只用 LLM 摘要：每次压缩都是一次 LLM 调用，成本高、延迟高
> 4. 如果只用截断：丢失语义，Agent 可能"失忆"
> 5. 两层结合：日常用轻量截断保持性能，极端情况下用 LLM 摘要保留语义

**Q2：ToolResultBudget 为什么选择"替换"而不是"截断"？**

> 截断的问题是**信息丢失不可逆**：
> 1. 如果截断日志文件的后半部分，模型可能看不到关键错误信息
> 2. 如果截断前半部分，模型可能看不到错误的上下文
> 3. 替换方案：完整内容保存在磁盘，模型需要时通过 ReadFile 按需读取
> 4. 优势：平时节省 Token（只发提示语），需要时精确获取（模型自己决定读哪个部分）
> 5. 代价：增加了一次 ReadFile 工具调用，但相比 10MB 的 Token 消耗，这点代价微不足道

**Q3：UsageAnchor 为什么需要从字符估算切换到真实 Token？**

> 字符估算和真实 Token 差异很大：
> 1. 中文字符：1 个字符 ≈ 2-3 个 Token
> 2. 英文单词：1 个单词 ≈ 1-2 个 Token
> 3. 代码：1 个字符 ≈ 0.3-0.5 个 Token
> 4. 如果用字符估算，压缩触发可能过早（中文场景）或过晚（代码场景）
> 5. 首轮冷启动用字符估算，首轮 API 调用后切换到真实 Token，后续决策精确

**Q4：ConfigLoader 的三层合并为什么 providers 是整体替换而不是合并？**

> 这是有意为之的设计决策：
> 1. **providers 整体替换**：因为 provider 配置之间可能有互斥关系（如不同的 API Key 不能混用），合并会导致不可预期的行为
> 2. **mcp_servers 同名替换/异名追加**：MCP Server 配置独立，合并合理
> 3. **hooks 追加合并**：Hook 是独立的，多个配置文件的 Hook 应该都生效
> 4. 核心原则：**有互斥风险的字段用替换，独立字段用合并**

**Q5：Agent 在虚拟线程中运行的优势和风险？**

> 优势：
> 1. **轻量级**：虚拟线程不占用 OS 线程，可以创建成千上万个
> 2. **阻塞友好**：`BlockingQueue.poll()` 阻塞时自动释放底层 OS 线程
> 3. **简化代码**：不需要 CompletableFuture 回调链，代码像同步一样写
>
> 风险：
> 1. **pin 问题**：`synchronized` 块中阻塞会 pin 住底层 OS 线程
> 2. **ThreadLocal 泄漏**：虚拟线程频繁创建/销毁，ThreadLocal 需要手动清理
> 3. **排查**：使用 `-Djdk.tracePinnedThreads=full` 检测 pin 问题

> **文档版本**：v1.0 | **更新日期**：2026-08-24 | **项目地址**：[github.com/xiaolincoder/jimi-code](https://github.com/xiaolincoder/jimi-code)
