package com.aztu.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aztu.support.domain.Platform;
import com.aztu.support.domain.Role;
import com.aztu.support.domain.Ticket;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.domain.enums.TicketStatus;
import com.aztu.support.dto.ticket.TicketDetailResponse;
import com.aztu.support.dto.ticket.UpdateTicketStatusRequest;
import com.aztu.support.exception.ApiException;
import com.aztu.support.repository.AttachmentRepository;
import com.aztu.support.repository.CategoryRepository;
import com.aztu.support.repository.CommentRepository;
import com.aztu.support.repository.PlatformRepository;
import com.aztu.support.repository.TicketRepository;
import com.aztu.support.repository.TicketStatusHistoryRepository;
import com.aztu.support.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock PlatformRepository platformRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @Mock TicketStatusHistoryRepository statusHistoryRepository;
    @Mock CommentRepository commentRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock NotificationService notificationService;

    TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, platformRepository, categoryRepository,
                userRepository, statusHistoryRepository, commentRepository, attachmentRepository,
                notificationService);
    }

    // ── Mandatory close reason ───────────────────────────────────────────────

    @Test
    void updateStatus_requiresReasonWhenClosing() {
        Ticket ticket = ticket(1L, TicketStatus.OPEN, user(1L, RoleName.USER));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, RoleName.SUPPORT_TEAM)));

        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest(TicketStatus.RESOLVED, "   ");

        assertThatThrownBy(() ->
                ticketService.updateStatus(2L, RoleName.SUPPORT_TEAM, 1L, request))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(ticketRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void updateStatus_closesWithReasonAndRecordsHistory() {
        Ticket ticket = ticket(1L, TicketStatus.IN_PROGRESS, user(1L, RoleName.USER));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, RoleName.SUPPORT_TEAM)));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(statusHistoryRepository.findByTicket_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(attachmentRepository.findByTicket_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        UpdateTicketStatusRequest request =
                new UpdateTicketStatusRequest(TicketStatus.RESOLVED, "Replaced the faulty cable.");

        TicketDetailResponse response =
                ticketService.updateStatus(2L, RoleName.SUPPORT_TEAM, 1L, request);

        assertThat(response.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(response.resolutionNote()).isEqualTo("Replaced the faulty cable.");
        assertThat(ticket.getClosedAt()).isNotNull();
        verify(statusHistoryRepository).save(any());
        verify(notificationService).ticketStatusChanged(any(), any(), any(), anyLong(), any());
    }

    // ── Role-scoped visibility (RBAC) ────────────────────────────────────────

    @Test
    void getDetail_userCannotViewOthersTicket() {
        Ticket ticket = ticket(1L, TicketStatus.OPEN, user(1L, RoleName.USER));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // A different USER (id=99) attempts to view ticket owned by user id=1.
        assertThatThrownBy(() -> ticketService.getDetail(99L, RoleName.USER, 1L))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getDetail_supportCanViewAnyTicket() {
        Ticket ticket = ticket(1L, TicketStatus.OPEN, user(1L, RoleName.USER));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(statusHistoryRepository.findByTicket_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of());
        when(attachmentRepository.findByTicket_IdOrderByCreatedAtAsc(1L)).thenReturn(List.of());

        TicketDetailResponse response = ticketService.getDetail(2L, RoleName.SUPPORT_TEAM, 1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Role role(RoleName name) {
        Role role = new Role();
        role.setId((long) name.ordinal() + 1);
        role.setName(name);
        return role;
    }

    private User user(Long id, RoleName roleName) {
        User user = new User();
        user.setId(id);
        user.setFirstName("User" + id);
        user.setLastName("Test");
        user.setEmail("user" + id + "@aztu.edu.az");
        user.setRole(role(roleName));
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    private Ticket ticket(Long id, TicketStatus status, User creator) {
        Platform platform = new Platform();
        platform.setId(1L);
        platform.setName("Wi-Fi & Network");
        platform.setActive(true);

        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTicketNumber(String.format("AZTU-%06d", id));
        ticket.setSubject("Test ticket");
        ticket.setDescription("Description");
        ticket.setStatus(status);
        ticket.setPlatform(platform);
        ticket.setCreatedBy(creator);
        return ticket;
    }
}
