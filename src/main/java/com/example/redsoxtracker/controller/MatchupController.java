package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.Team;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.dto.NumsoxModel;
import com.example.redsoxtracker.dto.StarterChoice;
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
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class MatchupController {

    private final MatchupService matchupService;
    private final GameRepository gameRepository;
    private final TeamStatsService teamStatsService;

    public MatchupController(MatchupService matchupService, GameRepository gameRepository,
                             TeamStatsService teamStatsService) {
        this.matchupService = matchupService;
        this.gameRepository = gameRepository;
        this.teamStatsService = teamStatsService;
    }

    @GetMapping("/matchup")
    public String matchup(@RequestParam(name = "gameId", required = false) Long gameId,
                          @RequestParam(name = "month", required = false) String month,
                          @RequestParam(name = "focusWeek", required = false) String focusWeek,
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

        // WinProbabilityService is reached through MatchupService so every page uses the
        // same expanded NumSOX model and the same live-game blend.
        NumsoxModel numsox = matchupService.calculateForGame(game);
        model.addAttribute("numsox", numsox);
        numsox.categories().stream()
                .filter(c -> "park".equals(c.key()))
                .findFirst()
                .ifPresent(c -> model.addAttribute("parkCategory", c));
        model.addAttribute("numsoxTitle", NumsoxModel.TITLE);
        model.addAttribute("numsoxDisclaimer", NumsoxModel.DISCLAIMER);

        // All games for switcher
        List<Game> allGames = gameRepository.findAllByOrderByGameDateAsc();
        YearMonth calendarMonth = resolveCalendarMonth(month);
        model.addAttribute("calendarMonth", calendarMonth.toString());
        model.addAttribute("calendarTitle", calendarMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + calendarMonth.getYear());
        model.addAttribute("previousCalendarMonth", calendarMonth.minusMonths(1).toString());
        model.addAttribute("nextCalendarMonth", calendarMonth.plusMonths(1).toString());
        model.addAttribute("currentCalendarMonth", YearMonth.now().toString());

        // The calendar opens on one week and expands to the month, so the whole month is
        // rendered and the focus week says which row to show first.
        List<CalendarWeek> calendarWeeks = buildCalendarWeeks(allGames, calendarMonth, game);
        model.addAttribute("calendarWeeks", calendarWeeks);
        model.addAttribute("focusWeekIndex", resolveFocusWeek(calendarWeeks, focusWeek));

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

    private List<CalendarWeek> buildCalendarWeeks(List<Game> allGames, YearMonth month, Game selectedGame) {
        LocalDate firstOfMonth = month.atDay(1);
        LocalDate calendarStart = firstOfMonth.minusDays(firstOfMonth.getDayOfWeek().getValue() % 7);
        LocalDate lastOfMonth = month.atEndOfMonth();
        LocalDate calendarEnd = lastOfMonth.plusDays(6 - (lastOfMonth.getDayOfWeek().getValue() % 7));
        LocalDate today = LocalDate.now();

        List<CalendarWeek> weeks = new ArrayList<>();
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
                weeks.add(toWeek(currentWeek));
                currentWeek = new ArrayList<>();
            }
        }
        if (!currentWeek.isEmpty()) {
            weeks.add(toWeek(currentWeek));
        }
        return weeks;
    }

    private CalendarWeek toWeek(List<CalendarDay> days) {
        List<CalendarDay> frozen = List.copyOf(days);
        LocalDate start = frozen.get(0).date();
        LocalDate end = frozen.get(frozen.size() - 1).date();
        boolean hasToday = frozen.stream().anyMatch(CalendarDay::today);
        boolean hasSelected = frozen.stream().anyMatch(CalendarDay::selected);
        boolean hasGames = frozen.stream().anyMatch(d -> !d.games().isEmpty());
        return new CalendarWeek(frozen, weekLabel(start, end), hasToday, hasSelected, hasGames);
    }

    /** A week reads as "Aug 9 - Aug 15", and drops the repeated month when it does not change. */
    private String weekLabel(LocalDate start, LocalDate end) {
        DateTimeFormatter monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US);
        if (start.getMonth() == end.getMonth()) {
            return monthDay.format(start) + " - " + end.getDayOfMonth();
        }
        return monthDay.format(start) + " - " + monthDay.format(end);
    }

    /**
     * Which week the collapsed view opens on. The selected game's week is the reason the page
     * was opened, so it wins; failing that today, then whichever edge the reader arrived from
     * when they stepped past the end of the previous month.
     */
    private int resolveFocusWeek(List<CalendarWeek> weeks, String focusWeek) {
        if (weeks.isEmpty()) return 0;
        for (int i = 0; i < weeks.size(); i++) {
            if (weeks.get(i).hasSelected()) return i;
        }
        if ("last".equalsIgnoreCase(focusWeek)) return lastWeekWithGames(weeks);
        if ("first".equalsIgnoreCase(focusWeek)) return firstWeekWithGames(weeks);
        for (int i = 0; i < weeks.size(); i++) {
            if (weeks.get(i).hasToday()) return i;
        }
        return firstWeekWithGames(weeks);
    }

    /** Landing on an empty week would look broken, so prefer one that has baseball in it. */
    private int firstWeekWithGames(List<CalendarWeek> weeks) {
        for (int i = 0; i < weeks.size(); i++) {
            if (weeks.get(i).hasGames()) return i;
        }
        return 0;
    }

    private int lastWeekWithGames(List<CalendarWeek> weeks) {
        for (int i = weeks.size() - 1; i >= 0; i--) {
            if (weeks.get(i).hasGames()) return i;
        }
        return weeks.size() - 1;
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

    /**
     * One row of the calendar. The calendar opens on a single week, so a week needs to know
     * enough about itself to be labelled and to be chosen as the one worth opening on.
     */
    public record CalendarWeek(List<CalendarDay> days, String label, boolean hasToday,
                               boolean hasSelected, boolean hasGames) {
    }
}
