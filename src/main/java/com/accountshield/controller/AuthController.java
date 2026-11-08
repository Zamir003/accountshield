package com.accountshield.controller;

import com.accountshield.dto.request.LoginRequest;
import com.accountshield.dto.request.RefreshTokenRequest;
import com.accountshield.dto.request.RegisterRequest;
import com.accountshield.dto.request.ResendVerificationRequest;
import com.accountshield.dto.request.VerifyEmailRequest;
import com.accountshield.dto.response.AuthResponse;
import com.accountshield.dto.response.MessageResponse;
import com.accountshield.dto.response.UserResponse;
import com.accountshield.service.AuthService;
import com.accountshield.service.EmailVerificationService;
import com.accountshield.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirements
public class AuthController {

    private static final String RESEND_RESPONSE_MESSAGE =
            "If the account exists and is not verified, a new verification token has been sent.";

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService,
                          EmailVerificationService emailVerificationService,
                          RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify")
    public MessageResponse verify(@Valid @RequestBody VerifyEmailRequest request) {
        String message = switch (emailVerificationService.verify(request.token())) {
            case VERIFIED -> "Email verified successfully. You can now log in.";
            case ALREADY_VERIFIED -> "Email is already verified.";
        };
        return new MessageResponse(message);
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request.email());
        return new MessageResponse(RESEND_RESPONSE_MESSAGE);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }
}
