package com.riftlog.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.GuestRequest;
import com.riftlog.dto.GuestResponse;
import com.riftlog.security.AuthenticatedUser;
import com.riftlog.service.GuestService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/guests")
public class GuestController {

    private final GuestService guestService;

    public GuestController(GuestService guestService) {
        this.guestService = guestService;
    }

    @GetMapping
    public List<GuestResponse> listAll() {
        return guestService.listAll();
    }

    /** Returns the existing guest when the name is already known. */
    @PostMapping
    public GuestResponse create(@Valid @RequestBody GuestRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return guestService.create(request.name(), currentUser.id());
    }
}
