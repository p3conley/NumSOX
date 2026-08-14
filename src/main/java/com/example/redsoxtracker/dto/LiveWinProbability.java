package com.example.redsoxtracker.dto;

/**
 * Win probability as it stands right now, from MLB's own per-play model.
 *
 * <p>The pre-game NumSOX model answers "who should win tonight". This answers "who is
 * winning as of this pitch", which is a different question and only has an answer once
 * the game is under way, so the two are kept apart rather than blended.</p>
 *
 * @param awayPct        away club's chance, 0-100
 * @param homePct        home club's chance, 0-100
 * @param leverageIndex  how much the current spot matters; 1.0 is an average situation
 * @param lastPlay       the play these numbers were computed after
 * @param swing          percentage points the last play moved the home club, signed
 * @param plays          how many plays the feed has scored so far
 */
public record LiveWinProbability(
        int awayPct,
        int homePct,
        Double leverageIndex,
        String lastPlay,
        Double swing,
        int plays
) {

    /** Leverage worth calling out; MLB treats anything past 2.0 as high leverage. */
    public boolean isHighLeverage() {
        return leverageIndex != null && leverageIndex >= 2.0;
    }

    /** A swing worth calling out, in percentage points. */
    public boolean isBigSwing() {
        return swing != null && Math.abs(swing) >= 10.0;
    }
}
