// 来源：公众号@小林coding
// 后端八股网站：xiaolincoding.com
// Agent网站：xiaolinnote.com
// 简历模版：jianli.xiaolinnote.com


package com.jimicode;

import com.jimicode.config.AppConfig;
import com.jimicode.config.ConfigLoader;
import com.jimicode.permission.PermissionMode;
import com.jimicode.tui.JimiCodeModel;

import com.williamcallahan.tui4j.compat.bubbletea.Program;

import java.util.List;
import sun.misc.Signal;

public class JimiCode {

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
        }

        AppConfig config;
        try {
            config = ConfigLoader.load(configPath);
        } catch (ConfigLoader.ConfigException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }

        PermissionMode permissionMode;
        try {
            permissionMode = PermissionMode.fromConfig(config.getPermissionMode());
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }

        var model = new JimiCodeModel(
                config.getProviders(),
                config.getMcpServers() != null ? config.getMcpServers() : List.of(),
                config.getHooks() != null ? config.getHooks() : List.of(),
                permissionMode
        );

        var program = new Program(model);

        model.setProgram(program);

        System.out.print("\033[?25l");

        // Re-register SIGINT handler after TUI4J's program.run() starts,
        // overriding the framework's default quit-on-SIGINT behavior.
        Thread.ofVirtual().start(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            try {
                Signal.handle(new Signal("INT"), sig -> model.handleSigint());
            } catch (IllegalArgumentException ignored) {}
        });

        try {
            program.run();
        } finally {
            System.out.print("\033[?25h");
            System.out.flush();
        }
    }
}
