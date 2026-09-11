package com.riftlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.riftlog.dto.CreateMatchRequest;
import com.riftlog.dto.MatchResponse;
import com.riftlog.dto.RoundRequest;
import com.riftlog.entity.Deck;
import com.riftlog.entity.Legend;
import com.riftlog.entity.Match;
import com.riftlog.entity.Player;
import com.riftlog.entity.Result;
import com.riftlog.exception.InvalidMatchException;
import com.riftlog.exception.NotFoundException;
import com.riftlog.repository.MatchRepository;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;
    @Mock
    private PlayerService playerService;
    @Mock
    private LegendService legendService;
    @Mock
    private DeckService deckService;

    @InjectMocks
    private MatchService matchService;

    private Player opponent;
    private Legend myLegend;
    private Legend opponentLegend;
    private Deck myDeck;
    private Deck opponentDeck;

    @BeforeEach
    void setUp() {
        opponent = new Player();
        opponent.setId(1L);
        opponent.setName("Julien");

        myLegend = new Legend();
        myLegend.setId(1L);
        myLegend.setName("Ashe");

        opponentLegend = new Legend();
        opponentLegend.setId(2L);
        opponentLegend.setName("Viktor");

        myDeck = new Deck();
        myDeck.setId(1L);
        myDeck.setName("Ashe Aggro");
        myDeck.setLegend(myLegend);

        opponentDeck = new Deck();
        opponentDeck.setId(2L);
        opponentDeck.setName("Viktor Control");
        opponentDeck.setLegend(opponentLegend);
    }

    private CreateMatchRequest requestWithRounds(RoundRequest... rounds) {
        return new CreateMatchRequest("Julien", "Ashe", "Ashe Aggro", "Viktor", "Viktor Control", List.of(rounds));
    }

    private void stubResolution() {
        when(playerService.findOrCreateByName("Julien")).thenReturn(opponent);
        when(legendService.findOrCreateByName("Ashe")).thenReturn(myLegend);
        when(legendService.findOrCreateByName("Viktor")).thenReturn(opponentLegend);
        when(deckService.findOrCreate("Ashe Aggro", myLegend)).thenReturn(myDeck);
        when(deckService.findOrCreate("Viktor Control", opponentLegend)).thenReturn(opponentDeck);
        when(matchRepository.save(any(Match.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void logMatch_winWhenMyScoreReaches8() {
        stubResolution();
        CreateMatchRequest request = requestWithRounds(
                new RoundRequest(1, 3, 0),
                new RoundRequest(2, 8, 4)
        );

        MatchResponse response = matchService.logMatch(request);

        assertEquals("WIN", response.result());
        assertEquals(8, response.myFinalScore());
        assertEquals(4, response.opponentFinalScore());
        assertEquals(2, response.rounds().size());
        verify(playerService).findOrCreateByName("Julien");
        verify(deckService).findOrCreate("Ashe Aggro", myLegend);
    }

    @Test
    void logMatch_lossWhenOpponentScoreReaches8() {
        stubResolution();
        CreateMatchRequest request = requestWithRounds(new RoundRequest(1, 4, 8));

        MatchResponse response = matchService.logMatch(request);

        assertEquals("LOSS", response.result());
    }

    @Test
    void logMatch_rejectsMatchWhereNeitherSideReaches8() {
        CreateMatchRequest request = requestWithRounds(new RoundRequest(1, 3, 2));

        assertThrows(InvalidMatchException.class, () -> matchService.logMatch(request));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void getMatch_returnsMappedResponseWhenFound() {
        Match match = new Match();
        match.setId(1L);
        match.setPlayedAt(LocalDateTime.now());
        match.setOpponent(opponent);
        match.setMyDeck(myDeck);
        match.setOpponentDeck(opponentDeck);
        match.setResult(Result.WIN);
        match.setMyFinalScore(8);
        match.setOpponentFinalScore(4);
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        MatchResponse response = matchService.getMatch(1L);

        assertEquals("Julien", response.opponentName());
        assertEquals("WIN", response.result());
    }

    @Test
    void getMatch_throwsWhenNotFound() {
        when(matchRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> matchService.getMatch(42L));
    }

    @Test
    void listMatches_filtersByOpponentWhenGiven() {
        when(matchRepository.findByOpponentId(1L)).thenReturn(List.of());

        matchService.listMatches(1L, null);

        verify(matchRepository).findByOpponentId(1L);
        verify(matchRepository, never()).findAll();
    }

    @Test
    void listMatches_filtersByDeckWhenOnlyDeckGiven() {
        when(matchRepository.findByMyDeckId(2L)).thenReturn(List.of());

        matchService.listMatches(null, 2L);

        verify(matchRepository).findByMyDeckId(2L);
    }

    @Test
    void listMatches_returnsAllWhenNoFilterGiven() {
        when(matchRepository.findAll()).thenReturn(List.of());

        matchService.listMatches(null, null);

        verify(matchRepository).findAll();
    }
}
