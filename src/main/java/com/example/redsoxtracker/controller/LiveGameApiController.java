package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.LiveGameDetail;
import com.example.redsoxtracker.dto.LiveWinProbability;
import com.example.redsoxtracker.dto.NumsoxModel;
import com.example.redsoxtracker.dto.ScoreboardView;
import com.example.redsoxtracker.repository.GameRepository;
import com.example.redsoxtracker.service.LiveGameDetailService;
import com.example.redsoxtracker.service.LiveGameSyncService;
import com.example.redsoxtracker.service.LiveWinProbabilityService;
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
    private final LiveWinProbabilityService liveWinProbabilityService;

    public LiveGameApiController(MatchupService matchupService,
                                 LiveScoreboardService liveScoreboardService,
                                 LiveGameDetailService liveGameDetailService,
                                 LiveGameSyncService liveGameSyncService,
                                 GameRepository gameRepository,
                                 LiveWinProbabilityService liveWinProbabilityService) {
        this.matchupService = matchupService;
        this.liveScoreboardService = liveScoreboardService;
        this.liveGameDetailService = liveGameDetailService;
        this.liveGameSyncService = liveGameSyncService;
        this.gameRepository = gameRepository;
        this.liveWinProbabilityService = liveWinProbabilityService;
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
        boolean live = scoreboard.map(ScoreboardView::isLive).orElse("In Progress".equals(game.getStatus()));
        boolean delayed = scoreboard.map(ScoreboardView::isDelayed)
                .orElse(game.getStatus() != null && game.getStatus().toLowerCase().contains("delay"));
        String status = scoreboard.map(ScoreboardView::getStatus).filter(s -> s != null && !s.isBlank()).orElse(game.getStatus());
        body.put("status", status);
        body.put("live", live);
        body.put("delayed", delayed);
        body.put("finalGame", scoreboard.map(ScoreboardView::isFinalGame).orElse("Final".equals(game.getStatus())));
        body.put("awayScore", awayScore);
        body.put("homeScore", homeScore);

        scoreboard.ifPresent(s -> body.put("scoreboard", s));

        Optional<LiveGameDetail> detail = liveGameDetailService.buildForGame(game);
        detail.ifPresent(d -> body.put("detail", d));

        Optional<LiveWinProbability> liveWinProbability = Optional.empty();
        if (live || delayed) {
            liveWinProbability = liveWinProbabilityService.forGame(game);
        }

        // Return the expanded NumSOX model, not raw MLB percentages. WinProbabilityService
        // remains the one place that blends pregame strength with live game state.
        NumsoxModel winProbability = matchupService.calculateForGame(
                game, liveWinProbability, scoreboard);
        body.put("winProbability", winProbability);

        // Always fresh: a cached response is the one thing this endpoint must never serve.
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(body);
    }
}
