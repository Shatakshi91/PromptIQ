package com.PromptIQ.backend.user.service;
import com.aiagent.platform.auth.dto.UserResponse;
import com.aiagent.platform.auth.entity.Role;
import com.aiagent.platform.auth.entity.User;
import com.aiagent.platform.auth.repository.UserRepository;
import com.aiagent.platform.common.dto.PageResponse;
import com.aiagent.platform.common.exception.ApiException;
import com.aiagent.platform.user.dto.ChangePasswordRequest;
import com.aiagent.platform.user.dto.UpdateProfileRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        user.setDisplayName(request.displayName());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUserOrThrow(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public PageResponse<UserResponse> listUsers(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), UserResponse::from);
    }

    public UserResponse getUser(UUID userId) {
        return UserResponse.from(getUserOrThrow(userId));
    }

    @Transactional
    public UserResponse updateStatus(UUID userId, boolean enabled) {
        User user = getUserOrThrow(userId);
        user.setEnabled(enabled);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateRole(UUID userId, Role role) {
        User user = getUserOrThrow(userId);
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}