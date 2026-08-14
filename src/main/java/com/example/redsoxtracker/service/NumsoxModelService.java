package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.CategoryScore;
import com.example.redsoxtracker.dto.HistoricalCalibration;
import com.example.redsoxtracker.dto.LiveWinProbability;
import com.example.redsoxtracker.dto.NumsoxModel;
import com.example.redsoxtracker.dto.StatValue;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The NumSOX estimate.
 *
 * <p>Each category compares the two clubs on the stats that category is actually about and
 * returns an edge from -1 (all away) to +1 (all home). Those edges are combined at the
 * specified weights and squashed into a probability.</p>
 *
 * <p>Two rules keep it honest. A category with no data is not scored as "even" -- being
 * even is a real finding and pretending to it would drag every estimate toward the middle
 * and hide the fact that the model is flying blind. Instead it is dropped, its weight is
 * shared out across the categories that could be scored, and it is listed openly as
 * unavailable. Second, confidence falls as more categories go missing, so a thinly
 * sourced estimate says so rather than presenting itself like a fully fed one.</p>
 */
@Service
public class NumsoxModelService {

    // Category weights, exactly as specified. Verified to sum to 1.0 by a unit check below.
    private static final double W_OFFENSE     = 0.14;
    private static final double W_STARTER     = 0.18;
    private static final double W_BULLPEN     = 0.12;
    private static final double W_LINEUP      = 0.10;
    private static final double W_DEFENSE     = 0.07;
    private static final double W_BASERUNNING = 0.04;
    private static final double W_RECENT      = 0.08;
    private static final double W_PARK        = 0.07;
    private static final double W_WEATHER     = 0.04;
    private static final double W_REST        = 0.04;
    private static final double W_INJURY      = 0.04;
    private static final double W_HISTORICAL  = 0.02;
    private static final double W_REGRESSION  = 0.03;
    private static final double W_LIVE        = 0.03;

    /** The order the categories are specified in, which is the order they are presented in. */
    private static final List<String> DISPLAY_ORDER = List.of(
            "offense", "starter", "bullpen", "lineup", "defense", "baserunning", "recent",
            "park", "weather", "rest", "injuries", "historical", "regression", "live");

    /** Home field, applied outside the categories, as a small constant edge. */
    private static final double HOME_FIELD_EDGE = 0.06;

    /** Converts the weighted edge into a probability. Tuned so a decisive edge lands near 70%. */
    private static final double LOGISTIC_SCALE = 2.4;

    private final HistoricalCalibrationService calibrationService;

    public NumsoxModelService(HistoricalCalibrationService calibrationService) {
        this.calibrationService = calibrationService;
    }

