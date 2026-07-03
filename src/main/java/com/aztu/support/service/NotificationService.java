package com.aztu.support.service;

import com.aztu.support.config.AppProperties;
import com.aztu.support.domain.Comment;
import com.aztu.support.domain.Notification;
import com.aztu.support.domain.Ticket;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.NotificationType;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.domain.enums.TicketStatus;
import com.aztu.support.repository.NotificationRepository;
import com.aztu.support.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single, reusable, event-driven notification hub. Every meaningful event maps to
 * a method here that (1) creates an in-dashboard notification with read/unread
 * state and (2) sends a corresponding templated email to the relevant user(s).
 */
@Service
public class NotificationService {

    private static final List<RoleName> SUPPORT_ROLES =
            List.of(RoleName.SUPPORT_TEAM, RoleName.ADMIN, RoleName.SUPER_ADMIN);
    private static final List<RoleName> ADMIN_ROLES =
            List.of(RoleName.ADMIN, RoleName.SUPER_ADMIN);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AppProperties props;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               EmailService emailService,
                               AppProperties props) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.props = props;
    }

    // ── Registration / account events ────────────────────────────────────────

    @Transactional
    public void registrationSubmitted(User applicant) {
        String msg = applicant.fullName() + " (" + applicant.getEmail()
                + ") has requested access and is awaiting approval.";
        for (User admin : userRepository.findByRole_NameIn(ADMIN_ROLES)) {
            dispatch(admin, NotificationType.REGISTRATION_PENDING,
                    "New registration pending approval", msg, "/admin/approvals", null,
                    "New registration pending approval",
                    List.of(msg, "Please review the request in the AzTU Support admin console."),
                    "Review request");
        }
    }

    @Transactional
    public void registrationApproved(User user) {
        String msg = "Your AzTU Support account has been approved. You can now sign in.";
        dispatch(user, NotificationType.REGISTRATION_APPROVED,
                "Account approved", msg, "/signin", null,
                "Your account has been approved",
                List.of("Good news! Your AzTU Support account has been approved by an administrator.",
                        "You can now sign in and start opening support tickets."),
                "Sign in");
    }

    @Transactional
    public void registrationRejected(User user, String reason) {
        String msg = "Your AzTU Support registration was rejected. Reason: " + reason;
        dispatch(user, NotificationType.REGISTRATION_REJECTED,
                "Registration rejected", msg, null, null,
                "Your registration was not approved",
                List.of("Unfortunately your AzTU Support registration was not approved.",
                        "Reason: " + reason,
                        "If you believe this is a mistake, please contact the IT department."),
                null);
    }

    @Transactional
    public void roleChanged(User user, RoleName newRole) {
        String msg = "Your role has been updated to " + newRole + ".";
        dispatch(user, NotificationType.ROLE_CHANGED,
                "Role updated", msg, "/", null,
                "Your role has been updated",
                List.of("An administrator has updated your role in AzTU Support.",
                        "Your role is now: " + newRole + "."),
                "Open dashboard");
    }

    // ── Ticket events ────────────────────────────────────────────────────────

    @Transactional
    public void ticketCreated(Ticket ticket) {
        User creator = ticket.getCreatedBy();
        String link = "/tickets/" + ticket.getId();

        // Confirmation to the person who opened it.
        String confirm = "We've received your ticket " + ticket.getTicketNumber()
                + " and the support team has been notified.";
        dispatch(creator, NotificationType.TICKET_OPENED,
                "Ticket " + ticket.getTicketNumber() + " created", confirm, link, ticket,
                "Ticket " + ticket.getTicketNumber() + " created",
                List.of(confirm, "Subject: " + ticket.getSubject(),
                        "You'll be notified here and by email as it progresses."),
                "View ticket");

        // Alert the support staff (excluding the creator if they are staff).
        String alert = creator.fullName() + " opened \"" + ticket.getSubject()
                + "\" on " + ticket.getPlatform().getName() + ".";
        for (User agent : supportStaffExcluding(creator.getId())) {
            dispatch(agent, NotificationType.TICKET_OPENED,
                    "New ticket " + ticket.getTicketNumber(), alert, link, ticket,
                    "New ticket " + ticket.getTicketNumber(),
                    List.of(alert, "Priority: " + ticket.getPriority() + "."),
                    "View ticket");
        }
    }

    @Transactional
    public void ticketAssigned(Ticket ticket, Long actorId) {
        String link = "/tickets/" + ticket.getId();
        User assignee = ticket.getAssignedTo();
        if (assignee != null && !assignee.getId().equals(actorId)) {
            String msg = "Ticket " + ticket.getTicketNumber() + " has been assigned to you.";
            dispatch(assignee, NotificationType.TICKET_ASSIGNED,
                    "Ticket assigned to you", msg, link, ticket,
                    "A ticket has been assigned to you",
                    List.of(msg, "Subject: " + ticket.getSubject()),
                    "View ticket");
        }
        User creator = ticket.getCreatedBy();
        if (creator != null && !creator.getId().equals(actorId)
                && (assignee == null || !creator.getId().equals(assignee.getId()))) {
            String who = assignee != null ? assignee.fullName() : "the support team";
            String msg = "Your ticket " + ticket.getTicketNumber() + " has been assigned to " + who + ".";
            dispatch(creator, NotificationType.TICKET_ASSIGNED,
                    "Ticket " + ticket.getTicketNumber() + " assigned", msg, link, ticket,
                    "Your ticket has been assigned", List.of(msg), "View ticket");
        }
    }

    @Transactional
    public void ticketStatusChanged(Ticket ticket, TicketStatus from, TicketStatus to,
                                    Long actorId, String reason) {
        String link = "/tickets/" + ticket.getId();
        boolean closing = to.isClosing();
        NotificationType type = closing ? NotificationType.TICKET_CLOSED : NotificationType.TICKET_STATUS_CHANGED;
        String title = closing
                ? "Ticket " + ticket.getTicketNumber() + " " + to
                : "Ticket " + ticket.getTicketNumber() + " updated";
        String msg = "Status changed from " + from + " to " + to + ".";

        for (User recipient : ticketParticipantsExcluding(ticket, actorId)) {
            var paras = new java.util.ArrayList<String>();
            paras.add(msg);
            if (reason != null && !reason.isBlank()) {
                paras.add((closing ? "Resolution note: " : "Note: ") + reason);
            }
            dispatch(recipient, type, title, msg, link, ticket, title, paras, "View ticket");
        }
    }

    @Transactional
    public void commentAdded(Ticket ticket, Comment comment) {
        String link = "/tickets/" + ticket.getId();
        User author = comment.getAuthor();
        Map<Long, User> recipients = new LinkedHashMap<>();

        if (ticket.getCreatedBy() != null) {
            recipients.put(ticket.getCreatedBy().getId(), ticket.getCreatedBy());
        }
        if (ticket.getAssignedTo() != null) {
            recipients.put(ticket.getAssignedTo().getId(), ticket.getAssignedTo());
        }
        // If a user replies on an unassigned ticket, alert all support staff to pick it up.
        boolean authorIsStaff = SUPPORT_ROLES.contains(author.roleName());
        if (!authorIsStaff && ticket.getAssignedTo() == null) {
            for (User agent : userRepository.findByRole_NameIn(SUPPORT_ROLES)) {
                recipients.put(agent.getId(), agent);
            }
        }
        // Internal notes are only visible to support staff — never notify the ticket owner.
        if (comment.isInternal() && ticket.getCreatedBy() != null) {
            User owner = ticket.getCreatedBy();
            if (!SUPPORT_ROLES.contains(owner.roleName())) {
                recipients.remove(owner.getId());
            }
        }
        recipients.remove(author.getId());

        String msg = author.fullName() + " commented on ticket " + ticket.getTicketNumber() + ".";
        for (User recipient : recipients.values()) {
            dispatch(recipient, NotificationType.COMMENT_ADDED,
                    "New reply on " + ticket.getTicketNumber(), msg, link, ticket,
                    "New reply on ticket " + ticket.getTicketNumber(),
                    List.of(msg, "\"" + trim(comment.getBody()) + "\""),
                    "View ticket");
        }
    }

    // ── Read side (dashboard notifications page) ─────────────────────────────

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Notification> list(
            Long userId, boolean unreadOnly, org.springframework.data.domain.Pageable pageable) {
        return unreadOnly
                ? notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipient_IdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> com.aztu.support.exception.ApiException.notFound("Notification not found."));
        if (!n.getRecipient().getId().equals(userId)) {
            throw com.aztu.support.exception.ApiException.forbidden("This notification does not belong to you.");
        }
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(java.time.Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, java.time.Instant.now());
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private List<User> supportStaffExcluding(Long excludedUserId) {
        return userRepository.findByRole_NameIn(SUPPORT_ROLES).stream()
                .filter(u -> !u.getId().equals(excludedUserId))
                .toList();
    }

    private List<User> ticketParticipantsExcluding(Ticket ticket, Long excludedUserId) {
        Map<Long, User> map = new LinkedHashMap<>();
        if (ticket.getCreatedBy() != null) {
            map.put(ticket.getCreatedBy().getId(), ticket.getCreatedBy());
        }
        if (ticket.getAssignedTo() != null) {
            map.put(ticket.getAssignedTo().getId(), ticket.getAssignedTo());
        }
        if (excludedUserId != null) {
            map.remove(excludedUserId);
        }
        return List.copyOf(map.values());
    }

    /** Persists the in-dashboard notification and fires the email. */
    private void dispatch(User recipient, NotificationType type, String title, String inAppMessage,
                          String relativeLink, Ticket ticket,
                          String emailHeading, List<String> emailParagraphs, String ctaLabel) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(inAppMessage);
        notification.setLink(relativeLink);
        notification.setTicket(ticket);
        notificationRepository.save(notification);

        EmailContent.Builder content = EmailContent.builder()
                .heading(emailHeading)
                .greeting("Hello " + recipient.getFirstName() + ",")
                .paragraphs(emailParagraphs);
        if (ctaLabel != null && relativeLink != null) {
            content.cta(ctaLabel, absoluteUrl(relativeLink));
        }
        emailService.send(recipient.getEmail(), title, content.build());
    }

    private String absoluteUrl(String relativeLink) {
        String base = props.getFrontendUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + (relativeLink.startsWith("/") ? relativeLink : "/" + relativeLink);
    }

    private String trim(String body) {
        String single = body.replaceAll("\\s+", " ").trim();
        return single.length() > 240 ? single.substring(0, 240) + "…" : single;
    }
}
