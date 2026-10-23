package com.example.backend.auth.dto;

import com.example.backend.user.dto.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
