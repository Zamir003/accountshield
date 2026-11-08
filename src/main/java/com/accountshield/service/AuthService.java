package com.accountshield.service;

import com.accountshield.dto.request.LoginRequest;
import com.accountshield.dto.request.RegisterRequest;
import com.accountshield.dto.response.AuthResponse;
import com.accountshield.dto.response.UserResponse;
import com.accountshield.entity.User;
import com.accountshield.enums.UserStatus;
import com.accountshield.exception.EmailAlreadyExistsException;
import com.accountshield.mapper.UserMapper;
import com.accountshield.repository.UserRepository;
import com.accountshield.security.JwtService;
import com.accountshield.security.UserPrincipal;
import com.accountshield.service.RefreshTokenService.IssuedRefreshToken;
import com.accountshield.service.RefreshTokenService.RotationResult;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       LoginAttemptService loginAttemptService,
                       EmailVerificationService emailVerificationService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.emailVerificationService = emailVerificationService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = userMapper.toEntity(request, email, passwordHash);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        User saved = userRepository.save(user);

        emailVerificationService.sendVerification(saved);

        return userMapper.toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.recordFailure(email); // mövcud olmayan email üçün heç nə etmir
            throw ex;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        loginAttemptService.recordSuccess(principal.getId());

        // B3: hər login yeni refresh token family-si başladır (öz transaction-ında)
        IssuedRefreshToken refreshToken = refreshTokenService.issue(principal.getId());

        return buildAuthResponse(principal, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        RotationResult rotation = refreshTokenService.rotate(refreshToken);
        // İcazələr bazadakı cari rola görə: user rotate zamanı bazadan yüklənib
        UserPrincipal principal = UserPrincipal.from(rotation.user());
        return buildAuthResponse(principal, rotation.refreshToken());
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal, IssuedRefreshToken refreshToken) {
        String accessToken = jwtService.generateToken(principal);
        // Absolute ömür: login-də tam müddət, refresh-də qalan vaxt. Mənfi olmasın (saat sərhədi).
        long refreshExpiresIn = Math.max(0, Duration.between(Instant.now(), refreshToken.expiresAt()).toSeconds());

        return AuthResponse.bearer(accessToken, jwtService.getExpirationSeconds(),
                refreshToken.token(), refreshExpiresIn);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
