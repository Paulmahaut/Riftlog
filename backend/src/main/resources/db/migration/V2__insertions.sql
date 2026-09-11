-- ============================================
-- LEGENDS
-- ============================================
INSERT INTO
    legends (name)
VALUES
    ('Jiro'),
    ('Ada'),
    ('Orion'),
    ('Kaya'),
    ('Paul');

-- ============================================
-- PLAYERS
-- ============================================
INSERT INTO
    players (name)
VALUES
    ('Alice'),
    ('Bob'),
    ('Charlie'),
    ('Dylan');

-- ============================================
-- DECKS
-- ============================================
INSERT INTO
    decks (name, legend_id)
VALUES
    ('Jiro Aggro', 1),
    ('Ada Control', 2),
    ('Orion Midrange', 3),
    ('Kaya Tempo', 4),
    ('Jiro Defense', 1),
    ('Ada Aggro', 2);

-- ============================================
-- MATCHES
-- ============================================
INSERT INTO
    matches (
        played_at,
        opponent_id,
        my_deck_id,
        opponent_deck_id,
        result,
        my_final_score,
        opponent_final_score
    )
VALUES
    ('2026-09-01 18:30:00', 1, 1, 2, 'WIN', 2, 1),
    ('2026-09-02 19:15:00', 2, 2, 3, 'LOSS', 1, 2),
    ('2026-09-03 20:00:00', 3, 3, 4, 'WIN', 2, 0),
    ('2026-09-04 18:45:00', 4, 4, 1, 'LOSS', 0, 2),
    ('2026-09-05 21:00:00', 1, 5, 6, 'WIN', 2, 1);

-- ============================================
-- MATCH ROUNDS
-- ============================================
-- Match 1 : WIN 2-1
INSERT INTO
    match_rounds (match_id, round_number, my_score, opponent_score)
VALUES
    (1, 1, 1, 0),
    (1, 2, 0, 1),
    (1, 3, 1, 0);

-- Match 2 : LOSS 1-2
INSERT INTO
    match_rounds (match_id, round_number, my_score, opponent_score)
VALUES
    (2, 1, 0, 1),
    (2, 2, 1, 0),
    (2, 3, 0, 1);

-- Match 3 : WIN 2-0
INSERT INTO
    match_rounds (match_id, round_number, my_score, opponent_score)
VALUES
    (3, 1, 1, 0),
    (3, 2, 1, 0);

-- Match 4 : LOSS 0-2
INSERT INTO
    match_rounds (match_id, round_number, my_score, opponent_score)
VALUES
    (4, 1, 0, 1),
    (4, 2, 0, 1);

-- Match 5 : WIN 2-1
INSERT INTO
    match_rounds (match_id, round_number, my_score, opponent_score)
VALUES
    (5, 1, 1, 0),
    (5, 2, 0, 1),
    (5, 3, 1, 0);