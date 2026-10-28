package com.taskflow.task.repository;

import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface TaskRepository extends Repository<Task, Long> {

    Task save(Task task);

    Page<Task> findByTenantId(Long tenantId, Pageable pageable);

    Page<Task> findByTenantIdAndStatus(Long tenantId, TaskStatus status, Pageable pageable);

    Optional<Task> findByIdAndTenantId(Long id, Long tenantId);

    void deleteByIdAndTenantId(Long id, Long tenantId);
}
