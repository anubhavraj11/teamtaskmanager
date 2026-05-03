package com.ethara.taskmanager.repository;

import com.ethara.taskmanager.entity.Task;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @EntityGraph(attributePaths = {"project", "assignee", "createdBy"})
    Optional<Task> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"project", "assignee", "createdBy"})
    List<Task> findAllByOrderByDueDateAsc();

    @EntityGraph(attributePaths = {"project", "assignee", "createdBy"})
    List<Task> findByAssigneeOrderByDueDateAsc(User assignee);

    @EntityGraph(attributePaths = {"project", "assignee", "createdBy"})
    List<Task> findByProjectIdInOrderByDueDateAsc(Collection<Long> projectIds);

    boolean existsByProjectIdAndAssigneeId(Long projectId, Long assigneeId);

    @Query("""
        select t.status as status, count(t) as total
        from Task t
        group by t.status
        """)
    List<TaskStatusCountProjection> countByStatus();

    @Query("""
        select count(t)
        from Task t
        where t.dueDate < :today
          and t.status <> :completedStatus
        """)
    long countOverdue(
        @Param("today") LocalDate today,
        @Param("completedStatus") TaskStatus completedStatus
    );

    @Query("""
        select t.status as status, count(t) as total
        from Task t
        where t.project.id in :projectIds
        group by t.status
        """)
    List<TaskStatusCountProjection> countByStatusForProjectIds(@Param("projectIds") Collection<Long> projectIds);

    @Query("""
        select count(t)
        from Task t
        where t.project.id in :projectIds
        """)
    long countByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    @Query("""
        select count(t)
        from Task t
        where t.project.id in :projectIds
          and t.dueDate < :today
          and t.status <> :completedStatus
        """)
    long countOverdueByProjectIds(
        @Param("projectIds") Collection<Long> projectIds,
        @Param("today") LocalDate today,
        @Param("completedStatus") TaskStatus completedStatus
    );

    @Query("""
        select t.status as status, count(t) as total
        from Task t
        where t.assignee.id = :assigneeId
        group by t.status
        """)
    List<TaskStatusCountProjection> countByStatusForAssignee(@Param("assigneeId") Long assigneeId);

    long countByAssigneeId(Long assigneeId);

    long countByAssigneeIdAndDueDateBeforeAndStatusNot(Long assigneeId, LocalDate dueDate, TaskStatus status);
}