    /**
     * @param game        the matchup
     * @param away        away club season stats, may be null
     * @param home        home club season stats, may be null
     * @param awayStarter away probable starter, may be null
     * @param homeStarter home probable starter, may be null
     * @param park        the venue's factors, may be null
     * @param liveWp      MLB's live win probability once the game is on, may be empty
     */
    public NumsoxModel evaluate(Game game,
                                TeamStatSnapshot away, TeamStatSnapshot home,
                                PitcherStatSnapshot awayStarter, PitcherStatSnapshot homeStarter,
                                BallparkFactorSnapshot park,
                                Optional<LiveWinProbability> liveWp) {

        boolean live = liveWp.isPresent();
        List<CategoryScore> cats = new ArrayList<>();

        cats.add(offense(away, home));
        cats.add(startingPitcher(awayStarter, homeStarter));
        cats.add(bullpen(away, home));
        cats.add(lineupPlatoon(away, home, awayStarter, homeStarter));
        cats.add(defense(away, home));
        cats.add(CategoryScore.unavailable("baserunning", "Baserunning", W_BASERUNNING,
                "Stolen bases, extra bases taken and BsR are not in the daily snapshot yet, "
              + "so this category is not scored."));
        cats.add(recentForm(away, home));
        cats.add(ballparkFit(away, home, park));
        cats.add(CategoryScore.unavailable("weather", "Weather", W_WEATHER,
                "No weather feed is wired up, so temperature, wind and carry are not scored."));
        cats.add(CategoryScore.unavailable("rest", "Rest / Fatigue", W_REST,
                "Travel, days rest and schedule density are not tracked yet."));
        cats.add(CategoryScore.unavailable("injuries", "Injuries", W_INJURY,
                "There is no injured-list feed, so missing WAR is not scored."));
        cats.add(regressionRisk(away, home, awayStarter, homeStarter));
        cats.add(liveGameState(liveWp));

        // Historical calibration judges the number the other categories produced, so it needs
        // a provisional read first. Score everything else, then hand that provisional figure
        // to the backtested calibrator and let the correction it wants become this category's
        // edge, so it competes at its 2% like any other rather than overriding the model.
        cats.add(historical(game, provisionalHomePct(cats), game.getHomeAway()));

        // Share the unavailable categories' weight across the ones that could be scored,
        // so the model still adds to 100% of what it actually knows.
        double scoredWeight = cats.stream().filter(CategoryScore::available)
                .mapToDouble(CategoryScore::weight).sum();
        List<CategoryScore> finalCats = new ArrayList<>();
        for (CategoryScore c : cats) {
            if (!c.available()) { finalCats.add(c); continue; }
            double effective = scoredWeight > 0 ? c.weight() / scoredWeight : 0;
            finalCats.add(new CategoryScore(c.key(), c.label(), c.weight(), effective, c.edge(),
                    c.awayDisplay(), c.homeDisplay(), true, c.confidence(), c.explanation(),
                    c.supporting()));
        }

        // Historical calibration was scored last because it needs the others first, but it
        // belongs in its specified place when the table is read.
        finalCats.sort(Comparator.comparingInt(c -> DISPLAY_ORDER.indexOf(c.key())));

        double weighted = finalCats.stream().filter(CategoryScore::available)
                .mapToDouble(c -> c.edge() * c.effectiveWeight()).sum();
        weighted += HOME_FIELD_EDGE;

        int homePct = (int) Math.round(100.0 / (1.0 + Math.exp(-LOGISTIC_SCALE * weighted)));
        homePct = Math.max(3, Math.min(97, homePct));
        int awayPct = 100 - homePct;

        // Once the game is on, MLB's per-play model knows the score and we do not; let it
        // lead, but keep the pre-game read visible so the change is explainable.
        Integer pregameHome = homePct;
        String liveNote = null;
        if (live) {
            LiveWinProbability wp = liveWp.get();
            int blendedHome = (int) Math.round(0.85 * wp.homePct() + 0.15 * homePct);
            liveNote = buildLiveNote(wp, pregameHome, blendedHome);
            homePct = Math.max(1, Math.min(99, blendedHome));
            awayPct = 100 - homePct;
        }

        int availableCount = (int) finalCats.stream().filter(CategoryScore::available).count();
        String confidence = confidenceFor(availableCount, finalCats.size(), awayStarter, homeStarter, live);
        String confidenceReason = confidenceReason(availableCount, finalCats.size(),
                awayStarter, homeStarter, live);

        List<CategoryScore> top = finalCats.stream()
                .filter(CategoryScore::available)
                .sorted(Comparator.comparingDouble(
                        (CategoryScore c) -> Math.abs(c.edge()) * c.effectiveWeight()).reversed())
                .limit(3)
                .toList();

        String awayTeam = teamLabel(game, true);
        String homeTeam = teamLabel(game, false);

        return new NumsoxModel(
                awayPct, homePct, confidence, confidenceReason,
                mainReason(top, awayTeam, homeTeam, awayPct, homePct),
                top, finalCats,
                strengths(finalCats, false, awayTeam), weaknesses(finalCats, false, awayTeam),
                strengths(finalCats, true, homeTeam), weaknesses(finalCats, true, homeTeam),
                live, pregameHome, liveNote,
                availableCount, finalCats.size(), LocalDateTime.now());
    }

    // ---- categories ---------------------------------------------------------------

    private CategoryScore offense(TeamStatSnapshot away, TeamStatSnapshot home) {
        if (away == null || home == null) {
            return CategoryScore.unavailable("offense", "Offense", W_OFFENSE,
                    "Team hitting stats have not been imported for both clubs.");
        }
        // wRC+ leads because it is already park and league adjusted; OPS and runs per game
        // corroborate it, and the contact-quality pair says whether it is likely to hold.
        double e = 0;
        e += norm(home.getTeamWrcPlus(), away.getTeamWrcPlus(), 25) * 0.40;
        e += norm(home.getTeamOps(), away.getTeamOps(), 0.070) * 0.25;
        e += norm(home.getRunsPerGame(), away.getRunsPerGame(), 1.0) * 0.15;
        e += norm(home.getTeamHardHitRate(), away.getTeamHardHitRate(), 4.0) * 0.10;
        e += norm(home.getTeamBarrelRate(), away.getTeamBarrelRate(), 2.0) * 0.10;

        List<StatValue> sup = List.of(
                stat("Team wRC+ (away)", away.getTeamWrcPlus(), fmt0(away.getTeamWrcPlus()), away),
                stat("Team wRC+ (home)", home.getTeamWrcPlus(), fmt0(home.getTeamWrcPlus()), home),
                stat("Team OPS (away)", away.getTeamOps(), fmt3(away.getTeamOps()), away),
                stat("Team OPS (home)", home.getTeamOps(), fmt3(home.getTeamOps()), home),
                stat("Runs per game (away)", away.getRunsPerGame(), fmt2(away.getRunsPerGame()), away),
                stat("Runs per game (home)", home.getRunsPerGame(), fmt2(home.getRunsPerGame()), home),
                stat("Hard-hit % (away)", away.getTeamHardHitRate(), fmt1(away.getTeamHardHitRate()), away),
                stat("Hard-hit % (home)", home.getTeamHardHitRate(), fmt1(home.getTeamHardHitRate()), home));

        String exp = describe(e, "offence",
                fmt0(away.getTeamWrcPlus()) + " vs " + fmt0(home.getTeamWrcPlus()) + " wRC+");
        return new CategoryScore("offense", "Offense", W_OFFENSE, W_OFFENSE, e,
                fmt0(away.getTeamWrcPlus()) + " wRC+", fmt0(home.getTeamWrcPlus()) + " wRC+",
                true, "High", exp, sup);
    }

