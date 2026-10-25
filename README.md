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

## 简历项目复刻（已撤销）

2026-10-25 删除：此前置于本目录内的 `live-commerce-platform`（Signal Room 直播电商智能分析演示，61 个文件 / 366K）**从未进过本仓 Git** —— `git ls-files` 为 0、`git log -- live-commerce-platform` 无任何提交、目录内也没有嵌套 `.git`，所以删除后磁盘与远端都没有第二份，它也不再作为简历项目引用。

直播电商方向改用 [`../Jimi-E-Com-Live-Warehouse`](../Jimi-E-Com-Live-Warehouse/README.md) 单一口径：RAG 问答与无依据拒答、NL2SQL 受控查数、多智能体编排、MCP 服务、媒体派生链均已实现并有实机读数。原先挂在 Signal Room 上的「Spring AI Alibaba Graph」表述失去可展示代码，替代说法用选型判断本身：评估过官方 `spring-ai-alibaba-graph-core`，因传递依赖与阻塞式 MVC 不符而拒绝引入，改自研同构 MiniGraph（依据见 Jimi 仓 `doc/02-决策记录/ADR-012-编排引擎Phase0选型.md`）。
