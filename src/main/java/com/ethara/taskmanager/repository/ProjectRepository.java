package com.ethara.taskmanager.repository;

import com.ethara.taskmanager.entity.Project;
import com.ethara.taskmanager.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = {"members", "createdBy", "tasks"})
    List<Project> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"members", "createdBy", "tasks"})
    List<Project> findDistinctByMembersContainingOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"members", "createdBy", "tasks"})
    Optional<Project> findWithMembersById(Long id);
}