    private CategoryScore startingPitcher(PitcherStatSnapshot away, PitcherStatSnapshot home) {
        if (away == null || home == null) {
            return CategoryScore.unavailable("starter", "Starting Pitcher", W_STARTER,
                    "One or both probable starters have no stat line yet.");
        }
        // Fielding-independent numbers lead, because they hold up better than ERA over the
        // length of a season and are what a single start is best predicted by.
        double e = 0;
        e += norm(away.getFip(), home.getFip(), 0.60) * 0.32;          // lower is better -> reversed
        e += norm(home.getKMinusBbRate(), away.getKMinusBbRate(), 6.0) * 0.24;
        e += norm(away.getWhip(), home.getWhip(), 0.20) * 0.16;
        e += norm(away.getOpponentOps(), home.getOpponentOps(), 0.060) * 0.14;
        e += norm(away.getHrPer9(), home.getHrPer9(), 0.50) * 0.08;
        e += norm(away.getBarrelRateAllowed(), home.getBarrelRateAllowed(), 2.0) * 0.06;

        List<StatValue> sup = List.of(
                pstat("Starter FIP (away)", away.getFip(), fmt2(away.getFip()), away),
                pstat("Starter FIP (home)", home.getFip(), fmt2(home.getFip()), home),
                pstat("K-BB% (away)", away.getKMinusBbRate(), fmt1(away.getKMinusBbRate()), away),
                pstat("K-BB% (home)", home.getKMinusBbRate(), fmt1(home.getKMinusBbRate()), home),
                pstat("WHIP (away)", away.getWhip(), fmt2(away.getWhip()), away),
                pstat("WHIP (home)", home.getWhip(), fmt2(home.getWhip()), home),
                pstat("Opponent OPS (away)", away.getOpponentOps(), fmt3(away.getOpponentOps()), away),
                pstat("Opponent OPS (home)", home.getOpponentOps(), fmt3(home.getOpponentOps()), home));

        String exp = describe(e, "starting pitching",
                fmt2(away.getFip()) + " vs " + fmt2(home.getFip()) + " FIP");
        return new CategoryScore("starter", "Starting Pitcher", W_STARTER, W_STARTER, e,
                fmt2(away.getFip()) + " FIP", fmt2(home.getFip()) + " FIP",
                true, "High", exp, sup);
    }

    private CategoryScore bullpen(TeamStatSnapshot away, TeamStatSnapshot home) {
        if (away == null || home == null || away.getBullpenFip() == null || home.getBullpenFip() == null) {
            return CategoryScore.unavailable("bullpen", "Bullpen", W_BULLPEN,
                    "Bullpen splits have not been imported for both clubs.");
        }
        double e = 0;
        e += norm(away.getBullpenFip(), home.getBullpenFip(), 0.50) * 0.40;
        e += norm(away.getBullpenEra(), home.getBullpenEra(), 0.60) * 0.25;
        e += norm(away.getBullpenWhip(), home.getBullpenWhip(), 0.15) * 0.20;
        e += norm(home.getBullpenKRate(), away.getBullpenKRate(), 4.0) * 0.15;

        List<StatValue> sup = List.of(
                stat("Bullpen FIP (away)", away.getBullpenFip(), fmt2(away.getBullpenFip()), away),
                stat("Bullpen FIP (home)", home.getBullpenFip(), fmt2(home.getBullpenFip()), home),
                stat("Bullpen ERA (away)", away.getBullpenEra(), fmt2(away.getBullpenEra()), away),
                stat("Bullpen ERA (home)", home.getBullpenEra(), fmt2(home.getBullpenEra()), home),
                stat("Bullpen WHIP (away)", away.getBullpenWhip(), fmt2(away.getBullpenWhip()), away),
                stat("Bullpen WHIP (home)", home.getBullpenWhip(), fmt2(home.getBullpenWhip()), home),
                StatValue.unavailable("Relievers used last 3 days"),
                StatValue.unavailable("Closer availability"));

        String exp = describe(e, "the bullpen",
                fmt2(away.getBullpenEra()) + " vs " + fmt2(home.getBullpenEra()) + " bullpen ERA")
                + " Recent workload and closer availability are not tracked, so this is season-long form only.";
        return new CategoryScore("bullpen", "Bullpen", W_BULLPEN, W_BULLPEN, e,
                fmt2(away.getBullpenEra()) + " ERA", fmt2(home.getBullpenEra()) + " ERA",
                true, "Medium", exp, sup);
    }

