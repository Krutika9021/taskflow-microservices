package com.taskflow.task.api.dto;

import com.taskflow.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 4000) String description,
        TaskStatus status,
        Long assignee
) {
}
