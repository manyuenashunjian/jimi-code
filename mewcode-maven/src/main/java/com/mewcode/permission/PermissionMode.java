// 来源：公众号@小林coding
// 后端八股网站：xiaolincoding.com
// Agent网站：xiaolinnote.com
// 简历模版：jianli.xiaolinnote.com


package com.jimicode.permission;

import com.jimicode.tool.ToolCategory;

import java.util.Locale;

public enum PermissionMode {

    DEFAULT,
    ACCEPT_EDITS,
    PLAN,
    BYPASS;

    public static PermissionMode fromConfig(String value) {
        if (value == null || value.isBlank()) return DEFAULT;

        return switch (value.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "")) {
            case "default" -> DEFAULT;
            case "acceptedits" -> ACCEPT_EDITS;
            case "plan" -> PLAN;
            case "bypass", "bypasspermissions" -> BYPASS;
            default -> throw new IllegalArgumentException("Unknown permission mode: " + value);
        };
    }

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
            case PLAN -> DEFAULT.decide(category);
            case BYPASS -> Decision.ALLOW;
        };
    }

    public enum Decision {
        ALLOW, DENY, ASK
    }
}
