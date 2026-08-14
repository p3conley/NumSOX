package com.example.redsoxtracker.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The finished NumSOX estimate: the number on the front, and everything needed to justify
 * it behind the expandable sections.
 *
 * @param awayPct          away club's estimated chance, 0-100
 * @param homePct          home club's estimated chance, 0-100
 * @param confidence       High, Medium, Low, or Demo Data
 * @param confidenceReason why it landed at that level
 * @param mainReason       the headline explanation, in plain English
 * @param topFactors       the three categories that moved the number most
 * @param categories       every category, scored or not, for the comparison table
 * @param awayStrengths    what the away club does well here
 * @param awayWeaknesses   where the away club is exposed
 * @param homeStrengths    what the home club does well here
 * @param homeWeaknesses   where the home club is exposed
 * @param live             true once the game is under way
 * @param pregamePct       the pre-game estimate for the Red Sox side, kept for comparison
 * @param liveNote         what changed since first pitch, when live
 * @param availableCount   how many categories could be scored
 * @param totalCount       how many categories the model defines
 * @param generatedAt      when this estimate was produced
 */
public record NumsoxModel(
        int awayPct,
        int homePct,
        String confidence,
        String confidenceReason,
        String mainReason,
        List<CategoryScore> topFactors,
        List<CategoryScore> categories,
        List<String> awayStrengths,
        List<String> awayWeaknesses,
        List<String> homeStrengths,
        List<String> homeWeaknesses,
        boolean live,
        Integer pregamePct,
        String liveNote,
        int availableCount,
        int totalCount,
        LocalDateTime generatedAt
) {

    /** Shown wherever the estimate appears, so it is never mistaken for a betting line. */
    public static final String DISCLAIMER =
            "This is a custom NumSOX estimate based on baseball statistics, "
          + "not a betting line or a prediction of certainty.";

    public static final String TITLE = "NumSOX Estimated Win Probability";

    /** Categories that carried weight, for the breakdown view. */
    public List<CategoryScore> scoredCategories() {
        return categories.stream().filter(CategoryScore::available).toList();
    }

    /** Categories the model wanted but could not source, listed openly rather than hidden. */
    public List<CategoryScore> missingCategories() {
        return categories.stream().filter(c -> !c.available()).toList();
    }

    public boolean hasMissing() {
        return !missingCategories().isEmpty();
    }
}
