package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.dto.WinProbabilityResult;
import com.example.redsoxtracker.repository.GameRepository;
import com.example.redsoxtracker.service.GameService;
import com.example.redsoxtracker.service.LiveScoreboardService;
import com.example.redsoxtracker.service.MatchupService;
import com.example.redsoxtracker.service.StandingsService;
import com.example.redsoxtracker.service.TeamStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class DashboardController {

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final LiveScoreboardService liveScoreboardService;
    private final MatchupService matchupService;
    private final TeamStatsService teamStatsService;
    private final StandingsService standingsService;

    public DashboardController(GameRepository gameRepository, GameService gameService,
                               LiveScoreboardService liveScoreboardService,
                               MatchupService matchupService, TeamStatsService teamStatsService,
                               StandingsService standingsService) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.liveScoreboardService = liveScoreboardService;
        this.matchupService = matchupService;
        this.teamStatsService = teamStatsService;
        this.standingsService = standingsService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Game> games = gameRepository.findAllByOrderByGameDateAsc();
        model.addAttribute("games", games);
        model.addAttribute("wins", gameService.countWins());
        model.addAttribute("losses", gameService.countLosses());

        // Featured matchup preview
        Optional<Game> featuredGame = matchupService.getFeaturedGame();
        if (featuredGame.isPresent()) {
            Game game = featuredGame.get();
            model.addAttribute("featuredGame", game);
            model.addAttribute("featuredTitle", matchupService.buildMatchupTitle(game));
            model.addAttribute("featuredScoreline", matchupService.buildScoreline(game));
            boolean isLive = "In Progress".equals(game.getStatus());
            model.addAttribute("featuredIsToday", isLive || game.getGameDate().isEqual(LocalDate.now()));
            model.addAttribute("featuredIsLive", isLive);
            boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
            model.addAttribute("featuredAwayTeam", redSoxAreAway ? "Boston Red Sox" : game.getOpponent());
            model.addAttribute("featuredHomeTeam", redSoxAreAway ? game.getOpponent() : "Boston Red Sox");
            model.addAttribute("featuredAwayRecord", redSoxAreAway ? game.getRedSoxRecord() : game.getOpponentRecord());
            model.addAttribute("featuredHomeRecord", redSoxAreAway ? game.getOpponentRecord() : game.getRedSoxRecord());
            model.addAttribute("featuredAwayScore", redSoxAreAway ? game.getRedSoxScore() : game.getOpponentScore());
            model.addAttribute("featuredHomeScore", redSoxAreAway ? game.getOpponentScore() : game.getRedSoxScore());
            WinProbabilityResult winProb = matchupService.calculateForGame(game);
            model.addAttribute("featuredWinProb", winProb);
            Optional<BallparkFactorSnapshot> park = matchupService.getParkForGame(game);
            park.ifPresent(p -> model.addAttribute("featuredPark", p));
            liveScoreboardService.buildForGame(game).ifPresent(s -> model.addAttribute("scoreboard", s));
        }

        // Previous completed game box score (skip if it's the same game already shown above)
        Optional<Game> previousGame = games.stream()
                .filter(g -> "Final".equals(g.getStatus()))
                .filter(g -> featuredGame.isEmpty() || !g.getId().equals(featuredGame.get().getId()))
                .reduce((first, second) -> second);
        if (previousGame.isPresent()) {
            Game prev = previousGame.get();
            model.addAttribute("previousGame", prev);
            boolean prevRedSoxAreAway = "Away".equalsIgnoreCase(prev.getHomeAway());
            model.addAttribute("previousAwayTeam", prevRedSoxAreAway ? "Boston Red Sox" : prev.getOpponent());
            model.addAttribute("previousHomeTeam", prevRedSoxAreAway ? prev.getOpponent() : "Boston Red Sox");
            liveScoreboardService.buildForGame(prev).ifPresent(s -> model.addAttribute("previousScoreboard", s));
        }

        // Red Sox team stats for sidebar
        teamStatsService.getLatestStats("BOS").ifPresent(s -> model.addAttribute("bosStats", s));

        model.addAttribute("standings", standingsService.buildStandings());

        return "dashboard";
    }
}
