package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.LiveGameDetail;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns MLB's live game feed into the play by play and box score shown under the
 * scoreboard. Everything here comes from one request, the same feed the linescore uses.
 */
@Service
public class LiveGameDetailService {

    private final MlbApiService api;

    public LiveGameDetailService(MlbApiService api) {
        this.api = api;
    }

    public Optional<LiveGameDetail> buildForGame(Game game) {
        if (game == null || game.getMlbGameId() == null) return Optional.empty();

        JsonNode root = api.fetchGameFeed(game.getMlbGameId());
        JsonNode liveData = root.path("liveData");
        if (liveData.isMissingNode() || liveData.isEmpty()) return Optional.empty();

        JsonNode linescore = liveData.path("linescore");
        JsonNode boxscore  = liveData.path("boxscore");
        String status = root.path("gameData").path("status").path("detailedState").asText(game.getStatus());
        boolean live = "In Progress".equalsIgnoreCase(status);

        return Optional.of(new LiveGameDetail(
                live,
                status,
                inningState(linescore, status, live),
                live ? linescore.path("offense").path("batter").path("fullName").asText("Awaiting batter") : status,
                live ? intOrNull(linescore, "balls") : null,
                live ? intOrNull(linescore, "strikes") : null,
                live ? intOrNull(linescore, "outs") : null,
                buildPlays(liveData.path("plays").path("allPlays"), live),
                buildTeamBox(boxscore.path("teams").path("away")),
                buildTeamBox(boxscore.path("teams").path("home")),
                buildInfo(boxscore.path("info"))
        ));
    }

    private String inningState(JsonNode linescore, String status, boolean live) {
        if (!live) return status;
        String half = linescore.path("inningHalf").asText("");
        int inning = linescore.path("currentInning").asInt(0);
        return (half.isBlank() || inning == 0) ? status : half + " " + ordinal(inning);
    }

    /**
     * Newest first, grouped under a half-inning heading the way a game log reads.
     * The at-bat in progress keeps its pitches so you can follow the count live.
     */
    private List<LiveGameDetail.HalfInning> buildPlays(JsonNode allPlays, boolean live) {
        List<LiveGameDetail.HalfInning> groups = new ArrayList<>();
        if (!allPlays.isArray()) return groups;

        String currentLabel = null;
        List<LiveGameDetail.Play> current = null;

        // Walk backwards so the most recent half-inning heads the list.
        for (int i = allPlays.size() - 1; i >= 0; i--) {
            JsonNode play = allPlays.get(i);
            JsonNode about = play.path("about");
            JsonNode result = play.path("result");

            String label = capitalise(about.path("halfInning").asText("")) + " "
                    + ordinal(about.path("inning").asInt(0));
            if (!label.equals(currentLabel)) {
                currentLabel = label;
                current = new ArrayList<>();
                // Plays are walked newest first, so the first group is the frame in
                // progress whenever the game is still going.
                boolean isLiveFrame = live && groups.isEmpty();
                groups.add(new LiveGameDetail.HalfInning(label, isLiveFrame, current));
            }

            String event = result.path("event").asText("");
            String description = result.path("description").asText("");
            // An at-bat still in progress has no event yet; show who is up instead.
            if (event.isBlank() && !about.path("isComplete").asBoolean(false)) {
                event = "At Bat";
                description = play.path("matchup").path("batter").path("fullName").asText("") + " batting.";
            }
            if (event.isBlank() && description.isBlank()) continue;

            int outs = play.path("count").path("outs").asInt(0);
            int awayScore = result.path("awayScore").asInt(0);
            int homeScore = result.path("homeScore").asInt(0);

            current.add(new LiveGameDetail.Play(
                    play.path("matchup").path("batter").path("fullName").asText(""),
                    event,
                    description,
                    outs > 0 ? outs + (outs == 1 ? " Out" : " Outs") : "",
                    about.path("isScoringPlay").asBoolean(false),
                    awayScore + " - " + homeScore,
                    buildPitches(play.path("playEvents"))
            ));
        }
        return groups;
    }

    /** Pitch detail, newest first, so the last pitch thrown reads at the top. */
    private List<LiveGameDetail.Pitch> buildPitches(JsonNode playEvents) {
        List<LiveGameDetail.Pitch> pitches = new ArrayList<>();
        if (!playEvents.isArray()) return pitches;

        for (int i = playEvents.size() - 1; i >= 0; i--) {
            JsonNode e = playEvents.get(i);
            if (!e.path("isPitch").asBoolean(false)) continue;

            JsonNode details = e.path("details");
            JsonNode count = e.path("count");
            double speed = e.path("pitchData").path("startSpeed").asDouble(0);

            pitches.add(new LiveGameDetail.Pitch(
                    count.path("balls").asInt(0) + " - " + count.path("strikes").asInt(0),
                    speed > 0 ? String.format("%.1f mph", speed) : "",
                    details.path("type").path("description").asText(""),
                    details.path("description").asText(""),
                    details.path("isInPlay").asBoolean(false)
            ));
        }
        return pitches;
    }

