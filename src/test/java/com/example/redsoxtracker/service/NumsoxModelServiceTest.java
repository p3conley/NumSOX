package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.CategoryScore;
import com.example.redsoxtracker.dto.LiveWinProbability;
import com.example.redsoxtracker.dto.NumsoxModel;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The model is only trustworthy if its arithmetic is. These cover the two things that would
 * make it quietly wrong: weights that do not add up, and unavailable categories that either
 * vote as "even" or take their weight with them.
 */
class NumsoxModelServiceTest {

    /** No calibration data, so that category comes back unavailable, which is what we want here. */
    private final NumsoxModelService service = new NumsoxModelService(null);

    @Test
    void specifiedWeightsSumToOneHundredPercent() {
        NumsoxModel m = service.evaluate(game(), strong(), weak(), goodStarter(), badStarter(),
                park(), Optional.empty());
        double total = m.categories().stream().mapToDouble(CategoryScore::weight).sum();
        assertEquals(1.0, total, 0.0001,
                "The 14 category weights must add to exactly 100%");
        assertEquals(14, m.categories().size());
    }

    @Test
    void unavailableCategoriesRedistributeTheirWeightInsteadOfVotingEven() {
        NumsoxModel m = service.evaluate(game(), strong(), weak(), goodStarter(), badStarter(),
                park(), Optional.empty());

        double effectiveTotal = m.scoredCategories().stream()
                .mapToDouble(CategoryScore::effectiveWeight).sum();
        assertEquals(1.0, effectiveTotal, 0.0001,
                "Scored categories must carry the full model between them");

        for (CategoryScore c : m.missingCategories()) {
            assertEquals(0.0, c.effectiveWeight(), 0.0001,
                    c.label() + " is unavailable so it must carry no weight");
            assertEquals(0.0, c.edge(), 0.0001,
                    c.label() + " is unavailable so it must not vote");
        }
        assertTrue(m.hasMissing(), "This fixture has no weather or injury feed");
        assertTrue(m.availableCount() < m.totalCount());
    }

    @Test
    void aStrongerAwayClubIsFavouredDespiteHomeField() {
        // Away club is better in every scored category.
        NumsoxModel m = service.evaluate(game(), strong(), weak(), goodStarter(), badStarter(),
                park(), Optional.empty());
        assertTrue(m.awayPct() > m.homePct(),
                "Away was better everywhere, so it should be favoured. Got "
                        + m.awayPct() + "/" + m.homePct());
        assertEquals(100, m.awayPct() + m.homePct(), "Percentages must add to 100");
    }

    @Test
    void homeFieldDecidesAnOtherwiseIdenticalMatchup() {
        NumsoxModel m = service.evaluate(game(), strong(), strong(), goodStarter(), goodStarter(),
                park(), Optional.empty());
        assertTrue(m.homePct() > m.awayPct(),
                "Identical clubs, so home field should break the tie");
        assertTrue(m.homePct() - m.awayPct() < 12,
                "Home field alone should be a small edge, not a decisive one");
    }

    @Test
    void liveGameStateTakesOverOnceTheGameIsUnderWay() {
        // Away club is far better on paper, but the home club is winning.
        LiveWinProbability wp = new LiveWinProbability(12, 88, 1.4, "Bregman flies out to right", 3.0, 140);
        NumsoxModel m = service.evaluate(game(), strong(), weak(), goodStarter(), badStarter(),
                park(), Optional.of(wp));

        assertTrue(m.live());
        assertTrue(m.homePct() > 70,
                "The home club is winning, so it should be favoured regardless of the pre-game read");
        assertNotNull(m.pregamePct(), "The pre-game figure must be kept for comparison");
        assertTrue(m.pregamePct() < 50, "Pre-game favoured the away club");
        assertNotNull(m.liveNote());
        assertTrue(m.liveNote().contains("Bregman"), "The last play belongs in the live note");
    }

    @Test
    void everyCategoryExplainsItselfInPlainEnglish() {
        NumsoxModel m = service.evaluate(game(), strong(), weak(), goodStarter(), badStarter(),
                park(), Optional.empty());
        for (CategoryScore c : m.categories()) {
            assertNotNull(c.explanation(), c.label() + " needs an explanation");
            assertFalse(c.explanation().isBlank(), c.label() + " needs an explanation");
            assertFalse(c.explanation().contains("model score"),
                    c.label() + " must not explain itself with model jargon");
            assertFalse(c.explanation().contains("—"),
                    c.label() + " must not contain an em dash");
        }
        assertFalse(m.mainReason().isBlank());
        assertFalse(m.mainReason().contains("—"));
        assertFalse(m.confidenceReason().isBlank());
    }

