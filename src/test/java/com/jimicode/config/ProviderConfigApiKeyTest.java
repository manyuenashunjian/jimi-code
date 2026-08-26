package com.jimicode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the api-key resolution in {@link ProviderConfig}:
 * <ol>
 *   <li>{@code api_key} written in the project config file is parsed and wins;</li>
 *   <li>an empty config value falls back to the protocol's environment variable;</li>
 *   <li>no source at all resolves to "" (callers turn that into an auth error).</li>
 * </ol>
 */
class ProviderConfigApiKeyTest {

    @Test
    void readsApiKeyFromConfigFile(@TempDir Path tempDir) throws Exception {
        Path config = tempDir.resolve("config.yaml");
        Files.writeString(config, """
                providers:
                  - name: test-provider
                    protocol: openai-compat
                    base_url: https://example.com/v1
                    model: qwen-max
                    api_key: sk-test-from-config

                permission_mode: default
                mcp_servers: []
                hooks: []
                """);

        AppConfig cfg = ConfigLoader.load(config.toString());
        ProviderConfig provider = cfg.getProviders().get(0);

        assertEquals("sk-test-from-config", provider.getApiKey());
        assertEquals("sk-test-from-config", provider.resolvedApiKey());
    }

    @Test
    void configApiKeyWinsOverEnvironmentFallback() {
        var provider = new ProviderConfig();
        provider.setProtocol("openai");
        provider.setApiKey("sk-from-config");

        assertEquals("sk-from-config", provider.resolvedApiKey());
    }

    @Test
    void emptyConfigApiKeyFallsBackToEnvironmentVariable() {
        var provider = new ProviderConfig();
        provider.setProtocol("openai");

        String env = System.getenv("OPENAI_API_KEY");
        assertEquals(env == null ? "" : env, provider.resolvedApiKey());
    }

    @Test
    void missingEverythingResolvesToEmptyString() {
        var provider = new ProviderConfig();
        provider.setProtocol("anthropic");

        assertEquals("", provider.resolvedApiKey());
    }
}
