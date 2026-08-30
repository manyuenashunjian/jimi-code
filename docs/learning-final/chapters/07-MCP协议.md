# 第 07 章：MCP协议

> 返回总稿：[JIMICODE-最终版学习教材.md](../JIMICODE-最终版学习教材.md)

<!-- 主干来源：docs/course/07-第7章-MCP协议.md -->

理解 MCP Host/Client/Server 角色以及第三方工具如何进入 Agent。学完能配置一个 MCP Server，并说明信任边界、schema 转换和失败处理。

把不可信 server 当本地可信代码；server 工具 schema 与本地 registry 冲突；进程崩溃导致 Agent 卡住；MCP 返回超大资源烧光上下文；凭据被传给错误 server。

MCP 标准化工具、资源和提示模板接入。jimicode 的 `McpManager`、`McpServerConfig` 把外部 server 接入 ToolRegistry，但权限和生命周期仍由 Host 控制。

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

**本章实际链路**：config.yaml → McpManager 启动 client/process → initialize/capabilities → tools/list → schema 映射 → model tool use → tools/call → ToolResult

McpServerConfig(name/command/args/env)、server id、capabilities、tool name/schema、call timeout、transport/process state。

| 字段 | 生产者 | 消费者 | 失败时必须保留 |
|---|---|---|---|
| 身份/名称 | 配置或注册表 | Agent/Provider/UI | 原始值与规范化值 |
| 参数/内容 | 用户、模型或工具 | 下一阶段模块 | 校验错误与来源 |
| 状态 | 编排器/持久化层 | 恢复、审计、UI | 转移前后状态 |
| 结果 | 本章实现 | Conversation/父任务 | 成功、错误、耗时 |

`src/main/java/com/jimicode/mcp/McpManager.java` 与 `config/McpServerConfig.java` 是阅读入口。Host 是 jimicode，Client 是 Host 与某个 server 的连接对象，Server 是外部进程或远端服务。启动时读取 command/args，建立 transport，协商能力，再把 tools/list 转换成内部 schema；调用时保留 server 名称和原始 tool 名，避免同名冲突。

接入流程要分为配置、启动、能力发现、权限注册、调用、关闭六步。配置中不要写长期秘密；环境变量通过受控映射传入。启动要有超时和 stderr 捕获；能力发现失败可以禁用该 server，但必须在 TUI/日志中可见。调用要限制参数、超时、输出大小和并发，server 返回的资源不能无上限回灌。

第三方 MCP 的信任边界比本地 Tool 更宽：它可能访问网络、文件、数据库或执行自己的进程。权限 UI 应显示 server 名称、tool 名称、关键参数和数据去向。不要因为 schema 看起来安全就跳过 server 级别审核。

反模式一是把所有 MCP 工具自动注册为永久允许；反模式二是只在进程启动时校验，运行中 server 能力变化却不更新；反模式三是把 server 原始错误和敏感环境变量完整回显给模型。

**目标**：配置一个只读 MCP Server，验证启动、tools/list、schema 显示和一次调用；记录 server stderr 与退出码。

**目标**：让 mock server 返回超长结果、错误和延迟，验证超时、截断和 isError，不连接未知第三方服务。

server 不出现→查配置路径/command/args；tools/list 失败→看 stderr/初始化超时；同名冲突→检查命名空间；调用卡住→查 transport timeout；输出过大→查 MCP 结果预算和 compaction。

### 题 1：MCP Host/Client/Server 如何分工？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 2：MCP Tool 与本地 Tool 的权限差异？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 3：server 崩溃如何恢复？

**回答提纲**：先给概念定义，再引用一个 jimicode 文件/类，说明一次取舍和一个失败场景。**加分点**：能讲测试、指标和边界。
### 题 4：为什么要限制 MCP 返回大小？

本章建立的契约会被下一章消费：上下文管理 只有在输入、状态、失败和权限边界明确后才不会把复杂度继续放大。

### FAQ 1：MCP 是模型直接连接 server 吗？不是，通常由 Host/Client 代为管理。

以当前项目源码和测试为准；第三方或闭源行为要明确标注边界。
### FAQ 2：能否信任官方 server？仍需按权限、版本和数据边界审查。

