package com.riftlog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.riftlog.entity.Card;
import com.riftlog.entity.User;
import com.riftlog.security.JwtService;
import com.riftlog.support.TestData;

/** The JSON the phone will send, end to end: JWT, deserialization, validation, 201 then 200. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private TestData data;

    private String token;
    private User paul;
    private Card vi;
    private Card leblanc;

    @BeforeEach
    void setUp() {
        paul = data.user("Paul");
        vi = data.card("http-001", "Vi - Piltover Enforcer", Card.TYPE_LEGEND);
        leblanc = data.card("http-002", "LeBlanc - Deceiver", Card.TYPE_LEGEND);
        token = "Bearer " + jwtService.generateToken(paul);
    }

    /** Bo1 won 8-1 by seat 1, then post-game mulligan added on the second upload. */
    private String matchJson(UUID gameId, String mulligan) {
        StringBuilder events = new StringBuilder();
        for (int seq = 1; seq <= 9; seq++) {
            int seat = seq == 5 ? 2 : 1;
            String type = seq % 3 == 0 ? "HOLD" : seq % 3 == 1 ? "CONQUER" : "ABILITY";
            if (seq > 1) {
                events.append(',');
            }
            events.append("""
                    {"id":"%s","seq":%d,"seat":%d,"type":"%s","occurredAt":"2026-11-26T18:%02d:00Z"}"""
                    .formatted(UUID.randomUUID(), seq, seat, type, seq * 2));
        }
        return """
                {
                  "format": "BO1",
                  "source": "LIVE",
                  "playedAt": "2026-11-26T18:00:00Z",
                  "players": [
                    {"seat": 1, "userId": %d, "legendCardId": %d},
                    {"seat": 2, "guestName": "Thomas", "legendCardId": %d}
                  ],
                  "games": [{
                    "id": "%s", "number": 1, "firstSeat": 2,
                    "startedAt": "2026-11-26T18:00:00Z", "endedAt": "2026-11-26T18:20:00Z",
                    "result": "SEAT1",
                    "players": [{"seat": 2, "mulliganCount": %s}],
                    "events": [%s]
                  }]
                }
                """.formatted(paul.getId(), vi.getId(), leblanc.getId(), gameId, mulligan, events);
    }

    @Test
    void putTwice_createsThenReplaces() throws Exception {
        UUID matchId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();

        mockMvc.perform(put("/api/matches/{id}", matchId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(matchJson(gameId, "null")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.winnerSeat").value(1))
                .andExpect(jsonPath("$.games[0].players[0].finalScore").value(8))
                .andExpect(jsonPath("$.games[0].players[1].finalScore").value(1))
                .andExpect(jsonPath("$.games[0].durationSeconds").value(1200))
                .andExpect(jsonPath("$.games[0].endReason").value("POINTS"));

        mockMvc.perform(put("/api/matches/{id}", matchId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(matchJson(gameId, "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].players[1].mulliganCount").value(2));

        mockMvc.perform(get("/api/matches").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].players[1].name").value("Thomas"))
                .andExpect(jsonPath("$[0].gameWins[0]").value(1));
    }

    @Test
    void unknownEnumValue_isA400WithAMessage() throws Exception {
        String json = matchJson(UUID.randomUUID(), "null").replace("\"BO1\"", "\"BO5\"");

        mockMvc.perform(put("/api/matches/{id}", UUID.randomUUID()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/api/matches")).andExpect(status().isUnauthorized());
    }

    @Test
    void cardsCanBeFilteredByType() throws Exception {
        data.card("http-003", "Star Spring", Card.TYPE_BATTLEFIELD);

        mockMvc.perform(get("/api/cards").param("type", "battlefield").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Star Spring"));
    }

    @Test
    void cardsCanBeSearchedByName() throws Exception {
        mockMvc.perform(get("/api/cards").param("q", "LEBLANC").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("LEGEND"));
    }

    @Test
    void malformedIdIsA400AndUnknownMatchA404() throws Exception {
        mockMvc.perform(get("/api/matches/not-a-uuid").header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/matches/{id}", UUID.randomUUID()).header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void meReturnsTheDefaultFormat() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultFormat").value("BO3"));
    }
}
