package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.LiveWinProbability;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Reads MLB's per-play win probability for a game in progress.
 *
 * <p>The pre-game model in {@link WinProbabilityService} weighs season-long form and
 * cannot move once first pitch lands. This does the opposite: it knows nothing about
 * season quality and everything about the score, the inning, the outs and who is on
 * base, which is what actually decides a game once it is being played.</p>
 */
@Service
public class LiveWinProbabilityService {

    private static final Logger log = LoggerFactory.getLogger(LiveWinProbabilityService.class);

    private final MlbApiService api;

    public LiveWinProbabilityService(MlbApiService api) {
        this.api = api;
    }

    /**
     * The latest scored play's probabilities, or empty when the game has not started or
     * MLB has not scored a play yet.
     */
    public Optional<LiveWinProbability> forGame(Game game) {
        if (game == null || game.getMlbGameId() == null) return Optional.empty();

        try {
            JsonNode plays = api.fetchWinProbability(game.getMlbGameId());
            if (plays == null || !plays.isArray() || plays.isEmpty()) return Optional.empty();

            // Walk back from the end: the last entries can be a pitching change or other
            // action with no probability attached yet.
            for (int i = plays.size() - 1; i >= 0; i--) {
                JsonNode play = plays.get(i);
                JsonNode home = play.path("homeTeamWinProbability");
                JsonNode away = play.path("awayTeamWinProbability");
                if (home.isMissingNode() || away.isMissingNode() || home.isNull() || away.isNull()) {
                    continue;
                }

                return Optional.of(new LiveWinProbability(
                        (int) Math.round(away.asDouble()),
                        (int) Math.round(home.asDouble()),
                        doubleOrNull(play, "leverageIndex"),
                        play.path("result").path("description").asText(null),
                        doubleOrNull(play, "homeTeamWinProbabilityAdded"),
                        plays.size()
                ));
            }
            return Optional.empty();
        } catch (Exception e) {
            // A missing win probability should never take the dashboard down with it.
            log.warn("Live win probability unavailable for game {}: {}",
                    game.getMlbGameId(), e.getMessage());
            return Optional.empty();
        }
    }

    private Double doubleOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return (v.isMissingNode() || v.isNull()) ? null : v.asDouble();
    }
}
