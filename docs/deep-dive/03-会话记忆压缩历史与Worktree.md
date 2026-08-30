# JimiCode 会话、记忆、上下文压缩、历史与 Worktree 深度解析

## 1. 为什么持久化是 Agent 的核心能力

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

压缩正确性有三个维度：

- 语义正确：模型能继续当前任务，不重复已完成工作；
- 协议正确：assistant tool_use 与 tool_result、thinking signature 等结构不被破坏；
- 审计正确：原始 session 文件仍可解释，边界之后不会产生歧义。

## 4. ContextCompactor：两层压缩

ContextCompactor 有自动管理和强制管理。自动管理根据 contextWindow、maxOutput、当前消息规模和 UsageAnchor 判断是否接近上限；强制管理由模型返回 context too long 时立即触发。

### 4.1 UsageAnchor 与估算

冷启动时没有 provider token 用量，代码会用字符/消息估算；第一次真实 stream 结束后，用 provider 报告的 input/output/cache token 建立 UsageAnchor。下一次判断可以在真实锚点附近估算，而不是完全依赖字符数。压缩后消息数量和内容结构改变，旧锚点失效，因此 Agent 会将 usageAnchor 置空。

这是一个一般化的工程问题：任何基于历史样本的估算器，在数据集发生结构突变后都必须重新校准。面试可以把它类比为缓存失效、分页游标失效或数据库统计信息失效。

### 4.2 Layer 1：工具结果预算

ToolResultBudget 在请求发送前创建 apiConv，不直接缩短 live conversation。它按结果年龄、大小、tool 类型和替换状态决定保留完整、替换为摘要、替换为文件引用或移除冗余内容。ContentReplacementState 记录某条结果是否已经被替换，ReplacementRecordsIO 将决策追加到 JSONL 文件。

为什么要记录“已替换”？因为每轮都重新决定可能产生不同文本，破坏 prompt cache 前缀，还可能在恢复时不知道某个结果的原始视图。状态机要保证：

```
FULL -> SUMMARIZED -> REFERENCED
  |       |              |
  +-------+--------------+
        幂等，不重复追加记录
```

实际状态可能更丰富，但阅读时要看 transition 是否允许回退、是否按 toolUseId 定位、文件是否存在、摘要失败时是否保留原文。

### 4.3 Layer 2：消息级压缩

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

## 6. HistoryStore：提示历史的边界

HistoryStore 将 prompt 以 JSONL 保存，最大 200 条，连续重复去重，append 时整体重写。它不是 session transcript，不能拿来恢复 Agent 状态，因为它没有 assistant/tool 结果和协议块。把两者混淆会造成用户以为“历史里看到过这句话就能继续任务”。

整体重写简单可靠但不是高性能；200 条上限使成本可接受。load 对坏行和空文本采取跳过策略，说明历史是 best effort。时间戳按 append 时统一生成，历史条目的 timestamp 并不代表原始输入发生时间，文档中不要过度解释它。

## 7. FileHistory：文件快照与恢复

FileHistory 使用 Path、Files、MessageDigest、Instant、CopyOnWriteArrayList 等实现文件备份、snapshot 和恢复。文件历史应回答三个问题：备份何时建立、如何识别版本、如何恢复到哪个点。

常见流程：

1. Write/Edit 前读取旧文件并计算 hash；
2. 把旧内容复制到 history 目录，记录 path/hash/timestamp；
3. 执行写入；
4. 写入成功后记录新 hash 和变更摘要；
5. 回滚时按 snapshot 复制回原路径；
6. 如果文件已经被用户修改，恢复前再次比较 hash，避免覆盖新变化。

CopyOnWriteArrayList 适合读多写少的监听器/快照列表，不适合高频写文件历史；源码使用它时要关注是否只是保存订阅者，而不是误以为文件操作本身线程安全。

## 8. Worktree 体系