    @Test
    void confidenceFallsWhenTheStartersAreUnknown() {
        NumsoxModel full = service.evaluate(game(), strong(), weak(), goodStarter(), badStarter(),
                park(), Optional.empty());
        NumsoxModel thin = service.evaluate(game(), strong(), weak(), null, null,
                park(), Optional.empty());

        assertTrue(thin.availableCount() < full.availableCount(),
                "Unknown starters means fewer scored categories");
        assertNotEquals("High", thin.confidence(),
                "A model missing both starters must not claim high confidence");
        assertTrue(thin.confidenceReason().contains("starters"),
                "It should say why confidence dropped");
    }

    @Test
    void aTotallyBlindModelStillProducesSomethingHonest() {
        NumsoxModel m = service.evaluate(game(), null, null, null, null, null, Optional.empty());
        assertEquals(0, m.availableCount(), "Nothing could be scored");
        assertEquals(100, m.awayPct() + m.homePct());
        assertEquals("Low", m.confidence());
        assertTrue(m.mainReason().toLowerCase().contains("not enough data"));
    }

    // ---- fixtures -----------------------------------------------------------------

    private Game game() {
        Game g = new Game();
        g.setGameDate(LocalDate.of(2026, 8, 14));
        g.setOpponent("Blue Jays");
        g.setHomeAway("Away");   // Red Sox away, so the opponent is the home club
        g.setStatus("Scheduled");
        return g;
    }

    /** A good club on every measure the model reads. */
    private TeamStatSnapshot strong() {
        TeamStatSnapshot s = new TeamStatSnapshot();
        s.setTeamWrcPlus(118.0);
        s.setTeamOps(0.790);
        s.setRunsPerGame(5.4);
        s.setTeamHardHitRate(43.0);
        s.setTeamBarrelRate(9.5);
        s.setTeamIso(0.185);
        s.setTeamBabip(0.300);
        s.setBullpenEra(3.20);
        s.setBullpenFip(3.35);
        s.setBullpenWhip(1.14);
        s.setBullpenKRate(26.0);
        s.setOpsVsLhp(0.800);
        s.setOpsVsRhp(0.780);
        s.setDefensiveEfficiency(0.705);
        s.setOutsAboveAverage(24);
        s.setLast10Wins(7);
        s.setLast10Losses(3);
        s.setLast5RunDifferential(9);
        s.setCurrentStreak("W3");
        s.setSourceName("MLB Stats API");
        s.setSourceLastUpdated(LocalDate.of(2026, 8, 14));
        return s;
    }

    /** The same club, worse everywhere. */
    private TeamStatSnapshot weak() {
        TeamStatSnapshot s = new TeamStatSnapshot();
        s.setTeamWrcPlus(88.0);
        s.setTeamOps(0.680);
        s.setRunsPerGame(3.8);
        s.setTeamHardHitRate(36.0);
        s.setTeamBarrelRate(6.0);
        s.setTeamIso(0.130);
        s.setTeamBabip(0.285);
        s.setBullpenEra(4.90);
        s.setBullpenFip(4.75);
        s.setBullpenWhip(1.42);
        s.setBullpenKRate(19.0);
        s.setOpsVsLhp(0.670);
        s.setOpsVsRhp(0.685);
        s.setDefensiveEfficiency(0.678);
        s.setOutsAboveAverage(-18);
        s.setLast10Wins(3);
        s.setLast10Losses(7);
        s.setLast5RunDifferential(-11);
        s.setCurrentStreak("L4");
        s.setSourceName("MLB Stats API");
        s.setSourceLastUpdated(LocalDate.of(2026, 8, 14));
        return s;
    }

    private PitcherStatSnapshot goodStarter() {
        PitcherStatSnapshot p = new PitcherStatSnapshot();
        p.setEra(2.90);
        p.setFip(3.05);
        p.setWhip(1.05);
        p.setKMinusBbRate(21.0);
        p.setOpponentOps(0.660);
        p.setHrPer9(0.90);
        p.setBarrelRateAllowed(6.5);
        p.setSourceName("MLB Stats API");
        p.setSourceLastUpdated(LocalDate.of(2026, 8, 14));
        return p;
    }

    private PitcherStatSnapshot badStarter() {
        PitcherStatSnapshot p = new PitcherStatSnapshot();
        p.setEra(5.10);
        p.setFip(4.95);
        p.setWhip(1.44);
        p.setKMinusBbRate(9.0);
        p.setOpponentOps(0.790);
        p.setHrPer9(1.60);
        p.setBarrelRateAllowed(10.5);
        p.setSourceName("MLB Stats API");
        p.setSourceLastUpdated(LocalDate.of(2026, 8, 14));
        return p;
    }

    private BallparkFactorSnapshot park() {
        BallparkFactorSnapshot b = new BallparkFactorSnapshot();
        b.setVenueName("Rogers Centre");
        b.setRunParkFactor(102);
        b.setHomeRunParkFactor(106);
        b.setDoubleParkFactor(99);
        b.setSourceName("Baseball Savant");
        b.setSourceLastUpdated(LocalDate.of(2026, 8, 14));
        return b;
    }
}
