package com.example.backend.user.dto;

import com.example.backend.user.enums.Role;
import com.example.backend.user.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(

        UUID id,

        String email,

        String username,

        Role role,

        UserStatus status,

        boolean emailVerified,

        LocalDateTime createdAt

) {
}