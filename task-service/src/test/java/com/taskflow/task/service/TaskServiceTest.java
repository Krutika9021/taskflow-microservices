package com.taskflow.task.service;

import com.taskflow.task.api.ForbiddenOperationException;
import com.taskflow.task.api.TaskNotFoundException;
import com.taskflow.task.api.dto.CreateTaskRequest;
import com.taskflow.task.domain.Role;
import com.taskflow.task.domain.Task;
import com.taskflow.task.domain.TaskStatus;
import com.taskflow.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getByIdRejectsOtherTenant() {
        when(taskRepository.findByIdAndTenantId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getById(2L, 10L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void listUsesCallerTenantOnly() {
        Task task = new Task("A", null, TaskStatus.TODO, null, 1L, 9L);
        ReflectionTestUtils.setField(task, "id", 5L);
        when(taskRepository.findByTenantId(1L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(task)));

        var page = taskService.list(1L, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        verify(taskRepository, never()).findByTenantId(2L, PageRequest.of(0, 20));
    }

    @Test
    void deleteRejectsNonAdmin() {
        Task task = new Task("A", null, TaskStatus.TODO, null, 1L, 9L);
        when(taskRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(1L, 10L, Role.USER))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(taskRepository, never()).deleteByIdAndTenantId(any(), any());
    }

    @Test
    void createPersistsTenantAndCreator() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 1L);
            return saved;
        });

        var response = taskService.create(7L, 3L, new CreateTaskRequest("Ship", "desc", null, null));

        assertThat(response.tenantId()).isEqualTo(7L);
        assertThat(response.createdBy()).isEqualTo(3L);
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    }
}
