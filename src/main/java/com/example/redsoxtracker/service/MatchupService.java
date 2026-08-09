package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.*;
import com.example.redsoxtracker.dto.StarterChoice;
import com.example.redsoxtracker.dto.WinProbabilityResult;
import com.example.redsoxtracker.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MatchupService {

    /** How long a finished game stays featured before the next matchup takes over. */
    private static final int POST_GAME_HOLD_HOURS = 1;

    private final GameRepository gameRepository;
    private final TeamStatsService teamStatsService;
    private final PlayerStatsService playerStatsService;
    private final BallparkFactorService ballparkFactorService;
    private final WinProbabilityService winProbabilityService;
    private final HistoricalCalibrationService historicalCalibrationService;

    public MatchupService(GameRepository gameRepository,
                          TeamStatsService teamStatsService,
                          PlayerStatsService playerStatsService,
                          BallparkFactorService ballparkFactorService,
                          WinProbabilityService winProbabilityService,
                          HistoricalCalibrationService historicalCalibrationService) {
        this.gameRepository = gameRepository;
        this.teamStatsService = teamStatsService;
        this.playerStatsService = playerStatsService;
        this.ballparkFactorService = ballparkFactorService;
        this.winProbabilityService = winProbabilityService;
        this.historicalCalibrationService = historicalCalibrationService;
    }

    public Optional<Game> getFeaturedGame() {
        List<Game> allGames = gameRepository.findAllByOrderByGameDateAsc();
        LocalDate today = LocalDate.now();

        // A live game always wins, regardless of calendar date: extra-inning games that
        // cross midnight still carry their original officialDate, so date-based matching
        // below would otherwise drop them the moment the clock rolls to the next day.
        Optional<Game> liveGame = allGames.stream()
                .filter(g -> "In Progress".equals(g.getStatus()))
                .reduce((first, second) -> second);
        if (liveGame.isPresent()) return liveGame;

        // Hold a just-finished game here for an hour before rolling over to the next one,
        // so the final score stays up instead of flipping straight to the next matchup.
        // The game-date bound is a safety net: only a game played today or last night can
        // hold the spot, so a bad timestamp can never pin an old game to the dashboard.
        LocalDateTime holdUntil = LocalDateTime.now().minusHours(POST_GAME_HOLD_HOURS);
        LocalDate earliestHoldable = today.minusDays(1);
        Optional<Game> justFinished = allGames.stream()
                .filter(g -> "Final".equals(g.getStatus()))
                .filter(g -> !g.getGameDate().isBefore(earliestHoldable))
                .filter(g -> g.getFinalizedAt() != null && g.getFinalizedAt().isAfter(holdUntil))
                .reduce((first, second) -> second);
        if (justFinished.isPresent()) return justFinished;

        // Past the hold window a finished game drops into the Previous Game box, so skip
        // finals here and let the next scheduled matchup take the spotlight.
        Optional<Game> todayGame = allGames.stream()
                .filter(g -> g.getGameDate().isEqual(today))
                .filter(g -> !"Final".equals(g.getStatus()))
                .findFirst();
        if (todayGame.isPresent()) return todayGame;

        Optional<Game> nextActiveGame = allGames.stream()
                .filter(g -> !"Final".equals(g.getStatus()) && !g.getGameDate().isBefore(today))
                .findFirst();
        if (nextActiveGame.isPresent()) return nextActiveGame;

        Optional<Game> nextScheduledGame = allGames.stream()
                .filter(g -> "Scheduled".equals(g.getStatus()))
                .findFirst();
        if (nextScheduledGame.isPresent()) return nextScheduledGame;

        return allGames.stream()
                .filter(g -> "Final".equals(g.getStatus()))
                .reduce((first, second) -> second); // most recent final
    }

    public WinProbabilityResult calculateForGame(Game game) {
        // Red Sox stats
        Optional<TeamStatSnapshot> bosStats = teamStatsService.getLatestStats("BOS");

        // Opponent stats
        String opponentName = game.getOpponent();
        Optional<Team> opponentTeam = teamStatsService.findByOpponentName(opponentName);
        Optional<TeamStatSnapshot> oppStats = opponentTeam.flatMap(teamStatsService::getLatestStats);

        // Determine away/home
        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        TeamStatSnapshot awayStats = redSoxAreAway ? bosStats.orElse(null) : oppStats.orElse(null);
        TeamStatSnapshot homeStats = redSoxAreAway ? oppStats.orElse(null) : bosStats.orElse(null);

        PitcherStatSnapshot awayStarter = getAwayStarterChoice(game).getSnapshot();
        PitcherStatSnapshot homeStarter = getHomeStarterChoice(game).getSnapshot();

        BallparkFactorSnapshot park = getParkForGame(game).orElse(null);

        // Fall back if stats are missing
        if (awayStats == null) awayStats = defaultStats();
        if (homeStats == null) homeStats = defaultStats();

        WinProbabilityResult result = winProbabilityService.calculate(awayStats, homeStats, awayStarter, homeStarter, park);
        applyHistoricalCalibration(game, result);
        return result;
    }

    private void applyHistoricalCalibration(Game game, WinProbabilityResult result) {
        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        int rawRedSoxPct = redSoxAreAway ? result.getAwayWinPct() : result.getHomeWinPct();
        var calibration = historicalCalibrationService.calibrate(game, rawRedSoxPct);
        int calibratedRedSoxPct = calibration.getCalibratedRedSoxPct();

        if (redSoxAreAway) {
            result.setAwayWinPct(calibratedRedSoxPct);
            result.setHomeWinPct(100 - calibratedRedSoxPct);
        } else {
            result.setHomeWinPct(calibratedRedSoxPct);
            result.setAwayWinPct(100 - calibratedRedSoxPct);
        }
        result.setHistoricalCalibration(calibration);
        result.setModelSummary(result.getModelSummary()
                + " Historical calibration blends the raw model with 2026 Red Sox results from March 26 through May 31.");
    }

    public Optional<PitcherStatSnapshot> getAwayStarterForGame(Game game) {
        return playerStatsService.getPitcherSnapshotByMlbPlayerId(game.getAwayProbablePitcherId());
    }

    public Optional<PitcherStatSnapshot> getHomeStarterForGame(Game game) {
        return playerStatsService.getPitcherSnapshotByMlbPlayerId(game.getHomeProbablePitcherId());
    }

    public StarterChoice getAwayStarterChoice(Game game) {
        return getStarterChoice(game, true);
    }

    public StarterChoice getHomeStarterChoice(Game game) {
        return getStarterChoice(game, false);
    }

    private StarterChoice getStarterChoice(Game game, boolean awaySide) {
        Integer confirmedId = awaySide ? game.getAwayProbablePitcherId() : game.getHomeProbablePitcherId();
        String confirmedName = awaySide ? game.getAwayProbablePitcherName() : game.getHomeProbablePitcherName();
        if (hasText(confirmedName)) {
            return StarterChoice.confirmed(confirmedName, playerStatsService.getPitcherSnapshotByMlbPlayerId(confirmedId).orElse(null));
        }

        String teamKey = teamKeyForSide(game, awaySide);
        List<PitcherRef> recent = recentKnownStarts(teamKey, game.getGameDate());
        if (recent.size() < 4) {
            return StarterChoice.unknown();
        }

        int gamesSinceMostRecentKnownStart = countTeamGamesBetween(teamKey, recent.get(0).date(), game.getGameDate());
        int predictionIndex = Math.floorMod(recent.size() - (gamesSinceMostRecentKnownStart % recent.size()), recent.size());
        PitcherRef predicted = recent.get(predictionIndex);
        long daysSinceStart = ChronoUnit.DAYS.between(predicted.date(), game.getGameDate());
        if (daysSinceStart < 4 || daysSinceStart > 21) {
            return StarterChoice.unknown();
        }

        String predictedName = predicted.name();
        PitcherStatSnapshot snapshot;
        String overrideNote = "";
        if ("BOS".equals(teamKey) && isTemporaryRedSoxStarter(predictedName)) {
            predictedName = "Brayan Bello";
            snapshot = playerStatsService.getRedSoxPitcherSnapshotByName(predictedName).orElse(null);
            overrideNote = " Temporary starter/opener pattern adjusted to Brayan Bello.";
        } else {
            snapshot = playerStatsService
                    .getPitcherSnapshotByMlbPlayerId(predicted.pitcherId())
                    .orElse(null);
        }
        String note = "Predicted from recent rotation order. Last known start: "
                + predicted.date() + " (" + daysSinceStart + " days ago)." + overrideNote;
        return StarterChoice.predicted(predictedName, snapshot, note);
    }

    private boolean isTemporaryRedSoxStarter(String name) {
        String normalized = normalizeTeamKey(name);
        return "TYLER SAMANIEGO".equals(normalized)
                || "GIOVANNI MORAN".equals(normalized)
                || "GIOVANI MORAN".equals(normalized);
    }

    private int countTeamGamesBetween(String teamKey, LocalDate afterDate, LocalDate throughDate) {
        int count = 0;
        for (Game g : gameRepository.findAllByOrderByGameDateAsc()) {
            if (!g.getGameDate().isAfter(afterDate) || g.getGameDate().isAfter(throughDate)) continue;
            boolean redSoxAreAway = "Away".equalsIgnoreCase(g.getHomeAway());
            String awayKey = redSoxAreAway ? "BOS" : normalizeTeamKey(g.getOpponent());
            String homeKey = redSoxAreAway ? normalizeTeamKey(g.getOpponent()) : "BOS";
            if (teamKey.equals(awayKey) || teamKey.equals(homeKey)) count++;
        }
        return Math.max(1, count);
    }

    private List<PitcherRef> recentKnownStarts(String teamKey, LocalDate beforeDate) {
        List<PitcherRef> starts = new ArrayList<>();
        for (Game prior : gameRepository.findAllByOrderByGameDateAsc()) {
            if (!prior.getGameDate().isBefore(beforeDate)) continue;
            PitcherRef ref = pitcherForTeamInGame(prior, teamKey);
            if (ref != null) starts.add(ref);
        }
        starts.sort(Comparator.comparing(PitcherRef::date).reversed());

        List<PitcherRef> distinct = new ArrayList<>();
        for (PitcherRef start : starts) {
            boolean alreadySeen = distinct.stream().anyMatch(existing -> samePitcher(existing, start));
            if (!alreadySeen) distinct.add(start);
            if (distinct.size() == 5) break;
        }
        return distinct;
    }

    private PitcherRef pitcherForTeamInGame(Game game, String teamKey) {
        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        String awayKey = redSoxAreAway ? "BOS" : normalizeTeamKey(game.getOpponent());
        String homeKey = redSoxAreAway ? normalizeTeamKey(game.getOpponent()) : "BOS";

        if (teamKey.equals(awayKey) && hasText(game.getAwayProbablePitcherName())) {
            return new PitcherRef(game.getAwayProbablePitcherId(), game.getAwayProbablePitcherName(), game.getGameDate());
        }
        if (teamKey.equals(homeKey) && hasText(game.getHomeProbablePitcherName())) {
            return new PitcherRef(game.getHomeProbablePitcherId(), game.getHomeProbablePitcherName(), game.getGameDate());
        }
        return null;
    }

    private String teamKeyForSide(Game game, boolean awaySide) {
        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        if (awaySide) {
            return redSoxAreAway ? "BOS" : normalizeTeamKey(game.getOpponent());
        }
        return redSoxAreAway ? normalizeTeamKey(game.getOpponent()) : "BOS";
    }

    private String normalizeTeamKey(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean samePitcher(PitcherRef a, PitcherRef b) {
        if (a.pitcherId() != null && b.pitcherId() != null) return a.pitcherId().equals(b.pitcherId());
        return normalizeTeamKey(a.name()).equals(normalizeTeamKey(b.name()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public Optional<BallparkFactorSnapshot> getParkForGame(Game game) {
        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        Optional<Team> bosTeam = teamStatsService.findByCode("BOS");
        Optional<Team> opponentTeam = teamStatsService.findByOpponentName(game.getOpponent());
        Team homeTeam = redSoxAreAway ? opponentTeam.orElse(null) : bosTeam.orElse(null);
        if (homeTeam != null) {
            Optional<BallparkFactorSnapshot> teamPark = ballparkFactorService.getParkForTeam(homeTeam);
            if (teamPark.isPresent()) return teamPark;
        }
        if (game.getVenue() != null && !game.getVenue().isBlank()) {
            return ballparkFactorService.getParkByVenueName(game.getVenue());
        }
        return Optional.empty();
    }

    public String buildMatchupTitle(Game game) {
        return "Away".equalsIgnoreCase(game.getHomeAway())
                ? "Red Sox @ " + game.getOpponent()
                : game.getOpponent() + " @ Red Sox";
    }

    public String buildScoreline(Game game) {
        if (game.getRedSoxScore() == null || game.getOpponentScore() == null) {
            return "No score yet";
        }
        if ("Away".equalsIgnoreCase(game.getHomeAway())) {
            return "Red Sox " + game.getRedSoxScore() + ", " + game.getOpponent() + " " + game.getOpponentScore();
        }
        return game.getOpponent() + " " + game.getOpponentScore() + ", Red Sox " + game.getRedSoxScore();
    }

    private TeamStatSnapshot defaultStats() {
        TeamStatSnapshot s = new TeamStatSnapshot();
        s.setTeamWrcPlus(100.0);
        s.setTeamOps(0.720);
        s.setRunsPerGame(4.5);
        s.setTeamObp(0.315);
        s.setTeamSlg(0.405);
        s.setTeamKRate(23.0);
        s.setTeamBbRate(8.5);
        s.setBullpenEra(4.00);
        s.setBullpenFip(4.00);
        s.setBullpenWhip(1.30);
        s.setBullpenKRate(24.0);
        s.setBullpenBbRate(9.0);
        s.setOutsAboveAverage(0);
        s.setDefensiveEfficiency(0.695);
        s.setLast10Wins(5);
        s.setLast10Losses(5);
        s.setLast5RunDifferential(0);
        s.setLast5RunsScored(22);
        s.setLast5RunsAllowed(22);
        s.setWins(40);
        s.setLosses(40);
        return s;
    }

    private record PitcherRef(Integer pitcherId, String name, LocalDate date) {}
}
