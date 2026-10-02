package com.hrms.service;

import com.hrms.dto.ApiResponse;
import com.hrms.entity.AppUser;
import com.hrms.repository.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AppUserRepository appUserRepository, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
    }

    public ApiResponse<Map<String, String>> login(String username, String password) {
        Optional<AppUser> userOpt = appUserRepository.findByUsername(username);
        if (userOpt.isEmpty() || userOpt.get().getIsActive() == null || userOpt.get().getIsActive() != 1
            || !passwordMatches(password, userOpt.get().getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        AppUser user = userOpt.get();
        user.setLastLoginAt(LocalDateTime.now());
        appUserRepository.save(user);
        Map<String, String> payload = new HashMap<>();
        payload.put("token", jwtService.generateToken(user));
        payload.put("role", user.getRole());
        payload.put("username", user.getUsername());
        return new ApiResponse<>(true, "Authentication successful", payload);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return MessageDigest.isEqual(
                rawPassword.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }
}
