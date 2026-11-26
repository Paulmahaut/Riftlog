package com.riftlog.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.PlayerSummary;
import com.riftlog.dto.UserResponse;
import com.riftlog.dto.UserSettingsRequest;
import com.riftlog.security.AuthenticatedUser;
import com.riftlog.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<PlayerSummary> listPlayers() {
        return userService.listPlayers();
    }

    @PutMapping("/me/settings")
    public UserResponse updateSettings(@Valid @RequestBody UserSettingsRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.updateSettings(currentUser.id(), request);
    }
}
