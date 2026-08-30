# 第 15 章：Agent Teams

> 返回总稿：[JIMICODE-最终版学习教材.md](../JIMICODE-最终版学习教材.md)

<!-- 主干来源：docs/course/15-第15章-Agent-Teams.md -->

把 SubAgent 提升为长期角色、任务看板、邮箱和人类主管协作系统。

成员职责不清；任务状态无人更新；消息重复/丢失；多个成员争抢同一文件；成本随成员数线性甚至超线性增长；没有人类最终裁决。

Team 是成员、角色、共享任务状态和协作协议的组合。`TeamManager`、`FileMailBox`、`SharedTaskStore`、`TeammateProgress`、`TeammateRunner` 和 tmux backend 构成 jimicode 团队层。

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

**本章实际链路**：Team create → add members → shared tasks/mailbox → member Agent run → progress/report → human supervisor review → merge/stop/cleanup

team name/mode、member、role、task id/status、mail message id/from/to/content、progress、worktree path、stop state。

| 字段 | 生产者 | 消费者 | 失败时必须保留 |
|---|---|---|---|
| 身份/名称 | 配置或注册表 | Agent/Provider/UI | 原始值与规范化值 |
| 参数/内容 | 用户、模型或工具 | 下一阶段模块 | 校验错误与来源 |
| 状态 | 编排器/持久化层 | 恢复、审计、UI | 转移前后状态 |
| 结果 | 本章实现 | Conversation/父任务 | 成功、错误、耗时 |

阅读 `src/main/java/com/jimicode/teams/TeamManager.java`、`FileMailBox.java`、`SharedTaskStore.java`、`TeammateProgress.java`、`TeammateRunner.java`。TeamManager 管成员生命周期，FileMailBox 负责跨成员消息，SharedTaskStore 负责任务看板，TmuxBackend/ITermBackend 负责进程或 pane 后端。不要把显示后端当作协作协议。

长期团队需要角色契约：前端成员负责 UI 文件，后端成员负责 API，测试成员负责复现与回归，主管负责拆分、审批、合并和冲突裁决。每个任务应有 owner、输入、输出、依赖、状态和证据；状态转移要可审计，消息要有唯一 id、时间和 ack/重放策略。

人类主管不是多余环节。涉及架构冲突、权限升级、数据库迁移、合并主分支和成本异常时必须暂停。成员之间的共享上下文应通过摘要、任务结果和路径引用传递，避免复制完整对话。

反模式一是只增加成员数量不增加协议；反模式二是用文件名猜冲突而不使用 Git diff；反模式三是成员无限等待邮箱，没有超时、心跳和退出。

**目标**：创建前端/后端/测试三个角色，分别领取只读分析任务，使用 FileMailBox 交换结论，主管手工汇总。

**目标**：故意让两个成员修改同一临时文件，演练冲突报告、暂停合并和人工裁决；不自动强制覆盖。

成员无进展→查 progress/heartbeat；消息丢失→查 mailbox 原子写/消费；任务重复→查 task id/ack；冲突未发现→查 worktree diff；成本过高→查成员并发/maxTurns。

### 题 1：Teams 与 SubAgent 的边界？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 2：文件邮箱如何保证可靠性？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 3：为什么需要 human supervisor？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 4：多 Agent 成本如何治理？

本章建立的契约会被下一章消费：回顾与展望 只有在输入、状态、失败和权限边界明确后才不会把复杂度继续放大。

### FAQ 1：Team 一定要 tmux 吗？不一定，IN_PROCESS 也可运行；tmux 只是后端。

以当前项目源码和测试为准；第三方或闭源行为要明确标注边界。
### FAQ 2：成员可以共享一个 Agent 实例吗？通常不应并发共享非线程安全实例。

先用最小实验复现，再沿事件、日志、状态和调用者逐层定位。
### FAQ 3：共享看板需要数据库吗？本地课程可用文件协议，生产需更强一致性。

