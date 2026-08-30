# JiMI Code / jimicode 自学课程总导航

> 项目定位声明：JiMI Code、jimicode、Jimecode Agent 在本文中指同一个课程项目的不同写法。它是一个“仿 Claude Code 理念的课程项目/自研实现”，用于学习 Coding Agent 的架构与工程实践，不是 Anthropic Claude Code 的官方实现，也不声称复制了任何闭源内部代码。

## 使用方式

本课程以仓库当前源码为准。已确认的事实会标记为【jimicode 实现】；用于解释通用原理、但不一定由本仓库实现的内容标记为【课程概念】；没有足够源码证据的内容标记为【待源码确认】。源码路径均以 `jimi-code/` 为根目录。

## 总目录

0. [必读｜JiMI Code 课程学习攻略](./00-必读-课程学习攻略.md)
1. [第1章：初识 Coding Agent](./01-第1章-初识-Coding-Agent.md)
2. [第2章：让 AI 开口说话](./02-第2章-让AI开口说话.md)
3. [第3章：工具系统](./03-第3章-工具系统.md)
4. [第4章：让 Agent 自己干活](./04-第4章-Agent-Loop.md)
5. [第5章：System Prompt 设计](./05-第5章-System-Prompt.md)
6. [第6章：权限系统，给 Agent 装上安全刹车](./06-第6章-权限系统.md)
7. [第7章：MCP 协议 - 开放式工具生态](./07-第7章-MCP协议.md)
8. [第8章：上下文管理，当 Token 开始烧钱](./08-第8章-上下文管理.md)
9. [第9章：记忆系统 - 跨会话的 Agent 记忆](./09-第9章-记忆系统.md)
10. [第10章：Slash Command，内置命令框架](./10-第10章-Slash-Command.md)
11. [第11章：Skill 系统，可复用的技能包](./11-第11章-Skill系统.md)
12. [第12章：Hook 系统 - 生命周期钩子与自动化](./12-第12章-Hook系统.md)
13. [第13章：SubAgent，子 Agent 与任务分发](./13-第13章-SubAgent.md)
14. [第14章：Worktree，Git 工作树并行开发](./14-第14章-Worktree.md)
15. [第15章：Agent Teams，从一次性子任务到长期协作团队](./15-第15章-Agent-Teams.md)
16. [第16章：回顾与展望，从一个空壳到一支团队](./16-第16章-回顾与展望.md)
17. [第17章：面试求职全攻略](./17-第17章-面试求职全攻略.md)

### 附录卷

- [附录 A：Claude Code 配置教程（对标与迁移）](./A-Claude-Code配置教程.md)
- [附录 B：Jimecode Agent 源码阅读与目录导读](./B-jimicode源码导读.md)
- [附录 C：项目简历模板](./C-项目简历模板.md)
- [附录 D：Vibe Coding 提示词复用手册](./D-Vibe-Coding提示词手册.md)

## 术语表

| 术语 | 课程统一定义 | 在 jimicode 中的对应证据 |
|---|---|---|
| Coding Agent | 能读取环境、调用工具、修改代码并根据结果继续推理的智能体 | `Agent`、`ToolRegistry`、文件工具、`BashTool` |
| LLM Provider | 提供模型推理 API 的服务方或兼容端点 | `AnthropicClient`、`OpenAiClient`、`OpenAiCompatClient` |
| Message | 发送给模型或从模型返回的一条带角色的消息 | `conversation/Message.java` |
| ToolUse | 模型请求执行某个工具的结构化调用 | `ToolUseBlock` |
| ToolResult | 工具执行后回传模型的结果 | `ToolResultBlock`、`ToolResult` |
| Agent Loop | 推理→行动→观察→再推理的循环 | `Agent.agentLoop` |
| ReAct | Reasoning and Acting，交替进行思考与行动 | 课程概念；jimicode 以 Agent Loop 实现 |
| System Prompt | 规定 Agent 角色、规则、工具规范和输出约束的系统消息 | `PromptBuilder`、`PromptSections` |
| Permission Mode | 决定工具自动允许、询问确认还是拒绝的策略 | `PermissionMode`、`PermissionChecker` |
| MCP | Model Context Protocol，标准化外部工具/资源接入的协议 | `McpManager`、`McpServerConfig` |
| Context Window | 模型一次请求可接收的上下文 token 上限 | `ProviderConfig` 的 context window 字段 |
| Compaction | 在预算不足时压缩历史、替换工具结果并保存恢复附件 | `ContextCompactor`、`RecoveryState` |
| Memory | 跨 turn 或跨会话可召回的长期信息 | `MemoryScanner`、`MemoryRecall`、`MemoryManager` |
| Slash Command | 以 `/` 开头的显式用户命令 | `CommandRegistry`、`Command` |
| Skill | 文档、脚本和触发条件组成的可复用能力包 | `SkillCatalog`、`SkillExecutor` |
| Hook | 在生命周期节点执行的观察/自动化逻辑 | `HookEngine`、`HookConfig` |
| SubAgent | 为隔离上下文或专业任务创建的子 Agent | `SubAgentTaskManager`、`AgentTool` |
| Worktree | Git 提供的独立工作树，用于并行修改而不互相覆盖 | `WorktreeManager`、`WorktreeSession` |
| Agent Teams | 多个长期运行的 Agent 成员及其协作协议 | `TeamManager`、`FileMailBox` |
| TUI | Terminal User Interface，终端交互界面 | `JimiCodeModel`、`MarkdownRenderer` |
| Provider-neutral | 不绑定某一个模型厂商的内部消息/事件表示 | `LlmClient` 与 `StreamEvent` 的抽象边界 |
| HITL | Human-in-the-loop，人类在高风险动作前确认 | `AskUserDialog`、`PlanApprovalDialog` |

## 能力演进总图

```mermaid
flowchart LR
    A[空壳 CLI] --> B[模型聊天]
    B --> C[统一消息与流式事件]
    C --> D[Tool 注册与结果回传]
    D --> E[Agent Loop / ReAct]
    E --> F[System Prompt 与项目规则]
    F --> G[Permission / HITL 安全刹车]
    G --> H[MCP 外部工具生态]
    H --> I[Context Budget / Compaction]
    I --> J[Memory 跨会话记忆]
    J --> K[Slash Command]
    K --> L[Skill 可复用能力]
    L --> M[Hook 自动化与审计]
    M --> N[SubAgent 任务分发]
    N --> O[Git Worktree 隔离]
    O --> P[Agent Teams 长期协作]
    P --> Q[可解释、可恢复、可治理的 Coding Agent]
```

## 【修订记录】

- 2026-08-29：根据当前 `jimi-code` 的 Java 21、Maven、TUI4J、Anthropic/OpenAI、MCP、权限、压缩、Memory、Worktree 和 Teams 源码对课程入口进行了对齐。
- 2026-08-29：明确“索引文档”和“语义学习文档”的差异；本课程从第 0 章、第 1 章开始逐章补齐。

## 全部完成后的复习资料

- [一页纸速查](./最终复习-一页纸速查.md)
- [作业总表](./最终复习-作业总表.md)
- [面试题索引](./最终复习-面试题索引.md)
