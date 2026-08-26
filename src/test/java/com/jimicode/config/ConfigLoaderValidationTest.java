package com.jimicode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderValidationTest {

    @Test
    void rejectsEmptyProviders(@TempDir Path tempDir) throws IOException {
        Path config = tempDir.resolve("config.yaml");
        Files.writeString(config, "providers: []\n");

        var error = assertThrows(ConfigLoader.ConfigException.class,
                () -> ConfigLoader.load(config.toString()));

        assertTrue(error.getMessage().contains("At least one provider"));
    }

    @Test
    void rejectsNullProviderEntry(@TempDir Path tempDir) throws IOException {
        Path config = tempDir.resolve("config.yaml");
        Files.writeString(config, "providers:\n  -\n");

        var error = assertThrows(ConfigLoader.ConfigException.class,
                () -> ConfigLoader.load(config.toString()));

        assertTrue(error.getMessage().contains("must not be null"));
    }
}
