package com.example.redsoxtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class MlbApiService {

    private static final Logger log = LoggerFactory.getLogger(MlbApiService.class);
    private static final String BASE = "https://statsapi.mlb.com/api/v1";
    private static final String LIVE_BASE = "https://statsapi.mlb.com/api/v1.1";
    private static final int BOS_TEAM_ID = 111;

    private final RestTemplate rest;
    private final ObjectMapper mapper;

    public MlbApiService() {
        this.rest = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    public int currentSeason() {
        LocalDate now = LocalDate.now();
        // MLB regular season: roughly April–October
        return now.getMonthValue() >= 3 ? now.getYear() : now.getYear() - 1;
    }

    /** Schedule for BOS over the given date range with linescore hydration. */
    public JsonNode fetchSchedule(LocalDate from, LocalDate to) {
        String url = BASE + "/schedule?sportId=1&teamId=" + BOS_TEAM_ID
                + "&startDate=" + fmt(from)
                + "&endDate=" + fmt(to)
                + "&hydrate=team,linescore,probablePitcher";
        return get(url);
    }

    /**
     * Every club's games in the window, not just Boston's. Standings for a past date are
     * tallied from this, so no team filter and no hydration beyond the bare result.
     */
    public JsonNode fetchLeagueSchedule(LocalDate from, LocalDate to) {
        String url = BASE + "/schedule?sportId=1"
                + "&startDate=" + fmt(from)
                + "&endDate=" + fmt(to);
        return get(url);
    }

    /** AL standings (leagueId 103). */
    public JsonNode fetchAlStandings(int season) {
        String url = BASE + "/standings?leagueId=103&season=" + season;
        return get(url);
    }

    /** MLB standings across both leagues. */
    public JsonNode fetchMlbStandings(int season) {
        String url = BASE + "/standings?leagueId=103,104&season=" + season;
        return get(url);
    }

    /** Current BOS roster. */
    public JsonNode fetchBosRoster() {
        String url = BASE + "/teams/" + BOS_TEAM_ID + "/roster?rosterType=active&hydrate=person";
        return get(url);
    }

    /** Player season stats. */
    public JsonNode fetchPlayerStats(int playerId, String group, int season) {
        String url = BASE + "/people/" + playerId + "/stats?stats=season&group=" + group
                + "&season=" + season + "&sportId=1";
        return get(url);
    }

    /** Player sabermetric stats (real WAR, wRC+, xFIP, FIP-, ERA- etc.) computed by MLB itself. */
    public JsonNode fetchPlayerSabermetrics(int playerId, String group, int season) {
        String url = BASE + "/people/" + playerId + "/stats?stats=sabermetrics&group=" + group
                + "&season=" + season + "&sportId=1";
        return get(url);
    }

    /** Player fielding stats (real fielding percentage). */
    public JsonNode fetchPlayerFieldingStats(int playerId, int season) {
        String url = BASE + "/people/" + playerId + "/stats?stats=season&group=fielding&season=" + season + "&sportId=1";
        return get(url);
    }

    /** Team fielding stats (real fielding percentage). */
    public JsonNode fetchTeamFieldingStats(int teamId, int season) {
        String url = BASE + "/teams/" + teamId + "/stats?stats=season&group=fielding&season=" + season + "&sportId=1";
        return get(url);
    }

    /** Live game feed with linescore, count, batter, and box-score context. */
    public JsonNode fetchGameFeed(int gamePk) {
        String url = LIVE_BASE + "/game/" + gamePk + "/feed/live";
        return get(url);
    }

    /**
     * MLB's own win probability, one entry per completed play, carrying the running
     * home/away percentages plus the leverage index. The live feed does not include any
     * of this, so it needs its own request.
     */
    public JsonNode fetchWinProbability(int gamePk) {
        String url = BASE + "/game/" + gamePk + "/winProbability";
        return get(url);
    }

    /**
     * Team splits by opposing hand. sitCodes vl/vr are "vs Left" and "vs Right", which is
     * what a platoon edge is actually built from.
     */
    public JsonNode fetchTeamHandednessSplits(int teamId, int season, String group) {
        String url = BASE + "/teams/" + teamId + "/stats?stats=statSplits&sitCodes=vl,vr"
                + "&group=" + group + "&season=" + season + "&sportId=1";
        return get(url);
    }

    /** Team hitting stats for a given team and season. */
    public JsonNode fetchTeamHittingStats(int teamId, int season) {
        String url = BASE + "/teams/" + teamId + "/stats?stats=season&group=hitting&season=" + season + "&sportId=1";
        return get(url);
    }

    /** Team pitching stats for a given team and season. */
    public JsonNode fetchTeamPitchingStats(int teamId, int season) {
        String url = BASE + "/teams/" + teamId + "/stats?stats=season&group=pitching&season=" + season + "&sportId=1";
        return get(url);
    }

    /** Current MLB team hitting stats, used for league-context estimates. */
    public JsonNode fetchLeagueHittingStats(int season) {
        String url = BASE + "/teams/stats?stats=season&group=hitting&season=" + season + "&sportIds=1";
        return get(url);
    }

    /** Current MLB team pitching stats, used for league-context estimates. */
    public JsonNode fetchLeaguePitchingStats(int season) {
        String url = BASE + "/teams/stats?stats=season&group=pitching&season=" + season + "&sportIds=1";
        return get(url);
    }

    private JsonNode get(String url) {
        try {
            log.info("MLB API → {}", url);
            String json = rest.getForObject(url, String.class);
            return mapper.readTree(json);
        } catch (Exception e) {
            log.warn("MLB API call failed: {} - {}", url, e.getMessage());
            return mapper.createObjectNode();
        }
    }

    private String fmt(LocalDate d) {
        return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public int getBosTeamId() { return BOS_TEAM_ID; }
}