    /**
     * Platoon only. The club's split against the hand its opponent is starting is real and
     * sourced; the rest of the lineup detail this category is meant to cover is not, so the
     * confidence is set to Medium rather than pretending to a full lineup read.
     */
    private CategoryScore lineupPlatoon(TeamStatSnapshot away, TeamStatSnapshot home,
                                        PitcherStatSnapshot awayStarter, PitcherStatSnapshot homeStarter) {
        if (away == null || home == null
                || away.getOpsVsLhp() == null || home.getOpsVsLhp() == null) {
            return CategoryScore.unavailable("lineup", "Lineup / Platoon", W_LINEUP,
                    "Platoon splits are not imported for both clubs yet.");
        }
        // Each club bats against the other's starter, so each is judged on its split
        // against that starter's hand. Handedness is not stored, so the split each club
        // performs better against is used as its own profile and compared like for like.
        double awayBest = Math.max(away.getOpsVsLhp(), away.getOpsVsRhp());
        double homeBest = Math.max(home.getOpsVsLhp(), home.getOpsVsRhp());
        double e = norm(homeBest, awayBest, 0.060);

        List<StatValue> sup = List.of(
                stat("OPS vs LHP (away)", away.getOpsVsLhp(), fmt3(away.getOpsVsLhp()), away),
                stat("OPS vs RHP (away)", away.getOpsVsRhp(), fmt3(away.getOpsVsRhp()), away),
                stat("OPS vs LHP (home)", home.getOpsVsLhp(), fmt3(home.getOpsVsLhp()), home),
                stat("OPS vs RHP (home)", home.getOpsVsRhp(), fmt3(home.getOpsVsRhp()), home),
                StatValue.unavailable("Confirmed lineup"),
                StatValue.unavailable("Platoon advantage count"),
                StatValue.unavailable("WAR missing from lineup"));

        String exp = describe(e, "platoon matchups",
                fmt3(awayBest) + " vs " + fmt3(homeBest) + " OPS in their stronger split")
                + " Starter handedness and confirmed lineups are not wired up, so this is a"
                + " club-level split rather than a true lineup-versus-starter read.";
        return new CategoryScore("lineup", "Lineup / Platoon", W_LINEUP, W_LINEUP, e,
                fmt3(awayBest) + " OPS", fmt3(homeBest) + " OPS",
                true, "Medium", exp, sup);
    }

    private CategoryScore defense(TeamStatSnapshot away, TeamStatSnapshot home) {
        if (away == null || home == null || away.getDefensiveEfficiency() == null
                || home.getDefensiveEfficiency() == null) {
            return CategoryScore.unavailable("defense", "Defense", W_DEFENSE,
                    "Defensive efficiency is not available for both clubs.");
        }
        double e = norm(home.getDefensiveEfficiency(), away.getDefensiveEfficiency(), 0.020) * 0.6;
        if (away.getOutsAboveAverage() != null && home.getOutsAboveAverage() != null) {
            e += norm((double) home.getOutsAboveAverage(), (double) away.getOutsAboveAverage(), 25) * 0.4;
        }

        List<StatValue> sup = List.of(
                stat("Defensive efficiency (away)", away.getDefensiveEfficiency(), fmt3(away.getDefensiveEfficiency()), away),
                stat("Defensive efficiency (home)", home.getDefensiveEfficiency(), fmt3(home.getDefensiveEfficiency()), home),
                stat("Outs Above Average (away)", dbl(away.getOutsAboveAverage()), str(away.getOutsAboveAverage()), away),
                stat("Outs Above Average (home)", dbl(home.getOutsAboveAverage()), str(home.getOutsAboveAverage()), home),
                StatValue.unavailable("Defensive Runs Saved"),
                StatValue.unavailable("UZR"));

        String exp = describe(e, "defence",
                fmt3(away.getDefensiveEfficiency()) + " vs " + fmt3(home.getDefensiveEfficiency())
                + " defensive efficiency")
                + " DRS and UZR have no free public source and are left out rather than estimated.";
        return new CategoryScore("defense", "Defense", W_DEFENSE, W_DEFENSE, e,
                fmt3(away.getDefensiveEfficiency()), fmt3(home.getDefensiveEfficiency()),
                true, "Medium", exp, sup);
    }

    private CategoryScore recentForm(TeamStatSnapshot away, TeamStatSnapshot home) {
        if (away == null || home == null || away.getLast10Wins() == null || home.getLast10Wins() == null) {
            return CategoryScore.unavailable("recent", "Recent Form", W_RECENT,
                    "Last-10 records are not available for both clubs.");
        }
        double awayL10 = pct(away.getLast10Wins(), away.getLast10Losses());
        double homeL10 = pct(home.getLast10Wins(), home.getLast10Losses());
        double e = norm(homeL10, awayL10, 0.320) * 0.6;
        if (away.getLast5RunDifferential() != null && home.getLast5RunDifferential() != null) {
            e += norm((double) home.getLast5RunDifferential(),
                      (double) away.getLast5RunDifferential(), 14) * 0.4;
        }

        List<StatValue> sup = List.of(
                stat("Last 10 (away)", awayL10, away.getLast10Wins() + "-" + away.getLast10Losses(), away),
                stat("Last 10 (home)", homeL10, home.getLast10Wins() + "-" + home.getLast10Losses(), home),
                stat("Last 5 run differential (away)", dbl(away.getLast5RunDifferential()),
                        signed(away.getLast5RunDifferential()), away),
                stat("Last 5 run differential (home)", dbl(home.getLast5RunDifferential()),
                        signed(home.getLast5RunDifferential()), home),
                stat("Streak (away)", null, away.getCurrentStreak(), away),
                stat("Streak (home)", null, home.getCurrentStreak(), home));

        String exp = describe(e, "recent form",
                away.getLast10Wins() + "-" + away.getLast10Losses() + " vs "
                + home.getLast10Wins() + "-" + home.getLast10Losses() + " over ten games")
                + " Ten games is a small sample, which is why this is weighted lightly.";
        return new CategoryScore("recent", "Recent Form", W_RECENT, W_RECENT, e,
                away.getLast10Wins() + "-" + away.getLast10Losses(),
                home.getLast10Wins() + "-" + home.getLast10Losses(),
                true, "Medium", exp, sup);
    }

