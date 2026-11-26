package com.riftlog.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.PlayerSummary;
import com.riftlog.dto.UserResponse;
import com.riftlog.dto.UserSettingsRequest;
import com.riftlog.entity.User;
import com.riftlog.exception.NotFoundException;
import com.riftlog.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Every registered player: the whole group can see each other (SPEC-v2 §2). */
    @Transactional(readOnly = true)
    public List<PlayerSummary> listPlayers() {
        return userRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(user -> new PlayerSummary(user.getId(), user.getDisplayName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return toResponse(require(userId));
    }

    @Transactional
    public UserResponse updateSettings(Long userId, UserSettingsRequest request) {
        User user = require(userId);
        user.setDefaultFormat(request.defaultFormat());
        return toResponse(user);
    }

    public User require(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getDefaultFormat());
    }
}
