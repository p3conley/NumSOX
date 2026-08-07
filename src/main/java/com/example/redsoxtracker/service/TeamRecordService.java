package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.domain.TeamStatSnapshot;
import com.example.redsoxtracker.dto.TeamRecord;
import com.example.redsoxtracker.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Keeps the Red Sox record, last-10 and streak honest between standings pulls.
 *
 * <p>MLB's standings feed can trail a finished game by a while. The app already knows the
 * outcome of every game it has marked Final, so whenever the local game log accounts for
 * more decided games than the standings snapshot does, the log wins.</p>
 */
@Service
public class TeamRecordService {

    private final GameRepository gameRepository;

    public TeamRecordService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /** The Red Sox record, preferring the local game log whenever the feed has fallen behind. */
    public TeamRecord bosRecord(TeamStatSnapshot snapshot) {
        TeamRecord fromLog = fromGameLog();
        if (snapshot == null) return fromLog;

        TeamRecord fromFeed = new TeamRecord(
                snapshot.getWins(),
                snapshot.getLosses(),
                snapshot.getLast10Record(),
                snapshot.getCurrentStreak(),
                false);

        if (snapshot.getWins() == null || snapshot.getLosses() == null) return fromLog;
        return fromLog.getGamesPlayed() > fromFeed.getGamesPlayed() ? fromLog : fromFeed;
    }

    /** Convenience overload for callers that only have the game log to work from. */
    public TeamRecord bosRecord() {
        return fromGameLog();
    }

    private TeamRecord fromGameLog() {
        List<Game> decided = gameRepository.findAllByOrderByGameDateAsc().stream()
                .filter(g -> "Final".equals(g.getStatus()))
                .filter(g -> "W".equals(g.getResult()) || "L".equals(g.getResult()))
                .toList();

        int wins = 0;
        int losses = 0;
        for (Game g : decided) {
            if ("W".equals(g.getResult())) wins++;
            else losses++;
        }

        return new TeamRecord(wins, losses, last10(decided), streak(decided), true);
    }

    private String last10(List<Game> decided) {
        if (decided.isEmpty()) return null;
        List<Game> window = decided.subList(Math.max(0, decided.size() - 10), decided.size());
        int wins = (int) window.stream().filter(g -> "W".equals(g.getResult())).count();
        return wins + "-" + (window.size() - wins);
    }

    private String streak(List<Game> decided) {
        if (decided.isEmpty()) return null;
        String latest = decided.get(decided.size() - 1).getResult();
        int run = 0;
        for (int i = decided.size() - 1; i >= 0; i--) {
            if (!latest.equals(decided.get(i).getResult())) break;
            run++;
        }
        return latest + run;
    }
}