WorktreeManager、AgentWorktree、PostCreationSetup、WorktreeSessionStore、WorktreeSession、WorktreeChanges、SlugValidator 和 StaleCleanup 共同实现隔离工作区。它不是简单的 `git worktree add` 包装，而是把“Agent 会话”和“Git 目录”绑定。

### 8.1 创建

EnterWorktreeTool 接收 slug 或名称，SlugValidator 限制字符集和长度，WorktreeManager 调用 git 创建目录，PostCreationSetup 复制或初始化必要文件，WorktreeSessionStore 写入 session -> path 映射。失败时可能发生半创建：Git worktree 已存在但 session 未写入，或者目录创建成功而依赖安装失败。清理策略必须可重入。

### 8.2 使用

进入后，Agent 的 workDir 被切换，Read/Edit/Write/Bash 都应以这个目录为基准；PromptBuilder 也应告诉模型当前路径，避免模型写主仓库。Session 文件通常仍需关联主会话 id，否则恢复时不知道在哪个 worktree 中。

### 8.3 退出

ExitWorktreeTool 和 WorktreeChanges 计算 git status/diff，向用户报告新增、修改、删除和未跟踪文件。退出不等于自动合并；如果源码没有实现合并，就不能在文档中承诺会自动回主分支。用户可能想保留 worktree、删除 worktree、复制补丁或继续会话，状态机要区分。

### 8.4 StaleCleanup

StaleCleanup 通过时间、目录属性和 session 记录清理陈旧 worktree。自动删除是破坏性动作，必须确认路径解析在目标根目录内，避免 session 文件被篡改导致清理任意目录。最好先移动到回收站或生成 dry-run 报告；至少保留日志和可恢复信息。

## 9. 失败场景推演

### 会话文件最后一行损坏

JSONL loader 应跳过损坏行并恢复此前消息；如果损坏行是 compact boundary，则 findLastCompactBoundary 找不到有效边界，回退到完整 replay。优点是尽量不丢数据，缺点是可能重新发送超长上下文，随后再次触发压缩。

### 压缩摘要请求失败

不能删除原消息后再请求摘要，否则会丢失上下文。正确顺序是先生成摘要和 boundary 对象，成功后再追加边界记录/更新内存视图。若摘要成功但写 boundary 失败，要区分内存当前状态和磁盘恢复状态，并给用户警告。

### 文件写成功但 history 失败

这是典型一致性问题。若 FileHistory 是回滚保障，应该在写文件前强制备份，备份失败则拒绝写入；若策略允许 best effort，则成功结果中必须告诉用户没有建立回滚点。不能静默声称可恢复。

### worktree 目录已被人工删除

WorktreeSessionStore 仍可能保存旧路径。读取时要检查存在性，报告 stale session，允许重新定位或清理记录。不要把不存在路径当作当前工作目录继续调用模型。

## 10. 面试原理

### JSONL 为什么适合 transcript

它支持追加、逐行恢复、低依赖和人工检查，适合 Agent 的长会话；缺点是查询和事务能力弱，需要 compact boundary、索引或周期重写。数据库适合复杂查询与并发，但会引入 schema、迁移和部署成本。选择 JSONL 是 CLI 本地优先的工程取舍。

### 摘要压缩为什么会产生“记忆幻觉”

摘要模型可能遗漏条件、混淆已完成和未完成、虚构文件状态。解决方法是保留最近精确消息、保存恢复附件、标记摘要来源、对关键事实采用结构化状态而非只靠自然语言。压缩不是无损编码，而是有损压缩加恢复提示。

### 文件历史和 Git 有什么关系

FileHistory 是应用级即时回滚，能记录单文件前后版本；Git 是仓库级版本控制，支持 diff、分支和提交。二者互补：模型工具需要在每次副作用前快速备份，长期协作需要 Git。只依赖 Git 会遇到用户未提交改动、二进制、生成文件和 worktree 生命周期问题。

### 什么时候应该使用分布式 session store

JimiCode 是本地 CLI，文件最简单；如果 Agent 运行在多实例服务中，需要跨节点恢复、并发租约、查询、过期和对象存储，就应抽象 SessionStore。核心契约应是 append、load、find boundary、list 和 optimistic version，而不是把 Path API 暴露给业务层。

