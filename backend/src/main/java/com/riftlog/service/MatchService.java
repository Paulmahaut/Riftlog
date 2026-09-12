package com.riftlog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.riftlog.dto.CreateMatchRequest;
import com.riftlog.dto.MatchResponse;
import com.riftlog.dto.RoundRequest;
import com.riftlog.dto.RoundResponse;
import com.riftlog.entity.Deck;
import com.riftlog.entity.Legend;
import com.riftlog.entity.Match;
import com.riftlog.entity.MatchRound;
import com.riftlog.entity.Player;
import com.riftlog.entity.Result;
import com.riftlog.entity.User;
import com.riftlog.exception.InvalidMatchException;
import com.riftlog.exception.NotFoundException;
import com.riftlog.repository.MatchRepository;
import com.riftlog.repository.UserRepository;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);
    private static final int WINNING_SCORE = 8;

    private final MatchRepository matchRepository;
    private final PlayerService playerService;
    private final LegendService legendService;
    private final DeckService deckService;
    private final UserRepository userRepository;

    public MatchService(MatchRepository matchRepository, PlayerService playerService,
                         LegendService legendService, DeckService deckService, UserRepository userRepository) {
        this.matchRepository = matchRepository;
        this.playerService = playerService;
        this.legendService = legendService;
        this.deckService = deckService;
        this.userRepository = userRepository;
    }

    public MatchResponse logMatch(CreateMatchRequest request, Long ownerId) {
        List<RoundRequest> rounds = request.rounds();
        RoundRequest lastRound = rounds.get(rounds.size() - 1);
        if (lastRound.myScore() < WINNING_SCORE && lastRound.opponentScore() < WINNING_SCORE) {
            throw new InvalidMatchException("Match must end with one side reaching " + WINNING_SCORE + " points");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("User " + ownerId + " not found"));
        Player opponent = playerService.findOrCreateByName(request.opponentName());
        Legend myLegend = legendService.findOrCreateByName(request.myLegendName());
        Legend opponentLegend = legendService.findOrCreateByName(request.opponentLegendName());
        Deck myDeck = deckService.findOrCreate(request.myDeckName(), myLegend, owner);
        Deck opponentDeck = deckService.findOrCreate(request.opponentDeckName(), opponentLegend, owner);

        Match match = new Match();
        match.setPlayedAt(LocalDateTime.now());
        match.setOwner(owner);
        match.setOpponent(opponent);
        match.setMyDeck(myDeck);
        match.setOpponentDeck(opponentDeck);
        match.setMyFinalScore(lastRound.myScore());
        match.setOpponentFinalScore(lastRound.opponentScore());
        match.setResult(lastRound.myScore() > lastRound.opponentScore() ? Result.WIN : Result.LOSS);

        for (RoundRequest roundRequest : rounds) {
            MatchRound round = new MatchRound();
            round.setMatch(match);
            round.setRoundNumber(roundRequest.roundNumber());
            round.setMyScore(roundRequest.myScore());
            round.setOpponentScore(roundRequest.opponentScore());
            match.getRounds().add(round);
        }

        Match saved = matchRepository.save(match);
        log.info("User {} logged match {} vs {} -> {} ({}-{})", ownerId, saved.getId(), opponent.getName(),
                saved.getResult(), saved.getMyFinalScore(), saved.getOpponentFinalScore());
        return toResponse(saved);
    }

    public MatchResponse getMatch(Long id, Long ownerId) {
        Match match = matchRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException("Match " + id + " not found"));
        return toResponse(match);
    }

    public List<MatchResponse> listMatches(Long opponentId, Long deckId, Long ownerId) {
        List<Match> matches;
        if (opponentId != null) {
            matches = matchRepository.findByOwnerIdAndOpponentId(ownerId, opponentId);
        } else if (deckId != null) {
            matches = matchRepository.findByOwnerIdAndMyDeckId(ownerId, deckId);
        } else {
            matches = matchRepository.findByOwnerId(ownerId);
        }
        return matches.stream().map(this::toResponse).toList();
    }

    private MatchResponse toResponse(Match match) {
        List<RoundResponse> rounds = match.getRounds().stream()
                .map(round -> new RoundResponse(round.getId(), round.getRoundNumber(), round.getMyScore(), round.getOpponentScore()))
                .toList();
        return new MatchResponse(
                match.getId(),
                match.getPlayedAt(),
                match.getOpponent().getName(),
                match.getMyDeck().getLegend().getName(),
                match.getMyDeck().getName(),
                match.getOpponentDeck().getLegend().getName(),
                match.getOpponentDeck().getName(),
                match.getResult().name(),
                match.getMyFinalScore(),
                match.getOpponentFinalScore(),
                rounds
        );
    }
}
