# MewCode / jimi-code

MewCode 是一个 AI Coding Agent 的完整实现，帮助你理解 AI Agent 的核心原理与工程实践。当前仓库的 Java 主实现已经切换为 Java 21 + Maven；Go 实现继续保留在 `cmd/` 与 `internal/`，作为并行参考实现。

## 环境要求

- Java 21+
- Maven 3.9+（项目自带 Maven Wrapper，无需单独安装）
- Windows 建议使用 JDK 21 和 `mvnw.cmd`

## 快速开始

### 1. 配置

编辑 `.mewcode/config.yaml`，填入你的 LLM 提供商信息：

```yaml
providers:
  - name: anthropic-official
    protocol: anthropic                          # 支持 anthropic / openai 两种协议
    base_url: https://your-api-provider.com/api/anthropic
    api_key: "your-api-key-here"
    model: claude-sonnet-4-6
    thinking: true                               # 是否开启 extended thinking

mcp_servers:
  - name: context7
    command: npx
    args: ["-y", "@upstash/context7-mcp"]
```

**说明：**
- `protocol`：填 `anthropic` 或 `openai`，取决于你的提供商兼容哪种 API
- `base_url`：你的 API 地址
- `api_key`：你的 API Key
- `model`：模型名称
- `mcp_servers`：MCP Server 列表，每项需要 `name`、`command` 和 `args`

### 2. 构建 & 运行 Java/Maven 版本

```bash
./mvnw clean package
java -jar target/mewcode.jar
```

### 3. 运行测试

```bash
./mvnw test
```

Windows PowerShell：

```powershell
.\mvnw.cmd clean verify
java -jar target\mewcode.jar
```

### Go 参考实现

Go 版本仍位于 `cmd/mewcode`、`internal` 和 `go.mod`，可使用 `go test ./...` 验证。它与 Java/Maven 版本共享 Agent、LLM、工具、权限、MCP、会话和压缩等概念，但不是同一套运行时；不要把 Go 构建结果当作 Maven 构建证据。

### 源码教材

详细中文源码教材位于 [`study-notes/sourcebook`](study-notes/sourcebook/README.md)，包含 Java/Go 双实现架构、Maven 配置、Agent Loop、工具权限、MCP、会话恢复、上下文压缩、Worktree 和复刻练习。
