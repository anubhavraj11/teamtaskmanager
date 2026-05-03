package com.ethara.taskmanager.service;

import com.ethara.taskmanager.dto.common.PagedResponse;
import com.ethara.taskmanager.dto.task.TaskAssignRequest;
import com.ethara.taskmanager.dto.task.TaskCreateRequest;
import com.ethara.taskmanager.dto.task.TaskQueryRequest;
import com.ethara.taskmanager.dto.task.TaskResponse;
import com.ethara.taskmanager.dto.task.TaskStatusUpdateRequest;
import com.ethara.taskmanager.entity.Project;
import com.ethara.taskmanager.entity.Task;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.Role;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import com.ethara.taskmanager.exception.BadRequestException;
import com.ethara.taskmanager.exception.ForbiddenException;
import com.ethara.taskmanager.exception.ResourceNotFoundException;
import com.ethara.taskmanager.repository.TaskRepository;
import com.ethara.taskmanager.repository.UserRepository;
import com.ethara.taskmanager.service.support.ResponseMapper;
import com.ethara.taskmanager.service.support.TaskSpecifications;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ProjectService projectService;
    private final ResponseMapper responseMapper;

    @Transactional
    public TaskResponse createTask(TaskCreateRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        validateAdmin(currentUser);

        Project project = projectService.getProjectById(request.getProjectId());
        User assignee = findAssignableMember(project, request.getAssigneeId());

        Task task = new Task();
        task.setTitle(request.getTitle().trim());
        task.setDescription(normalizeOptionalText(request.getDescription()));
        task.setDueDate(request.getDueDate());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setProject(project);
        task.setAssignee(assignee);
        task.setCreatedBy(currentUser);

        return responseMapper.toTaskResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse assignTask(Long taskId, TaskAssignRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        validateAdmin(currentUser);

        Task task = getTask(taskId);
        User assignee = findAssignableMember(task.getProject(), request.getAssigneeId());
        task.setAssignee(assignee);

        return responseMapper.toTaskResponse(task);
    }

    public PagedResponse<TaskResponse> getAllTasks(TaskQueryRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        validateAdmin(currentUser);

        Page<Task> taskPage = taskRepository.findAll(
            buildSpecification(request, null, false),
            buildPageable(request)
        );

        return responseMapper.toPagedResponse(taskPage.map(responseMapper::toTaskResponse));
    }

    public PagedResponse<TaskResponse> getMyTasks(TaskQueryRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Page<Task> taskPage = taskRepository.findAll(
            buildSpecification(request, currentUser.getId(), false),
            buildPageable(request)
        );

        return responseMapper.toPagedResponse(taskPage.map(responseMapper::toTaskResponse));
    }

    public PagedResponse<TaskResponse> getOverdueTasks(TaskQueryRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Long assigneeId = currentUser.getRole() == Role.ADMIN ? null : currentUser.getId();

        Page<Task> taskPage = taskRepository.findAll(
            buildSpecification(request, assigneeId, true),
            buildPageable(request)
        );

        return responseMapper.toPagedResponse(taskPage.map(responseMapper::toTaskResponse));
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, TaskStatusUpdateRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Task task = getTask(taskId);

        if (!task.getAssignee().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only update tasks assigned to you");
        }

        validateStatusTransition(task.getStatus(), request.getStatus());
        task.setStatus(request.getStatus());

        return responseMapper.toTaskResponse(task);
    }

    private Task getTask(Long taskId) {
        return taskRepository.findDetailedById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private User findAssignableMember(Project project, Long assigneeId) {
        User assignee = userRepository.findById(assigneeId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));

        boolean assigneeBelongsToProject = project.getMembers().stream()
            .anyMatch(member -> member.getId().equals(assignee.getId()));

        if (!assigneeBelongsToProject) {
            throw new BadRequestException("Assignee must be a member of the selected project");
        }

        return assignee;
    }

    private Specification<Task> buildSpecification(TaskQueryRequest request, Long assigneeId, boolean overdueOnly) {
        Specification<Task> specification = Specification.allOf(
            TaskSpecifications.titleOrDescriptionContains(request.getSearch()),
            TaskSpecifications.hasStatus(request.getStatus()),
            TaskSpecifications.hasPriority(request.getPriority()),
            TaskSpecifications.belongsToProject(request.getProjectId()),
            TaskSpecifications.assignedTo(assigneeId)
        );

        if (overdueOnly) {
            specification = specification.and(TaskSpecifications.overdueOnly());
        }

        return specification;
    }

    private Pageable buildPageable(TaskQueryRequest request) {
        int page = request.getPage() == null || request.getPage() < 0 ? 0 : request.getPage();
        int size = request.getSize() == null ? 10 : request.getSize();
        size = Math.min(Math.max(size, 1), 50);

        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "dueDate", "id"));
    }

    private void validateStatusTransition(TaskStatus currentStatus, TaskStatus requestedStatus) {
        if (currentStatus == requestedStatus) {
            throw new BadRequestException("Task is already in the requested status");
        }

        if (currentStatus == TaskStatus.DONE) {
            throw new BadRequestException("Done tasks cannot be moved to another status");
        }

        if (currentStatus == TaskStatus.TODO && requestedStatus == TaskStatus.IN_PROGRESS) {
            return;
        }

        if (currentStatus == TaskStatus.IN_PROGRESS && requestedStatus == TaskStatus.DONE) {
            return;
        }

        throw new BadRequestException("Invalid task status transition");
    }

    private void validateAdmin(User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to manage tasks");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
