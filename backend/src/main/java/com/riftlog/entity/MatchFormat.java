package com.riftlog.entity;

public enum MatchFormat {
    BO1(1, 1),
    BO3(2, 3);

    private final int gamesToWin;
    private final int maxGames;

    MatchFormat(int gamesToWin, int maxGames) {
        this.gamesToWin = gamesToWin;
        this.maxGames = maxGames;
    }

    public int gamesToWin() {
        return gamesToWin;
    }

    public int maxGames() {
        return maxGames;
    }
}
