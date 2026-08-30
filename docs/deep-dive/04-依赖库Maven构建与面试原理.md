# JimiCode 依赖库、Maven 构建、测试与面试原理深度解析

## 1. pom.xml 是项目运行时架构的声明

JimiCode 的 pom.xml 采用单模块 Maven，Java release 21，产物最终名称为 jimicode，并通过 Shade 插件把依赖打进一个可执行 jar。依赖列表并不是“装饰”，每个库都对应源码中的一个边界：

| 依赖 | 版本 | 在源码中的作用 | 学习重点 |
|---|---|---|---|
| tui4j | 0.3.3 | Bubble Tea 风格终端程序、Model、Message、Style | 事件循环与 UI 线程 |
| Mordant | 3.0.2 | 终端颜色、布局与 Markdown 渲染 | ANSI 输出与终端兼容 |
| mordant-markdown | 3.0.2 | Markdown 渲染 | 代码块、列表、流式文本 |
| MCP SDK | 1.1.3 | stdio/HTTP MCP client、schema、工具调用 | 外部进程与协议生命周期 |
| slf4j-nop | 2.0.16 | runtime 日志绑定 | 为什么没有默认日志输出 |
| anthropic-java | 2.34.0 | Anthropic Messages SDK 与 streaming | thinking/tool/usage 映射 |
| openai-java | 4.37.0 | OpenAI Responses SDK 与 streaming | response item、reasoning、工具 |
| SnakeYAML | 2.2 | YAML 配置和 Skill/Agent 元数据 | 反序列化安全 |
| jackson-databind | 2.21.3 | JSON、JSONL、schema、持久化 | 版本、树模型与容错 |
| jackson-datatype-jsr310 | 2.21.3 | Instant/Java time 序列化 | 时间字段兼容 |
| JUnit Jupiter | 5.11.4 | 单元测试 | 临时目录、参数、断言 |
| Maven Enforcer | 3.5.0 | Java/Maven 版本门禁 | reproducible build |
| Compiler plugin | 3.14.0 | release 21、参数名、编码 | 编译契约 |
| Surefire | 3.5.3 | 测试执行 | module path、UTF-8 |
| Shade | 3.6.0 | fat jar、Main-Class、SPI 资源 | 服务发现与签名过滤 |

## 2. Java 21：为什么项目选择新版本

源码使用虚拟线程、record、模式匹配 switch、List.getLast 等 Java 21 能力。maven.compiler.release=21 让编译器同时约束 API 和字节码目标，不能只在本机装 JDK 21 却用更高版本 API。Enforcer 的 requireJavaVersion [21,) 与 requireMavenVersion [3.9,) 在构建早期失败，避免生成“在开发机能跑、在部署机才失败”的 jar。

虚拟线程适合 Agent 的阻塞式网络、ProcessBuilder、BlockingQueue poll，但不自动解决资源上限；每个工具调用仍需超时，外部进程仍需销毁，队列仍需背压。Java 21 的 pattern switch 让 StreamEvent 解析清晰，但如果项目要回移 Java 17，需要重写这些语法并替换 List.getLast/getFirst 等 API。

## 3. tui4j 与 Mordant

tui4j 提供 bubbletea 风格的 Model-Update-View。JimiCodeModel 不是被动渲染器，而是整个交互式应用状态机。Program.run 持有事件循环，用户键盘消息和异步 AgentEvent 都会进入 update。模型状态变更应集中在 UI 线程，后台 Agent 只能通过消息/队列投递。

tui4j 的 Command 通常代表异步动作。JimiCode 的启动、Agent 运行、后台 mailbox 轮询、文件读取和用户确认都可能产生 Command。面试时可比较 Swing EDT、JavaFX Application Thread、Redux reducer 和 Bubble Tea：共同点是单向事件流，区别是 tui4j 更接近函数式 update。

