# JimiCode

JimiCode 是一个 AI Coding Agent 的完整 Java 实现，帮助你理解 AI Agent 的核心原理与工程实践。当前仓库已统一为 Java 21 + Maven 构建。

## 环境要求

- Java 21+
- Maven 3.9+

## 快速开始

### 1. 配置

编辑 `.jimicode/config.yaml`，填入你的 LLM 提供商信息：

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
mvn clean package
java -jar target/jimicode.jar
```

### 3. 运行测试

```bash
mvn test
```

Windows PowerShell：

```powershell
mvn.cmd clean verify
java -jar target\jimicode.jar
```

### 源码教材

详细中文源码教材位于 [`study-notes/sourcebook`](study-notes/sourcebook/README.md)，包含 Java/Maven 架构、Agent Loop、工具权限、MCP、会话恢复、上下文压缩、Worktree 和复刻练习。

### 整合学习教材

仓库内多套课程、深度学习、deep-dive 和 sourcebook 文档已按主题合并为一份主教材：[JIMICODE-整合学习教材.md](docs/JIMICODE-整合学习教材.md)。原始文档全部保留，主教材通过来源标记和主题分卷减少重复查找。

最终推荐入口已经统一为：[docs/learning-final](docs/learning-final/README.md)。该目录包含唯一主教材、融合统计、来源索引、原始资料归档和历史整合版；此前的 `docs/learning-integrated` 与 `docs/final-learning` 仅作为历史生成目录保留，不再作为学习入口。
