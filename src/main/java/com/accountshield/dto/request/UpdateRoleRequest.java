package com.accountshield.dto.request;

import com.accountshield.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(

        @NotNull(message = "Role is required")
        Role role
) {
}