Mordant 与 mordant-markdown 负责 ANSI style、颜色和 Markdown。MarkdownRenderer 可能还有轻量正则处理，用于流式文本中未闭合代码块、工具输出和思考内容。终端渲染不能假设 stdout 是真实终端；重定向到文件、Windows Terminal、SSH 和窄窗口都会影响换行、宽字符和颜色。Styles 集中定义样式，避免业务代码到处写 ANSI escape。

## 4. Anthropic Java SDK

AnthropicClient 使用 AnthropicOkHttpClient、Messages API 和 StreamResponse。它需要将内部 Message 结构映射到 Anthropic 的 content blocks，包括 text、thinking、tool_use、tool_result；把 SDK 的增量事件统一成 StreamEvent.TextDelta、ThinkingDelta、ToolCallStart/Delta/Complete、StreamEnd 和 Error。

难点一是工具参数是增量 JSON。ToolCallDelta 到达时不能立即当成完整 Map，必须累计 JSON 字符串，ToolCallComplete 时解析成 Map。难点二是 thinking signature：后续请求可能需要完整 signature，截断、压缩和恢复时不能只保存可见文本。难点三是 usage 字段版本差异，AnthropicClientContextWindowTest 和 UsageParsingTest 说明用量解析需要对缺失字段、cache read、cache creation 和不同 response 形态容错。

Anthropic 的 prompt cache 让稳定前缀有实际成本收益，因此 ToolResultBudget 记录替换决策并保持 prefix byte-stable。新增 system reminder 时应考虑插入位置，不能每轮在缓存前缀开头加入随机内容。

## 5. OpenAI Java SDK

OpenAiClient 使用 OpenAIOkHttpClient 与 Responses API。Responses API 的输出不是只有一个 message 字符串，可能包含 reasoning item、output text、function call、function call output。内部 StreamEvent 抽象将这些差异压平，让 Agent 只处理统一事件。

Reasoning 与 Anthropic thinking 不完全等价：字段、签名、可见性和缓存语义不同。Adapter 应保证 conversation 中的内部块能被同一 provider 正确重建，而不是把 provider A 的块原样发送给 provider B。ModelResolver 根据 protocol/provider/model 选择客户端；配置切换模型时，协议和工具 schema 也可能变化。

OpenAI 错误分类需要映射认证、限流、上下文超长、服务端错误和网络断开。不能只根据 HTTP status 判断，因为兼容服务可能返回 200 + error JSON，或者 429 的 body 结构不同。OpenAiCompatClient 因而使用 Java HttpClient、BufferedReader、ObjectMapper 和 JsonNode 自己解析 SSE/JSON，灵活但要自行处理连接关闭、UTF-8、data:[DONE]、空行和 JSON 片段。

## 6. OpenAiCompatClient：为什么要自己写兼容适配

兼容端点的优点是可以接入 DeepSeek、Qwen、Kimi、OpenRouter 或本地服务；缺点是“兼容”通常只覆盖请求路径，不保证 tool call、reasoning、usage、stream event 完全一致。OpenAiCompatClient 用 ObjectMapper tree model 处理未知字段，适合多供应商，但应有 capability 配置：

- 是否支持 streaming；
- 是否支持 tools；
- 是否支持 reasoning；
- 是否支持 system role；
- tool arguments 是 JSON string 还是 object；
- usage 是否在 stream 末尾；
- stop reason 的取值；
- context window 和 max output。

SSE 解析器必须按事件边界而不是按 TCP read 边界解析；一条 JSON 可能被分成多个 read，也可能一个 read 包含多个事件。正确做法是 BufferedReader 按行读取，以空行结束 event，累积 data 行，再交给 Jackson。网络流异常时要把线程中断、连接关闭和解析错误分别映射。

## 7. Jackson：项目的持久化骨架

Jackson Databind 在 AppConfig、HistoryStore、SessionManager、TaskList、FileMailBox、SharedTaskStore、WorktreeSessionStore、MemoryManager 和 ToolSearchTool 中出现。Tree model JsonNode 适合解析 provider 未知字段、JSONL 坏行和动态工具参数；POJO 反序列化适合稳定的 config/record。

