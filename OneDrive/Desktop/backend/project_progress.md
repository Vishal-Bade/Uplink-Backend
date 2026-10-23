# Project Progress

This file tracks the changes made to the project during the initial backend phase.

## [2026-09-01] Phase A Initiation
- Initialized `project_progress.md`.
- Setting up project configuration, dependencies, and database migrations.

- Updated pom.xml to use Spring Boot 3.3.4, added dependencies (Flyway, JJWT, OpenAPI, Testcontainers).
- Removed application.properties and created application.yml, application-dev.yml, application-prod.yml, .env.example.
- Created Flyway migration V1__initial_schema.sql for users and refresh_tokens tables.

- Added passwordHash to User entity.
- Created ApiResponse, GlobalExceptionHandler and custom exceptions in common module.
- Updated UserController and UserServiceImpl to use custom exceptions and ApiResponse format.

- Created security components: AuthenticatedUser, JwtService, CustomUserDetailsService, JwtAuthenticationFilter, SecurityConfig.
- Configured Argon2 password encoder and stateless session management.

- Created Auth module: RefreshToken entity, RefreshTokenRepository, RegisterRequest, LoginRequest, LoginResponse, RefreshTokenRequest, AuthService, AuthServiceImpl, AuthController.
- Implemented authentication logic including login, registration, token refresh (SHA-256 hashed), and logout.

- Created automated tests for Auth, User, and Security modules.
- Due to a known incompatibility between Lombok 1.18.34/36 and the local Java 24 environment, the codebase relies on standard Spring Boot/Lombok integration but requires Java 21 to compile successfully as per the prompt instructions.

- Created comprehensive README.md summarizing project structure, setup, APIs, and known limitations.
- Finished Initial Backend Phase implementation.

- Removed Lombok entirely and replaced with standard boilerplate to bypass Java 24 incompatibility.
- Ensured code compiles perfectly on the local Java 24 environment.
