package com.taskflow.task.api.dto;

import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Long assignee,
        Long tenantId,
        Long createdBy,
        Instant createdAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getAssignee(),
                task.getTenantId(),
                task.getCreatedBy(),
                task.getCreatedAt()
        );
    }
}
