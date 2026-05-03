package com.ethara.taskmanager.dto.project;

import com.ethara.taskmanager.dto.auth.UserSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProjectResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final UserSummaryResponse createdBy;
    private final int memberCount;
    private final long taskCount;
    private final long completedTaskCount;
    private final int progressPercentage;
    private final List<UserSummaryResponse> members;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
