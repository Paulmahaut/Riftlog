package com.riftlog.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "opponent_id", nullable = false)
    private Player opponent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "my_deck_id", nullable = false)
    private Deck myDeck;

    @ManyToOne(optional = false)
    @JoinColumn(name = "opponent_deck_id", nullable = false)
    private Deck opponentDeck;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Result result;

    @Column(nullable = false)
    private int myFinalScore;

    @Column(nullable = false)
    private int opponentFinalScore;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundNumber ASC")
    private List<MatchRound> rounds = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }

    public Player getOpponent() {
        return opponent;
    }

    public void setOpponent(Player opponent) {
        this.opponent = opponent;
    }

    public Deck getMyDeck() {
        return myDeck;
    }

    public void setMyDeck(Deck myDeck) {
        this.myDeck = myDeck;
    }

    public Deck getOpponentDeck() {
        return opponentDeck;
    }

    public void setOpponentDeck(Deck opponentDeck) {
        this.opponentDeck = opponentDeck;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public int getMyFinalScore() {
        return myFinalScore;
    }

    public void setMyFinalScore(int myFinalScore) {
        this.myFinalScore = myFinalScore;
    }

    public int getOpponentFinalScore() {
        return opponentFinalScore;
    }

    public void setOpponentFinalScore(int opponentFinalScore) {
        this.opponentFinalScore = opponentFinalScore;
    }

    public List<MatchRound> getRounds() {
        return rounds;
    }
}