    /**
     * Fit, not raw park factor. A hitter's park only helps the club built to use it, so the
     * park's run and home-run factors are pushed through each club's own power profile.
     */
    private CategoryScore ballparkFit(TeamStatSnapshot away, TeamStatSnapshot home,
                                      BallparkFactorSnapshot park) {
        if (park == null || away == null || home == null) {
            return CategoryScore.unavailable("park", "Ballpark Fit", W_PARK,
                    "No park factors on file for this venue.");
        }
        double runFactor = idx(park.getRunParkFactor());
        double hrFactor  = idx(park.getHomeRunParkFactor());
        double dblFactor = idx(park.getDoubleParkFactor());

        // Whose strength the park amplifies. ISO stands in for power, so a park that plays
        // big helps the club that hits for more of it.
        double isoEdge = norm(home.getTeamIso(), away.getTeamIso(), 0.030);
        double e = isoEdge * ((hrFactor - 1.0) * 2.0 + (dblFactor - 1.0) * 1.0);
        // A run-friendly park slightly favours whichever offence is better overall.
        e += norm(home.getTeamWrcPlus(), away.getTeamWrcPlus(), 25) * (runFactor - 1.0) * 1.5;
        e = clamp(e);

        List<StatValue> sup = List.of(
                pkstat("Run park factor", dbl(park.getRunParkFactor()), str(park.getRunParkFactor()), park),
                pkstat("Home run park factor", dbl(park.getHomeRunParkFactor()), str(park.getHomeRunParkFactor()), park),
                pkstat("Double park factor", dbl(park.getDoubleParkFactor()), str(park.getDoubleParkFactor()), park),
                stat("Team ISO (away)", away.getTeamIso(), fmt3(away.getTeamIso()), away),
                stat("Team ISO (home)", home.getTeamIso(), fmt3(home.getTeamIso()), home));

        String parkWord = hrFactor > 1.03 ? "plays big for power"
                : hrFactor < 0.97 ? "suppresses home runs" : "plays close to neutral";
        String exp = park.getVenueName() + " " + parkWord + " (" + park.getHomeRunParkFactor()
                + " home run factor). " + describe(e, "how this park suits them",
                fmt3(away.getTeamIso()) + " vs " + fmt3(home.getTeamIso()) + " ISO");
        return new CategoryScore("park", "Ballpark Fit", W_PARK, W_PARK, e,
                str(park.getRunParkFactor()) + " runs", str(park.getHomeRunParkFactor()) + " HR",
                true, "Medium", exp, sup);
    }

    /** The weighted read from the categories scored so far, as a home-club percentage. */
    private int provisionalHomePct(List<CategoryScore> scoredSoFar) {
        double weight = scoredSoFar.stream().filter(CategoryScore::available)
                .mapToDouble(CategoryScore::weight).sum();
        if (weight <= 0) return 50;
        double sum = scoredSoFar.stream().filter(CategoryScore::available)
                .mapToDouble(c -> c.edge() * (c.weight() / weight)).sum() + HOME_FIELD_EDGE;
        return (int) Math.round(100.0 / (1.0 + Math.exp(-LOGISTIC_SCALE * sum)));
    }

