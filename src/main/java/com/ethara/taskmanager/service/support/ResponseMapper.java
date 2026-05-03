package com.ethara.taskmanager.service.support;

import com.ethara.taskmanager.dto.auth.UserSummaryResponse;
import com.ethara.taskmanager.dto.common.PagedResponse;
import com.ethara.taskmanager.dto.project.ProjectResponse;
import com.ethara.taskmanager.dto.task.TaskResponse;
import com.ethara.taskmanager.entity.Project;
import com.ethara.taskmanager.entity.Task;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.TaskPriority;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ResponseMapper {

    public UserSummaryResponse toUserSummary(User user) {
        return UserSummaryResponse.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .role(user.getRole())
            .build();
    }

    public ProjectResponse toProjectResponse(Project project) {
        List<UserSummaryResponse> members = project.getMembers().stream()
            .map(this::toUserSummary)
            .sorted(Comparator
                .comparing(UserSummaryResponse::getFullName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(UserSummaryResponse::getId))
            .toList();

        long taskCount = project.getTasks().size();
        long completedTaskCount = project.getTasks().stream()
            .filter(task -> task.getStatus() == TaskStatus.DONE)
            .count();
        int progressPercentage = taskCount == 0 ? 0 : (int) Math.round((completedTaskCount * 100.0) / taskCount);

        return ProjectResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .createdBy(toUserSummary(project.getCreatedBy()))
            .memberCount(members.size())
            .taskCount(taskCount)
            .completedTaskCount(completedTaskCount)
            .progressPercentage(progressPercentage)
            .members(members)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    public TaskResponse toTaskResponse(Task task) {
        TaskPriority priority = task.getPriority() == null ? TaskPriority.MEDIUM : task.getPriority();
        boolean overdue = task.getDueDate().isBefore(LocalDate.now()) && task.getStatus() != TaskStatus.DONE;

        return TaskResponse.builder()
            .id(task.getId())
            .title(task.getTitle())
            .description(task.getDescription())
            .dueDate(task.getDueDate())
            .status(task.getStatus())
            .priority(priority)
            .projectId(task.getProject().getId())
            .projectName(task.getProject().getName())
            .assignee(toUserSummary(task.getAssignee()))
            .createdBy(toUserSummary(task.getCreatedBy()))
            .overdue(overdue)
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .build();
    }

    public <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .first(page.isFirst())
            .last(page.isLast())
            .build();
    }
}
