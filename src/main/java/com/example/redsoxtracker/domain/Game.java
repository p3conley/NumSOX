package com.example.redsoxtracker.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer mlbGameId;

    @NotNull
    private LocalDate gameDate;

    @NotBlank
    private String opponent;

    @NotBlank
    private String homeAway;

    private Integer redSoxScore;
    private Integer opponentScore;
    private Integer redSoxHits;
    private Integer opponentHits;
    private Integer redSoxErrors;
    private Integer opponentErrors;

    @NotBlank
    private String status;

    private String result;
    private String venue;
    private String redSoxRecord;
    private String opponentRecord;
    private Integer awayProbablePitcherId;
    private String awayProbablePitcherName;
    private Integer homeProbablePitcherId;
    private String homeProbablePitcherName;
    private Boolean favorite = false;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameNote> notes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMlbGameId() {
        return mlbGameId;
    }

    public void setMlbGameId(Integer mlbGameId) {
        this.mlbGameId = mlbGameId;
    }

    public LocalDate getGameDate() {
        return gameDate;
    }

    public void setGameDate(LocalDate gameDate) {
        this.gameDate = gameDate;
    }

    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public String getHomeAway() {
        return homeAway;
    }

    public void setHomeAway(String homeAway) {
        this.homeAway = homeAway;
    }

    public Integer getRedSoxScore() {
        return redSoxScore;
    }

    public void setRedSoxScore(Integer redSoxScore) {
        this.redSoxScore = redSoxScore;
    }

    public Integer getOpponentScore() {
        return opponentScore;
    }

    public void setOpponentScore(Integer opponentScore) {
        this.opponentScore = opponentScore;
    }

    public Integer getRedSoxHits() {
        return redSoxHits;
    }

    public void setRedSoxHits(Integer redSoxHits) {
        this.redSoxHits = redSoxHits;
    }

    public Integer getOpponentHits() {
        return opponentHits;
    }

    public void setOpponentHits(Integer opponentHits) {
        this.opponentHits = opponentHits;
    }

    public Integer getRedSoxErrors() {
        return redSoxErrors;
    }

    public void setRedSoxErrors(Integer redSoxErrors) {
        this.redSoxErrors = redSoxErrors;
    }

    public Integer getOpponentErrors() {
        return opponentErrors;
    }

    public void setOpponentErrors(Integer opponentErrors) {
        this.opponentErrors = opponentErrors;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getRedSoxRecord() {
        return redSoxRecord;
    }

    public void setRedSoxRecord(String redSoxRecord) {
        this.redSoxRecord = redSoxRecord;
    }

    public String getOpponentRecord() {
        return opponentRecord;
    }

    public void setOpponentRecord(String opponentRecord) {
        this.opponentRecord = opponentRecord;
    }

    public Integer getAwayProbablePitcherId() {
        return awayProbablePitcherId;
    }

    public void setAwayProbablePitcherId(Integer awayProbablePitcherId) {
        this.awayProbablePitcherId = awayProbablePitcherId;
    }

    public String getAwayProbablePitcherName() {
        return awayProbablePitcherName;
    }

    public void setAwayProbablePitcherName(String awayProbablePitcherName) {
        this.awayProbablePitcherName = awayProbablePitcherName;
    }

    public Integer getHomeProbablePitcherId() {
        return homeProbablePitcherId;
    }

    public void setHomeProbablePitcherId(Integer homeProbablePitcherId) {
        this.homeProbablePitcherId = homeProbablePitcherId;
    }

    public String getHomeProbablePitcherName() {
        return homeProbablePitcherName;
    }

    public void setHomeProbablePitcherName(String homeProbablePitcherName) {
        this.homeProbablePitcherName = homeProbablePitcherName;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }

    public List<GameNote> getNotes() {
        return notes;
    }

    public void setNotes(List<GameNote> notes) {
        this.notes = notes;
    }
}