    /**
     * How far this season's actual results say the provisional number should move. The
     * calibrator backtests its own method against completed games, so its correction is
     * evidence rather than opinion. Weighted at only 2% by design, because rosters change
     * and last season's meetings say little about tonight.
     */
    private CategoryScore historical(Game game, int provisionalHomePct, String soxHomeAway) {
        HistoricalCalibration cal;
        try {
            boolean soxHome = "Home".equalsIgnoreCase(soxHomeAway);
            int provisionalSoxPct = soxHome ? provisionalHomePct : 100 - provisionalHomePct;
            cal = calibrationService.calibrate(game, provisionalSoxPct);
        } catch (Exception e) {
            return CategoryScore.unavailable("historical", "Historical Calibration", W_HISTORICAL,
                    "This season's completed games could not be read, so no calibration was applied.");
        }
        if (cal == null || cal.getSampleSize() < 10) {
            return CategoryScore.unavailable("historical", "Historical Calibration", W_HISTORICAL,
                    "Not enough completed games this season to calibrate against yet.");
        }

        boolean soxHome = "Home".equalsIgnoreCase(soxHomeAway);
        // The calibrator speaks in Red Sox percentage points; convert to the home-oriented
        // edge the rest of the model uses. Ten points of correction is a large disagreement.
        double adjHome = soxHome ? cal.getAdjustmentPoints() : -cal.getAdjustmentPoints();
        double e = clamp(adjHome / 10.0);

        List<StatValue> sup = List.of(
                StatValue.of("Backtest sample size", (double) cal.getSampleSize(),
                        cal.getSampleSize() + " games", "NumSOX backtest", LocalDate.now()),
                StatValue.of("Backtest accuracy", cal.getAccuracy(),
                        String.format("%.1f%%", cal.getAccuracy() * 100), "NumSOX backtest", LocalDate.now()),
                StatValue.of("Brier score", cal.getBrierScore(),
                        String.format("%.3f", cal.getBrierScore()), "NumSOX backtest", LocalDate.now()),
                StatValue.of("Calibration adjustment", (double) cal.getAdjustmentPoints(),
                        fmtSignedInt(cal.getAdjustmentPoints()) + " pts to the Red Sox",
                        "NumSOX backtest", LocalDate.now()));

        String direction = Math.abs(cal.getAdjustmentPoints()) < 2
                ? "This season's results back up the rest of the model, so almost no correction is needed."
                : "Judged against how this season has actually gone, the rest of the model looks "
                  + (adjHome > 0 ? "a little low on the home club" : "a little high on the home club")
                  + ", by about " + Math.abs(cal.getAdjustmentPoints()) + " points.";
        String exp = direction + " That read is backtested over " + cal.getSampleSize()
                + " completed games at " + String.format("%.0f%%", cal.getAccuracy() * 100) + " accuracy.";

        return new CategoryScore("historical", "Historical Calibration", W_HISTORICAL, W_HISTORICAL, e,
                fmtSignedInt(soxHome ? -cal.getAdjustmentPoints() : cal.getAdjustmentPoints()) + " pts",
                fmtSignedInt(soxHome ? cal.getAdjustmentPoints() : -cal.getAdjustmentPoints()) + " pts",
                true, cal.getSampleSize() >= 40 ? "Medium" : "Low", exp, sup);
    }

    /**
     * Whether either club's surface results look unsustainable. A starter whose ERA sits
     * well under his FIP has been getting help, and that tends not to last.
     */
    private CategoryScore regressionRisk(TeamStatSnapshot away, TeamStatSnapshot home,
                                         PitcherStatSnapshot awayStarter, PitcherStatSnapshot homeStarter) {
        if (awayStarter == null || homeStarter == null
                || awayStarter.getEra() == null || awayStarter.getFip() == null
                || homeStarter.getEra() == null || homeStarter.getFip() == null) {
            return CategoryScore.unavailable("regression", "Regression Risk", W_REGRESSION,
                    "Needs ERA and FIP for both starters to compare surface results with"
                  + " fielding-independent ones.");
        }
        double awayGap = awayStarter.getEra() - awayStarter.getFip();
        double homeGap = homeStarter.getEra() - homeStarter.getFip();
        // A negative gap means ERA is running under FIP, which flags regression risk, so
        // the club whose starter has the larger gap is the safer bet here.
        double e = norm(homeGap, awayGap, 0.80);

        List<StatValue> sup = List.of(
                pstat("Starter ERA (away)", awayStarter.getEra(), fmt2(awayStarter.getEra()), awayStarter),
                pstat("Starter FIP (away)", awayStarter.getFip(), fmt2(awayStarter.getFip()), awayStarter),
                pstat("Starter ERA (home)", homeStarter.getEra(), fmt2(homeStarter.getEra()), homeStarter),
                pstat("Starter FIP (home)", homeStarter.getFip(), fmt2(homeStarter.getFip()), homeStarter),
                stat("Team BABIP (away)", away == null ? null : away.getTeamBabip(),
                        away == null ? "Unavailable" : fmt3(away.getTeamBabip()), away),
                stat("Team BABIP (home)", home == null ? null : home.getTeamBabip(),
                        home == null ? "Unavailable" : fmt3(home.getTeamBabip()), home));

        String flagged = awayGap < -0.60 ? "The away starter's ERA is running well under his FIP, which suggests he has been getting some help."
                : homeGap < -0.60 ? "The home starter's ERA is running well under his FIP, which suggests he has been getting some help."
                : "Neither starter's ERA is far out of line with his fielding-independent numbers.";
        return new CategoryScore("regression", "Regression Risk", W_REGRESSION, W_REGRESSION, e,
                fmtSigned(awayGap) + " ERA-FIP", fmtSigned(homeGap) + " ERA-FIP",
                true, "Medium", flagged, sup);
    }

