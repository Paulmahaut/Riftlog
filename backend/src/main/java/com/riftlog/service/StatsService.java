package com.riftlog.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.riftlog.dto.StatsResponse;
import com.riftlog.entity.Match;
import com.riftlog.entity.Result;
import com.riftlog.repository.MatchRepository;

@Service
public class StatsService {

    private final MatchRepository matchRepository;

    public StatsService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public StatsResponse computeStats() {
        List<Match> matches = matchRepository.findAll();

        Map<String, Double> winRateByDeck = matches.stream()
                .collect(Collectors.groupingBy(match -> match.getMyDeck().getName()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> winRate(entry.getValue())));

        Map<String, Double> winRateByMatchup = matches.stream()
                .collect(Collectors.groupingBy(match -> match.getMyDeck().getName() + " vs " + match.getOpponentDeck().getName()))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> winRate(entry.getValue())));

        return new StatsResponse(matches.size(), winRate(matches), winRateByDeck, winRateByMatchup);
    }

    private double winRate(List<Match> matches) {
        if (matches.isEmpty()) {
            return 0.0;
        }
        long wins = matches.stream().filter(match -> match.getResult() == Result.WIN).count();
        return (double) wins / matches.size();
    }
}
