package com.riftlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.riftlog.dto.StatsResponse;
import com.riftlog.entity.Deck;
import com.riftlog.entity.Legend;
import com.riftlog.entity.Match;
import com.riftlog.entity.Result;
import com.riftlog.repository.MatchRepository;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private StatsService statsService;

    private Deck deck(String name, String legendName) {
        Legend legend = new Legend();
        legend.setName(legendName);
        Deck deck = new Deck();
        deck.setName(name);
        deck.setLegend(legend);
        return deck;
    }

    private Match match(Deck myDeck, Deck opponentDeck, Result result) {
        Match match = new Match();
        match.setMyDeck(myDeck);
        match.setOpponentDeck(opponentDeck);
        match.setResult(result);
        return match;
    }

    @Test
    void computeStats_returnsZeroesWhenNoMatches() {
        when(matchRepository.findAll()).thenReturn(List.of());

        StatsResponse stats = statsService.computeStats();

        assertEquals(0, stats.totalMatches());
        assertEquals(0.0, stats.overallWinRate());
        assertTrue(stats.winRateByDeck().isEmpty());
        assertTrue(stats.winRateByMatchup().isEmpty());
    }

    @Test
    void computeStats_computesRatesAcrossDecksAndMatchups() {
        Deck asheAggro = deck("Ashe Aggro", "Ashe");
        Deck viktorControl = deck("Viktor Control", "Viktor");
        Deck zoeTempo = deck("Zoe Tempo", "Zoe");

        when(matchRepository.findAll()).thenReturn(List.of(
                match(asheAggro, viktorControl, Result.WIN),
                match(asheAggro, zoeTempo, Result.LOSS)
        ));

        StatsResponse stats = statsService.computeStats();

        assertEquals(2, stats.totalMatches());
        assertEquals(0.5, stats.overallWinRate());
        assertEquals(0.5, stats.winRateByDeck().get("Ashe Aggro"));
        assertEquals(1.0, stats.winRateByMatchup().get("Ashe Aggro vs Viktor Control"));
        assertEquals(0.0, stats.winRateByMatchup().get("Ashe Aggro vs Zoe Tempo"));
    }
}
