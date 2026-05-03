package com.ethara.taskmanager.controller;

import com.ethara.taskmanager.dto.common.PagedResponse;
import com.ethara.taskmanager.dto.task.TaskAssignRequest;
import com.ethara.taskmanager.dto.task.TaskCreateRequest;
import com.ethara.taskmanager.dto.task.TaskQueryRequest;
import com.ethara.taskmanager.dto.task.TaskResponse;
import com.ethara.taskmanager.dto.task.TaskStatusUpdateRequest;
import com.ethara.taskmanager.entity.enums.TaskPriority;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import com.ethara.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> assignTask(@PathVariable Long id, @Valid @RequestBody TaskAssignRequest request) {
        return ResponseEntity.ok(taskService.assignTask(id, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<TaskResponse>> getTasks(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskPriority priority,
        @RequestParam(required = false) Long projectId,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(taskService.getAllTasks(buildQuery(search, status, priority, projectId, page, size)));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<PagedResponse<TaskResponse>> getMyTasks(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskPriority priority,
        @RequestParam(required = false) Long projectId,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(taskService.getMyTasks(buildQuery(search, status, priority, projectId, page, size)));
    }

    @GetMapping("/overdue")
    public ResponseEntity<PagedResponse<TaskResponse>> getOverdueTasks(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(required = false) TaskPriority priority,
        @RequestParam(required = false) Long projectId,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        return ResponseEntity.ok(taskService.getOverdueTasks(buildQuery(search, status, priority, projectId, page, size)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
        @PathVariable Long id,
        @Valid @RequestBody TaskStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request));
    }

    private TaskQueryRequest buildQuery(
        String search,
        TaskStatus status,
        TaskPriority priority,
        Long projectId,
        Integer page,
        Integer size
    ) {
        TaskQueryRequest query = new TaskQueryRequest();
        query.setSearch(search);
        query.setStatus(status);
        query.setPriority(priority);
        query.setProjectId(projectId);
        query.setPage(page);
        query.setSize(size);
        return query;
    }
}
