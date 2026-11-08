package com.accountshield.dto.request;

import com.accountshield.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(

        @NotNull(message = "Status is required")
        UserStatus status
) {
}
