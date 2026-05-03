package com.ethara.taskmanager.dto.dashboard;

import com.ethara.taskmanager.entity.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TaskStatusSummaryResponse {

    private final TaskStatus status;
    private final long count;
}
