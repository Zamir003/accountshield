package com.accountshield.controller;

import com.accountshield.dto.request.UpdateProfileRequest;
import com.accountshield.dto.response.UserResponse;
import com.accountshield.security.UserPrincipal;
import com.accountshield.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getMyProfile(principal.getId());
    }

    @PutMapping
    public UserResponse updateMyProfile(@AuthenticationPrincipal UserPrincipal principal,
                                        @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateMyProfile(principal.getId(), request);
    }
}