    private CategoryScore liveGameState(Optional<LiveWinProbability> liveWp) {
        if (liveWp.isEmpty()) {
            return CategoryScore.unavailable("live", "Live Game State", W_LIVE,
                    "The game has not started, so score, inning and base state carry no weight yet.");
        }
        LiveWinProbability wp = liveWp.get();
        // Turn MLB's live probability into the same -1..+1 edge the other categories use.
        double e = clamp((wp.homePct() - 50) / 50.0);
        List<StatValue> sup = List.of(
                StatValue.of("Live win probability (home)", (double) wp.homePct(),
                        wp.homePct() + "%", "MLB Stats API", LocalDate.now()),
                StatValue.of("Live win probability (away)", (double) wp.awayPct(),
                        wp.awayPct() + "%", "MLB Stats API", LocalDate.now()),
                StatValue.of("Leverage index", wp.leverageIndex(),
                        wp.leverageIndex() == null ? "Unavailable"
                                : String.format("%.2f", wp.leverageIndex()),
                        "MLB Stats API", LocalDate.now()),
                StatValue.of("Plays scored", (double) wp.plays(), String.valueOf(wp.plays()),
                        "MLB Stats API", LocalDate.now()));

        String exp = "The game is under way, so the score, inning, outs and base state are"
                + " doing most of the work. MLB's per-play model has the home club at "
                + wp.homePct() + "%."
                + (wp.isHighLeverage() ? " This is a high-leverage spot, so the number can move sharply." : "");
        return new CategoryScore("live", "Live Game State", W_LIVE, W_LIVE, e,
                wp.awayPct() + "%", wp.homePct() + "%", true, "High", exp, sup);
    }

    // ---- narrative ----------------------------------------------------------------

    private String mainReason(List<CategoryScore> top, String awayTeam, String homeTeam,
                              int awayPct, int homePct) {
        if (top.isEmpty()) {
            return "There is not enough data on file to separate these two clubs tonight.";
        }
        String leader = homePct >= awayPct ? homeTeam : awayTeam;
        StringBuilder sb = new StringBuilder();
        // Every MLB club name is a plural noun, so "have" is always right here.
        sb.append("The ").append(leader).append(" have the edge at ")
          .append(Math.max(awayPct, homePct)).append("%. ");

        List<String> forLeader = new ArrayList<>();
        List<String> against = new ArrayList<>();
        boolean leaderIsHome = homePct >= awayPct;
        for (CategoryScore c : top) {
            boolean favoursLeader = leaderIsHome ? c.edge() > 0 : c.edge() < 0;
            String phrase = noun(c.key());
            if (Math.abs(c.edge()) < 0.04) continue;
            if (favoursLeader) forLeader.add(phrase); else against.add(phrase);
        }
        if (!forLeader.isEmpty()) {
            sb.append("The biggest reasons are ").append(joinWords(forLeader)).append(". ");
        }
        if (!against.isEmpty()) {
            String other = leaderIsHome ? awayTeam : homeTeam;
            sb.append("The ").append(other).append(" push back on ")
              .append(joinWords(against)).append(". ");
        }
        if (forLeader.isEmpty() && against.isEmpty()) {
            sb.append("The two clubs grade out close to level across the board.");
        }
        return sb.toString().trim();
    }

