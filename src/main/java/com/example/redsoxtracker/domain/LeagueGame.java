package com.example.redsoxtracker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * One completed or scheduled game for any of the 30 clubs.
 *
 * <p>{@link Game} only tracks the Red Sox. Standings for an arbitrary date have to be
 * tallied from every club's results, so this is a deliberately thin league-wide log:
 * enough to reconstruct wins, losses, runs and home/away splits on any day of the
 * season without calling the MLB standings feed once per date.</p>
 */
@Entity
@Table(name = "league_game", indexes = {
        @Index(name = "idx_league_game_date", columnList = "game_date")
})
public class LeagueGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mlb_game_id", unique = true)
    private Integer mlbGameId;

    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    @Column(name = "home_team_id", nullable = false)
    private Integer homeTeamId;

    @Column(name = "away_team_id", nullable = false)
    private Integer awayTeamId;

    private Integer homeScore;
    private Integer awayScore;

    @Column(nullable = false)
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getMlbGameId() { return mlbGameId; }
    public void setMlbGameId(Integer mlbGameId) { this.mlbGameId = mlbGameId; }

    public LocalDate getGameDate() { return gameDate; }
    public void setGameDate(LocalDate gameDate) { this.gameDate = gameDate; }

    public Integer getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(Integer homeTeamId) { this.homeTeamId = homeTeamId; }

    public Integer getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(Integer awayTeamId) { this.awayTeamId = awayTeamId; }

    public Integer getHomeScore() { return homeScore; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }

    public Integer getAwayScore() { return awayScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** A game only moves the standings once it is final and actually has a score. */
    public boolean isDecided() {
        return "Final".equals(status) && homeScore != null && awayScore != null
                && !homeScore.equals(awayScore);
    }
}
