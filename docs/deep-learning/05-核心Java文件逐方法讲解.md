# jimi-code：05-核心Java文件逐方法讲解.md


本文不是只对 README 的转述，而是按照源码证据生成的学习材料。阅读时建议先看模块图，再打开文中列出的真实文件，最后运行对应测试验证结论。文中“证据”指文件路径、包名、类型声明、公开方法、导入库和代码行数；当某个模块没有 Controller 时，会明确说明它是 SDK/CLI 或基础设施模块，而不会臆造 HTTP 接口。


## 这份文档与覆盖索引的区别

上一份 02 文档回答“仓库里有哪些文件”；本文件回答“每个 Java 文件中有哪些可执行方法、方法体开头做了什么、应该沿哪条调用链验证”。方法体片段来自当前源码，最多展示每个方法前 10 行，避免把摘要冒充完整源码；完整实现仍以链接文件为准。

## JimiCode 的真实主链路

`JimiCode.main`/启动组装 → `ConfigLoader` → `JimiCodeModel` → `CommandRegistry`/用户输入 → `Agent` → `PromptBuilder` + `ConversationManager` → `LlmClient` 流 → `StreamingExecutor` → `ToolRegistry` → `PermissionChecker` → 具体 Tool → `ToolResult` → 会话追加/再次模型调用。

## 逐 Java 文件、逐方法证据

### `jimi-code/src/main/java/com/jimicode/agent/Agent.java`

