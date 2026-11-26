package com.riftlog.entity;

public enum GameResult {
    SEAT1,
    SEAT2,
    DRAW;

    /** Winning seat (1 or 2), or null for a draw. */
    public Integer winnerSeat() {
        return switch (this) {
            case SEAT1 -> 1;
            case SEAT2 -> 2;
            case DRAW -> null;
        };
    }
}
