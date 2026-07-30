package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.Game;
import com.example.redsoxtracker.dto.ScoreboardView;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

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

        String status = root.path("gameData").path("status").path("detailedState").asText(game.getStatus());
        boolean finalGame = "Final".equalsIgnoreCase(status) || "Game Over".equalsIgnoreCase(status);
        if (finalGame) {
            view.setBalls(0);
            view.setStrikes(0);
            view.setOuts(0);
            view.setAtBat("Final");
            view.setInningState("Final");
        } else {
            view.setBalls(intOrNull(linescore, "balls"));
            view.setStrikes(intOrNull(linescore, "strikes"));
            view.setOuts(intOrNull(linescore, "outs"));
            view.setAtBat(linescore.path("offense").path("batter").path("fullName").asText("Awaiting batter"));
            String half = linescore.path("inningHalf").asText("");
            int inning = linescore.path("currentInning").asInt(0);
            view.setInningState((half.isBlank() || inning == 0) ? status : half + " " + inning);
        }
        return Optional.of(view);
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
        view.setBalls(0);
        view.setStrikes(0);
        view.setOuts(0);
        view.setAtBat("Awaiting live feed");
        view.setInningState(game.getStatus());
        return view;
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
