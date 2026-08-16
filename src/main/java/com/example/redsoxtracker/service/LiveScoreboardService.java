package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.ScoreboardView;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class LiveScoreboardService {

    private final MlbApiService api;

    public LiveScoreboardService(MlbApiService api) {
        this.api = api;
    }

    public Optional<ScoreboardView> buildForGame(Game game) {
        if (game == null || game.getMlbGameId() == null) return Optional.empty();
        JsonNode root = api.fetchGameFeed(game.getMlbGameId());
        JsonNode linescore = root.path("liveData").path("linescore");
        if (linescore.isMissingNode() || linescore.isEmpty()) return Optional.of(fallbackBoard(game));

        ScoreboardView view = new ScoreboardView();
        boolean redSoxAway = "Away".equalsIgnoreCase(game.getHomeAway());
        view.setAwayName(redSoxAway ? "BOSTON" : shortName(game.getOpponent()));
        view.setHomeName(redSoxAway ? shortName(game.getOpponent()) : "BOSTON");

        JsonNode mlbAway = linescore.path("teams").path("away");
        JsonNode mlbHome = linescore.path("teams").path("home");
        view.setAwayRuns(intOrNull(mlbAway, "runs"));
        view.setAwayHits(intOrNull(mlbAway, "hits"));
        view.setAwayErrors(intOrNull(mlbAway, "errors"));
        view.setHomeRuns(intOrNull(mlbHome, "runs"));
        view.setHomeHits(intOrNull(mlbHome, "hits"));
        view.setHomeErrors(intOrNull(mlbHome, "errors"));

        for (JsonNode inning : linescore.path("innings")) {
            view.getInnings().add(new ScoreboardView.InningLine(
                    inning.path("num").asInt(view.getInnings().size() + 1),
                    intOrNull(inning.path("away"), "runs"),
                    intOrNull(inning.path("home"), "runs")));
        }
        while (view.getInnings().size() < 10) {
            view.getInnings().add(new ScoreboardView.InningLine(view.getInnings().size() + 1, null, null));
        }

        String feedStatus = root.path("gameData").path("status").path("detailedState").asText(game.getStatus());
        String abstractState = root.path("gameData").path("status").path("abstractGameState").asText("");
        boolean finalGame = "Final".equalsIgnoreCase(abstractState) || isFinalStatus(feedStatus);
        String status = finalGame ? "Final" : feedStatus;
        boolean delayed = !finalGame && status != null && status.toLowerCase(Locale.ROOT).contains("delay");
        boolean live = !finalGame && !delayed && ("Live".equalsIgnoreCase(abstractState)
                || "In Progress".equalsIgnoreCase(status));

        // Drives the bulb panel: Middle/End mean the side was retired, and the last
        // completed at-bat tells the page whether to flash WALK or STRIKE.
        view.setStatus(status);
        view.setDelayed(delayed);
        view.setFinalGame(finalGame);
        view.setLive(live);
        view.setInningPhase(linescore.path("inningState").asText(null));
        view.setInningOrdinal(linescore.path("currentInningOrdinal").asText(null));
        JsonNode plays = root.path("liveData").path("plays");
        applyLastPlay(view, plays.path("allPlays"), redSoxAway);
        applyRecentPitches(view, plays);
        applyChallenges(view, root.path("gameData").path("absChallenges"));

        if (finalGame) {
            view.setBalls(0);
            view.setStrikes(0);
            view.setOuts(0);
            view.setAtBat("Final");
            view.setPitching("Final");
            view.setInningState("Final");
        } else if (!live) {
            view.setBalls(0);
            view.setStrikes(0);
            view.setOuts(0);
            view.setAtBat(delayed ? "Delayed" : "Awaiting batter");
            view.setPitching(delayed ? "Delayed" : "Awaiting pitcher");
            view.setInningState(delayed ? "DELAY" : status);
        } else {
            view.setBalls(intOrNull(linescore, "balls"));
            view.setStrikes(intOrNull(linescore, "strikes"));
            view.setOuts(intOrNull(linescore, "outs"));
            view.setAtBat(linescore.path("offense").path("batter").path("fullName").asText("Awaiting batter"));
            // defense.pitcher is whoever is on the mound now; offense.pitcher is the other side's starter.
            view.setPitching(linescore.path("defense").path("pitcher").path("fullName").asText("Awaiting pitcher"));
            // A base only appears under offense while someone is standing on it.
            JsonNode offense = linescore.path("offense");
            view.setOnFirst(offense.has("first"));
            view.setOnSecond(offense.has("second"));
            view.setOnThird(offense.has("third"));
            String half = linescore.path("inningHalf").asText("");
            int inning = linescore.path("currentInning").asInt(0);
            view.setInningState((half.isBlank() || inning == 0) ? status : half + " " + inning);
            // Only the frame in progress lights up on the board; everything else reads white.
            if (inning > 0) {
                view.setCurrentInning(inning);
                view.setInningHalf(half);
            }
        }
        return Optional.of(view);
    }

    /**
     * The most recently completed at-bat. The count resets the instant a walk or a
     * strikeout lands, so the bulbs alone can never show that it happened; the page
     * flashes the word off the back of this instead.
     */
    private void applyLastPlay(ScoreboardView view, JsonNode allPlays, boolean redSoxAway) {
        if (!allPlays.isArray()) return;
        for (int i = allPlays.size() - 1; i >= 0; i--) {
            JsonNode play = allPlays.get(i);
            if (!play.path("about").path("isComplete").asBoolean(false)) continue;
            JsonNode result = play.path("result");
            JsonNode about = play.path("about");
            view.setLastPlayEvent(result.path("eventType").asText(null));
            view.setLastPlayLabel(result.path("event").asText(null));
            view.setLastPlayDescription(result.path("description").asText(null));
            view.setLastPlayBatter(play.path("matchup").path("batter").path("fullName").asText(null));
            view.setLastPlayIndex(about.path("atBatIndex").asInt());

            String half = about.path("halfInning").asText("");
            boolean topHalf = half.equalsIgnoreCase("top");
            boolean bottomHalf = half.equalsIgnoreCase("bottom");
            view.setLastPlayByRedSox((redSoxAway && topHalf) || (!redSoxAway && bottomHalf));
            return;
        }
    }

    /**
     * Ordinary pitches from the current at-bat. The final pitch of a walk, strikeout or
     * ball put in play is excluded because the completed-play notification owns it.
     */
    private void applyRecentPitches(ScoreboardView view, JsonNode plays) {
        JsonNode play = plays.path("currentPlay");
        if (play.isMissingNode() || play.isEmpty()) {
            JsonNode allPlays = plays.path("allPlays");
            if (!allPlays.isArray() || allPlays.isEmpty()) return;
            play = allPlays.get(allPlays.size() - 1);
        }

        int atBatIndex = play.path("about").path("atBatIndex").asInt(-1);
        JsonNode events = play.path("playEvents");
        if (!events.isArray()) return;

        for (int i = 0; i < events.size(); i++) {
            JsonNode event = events.get(i);
            if (!event.path("isPitch").asBoolean(false)) continue;

            JsonNode details = event.path("details");
            JsonNode count = event.path("count");
            int balls = count.path("balls").asInt(0);
            int strikes = count.path("strikes").asInt(0);
            if (details.path("isInPlay").asBoolean(false) || balls >= 4 || strikes >= 3) continue;

            String call = details.path("call").path("description").asText("").trim();
            if (call.isBlank()) continue;

            String id = event.path("playId").asText("").trim();
            if (id.isBlank()) {
                int eventIndex = event.path("index").asInt(i);
                id = atBatIndex + ":" + eventIndex;
            }

            JsonNode startSpeed = event.path("pitchData").path("startSpeed");
            String speed = startSpeed.isNumber()
                    ? String.format(Locale.US, "%.1f mph", startSpeed.asDouble())
                    : "Speed unavailable";
            String pitchType = details.path("type").path("description").asText("").trim();
            if (pitchType.isBlank()) pitchType = "Pitch type unavailable";

            view.getRecentPitches().add(new ScoreboardView.PitchNotification(
                    id, atBatIndex, balls + " - " + strikes, speed, pitchType, call));
        }
    }

    /** Automated ball-strike challenges, which a club keeps when its challenge succeeds. */
    private void applyChallenges(ScoreboardView view, JsonNode abs) {
        if (abs.isMissingNode() || !abs.path("hasChallenges").asBoolean(false)) return;
        view.setHasChallenges(true);
        view.setAwayChallengesLeft(intOrNull(abs.path("away"), "remaining"));
        view.setAwayChallengesLost(intOrNull(abs.path("away"), "usedFailed"));
        view.setHomeChallengesLeft(intOrNull(abs.path("home"), "remaining"));
        view.setHomeChallengesLost(intOrNull(abs.path("home"), "usedFailed"));
    }

    private ScoreboardView fallbackBoard(Game game) {
        ScoreboardView view = new ScoreboardView();
        boolean redSoxAway = "Away".equalsIgnoreCase(game.getHomeAway());
        view.setAwayName(redSoxAway ? "BOSTON" : shortName(game.getOpponent()));
        view.setHomeName(redSoxAway ? shortName(game.getOpponent()) : "BOSTON");
        view.setAwayRuns(redSoxAway ? game.getRedSoxScore() : game.getOpponentScore());
        view.setHomeRuns(redSoxAway ? game.getOpponentScore() : game.getRedSoxScore());
        view.setAwayHits(redSoxAway ? game.getRedSoxHits() : game.getOpponentHits());
        view.setHomeHits(redSoxAway ? game.getOpponentHits() : game.getRedSoxHits());
        view.setAwayErrors(redSoxAway ? game.getRedSoxErrors() : game.getOpponentErrors());
        view.setHomeErrors(redSoxAway ? game.getOpponentErrors() : game.getRedSoxErrors());
        for (int i = 1; i <= 10; i++) {
            view.getInnings().add(new ScoreboardView.InningLine(i, null, null));
        }
        boolean finalGame = isFinalStatus(game.getStatus());
        boolean delayed = !finalGame && game.getStatus() != null
                && game.getStatus().toLowerCase(Locale.ROOT).contains("delay");
        view.setBalls(0);
        view.setStrikes(0);
        view.setOuts(0);
        view.setAtBat(finalGame ? "Final" : "Awaiting live feed");
        view.setPitching(finalGame ? "Final" : "Awaiting live feed");
        view.setInningState(finalGame ? "Final" : game.getStatus());
        view.setStatus(finalGame ? "Final" : game.getStatus());
        view.setFinalGame(finalGame);
        view.setLive(!finalGame && "In Progress".equalsIgnoreCase(game.getStatus()));
        view.setDelayed(delayed);
        return view;
    }

    private boolean isFinalStatus(String status) {
        if (status == null) return false;
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("final")
                || normalized.startsWith("final:")
                || normalized.equals("game over")
                || normalized.contains("completed early");
    }

    private Integer intOrNull(JsonNode node, String field) {
        return node.has(field) && !node.path(field).isNull() ? node.path(field).asInt() : null;
    }

    private String shortName(String value) {
        if ("BOS".equalsIgnoreCase(value)) return "BOSTON";
        if ("Guardians".equalsIgnoreCase(value)) return "CLE";
        if ("Yankees".equalsIgnoreCase(value)) return "NYY";
        if ("Rays".equalsIgnoreCase(value)) return "TB";
        if ("Blue Jays".equalsIgnoreCase(value)) return "TOR";
        if ("Orioles".equalsIgnoreCase(value)) return "BAL";
        if ("Tigers".equalsIgnoreCase(value)) return "DET";
        if ("White Sox".equalsIgnoreCase(value)) return "CWS";
        if ("Royals".equalsIgnoreCase(value)) return "KC";
        if ("Twins".equalsIgnoreCase(value)) return "MIN";
        if ("Astros".equalsIgnoreCase(value)) return "HOU";
        if ("Rangers".equalsIgnoreCase(value)) return "TEX";
        if ("Mariners".equalsIgnoreCase(value)) return "SEA";
        if ("Angels".equalsIgnoreCase(value)) return "LAA";
        if ("Athletics".equalsIgnoreCase(value)) return "ATH";
        return value == null || value.isBlank() ? "TEAM" : value.toUpperCase();
    }
}
