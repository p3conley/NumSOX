package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.BallparkFactorSnapshot;
import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.PitcherStatSnapshot;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.LiveWinProbability;
import com.example.redsoxtracker.dto.NumsoxModel;
import com.example.redsoxtracker.dto.ScoreboardView;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Single entry point for every NumSOX win-probability calculation.
 *
 * <p>The expanded model implementation stays isolated in {@link NumsoxModelService},
 * but controllers and page services must come through this class. That keeps the
 * dashboard, Matchup Center and live endpoint on one probability and one explanation.</p>
 */
@Service
public class WinProbabilityService {

    private final NumsoxModelService expandedModel;

    public WinProbabilityService(HistoricalCalibrationService calibrationService) {
        this.expandedModel = new NumsoxModelService(calibrationService);
    }

    public NumsoxModel calculate(
            Game game,
            TeamStatSnapshot away,
            TeamStatSnapshot home,
            PitcherStatSnapshot awayStarter,
            PitcherStatSnapshot homeStarter,
            BallparkFactorSnapshot park,
            Optional<LiveWinProbability> liveWinProbability,
            Optional<ScoreboardView> liveGameState) {

        return expandedModel.evaluate(game, away, home, awayStarter, homeStarter, park,
                liveWinProbability, liveGameState);
    }
}