**类型与边界。** 包 `com.jimicode.agent`；类型 `Agent, into, ToolCallInfo, ToolCallResult`；433 行；直接依赖 `com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, com.jimicode.conversation.ThinkingBlock, com.jimicode.conversation.ToolResultBlock, com.jimicode.conversation.ToolUseBlock, com.jimicode.hook.HookEngine, com.jimicode.llm.LlmClient, com.jimicode.llm.StreamEvent, com.jimicode.permission.PermissionChecker, com.jimicode.permission.PermissionMode`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public ContentReplacementState getReplacementState() { return replacementState; }`（源码行 76-76）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ContentReplacementState getReplacementState() { return replacementState; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setReplacementState(ContentReplacementState state) { this.replacementState = state; }`（源码行 77-77）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setReplacementState(ContentReplacementState state) { this.replacementState = state; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setFileHistory(com.jimicode.filehistory.FileHistory fh) { this.fileHistory = fh; }`（源码行 91-91）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setFileHistory(com.jimicode.filehistory.FileHistory fh) { this.fileHistory = fh; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolRegistry getRegistry() { return registry; }`（源码行 94-94）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolRegistry getRegistry() { return registry; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getProtocol() { return protocol; }`（源码行 95-95）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getProtocol() { return protocol; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Agent(LlmClient client, ToolRegistry registry, String protocol, ProviderConfig cfg) {`（源码行 97-103）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Agent(LlmClient client, ToolRegistry registry, String protocol, ProviderConfig cfg) {
        this.client = client;
        this.registry = registry;
        this.protocol = protocol;
        this.contextWindow = cfg.resolvedContextWindow();
        this.maxOutput = cfg.resolvedMaxOutputTokens();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setChecker(PermissionChecker checker) { this.checker = checker; }`（源码行 105-105）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setChecker(PermissionChecker checker) { this.checker = checker; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setHookEngine(HookEngine hookEngine) { this.hookEngine = hookEngine; }`（源码行 106-106）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setHookEngine(HookEngine hookEngine) { this.hookEngine = hookEngine; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMaxIterations(int max) { this.maxIterations = max; }`（源码行 107-107）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMaxIterations(int max) { this.maxIterations = max; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setWorkDir(String workDir) { this.workDir = workDir; }`（源码行 108-108）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setWorkDir(String workDir) { this.workDir = workDir; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setSessionId(String sessionId) { this.sessionId = sessionId; }`（源码行 109-109）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getSessionId() { return sessionId; }`（源码行 110-110）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getSessionId() { return sessionId; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setNotificationFn(java.util.function.Supplier<List<String>> fn) { this.notificationFn = fn; }`（源码行 111-111）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setNotificationFn(java.util.function.Supplier<List<String>> fn) { this.notificationFn = fn; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setToolNameFilter(java.util.function.Predicate<String> filter) { this.toolNameFilter = filter; }`（源码行 113-113）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setToolNameFilter(java.util.function.Predicate<String> filter) { this.toolNameFilter = filter; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setInstructions(String instructions) { this.instructions = instructions; }`（源码行 114-114）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setInstructions(String instructions) { this.instructions = instructions; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMemoryContent(String memoryContent) { this.memoryContent = memoryContent; }`（源码行 115-115）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMemoryContent(String memoryContent) { this.memoryContent = memoryContent; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public HookEngine getHookEngine() { return hookEngine; }`（源码行 116-116）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public HookEngine getHookEngine() { return hookEngine; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public BlockingQueue<AgentEvent> run(ConversationManager conv) {`（源码行 118-128）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public BlockingQueue<AgentEvent> run(ConversationManager conv) {
        var queue = new LinkedBlockingQueue<AgentEvent>(64);
        Thread.startVirtualThread(() -> {
            try {
                agentLoop(conv, queue);
            } catch (Exception e) {
                putSafe(queue, new AgentEvent.ErrorEvent("Agent error: " + e.getMessage()));
            }
        });
        return queue;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void agentLoop(ConversationManager conv, BlockingQueue<AgentEvent> queue) {`（源码行 130-250）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void agentLoop(ConversationManager conv, BlockingQueue<AgentEvent> queue) {
        conv.injectLongTermMemory(instructions, memoryContent);

        int totalInput = 0, totalOutput = 0;
        int outputRecoveries = 0;
        boolean maxTokensEscalated = false;

        int contextRetries = 0;
        boolean loopCompleted = false;

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (event == null) {`（源码行 251-254）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                if (event == null) {
                    putSafe(queue, new AgentEvent.ErrorEvent("Stream timeout"));
                    return;
                }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `switch (event) {`（源码行 256-287）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                switch (event) {
                    case StreamEvent.TextDelta td -> {
                        text.append(td.text());
                        putSafe(queue, new AgentEvent.StreamText(td.text()));
                    }
                    case StreamEvent.ThinkingDelta td ->
                            putSafe(queue, new AgentEvent.ThinkingText(td.text()));
                    case StreamEvent.ThinkingComplete tc -> {
                        thinkingBlocks.add(new ThinkingBlock(tc.thinking(), tc.signature()));
                        putSafe(queue, new AgentEvent.ThinkingComplete(tc.thinking(), tc.signature()));
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (event instanceof StreamEvent.StreamEnd || event instanceof StreamEvent.Error) break;`（源码行 289-293）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                if (event instanceof StreamEvent.StreamEnd || event instanceof StreamEvent.Error) break;
            }

            // Error recovery
            if (streamError) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (lastErr != null && (lastErr.contains("context") || lastErr.contains("too long")`（源码行 295-315）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                if (lastErr != null && (lastErr.contains("context") || lastErr.contains("too long")
                        || lastErr.contains("prompt"))) {
                    if (contextRetries < 3) {
                        contextRetries++;
                        putSafe(queue, new AgentEvent.RetryEvent("Context too long, compacting...", 0));
                        int sizeBeforeForce = conv.size();
                        try {
                            String wdForce = workDir != null ? workDir : System.getProperty("user.dir");
                            com.jimicode.compact.ContextCompactor.forceCompact(
                                    conv, client, contextWindow, wdForce, sessionId,
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (lastErr != null && lastErr.toLowerCase().contains("rate limit")) {`（源码行 316-320）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                if (lastErr != null && lastErr.toLowerCase().contains("rate limit")) {
                    putSafe(queue, new AgentEvent.RetryEvent("Rate limited, waiting 5s...", 5000));
                    try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
                    continue;
                }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `putSafe(queue, new AgentEvent.UsageEvent(totalInput, totalOutput));`（源码行 326-350）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            putSafe(queue, new AgentEvent.UsageEvent(totalInput, totalOutput));

            // Max tokens handling
            if ("max_tokens".equals(stopReason)) {
                if (!maxTokensEscalated) {
                    maxTokensEscalated = true;
                    client.setMaxOutputTokens(MAX_TOKENS_CEILING);
                    if (!text.isEmpty()) {
                        conv.addAssistantFull(text.toString(), thinkingBlocks, List.of());
                        conv.addUserMessage("Output token limit hit. Resume directly from where you stopped. Do not apologize or repeat previous content. Pick up mid-thought if needed.");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (turnInput > 0 || turnOutput > 0 || turnCacheRead > 0 || turnCacheCreation > 0) {`（源码行 365-369）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (turnInput > 0 || turnOutput > 0 || turnCacheRead > 0 || turnCacheCreation > 0) {
                int baseline = turnInput + turnCacheRead + turnCacheCreation + turnOutput;
                usageAnchor = new com.jimicode.compact.ContextCompactor.UsageAnchor(
                        baseline, conv.size());
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (toolCalls.isEmpty()) {`（源码行 372-384）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (toolCalls.isEmpty()) {
                if (fileHistory != null) {
                    String summary = text.length() > 60 ? text.substring(0, 60) + "..." : text.toString();
                    fileHistory.makeSnapshot(conv.size(), summary);
                }
                // No TurnComplete on the terminal (no-tool) turn. The TUI's TurnComplete
                // handler flushes+clears streamBuf without persisting; if we emitted
                // it first, LoopComplete would see an empty buffer and the final
                // assistant message would never be saved to the session file.
                putSafe(queue, new AgentEvent.LoopComplete(iteration));
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (exitPlanCalled) {`（源码行 401-406）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (exitPlanCalled) {
                putSafe(queue, new AgentEvent.TurnComplete(iteration));
                putSafe(queue, new AgentEvent.LoopComplete(iteration));
                loopCompleted = true;
                break;
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `putSafe(queue, new AgentEvent.TurnComplete(iteration));`（源码行 408-410）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            putSafe(queue, new AgentEvent.TurnComplete(iteration));
        }
        } finally {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!loopCompleted) {`（源码行 411-413）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (!loopCompleted) {
                putSafe(queue, new AgentEvent.LoopComplete(0));
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String events_drain_last_error(BlockingQueue<AgentEvent> queue) {`（源码行 419-421）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String events_drain_last_error(BlockingQueue<AgentEvent> queue) {
        return lastStreamError;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void putSafe(BlockingQueue<AgentEvent> queue, AgentEvent event) {`（源码行 423-429）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void putSafe(BlockingQueue<AgentEvent> queue, AgentEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private record ToolCallInfo(String toolId, String toolName, Map<String, Object> args) {}`（源码行 431-431）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private record ToolCallInfo(String toolId, String toolName, Map<String, Object> args) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private record ToolCallResult(String toolId, String output, boolean isError) {}`（源码行 432-432）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private record ToolCallResult(String toolId, String output, boolean isError) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/agent/AgentEvent.java`

**类型与边界。** 包 `com.jimicode.agent`；类型 `AgentEvent, StreamText, ThinkingText, ThinkingComplete, ToolUseEvent, ToolResultEvent, TurnComplete, LoopComplete, UsageEvent, ErrorEvent, CompactEvent, RetryEvent, PermissionRequestEvent, AskUserRequestEvent`；44 行；直接依赖 `com.jimicode.permission.PermissionResponse, java.util.Map, java.util.concurrent.CompletableFuture`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `record StreamText(String text) implements AgentEvent {}`（源码行 15-15）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record StreamText(String text) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ThinkingText(String text) implements AgentEvent {}`（源码行 17-17）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ThinkingText(String text) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ThinkingComplete(String thinking, String signature) implements AgentEvent {}`（源码行 19-19）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ThinkingComplete(String thinking, String signature) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ToolUseEvent(String toolId, String toolName, Map<String, Object> args) implements AgentEvent {}`（源码行 21-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ToolUseEvent(String toolId, String toolName, Map<String, Object> args) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record TurnComplete(int turn) implements AgentEvent {}`（源码行 26-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record TurnComplete(int turn) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record LoopComplete(int totalTurns) implements AgentEvent {}`（源码行 28-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record LoopComplete(int totalTurns) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record UsageEvent(int inputTokens, int outputTokens) implements AgentEvent {}`（源码行 30-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record UsageEvent(int inputTokens, int outputTokens) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ErrorEvent(String message) implements AgentEvent {}`（源码行 32-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ErrorEvent(String message) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record CompactEvent(String message) implements AgentEvent {}`（源码行 34-34）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record CompactEvent(String message) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record RetryEvent(String reason, long waitMs) implements AgentEvent {}`（源码行 36-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record RetryEvent(String reason, long waitMs) implements AgentEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/agent/StreamingExecutor.java`

**类型与边界。** 包 `com.jimicode.agent`；类型 `StreamingExecutor, ToolCallInfo, ToolExecResult`；212 行；直接依赖 `com.jimicode.compact.RecoveryState, com.jimicode.hook.HookEngine, com.jimicode.permission.PermissionChecker, com.jimicode.permission.PermissionResponse, com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolRegistry, com.jimicode.tool.ToolResult, java.io.IOException, java.nio.file.Files`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ToolCallInfo(String toolId, String toolName, Map<String, Object> args) {}`（源码行 41-41）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record ToolCallInfo(String toolId, String toolName, Map<String, Object> args) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record ToolExecResult(String toolId, String output, boolean isError) {}`（源码行 42-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record ToolExecResult(String toolId, String output, boolean isError) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `this(registry, checker, hookEngine, eventQueue, null);`（源码行 46-51）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        this(registry, checker, hookEngine, eventQueue, null);
    }

    public StreamingExecutor(ToolRegistry registry, PermissionChecker checker,
                             HookEngine hookEngine, BlockingQueue<AgentEvent> eventQueue,
                             RecoveryState recoveryState) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<ToolExecResult> executeAll(List<ToolCallInfo> calls) {`（源码行 59-90）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<ToolExecResult> executeAll(List<ToolCallInfo> calls) {
        var readCalls = new ArrayList<ToolCallInfo>();
        var otherCalls = new ArrayList<ToolCallInfo>();
        for (var call : calls) {
            var tool = registry.get(call.toolName());
            if (tool != null && tool.category() == ToolCategory.READ) {
                readCalls.add(call);
            } else {
                otherCalls.add(call);
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private ToolExecResult executeSingle(ToolCallInfo call) {`（源码行 92-169）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ToolExecResult executeSingle(ToolCallInfo call) {
        Tool tool = registry.get(call.toolName());
        if (tool == null) {
            putSafe(new AgentEvent.ToolResultEvent(call.toolId(), call.toolName(), "Unknown tool", true, 0));
            return new ToolExecResult(call.toolId(), "Error: unknown tool '" + call.toolName() + "'", true);
        }

        // Pre-tool hooks can reject execution
        if (hookEngine != null) {
            var hookResult = hookEngine.runPreToolHooks(call.toolName(), call.args());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void putSafe(AgentEvent event) {`（源码行 171-177）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void putSafe(AgentEvent event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void snapshotForRecovery(ToolCallInfo call, ToolResult result) {`（源码行 185-199）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void snapshotForRecovery(ToolCallInfo call, ToolResult result) {
        if (recoveryState == null || result.isError()) return;
        if (!"ReadFile".equals(call.toolName())) return;
        Object pathObj = call.args() == null ? null : call.args().get("file_path");
        if (!(pathObj instanceof String) || ((String) pathObj).isEmpty()) return;
        String path = (String) pathObj;
        try {
            String content = Files.readString(Path.of(path));
            recoveryState.recordFileRead(path, content);
        } catch (IOException ignored) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String extractContent(String toolName, Map<String, Object> args) {`（源码行 201-211）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String extractContent(String toolName, Map<String, Object> args) {
        String field = switch (toolName) {
            case "Bash" -> "command";
            case "ReadFile", "WriteFile", "EditFile" -> "file_path";
            case "Glob", "Grep" -> "pattern";
            default -> null;
        };
        if (field == null) return null;
        var v = args.get(field);
        return v instanceof String s ? s : null;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/command/Command.java`

**类型与边界。** 包 `com.jimicode.command`；类型 `Command, CommandType`；52 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public boolean matches(String input) {`（源码行 40-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean matches(String input) {
        if (name.equals(input)) {
            return true;
        }
        for (var alias : aliases) {
            if (alias.equals(input)) {
                return true;
            }
        }
        return false;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/command/CommandContext.java`

**类型与边界。** 包 `com.jimicode.command`；类型 `CommandContext`；30 行；直接依赖 `java.util.List, java.util.function.IntSupplier, java.util.function.Supplier`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- 未识别到常规方法签名；重点检查 record 组件、重写方法、lambda、静态初始化块和注解处理器生成代码。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/command/CommandRegistry.java`

**类型与边界。** 包 `com.jimicode.command`；类型 `CommandRegistry`；300 行；直接依赖 `com.jimicode.command.Command.CommandType, java.util.*, java.util.function.Function, java.util.stream.Collectors`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public CommandRegistry() {`（源码行 24-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public CommandRegistry() {
        registerDefaults();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void register(Command cmd, Function<CommandContext, String> handler) {`（源码行 38-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void register(Command cmd, Function<CommandContext, String> handler) {
        commands.add(cmd);
        if (handler != null) {
            handlers.put(cmd.name(), handler);
            for (var alias : cmd.aliases()) {
                handlers.put(alias, handler);
            }
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Command> search(String prefix) {`（源码行 52-69）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Command> search(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return commands.stream()
                .filter(c -> !c.hidden())
                .filter(c -> {
                    if (c.name().toLowerCase(Locale.ROOT).startsWith(lower)) {
                        return true;
                    }
                    for (var alias : c.aliases()) {
                        if (alias.toLowerCase(Locale.ROOT).startsWith(lower)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Optional<Command> find(String name) {`（源码行 72-76）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Optional<Command> find(String name) {
        return commands.stream()
                .filter(c -> c.matches(name))
                .findFirst();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String execute(String name, CommandContext ctx) {`（源码行 85-99）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String execute(String name, CommandContext ctx) {
        Function<CommandContext, String> handler = handlers.get(name);
        if (handler != null) {
            return handler.apply(ctx);
        }
        Optional<Command> cmd = find(name);
        if (cmd.isEmpty()) {
            return "Unknown command: " + name;
        }
        handler = handlers.get(cmd.get().name());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Command> listAll() {`（源码行 102-104）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Command> listAll() {
        return Collections.unmodifiableList(commands);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Command> listVisible() {`（源码行 107-112）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Command> listVisible() {
        return commands.stream()
                .filter(c -> !c.hidden())
                .sorted(Comparator.comparing(Command::name))
                .collect(Collectors.toList());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void registerDefaults() {`（源码行 118-238）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void registerDefaults() {
        // /help (LOCAL, aliases: h, ?)
        register(
                new Command("help", "Show available commands",
                        new String[]{"h", "?"}, CommandType.LOCAL, false),
                ctx -> {
                    String args = ctx.args();
                    if (args != null && !args.isBlank()) {
                        Optional<Command> target = find(args.strip());
                        if (target.isEmpty()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `return switch (sub) {`（源码行 249-254）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                    return switch (sub) {
                        case "info" -> "Current permission mode: " + ctx.permissionMode().get();
                        case "mode" -> "Usage: /permission mode <default|acceptEdits|plan|bypassPermissions>";

                        default -> "Usage: /permission [info|mode <mode>|rules]";
                    };
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (skills.isEmpty()) return "No skills installed.\n\nAdd skills to .JimiCode/skills/<skill-name>/SKILL.md";`（源码行 278-288）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                    if (skills.isEmpty()) return "No skills installed.\n\nAdd skills to .JimiCode/skills/<skill-name>/SKILL.md";
                    var sb = new StringBuilder("Installed skills (%d):\n".formatted(skills.size()));
                    for (var s : skills) sb.append("  • ").append(s).append("\n");
                    return sb.toString();
                }
        );

        // /review (PROMPT)
        register(
                new Command("review", "Review current code changes",
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (args != null && !args.isBlank()) {`（源码行 293-295）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                    if (args != null && !args.isBlank()) {
                        prompt += "\n\nAdditional focus: " + args.strip();
                    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/compact/ContextCompactor.java`

**类型与边界。** 包 `com.jimicode.compact`；类型 `ContextCompactor, AutoCompactTrackingState, UsageAnchor, so`；729 行；直接依赖 `com.fasterxml.jackson.core.JsonProcessingException, com.fasterxml.jackson.databind.ObjectMapper, com.jimicode.conversation.ConversationManager, com.jimicode.conversation.Message, com.jimicode.conversation.ThinkingBlock, com.jimicode.conversation.ToolResultBlock, com.jimicode.conversation.ToolUseBlock, com.jimicode.llm.LlmClient, com.jimicode.llm.StreamEvent, com.jimicode.session.SessionManager`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private ContextCompactor() {}`（源码行 108-108）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ContextCompactor() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean isTripped() {`（源码行 115-117）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public boolean isTripped() {
            return consecutiveFailures >= MAX_CONSECUTIVE_FAILURES;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void recordFailure() {`（源码行 119-121）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void recordFailure() {
            consecutiveFailures++;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void reset() {`（源码行 123-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void reset() {
            consecutiveFailures = 0;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static int computeCompactThreshold(int contextWindow, int maxOutput, boolean manual) {`（源码行 136-144）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static int computeCompactThreshold(int contextWindow, int maxOutput, boolean manual) {
        int reserve = SUMMARY_OUTPUT_RESERVE;
        if (maxOutput > 0 && maxOutput < reserve) {
            reserve = maxOutput;
        }
        int effectiveWindow = contextWindow - reserve;
        int margin = manual ? MANUAL_COMPACT_SAFETY_MARGIN : AUTO_COMPACT_SAFETY_MARGIN;
        return effectiveWindow - margin;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (tokens < computeCompactThreshold(contextWindow, maxOutput, false)) {`（源码行 188-190）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (tokens < computeCompactThreshold(contextWindow, maxOutput, false)) {
            return l1;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (tokens >= computeCompactThreshold(contextWindow, maxOutput, true)) {`（源码行 195-197）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (tokens >= computeCompactThreshold(contextWindow, maxOutput, true)) {
            return forceCompact(conv, client, contextWindow, workDir, sessionId, recovery, toolSchemas);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (tracking == null || !tracking.isTripped()) {`（源码行 199-207）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (tracking == null || !tracking.isTripped()) {
            try {
                String l2 = autoCompact(conv, client, contextWindow, workDir, sessionId, recovery, toolSchemas);
                if (tracking != null) tracking.reset();
                return l2;
            } catch (Exception e) {
                if (tracking != null) tracking.recordFailure();
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `return autoCompact(conv, client, contextWindow, null, null, recovery, toolSchemas);`（源码行 214-219）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        return autoCompact(conv, client, contextWindow, null, null, recovery, toolSchemas);
    }

    /**
     * Force a full auto-compact, writing a compact_boundary into the session log
     * when {@code workDir}/{@code sessionId} are provided.
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `return autoCompact(conv, client, contextWindow, workDir, sessionId, recovery, toolSchemas);`（源码行 224-229）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        return autoCompact(conv, client, contextWindow, workDir, sessionId, recovery, toolSchemas);
    }

    /**
     * Real API-usage anchor for context-window accounting. Captured after each
     * stream ends: {@code baselineTokens} = input + cacheRead + cacheCreation +
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record UsageAnchor(int baselineTokens, int anchorCount) {}`（源码行 236-236）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record UsageAnchor(int baselineTokens, int anchorCount) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static int currentTokens(List<Message> messages, UsageAnchor anchor) {`（源码行 247-254）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static int currentTokens(List<Message> messages, UsageAnchor anchor) {
        if (anchor == null || anchor.anchorCount() < 0
                || anchor.anchorCount() > messages.size()) {
            return estimateTokens(messages);
        }
        List<Message> appended = messages.subList(anchor.anchorCount(), messages.size());
        return anchor.baselineTokens() + estimateTokens(appended);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static int estimateTokens(List<Message> messages) {`（源码行 257-287）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static int estimateTokens(List<Message> messages) {
        int total = 0;
        for (Message m : messages) {
            total += (int) (safeLength(m.getContent()) / 3.5) + 4;

            if (m.getToolUses() != null) {
                for (ToolUseBlock tu : m.getToolUses()) {
                    String argsJson;
                    try {
                        argsJson = MAPPER.writeValueAsString(tu.arguments());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String offloadAndSnip(ConversationManager conv, String workDir) {`（源码行 291-386）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String offloadAndSnip(ConversationManager conv, String workDir) {
        List<Message> messages = conv.getMessagesMutable();
        if (messages.isEmpty()) return "";

        String spillDir = workDir != null
                ? Path.of(workDir, SPILL_SUBDIR).toString()
                : null;
        int spillCount = 0;
        int snipCount = 0;
        int savedChars = 0;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static int computeKeepStartIndex(List<Message> messages) {`（源码行 409-440）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static int computeKeepStartIndex(List<Message> messages) {
        int n = messages.size();
        if (n == 0) return 0;

        int accumulated = 0;
        int kept = 0;
        int keepStart = n; // exclusive walk: keepStart is the first kept index
        for (int i = n - 1; i >= 0; i--) {
            int msgTokens = estimateTokens(List.of(messages.get(i)));
            // Cap: if this message would push the kept tail past the ceiling,
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean isToolResultMessage(Message m) {`（源码行 442-446）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean isToolResultMessage(Message m) {
        return "user".equals(m.getRole())
                && m.getToolResults() != null
                && !m.getToolResults().isEmpty();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (keepStartIndex <= 0 || keepStartIndex < MIN_KEEP_MESSAGES) {`（源码行 460-462）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (keepStartIndex <= 0 || keepStartIndex < MIN_KEEP_MESSAGES) {
            return "";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (workDir != null && !workDir.isBlank() && sessionId != null && !sessionId.isBlank()) {`（源码行 481-487）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (workDir != null && !workDir.isBlank() && sessionId != null && !sessionId.isBlank()) {
            List<SessionManager.KeepMessage> keepRecords = new ArrayList<>(toKeep.size());
            for (Message m : toKeep) {
                keepRecords.add(new SessionManager.KeepMessage(m.getRole(), nullSafe(m.getContent())));
            }
            SessionManager.saveCompactBoundary(workDir, sessionId, summaryText, keepRecords);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!toKeep.isEmpty()) {`（源码行 490-492）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (!toKeep.isEmpty()) {
            content += "\n\n近期消息已原样保留。";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (workDir != null && !workDir.isBlank() && sessionId != null && !sessionId.isBlank()) {`（源码行 493-496）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (workDir != null && !workDir.isBlank() && sessionId != null && !sessionId.isBlank()) {
            content += "\n\n如果你需要压缩前的具体细节（代码片段、报错信息等），请用 ReadFile 读取完整会话记录："
                    + Path.of(workDir, ".JimiCode", "sessions", sessionId + ".jsonl");
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!attachment.isEmpty()) {`（源码行 498-500）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (!attachment.isEmpty()) {
            content += "\n\n---\n\n" + attachment;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (Message m : toKeep) {`（源码行 505-507）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (Message m : toKeep) {
            appendMessage(compacted, m);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `replaceConversation(conv, compacted);`（源码行 509-526）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        replaceConversation(conv, compacted);

        int afterTokens = estimateTokens(conv.getMessages());
        return String.format("Compacted: %d -> %d estimated tokens", beforeTokens, afterTokens);
    }

    // ── Post-compact recovery attachment ───────────────────────────────

    /**
     * Render the four-section recovery block that gets appended to the
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (state != null) {`（源码行 529-544）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (state != null) {
            var files = state.snapshotFiles(RECOVERY_FILE_LIMIT);
            if (!files.isEmpty()) {
                sb.append("## Recently read files\n\n")
                  .append("These snapshots are what the file-reading tool last returned. ")
                  .append("Re-open with the tool if you need the current bytes.\n\n");
                for (var f : files) {
                    String body = truncateByTokens(f.content(), RECOVERY_TOKENS_PER_FILE);
                    sb.append("### ").append(f.path())
                      .append("  (read ").append(RECOVERY_TS.format(f.timestamp())).append(")\n\n")
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (state != null) {`（源码行 546-566）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (state != null) {
            var skills = state.snapshotSkills();
            if (!skills.isEmpty()) {
                var section = new StringBuilder();
                section.append("## Active skills\n\n")
                       .append("These skills were invoked earlier in the session. ")
                       .append("Continue to follow each SOP when its triggering condition applies.\n\n");
                int used = 0;
                boolean emitted = false;
                for (var sk : skills) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (toolSchemas != null && !toolSchemas.isEmpty()) {`（源码行 568-586）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (toolSchemas != null && !toolSchemas.isEmpty()) {
            sb.append("## Available tools\n\n")
              .append("You still have access to the following tools — call them directly when the task needs one:\n\n");
            for (var t : toolSchemas) {
                if (t == null) continue;
                Object nameObj = t.get("name");
                if (nameObj == null) continue;
                String name = nameObj.toString();
                if (name.isEmpty()) continue;
                Object descObj = t.get("description");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (sb.length() == 0) return "";`（源码行 588-596）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (sb.length() == 0) return "";

        sb.append("## Note\n\nEverything above the divider is reconstructed context. ")
          .append("For exact code, error strings, or user-typed text, re-read the source rather than ")
          .append("guess from the summary.\n");
        return sb.toString();
    }

    private static int approxTokens(String s) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (s == null || s.isEmpty()) return 0;`（源码行 597-601）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (s == null || s.isEmpty()) return 0;
        return (int) (s.length() / RECOVERY_CHARS_PER_TOKEN);
    }

    private static String truncateByTokens(String s, int tokenBudget) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (s == null || s.isEmpty() || tokenBudget <= 0) return s == null ? "" : s;`（源码行 602-609）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (s == null || s.isEmpty() || tokenBudget <= 0) return s == null ? "" : s;
        if (approxTokens(s) <= tokenBudget) return s;
        int maxChars = (int) (tokenBudget * RECOVERY_CHARS_PER_TOKEN);
        if (maxChars <= 0 || maxChars >= s.length()) return s;
        return s.substring(0, maxChars) + "\n… (content truncated)";
    }

    private static String firstLine(String s) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (s == null) return "";`（源码行 610-614）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (s == null) return "";
        for (String line : s.split("\n")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) return trimmed;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean alreadyProcessed(String s) {`（源码行 620-622）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean alreadyProcessed(String s) {
        return s != null && (s.startsWith("[Result of ") || s.startsWith("[Stale output snipped:"));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Path writeSpill(String spillDir, String toolUseId, String content) {`（源码行 624-636）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Path writeSpill(String spillDir, String toolUseId, String content) {
        try {
            Path dir = Path.of(spillDir);
            Files.createDirectories(dir);
            Path file = dir.resolve(toolUseId);
            Files.writeString(file, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return file;
        } catch (FileAlreadyExistsException e) {
            return Path.of(spillDir).resolve(toolUseId);
        } catch (IOException e) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String formatCompactSummary(String raw) {`（源码行 638-645）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String formatCompactSummary(String raw) {
        int start = raw.indexOf("<summary>");
        int end = raw.indexOf("</summary>");
        if (start >= 0 && end > start) {
            return raw.substring(start + "<summary>".length(), end).strip();
        }
        return raw.strip();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String requestSummary(LlmClient client, String prompt) {`（源码行 647-671）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String requestSummary(LlmClient client, String prompt) {
        ConversationManager summaryConv = new ConversationManager();
        summaryConv.addUserMessage(prompt);

        BlockingQueue<StreamEvent> events = client.stream(summaryConv, null);
        var summary = new StringBuilder();

        try {
            while (true) {
                StreamEvent ev = events.take();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String serializeForSummary(List<Message> messages, int toolResultCap) {`（源码行 673-694）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String serializeForSummary(List<Message> messages, int toolResultCap) {
        var sb = new StringBuilder();
        for (Message m : messages) {
            sb.append(String.format("[%s]: %s\n", m.getRole(), nullSafe(m.getContent())));

            if (m.getToolUses() != null) {
                for (ToolUseBlock tu : m.getToolUses()) {
                    sb.append(String.format("[tool_use %s]: %s\n", tu.toolName(), tu.toolUseId()));
                }
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void appendMessage(ConversationManager conv, Message m) {`（源码行 696-706）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void appendMessage(ConversationManager conv, Message m) {
        if (m.getToolUses() != null && !m.getToolUses().isEmpty()) {
            conv.addAssistantFull(m.getContent(), m.getThinkingBlocks(), m.getToolUses());
        } else if (m.getToolResults() != null && !m.getToolResults().isEmpty()) {
            conv.addToolResultsMessage(m.getToolResults());
        } else if ("user".equals(m.getRole())) {
            conv.addUserMessage(m.getContent());
        } else if ("assistant".equals(m.getRole())) {
            conv.addAssistantFull(m.getContent(), m.getThinkingBlocks(), null);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void rebuildConversation(ConversationManager conv, List<Message> messages) {`（源码行 708-714）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void rebuildConversation(ConversationManager conv, List<Message> messages) {
        ConversationManager rebuilt = new ConversationManager();
        for (Message m : messages) {
            appendMessage(rebuilt, m);
        }
        replaceConversation(conv, rebuilt);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void replaceConversation(ConversationManager target, ConversationManager source) {`（源码行 716-720）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void replaceConversation(ConversationManager target, ConversationManager source) {
        List<Message> targetList = target.getMessagesMutable();
        targetList.clear();
        targetList.addAll(source.getMessages());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static int safeLength(String s) {`（源码行 722-724）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static int safeLength(String s) {
        return s == null ? 0 : s.length();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String nullSafe(String s) {`（源码行 726-728）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/compact/RecoveryState.java`

**类型与边界。** 包 `com.jimicode.compact`；类型 `RecoveryState, FileReadRecord, SkillInvocationRecord, for, for`；79 行；直接依赖 `java.time.Instant, java.util.ArrayList, java.util.Comparator, java.util.HashMap, java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record FileReadRecord(String path, String content, Instant timestamp) {}`（源码行 30-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record FileReadRecord(String path, String content, Instant timestamp) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record SkillInvocationRecord(String name, String body, Instant timestamp) {}`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record SkillInvocationRecord(String name, String body, Instant timestamp) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void recordFileRead(String path, String content) {`（源码行 41-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void recordFileRead(String path, String content) {
        if (path == null || path.isEmpty()) return;

        synchronized (lock) {
            files.put(path, new FileReadRecord(path, content, Instant.now()));
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void recordSkillInvocation(String name, String body) {`（源码行 50-55）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void recordSkillInvocation(String name, String body) {
        if (name == null || name.isEmpty()) return;
        synchronized (lock) {
            skills.put(name, new SkillInvocationRecord(name, body, Instant.now()));
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<FileReadRecord> snapshotFiles(int limit) {`（源码行 58-68）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<FileReadRecord> snapshotFiles(int limit) {
        List<FileReadRecord> out;
        synchronized (lock) {
            out = new ArrayList<>(files.values());
        }
        out.sort(Comparator.comparing(FileReadRecord::timestamp).reversed());
        if (limit > 0 && out.size() > limit) {
            return out.subList(0, limit);
        }
        return out;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<SkillInvocationRecord> snapshotSkills() {`（源码行 71-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<SkillInvocationRecord> snapshotSkills() {
        List<SkillInvocationRecord> out;
        synchronized (lock) {
            out = new ArrayList<>(skills.values());
        }
        out.sort(Comparator.comparing(SkillInvocationRecord::timestamp).reversed());
        return out;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/config/AppConfig.java`

**类型与边界。** 包 `com.jimicode.config`；类型 `AppConfig`；31 行；直接依赖 `java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public List<ProviderConfig> getProviders() { return providers; }`（源码行 18-18）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<ProviderConfig> getProviders() { return providers; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setProviders(List<ProviderConfig> providers) { this.providers = providers; }`（源码行 20-20）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setProviders(List<ProviderConfig> providers) { this.providers = providers; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getPermissionMode() { return permissionMode; }`（源码行 22-22）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getPermissionMode() { return permissionMode; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setPermissionMode(String permissionMode) { this.permissionMode = permissionMode; }`（源码行 24-24）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setPermissionMode(String permissionMode) { this.permissionMode = permissionMode; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<McpServerConfig> getMcpServers() { return mcpServers; }`（源码行 26-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<McpServerConfig> getMcpServers() { return mcpServers; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMcpServers(List<McpServerConfig> mcpServers) { this.mcpServers = mcpServers; }`（源码行 27-27）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMcpServers(List<McpServerConfig> mcpServers) { this.mcpServers = mcpServers; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<HookConfig> getHooks() { return hooks; }`（源码行 29-29）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<HookConfig> getHooks() { return hooks; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setHooks(List<HookConfig> hooks) { this.hooks = hooks; }`（源码行 30-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setHooks(List<HookConfig> hooks) { this.hooks = hooks; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/config/ConfigLoader.java`

**类型与边界。** 包 `com.jimicode.config`；类型 `ConfigLoader, SnakeCasePropertyUtils, ConfigException`；182 行；直接依赖 `org.yaml.snakeyaml.LoaderOptions, org.yaml.snakeyaml.Yaml, org.yaml.snakeyaml.constructor.Constructor, org.yaml.snakeyaml.introspector.Property, org.yaml.snakeyaml.introspector.PropertyUtils, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.ArrayList, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public static AppConfig load(String path) throws ConfigException {`（源码行 25-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static AppConfig load(String path) throws ConfigException {
        if (path != null && !path.isEmpty()) {
            var cfg = loadSingleFile(Path.of(path));
            validate(cfg);
            return cfg;
        }

        var cwd = Path.of(System.getProperty("user.dir"));
        var home = Path.of(System.getProperty("user.home"));
        var candidates = List.of(
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static AppConfig loadSingleFile(Path configPath) throws ConfigException {`（源码行 60-82）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static AppConfig loadSingleFile(Path configPath) throws ConfigException {
        if (!Files.exists(configPath)) {
            throw new ConfigException("Config file not found: " + configPath);
        }
        String content;
        try {
            content = Files.readString(configPath);
        } catch (IOException e) {
            throw new ConfigException("Failed to read config " + configPath + ": " + e.getMessage());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void mergeConfig(AppConfig base, AppConfig override) {`（源码行 84-115）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void mergeConfig(AppConfig base, AppConfig override) {
        if (override.getProviders() != null && !override.getProviders().isEmpty()) {
            base.setProviders(override.getProviders());
        }
        if (override.getPermissionMode() != null && !override.getPermissionMode().isBlank()) {
            base.setPermissionMode(override.getPermissionMode());
        }
        if (override.getMcpServers() != null && !override.getMcpServers().isEmpty()) {
            var servers = base.getMcpServers() != null
                    ? new ArrayList<>(base.getMcpServers()) : new ArrayList<McpServerConfig>();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void validate(AppConfig cfg) throws ConfigException {`（源码行 117-147）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void validate(AppConfig cfg) throws ConfigException {
        if (cfg == null || cfg.getProviders() == null || cfg.getProviders().isEmpty()) {
            throw new ConfigException("At least one provider must be configured");
        }

        for (int i = 0; i < cfg.getProviders().size(); i++) {
            var p = cfg.getProviders().get(i);
            if (p == null) {
                throw new ConfigException("Provider #%d must not be null".formatted(i + 1));
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean isBlank(String s) {`（源码行 149-151）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Property getProperty(Class<?> type, String name) {`（源码行 156-159）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Property getProperty(Class<?> type, String name) {
            String camel = snakeToCamel(name);
            return super.getProperty(type, camel);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String snakeToCamel(String snake) {`（源码行 161-174）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        private static String snakeToCamel(String snake) {
            if (!snake.contains("_")) return snake;
            var sb = new StringBuilder();
            boolean upper = false;
            for (char c : snake.toCharArray()) {
                if (c == '_') {
                    upper = true;
                } else {
                    sb.append(upper ? Character.toUpperCase(c) : c);
                    upper = false;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ConfigException(String message) {`（源码行 178-180）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ConfigException(String message) {
            super(message);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/config/HookConfig.java`

**类型与边界。** 包 `com.jimicode.config`；类型 `HookConfig`；41 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public String getId() { return id; }`（源码行 19-19）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getId() { return id; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setId(String id) { this.id = id; }`（源码行 21-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setId(String id) { this.id = id; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getEvent() { return event; }`（源码行 23-23）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getEvent() { return event; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setEvent(String event) { this.event = event; }`（源码行 25-25）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setEvent(String event) { this.event = event; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getCondition() { return condition; }`（源码行 27-27）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getCondition() { return condition; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setCondition(String condition) { this.condition = condition; }`（源码行 28-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setCondition(String condition) { this.condition = condition; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getType() { return type; }`（源码行 30-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getType() { return type; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setType(String type) { this.type = type; }`（源码行 31-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setType(String type) { this.type = type; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getCommand() { return command; }`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getCommand() { return command; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setCommand(String command) { this.command = command; }`（源码行 34-34）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setCommand(String command) { this.command = command; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getMessage() { return message; }`（源码行 36-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getMessage() { return message; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMessage(String message) { this.message = message; }`（源码行 37-37）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMessage(String message) { this.message = message; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean isReject() { return reject; }`（源码行 39-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean isReject() { return reject; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setReject(boolean reject) { this.reject = reject; }`（源码行 40-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setReject(boolean reject) { this.reject = reject; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/config/McpServerConfig.java`

**类型与边界。** 包 `com.jimicode.config`；类型 `McpServerConfig`；39 行；直接依赖 `java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public String getName() { return name; }`（源码行 20-20）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getName() { return name; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setName(String name) { this.name = name; }`（源码行 21-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setName(String name) { this.name = name; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getCommand() { return command; }`（源码行 23-23）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getCommand() { return command; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setCommand(String command) { this.command = command; }`（源码行 25-25）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setCommand(String command) { this.command = command; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> getArgs() { return args; }`（源码行 27-27）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<String> getArgs() { return args; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setArgs(List<String> args) { this.args = args; }`（源码行 28-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setArgs(List<String> args) { this.args = args; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getUrl() { return url; }`（源码行 30-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getUrl() { return url; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setUrl(String url) { this.url = url; }`（源码行 31-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setUrl(String url) { this.url = url; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, String> getHeaders() { return headers; }`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, String> getHeaders() { return headers; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setHeaders(Map<String, String> headers) { this.headers = headers; }`（源码行 35-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, String> getEnv() { return env; }`（源码行 37-37）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, String> getEnv() { return env; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setEnv(Map<String, String> env) { this.env = env; }`（源码行 38-38）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setEnv(Map<String, String> env) { this.env = env; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/config/ProviderConfig.java`

**类型与边界。** 包 `com.jimicode.config`；类型 `ProviderConfig`；149 行；直接依赖 `java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public String getName() { return name; }`（源码行 37-37）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getName() { return name; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setName(String name) { this.name = name; }`（源码行 38-38）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setName(String name) { this.name = name; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getProtocol() { return protocol; }`（源码行 40-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getProtocol() { return protocol; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setProtocol(String protocol) { this.protocol = protocol; }`（源码行 42-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setProtocol(String protocol) { this.protocol = protocol; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getBaseUrl() { return baseUrl; }`（源码行 44-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getBaseUrl() { return baseUrl; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }`（源码行 45-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getModel() { return model; }`（源码行 47-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getModel() { return model; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setModel(String model) { this.model = model; }`（源码行 48-48）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setModel(String model) { this.model = model; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getApiKey() { return apiKey; }`（源码行 50-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getApiKey() { return apiKey; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setApiKey(String apiKey) { this.apiKey = apiKey; }`（源码行 51-51）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean isThinking() { return thinking; }`（源码行 53-53）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean isThinking() { return thinking; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setThinking(boolean thinking) { this.thinking = thinking; }`（源码行 54-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setThinking(boolean thinking) { this.thinking = thinking; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int getContextWindow() { return contextWindow; }`（源码行 56-56）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int getContextWindow() { return contextWindow; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setContextWindow(int contextWindow) { this.contextWindow = contextWindow; }`（源码行 58-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setContextWindow(int contextWindow) { this.contextWindow = contextWindow; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int getMaxOutputTokens() { return maxOutputTokens; }`（源码行 60-60）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int getMaxOutputTokens() { return maxOutputTokens; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }`（源码行 61-61）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setFetchedContextWindow(int window) {`（源码行 68-70）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setFetchedContextWindow(int window) {
        if (window > 0) this.fetchedContextWindow = window;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int resolvedContextWindow() {`（源码行 85-93）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int resolvedContextWindow() {
        // Layer 1: explicit config override.
        if (contextWindow > 0) return contextWindow;
        // Layer 2: auto-fetched from the provider (cached at client creation).
        Integer fetched = fetchedContextWindow;
        if (fetched != null && fetched > 0) return fetched;
        // Layers 3 + 4: built-in table, then conservative default.
        return windowForModel(model);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static int windowForModel(String model) {`（源码行 104-115）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static int windowForModel(String model) {
        String m = model == null ? "" : model.toLowerCase();
        // Most specific first.
        if (m.contains("1m") || m.contains("-1m")) return 1_000_000; // explicit 1M-context variants
        if (m.contains("gpt-4.1")) return 1_000_000;
        if (m.contains("gpt-4o")) return 128_000;
        if (m.contains("gpt-4-turbo")) return 128_000;
        if (m.contains("o1") || m.contains("o3") || m.contains("o4")) return 200_000; // OpenAI reasoning models
        if (m.contains("gpt-3.5")) return 16_385;
        if (m.contains("claude")) return 200_000;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int resolvedMaxOutputTokens() {`（源码行 117-120）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int resolvedMaxOutputTokens() {
        if (maxOutputTokens > 0) return maxOutputTokens;
        return thinking ? 64_000 : 8192;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String resolvedApiKey() {`（源码行 136-141）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String resolvedApiKey() {
        // 1) Config file api_key — highest priority.
        if (apiKey != null && !apiKey.isEmpty()) return apiKey;
        // 2) Fall back to the protocol's standard environment variable.
        return envApiKey();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String envApiKey() {`（源码行 143-148）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String envApiKey() {
        String envVar = ENV_KEY_MAP.get(protocol);
        if (envVar == null) return "";
        String val = System.getenv(envVar);
        return val != null ? val : "";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/conversation/ConversationManager.java`

**类型与边界。** 包 `com.jimicode.conversation`；类型 `ConversationManager`；85 行；直接依赖 `java.util.ArrayList, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public void addUserMessage(String content) {`（源码行 17-19）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addUserMessage(String content) {
        history.add(new Message("user", content));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void addAssistantMessage(String content) {`（源码行 21-23）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addAssistantMessage(String content) {
        history.add(new Message("assistant", content));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void addAssistantFull(String text, List<ThinkingBlock> thinking, List<ToolUseBlock> toolUses) {`（源码行 25-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addAssistantFull(String text, List<ThinkingBlock> thinking, List<ToolUseBlock> toolUses) {
        var msg = new Message("assistant", text);
        msg.setThinkingBlocks(thinking);
        msg.setToolUses(toolUses);
        history.add(msg);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void addAssistantMessageWithTools(String text, List<ToolUseBlock> toolUses) {`（源码行 32-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addAssistantMessageWithTools(String text, List<ToolUseBlock> toolUses) {
        var msg = new Message("assistant", text);
        msg.setToolUses(toolUses);
        history.add(msg);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void addToolResultsMessage(List<ToolResultBlock> results) {`（源码行 38-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addToolResultsMessage(List<ToolResultBlock> results) {
        var msg = new Message("user", "");
        msg.setToolResults(results);
        history.add(msg);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void injectLongTermMemory(String instructions, String memories) {`（源码行 44-61）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void injectLongTermMemory(String instructions, String memories) {
        if (ltmInjected) return;
        var sections = new ArrayList<String>();
        if (instructions != null && !instructions.isEmpty()) {
            sections.add("# JimiCodeMd\nCodebase and user instructions are shown below. Be sure to adhere to these instructions. IMPORTANT: These instructions OVERRIDE any default behavior and you MUST follow them exactly as written.\n\n" + instructions);
        }
        if (memories != null && !memories.isEmpty()) {
            sections.add("# autoMemory\n" + memories);
        }
        if (sections.isEmpty()) return;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void addSystemReminder(String content) {`（源码行 63-65）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addSystemReminder(String content) {
        history.add(new Message("user", "<system-reminder>\n" + content + "\n</system-reminder>"));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Message> getMessages() {`（源码行 67-69）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Message> getMessages() {
        return List.copyOf(history);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Message> getMessagesMutable() {`（源码行 71-73）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Message> getMessagesMutable() {
        return history;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int size() {`（源码行 75-77）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int size() {
        return history.size();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void truncateTo(int index) {`（源码行 79-83）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void truncateTo(int index) {
        if (index >= 0 && index < history.size()) {
            history.subList(index, history.size()).clear();
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/conversation/Message.java`

**类型与边界。** 包 `com.jimicode.conversation`；类型 `Message`；41 行；直接依赖 `java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public Message(String role, String content) {`（源码行 20-23）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getRole() { return role; }`（源码行 25-25）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getRole() { return role; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setRole(String role) { this.role = role; }`（源码行 26-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setRole(String role) { this.role = role; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getContent() { return content; }`（源码行 28-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getContent() { return content; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setContent(String content) { this.content = content; }`（源码行 29-29）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setContent(String content) { this.content = content; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<ThinkingBlock> getThinkingBlocks() { return thinkingBlocks; }`（源码行 31-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<ThinkingBlock> getThinkingBlocks() { return thinkingBlocks; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setThinkingBlocks(List<ThinkingBlock> thinkingBlocks) { this.thinkingBlocks = thinkingBlocks; }`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setThinkingBlocks(List<ThinkingBlock> thinkingBlocks) { this.thinkingBlocks = thinkingBlocks; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<ToolUseBlock> getToolUses() { return toolUses; }`（源码行 35-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<ToolUseBlock> getToolUses() { return toolUses; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setToolUses(List<ToolUseBlock> toolUses) { this.toolUses = toolUses; }`（源码行 37-37）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setToolUses(List<ToolUseBlock> toolUses) { this.toolUses = toolUses; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<ToolResultBlock> getToolResults() { return toolResults; }`（源码行 39-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<ToolResultBlock> getToolResults() { return toolResults; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setToolResults(List<ToolResultBlock> toolResults) { this.toolResults = toolResults; }`（源码行 40-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setToolResults(List<ToolResultBlock> toolResults) { this.toolResults = toolResults; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/conversation/ThinkingBlock.java`

**类型与边界。** 包 `com.jimicode.conversation`；类型 `ThinkingBlock`；8 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ThinkingBlock(String thinking, String signature) {}`（源码行 8-8）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
public record ThinkingBlock(String thinking, String signature) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/conversation/ToolResultBlock.java`

**类型与边界。** 包 `com.jimicode.conversation`；类型 `ToolResultBlock`；8 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ToolResultBlock(String toolUseId, String content, boolean isError) {}`（源码行 8-8）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
public record ToolResultBlock(String toolUseId, String content, boolean isError) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/conversation/ToolUseBlock.java`

**类型与边界。** 包 `com.jimicode.conversation`；类型 `ToolUseBlock`；11 行；直接依赖 `java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ToolUseBlock(String toolUseId, String toolName, Map<String, Object> arguments) {}`（源码行 11-11）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
public record ToolUseBlock(String toolUseId, String toolName, Map<String, Object> arguments) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/filehistory/FileHistory.java`

**类型与边界。** 包 `com.jimicode.filehistory`；类型 `FileHistory, Backup, Snapshot`；155 行；直接依赖 `java.io.IOException, java.nio.file.*, java.security.MessageDigest, java.time.Instant, java.util.*, java.util.concurrent.CopyOnWriteArrayList`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record Backup(String backupPath, int version, Instant time) {}`（源码行 17-17）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Backup(String backupPath, int version, Instant time) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public FileHistory(String baseDir, String sessionId) {`（源码行 32-37）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public FileHistory(String baseDir, String sessionId) {
        this.sessionDir = Path.of(baseDir, ".JimiCode", "file-history", sessionId);
        try {
            Files.createDirectories(sessionDir);
        } catch (IOException ignored) {}
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String backupName(String filePath, int version) {`（源码行 39-49）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String backupName(String filePath, int version) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(filePath.getBytes());
            return "%02x%02x%02x%02x%02x%02x%02x%02x@v%d".formatted(
                    hash[0], hash[1], hash[2], hash[3],
                    hash[4], hash[5], hash[6], hash[7], version);
        } catch (Exception e) {
            return "backup-" + filePath.hashCode() + "@v" + version;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void trackEdit(String path) {`（源码行 51-72）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void trackEdit(String path) {
        Path absPath;
        try {
            absPath = Path.of(path).toAbsolutePath();
        } catch (Exception e) {
            absPath = Path.of(path);
        }
        String key = absPath.toString();

        int ver = trackedFiles.getOrDefault(key, 0);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void makeSnapshot(int msgIndex, String userText) {`（源码行 74-98）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void makeSnapshot(int msgIndex, String userText) {
        var backups = new LinkedHashMap<String, Backup>();
        for (var entry : trackedFiles.entrySet()) {
            String path = entry.getKey();
            int ver = entry.getValue();
            String bpName = backupName(path, ver);
            Path bp = sessionDir.resolve(bpName);

            // If backup file missing (new file created in this turn), backup current state
            if (!Files.exists(bp)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Snapshot> getSnapshots() {`（源码行 100-102）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Snapshot> getSnapshots() {
        return List.copyOf(snapshots);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean hasSnapshots() {`（源码行 104-106）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean hasSnapshots() {
        return !snapshots.isEmpty();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<String> rewind(int snapshotIndex) {`（源码行 108-154）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized List<String> rewind(int snapshotIndex) {
        if (snapshotIndex < 0 || snapshotIndex >= snapshots.size()) {
            return List.of();
        }

        Snapshot target = snapshots.get(snapshotIndex);
        var changed = new ArrayList<String>();

        for (var entry : target.backups().entrySet()) {
            String filePath = entry.getKey();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/history/HistoryStore.java`

**类型与边界。** 包 `com.jimicode.history`；类型 `HistoryStore`；155 行；直接依赖 `com.fasterxml.jackson.databind.ObjectMapper, java.io.*, java.nio.file.*, java.time.Instant, java.util.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public HistoryStore() {`（源码行 31-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public HistoryStore() {
        this(Path.of(System.getProperty("user.home"), ".JimiCode", "prompt_history.jsonl"));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public HistoryStore(Path filePath) {`（源码行 36-38）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public HistoryStore(Path filePath) {
        this.filePath = filePath;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void load() {`（源码行 49-73）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void load() {
        entries.clear();
        if (!Files.exists(filePath)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    var node = MAPPER.readTree(line);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void append(String text) {`（源码行 89-108）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void append(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // Deduplicate consecutive identical entries.
        if (!entries.isEmpty() && entries.getLast().equals(text)) {
            return;
        }

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> getEntries() {`（源码行 115-117）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<String> getEntries() {
        return List.copyOf(entries);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int size() {`（源码行 120-122）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int size() {
        return entries.size();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String get(int index) {`（源码行 125-127）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String get(int index) {
        return entries.get(index);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void writeToDisk() {`（源码行 134-154）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void writeToDisk() {
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            long now = Instant.now().getEpochSecond();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/hook/HookEngine.java`

**类型与边界。** 包 `com.jimicode.hook`；类型 `HookEngine, EventName, ActionType, Action, Hook, HookContext, HookResult, PreToolResult`；221 行；直接依赖 `java.io.IOException, java.util.*, java.util.regex.Pattern, java.util.regex.PatternSyntaxException`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `SESSION_START("session_start"),`（源码行 18-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        SESSION_START("session_start"),
        SESSION_END("session_end"),
        TURN_START("turn_start"),
        TURN_END("turn_end"),
        PRE_SEND("pre_send"),
        POST_RECEIVE("post_receive"),
        PRE_TOOL_USE("pre_tool_use"),
        POST_TOOL_USE("post_tool_use"),
        SHUTDOWN("shutdown");

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String value() { return value; }`（源码行 32-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String value() { return value; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `COMMAND("command"),`（源码行 38-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        COMMAND("command"),
        SCRIPT("script"),
        PROMPT("prompt");

        private final String value;

        ActionType(String value) { this.value = value; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String value() { return value; }`（源码行 46-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String value() { return value; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record Action(ActionType type, String command, String message) {}`（源码行 51-51）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Action(ActionType type, String command, String message) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record Hook(String id, EventName event, String condition, Action action, boolean reject) {}`（源码行 53-53）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Hook(String id, EventName event, String condition, Action action, boolean reject) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record HookResult(String hookId, String output, boolean success, boolean reject) {}`（源码行 58-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record HookResult(String hookId, String output, boolean success, boolean reject) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record PreToolResult(boolean rejected, String message) {}`（源码行 60-60）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record PreToolResult(boolean rejected, String message) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void addHook(Hook hook) {`（源码行 69-71）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addHook(Hook hook) {
        hooks.add(hook);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void loadHooks(List<Hook> hookList) {`（源码行 73-76）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void loadHooks(List<Hook> hookList) {
        hooks.clear();
        hooks.addAll(hookList);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<HookResult> runHooks(HookContext ctx) {`（源码行 80-95）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<HookResult> runHooks(HookContext ctx) {
        List<HookResult> results = new ArrayList<>();
        for (Hook h : hooks) {
            if (h.event() != ctx.event()) {
                continue;
            }
            if (h.condition() != null && !h.condition().isEmpty()
                    && !evaluateCondition(h.condition(), ctx)) {
                continue;
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public PreToolResult runPreToolHooks(String toolName, Map<String, Object> args) {`（源码行 97-114）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public PreToolResult runPreToolHooks(String toolName, Map<String, Object> args) {
        HookContext ctx = new HookContext(
                EventName.PRE_TOOL_USE, toolName, args, null, null, null);
        for (Hook h : hooks) {
            if (h.event() != EventName.PRE_TOOL_USE) {
                continue;
            }
            if (h.condition() != null && !h.condition().isEmpty()
                    && !evaluateCondition(h.condition(), ctx)) {
                continue;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<HookResult> drainNotifications() {`（源码行 118-122）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<HookResult> drainNotifications() {
        List<HookResult> result = List.copyOf(notifications);
        notifications.clear();
        return result;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private boolean evaluateCondition(String condition, HookContext ctx) {`（源码行 126-152）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private boolean evaluateCondition(String condition, HookContext ctx) {
        String cond = condition.strip();

        // Equality: "tool==bash"
        if (cond.contains("==")) {
            String[] parts = cond.split("==", 2);
            String left = parts[0].strip();
            String right = stripQuotes(parts[1].strip());
            return resolveVar(left, ctx).equals(right);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String resolveVar(String name, HookContext ctx) {`（源码行 154-170）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String resolveVar(String name, HookContext ctx) {
        return switch (name) {
            case "tool"      -> ctx.toolName()  != null ? ctx.toolName()  : "";
            case "event"     -> ctx.event()     != null ? ctx.event().value() : "";
            case "file_path" -> ctx.filePath()  != null ? ctx.filePath()  : "";
            case "message"   -> ctx.message()   != null ? ctx.message()   : "";

            default -> {
                if (name.startsWith("args.") && ctx.toolArgs() != null) {
                    String key = name.substring("args.".length());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stripQuotes(String s) {`（源码行 172-183）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stripQuotes(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last  = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"')
                    || (first == '\'' && last == '\'')
                    || (first == '/' && last == '/')) {
                return s.substring(1, s.length() - 1);
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private HookResult executeAction(Hook h, HookContext ctx) {`（源码行 187-194）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private HookResult executeAction(Hook h, HookContext ctx) {
        return switch (h.action().type()) {
            case COMMAND -> executeCommand(h, ctx);
            case PROMPT  -> new HookResult(h.id(), h.action().message(), true, h.reject());
            default      -> new HookResult(h.id(),
                    "Unknown action type: " + h.action().type().value(), false, false);
        };
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private HookResult executeCommand(Hook h, HookContext ctx) {`（源码行 196-220）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private HookResult executeCommand(Hook h, HookContext ctx) {
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", h.action().command());
            Map<String, String> env = pb.environment();
            env.put("JimiCode_EVENT", ctx.event() != null ? ctx.event().value() : "");
            env.put("JimiCode_TOOL",  ctx.toolName() != null ? ctx.toolName() : "");
            pb.redirectErrorStream(false);

            Process proc = pb.start();
            String stdout = new String(proc.getInputStream().readAllBytes()).strip();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/JimiCode.java`

**类型与边界。** 包 `com.jimicode`；类型 `JimiCode`；80 行；直接依赖 `com.jimicode.config.AppConfig, com.jimicode.config.ConfigLoader, com.jimicode.permission.PermissionMode, com.jimicode.tui.JimiCodeModel, com.williamcallahan.tui4j.compat.bubbletea.Program, java.util.List, sun.misc.Signal`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public static void main(String[] args) {`（源码行 21-79）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void main(String[] args) {
        // Resolve config path: first CLI arg, then env var, then default
        String configPath = null;
        if (args.length > 0) {
            configPath = args[0];
        } else {
            String envPath = System.getenv("JimiCode_CONFIG");
            if (envPath != null && !envPath.isBlank()) {
                configPath = envPath;
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/llm/AnthropicClient.java`

**类型与边界。** 包 `com.jimicode.llm`；类型 `AnthropicClient`；458 行；直接依赖 `com.anthropic.client.okhttp.AnthropicOkHttpClient, com.anthropic.core.JsonValue, com.anthropic.core.http.StreamResponse, com.anthropic.models.messages.*, com.fasterxml.jackson.databind.ObjectMapper, com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, com.jimicode.conversation.Message, java.util.*, java.util.concurrent.BlockingQueue`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public AnthropicClient(ProviderConfig cfg, String systemPrompt) {`（源码行 32-52）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public AnthropicClient(ProviderConfig cfg, String systemPrompt) {
        String apiKey = cfg.resolvedApiKey();
        if (apiKey.isEmpty()) {
            throw new LlmException.AuthenticationException(
                    "Anthropic API key not found. Set it in .JimiCode/config.yaml or via ANTHROPIC_API_KEY env var.");
        }
        this.sdkClient = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(cfg.getBaseUrl())
                .build();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `int fetchModelContextWindow() {`（源码行 64-79）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    int fetchModelContextWindow() {
        try {
            var info = sdkClient.models().retrieve(
                    model,
                    com.anthropic.core.RequestOptions.builder()
                            .timeout(java.time.Duration.ofSeconds(5))
                            .build());
            return info.maxInputTokens()
                    .map(Long::intValue)
                    .filter(v -> v > 0)
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMaxOutputTokens(int tokens) {`（源码行 82-84）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMaxOutputTokens(int tokens) {
        this.maxOutputTokens = tokens;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools) {`（源码行 87-102）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools) {
        var queue = new LinkedBlockingQueue<StreamEvent>(64);

        Thread.startVirtualThread(() -> {
            try {
                doStream(conv, tools, queue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                try { queue.put(new StreamEvent.Error(classifyError(e).getMessage())); }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `markLastUserTailForCache(messageParams);`（源码行 116-131）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        markLastUserTailForCache(messageParams);
        var paramsBuilder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxOutputTokens)
                .system(MessageCreateParams.System.ofTextBlockParams(List.of(systemBlock)))
                .messages(messageParams);

        if (thinking) {
            if (ModelResolver.supportsAdaptiveThinking(model)) {
                paramsBuilder.thinking(ThinkingConfigAdaptive.builder().build());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (tools != null && !tools.isEmpty()) {`（源码行 133-140）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (tools != null && !tools.isEmpty()) {
            // Tool schemas are stable across turns, so marking the last
            // tool effectively caches the entire tools block on the wire.
            for (int i = 0; i < tools.size(); i++) {
                boolean isLast = (i == tools.size() - 1);
                paramsBuilder.addTool(buildTool(tools.get(i), isLast));
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `while (iterator.hasNext()) {`（源码行 156-236）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            while (iterator.hasNext()) {
                var event = iterator.next();
                if (event.isContentBlockStart()) {
                    var startEvent = event.asContentBlockStart();
                    var block = startEvent.contentBlock();
                    if (block.isThinking()) {
                        inThinking = true;
                        thinkingAccum.setLength(0);
                        thinkingSignature = "";
                    } else if (block.isToolUse()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private List<MessageParam> buildMessages(List<Message> messages) {`（源码行 244-318）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private List<MessageParam> buildMessages(List<Message> messages) {
        var result = new ArrayList<MessageParam>();
        for (var msg : messages) {
            boolean hasThinking = msg.getThinkingBlocks() != null && !msg.getThinkingBlocks().isEmpty();
            boolean hasToolUses = msg.getToolUses() != null && !msg.getToolUses().isEmpty();

            if ("assistant".equals(msg.getRole()) && (hasThinking || hasToolUses)) {
                var content = new ArrayList<ContentBlockParam>();
                if (hasThinking) {
                    for (var tb : msg.getThinkingBlocks()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private List<MessageParam> mergeConsecutiveSameRole(List<MessageParam> messages) {`（源码行 320-345）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private List<MessageParam> mergeConsecutiveSameRole(List<MessageParam> messages) {
        if (messages.size() <= 1) return messages;
        var merged = new ArrayList<MessageParam>();
        merged.add(messages.getFirst());
        for (int i = 1; i < messages.size(); i++) {
            var prev = merged.getLast();
            var curr = messages.get(i);
            if (prev.role().equals(curr.role())) {
                // Both are simple text content — merge them
                var prevContent = prev.content();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Tool buildTool(Map<String, Object> schema, boolean markCache) {`（源码行 348-364）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Tool buildTool(Map<String, Object> schema, boolean markCache) {
        var builder = Tool.builder()
                .name((String) schema.get("name"));
        if (schema.containsKey("description")) {
            builder.description((String) schema.get("description"));
        }
        var inputSchema = (Map<String, Object>) schema.getOrDefault("input_schema",
                Map.of("type", "object", "properties", Map.of()));
        builder.inputSchema(Tool.InputSchema.builder()
                .type(JsonValue.from(inputSchema.getOrDefault("type", "object")))
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void markLastUserTailForCache(List<MessageParam> messages) {`（源码行 377-421）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void markLastUserTailForCache(List<MessageParam> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            var msg = messages.get(i);
            if (msg.role() != MessageParam.Role.USER) continue;
            // The user message's content is either a string or block list.
            // We need block form to attach cache_control, so up-convert if
            // it's a string.
            var content = msg.content();
            List<ContentBlockParam> blocks;
            if (content.string().isPresent()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Map<String, JsonValue> toJsonValueMap(Map<String, Object> map) {`（源码行 423-430）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Map<String, JsonValue> toJsonValueMap(Map<String, Object> map) {
        var result = new LinkedHashMap<String, JsonValue>();
        for (var entry : map.entrySet()) {
            if ("type".equals(entry.getKey())) continue;
            result.put(entry.getKey(), JsonValue.from(entry.getValue()));
        }
        return result;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private LlmException classifyError(Exception e) {`（源码行 432-457）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private LlmException classifyError(Exception e) {
        if (e instanceof LlmException le) return le;
        if (e instanceof com.anthropic.errors.UnauthorizedException ue) {
            return new LlmException.AuthenticationException("Invalid API key: " + ue.getMessage());
        }
        if (e instanceof com.anthropic.errors.RateLimitException) {
            return new LlmException.RateLimitException("Rate limited. Please wait.", "");
        }
        if (e instanceof com.anthropic.errors.BadRequestException bre) {
            String msg = bre.getMessage() != null ? bre.getMessage().toLowerCase() : "";
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/llm/LlmClient.java`

**类型与边界。** 包 `com.jimicode.llm`；类型 `LlmClient`；31 行；直接依赖 `com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, java.util.List, java.util.Map, java.util.concurrent.BlockingQueue`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools);`（源码行 18-20）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools);

    default void setMaxOutputTokens(int tokens) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static LlmClient create(ProviderConfig cfg, String systemPrompt) {`（源码行 22-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static LlmClient create(ProviderConfig cfg, String systemPrompt) {
        return switch (cfg.getProtocol()) {
            case "anthropic" -> new AnthropicClient(cfg, systemPrompt);
            case "openai" -> new OpenAiClient(cfg, systemPrompt);
            case "openai-compat" -> new OpenAiCompatClient(cfg, systemPrompt);

            default -> throw new IllegalArgumentException("Unknown protocol: " + cfg.getProtocol());
        };
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/llm/LlmException.java`

**类型与边界。** 包 `com.jimicode.llm`；类型 `LlmException, AuthenticationException, RateLimitException, ContextTooLongException, NetworkException`；47 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public LlmException(String message) {`（源码行 10-12）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public LlmException(String message) {
        super(message);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public LlmException(String message, Throwable cause) {`（源码行 14-16）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public AuthenticationException(String message) {`（源码行 20-22）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public AuthenticationException(String message) {
            super(message);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public RateLimitException(String message, String retryAfter) {`（源码行 28-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public RateLimitException(String message, String retryAfter) {
            super(message);
            this.retryAfter = retryAfter;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getRetryAfter() { return retryAfter; }`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getRetryAfter() { return retryAfter; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ContextTooLongException(String message) {`（源码行 37-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ContextTooLongException(String message) {
            super(message);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public NetworkException(String message, Throwable cause) {`（源码行 43-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public NetworkException(String message, Throwable cause) {
            super(message, cause);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/llm/ModelResolver.java`

**类型与边界。** 包 `com.jimicode.llm`；类型 `ModelResolver`；31 行；直接依赖 `java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public static String resolve(String model) {`（源码行 18-20）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String resolve(String model) {
        return ALIASES.getOrDefault(model, model);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean supportsAdaptiveThinking(String model) {`（源码行 22-25）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean supportsAdaptiveThinking(String model) {
        String resolved = resolve(model);
        return resolved.contains("opus-4-6") || resolved.contains("sonnet-4-6");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean supportsThinking(String model) {`（源码行 27-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean supportsThinking(String model) {
        String resolved = resolve(model);
        return resolved.contains("claude");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/llm/OpenAiClient.java`

**类型与边界。** 包 `com.jimicode.llm`；类型 `OpenAiClient`；258 行；直接依赖 `com.fasterxml.jackson.databind.ObjectMapper, com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, com.jimicode.conversation.Message, com.openai.client.okhttp.OpenAIOkHttpClient, com.openai.core.JsonValue, com.openai.core.http.StreamResponse, com.openai.models.Reasoning, com.openai.models.ReasoningEffort, com.openai.models.responses.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public OpenAiClient(ProviderConfig cfg, String systemPrompt) {`（源码行 33-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public OpenAiClient(ProviderConfig cfg, String systemPrompt) {
        String apiKey = cfg.resolvedApiKey();
        if (apiKey.isEmpty()) {
            throw new LlmException.AuthenticationException(
                    "OpenAI API key not found. Set it in .JimiCode/config.yaml or via OPENAI_API_KEY env var.");
        }
        this.sdkClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(cfg.getBaseUrl())
                .build();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMaxOutputTokens(int tokens) {`（源码行 50-52）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMaxOutputTokens(int tokens) {
        this.maxOutputTokens = tokens;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools) {`（源码行 55-70）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools) {
        var queue = new LinkedBlockingQueue<StreamEvent>(64);

        Thread.startVirtualThread(() -> {
            try {
                doStream(conv, tools, queue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                try { queue.put(new StreamEvent.Error(classifyError(e).getMessage())); }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (thinking) {`（源码行 80-85）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (thinking) {
            paramsBuilder.reasoning(Reasoning.builder()
                    .effort(ReasoningEffort.HIGH)
                    .summary(Reasoning.Summary.DETAILED)
                    .build());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (tools != null && !tools.isEmpty()) {`（源码行 87-91）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (tools != null && !tools.isEmpty()) {
            for (var schema : tools) {
                paramsBuilder.addTool(buildFunctionTool(schema));
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `while (iterator.hasNext()) {`（源码行 103-157）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            while (iterator.hasNext()) {
                var event = iterator.next();

                if (event.isOutputTextDelta()) {
                    queue.put(new StreamEvent.TextDelta(event.asOutputTextDelta().delta()));
                } else if (event.isOutputItemAdded()) {
                    var item = event.asOutputItemAdded().item();
                    if (item.isFunctionCall()) {
                        var fc = item.asFunctionCall();
                        currentToolName = fc.name();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private List<ResponseInputItem> buildInput(List<Message> messages) {`（源码行 161-206）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private List<ResponseInputItem> buildInput(List<Message> messages) {
        var result = new ArrayList<ResponseInputItem>();
        for (var msg : messages) {
            if (msg.getToolUses() != null && !msg.getToolUses().isEmpty()) {
                if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                    result.add(ResponseInputItem.ofEasyInputMessage(
                            EasyInputMessage.builder()
                                    .role(EasyInputMessage.Role.ASSISTANT)
                                    .content(msg.getContent())
                                    .build()));
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private FunctionTool buildFunctionTool(Map<String, Object> schema) {`（源码行 209-222）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private FunctionTool buildFunctionTool(Map<String, Object> schema) {
        var builder = FunctionTool.builder()
                .name((String) schema.get("name"))
                .strict(false);
        if (schema.containsKey("description")) {
            builder.description(Objects.toString(schema.get("description"), ""));
        }
        var params = (Map<String, Object>) schema.getOrDefault("parameters",
                schema.getOrDefault("input_schema", Map.of()));
        builder.parameters(FunctionTool.Parameters.builder()
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Map<String, JsonValue> toJsonValueMap(Map<String, Object> map) {`（源码行 224-230）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Map<String, JsonValue> toJsonValueMap(Map<String, Object> map) {
        var result = new LinkedHashMap<String, JsonValue>();
        for (var entry : map.entrySet()) {
            result.put(entry.getKey(), JsonValue.from(entry.getValue()));
        }
        return result;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private LlmException classifyError(Exception e) {`（源码行 232-257）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private LlmException classifyError(Exception e) {
        if (e instanceof LlmException le) return le;
        if (e instanceof com.openai.errors.UnauthorizedException) {
            return new LlmException.AuthenticationException("Invalid API key: " + e.getMessage());
        }
        if (e instanceof com.openai.errors.RateLimitException) {
            return new LlmException.RateLimitException("Rate limited. Please wait.", "");
        }
        if (e instanceof com.openai.errors.BadRequestException bre) {
            String msg = bre.getMessage() != null ? bre.getMessage().toLowerCase() : "";
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/llm/OpenAiCompatClient.java`

**类型与边界。** 包 `com.jimicode.llm`；类型 `OpenAiCompatClient`；449 行；直接依赖 `com.fasterxml.jackson.core.JsonProcessingException, com.fasterxml.jackson.databind.JsonNode, com.fasterxml.jackson.databind.ObjectMapper, com.fasterxml.jackson.databind.node.ArrayNode, com.fasterxml.jackson.databind.node.ObjectNode, com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, com.jimicode.conversation.Message, java.io.BufferedReader, java.io.IOException`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public OpenAiCompatClient(ProviderConfig cfg, String systemPrompt) {`（源码行 49-65）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public OpenAiCompatClient(ProviderConfig cfg, String systemPrompt) {
        String key = cfg.resolvedApiKey();
        if (key.isEmpty()) {
            throw new LlmException.AuthenticationException(
                    "API key not found for openai-compat provider '" + cfg.getName()
                            + "'. Set it in .JimiCode/config.yaml or via OPENAI_API_KEY env var.");
        }
        this.apiKey = key;
        this.baseUrl = cfg.getBaseUrl().replaceAll("/+$", "");
        this.model = cfg.getModel();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMaxOutputTokens(int tokens) {`（源码行 68-70）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMaxOutputTokens(int tokens) {
        this.maxOutputTokens = tokens;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools) {`（源码行 77-95）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools) {
        var queue = new LinkedBlockingQueue<StreamEvent>(64);

        Thread.startVirtualThread(() -> {
            try {
                doStream(conv, tools, queue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                try {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (statusCode != 200) {`（源码行 115-121）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (statusCode != 200) {
            String errBody;
            try (var is = response.body()) {
                errBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("HTTP " + statusCode + ": " + errBody);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {`（源码行 130-150）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;       // blank SSE separator
                if (line.startsWith(":")) continue;  // SSE comment

                if (!line.startsWith("data: ")) continue;
                String data = line.substring(6).trim();

                if ("[DONE]".equals(data)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (root.has("error")) {`（源码行 174-179）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (root.has("error")) {
            var errNode = root.get("error");
            String errMsg = errNode.has("message") ? errNode.get("message").asText() : errNode.toString();
            queue.put(new StreamEvent.Error(errMsg));
            return false;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!choices.isArray() || choices.isEmpty()) {`（源码行 182-185）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (!choices.isArray() || choices.isEmpty()) {
            // Might be a usage-only chunk (stream_options.include_usage)
            return emitUsageIfPresent(root, queue);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (delta.has("content") && !delta.get("content").isNull()) {`（源码行 191-196）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (delta.has("content") && !delta.get("content").isNull()) {
            String text = delta.get("content").asText();
            if (!text.isEmpty()) {
                queue.put(new StreamEvent.TextDelta(text));
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (delta.has("tool_calls") && delta.get("tool_calls").isArray()) {`（源码行 199-222）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (delta.has("tool_calls") && delta.get("tool_calls").isArray()) {
            for (JsonNode tc : delta.get("tool_calls")) {
                int idx = tc.path("index").asInt(0);

                // id is present only in the first chunk for a given tool call
                if (tc.has("id") && !tc.get("id").isNull()) {
                    toolIds.put(idx, tc.get("id").asText());
                }

                JsonNode fn = tc.path("function");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if ("tool_calls".equals(finishReason)) {`（源码行 228-237）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if ("tool_calls".equals(finishReason)) {
            flushPendingToolCalls(queue, toolNames, toolArgs, toolIds);
            // Don't emit StreamEnd yet — the agent loop will continue after tool execution
            return false;
        } else if ("stop".equals(finishReason) || "length".equals(finishReason)) {
            String stopReason = "length".equals(finishReason) ? "max_tokens" : "end_turn";
            int[] usage = extractUsage(root);
            queue.put(new StreamEvent.StreamEnd(stopReason, usage[0], usage[1], usage[2], 0));
            return true;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (toolNames.isEmpty()) return;`（源码行 250-269）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (toolNames.isEmpty()) return;

        var sorted = new ArrayList<>(toolNames.keySet());
        Collections.sort(sorted);

        for (int idx : sorted) {
            String name = toolNames.get(idx).toString();
            String callId = toolIds.getOrDefault(idx, "call_" + idx);
            String rawArgs = toolArgs.containsKey(idx) ? toolArgs.get(idx).toString() : "{}";

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private boolean emitUsageIfPresent(JsonNode root, BlockingQueue<StreamEvent> queue) throws InterruptedException {`（源码行 280-287）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private boolean emitUsageIfPresent(JsonNode root, BlockingQueue<StreamEvent> queue) throws InterruptedException {
        int[] usage = extractUsage(root);
        if (usage[0] > 0 || usage[1] > 0) {
            queue.put(new StreamEvent.StreamEnd("end_turn", usage[0], usage[1], usage[2], 0));
            return true;
        }
        return false;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static int[] extractUsage(JsonNode root) {`（源码行 294-305）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static int[] extractUsage(JsonNode root) {
        JsonNode usage = root.path("usage");
        if (usage.isMissingNode()) return new int[]{0, 0, 0};
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int output = usage.path("completion_tokens").asInt(0);
        int cacheRead = usage.path("prompt_tokens_details").path("cached_tokens").asInt(0);
        // OpenAI's prompt_tokens already includes the cached portion; split it out
        // so the anchor's (input + cacheRead + cacheCreation + output) does not
        // double-count the cache hit.
        int input = Math.max(0, promptTokens - cacheRead);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String buildRequestBody(List<Message> messages, List<Map<String, Object>> tools)`（源码行 311-332）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String buildRequestBody(List<Message> messages, List<Map<String, Object>> tools)
            throws JsonProcessingException {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("model", model);
        root.put("stream", true);
        root.put("max_tokens", maxOutputTokens);

        // stream_options: ask for usage in stream
        ObjectNode streamOpts = MAPPER.createObjectNode();
        streamOpts.put("include_usage", true);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private ArrayNode buildChatMessages(List<Message> messages) {`（源码行 335-397）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ArrayNode buildChatMessages(List<Message> messages) {
        ArrayNode arr = MAPPER.createArrayNode();

        // System prompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode sys = MAPPER.createObjectNode();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            arr.add(sys);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private ArrayNode buildToolsArray(List<Map<String, Object>> tools) {`（源码行 400-420）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ArrayNode buildToolsArray(List<Map<String, Object>> tools) {
        ArrayNode arr = MAPPER.createArrayNode();
        for (var schema : tools) {
            ObjectNode tool = MAPPER.createObjectNode();
            tool.put("type", "function");

            ObjectNode fn = MAPPER.createObjectNode();
            fn.put("name", (String) schema.get("name"));
            if (schema.containsKey("description")) {
                fn.put("description", Objects.toString(schema.get("description"), ""));
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private LlmException classifyError(Exception e) {`（源码行 426-448）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private LlmException classifyError(Exception e) {
        if (e instanceof LlmException le) return le;
        String msg = e.getMessage() != null ? e.getMessage() : "";
        String lower = msg.toLowerCase();

        if (msg.startsWith("HTTP 401") || lower.contains("unauthorized") || lower.contains("invalid api key")) {
            return new LlmException.AuthenticationException("Invalid API key: " + msg);
        }
        if (msg.startsWith("HTTP 429") || lower.contains("rate limit")) {
            return new LlmException.RateLimitException("Rate limited. Please wait.", "");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/llm/StreamEvent.java`

**类型与边界。** 包 `com.jimicode.llm`；类型 `StreamEvent, TextDelta, ThinkingDelta, ThinkingComplete, ToolCallStart, ToolCallDelta, ToolCallComplete, StreamEnd, Error`；34 行；直接依赖 `java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `record TextDelta(String text) implements StreamEvent {}`（源码行 12-12）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record TextDelta(String text) implements StreamEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ThinkingDelta(String text) implements StreamEvent {}`（源码行 14-14）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ThinkingDelta(String text) implements StreamEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ThinkingComplete(String thinking, String signature) implements StreamEvent {}`（源码行 16-16）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ThinkingComplete(String thinking, String signature) implements StreamEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ToolCallStart(String toolId, String toolName) implements StreamEvent {}`（源码行 18-18）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ToolCallStart(String toolId, String toolName) implements StreamEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ToolCallDelta(String text) implements StreamEvent {}`（源码行 20-20）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ToolCallDelta(String text) implements StreamEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record ToolCallComplete(String toolId, String toolName, Map<String, Object> arguments) implements StreamEvent {}`（源码行 22-22）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record ToolCallComplete(String toolId, String toolName, Map<String, Object> arguments) implements StreamEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public StreamEnd(String stopReason, int inputTokens, int outputTokens) {`（源码行 28-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public StreamEnd(String stopReason, int inputTokens, int outputTokens) {
            this(stopReason, inputTokens, outputTokens, 0, 0);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record Error(String message) implements StreamEvent {}`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record Error(String message) implements StreamEvent {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/mcp/McpManager.java`

**类型与边界。** 包 `com.jimicode.mcp`；类型 `McpManager, ServerInfo, ConnectResult, McpToolWrapper`；209 行；直接依赖 `com.jimicode.config.McpServerConfig, com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolRegistry, com.jimicode.tool.ToolResult, io.modelcontextprotocol.client.McpClient, io.modelcontextprotocol.client.McpSyncClient, io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport, io.modelcontextprotocol.client.transport.ServerParameters, io.modelcontextprotocol.client.transport.StdioClientTransport`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ServerInfo(String name, String instructions) {}`（源码行 31-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record ServerInfo(String name, String instructions) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record ConnectResult(List<Tool> tools, List<ServerInfo> servers, List<String> errors) {}`（源码行 32-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record ConnectResult(List<Tool> tools, List<ServerInfo> servers, List<String> errors) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public McpManager(List<McpServerConfig> configs) {`（源码行 37-41）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public McpManager(List<McpServerConfig> configs) {
        if (configs != null) {
            for (var cfg : configs) this.configs.put(cfg.getName(), cfg);
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ConnectResult connectAll() {`（源码行 43-72）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ConnectResult connectAll() {
        var tools = new ArrayList<Tool>();
        var servers = new ArrayList<ServerInfo>();
        var errors = new ArrayList<String>();

        for (var entry : configs.entrySet()) {
            String name = entry.getKey();
            var cfg = entry.getValue();

            try {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> registerAllTools(ToolRegistry registry) {`（源码行 74-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<String> registerAllTools(ToolRegistry registry) {
        var result = connectAll();
        for (var t : result.tools()) registry.register(t);
        return result.errors();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void shutdown() {`（源码行 80-85）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void shutdown() {
        for (var client : clients.values()) {
            try { client.closeGracefully(); } catch (Exception ignored) {}
        }
        clients.clear();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private McpSyncClient createClient(McpServerConfig cfg) {`（源码行 87-121）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private McpSyncClient createClient(McpServerConfig cfg) {
        io.modelcontextprotocol.spec.McpClientTransport transport;

        if (cfg.getCommand() != null && !cfg.getCommand().isBlank()) {
            var paramsBuilder = ServerParameters.builder(windowsSafe(cfg.getCommand()));
            if (cfg.getArgs() != null) {
                paramsBuilder.args(cfg.getArgs());
            }
            if (cfg.getEnv() != null) {
                var resolvedEnv = new HashMap<String, String>();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String windowsSafe(String command) {`（源码行 126-131）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String windowsSafe(String command) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return command;
        String base = command.toLowerCase();
        if (WIN_CMD_SUFFIXED.contains(base)) return command + ".cmd";
        return command;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String sanitizeName(String name) {`（源码行 133-135）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String sanitizeName(String name) {
        return NON_ALNUM.matcher(name).replaceAll("_");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String resolveEnvVars(String value) {`（源码行 137-143）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String resolveEnvVars(String value) {
        if (value == null) return null;
        return ENV_VAR.matcher(value).replaceAll(m -> {
            String env = System.getenv(m.group(1));
            return env != null ? env : m.group(0);
        });
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `McpToolWrapper(String serverName, McpSchema.Tool sdkTool, McpSyncClient client) {`（源码行 152-156）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        McpToolWrapper(String serverName, McpSchema.Tool sdkTool, McpSyncClient client) {
            this.serverName = serverName;
            this.sdkTool = sdkTool;
            this.client = client;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public String name() {`（源码行 158-160）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public String name() {
            return "mcp__" + sanitizeName(serverName) + "__" + sanitizeName(sdkTool.name());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public String description() {`（源码行 162-164）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public String description() {
            return sdkTool.description() != null ? sdkTool.description() : "";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public ToolCategory category() { return ToolCategory.COMMAND; }`（源码行 166-166）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public ToolCategory category() { return ToolCategory.COMMAND; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public boolean shouldDefer() { return true; }`（源码行 167-167）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public boolean shouldDefer() { return true; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public Map<String, Object> schema() {`（源码行 169-182）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public Map<String, Object> schema() {
            var input = new LinkedHashMap<String, Object>();
            var jsonSchema = sdkTool.inputSchema();
            if (jsonSchema != null) {
                if (jsonSchema.type() != null) input.put("type", jsonSchema.type());
                if (jsonSchema.properties() != null) input.put("properties", jsonSchema.properties());
                if (jsonSchema.required() != null) input.put("required", jsonSchema.required());
            }
            if (input.isEmpty()) {
                input.put("type", "object");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public ToolResult execute(Map<String, Object> args) {`（源码行 184-195）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public ToolResult execute(Map<String, Object> args) {
            try {
                var request = new McpSchema.CallToolRequest(
                        sdkTool.name(), args != null ? args : Map.of());
                var result = client.callTool(request);
                String text = extractTextContent(result);
                boolean isError = result.isError() != null && result.isError();
                return isError ? ToolResult.error(text) : ToolResult.success(text);
            } catch (Exception e) {
                return ToolResult.error("MCP tool call failed: " + e.getMessage());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String extractTextContent(McpSchema.CallToolResult result) {`（源码行 198-208）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String extractTextContent(McpSchema.CallToolResult result) {
        if (result.content() == null || result.content().isEmpty()) return "(no output)";
        var sb = new StringBuilder();
        for (var content : result.content()) {
            if (content instanceof McpSchema.TextContent tc) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(tc.text());
            }
        }
        return sb.isEmpty() ? "(no output)" : sb.toString();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/memory/MemoryAge.java`

**类型与边界。** 包 `com.jimicode.memory`；类型 `MemoryAge`；48 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private MemoryAge() {}`（源码行 15-15）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private MemoryAge() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static int ageDays(long mtimeMs) {`（源码行 21-24）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static int ageDays(long mtimeMs) {
        long d = (System.currentTimeMillis() - mtimeMs) / 86_400_000L;
        return d < 0 ? 0 : (int) d;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String age(long mtimeMs) {`（源码行 29-34）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String age(long mtimeMs) {
        int d = ageDays(mtimeMs);
        if (d == 0) return "today";
        if (d == 1) return "yesterday";
        return d + " days ago";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String freshnessText(long mtimeMs) {`（源码行 40-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String freshnessText(long mtimeMs) {
        int d = ageDays(mtimeMs);
        if (d <= 1) return "";
        return "This memory is " + d + " days old. "
                + "Memories are point-in-time observations, not live state — "
                + "claims about code behavior or file:line citations may be outdated. "
                + "Verify against current code before asserting as fact.";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/memory/MemoryManager.java`

**类型与边界。** 包 `com.jimicode.memory`；类型 `MemoryManager, MemoryEntry`；276 行；直接依赖 `com.fasterxml.jackson.annotation.JsonInclude, com.fasterxml.jackson.core.type.TypeReference, com.fasterxml.jackson.databind.ObjectMapper, com.jimicode.conversation.ConversationManager, com.jimicode.conversation.Message, com.jimicode.llm.LlmClient, com.jimicode.llm.StreamEvent, java.io.IOException, java.nio.file.*, java.time.Instant`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record MemoryEntry(String content, String timestamp, String type) {`（源码行 26-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record MemoryEntry(String content, String timestamp, String type) {
        public MemoryEntry(String content, String timestamp) {
            this(content, timestamp, null);
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public MemoryManager(String workDir) {`（源码行 46-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public MemoryManager(String workDir) {
        this.projectFilePath = Path.of(workDir, MEMORY_DIR, MEMORY_FILE);
        this.userFilePath = Path.of(System.getProperty("user.home"), MEMORY_DIR, MEMORY_FILE);
        load();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Path userMemDir() {`（源码行 58-60）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Path userMemDir() {
        return userFilePath.getParent();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Path projectMemDir() {`（源码行 66-68）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Path projectMemDir() {
        return projectFilePath.getParent();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void load() {`（源码行 72-76）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void load() {
        entries = new ArrayList<>();
        loadFile(userFilePath);
        loadFile(projectFilePath);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void loadFile(Path path) {`（源码行 78-89）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void loadFile(Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            byte[] data = Files.readAllBytes(path);
            List<MemoryEntry> loaded = MAPPER.readValue(data, new TypeReference<List<MemoryEntry>>() {});
            entries.addAll(loaded);
        } catch (IOException ignored) {
            // best-effort: corrupt file shouldn't kill the manager
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void save() {`（源码行 91-106）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void save() {
        List<MemoryEntry> userScoped = new ArrayList<>();
        List<MemoryEntry> projectScoped = new ArrayList<>();
        for (MemoryEntry e : entries) {
            if (e.type() != null && USER_TYPES.contains(e.type())) {
                userScoped.add(e);
            } else if (e.type() != null && PROJECT_TYPES.contains(e.type())) {
                projectScoped.add(e);
            } else {
                // Legacy entries without a type stay project-level (matches old behavior).
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void writeJson(Path path, List<MemoryEntry> filtered) {`（源码行 108-116）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void writeJson(Path path, List<MemoryEntry> filtered) {
        try {
            Files.createDirectories(path.getParent());
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(filtered);
            Files.writeString(path, json);
        } catch (IOException ignored) {
            // best-effort
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> getMemories() {`（源码行 120-122）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<String> getMemories() {
        return entries.stream().map(MemoryEntry::content).toList();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldExtract() {`（源码行 124-127）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean shouldExtract() {
        turnCount++;
        return turnCount % EXTRACTION_INTERVAL == 0;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void clear() {`（源码行 129-133）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void clear() {
        entries = new ArrayList<>();
        writeJson(userFilePath, List.of());
        writeJson(projectFilePath, List.of());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void extract(LlmClient client, ConversationManager conv) {`（源码行 137-206）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void extract(LlmClient client, ConversationManager conv) {
        List<Message> messages = conv.getMessages();
        if (messages.size() < 4) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            sb.append('[').append(msg.getRole()).append("]: ")
              .append(msg.getContent()).append('\n');
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static Map<String, String> parseTypedSections(String text) {`（源码行 213-239）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static Map<String, String> parseTypedSections(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        String currentType = null;
        StringBuilder buf = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("### ")) {
                if (currentType != null) {
                    String body = buf.toString().trim();
                    if (!body.isEmpty()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void injectMemories(ConversationManager conv) {`（源码行 243-258）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void injectMemories(ConversationManager conv) {
        List<String> memories = getMemories();
        if (memories.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder("## Auto Memory\n\n");
        for (String mem : memories) {
            sb.append(mem).append("\n\n");
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String loadInstructions(String workDir) {`（源码行 262-275）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String loadInstructions(String workDir) {
        List<Path> candidates = List.of(
                Path.of(workDir, "JimiCode.md"),
                Path.of(workDir, ".JimiCode", "INSTRUCTIONS.md")
        );
        for (Path p : candidates) {
            try {
                return Files.readString(p);
            } catch (IOException ignored) {
                // try next
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/memory/MemoryRecall.java`

**类型与边界。** 包 `com.jimicode.memory`；类型 `MemoryRecall, RelevantMemory, SelectorFn`；231 行；直接依赖 `com.fasterxml.jackson.databind.JsonNode, com.fasterxml.jackson.databind.ObjectMapper, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private MemoryRecall() {}`（源码行 24-24）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private MemoryRecall() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `or API documentation for those tools (JimiCode is already exercising them). DO still select \`（源码行 41-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            or API documentation for those tools (JimiCode is already exercising them). DO still select \
            memories containing warnings, gotchas, or known issues about those tools — active use is exactly \
            when those matter.

            Respond with valid JSON only, no markdown, in this exact shape: \
            {"selected_memories": ["filename1.md", "filename2.md"]}""";
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record RelevantMemory(String path, long mtimeMs) {}`（源码行 55-55）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record RelevantMemory(String path, long mtimeMs) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `String select(String systemPrompt, String userMessage) throws Exception;`（源码行 67-91）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        String select(String systemPrompt, String userMessage) throws Exception;
    }

    // ── Main entry point ───────────────────────────────────────────────

    /**
     * Scans both directories, filters already-surfaced paths, asks the
     * selector to pick up to 5 relevant filenames, and returns the
     * corresponding absolute paths + mtimes.
     *
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (selector == null) return List.of();`（源码行 93-98）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (selector == null) return List.of();

        List<MemoryScanner.MemoryHeader> all = new ArrayList<>();
        if (userMemDir != null) {
            all.addAll(MemoryScanner.scanMemoryFiles(userMemDir, "user"));
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (projectMemDir != null) {`（源码行 99-101）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (projectMemDir != null) {
            all.addAll(MemoryScanner.scanMemoryFiles(projectMemDir, "project"));
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (var m : all) {`（源码行 106-110）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (var m : all) {
            if (!surfaced.contains(m.filePath())) {
                candidates.add(m);
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (candidates.isEmpty()) return List.of();`（源码行 111-121）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (candidates.isEmpty()) return List.of();

        List<String> selectedFilenames = selectRelevantMemories(
                query, candidates, recentTools, selector);

        // Build a lookup from both filePath and filename to header.
        Map<String, MemoryScanner.MemoryHeader> byKey = new HashMap<>();
        for (var m : candidates) {
            byKey.put(m.filePath(), m);
            byKey.putIfAbsent(m.filename(), m);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (String fn : selectedFilenames) {`（源码行 124-129）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (String fn : selectedFilenames) {
            var m = byKey.get(fn);
            if (m != null) {
                result.add(new RelevantMemory(m.filePath(), m.mtimeMs()));
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (var m : memories) {`（源码行 142-144）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (var m : memories) {
            validFilenames.add(m.filename());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (recentTools != null && !recentTools.isEmpty()) {`（源码行 149-151）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (recentTools != null && !recentTools.isEmpty()) {
            toolsSection = "\n\nRecently used tools: " + String.join(", ", recentTools);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (clean.isEmpty()) return List.of();`（源码行 163-180）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (clean.isEmpty()) return List.of();

        try {
            JsonNode root = MAPPER.readTree(clean);
            JsonNode arr = root.get("selected_memories");
            if (arr == null || !arr.isArray()) return List.of();

            List<String> out = new ArrayList<>();
            for (JsonNode node : arr) {
                String f = node.asText();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String extractJsonObject(String raw) {`（源码行 188-231）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String extractJsonObject(String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim();
        if (trimmed.startsWith("{")) return trimmed;
        int start = trimmed.indexOf('{');
        if (start < 0) return "";
        int end = trimmed.lastIndexOf('}');
        if (end < start) return "";
        return trimmed.substring(start, end + 1);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/memory/MemoryScanner.java`

**类型与边界。** 包 `com.jimicode.memory`；类型 `MemoryScanner, MemoryHeader, Frontmatter`；211 行；直接依赖 `java.io.BufferedReader, java.io.IOException, java.nio.file.*, java.time.Instant, java.time.ZoneOffset, java.time.format.DateTimeFormatter, java.util.*, java.util.regex.Matcher, java.util.regex.Pattern, java.util.stream.Stream`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private MemoryScanner() {}`（源码行 26-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private MemoryScanner() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static List<MemoryHeader> scanMemoryFiles(Path memoryDir, String scope) {`（源码行 66-96）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static List<MemoryHeader> scanMemoryFiles(Path memoryDir, String scope) {
        if (memoryDir == null || !Files.isDirectory(memoryDir)) {
            return List.of();
        }
        List<Path> mdFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(memoryDir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return name.endsWith(".md") && !name.equals(ENTRYPOINT_NAME);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static MemoryHeader readMemoryHeader(Path filePath, Path memoryDir, String scope) {`（源码行 100-130）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static MemoryHeader readMemoryHeader(Path filePath, Path memoryDir, String scope) {
        long mtimeMs;
        try {
            mtimeMs = Files.getLastModifiedTime(filePath).toMillis();
        } catch (IOException e) {
            return null;
        }

        // Read first FRONTMATTER_MAX_LINES for frontmatter parsing.
        StringBuilder sb = new StringBuilder();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `record Frontmatter(String name, String description, String type) {}`（源码行 134-134）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    record Frontmatter(String name, String description, String type) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static Frontmatter parseFrontmatter(String content) {`（源码行 141-169）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static Frontmatter parseFrontmatter(String content) {
        Matcher m = FRONTMATTER_RE.matcher(content);
        if (!m.find()) {
            return new Frontmatter("", "", "");
        }
        String block = m.group(1);
        String name = "";
        String description = "";
        String type = "";
        for (String line : block.split("\n")) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean isValidType(String raw) {`（源码行 174-176）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean isValidType(String raw) {
        return VALID_TYPES.contains(raw);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String formatMemoryManifest(List<MemoryHeader> memories) {`（源码行 188-210）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String formatMemoryManifest(List<MemoryHeader> memories) {
        if (memories.isEmpty()) return "";
        var sb = new StringBuilder();
        for (int i = 0; i < memories.size(); i++) {
            if (i > 0) sb.append('\n');
            var m = memories.get(i);
            String scope = (m.scope() != null && !m.scope().isEmpty())
                    ? "[" + m.scope() + "-scope] " : "";
            String tag = (m.type() != null && !m.type().isEmpty())
                    ? "[" + m.type() + "] " : "";
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/permission/PermissionChecker.java`

**类型与边界。** 包 `com.jimicode.permission`；类型 `PermissionChecker, PermissionRule, RuleEffect, CheckResult`；351 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, org.yaml.snakeyaml.Yaml, java.io.IOException, java.nio.file.FileSystems, java.nio.file.Files, java.nio.file.Path, java.nio.file.PathMatcher, java.util.ArrayList, java.util.Collections`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private record PermissionRule(String toolName, String pattern, RuleEffect effect) {`（源码行 38-52）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private record PermissionRule(String toolName, String pattern, RuleEffect effect) {
        boolean matches(String toolName, String content) {
            if (!this.toolName.equals(toolName)) {
                return false;
            }
            // Use glob matching for the pattern against the content
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            try {
                return matcher.matches(Path.of(content));
            } catch (Exception e) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public PermissionChecker(PermissionMode mode, Path projectRoot) {`（源码行 96-100）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public PermissionChecker(PermissionMode mode, Path projectRoot) {
        this.mode = mode;
        this.projectRoot = projectRoot;
        this.fileRules = new ArrayList<>(loadRules());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public PermissionMode getMode() { return mode; }`（源码行 102-102）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public PermissionMode getMode() { return mode; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMode(PermissionMode mode) { this.mode = mode; }`（源码行 103-103）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setMode(PermissionMode mode) { this.mode = mode; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setPlanFilePath(String path) { this.planFilePath = path; }`（源码行 104-104）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setPlanFilePath(String path) { this.planFilePath = path; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record CheckResult(PermissionMode.Decision decision, String reason) {`（源码行 106-111）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record CheckResult(PermissionMode.Decision decision, String reason) {
        public static CheckResult allow() { return new CheckResult(PermissionMode.Decision.ALLOW, ""); }

        public static CheckResult deny(String reason) { return new CheckResult(PermissionMode.Decision.DENY, reason); }
        public static CheckResult ask() { return new CheckResult(PermissionMode.Decision.ASK, ""); }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public CheckResult check(Tool tool, Map<String, Object> args) {`（源码行 113-176）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public CheckResult check(Tool tool, Map<String, Object> args) {
        String toolName = tool.name();
        String content = extractContent(toolName, args);

        // Layer 0: Plan mode exceptions
        if (mode == PermissionMode.PLAN) {
            if (PLAN_MODE_ALLOWED_TOOLS.contains(toolName)) {
                return CheckResult.allow();
            }
            if ("WriteFile".equals(toolName) || "EditFile".equals(toolName)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void addAllowAlwaysRule(String toolName, String content) {`（源码行 178-180）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void addAllowAlwaysRule(String toolName, String content) {
        allowAlwaysRules.add(toolName + ":" + content);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private List<PermissionRule> loadRules() {`（源码行 194-213）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private List<PermissionRule> loadRules() {
        var rules = new ArrayList<PermissionRule>();

        // User-level rules
        Path userHome = Path.of(System.getProperty("user.home"));
        Path userFile = userHome.resolve(".JimiCode").resolve("permissions.yaml");
        rules.addAll(loadRulesFile(userFile));

        // Project-level rules
        if (projectRoot != null) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void appendLocalRule(String toolName, String pattern) {`（源码行 215-234）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void appendLocalRule(String toolName, String pattern) {
        if (projectRoot == null) return;
        Path localFile = projectRoot.resolve(".JimiCode").resolve("permissions.local.yaml");
        try {
            Files.createDirectories(localFile.getParent());
            var rules = new ArrayList<>(loadRulesFile(localFile));
            rules.add(new PermissionRule(toolName, pattern, RuleEffect.ALLOW));

            var entries = new ArrayList<Map<String, String>>();
            for (var r : rules) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private List<PermissionRule> loadRulesFile(Path path) {`（源码行 246-297）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private List<PermissionRule> loadRulesFile(Path path) {
        if (!Files.exists(path)) {
            return List.of();
        }

        String content;
        try {
            content = Files.readString(path);
        } catch (IOException e) {
            return List.of();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private boolean isSafeCommand(String command) {`（源码行 299-311）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private boolean isSafeCommand(String command) {
        String trimmed = command.trim();
        if (trimmed.contains("|") || trimmed.contains(";") || trimmed.contains("&&")
                || trimmed.contains(">") || trimmed.contains("$(") || trimmed.contains("`")) {
            return false;
        }
        for (var safe : SAFE_COMMANDS) {
            if (trimmed.equals(safe) || trimmed.startsWith(safe + " ")) {
                return true;
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private boolean isPathTool(String toolName) {`（源码行 313-315）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private boolean isPathTool(String toolName) {
        return "ReadFile".equals(toolName) || "WriteFile".equals(toolName) || "EditFile".equals(toolName);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private boolean isPathAllowed(String pathStr) {`（源码行 317-326）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private boolean isPathAllowed(String pathStr) {
        try {
            Path p = Path.of(pathStr).toAbsolutePath().normalize();
            Path root = projectRoot.toAbsolutePath().normalize();
            Path tmp = Path.of("/tmp").toAbsolutePath().normalize();
            return p.startsWith(root) || p.startsWith(tmp);
        } catch (Exception e) {
            return true;
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String extractContent(String toolName, Map<String, Object> args) {`（源码行 328-333）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String extractContent(String toolName, Map<String, Object> args) {
        String field = CONTENT_FIELDS.get(toolName);
        if (field == null) return null;
        var v = args.get(field);
        return v instanceof String s ? s : null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 335-338）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String describeToolAction(String toolName, Map<String, Object> args) {`（源码行 340-350）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String describeToolAction(String toolName, Map<String, Object> args) {
        return switch (toolName) {
            case "Bash" -> "Execute: " + stringArg(args, "command", "");
            case "ReadFile" -> "Read: " + stringArg(args, "file_path", "");
            case "WriteFile" -> "Write: " + stringArg(args, "file_path", "");
            case "EditFile" -> "Edit: " + stringArg(args, "file_path", "");
            case "Glob" -> "Glob: " + stringArg(args, "pattern", "");
            case "Grep" -> "Grep: " + stringArg(args, "pattern", "");
            default -> toolName;
        };
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/permission/PermissionMode.java`

**类型与边界。** 包 `com.jimicode.permission`；类型 `PermissionMode, Decision`；50 行；直接依赖 `com.jimicode.tool.ToolCategory, java.util.Locale`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public static PermissionMode fromConfig(String value) {`（源码行 20-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static PermissionMode fromConfig(String value) {
        if (value == null || value.isBlank()) return DEFAULT;

        return switch (value.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "")) {
            case "default" -> DEFAULT;
            case "acceptedits" -> ACCEPT_EDITS;
            case "plan" -> PLAN;
            case "bypass", "bypasspermissions" -> BYPASS;
            default -> throw new IllegalArgumentException("Unknown permission mode: " + value);
        };
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Decision decide(ToolCategory category) {`（源码行 32-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Decision decide(ToolCategory category) {
        return switch (this) {
            case DEFAULT -> switch (category) {
                case READ -> Decision.ALLOW;
                case WRITE, COMMAND -> Decision.ASK;
            };
            case ACCEPT_EDITS -> switch (category) {
                case READ, WRITE -> Decision.ALLOW;
                case COMMAND -> Decision.ASK;
            };
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/permission/PermissionResponse.java`

**类型与边界。** 包 `com.jimicode.permission`；类型 `PermissionResponse`；13 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- 未识别到常规方法签名；重点检查 record 组件、重写方法、lambda、静态初始化块和注解处理器生成代码。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/plan/PlanFile.java`

**类型与边界。** 包 `com.jimicode.plan`；类型 `PlanFile`；128 行；直接依赖 `java.io.IOException, java.nio.file.*, java.time.LocalDateTime, java.time.format.DateTimeFormatter`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public static String generateSlug() {`（源码行 45-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String generateSlug() {
        long nanos = System.nanoTime();
        int ai = (int) ((nanos / 1000) % ADJECTIVES.length);
        int ni = (int) ((nanos / 100) % NOUNS.length);
        if (ai < 0) ai += ADJECTIVES.length;
        if (ni < 0) ni += NOUNS.length;
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMdd-HHmm"));
        return ADJECTIVES[ai] + "-" + NOUNS[ni] + "-" + timestamp;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String getOrCreatePlanPath(String workDir) {`（源码行 58-71）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String getOrCreatePlanPath(String workDir) {
        if (currentPlanPath != null) {
            return currentPlanPath;
        }
        Path dir = Path.of(workDir, PLANS_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // best effort
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String getPlanFilePath(String workDir) {`（源码行 73-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String getPlanFilePath(String workDir) {
        if (currentPlanPath != null) {
            return currentPlanPath;
        }
        return getOrCreatePlanPath(workDir);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void setPlanFilePath(String path) {`（源码行 80-82）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void setPlanFilePath(String path) {
        currentPlanPath = path;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void resetPlanPath() {`（源码行 84-86）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void resetPlanPath() {
        currentPlanPath = null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean planExists() {`（源码行 90-92）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean planExists() {
        return currentPlanPath != null && Files.exists(Path.of(currentPlanPath));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String loadPlan() throws IOException {`（源码行 94-103）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String loadPlan() throws IOException {
        if (currentPlanPath == null) {
            return "";
        }
        Path path = Path.of(currentPlanPath);
        if (!Files.exists(path)) {
            return "";
        }
        return Files.readString(path);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void savePlan(String workDir, String content) throws IOException {`（源码行 105-110）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void savePlan(String workDir, String content) throws IOException {
        String path = getOrCreatePlanPath(workDir);
        Path target = Path.of(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean isPlanFilePath(String targetPath, String planPath) {`（源码行 120-127）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean isPlanFilePath(String targetPath, String planPath) {
        if (planPath == null || planPath.isBlank()) {
            return false;
        }
        String cleanTarget = Path.of(targetPath).normalize().toString();
        String cleanPlan = Path.of(planPath).normalize().toString();
        return cleanTarget.equals(cleanPlan) || cleanTarget.endsWith(cleanPlan);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/prompt/PlanModePrompt.java`

**类型与边界。** 包 `com.jimicode.prompt`；类型 `PlanModePrompt`；182 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `MUST NOT make any edits (with the exception of the plan file mentioned below), run any \`（源码行 18-137）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            MUST NOT make any edits (with the exception of the plan file mentioned below), run any \
            non-readonly tools (including changing configs or making commits), or otherwise make any \
            changes to the system. This supercedes any other instructions you have received.

            ## Plan File Info:
            %s
            You should build your plan incrementally by writing to or editing this file. NOTE that this \
            is the only file you are allowed to edit - other than this you are only allowed to take \
            READ-ONLY actions.

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String buildReminder(String planPath, boolean planExists, int iteration) {`（源码行 147-167）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String buildReminder(String planPath, boolean planExists, int iteration) {
        String planFileInfo = "Plan file: " + planPath;
        if (planExists) {
            planFileInfo += "\nA plan file already exists at " + planPath
                    + ". You can read it and make incremental edits using the EditFile tool.";
        } else {
            planFileInfo += "\nNo plan file exists yet. You should create your plan at " + planPath
                    + " using the WriteFile tool.";
        }

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String buildReentryReminder(String planPath) {`（源码行 170-172）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String buildReentryReminder(String planPath) {
        return String.format(PLAN_MODE_REENTRY_REMINDER, planPath);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String buildExitReminder(String planPath, boolean planExists) {`（源码行 175-181）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String buildExitReminder(String planPath, boolean planExists) {
        String extra = "";
        if (planExists) {
            extra = " The plan file is located at " + planPath + " if you need to reference it.";
        }
        return String.format(PLAN_MODE_EXIT_REMINDER, extra);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/prompt/PromptBuilder.java`

**类型与边界。** 包 `com.jimicode.prompt`；类型 `PromptBuilder, Section, EnvironmentContext, BuildOptions`；129 行；直接依赖 `java.io.BufferedReader, java.io.InputStreamReader, java.time.LocalDate, java.util.ArrayList, java.util.Comparator, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record Section(String name, int priority, String content) {}`（源码行 22-22）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Section(String name, int priority, String content) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public PromptBuilder add(Section section) {`（源码行 41-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public PromptBuilder add(Section section) {
        sections.add(section);
        return this;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String build() {`（源码行 46-57）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String build() {
        sections.sort(Comparator.comparingInt(Section::priority));

        var parts = new ArrayList<String>();
        for (Section s : sections) {
            String content = s.content() == null ? "" : s.content().strip();
            if (!content.isEmpty()) {
                parts.add(content);
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static EnvironmentContext detectEnvironment(String model) {`（源码行 62-108）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static EnvironmentContext detectEnvironment(String model) {
        String workDir = System.getProperty("user.dir");
        String osName = System.getProperty("os.name", "unknown").toLowerCase();
        String arch = System.getProperty("os.arch", "unknown");
        String shell = System.getenv("SHELL");
        if (shell == null || shell.isEmpty()) {
            shell = "bash";
        }

        boolean isGitRepo = false;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String buildSystemPrompt(EnvironmentContext env, BuildOptions options) {`（源码行 111-128）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String buildSystemPrompt(EnvironmentContext env, BuildOptions options) {
        var builder = new PromptBuilder();

        builder.add(PromptSections.identitySection());
        builder.add(PromptSections.systemSection());
        builder.add(PromptSections.doingTasksSection());
        builder.add(PromptSections.executingActionsSection());
        builder.add(PromptSections.usingToolsSection());
        builder.add(PromptSections.toneStyleSection());
        builder.add(PromptSections.outputEfficiencySection());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/prompt/PromptSections.java`

**类型与边界。** 包 `com.jimicode.prompt`；类型 `PromptSections`；227 行；直接依赖 `com.jimicode.prompt.PromptBuilder.EnvironmentContext, com.jimicode.prompt.PromptBuilder.Section`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private PromptSections() {}`（源码行 16-16）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private PromptSections() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section identitySection() {`（源码行 31-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section identitySection() {
        return new Section("Identity", 0, IDENTITY_CONTENT);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section systemSection() {`（源码行 52-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section systemSection() {
        return new Section("System", 10, SYSTEM_CONTENT);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section doingTasksSection() {`（源码行 97-99）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section doingTasksSection() {
        return new Section("DoingTasks", 20, DOING_TASKS_CONTENT);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section executingActionsSection() {`（源码行 123-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section executingActionsSection() {
        return new Section("ExecutingActions", 30, EXECUTING_ACTIONS_CONTENT);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section usingToolsSection() {`（源码行 164-166）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section usingToolsSection() {
        return new Section("UsingTools", 40, USING_TOOLS_CONTENT);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section toneStyleSection() {`（源码行 179-181）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section toneStyleSection() {
        return new Section("ToneStyle", 50, TONE_STYLE_CONTENT);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section outputEfficiencySection() {`（源码行 205-207）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section outputEfficiencySection() {
        return new Section("TextOutput", 60, OUTPUT_EFFICIENCY_CONTENT);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Section environmentSection(EnvironmentContext env) {`（源码行 211-226）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Section environmentSection(EnvironmentContext env) {
        var sb = new StringBuilder();
        sb.append("# Environment\n");
        sb.append(" - Working directory: ").append(env.workDir()).append('\n');
        sb.append(" - Platform: ").append(env.os()).append('/').append(env.arch()).append('\n');
        sb.append(" - Shell: ").append(env.shell()).append('\n');
        sb.append(" - Is git repo: ").append(env.isGitRepo());
        if (env.isGitRepo() && env.gitBranch() != null && !env.gitBranch().isEmpty()) {
            sb.append('\n').append(" - Git branch: ").append(env.gitBranch());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/session/SessionManager.java`

**类型与边界。** 包 `com.jimicode.session`；类型 `SessionManager, as, kinds, SessionMessage, KeepMessage, CompactBoundary, BoundaryScan, SessionInfo, so`；343 行；直接依赖 `com.fasterxml.jackson.core.JsonProcessingException, com.fasterxml.jackson.databind.ObjectMapper, com.jimicode.conversation.ConversationManager, java.io.BufferedReader, java.io.IOException, java.nio.file.*, java.time.*, java.time.format.DateTimeFormatter, java.util.*, java.util.stream.Stream`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record SessionMessage(String role, String type, String content, long timestamp) {`（源码行 38-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record SessionMessage(String role, String type, String content, long timestamp) {
        /** Convenience constructor for plain (non-boundary) messages. */
        public SessionMessage(String role, String content, long timestamp) {
            this(role, null, content, timestamp);
        }

        public boolean isCompactBoundary() {
            return TYPE_COMPACT_BOUNDARY.equals(type);
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record KeepMessage(String role, String content) {}`（源码行 54-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record KeepMessage(String role, String content) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record CompactBoundary(String summary, List<KeepMessage> keep) {}`（源码行 63-63）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record CompactBoundary(String summary, List<KeepMessage> keep) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record BoundaryScan(CompactBoundary boundary, List<SessionMessage> after, boolean found) {}`（源码行 66-66）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record BoundaryScan(CompactBoundary boundary, List<SessionMessage> after, boolean found) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Path sessionsDir(String workDir) {`（源码行 73-75）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Path sessionsDir(String workDir) {
        return Path.of(workDir, ".JimiCode", "sessions");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String newId() {`（源码行 79-81）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String newId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void saveMessage(String workDir, String sessionId, String role, String content) {`（源码行 85-87）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void saveMessage(String workDir, String sessionId, String role, String content) {
        saveRecord(workDir, sessionId, role, null, content);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (workDir == null || workDir.isBlank() || sessionId == null || sessionId.isBlank()) {`（源码行 100-102）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (workDir == null || workDir.isBlank() || sessionId == null || sessionId.isBlank()) {
            return;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `new CompactBoundary(summary, keep == null ? List.of() : keep));`（源码行 105-107）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                    new CompactBoundary(summary, keep == null ? List.of() : keep));
            saveRecord(workDir, sessionId, "system", TYPE_COMPACT_BOUNDARY, blob);
        } catch (JsonProcessingException ignored) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (type != null && !type.isEmpty()) {`（源码行 123-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (type != null && !type.isEmpty()) {
                line.put("type", type);
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static List<SessionMessage> loadSession(String workDir, String sessionId) {`（源码行 135-163）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static List<SessionMessage> loadSession(String workDir, String sessionId) {
        Path file = sessionsDir(workDir).resolve(sessionId + ".jsonl");
        if (!Files.exists(file)) {
            return List.of();
        }
        List<SessionMessage> messages = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static BoundaryScan findLastCompactBoundary(List<SessionMessage> messages) {`（源码行 174-199）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static BoundaryScan findLastCompactBoundary(List<SessionMessage> messages) {
        int last = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).isCompactBoundary()) {
                last = i;
            }
        }
        if (last < 0) {
            return new BoundaryScan(null, List.of(), false);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static ConversationManager rebuildConversation(List<SessionMessage> messages) {`（源码行 210-229）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static ConversationManager rebuildConversation(List<SessionMessage> messages) {
        BoundaryScan scan = findLastCompactBoundary(messages);
        if (!scan.found()) {
            return replay(messages);
        }
        List<SessionMessage> replay = new ArrayList<>();
        // Summary becomes the leading user message with the same Chinese framing
        // as autoCompact, so the model sees a consistent context header on resume.
        String resumeSummary = "本次会话延续自之前的对话，因上下文空间不足进行了压缩。以下是早期对话的摘要：\n\n"
                + scan.boundary().summary();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static ConversationManager replay(List<SessionMessage> messages) {`（源码行 231-241）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static ConversationManager replay(List<SessionMessage> messages) {
        ConversationManager conversation = new ConversationManager();
        for (SessionMessage msg : messages) {
            if (msg.isCompactBoundary()) continue; // never replay the raw boundary blob
            switch (msg.role()) {
                case "assistant" -> conversation.addAssistantMessage(msg.content());
                default -> conversation.addUserMessage(msg.content());
            }
        }
        return conversation;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static List<SessionInfo> listSessions(String workDir) {`（源码行 245-278）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static List<SessionInfo> listSessions(String workDir) {
        Path baseDir = sessionsDir(workDir);
        if (!Files.isDirectory(baseDir)) {
            return List.of();
        }
        String branch = currentGitBranch(workDir);
        List<SessionInfo> sessions = new ArrayList<>();
        try (Stream<Path> paths = Files.list(baseDir)) {
            paths.filter(p -> p.toString().endsWith(".jsonl"))
                 .filter(Files::isRegularFile)
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String currentGitBranch(String workDir) {`（源码行 282-293）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String currentGitBranch(String workDir) {
        try {
            Process proc = new ProcessBuilder("git", "-C", workDir, "rev-parse", "--abbrev-ref", "HEAD")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(proc.getInputStream().readAllBytes()).trim();
            int code = proc.waitFor();
            return code == 0 ? output : "";
        } catch (IOException | InterruptedException e) {
            return "";
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String formatRelativeTime(Instant t) {`（源码行 297-317）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String formatRelativeTime(Instant t) {
        Duration d = Duration.between(t, Instant.now());
        long seconds = d.getSeconds();
        if (seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String formatFileSize(long bytes) {`（源码行 319-331）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            double kb = bytes / 1024.0;
            return kb == (long) kb
                    ? String.format("%.0fKB", kb)
                    : String.format("%.1fKB", kb);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean matchesSearch(SessionInfo s, String query) {`（源码行 335-342）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean matchesSearch(SessionInfo s, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase();
        return s.firstMessage().toLowerCase().contains(q)
                || s.id().toLowerCase().contains(q);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/skill/SkillCatalog.java`

**类型与边界。** 包 `com.jimicode.skill`；类型 `SkillCatalog, SkillMeta, Skill`；325 行；直接依赖 `org.yaml.snakeyaml.Yaml, java.io.IOException, java.nio.file.*, java.util.*, java.util.stream.Stream`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record Skill(SkillMeta meta, String promptBody, Path sourceDir, boolean bodyLoaded) {`（源码行 40-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Skill(SkillMeta meta, String promptBody, Path sourceDir, boolean bodyLoaded) {
        public Skill withBody(String newBody) {
            return new Skill(meta, newBody, sourceDir, true);
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void register(Skill skill, String source) {`（源码行 55-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void register(Skill skill, String source) {
        skills.put(skill.meta().name(), skill);
        sources.put(skill.meta().name(), source);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void register(Skill skill) {`（源码行 60-62）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void register(Skill skill) {
        register(skill, "");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Skill> getSkills() {`（源码行 64-66）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Skill> getSkills() {
        return Collections.unmodifiableMap(skills);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Optional<Skill> get(String name) {`（源码行 68-70）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Optional<Skill> get(String name) {
        return Optional.ofNullable(skills.get(name));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Optional<Skill> getFull(String name) {`（源码行 77-95）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Optional<Skill> getFull(String name) {
        Skill skill = skills.get(name);
        if (skill == null) {
            return Optional.empty();
        }
        if (skill.sourceDir() == null) {
            return Optional.of(skill);
        }
        try {
            Skill reloaded = loadSkill(skill.sourceDir());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<SkillMeta> list() {`（源码行 97-99）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<SkillMeta> list() {
        return skills.values().stream().map(Skill::meta).toList();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String source(String name) {`（源码行 101-103）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String source(String name) {
        return sources.getOrDefault(name, "");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static SkillCatalog loadCatalog(String workDir) {`（源码行 113-129）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static SkillCatalog loadCatalog(String workDir) {
        SkillCatalog c = new SkillCatalog();
        c.workDir = workDir;

        // Tier 1: builtins (placeholder — loaded by Agent layer)

        // Tier 2: user global
        String home = System.getProperty("user.home");
        if (home != null) {
            c.loadTier(Path.of(home, ".JimiCode", "skills"), "user");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void reload(String workDir) {`（源码行 131-138）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void reload(String workDir) {
        SkillCatalog fresh = loadCatalog(workDir);
        this.skills.clear();
        this.skills.putAll(fresh.skills);
        this.sources.clear();
        this.sources.putAll(fresh.sources);
        this.workDir = fresh.workDir;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void loadFromDirectory(Path dir) {`（源码行 144-146）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void loadFromDirectory(Path dir) {
        loadTier(dir, dir.toString());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void loadTier(Path dir, String source) {`（源码行 148-164）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void loadTier(Path dir, String source) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(Files::isDirectory).forEach(skillDir -> {
                try {
                    Skill skill = loadSkill(skillDir);
                    if (skill != null) {
                        register(skill, source);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String buildActiveContext(Set<String> activeSkillNames) {`（源码行 172-186）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String buildActiveContext(Set<String> activeSkillNames) {
        if (activeSkillNames == null || activeSkillNames.isEmpty()) {
            return "";
        }
        var sb = new StringBuilder();
        sb.append("## Active Skills\n\n");
        for (var name : activeSkillNames) {
            var skill = skills.get(name);
            if (skill != null) {
                sb.append("### ").append(name).append("\n");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Skill loadSkill(Path dir) throws IOException {`（源码行 190-205）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Skill loadSkill(Path dir) throws IOException {
        // Strategy 1: skill.yaml + prompt.md
        Path metaPath = dir.resolve("skill.yaml");
        if (Files.isRegularFile(metaPath)) {
            return loadFromYamlAndPrompt(dir, metaPath);
        }

        // Strategy 2: SKILL.md with optional YAML front-matter
        Path mdPath = dir.resolve("SKILL.md");
        if (Files.isRegularFile(mdPath)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Skill loadFromYamlAndPrompt(Path dir, Path metaPath) throws IOException {`（源码行 208-225）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Skill loadFromYamlAndPrompt(Path dir, Path metaPath) throws IOException {
        String yamlText = Files.readString(metaPath);
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlText);
        if (map == null) {
            map = Map.of();
        }

        SkillMeta meta = metaFromMap(map, dir);

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Skill parseSkillMD(Path dir, String content) {`（源码行 228-268）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Skill parseSkillMD(Path dir, String content) {
        String body = content;
        Map<String, Object> frontMatter = Map.of();

        String trimmed = content.stripLeading();
        if (trimmed.startsWith("---")) {
            int firstSep = content.indexOf("---");
            int secondSep = content.indexOf("---", firstSep + 3);
            if (secondSep >= 0) {
                String yamlBlock = content.substring(firstSep + 3, secondSep);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static SkillMeta metaFromMap(Map<String, Object> map, Path dir) {`（源码行 271-319）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static SkillMeta metaFromMap(Map<String, Object> map, Path dir) {
        String name = stringVal(map, "name");
        if (name == null || name.isBlank()) {
            name = dir.getFileName().toString().toLowerCase().replace(' ', '-');
        }
        String description = stringVal(map, "description");
        String whenToUse = stringVal(map, "when_to_use");

        List<String> tags = List.of();
        Object rawTags = map.get("tags");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringVal(Map<String, Object> map, String key) {`（源码行 321-324）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringVal(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/skill/SkillExecutor.java`

**类型与边界。** 包 `com.jimicode.skill`；类型 `SkillExecutor`；95 行；直接依赖 `com.jimicode.conversation.Message, com.jimicode.skill.SkillCatalog.Skill, com.jimicode.skill.SkillCatalog.SkillMeta, com.jimicode.tool.Tool, com.jimicode.tool.ToolRegistry, java.util.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private SkillExecutor() {}`（源码行 23-23）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private SkillExecutor() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String executeInline(Skill skill, String args, SkillHost host) {`（源码行 29-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String executeInline(Skill skill, String args, SkillHost host) {
        assertAllowedToolsExist(skill, host.toolRegistry());
        String body = substituteArguments(skill.promptBody(), args);
        host.activateSkill(skill.meta().name(), body);
        host.recordSkillInvocation(skill.meta().name(), body);

        if (skill.meta().allowedTools() != null && !skill.meta().allowedTools().isEmpty()) {
            Set<String> allowed = new HashSet<>(skill.meta().allowedTools());
            host.setToolFilter(allowed::contains);
        } else {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String executeFork(Skill skill, String args, SkillForkHost host) {`（源码行 48-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String executeFork(Skill skill, String args, SkillForkHost host) {
        assertAllowedToolsExist(skill, host.toolRegistry());
        String body = substituteArguments(skill.promptBody(), args);
        host.recordSkillInvocation(skill.meta().name(), skill.promptBody());
        List<Message> seed = buildForkSeed(skill.meta().forkContext(), host.snapshotParentMessages());
        return host.runSubAgent(body, seed, skill.meta().allowedTools(), skill.meta().model());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String substituteArguments(String body, String args) {`（源码行 56-64）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String substituteArguments(String body, String args) {
        if (args == null || args.isBlank()) {
            return body;
        }
        if (body.contains("$ARGUMENTS")) {
            return body.replace("$ARGUMENTS", args);
        }
        return body + "\n\n## User Request\n\n" + args;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static List<Message> buildForkSeed(String mode, List<Message> parent) {`（源码行 66-80）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static List<Message> buildForkSeed(String mode, List<Message> parent) {
        if (parent == null || parent.isEmpty()) {
            return List.of();
        }
        return switch (mode != null ? mode : "none") {
            case "full" -> new ArrayList<>(parent);
            case "recent" -> {
                if (parent.size() <= FORK_RECENT_COUNT) {
                    yield new ArrayList<>(parent);
                }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void assertAllowedToolsExist(Skill skill, ToolRegistry registry) {`（源码行 82-94）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void assertAllowedToolsExist(Skill skill, ToolRegistry registry) {
        List<String> allowed = skill.meta().allowedTools();
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        for (String name : allowed) {
            if (registry.get(name) == null) {
                throw new IllegalStateException(
                        "Skill '" + skill.meta().name() + "' declares allowed tool '"
                                + name + "' which is not registered");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/skill/SkillForkHost.java`

**类型与边界。** 包 `com.jimicode.skill`；类型 `SkillForkHost`；24 行；直接依赖 `com.jimicode.conversation.Message, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `String runSubAgent(String body, List<Message> seed, List<String> allowedTools, String model);`（源码行 20-25）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    String runSubAgent(String body, List<Message> seed, List<String> allowedTools, String model);

    List<Message> snapshotParentMessages();
}

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/skill/SkillHost.java`

**类型与边界。** 包 `com.jimicode.skill`；类型 `so, SkillHost`；33 行；直接依赖 `com.jimicode.tool.ToolRegistry, java.util.function.Predicate`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `void activateSkill(String name, String body);`（源码行 20-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    void activateSkill(String name, String body);

    void setToolFilter(Predicate<String> filter);

    ToolRegistry toolRegistry();

    /**
     * Record that this skill ran, so its SOP body can be re-attached after
     * a Layer 2 compaction wipes the transcript. Default no-op for hosts
     * that don't track recovery state.
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/subagent/AgentLoader.java`

**类型与边界。** 包 `com.jimicode.subagent`；类型 `AgentLoader`；176 行；直接依赖 `org.yaml.snakeyaml.Yaml, java.io.IOException, java.nio.file.DirectoryStream, java.nio.file.Files, java.nio.file.Path, java.util.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private AgentLoader() {}`（源码行 36-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private AgentLoader() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Map<String, SubAgentSpec> loadAll(Path projectRoot) {`（源码行 44-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Map<String, SubAgentSpec> loadAll(Path projectRoot) {
        var loader = new AgentLoader();
        loader.loadBuiltins();

        String home = System.getProperty("user.home");
        if (home != null && !home.isEmpty()) {
            loader.loadDir(Path.of(home, ".JimiCode", "agents"));
        }

        if (projectRoot != null) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static List<String> listNames(Map<String, SubAgentSpec> agents) {`（源码行 63-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static List<String> listNames(Map<String, SubAgentSpec> agents) {
        var names = new ArrayList<>(agents.keySet());
        Collections.sort(names);
        return names;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void loadBuiltins() {`（源码行 69-73）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void loadBuiltins() {
        agents.put(SubAgentSpec.GENERAL_PURPOSE.name(), SubAgentSpec.GENERAL_PURPOSE);
        agents.put(SubAgentSpec.PLAN.name(), SubAgentSpec.PLAN);
        agents.put(SubAgentSpec.EXPLORE.name(), SubAgentSpec.EXPLORE);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void loadDir(Path dir) {`（源码行 75-94）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void loadDir(Path dir) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.md")) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    continue;
                }
                try {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static SubAgentSpec parseAgentFile(Path path) throws IOException {`（源码行 100-155）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static SubAgentSpec parseAgentFile(Path path) throws IOException {
        String content = Files.readString(path);
        String trimmed = content.strip();

        String yamlBlock = null;
        String body = trimmed;

        if (trimmed.startsWith("---")) {
            // Split on the second "---" delimiter
            int firstEnd = trimmed.indexOf("---", 3);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String getString(Map<String, Object> map, String key) {`（源码行 157-160）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static List<String> getStringList(Map<String, Object> map, String key) {`（源码行 163-175）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static List<String> getStringList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof List<?> list) {
            var result = new ArrayList<String>();
            for (Object item : list) {
                if (item instanceof String s) {
                    result.add(s);
                }
            }
            return List.copyOf(result);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/subagent/AgentTool.java`

**类型与边界。** 包 `com.jimicode.subagent`；类型 `AgentTool`；560 行；直接依赖 `com.jimicode.agent.Agent, com.jimicode.agent.AgentEvent, com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, com.jimicode.llm.LlmClient, com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolRegistry, com.jimicode.tool.ToolResult, java.util.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `Rules (non-negotiable):`（源码行 68-82）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            Rules (non-negotiable):
            1. Do NOT fork again.
            2. Do NOT converse, ask questions, or request confirmation.
            3. Use tools directly: read files, search code, make changes.
            4. Stay strictly within your assigned task scope.
            5. Final report must be under 500 characters, starting with "Scope:".
            </fork_boilerplate>""";

    public AgentTool(LlmClient client, ToolRegistry parentRegistry, String protocol,
                     ProviderConfig providerConfig) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setModelResolver(Function<String, LlmClient> modelResolver) {`（源码行 84-86）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setModelResolver(Function<String, LlmClient> modelResolver) {
        this.modelResolver = modelResolver;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setAgentSpecs(Map<String, SubAgentSpec> agentSpecs) {`（源码行 88-90）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setAgentSpecs(Map<String, SubAgentSpec> agentSpecs) {
        this.agentSpecs = agentSpecs;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setProgressListener(Consumer<SubAgentProgress> progressListener) {`（源码行 92-94）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setProgressListener(Consumer<SubAgentProgress> progressListener) {
        this.progressListener = progressListener;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setTaskManager(SubAgentTaskManager taskManager) {`（源码行 96-98）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setTaskManager(SubAgentTaskManager taskManager) {
        this.taskManager = taskManager;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public SubAgentTaskManager getTaskManager() {`（源码行 100-102）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public SubAgentTaskManager getTaskManager() {
        return taskManager;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setParentConversation(ConversationManager parentConversation) {`（源码行 104-106）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setParentConversation(ConversationManager parentConversation) {
        this.parentConversation = parentConversation;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setParentReplacementState(com.jimicode.toolresult.ContentReplacementState state) {`（源码行 117-119）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setParentReplacementState(com.jimicode.toolresult.ContentReplacementState state) {
        this.parentReplacementState = state;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setWorktreeManager(com.jimicode.worktree.WorktreeManager worktreeManager) {`（源码行 121-123）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setWorktreeManager(com.jimicode.worktree.WorktreeManager worktreeManager) {
        this.worktreeManager = worktreeManager;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setTeamManager(com.jimicode.teams.TeamManager teamManager) {`（源码行 125-127）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setTeamManager(com.jimicode.teams.TeamManager teamManager) {
        this.teamManager = teamManager;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 132-134）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "Agent";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 137-160）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        var sb = new StringBuilder();
        sb.append("Launch a sub-agent to handle a complex task. Each agent runs independently ");
        sb.append("with its own context.\n\n");
        sb.append("Use this when a task benefits from focused, isolated work -- e.g., ");
        sb.append("researching a question, implementing a component, or reviewing code. ");
        sb.append("The sub-agent cannot see the current conversation.\n\n");
        sb.append("Available agent types:");

        if (agentSpecs != null && !agentSpecs.isEmpty()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 163-165）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.COMMAND;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 168-223）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        List<String> agentTypes;
        if (agentSpecs != null && !agentSpecs.isEmpty()) {
            agentTypes = AgentLoader.listNames(agentSpecs);
        } else {
            agentTypes = List.of("general-purpose", "plan", "explore");
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("description", Map.of(
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldDefer() {`（源码行 226-228）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean shouldDefer() {
        return false;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 231-272）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String description = getStringArg(args, "description");
        String prompt = getStringArg(args, "prompt");
        if (description == null || description.isEmpty() || prompt == null || prompt.isEmpty()) {
            return ToolResult.error("Error: description and prompt are required");
        }

        String subagentType = getStringArg(args, "subagent_type");
        String modelOverride = getStringArg(args, "model");
        String isolation = getStringArg(args, "isolation");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private ToolResult runAsync(SubAgentSpec spec, String description, String prompt, String modelOverride) {`（源码行 276-285）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ToolResult runAsync(SubAgentSpec spec, String description, String prompt, String modelOverride) {
        if (taskManager == null) {
            return ToolResult.error("Background execution not available (no task manager configured)");
        }
        LlmClient subClient = selectClient(spec.model(), modelOverride);
        String taskId = taskManager.spawnSubAgent(subClient, parentRegistry, protocol, providerConfig, spec, prompt);
        return ToolResult.success(
                "Agent \"%s\" launched in background (task %s). You will be notified when it completes."
                        .formatted(description, taskId));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private ToolResult runFork(String description, String prompt, String modelOverride) {`（源码行 287-316）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ToolResult runFork(String description, String prompt, String modelOverride) {
        if (parentConversation == null) {
            return ToolResult.error("Error: fork requires parent conversation context");
        }
        if (taskManager == null) {
            return ToolResult.error("Error: fork requires task manager for background execution");
        }

        // Check for nested fork
        for (var msg : parentConversation.getMessages()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static ConversationManager buildForkedConversation(ConversationManager parent, String task) {`（源码行 318-342）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static ConversationManager buildForkedConversation(ConversationManager parent, String task) {
        ConversationManager forked = new ConversationManager();
        for (var msg : parent.getMessages()) {
            if (msg.getToolUses() != null && !msg.getToolUses().isEmpty()
                    && (msg.getToolResults() == null || msg.getToolResults().isEmpty())) {
                // Assistant message with pending tool_use - patch with placeholder results
                forked.addAssistantFull(msg.getContent(), msg.getThinkingBlocks(), msg.getToolUses());
                var placeholders = msg.getToolUses().stream()
                        .map(tu -> new com.jimicode.conversation.ToolResultBlock(
                                tu.toolUseId(), "(tool execution interrupted by fork)", false))
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private ToolResult runSync(SubAgentSpec spec, String description, String prompt, String modelOverride, String isolation) {`（源码行 344-447）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ToolResult runSync(SubAgentSpec spec, String description, String prompt, String modelOverride, String isolation) {
        ToolRegistry subRegistry = ToolFilter.filterForAgent(parentRegistry, spec);
        LlmClient subClient = selectClient(spec.model(), modelOverride);

        Agent subAgent = new Agent(subClient, subRegistry, protocol, providerConfig);
        int maxTurns = spec.maxTurns() > 0 ? spec.maxTurns() : 200;
        subAgent.setMaxIterations(maxTurns);

        // Worktree isolation via AgentWorktree API
        com.jimicode.worktree.AgentWorktree.Result wtResult = null;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private SubAgentSpec resolveSpec(String name) {`（源码行 449-459）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private SubAgentSpec resolveSpec(String name) {
        if (agentSpecs != null) {
            return agentSpecs.get(name);
        }
        return switch (name) {
            case "general-purpose" -> SubAgentSpec.GENERAL_PURPOSE;
            case "plan" -> SubAgentSpec.PLAN;
            case "explore" -> SubAgentSpec.EXPLORE;
            default -> null;
        };
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (team == null) {`（源码行 464-466）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (team == null) {
            return ToolResult.error("Error: team '%s' not found. Create it first with TeamCreate.".formatted(teamName));
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (memberName.length() > 30) memberName = memberName.substring(0, 30);`（源码行 470-475）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (memberName.length() > 30) memberName = memberName.substring(0, 30);
        int suffix = 2;
        String base = memberName;
        while (team.hasMember(memberName)) {
            memberName = base + "-" + suffix++;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if ("worktree".equals(isolation) && worktreeManager != null) {`（源码行 492-506）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if ("worktree".equals(isolation) && worktreeManager != null) {
            try {
                byte[] rndBytes = new byte[4];
                new java.security.SecureRandom().nextBytes(rndBytes);
                String slug = "agent-a" + java.util.HexFormat.of().formatHex(rndBytes).substring(0, 7);
                var wtResult = com.jimicode.worktree.AgentWorktree.create(
                        slug, worktreeManager.getProjectRoot(), worktreeManager.getSymlinkDirs());
                workdir = wtResult.worktreePath();
                String notice = com.jimicode.worktree.AgentWorktree.buildNotice(
                        System.getProperty("user.dir"), wtResult.worktreePath());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private LlmClient selectClient(String specModel, String overrideModel) {`（源码行 523-535）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private LlmClient selectClient(String specModel, String overrideModel) {
        String model = (overrideModel != null && !overrideModel.isEmpty()) ? overrideModel : specModel;
        if (model == null || model.isEmpty() || "inherit".equals(model)) {
            return client;
        }
        if (modelResolver != null) {
            LlmClient resolved = modelResolver.apply(model);
            if (resolved != null) {
                return resolved;
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `emitProgress(description, agentType, null, null, isError, done, toolCount, totalTime);`（源码行 539-544）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        emitProgress(description, agentType, null, null, isError, done, toolCount, totalTime);
    }

    private void emitProgress(String description, String agentType,
                              String toolName, String toolOutput,
                              boolean isError, boolean done, int toolCount, double totalTime) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (progressListener != null) {`（源码行 545-549）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (progressListener != null) {
            progressListener.accept(new SubAgentProgress(
                    agentType, description, toolName, toolOutput,
                    isError, done, toolCount, totalTime));
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static double elapsedSeconds(long startNanos) {`（源码行 552-554）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static double elapsedSeconds(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000_000.0;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String getStringArg(Map<String, Object> args, String key) {`（源码行 556-559）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String getStringArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v instanceof String s ? s : null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/subagent/SubAgentProgress.java`

**类型与边界。** 包 `com.jimicode.subagent`；类型 `SubAgentProgress`；31 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- 未识别到常规方法签名；重点检查 record 组件、重写方法、lambda、静态初始化块和注解处理器生成代码。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/subagent/SubAgentSpec.java`

**类型与边界。** 包 `com.jimicode.subagent`；类型 `SubAgentSpec`；91 行；直接依赖 `java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- 未识别到常规方法签名；重点检查 record 组件、重写方法、lambda、静态初始化块和注解处理器生成代码。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/subagent/SubAgentTaskManager.java`

**类型与边界。** 包 `com.jimicode.subagent`；类型 `SubAgentTaskManager, TaskStatus, Task, TaskNotification, TaskEntry`；197 行；直接依赖 `com.jimicode.agent.AgentEvent, java.util.ArrayList, java.util.LinkedHashMap, java.util.List, java.util.Map, java.util.concurrent.BlockingQueue, java.util.concurrent.TimeUnit, java.util.concurrent.atomic.AtomicInteger`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record Task(String id, String name, TaskStatus status, String output, String error) {}`（源码行 26-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Task(String id, String name, TaskStatus status, String output, String error) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record TaskNotification(String taskId, String name, TaskStatus status, String output) {}`（源码行 28-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record TaskNotification(String taskId, String name, TaskStatus status, String output) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `TaskEntry(String id, String name) {`（源码行 43-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        TaskEntry(String id, String name) {
            this.id = id;
            this.name = name;
            this.status = TaskStatus.PENDING;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized String createTask(String name) {`（源码行 50-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized String createTask(String name) {
        String id = "task_" + nextId.incrementAndGet();
        tasks.put(id, new TaskEntry(id, name));
        return id;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void setRunning(String id, Thread thread) {`（源码行 56-62）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void setRunning(String id, Thread thread) {
        TaskEntry t = tasks.get(id);
        if (t != null) {
            t.status = TaskStatus.RUNNING;
            t.thread = thread;
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void setCompleted(String id, String output) {`（源码行 64-71）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void setCompleted(String id, String output) {
        TaskEntry t = tasks.get(id);
        if (t != null) {
            t.status = TaskStatus.COMPLETED;
            t.output = output;
            notifications.add(new TaskNotification(id, t.name, TaskStatus.COMPLETED, output));
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void setFailed(String id, String errMsg) {`（源码行 73-80）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void setFailed(String id, String errMsg) {
        TaskEntry t = tasks.get(id);
        if (t != null) {
            t.status = TaskStatus.FAILED;
            t.error = errMsg;
            notifications.add(new TaskNotification(id, t.name, TaskStatus.FAILED, errMsg));
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void cancelTask(String id) {`（源码行 82-91）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void cancelTask(String id) {
        TaskEntry t = tasks.get(id);
        if (t != null && t.status == TaskStatus.RUNNING) {
            t.status = TaskStatus.CANCELLED;
            if (t.thread != null) {
                t.thread.interrupt();
            }
            notifications.add(new TaskNotification(id, t.name, TaskStatus.CANCELLED, ""));
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<TaskNotification> drainNotifications() {`（源码行 93-97）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized List<TaskNotification> drainNotifications() {
        var result = new ArrayList<>(notifications);
        notifications.clear();
        return result;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized Task getTask(String id) {`（源码行 99-103）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized Task getTask(String id) {
        TaskEntry t = tasks.get(id);
        if (t == null) return null;
        return new Task(t.id, t.name, t.status, t.output, t.error);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<Task> listTasks() {`（源码行 105-109）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized List<Task> listTasks() {
        return tasks.values().stream()
                .map(t -> new Task(t.id, t.name, t.status, t.output, t.error))
                .toList();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `return spawnSubAgent(client, registry, protocol, cfg, spec, prompt, null);`（源码行 122-139）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        return spawnSubAgent(client, registry, protocol, cfg, spec, prompt, null);
    }

    /**
     * Spawn variant that lets the caller pre-seed the child agent's
     * tool-result decision log with a clone of the parent's state. Used by
     * the fork path so parent and child share a byte-identical prompt-cache
     * prefix on tool_use_ids that exist in their shared history.
     */
    public String spawnSubAgent(
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (parentState != null) {`（源码行 147-149）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (parentState != null) {
                subAgent.setReplacementState(parentState.copy());
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (spec.systemPromptOverride() != null && !spec.systemPromptOverride().isEmpty()) {`（源码行 152-154）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (spec.systemPromptOverride() != null && !spec.systemPromptOverride().isEmpty()) {
                conv.addSystemReminder(spec.systemPromptOverride());
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `while (!Thread.currentThread().isInterrupted()) {`（源码行 160-187）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            while (!Thread.currentThread().isInterrupted()) {
                AgentEvent event;
                try {
                    event = queue.poll(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    setFailed(taskId, "Interrupted");
                    return;
                }
                if (event == null) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `setRunning(taskId, thread);`（源码行 190-194）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        setRunning(taskId, thread);
        return taskId;
    }

    private static String truncate(String s, int n) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/subagent/ToolFilter.java`

**类型与边界。** 包 `com.jimicode.subagent`；类型 `ToolFilter`；143 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolRegistry, java.util.HashSet, java.util.Set`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private ToolFilter() {}`（源码行 59-59）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ToolFilter() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static ToolRegistry filterForAgent(ToolRegistry source, SubAgentSpec spec) {`（源码行 65-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static ToolRegistry filterForAgent(ToolRegistry source, SubAgentSpec spec) {
        return filterForAgent(source, spec, false, false, false);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (Tool tool : source.listTools()) {`（源码行 92-136）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (Tool tool : source.listTools()) {
            String name = tool.name();

            // Layer 1: MCP tools always pass through
            if (isMcpTool(name)) {
                filtered.register(tool);
                continue;
            }

            // Layer 2: Globally blocked tools
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean isMcpTool(String name) {`（源码行 140-142）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean isMcpTool(String name) {
        return name.startsWith("mcp__");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/task/TaskList.java`

**类型与边界。** 包 `com.jimicode.task`；类型 `TaskList, Status, Task, UpdateResult`；341 行；直接依赖 `com.fasterxml.jackson.core.type.TypeReference, com.fasterxml.jackson.databind.ObjectMapper, com.fasterxml.jackson.databind.SerializationFeature, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.security.SecureRandom, java.util.ArrayList, java.util.LinkedHashMap, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `PENDING("pending"),`（源码行 33-41）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        PENDING("pending"),
        IN_PROGRESS("in_progress"),
        COMPLETED("completed");

        private final String value;

        Status(String value) {
            this.value = value;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String value() {`（源码行 43-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String value() {
            return value;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Status fromString(String s) {`（源码行 47-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public static Status fromString(String s) {
            for (Status st : values()) {
                if (st.value.equals(s)) {
                    return st;
                }
            }
            throw new IllegalArgumentException("unknown status: " + s);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Task() {}`（源码行 70-70）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Task() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getId() { return id; }`（源码行 72-72）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getId() { return id; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setId(String id) { this.id = id; }`（源码行 73-73）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setId(String id) { this.id = id; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getSubject() { return subject; }`（源码行 75-75）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getSubject() { return subject; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setSubject(String subject) { this.subject = subject; }`（源码行 76-76）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setSubject(String subject) { this.subject = subject; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getDescription() { return description; }`（源码行 78-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getDescription() { return description; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setDescription(String description) { this.description = description; }`（源码行 79-79）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setDescription(String description) { this.description = description; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getActiveForm() { return activeForm; }`（源码行 81-81）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getActiveForm() { return activeForm; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setActiveForm(String activeForm) { this.activeForm = activeForm; }`（源码行 82-82）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setActiveForm(String activeForm) { this.activeForm = activeForm; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getStatus() { return status; }`（源码行 84-84）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getStatus() { return status; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setStatus(String status) { this.status = status; }`（源码行 85-85）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setStatus(String status) { this.status = status; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getOwner() { return owner; }`（源码行 87-87）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getOwner() { return owner; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setOwner(String owner) { this.owner = owner; }`（源码行 88-88）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setOwner(String owner) { this.owner = owner; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> getBlocks() { return blocks; }`（源码行 90-90）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public List<String> getBlocks() { return blocks; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setBlocks(List<String> blocks) { this.blocks = blocks; }`（源码行 91-91）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setBlocks(List<String> blocks) { this.blocks = blocks; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> getBlockedBy() { return blockedBy; }`（源码行 93-93）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public List<String> getBlockedBy() { return blockedBy; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setBlockedBy(List<String> blockedBy) { this.blockedBy = blockedBy; }`（源码行 94-94）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setBlockedBy(List<String> blockedBy) { this.blockedBy = blockedBy; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> getMetadata() { return metadata; }`（源码行 96-96）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> getMetadata() { return metadata; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }`（源码行 97-97）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TaskList(String listId, String workDir) {`（源码行 107-110）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public TaskList(String listId, String workDir) {
        this.listId = listId;
        this.storePath = Path.of(workDir, ".JimiCode", "tasks", listId + ".json");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getListId() {`（源码行 112-114）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getListId() {
        return listId;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `save(tasks);`（源码行 136-143）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        save(tasks);
        return task;
    }

    /**
     * Returns a task by its ID, or empty if not found.
     */
    public synchronized Optional<Task> get(String id) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `return load().stream()`（源码行 144-152）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        return load().stream()
                .filter(t -> t.id.equals(id))
                .findFirst();
    }

    /**
     * Lists all visible (non-internal) tasks.
     */
    public synchronized List<Task> list() {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (Task t : load()) {`（源码行 154-159）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (Task t : load()) {
            if (t.metadata != null && t.metadata.containsKey("_internal")) {
                continue;
            }
            visible.add(t);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized Optional<UpdateResult> update(String id, Map<String, Object> updates) {`（源码行 174-278）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized Optional<UpdateResult> update(String id, Map<String, Object> updates) {
        List<Task> tasks = load();

        Task target = null;
        for (Task t : tasks) {
            if (t.id.equals(id)) {
                target = t;
                break;
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record UpdateResult(Task task, List<String> changed) {}`（源码行 283-283）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record UpdateResult(Task task, List<String> changed) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private List<Task> load() {`（源码行 287-301）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private List<Task> load() {
        try {
            if (!Files.exists(storePath)) {
                return new ArrayList<>();
            }
            byte[] data = Files.readAllBytes(storePath);
            if (data.length == 0) {
                return new ArrayList<>();
            }
            return MAPPER.readValue(data, new TypeReference<List<Task>>() {});
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void save(List<Task> tasks) {`（源码行 303-310）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void save(List<Task> tasks) {
        try {
            Files.createDirectories(storePath.getParent());
            MAPPER.writeValue(storePath.toFile(), tasks);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save task list to " + storePath, e);
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String generateId() {`（源码行 314-322）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String generateId() {
        byte[] bytes = new byte[4];
        RNG.nextBytes(bytes);
        var sb = new StringBuilder("t");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String asString(Object v) {`（源码行 324-326）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String asString(Object v) {
        return v instanceof String s ? s : null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static List<String> toStringList(Object v) {`（源码行 329-340）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static List<String> toStringList(Object v) {
        if (v instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s) {
                    result.add(s);
                }
            }
            return result;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/task/TaskTools.java`

**类型与边界。** 包 `com.jimicode.task`；类型 `TaskTools, TaskCreateTool, TaskGetTool, TaskListTool, TaskUpdateTool`；385 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, java.util.ArrayList, java.util.LinkedHashMap, java.util.LinkedHashSet, java.util.List, java.util.Map, java.util.Optional, java.util.StringJoiner`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private TaskTools() {} // utility class`（源码行 28-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private TaskTools() {} // utility class
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TaskCreateTool(TaskList taskList) {`（源码行 38-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public TaskCreateTool(TaskList taskList) {
            this.taskList = taskList;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 43-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String name() {
            return "TaskCreate";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 48-51）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String description() {
            return "Create a new task to track work. Use this to break complex work "
                    + "into smaller, trackable steps before starting implementation.";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 54-56）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolCategory category() {
            return ToolCategory.COMMAND;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldDefer() {`（源码行 59-61）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public boolean shouldDefer() {
            return true;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 64-84）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> schema() {
            return Map.of(
                    "name", name(),
                    "description", description(),
                    "input_schema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "subject", Map.of("type", "string",
                                            "description", "A brief title for the task"),
                                    "description", Map.of("type", "string",
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 88-108）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolResult execute(Map<String, Object> args) {
            String subject = asString(args, "subject");
            String desc = asString(args, "description");
            if (subject == null || subject.isEmpty() || desc == null || desc.isEmpty()) {
                return ToolResult.error("Error: subject and description are required");
            }

            String activeForm = asString(args, "activeForm");
            Map<String, Object> metadata = null;
            Object raw = args.get("metadata");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TaskGetTool(TaskList taskList) {`（源码行 119-121）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public TaskGetTool(TaskList taskList) {
            this.taskList = taskList;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 124-126）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String name() {
            return "TaskGet";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 129-131）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String description() {
            return "Get the details of a specific task by its ID.";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 134-136）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolCategory category() {
            return ToolCategory.COMMAND;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldDefer() {`（源码行 139-141）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public boolean shouldDefer() {
            return true;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 144-157）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> schema() {
            return Map.of(
                    "name", name(),
                    "description", description(),
                    "input_schema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "taskId", Map.of("type", "string",
                                            "description", "The ID of the task to retrieve")
                            ),
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 160-187）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolResult execute(Map<String, Object> args) {
            String taskId = asString(args, "taskId");
            if (taskId == null || taskId.isEmpty()) {
                return ToolResult.error("Error: taskId is required");
            }

            Optional<TaskList.Task> opt = taskList.get(taskId);
            if (opt.isEmpty()) {
                return ToolResult.success("Task #" + taskId + " not found");
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TaskListTool(TaskList taskList) {`（源码行 198-200）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public TaskListTool(TaskList taskList) {
            this.taskList = taskList;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 203-205）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String name() {
            return "TaskList";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 208-211）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String description() {
            return "List all tasks in the current task list. Shows ID, status, subject, "
                    + "and blocking info.";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 214-216）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolCategory category() {
            return ToolCategory.COMMAND;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldDefer() {`（源码行 219-221）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public boolean shouldDefer() {
            return true;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 224-233）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> schema() {
            return Map.of(
                    "name", name(),
                    "description", description(),
                    "input_schema", Map.of(
                            "type", "object",
                            "properties", Map.of()
                    )
            );
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 236-277）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolResult execute(Map<String, Object> args) {
            List<TaskList.Task> tasks = taskList.list();
            if (tasks.isEmpty()) {
                return ToolResult.success("No tasks found.");
            }

            // Build a set of completed task IDs so we can filter active blockers
            var completedIds = new LinkedHashSet<String>();
            for (TaskList.Task task : tasks) {
                if (TaskList.Status.COMPLETED.value().equals(task.getStatus())) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TaskUpdateTool(TaskList taskList) {`（源码行 288-290）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public TaskUpdateTool(TaskList taskList) {
            this.taskList = taskList;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 293-295）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String name() {
            return "TaskUpdate";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 298-302）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String description() {
            return "Update a task's status, subject, description, or dependencies. "
                    + "Set status to \"in_progress\" when starting work, \"completed\" when done. "
                    + "Set status to \"deleted\" to remove a task.";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 305-307）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolCategory category() {
            return ToolCategory.COMMAND;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldDefer() {`（源码行 310-312）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public boolean shouldDefer() {
            return true;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 315-325）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> schema() {
            return Map.of(
                    "name", name(),
                    "description", description(),
                    "input_schema", Map.of(
                            "type", "object",
                            "properties", orderedProps(),
                            "required", List.of("taskId")
                    )
            );
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 328-345）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolResult execute(Map<String, Object> args) {
            String taskId = asString(args, "taskId");
            if (taskId == null || taskId.isEmpty()) {
                return ToolResult.error("Error: taskId is required");
            }

            Optional<TaskList.UpdateResult> result = taskList.update(taskId, args);
            if (result.isEmpty()) {
                return ToolResult.error("Error: task #" + taskId + " not found");
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Map<String, Object> orderedProps() {`（源码行 352-376）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        private static Map<String, Object> orderedProps() {
            var props = new LinkedHashMap<String, Object>();
            props.put("taskId", Map.of("type", "string",
                    "description", "The ID of the task to update"));
            props.put("subject", Map.of("type", "string",
                    "description", "New subject for the task"));
            props.put("description", Map.of("type", "string",
                    "description", "New description for the task"));
            props.put("activeForm", Map.of("type", "string",
                    "description", "Present continuous form shown in spinner when in_progress"));
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String asString(Map<String, Object> args, String key) {`（源码行 381-384）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String asString(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v instanceof String s ? s : null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/AgentNameRegistry.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `AgentNameRegistry`；41 行；直接依赖 `java.util.LinkedHashMap, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private AgentNameRegistry() {}`（源码行 20-20）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private AgentNameRegistry() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static AgentNameRegistry getInstance() { return INSTANCE; }`（源码行 22-22）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static AgentNameRegistry getInstance() { return INSTANCE; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void register(String name, String agentId) {`（源码行 24-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void register(String name, String agentId) {
        nameToId.put(name, agentId);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized String resolve(String nameOrId) {`（源码行 28-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized String resolve(String nameOrId) {
        if (nameToId.containsKey(nameOrId)) return nameToId.get(nameOrId);
        if (nameToId.containsValue(nameOrId)) return nameOrId;
        return null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void unregister(String name) {`（源码行 34-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void unregister(String name) {
        nameToId.remove(name);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized Map<String, String> listAll() {`（源码行 38-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized Map<String, String> listAll() {
        return new LinkedHashMap<>(nameToId);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/Coordinator.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `Coordinator`；43 行；直接依赖 `java.util.Set`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private Coordinator() {}`（源码行 23-23）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Coordinator() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean isCoordinatorTool(String name) {`（源码行 40-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean isCoordinatorTool(String name) {
        return ALLOWED_TOOLS.contains(name);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/FileMailBox.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `FileMailBox, MailMessage, MutationFn`；137 行；直接依赖 `com.fasterxml.jackson.core.type.TypeReference, com.fasterxml.jackson.databind.ObjectMapper, java.io.IOException, java.nio.file.*, java.time.Instant, java.time.format.DateTimeFormatter, java.util.ArrayList, java.util.List, java.util.concurrent.ThreadLocalRandom`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public MailMessage(String from, String text) {`（源码行 23-25）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public MailMessage(String from, String text) {
            this(from, text, DateTimeFormatter.ISO_INSTANT.format(Instant.now()), false, "", "");
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public FileMailBox(Path baseDir) {`（源码行 35-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public FileMailBox(Path baseDir) {
        this.baseDir = baseDir;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException ignored) {}
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Path inboxPath(String agentId) {`（源码行 42-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Path inboxPath(String agentId) {
        return baseDir.resolve(agentId + ".json");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Path lockPath(String agentId) {`（源码行 46-48）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Path lockPath(String agentId) {
        return baseDir.resolve(agentId + ".json.lock");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void send(String recipient, MailMessage msg) {`（源码行 50-56）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void send(String recipient, MailMessage msg) {
        withLock(recipient, messages -> {
            var m = new MailMessage(msg.from(), msg.text(), msg.timestamp(), false, msg.color(), msg.summary());
            messages.add(m);
            return messages;
        });
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<MailMessage> readUnread(String agentId) {`（源码行 58-65）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<MailMessage> readUnread(String agentId) {
        List<MailMessage> messages = readInbox(agentId);
        List<MailMessage> unread = new ArrayList<>();
        for (var m : messages) {
            if (!m.read()) unread.add(m);
        }
        return unread;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void markAllRead(String agentId) {`（源码行 67-75）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void markAllRead(String agentId) {
        withLock(agentId, messages -> {
            List<MailMessage> updated = new ArrayList<>();
            for (var m : messages) {
                updated.add(new MailMessage(m.from(), m.text(), m.timestamp(), true, m.color(), m.summary()));
            }
            return updated;
        });
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `List<MailMessage> apply(List<MailMessage> messages);`（源码行 78-81）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        List<MailMessage> apply(List<MailMessage> messages);
    }

    private void withLock(String agentId, MutationFn fn) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (Instant.now().minusSeconds(10).isAfter(modTime)) {`（源码行 94-96）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                    if (Instant.now().minusSeconds(10).isAfter(modTime)) {
                        Files.deleteIfExists(lock);
                    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!acquired) return;`（源码行 108-116）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (!acquired) return;

        try {
            List<MailMessage> messages = readInbox(agentId);
            messages = fn.apply(messages);
            writeInbox(agentId, messages);
        } finally {
            try { Files.deleteIfExists(lock); } catch (IOException ignored) {}
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private List<MailMessage> readInbox(String agentId) {`（源码行 119-128）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private List<MailMessage> readInbox(String agentId) {
        Path path = inboxPath(agentId);
        if (!Files.exists(path)) return new ArrayList<>();
        try {
            byte[] data = Files.readAllBytes(path);
            return MAPPER.readValue(data, new TypeReference<List<MailMessage>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void writeInbox(String agentId, List<MailMessage> messages) {`（源码行 130-136）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void writeInbox(String agentId, List<MailMessage> messages) {
        Path path = inboxPath(agentId);
        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(messages);
            Files.writeString(path, json);
        } catch (IOException ignored) {}
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/ITermBackend.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `ITermBackend`；66 行；直接依赖 `java.util.concurrent.TimeUnit, java.util.logging.Logger`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private ITermBackend() {}`（源码行 18-18）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ITermBackend() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String spawnITermTeammate(String teamName, String memberName, String cliCommand) throws Exception {`（源码行 20-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String spawnITermTeammate(String teamName, String memberName, String cliCommand) throws Exception {
        String tabName = teamName + "-" + memberName;
        String escapedCmd = cliCommand.replace("\"", "\\\"");
        String script = """
                tell application "iTerm2"
                    tell current window
                        create tab with profile "Default"
                        tell current session of current tab
                            set name to "%s"
                            write text "%s"
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void stopITermTeammate(String tabName) {`（源码行 46-65）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void stopITermTeammate(String tabName) {
        try {
            String script = """
                    tell application "iTerm2"
                        repeat with w in windows
                            repeat with t in tabs of w
                                if name of current session of t is "%s" then
                                    close t
                                    return
                                end if
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/SharedTaskStore.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `SharedTaskStore, SharedTask`；109 行；直接依赖 `com.fasterxml.jackson.annotation.JsonIgnoreProperties, com.fasterxml.jackson.core.type.TypeReference, com.fasterxml.jackson.databind.ObjectMapper, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.ArrayList, java.util.List, java.util.concurrent.atomic.AtomicInteger`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public SharedTask withStatus(String s) {`（源码行 30-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public SharedTask withStatus(String s) {
            return new SharedTask(id, title, description, s, assignee, blocks, blockedBy, createdBy);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public SharedTask withAssignee(String a) {`（源码行 34-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public SharedTask withAssignee(String a) {
            return new SharedTask(id, title, description, status, a, blocks, blockedBy, createdBy);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public SharedTaskStore(Path teamDir) {`（源码行 45-48）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public SharedTaskStore(Path teamDir) {
        this.filePath = teamDir.resolve("tasks.json");
        load();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized SharedTask create(String title, String description, String createdBy) {`（源码行 50-56）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized SharedTask create(String title, String description, String createdBy) {
        int id = nextId.getAndIncrement();
        var task = new SharedTask(id, title, description, "todo", "", List.of(), List.of(), createdBy);
        tasks.add(task);
        save();
        return task;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized SharedTask get(int id) {`（源码行 58-60）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized SharedTask get(int id) {
        return tasks.stream().filter(t -> t.id() == id).findFirst().orElse(null);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<SharedTask> listTasks(String status, String assignee) {`（源码行 62-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized List<SharedTask> listTasks(String status, String assignee) {
        return tasks.stream()
                .filter(t -> status == null || status.isEmpty() || t.status().equals(status))
                .filter(t -> assignee == null || assignee.isEmpty() || t.assignee().equals(assignee))
                .toList();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (tasks.get(i).id() == id) {`（源码行 72-88）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (tasks.get(i).id() == id) {
                var old = tasks.get(i);
                String newStatus = (status != null && !status.isEmpty()) ? status : old.status();
                String newAssignee = (assignee != null) ? assignee : old.assignee();

                var newBlocks = new ArrayList<>(old.blocks());
                if (addBlocks != null) newBlocks.addAll(addBlocks);

                var newBlockedBy = new ArrayList<>(old.blockedBy());
                if (addBlockedBy != null) newBlockedBy.addAll(addBlockedBy);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void load() {`（源码行 93-101）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void load() {
        if (!Files.exists(filePath)) return;
        try {
            List<SharedTask> loaded = MAPPER.readValue(filePath.toFile(), new TypeReference<>() {});
            tasks.addAll(loaded);
            int maxId = tasks.stream().mapToInt(SharedTask::id).max().orElse(0);
            nextId.set(maxId + 1);
        } catch (IOException ignored) {}
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void save() {`（源码行 103-108）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void save() {
        try {
            Files.createDirectories(filePath.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), tasks);
        } catch (IOException ignored) {}
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/SpawnDispatcher.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `SpawnDispatcher, SpawnConfig, SpawnResult`；94 行；直接依赖 `com.jimicode.llm.LlmClient, com.jimicode.tool.ToolRegistry, java.nio.file.Path, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private SpawnDispatcher() {}`（源码行 36-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private SpawnDispatcher() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static SpawnResult spawnTeammate(SpawnConfig config) throws Exception {`（源码行 38-66）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static SpawnResult spawnTeammate(SpawnConfig config) throws Exception {
        var team = config.team();
        var mode = team.getMode();

        switch (mode) {
            case IN_PROCESS -> {
                var member = team.addMember(config.memberName(), config.client(),
                        config.registry(), config.protocol(), config.providerConfig());
                if (config.workdir() != null) {
                    member.agent.setWorkDir(config.workdir());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String buildTeammateCLI(String teamName, String memberName, String workdir) {`（源码行 72-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String buildTeammateCLI(String teamName, String memberName, String workdir) {
        String wd = workdir != null ? workdir : System.getProperty("user.dir");
        // Find JimiCode executable (assume it's the current JAR or on PATH)
        String JimiCode = ProcessHandle.current().info().command().orElse("JimiCode");
        return "cd %s && %s --teammate --team-name %s --agent-name %s".formatted(
                shellQuote(wd), shellQuote(JimiCode), shellQuote(teamName), shellQuote(memberName));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static String shellQuote(String s) {`（源码行 80-83）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static String shellQuote(String s) {
        if (s.matches("[a-zA-Z0-9_./-]+")) return s;
        return "'" + s.replace("'", "'\\''") + "'";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void recordExternalMember(TeamManager.Team team, String name, String paneId) {`（源码行 85-93）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void recordExternalMember(TeamManager.Team team, String name, String paneId) {
        // For external backends, create a placeholder member
        var member = new TeamManager.Member(name, null, null);
        member.active = true;
        // Store paneId via field access (simple approach)
        synchronized (team) {
            team.members.put(name, member);
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/TeamManager.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `TeamManager, TeamMode, Team, Member`；179 行；直接依赖 `com.jimicode.agent.Agent, com.jimicode.agent.AgentEvent, com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, com.jimicode.llm.LlmClient, com.jimicode.tool.ToolRegistry, java.nio.file.Path, java.util.ArrayList, java.util.LinkedHashMap, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public synchronized Team createTeam(String name, TeamMode mode) {`（源码行 31-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized Team createTeam(String name, TeamMode mode) {
        Team team = new Team(name, mode);
        teams.put(name, team);
        return team;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized Team getTeam(String name) {`（源码行 37-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized Team getTeam(String name) {
        return teams.get(name);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void deleteTeam(String name) {`（源码行 41-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void deleteTeam(String name) {
        Team team = teams.remove(name);
        if (team != null) {
            team.stopAll();
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<String> listTeams() {`（源码行 48-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized List<String> listTeams() {
        return new ArrayList<>(teams.keySet());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void closeAll() {`（源码行 52-57）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void closeAll() {
        for (Team team : teams.values()) {
            team.stopAll();
        }
        teams.clear();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<TeammateProgress> getAllTeammateProgress() {`（源码行 59-63）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized List<TeammateProgress> getAllTeammateProgress() {
        return teams.values().stream()
                .flatMap(t -> t.getTeammateProgressList().stream())
                .toList();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static TeamMode detectBackend() {`（源码行 65-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static TeamMode detectBackend() {
        return TeamMode.IN_PROCESS;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static TeamMode detectPaneBackend() {`（源码行 69-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static TeamMode detectPaneBackend() {
        if (System.getenv("TMUX") != null && !System.getenv("TMUX").isEmpty()) {
            return TeamMode.TMUX;
        }
        try {
            Process p = new ProcessBuilder("which", "tmux").start();
            if (p.waitFor() == 0) return TeamMode.TMUX;
        } catch (Exception ignored) {}
        return TeamMode.IN_PROCESS;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Path teamsBaseDir() {`（源码行 82-84）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Path teamsBaseDir() {
        return Path.of(System.getProperty("user.dir"), ".JimiCode", "teams");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Team(String name, TeamMode mode) {`（源码行 92-96）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Team(String name, TeamMode mode) {
            this.name = name;
            this.mode = mode;
            this.mailBox = new FileMailBox(teamsBaseDir().resolve(name).resolve("inboxes"));
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getName() { return name; }`（源码行 98-98）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getName() { return name; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TeamMode getMode() { return mode; }`（源码行 99-99）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public TeamMode getMode() { return mode; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public FileMailBox getMailBox() { return mailBox; }`（源码行 101-101）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public FileMailBox getMailBox() { return mailBox; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized BlockingQueue<AgentEvent> startMember(String name, String task) {`（源码行 111-118）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public synchronized BlockingQueue<AgentEvent> startMember(String name, String task) {
            Member member = members.get(name);
            if (member == null) return null;
            member.conv.addUserMessage(task);
            BlockingQueue<AgentEvent> queue = member.agent.run(member.conv);
            member.active = true;
            return queue;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void stopMember(String name) {`（源码行 120-128）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public synchronized void stopMember(String name) {
            Member member = members.get(name);
            if (member != null) {
                member.active = false;
                if (member.thread != null) {
                    member.thread.interrupt();
                }
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void stopAll() {`（源码行 130-135）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public synchronized void stopAll() {
            for (Member m : members.values()) {
                m.active = false;
                if (m.thread != null) m.thread.interrupt();
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized Member getMember(String name) {`（源码行 137-139）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public synchronized Member getMember(String name) {
            return members.get(name);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized boolean hasMember(String name) {`（源码行 141-143）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public synchronized boolean hasMember(String name) {
            return members.containsKey(name);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<String> memberNames() {`（源码行 145-147）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public synchronized List<String> memberNames() {
            return new ArrayList<>(members.keySet());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void sendMessage(String from, String to, String content) {`（源码行 149-151）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public void sendMessage(String from, String to, String content) {
            mailBox.send(to, new FileMailBox.MailMessage(from, content));
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<TeammateProgress> getTeammateProgressList() {`（源码行 153-158）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public List<TeammateProgress> getTeammateProgressList() {
            return members.values().stream()
                    .filter(m -> m.progress != null)
                    .map(m -> m.progress)
                    .toList();
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Member(String name, Agent agent, ConversationManager conv) {`（源码行 169-173）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Member(String name, Agent agent, ConversationManager conv) {
            this.name = name;
            this.agent = agent;
            this.conv = conv;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getName() { return name; }`（源码行 175-175）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String getName() { return name; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean isActive() { return active; }`（源码行 176-176）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public boolean isActive() { return active; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/TeammateProgress.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `TeammateProgress, ToolActivity`；85 行；直接依赖 `java.util.ArrayDeque, java.util.Deque, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public ToolActivity(String toolName, Map<String, Object> input) {`（源码行 16-19）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolActivity(String toolName, Map<String, Object> input) {
            this.toolName = toolName;
            this.description = describeActivity(toolName, input);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String describeActivity(String toolName, Map<String, Object> input) {`（源码行 20-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        private static String describeActivity(String toolName, Map<String, Object> input) {
            return switch (toolName) {
                case "ReadFile" -> "Reading " + input.getOrDefault("file_path", "");
                case "EditFile" -> "Editing " + input.getOrDefault("file_path", "");
                case "WriteFile" -> "Writing " + input.getOrDefault("file_path", "");
                case "Bash" -> {
                    String cmd = String.valueOf(input.getOrDefault("command", ""));
                    yield "Running " + (cmd.length() > 40 ? cmd.substring(0, 40) + "…" : cmd);
                }
                case "Glob" -> "Searching " + input.getOrDefault("pattern", "");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TeammateProgress(String name, String teamName, String spinnerVerb) {`（源码行 46-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public TeammateProgress(String name, String teamName, String spinnerVerb) {
        this.name = name;
        this.teamName = teamName;
        this.spinnerVerb = spinnerVerb;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void recordToolUse(String toolName, Map<String, Object> input) {`（源码行 52-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void recordToolUse(String toolName, Map<String, Object> input) {
        toolUseCount++;
        ToolActivity act = new ToolActivity(toolName, input);
        lastActivity = act;
        recentActivities.addLast(act);
        if (recentActivities.size() > 5) recentActivities.removeFirst();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void recordTokens(long input, long output) {`（源码行 60-62）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void recordTokens(long input, long output) {
        tokenCount = input + output;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void setStatus(String s) { status = s; }`（源码行 64-64）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void setStatus(String s) { status = s; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getName() { return name; }`（源码行 67-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getName() { return name; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getTeamName() { return teamName; }`（源码行 68-68）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getTeamName() { return teamName; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int getToolUseCount() { return toolUseCount; }`（源码行 69-69）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int getToolUseCount() { return toolUseCount; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public long getTokenCount() { return tokenCount; }`（源码行 70-70）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public long getTokenCount() { return tokenCount; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getStatus() { return status; }`（源码行 71-71）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getStatus() { return status; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getSpinnerVerb() { return spinnerVerb; }`（源码行 72-72）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getSpinnerVerb() { return spinnerVerb; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolActivity getLastActivity() { return lastActivity; }`（源码行 73-73）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolActivity getLastActivity() { return lastActivity; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized String getActivitySummary() {`（源码行 75-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized String getActivitySummary() {
        if (lastActivity != null) return lastActivity.description;
        return spinnerVerb;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String formatTokens(long n) {`（源码行 80-84）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String formatTokens(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/TeammateRunner.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `TeammateRunner, WaitResult`；210 行；直接依赖 `com.jimicode.agent.AgentEvent, com.jimicode.tui.SpinnerVerbs, java.util.List, java.util.concurrent.BlockingQueue, java.util.concurrent.LinkedBlockingQueue, java.util.concurrent.TimeUnit`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private TeammateRunner() {}`（源码行 26-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private TeammateRunner() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (addendum != null && !addendum.isEmpty()) {`（源码行 44-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (addendum != null && !addendum.isEmpty()) {
            member.conv.addSystemReminder(addendum);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `injectPendingMessages(team, member.getName(), member.conv);`（源码行 49-73）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        injectPendingMessages(team, member.getName(), member.conv);

        // First turn: use initial prompt
        member.conv.addUserMessage(initialPrompt);

        // Run agent
        var agentQueue = member.agent.run(member.conv);
        drainAgentEvents(agentQueue, eventOut, progress);

        // Send idle notification
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static List<String> drainLeadMailbox(TeamManager teamMgr) {`（源码行 83-103）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static List<String> drainLeadMailbox(TeamManager teamMgr) {
        if (teamMgr == null) return List.of();
        var result = new java.util.ArrayList<String>();
        for (String teamName : teamMgr.listTeams()) {
            var team = teamMgr.getTeam(teamName);
            if (team == null) continue;
            var messages = team.getMailBox().readUnread(LEAD_NAME);
            if (messages.isEmpty()) continue;

            var sb = new StringBuilder();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String buildTeammateAddendum(String teamName, String memberName, List<String> otherMembers) {`（源码行 108-120）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String buildTeammateAddendum(String teamName, String memberName, List<String> otherMembers) {
        var sb = new StringBuilder();
        sb.append("You are a member of team \"").append(teamName).append("\". ");
        sb.append("Your name is \"").append(memberName).append("\".\n\n");
        if (otherMembers != null && !otherMembers.isEmpty()) {
            sb.append("Other team members: ").append(String.join(", ", otherMembers)).append("\n\n");
        }
        sb.append("You can communicate with teammates using the SendMessage tool.\n");
        sb.append("Messages from teammates arrive as system reminders at the start of each turn.\n");
        sb.append("When you finish your current task, simply stop calling tools — ");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (messages.isEmpty()) return;`（源码行 130-135）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (messages.isEmpty()) return;

        var sb = new StringBuilder("You have new messages:\n\n");
        for (var msg : messages) {
            sb.append("From ").append(msg.from()).append(": ").append(msg.text()).append("\n\n");
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean isShutdownRequest(String message) {`（源码行 140-142）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean isShutdownRequest(String message) {
        return message != null && message.strip().startsWith(SHUTDOWN_PREFIX);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String createIdleNotification(String memberName, String reason) {`（源码行 144-147）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String createIdleNotification(String memberName, String reason) {
        return "[idle] %s: %s (at %s)".formatted(memberName, reason,
                java.time.Instant.now().toString());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private record WaitResult(String prompt, boolean shutdown) {}`（源码行 151-151）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private record WaitResult(String prompt, boolean shutdown) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static WaitResult waitForNextPromptOrShutdown(TeamManager.Team team, String memberName) {`（源码行 153-181）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static WaitResult waitForNextPromptOrShutdown(TeamManager.Team team, String memberName) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(IDLE_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new WaitResult(null, true);
            }

            var messages = team.getMailBox().readUnread(memberName);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `while (true) {`（源码行 185-208）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        while (true) {
            AgentEvent event;
            try {
                event = source.poll(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                progress.setStatus("failed");
                return;
            }
            if (event == null) return;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/TeamTools.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `TeamTools, SendMessageTool, TeamCreateTool, TeamDeleteTool`；238 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, java.util.LinkedHashMap, java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private TeamTools() {}`（源码行 21-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private TeamTools() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public SendMessageTool(TeamManager teamMgr, String senderName) {`（源码行 29-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public SendMessageTool(TeamManager teamMgr, String senderName) {
            this.teamMgr = teamMgr;
            this.senderName = senderName;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public String name() { return "SendMessage"; }`（源码行 34-34）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public String name() { return "SendMessage"; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public ToolCategory category() { return ToolCategory.COMMAND; }`（源码行 35-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public ToolCategory category() { return ToolCategory.COMMAND; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 38-41）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String description() {
            return "Send a message to another named agent in the team. The recipient will see it on their next turn. "
                    + "Use SendMessage to communicate with teammates by name. Messages arrive as system reminders.";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 44-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> schema() {
            var props = new LinkedHashMap<String, Object>();
            props.put("to", Map.of("type", "string", "description", "Name of the recipient agent"));
            props.put("content", Map.of("type", "string", "description", "Message content to send"));

            return Map.of(
                    "name", name(),
                    "description", description(),
                    "input_schema", Map.of(
                            "type", "object",
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 61-96）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolResult execute(Map<String, Object> args) {
            String to = (String) args.get("to");
            String content = (String) args.get("content");
            if (to == null || to.isEmpty() || content == null || content.isEmpty()) {
                return ToolResult.error("Error: 'to' and 'content' are required");
            }

            // Route to lead by finding any team the sender belongs to.
            if ("lead".equals(to)) {
                for (String teamName : teamMgr.listTeams()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TeamCreateTool(TeamManager teamMgr) {`（源码行 105-107）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public TeamCreateTool(TeamManager teamMgr) {
            this.teamMgr = teamMgr;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public String name() { return "TeamCreate"; }`（源码行 109-109）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public String name() { return "TeamCreate"; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public ToolCategory category() { return ToolCategory.COMMAND; }`（源码行 110-110）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public ToolCategory category() { return ToolCategory.COMMAND; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 113-145）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String description() {
            return "Create a new team for coordinating multiple agents.\n\n"
                    + "## When to Use\n\n"
                    + "Use this tool proactively whenever:\n"
                    + "- The user explicitly asks to use a team, swarm, or group of agents\n"
                    + "- The user mentions wanting agents to work together, coordinate, or collaborate\n"
                    + "- A task requires sequential or parallel collaboration between multiple agents\n\n"
                    + "When in doubt about whether a task warrants a team, prefer spawning a team.\n\n"
                    + "## Team Workflow\n\n"
                    + "1. **Create a team** with TeamCreate\n"
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 148-162）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> schema() {
            var props = new LinkedHashMap<String, Object>();
            props.put("team_name", Map.of("type", "string", "description", "Name for the team"));
            props.put("description", Map.of("type", "string", "description", "What this team will work on"));

            return Map.of(
                    "name", name(),
                    "description", description(),
                    "input_schema", Map.of(
                            "type", "object",
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 165-183）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolResult execute(Map<String, Object> args) {
            String name = (String) args.get("team_name");
            if (name == null || name.isEmpty()) {
                return ToolResult.error("Error: team_name is required");
            }

            String baseName = name;
            for (int i = 2; teamMgr.getTeam(name) != null; i++) {
                name = baseName + "-" + i;
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public TeamDeleteTool(TeamManager teamMgr) {`（源码行 191-193）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public TeamDeleteTool(TeamManager teamMgr) {
            this.teamMgr = teamMgr;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public String name() { return "TeamDelete"; }`（源码行 195-195）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public String name() { return "TeamDelete"; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public ToolCategory category() { return ToolCategory.COMMAND; }`（源码行 196-196）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        @Override public ToolCategory category() { return ToolCategory.COMMAND; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 199-201）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public String description() {
            return "Delete a team, stopping all its members.";
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 204-217）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public Map<String, Object> schema() {
            var props = new LinkedHashMap<String, Object>();
            props.put("team_name", Map.of("type", "string", "description", "Name of the team to delete"));

            return Map.of(
                    "name", name(),
                    "description", description(),
                    "input_schema", Map.of(
                            "type", "object",
                            "properties", props,
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 220-236）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        public ToolResult execute(Map<String, Object> args) {
            String name = (String) args.get("team_name");
            if (name == null || name.isEmpty()) {
                return ToolResult.error("Error: team_name is required");
            }

            TeamManager.Team team = teamMgr.getTeam(name);
            if (team == null) {
                return ToolResult.error("Error: team '%s' not found".formatted(name));
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/teams/TmuxBackend.java`

**类型与边界。** 包 `com.jimicode.teams`；类型 `TmuxBackend`；46 行；直接依赖 `java.util.concurrent.TimeUnit, java.util.logging.Logger`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private TmuxBackend() {}`（源码行 18-18）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private TmuxBackend() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String spawnTmuxTeammate(String teamName, String memberName, String cliCommand) throws Exception {`（源码行 20-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String spawnTmuxTeammate(String teamName, String memberName, String cliCommand) throws Exception {
        String paneName = teamName + "-" + memberName;
        var pb = new ProcessBuilder("tmux", "new-window", "-d", "-n", paneName, cliCommand);
        pb.redirectErrorStream(true);
        var proc = pb.start();
        proc.getInputStream().readAllBytes();
        boolean finished = proc.waitFor(30, TimeUnit.SECONDS);
        if (!finished || proc.exitValue() != 0) {
            throw new RuntimeException("Failed to spawn tmux window: " + paneName);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void stopTmuxTeammate(String paneName) {`（源码行 33-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void stopTmuxTeammate(String paneName) {
        try {
            // Send Ctrl-C first
            new ProcessBuilder("tmux", "send-keys", "-t", paneName, "C-c")
                    .start().waitFor(5, TimeUnit.SECONDS);
            Thread.sleep(200);
            // Then kill the window
            new ProcessBuilder("tmux", "kill-window", "-t", paneName)
                    .start().waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/FileStateCache.java`

**类型与边界。** 包 `com.jimicode.tool`；类型 `FileStateCache, FileState`；67 行；直接依赖 `java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.concurrent.ConcurrentHashMap`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record FileState(String content, long mtimeMs) {}`（源码行 21-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record FileState(String content, long mtimeMs) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void record(String absPath, String content, long mtimeMs) {`（源码行 26-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void record(String absPath, String content, long mtimeMs) {
        cache.put(absPath, new FileState(content, mtimeMs));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void update(String absPath, String newContent) {`（源码行 31-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void update(String absPath, String newContent) {
        long mtime;
        try {
            mtime = Files.getLastModifiedTime(Path.of(absPath)).toMillis();
        } catch (IOException e) {
            mtime = System.currentTimeMillis();
        }
        cache.put(absPath, new FileState(newContent, mtime));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public FileState get(String absPath) {`（源码行 42-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public FileState get(String absPath) {
        return cache.get(absPath);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String validate(String absPath) {`（源码行 50-66）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String validate(String absPath) {
        FileState state = cache.get(absPath);
        if (state == null) {
            return "Error: file has not been read yet. Read it first before editing.";
        }
        long currentMtime;
        try {
            currentMtime = Files.getLastModifiedTime(Path.of(absPath)).toMillis();
        } catch (IOException e) {
            // File might have been deleted; let the caller's own exists-check handle it
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/AskUserTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `AskUserTool`；164 行；直接依赖 `com.jimicode.agent.AgentEvent, com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, com.jimicode.tui.dialog.AskUserDialog, java.util.ArrayList, java.util.LinkedHashMap, java.util.List, java.util.Map, java.util.concurrent.BlockingQueue`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public void setEventQueue(BlockingQueue<AgentEvent> queue) {`（源码行 36-38）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setEventQueue(BlockingQueue<AgentEvent> queue) {
        this.eventQueue = queue;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 41-43）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "AskUserQuestion";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 46-48）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 51-53）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.READ;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldDefer() {`（源码行 56-58）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean shouldDefer() {
        return true;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 61-106）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        var optionProperties = new LinkedHashMap<String, Object>();
        optionProperties.put("label", Map.of("type", "string", "description", "Option display text (1-5 words)"));
        optionProperties.put("description", Map.of("type", "string", "description", "What this option means"));

        var optionSchema = new LinkedHashMap<String, Object>();
        optionSchema.put("type", "object");
        optionSchema.put("properties", optionProperties);
        optionSchema.put("required", List.of("label", "description"));

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 110-163）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        if (eventQueue == null) {
            return ToolResult.error("AskUser tool not wired to event queue");
        }

        var rawQuestions = (List<Map<String, Object>>) args.get("questions");
        if (rawQuestions == null || rawQuestions.isEmpty()) {
            return ToolResult.error("No questions provided");
        }

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/BashTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `BashTool`；148 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, java.io.IOException, java.io.InputStream, java.util.List, java.util.Map, java.util.concurrent.TimeUnit`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public String name() {`（源码行 47-49）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "Bash";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 52-54）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 57-59）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.COMMAND;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 62-75）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "command", Map.of("type", "string", "description", "Shell command to execute"),
                                "timeout", Map.of("type", "integer", "description", "Timeout in seconds (max 600)", "default", 120)
                        ),
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 78-136）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String command = stringArg(args, "command", "");
        if (command.isEmpty()) {
            return ToolResult.error("Error: command is required");
        }

        int timeout = intArg(args, "timeout", 120);
        if (timeout > MAX_TIMEOUT) {
            timeout = MAX_TIMEOUT;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 138-141）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static int intArg(Map<String, Object> args, String key, int def) {`（源码行 143-147）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static int intArg(Map<String, Object> args, String key, int def) {
        var v = args.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/EditFileTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `EditFileTool`；144 行；直接依赖 `com.jimicode.tool.FileStateCache, com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public void setFileStateCache(FileStateCache c) { this.fileStateCache = c; }`（源码行 24-24）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setFileStateCache(FileStateCache c) { this.fileStateCache = c; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 38-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "EditFile";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 43-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 48-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.WRITE;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 53-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "file_path", Map.of("type", "string", "description", "Path to the file to edit"),
                                "old_string", Map.of("type", "string", "description", "The exact string to find and replace (must be unique in file)"),
                                "new_string", Map.of("type", "string", "description", "The replacement string")
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setFileHistory(com.jimicode.filehistory.FileHistory fh) { this.fileHistory = fh; }`（源码行 69-69）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setFileHistory(com.jimicode.filehistory.FileHistory fh) { this.fileHistory = fh; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 72-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String filePath = stringArg(args, "file_path", "");
        String oldStr = stringArg(args, "old_string", "");
        String newStr = stringArg(args, "new_string", "");

        if (filePath.isEmpty()) {
            return ToolResult.error("Error: file_path is required");
        }

        if (fileHistory != null) fileHistory.trackEdit(filePath);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static int countOccurrences(String text, String sub) {`（源码行 127-138）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static int countOccurrences(String text, String sub) {
        if (sub.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 140-143）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/EnterWorktreeTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `EnterWorktreeTool`；96 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, com.jimicode.worktree.WorktreeManager, com.jimicode.worktree.WorktreeSessionStore, com.jimicode.worktree.SlugValidator, java.security.SecureRandom, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public EnterWorktreeTool(WorktreeManager worktreeManager, String sessionId) {`（源码行 27-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public EnterWorktreeTool(WorktreeManager worktreeManager, String sessionId) {
        this.worktreeManager = worktreeManager;
        this.sessionId = sessionId;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public String name() { return "EnterWorktree"; }`（源码行 32-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    @Override public String name() { return "EnterWorktree"; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public ToolCategory category() { return ToolCategory.COMMAND; }`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    @Override public ToolCategory category() { return ToolCategory.COMMAND; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public boolean shouldDefer() { return true; }`（源码行 34-34）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    @Override public boolean shouldDefer() { return true; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 37-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return "Creates an isolated worktree (via git) and switches the session into it";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 42-56）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "name", Map.of(
                                        "type", "string",
                                        "description", "Optional name for the worktree. Max 64 chars."
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 59-95）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        if (WorktreeSessionStore.getCurrentSession() != null) {
            return ToolResult.error("Already in a worktree session");
        }

        String slug = args.containsKey("name") ? String.valueOf(args.get("name")) : null;
        if (slug == null || slug.isBlank()) {
            slug = "wt-" + Integer.toHexString(RANDOM.nextInt());
        }

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/ExitPlanModeTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `ExitPlanModeTool`；77 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, java.util.Map, java.util.function.BooleanSupplier`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public void setIsPlanMode(BooleanSupplier isPlanMode) {`（源码行 24-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setIsPlanMode(BooleanSupplier isPlanMode) {
        this.isPlanMode = isPlanMode;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setPlanExists(BooleanSupplier planExists) {`（源码行 28-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setPlanExists(BooleanSupplier planExists) {
        this.planExists = planExists;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 33-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "ExitPlanMode";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 38-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 43-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.READ;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 48-57）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of()
                )
        );
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 60-76）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        if (isPlanMode != null && !isPlanMode.getAsBoolean()) {
            return new ToolResult(
                    "You are not in plan mode. This tool is only for exiting plan mode after writing a plan.",
                    true);
        }
        if (planExists != null && !planExists.getAsBoolean()) {
            return new ToolResult(
                    "No plan file found. Please write your plan to the plan file before calling ExitPlanMode.",
                    true);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/ExitWorktreeTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `ExitWorktreeTool`；126 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, com.jimicode.worktree.WorktreeChanges, com.jimicode.worktree.WorktreeManager, com.jimicode.worktree.WorktreeSessionStore, java.util.ArrayList, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public ExitWorktreeTool(WorktreeManager worktreeManager) {`（源码行 25-27）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ExitWorktreeTool(WorktreeManager worktreeManager) {
        this.worktreeManager = worktreeManager;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public String name() { return "ExitWorktree"; }`（源码行 29-29）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    @Override public String name() { return "ExitWorktree"; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public ToolCategory category() { return ToolCategory.COMMAND; }`（源码行 30-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    @Override public ToolCategory category() { return ToolCategory.COMMAND; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `@Override public boolean shouldDefer() { return true; }`（源码行 31-31）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    @Override public boolean shouldDefer() { return true; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 34-36）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return "Exits a worktree session created by EnterWorktree and restores the original working directory";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 39-59）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "action", Map.of(
                                        "type", "string",
                                        "enum", new String[]{"keep", "remove"},
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 62-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        var session = WorktreeSessionStore.getCurrentSession();
        if (session == null) {
            return ToolResult.error(
                    "No-op: there is no active EnterWorktree session to exit. "
                            + "This tool only operates on worktrees created by EnterWorktree in the current session."
            );
        }

        String action = String.valueOf(args.getOrDefault("action", "keep"));
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/GlobTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `GlobTool`；126 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, java.io.IOException, java.nio.file.*, java.nio.file.attribute.BasicFileAttributes, java.util.ArrayList, java.util.Collections, java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public String name() {`（源码行 37-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "Glob";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 42-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 47-49）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.READ;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 52-65）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "pattern", Map.of("type", "string", "description", "Glob pattern to match (e.g. '**/*.py')"),
                                "path", Map.of("type", "string", "description", "Base directory to search from", "default", ".")
                        ),
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 68-120）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String pattern = stringArg(args, "pattern", "");
        String basePath = stringArg(args, "path", ".");
        if (basePath.isEmpty()) {
            basePath = ".";
        }
        if (pattern.isEmpty()) {
            return ToolResult.error("Error: pattern is required");
        }

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 122-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/GrepTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `GrepTool`；191 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolRegistry, com.jimicode.tool.ToolResult, java.io.BufferedReader, java.io.IOException, java.io.InputStream, java.nio.file.*, java.nio.file.attribute.BasicFileAttributes, java.util.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public String name() {`（源码行 39-41）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "Grep";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 44-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 49-51）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.READ;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 54-69）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("pattern", Map.of("type", "string", "description", "Regex pattern to search for"));
        properties.put("path", Map.of("type", "string", "description", "Base directory to search from", "default", "."));
        properties.put("include", Map.of("type", "string", "description", "Glob filter for filenames (e.g. '*.py')"));

        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 72-164）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String pattern = stringArg(args, "pattern", "");
        String basePath = stringArg(args, "path", ".");
        String include = stringArg(args, "include", "");
        if (basePath.isEmpty()) {
            basePath = ".";
        }
        if (pattern.isEmpty()) {
            return ToolResult.error("Error: pattern is required");
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean isBinaryFile(Path file) {`（源码行 169-185）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean isBinaryFile(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            byte[] buf = new byte[512];
            int bytesRead = is.read(buf);
            if (bytesRead <= 0) {
                return false;
            }
            for (int i = 0; i < bytesRead; i++) {
                if (buf[i] == 0) {
                    return true;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 187-190）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/ReadFileTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `ReadFileTool`；137 行；直接依赖 `com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, com.jimicode.tool.FileStateCache, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public void setFileStateCache(FileStateCache c) { this.fileStateCache = c; }`（源码行 24-24）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setFileStateCache(FileStateCache c) { this.fileStateCache = c; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 38-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "ReadFile";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 43-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 48-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.READ;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 53-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "file_path", Map.of("type", "string", "description", "Absolute or relative path to the file to read"),
                                "offset", Map.of("type", "integer", "description", "Line offset to start reading from (0-based)", "default", 0),
                                "limit", Map.of("type", "integer", "description", "Maximum number of lines to read", "default", 2000)
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 70-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String filePath = stringArg(args, "file_path", "");
        if (filePath.isEmpty()) {
            return ToolResult.error("Error: file_path is required");
        }

        int offset = intArg(args, "offset", 0);
        int limit = intArg(args, "limit", 2000);

        Path path = Path.of(filePath);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 127-130）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static int intArg(Map<String, Object> args, String key, int def) {`（源码行 132-136）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static int intArg(Map<String, Object> args, String key, int def) {
        var v = args.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/ToolSearchTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `ToolSearchTool`；159 行；直接依赖 `com.fasterxml.jackson.core.JsonProcessingException, com.fasterxml.jackson.databind.ObjectMapper, com.fasterxml.jackson.databind.SerializationFeature, com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolRegistry, com.jimicode.tool.ToolResult, java.util.Arrays, java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `Some tools are deferred (not loaded by default) to save context space. \`（源码行 28-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            Some tools are deferred (not loaded by default) to save context space. \
            Use this tool to discover and load them.

            Query forms:
            - "select:ToolName,AnotherTool" -- fetch exact tools by name
            - "keyword search" -- keyword search, returns up to max_results matches

            When you need a tool that isn't in your current tool list, use this to find it.""";

    private final ToolRegistry registry;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolSearchTool(ToolRegistry registry, String protocol) {`（源码行 44-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolSearchTool(ToolRegistry registry, String protocol) {
        this.registry = registry;
        this.protocol = protocol;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 50-52）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "ToolSearch";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 55-57）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 60-62）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.READ;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean shouldDefer() {`（源码行 65-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean shouldDefer() {
        return false;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 70-90）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "Query to find deferred tools. Use \"select:Name1,Name2\" for direct selection, or keywords to search."
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 93-147）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String query = stringArg(args, "query", "");
        if (query.isEmpty()) {
            return ToolResult.error("Error: query is required");
        }

        int maxResults = intArg(args, "max_results", 5);
        if (maxResults < 1) {
            maxResults = 5;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 149-152）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static int intArg(Map<String, Object> args, String key, int def) {`（源码行 154-158）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static int intArg(Map<String, Object> args, String key, int def) {
        var v = args.get(key);
        if (v instanceof Number n) return n.intValue();
        return def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/impl/WriteFileTool.java`

**类型与边界。** 包 `com.jimicode.tool.impl`；类型 `WriteFileTool`；128 行；直接依赖 `com.jimicode.tool.FileStateCache, com.jimicode.tool.Tool, com.jimicode.tool.ToolCategory, com.jimicode.tool.ToolResult, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.nio.file.attribute.PosixFilePermission, java.nio.file.attribute.PosixFilePermissions, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public void setFileHistory(com.jimicode.filehistory.FileHistory fh) { this.fileHistory = fh; }`（源码行 27-27）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setFileHistory(com.jimicode.filehistory.FileHistory fh) { this.fileHistory = fh; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setFileStateCache(FileStateCache c) { this.fileStateCache = c; }`（源码行 28-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setFileStateCache(FileStateCache c) { this.fileStateCache = c; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String name() {`（源码行 40-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String name() {
        return "WriteFile";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String description() {`（源码行 45-47）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String description() {
        return DESCRIPTION;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolCategory category() {`（源码行 50-52）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolCategory category() {
        return ToolCategory.WRITE;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, Object> schema() {`（源码行 55-68）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, Object> schema() {
        return Map.of(
                "name", name(),
                "description", description(),
                "input_schema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "file_path", Map.of("type", "string", "description", "Path to the file to write"),
                                "content", Map.of("type", "string", "description", "Content to write to the file")
                        ),
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ToolResult execute(Map<String, Object> args) {`（源码行 71-122）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ToolResult execute(Map<String, Object> args) {
        String filePath = stringArg(args, "file_path", "");
        String content = stringArg(args, "content", "");

        if (filePath.isEmpty()) {
            return ToolResult.error("Error: file_path is required");
        }

        if (fileHistory != null) fileHistory.trackEdit(filePath);

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String stringArg(Map<String, Object> args, String key, String def) {`（源码行 124-127）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String stringArg(Map<String, Object> args, String key, String def) {
        var v = args.get(key);
        return v instanceof String s ? s : def;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/Tool.java`

**类型与边界。** 包 `com.jimicode.tool`；类型 `Tool`；27 行；直接依赖 `java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `String name();`（源码行 13-25）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    String name();

    String description();

    ToolCategory category();

    Map<String, Object> schema();

    ToolResult execute(Map<String, Object> args);

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/ToolCategory.java`

**类型与边界。** 包 `com.jimicode.tool`；类型 `ToolCategory`；11 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- 未识别到常规方法签名；重点检查 record 组件、重写方法、lambda、静态初始化块和注解处理器生成代码。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/ToolRegistry.java`

**类型与边界。** 包 `com.jimicode.tool`；类型 `ToolRegistry`；126 行；直接依赖 `java.util.*`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public void markDiscovered(String name) {`（源码行 17-19）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void markDiscovered(String name) {
        discoveredTools.add(name);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean isDiscovered(String name) {`（源码行 21-23）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean isDiscovered(String name) {
        return discoveredTools.contains(name);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> getDeferredToolNames() {`（源码行 25-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<String> getDeferredToolNames() {
        return tools.values().stream()
                .filter(t -> t.shouldDefer() && !discoveredTools.contains(t.name()))
                .map(Tool::name)
                .toList();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void register(Tool tool) {`（源码行 32-34）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Tool get(String name) {`（源码行 36-38）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Tool get(String name) {
        return tools.get(name);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Tool> listTools() {`（源码行 40-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Tool> listTools() {
        return List.copyOf(tools.values());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Map<String, Object>> getAllSchemas(String protocol) {`（源码行 44-61）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Map<String, Object>> getAllSchemas(String protocol) {
        var schemas = new ArrayList<Map<String, Object>>();
        for (var tool : tools.values()) {
            if (tool.shouldDefer() && !discoveredTools.contains(tool.name())) continue;
            var base = tool.schema();
            if ("openai".equals(protocol)) {
                schemas.add(Map.of(
                        "type", "function",
                        "name", base.get("name"),
                        "description", base.get("description"),
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Tool> getDeferredTools() {`（源码行 63-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Tool> getDeferredTools() {
        return tools.values().stream()
                .filter(Tool::shouldDefer)
                .toList();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Map<String, Object>> searchDeferred(String query, int maxResults, String protocol) {`（源码行 69-91）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Map<String, Object>> searchDeferred(String query, int maxResults, String protocol) {
        String lower = query.toLowerCase();
        var matches = new ArrayList<Map<String, Object>>();
        for (var tool : tools.values()) {
            if (!tool.shouldDefer()) continue;
            if (tool.name().toLowerCase().contains(lower)
                    || tool.description().toLowerCase().contains(lower)) {
                var base = tool.schema();
                if ("openai".equals(protocol)) {
                    matches.add(Map.of(
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<Map<String, Object>> findDeferredByNames(List<String> names, String protocol) {`（源码行 93-114）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<Map<String, Object>> findDeferredByNames(List<String> names, String protocol) {
        var nameSet = new HashSet<String>();
        for (var n : names) nameSet.add(n.toLowerCase());

        var matches = new ArrayList<Map<String, Object>>();
        for (var tool : tools.values()) {
            if (nameSet.contains(tool.name().toLowerCase())) {
                var base = tool.schema();
                if ("openai".equals(protocol)) {
                    matches.add(Map.of(
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static ToolRegistry createDefault() {`（源码行 116-125）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static ToolRegistry createDefault() {
        var reg = new ToolRegistry();
        reg.register(new com.jimicode.tool.impl.ReadFileTool());
        reg.register(new com.jimicode.tool.impl.WriteFileTool());
        reg.register(new com.jimicode.tool.impl.EditFileTool());
        reg.register(new com.jimicode.tool.impl.BashTool());
        reg.register(new com.jimicode.tool.impl.GlobTool());
        reg.register(new com.jimicode.tool.impl.GrepTool());
        return reg;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tool/ToolResult.java`

**类型与边界。** 包 `com.jimicode.tool`；类型 `ToolResult`；18 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ToolResult(String output, boolean isError) {`（源码行 8-17）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
public record ToolResult(String output, boolean isError) {

    public static ToolResult success(String output) {
        return new ToolResult(output, false);
    }

    public static ToolResult error(String message) {
        return new ToolResult(message, true);
    }
}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/toolresult/ApplyResult.java`

**类型与边界。** 包 `com.jimicode.toolresult`；类型 `ApplyResult`；22 行；直接依赖 `com.jimicode.conversation.ConversationManager, java.util.List`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ApplyResult(ConversationManager apiConv, List<ContentReplacementRecord> newRecords) {}`（源码行 21-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
public record ApplyResult(ConversationManager apiConv, List<ContentReplacementRecord> newRecords) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/toolresult/ContentReplacementLifecycle.java`

**类型与边界。** 包 `com.jimicode.toolresult`；类型 `ContentReplacementLifecycle`；63 行；直接依赖 `com.jimicode.conversation.Message, com.jimicode.conversation.ToolResultBlock, java.util.HashSet, java.util.List, java.util.Map, java.util.Set`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private ContentReplacementLifecycle() {}`（源码行 32-32）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ContentReplacementLifecycle() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (Message m : messages) {`（源码行 41-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (Message m : messages) {
            if (m.getToolResults() == null) continue;
            for (ToolResultBlock tr : m.getToolResults()) {
                candidateIds.add(tr.toolUseId());
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (ContentReplacementRecord r : records) {`（源码行 48-53）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (ContentReplacementRecord r : records) {
            if (!ContentReplacementRecord.KIND_TOOL_RESULT.equals(r.kind())) continue;
            if (candidateIds.contains(r.toolUseId())) {
                state.replacements().put(r.toolUseId(), r.replacement());
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (inheritedReplacements != null) {`（源码行 54-59）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (inheritedReplacements != null) {
            for (Map.Entry<String, String> e : inheritedReplacements.entrySet()) {
                if (!candidateIds.contains(e.getKey())) continue;
                state.replacements().putIfAbsent(e.getKey(), e.getValue());
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/toolresult/ContentReplacementRecord.java`

**类型与边界。** 包 `com.jimicode.toolresult`；类型 `ContentReplacementRecord`；22 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ContentReplacementRecord(String kind, String toolUseId, String replacement) {`（源码行 14-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
public record ContentReplacementRecord(String kind, String toolUseId, String replacement) {

    public static final String KIND_TOOL_RESULT = "tool-result";

    public static ContentReplacementRecord toolResult(String toolUseId, String replacement) {
        return new ContentReplacementRecord(KIND_TOOL_RESULT, toolUseId, replacement);
    }
}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/toolresult/ContentReplacementState.java`

**类型与边界。** 包 `com.jimicode.toolresult`；类型 `ContentReplacementState`；52 行；直接依赖 `java.util.HashMap, java.util.HashSet, java.util.Map, java.util.Set`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public Set<String> seenIds() {`（源码行 33-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Set<String> seenIds() {
        return seenIds;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, String> replacements() {`（源码行 37-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, String> replacements() {
        return replacements;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ContentReplacementState copy() {`（源码行 46-51）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ContentReplacementState copy() {
        ContentReplacementState out = new ContentReplacementState();
        out.seenIds.addAll(this.seenIds);
        out.replacements.putAll(this.replacements);
        return out;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/toolresult/ReplacementRecordsIO.java`

**类型与边界。** 包 `com.jimicode.toolresult`；类型 `per, ReplacementRecordsIO`；70 行；直接依赖 `com.fasterxml.jackson.core.JsonProcessingException, com.fasterxml.jackson.databind.ObjectMapper, java.io.BufferedWriter, java.io.IOException, java.nio.charset.StandardCharsets, java.nio.file.Files, java.nio.file.Path, java.nio.file.StandardOpenOption, java.util.ArrayList, java.util.Collections`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private ReplacementRecordsIO() {}`（源码行 33-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ReplacementRecordsIO() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void append(Path sessionDir, List<ContentReplacementRecord> records) throws IOException {`（源码行 35-57）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void append(Path sessionDir, List<ContentReplacementRecord> records) throws IOException {
        if (records.isEmpty()) return;
        Files.createDirectories(sessionDir);
        Path file = sessionDir.resolve(RECORDS_FILENAME);
        try (BufferedWriter w = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND
        )) {
            for (ContentReplacementRecord r : records) {
                String kind = r.kind() == null || r.kind().isEmpty()
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static List<ContentReplacementRecord> load(Path sessionDir) throws IOException {`（源码行 59-69）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static List<ContentReplacementRecord> load(Path sessionDir) throws IOException {
        Path file = sessionDir.resolve(RECORDS_FILENAME);
        if (!Files.exists(file)) return Collections.emptyList();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<ContentReplacementRecord> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line.isEmpty()) continue;
            out.add(MAPPER.readValue(line, ContentReplacementRecord.class));
        }
        return out;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/toolresult/ToolResultBudget.java`

**类型与边界。** 包 `com.jimicode.toolresult`；类型 `this, ToolResultBudget`；276 行；直接依赖 `com.jimicode.conversation.ConversationManager, com.jimicode.conversation.Message, com.jimicode.conversation.ToolResultBlock, java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.ArrayList, java.util.Comparator, java.util.HashMap, java.util.HashSet`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private ToolResultBudget() {}`（源码行 57-57）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private ToolResultBudget() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (messages.isEmpty()) {`（源码行 65-67）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (messages.isEmpty()) {
            return new ApplyResult(conv, List.of());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `for (Message msg : messages) {`（源码行 73-173）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        for (Message msg : messages) {
            List<ToolResultBlock> trs = msg.getToolResults();
            if (trs == null || trs.isEmpty()) {
                newHistory.add(msg);
                continue;
            }

            Map<String, String> decisions = new HashMap<>(trs.size() * 2);
            List<ToolResultBlock> fresh = new ArrayList<>();

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `return new ApplyResult(buildManager(newHistory), records);`（源码行 179-182）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        return new ApplyResult(buildManager(newHistory), records);
    }

    private static String buildSpillPreview(int originalSize, Path path) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String spillAndPreview(Path spillDir, ToolResultBlock tr) {`（源码行 187-199）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String spillAndPreview(Path spillDir, ToolResultBlock tr) {
        try {
            Files.createDirectories(spillDir);
            Path file = spillDir.resolve(tr.toolUseId());
            if (Files.exists(file) && Files.size(file) == tr.content().length()) {
                return buildSpillPreview(tr.content().length(), file);
            }
            Files.writeString(file, tr.content());
            return buildSpillPreview(tr.content().length(), file);
        } catch (IOException e) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean isAlreadyReplaced(String s) {`（源码行 201-203）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean isAlreadyReplaced(String s) {
        return s != null && (s.startsWith(PERSISTED_TAG_PREFIX) || s.startsWith(SNIPPED_TAG_PREFIX));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static List<Message> snipStale(List<Message> messages) {`（源码行 205-243）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static List<Message> snipStale(List<Message> messages) {
        int totalTurns = 0;
        for (Message m : messages) {
            if ("assistant".equals(m.getRole()) && (m.getToolUses() == null || m.getToolUses().isEmpty())) {
                totalTurns++;
            }
        }
        if (totalTurns <= KEEP_RECENT_TURNS) return messages;

        List<Message> out = new ArrayList<>(messages.size());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Message copyMessageWithResults(Message src, List<ToolResultBlock> newResults) {`（源码行 245-251）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Message copyMessageWithResults(Message src, List<ToolResultBlock> newResults) {
        Message copy = new Message(src.getRole(), src.getContent());
        copy.setThinkingBlocks(src.getThinkingBlocks());
        copy.setToolUses(src.getToolUses());
        copy.setToolResults(newResults);
        return copy;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static ConversationManager buildManager(List<Message> messages) {`（源码行 259-275）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static ConversationManager buildManager(List<Message> messages) {
        ConversationManager out = new ConversationManager();
        for (Message m : messages) {
            boolean hasToolUses = m.getToolUses() != null && !m.getToolUses().isEmpty();
            boolean hasToolResults = m.getToolResults() != null && !m.getToolResults().isEmpty();
            if (hasToolUses) {
                out.addAssistantFull(m.getContent(), m.getThinkingBlocks(), m.getToolUses());
            } else if (hasToolResults) {
                out.addToolResultsMessage(m.getToolResults());
            } else if ("user".equals(m.getRole())) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/AppState.java`

**类型与边界。** 包 `com.jimicode.tui`；类型 `AppState`；8 行；直接依赖 `java.lang/同包`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- 未识别到常规方法签名；重点检查 record 组件、重写方法、lambda、静态初始化块和注解处理器生成代码。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/ChatMessage.java`

**类型与边界。** 包 `com.jimicode.tui`；类型 `ChatMessage, ToolBlockInfo, SubAgentBlockState`；60 行；直接依赖 `java.util.ArrayList, java.util.List, java.util.Map`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public ChatMessage(String role, String content) {`（源码行 24-27）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public ChatMessage(String role, String content, List<ToolBlockInfo> toolGroup) {`（源码行 29-33）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public ChatMessage(String role, String content, List<ToolBlockInfo> toolGroup) {
        this.role = role;
        this.content = content;
        this.toolGroup = toolGroup;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/dialog/AskUserDialog.java`

**类型与边界。** 包 `com.jimicode.tui.dialog`；类型 `AskUserDialog, Question, Option`；560 行；直接依赖 `com.williamcallahan.tui4j.compat.bubbletea.lipgloss.Style, com.williamcallahan.tui4j.compat.bubbletea.lipgloss.color.ANSI256Color, java.util.ArrayList, java.util.HashMap, java.util.LinkedHashMap, java.util.List, java.util.Map, java.util.Set, java.util.TreeSet`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record Question(String text, String header, List<Option> options, boolean multiSelect) {}`（源码行 93-93）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Question(String text, String header, List<Option> options, boolean multiSelect) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record Option(String label, String description) {}`（源码行 101-101）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Option(String label, String description) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void activate(List<Question> questions) {`（源码行 110-127）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void activate(List<Question> questions) {
        this.active = true;
        this.questions = questions != null ? questions : List.of();
        this.questionIndex = 0;

        int n = this.questions.size();
        this.cursors = new int[n];
        this.otherText = new String[n];
        this.selected = new ArrayList<>(n);
        this.answers = new HashMap<>();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean isActive() {`（源码行 130-132）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean isActive() {
        return active;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Map<String, String> handleKey(String key) {`（源码行 144-229）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Map<String, String> handleKey(String key) {
        if (questions == null || questions.isEmpty()) {
            return null;
        }

        boolean multiQuestion = questions.size() > 1;

        // ── Submit tab ──────────────────────────────────────────────────
        if (onSubmitTab) {
            return handleSubmitTabKey(key, multiQuestion);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Map<String, String> handleSubmitTabKey(String key, boolean multiQuestion) {`（源码行 234-259）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Map<String, String> handleSubmitTabKey(String key, boolean multiQuestion) {
        switch (key) {
            case "up", "k" -> {
                if (submitCursor > 0) submitCursor--;
            }
            case "down", "j" -> {
                if (submitCursor < 1) submitCursor++;
            }
            case "left", "shift+tab" -> {
                if (multiQuestion) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void saveCurrentAnswer() {`（源码行 267-291）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void saveCurrentAnswer() {
        Question q = questions.get(questionIndex);
        int cursor = cursors[questionIndex];

        if (cursor == q.options().size()) {
            // "Other" selected
            String other = otherText[questionIndex];
            answers.put(questionIndex, other.isEmpty() ? "Other" : other);
        } else if (q.multiSelect()) {
            Set<Integer> sel = selected.get(questionIndex);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Map<String, String> submitAllAnswers() {`（源码行 296-306）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Map<String, String> submitAllAnswers() {
        active = false;
        var result = new LinkedHashMap<String, String>();
        for (int i = 0; i < questions.size(); i++) {
            String answer = answers.get(i);
            if (answer != null) {
                result.put(questions.get(i).text(), answer);
            }
        }
        return result;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private Map<String, String> cancelDialog() {`（源码行 311-314）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Map<String, String> cancelDialog() {
        active = false;
        return Map.of("_declined", "true");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String render(int width) {`（源码行 324-353）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String render(int width) {
        if (!active || questions == null || questions.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        boolean multiQuestion = questions.size() > 1;

        // Navigation bar (multi-question only)
        if (multiQuestion) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String renderNavBar() {`（源码行 357-399）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String renderNavBar() {
        var sb = new StringBuilder();

        // Left arrow
        if (questionIndex == 0 && !onSubmitTab) {
            sb.append(DIM_ARROW.render(" ←"));
        } else {
            sb.append(BRIGHT_ARROW.render(" ←"));
        }

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String renderQuestionView() {`（源码行 403-488）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String renderQuestionView() {
        var sb = new StringBuilder();
        Question q = questions.get(questionIndex);
        int cursor = cursors[questionIndex];
        int lines = 0;

        // Question header
        sb.append(HEADER_STYLE.render(" " + q.text()));
        sb.append("\n\n");
        lines += 2;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String renderSubmitView() {`（源码行 492-539）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String renderSubmitView() {
        var sb = new StringBuilder();
        int lines = 0;

        sb.append(HEADER_STYLE.render(" Review your answers:"));
        sb.append("\n\n");
        lines += 2;

        // Answer summary
        for (int i = 0; i < questions.size(); i++) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private int maxLines() {`（源码行 547-559）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private int maxLines() {
        int max = 0;
        for (Question q : questions) {
            int lines = 2 + q.options().size() + 1; // header + blank + options + Other
            if (q.multiSelect()) {
                lines++; // "space to toggle" hint
            }
            if (lines > max) {
                max = lines;
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/dialog/PlanApprovalDialog.java`

**类型与边界。** 包 `com.jimicode.tui.dialog`；类型 `PlanApprovalDialog, Result, DialogResult`；218 行；直接依赖 `com.jimicode.tui.Styles, com.williamcallahan.tui4j.compat.bubbletea.lipgloss.Style, com.williamcallahan.tui4j.compat.bubbletea.lipgloss.color.ANSI256Color`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record DialogResult(Result type, String feedback) {}`（源码行 78-78）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record DialogResult(Result type, String feedback) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void activate() {`（源码行 85-89）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void activate() {
        active = true;
        cursor = 0;
        feedbackInput.setLength(0);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public boolean isActive() {`（源码行 92-94）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public boolean isActive() {
        return active;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public DialogResult handleKey(String key) {`（源码行 105-155）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public DialogResult handleKey(String key) {
        switch (key) {
            case "up", "k" -> {
                if (cursor > 0) cursor--;
            }
            case "down", "j" -> {
                if (cursor < 2) cursor++;
            }
            case "enter" -> {
                if (cursor == 2 && feedbackInput.isEmpty()) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String render() {`（源码行 163-217）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String render() {
        var sb = new StringBuilder();

        // Header
        sb.append(HEADER_STYLE.render(
                " JimiCode has written up a plan and is ready to execute. Would you like to proceed?"));
        sb.append("\n\n");

        // Options list
        for (int i = 0; i < OPTIONS.length; i++) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/JimiCodeModel.java`

**类型与边界。** 包 `com.jimicode.tui`；类型 `JimiCodeModel, AgentEventMessage, MailboxPollMessage`；2220 行；直接依赖 `com.jimicode.agent.Agent, com.jimicode.agent.AgentEvent, com.jimicode.command.CommandRegistry, com.jimicode.config.HookConfig, com.jimicode.config.McpServerConfig, com.jimicode.config.ProviderConfig, com.jimicode.conversation.ConversationManager, com.jimicode.history.HistoryStore, com.jimicode.hook.HookEngine, com.jimicode.llm.LlmClient`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record AgentEventMessage() implements Message {}`（源码行 201-201）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record AgentEventMessage() implements Message {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public record MailboxPollMessage() implements Message {}`（源码行 202-202）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record MailboxPollMessage() implements Message {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `this(providers, mcpServers, hookConfigs, PermissionMode.DEFAULT);`（源码行 206-210）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        this(providers, mcpServers, hookConfigs, PermissionMode.DEFAULT);
    }

    public JimiCodeModel(List<ProviderConfig> providers, List<McpServerConfig> mcpServers,
                        List<HookConfig> hookConfigs, PermissionMode initialPermissionMode) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (this.providers.size() == 1) {`（源码行 216-222）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (this.providers.size() == 1) {
            this.selectedProvider = this.providers.get(0);
            this.state = AppState.CHAT;
            this.pendingSingleProviderInit = true;
        } else {
            this.state = AppState.PROVIDER_SELECT;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!this.hookConfigs.isEmpty()) {`（源码行 234-242）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (!this.hookConfigs.isEmpty()) {
            List<HookEngine.Hook> hooks = this.hookConfigs.stream().map(hc -> {
                HookEngine.EventName event = parseEventName(hc.getEvent());
                HookEngine.ActionType actionType = parseActionType(hc.getType());
                var action = new HookEngine.Action(actionType, hc.getCommand(), hc.getMessage());
                return new HookEngine.Hook(hc.getId(), event, hc.getCondition(), action, hc.isReject());
            }).toList();
            this.hookEngine.loadHooks(hooks);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (s == null) return HookEngine.EventName.SESSION_START;`（源码行 246-258）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (s == null) return HookEngine.EventName.SESSION_START;
        return switch (s.toLowerCase()) {
            case "session_start" -> HookEngine.EventName.SESSION_START;
            case "session_end" -> HookEngine.EventName.SESSION_END;
            case "turn_start" -> HookEngine.EventName.TURN_START;
            case "turn_end" -> HookEngine.EventName.TURN_END;
            case "pre_send" -> HookEngine.EventName.PRE_SEND;
            case "post_receive" -> HookEngine.EventName.POST_RECEIVE;
            case "pre_tool_use" -> HookEngine.EventName.PRE_TOOL_USE;
            case "post_tool_use" -> HookEngine.EventName.POST_TOOL_USE;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (s == null) return HookEngine.ActionType.COMMAND;`（源码行 262-268）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (s == null) return HookEngine.ActionType.COMMAND;
        return switch (s.toLowerCase()) {
            case "command" -> HookEngine.ActionType.COMMAND;
            case "script" -> HookEngine.ActionType.SCRIPT;
            case "prompt" -> HookEngine.ActionType.PROMPT;
            default -> HookEngine.ActionType.COMMAND;
        };
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void setProgram(Program program) {`（源码行 275-277）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void setProgram(Program program) {
        this.program = program;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public void handleSigint() {`（源码行 283-294）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public void handleSigint() {
        if (streaming) {
            savePartialResponse();
            if (program != null) {
                program.send(new AgentEventMessage());
            }
        } else {
            if (program != null) {
                program.send(new QuitMessage());
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public Command init() {`（源码行 301-303）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public Command init() {
        return Command.checkWindowSize();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public UpdateResult<? extends Model> update(Message msg) {`（源码行 306-412）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public UpdateResult<? extends Model> update(Message msg) {
        if (pendingSingleProviderInit) {
            pendingSingleProviderInit = false;
            initializeProvider();
        }

        // ── Window resize ───────────────────────────────────────────────
        if (msg instanceof WindowSizeMessage wsm) {
            this.width = wsm.width();
            this.height = wsm.height();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String view() {`（源码行 415-421）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String view() {
        return switch (state) {
            case PROVIDER_SELECT -> viewProviderSelect();
            case CHAT -> viewChat();
            case RESUME -> viewResume();
        };
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private UpdateResult<JimiCodeModel> handleProviderSelectKey(KeyPressMessage kpm) {`（源码行 427-447）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private UpdateResult<JimiCodeModel> handleProviderSelectKey(KeyPressMessage kpm) {
        String key = kpm.key();
        return switch (key) {
            case "up", "k" -> {
                if (providerCursor > 0) providerCursor--;
                yield UpdateResult.from(this);
            }
            case "down", "j" -> {
                if (providerCursor < providers.size() - 1) providerCursor++;
                yield UpdateResult.from(this);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void initializeProvider() {`（源码行 449-569）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void initializeProvider() {
        try {
            String workDir = System.getProperty("user.dir");

            memoryManager = new MemoryManager(workDir);

            var env = PromptBuilder.detectEnvironment(selectedProvider.getModel());
            instructionsContent = MemoryManager.loadInstructions(workDir);
            memoryContentField = buildMemorySection();
            var options = new PromptBuilder.BuildOptions(null);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!toolNames.isEmpty()) {`（源码行 570-572）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                            if (!toolNames.isEmpty()) {
                                sb2.append("\nAvailable tools: ").append(String.join(", ", toolNames));
                            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!mcpParts.isEmpty()) {`（源码行 575-579）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                        if (!mcpParts.isEmpty()) {
                            mcpInstructions = "# MCP Server Instructions\n\n"
                                    + "The following MCP servers are connected. Use their tools when the user asks.\n\n"
                                    + String.join("\n\n", mcpParts);
                        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `synchronized (chatMessages) {`（源码行 581-583）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                        synchronized (chatMessages) {
                            chatMessages.add(new ChatMessage("error", "MCP init failed: " + e.getMessage()));
                        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (java.nio.file.Files.isDirectory(skillDir)) {`（源码行 592-594）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            if (java.nio.file.Files.isDirectory(skillDir)) {
                skillCatalog.loadFromDirectory(skillDir);
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `wireSkillsToAgent();`（源码行 596-601）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
            wireSkillsToAgent();

            agent.setHookEngine(hookEngine);
            fireHook(HookEngine.EventName.SESSION_START, null, null);

        } catch (Exception e) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void wireSkillsToAgent() {`（源码行 607-612）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void wireSkillsToAgent() {
        if (skillCatalog == null || cmdRegistry == null) return;
        for (var meta : skillCatalog.list()) {
            registerSkillCommand(meta.name());
        }
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void registerSkillCommand(String name) {`（源码行 614-629）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void registerSkillCommand(String name) {
        if (cmdRegistry.find(name).isPresent()) return;
        var skill = skillCatalog.get(name);
        if (skill.isEmpty()) return;

        var meta = skill.get().meta();
        var cmd = new com.jimicode.command.Command(name, meta.description() + " [skill]",
                              new String[]{}, com.jimicode.command.Command.CommandType.PROMPT, false);

        String captured = name;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String renderBanner() {`（源码行 631-637）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String renderBanner() {
        String modelName = selectedProvider != null ? selectedProvider.getModel() : "";
        String workDir = System.getProperty("user.dir");
        return Styles.banner.render(" /\\_/\\    ") + Styles.bannerDim.render(VERSION) + "\n" +
               Styles.banner.render("( o.o )   ") + Styles.bannerDim.render(modelName) + "\n" +
               Styles.banner.render(" > ^ <    ") + Styles.bannerDim.render(workDir);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String viewProviderSelect() {`（源码行 639-662）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String viewProviderSelect() {
        var sb = new StringBuilder();
        sb.append("\n");
        sb.append(renderBanner());
        sb.append("\n\n");
        sb.append(Styles.selectLabel.render("  Select a Provider"));
        sb.append("\n\n");

        for (int i = 0; i < providers.size(); i++) {
            var p = providers.get(i);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private UpdateResult<JimiCodeModel> handleChatKey(KeyPressMessage kpm) {`（源码行 668-788）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private UpdateResult<JimiCodeModel> handleChatKey(KeyPressMessage kpm) {
        String key = kpm.key();

        // shift+tab: cycle permission mode
        if (key.equals("shift+tab") && !streaming && permChecker != null) {
            var current = permChecker.getMode();
            var next = switch (current) {
                case DEFAULT -> PermissionMode.ACCEPT_EDITS;
                case ACCEPT_EDITS -> PermissionMode.PLAN;
                case PLAN -> PermissionMode.BYPASS;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (key.equals("backspace") || key.equals("ctrl+h")) {`（源码行 791-802）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
                    if (key.equals("backspace") || key.equals("ctrl+h")) {
                        if (!inputBuffer.isEmpty()) inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                        updateAtMenu();
                    } else {
                        char[] runes = kpm.runes();
                        if (runes != null && runes.length > 0) {
                            for (char ch : runes) if (ch >= 32) inputBuffer.append(ch);
                        } else if (key.length() == 1 && key.charAt(0) >= 32) {
                            inputBuffer.append(key.charAt(0));
                        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (key.equals("enter")) {`（源码行 809-831）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (key.equals("enter")) {
            if (inputBuffer.isEmpty()) return UpdateResult.from(this);
            if (streaming) {
                savePartialResponse();
            }
            String text = inputBuffer.toString().trim();
            if (text.startsWith("/")) {
                String[] parts = text.substring(1).split("\\s+", 2);
                String cmdName = parts[0];
                String cmdArgs = parts.length > 1 ? parts[1] : "";
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (!streaming && (key.equals("up") || key.equals("down"))) {`（源码行 834-861）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (!streaming && (key.equals("up") || key.equals("down"))) {
            var entries = historyStore.getEntries();
            if (!entries.isEmpty()) {
                if (key.equals("up")) {
                    if (historyIndex < 0) {
                        historyDraft = inputBuffer.toString();
                        historyIndex = entries.size() - 1;
                    } else if (historyIndex > 0) {
                        historyIndex--;
                    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (key.equals("backspace") || key.equals("ctrl+h")) {`（源码行 864-870）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (key.equals("backspace") || key.equals("ctrl+h")) {
            if (!inputBuffer.isEmpty()) {
                inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                updateSlashMenu();
            }
            return UpdateResult.from(this);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (key.equals("space") || key.equals(" ")) {`（源码行 873-876）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (key.equals("space") || key.equals(" ")) {
            inputBuffer.append(' ');
            return UpdateResult.from(this);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (key.equals("tab")) {`（源码行 879-882）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (key.equals("tab")) {
            inputBuffer.append("    ");
            return UpdateResult.from(this);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (runes != null && runes.length > 0) {`（源码行 886-904）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (runes != null && runes.length > 0) {
            for (char ch : runes) {
                if (ch >= 32) {
                    inputBuffer.append(ch);
                }
            }
            if (inputBuffer.length() == 1 && inputBuffer.charAt(0) == '/') {
                updateSlashMenu();
            } else if (slashMenuOpen) {
                updateSlashMenu();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (key.length() == 1 && key.charAt(0) >= 32) {`（源码行 907-916）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (key.length() == 1 && key.charAt(0) >= 32) {
            inputBuffer.append(key.charAt(0));
            if (key.charAt(0) == '/' && inputBuffer.length() == 1) {
                updateSlashMenu();
            } else if (slashMenuOpen) {
                updateSlashMenu();
            }
            if (key.charAt(0) == '@') updateAtMenu();
            return UpdateResult.from(this);
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void updateSlashMenu() {`（源码行 921-931）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void updateSlashMenu() {
        String text = inputBuffer.toString();
        if (text.startsWith("/") && !text.contains(" ")) {
            String prefix = text.substring(1);
            slashMatches = cmdRegistry.search(prefix);
            slashMenuOpen = !slashMatches.isEmpty();
            slashCursor = 0;
        } else {
            slashMenuOpen = false;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void updateAtMenu() {`（源码行 933-957）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void updateAtMenu() {
        String text = inputBuffer.toString();
        int atIdx = text.lastIndexOf('@');
        if (atIdx < 0) { atMenuOpen = false; return; }
        String prefix = text.substring(atIdx + 1);
        if (prefix.contains(" ")) { atMenuOpen = false; return; }

        var skipDirs = Set.of(".git", "node_modules", ".venv", "__pycache__", ".JimiCode", "build", ".gradle");
        var matches = new ArrayList<String>();
        try {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private UpdateResult<JimiCodeModel> executeSlashCommand(com.jimicode.command.Command cmd, String args) {`（源码行 962-1068）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private UpdateResult<JimiCodeModel> executeSlashCommand(com.jimicode.command.Command cmd, String args) {
        return switch (cmd.type()) {
            case LOCAL -> {
                var ctx = buildCommandContext(args);
                String output = cmdRegistry.execute(cmd.name(), ctx);
                if (output != null && !output.isEmpty()) {
                    chatMessages.add(new ChatMessage("system", output));
                }
                yield UpdateResult.from(this);
            }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private UpdateResult<JimiCodeModel> sendUserMessage() {`（源码行 1090-1145）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private UpdateResult<JimiCodeModel> sendUserMessage() {
        String userText = inputBuffer.toString().trim();
        inputBuffer.setLength(0);
        userScrolled = false;
        scrollOffset = 0;

        if (userText.isEmpty()) {
            return UpdateResult.from(this);
        }

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private UpdateResult<JimiCodeModel> handleAgentEvents() {`（源码行 1151-1262）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private UpdateResult<JimiCodeModel> handleAgentEvents() {
        drainSubAgentProgress();

        if (agentQueue == null) return UpdateResult.from(this);

        var events = new ArrayList<AgentEvent>();
        agentQueue.drainTo(events);

        boolean loopDone = false;

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void fireHook(HookEngine.EventName event, String toolName, Map<String, Object> args) {`（源码行 1264-1268）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void fireHook(HookEngine.EventName event, String toolName, Map<String, Object> args) {
        if (hookEngine == null) return;
        var ctx = new HookEngine.HookContext(event, toolName, args, null, null, null);
        hookEngine.runHooks(ctx);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private String buildMemorySection() {`（源码行 1270-1279）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private String buildMemorySection() {
        if (memoryManager == null) return "";
        var memories = memoryManager.getMemories();
        if (memories.isEmpty()) return "";
        var sb = new StringBuilder("# Auto Memory\n\n");
        for (String mem : memories) {
            sb.append(mem).append("\n\n");
        }
        return sb.toString();
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private void triggerMemoryExtraction() {`（源码行 1281-1285）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private void triggerMemoryExtraction() {
        if (memoryManager == null || client == null) return;
        if (!memoryManager.shouldExtract()) return;
        Thread.startVirtualThread(() -> memoryManager.extract(client, conversation));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/MarkdownRenderer.java`

**类型与边界。** 包 `com.jimicode.tui`；类型 `MarkdownRenderer`；102 行；直接依赖 `java.util.regex.Pattern`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private MarkdownRenderer() {}`（源码行 12-12）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private MarkdownRenderer() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String render(String markdown, int width) {`（源码行 33-93）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String render(String markdown, int width) {
        if (markdown == null || markdown.isEmpty()) return "";

        var sb = new StringBuilder();
        var lines = markdown.split("\n", -1);
        boolean inCodeBlock = false;
        String codeBlockLang = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String renderInline(String text) {`（源码行 95-101）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String renderInline(String text) {
        text = BOLD_PATTERN.matcher(text).replaceAll(BOLD + "$1" + RESET);
        text = ITALIC_PATTERN.matcher(text).replaceAll(ITALIC + "$1" + RESET);
        text = CODE_PATTERN.matcher(text).replaceAll(CYAN + "`$1`" + RESET);
        text = LINK_PATTERN.matcher(text).replaceAll(UNDERLINE + CYAN + "$1" + RESET);
        return text;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/SpinnerVerbs.java`

**类型与边界。** 包 `com.jimicode.tui`；类型 `SpinnerVerbs`；207 行；直接依赖 `java.util.List, java.util.concurrent.ThreadLocalRandom`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private SpinnerVerbs() {}`（源码行 17-17）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private SpinnerVerbs() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String random() {`（源码行 127-129）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String random() {
        return VERBS.get(ThreadLocalRandom.current().nextInt(VERBS.size()));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static List<String> all() {`（源码行 132-134）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static List<String> all() {
        return VERBS;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String pastTense(String verb) {`（源码行 141-158）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String pastTense(String verb) {
        if (verb == null || verb.isEmpty()) {
            return verb;
        }

        // Irregular / special-case mappings
        return switch (verb) {
            case "Thinking"  -> "Thought";
            case "Brewing"   -> "Brewed";
            case "Mewing"    -> "Mewed";
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String convertRegular(String verb) {`（源码行 160-198）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String convertRegular(String verb) {
        if (!verb.endsWith("ing")) {
            return verb + "ed";
        }

        String base = verb.substring(0, verb.length() - 3); // strip "ing"

        // Verbs whose base already ends with 'e' that was dropped before -ing:
        //   e.g. "Composing" -> base "Compos" -> restore 'e' -> "Composed"
        // Detect by checking if the stem looks like it had a silent-e
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean isDoubledConsonant(String base) {`（源码行 201-206）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean isDoubledConsonant(String base) {
        if (base.length() < 2) return false;
        char c1 = base.charAt(base.length() - 1);
        char c2 = base.charAt(base.length() - 2);
        return c1 == c2 && "aeiouy".indexOf(c1) < 0;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/tui/Styles.java`

**类型与边界。** 包 `com.jimicode.tui`；类型 `Styles`；147 行；直接依赖 `com.williamcallahan.tui4j.compat.bubbletea.lipgloss.Style, com.williamcallahan.tui4j.compat.bubbletea.lipgloss.color.ANSI256Color`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private Styles() {}`（源码行 17-17）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private Styles() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String cyan(String s) { return inlineCyan.render(s); }`（源码行 142-142）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String cyan(String s) { return inlineCyan.render(s); }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String dim(String s) { return inlineDim.render(s); }`（源码行 143-143）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String dim(String s) { return inlineDim.render(s); }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String green(String s) { return inlineGreen.render(s); }`（源码行 144-144）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String green(String s) { return inlineGreen.render(s); }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String red(String s) { return inlineRed.render(s); }`（源码行 145-145）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String red(String s) { return inlineRed.render(s); }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String yellow(String s) { return inlineYellow.render(s); }`（源码行 146-146）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String yellow(String s) { return inlineYellow.render(s); }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/AgentWorktree.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `AgentWorktree, Result`；122 行；直接依赖 `java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.time.Instant, java.util.List, java.util.concurrent.TimeUnit, java.util.logging.Logger`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record Result(String worktreePath, String worktreeBranch, String headCommit, String gitRoot) {}`（源码行 24-24）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record Result(String worktreePath, String worktreeBranch, String headCommit, String gitRoot) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private AgentWorktree() {}`（源码行 26-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private AgentWorktree() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static Result create(String slug, String repoRoot, List<String> symlinkDirs) throws Exception {`（源码行 31-63）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static Result create(String slug, String repoRoot, List<String> symlinkDirs) throws Exception {
        SlugValidator.validate(slug);

        Path wtPath = Path.of(repoRoot, ".JimiCode", "worktrees", SlugValidator.flatten(slug));
        String branch = "worktree-" + SlugValidator.flatten(slug);

        // Fast-resume: check if worktree already exists
        if (Files.isDirectory(wtPath)) {
            // Bump mtime to prevent stale cleanup
            Files.setLastModifiedTime(wtPath, java.nio.file.attribute.FileTime.from(Instant.now()));
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean remove(String worktreePath, String worktreeBranch, String gitRoot) {`（源码行 68-93）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean remove(String worktreePath, String worktreeBranch, String gitRoot) {
        if (gitRoot == null || gitRoot.isBlank()) return false;
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "worktree", "remove", "--force", worktreePath);
            pb.directory(Path.of(gitRoot).toFile());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            proc.getInputStream().readAllBytes();
            proc.waitFor(30, TimeUnit.SECONDS);
            if (proc.exitValue() != 0) return false;
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String buildNotice(String parentCwd, String worktreeCwd) {`（源码行 98-107）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String buildNotice(String parentCwd, String worktreeCwd) {
        return "You've inherited the conversation context above from a parent agent working in %s. "
                .formatted(parentCwd)
                + "You are operating in an isolated git worktree at %s — same repository, same relative "
                .formatted(worktreeCwd)
                + "file structure, separate working copy. Paths in the inherited context refer to the "
                + "parent's working directory; translate them to your worktree root. Re-read files before "
                + "editing if the parent may have modified them since they appear in the context. Your "
                + "changes stay in this worktree and will not affect the parent's files.";
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String readHead(String worktreePath) {`（源码行 109-121）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String readHead(String worktreePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(Path.of(worktreePath).toFile());
            pb.redirectErrorStream(false);
            Process proc = pb.start();
            String out = new String(proc.getInputStream().readAllBytes()).strip();
            proc.waitFor(10, TimeUnit.SECONDS);
            return proc.exitValue() == 0 ? out : null;
        } catch (Exception e) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/PostCreationSetup.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `PostCreationSetup`；121 行；直接依赖 `java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.util.List, java.util.logging.Logger`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private PostCreationSetup() {}`（源码行 21-21）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private PostCreationSetup() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void perform(String repoRoot, String worktreePath, List<String> symlinkDirs) {`（源码行 23-28）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void perform(String repoRoot, String worktreePath, List<String> symlinkDirs) {
        copySettingsLocal(repoRoot, worktreePath);
        configureHooksPath(repoRoot, worktreePath);
        symlinkDirectories(repoRoot, worktreePath, symlinkDirs);
        copyWorktreeIncludeFiles(repoRoot, worktreePath);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void copySettingsLocal(String repoRoot, String worktreePath) {`（源码行 30-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void copySettingsLocal(String repoRoot, String worktreePath) {
        Path src = Path.of(repoRoot, ".JimiCode", "settings.local.json");
        Path dst = Path.of(worktreePath, ".JimiCode", "settings.local.json");
        if (!Files.exists(src)) return;
        try {
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst);
        } catch (IOException e) {
            log.fine("Failed to copy settings.local.json: " + e.getMessage());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void configureHooksPath(String repoRoot, String worktreePath) {`（源码行 42-62）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void configureHooksPath(String repoRoot, String worktreePath) {
        String[] candidates = {".husky", ".git/hooks"};
        String hooksPath = null;
        for (String c : candidates) {
            Path p = Path.of(repoRoot, c);
            if (Files.isDirectory(p)) {
                hooksPath = p.toString();
                break;
            }
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void symlinkDirectories(String repoRoot, String worktreePath, List<String> dirs) {`（源码行 64-77）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void symlinkDirectories(String repoRoot, String worktreePath, List<String> dirs) {
        if (dirs == null) return;
        for (String dir : dirs) {
            if (dir.contains("..")) continue;
            Path src = Path.of(repoRoot, dir);
            Path dst = Path.of(worktreePath, dir);
            if (!Files.exists(src) || Files.exists(dst)) continue;
            try {
                Files.createSymbolicLink(dst, src);
            } catch (IOException e) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static void copyWorktreeIncludeFiles(String repoRoot, String worktreePath) {`（源码行 79-110）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static void copyWorktreeIncludeFiles(String repoRoot, String worktreePath) {
        Path includeFile = Path.of(repoRoot, ".worktreeinclude");
        if (!Files.exists(includeFile)) return;
        try {
            List<String> patterns = Files.readAllLines(includeFile).stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .toList();
            if (patterns.isEmpty()) return;

```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static boolean matchesAnyPattern(String path, List<String> patterns) {`（源码行 112-120）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static boolean matchesAnyPattern(String path, List<String> patterns) {
        String baseName = Path.of(path).getFileName().toString();
        for (String p : patterns) {
            String normalized = p.startsWith("/") ? p.substring(1) : p;
            if (normalized.equals(path) || normalized.equals(baseName)) return true;
            if (normalized.endsWith("/") && path.startsWith(normalized)) return true;
        }
        return false;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/SlugValidator.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `SlugValidator`；51 行；直接依赖 `java.util.regex.Pattern`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private SlugValidator() {}`（源码行 19-19）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private SlugValidator() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void validate(String slug) {`（源码行 21-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void validate(String slug) {
        if (slug == null || slug.isEmpty()) {
            throw new IllegalArgumentException("Invalid worktree name: cannot be empty");
        }
        if (slug.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid worktree name: must be %d characters or fewer (got %d)"
                            .formatted(MAX_LENGTH, slug.length()));
        }
        for (String segment : slug.split("/", -1)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String flatten(String slug) {`（源码行 44-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String flatten(String slug) {
        return slug.replace('/', '+');
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String branchName(String slug) {`（源码行 48-50）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String branchName(String slug) {
        return "worktree-" + flatten(slug);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/StaleCleanup.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `StaleCleanup`；139 行；直接依赖 `java.io.IOException, java.nio.file.Files, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes, java.time.Instant, java.util.List, java.util.concurrent.ScheduledExecutorService, java.util.concurrent.TimeUnit, java.util.logging.Logger, java.util.regex.Pattern`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private StaleCleanup() {}`（源码行 35-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private StaleCleanup() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static boolean isEphemeral(String slug) {`（源码行 37-39）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static boolean isEphemeral(String slug) {
        return EPHEMERAL_PATTERNS.stream().anyMatch(p -> p.matcher(slug).matches());
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static int cleanup(String repoRoot, Instant cutoff) {`（源码行 45-92）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static int cleanup(String repoRoot, Instant cutoff) {
        Path dir = Path.of(repoRoot, ".JimiCode", "worktrees");
        if (!Files.isDirectory(dir)) return 0;

        String currentPath = null;
        var session = WorktreeSessionStore.getCurrentSession();
        if (session != null) currentPath = session.worktreePath();

        int removed = 0;
        try (Stream<Path> entries = Files.list(dir)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `if (intervalSeconds <= 0) return;`（源码行 103-114）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
        if (intervalSeconds <= 0) return;
        executor.scheduleAtFixedRate(() -> {
            try {
                Instant cutoff = Instant.now().minusSeconds((long) cutoffHours * 3600);
                int removed = cleanup(repoRoot, cutoff);
                if (removed > 0) {
                    log.fine("Cleaned up " + removed + " stale worktree(s)");
                }
            } catch (Exception e) {
                log.fine("Stale cleanup error: " + e.getMessage());
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String runGitQuiet(String cwd, String... args) {`（源码行 117-138）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String runGitQuiet(String cwd, String... args) {
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = "git";
            System.arraycopy(args, 0, cmd, 1, args.length);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(Path.of(cwd).toFile());
            pb.environment().put("GIT_TERMINAL_PROMPT", "0");
            pb.environment().put("GIT_ASKPASS", "");
            pb.redirectErrorStream(false);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/WorktreeChanges.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `WorktreeChanges, ChangeSummary`；92 行；直接依赖 `java.nio.file.Path, java.util.concurrent.TimeUnit`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record ChangeSummary(int changedFiles, int commits) {}`（源码行 16-16）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record ChangeSummary(int changedFiles, int commits) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private WorktreeChanges() {}`（源码行 18-18）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private WorktreeChanges() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static boolean hasChanges(String worktreePath, String headCommit) {`（源码行 24-35）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static boolean hasChanges(String worktreePath, String headCommit) {
        try {
            String statusOut = runGit(worktreePath, "status", "--porcelain");
            if (statusOut == null || !statusOut.isBlank()) return true;

            String revOut = runGit(worktreePath, "rev-list", "--count", headCommit + "..HEAD");
            if (revOut == null) return true;
            return Integer.parseInt(revOut.strip()) > 0;
        } catch (Exception e) {
            return true; // fail-closed
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static ChangeSummary countChanges(String worktreePath, String originalHeadCommit) {`（源码行 42-66）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static ChangeSummary countChanges(String worktreePath, String originalHeadCommit) {
        if (originalHeadCommit == null || originalHeadCommit.isBlank()) {
            return null; // fail-closed: no baseline
        }

        String statusOut = runGit(worktreePath, "status", "--porcelain");
        if (statusOut == null) return null;

        int changedFiles = 0;
        for (String line : statusOut.split("\n")) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String runGit(String cwd, String... args) {`（源码行 68-91）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String runGit(String cwd, String... args) {
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = "git";
            System.arraycopy(args, 0, cmd, 1, args.length);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(Path.of(cwd).toFile());
            pb.environment().put("GIT_TERMINAL_PROMPT", "0");
            pb.environment().put("GIT_ASKPASS", "");
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/WorktreeManager.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `WorktreeManager, WorktreeInfo`；247 行；直接依赖 `java.io.IOException, java.io.InputStream, java.nio.file.Files, java.nio.file.Path, java.time.Instant, java.util.ArrayList, java.util.LinkedHashMap, java.util.List, java.util.Map, java.util.Optional`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `public record WorktreeInfo(String path, String branch, Instant createdAt) {}`（源码行 30-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public record WorktreeInfo(String path, String branch, Instant createdAt) {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public WorktreeManager(String projectRoot, List<String> symlinkDirs, int staleCutoffHours) {`（源码行 38-42）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public WorktreeManager(String projectRoot, List<String> symlinkDirs, int staleCutoffHours) {
        this.projectRoot = projectRoot;
        this.symlinkDirs = symlinkDirs != null ? symlinkDirs : List.of();
        this.staleCutoffHours = staleCutoffHours > 0 ? staleCutoffHours : 24;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public String getProjectRoot() { return projectRoot; }`（源码行 44-44）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public String getProjectRoot() { return projectRoot; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public List<String> getSymlinkDirs() { return symlinkDirs; }`（源码行 45-45）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public List<String> getSymlinkDirs() { return symlinkDirs; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public int getStaleCutoffHours() { return staleCutoffHours; }`（源码行 46-46）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public int getStaleCutoffHours() { return staleCutoffHours; }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized WorktreeInfo create(String branch, Path targetDir) throws Exception {`（源码行 57-71）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized WorktreeInfo create(String branch, Path targetDir) throws Exception {
        Path wtDir = targetDir != null
                ? targetDir
                : Path.of(projectRoot, ".JimiCode", "worktrees", branch);

        // -B (uppercase) resets any orphan branch left behind by a removed worktree
        String output = runGit(projectRoot, "git", "worktree", "add", "-B", branch, wtDir.toString());

        // Post-creation setup: settings, hooks, symlinks, .worktreeinclude
        PostCreationSetup.perform(projectRoot, wtDir.toString(), symlinkDirs);
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void remove(String branch) throws Exception {`（源码行 76-84）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void remove(String branch) throws Exception {
        WorktreeInfo info = worktrees.get(branch);
        if (info == null) {
            throw new IllegalArgumentException("worktree not found: " + branch);
        }

        runGit(projectRoot, "git", "worktree", "remove", info.path(), "--force");
        worktrees.remove(branch);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized List<WorktreeInfo> list() {`（源码行 92-103）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized List<WorktreeInfo> list() {
        try {
            String output = runGit(projectRoot, "git", "worktree", "list", "--porcelain");
            List<WorktreeInfo> result = parsePorcelain(output);
            if (!result.isEmpty()) {
                return result;
            }
        } catch (Exception ignored) {
            // fall through to in-memory map
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized Optional<WorktreeInfo> get(String branch) {`（源码行 108-110）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized Optional<WorktreeInfo> get(String branch) {
        return Optional.ofNullable(worktrees.get(branch));
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized int cleanupStale(int cutoffHours) {`（源码行 118-138）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized int cleanupStale(int cutoffHours) {
        int hours = cutoffHours > 0 ? cutoffHours : staleCutoffHours;
        Instant cutoff = Instant.now().minusSeconds((long) hours * 3600);
        int removed = 0;

        var it = worktrees.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            WorktreeInfo info = entry.getValue();
            if (info.createdAt().isBefore(cutoff)) {
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public synchronized void removeAll() {`（源码行 143-154）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public synchronized void removeAll() {
        var it = worktrees.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            try {
                runGit(projectRoot, "git", "worktree", "remove", entry.getValue().path(), "--force");
            } catch (Exception ignored) {
                // best-effort
            }
            it.remove();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static String detectChanges(String worktreePath) throws Exception {`（源码行 162-182）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static String detectChanges(String worktreePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("git", "diff", "--stat");
        pb.directory(Path.of(worktreePath).toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static String runGit(String workDir, String... command) throws Exception {`（源码行 186-206）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static String runGit(String workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(Path.of(workDir).toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output;
        try (InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes());
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static List<WorktreeInfo> parsePorcelain(String output) {`（源码行 217-246）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static List<WorktreeInfo> parsePorcelain(String output) {
        List<WorktreeInfo> result = new ArrayList<>();
        String currentPath = null;
        String currentBranch = null;

        for (String line : output.split("\n")) {
            if (line.startsWith("worktree ")) {
                currentPath = line.substring("worktree ".length()).strip();
            } else if (line.startsWith("branch ")) {
                String ref = line.substring("branch ".length()).strip();
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/WorktreeSession.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `WorktreeSession`；26 行；直接依赖 `com.fasterxml.jackson.annotation.JsonIgnoreProperties, com.fasterxml.jackson.annotation.JsonProperty`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- 未识别到常规方法签名；重点检查 record 组件、重写方法、lambda、静态初始化块和注解处理器生成代码。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？

### `jimi-code/src/main/java/com/jimicode/worktree/WorktreeSessionStore.java`

**类型与边界。** 包 `com.jimicode.worktree`；类型 `WorktreeSessionStore`；61 行；直接依赖 `com.fasterxml.jackson.databind.ObjectMapper, java.io.IOException, java.nio.file.Files, java.nio.file.Path`。先判断它是 API、实现、数据载体、适配器还是测试辅助，决定阅读方式。

**方法级证据。**
- `private WorktreeSessionStore() {}`（源码行 22-22）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private WorktreeSessionStore() {}
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static WorktreeSession getCurrentSession() {`（源码行 24-26）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static WorktreeSession getCurrentSession() {
        return currentSession;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void restoreSession(WorktreeSession session) {`（源码行 28-30）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void restoreSession(WorktreeSession session) {
        currentSession = session;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static void save(String repoRoot, WorktreeSession session) throws IOException {`（源码行 32-40）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static void save(String repoRoot, WorktreeSession session) throws IOException {
        Path path = sessionPath(repoRoot);
        if (session == null) {
            Files.deleteIfExists(path);
            return;
        }
        Files.createDirectories(path.getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), session);
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `public static WorktreeSession load(String repoRoot) {`（源码行 42-52）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    public static WorktreeSession load(String repoRoot) {
        Path path = sessionPath(repoRoot);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return MAPPER.readValue(path.toFile(), WorktreeSession.class);
        } catch (IOException e) {
            return null;
        }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `static void clearForTesting() {`（源码行 54-56）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    static void clearForTesting() {
        currentSession = null;
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
- `private static Path sessionPath(String repoRoot) {`（源码行 58-60）：先确认参数校验与返回类型，再观察副作用。源码片段：
```java
    private static Path sessionPath(String repoRoot) {
        return Path.of(repoRoot, ".JimiCode", "worktree_session.json");
    }
```
  调用关系线索：该方法中出现的类型/方法名需要用 IDE 的“查找用法”反查；若返回 `Mono`/`Flux`，沿订阅边界检查取消、重试和错误传播；若读写文件/网络，检查超时、关闭和幂等。
**学习问题。** 谁创建此对象？状态是否可变？异常由谁捕获？线程模型是什么？替换该类需要实现哪个接口或保持哪个序列化格式？



## 深入调用链练习

对每条工具链画出四张图：对象创建图、一次 turn 的时序图、异常传播图、文件状态变化图。特别核对 toolUseId 是否贯穿模型 delta、工具执行、压缩恢复和历史持久化。
