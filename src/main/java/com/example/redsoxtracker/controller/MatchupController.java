package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.dto.StarterChoice;
import com.example.redsoxtracker.dto.WinProbabilityResult;
import com.example.redsoxtracker.repository.GameRepository;
import com.example.redsoxtracker.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.util.Optional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class MatchupController {

    private final MatchupService matchupService;
    private final GameRepository gameRepository;
    private final TeamStatsService teamStatsService;
    private final PlayerStatsService playerStatsService;
    private final BallparkFactorService ballparkFactorService;

    public MatchupController(MatchupService matchupService, GameRepository gameRepository,
                             TeamStatsService teamStatsService, PlayerStatsService playerStatsService,
                             BallparkFactorService ballparkFactorService) {
        this.matchupService = matchupService;
        this.gameRepository = gameRepository;
        this.teamStatsService = teamStatsService;
        this.playerStatsService = playerStatsService;
        this.ballparkFactorService = ballparkFactorService;
    }

    @GetMapping("/matchup")
    public String matchup(@RequestParam(name = "gameId", required = false) Long gameId,
                          @RequestParam(name = "month", required = false) String month,
                          Model model) {
        Game game;
        if (gameId != null) {
            game = gameRepository.findById(gameId).orElse(null);
        } else {
            game = matchupService.getFeaturedGame().orElse(null);
        }

        if (game == null) {
            model.addAttribute("noGame", true);
            return "matchup";
        }

        model.addAttribute("game", game);
        model.addAttribute("matchupTitle", matchupService.buildMatchupTitle(game));
        model.addAttribute("scoreline", matchupService.buildScoreline(game));

        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        model.addAttribute("redSoxAreAway", redSoxAreAway);

        // Away team info
        String awayTeamName = redSoxAreAway ? "Red Sox" : game.getOpponent();
        String homeTeamName = redSoxAreAway ? game.getOpponent() : "Red Sox";
        model.addAttribute("awayTeamName", awayTeamName);
        model.addAttribute("homeTeamName", homeTeamName);

        // Stats
        Optional<TeamStatSnapshot> bosStats = teamStatsService.getLatestStats("BOS");
        Optional<Team> oppTeam = teamStatsService.findByOpponentName(game.getOpponent());
        Optional<TeamStatSnapshot> oppStats = oppTeam.flatMap(teamStatsService::getLatestStats);

        TeamStatSnapshot awayStats = redSoxAreAway ? bosStats.orElse(null) : oppStats.orElse(null);
        TeamStatSnapshot homeStats = redSoxAreAway ? oppStats.orElse(null) : bosStats.orElse(null);
        model.addAttribute("awayStats", awayStats);
        model.addAttribute("homeStats", homeStats);

        StarterChoice awayStarterChoice = matchupService.getAwayStarterChoice(game);
        StarterChoice homeStarterChoice = matchupService.getHomeStarterChoice(game);
        PitcherStatSnapshot awayStarter = awayStarterChoice.getSnapshot();
        PitcherStatSnapshot homeStarter = homeStarterChoice.getSnapshot();
        model.addAttribute("awayStarter", awayStarter);
        model.addAttribute("homeStarter", homeStarter);
        model.addAttribute("awayStarterName", awayStarterChoice.getName());
        model.addAttribute("homeStarterName", homeStarterChoice.getName());
        model.addAttribute("awayStarterConfirmed", awayStarterChoice.isConfirmed());
        model.addAttribute("homeStarterConfirmed", homeStarterChoice.isConfirmed());
        model.addAttribute("awayStarterPredicted", awayStarterChoice.isPredicted());
        model.addAttribute("homeStarterPredicted", homeStarterChoice.isPredicted());
        model.addAttribute("awayStarterNote", awayStarterChoice.getNote());
        model.addAttribute("homeStarterNote", homeStarterChoice.getNote());
        model.addAttribute("awayRecord", redSoxAreAway ? firstNonBlank(game.getRedSoxRecord(), recordFromStats(awayStats)) : firstNonBlank(game.getOpponentRecord(), recordFromStats(awayStats)));
        model.addAttribute("homeRecord", redSoxAreAway ? firstNonBlank(game.getOpponentRecord(), recordFromStats(homeStats)) : firstNonBlank(game.getRedSoxRecord(), recordFromStats(homeStats)));
        model.addAttribute("awayLast10", last10FromStats(awayStats));
        model.addAttribute("homeLast10", last10FromStats(homeStats));

        // Park factors
        BallparkFactorSnapshot park = matchupService.getParkForGame(game).orElse(null);
        model.addAttribute("park", park);

        // Win probability
        WinProbabilityResult winProb = matchupService.calculateForGame(game);
        model.addAttribute("winProb", winProb);

        // All games for switcher
        List<Game> allGames = gameRepository.findAllByOrderByGameDateAsc();
        YearMonth calendarMonth = resolveCalendarMonth(month);
        model.addAttribute("calendarMonth", calendarMonth.toString());
        model.addAttribute("calendarTitle", calendarMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + calendarMonth.getYear());
        model.addAttribute("previousCalendarMonth", calendarMonth.minusMonths(1).toString());
        model.addAttribute("nextCalendarMonth", calendarMonth.plusMonths(1).toString());
        model.addAttribute("currentCalendarMonth", YearMonth.now().toString());
        model.addAttribute("calendarWeeks", buildCalendarWeeks(allGames, calendarMonth, game));

        return "matchup";
    }

    private YearMonth resolveCalendarMonth(String month) {
        if (month != null && !month.isBlank()) {
            try {
                return YearMonth.parse(month);
            } catch (Exception ignored) {
                return YearMonth.now();
            }
        }
        return YearMonth.now();
    }

    private List<List<CalendarDay>> buildCalendarWeeks(List<Game> allGames, YearMonth month, Game selectedGame) {
        LocalDate firstOfMonth = month.atDay(1);
        LocalDate calendarStart = firstOfMonth.minusDays(firstOfMonth.getDayOfWeek().getValue() % 7);
        LocalDate lastOfMonth = month.atEndOfMonth();
        LocalDate calendarEnd = lastOfMonth.plusDays(6 - (lastOfMonth.getDayOfWeek().getValue() % 7));
        LocalDate today = LocalDate.now();

        List<List<CalendarDay>> weeks = new ArrayList<>();
        List<CalendarDay> currentWeek = new ArrayList<>();
        for (LocalDate date = calendarStart; !date.isAfter(calendarEnd); date = date.plusDays(1)) {
            LocalDate calendarDate = date;
            List<Game> games = allGames.stream()
                    .filter(g -> calendarDate.equals(g.getGameDate()))
                    .toList();
            boolean selected = selectedGame != null && selectedGame.getGameDate() != null
                    && selectedGame.getGameDate().equals(calendarDate);
            currentWeek.add(new CalendarDay(
                    calendarDate,
                    calendarDate.getDayOfMonth(),
                    YearMonth.from(calendarDate).equals(month),
                    calendarDate.equals(today),
                    selected,
                    games));
            if (calendarDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                weeks.add(currentWeek);
                currentWeek = new ArrayList<>();
            }
        }
        if (!currentWeek.isEmpty()) {
            weeks.add(currentWeek);
        }
        return weeks;
    }

    private String recordFromStats(TeamStatSnapshot stats) {
        return stats == null ? null : stats.getRecord();
    }

    private String last10FromStats(TeamStatSnapshot stats) {
        return stats == null || stats.getLast10Record() == null ? "Not available" : stats.getLast10Record();
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) return preferred;
        if (fallback != null && !fallback.isBlank()) return fallback;
        return "Record unavailable";
    }

    public record CalendarDay(LocalDate date, int dayNumber, boolean currentMonth, boolean today,
                              boolean selected, List<Game> games) {
    }
}
