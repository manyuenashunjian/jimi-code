# jimi-code 深度学习文档导航

## 这套文档解决什么问题

文档分成三种深度，不能混为一谈：

1. `00`～`04` 是项目级学习材料，解释架构、依赖、运行、异常、安全、性能和面试原理。
2. `02-逐文件源码精读.md` 是全仓库覆盖索引，保证文件不漏，但其中配置/文档文件只做定位说明。
3. `05` 与 `06` 是源码语义层：`05` 给出 Java 类型、方法和源码片段证据，`06` 把入口、Agent Loop、工具、压缩、并发和恢复串成真实调用链。

因此，看到一个文件时先在 02 定位，再在 05 找方法证据，最后用 06 理解它在系统中的位置。

## 推荐阅读顺序

`00` → `01` → `06` → `05` → `02` → `03` → `04`。

读完每个核心类后，至少完成一次：查找调用者、查找测试、运行相关测试、记录异常路径和副作用。只有完成这五步，才算“学会”而不是“看过”。

## 重要源码入口

- `src/main/java/com/jimicode/JimiCode.java`
- `src/main/java/com/jimicode/config/ConfigLoader.java`
- `src/main/java/com/jimicode/agent/Agent.java`
- `src/main/java/com/jimicode/conversation/ConversationManager.java`
- `src/main/java/com/jimicode/llm/LlmClient.java`
- `src/main/java/com/jimicode/tool/ToolRegistry.java`
- `src/main/java/com/jimicode/permission/PermissionChecker.java`
- `src/main/java/com/jimicode/compact/ContextCompactor.java`
- `src/main/java/com/jimicode/worktree/WorktreeManager.java`
- `src/main/java/com/jimicode/teams/TeamManager.java`

## 覆盖边界

当前主源码为 103 个 Java 文件。TUI 框架、Maven 插件和外部模型 SDK 的内部实现不属于本仓库源码；文档解释它们在本项目中的调用契约，不声称解释第三方库的每一行实现。
