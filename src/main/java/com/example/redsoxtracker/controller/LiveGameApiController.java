package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.LiveGameDetail;
import com.example.redsoxtracker.dto.ScoreboardView;
import com.example.redsoxtracker.repository.GameRepository;
import com.example.redsoxtracker.service.LiveGameDetailService;
import com.example.redsoxtracker.service.LiveGameSyncService;
import com.example.redsoxtracker.service.LiveScoreboardService;
import com.example.redsoxtracker.service.MatchupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final LiveGameSyncService liveGameSyncService;
    private final GameRepository gameRepository;

    public LiveGameApiController(MatchupService matchupService,
                                 LiveScoreboardService liveScoreboardService,
                                 LiveGameDetailService liveGameDetailService,
                                 LiveGameSyncService liveGameSyncService,
                                 GameRepository gameRepository) {
        this.matchupService = matchupService;
        this.liveScoreboardService = liveScoreboardService;
        this.liveGameDetailService = liveGameDetailService;
        this.liveGameSyncService = liveGameSyncService;
        this.gameRepository = gameRepository;
    }

    /**
     * Detail for one specific game rather than whatever is featured, so the Previous Game
     * box can show its own play by play. Finished games never change, so this one caches.
     */
    @GetMapping("/api/game-detail")
    public ResponseEntity<Map<String, Object>> gameDetail(@RequestParam("gameId") Integer gameId) {
        Map<String, Object> body = new LinkedHashMap<>();
        Optional<Game> game = gameRepository.findByMlbGameId(gameId);
        if (game.isEmpty()) {
            body.put("available", false);
            return ResponseEntity.ok(body);
        }

        Optional<LiveGameDetail> detail = liveGameDetailService.buildForGame(game.get());
        body.put("available", detail.isPresent());
        detail.ifPresent(dd -> body.put("detail", dd));

        boolean settled = "Final".equals(game.get().getStatus());
        return ResponseEntity.ok()
                .header("Cache-Control", settled ? "public, max-age=600" : "no-store")
                .body(body);
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
        Optional<ScoreboardView> scoreboard = liveScoreboardService.buildForGame(game);

        // Write the feed back onto the stored row first, so the headline, the linescore,
        // the games list and the record all read the same numbers.
        scoreboard.ifPresent(s -> liveGameSyncService.apply(game, s));

        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        Integer awayScore = redSoxAreAway ? game.getRedSoxScore() : game.getOpponentScore();
        Integer homeScore = redSoxAreAway ? game.getOpponentScore() : game.getRedSoxScore();
        // The linescore is the freshest thing we have; prefer it outright.
        if (scoreboard.isPresent()) {
            if (scoreboard.get().getAwayRuns() != null) awayScore = scoreboard.get().getAwayRuns();
            if (scoreboard.get().getHomeRuns() != null) homeScore = scoreboard.get().getHomeRuns();
        }

        body.put("available", true);
        body.put("gameId", game.getMlbGameId());
        body.put("status", game.getStatus());
        body.put("live", "In Progress".equals(game.getStatus()));
        body.put("awayScore", awayScore);
        body.put("homeScore", homeScore);

        scoreboard.ifPresent(s -> body.put("scoreboard", s));

        Optional<LiveGameDetail> detail = liveGameDetailService.buildForGame(game);
        detail.ifPresent(d -> body.put("detail", d));

        // Always fresh: a cached response is the one thing this endpoint must never serve.
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(body);
    }
}
