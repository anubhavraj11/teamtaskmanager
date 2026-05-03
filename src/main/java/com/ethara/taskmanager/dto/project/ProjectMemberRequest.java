package com.ethara.taskmanager.dto.project;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectMemberRequest {

    @NotEmpty(message = "At least one member is required")
    private Set<@NotNull(message = "Member id cannot be null") Long> memberIds;
}
