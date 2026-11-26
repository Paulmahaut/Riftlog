package com.riftlog.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.dto.GuestResponse;
import com.riftlog.entity.GuestPlayer;
import com.riftlog.entity.User;
import com.riftlog.exception.BadRequestException;
import com.riftlog.repository.GuestPlayerRepository;

@Service
public class GuestService {

    private static final Logger log = LoggerFactory.getLogger(GuestService.class);

    private final GuestPlayerRepository guestPlayerRepository;
    private final UserService userService;

    public GuestService(GuestPlayerRepository guestPlayerRepository, UserService userService) {
        this.guestPlayerRepository = guestPlayerRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<GuestResponse> listAll() {
        return guestPlayerRepository.findAllByOrderByNameAsc().stream().map(GuestService::toResponse).toList();
    }

    @Transactional
    public GuestResponse create(String name, Long creatorId) {
        return toResponse(findOrCreate(name, userService.require(creatorId)));
    }

    /** Guests are matched by name, ignoring case, so "thomas" and "Thomas" are the same person. */
    public GuestPlayer findOrCreate(String name, User creator) {
        String trimmed = name.trim();
        return guestPlayerRepository.findFirstByNameIgnoreCase(trimmed).orElseGet(() -> {
            GuestPlayer guest = new GuestPlayer();
            guest.setName(trimmed);
            guest.setCreatedBy(creator);
            guest.setCreatedAt(Instant.now());
            GuestPlayer saved = guestPlayerRepository.save(guest);
            log.info("User {} added guest player {} ({})", creator.getId(), saved.getId(), saved.getName());
            return saved;
        });
    }

    public GuestPlayer require(Long id) {
        return guestPlayerRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Unknown guest player " + id));
    }

    private static GuestResponse toResponse(GuestPlayer guest) {
        return new GuestResponse(guest.getId(), guest.getName());
    }
}
