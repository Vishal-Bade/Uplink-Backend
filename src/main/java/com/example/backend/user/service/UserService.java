package com.example.backend.user.service;

import com.example.backend.user.dto.UpdateUserRequest;
import com.example.backend.user.dto.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(UUID userId);

    UserResponse getUserById(UUID userId);

    UserResponse getUserByUsername(String username);

    UserResponse updateCurrentUser(
            UUID userId,
            UpdateUserRequest request
    );

    void deactivateUser(UUID userId);
}