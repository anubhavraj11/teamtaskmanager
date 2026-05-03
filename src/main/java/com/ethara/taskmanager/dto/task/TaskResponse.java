package com.ethara.taskmanager.dto.task;

import com.ethara.taskmanager.dto.auth.UserSummaryResponse;
import com.ethara.taskmanager.entity.enums.TaskPriority;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TaskResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final LocalDate dueDate;
    private final TaskStatus status;
    private final TaskPriority priority;
    private final Long projectId;
    private final String projectName;
    private final UserSummaryResponse assignee;
    private final UserSummaryResponse createdBy;
    private final boolean overdue;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
