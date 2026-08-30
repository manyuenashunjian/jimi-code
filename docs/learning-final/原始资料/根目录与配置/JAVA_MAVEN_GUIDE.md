# JimiCode Java/Maven 项目指南

本文只描述当前仓库中实际保留的 Java 21 实现。项目已经统一使用 Maven；不再把其他语言实现或旧构建系统当作当前运行入口。

## 1. 项目定位

JimiCode 是运行在终端中的 AI Coding Agent。它把模型流式响应转换为统一事件，允许模型在权限检查后调用文件、搜索、命令、MCP、子 Agent、任务和 Git worktree 工具。程序不是 HTTP 服务，没有 Controller 或 Spring 容器；`JimiCode.main` 创建配置、模型和 TUI，`JimiCodeModel` 负责交互状态，`Agent` 负责循环。

## 2. 目录

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

## 3. 构建与运行

要求 JDK 21、Maven 3.9 以上。使用 Wrapper 可避免本机 Maven 版本差异：

```powershell
.\mvnw.cmd -DskipTests package
java -jar target\jimicode.jar
```

Linux/macOS：

```bash
./mvnw -DskipTests package
java -jar target/jimicode.jar
```

测试命令是 `mvn test`。如果离线仓库缺少 Surefire provider，编译仍可用 `-DskipTests package` 验证；联网后再运行完整测试。

## 4. 配置查找顺序

`ConfigLoader.load(null)` 依次读取：

1. `~/.JimiCode/config.yaml`；
2. 当前项目 `.JimiCode/config.yaml`；
3. 当前项目 `.JimiCode/config.local.yaml`。

后层只覆盖已提供字段；MCP 按名称替换或追加，Hook 追加。显式传入命令行路径时只读取该文件。API Key 优先使用 `api_key`，为空时按协议读取 `ANTHROPIC_API_KEY` 或 `OPENAI_API_KEY`。

## 5. 继续阅读

完整逐文件教材位于 [`study-notes/sourcebook/README.md`](../study-notes/sourcebook/README.md)。建议先读配置和消息模型，再读 `Agent`，然后读一个内置工具，最后读 LLM、MCP、Session、SubAgent 和 Worktree。每一章都区分“当前源码事实”“Java/框架原理”和“生产建议”。
