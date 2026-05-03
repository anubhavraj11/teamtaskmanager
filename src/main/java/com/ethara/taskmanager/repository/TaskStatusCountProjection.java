package com.ethara.taskmanager.repository;

import com.ethara.taskmanager.entity.enums.TaskStatus;

public interface TaskStatusCountProjection {

    TaskStatus getStatus();

    long getTotal();
}
