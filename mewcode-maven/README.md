# MewCode Maven

MewCode 的独立 Java 21 + Maven 实现。项目从同仓库现有 Java 实现迁移，保留 Agent Loop、LLM Provider、工具、权限、上下文压缩、Session、Memory、Skill、MCP、SubAgent、Team、Task、TUI 和 Git Worktree 能力。

## 环境要求

- Java 21 或更高版本
- Maven 3.9 或使用项目自带 Maven Wrapper
- Windows 使用 `Bash` 工具时，需要 Git Bash、WSL 或其他可用的 `bash.exe`

## 配置

在启动目录创建 `.mewcode/config.yaml`：

```powershell
New-Item -ItemType Directory -Force .mewcode
Copy-Item config.example.yaml .mewcode\config.yaml
```

API Key 可以写入 Provider 的 `api_key`，也可以通过环境变量提供：

- Anthropic：`ANTHROPIC_API_KEY`
- OpenAI/OpenAI-compatible：`OPENAI_API_KEY`

支持三个配置层，后层覆盖或合并前层：

1. `~/.mewcode/config.yaml`
2. `<当前目录>/.mewcode/config.yaml`
3. `<当前目录>/.mewcode/config.local.yaml`

也可以把显式配置文件作为第一个参数传入，或设置 `MEWCODE_CONFIG`。

## 构建和测试

Windows 推荐直接使用自动选择 Java 21 的构建脚本：

```powershell
.\build.cmd
```

可以把 Maven 参数继续传给脚本，例如 `build.cmd clean package -DskipTests`。

直接调用 Maven Wrapper 时，当前终端必须先指向 JDK 21。cmd.exe：

```bat
set "JAVA_HOME=E:\codesoft\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
mvnw.cmd clean verify
```

PowerShell：

```powershell
$env:JAVA_HOME = "E:\codesoft\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
```

Linux/macOS：

```bash
./mvnw clean verify
```

构建会生成可执行 fat JAR：

```text
target/mewcode.jar
```

## 运行

```powershell
java -jar target\mewcode.jar
```

使用显式配置：

```powershell
java -jar target\mewcode.jar D:\path\to\config.yaml
```

## 主要包

| 包 | 职责 |
|---|---|
| `agent` | Agent Loop、流事件和并行工具执行 |
| `llm` | Anthropic、OpenAI Responses、OpenAI-compatible 适配器 |
| `tool` | Tool 接口、Registry 和内置文件/命令工具 |
| `permission` | 权限模式、路径沙箱和 Always Allow |
| `compact`、`toolresult` | 对话压缩及大工具结果 spill/snip |
| `session`、`history`、`filehistory` | 会话、输入历史和文件回退 |
| `memory`、`skill`、`mcp` | 长期记忆、Skill 和 MCP 扩展 |
| `subagent`、`teams`、`task` | 子 Agent、长期团队和任务协作 |
| `worktree` | Git Worktree 隔离和清理 |
| `tui` | Provider 选择、聊天和交互对话框 |
