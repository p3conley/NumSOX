package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.ScoreboardView;
import com.example.redsoxtracker.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Writes what the live feed says back onto the stored game.
 *
 * <p>The dashboard used to reload itself every 45 seconds through a schedule import, and
 * that import was the only thing keeping the stored row in step with a game in progress.
 * Once the reload was replaced by polling, the row went stale: the headline score sat
 * behind the linescore, the games list showed an old score, and a game that finished was
 * never marked Final, so it never rolled into Previous Game and never counted towards the
 * record. This keeps the row current from the same feed the scoreboard reads.</p>
 */
@Service
public class LiveGameSyncService {

    private static final Logger log = LoggerFactory.getLogger(LiveGameSyncService.class);

    private final GameRepository gameRepository;

    public LiveGameSyncService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Bring the stored game in line with the feed, saving only when something moved so a
     * poll every twelve seconds is not a write every twelve seconds.
     *
     * @return the game, updated in memory whether or not a save was needed
     */
    @Transactional
    public Game apply(Game game, ScoreboardView view) {
        if (game == null || view == null) return game;

        boolean redSoxAreAway = "Away".equalsIgnoreCase(game.getHomeAway());
        Integer bosScore  = redSoxAreAway ? view.getAwayRuns()   : view.getHomeRuns();
        Integer oppScore  = redSoxAreAway ? view.getHomeRuns()   : view.getAwayRuns();
        Integer bosHits   = redSoxAreAway ? view.getAwayHits()   : view.getHomeHits();
        Integer oppHits   = redSoxAreAway ? view.getHomeHits()   : view.getAwayHits();
        Integer bosErrors = redSoxAreAway ? view.getAwayErrors() : view.getHomeErrors();
        Integer oppErrors = redSoxAreAway ? view.getHomeErrors() : view.getAwayErrors();

        // The feed has nothing useful yet; leave the row alone.
        if (bosScore == null && oppScore == null) return game;

        String status = view.isFinalGame() ? "Final" : (view.isLive() ? "In Progress" : game.getStatus());

        boolean changed = false;
        changed |= set(game::getRedSoxScore,    game::setRedSoxScore,    bosScore);
        changed |= set(game::getOpponentScore,  game::setOpponentScore,  oppScore);
        changed |= set(game::getRedSoxHits,     game::setRedSoxHits,     bosHits);
        changed |= set(game::getOpponentHits,   game::setOpponentHits,   oppHits);
        changed |= set(game::getRedSoxErrors,   game::setRedSoxErrors,   bosErrors);
        changed |= set(game::getOpponentErrors, game::setOpponentErrors, oppErrors);

        boolean wasFinal = "Final".equals(game.getStatus());
        if (!Objects.equals(game.getStatus(), status)) {
            game.setStatus(status);
            changed = true;
        }

        if ("Final".equals(status) && bosScore != null && oppScore != null) {
            String result = bosScore > oppScore ? "W" : (bosScore < oppScore ? "L" : null);
            if (!Objects.equals(game.getResult(), result)) {
                game.setResult(result);
                changed = true;
            }
            // Stamp the moment we watch it end, which is what holds it on the dashboard
            // for an hour before the next matchup takes over.
            if (!wasFinal && game.getFinalizedAt() == null) {
                game.setFinalizedAt(LocalDateTime.now());
                changed = true;
                log.info("Game {} went final from the live feed: {}-{}",
                        game.getMlbGameId(), bosScore, oppScore);
            }
        }

        if (changed) gameRepository.save(game);
        return game;
    }

    private <T> boolean set(java.util.function.Supplier<T> getter,
                            java.util.function.Consumer<T> setter, T value) {
        if (value == null || Objects.equals(getter.get(), value)) return false;
        setter.accept(value);
        return true;
    }
}
