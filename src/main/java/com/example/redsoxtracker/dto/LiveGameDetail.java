package com.example.redsoxtracker.dto;

import java.util.List;

/**
 * Everything the dashboard shows about one game beyond the linescore: the play by play,
 * both box scores, and the footer of umpires and weather.
 *
 * <p>Sent to the browser as JSON and re-rendered on each poll, so a live game updates
 * without the page reloading.</p>
 */
public record LiveGameDetail(
        boolean live,
        String status,
        String inningState,
        String atBat,
        Integer balls,
        Integer strikes,
        Integer outs,
        List<HalfInning> halfInnings,
        TeamBox away,
        TeamBox home,
        List<InfoLine> info
) {

    /** Plays are grouped under their half-inning heading, newest first. */
    public record HalfInning(String label, List<Play> plays) {}

    public record Play(
            String batter,
            String event,
            String description,
            String outs,
            boolean scoringPlay,
            String scoreline,
            List<Pitch> pitches
    ) {}

    /** One pitch: the count it was thrown on, how hard, what it was, and the call. */
    public record Pitch(String count, String speed, String type, String call, boolean inPlay) {}

    public record TeamBox(
            String teamName,
            List<BatterLine> batters,
            BatterLine batterTotals,
            List<PitcherLine> pitchers,
            PitcherLine pitcherTotals,
            List<InfoLine> notes
    ) {}

    public record BatterLine(
            String name,
            String position,
            String ab, String r, String h, String rbi, String bb, String so,
            String avg, String ops,
            boolean substitute
    ) {}

    public record PitcherLine(
            String name,
            String ip, String h, String r, String er, String bb, String so, String hr,
            String era
    ) {}

    public record InfoLine(String label, String value) {}
}
