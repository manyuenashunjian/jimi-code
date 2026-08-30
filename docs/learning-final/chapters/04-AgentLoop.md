# 第 04 章：Agent Loop

> 返回总稿：[JIMICODE-最终版学习教材.md](../JIMICODE-最终版学习教材.md)

<!-- 主干来源：docs/course/04-第4章-Agent-Loop.md -->

把模型和工具连接成 ReAct/Agent Loop。学完能追踪一次“读文件—分析—修改—测试”的完整时序，并治理最大步数、终止条件、取消和死循环。

只调用一次模型就结束；工具结果没有回灌；模型反复调用同一个失败工具；无上限循环烧光 token；Ctrl-C 后留下孤儿工具调用。

ReAct = Reasoning + Acting；Loop 维护 iteration、conversation、工具结果和终止状态。`Agent.agentLoop` 是 jimicode 的核心循环，`AgentEvent` 让 TUI 观察过程。

| 对比维度 | 本章能力 | 上一章能力 | 下一章依赖 |
|---|---|---|---|
| 输入 | 结构化、可校验的本章对象 | 上一层输出 | 为下一层提供稳定契约 |
| 控制 | 明确的状态/权限/预算 | 规则较少 | 可观测、可恢复、可扩展 |
| 失败 | 分类、可见、可恢复 | 可能只返回错误文本 | 由后续治理层接管 |

```mermaid
flowchart LR
    A[用户/TUI] --> B[本章入口]
    B --> C[核心状态/协议]
    C --> D[源码模块]
    D --> E[事件/结果]
    E --> F[下一轮推理或用户反馈]
```

**本章实际链路**：输入 → agentLoop iteration → schema/compact → LLM stream → text/thinking/tool use → permission → tool → result → conversation → next iteration/complete

iteration、maxIterations、ConversationManager、BlockingQueue<AgentEvent>、ToolUseBlock、ToolResultBlock、LoopComplete/ErrorEvent、cancel/interrupted。

| 字段 | 生产者 | 消费者 | 失败时必须保留 |
|---|---|---|---|
| 身份/名称 | 配置或注册表 | Agent/Provider/UI | 原始值与规范化值 |
| 参数/内容 | 用户、模型或工具 | 下一阶段模块 | 校验错误与来源 |
| 状态 | 编排器/持久化层 | 恢复、审计、UI | 转移前后状态 |
| 结果 | 本章实现 | Conversation/父任务 | 成功、错误、耗时 |

阅读 `src/main/java/com/jimicode/agent/Agent.java` 的 `run` 与 `agentLoop`。`run` 创建容量有限的 `LinkedBlockingQueue` 并启动虚拟线程；`agentLoop` 先注入 instructions/memory，再在每轮检查最大迭代和线程中断，计算工具 schema、执行压缩、注入系统提醒，然后调用模型。收到工具调用后执行并把结果写回 conversation，下一轮重新推理。

终止条件至少包括：模型返回无 ToolUse 的最终文本；达到 `maxIterations`；用户拒绝或取消；不可恢复的配置/协议异常。成功与失败都要有 AgentEvent，让 TUI、父 Agent 和历史记录获得一致事实。队列容量 64 是一个背压边界，不能在事件生产速度远大于 UI 消费速度时无限缓存。

死循环治理要组合使用 maxIterations、相同 toolUse/参数去重、连续相同错误检测、每轮 token 预算、超时和人工确认。不要只依赖 Prompt 告诉模型“不要循环”，因为这是软约束。取消时要先检查线程中断，再让 HTTP、子进程和工具执行响应取消；若已有 ToolUse 没有结果，应合成错误 ToolResult，保证消息协议完整。

反模式一是递归调用 Agent 而没有计数器，栈和 token 都不可控。反模式二是把所有工具执行放在 UI 线程，导致界面冻结且无法取消。反模式三是 catch(Exception) 后直接返回空字符串，用户无法判断是模型完成还是系统失败。

**目标**：在临时仓库让 Agent 先读取一个 Java 文件再回答，记录每个 AgentEvent 的顺序、iteration 和 ToolResult。

**目标**：构造一个总是返回错误的 mock Tool，验证 maxIterations、重复错误检测和 Ctrl-C 取消；成功标准是不会无限请求。

只执行一轮→查 ToolResult 是否追加；无限循环→查 maxIterations/终止条件；UI 卡顿→查工具是否跑在虚拟线程；取消无效→查 interrupt 是否传到 HTTP/进程；队列超时→查消费者和 putSafe。

### 题 1：ReAct Loop 与工作流引擎有何不同？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 2：最大迭代数放在哪里配置？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 3：如何处理孤儿 ToolUse？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 4：背压和取消为什么是 Agent 的核心问题？

本章建立的契约会被下一章消费：System Prompt 设计 只有在输入、状态、失败和权限边界明确后才不会把复杂度继续放大。

