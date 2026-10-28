package com.taskflow.task.api;

import com.taskflow.task.api.dto.CreateTaskRequest;
import com.taskflow.task.api.dto.TaskResponse;
import com.taskflow.task.api.dto.UpdateTaskRequest;
import com.taskflow.task.domain.Role;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ROLE = "X-Role";

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @RequestHeader(HEADER_TENANT_ID) Long tenantId,
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestHeader(HEADER_ROLE) Role role,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        return taskService.create(tenantId, userId, request);
    }

    @GetMapping
    public Page<TaskResponse> list(
            @RequestHeader(HEADER_TENANT_ID) Long tenantId,
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestHeader(HEADER_ROLE) Role role,
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable
    ) {
        return taskService.list(tenantId, status, pageable);
    }

    @GetMapping("/{id}")
    public TaskResponse get(
            @RequestHeader(HEADER_TENANT_ID) Long tenantId,
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestHeader(HEADER_ROLE) Role role,
            @PathVariable Long id
    ) {
        return taskService.getById(tenantId, id);
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @RequestHeader(HEADER_TENANT_ID) Long tenantId,
            @RequestHeader(HEADER_USER_ID) Long userId,
            @RequestHeader(HEADER_ROLE) Role role,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(tenantId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(HEADER_TENANT_ID) Long tenantId,
            @RequestHeader(HEADER_ROLE) Role role,
            @PathVariable Long id
    ) {
        taskService.delete(tenantId, id, role);
    }
}
