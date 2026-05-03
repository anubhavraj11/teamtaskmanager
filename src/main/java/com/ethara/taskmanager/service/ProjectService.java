package com.ethara.taskmanager.service;

import com.ethara.taskmanager.dto.common.ApiMessageResponse;
import com.ethara.taskmanager.dto.project.ProjectCreateRequest;
import com.ethara.taskmanager.dto.project.ProjectMemberRequest;
import com.ethara.taskmanager.dto.project.ProjectResponse;
import com.ethara.taskmanager.dto.project.ProjectUpdateRequest;
import com.ethara.taskmanager.entity.Project;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.Role;
import com.ethara.taskmanager.exception.BadRequestException;
import com.ethara.taskmanager.exception.ForbiddenException;
import com.ethara.taskmanager.exception.ResourceNotFoundException;
import com.ethara.taskmanager.repository.ProjectRepository;
import com.ethara.taskmanager.repository.TaskRepository;
import com.ethara.taskmanager.repository.UserRepository;
import com.ethara.taskmanager.service.support.ResponseMapper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        Project project = new Project();
        project.setName(request.getName().trim());
        project.setDescription(normalizeOptionalText(request.getDescription()));
        project.setCreatedBy(currentUser);
        project.getMembers().add(currentUser);

        return responseMapper.toProjectResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> getProjects() {
        User currentUser = currentUserService.getCurrentUser();

        List<Project> projects = currentUser.getRole() == Role.ADMIN
            ? projectRepository.findAllByOrderByCreatedAtDesc()
            : projectRepository.findDistinctByMembersContainingOrderByCreatedAtDesc(currentUser);

        return projects.stream()
            .map(responseMapper::toProjectResponse)
            .toList();
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectUpdateRequest request) {
        Project project = getManagedProject(projectId);
        project.setName(request.getName().trim());
        project.setDescription(normalizeOptionalText(request.getDescription()));
        return responseMapper.toProjectResponse(project);
    }

    @Transactional
    public ProjectResponse addMembers(Long projectId, ProjectMemberRequest request) {
        Project project = getManagedProject(projectId);
        Set<Long> requestedMemberIds = request.getMemberIds();
        List<User> membersToAdd = userRepository.findByIdIn(requestedMemberIds);

        if (membersToAdd.size() != requestedMemberIds.size()) {
            throw new ResourceNotFoundException("One or more users do not exist");
        }

        project.getMembers().addAll(membersToAdd);
        return responseMapper.toProjectResponse(project);
    }

    @Transactional
    public ApiMessageResponse removeMember(Long projectId, Long memberId) {
        Project project = getManagedProject(projectId);

        if (project.getCreatedBy().getId().equals(memberId)) {
            throw new BadRequestException("Project owner cannot be removed from the project");
        }

        User member = project.getMembers().stream()
            .filter(user -> user.getId().equals(memberId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Member is not part of this project"));

        if (taskRepository.existsByProjectIdAndAssigneeId(projectId, memberId)) {
            throw new BadRequestException("Member still has assigned tasks in this project");
        }

        project.getMembers().remove(member);

        return ApiMessageResponse.builder()
            .message("Member removed successfully")
            .build();
    }

    @Transactional
    public ApiMessageResponse deleteProject(Long projectId) {
        Project project = getManagedProject(projectId);
        projectRepository.delete(project);

        return ApiMessageResponse.builder()
            .message("Project deleted successfully")
            .build();
    }

    public Project getProjectById(Long projectId) {
        return projectRepository.findWithMembersById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private Project getManagedProject(Long projectId) {
        User currentUser = currentUserService.getCurrentUser();
        Project project = getProjectById(projectId);

        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to manage this project");
        }

        return project;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
