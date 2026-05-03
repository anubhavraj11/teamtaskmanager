package com.ethara.taskmanager.dto.task;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskAssignRequest {

    @NotNull(message = "Assignee id is required")
    private Long assigneeId;
}
