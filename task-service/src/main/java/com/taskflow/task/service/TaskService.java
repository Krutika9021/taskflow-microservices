package com.taskflow.task.service;

import com.taskflow.task.api.ForbiddenOperationException;
import com.taskflow.task.api.TaskNotFoundException;
import com.taskflow.task.api.dto.CreateTaskRequest;
import com.taskflow.task.api.dto.TaskResponse;
import com.taskflow.task.api.dto.UpdateTaskRequest;
import com.taskflow.task.domain.Role;
import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TaskResponse create(Long tenantId, Long userId, CreateTaskRequest request) {
        TaskStatus status = request.status() == null ? TaskStatus.TODO : request.status();
        Task task = taskRepository.save(new Task(
                request.title(),
                request.description(),
                status,
                request.assignee(),
                tenantId,
                userId
        ));
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(Long tenantId, Long taskId) {
        return TaskResponse.from(requireForTenant(taskId, tenantId));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(Long tenantId, TaskStatus status, Pageable pageable) {
        Page<Task> page = status == null
                ? taskRepository.findByTenantId(tenantId, pageable)
                : taskRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        return page.map(TaskResponse::from);
    }

    @Transactional
    public TaskResponse update(Long tenantId, Long taskId, UpdateTaskRequest request) {
        Task task = requireForTenant(taskId, tenantId);
        task.update(request.title(), request.description(), request.status(), request.assignee());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long tenantId, Long taskId, Role role) {
        Task task = requireForTenant(taskId, tenantId);
        if (role != Role.ADMIN) {
            throw new ForbiddenOperationException();
        }
        taskRepository.deleteByIdAndTenantId(taskId, tenantId);
    }

    private Task requireForTenant(Long taskId, Long tenantId) {
        return taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(TaskNotFoundException::new);
    }
}
