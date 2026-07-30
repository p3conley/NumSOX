package com.example.redsoxtracker.dto;

import java.util.List;

public class WinProbabilityResult {

    private int awayWinPct;
    private int homeWinPct;
    private String confidenceLevel;

    // Category edges
    private String offenseEdge;
    private String starterEdge;
    private String bullpenEdge;
    private String defenseEdge;
    private String recentFormEdge;
    private String ballparkEdge;

    // Human-readable category row data
    private List<CategoryComparison> categories;

    // Plain-English explanations
    private List<String> mainReasons;
    private List<String> parkHighlights;

    // Strengths and weaknesses
    private List<String> awayStrengths;
    private List<String> awayWeaknesses;
    private List<String> homeStrengths;
    private List<String> homeWeaknesses;

    // Ballpark narrative
    private String ballparkNarrative;
    private String modelSummary;
    private HistoricalCalibration historicalCalibration;

    public WinProbabilityResult() {}

    public int getAwayWinPct() { return awayWinPct; }
    public void setAwayWinPct(int awayWinPct) { this.awayWinPct = awayWinPct; }
    public int getHomeWinPct() { return homeWinPct; }
    public void setHomeWinPct(int homeWinPct) { this.homeWinPct = homeWinPct; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    public String getOffenseEdge() { return offenseEdge; }
    public void setOffenseEdge(String offenseEdge) { this.offenseEdge = offenseEdge; }
    public String getStarterEdge() { return starterEdge; }
    public void setStarterEdge(String starterEdge) { this.starterEdge = starterEdge; }
    public String getBullpenEdge() { return bullpenEdge; }
    public void setBullpenEdge(String bullpenEdge) { this.bullpenEdge = bullpenEdge; }
    public String getDefenseEdge() { return defenseEdge; }
    public void setDefenseEdge(String defenseEdge) { this.defenseEdge = defenseEdge; }
    public String getRecentFormEdge() { return recentFormEdge; }
    public void setRecentFormEdge(String recentFormEdge) { this.recentFormEdge = recentFormEdge; }
    public String getBallparkEdge() { return ballparkEdge; }
    public void setBallparkEdge(String ballparkEdge) { this.ballparkEdge = ballparkEdge; }
    public List<CategoryComparison> getCategories() { return categories; }
    public void setCategories(List<CategoryComparison> categories) { this.categories = categories; }
    public List<String> getMainReasons() { return mainReasons; }
    public void setMainReasons(List<String> mainReasons) { this.mainReasons = mainReasons; }
    public List<String> getParkHighlights() { return parkHighlights; }
    public void setParkHighlights(List<String> parkHighlights) { this.parkHighlights = parkHighlights; }
    public List<String> getAwayStrengths() { return awayStrengths; }
    public void setAwayStrengths(List<String> awayStrengths) { this.awayStrengths = awayStrengths; }
    public List<String> getAwayWeaknesses() { return awayWeaknesses; }
    public void setAwayWeaknesses(List<String> awayWeaknesses) { this.awayWeaknesses = awayWeaknesses; }
    public List<String> getHomeStrengths() { return homeStrengths; }
    public void setHomeStrengths(List<String> homeStrengths) { this.homeStrengths = homeStrengths; }
    public List<String> getHomeWeaknesses() { return homeWeaknesses; }
    public void setHomeWeaknesses(List<String> homeWeaknesses) { this.homeWeaknesses = homeWeaknesses; }
    public String getBallparkNarrative() { return ballparkNarrative; }
    public void setBallparkNarrative(String ballparkNarrative) { this.ballparkNarrative = ballparkNarrative; }
    public String getModelSummary() { return modelSummary; }
    public void setModelSummary(String modelSummary) { this.modelSummary = modelSummary; }
    public HistoricalCalibration getHistoricalCalibration() { return historicalCalibration; }
    public void setHistoricalCalibration(HistoricalCalibration historicalCalibration) { this.historicalCalibration = historicalCalibration; }
}
