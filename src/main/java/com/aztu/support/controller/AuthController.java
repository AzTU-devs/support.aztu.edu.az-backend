package com.aztu.support.controller;

import com.aztu.support.dto.auth.AuthResponse;
import com.aztu.support.dto.auth.ForgotPasswordRequest;
import com.aztu.support.dto.auth.LoginRequest;
import com.aztu.support.dto.auth.RefreshRequest;
import com.aztu.support.dto.auth.RegisterRequest;
import com.aztu.support.dto.auth.ResetPasswordRequest;
import com.aztu.support.dto.auth.TokenRequest;
import com.aztu.support.dto.common.MessageResponse;
import com.aztu.support.dto.user.UserResponse;
import com.aztu.support.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
        authService.verifyEmail(request.token());
        return MessageResponse.of("Your email has been verified. An administrator will review your account.");
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerificationEmail(request.email());
        return MessageResponse.of("If the account exists and is unverified, a new verification email has been sent.");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return MessageResponse.of("Signed out.");
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return MessageResponse.of("If an account exists for that email, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return MessageResponse.of("Your password has been reset. You can now sign in.");
    }
}
