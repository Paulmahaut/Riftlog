package com.riftlog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.riftlog.dto.LegendRequest;
import com.riftlog.dto.LegendResponse;
import com.riftlog.service.LegendService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/legends")
public class LegendController {

    private final LegendService legendService;

    public LegendController(LegendService legendService) {
        this.legendService = legendService;
    }

    @GetMapping
    public List<LegendResponse> listAll() {
        return legendService.listAll();
    }

    @PostMapping
    public ResponseEntity<LegendResponse> create(@Valid @RequestBody LegendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(legendService.create(request));
    }
}
