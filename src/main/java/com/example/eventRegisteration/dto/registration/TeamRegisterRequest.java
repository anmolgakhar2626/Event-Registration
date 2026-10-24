package com.example.eventRegisteration.dto.registration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record TeamRegisterRequest(
        @NotEmpty(message = "Member emails are required")
        List<String> memberEmails,

        @Min(value = 2, message = "Team must have at least 2 members")
        int memberCount
) {}
