package com.riftlog.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.riftlog.dto.LegendRequest;
import com.riftlog.dto.LegendResponse;
import com.riftlog.entity.Legend;
import com.riftlog.repository.LegendRepository;

@Service
public class LegendService {

    private final LegendRepository legendRepository;

    public LegendService(LegendRepository legendRepository) {
        this.legendRepository = legendRepository;
    }

    public List<LegendResponse> listAll() {
        return legendRepository.findAll().stream().map(this::toResponse).toList();
    }

    public LegendResponse create(LegendRequest request) {
        return toResponse(findOrCreateByName(request.name()));
    }

    public Legend findOrCreateByName(String name) {
        return legendRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Legend legend = new Legend();
                    legend.setName(name);
                    return legendRepository.save(legend);
                });
    }

    private LegendResponse toResponse(Legend legend) {
        return new LegendResponse(legend.getId(), legend.getName());
    }
}
