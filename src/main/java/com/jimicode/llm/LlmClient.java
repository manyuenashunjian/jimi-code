// 来源：公众号@小林coding
// 后端八股网站：xiaolincoding.com
// Agent网站：xiaolinnote.com
// 简历模版：jianli.xiaolinnote.com


package com.jimicode.llm;

import com.jimicode.config.ProviderConfig;
import com.jimicode.conversation.ConversationManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public interface LlmClient {

    BlockingQueue<StreamEvent> stream(ConversationManager conv, List<Map<String, Object>> tools);

    default void setMaxOutputTokens(int tokens) {}

    static LlmClient create(ProviderConfig cfg, String systemPrompt) {
        return switch (cfg.getProtocol()) {
            case "anthropic" -> new AnthropicClient(cfg, systemPrompt);
            case "openai" -> new OpenAiClient(cfg, systemPrompt);
            case "openai-compat" -> new OpenAiCompatClient(cfg, systemPrompt);

            default -> throw new IllegalArgumentException("Unknown protocol: " + cfg.getProtocol());
        };
    }
}