JSONL 的常见模式是逐行 readTree，坏行 catch Exception 后跳过。它提高了可恢复性，但如果所有异常都 ignored，用户可能不知道数据损坏。生产建议记录文件路径、行号和一次性警告。写文件时应考虑 UTF-8、临时文件、flush、原子 move 和并发 writer。

Jackson 版本要统一。pom 使用属性 jackson.version 同时管理 databind 与 jsr310，但 transitive jackson-core、annotations 也可能来自其他依赖；用 dependency:tree 检查是否存在版本冲突。Shade fat jar 还要保留 META-INF/services，否则 MCP、ModelProvider 或其他 SPI 可能在打包后找不到实现。

## 8. SnakeYAML：配置与技能的安全边界

SnakeYAML 2.2 负责 app YAML、hooks、skills、subagents。LoaderOptions 和 Constructor 的配置决定是否允许任意类型、未知属性和递归深度。安全原则是只反序列化到明确的 Java 类型，不启用任意类实例化；对文件大小、别名数量和嵌套深度做限制，防止 YAML bomb。

SkillCatalog、AgentLoader 和 ConfigLoader 都把 YAML 内容变成运行时行为。Skill 的 description、allowed tools、prompt 和 script 路径本质上都可能影响 Agent；来自项目目录的 SKILL.md 不是天然可信，应该检查路径、命令、网络和权限。面试可以问“YAML 配置为什么也需要安全审查”：因为配置不只是静态参数，它会决定执行什么进程、访问哪些文件和调用哪个模型。

## 9. MCP SDK

MCP SDK 提供 McpClient/McpSyncClient、StdioClientTransport、HttpClientStreamableHttpTransport、ServerParameters、McpSchema。JimiCode 通过 McpManager 将外部服务暴露为内部 Tool。MCP 连接使用同步 client，但外层 Agent 在虚拟线程中运行，需注意同步调用仍可能无限阻塞；应设置 request timeout 和 shutdown hook。

MCP schema 与内部 schema 的转换要保留 required、properties、description、enum 和 nested object。工具返回可能是 text、image、resource 或 structured content，ToolResult 不能只取一个字符串。若外部服务断开，已有工具是否从 Registry 移除、下一轮是否重新连接、错误如何向模型解释，都属于生命周期设计。

## 10. SLF4J NOP 的含义

项目 runtime 使用 slf4j-nop，意味着 SLF4J API 调用不会输出日志。对一个交互式 CLI 来说，避免库日志污染终端有价值，但也会让网络、MCP、Hook 失败难以诊断。正式部署可替换为 logback-classic 或 slf4j-simple，并把日志写到文件而不是 stdout。

日志中不能打印 API key、完整工具参数、任意文件内容和用户 prompt。建议使用 session id、tool call id、provider、latency、status、token usage、error category 等结构化字段。JimiCode 当前部分类使用 java.util.logging、部分依赖 SDK 日志，统一日志门面可以改善排查。

## 11. JUnit Jupiter 与测试策略

测试目录的 14 个测试文件覆盖压缩、恢复、配置、provider key/context window、Anthropic usage、权限模式、session、mailbox、tool search、tool result budget。它们体现了本项目的测试优先级：不是追求每个 getter 的覆盖率，而是验证状态转换和边界。

`@TempDir` 用于隔离文件系统；测试构造显式 Path，避免写入真实 user.home。StubSummaryClient 让压缩测试不依赖真实模型。UsageParsingTest 使用 JsonNode 验证外部 response 的多形态解析。FileMailBoxTest 和 ToolResultBudgetTest 关注并发/幂等/持久化边界。

建议补充的测试包括：SIGINT 清理光标、Bash 超时销毁、MCP 连接失败、Windows path、symlink escape、Hook regex 非法、队列满、Agent 线程中断、重复 tool call id、session boundary 多次压缩、Jackson 版本冲突和 fat jar 的 ServiceLoader。

