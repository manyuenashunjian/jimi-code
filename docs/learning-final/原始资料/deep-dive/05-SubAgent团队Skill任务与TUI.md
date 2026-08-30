# JimiCode SubAgent、Team、Skill、Task 与 TUI 深度源码学习

## 1. 为什么这些模块必须放在一起读

JimiCode 的 Agent Loop 解决的是单个 Agent 如何思考和调用工具；但真实 coding 任务经常需要拆分研究、并行执行、等待用户、跟踪待办、加载 SOP、在终端展示多个进度。这些能力分别位于 subagent、teams、skill、task 和 tui 包，却共享同一套并发与状态问题：任务如何创建、如何完成、如何通知父 Agent、如何让 UI 显示、如何恢复、如何限制工具。

总体关系：

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

## 3. AgentTool 的同步与异步路径

一个 SubAgent 调用可能有两种模式：

- force sync：父 Agent 等待子 Agent 完成后再继续；
- background：立即返回 task id，后台运行，通过 notificationFn/TaskTools 查询。

同步模式适合需要子 Agent 结果才能决定下一步的研究任务，但会增加父 Agent 延迟；后台模式适合并行扫描、测试或文档生成，需要可靠的任务仓库和通知。SubAgentProgress 用于 UI/父 Agent 的进度快照，不应该把所有子 Agent transcript 塞到父模型上下文。

子 Agent 可以有独立 ConversationManager、LlmClient、ToolRegistry 和 sessionId。是否共享 FileHistory、Memory、Permission 和 Worktree 要明确：共享 workdir 可能产生并发写冲突；共享 permission 可能让子 Agent继承过高权限；共享 memory 可能把临时结论污染长期记忆。AgentTool 中 clone replacement state、继承工具过滤和配置传递的代码是关键证据。

SubAgentTaskManager 维护 task id、状态、队列和通知。它使用 BlockingQueue、AtomicInteger 等并发结构，重点检查状态读写是否原子、task id 是否稳定、完成结果是否只消费一次、后台异常是否被父 Agent 看到。若父 Agent被中断，子 Agent 是取消、继续后台还是转 orphan，应由明确策略决定，不能依赖线程默认行为。

## 4. ToolFilter：能力最小化

ToolFilter 根据 ToolRegistry 和 Set<String> 形成 allow/deny 视图。它可能用于 SubAgent、Skill、Team member 和 Plan mode。正确做法是同时过滤模型 schema 和执行入口：

visibleTools = registry.schemas().filter(filter)
execute(toolCall) = registry.find(tool).thenCheck(filter)

只过滤 schema 会留下“隐藏工具调用”漏洞，只过滤执行会让模型反复调用永远被拒绝的工具。过滤名称时还要注意工具别名、MCP 前缀、group 和 deferred name；一个工具在 schema 中叫 mcp_x，而执行层用原始名，可能出现错配。

## 5. TeamManager 与团队状态

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

## 9. TaskList 与 TaskTools

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

## 12. 全链路例子：团队后台研究

用户要求“研究三个模块并总结”。主 Agent 创建 TaskList 任务，TeamCreateTool 建立团队，SpawnDispatcher 产生三个成员。每个成员继承只读工具过滤器，在自己的 Agent loop 中读取文件并写入 mailbox。TeammateRunner 将子事件变成 TeammateProgress，FileMailBox 发送完成消息，JimiCodeModel 通过 MailboxPollMessage 读取并更新 ChatMessage 的 SubAgentBlockState。

主 Agent 的 notificationFn 将“成员 A 已完成”注入 system reminder，模型通过 TeamTools/TaskTools 读取汇总，更新任务状态为 completed，并输出最终答案。如果一个成员失败，TeamTask 标记 failed，父模型可以重新分派，但必须避免重复创建相同任务。若用户 Ctrl-C，TeammateRunner、child Agent、tmux/backend 和 mailbox poll 都需要收到取消；不能只停止主 UI。

## 13. 全局异常与恢复

SubAgent 失败应保留 task record 和失败原因，不能只返回空字符串；Team message 投递失败应可重试/标记未送达；Skill 解析失败应回退到普通 Agent 而不是加载半份 SOP；TaskList JSON 损坏应尽量跳过坏任务并告警；Dialog future 取消时必须让等待工具结束，否则 Agent 会永久卡在 pending。

TUI 异常要保证 view 不访问 null pending 对象；用户在 PermissionDialog 关闭时应有明确 reject/cancel；窗口 resize 不应改变业务状态；Agent 队列超时要显示错误或结束状态。tmux 进程崩溃时，TeammateRunner 应转为 failed/orphan，而不是让 UI 永远显示 running。

## 14. 面试原理

### SubAgent 与线程池任务的区别

SubAgent 有模型、消息、工具、权限、session、事件和最终语义；线程池任务只是执行代码。把 SubAgent 当 Runnable 会丢失上下文隔离、工具过滤、停止和恢复。

### 团队消息为什么要持久化

后台成员完成时父进程可能不在消费；持久化允许重启后恢复、重试、审计和去重。内存队列只能传递在线事件，不能承担任务结果的可靠投递。

### Skill 为什么需要工具过滤

Skill SOP 可能要求只允许特定工具，减少模型自由度和风险；过滤必须同时作用于 schema 和执行，且结束时恢复原始工具集合。

### TUI 为什么不能直接读取 Agent 状态

Agent 在后台线程/虚拟线程运行，直接读取可变字段会产生竞态和半更新快照；事件消息把并发转换成单线程 reducer 的状态变化。

## 15. 本册验收标准

学习者应该能够画出 SubAgent 同步/后台两条链路，解释 AgentTool、TaskManager、ToolFilter 的配合；说明 FileMailBox 如何处理跨进程消息和重复读取；说清 Team 与 SubAgent 的边界；解释 SkillCatalog 到 SkillExecutor 的生命周期；列出 TaskList 状态迁移；指出 AskUserDialog/PlanApprovalDialog 的 Future 如何恢复执行；并能在父 Agent 中断、子 Agent orphan、tmux 不存在、技能解析失败、任务 JSON 损坏五种场景下设计恢复路径。

