package com.example.redsoxtracker.service;

import com.example.redsoxtracker.domain.*;
import com.example.redsoxtracker.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MlbDataImportService {

    private static final Logger log = LoggerFactory.getLogger(MlbDataImportService.class);

    private final GameRepository gameRepo;
    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;
    private final TeamStatSnapshotRepository teamStatRepo;
    private final HitterStatSnapshotRepository hitterStatRepo;
    private final PitcherStatSnapshotRepository pitcherStatRepo;
    private final SyncLogRepository syncLogRepo;
    private final MlbApiService api;
    private final BaseballSavantService savant;

    // MLB team-id → short display name used in the app
    private static final Map<Integer, String> TEAM_SHORT = Map.ofEntries(
        Map.entry(111, "Red Sox"), Map.entry(147, "Yankees"), Map.entry(139, "Rays"),
        Map.entry(141, "Blue Jays"), Map.entry(110, "Orioles"), Map.entry(117, "Astros"),
        Map.entry(140, "Rangers"), Map.entry(108, "Angels"), Map.entry(133, "Athletics"),
        Map.entry(136, "Mariners"), Map.entry(116, "Tigers"), Map.entry(114, "Guardians"),
        Map.entry(145, "White Sox"), Map.entry(118, "Royals"), Map.entry(142, "Twins"),
        Map.entry(112, "Cubs"), Map.entry(113, "Reds"), Map.entry(158, "Brewers"),
        Map.entry(134, "Pirates"), Map.entry(138, "Cardinals"), Map.entry(144, "Braves"),
        Map.entry(146, "Marlins"), Map.entry(121, "Mets"), Map.entry(143, "Phillies"),
        Map.entry(120, "Nationals"), Map.entry(109, "Diamondbacks"), Map.entry(115, "Rockies"),
        Map.entry(119, "Dodgers"), Map.entry(135, "Padres"), Map.entry(137, "Giants")
    );

    public MlbDataImportService(GameRepository gameRepo, TeamRepository teamRepo,
                                PlayerRepository playerRepo,
                                TeamStatSnapshotRepository teamStatRepo,
                                HitterStatSnapshotRepository hitterStatRepo,
                                PitcherStatSnapshotRepository pitcherStatRepo,
                                SyncLogRepository syncLogRepo, MlbApiService api, BaseballSavantService savant) {
        this.gameRepo = gameRepo;
        this.teamRepo = teamRepo;
        this.playerRepo = playerRepo;
        this.teamStatRepo = teamStatRepo;
        this.hitterStatRepo = hitterStatRepo;
        this.pitcherStatRepo = pitcherStatRepo;
        this.syncLogRepo = syncLogRepo;
        this.api = api;
        this.savant = savant;
    }

    /** Import games from Opening Day through the full MLB season window. */
    @Transactional
    public int importSchedule() {
        LocalDate from = LocalDate.of(api.currentSeason(), 3, 26);
        LocalDate to   = LocalDate.of(api.currentSeason(), 10, 31);
        JsonNode root  = api.fetchSchedule(from, to);

        int count = 0;
        JsonNode dates = root.path("dates");
        for (JsonNode dateNode : dates) {
            for (JsonNode gameNode : dateNode.path("games")) {
                try {
                    if (upsertGame(gameNode)) count++;
                } catch (Exception e) {
                    log.warn("Failed to import game {}: {}", gameNode.path("gamePk").asInt(), e.getMessage());
                }
            }
        }
        log.info("Schedule import: {} games saved/updated", count);
        markSynced("schedule", "MLB Stats API", count + " games imported for " + from + " – " + to);
        return count;
    }

    private boolean upsertGame(JsonNode g) {
        int gamePk = g.path("gamePk").asInt();
        if (gamePk == 0) return false;

        JsonNode awayNode  = g.path("teams").path("away");
        JsonNode homeNode  = g.path("teams").path("home");
        int awayId = awayNode.path("team").path("id").asInt();
        int homeId = homeNode.path("team").path("id").asInt();
        int bosId  = api.getBosTeamId();

        boolean bosIsAway = (awayId == bosId);
        boolean bosIsHome = (homeId == bosId);
        if (!bosIsAway && !bosIsHome) return false;

        int oppTeamId    = bosIsAway ? homeId : awayId;
        String oppName   = TEAM_SHORT.getOrDefault(oppTeamId, g.path("teams")
                .path(bosIsAway ? "home" : "away").path("team").path("name").asText("Unknown"));
        String homeAway  = bosIsAway ? "Away" : "Home";
        String venue     = g.path("venue").path("name").asText(bosIsAway ? null : "Fenway Park");

        String officialDate = g.path("officialDate").asText("");
        if (officialDate.isEmpty()) return false;
        LocalDate gameDate = LocalDate.parse(officialDate);

        String abstractState  = g.path("status").path("abstractGameState").asText("Preview");
        String detailedState  = g.path("status").path("detailedState").asText("");
        String status = mapStatus(abstractState, detailedState);

        Integer bosSc = null, oppSc = null;
        Integer bosHits = null, oppHits = null, bosErrors = null, oppErrors = null;
        String result = null;
        JsonNode awayProbable = awayNode.path("probablePitcher");
        JsonNode homeProbable = homeNode.path("probablePitcher");

        if ("Final".equals(status) || "In Progress".equals(status)) {
            if (!awayNode.path("score").isMissingNode()) {
                int awayScore = awayNode.path("score").asInt();
                int homeScore = homeNode.path("score").asInt();
                bosSc = bosIsAway ? awayScore : homeScore;
                oppSc = bosIsAway ? homeScore : awayScore;
                if ("Final".equals(status)) {
                    boolean bosWon = bosIsAway
                            ? awayNode.path("isWinner").asBoolean(false)
                            : homeNode.path("isWinner").asBoolean(false);
                    result = bosWon ? "W" : "L";
                }
            }
        }

        JsonNode lineAway = g.path("linescore").path("teams").path("away");
        JsonNode lineHome = g.path("linescore").path("teams").path("home");
        if (!lineAway.isMissingNode() && !lineAway.isEmpty()) {
            bosHits = bosIsAway ? intOrNull(lineAway, "hits") : intOrNull(lineHome, "hits");
            oppHits = bosIsAway ? intOrNull(lineHome, "hits") : intOrNull(lineAway, "hits");
            bosErrors = bosIsAway ? intOrNull(lineAway, "errors") : intOrNull(lineHome, "errors");
            oppErrors = bosIsAway ? intOrNull(lineHome, "errors") : intOrNull(lineAway, "errors");
        }

        Game game = gameRepo.findByMlbGameId(gamePk).orElse(new Game());
        game.setMlbGameId(gamePk);
        game.setGameDate(gameDate);
        game.setOpponent(oppName);
        game.setHomeAway(homeAway);
        game.setVenue(venue);
        game.setStatus(status);
        game.setRedSoxScore(bosSc);
        game.setOpponentScore(oppSc);
        game.setRedSoxHits(bosHits);
        game.setOpponentHits(oppHits);
        game.setRedSoxErrors(bosErrors);
        game.setOpponentErrors(oppErrors);
        game.setRedSoxRecord(recordText(bosIsAway ? awayNode : homeNode));
        game.setOpponentRecord(recordText(bosIsAway ? homeNode : awayNode));
        game.setAwayProbablePitcherId(idOrNull(awayProbable));
        game.setAwayProbablePitcherName(nameOrNull(awayProbable));
        game.setHomeProbablePitcherId(idOrNull(homeProbable));
        game.setHomeProbablePitcherName(nameOrNull(homeProbable));
        game.setResult(result);
        if (game.getFavorite() == null) game.setFavorite(false);
        gameRepo.save(game);

        upsertProbablePitcher(awayProbable, awayId);
        upsertProbablePitcher(homeProbable, homeId);
        return true;
    }

    // MLB division-id → {league, division name}, stable IDs used by the standings feed.
    private static final Map<Integer, String[]> DIVISION_INFO = Map.of(
        200, new String[]{"AL", "West"}, 201, new String[]{"AL", "East"}, 202, new String[]{"AL", "Central"},
        203, new String[]{"NL", "West"}, 204, new String[]{"NL", "East"}, 205, new String[]{"NL", "Central"}
    );

    /** Import MLB standings → update TeamStatSnapshot record, rank, GB, streak for every tracked team. */
    @Transactional
    public void importStandings() {
        int season = api.currentSeason();
        JsonNode root = api.fetchMlbStandings(season);

        int count = 0;
        for (JsonNode record : root.path("records")) {
            String[] divisionInfo = DIVISION_INFO.get(record.path("division").path("id").asInt(0));

            for (JsonNode tr : record.path("teamRecords")) {
                int mlbTeamId = tr.path("team").path("id").asInt(0);
                if (mlbTeamId == 0) continue;

                Team team = teamForMlbId(mlbTeamId);
                String apiName = tr.path("team").path("name").asText(null);
                if (apiName != null && !apiName.isBlank() && team.getTeamName() != null && team.getTeamName().startsWith("Team ")) {
                    team.setTeamName(apiName);
                }
                if (divisionInfo != null) {
                    team.setLeague(divisionInfo[0]);
                    team.setDivision(divisionInfo[1]);
                }
                teamRepo.save(team);

                int wins = tr.path("wins").asInt(0);
                int losses = tr.path("losses").asInt(0);
                String streak = tr.path("streak").path("streakCode").asText(null);

                int l10w = 0, l10l = 0;
                for (JsonNode split : tr.path("records").path("splitRecords")) {
                    if ("lastTen".equals(split.path("type").asText())) {
                        l10w = split.path("wins").asInt(0);
                        l10l = split.path("losses").asInt(0);
                        break;
                    }
                }

                TeamStatSnapshot snap = teamStatRepo
                        .findTopByTeamOrderBySnapshotDateDesc(team)
                        .orElse(new TeamStatSnapshot());
                snap.setTeam(team);
                snap.setSeason(season);
                snap.setSnapshotDate(LocalDate.now());
                snap.setWins(wins);
                snap.setLosses(losses);
                snap.setCurrentStreak(streak);
                snap.setLast10Wins(l10w);
                snap.setLast10Losses(l10l);
                snap.setDivisionRank(parseInt(tr.path("divisionRank").asText(null)));
                snap.setWildCardRank(parseInt(tr.path("wildCardRank").asText(null)));
                snap.setGamesBack(tr.path("gamesBack").asText("-"));
                snap.setWildCardGamesBack(tr.path("wildCardGamesBack").asText("-"));
                snap.setDivisionLeader(tr.path("divisionLeader").asBoolean(false));
                snap.setHomeRecord(splitRecordText(tr, "home"));
                snap.setAwayRecord(splitRecordText(tr, "away"));
                snap.setExpectedRecord(expectedRecordText(tr));
                if (snap.getSourceName() == null) snap.setSourceName("MLB Stats API");
                snap.setSourceLastUpdated(LocalDate.now());
                teamStatRepo.save(snap);
                count++;
            }
        }
        markSynced("standings", "MLB Stats API", count + " team standings imported for " + season);
    }

    /** Import hitting + pitching stats for every known team (not just BOS) and merge into each snapshot. */
    @Transactional
    public void importTeamStats() {
        int season = api.currentSeason();
        List<Team> teams = teamRepo.findAll();
        if (teams.isEmpty()) { log.warn("No teams found in DB"); return; }

        LeagueContext league = leagueContext(season);
        int count = 0;
        for (Team team : teams) {
            if (team.getMlbTeamId() == null) continue;
            try {
                importTeamStats(team, season, league);
                count++;
            } catch (Exception e) {
                log.warn("Failed to import team stats for {}: {}", team.getTeamCode(), e.getMessage());
            }
        }
        log.info("Team stats imported for {} teams", count);
        markSynced("team_stats", "MLB Stats API", count + " teams' hitting + pitching stats for season " + season);
    }

    private void importTeamStats(Team team, int season, LeagueContext league) {
        TeamStatSnapshot snap = teamStatRepo.findTopByTeamOrderBySnapshotDateDesc(team)
                .orElse(new TeamStatSnapshot());
        snap.setTeam(team);
        snap.setSeason(season);
        snap.setSnapshotDate(LocalDate.now());

        // Hitting stats
        JsonNode hitting = api.fetchTeamHittingStats(team.getMlbTeamId(), season);
        JsonNode hStat   = firstSplit(hitting);
        if (hStat != null) {
            snap.setRunsScored(hStat.path("runs").asInt(0));
            snap.setTeamHomeRuns(hStat.path("homeRuns").asInt(0));
            snap.setTeamAvg(parseDouble(hStat, "avg"));
            snap.setTeamObp(parseDouble(hStat, "obp"));
            snap.setTeamSlg(parseDouble(hStat, "slg"));
            snap.setTeamOps(parseDouble(hStat, "ops"));

            int pa = hStat.path("plateAppearances").asInt(1);
            int k  = hStat.path("strikeOuts").asInt(0);
            int bb = hStat.path("baseOnBalls").asInt(0);
            if (pa > 0) {
                snap.setTeamKRate(round2(100.0 * k / pa));
                snap.setTeamBbRate(round2(100.0 * bb / pa));
            }
            int gp = hStat.path("gamesPlayed").asInt(1);
            if (gp > 0) snap.setRunsPerGame(round2((double) hStat.path("runs").asInt(0) / gp));

            // ISO = SLG − AVG
            Double slg = snap.getTeamSlg(), avg = snap.getTeamAvg();
            if (slg != null && avg != null) snap.setTeamIso(round2(slg - avg));

            // Team wRC+ estimate, same OBP/SLG-vs-league-average formula used for individual hitters.
            if (snap.getTeamObp() != null && snap.getTeamSlg() != null && league.hitterObp > 0 && league.hitterSlg > 0) {
                double wrcPlusEstimate = 100.0 * ((snap.getTeamObp() / league.hitterObp) + (snap.getTeamSlg() / league.hitterSlg) - 1.0);
                snap.setTeamWrcPlus(round2(wrcPlusEstimate));
            }
        }

        // Pitching stats
        JsonNode pitching = api.fetchTeamPitchingStats(team.getMlbTeamId(), season);
        JsonNode pStat    = firstSplit(pitching);
        if (pStat != null) {
            snap.setRunsAllowed(pStat.path("runs").asInt(0));
            snap.setHomeRunsAllowed(pStat.path("homeRuns").asInt(0));
            snap.setTeamEra(parseDouble(pStat, "era"));
            snap.setTeamWhip(parseDouble(pStat, "whip"));

            int pa2 = pStat.path("battersFaced").asInt(1);
            int k2  = pStat.path("strikeOuts").asInt(0);
            int bb2 = pStat.path("baseOnBalls").asInt(0);
            if (pa2 > 0) {
                snap.setTeamKRatePitching(round2(100.0 * k2 / pa2));
                snap.setTeamBbRatePitching(round2(100.0 * bb2 / pa2));
            }

            // Team FIP, same formula used for individual pitchers.
            int hitBatters2 = pStat.path("hitBatsmen").asInt(pStat.path("hitByPitch").asInt(0));
            Double innings2 = parseInnings(pStat.path("inningsPitched").asText(null));
            snap.setTeamFip(calcFip(pStat.path("homeRuns").asInt(0), bb2, hitBatters2, k2, innings2, league.fipConstant));

            // Defensive Efficiency Ratio: rate at which balls put in play (excluding HR) are converted to outs.
            int hitsAllowed = pStat.path("hits").asInt(0);
            int homeRunsAllowed = pStat.path("homeRuns").asInt(0);
            int ballsInPlay = pa2 - bb2 - k2 - hitBatters2 - homeRunsAllowed;
            if (ballsInPlay > 0) {
                snap.setDefensiveEfficiency(round3(1.0 - ((double) (hitsAllowed - homeRunsAllowed) / ballsInPlay)));
            }
        }

        // Real team fielding percentage (MLB Stats API, group=fielding).
        JsonNode fieldingStat = firstSplit(api.fetchTeamFieldingStats(team.getMlbTeamId(), season));
        if (fieldingStat != null) {
            snap.setFieldingPercentage(parseDouble(fieldingStat, "fielding"));
        }

        // Compute run differential from what we know
        Integer rs = snap.getRunsScored(), ra = snap.getRunsAllowed();
        if (rs != null && ra != null) snap.setRunDifferential(rs - ra);

        applyBullpenAggregate(snap, team, season);

        snap.setSourceName("MLB Stats API");
        snap.setSourceLastUpdated(LocalDate.now());
        teamStatRepo.save(snap);
        log.info("Team stats imported for {}: OPS={} ERA={}", team.getTeamCode(), snap.getTeamOps(), snap.getTeamEra());
    }

    /**
     * Derives bullpen-only rates from individually tracked relief pitchers (innings-weighted).
     * Only teams with per-player pitching snapshots already imported (currently BOS, plus any
     * opponent probable starters synced via upsertProbablePitcher) contribute here.
     */
    private void applyBullpenAggregate(TeamStatSnapshot snap, Team team, int season) {
        List<PitcherStatSnapshot> relievers = pitcherStatRepo.findByTeamAndSeasonAndRoleOrderByWarDesc(team, season, "RP");
        double totalIp = 0, earnedRuns = 0, fipSum = 0, whipSum = 0, kSum = 0, bbSum = 0;
        for (PitcherStatSnapshot p : relievers) {
            if (p.getInningsPitched() == null || p.getInningsPitched() <= 0) continue;
            double ip = p.getInningsPitched();
            totalIp += ip;
            if (p.getEra() != null) earnedRuns += p.getEra() * ip / 9.0;
            if (p.getFip() != null) fipSum += p.getFip() * ip;
            if (p.getWhip() != null) whipSum += p.getWhip() * ip;
            if (p.getKRate() != null) kSum += p.getKRate() * ip;
            if (p.getBbRate() != null) bbSum += p.getBbRate() * ip;
        }
        if (totalIp <= 0) return;
        snap.setBullpenEra(round2(earnedRuns * 9.0 / totalIp));
        snap.setBullpenFip(round2(fipSum / totalIp));
        snap.setBullpenWhip(round2(whipSum / totalIp));
        snap.setBullpenKRate(round2(kSum / totalIp));
        snap.setBullpenBbRate(round2(bbSum / totalIp));
    }

    /** Import current BOS active roster. */
    @Transactional
    public int importRoster() {
        Optional<Team> bosOpt = teamRepo.findByTeamCode("BOS");
        if (bosOpt.isEmpty()) {
            log.warn("BOS team not found in DB");
            return 0;
        }

        Team bos = bosOpt.get();
        JsonNode root = api.fetchBosRoster();
        int count = 0;
        for (JsonNode item : root.path("roster")) {
            JsonNode person = item.path("person");
            int mlbId = person.path("id").asInt(0);
            if (mlbId == 0) continue;

            Player player = playerRepo.findByMlbPlayerId(mlbId).orElse(new Player());
            player.setTeam(bos);
            player.setMlbPlayerId(mlbId);
            setPlayerName(player, person.path("fullName").asText("Unknown Player"));
            player.setPosition(item.path("position").path("abbreviation")
                    .asText(person.path("primaryPosition").path("abbreviation").asText(null)));
            player.setJerseyNumber(parseInt(item.path("jerseyNumber").asText(null)));
            player.setBats(person.path("batSide").path("code").asText(null));
            player.setThrowsHand(person.path("pitchHand").path("code").asText(null));
            playerRepo.save(player);
            count++;
        }
        markSynced("rosters", "MLB Stats API", count + " active roster players imported");
        return count;
    }

    /** Import basic MLB season stats for players currently on the BOS roster. */
    @Transactional
    public int importPlayerStats() {
        int season = api.currentSeason();
        Optional<Team> bosOpt = teamRepo.findByTeamCode("BOS");
        if (bosOpt.isEmpty()) return 0;

        Team bos = bosOpt.get();
        if (playerRepo.findByTeamOrderByLastNameAsc(bos).isEmpty()) {
            importRoster();
        }

        LeagueContext league = leagueContext(season);
        int count = 0;
        for (Player player : playerRepo.findByTeamOrderByLastNameAsc(bos)) {
            if (player.getMlbPlayerId() == null) continue;
            if ("P".equalsIgnoreCase(player.getPosition())) {
                if (upsertPitcherStats(player, bos, season, league)) count++;
            } else {
                if (upsertHitterStats(player, bos, season, league)) count++;
            }
        }

        applyTeamWar(bos, season);

        markSynced("player_stats", "MLB Stats API", count + " player stat snapshots imported/updated");
        return count;
    }

    /**
     * Team WAR = sum of every rostered player's individual WAR (only meaningful for teams with
     * full player-level import, currently BOS).
     */
    private void applyTeamWar(Team team, int season) {
        double total = 0;
        boolean any = false;
        for (HitterStatSnapshot h : hitterStatRepo.findByTeamAndSeasonOrderByWarDesc(team, season)) {
            if (h.getWar() != null) { total += h.getWar(); any = true; }
        }
        for (PitcherStatSnapshot p : pitcherStatRepo.findByTeamAndSeasonOrderByWarDesc(team, season)) {
            if (p.getWar() != null) { total += p.getWar(); any = true; }
        }
        if (!any) return;
        double finalTotal = total;
        teamStatRepo.findTopByTeamOrderBySnapshotDateDesc(team).ifPresent(snap -> {
            snap.setTeamWar(round1(finalTotal));
            teamStatRepo.save(snap);
        });
    }

    /**
     * Enriches every already-imported hitter/pitcher snapshot with Statcast quality-of-contact
     * metrics and Outs Above Average from Baseball Savant's public leaderboard export. Runs
     * league-wide in three requests total (not per-player), matched back by MLBAM player_id.
     */
    @Transactional
    public void importAdvancedMetrics() {
        int season = api.currentSeason();
        Map<Integer, Map<String, String>> batterMetrics = indexByPlayerId(savant.fetchBatterMetrics(season));
        Map<Integer, Map<String, String>> pitcherMetrics = indexByPlayerId(savant.fetchPitcherMetrics(season));
        List<Map<String, String>> oaaRows = savant.fetchOutsAboveAverage(season);

        int hitterCount = 0;
        for (HitterStatSnapshot snap : hitterStatRepo.findAll()) {
            Integer mlbId = snap.getPlayer() != null ? snap.getPlayer().getMlbPlayerId() : null;
            Map<String, String> row = mlbId != null ? batterMetrics.get(mlbId) : null;
            if (row == null) continue;
            snap.setHardHitRate(parseCsvDouble(row.get("hard_hit_percent")));
            snap.setBarrelRate(parseCsvDouble(row.get("barrel_batted_rate")));
            snap.setAvgExitVelocity(parseCsvDouble(row.get("exit_velocity_avg")));
            snap.setLaunchAngle(parseCsvDouble(row.get("launch_angle_avg")));
            hitterStatRepo.save(snap);
            hitterCount++;
        }

        int pitcherCount = 0;
        for (PitcherStatSnapshot snap : pitcherStatRepo.findAll()) {
            Integer mlbId = snap.getPlayer() != null ? snap.getPlayer().getMlbPlayerId() : null;
            Map<String, String> row = mlbId != null ? pitcherMetrics.get(mlbId) : null;
            if (row == null) continue;
            snap.setHardHitRateAllowed(parseCsvDouble(row.get("hard_hit_percent")));
            snap.setBarrelRateAllowed(parseCsvDouble(row.get("barrel_batted_rate")));
            snap.setWhiffRate(parseCsvDouble(row.get("whiff_percent")));
            snap.setAvgExitVelocityAllowed(parseCsvDouble(row.get("exit_velocity_avg")));
            snap.setAvgFastballVelocity(parseCsvDouble(row.get("fastball_avg_speed")));
            pitcherStatRepo.save(snap);
            pitcherCount++;
        }

        // Outs Above Average: one row per player per position, so sum across positions/team.
        Map<Integer, Integer> playerOaa = new HashMap<>();
        Map<String, Integer> teamOaa = new HashMap<>();
        for (Map<String, String> row : oaaRows) {
            Integer pid = parseCsvInt(row.get("player_id"));
            Integer oaa = parseCsvInt(row.get("outs_above_average"));
            if (pid == null || oaa == null) continue;
            playerOaa.merge(pid, oaa, Integer::sum);
            String teamName = row.get("display_team_name");
            if (teamName != null && !teamName.isBlank()) teamOaa.merge(teamName, oaa, Integer::sum);
        }
        for (HitterStatSnapshot snap : hitterStatRepo.findAll()) {
            Integer mlbId = snap.getPlayer() != null ? snap.getPlayer().getMlbPlayerId() : null;
            if (mlbId != null && playerOaa.containsKey(mlbId)) {
                snap.setOutsAboveAverage(playerOaa.get(mlbId));
                hitterStatRepo.save(snap);
            }
        }
        for (Team team : teamRepo.findAll()) {
            Integer oaa = team.getShortName() != null ? teamOaa.get(team.getShortName()) : null;
            if (oaa == null) continue;
            teamStatRepo.findTopByTeamOrderBySnapshotDateDesc(team).ifPresent(snap -> {
                snap.setOutsAboveAverage(oaa);
                teamStatRepo.save(snap);
            });
        }

        markSynced("advanced_metrics", "Baseball Savant",
                hitterCount + " hitters + " + pitcherCount + " pitchers enriched with Statcast metrics for season " + season);
    }

    private Map<Integer, Map<String, String>> indexByPlayerId(List<Map<String, String>> rows) {
        Map<Integer, Map<String, String>> byId = new HashMap<>();
        for (Map<String, String> row : rows) {
            Integer id = parseCsvInt(row.get("player_id"));
            if (id != null) byId.put(id, row);
        }
        return byId;
    }

    private Double parseCsvDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseCsvInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return (int) Math.round(Double.parseDouble(s.trim())); } catch (NumberFormatException e) { return null; }
    }

    /** Seed static reference data (teams, park factors) if the DB is empty. */
    @Transactional
    public void seedStaticDataIfNeeded() {
        if (teamRepo.count() > 0) {
            log.info("Static data already seeded — skipping");
            return;
        }
        log.info("Seeding static reference data...");
        seedTeams();
        seedBallparkFactors();
        seedPlayers();
        seedSyncLog();
        log.info("Static data seeded");
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private JsonNode firstSplit(JsonNode root) {
        JsonNode stats = root.path("stats");
        if (stats.isArray() && stats.size() > 0) {
            JsonNode splits = stats.get(0).path("splits");
            if (splits.isArray() && splits.size() > 0) {
                return splits.get(0).path("stat");
            }
        }
        return null;
    }

    private Double parseDouble(JsonNode node, String field) {
        String val = node.path(field).asText(null);
        if (val == null || val.isBlank()) return null;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return null; }
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    private boolean upsertHitterStats(Player player, Team team, int season, LeagueContext league) {
        JsonNode stat = firstSplit(api.fetchPlayerStats(player.getMlbPlayerId(), "hitting", season));
        if (stat == null || stat.isMissingNode()) return false;

        HitterStatSnapshot snap = hitterStatRepo
                .findTopByPlayerAndSeasonOrderBySnapshotDateDesc(player, season)
                .orElse(new HitterStatSnapshot());
        snap.setPlayer(player);
        snap.setTeam(team);
        snap.setSeason(season);
        snap.setSnapshotDate(LocalDate.now());
        snap.setGamesPlayed(stat.path("gamesPlayed").asInt(0));
        snap.setPlateAppearances(stat.path("plateAppearances").asInt(0));
        snap.setAvg(parseDouble(stat, "avg"));
        snap.setObp(parseDouble(stat, "obp"));
        snap.setSlg(parseDouble(stat, "slg"));
        snap.setOps(parseDouble(stat, "ops"));
        snap.setHomeRuns(stat.path("homeRuns").asInt(0));
        snap.setRbi(stat.path("rbi").asInt(0));
        snap.setRuns(stat.path("runs").asInt(0));
        snap.setStolenBases(stat.path("stolenBases").asInt(0));

        int pa = stat.path("plateAppearances").asInt(0);
        if (pa > 0) {
            snap.setKRate(round2(100.0 * stat.path("strikeOuts").asInt(0) / pa));
            snap.setBbRate(round2(100.0 * stat.path("baseOnBalls").asInt(0) / pa));
        }
        Double slg = snap.getSlg();
        Double avg = snap.getAvg();
        if (slg != null && avg != null) snap.setIso(round2(slg - avg));
        snap.setBabip(parseDouble(stat, "babip"));
        if (snap.getObp() != null && snap.getSlg() != null && league.hitterObp > 0 && league.hitterSlg > 0) {
            double wrcPlusEstimate = 100.0 * ((snap.getObp() / league.hitterObp) + (snap.getSlg() / league.hitterSlg) - 1.0);
            snap.setWrcPlus(round2(wrcPlusEstimate));
        }
        if (snap.getWrcPlus() != null && pa > 0) {
            double offensiveRuns = ((snap.getWrcPlus() - 100.0) / 100.0) * (pa / 60.0);
            double playingTimeCredit = pa / 600.0;
            snap.setWar(round1(offensiveRuns + playingTimeCredit));
        }

        // Real wRC+ and WAR from MLB's own sabermetrics endpoint, overriding the estimate above when available.
        JsonNode saber = firstSplit(api.fetchPlayerSabermetrics(player.getMlbPlayerId(), "hitting", season));
        if (saber != null) {
            Double realWrcPlus = parseDouble(saber, "wRcPlus");
            if (realWrcPlus != null) snap.setWrcPlus(round1(realWrcPlus));
            Double realWar = parseDouble(saber, "war");
            if (realWar != null) snap.setWar(round1(realWar));
        }

        JsonNode fielding = firstSplit(api.fetchPlayerFieldingStats(player.getMlbPlayerId(), season));
        if (fielding != null) snap.setFieldingPercentage(parseDouble(fielding, "fielding"));

        snap.setSourceName("MLB Stats API");
        snap.setSourceLastUpdated(LocalDate.now());
        hitterStatRepo.save(snap);
        return true;
    }

    private boolean upsertPitcherStats(Player player, Team team, int season, LeagueContext league) {
        JsonNode stat = firstSplit(api.fetchPlayerStats(player.getMlbPlayerId(), "pitching", season));
        if (stat == null || stat.isMissingNode()) return false;

        PitcherStatSnapshot snap = pitcherStatRepo
                .findTopByPlayerAndSeasonOrderBySnapshotDateDesc(player, season)
                .orElse(new PitcherStatSnapshot());
        snap.setPlayer(player);
        snap.setTeam(team);
        snap.setSeason(season);
        snap.setSnapshotDate(LocalDate.now());
        int starts = stat.path("gamesStarted").asInt(0);
        int games = stat.path("gamesPitched").asInt(stat.path("games").asInt(0));
        snap.setRole(starts >= 3 || (games > 0 && starts >= Math.max(1, games / 3)) ? "SP" : "RP");
        snap.setGamesStarted(starts);
        snap.setGames(games);
        snap.setEra(parseDouble(stat, "era"));
        snap.setWhip(parseDouble(stat, "whip"));
        snap.setInningsPitched(parseInnings(stat.path("inningsPitched").asText(null)));
        snap.setOpponentOps(parseDouble(stat, "ops"));
        snap.setHomeRunsAllowed(stat.path("homeRuns").asInt(0));
        snap.setKPer9(parseDouble(stat, "strikeoutsPer9Inn"));
        snap.setBbPer9(parseDouble(stat, "walksPer9Inn"));
        snap.setHrPer9(calcPer9(stat.path("homeRuns").asInt(0), snap.getInningsPitched()));
        snap.setSaves(stat.path("saves").asInt(0));
        snap.setSaveOpportunities(stat.path("saveOpportunities").asInt(0));
        snap.setHolds(stat.path("holds").asInt(0));
        snap.setBlownSaves(stat.path("blownSaves").asInt(0));

        Integer strikeouts = stat.path("strikeOuts").asInt(0);
        Integer walks = stat.path("baseOnBalls").asInt(0);
        Integer hitBatters = stat.path("hitBatsmen").asInt(stat.path("hitByPitch").asInt(0));
        Double fip = calcFip(stat.path("homeRuns").asInt(0), walks, hitBatters, strikeouts, snap.getInningsPitched(), league.fipConstant);
        snap.setFip(fip);
        if (fip != null && snap.getInningsPitched() != null && league.pitcherEra > 0) {
            double preventionRuns = (league.pitcherEra - fip) * snap.getInningsPitched() / 9.0;
            double roleCredit = "SP".equals(snap.getRole()) ? snap.getInningsPitched() / 180.0 : snap.getInningsPitched() / 90.0;
            snap.setWar(round1(preventionRuns / 10.0 + roleCredit));
        }

        int battersFaced = stat.path("battersFaced").asInt(0);
        if (battersFaced > 0) {
            double kRate = round2(100.0 * strikeouts / battersFaced);
            double bbRate = round2(100.0 * walks / battersFaced);
            snap.setKRate(kRate);
            snap.setBbRate(bbRate);
            snap.setKMinusBbRate(round2(kRate - bbRate));
        }

        // Real xFIP and WAR from MLB's own sabermetrics endpoint, overriding the FIP-based estimate above.
        JsonNode saber = firstSplit(api.fetchPlayerSabermetrics(player.getMlbPlayerId(), "pitching", season));
        if (saber != null) {
            Double realXfip = parseDouble(saber, "xfip");
            if (realXfip != null) snap.setXfip(round2(realXfip));
            Double realWar = parseDouble(saber, "war");
            if (realWar != null) snap.setWar(round1(realWar));
        }

        snap.setSourceName("MLB Stats API");
        snap.setSourceLastUpdated(LocalDate.now());
        pitcherStatRepo.save(snap);
        return true;
    }

    private Double parseInnings(String text) {
        if (text == null || text.isBlank()) return null;
        String[] parts = text.split("\\.");
        try {
            double whole = Double.parseDouble(parts[0]);
            if (parts.length == 1) return whole;
            int outs = Integer.parseInt(parts[1]);
            return round2(whole + outs / 3.0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double calcPer9(int events, Double innings) {
        if (innings == null || innings <= 0) return null;
        return round2(events * 9.0 / innings);
    }

    private Double calcFip(int homeRuns, int walks, int hitBatters, int strikeouts, Double innings, double constant) {
        if (innings == null || innings <= 0) return null;
        return round2(((13.0 * homeRuns) + (3.0 * (walks + hitBatters)) - (2.0 * strikeouts)) / innings + constant);
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }

    private LeagueContext leagueContext(int season) {
        LeagueContext league = new LeagueContext();

        long hAb = 0, hHits = 0, hBb = 0, hHbp = 0, hSf = 0, hTb = 0;
        JsonNode hittingSplits = api.fetchLeagueHittingStats(season).path("stats").path(0).path("splits");
        for (JsonNode split : hittingSplits) {
            JsonNode stat = split.path("stat");
            hAb += stat.path("atBats").asLong(0);
            hHits += stat.path("hits").asLong(0);
            hBb += stat.path("baseOnBalls").asLong(0);
            hHbp += stat.path("hitByPitch").asLong(0);
            hSf += stat.path("sacFlies").asLong(0);
            hTb += stat.path("totalBases").asLong(0);
        }
        long obpDenominator = hAb + hBb + hHbp + hSf;
        league.hitterObp = obpDenominator > 0 ? (double) (hHits + hBb + hHbp) / obpDenominator : 0.315;
        league.hitterSlg = hAb > 0 ? (double) hTb / hAb : 0.400;

        long pHr = 0, pBb = 0, pHbp = 0, pK = 0, pEr = 0;
        double pIp = 0.0;
        JsonNode pitchingSplits = api.fetchLeaguePitchingStats(season).path("stats").path(0).path("splits");
        for (JsonNode split : pitchingSplits) {
            JsonNode stat = split.path("stat");
            pHr += stat.path("homeRuns").asLong(0);
            pBb += stat.path("baseOnBalls").asLong(0);
            pHbp += stat.path("hitBatsmen").asLong(stat.path("hitByPitch").asLong(0));
            pK += stat.path("strikeOuts").asLong(0);
            pEr += stat.path("earnedRuns").asLong(0);
            Double ip = parseInnings(stat.path("inningsPitched").asText(null));
            if (ip != null) pIp += ip;
        }
        league.pitcherEra = pIp > 0 ? 9.0 * pEr / pIp : 4.20;
        double fipNoConstant = pIp > 0 ? ((13.0 * pHr) + (3.0 * (pBb + pHbp)) - (2.0 * pK)) / pIp : 1.0;
        league.fipConstant = league.pitcherEra - fipNoConstant;
        return league;
    }

    private static class LeagueContext {
        private double hitterObp = 0.315;
        private double hitterSlg = 0.400;
        private double pitcherEra = 4.20;
        private double fipConstant = 3.10;
    }

    private Integer parseInt(String text) {
        if (text == null || text.isBlank()) return null;
        try { return Integer.parseInt(text); } catch (NumberFormatException e) { return null; }
    }

    private void setPlayerName(Player player, String fullName) {
        String cleaned = fullName == null || fullName.isBlank() ? "Unknown Player" : fullName.trim();
        int lastSpace = cleaned.lastIndexOf(' ');
        if (lastSpace > 0) {
            player.setFirstName(cleaned.substring(0, lastSpace));
            player.setLastName(cleaned.substring(lastSpace + 1));
        } else {
            player.setFirstName(null);
            player.setLastName(cleaned);
        }
    }

    private String mapStatus(String abstractState, String detailed) {
        return switch (abstractState) {
            case "Final"   -> "Final";
            case "Live"    -> "In Progress";
            default        -> {
                if (detailed.contains("Postponed")) yield "Postponed";
                if (detailed.contains("Cancelled")) yield "Cancelled";
                yield "Scheduled";
            }
        };
    }

    private String recordText(JsonNode teamNode) {
        JsonNode record = teamNode.path("leagueRecord");
        if (record.isMissingNode() || record.isEmpty()) return null;
        if (record.has("wins") && record.has("losses")) {
            return record.path("wins").asInt() + " - " + record.path("losses").asInt();
        }
        return null;
    }

    private String splitRecordText(JsonNode teamRecord, String splitType) {
        for (JsonNode split : teamRecord.path("records").path("splitRecords")) {
            if (splitType.equals(split.path("type").asText())) {
                return split.path("wins").asInt(0) + "-" + split.path("losses").asInt(0);
            }
        }
        return "-";
    }

    private String expectedRecordText(JsonNode teamRecord) {
        for (JsonNode expected : teamRecord.path("records").path("expectedRecords")) {
            if ("xWinLoss".equals(expected.path("type").asText())) {
                return expected.path("wins").asInt(0) + "-" + expected.path("losses").asInt(0);
            }
        }
        return "-";
    }

    private Integer intOrNull(JsonNode node, String field) {
        return node.has(field) && !node.path(field).isNull() ? node.path(field).asInt() : null;
    }

    private Integer idOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.has("id")) return null;
        int id = node.path("id").asInt(0);
        return id > 0 ? id : null;
    }

    private String nameOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String name = node.path("fullName").asText(null);
        return name == null || name.isBlank() ? null : name;
    }

    private void upsertProbablePitcher(JsonNode pitcherNode, int teamId) {
        Integer playerId = idOrNull(pitcherNode);
        if (playerId == null) return;

        int season = api.currentSeason();
        Team team = teamForMlbId(teamId);
        Player player = playerRepo.findByMlbPlayerId(playerId).orElse(new Player());
        player.setTeam(team);
        player.setMlbPlayerId(playerId);
        player.setPosition("P");
        setPlayerName(player, nameOrNull(pitcherNode));
        playerRepo.save(player);

        Optional<PitcherStatSnapshot> existing = pitcherStatRepo
                .findTopByPlayerAndSeasonOrderBySnapshotDateDesc(player, season);
        if (existing.isPresent() && LocalDate.now().equals(existing.get().getSourceLastUpdated())) {
            return;
        }

        try {
            upsertPitcherStats(player, team, season, leagueContext(season));
        } catch (Exception e) {
            log.warn("Failed to import probable starter stats for {}: {}", player.getFullName(), e.getMessage());
        }
    }

    private Team teamForMlbId(int mlbTeamId) {
        return teamRepo.findByMlbTeamId(mlbTeamId).orElseGet(() -> {
            Team team = new Team();
            String shortName = TEAM_SHORT.getOrDefault(mlbTeamId, "Team " + mlbTeamId);
            team.setMlbTeamId(mlbTeamId);
            team.setShortName(shortName);
            team.setTeamName(shortName);
            team.setTeamCode("MLB" + mlbTeamId);
            return teamRepo.save(team);
        });
    }

    private void markSynced(String type, String source, String notes) {
        SyncLog log2 = syncLogRepo.findByDataType(type).orElse(new SyncLog());
        log2.setDataType(type);
        log2.setDisplayLabel(toLabel(type));
        log2.setLastUpdated(LocalDateTime.now());
        log2.setSourceName(source);
        log2.setStatus("Success");
        log2.setNotes(notes);
        syncLogRepo.save(log2);
    }

    private String toLabel(String type) {
        return switch (type) {
            case "schedule"    -> "Schedule / Game Results";
            case "standings"   -> "Standings";
            case "team_stats"  -> "Team Stats";
            case "park_factors"-> "Ballpark Factors";
            case "rosters"     -> "Rosters";
            case "advanced_metrics" -> "Advanced Metrics (Statcast)";
            default            -> type;
        };
    }

    // ── Static seed data ─────────────────────────────────────────────────────

    private void seedTeams() {
        List<Object[]> teams = List.of(
            new Object[]{"BOS","Boston Red Sox","Red Sox","AL","East","Fenway Park",111},
            new Object[]{"NYY","New York Yankees","Yankees","AL","East","Yankee Stadium",147},
            new Object[]{"TBR","Tampa Bay Rays","Rays","AL","East","Tropicana Field",139},
            new Object[]{"TOR","Toronto Blue Jays","Blue Jays","AL","East","Rogers Centre",141},
            new Object[]{"BAL","Baltimore Orioles","Orioles","AL","East","Oriole Park",110},
            new Object[]{"HOU","Houston Astros","Astros","AL","West","Minute Maid Park",117}
        );
        for (Object[] t : teams) {
            if (teamRepo.findByTeamCode((String)t[0]).isEmpty()) {
                Team team = new Team();
                team.setTeamCode((String)t[0]);
                team.setTeamName((String)t[1]);
                team.setShortName((String)t[2]);
                team.setLeague((String)t[3]);
                team.setDivision((String)t[4]);
                team.setHomeVenue((String)t[5]);
                team.setMlbTeamId((Integer)t[6]);
                teamRepo.save(team);
            }
        }
    }

    private void seedBallparkFactors() {
        // Park factors are seeded lazily in the BallparkFactorSnapshotRepository
        // via the BallparkFactorSnapshot entity — done by a separate seeder if needed
        // Implemented separately to keep this method short.
    }

    private void seedPlayers() {
        // Players are seeded from roster API in future; skip initial seeding
        // The player stats pages will show "no data" until manually imported
    }

    private void seedSyncLog() {
        List<String[]> logs = List.of(
            new String[]{"schedule",   "Not yet synced — click Refresh"},
            new String[]{"standings",  "Not yet synced — click Refresh"},
            new String[]{"team_stats", "Not yet synced — click Refresh"},
            new String[]{"park_factors","Bundled static data"},
            new String[]{"rosters",    "Not yet synced"},
            new String[]{"advanced_metrics", "Not yet synced — click Refresh"}
        );
        for (String[] l : logs) {
            if (syncLogRepo.findByDataType(l[0]).isEmpty()) {
                SyncLog sl = new SyncLog();
                sl.setDataType(l[0]);
                sl.setDisplayLabel(toLabel(l[0]));
                sl.setStatus("Pending");
                sl.setNotes(l[1]);
                sl.setSourceName("MLB Stats API");
                sl.setLastUpdated(LocalDateTime.now().minusYears(1));
                syncLogRepo.save(sl);
            }
        }
    }
}