## 12. Maven 插件与打包

Enforcer 在 validate 阶段检查 JDK/Maven；Compiler 设置 release 21、UTF-8 和 parameters=true。parameters=true 让反射或工具生成 schema 时可以取得 Java 参数名，尤其对工具方法很重要；关闭后参数可能变成 arg0，模型工具 schema 会变差。

Surefire 设置 useModulePath=false、useManifestOnlyJar=false 和 UTF-8，适合当前非 JPMS 项目。Shade 在 package 阶段生成带依赖的 jimicode jar，ManifestResourceTransformer 写入 Main-Class，ServicesResourceTransformer 保留 SPI。过滤 SF/DSA/RSA 签名文件是必要的，因为依赖重打包后原签名通常失效。

检查 fat jar 的方法：

```
mvn -q clean test
mvn -q package
java -jar target/jimicode.jar
jar tf target/jimicode.jar | Select-String "META-INF/services|com/jimicode/JimiCode"
```

构建可复现还要固定插件版本、依赖版本、编码、时区和环境变量。Shade 的 createDependencyReducedPom=false 避免修改项目 pom，但会让依赖树和发布行为与普通 jar 不同；学习时要区分“源码依赖”和“最终运行时依赖”。

## 13. 面试问题清单与标准回答方向

### Maven dependencyManagement 与 dependencies 的区别

dependencies 表示当前模块实际依赖；dependencyManagement 只统一版本和默认配置，不自动引入依赖。JimiCode 是单模块，直接在 dependencies 写版本较直观；多模块 AgentScope 更需要 BOM/parent 管理。

### fat jar 为什么需要 ServicesResourceTransformer

Java SPI 通过 META-INF/services 文件发现实现。多个依赖各自带这个文件，普通 shade 可能只保留一个；ServicesResourceTransformer 会合并它们。没有它，MCP transport、JDBC driver 或自定义 provider 可能在 IDE 能跑、fat jar 不能跑。

### release 与 source/target 有什么区别

release 同时约束可用 API、语言级别和字节码，避免在高 JDK 编译出低版本无法运行的 API 引用。source/target 只约束语法和字节码，不保证 API 兼容。

### 为什么要把 SDK 事件统一成 StreamEvent

Agent Loop 不应知道 Anthropic 和 OpenAI 的 SDK 类型。统一事件降低业务层分支，便于新增 provider、测试和错误恢复；代价是抽象必须覆盖 thinking/tool/usage/cache/stop reason 等差异，不能只定义 text。

### 外部依赖版本如何治理

固定版本、检查 dependency:tree、使用 BOM、避免多个 JSON/HTTP 栈随意混用、锁定 transitive 关键版本、对安全公告建立升级窗口。provider SDK 升级要用 adapter 测试验证事件和 usage，不应只看编译通过。

## 14. 构建排查顺序

1. 检查 java -version 是否 21+，mvn -version 是否 3.9+。
2. 删除 target 后 clean test，排除旧 class。
3. 用 dependency:tree 检查 Jackson、OkHttp、SLF4J、MCP 冲突。
4. 若 TUI 报类找不到，检查 tui4j/Mordant 版本和 shade。
5. 若 fat jar 找不到 provider，检查 ServiceLoader 资源是否被合并。
6. 若工具参数名为 arg0，检查 compiler parameters=true。
7. 若 YAML 能读但运行行为错误，检查语义默认值和 environment key。
8. 若模型流卡住，检查 HttpClient/SSE timeout、线程 dump、进程是否仍活跃。
9. 若测试在 Windows 失败，检查 shell、路径分隔符、PosixFilePermission 和 SIGINT 差异。

## 15. 生产依赖逐项复盘

`tui4j` 的核心价值是把终端输入、窗口尺寸、异步命令和退出事件组织成 Model/Update/View；它不是普通的 console print 工具。`mordant` 和 `mordant-markdown` 负责视觉层，Styles/MarkdownRenderer 通过它们把 AgentEvent 变成可读的终端内容。三者版本必须一起验证，因为 ANSI style、Markdown parser 和 Bubble Tea compatibility 层可能互相依赖。