### FAQ 1：模型一次返回多个工具怎么办？按 id 保存并执行，必要时按 concurrencySafe 决定并发。

以当前项目源码和测试为准；第三方或闭源行为要明确标注边界。
### FAQ 2：达到最大迭代算成功吗？通常应产生明确错误/未完成事件，不能冒充最终答案。

先用最小实验复现，再沿事件、日志、状态和调用者逐层定位。
### FAQ 3：为什么不直接 while(true)？因为循环必须可预算、可取消、可观测。

从当前章节的真实入口开始：输入 → agentLoop iteration → schema/compact → LLM stream → text/thinking/tool use → permission → tool → result → conversation → next iteration/complete。打开链路中的第一个 jimicode 类，记录构造器参数、字段初始化和直接调用者；再向后追踪一个结果对象的消费点。不要只搜索类名，要搜索方法调用和事件类型。

本章的输入不能只写成“用户问题”。应拆成配置输入、自然语言输入、模型结构化输入、文件/进程输入和上游模块状态。对每一种输入写明允许为空的字段、长度上限、编码、来源和校验时机。iteration、maxIterations、ConversationManager、BlockingQueue<AgentEvent>、ToolUseBlock、ToolResultBlock、LoopComplete/ErrorEvent、cancel/interrupted。 中出现的身份、参数和状态字段都要在进入下一层前规范化。

将本章失败拆成输入错误、依赖错误、业务拒绝、超时、取消、部分成功和持久化失败。针对当前主题 `System Prompt 设计` 的前置依赖，写出“失败后能否重试、是否幂等、是否需要人工确认、是否需要回滚”的结论。

下一步是 `System Prompt 设计`。提前列出本章必须对外稳定的字段和事件；如果下一章要并发、压缩或扩展，本章的 id、版本、错误和取消语义必须先固定。

把本章放回完整闭环：输入 → agentLoop iteration → schema/compact → LLM stream → text/thinking/tool use → permission → tool → result → conversation → next iteration/complete。如果删除本章能力，系统会在这些地方退化：只调用一次模型就结束；工具结果没有回灌；模型反复调用同一个失败工具；无上限循环烧光 token；Ctrl-C 后留下孤儿工具调用。。如果增强本章能力，优先增强契约、测试、可观测性和安全边界，再考虑增加模型或 Agent 数量。最后用一个真实但隔离的实验验证“输入—状态—结果—失败—恢复”五个维度，并把证据保存到学习笔记。
## 综合案例与边界演练

### 场景 1：最小成功路径：使用固定输入、单一依赖和一个工具完成一次操作，记录从入口到结果的每个对象。

本章的具体检查点是：ReAct = Reasoning + Acting；Loop 维护 iteration、conversation、工具结果和终止状态。`Agent.agentLoop` 是 jimicode 的核心循环，`AgentEvent` 让 TUI 观察过程。 生产的输入必须经过本章状态和权限边界，再形成 System Prompt 设计 可以消费的结果。实验结束后保存命令、事件、日志、测试、diff 和限制说明。

### 场景 2：空值路径：删除可选字段、返回空集合或让模型不给出内容，确认系统使用默认值、明确错误还是安全退出。

### 场景 3：重复路径：重复提交同一个请求、toolUseId、命令或消息，检查是否幂等、去重或要求人工确认。

### 场景 4：超时路径：让网络、脚本、文件或子任务延迟，检查 timeout、取消、资源释放和用户提示。

### 场景 5：部分成功路径：让多个动作中只有一部分成功，确认结果如何汇总、是否需要补偿或回滚。

### 场景 6：权限路径：使用只读、询问和拒绝三种模式，确保决策发生在副作用之前且结果可审计。

### 场景 7：恢复路径：在关键状态写入前后模拟进程退出，重新启动后检查状态、消息顺序和重复执行风险。

### 场景 8：观测路径：为一次操作关联 id、阶段、耗时和错误类别，确认 UI、日志和 trace 的信息粒度不同但事实一致。

### 场景 9：兼容路径：替换 provider、工具实现或配置版本，确认核心契约不需要大面积改动。

### 场景 10：评审路径：邀请另一位同学只看文档和证据复现实验，记录他无法理解或无法复现的地方。

围绕本章的实际链路 `输入 → agentLoop iteration → schema/compact → LLM stream → text/thinking/tool use → permission → tool → result → conversation → next iteration/complete`，在源码中定位一个具体类/方法，写出它的输入、输出、状态、调用者和失败处理。然后在隔离环境执行一个最小验证，保存测试/日志/diff；如果无法从当前仓库证实，明确写【待源码确认】。针对 `System Prompt 设计` 的后续能力，补充说明该契约为什么必须稳定、哪些字段不能随意改名，以及发生故障时如何向用户解释。
