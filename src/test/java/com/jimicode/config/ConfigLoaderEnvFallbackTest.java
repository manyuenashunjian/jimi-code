package com.jimicode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderEnvFallbackTest {

    @Test
    void buildsDefaultProviderFromDotenvOnly(@TempDir Path dir) throws Exception {
        String apiKey = "test-key-" + System.nanoTime();
        Path file = dir.resolve(".env");
        Files.writeString(file,
                "JimiCode_PROTOCOL=anthropic\n"
                        + "JimiCode_API_KEY=" + apiKey + "\n"
                        + "JimiCode_MODEL=my-test-model\n"
                        + "JimiCode_BASE_URL=https://api.example.com\n");

        DotenvLoader.reset();
        DotenvLoader.loadFrom(file);

        AppConfig cfg = ConfigLoader.buildFromEnv();
        assertNotNull(cfg);
        assertEquals(1, cfg.getProviders().size());
        var p = cfg.getProviders().get(0);
        assertEquals("anthropic", p.getProtocol());
        assertEquals(apiKey, p.getApiKey());
        assertEquals("my-test-model", p.getModel());
        assertEquals("https://api.example.com", p.getBaseUrl());
    }

    @Test
    void returnsNullWhenNoProtocolAndNoApiKey(@TempDir Path dir) throws Exception {
        DotenvLoader.reset();
        Path file = dir.resolve(".env");
        Files.writeString(file, "SOME_OTHER_VAR=value\n");
        DotenvLoader.loadFrom(file);

        assertNull(ConfigLoader.buildFromEnv());
    }

    @Test
    void throwsWhenProtocolSetButApiKeyMissing(@TempDir Path dir) throws IOException {
        DotenvLoader.reset();
        Path file = dir.resolve(".env");
        Files.writeString(file, "JimiCode_PROTOCOL=anthropic\n");
        DotenvLoader.loadFrom(file);

        var error = assertThrows(ConfigLoader.ConfigException.class,
                () -> ConfigLoader.buildFromEnv());
        assertTrue(error.getMessage().contains("no API key available"));
    }

    @Test
    void rejectsInvalidProtocol(@TempDir Path dir) throws IOException {
        DotenvLoader.reset();
        Path file = dir.resolve(".env");
        Files.writeString(file,
                "JimiCode_PROTOCOL=bogus\nJimiCode_API_KEY=test-key\n");
        DotenvLoader.loadFrom(file);

        var error = assertThrows(ConfigLoader.ConfigException.class,
                () -> ConfigLoader.buildFromEnv());
        assertTrue(error.getMessage().contains("invalid protocol"));
    }
}
