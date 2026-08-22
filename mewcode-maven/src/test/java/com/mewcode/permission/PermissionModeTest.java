package com.mewcode.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionModeTest {

    @Test
    void parsesConfiguredAliases() {
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromConfig(null));
        assertEquals(PermissionMode.ACCEPT_EDITS, PermissionMode.fromConfig("accept_edits"));
        assertEquals(PermissionMode.PLAN, PermissionMode.fromConfig("plan"));
        assertEquals(PermissionMode.BYPASS, PermissionMode.fromConfig("bypassPermissions"));
    }

    @Test
    void rejectsUnknownMode() {
        assertThrows(IllegalArgumentException.class, () -> PermissionMode.fromConfig("unsafe"));
    }
}
