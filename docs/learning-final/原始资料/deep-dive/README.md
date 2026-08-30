# JimiCode 深度源码学习文档导航

## 1. 这套文档解决什么问题

这不是一个只介绍功能的 README，而是一套围绕当前源码建立的学习材料。项目包含 117 个 Java 文件、单模块 Maven 构建、Java 21、TUI、多个 LLM provider、MCP、工具执行、权限、Hook、上下文压缩、会话、记忆、文件历史、Worktree、SubAgent、Team、Skill 和 Task。单篇文章无法在一个上下文中同时保持逐文件准确和链路完整，因此文档采用三种粒度：

1. 文件索引：每个文件都有路径、行数、类型、方法入口和依赖；
2. 模块分册：解释包内文件怎样分工、字段怎样流转、异常怎样恢复；
3. 系统链路：把启动、用户输入、模型流、工具副作用、持久化和 TUI 串起来，并补充面试原理、实验和排障。

文档中的“源码行为”来自当前工作区扫描和源码阅读；“工程建议”会明确使用建议、应该、风险等措辞，不把尚未实现的功能说成已有能力。

## 2. 文档清单

| 文件 | 作用 | 阅读顺序 |
|---|---|---:|
| 00-源码逐文件覆盖矩阵.md | 117 个 Java 文件的总地址簿、包职责、覆盖统计 | 1 |
| 01-入口配置消息与Agent主循环.md | 从 main 到 Agent Loop、Prompt、消息和 UI | 2 |
| 02-工具权限Hook-MCP与文件变更.md | Tool、Registry、文件工具、Bash、权限、Hook、MCP | 3 |
| 03-会话记忆压缩历史与Worktree.md | Session、JSONL、Memory、Budget、FileHistory、Git worktree | 4 |
| 04-依赖库Maven构建与面试原理.md | 每个直接依赖、SDK、Jackson/SnakeYAML、Maven、fat jar | 5 |
| 05-SubAgent团队Skill任务与TUI.md | 子 Agent、团队 mailbox、技能、任务、对话框和进度 | 6 |
| 06-系统架构逐模块调用链与异常体系.md | 包级架构、状态所有权、链路、错误域和设计面试 | 7 |
| 07-构建部署配置调试测试与常见问题.md | 从环境准备到发布、调试、性能、安全和故障处理 | 8 |
| 08-测试源码逐文件精读与行为契约.md | 14 个测试文件逐个解释和测试补强建议 | 9 |
| 09-117个Java文件逐文件精读索引.md | 每个生产/测试文件的独立条目和方法依赖索引 | 10 |

## 3. 推荐阅读路径

第一遍不要一上来读所有 117 个文件。先读 01 的主链路，知道一次请求发生什么；再读 00 和 09，建立每个文件的地图；随后按 02、03 读取工具安全和状态恢复；再读 05 理解并发协作；最后用 04、07、08 补齐依赖、构建和验证。

第二遍按源码顺序阅读：

- 入口：JimiCode.java；
- 配置：AppConfig、ConfigLoader、ProviderConfig、HookConfig、McpServerConfig；
- UI 编排：JimiCodeModel、ChatMessage、AppState；
- 消息：Message、ConversationManager、ThinkingBlock、ToolUseBlock、ToolResultBlock；
- Agent：AgentEvent、Agent、StreamingExecutor；
- LLM：LlmClient、StreamEvent、ModelResolver、三个 client、LlmException；
- 工具：Tool、ToolCategory、ToolRegistry、ToolResult、FileStateCache、impl；
- 安全：PermissionMode、PermissionResponse、PermissionChecker、HookEngine；
- 上下文：ContextCompactor、RecoveryState、toolresult 子包；
- 持久化：SessionManager、HistoryStore、Memory、FileHistory；
- 协作：SubAgent、Teams、Skill、Task；
- 隔离：Worktree；
- UI 细节：dialog、MarkdownRenderer、Styles、SpinnerVerbs；
- 测试：按 08 的测试契约回看生产实现。

