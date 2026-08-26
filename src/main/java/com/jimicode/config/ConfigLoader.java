// 来源：公众号@小林coding
// 后端八股网站：xiaolincoding.com
// Agent网站：xiaolinnote.com
// 简历模版：jianli.xiaolinnote.com

package com.jimicode.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigLoader {

    private static final Set<String> VALID_PROTOCOLS = Set.of("anthropic", "openai", "openai-compat");

    /** Default endpoints used when building a provider purely from .env / env vars. */
    private static final Map<String, String> DEFAULT_BASE_URLS = Map.of(
            "anthropic", "https://api.anthropic.com",
            "openai", "https://api.openai.com",
            "openai-compat", "https://api.openai.com"
    );

    /** Default models used when building a provider purely from .env / env vars. */
    private static final Map<String, String> DEFAULT_MODELS = Map.of(
            "anthropic", "claude-sonnet-4-6",
            "openai", "gpt-4o",
            "openai-compat", "gpt-4o"
    );

    public static AppConfig load(String path) throws ConfigException {
        // Load .env first so values defined there (e.g. JimiCode_CONFIG,
        // ANTHROPIC_API_KEY / OPENAI_API_KEY) participate in startup.
        DotenvLoader.load();

        if (path != null && !path.isEmpty()) {
            var cfg = loadSingleFile(Path.of(path));
            validate(cfg);
            return cfg;
        }

        // A .env file may also point at the config file location.
        String envPath = DotenvLoader.get("JimiCode_CONFIG");
        if (envPath != null && !envPath.isBlank()) {
            var cfg = loadSingleFile(Path.of(envPath));
            validate(cfg);
            return cfg;
        }

        var cwd = Path.of(System.getProperty("user.dir"));
        var home = Path.of(System.getProperty("user.home"));
        var candidates = List.of(
                home.resolve(".JimiCode").resolve("config.yaml"),
                cwd.resolve(".JimiCode").resolve("config.yaml"),
                cwd.resolve(".JimiCode").resolve("config.local.yaml")
        );

        AppConfig merged = null;
        for (var p : candidates) {
            if (!Files.exists(p)) continue;
            var layer = loadSingleFile(p);
            if (merged == null) {
                merged = layer;
            } else {
                mergeConfig(merged, layer);
            }
        }

        if (merged == null) {
            // No config file at all: if an API key is available from the
            // environment or a .env file, build a default provider so the app
            // can start without writing any config.yaml.
            AppConfig fromEnv = buildFromEnv();
            if (fromEnv != null) {
                validate(fromEnv);
                return fromEnv;
            }
            throw new ConfigException(
                    "No config file found. Expected .JimiCode/config.yaml in project or ~/.JimiCode/config.yaml");
        }

        validate(merged);
        return merged;
    }

    /**
     * Build a minimal default {@link AppConfig} from environment / .env
     * variables so JimiCode can start without a config.yaml file.
     *
     * <p>Recognized variables (all optional):</p>
     * <ul>
     *   <li>{@code JimiCode_API_KEY} — explicit key; falls back to
     *       {@code ANTHROPIC_API_KEY} then {@code OPENAI_API_KEY}</li>
     *   <li>{@code JimiCode_PROTOCOL} — one of {@code anthropic} /
     *       {@code openai} / {@code openai-compat}; auto-detected from the key
     *       name when absent</li>
     *   <li>{@code JimiCode_BASE_URL} — defaults to the official endpoint</li>
     *   <li>{@code JimiCode_MODEL} — defaults per protocol</li>
     * </ul>
     *
     * @return the built config, or {@code null} when no API key is available
     */
    static AppConfig buildFromEnv() throws ConfigException {
        String protocol = blankToNull(DotenvLoader.get("JimiCode_PROTOCOL"));
        if (protocol == null) {
            protocol = DotenvLoader.has("ANTHROPIC_API_KEY") ? "anthropic"
                    : DotenvLoader.has("OPENAI_API_KEY") ? "openai"
                    : null;
        }
        if (protocol == null) return null;
        if (!VALID_PROTOCOLS.contains(protocol)) {
            throw new ConfigException(
                    "JimiCode_PROTOCOL invalid protocol '%s', must be one of: anthropic, openai, openai-compat"
                            .formatted(protocol));
        }

        String apiKey = blankToNull(DotenvLoader.get("JimiCode_API_KEY"));
        if (apiKey == null) apiKey = blankToNull(DotenvLoader.get("ANTHROPIC_API_KEY"));
        if (apiKey == null) apiKey = blankToNull(DotenvLoader.get("OPENAI_API_KEY"));
        if (apiKey == null) {
            throw new ConfigException("No config file found and no API key available. " +
                    "Set JimiCode_API_KEY or ANTHROPIC_API_KEY / OPENAI_API_KEY in the environment or a .env file.");
        }

        var p = new ProviderConfig();
        p.setName("env-default");
        p.setProtocol(protocol);
        String baseUrl = blankToNull(DotenvLoader.get("JimiCode_BASE_URL"));
        p.setBaseUrl(baseUrl != null ? baseUrl : DEFAULT_BASE_URLS.get(protocol));
        String model = blankToNull(DotenvLoader.get("JimiCode_MODEL"));
        p.setModel(model != null ? model : DEFAULT_MODELS.get(protocol));
        p.setApiKey(apiKey);

        var cfg = new AppConfig();
        cfg.setProviders(List.of(p));
        return cfg;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

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
        var loaderOptions = new LoaderOptions();
        var constructor = new Constructor(AppConfig.class, loaderOptions);
        constructor.setPropertyUtils(new SnakeCasePropertyUtils());
        var yaml = new Yaml(constructor);
        AppConfig cfg;
        try {
            cfg = yaml.load(content);
        } catch (Exception e) {
            throw new ConfigException("Failed to parse config " + configPath + ": " + e.getMessage());
        }
        if (cfg == null) cfg = new AppConfig();
        return cfg;
    }

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
            var byName = new java.util.LinkedHashMap<String, Integer>();
            for (int i = 0; i < servers.size(); i++) {
                byName.put(servers.get(i).getName(), i);
            }
            for (var s : override.getMcpServers()) {
                var idx = byName.get(s.getName());
                if (idx != null) {
                    servers.set(idx, s);
                } else {
                    servers.add(s);
                    byName.put(s.getName(), servers.size() - 1);
                }
            }
            base.setMcpServers(servers);
        }
        if (override.getHooks() != null) {
            var hooks = base.getHooks() != null
                    ? new ArrayList<>(base.getHooks()) : new ArrayList<HookConfig>();
            hooks.addAll(override.getHooks());
            base.setHooks(hooks);
        }
    }

    private static void validate(AppConfig cfg) throws ConfigException {
        if (cfg == null || cfg.getProviders() == null || cfg.getProviders().isEmpty()) {
            throw new ConfigException("At least one provider must be configured");
        }

        for (int i = 0; i < cfg.getProviders().size(); i++) {
            var p = cfg.getProviders().get(i);
            if (p == null) {
                throw new ConfigException("Provider #%d must not be null".formatted(i + 1));
            }
            var missing = new ArrayList<String>();

            if (isBlank(p.getName())) missing.add("name");
            if (isBlank(p.getProtocol())) missing.add("protocol");
            if (isBlank(p.getBaseUrl())) missing.add("base_url");
            if (isBlank(p.getModel())) missing.add("model");

            if (!missing.isEmpty()) {
                throw new ConfigException(
                        "Provider #%d: missing fields: %s".formatted(i + 1, String.join(", ", missing))
                );
            }

            if (!VALID_PROTOCOLS.contains(p.getProtocol())) {
                throw new ConfigException(
                        "Provider #%d: invalid protocol '%s', must be one of: anthropic, openai, openai-compat"
                                .formatted(i + 1, p.getProtocol())
                );
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static class SnakeCasePropertyUtils extends PropertyUtils {

        @Override
        public Property getProperty(Class<?> type, String name) {
            String camel = snakeToCamel(name);
            return super.getProperty(type, camel);
        }

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
                }
            }
            return sb.toString();
        }
    }

    public static class ConfigException extends Exception {
        public ConfigException(String message) {
            super(message);
        }
    }
}
