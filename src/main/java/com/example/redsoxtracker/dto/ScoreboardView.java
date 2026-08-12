package com.example.redsoxtracker.dto;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardView {

    private String awayName;
    private String homeName;
    private Integer awayRuns;
    private Integer awayHits;
    private Integer awayErrors;
    private Integer homeRuns;
    private Integer homeHits;
    private Integer homeErrors;
    private Integer balls;
    private Integer strikes;
    private Integer outs;
    private String atBat;
    /** Whoever is on the mound right now. */
    private String pitching;
    private String inningState;
    /** The frame currently being played; null once the game is over. */
    private Integer currentInning;
    /** "Top" or "Bottom" for the half being played. */
    private String inningHalf;
    /** Raw feed state: Top, Middle, Bottom or End. Middle and End mean the side was retired. */
    private String inningPhase;
    /** "5th", used for the MID/END caption between innings. */
    private String inningOrdinal;
    /** eventType of the most recently completed at-bat, e.g. "walk" or "strikeout". */
    private String lastPlayEvent;
    /** atBatIndex of that play, so the page can tell a new one from a repeat poll. */
    private Integer lastPlayIndex;
    private boolean finalGame;
    /** Occupied bases, for the diamond under the count. */
    private boolean onFirst;
    private boolean onSecond;
    private boolean onThird;
    /** ABS challenges: how many each side has left, and how many they have burned. */
    private boolean hasChallenges;
    private Integer awayChallengesLeft;
    private Integer awayChallengesLost;
    private Integer homeChallengesLeft;
    private Integer homeChallengesLost;
    private String status;
    private boolean delayed;
    private boolean live;
    private List<InningLine> innings = new ArrayList<>();

    public String getAwayName() { return awayName; }
    public void setAwayName(String awayName) { this.awayName = awayName; }
    public String getHomeName() { return homeName; }
    public void setHomeName(String homeName) { this.homeName = homeName; }
    public Integer getAwayRuns() { return awayRuns; }
    public void setAwayRuns(Integer awayRuns) { this.awayRuns = awayRuns; }
    public Integer getAwayHits() { return awayHits; }
    public void setAwayHits(Integer awayHits) { this.awayHits = awayHits; }
    public Integer getAwayErrors() { return awayErrors; }
    public void setAwayErrors(Integer awayErrors) { this.awayErrors = awayErrors; }
    public Integer getHomeRuns() { return homeRuns; }
    public void setHomeRuns(Integer homeRuns) { this.homeRuns = homeRuns; }
    public Integer getHomeHits() { return homeHits; }
    public void setHomeHits(Integer homeHits) { this.homeHits = homeHits; }
    public Integer getHomeErrors() { return homeErrors; }
    public void setHomeErrors(Integer homeErrors) { this.homeErrors = homeErrors; }
    public Integer getBalls() { return balls; }
    public void setBalls(Integer balls) { this.balls = balls; }
    public Integer getStrikes() { return strikes; }
    public void setStrikes(Integer strikes) { this.strikes = strikes; }
    public Integer getOuts() { return outs; }
    public void setOuts(Integer outs) { this.outs = outs; }
    public String getAtBat() { return atBat; }
    public void setAtBat(String atBat) { this.atBat = atBat; }
    public String getPitching() { return pitching; }
    public void setPitching(String pitching) { this.pitching = pitching; }
    public String getInningState() { return inningState; }
    public void setInningState(String inningState) { this.inningState = inningState; }
    public String getInningPhase() { return inningPhase; }
    public void setInningPhase(String inningPhase) { this.inningPhase = inningPhase; }
    public String getInningOrdinal() { return inningOrdinal; }
    public void setInningOrdinal(String inningOrdinal) { this.inningOrdinal = inningOrdinal; }
    public String getLastPlayEvent() { return lastPlayEvent; }
    public void setLastPlayEvent(String lastPlayEvent) { this.lastPlayEvent = lastPlayEvent; }
    public Integer getLastPlayIndex() { return lastPlayIndex; }
    public void setLastPlayIndex(Integer lastPlayIndex) { this.lastPlayIndex = lastPlayIndex; }
    public boolean isFinalGame() { return finalGame; }
    public void setFinalGame(boolean finalGame) { this.finalGame = finalGame; }
    public boolean isHasChallenges() { return hasChallenges; }
    public void setHasChallenges(boolean hasChallenges) { this.hasChallenges = hasChallenges; }
    public Integer getAwayChallengesLeft() { return awayChallengesLeft; }
    public void setAwayChallengesLeft(Integer v) { this.awayChallengesLeft = v; }
    public Integer getAwayChallengesLost() { return awayChallengesLost; }
    public void setAwayChallengesLost(Integer v) { this.awayChallengesLost = v; }
    public Integer getHomeChallengesLeft() { return homeChallengesLeft; }
    public void setHomeChallengesLeft(Integer v) { this.homeChallengesLeft = v; }
    public Integer getHomeChallengesLost() { return homeChallengesLost; }
    public void setHomeChallengesLost(Integer v) { this.homeChallengesLost = v; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isDelayed() { return delayed; }
    public void setDelayed(boolean delayed) { this.delayed = delayed; }

    /** Lamps to draw for a side: the standard pair, or more if a club still holds extra. */
    public int challengeSlots() {
        int a = awayChallengesLeft == null ? 0 : awayChallengesLeft;
        int h = homeChallengesLeft == null ? 0 : homeChallengesLeft;
        return Math.max(2, Math.max(a, h));
    }

    public boolean isOnFirst() { return onFirst; }
    public void setOnFirst(boolean onFirst) { this.onFirst = onFirst; }
    public boolean isOnSecond() { return onSecond; }
    public void setOnSecond(boolean onSecond) { this.onSecond = onSecond; }
    public boolean isOnThird() { return onThird; }
    public void setOnThird(boolean onThird) { this.onThird = onThird; }

    /** Between halves the board shows MID/END rather than a live count. */
    public boolean isBetweenInnings() {
        return "Middle".equalsIgnoreCase(inningPhase) || "End".equalsIgnoreCase(inningPhase);
    }

    /** "MID 5TH", "END 5TH", or plain "END" once the game is over. */
    public String getBreakCaption() {
        if (finalGame) return "END";
        if (!isBetweenInnings()) return null;
        String prefix = "Middle".equalsIgnoreCase(inningPhase) ? "MID" : "END";
        return inningOrdinal == null ? prefix : prefix + " " + inningOrdinal.toUpperCase();
    }

    public Integer getCurrentInning() { return currentInning; }
    public void setCurrentInning(Integer currentInning) { this.currentInning = currentInning; }
    public String getInningHalf() { return inningHalf; }
    public void setInningHalf(String inningHalf) { this.inningHalf = inningHalf; }
    public boolean isLive() { return live; }
    public void setLive(boolean live) { this.live = live; }
    public List<InningLine> getInnings() { return innings; }
    public void setInnings(List<InningLine> innings) { this.innings = innings; }

    /** True while the visiting team is batting, so only their frame lights up. */
    public boolean isTopHalf() { return inningHalf != null && inningHalf.toLowerCase().startsWith("top"); }

    /** True while the home team is batting. */
    public boolean isBottomHalf() { return inningHalf != null && inningHalf.toLowerCase().startsWith("bot"); }

    public static class InningLine {
        private int number;
        private Integer awayRuns;
        private Integer homeRuns;

        public InningLine(int number, Integer awayRuns, Integer homeRuns) {
            this.number = number;
            this.awayRuns = awayRuns;
            this.homeRuns = homeRuns;
        }

        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        public Integer getAwayRuns() { return awayRuns; }
        public void setAwayRuns(Integer awayRuns) { this.awayRuns = awayRuns; }
        public Integer getHomeRuns() { return homeRuns; }
        public void setHomeRuns(Integer homeRuns) { this.homeRuns = homeRuns; }
    }
}