## 4. 一条主链路的文件映射

用户启动程序时：JimiCode → ConfigLoader → AppConfig/ProviderConfig → PermissionMode → JimiCodeModel → Program。

用户提交普通文本时：JimiCodeModel.update → CommandRegistry 判断命令/普通输入 → HistoryStore → ConversationManager → PromptBuilder/PromptSections → Agent.run。

模型请求时：Agent.agentLoop → ContextCompactor → ToolResultBudget → LlmClient → AnthropicClient/OpenAiClient/OpenAiCompatClient → StreamEvent → AgentEvent。

模型返回工具时：StreamingExecutor → ToolRegistry → HookEngine.runPreToolHooks → PermissionChecker → Tool 实现 → ToolResult → ConversationManager → 下一轮 Agent。

需要文件时：ReadFileTool/GrepTool/GlobTool → Path/Files/ProcessBuilder → FileStateCache/RecoveryState → ToolResultBudget。

需要写入时：WriteFileTool/EditFileTool → FileHistory → FileStateCache → 磁盘 → ToolResult；若隔离则 EnterWorktreeTool/WorktreeManager/WorktreeSessionStore。

需要恢复时：SessionManager.loadSession → findLastCompactBoundary → rebuildConversation；若上下文太长则 ContextCompactor.forceCompact → ReplacementRecordsIO/Session boundary。

需要协作时：AgentTool → SubAgentTaskManager 或 TeamManager → child Agent/TeammateRunner → FileMailBox/SharedTaskStore → notificationFn → system reminder → JimiCodeModel。

## 5. 学习时的证据等级

第一等级是实际代码路径：方法调用、字段写入、异常分支、文件写入和测试断言。第二等级是类型和命名：接口、record、enum、包路径反映的设计意图。第三等级是 README/已有学习文档：适合了解背景，但遇到与源码冲突时以源码和测试为准。第四等级是工程推断：可以用于面试和改进建议，但必须标注为建议，不得混成已实现功能。

例如，源码中确实有 ContextCompactor 和 RecoveryState，可以说项目已经实现压缩和恢复附件；源码中只有 best-effort catch，则不能说“持久化强一致”；源码没有 GlobalExceptionHandler，则应说“异常按层分域处理”；源码没有数据库实体和 Controller，则不要套用 CRUD 项目模板。

## 6. 每个文件的十个问题

1. 文件的顶层类型是什么，为什么放在这个 package？
2. 构造器接收什么依赖，哪些依赖由自身创建？
3. 字段中哪些是跨调用状态，哪些是一轮临时状态？
4. 谁调用公共方法，公共方法又调用谁？
5. 正常输入、空输入、非法输入分别怎样走？
6. IO、网络、进程、线程中断和用户取消分别如何处理？
7. 返回值是给 UI、模型、磁盘还是另一个内部对象？
8. 是否修改了共享集合、文件、Git、权限或工具 registry？
9. 哪个测试锁定了这项行为，是否缺少集成测试？
10. 如果要替换实现，应该依赖哪个 interface/record，而不是修改调用方？

## 7. 每个库的五个问题

1. 这个库在哪些源码文件中直接出现？
2. 它解决的是协议、UI、序列化、网络、并发还是构建问题？
3. 它引入了什么资源生命周期或版本兼容边界？
4. fat jar、ServiceLoader、反射或 runtime scope 会不会改变行为？
5. 升级它需要哪些 fixture、集成测试和安全审查？

直接依赖包括 tui4j、Mordant、mordant-markdown、MCP SDK、SLF4J NOP、Anthropic Java、OpenAI Java、SnakeYAML、Jackson databind、Jackson JSR310 和 JUnit Jupiter；Maven plugins 还决定 JDK 门禁、编译参数、测试执行和 Shade 产物。

## 8. 30 分钟快速实验

实验 A：用一个最小 provider 配置启动，检查 ConfigLoader、PermissionMode、JimiCodeModel 和 TUI。

