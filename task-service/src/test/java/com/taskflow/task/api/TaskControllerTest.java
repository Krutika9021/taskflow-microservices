package com.taskflow.task.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crossTenantGetReturns404() throws Exception {
        long taskId = createTask(1L, 10L, "Tenant one task");

        mockMvc.perform(get("/tasks/{id}", taskId)
                        .header(TaskController.HEADER_TENANT_ID, "2")
                        .header(TaskController.HEADER_USER_ID, "20")
                        .header(TaskController.HEADER_ROLE, "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crossTenantListIsEmpty() throws Exception {
        createTask(1L, 10L, "Tenant one task");

        mockMvc.perform(get("/tasks")
                        .header(TaskController.HEADER_TENANT_ID, "2")
                        .header(TaskController.HEADER_USER_ID, "20")
                        .header(TaskController.HEADER_ROLE, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void nonAdminDeleteReturns403() throws Exception {
        long taskId = createTask(1L, 10L, "Keep me");

        mockMvc.perform(delete("/tasks/{id}", taskId)
                        .header(TaskController.HEADER_TENANT_ID, "1")
                        .header(TaskController.HEADER_USER_ID, "10")
                        .header(TaskController.HEADER_ROLE, "USER"))
                .andExpect(status().isForbidden());
    }

    private long createTask(long tenantId, long userId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/tasks")
                        .header(TaskController.HEADER_TENANT_ID, String.valueOf(tenantId))
                        .header(TaskController.HEADER_USER_ID, String.valueOf(userId))
                        .header(TaskController.HEADER_ROLE, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
