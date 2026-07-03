package com.aztu.support.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aztu.support.config.AppProperties;
import com.aztu.support.domain.Role;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.domain.enums.TicketStatus;
import com.aztu.support.dto.ticket.TicketDetailResponse;
import com.aztu.support.security.AppUserDetailsService;
import com.aztu.support.security.AppUserPrincipal;
import com.aztu.support.security.JwtAuthenticationFilter;
import com.aztu.support.security.JwtService;
import com.aztu.support.security.SecurityConfig;
import com.aztu.support.service.AttachmentService;
import com.aztu.support.service.TicketService;
import com.aztu.support.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that the {@code @PreAuthorize} RBAC gate is actually enforced at the
 * web layer: only SUPPORT_TEAM and above may change a ticket's status.
 */
@WebMvcTest(controllers = TicketController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TicketControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean TicketService ticketService;
    @MockitoBean AttachmentService attachmentService;
    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean AppUserDetailsService appUserDetailsService;

    @TestConfiguration
    static class TestProps {
        @Bean
        AppProperties appProperties() {
            return new AppProperties();
        }
    }

    @Test
    void regularUser_cannotChangeTicketStatus() throws Exception {
        mvc.perform(patch("/api/tickets/1/status")
                        .with(user(principal(RoleName.USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void supportTeam_canChangeTicketStatus() throws Exception {
        when(ticketService.updateStatus(anyLong(), any(), anyLong(), any()))
                .thenReturn(stubDetail());

        mvc.perform(patch("/api/tickets/1/status")
                        .with(user(principal(RoleName.SUPPORT_TEAM)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isUnauthorized());
    }

    private AppUserPrincipal principal(RoleName roleName) {
        Role role = new Role();
        role.setId(1L);
        role.setName(roleName);
        User user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@aztu.edu.az");
        user.setPasswordHash("x");
        user.setRole(role);
        user.setStatus(AccountStatus.ACTIVE);
        return new AppUserPrincipal(user);
    }

    private TicketDetailResponse stubDetail() {
        return new TicketDetailResponse(1L, "AZTU-000001", "Subject", "Desc", null,
                TicketStatus.IN_PROGRESS, 1L, "Wi-Fi", null, null, null, null, null,
                null, null, null, List.of(), List.of(), List.of());
    }
}
