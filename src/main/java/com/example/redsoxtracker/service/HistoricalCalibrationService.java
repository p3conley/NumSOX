package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.HistoricalCalibration;
import com.example.redsoxtracker.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class HistoricalCalibrationService {

    private static final LocalDate OPENING_DAY_2026 = LocalDate.of(2026, 3, 26);
    private static final LocalDate BACKTEST_END_2026 = LocalDate.of(2026, 5, 31);

    private final GameRepository gameRepository;
    private final TeamStatsService teamStatsService;

    public HistoricalCalibrationService(GameRepository gameRepository, TeamStatsService teamStatsService) {
        this.gameRepository = gameRepository;
        this.teamStatsService = teamStatsService;
    }

    public HistoricalCalibration calibrate(Game targetGame, int rawRedSoxPct) {
        List<Game> completed = completedGamesThrough(BACKTEST_END_2026);
        List<Game> prior = completed.stream()
                .filter(game -> game.getGameDate().isBefore(targetGame.getGameDate()))
                .toList();

        Backtest backtest = backtest(completed);
        int empiricalPct = empiricalRedSoxPct(targetGame, prior);

        double sampleWeight = Math.min(0.55, prior.size() / 120.0);
        if (backtest.accuracy() < 0.50) sampleWeight = Math.min(0.62, sampleWeight + 0.10);
        if (backtest.brierScore() > 0.255) sampleWeight = Math.min(0.66, sampleWeight + 0.06);
        int calibrated = clamp((int) Math.round(rawRedSoxPct * (1.0 - sampleWeight) + empiricalPct * sampleWeight), 28, 72);
        int adjustment = calibrated - rawRedSoxPct;

        List<String> notes = buildNotes(targetGame, prior, rawRedSoxPct, empiricalPct, sampleWeight, backtest);
        return new HistoricalCalibration(
                backtest.sampleSize,
                backtest.correct,
                backtest.accuracy(),
                backtest.brierScore(),
                rawRedSoxPct,
                calibrated,
                adjustment,
                notes);
    }

    private List<Game> completedGamesThrough(LocalDate through) {
        return gameRepository.findAllByOrderByGameDateAsc().stream()
                .filter(game -> game.getGameDate() != null)
                .filter(game -> !game.getGameDate().isBefore(OPENING_DAY_2026))
                .filter(game -> !game.getGameDate().isAfter(through))
                .filter(game -> "Final".equalsIgnoreCase(game.getStatus()))
                .filter(game -> game.getRedSoxScore() != null && game.getOpponentScore() != null)
                .sorted(Comparator.comparing(Game::getGameDate))
                .toList();
    }

    private Backtest backtest(List<Game> completed) {
        Backtest result = new Backtest();
        for (int i = 6; i < completed.size(); i++) {
            Game game = completed.get(i);
            List<Game> prior = completed.subList(0, i);
            int predictedRedSoxPct = empiricalRedSoxPct(game, prior);
            boolean predictedRedSox = predictedRedSoxPct >= 50;
            boolean redSoxWon = "W".equalsIgnoreCase(game.getResult());
            if (predictedRedSox == redSoxWon) result.correct++;
            double probability = predictedRedSoxPct / 100.0;
            double actual = redSoxWon ? 1.0 : 0.0;
            result.brierSum += Math.pow(probability - actual, 2);
            result.sampleSize++;
        }
        return result;
    }

    private int empiricalRedSoxPct(Game targetGame, List<Game> prior) {
        if (prior.size() < 6) return 50;

        Record all = record(prior);
        Record sameVenue = record(prior.stream()
                .filter(game -> targetGame.getHomeAway().equalsIgnoreCase(game.getHomeAway()))
                .toList());
        Record last10 = record(prior.stream()
                .skip(Math.max(0, prior.size() - 10))
                .toList());
        Record last5 = record(prior.stream()
                .skip(Math.max(0, prior.size() - 5))
                .toList());
        Record headToHead = record(prior.stream()
                .filter(game -> game.getOpponent().equalsIgnoreCase(targetGame.getOpponent()))
                .toList());

        double pyth = pythagorean(all.runsFor, all.runsAgainst);
        double actual = all.winPctOr(0.5);
        double venue = sameVenue.winPctOr(actual);
        double recent = last10.winPctOr(actual);
        double h2h = headToHead.games >= 3 ? headToHead.winPctOr(0.5) : 0.5;
        double last5RunDiffPerGame = last5.games > 0 ? (double) (last5.runsFor - last5.runsAgainst) / last5.games : 0.0;
        double last5RunsAllowedPerGame = last5.games > 0 ? (double) last5.runsAgainst / last5.games : 4.5;
        double opponentStrength = opponentStrengthSignal(targetGame);
        double oneRunVolatility = oneRunShare(last10);
        StreakInfo redSoxStreak = currentStreak(prior);
        StreakInfo opponentStreak = opponentCurrentStreak(targetGame);
        StreakProfile profile = streakProfile(prior);
        double streakImpact = streakImpact(redSoxStreak, opponentStreak, profile);

        double score = 50.0
                + (pyth - 0.5) * 34.0
                + (actual - 0.5) * 12.0
                + (venue - 0.5) * 16.0
                + (recent - 0.5) * 14.0
                + (h2h - 0.5) * 6.0
                + last5RunDiffPerGame * 1.15
                + (4.5 - last5RunsAllowedPerGame) * 1.35
                + opponentStrength * 16.0
                + streakImpact;

        if ("Home".equalsIgnoreCase(targetGame.getHomeAway())) {
            score += venue >= 0.5 ? 1.2 : -2.2;
        } else {
            score += venue >= actual ? 0.8 : -1.2;
        }

        if (oneRunVolatility >= 0.40) {
            score = 50.0 + (score - 50.0) * 0.82;
        }

        return clamp((int) Math.round(score), 35, 65);
    }

    private List<String> buildNotes(Game targetGame, List<Game> prior, int rawPct, int empiricalPct,
                                    double sampleWeight, Backtest backtest) {
        List<String> notes = new ArrayList<>();
        Record all = record(prior);
        Record venue = record(prior.stream()
                .filter(game -> targetGame.getHomeAway().equalsIgnoreCase(game.getHomeAway()))
                .toList());
        Record last10 = record(prior.stream()
                .skip(Math.max(0, prior.size() - 10))
                .toList());
        Record last5 = record(prior.stream()
                .skip(Math.max(0, prior.size() - 5))
                .toList());
        Record h2h = record(prior.stream()
                .filter(game -> game.getOpponent().equalsIgnoreCase(targetGame.getOpponent()))
                .toList());
        StreakInfo redSoxStreak = currentStreak(prior);
        StreakInfo opponentStreak = opponentCurrentStreak(targetGame);
        StreakProfile profile = streakProfile(prior);
        double streakImpact = streakImpact(redSoxStreak, opponentStreak, profile);

        notes.add("Backtested completed Red Sox games from March 26 through May 31, 2026. Sample is small, so the correction is intentionally shrunk toward the original model.");
        notes.add("Short-sample backtest: " + backtest.correct + "-" + (backtest.sampleSize - backtest.correct)
                + " directional accuracy (" + fmtPct(backtest.accuracy()) + "), Brier " + String.format("%.3f", backtest.brierScore()) + ".");
        notes.add("Red Sox prior sample before this game: " + all.wins + "-" + all.losses
                + ", run differential " + signed(all.runsFor - all.runsAgainst)
                + ", Pythagorean win rate " + fmtPct(pythagorean(all.runsFor, all.runsAgainst)) + ".");
        notes.add(("Home".equalsIgnoreCase(targetGame.getHomeAway()) ? "Home" : "Road")
                + " split in sample: " + venue.wins + "-" + venue.losses + ".");
        notes.add("Recent form entering this game: " + last10.wins + "-" + last10.losses + " over previous "
                + last10.games + " completed games.");
        notes.add("Streak context: Boston was on a " + redSoxStreak.label()
                + ". In this sample, Boston win streaks lasted about " + String.format("%.1f", profile.avgWinLength)
                + " games and losing streaks lasted about " + String.format("%.1f", profile.avgLossLength)
                + " games on average.");
        if (opponentStreak.length() > 0) {
            notes.add(targetGame.getOpponent() + " current standings streak: " + opponentStreak.label()
                    + ". Streak adjustment to Boston: " + signed((int) Math.round(streakImpact)) + " probability points.");
        } else {
            notes.add("Opponent streak is unavailable, so only Boston's computed streak affected the streak adjustment.");
        }
        notes.add("Last-5 run shape: " + signed(last5.runsFor - last5.runsAgainst)
                + " run differential, " + String.format("%.1f", last5.games > 0 ? (double) last5.runsAgainst / last5.games : 0.0)
                + " runs allowed per game.");
        double opponentStrength = opponentStrengthSignal(targetGame);
        if (Math.abs(opponentStrength) >= 0.03) {
            notes.add("Opponent record signal: "
                    + (opponentStrength > 0 ? "Boston had the better pregame record profile." : targetGame.getOpponent() + " had the better pregame record profile.")
                    + " This corrected several earlier misses where opponent quality was underweighted.");
        }
        if (oneRunShare(last10) >= 0.40) {
            notes.add("Recent sample had heavy one-run volatility, so the calibration dampens confidence instead of chasing noise.");
        }
        if (h2h.games >= 3) {
            notes.add("Opponent-specific signal vs " + targetGame.getOpponent() + ": "
                    + h2h.wins + "-" + h2h.losses + " head-to-head.");
        }
        notes.add("Raw model Red Sox probability " + rawPct + "%; short-sample empirical read "
                + empiricalPct + "%; blend weight " + Math.round(sampleWeight * 100) + "%.");
        return notes;
    }

    private Record record(List<Game> games) {
        Record record = new Record();
        record.games = games.size();
        for (Game game : games) {
            if ("W".equalsIgnoreCase(game.getResult())) record.wins++;
            if ("L".equalsIgnoreCase(game.getResult())) record.losses++;
            record.runsFor += game.getRedSoxScore() != null ? game.getRedSoxScore() : 0;
            record.runsAgainst += game.getOpponentScore() != null ? game.getOpponentScore() : 0;
            if (game.getRedSoxScore() != null && game.getOpponentScore() != null
                    && Math.abs(game.getRedSoxScore() - game.getOpponentScore()) == 1) {
                record.oneRunGames++;
            }
        }
        return record;
    }

    private double opponentStrengthSignal(Game game) {
        Double redSoxPct = parseWinPct(game.getRedSoxRecord());
        Double opponentPct = parseWinPct(game.getOpponentRecord());
        if (redSoxPct == null || opponentPct == null) return 0.0;
        return clampDouble(redSoxPct - opponentPct, -0.25, 0.25);
    }

    private StreakInfo currentStreak(List<Game> prior) {
        if (prior.isEmpty()) return StreakInfo.none();
        String result = null;
        int length = 0;
        for (int i = prior.size() - 1; i >= 0; i--) {
            String gameResult = prior.get(i).getResult();
            if (!"W".equalsIgnoreCase(gameResult) && !"L".equalsIgnoreCase(gameResult)) continue;
            if (result == null) {
                result = gameResult.toUpperCase();
                length = 1;
            } else if (result.equalsIgnoreCase(gameResult)) {
                length++;
            } else {
                break;
            }
        }
        return result == null ? StreakInfo.none() : new StreakInfo(result, length);
    }

    private StreakInfo opponentCurrentStreak(Game targetGame) {
        return teamStatsService.findByOpponentName(targetGame.getOpponent())
                .flatMap(teamStatsService::getLatestStats)
                .map(TeamStatSnapshot::getCurrentStreak)
                .map(this::parseStreak)
                .orElse(StreakInfo.none());
    }

    private StreakInfo parseStreak(String text) {
        if (text == null || text.isBlank()) return StreakInfo.none();
        String cleaned = text.trim().toUpperCase();
        if (cleaned.length() < 2) return StreakInfo.none();
        String type = cleaned.substring(0, 1);
        if (!"W".equals(type) && !"L".equals(type)) return StreakInfo.none();
        try {
            return new StreakInfo(type, Integer.parseInt(cleaned.substring(1)));
        } catch (NumberFormatException e) {
            return StreakInfo.none();
        }
    }

    private StreakProfile streakProfile(List<Game> prior) {
        StreakProfile profile = new StreakProfile();
        String active = null;
        int length = 0;
        for (Game game : prior) {
            String result = game.getResult();
            if (!"W".equalsIgnoreCase(result) && !"L".equalsIgnoreCase(result)) continue;
            result = result.toUpperCase();
            if (active == null) {
                active = result;
                length = 1;
            } else if (active.equals(result)) {
                length++;
            } else {
                profile.add(active, length);
                active = result;
                length = 1;
            }
        }
        if (active != null) profile.add(active, length);
        profile.finish();
        return profile;
    }

    private double streakImpact(StreakInfo redSox, StreakInfo opponent, StreakProfile profile) {
        double impact = 0.0;

        if (redSox.length() > 0) {
            double avg = redSox.isWin() ? profile.avgWinLength : profile.avgLossLength;
            double persistence = redSox.length() <= avg + 1.0 ? 1.0 : 0.55;
            double base = Math.min(4.0, 1.15 * redSox.length()) * persistence;
            impact += redSox.isWin() ? base : -base;
        }

        if (opponent.length() > 0) {
            double base = Math.min(3.5, 0.95 * opponent.length());
            impact += opponent.isWin() ? -base : base;
        }

        return clampDouble(impact, -6.0, 6.0);
    }

    private Double parseWinPct(String recordText) {
        if (recordText == null || recordText.isBlank()) return null;
        String cleaned = recordText.replace(" ", "");
        String[] parts = cleaned.split("-");
        if (parts.length != 2) return null;
        try {
            int wins = Integer.parseInt(parts[0]);
            int losses = Integer.parseInt(parts[1]);
            int total = wins + losses;
            return total > 0 ? (double) wins / total : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double oneRunShare(Record record) {
        return record.games > 0 ? (double) record.oneRunGames / record.games : 0.0;
    }

    private double pythagorean(int runsFor, int runsAgainst) {
        if (runsFor <= 0 || runsAgainst <= 0) return 0.5;
        double rf = Math.pow(runsFor, 1.83);
        double ra = Math.pow(runsAgainst, 1.83);
        return rf / (rf + ra);
    }

    private String fmtPct(double value) {
        return String.format("%.1f%%", value * 100.0);
    }

    private String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Record {
        int games;
        int wins;
        int losses;
        int runsFor;
        int runsAgainst;
        int oneRunGames;

        double winPctOr(double fallback) {
            int decisions = wins + losses;
            return decisions > 0 ? (double) wins / decisions : fallback;
        }
    }

    private record StreakInfo(String type, int length) {
        static StreakInfo none() {
            return new StreakInfo("", 0);
        }

        boolean isWin() {
            return "W".equals(type);
        }

        String label() {
            if (length <= 0) return "no active streak";
            return (isWin() ? "W" : "L") + length;
        }
    }

    private static class StreakProfile {
        int winStreaks;
        int lossStreaks;
        int winLengthTotal;
        int lossLengthTotal;
        double avgWinLength = 2.0;
        double avgLossLength = 2.0;

        void add(String type, int length) {
            if ("W".equals(type)) {
                winStreaks++;
                winLengthTotal += length;
            } else if ("L".equals(type)) {
                lossStreaks++;
                lossLengthTotal += length;
            }
        }

        void finish() {
            if (winStreaks > 0) avgWinLength = (double) winLengthTotal / winStreaks;
            if (lossStreaks > 0) avgLossLength = (double) lossLengthTotal / lossStreaks;
        }
    }

    private static class Backtest {
        int sampleSize;
        int correct;
        double brierSum;

        double accuracy() {
            return sampleSize == 0 ? 0.0 : (double) correct / sampleSize;
        }

        double brierScore() {
            return sampleSize == 0 ? 0.25 : brierSum / sampleSize;
        }
    }
}
