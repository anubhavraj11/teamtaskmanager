package com.ethara.taskmanager.controller;

import com.ethara.taskmanager.dto.common.ApiMessageResponse;
import com.ethara.taskmanager.dto.project.ProjectCreateRequest;
import com.ethara.taskmanager.dto.project.ProjectMemberRequest;
import com.ethara.taskmanager.dto.project.ProjectResponse;
import com.ethara.taskmanager.dto.project.ProjectUpdateRequest;
import com.ethara.taskmanager.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        return ResponseEntity.ok(projectService.getProjects());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> updateProject(
        @PathVariable Long id,
        @Valid @RequestBody ProjectUpdateRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @PutMapping("/{id}/add-member")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> addMembers(
        @PathVariable Long id,
        @Valid @RequestBody ProjectMemberRequest request
    ) {
        return ResponseEntity.ok(projectService.addMembers(id, request));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> addMembersLegacy(
        @PathVariable Long id,
        @Valid @RequestBody ProjectMemberRequest request
    ) {
        return ResponseEntity.ok(projectService.addMembers(id, request));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiMessageResponse> removeMember(@PathVariable Long id, @PathVariable Long memberId) {
        return ResponseEntity.ok(projectService.removeMember(id, memberId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiMessageResponse> deleteProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.deleteProject(id));
    }
}
