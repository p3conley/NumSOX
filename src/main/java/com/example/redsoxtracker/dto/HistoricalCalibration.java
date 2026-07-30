package com.example.redsoxtracker.dto;

import java.util.List;

public class HistoricalCalibration {

    private final int sampleSize;
    private final int correctPredictions;
    private final double accuracy;
    private final double brierScore;
    private final int rawRedSoxPct;
    private final int calibratedRedSoxPct;
    private final int adjustmentPoints;
    private final List<String> notes;

    public HistoricalCalibration(int sampleSize, int correctPredictions, double accuracy, double brierScore,
                                 int rawRedSoxPct, int calibratedRedSoxPct, int adjustmentPoints,
                                 List<String> notes) {
        this.sampleSize = sampleSize;
        this.correctPredictions = correctPredictions;
        this.accuracy = accuracy;
        this.brierScore = brierScore;
        this.rawRedSoxPct = rawRedSoxPct;
        this.calibratedRedSoxPct = calibratedRedSoxPct;
        this.adjustmentPoints = adjustmentPoints;
        this.notes = notes;
    }

    public int getSampleSize() { return sampleSize; }
    public int getCorrectPredictions() { return correctPredictions; }
    public double getAccuracy() { return accuracy; }
    public double getBrierScore() { return brierScore; }
    public int getRawRedSoxPct() { return rawRedSoxPct; }
    public int getCalibratedRedSoxPct() { return calibratedRedSoxPct; }
    public int getAdjustmentPoints() { return adjustmentPoints; }
    public List<String> getNotes() { return notes; }
}
