package com.example.backend.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(max = 50)
        String username

) {
}