package com.jimicode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotenvLoaderTest {

    @Test
    void parsesSimpleKeyValue(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(".env");
        Files.writeString(file, "FOO=bar\nBAZ=qux\n");

        Map<String, String> out = new HashMap<>();
        DotenvLoader.parseInto(file, out);

        assertEquals("bar", out.get("FOO"));
        assertEquals("qux", out.get("BAZ"));
    }

    @Test
    void skipsCommentsAndBlankLines(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(".env");
        Files.writeString(file, "# comment\n\n  \nFOO=bar\n");

        Map<String, String> out = new HashMap<>();
        DotenvLoader.parseInto(file, out);

        assertEquals("bar", out.get("FOO"));
        assertFalse(out.containsKey("comment"));
    }

    @Test
    void stripsQuotesAndExportPrefix(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(".env");
        Files.writeString(file, "export A=\"quoted\"\nB='single'\n");

        Map<String, String> out = new HashMap<>();
        DotenvLoader.parseInto(file, out);

        assertEquals("quoted", out.get("A"));
        assertEquals("single", out.get("B"));
    }

    @Test
    void laterFileOverridesEarlierOnSameKey(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.env");
        Path b = dir.resolve("b.env");
        Files.writeString(a, "FOO=first\n");
        Files.writeString(b, "FOO=second\n");

        Map<String, String> out = new HashMap<>();
        DotenvLoader.parseInto(a, out);
        DotenvLoader.parseInto(b, out);

        assertEquals("second", out.get("FOO"));
    }

    @Test
    void missingFileIsIgnored(@TempDir Path dir) {
        Map<String, String> out = new HashMap<>();
        DotenvLoader.parseInto(dir.resolve("does-not-exist.env"), out);
        assertTrue(out.isEmpty());
    }

    @Test
    void getFallsBackToDotenvValueWhenEnvMissing(@TempDir Path dir) throws IOException {
        String key = "JimiCode_TEST_DOTENV_ONLY_KEY";
        // Guard against the (unlikely) case where the real environment has it.
        if (System.getenv(key) != null) return;

        Path file = dir.resolve(".env");
        Files.writeString(file, key + "=from-dotenv\n");

        DotenvLoader.loadFrom(file);

        assertEquals("from-dotenv", DotenvLoader.get(key));
        assertTrue(DotenvLoader.has(key));
    }

    @Test
    void getPrefersRealEnvironmentOverDotenv(@TempDir Path dir) throws IOException {
        String key = "PATH"; // a well-known real environment variable
        String real = System.getenv(key);
        if (real == null) return;

        Path file = dir.resolve(".env");
        Files.writeString(file, key + "=should-not-win\n");
        DotenvLoader.loadFrom(file);

        assertEquals(real, DotenvLoader.get(key));
    }
}
