package com.ethara.taskmanager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TaskDataMigrationService {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    public ApplicationRunner taskDataMigrationRunner() {
        return args -> {
            try {
                jdbcTemplate.execute("alter table tasks drop constraint if exists tasks_status_check");
                jdbcTemplate.execute("alter table tasks add constraint tasks_status_check check (status in ('TODO', 'IN_PROGRESS', 'DONE'))");
                jdbcTemplate.execute("alter table tasks drop constraint if exists tasks_priority_check");
                jdbcTemplate.execute("alter table tasks add constraint tasks_priority_check check (priority in ('LOW', 'MEDIUM', 'HIGH'))");

                int renamedTodo = jdbcTemplate.update("update tasks set status = 'TODO' where status = 'PENDING'");
                int renamedDone = jdbcTemplate.update("update tasks set status = 'DONE' where status = 'COMPLETED'");
                int prioritized = jdbcTemplate.update("update tasks set priority = 'MEDIUM' where priority is null");

                if (renamedTodo > 0 || renamedDone > 0 || prioritized > 0) {
                    log.info(
                        "Task data migration applied: {} TODO updates, {} DONE updates, {} priority updates",
                        renamedTodo,
                        renamedDone,
                        prioritized
                    );
                }
            } catch (Exception ex) {
                log.warn("Task data migration skipped: {}", ex.getMessage());
            }
        };
    }
}
