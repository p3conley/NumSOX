package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.LiveGameDetail;
import com.example.redsoxtracker.dto.ScoreboardView;
import com.example.redsoxtracker.service.LiveGameDetailService;
import com.example.redsoxtracker.service.LiveScoreboardService;
import com.example.redsoxtracker.service.MatchupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Polled by the dashboard while a game is on, so the scoreboard, the play by play and the
 * box score all move without the page reloading.
 */
@RestController
public class LiveGameApiController {

    private final MatchupService matchupService;
    private final LiveScoreboardService liveScoreboardService;
    private final LiveGameDetailService liveGameDetailService;

    public LiveGameApiController(MatchupService matchupService,
                                 LiveScoreboardService liveScoreboardService,
                                 LiveGameDetailService liveGameDetailService) {
        this.matchupService = matchupService;
        this.liveScoreboardService = liveScoreboardService;
        this.liveGameDetailService = liveGameDetailService;
    }

    @GetMapping("/api/live-game")
    public ResponseEntity<Map<String, Object>> liveGame() {
        Optional<Game> featured = matchupService.getFeaturedGame();
        Map<String, Object> body = new LinkedHashMap<>();

        if (featured.isEmpty()) {
            body.put("available", false);
            return ResponseEntity.ok(body);
        }

        Game game = featured.get();
        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());

        body.put("available", true);
        body.put("gameId", game.getMlbGameId());
        body.put("status", game.getStatus());
        body.put("live", "In Progress".equals(game.getStatus()));
        body.put("awayScore", redSoxAreAway ? game.getRedSoxScore() : game.getOpponentScore());
        body.put("homeScore", redSoxAreAway ? game.getOpponentScore() : game.getRedSoxScore());

        Optional<ScoreboardView> scoreboard = liveScoreboardService.buildForGame(game);
        scoreboard.ifPresent(s -> body.put("scoreboard", s));

        Optional<LiveGameDetail> detail = liveGameDetailService.buildForGame(game);
        detail.ifPresent(d -> body.put("detail", d));

        // Always fresh: a cached response is the one thing this endpoint must never serve.
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(body);
    }
}
