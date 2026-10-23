package com.example.backend.user.controller;

import com.example.backend.user.dto.UpdateUserRequest;
import com.example.backend.user.dto.UserResponse;
import com.example.backend.user.service.UserService;
import com.example.backend.common.response.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UUID userId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", userService.getCurrentUser(userId))
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success("User updated successfully", userService.updateCurrentUser(
                        userId,
                        request
                ))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable UUID id
    ) {

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", userService.getUserById(id))
        );
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getByUsername(
            @PathVariable String username
    ) {

        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", userService.getUserByUsername(username))
        );
    }

    @PostMapping("/me/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @AuthenticationPrincipal UUID userId
    ) {

        userService.deactivateUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully", null)
        );
    }
}