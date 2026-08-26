package com.jimicode.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal, dependency-free .env loader.
 *
 * <p>JimiCode deliberately avoids pulling in a third-party dotenv library.
 * This loader reads simple {@code KEY=VALUE} lines from local {@code .env}
 * files and exposes them as a fallback for the real process environment.
 * Lookup order inside {@link #get(String)} is:</p>
 *
 * <ol>
 *   <li>the real environment variable ({@link System#getenv(String)}) — always wins;</li>
 *   <li>a value from one of the loaded {@code .env} files.</li>
 * </ol>
 *
 * <p>Candidate files are loaded lazily once, with later files overriding
 * earlier ones on the same key:</p>
 *
 * <ol>
 *   <li>{@code ~/.JimiCode/.env}</li>
 *   <li>{@code <cwd>/.JimiCode/.env}</li>
 *   <li>{@code <cwd>/.env}</li>
 * </ol>
 */
public final class DotenvLoader {

    private static final Map<String, String> VALUES = new ConcurrentHashMap<>();
    private static volatile boolean loaded;
    private static volatile Path loadedFrom;

    private DotenvLoader() {
    }

    /** Resolve a variable: real environment first, then .env fallback. */
    public static String get(String key) {
        load();
        String env = System.getenv(key);
        return env != null ? env : VALUES.get(key);
    }

    /** True when either the real environment or a .env file provides the key. */
    public static boolean has(String key) {
        load();
        return System.getenv(key) != null || VALUES.containsKey(key);
    }

    /** Idempotent lazy load of the .env candidate chain. */
    public static synchronized void load() {
        if (loaded) return;
        var home = Path.of(System.getProperty("user.home"));
        var cwd = Path.of(System.getProperty("user.dir"));
        for (var p : List.of(
                home.resolve(".JimiCode").resolve(".env"),
                cwd.resolve(".JimiCode").resolve(".env"),
                cwd.resolve(".env"))) {
            if (!Files.isRegularFile(p)) continue;
            parseInto(p, VALUES);
            loadedFrom = p;
        }
        loaded = true;
    }

    /** Path of the last .env file that contributed a value, or null. */
    public static Path loadedFrom() {
        return loadedFrom;
    }

    /** Load a specific .env file into the shared table (test hook). */
    static void loadFrom(Path file) {
        if (file != null && Files.isRegularFile(file)) {
            parseInto(file, VALUES);
        }
    }

    /** Test hook: clear all state. */
    static synchronized void reset() {
        VALUES.clear();
        loaded = false;
        loadedFrom = null;
    }

    /** Parse {@code KEY=VALUE} lines (package-private for tests). */
    static void parseInto(Path file, Map<String, String> target) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            return;
        }
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            // Optional shell-style "export " prefix.
            if (line.startsWith("export ")) line = line.substring("export ".length()).strip();
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).strip();
            if (key.isBlank()) continue;
            target.put(key, stripQuotes(line.substring(eq + 1).strip()));
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
