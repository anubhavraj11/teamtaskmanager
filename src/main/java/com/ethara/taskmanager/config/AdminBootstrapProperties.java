package com.ethara.taskmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.bootstrap.admin")
public record AdminBootstrapProperties(
    String fullName,
    String email,
    String password
) {
}
