package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.CategoryComparison;
import com.example.redsoxtracker.dto.WinProbabilityResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WinProbabilityService {

    // Category weights (must sum to 1.0)
    private static final double OFFENSE_WEIGHT = 0.20;
    private static final double STARTER_WEIGHT = 0.25;
    private static final double BULLPEN_WEIGHT = 0.15;
    private static final double DEFENSE_WEIGHT = 0.10;
    private static final double RECENT_FORM_WEIGHT = 0.10;
    private static final double BALLPARK_WEIGHT = 0.14;
    private static final double HOME_FIELD_WEIGHT = 0.06;

    // Home field score (0-1): 0.35 means away team has 35% of home field category → -1.5% contribution
    private static final double HOME_FIELD_SCORE_FOR_AWAY = 0.35;

    public WinProbabilityResult calculate(
            TeamStatSnapshot away,
            TeamStatSnapshot home,
            PitcherStatSnapshot awayStarter,
            PitcherStatSnapshot homeStarter,
            BallparkFactorSnapshot park) {

        double offenseScore = calcOffenseScore(away, home);
        double starterScore = calcStarterScore(awayStarter, homeStarter);
        double bullpenScore = calcBullpenScore(away, home);
        double defenseScore = calcDefenseScore(away, home);
        double recentFormScore = calcRecentFormScore(away, home);
        double ballparkScore = calcBallparkFitScore(away, home, park);

        // Win probability: 50% base, each category shifts it
        // category score 0.5 = neutral, >0.5 = away advantage
        double awayWinPctRaw = 50.0
                + (offenseScore - 0.5) * OFFENSE_WEIGHT * 100
                + (starterScore - 0.5) * STARTER_WEIGHT * 100
                + (bullpenScore - 0.5) * BULLPEN_WEIGHT * 100
                + (defenseScore - 0.5) * DEFENSE_WEIGHT * 100
                + (recentFormScore - 0.5) * RECENT_FORM_WEIGHT * 100
                + (ballparkScore - 0.5) * BALLPARK_WEIGHT * 100
                + (HOME_FIELD_SCORE_FOR_AWAY - 0.5) * HOME_FIELD_WEIGHT * 100;

        int awayPct = (int) Math.round(Math.max(28, Math.min(72, awayWinPctRaw)));
        int homePct = 100 - awayPct;

        WinProbabilityResult result = new WinProbabilityResult();
        result.setAwayWinPct(awayPct);
        result.setHomeWinPct(homePct);
        result.setConfidenceLevel(calcConfidence(Math.abs(awayPct - 50)));

        result.setOffenseEdge(edge(offenseScore));
        result.setStarterEdge(edge(starterScore));
        result.setBullpenEdge(edge(bullpenScore));
        result.setDefenseEdge(edge(defenseScore));
        result.setRecentFormEdge(edge(recentFormScore));
        result.setBallparkEdge(edge(ballparkScore));

        result.setCategories(buildCategories(away, home, awayStarter, homeStarter,
                offenseScore, starterScore, bullpenScore, defenseScore, recentFormScore, ballparkScore));

        result.setAwayStrengths(buildStrengths(away, awayStarter, true));
        result.setAwayWeaknesses(buildWeaknesses(away, awayStarter, true));
        result.setHomeStrengths(buildStrengths(home, homeStarter, false));
        result.setHomeWeaknesses(buildWeaknesses(home, homeStarter, false));

        result.setBallparkNarrative(buildBallparkNarrative(away, home, park));
        result.setParkHighlights(buildParkHighlights(away, home, park, ballparkScore));
        result.setMainReasons(buildMainReasons(offenseScore, starterScore, bullpenScore,
                defenseScore, recentFormScore, ballparkScore));
        result.setModelSummary("Weighted model: offense 20%, starters 25%, bullpen 15%, defense 10%, recent form 10%, ballpark fit 14%, home field 6%.");

        return result;
    }

    // --- Category score calculations ---

    private double calcOffenseScore(TeamStatSnapshot away, TeamStatSnapshot home) {
        double wrcScore = higherBetter(safe(away.getTeamWrcPlus(), 100), safe(home.getTeamWrcPlus(), 100));
        double opsScore = higherBetter(safe(away.getTeamOps(), 0.720), safe(home.getTeamOps(), 0.720));
        double rpgScore = higherBetter(safe(away.getRunsPerGame(), 4.5), safe(home.getRunsPerGame(), 4.5));
        double kScore = lowerBetter(safe(away.getTeamKRate(), 23.0), safe(home.getTeamKRate(), 23.0));
        double bbScore = higherBetter(safe(away.getTeamBbRate(), 8.5), safe(home.getTeamBbRate(), 8.5));
        return wrcScore * 0.35 + opsScore * 0.25 + rpgScore * 0.20 + kScore * 0.10 + bbScore * 0.10;
    }

    private double calcStarterScore(PitcherStatSnapshot away, PitcherStatSnapshot home) {
        if (away == null && home == null) return 0.5;
        if (away == null || home == null) return 0.5;

        double fipScore = lowerBetter(safe(away.getFip(), 4.0), safe(home.getFip(), 4.0));
        double eraScore = lowerBetter(safe(away.getEra(), 4.0), safe(home.getEra(), 4.0));
        double whipScore = lowerBetter(safe(away.getWhip(), 1.30), safe(home.getWhip(), 1.30));
        double kbbScore = higherBetter(safe(away.getKMinusBbRate(), 15.0), safe(home.getKMinusBbRate(), 15.0));
        double oppOpsScore = lowerBetter(safe(away.getOpponentOps(), 0.720), safe(home.getOpponentOps(), 0.720));
        double hrScore = lowerBetter(safe(away.getHrPer9(), 1.2), safe(home.getHrPer9(), 1.2));

        return fipScore * 0.30 + eraScore * 0.15 + whipScore * 0.15
                + kbbScore * 0.20 + oppOpsScore * 0.10 + hrScore * 0.10;
    }

    private double calcBullpenScore(TeamStatSnapshot away, TeamStatSnapshot home) {
        double fipScore = lowerBetter(safe(away.getBullpenFip(), 4.0), safe(home.getBullpenFip(), 4.0));
        double eraScore = lowerBetter(safe(away.getBullpenEra(), 4.0), safe(home.getBullpenEra(), 4.0));
        double whipScore = lowerBetter(safe(away.getBullpenWhip(), 1.30), safe(home.getBullpenWhip(), 1.30));
        double kScore = higherBetter(safe(away.getBullpenKRate(), 24.0), safe(home.getBullpenKRate(), 24.0));
        double bbScore = lowerBetter(safe(away.getBullpenBbRate(), 9.0), safe(home.getBullpenBbRate(), 9.0));
        return fipScore * 0.35 + eraScore * 0.25 + whipScore * 0.15 + kScore * 0.15 + bbScore * 0.10;
    }

    private double calcDefenseScore(TeamStatSnapshot away, TeamStatSnapshot home) {
        double oaaScore = higherBetter(safe(away.getOutsAboveAverage(), 0) + 20.0, safe(home.getOutsAboveAverage(), 0) + 20.0);
        double defEffScore = higherBetter(safe(away.getDefensiveEfficiency(), 0.695), safe(home.getDefensiveEfficiency(), 0.695));
        return oaaScore * 0.65 + defEffScore * 0.35;
    }

    private double calcRecentFormScore(TeamStatSnapshot away, TeamStatSnapshot home) {
        double last10Score = higherBetter(
                safe(away.getLast10Wins(), 5) + 0.1,
                safe(home.getLast10Wins(), 5) + 0.1);
        double rdScore = higherBetter(
                safe(away.getLast5RunDifferential(), 0) + 30.0,
                safe(home.getLast5RunDifferential(), 0) + 30.0);
        double rsScore = higherBetter(safe(away.getLast5RunsScored(), 20.0), safe(home.getLast5RunsScored(), 20.0));
        double raScore = lowerBetter(safe(away.getLast5RunsAllowed(), 20.0), safe(home.getLast5RunsAllowed(), 20.0));
        return last10Score * 0.30 + rdScore * 0.30 + rsScore * 0.20 + raScore * 0.20;
    }

    private double calcBallparkFitScore(TeamStatSnapshot away, TeamStatSnapshot home, BallparkFactorSnapshot park) {
        if (park == null) return 0.48; // slight home team park advantage by default
        double runEnv = factor(park.getRunParkFactor());
        double hrEnv = factor(park.getHomeRunParkFactor());
        double doubleEnv = factor(park.getDoubleParkFactor());
        double hitEnv = factor(park.getHitParkFactor());
        double lhbEnv = factor(park.getLhbParkFactor());
        double rhbEnv = factor(park.getRhbParkFactor());
        double lfHrEnv = factor(park.getLfHomeRunFactor());
        double cfHrEnv = factor(park.getCfHomeRunFactor());
        double rfHrEnv = factor(park.getRfHomeRunFactor());

        double awayIso = safe(away.getTeamIso(), 0.155);
        double homeIso = safe(home.getTeamIso(), 0.155);
        double awayObp = safe(away.getTeamObp(), 0.315);
        double homeObp = safe(home.getTeamObp(), 0.315);
        double awaySlg = safe(away.getTeamSlg(), 0.405);
        double homeSlg = safe(home.getTeamSlg(), 0.405);
        double awayK = safe(away.getTeamKRate(), 23.0);
        double homeK = safe(home.getTeamKRate(), 23.0);

        double powerScore = 0.5 + (awayIso - homeIso) * (0.9 + Math.abs(hrEnv - 1.0) * 4.0);
        double contactScore = 0.5 + ((awayObp - homeObp) * 1.2 + (awaySlg - homeSlg) * 0.8)
                * (0.8 + Math.abs(hitEnv - 1.0) * 3.0);
        double doublesScore = 0.5 + (awaySlg - homeSlg) * (0.8 + Math.abs(doubleEnv - 1.0) * 3.0);
        double handednessScore = 0.5 + (awayIso - homeIso) * ((lhbEnv + rhbEnv) / 2.0) * 0.8;
        double directionalScore = 0.5 + (awayIso - homeIso)
                * ((lfHrEnv * 0.38) + (cfHrEnv * 0.20) + (rfHrEnv * 0.42) - 1.0) * 2.5;
        double strikeoutScore = 0.5 + (homeK - awayK) * 0.004;
        double runContextScore = 0.5 + (safe(away.getRunsPerGame(), 4.5) - safe(home.getRunsPerGame(), 4.5))
                * Math.abs(runEnv - 1.0) * 0.25;

        double score = powerScore * 0.28
                + contactScore * 0.18
                + doublesScore * 0.14
                + handednessScore * 0.12
                + directionalScore * 0.14
                + strikeoutScore * 0.06
                + runContextScore * 0.08;

        // Familiarity matters, but it should not swamp the baseball traits.
        score -= 0.015;

        return Math.max(0.30, Math.min(0.70, score));
    }

    // --- Helpers ---

    private double higherBetter(double away, double home) {
        double total = away + home;
        if (total == 0) return 0.5;
        return away / total;
    }

    private double lowerBetter(double away, double home) {
        double total = away + home;
        if (total == 0) return 0.5;
        return home / total;
    }

    private double safe(Double val, double fallback) {
        return val != null ? val : fallback;
    }

    private double safe(Integer val, double fallback) {
        return val != null ? val.doubleValue() : fallback;
    }

    private double factor(Integer val) {
        return (val != null ? val : 100) / 100.0;
    }

    private String edge(double score) {
        if (score > 0.53) return "Away";
        if (score < 0.47) return "Home";
        return "Even";
    }

    private String calcConfidence(int margin) {
        if (margin >= 10) return "High";
        if (margin >= 5) return "Medium";
        return "Low";
    }

    // --- Category table builder ---

    private List<CategoryComparison> buildCategories(TeamStatSnapshot away, TeamStatSnapshot home,
            PitcherStatSnapshot awayStarter, PitcherStatSnapshot homeStarter,
            double offScore, double startScore, double bullScore, double defScore,
            double formScore, double parkScore) {

        List<CategoryComparison> cats = new ArrayList<>();

        cats.add(new CategoryComparison("Offense (wRC+)",
                fmt(away.getTeamWrcPlus(), 1),
                fmt(home.getTeamWrcPlus(), 1),
                edge(offScore)));

        cats.add(new CategoryComparison("Offense (OPS)",
                fmt3(away.getTeamOps()),
                fmt3(home.getTeamOps()),
                edge(offScore)));

        cats.add(new CategoryComparison("Plate Discipline",
                pct(away.getTeamBbRate()) + " BB%, " + pct(away.getTeamKRate()) + " K%",
                pct(home.getTeamBbRate()) + " BB%, " + pct(home.getTeamKRate()) + " K%",
                edge(calcOffenseDiscipline(away, home))));

        String awayStarterLabel = awayStarter != null ? fmt(awayStarter.getFip(), 2) + " FIP" : "To be announced";
        String homeStarterLabel = homeStarter != null ? fmt(homeStarter.getFip(), 2) + " FIP" : "To be announced";
        cats.add(new CategoryComparison("Starting Pitcher", awayStarterLabel, homeStarterLabel, edge(startScore)));

        cats.add(new CategoryComparison("Bullpen",
                fmt(away.getBullpenEra(), 2) + " ERA",
                fmt(home.getBullpenEra(), 2) + " ERA",
                edge(bullScore)));

        cats.add(new CategoryComparison("Defense (OAA)",
                oaaLabel(away.getOutsAboveAverage()),
                oaaLabel(home.getOutsAboveAverage()),
                edge(defScore)));

        cats.add(new CategoryComparison("Recent Form (L10)",
                away.getLast10Record(),
                home.getLast10Record(),
                edge(formScore)));

        cats.add(new CategoryComparison("Ballpark Fit", "See below", "Home park", edge(parkScore)));

        return cats;
    }

    private double calcOffenseDiscipline(TeamStatSnapshot away, TeamStatSnapshot home) {
        double awayScore = safe(away.getTeamBbRate(), 8.5) - safe(away.getTeamKRate(), 23.0) * 0.3;
        double homeScore = safe(home.getTeamBbRate(), 8.5) - safe(home.getTeamKRate(), 23.0) * 0.3;
        return higherBetter(awayScore + 20, homeScore + 20);
    }

    // --- Strengths / Weaknesses ---

    private List<String> buildStrengths(TeamStatSnapshot stats, PitcherStatSnapshot starter, boolean isAway) {
        List<String> list = new ArrayList<>();
        if (stats == null) return list;

        if (stats.getTeamWrcPlus() != null && stats.getTeamWrcPlus() >= 108)
            list.add("Team wRC+ " + fmt(stats.getTeamWrcPlus(), 0) + " - above-average offense");
        if (stats.getTeamObp() != null && stats.getTeamObp() >= 0.325)
            list.add("Team OBP " + fmt3(stats.getTeamObp()) + " - strong on-base rate");
        if (stats.getBullpenEra() != null && stats.getBullpenEra() <= 3.60)
            list.add("Bullpen ERA " + fmt(stats.getBullpenEra(), 2) + " - reliable late innings");
        if (stats.getRunDifferential() != null && stats.getRunDifferential() >= 20)
            list.add("Run differential " + (stats.getRunDifferential() > 0 ? "+" : "") + stats.getRunDifferential());
        if (stats.getOutsAboveAverage() != null && stats.getOutsAboveAverage() >= 5)
            list.add("Defense: +" + stats.getOutsAboveAverage() + " Outs Above Average");
        if (starter != null && starter.getKMinusBbRate() != null && starter.getKMinusBbRate() >= 18)
            list.add("Starter K-BB% " + fmt(starter.getKMinusBbRate(), 1) + "% - elite control and strikeouts");
        if (stats.getLast10Wins() != null && stats.getLast10Wins() >= 7)
            list.add("Last 10 games: " + stats.getLast10Record() + " - hot streak");

        if (list.isEmpty()) list.add("Competitive overall profile");
        return list.size() > 4 ? list.subList(0, 4) : list;
    }

    private List<String> buildWeaknesses(TeamStatSnapshot stats, PitcherStatSnapshot starter, boolean isAway) {
        List<String> list = new ArrayList<>();
        if (stats == null) return list;

        if (stats.getTeamKRate() != null && stats.getTeamKRate() >= 24.0)
            list.add("K% " + fmt(stats.getTeamKRate(), 1) + "% - strikeout rate is high");
        if (stats.getOutsAboveAverage() != null && stats.getOutsAboveAverage() <= -5)
            list.add("OAA " + stats.getOutsAboveAverage() + " - below-average defense");
        if (starter != null && starter.getHrPer9() != null && starter.getHrPer9() >= 1.3)
            list.add("Starter HR/9 " + fmt(starter.getHrPer9(), 2) + " - allows too many home runs");
        if (stats.getBullpenBbRate() != null && stats.getBullpenBbRate() >= 10.0)
            list.add("Bullpen BB% " + fmt(stats.getBullpenBbRate(), 1) + "% - too many free passes");
        if (starter != null && starter.getWhip() != null && starter.getWhip() >= 1.35)
            list.add("Starter WHIP " + fmt(starter.getWhip(), 2) + " - too many baserunners");
        if (stats.getLast10Wins() != null && stats.getLast10Wins() <= 3)
            list.add("Last 10 games: " + stats.getLast10Record() + " - struggling recently");

        if (list.isEmpty()) list.add("No glaring weaknesses identified");
        return list.size() > 4 ? list.subList(0, 4) : list;
    }

    private String buildBallparkNarrative(TeamStatSnapshot away, TeamStatSnapshot home, BallparkFactorSnapshot park) {
        if (park == null) return "No park factor data available.";
        StringBuilder sb = new StringBuilder();
        sb.append(park.getVenueName()).append(": ");

        int hrFactor = park.getHomeRunParkFactor() != null ? park.getHomeRunParkFactor() : 100;
        int dblFactor = park.getDoubleParkFactor() != null ? park.getDoubleParkFactor() : 100;
        int runFactor = park.getRunParkFactor() != null ? park.getRunParkFactor() : 100;

        if (hrFactor > 105) sb.append("Boosts home runs (+").append(hrFactor - 100).append("%). ");
        else if (hrFactor < 95) sb.append("Suppresses home runs (").append(hrFactor - 100).append("%). ");

        if (dblFactor > 110) sb.append("Strong doubles environment (+").append(dblFactor - 100).append("%). ");

        if (runFactor > 104) sb.append("Slightly offense-friendly. ");
        else if (runFactor < 96) sb.append("Pitcher-friendly park. ");

        if (park.getLhbParkFactor() != null && park.getLhbParkFactor() > 105)
            sb.append("Favors left-handed hitters. ");
        if (park.getRhbParkFactor() != null && park.getRhbParkFactor() > 105)
            sb.append("Favors right-handed hitters. ");
        if (park.getLfHomeRunFactor() != null && park.getLfHomeRunFactor() >= 108)
            sb.append("Left-field home runs play up. ");
        if (park.getRfHomeRunFactor() != null && park.getRfHomeRunFactor() >= 108)
            sb.append("Right-field home runs play up. ");

        return sb.toString().trim();
    }

    private List<String> buildParkHighlights(TeamStatSnapshot away, TeamStatSnapshot home,
                                             BallparkFactorSnapshot park, double parkScore) {
        List<String> list = new ArrayList<>();
        if (park == null) {
            list.add("Park factor data is missing, so the model keeps this category close to neutral.");
            return list;
        }

        list.add("Ballpark edge: " + edge(parkScore) + " (" + Math.round(parkScore * 100) + "/100 fit score).");
        addFactorHighlight(list, "Run environment", park.getRunParkFactor());
        addFactorHighlight(list, "Home run environment", park.getHomeRunParkFactor());
        addFactorHighlight(list, "Doubles environment", park.getDoubleParkFactor());
        addFactorHighlight(list, "Left-field HR lane", park.getLfHomeRunFactor());
        addFactorHighlight(list, "Right-field HR lane", park.getRfHomeRunFactor());

        double isoGap = safe(away.getTeamIso(), 0.155) - safe(home.getTeamIso(), 0.155);
        if (Math.abs(isoGap) >= 0.015) {
            list.add((isoGap > 0 ? "Away" : "Home") + " lineup brings the stronger isolated-power profile into this park.");
        }

        if (list.size() > 5) return list.subList(0, 5);
        return list;
    }

    private void addFactorHighlight(List<String> list, String label, Integer factor) {
        if (factor == null) return;
        int diff = factor - 100;
        if (Math.abs(diff) < 5) return;
        String direction = diff > 0 ? "boosts" : "suppresses";
        list.add(label + " " + direction + " by about " + Math.abs(diff) + "%.");
    }

    private List<String> buildMainReasons(double offenseScore, double starterScore, double bullpenScore,
                                          double defenseScore, double formScore, double parkScore) {
        List<String> list = new ArrayList<>();
        addReason(list, "Offense", offenseScore);
        addReason(list, "Starting pitching", starterScore);
        addReason(list, "Bullpen", bullpenScore);
        addReason(list, "Defense", defenseScore);
        addReason(list, "Recent form", formScore);
        addReason(list, "Ballpark fit", parkScore);
        return list;
    }

    private void addReason(List<String> list, String label, double score) {
        if (Math.abs(score - 0.5) < 0.025) return;
        list.add(label + " leans " + edge(score).toLowerCase() + ".");
    }

    // --- Formatting helpers ---

    private String fmt(Double val, int decimals) {
        if (val == null) return "N/A";
        return String.format("%." + decimals + "f", val);
    }

    private String fmt3(Double val) {
        if (val == null) return "N/A";
        return String.format(".%03d", Math.round(val * 1000));
    }

    private String pct(Double val) {
        if (val == null) return "N/A";
        return String.format("%.1f", val);
    }

    private String oaaLabel(Integer oaa) {
        return oaa != null ? (oaa >= 0 ? "+" : "") + oaa + " OAA" : "N/A OAA";
    }
}
