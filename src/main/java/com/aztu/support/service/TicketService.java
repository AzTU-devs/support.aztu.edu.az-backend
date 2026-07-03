package com.aztu.support.service;

import com.aztu.support.domain.Category;
import com.aztu.support.domain.Comment;
import com.aztu.support.domain.Platform;
import com.aztu.support.domain.Ticket;
import com.aztu.support.domain.TicketStatusHistory;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.domain.enums.TicketPriority;
import com.aztu.support.domain.enums.TicketStatus;
import com.aztu.support.dto.dashboard.DashboardStatsResponse;
import com.aztu.support.dto.ticket.AttachmentResponse;
import com.aztu.support.dto.ticket.CommentRequest;
import com.aztu.support.dto.ticket.CommentResponse;
import com.aztu.support.dto.ticket.CreateTicketRequest;
import com.aztu.support.dto.ticket.StatusHistoryResponse;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final List<RoleName> SUPPORT_ROLES =
            List.of(RoleName.SUPPORT_TEAM, RoleName.ADMIN, RoleName.SUPER_ADMIN);

    private final TicketRepository ticketRepository;
    private final PlatformRepository platformRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TicketStatusHistoryRepository statusHistoryRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final NotificationService notificationService;

    public TicketService(TicketRepository ticketRepository,
                         PlatformRepository platformRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository,
                         TicketStatusHistoryRepository statusHistoryRepository,
                         CommentRepository commentRepository,
                         AttachmentRepository attachmentRepository,
                         NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.platformRepository = platformRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.notificationService = notificationService;
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public TicketDetailResponse create(Long creatorId, CreateTicketRequest request) {
        User creator = loadUser(creatorId);
        Platform platform = platformRepository.findById(request.platformId())
                .orElseThrow(() -> ApiException.notFound("Selected platform not found."));
        if (!platform.isActive()) {
            throw ApiException.badRequest("The selected platform is not accepting tickets.");
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> ApiException.notFound("Selected category not found."));
            if (!category.getPlatform().getId().equals(platform.getId())) {
                throw ApiException.badRequest("The selected category does not belong to the selected platform.");
            }
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNumber(nextTicketNumber());
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority() != null ? request.priority() : TicketPriority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPlatform(platform);
        ticket.setCategory(category);
        ticket.setCreatedBy(creator);
        ticket = ticketRepository.save(ticket);

        recordHistory(ticket, null, TicketStatus.OPEN, creator, "Ticket opened");
        notificationService.ticketCreated(ticket);

        return detailOf(ticket, RoleName.SUPPORT_TEAM); // creator always sees full own ticket
    }

    // ── List (role-scoped, filtered) ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Ticket> list(Long userId, RoleName role,
                             TicketStatus status, TicketPriority priority, Long platformId,
                             Long assignedToId, Boolean mine, String q, Pageable pageable) {
        List<Specification<Ticket>> specs = new ArrayList<>();

        boolean support = SUPPORT_ROLES.contains(role);
        if (!support) {
            // Regular users only ever see their own tickets.
            specs.add((root, query, cb) -> cb.equal(root.get("createdBy").get("id"), userId));
        } else if (Boolean.TRUE.equals(mine)) {
            specs.add((root, query, cb) -> cb.equal(root.get("createdBy").get("id"), userId));
        }

        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (priority != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("priority"), priority));
        }
        if (platformId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("platform").get("id"), platformId));
        }
        if (support && assignedToId != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("assignedTo").get("id"), assignedToId));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            specs.add((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("subject")), like),
                    cb.like(cb.lower(root.get("ticketNumber")), like)));
        }

        Specification<Ticket> combined = specs.stream().reduce(Specification::and).orElse(null);
        return ticketRepository.findAll(combined, pageable);
    }

    // ── Detail ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketDetailResponse getDetail(Long userId, RoleName role, Long ticketId) {
        Ticket ticket = loadTicket(ticketId);
        requireView(ticket, userId, role);
        return detailOf(ticket, role);
    }

    // ── Status change (SUPPORT_TEAM and above) ───────────────────────────────

    @Transactional
    public TicketDetailResponse updateStatus(Long actorId, RoleName role, Long ticketId,
                                             UpdateTicketStatusRequest request) {
        requireSupport(role);
        Ticket ticket = loadTicket(ticketId);
        User actor = loadUser(actorId);

        TicketStatus from = ticket.getStatus();
        TicketStatus to = request.status();
        if (from == to) {
            throw ApiException.badRequest("The ticket is already " + to + ".");
        }

        // MANDATORY close reason: any closing status requires a resolution/reason note.
        String reason = request.reason() != null ? request.reason().trim() : null;
        if (to.isClosing() && (reason == null || reason.isBlank())) {
            throw ApiException.badRequest(
                    "A reason / resolution note is required when closing a ticket (" + to + ").");
        }

        ticket.setStatus(to);
        if (to.isClosing()) {
            ticket.setResolutionNote(reason);
            ticket.setClosedAt(Instant.now());
        } else {
            // Re-opening clears the closed marker.
            ticket.setClosedAt(null);
        }
        ticketRepository.save(ticket);

        recordHistory(ticket, from, to, actor, reason);
        notificationService.ticketStatusChanged(ticket, from, to, actorId, reason);

        return detailOf(ticket, role);
    }

    // ── Assignment (SUPPORT_TEAM and above) ──────────────────────────────────

    @Transactional
    public TicketDetailResponse assign(Long actorId, RoleName role, Long ticketId, Long assigneeId) {
        requireSupport(role);
        Ticket ticket = loadTicket(ticketId);

        if (assigneeId == null) {
            ticket.setAssignedTo(null);
            ticketRepository.save(ticket);
            return detailOf(ticket, role);
        }

        User assignee = loadUser(assigneeId);
        if (!SUPPORT_ROLES.contains(assignee.roleName()) || assignee.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.badRequest("Tickets can only be assigned to active support staff.");
        }
        ticket.setAssignedTo(assignee);
        ticketRepository.save(ticket);
        notificationService.ticketAssigned(ticket, actorId);
        return detailOf(ticket, role);
    }

    // ── Comments ─────────────────────────────────────────────────────────────

    @Transactional
    public CommentResponse addComment(Long actorId, RoleName role, Long ticketId, CommentRequest request) {
        Ticket ticket = loadTicket(ticketId);
        requireView(ticket, actorId, role);
        User author = loadUser(actorId);

        boolean support = SUPPORT_ROLES.contains(role);
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setAuthor(author);
        comment.setBody(request.body());
        // Only support staff may post internal notes.
        comment.setInternal(support && request.internal());
        comment = commentRepository.save(comment);

        // Bump the ticket's updated timestamp so lists re-sort.
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.save(ticket);

        notificationService.commentAdded(ticket, comment);
        return CommentResponse.from(comment);
    }

    // ── Dashboard ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboardStats(Long userId, RoleName role) {
        boolean support = SUPPORT_ROLES.contains(role);
        boolean admin = role == RoleName.ADMIN || role == RoleName.SUPER_ADMIN;

        long myTickets = ticketRepository.countByCreatedBy_Id(userId);
        long myOpen = ticketRepository.countByCreatedBy_IdAndStatus(userId, TicketStatus.OPEN)
                + ticketRepository.countByCreatedBy_IdAndStatus(userId, TicketStatus.IN_PROGRESS);
        long unread = notificationService.unreadCount(userId);

        Long assignedToMe = support ? ticketRepository.countByAssignedTo_Id(userId) : null;
        Long total = support ? ticketRepository.count() : null;
        Long open = support ? ticketRepository.countByStatus(TicketStatus.OPEN) : null;
        Long inProgress = support ? ticketRepository.countByStatus(TicketStatus.IN_PROGRESS) : null;
        Long resolved = support ? ticketRepository.countByStatus(TicketStatus.RESOLVED) : null;
        Long closed = support
                ? ticketRepository.countByStatus(TicketStatus.CLOSED_WITHOUT_RESOLVE)
                        + ticketRepository.countByStatus(TicketStatus.REJECTED)
                : null;

        Long pendingApprovals = admin ? userRepository.countByStatus(AccountStatus.PENDING_APPROVAL) : null;
        Long totalUsers = admin ? userRepository.count() : null;

        return new DashboardStatsResponse(role, myTickets, myOpen, assignedToMe, total, open,
                inProgress, resolved, closed, pendingApprovals, totalUsers, unread);
    }

    // ── Internals ────────────────────────────────────────────────────────────

    /** Loads a ticket for internal use (e.g. attachment service). */
    @Transactional(readOnly = true)
    public Ticket loadTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> ApiException.notFound("Ticket not found."));
    }

    public boolean canView(Ticket ticket, Long userId, RoleName role) {
        return SUPPORT_ROLES.contains(role)
                || (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(userId));
    }

    public void requireView(Ticket ticket, Long userId, RoleName role) {
        if (!canView(ticket, userId, role)) {
            throw ApiException.forbidden("You do not have access to this ticket.");
        }
    }

    private void requireSupport(RoleName role) {
        if (!SUPPORT_ROLES.contains(role)) {
            throw ApiException.forbidden("Only support staff can perform this action.");
        }
    }

    private TicketDetailResponse detailOf(Ticket ticket, RoleName viewerRole) {
        boolean support = SUPPORT_ROLES.contains(viewerRole);
        List<CommentResponse> comments = (support
                ? commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId())
                : commentRepository.findByTicket_IdAndInternalFalseOrderByCreatedAtAsc(ticket.getId()))
                .stream().map(CommentResponse::from).toList();
        List<StatusHistoryResponse> history =
                statusHistoryRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId())
                        .stream().map(StatusHistoryResponse::from).toList();
        List<AttachmentResponse> attachments =
                attachmentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId())
                        .stream().map(AttachmentResponse::from).toList();
        return TicketDetailResponse.from(ticket, comments, history, attachments);
    }

    private void recordHistory(Ticket ticket, TicketStatus from, TicketStatus to, User actor, String reason) {
        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicket(ticket);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedBy(actor);
        history.setReason(reason);
        statusHistoryRepository.save(history);
    }

    private String nextTicketNumber() {
        return String.format("AZTU-%06d", ticketRepository.nextTicketNumber());
    }

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }
}