## 11. 学习实验

- 手动在 session JSONL 中插入坏行，观察恢复策略。
- 构造多次 compact boundary，验证只重放最后边界后的消息。
- 让工具结果超过预算，检查原始结果、apiConv 和 replacement records 的差异。
- 修改已读取文件后调用 EditFile，确认 stale 检查。
- 在 worktree 创建失败的不同阶段终止进程，再运行 StaleCleanup，检查是否误删。
- 删除 memory 文件 frontmatter 的必填字段，观察 scanner 是否跳过而不是污染 Prompt。
- 在文件写入过程中终止程序，检查临时文件、旧文件和历史备份是否可解释。

## 12. 结论

JimiCode 的长期可用性依赖的不是某一个“保存聊天记录”的类，而是多种状态的边界：session 用于恢复，history 用于输入回看，memory 用于跨会话知识，replacement records 用于上下文视图稳定，FileHistory 用于副作用回滚，WorktreeSession 用于代码隔离。源码学习时，凡是看到 `Files.write`、`Files.move`、`ObjectMapper`、`ProcessBuilder`、`git` 和 `Thread`，都要追问它的生命周期、故障恢复和安全范围。

## 13. 逐文件深读索引

`ConversationManager.java` 是内存消息的最小操作面：添加 user/assistant、thinking、tool use、tool result 和 system reminder。它不负责文件落盘，也不应决定何时压缩；这两个职责分别由 SessionManager 和 ContextCompactor 承担。`Message.java`、`ThinkingBlock.java`、`ToolUseBlock.java`、`ToolResultBlock.java` 是协议数据结构，重点检查字段能否无损重建 provider 请求。

`SessionManager.java` 是可恢复 transcript 的聚合器，负责 sessionsDir、append、load、find boundary、rebuild、list 和 search。它同时调用 Git 获取当前 branch，并把文件大小、消息数、首条用户消息和修改时间变成 SessionInfo。`HistoryStore.java` 的边界更窄，只保存最近 200 条 prompt，连续重复去重并整体重写；不能把它当成 Agent session。`FileHistory.java` 保存文件版本，`WorktreeSessionStore.java` 保存 session 与 worktree 的映射，二者都需要显式 Path 注入以便测试。

`ContextCompactor.java` 是最长的状态变换文件之一，既生成摘要，又选择 keep tail、写 compact boundary、管理 UsageAnchor 和恢复附件。`RecoveryState.java` 是采集证据的轻量状态，记录最近文件读取和 Skill 调用；它本身不负责生成摘要。`toolresult/ContentReplacementLifecycle.java`、`ContentReplacementState.java`、`ReplacementRecordsIO.java` 和 `ToolResultBudget.java` 则负责请求视图的预算状态，不能误认为是消息删除器。

Worktree 子包的分工是：`SlugValidator` 先限制用户输入；`WorktreeManager` 执行 Git 操作并维护信息；`AgentWorktree` 表达一次创建/使用结果；`PostCreationSetup` 做创建后的初始化；`WorktreeChanges` 查询 diff/status；`WorktreeSession` 与 Store 做持久化；`StaleCleanup` 做生命周期回收。调试 Worktree 时从工具进入 Manager，再检查 Store 和 Git 命令输出，不要只看目录是否存在。

## 14. Compact boundary 的一致性证明思路

可以把 session 看成事件序列 E，把压缩结果看成函数 C(E) = (S, K, B)，其中 S 是摘要，K 是保留尾部，B 是边界之后的新事件。恢复函数 R(C(E)) 产生 [summary(S), K, B]。它不可能保证与原始 E 在 token 级完全相同，只能要求对当前任务相关状态近似保持：已完成文件、待完成任务、当前路径、工具限制、用户选择和最近 tool call 不能丢。

