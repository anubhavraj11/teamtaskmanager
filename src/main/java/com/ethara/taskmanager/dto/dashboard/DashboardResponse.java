package com.ethara.taskmanager.dto.dashboard;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponse {

    private final long totalTasks;
    private final long overdueTasks;
    private final List<TaskStatusSummaryResponse> tasksByStatus;
}
