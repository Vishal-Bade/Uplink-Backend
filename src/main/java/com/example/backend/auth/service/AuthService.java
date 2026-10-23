package com.example.backend.auth.service;

import com.example.backend.auth.dto.LoginRequest;
import com.example.backend.auth.dto.LoginResponse;
import com.example.backend.auth.dto.RefreshTokenRequest;
import com.example.backend.auth.dto.RegisterRequest;
import com.example.backend.user.dto.UserResponse;

import java.util.UUID;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse refreshToken(RefreshTokenRequest request);
    void logout(UUID userId);
}
