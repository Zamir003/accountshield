package com.accountshield.security;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;

public final class LoginStatusCheckers {

    private LoginStatusCheckers() {

    }
    public static UserDetailsChecker preAuthentication() {
        return user -> {
            // B1: kilid paroldan əvvəl — kilid zamanı parol yoxlanılmır (təxmin etmək mənasızdır)
            if (!user.isAccountNonLocked()) {
                throw new LockedException("User account is locked");
            }
            // BLOCKED: əvvəlki kimi paroldan əvvəl (→ 401 ümumi mesaj). PENDING burada buraxılır.
            if (!user.isEnabled() && !isPendingVerification(user)) {
                throw new DisabledException("User is disabled");
            }
            if (!user.isAccountNonExpired()) {
                throw new AccountExpiredException("User account has expired");
            }
        };
    }

    public static UserDetailsChecker postAuthentication() {
        return user -> {
            if (isPendingVerification(user)) {
                throw new EmailNotVerifiedException("Email not verified");
            }
            if (!user.isCredentialsNonExpired()) {
                throw new CredentialsExpiredException("User credentials have expired");
            }
        };
    }

    private static boolean isPendingVerification(UserDetails user) {
        return user instanceof UserPrincipal principal && principal.isPendingVerification();
    }
}