`anthropic-java` 和 `openai-java` 是两个强类型 SDK，但强类型不代表内部协议相同。AnthropicClient 要处理 Messages content block、thinking signature、cache token；OpenAiClient 要处理 Responses item、reasoning 和 function call。它们都通过内部 `StreamEvent` 交给 Agent，新增 provider 时应遵循同样的隔离，而不是把 SDK 类型渗透到 ConversationManager。

`mcp` 把远程/本地工具的发现和调用协议带进来，运行时可能启动 stdio 子进程或建立 HTTP 连接，所以它同时是扩展点和资源边界。`slf4j-nop` 刻意不向终端输出库日志，适合 TUI，但生产调试必须配置文件日志或替换 binding。`snakeyaml` 解析配置、Skill 和 SubAgent 元数据；任何能改变命令、路径或 prompt 的 YAML 都应按可执行配置审查。

Jackson 在项目中承担多种语义：POJO 配置、JsonNode provider response、JSONL session、task/mailbox、worktree session、memory 和 tool schema。不要把所有 JSON 都用同一种容错策略：配置错误应尽早失败，历史坏行可以跳过，tool response 缺字段应分类，session boundary 损坏应回退。`jackson-datatype-jsr310` 让 Instant/时间类型有稳定表示，但仍要明确时区和 epoch 语义。

## 16. 依赖与源码文件对应表

| 源码区域 | 直接库 | 需要重点验证的边界 |
|---|---|---|
| `JimiCode.java`、`JimiCodeModel.java`、`Styles.java`、dialog | tui4j、Mordant | UI 单线程、ANSI、退出清理、窗口尺寸 |
| `AnthropicClient.java` | anthropic-java、Jackson | 流事件、thinking、usage、上下文窗口 |
| `OpenAiClient.java` | openai-java、Jackson | Responses item、reasoning、工具调用 |
| `OpenAiCompatClient.java` | JDK HttpClient、Jackson | SSE 边界、兼容协议、超时、错误 body |
| `ConfigLoader.java`、`SkillCatalog.java`、`AgentLoader.java` | SnakeYAML | 类型白名单、未知字段、路径和脚本 |
| `SessionManager.java`、`HistoryStore.java`、`TaskList.java` | Jackson | JSONL 追加、坏行、版本、原子写 |
| `McpManager.java` | MCP SDK、SLF4J | transport 生命周期、schema、连接关闭 |
| `FileHistory.java`、Worktree 包 | JDK NIO、ProcessBuilder | Git 状态、备份、路径安全、平台差异 |
| `src/test/java` | JUnit Jupiter | 临时目录、fake model、协议和状态机 |

## 17. 依赖升级的真实流程

升级 provider SDK 不能只修改版本号。先读取 release notes，确认 streaming event、usage、tool schema 和异常类型是否改变；再运行 adapter 单元测试和 JSON fixture 测试；最后用真实 provider 做最小 smoke test。升级 Jackson 要检查 SnakeYAML/SDK 的 transitive 版本、JsonNode 行为、Java time module 和 Shade 后的重复类。升级 tui4j 要验证 KeyPressMessage、UpdateResult、Program.run、Style API 和 Windows terminal。

升级 MCP SDK 要检查 stdio transport 是否仍由用户配置的 command/args 启动、同步 client 的 close 是否阻塞、工具结果是否新增 image/resource block，以及 server capability negotiation 是否变化。升级 Maven Shade 要检查 Main-Class、ServiceLoader、签名过滤和最终 jar 文件大小。任何依赖升级都应生成 `dependency:tree` 前后差异，避免“编译通过但运行时 classpath 变了”。

## 18. 运行时 classpath 与 fat jar

