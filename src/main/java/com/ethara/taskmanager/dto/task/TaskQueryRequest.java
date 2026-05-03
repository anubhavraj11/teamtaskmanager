package com.ethara.taskmanager.dto.task;

import com.ethara.taskmanager.entity.enums.TaskPriority;
import com.ethara.taskmanager.entity.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskQueryRequest {

    private String search;
    private TaskStatus status;
    private TaskPriority priority;
    private Long projectId;
    private Integer page = 0;
    private Integer size = 10;
}
