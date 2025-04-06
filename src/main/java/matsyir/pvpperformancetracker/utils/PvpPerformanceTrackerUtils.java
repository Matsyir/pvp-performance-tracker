package matsyir.pvpperformancetracker.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PvpPerformanceTrackerUtils {
    /**
     * Calculates the chance of knocking out an opponent with a single hit.
     *
     * @param accuracy            The attacker's accuracy (0.0 to 1.0).
     * @param minHit              The attacker's minimum possible hit.
     * @param maxHit              The attacker's maximum possible hit.
     * @param estimatedOpponentHp The estimated HP of the opponent before the hit.
     * @return The KO chance (0.0 to 1.0), or null if the max hit is less than the
     *         opponent's HP.
     */
    public static Double calculateKoChance(double accuracy, int minHit, int maxHit, int estimatedOpponentHp) {
        // Cannot KO if max hit is less than opponent's current HP.
        if (maxHit < estimatedOpponentHp || estimatedOpponentHp <= 0) {
            return null;
        }

        // Ensure minHit is not greater than maxHit (shouldn't happen, but safety check)
        // and not negative.
        minHit = Math.max(0, Math.min(minHit, maxHit));

        int totalPossibleHits = maxHit - minHit + 1;
        if (totalPossibleHits <= 0) {
            // Avoid division by zero if minHit somehow exceeds maxHit after clamping.
            log.warn("Calculated totalPossibleHits <= 0 (min: {}, max: {})", minHit, maxHit);
            return null;
        }

        // Number of hits that would result in a KO (damage >= opponent's HP).
        // Clamp estimatedOpponentHp to be at least minHit for calculation,
        // otherwise koHits could be larger than totalPossibleHits.
        int effectiveHpForCalc = Math.max(minHit, estimatedOpponentHp);
        int koHits = maxHit - effectiveHpForCalc + 1;

        // Ensure koHits is not negative (if effectiveHpForCalc > maxHit somehow)
        koHits = Math.max(0, koHits);

        // KO chance = Accuracy * (Number of KO hits / Total possible hits)
        double chance = accuracy * ((double) koHits / totalPossibleHits);

        // Clamp chance between 0 and 1 (due to potential floating point inaccuracies)
        return Math.max(0.0, Math.min(chance, 1.0));
    }
}
