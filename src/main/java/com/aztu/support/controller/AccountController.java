package com.aztu.support.controller;

import com.aztu.support.dto.auth.ChangePasswordRequest;
import com.aztu.support.dto.common.MessageResponse;
import com.aztu.support.dto.user.UserResponse;
import com.aztu.support.security.AppUserPrincipal;
import com.aztu.support.security.Authorities;
import com.aztu.support.service.AuthService;
import com.aztu.support.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated self-service (current user profile + change password). */
@RestController
@RequestMapping("/api/account")
@PreAuthorize(Authorities.AUTHENTICATED)
public class AccountController {

    private final UserService userService;
    private final AuthService authService;

    public AccountController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.getById(principal.getId());
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return MessageResponse.of("Your password has been changed. Please sign in again on other devices.");
    }
}
