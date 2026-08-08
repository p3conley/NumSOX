package com.example.redsoxtracker.controller;

import com.example.redsoxtracker.dto.DivisionStandings;
import com.example.redsoxtracker.dto.LeagueStandings;
import com.example.redsoxtracker.dto.StandingsRow;
import com.example.redsoxtracker.service.HistoricalStandingsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Feeds the standings timelapse. The whole season is sent in one response so playback
 * never waits on the network mid-animation, and it is only fetched when the user
 * actually starts the timelapse.
 */
@RestController
public class StandingsApiController {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final HistoricalStandingsService historicalStandingsService;

    public StandingsApiController(HistoricalStandingsService historicalStandingsService) {
        this.historicalStandingsService = historicalStandingsService;
    }

    @GetMapping("/api/standings-series")
    public ResponseEntity<Map<String, Object>> series() {
        List<Map<String, Object>> frames = new ArrayList<>();
        for (HistoricalStandingsService.SeasonFrame frame : historicalStandingsService.series()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("date", frame.date().toString());
            f.put("label", frame.date().format(LABEL));
            f.put("rows", flatten(frame.standings()));
            frames.add(f);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("frames", frames);

        // The season only grows by a day at a time, so a short browser cache is plenty.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
                .body(body);
    }

    /**
     * Rows keyed by team name, since the browser updates the table that Thymeleaf already
     * rendered rather than rebuilding it.
     */
    private List<Map<String, Object>> flatten(List<LeagueStandings> standings) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LeagueStandings league : standings) {
            for (DivisionStandings division : league.getDivisions()) {
                int order = 0;
                for (StandingsRow r : division.getRows()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("team", r.getTeamName());
                    m.put("division", league.getLeague() + " " + division.getName());
                    m.put("order", order++);
                    m.put("w", r.getWins());
                    m.put("l", r.getLosses());
                    m.put("pct", r.getPct());
                    m.put("gb", r.getGamesBack());
                    m.put("wcgb", r.getWildCardGamesBack());
                    m.put("l10", r.getLast10());
                    m.put("strk", r.getStreak());
                    m.put("rs", r.getRunsScored());
                    m.put("ra", r.getRunsAllowed());
                    m.put("diff", r.getRunDifferential());
                    m.put("home", r.getHomeRecord());
                    m.put("away", r.getAwayRecord());
                    m.put("xwl", r.getExpectedRecord());
                    m.put("tagLabel", r.getTagLabel());
                    m.put("tagClass", r.getTagClass());
                    m.put("hasTag", r.hasTag());
                    rows.add(m);
                }
            }
        }
        return rows;
    }
}
