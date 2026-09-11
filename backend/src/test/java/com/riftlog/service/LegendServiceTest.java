package com.riftlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.riftlog.entity.Legend;
import com.riftlog.repository.LegendRepository;

@ExtendWith(MockitoExtension.class)
class LegendServiceTest {

    @Mock
    private LegendRepository legendRepository;

    @InjectMocks
    private LegendService legendService;

    @Test
    void findOrCreateByName_returnsExistingLegendWithoutSaving() {
        Legend existing = new Legend();
        existing.setId(1L);
        existing.setName("Ashe");
        when(legendRepository.findByNameIgnoreCase("ashe")).thenReturn(Optional.of(existing));

        Legend result = legendService.findOrCreateByName("ashe");

        assertEquals(existing, result);
        verify(legendRepository, never()).save(any());
    }

    @Test
    void findOrCreateByName_createsNewLegendWhenNotFound() {
        when(legendRepository.findByNameIgnoreCase("Zoe")).thenReturn(Optional.empty());
        when(legendRepository.save(any(Legend.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Legend result = legendService.findOrCreateByName("Zoe");

        assertEquals("Zoe", result.getName());
        verify(legendRepository).save(any(Legend.class));
    }
}