    private String buildLiveNote(LiveWinProbability wp, int pregameHome, int blendedHome) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pre-game the model had the home club at ").append(pregameHome)
          .append("%. With the game under way it is ").append(blendedHome).append("%.");
        if (wp.lastPlay() != null && !wp.lastPlay().isBlank()) {
            sb.append(" Last play: ").append(wp.lastPlay());
        }
        if (wp.isBigSwing()) {
            sb.append(" That was a big swing.");
        }
        return sb.toString();
    }

    private List<String> strengths(List<CategoryScore> cats, boolean home, String team) {
        List<String> out = new ArrayList<>();
        for (CategoryScore c : cats) {
            if (!c.available()) continue;
            boolean favours = home ? c.edge() > 0.10 : c.edge() < -0.10;
            if (favours) out.add(c.label() + " (" + (home ? c.homeDisplay() : c.awayDisplay()) + ")");
        }
        if (out.isEmpty()) out.add("No category stands out as a clear strength.");
        return out;
    }

    private List<String> weaknesses(List<CategoryScore> cats, boolean home, String team) {
        List<String> out = new ArrayList<>();
        for (CategoryScore c : cats) {
            if (!c.available()) continue;
            boolean against = home ? c.edge() < -0.10 : c.edge() > 0.10;
            if (against) out.add(c.label() + " (" + (home ? c.homeDisplay() : c.awayDisplay()) + ")");
        }
        if (out.isEmpty()) out.add("No category stands out as a clear weakness.");
        return out;
    }

    // ---- confidence ---------------------------------------------------------------

    private String confidenceFor(int available, int total, PitcherStatSnapshot a,
                                 PitcherStatSnapshot h, boolean live) {
        double coverage = (double) available / total;
        boolean startersKnown = a != null && h != null;
        if (live) return coverage >= 0.55 ? "High" : "Medium";
        if (coverage >= 0.70 && startersKnown) return "High";
        if (coverage >= 0.50) return "Medium";
        return "Low";
    }

    private String confidenceReason(int available, int total, PitcherStatSnapshot a,
                                    PitcherStatSnapshot h, boolean live) {
        StringBuilder sb = new StringBuilder();
        sb.append(available).append(" of ").append(total)
          .append(" categories could be scored from the data on file.");
        if (a == null || h == null) {
            sb.append(" One or both starters are unconfirmed, which lowers confidence.");
        }
        if (live) {
            sb.append(" The game is live, so the estimate leans on the actual game state.");
        }
        int missing = total - available;
        if (missing > 0) {
            sb.append(" The ").append(missing)
              .append(" unscored categories are listed openly rather than guessed at, and their"
                    + " weight is shared across the categories that could be measured.");
        }
        return sb.toString();
    }

    // ---- helpers ------------------------------------------------------------------

    /** Difference between two values, scaled by what counts as a meaningful gap, clamped. */
    private double norm(Double favourHome, Double favourAway, double meaningfulGap) {
        if (favourHome == null || favourAway == null || meaningfulGap == 0) return 0;
        return clamp((favourHome - favourAway) / meaningfulGap);
    }

    private double clamp(double v) { return Math.max(-1.0, Math.min(1.0, v)); }

    private double idx(Integer factor) { return factor == null ? 1.0 : factor / 100.0; }

    private double pct(Integer w, Integer l) {
        if (w == null || l == null || w + l == 0) return 0.5;
        return (double) w / (w + l);
    }

    /** @param noun what the category is about, as a bare noun phrase to follow "edge in" */
    private String describe(double edge, String noun, String numbers) {
        double m = Math.abs(edge);
        if (m < 0.04) {
            return "Neither club has a meaningful edge in " + noun + " (" + numbers + ").";
        }
        String who = edge > 0 ? "The home club has " : "The away club has ";
        String strength = m < 0.18 ? "a slight " : m < 0.40 ? "a clear " : "a strong ";
        return who + strength + "edge in " + noun + " (" + numbers + ").";
    }

    /** The category as a noun that reads naturally inside a sentence. */
    private String noun(String key) {
        return switch (key) {
            case "offense"    -> "offence";
            case "starter"    -> "starting pitching";
            case "bullpen"    -> "the bullpen";
            case "lineup"     -> "platoon matchups";
            case "defense"    -> "defence";
            case "recent"     -> "recent form";
            case "park"       -> "how this park suits them";
            case "regression" -> "how sustainable their pitching has been";
            case "historical" -> "this season's results";
            case "live"       -> "the state of the game";
            default           -> "this category";
        };
    }

    private String joinWords(List<String> items) {
        if (items.size() == 1) return items.get(0);
        if (items.size() == 2) return items.get(0) + " and " + items.get(1);
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    private String teamLabel(Game game, boolean away) {
        boolean soxAway = "Away".equalsIgnoreCase(game.getHomeAway());
        if (away) return soxAway ? "Red Sox" : game.getOpponent();
        return soxAway ? game.getOpponent() : "Red Sox";
    }

    private StatValue stat(String label, Double v, String display, TeamStatSnapshot s) {
        if (v == null && (display == null || display.isBlank())) return StatValue.unavailable(label);
        return StatValue.of(label, v, display,
                s == null ? null : s.getSourceName(), s == null ? null : s.getSourceLastUpdated());
    }

    private StatValue pstat(String label, Double v, String display, PitcherStatSnapshot s) {
        if (v == null) return StatValue.unavailable(label);
        return StatValue.of(label, v, display,
                s == null ? null : s.getSourceName(), s == null ? null : s.getSourceLastUpdated());
    }

    private StatValue pkstat(String label, Double v, String display, BallparkFactorSnapshot s) {
        if (v == null) return StatValue.unavailable(label);
        return StatValue.of(label, v, display,
                s == null ? null : s.getSourceName(), s == null ? null : s.getSourceLastUpdated());
    }

    private Double dbl(Integer i) { return i == null ? null : i.doubleValue(); }
    private String str(Integer i) { return i == null ? "Unavailable" : String.valueOf(i); }
    private String signed(Integer i) { return i == null ? "Unavailable" : (i > 0 ? "+" + i : String.valueOf(i)); }
    private String fmt0(Double d) { return d == null ? "Unavailable" : String.format("%.0f", d); }
    private String fmt1(Double d) { return d == null ? "Unavailable" : String.format("%.1f", d); }
    private String fmt2(Double d) { return d == null ? "Unavailable" : String.format("%.2f", d); }
    /**
     * Three-decimal rate stats, written the way baseball writes them. OPS, ISO, BABIP and
     * defensive efficiency all drop the leading zero, so ".768" rather than "0.768".
     */
    private String fmt3(Double d) {
        if (d == null) return "Unavailable";
        String s = String.format("%.3f", d);
        if (s.startsWith("0.")) return s.substring(1);
        if (s.startsWith("-0.")) return "-" + s.substring(2);
        return s;
    }
    private String fmtSigned(double d) { return (d > 0 ? "+" : "") + String.format("%.2f", d); }
    private String fmtSignedInt(int i) { return (i > 0 ? "+" : "") + i; }
}
