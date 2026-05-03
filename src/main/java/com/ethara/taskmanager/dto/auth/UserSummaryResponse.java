package com.ethara.taskmanager.dto.auth;

import com.ethara.taskmanager.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserSummaryResponse {

    private final Long id;
    private final String fullName;
    private final String email;
    private final Role role;
}
