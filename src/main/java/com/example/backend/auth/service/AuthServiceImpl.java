package com.example.backend.auth.service;

import com.example.backend.auth.dto.LoginRequest;
import com.example.backend.auth.dto.LoginResponse;
import com.example.backend.auth.dto.RefreshTokenRequest;
import com.example.backend.auth.dto.RegisterRequest;
import com.example.backend.auth.entity.RefreshToken;
import com.example.backend.auth.repository.RefreshTokenRepository;
import com.example.backend.common.exception.BadRequestException;
import com.example.backend.common.exception.ConflictException;
import com.example.backend.security.AuthenticatedUser;
import com.example.backend.security.JwtService;
import com.example.backend.user.dto.UserResponse;
import com.example.backend.user.entity.User;
import com.example.backend.user.enums.Role;
import com.example.backend.user.enums.UserStatus;
import com.example.backend.user.mapper.UserMapper;
import com.example.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder, JwtService jwtService,
                           AuthenticationManager authenticationManager, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
    }

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username already exists");
        }

        Role role = request.role();
        if (role == null) {
            role = Role.STUDENT;
        } else if (role == Role.ADMIN) {
            throw new BadRequestException("Cannot register as ADMIN");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Authenticate the user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (user.getStatus() == UserStatus.DISABLED || user.getStatus() == UserStatus.DELETED) {
            throw new BadRequestException("Account is disabled or deleted");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        String refreshTokenString = UUID.randomUUID().toString();
        String refreshTokenHash = hashRefreshToken(refreshTokenString);

        // Invalidate old tokens (simple approach for now)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(refreshTokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plus(refreshExpiration, java.time.temporal.ChronoUnit.MILLIS));
        
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(
                jwtToken,
                refreshTokenString,
                userMapper.toResponse(user)
        );
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashRefreshToken(request.refreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null) {
            throw new BadRequestException("Refresh token is revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh token is expired");
        }

        User user = refreshToken.getUser();
        if (user.getStatus() == UserStatus.DISABLED || user.getStatus() == UserStatus.DELETED) {
            throw new BadRequestException("Account is disabled or deleted");
        }

        // Issue new tokens
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
        String jwtToken = jwtService.generateToken(authenticatedUser);
        
        String newRefreshTokenString = UUID.randomUUID().toString();
        String newRefreshTokenHash = hashRefreshToken(newRefreshTokenString);

        // Rotate token
        refreshTokenRepository.delete(refreshToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(newRefreshTokenHash);
        newRefreshToken.setExpiresAt(LocalDateTime.now().plus(refreshExpiration, java.time.temporal.ChronoUnit.MILLIS));

        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponse(
                jwtToken,
                newRefreshTokenString,
                userMapper.toResponse(user)
        );
    }

    @Override
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        refreshTokenRepository.deleteByUser(user);
    }

    private String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }
}