因此测试不能只断言恢复消息数量。应该断言：最后一个 boundary 生效；旧 boundary 不重复发送；boundary 后消息顺序不变；boundary 损坏时不会静默丢失原始消息；tool result 的 id 仍能配对；summary framing 清楚表明内容来自早期会话；RecoveryState 附件只附加真实记录，不凭空生成文件事实。

如果摘要模型失败，最安全的策略是保留原始 conv，不写一个空 summary 充当成功。若 boundary 写入失败，应该让 session file 的恢复语义显式标为不完整；如果代码采用 best effort，则至少通过通知或日志告诉用户，不能让 UI 显示“已压缩并可恢复”。

## 15. 一次长会话的状态转移

启动时 SessionManager.loadSession 读取 JSONL，ConversationManager.rebuildConversation 生成内存消息。用户输入追加到 session；Agent 每轮可能追加 system reminder、assistant output 和 tool result。工具读取文件时 RecoveryState 记录路径和快照，工具输出过大时 ToolResultBudget 在 apiConv 中建立替换记录。模型返回 context too long 时，Agent 调 ContextCompactor.forceCompact，摘要成功后追加 compact boundary，并清空旧 UsageAnchor。

下一轮使用缩小后的 conv；如果程序此时退出，JSONL 中既有旧审计消息，也有 boundary。下次 resume 时，SessionManager 反向找最后 boundary，只恢复 summary、keep tail 和后续消息。若模型继续修改文件，FileHistory 在写前建立新 snapshot，WorktreeChanges 在退出时报告 diff。这个流程横跨内存、磁盘、模型和 Git，任何一步都可能在重启后只完成一半，因此每个状态都要有 version、时间或边界标记。

## 16. 记忆污染与安全

长期记忆最大的风险不是读取失败，而是错误内容永久注入。模型可能在一次任务中把临时路径、猜测的 API、用户未确认的偏好写入 memory。MemoryManager 应区分用户显式规则、项目事实、模型摘要和临时 session note，使用不同目录或 frontmatter 类型；MemoryRecall 返回时带 source、updatedAt 和 scope，让 PromptBuilder 能提醒模型“这是记忆，不是当前文件事实”。

MemoryScanner 解析 frontmatter 时不能执行其中的脚本或自动加载任意资源。Skill/Memory 文件可能包含 prompt injection，注入系统提示前应做大小限制、来源标记和敏感信息检查。用户需要拥有查看、删除、禁用和清空记忆的入口，否则长时间使用会出现难以解释的行为漂移。

## 17. 进阶面试题

问：压缩后为什么要清空 token anchor？答：anchor 是旧消息序列上的真实 usage 参照，压缩改变了消息集合和前缀，继续使用会把新上下文误判为旧上下文的 token 分布。

问：Session JSONL 为什么不直接覆盖为压缩后的消息？答：追加边界保留审计和故障回退；覆盖会丢失原始证据，摘要失败或摘要错误时无法诊断。代价是恢复算法必须识别 boundary。

问：FileHistory 能否替代 Git？答：不能。FileHistory 面向工具级即时回滚，Git 面向仓库级协作、diff、分支和提交；Worktree 还需要 Git 的隔离语义。

问：如何处理两个进程同时写 session？答：使用文件锁、append-only 原子写、临时文件/rename 或引入带版本的 Store；仅靠 ArrayList 和 `Files.write` 不能保证跨进程一致性。

问：摘要里是否应该保留全部工具输出？答：不应。应保留对未完成任务有用的事实、路径、版本、错误和决策；完整原文放审计/文件系统，最近关键结果通过 RecoveryState 或引用恢复。

## 18. 本册验收标准

学习者应该能画出“原始 session → boundary → resume”的消息序列，解释 Layer 1 工具结果替换与 Layer 2 消息压缩的区别，指出 HistoryStore 为什么不能恢复任务，说明 FileHistory 与 WorktreeChanges 的职责差异，并能在会话最后一行损坏、摘要失败、文件被外部修改、worktree 被删除四种故障下给出不会丢数据的处理方案。