IDE 运行时 classpath 是多个 target/classes 和 ~/.m2 jar 的集合，ServiceLoader 能看到各个 jar 的 META-INF/services。Shade 后所有内容合并到一个 jar，服务文件、资源路径、签名、重复 package 和反射配置都可能改变。因而测试不能只在 IDE 里启动；至少要运行 `mvn clean package` 后用 `java -jar` 启动，并测试 provider、MCP、YAML、Markdown 和 TUI 入口。

如果 fat jar 启动报 NoClassDefFoundError，先看依赖是否是 runtime scope、shade 是否包含；若报服务实现找不到，检查 ServicesResourceTransformer；若 Jackson 报模块或重复类，检查 dependency tree 和 shade jar；若终端显示异常，检查编码、ANSI 和终端宽度；若模型流卡住，检查不是编译问题，而是 HTTP timeout、SSE 解析或未关闭的 process。

## 19. 测试文件逐个学习角度

`ContextCompactorTest` 验证摘要调用和消息折叠，`RecoveryAttachmentTest` 验证压缩后恢复文件/技能证据；`ConfigLoaderValidationTest` 验证错误配置，`ProviderConfigApiKeyTest` 验证密钥优先级，`ProviderConfigContextWindowTest` 验证上下文/输出默认值；`AnthropicClientContextWindowTest` 验证模型上下文策略，`UsageParsingTest` 验证 provider usage 归一化。

`PermissionModeTest` 锁定模式字符串到 Decision 的映射；`SessionManagerTest` 锁定 JSONL、boundary 和重建；`FileMailBoxTest` 锁定文件通信的追加/读取；`ToolSearchTest` 锁定 deferred schema 的选择语法；`ContentReplacementStateTest` 与 `ReplacementRecordsIOTest` 锁定替换状态和落盘；`ToolResultBudgetTest` 锁定预算算法。读测试时要问：它在防哪种回归？如果删掉断言，哪条链路会悄悄变坏？

## 20. 进阶面试题

问：为什么单模块也要 Enforcer？答：单模块仍有开发者 JDK、Maven 和 CI 环境差异；门禁在编译前失败，反馈更早且结果更一致。

问：为什么 compiler parameters=true 会影响 Agent？答：工具 schema 需要 Java 方法参数名；没有它，反射可能只能看到 arg0/arg1，模型难以正确构造参数。

问：为什么不把所有 JSONL 换成数据库？答：本地 CLI 优先考虑零部署、追加、可读和损坏隔离；当并发、多节点查询和事务需求出现，再抽象 Store，而不是先引入重基础设施。

问：为什么 NOP logger 有利有弊？答：它不污染 TUI 输出，但会丢失 SDK/transport/MCP 的诊断；生产应将结构化日志写到文件，并对 prompt/key/tool 输出脱敏。

问：为什么兼容 OpenAI 的服务仍需独立 adapter？答：路径和基本 JSON 兼容不等于 streaming event、tools、reasoning、usage、context window 和错误码兼容；adapter 是能力协商和差异隔离的位置。

## 21. 本册验收标准

读者应该能从 pom.xml 反推出运行时架构，说明每个直接依赖对应哪些源码文件和资源边界；能解释 `mvn clean test`、`mvn package`、`java -jar` 分别验证什么；能定位 SPI 丢失、参数名变 arg0、SSE 卡住、YAML 语义错误、Jackson 冲突和终端日志污染等常见问题；还能说出升级任一 provider SDK 时需要补哪些 adapter 和 fixture 测试。

## 22. 总结

JimiCode 的依赖选择体现了一个本地优先 Agent 的工程取舍：tui4j/Mordant 解决交互，provider SDK 和 HttpClient 解决模型，MCP 扩展工具，SnakeYAML/Jackson 解决可配置和可恢复状态，JUnit 锁定关键状态机，Maven Enforcer/Compiler/Surefire/Shade 把运行环境和交付物固定下来。真正的面试亮点不是背依赖名称，而是能说清每个库在调用链中的位置、它引入了什么线程/资源/协议边界，以及失败时如何被上层接管。
