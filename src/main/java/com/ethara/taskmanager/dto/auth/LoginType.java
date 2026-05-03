package com.ethara.taskmanager.dto.auth;

import com.ethara.taskmanager.entity.enums.Role;
import com.ethara.taskmanager.exception.BadRequestException;
import java.util.Locale;

public enum LoginType {
    ADMIN(Role.ADMIN),
    MEMBER(Role.MEMBER);

    private final Role requiredRole;

    LoginType(Role requiredRole) {
        this.requiredRole = requiredRole;
    }

    public Role getRequiredRole() {
        return requiredRole;
    }

    public String getLabel() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LoginType from(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Login type is required");
        }

        try {
            return LoginType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Login type must be either admin or member");
        }
    }
}
