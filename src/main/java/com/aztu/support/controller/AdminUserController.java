package com.aztu.support.controller;

import com.aztu.support.domain.enums.AccountStatus;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.dto.common.PageResponse;
import com.aztu.support.dto.user.RejectRegistrationRequest;
import com.aztu.support.dto.user.UpdateRoleRequest;
import com.aztu.support.dto.user.UpdateStatusRequest;
import com.aztu.support.dto.user.UserResponse;
import com.aztu.support.security.AppUserPrincipal;
import com.aztu.support.security.Authorities;
import com.aztu.support.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    // ── Registration approvals (ADMIN / SUPER_ADMIN) ─────────────────────────

    @GetMapping("/pending")
    @PreAuthorize(Authorities.ADMIN)
    public PageResponse<UserResponse> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "createdAt"));
        return PageResponse.from(userService.pendingApprovals(pageable), UserResponse::from);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Authorities.ADMIN)
    public UserResponse approve(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long id,
                                @RequestBody(required = false) UpdateRoleRequest request) {
        RoleName role = request != null ? request.role() : null;
        return userService.approve(principal.getId(), id, role);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(Authorities.ADMIN)
    public UserResponse reject(@AuthenticationPrincipal AppUserPrincipal principal,
                               @PathVariable Long id,
                               @Valid @RequestBody RejectRegistrationRequest request) {
        return userService.reject(principal.getId(), id, request.reason());
    }

    // ── User directory (ADMIN can view) ──────────────────────────────────────

    @GetMapping
    @PreAuthorize(Authorities.ADMIN)
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) RoleName role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(userService.list(status, role, pageable), UserResponse::from);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Authorities.ADMIN)
    public UserResponse get(@PathVariable Long id) {
        return userService.getById(id);
    }

    // ── Role & status management (SUPER_ADMIN only) ──────────────────────────

    @PatchMapping("/{id}/role")
    @PreAuthorize(Authorities.SUPER_ADMIN)
    public UserResponse changeRole(@AuthenticationPrincipal AppUserPrincipal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        return userService.changeRole(principal.getId(), id, request.role());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Authorities.SUPER_ADMIN)
    public UserResponse updateStatus(@AuthenticationPrincipal AppUserPrincipal principal,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdateStatusRequest request) {
        return userService.updateStatus(principal.getId(), id, request.status());
    }
}
