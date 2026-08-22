# jimi-code 源码学习教材

## 项目定位

`jimi-code` 是从 `mewcode-java` 改名后的 Java 项目。它现在统一使用 Java 21/Maven，代码位于 `src/main/java/com/mewcode`。原 Maven 子项目已独立复制为工作区根目录下的 [java-maven](../../../java-maven/README.md)。

## 阅读顺序

1. [项目总览与学习地图](01-项目总览与学习地图.md)
2. [环境、配置与构建](02-环境配置与构建.md)
3. [Java/Maven 架构](03-Java-Maven架构.md)
4. [Agent Loop、LLM 与上下文压缩](04-Agent-Loop-LLM与上下文压缩.md)
5. [工具、权限、MCP 与 SubAgent](05-工具权限MCP与SubAgent.md)
6. [会话、Memory、Worktree 与恢复](06-会话MemoryWorktree与恢复.md)
7. [源码文件与依赖索引](07-源码文件与依赖索引.md)
8. [调试实验、复刻项目与面试](08-调试实验复刻项目与面试.md)

## 文件覆盖

- [生产文件覆盖清单](00-文件覆盖清单.md)
- [依赖覆盖清单](00-依赖覆盖清单.md)

清单记录了当前源码规模和阅读入口；它不应被误解为所有文件都已经逐行讲解。核心类会在正文中展开，普通支撑文件按同一模板补读。

## 事实边界

文档以当前本地源码为准。`jimi-code` 是 Java/Maven 终端 coding agent，不是 Spring Boot 服务；它通过终端 UI、LLM SDK、工具注册表、权限规则、会话文件、MCP 和 Git Worktree 组合成 coding agent。`java-maven` 是独立构建变体，依赖和源码应单独核对。
