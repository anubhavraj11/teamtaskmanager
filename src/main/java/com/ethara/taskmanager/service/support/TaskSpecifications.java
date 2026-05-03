package com.ethara.taskmanager.service.support;

import com.ethara.taskmanager.entity.Task;
import com.ethara.taskmanager.entity.enums.TaskPriority;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> titleOrDescriptionContains(String search) {
        if (!StringUtils.hasText(search)) {
            return alwaysTrue();
        }

        String pattern = "%" + search.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
            criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
        );
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return status == null
            ? alwaysTrue()
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return priority == null
            ? alwaysTrue()
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority);
    }

    public static Specification<Task> belongsToProject(Long projectId) {
        return projectId == null
            ? alwaysTrue()
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> assignedTo(Long assigneeId) {
        return assigneeId == null
            ? alwaysTrue()
            : (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> overdueOnly() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.lessThan(root.get("dueDate"), LocalDate.now()),
            criteriaBuilder.notEqual(root.get("status"), TaskStatus.DONE)
        );
    }

    private static Specification<Task> alwaysTrue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }
}