    private LiveGameDetail.TeamBox buildTeamBox(JsonNode team) {
        JsonNode players = team.path("players");

        List<LiveGameDetail.BatterLine> batters = new ArrayList<>();
        for (JsonNode idNode : team.path("batters")) {
            JsonNode p = players.path("ID" + idNode.asInt());
            if (p.isMissingNode()) continue;
            JsonNode batting = p.path("stats").path("batting");
            if (batting.isEmpty()) continue;
            JsonNode season = p.path("seasonStats").path("batting");

            // A batting order ending in anything but 00 came off the bench.
            int order = p.path("battingOrder").asInt(0);
            batters.add(new LiveGameDetail.BatterLine(
                    p.path("person").path("fullName").asText(""),
                    p.path("position").path("abbreviation").asText(""),
                    str(batting, "atBats"), str(batting, "runs"), str(batting, "hits"),
                    str(batting, "rbi"), str(batting, "baseOnBalls"), str(batting, "strikeOuts"),
                    season.path("avg").asText(".000"), season.path("ops").asText(".000"),
                    order != 0 && order % 100 != 0
            ));
        }

        List<LiveGameDetail.PitcherLine> pitchers = new ArrayList<>();
        for (JsonNode idNode : team.path("pitchers")) {
            JsonNode p = players.path("ID" + idNode.asInt());
            if (p.isMissingNode()) continue;
            JsonNode pitching = p.path("stats").path("pitching");
            if (pitching.isEmpty()) continue;
            JsonNode season = p.path("seasonStats").path("pitching");

            pitchers.add(new LiveGameDetail.PitcherLine(
                    p.path("person").path("fullName").asText(""),
                    pitching.path("inningsPitched").asText("0.0"),
                    str(pitching, "hits"), str(pitching, "runs"), str(pitching, "earnedRuns"),
                    str(pitching, "baseOnBalls"), str(pitching, "strikeOuts"), str(pitching, "homeRuns"),
                    season.path("era").asText("-")
            ));
        }

        JsonNode teamBatting = team.path("teamStats").path("batting");
        JsonNode teamPitching = team.path("teamStats").path("pitching");

        return new LiveGameDetail.TeamBox(
                team.path("team").path("name").asText(""),
                batters,
                new LiveGameDetail.BatterLine("Totals", "",
                        str(teamBatting, "atBats"), str(teamBatting, "runs"), str(teamBatting, "hits"),
                        str(teamBatting, "rbi"), str(teamBatting, "baseOnBalls"), str(teamBatting, "strikeOuts"),
                        "", "", false),
                pitchers,
                new LiveGameDetail.PitcherLine("Totals",
                        teamPitching.path("inningsPitched").asText("0.0"),
                        str(teamPitching, "hits"), str(teamPitching, "runs"), str(teamPitching, "earnedRuns"),
                        str(teamPitching, "baseOnBalls"), str(teamPitching, "strikeOuts"),
                        str(teamPitching, "homeRuns"), ""),
                buildInfo(team.path("info"))
        );
    }

    /**
     * MLB already formats these footer lines, so they are passed through as written.
     * The per-team box notes arrive as grouped entries with a title and field values.
     */
    private List<LiveGameDetail.InfoLine> buildInfo(JsonNode info) {
        List<LiveGameDetail.InfoLine> lines = new ArrayList<>();
        if (!info.isArray()) return lines;
        for (JsonNode entry : info) {
            if (entry.has("fieldList")) {
                for (JsonNode field : entry.path("fieldList")) {
                    lines.add(new LiveGameDetail.InfoLine(
                            field.path("label").asText(""), field.path("value").asText("")));
                }
            } else {
                String label = entry.path("label").asText("");
                String value = entry.path("value").asText("");
                if (!label.isBlank() || !value.isBlank()) {
                    lines.add(new LiveGameDetail.InfoLine(label, value));
                }
            }
        }
        return lines;
    }

    private String str(JsonNode node, String field) {
        return node.has(field) ? node.path(field).asText("0") : "0";
    }

    private Integer intOrNull(JsonNode node, String field) {
        return node.has(field) && !node.path(field).isNull() ? node.path(field).asInt() : null;
    }

    private String capitalise(String value) {
        if (value == null || value.isBlank()) return "";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String ordinal(int n) {
        if (n <= 0) return String.valueOf(n);
        if (n % 100 >= 11 && n % 100 <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
