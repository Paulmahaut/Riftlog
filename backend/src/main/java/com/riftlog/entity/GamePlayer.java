package com.riftlog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Per-seat details of a game, mostly filled in after the game. */
@Entity
@Table(name = "game_players")
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private int seat;

    @Column(nullable = false)
    private int finalScore;

    @ManyToOne
    @JoinColumn(name = "battlefield_card_id")
    private Card battlefield;

    private Integer mulliganCount;

    private String notes;

    public Long getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int getSeat() {
        return seat;
    }

    public void setSeat(int seat) {
        this.seat = seat;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

    public Card getBattlefield() {
        return battlefield;
    }

    public void setBattlefield(Card battlefield) {
        this.battlefield = battlefield;
    }

    public Integer getMulliganCount() {
        return mulliganCount;
    }

    public void setMulliganCount(Integer mulliganCount) {
        this.mulliganCount = mulliganCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