从当前章节的真实入口开始：Team create → add members → shared tasks/mailbox → member Agent run → progress/report → human supervisor review → merge/stop/cleanup。打开链路中的第一个 jimicode 类，记录构造器参数、字段初始化和直接调用者；再向后追踪一个结果对象的消费点。不要只搜索类名，要搜索方法调用和事件类型。

本章的输入不能只写成“用户问题”。应拆成配置输入、自然语言输入、模型结构化输入、文件/进程输入和上游模块状态。对每一种输入写明允许为空的字段、长度上限、编码、来源和校验时机。team name/mode、member、role、task id/status、mail message id/from/to/content、progress、worktree path、stop state。 中出现的身份、参数和状态字段都要在进入下一层前规范化。

将本章失败拆成输入错误、依赖错误、业务拒绝、超时、取消、部分成功和持久化失败。针对当前主题 `回顾与展望` 的前置依赖，写出“失败后能否重试、是否幂等、是否需要人工确认、是否需要回滚”的结论。

下一步是 `回顾与展望`。提前列出本章必须对外稳定的字段和事件；如果下一章要并发、压缩或扩展，本章的 id、版本、错误和取消语义必须先固定。

把本章放回完整闭环：Team create → add members → shared tasks/mailbox → member Agent run → progress/report → human supervisor review → merge/stop/cleanup。如果删除本章能力，系统会在这些地方退化：成员职责不清；任务状态无人更新；消息重复/丢失；多个成员争抢同一文件；成本随成员数线性甚至超线性增长；没有人类最终裁决。。如果增强本章能力，优先增强契约、测试、可观测性和安全边界，再考虑增加模型或 Agent 数量。最后用一个真实但隔离的实验验证“输入—状态—结果—失败—恢复”五个维度，并把证据保存到学习笔记。
## 综合案例与边界演练

### 场景 1：最小成功路径：使用固定输入、单一依赖和一个工具完成一次操作，记录从入口到结果的每个对象。

本章的具体检查点是：Team 是成员、角色、共享任务状态和协作协议的组合。`TeamManager`、`FileMailBox`、`SharedTaskStore`、`TeammateProgress`、`TeammateRunner` 和 tmux backend 构成 jimicode 团队层。 生产的输入必须经过本章状态和权限边界，再形成 回顾与展望 可以消费的结果。实验结束后保存命令、事件、日志、测试、diff 和限制说明。

### 场景 2：空值路径：删除可选字段、返回空集合或让模型不给出内容，确认系统使用默认值、明确错误还是安全退出。

### 场景 3：重复路径：重复提交同一个请求、toolUseId、命令或消息，检查是否幂等、去重或要求人工确认。

### 场景 4：超时路径：让网络、脚本、文件或子任务延迟，检查 timeout、取消、资源释放和用户提示。

### 场景 5：部分成功路径：让多个动作中只有一部分成功，确认结果如何汇总、是否需要补偿或回滚。

### 场景 6：权限路径：使用只读、询问和拒绝三种模式，确保决策发生在副作用之前且结果可审计。

### 场景 7：恢复路径：在关键状态写入前后模拟进程退出，重新启动后检查状态、消息顺序和重复执行风险。

### 场景 8：观测路径：为一次操作关联 id、阶段、耗时和错误类别，确认 UI、日志和 trace 的信息粒度不同但事实一致。

### 场景 9：兼容路径：替换 provider、工具实现或配置版本，确认核心契约不需要大面积改动。

### 场景 10：评审路径：邀请另一位同学只看文档和证据复现实验，记录他无法理解或无法复现的地方。

围绕本章的实际链路 `Team create → add members → shared tasks/mailbox → member Agent run → progress/report → human supervisor review → merge/stop/cleanup`，在源码中定位一个具体类/方法，写出它的输入、输出、状态、调用者和失败处理。然后在隔离环境执行一个最小验证，保存测试/日志/diff；如果无法从当前仓库证实，明确写【待源码确认】。针对 `回顾与展望` 的后续能力，补充说明该契约为什么必须稳定、哪些字段不能随意改名，以及发生故障时如何向用户解释。