实验 B：让模型只回答文本，记录 StreamEvent、AgentEvent、ConversationManager 和 UsageEvent。

实验 C：让模型调用 ReadFile，比较 tool schema、Permission、ToolResult 和下一轮消息。

实验 D：设置小 contextWindow，观察 Layer 1 工具结果预算、Layer 2 compact、RecoveryState 和 session boundary。

实验 E：配置一个拒绝 Bash 的规则，比较 Permission deny、Hook reject 和用户取消。

实验 F：创建一个 worktree，写一个文件，查看 WorktreeSessionStore、git status/diff 和退出结果。

实验 G：启动 SubAgent 或 Team，观察 task id、progress、mailbox、notification 和 UI block。

实验 H：故意写坏 JSONL 最后一行，测试 SessionManager 的恢复策略。

## 9. 面试表达模板

介绍项目时可以说：这是一个 Java 21 的终端 coding agent，核心不是 CRUD，而是事件驱动的 ReAct Agent Loop。JimiCodeModel 负责 TUI 与应用编排，Agent 负责迭代和流，LlmClient 通过 adapter 统一 Anthropic/OpenAI/兼容协议，ToolRegistry/StreamingExecutor 负责工具调用，PermissionChecker/HookEngine 负责副作用控制，ContextCompactor/ToolResultBudget 负责上下文治理，Session/Memory/FileHistory/Worktree 负责可恢复和可回滚，SubAgent/Team/Skill/Task 负责协作工作流。

被问全局异常时说：没有一个 Web 式 GlobalExceptionHandler，而是按故障域处理：配置入口直接失败，模型异常分类重试/压缩，工具异常转 ToolResult，权限返回控制决定，TUI转 ErrorEvent，持久化按 best-effort 与恢复关键程度区分。然后指出统一 error code、correlation id 和结构化日志是可改进点。

被问性能时说：token 是主成本，ToolResultBudget 和 ContextCompactor 控制上下文，ToolSearch 延迟 schema，FileStateCache 减少重复读取，虚拟线程承载阻塞等待，队列和输出上限避免内存爆炸；但需要对进程、HTTP、MCP、队列和并发 SubAgent 设置 timeout/limit。

## 10. 验收标准

文档阅读完成后，不要求背出所有行号，但要求能够：

- 打开任意一个 Java 文件并定位它在主链路中的位置；
- 用实际类名解释一次普通请求和一次工具请求；
- 解释 provider adapter 如何归一化 StreamEvent；
- 解释权限、Hook、工具校验和工作区为什么是多层安全边界；
- 解释 apiConv、live conversation、session audit 的差异；
- 解释 compact boundary 怎样支撑 resume；
- 解释 FileHistory、Git worktree 和 SessionStore 的边界；
- 解释 SubAgent/Team/Skill/Task 的生命周期；
- 运行测试和 fat jar，并根据症状定位故障域；
- 在面试中区分“源码已实现”“测试已证明”和“工程上建议”。

## 11. 文档维护规则

源码新增文件时，先更新 00/09 索引，再把文件加入所属模块分册；新增依赖时更新 04 的库表和 pom 映射；新增测试时更新 08；修改 Agent/Tool/Session 状态机时同步更新 01、02、03、06。每次维护后重新统计字符数、Java 文件数量、索引覆盖率和链接路径。

不要把 target、.git、真实 session、memory、API key 和个人 worktree 纳入学习文档；不要把自动生成的临时输出提交进项目；不要为了满足字数重复粘贴相同段落，应该补充字段、调用链、故障、测试和原理。

## 12. 最后给学习者的建议

最有效的方式不是从第一行读到最后一行，而是带着一次具体任务读：例如“读取一个文件、修改一个方法、运行测试并恢复会话”。沿着这条任务追踪 JimiCodeModel、Agent、LLM、Tool、Permission、FileHistory、Session 和 UI；然后换一个会触发压缩或 SubAgent 的长任务，观察状态如何扩展。源码中的设计只有在状态变化和失败场景中才真正显现。

