package com.aztu.support.controller;

import com.aztu.support.dto.common.MessageResponse;
import com.aztu.support.dto.common.PageResponse;
import com.aztu.support.dto.notification.NotificationResponse;
import com.aztu.support.security.AppUserPrincipal;
import com.aztu.support.security.Authorities;
import com.aztu.support.service.NotificationService;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize(Authorities.AUTHENTICATED)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return PageResponse.from(
                notificationService.list(principal.getId(), unreadOnly, pageable),
                NotificationResponse::from);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return Map.of("count", notificationService.unreadCount(principal.getId()));
    }

    @PostMapping("/{id}/read")
    public MessageResponse markRead(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long id) {
        notificationService.markRead(principal.getId(), id);
        return MessageResponse.of("Marked as read.");
    }

    @PostMapping("/read-all")
    public MessageResponse markAllRead(@AuthenticationPrincipal AppUserPrincipal principal) {
        notificationService.markAllRead(principal.getId());
        return MessageResponse.of("All notifications marked as read.");
    }
}
