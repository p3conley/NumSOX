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
    private String inningState;
    /** The frame currently being played; null once the game is over. */
    private Integer currentInning;
    /** "Top" or "Bottom" for the half being played. */
    private String inningHalf;
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
    public String getInningState() { return inningState; }
    public void setInningState(String inningState) { this.inningState = inningState; }
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
