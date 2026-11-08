package com.accountshield.controller;

import com.accountshield.dto.request.UpdateRoleRequest;
import com.accountshield.dto.request.UpdateStatusRequest;
import com.accountshield.dto.response.PageResponse;
import com.accountshield.dto.response.UserResponse;
import com.accountshield.exception.BadRequestException;
import com.accountshield.security.UserPrincipal;
import com.accountshield.service.AdminService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "email", "createdAt");

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public PageResponse<UserResponse> listUsers(@ParameterObject @SortDefault(sort = "id") Pageable pageable) {
        validateSort(pageable.getSort());
        return adminService.listUsers(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return adminService.getUser(id);
    }

    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(@AuthenticationPrincipal UserPrincipal admin,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdateStatusRequest request) {
        return adminService.changeStatus(admin.getId(), id, request);
    }

    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@AuthenticationPrincipal UserPrincipal admin,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        return adminService.changeRole(admin.getId(), id, request);
    }

    @PostMapping("/{id}/unlock")
    public UserResponse unlockUser(@PathVariable Long id) {
        return adminService.unlockUser(id);
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException(
                        "Invalid sort field '" + order.getProperty() + "'. Allowed: id, name, email, createdAt");
            }
        }
    }
}
