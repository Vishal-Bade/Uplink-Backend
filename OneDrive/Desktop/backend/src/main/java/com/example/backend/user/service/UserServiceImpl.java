package com.example.backend.user.service;

import com.example.backend.user.dto.UpdateUserRequest;
import com.example.backend.user.dto.UserResponse;
import com.example.backend.user.entity.User;
import com.example.backend.user.enums.UserStatus;
import com.example.backend.user.mapper.UserMapper;
import com.example.backend.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import com.example.backend.common.exception.ResourceNotFoundException;
import com.example.backend.common.exception.ConflictException;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {

        User user = findUser(userId);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {

        User user = findUser(userId);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse updateCurrentUser(
            UUID userId,
            UpdateUserRequest request
    ) {

        User user = findUser(userId);

        if (request.username() != null &&
                !request.username().equals(user.getUsername())) {

            if (userRepository.existsByUsername(request.username())) {
                throw new ConflictException(
                        "Username already exists"
                );
            }

            user.setUsername(request.username());
        }

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Override
    public void deactivateUser(UUID userId) {

        User user = findUser(userId);

        user.setStatus(UserStatus.DISABLED);

        user.setDeletedAt(null);

        userRepository.save(user);
    }

    private User findUser(UUID userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }
}