先用最小实验复现，再沿事件、日志、状态和调用者逐层定位。
### FAQ 3：MCP 只能提供工具吗？协议也可扩展资源、提示等能力，具体以实现协商为准。

从当前章节的真实入口开始：config.yaml → McpManager 启动 client/process → initialize/capabilities → tools/list → schema 映射 → model tool use → tools/call → ToolResult。打开链路中的第一个 jimicode 类，记录构造器参数、字段初始化和直接调用者；再向后追踪一个结果对象的消费点。不要只搜索类名，要搜索方法调用和事件类型。

本章的输入不能只写成“用户问题”。应拆成配置输入、自然语言输入、模型结构化输入、文件/进程输入和上游模块状态。对每一种输入写明允许为空的字段、长度上限、编码、来源和校验时机。McpServerConfig(name/command/args/env)、server id、capabilities、tool name/schema、call timeout、transport/process state。 中出现的身份、参数和状态字段都要在进入下一层前规范化。

将本章失败拆成输入错误、依赖错误、业务拒绝、超时、取消、部分成功和持久化失败。针对当前主题 `上下文管理` 的前置依赖，写出“失败后能否重试、是否幂等、是否需要人工确认、是否需要回滚”的结论。

下一步是 `上下文管理`。提前列出本章必须对外稳定的字段和事件；如果下一章要并发、压缩或扩展，本章的 id、版本、错误和取消语义必须先固定。

把本章放回完整闭环：config.yaml → McpManager 启动 client/process → initialize/capabilities → tools/list → schema 映射 → model tool use → tools/call → ToolResult。如果删除本章能力，系统会在这些地方退化：把不可信 server 当本地可信代码；server 工具 schema 与本地 registry 冲突；进程崩溃导致 Agent 卡住；MCP 返回超大资源烧光上下文；凭据被传给错误 server。。如果增强本章能力，优先增强契约、测试、可观测性和安全边界，再考虑增加模型或 Agent 数量。最后用一个真实但隔离的实验验证“输入—状态—结果—失败—恢复”五个维度，并把证据保存到学习笔记。
## 综合案例与边界演练

### 场景 1：最小成功路径：使用固定输入、单一依赖和一个工具完成一次操作，记录从入口到结果的每个对象。

本章的具体检查点是：MCP 标准化工具、资源和提示模板接入。jimicode 的 `McpManager`、`McpServerConfig` 把外部 server 接入 ToolRegistry，但权限和生命周期仍由 Host 控制。 生产的输入必须经过本章状态和权限边界，再形成 上下文管理 可以消费的结果。实验结束后保存命令、事件、日志、测试、diff 和限制说明。

### 场景 2：空值路径：删除可选字段、返回空集合或让模型不给出内容，确认系统使用默认值、明确错误还是安全退出。

### 场景 3：重复路径：重复提交同一个请求、toolUseId、命令或消息，检查是否幂等、去重或要求人工确认。

### 场景 4：超时路径：让网络、脚本、文件或子任务延迟，检查 timeout、取消、资源释放和用户提示。

### 场景 5：部分成功路径：让多个动作中只有一部分成功，确认结果如何汇总、是否需要补偿或回滚。

### 场景 6：权限路径：使用只读、询问和拒绝三种模式，确保决策发生在副作用之前且结果可审计。

### 场景 7：恢复路径：在关键状态写入前后模拟进程退出，重新启动后检查状态、消息顺序和重复执行风险。

### 场景 8：观测路径：为一次操作关联 id、阶段、耗时和错误类别，确认 UI、日志和 trace 的信息粒度不同但事实一致。

### 场景 9：兼容路径：替换 provider、工具实现或配置版本，确认核心契约不需要大面积改动。

### 场景 10：评审路径：邀请另一位同学只看文档和证据复现实验，记录他无法理解或无法复现的地方。

围绕本章的实际链路 `config.yaml → McpManager 启动 client/process → initialize/capabilities → tools/list → schema 映射 → model tool use → tools/call → ToolResult`，在源码中定位一个具体类/方法，写出它的输入、输出、状态、调用者和失败处理。然后在隔离环境执行一个最小验证，保存测试/日志/diff；如果无法从当前仓库证实，明确写【待源码确认】。针对 `上下文管理` 的后续能力，补充说明该契约为什么必须稳定、哪些字段不能随意改名，以及发生故障时如何向用户解释。
