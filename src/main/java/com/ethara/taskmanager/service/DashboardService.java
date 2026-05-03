package com.ethara.taskmanager.service;

import com.ethara.taskmanager.dto.dashboard.DashboardResponse;
import com.ethara.taskmanager.dto.dashboard.TaskStatusSummaryResponse;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.Role;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import com.ethara.taskmanager.repository.TaskRepository;
import com.ethara.taskmanager.repository.TaskStatusCountProjection;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;

    public DashboardResponse getDashboard() {
        User currentUser = currentUserService.getCurrentUser();
        Map<TaskStatus, Long> statusCounts = initializeStatusCounts();
        long totalTasks;
        long overdueTasks;

        if (currentUser.getRole() == Role.ADMIN) {
            totalTasks = taskRepository.count();
            overdueTasks = taskRepository.countOverdue(LocalDate.now(), TaskStatus.DONE);
            mergeCounts(statusCounts, taskRepository.countByStatus());
        } else {
            totalTasks = taskRepository.countByAssigneeId(currentUser.getId());
            overdueTasks = taskRepository.countByAssigneeIdAndDueDateBeforeAndStatusNot(
                currentUser.getId(),
                LocalDate.now(),
                TaskStatus.DONE
            );
            mergeCounts(statusCounts, taskRepository.countByStatusForAssignee(currentUser.getId()));
        }

        return buildDashboardResponse(totalTasks, overdueTasks, statusCounts);
    }

    private void mergeCounts(Map<TaskStatus, Long> statusCounts, List<TaskStatusCountProjection> projections) {
        projections.forEach(projection -> statusCounts.put(projection.getStatus(), projection.getTotal()));
    }

    private Map<TaskStatus, Long> initializeStatusCounts() {
        Map<TaskStatus, Long> statusCounts = new EnumMap<>(TaskStatus.class);
        Arrays.stream(TaskStatus.values()).forEach(status -> statusCounts.put(status, 0L));
        return statusCounts;
    }

    private DashboardResponse buildDashboardResponse(long totalTasks, long overdueTasks, Map<TaskStatus, Long> statusCounts) {
        List<TaskStatusSummaryResponse> tasksByStatus = Arrays.stream(TaskStatus.values())
            .map(status -> TaskStatusSummaryResponse.builder()
                .status(status)
                .count(statusCounts.getOrDefault(status, 0L))
                .build())
            .toList();

        return DashboardResponse.builder()
            .totalTasks(totalTasks)
            .overdueTasks(overdueTasks)
            .tasksByStatus(tasksByStatus)
            .build();
    }
}
