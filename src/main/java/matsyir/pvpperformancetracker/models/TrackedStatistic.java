package matsyir.pvpperformancetracker.models;

import lombok.Getter;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.controllers.Fighter;
import matsyir.pvpperformancetracker.views.PanelFactory;
import matsyir.pvpperformancetracker.views.TableComponent;
import net.runelite.client.ui.ColorScheme;

import javax.swing.JPanel;
import java.awt.Color;
import java.math.RoundingMode;
import java.security.InvalidParameterException;
import java.text.NumberFormat;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public enum TrackedStatistic
{
    OFF_PRAY("Off-pray", "OP",
            "Off-pray statistic, # of times you correctly used a different style than your opponent's overhead pray." +
                    "<br>For example, when you use melee vs. protect from magic, that's a successful off-pray hit."),
    AVG_DMG("Average damage", "aD", // Note: Previously referred to as 'deserved damage'
            "Average damage statistic, # of damage you would've dealt if the game had averaged rng."),
    DMG_DEALT("Damage dealt", "D",
            "Damage dealt statistic, sum of your actual damage hitsplats on your opponent."),
    MAGIC_HITS("Magic hits luck", "M",
            "Magic luck statistic, checks average hits vs. # of actual magic hits (as opposed to splashes)."),
    OFFENSIVE_PRAY("Offensive pray", "P",
            "Offensive prayer statistic, checks how many offensive prays you got right, e.g piety for melee."),
    HP_HEALED("HP healed", "HP",
            "HP healed statistic, # of HP recovered during the fight (from all sources)."),
    ROBE_HITS("Hits on robes", "rH",
            "Hits on robes statistic, # of times you got range/melee'd in robes (don't)."),
    KO_CHANCES("KO chances", "KO",
            "KO chance statistic, sums up the KO chances you got and gives you a total chance of KO." +
            "<br>When you spec your opponent on 20hp and hit a 12, this shows you the KO % chance in the fight log."),

    // NOTE: let's keep ghost barrage as the bottom-most statistic:
    // It's only relevant to people fighting in PvP Arena, and it's mostly only relevant
    // to people who can share their tracker with each-other - so pretty rarely useful.
    // ... also, more often than not it literally has 0 data. 0 hits, nothing. so let's just not show
    // this line when there is no data, since it's also the bottom row now, anyways.
    GHOST_BARRAGES("Ghost barrages", "GB",
            "Ghost-barrage statistic, for when you're animation stalled while barraging." +
                    "<br>This statistic is hidden if both players have 0gb, which is common." +
                    "<br>Weird and for advanced users only.")
    ;

    private static final String NO_DATA_SHORT = "-";
    private static final String NO_DATA = "N/A";

    // TODO ideally refactor these NFs into their own Util class or smth, we spam this NF stuff everywhere....
    private static final NumberFormat nf2 = NumberFormat.getInstance();
    private static final NumberFormat nfP1 = NumberFormat.getPercentInstance(); // For KO Chance %
    static
    {
        // initialize number format
        nf2.setMaximumFractionDigits(2);
        nf2.setRoundingMode(RoundingMode.HALF_UP);

        // initialize percent format
        nfP1.setMaximumFractionDigits(1);
        nfP1.setRoundingMode(RoundingMode.HALF_UP);

        // for every TrackedStatistic, init behaviors/functions/providers:
        // 1) Init/generate FightPerformancePanel line for this statistic
        // 2) Init/generate FightPerformanceOverlay line for this statistic (TableComponent)
        // 3) Update TableComponent from step 2 from ongoing fight data
        // these have to be static in order to self-reference themselves and use things like numberFormats
        OFF_PRAY.init(
                (fight, oppFight) -> PanelFactory.createStatsLine(
                        OFF_PRAY.acronym, OFF_PRAY.acronymTooltip
                        ,fight.competitor.getOffPrayStats()
                        ,(fight.competitor.getName() + " hit " + fight.competitor.getOffPraySuccessCount() + " successful off-pray attacks out of " +
                                fight.competitor.getAttackCount() + " total attacks (" +
                                nf2.format(fight.competitor.calculateOffPraySuccessPercentage()) + "%)")
                        ,fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE

                        ,fight.opponent.getOffPrayStats()
                        ,(fight.opponent.getName() + " hit " + fight.opponent.getOffPraySuccessCount() + " successful off-pray attacks out of " +
                                fight.opponent.getAttackCount() + " total attacks (" +
                                nf2.format(fight.opponent.calculateOffPraySuccessPercentage()) + "%)")
                        ,fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE
                ),
                () -> PanelFactory.createOverlayStatsLine(OFF_PRAY.acronym, 50, 50,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> component.updateLeftRightCells(
                            fight.getCompetitor().getOffPrayStats(true)
                            ,fight.competitorOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE
                            ,fight.getOpponent().getOffPrayStats(true)
                            ,fight.opponentOffPraySuccessIsGreater() ? Color.GREEN : Color.WHITE
                )
        );

        AVG_DMG.init(
                (fight, oppFight) -> PanelFactory.createStatsLine(AVG_DMG.acronym, AVG_DMG.acronymTooltip
                    ,fight.competitor.getAverageDmgString(fight.opponent)
                    ,("On average, " + fight.competitor.getName() + " would have dealt " + nf2.format(fight.competitor.getAvgDamage()) +
                            " damage, based on gear & overheads (" + fight.competitor.getAverageDmgString(fight.opponent, 1, true) + " vs opponent)")
                    ,fight.competitorAverageDmgIsGreater() ? Color.GREEN : Color.WHITE

                    ,fight.opponent.getAverageDmgString(fight.competitor)
                    ,("On average, " + fight.opponent.getName() + " would have dealt " + nf2.format(fight.opponent.getAvgDamage()) +
                            " damage, based on gear & overheads (" + fight.opponent.getAverageDmgString(fight.competitor, 1, true) + " vs you)")
                    ,fight.opponentAverageDmgIsGreater() ? Color.GREEN : Color.WHITE
                ),
                () -> PanelFactory.createOverlayStatsLine(AVG_DMG.acronym, 70, 30,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> component.updateLeftRightCells(
                    fight.getCompetitor().getAverageDmgString(fight.getOpponent())
                    ,fight.competitorAverageDmgIsGreater() ? Color.GREEN : Color.WHITE
                    ,String.valueOf((int)Math.round(fight.getOpponent().getAvgDamage()))
                    ,fight.opponentAverageDmgIsGreater() ? Color.GREEN : Color.WHITE
                )
        );

        DMG_DEALT.init(
                (fight, oppFight) -> PanelFactory.createStatsLine(DMG_DEALT.acronym, DMG_DEALT.acronymTooltip
                        ,fight.competitor.getDmgDealtString(fight.opponent)
                        ,fight.competitor.getName() + " dealt " + fight.competitor.getDamageDealt() +
                                " damage (" + fight.competitor.getDmgDealtString(fight.opponent, true) + " vs opponent)"
                        ,fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE

                        ,fight.opponent.getDmgDealtString(fight.competitor)
                        ,fight.opponent.getName() + " dealt " + fight.opponent.getDamageDealt() +
                                " damage (" + fight.opponent.getDmgDealtString(fight.competitor, true) + " vs you)"
                        ,fight.opponentAverageDmgIsGreater() ? Color.GREEN : Color.WHITE

                ),
                () -> PanelFactory.createOverlayStatsLine(DMG_DEALT.acronym, 70, 30,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> component.updateLeftRightCells(
                    String.valueOf(fight.getCompetitor().getDmgDealtString(fight.getOpponent()))
                    ,fight.competitorDmgDealtIsGreater() ? Color.GREEN : Color.WHITE
                    ,String.valueOf(fight.getOpponent().getDamageDealt())
                    ,fight.opponentDmgDealtIsGreater() ? Color.GREEN : Color.WHITE
		        )
        );

        MAGIC_HITS.init(
                (fight, oppFight) -> PanelFactory.createStatsLine(MAGIC_HITS.acronym, MAGIC_HITS.acronymTooltip
                        ,String.valueOf(fight.competitor.getMagicHitStats())
                        ,fight.competitor.getName() + " successfully hit " +
                                fight.competitor.getMagicHitCount() + " of " + fight.competitor.getMagicAttackCount() + " magic attacks, but would have hit " +
                                nf2.format(fight.competitor.getAvgMagicHitCount()) + " on average.<br>Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky"
                        ,fight.competitorMagicHitsLuckier() ? Color.GREEN : Color.WHITE

                        ,String.valueOf(fight.opponent.getMagicHitStats())
                        ,fight.opponent.getName() + " successfully hit " +
                                fight.opponent.getMagicHitCount() + " of " + fight.opponent.getMagicAttackCount() + " magic attacks, but would have hit " +
                                nf2.format(fight.opponent.getAvgMagicHitCount()) + " on average.<br>Luck percentage: 100% = expected hits, &gt;100% = lucky, &lt;100% = unlucky"
                        ,fight.opponentMagicHitsLuckier() ? Color.GREEN : Color.WHITE

                ),
                () -> PanelFactory.createOverlayStatsLine(MAGIC_HITS.acronym, 70, 30,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> component.updateLeftRightCells(
                        String.valueOf(fight.getCompetitor().getShortMagicHitStats())
                        ,fight.competitorMagicHitsLuckier() ? Color.GREEN : Color.WHITE
                        ,String.valueOf(fight.getOpponent().getShortMagicHitStats())
                        ,fight.opponentMagicHitsLuckier() ? Color.GREEN : Color.WHITE
                )
        );

        OFFENSIVE_PRAY.init(
                (fight, oppFight) -> { // returns FightPerformancePanel component
                    // OFFENSIVE PRAYS RIGHT: prepare opponent data if its available
                    String oppOffensivePrayStats = NO_DATA;
                    String oppOffensivePrayTooltip = "No data is available for the opponent's offensive prayers";
                    Color oppOffensivePrayColor = Color.WHITE;
                    if (oppFight != null) {
                        Fighter oppComp = oppFight.getCompetitor();

                        oppOffensivePrayStats = String.valueOf(oppComp.getOffensivePrayStats());
                        oppOffensivePrayTooltip = (oppComp.getName() + " did " + oppComp.getOffensivePraySuccessCount() + " successful offensive prayers out of " +
                                oppComp.getAttackCount() + " total attacks (" +
                                nf2.format(oppComp.calculateOffensivePraySuccessPercentage()) + "%)");
                        oppOffensivePrayColor = (
                                oppFight.getCompetitor().calculateOffensivePraySuccessPercentage() > fight.competitor.calculateOffensivePraySuccessPercentage()
                                        ? Color.GREEN : Color.WHITE);
                    }

                    // OFFENSIVE PRAYS: player's offensive pray stats (only player's, usually no data for opponent)
                    return PanelFactory.createStatsLine(OFFENSIVE_PRAY.acronym, OFFENSIVE_PRAY.acronymTooltip
                            ,String.valueOf(fight.competitor.getOffensivePrayStats())
                            ,(fight.competitor.getName() + " did " + fight.competitor.getOffensivePraySuccessCount() + " successful offensive prayers out of " +
                                    fight.competitor.getAttackCount() + " total attacks (" +
                                    nf2.format(fight.competitor.calculateOffensivePraySuccessPercentage()) + "%)")
                            ,((oppFight != null && fight.competitor.calculateOffensivePraySuccessPercentage() >
                                    oppFight.getCompetitor().calculateOffensivePraySuccessPercentage()) ?
                                    Color.GREEN : Color.WHITE)

                            ,oppOffensivePrayStats
                            ,oppOffensivePrayTooltip
                            ,oppOffensivePrayColor
                    );
                },
                () -> PanelFactory.createOverlayStatsLine(OFFENSIVE_PRAY.acronym, 80, 20,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> component.updateLeftCellText(String.valueOf(fight.getCompetitor().getOffensivePrayStats(true)))
        );

        HP_HEALED.init(
                (fight, oppFight) -> { // returns FightPerformancePanel component
                    // hp healed, right
                    String oppHpHealedStats = NO_DATA;
                    String oppHpHealedTooltip = "No data is available for the opponent's hp healed";
                    Color oppHpHealedColor = Color.WHITE;
                    if (oppFight != null) {
                        Fighter oppComp = oppFight.getCompetitor();

                        oppHpHealedStats = (String.valueOf(oppComp.getHpHealed()));
                        oppHpHealedTooltip = (oppComp.getName() + " recovered " + oppComp.getHpHealed() + " hitpoints during the fight");
                        oppHpHealedColor = (oppFight.getCompetitor().getHpHealed() > fight.competitor.getHpHealed() ? Color.GREEN : Color.WHITE);
                    }

                    // HP healed (only player's, no data for opponent usually)
                    return PanelFactory.createStatsLine(HP_HEALED.acronym, HP_HEALED.acronymTooltip
                            ,String.valueOf(fight.competitor.getHpHealed())
                            ,(fight.competitor.getName() + " recovered " + fight.competitor.getHpHealed() + " hitpoints during the fight")
                            ,((oppFight != null && fight.competitor.getHpHealed() > oppFight.getCompetitor().getHpHealed()) ?
                                    Color.GREEN : Color.WHITE)

                            ,oppHpHealedStats
                            ,oppHpHealedTooltip
                            ,oppHpHealedColor
                    );
                },
                () -> PanelFactory.createOverlayStatsLine(HP_HEALED.acronym, 80, 20,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> component.updateLeftCellText(String.valueOf(fight.getCompetitor().getHpHealed()))
        );

        ROBE_HITS.init(
                (fight, oppFight) -> { // returns FightPerformancePanel component
                    // Competitor's hits on robes
                    int compHits = fight.getCompetitor().getRobeHits();
                    int compTotal = fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
                    double compRatio = compTotal > 0 ? (double) compHits / compTotal : 0.0;
                    // Opponent's hits on robes
                    int oppHits = fight.getOpponent().getRobeHits();
                    int oppTotal = fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();
                    double oppRatio = oppTotal > 0 ? (double) oppHits / oppTotal : 0.0;

                    return PanelFactory.createStatsLine(ROBE_HITS.acronym, ROBE_HITS.acronymTooltip
                            ,(compHits + "/" + compTotal + " (" + nfP1.format(compRatio) + ")")
                            ,(fight.getCompetitor().getName() + " was hit with range/melee while wearing robes: " +
                                    compHits + "/" + compTotal + " (" + nfP1.format(compRatio) + ")<br>" +
                                    "In other words, of his opponent's " + compTotal + " range/melee attacks, " +
                                    fight.getCompetitor().getName() + " tanked " + compHits + " of them with robes.")
                            ,(compRatio < ((double) (fight.getOpponent().getRobeHits()) /
                                    Math.max(1, fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount())) ?
                                    Color.GREEN : Color.WHITE)

                            ,(oppHits + "/" + oppTotal + " (" + nfP1.format(oppRatio) + ")")
                            ,(fight.getOpponent().getName() + " was hit with range/melee while wearing robes: " +
                                    oppHits + "/" + oppTotal + " (" + nfP1.format(oppRatio) + ")<br>" +
                                    "In other words, of his opponent's " + oppTotal + " range/melee attacks, " +
                                    fight.getOpponent().getName() + " tanked " + oppHits + " of them with robes.")
                            ,(oppRatio < compRatio ? Color.GREEN : Color.WHITE)
                    );
                },
                () -> PanelFactory.createOverlayStatsLine(ROBE_HITS.acronym, 50, 50,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> {
                    int compHits = fight.getCompetitor().getRobeHits();
                    int compTotal = fight.getOpponent().getAttackCount() - fight.getOpponent().getTotalMagicAttackCount();
                    double compRatio = compTotal > 0 ? (double) compHits / compTotal : 0.0;
                    String compStr = compHits + "/" + compTotal;
                    component.updateLeftCell(compStr
                            ,compRatio < ((double) fight.getOpponent().getRobeHits() / Math.max(1, fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount())) ?
                                    Color.GREEN : Color.WHITE
                    );

                    int oppHits = fight.getOpponent().getRobeHits();
                    int oppTotal = fight.getCompetitor().getAttackCount() - fight.getCompetitor().getTotalMagicAttackCount();
                    double oppRatio = oppTotal > 0 ? (double) oppHits / oppTotal : 0.0;
                    String oppStr = oppHits + "/" + oppTotal;
                    component.updateRightCell(oppStr
                            ,oppRatio < compRatio ? Color.GREEN : Color.WHITE
                    );
                }
        );

        KO_CHANCES.init(
                (fight, oppFight) -> { // returns FightPerformancePanel component
                    // Total KO Chances
                    // Calculate total KO chances and overall probability // MODIFIED Calculation
                    int competitorKoChances = 0;
                    double competitorSurvivalProb = 1.0; // Start with 100% survival chance
                    int opponentKoChances = 0;
                    double opponentSurvivalProb = 1.0; // Start with 100% survival chance
                    List<FightLogEntry> logs = fight.getAllFightLogEntries();
                    for (FightLogEntry log : logs) {
                        Double koChance = log.getKoChance();
                        if (koChance != null) {
                            if (log.attackerName.equals(fight.competitor.getName())) {
                                competitorKoChances++;
                                competitorSurvivalProb *= (1.0 - koChance); // Calculate survival prob
                            } else {
                                opponentKoChances++;
                                opponentSurvivalProb *= (1.0 - koChance); // Calculate survival prob
                            }
                        }
                    }

                    // Calculate overall KO probability
                    Double competitorOverallKoProb = (competitorKoChances > 0) ? (1.0 - competitorSurvivalProb) : 0;
                    Double opponentOverallKoProb = (opponentKoChances > 0) ? (1.0 - opponentSurvivalProb) : 0;

                    String compTotalKoChanceText = competitorKoChances + (competitorOverallKoProb > 0 ? " (" + nfP1.format(competitorOverallKoProb) + ")" : ""); // Use overall prob
                    String oppTotalKoChanceText = opponentKoChances + (opponentOverallKoProb > 0 ? " (" + nfP1.format(opponentOverallKoProb) + ")" : ""); // Use overall prob

                    return PanelFactory.createStatsLine(KO_CHANCES.acronym, KO_CHANCES.acronymTooltip
                            ,compTotalKoChanceText
                            ,fight.competitor.getName() + " got " + competitorKoChances + " KO attempts with an overall KO probability of " + nfP1.format(competitorOverallKoProb)
                            ,(competitorOverallKoProb > opponentOverallKoProb ? Color.GREEN : Color.WHITE)

                            ,oppTotalKoChanceText
                            ,fight.opponent.getName() + " got " + opponentKoChances + " KO attempts with an overall KO probability of " + nfP1.format(opponentOverallKoProb)
                            ,(opponentOverallKoProb > competitorOverallKoProb ? Color.GREEN : Color.WHITE)
                    );
                },
                () -> PanelFactory.createOverlayStatsLine(KO_CHANCES.acronym, 50, 50,
                        NO_DATA_SHORT, Color.WHITE, NO_DATA_SHORT, Color.WHITE),
                (fight, component) -> {
                    if (fight.getCompetitorTotalKoChance() > 0)
                    {
                        component.updateLeftCellText(fight.getCompetitorKoChanceCount() +
                                " (" + nfP1.format(fight.getCompetitorTotalKoChance()) + ")");
                    }
                    if (fight.getOpponentTotalKoChance() > 0)
                    {
                        component.updateRightCellText(fight.getOpponentKoChanceCount() +
                                " (" + nfP1.format(fight.getOpponentTotalKoChance()) + ")");
                    }
                }
        );

        GHOST_BARRAGES.init(
                (fight, oppFight) -> { // returns FightPerformancePanel component
                    String oppGhostBarrageText = NO_DATA;
                    String oppGhostBarrageTooltipText = "No data is available for the opponent's ghost barrages";
                    Color oppGhostBarrageColor = ColorScheme.BRAND_ORANGE;

                    if (oppFight != null) {
                        Fighter oppComp = oppFight.getCompetitor();

                        oppGhostBarrageText = (oppComp.getGhostBarrageStats());
                        oppGhostBarrageTooltipText = ("(Advanced): " + oppComp.getName() + " hit " + oppComp.getGhostBarrageCount()
                                + " ghost barrages during the fight, worth an extra " + nf2.format(oppComp.getGhostBarrageAvgDamage())
                                + " average damage.<br>Unless fighting in PvP Arena, your opponent likely had a similar value.");
                        oppGhostBarrageColor = (oppFight.getCompetitor().getGhostBarrageAvgDamage() > fight.competitor.getGhostBarrageAvgDamage()
                                ? Color.GREEN : ColorScheme.BRAND_ORANGE);
                    }

                    return PanelFactory.createStatsLine(GHOST_BARRAGES.acronym, GHOST_BARRAGES.acronymTooltip
                            ,fight.competitor.getGhostBarrageStats()
                            ,("(Advanced): " + fight.competitor.getName() + " hit " + fight.competitor.getGhostBarrageCount()
                                    + " ghost barrages during the fight, worth an extra " + nf2.format(fight.competitor.getGhostBarrageAvgDamage())
                                    + " average damage.<br>Unless fighting in PvP Arena, your opponent likely had a similar value.")
                            ,((oppFight != null
                                    && fight.competitor.getGhostBarrageAvgDamage() > oppFight.getCompetitor().getGhostBarrageAvgDamage())
                                    ? Color.GREEN : ColorScheme.BRAND_ORANGE)

                            ,oppGhostBarrageText
                            ,oppGhostBarrageTooltipText
                            ,oppGhostBarrageColor
                    );
                },
                () -> PanelFactory.createOverlayStatsLine(GHOST_BARRAGES.acronym, 80, 20,
                        NO_DATA_SHORT, ColorScheme.BRAND_ORANGE, NO_DATA_SHORT, ColorScheme.BRAND_ORANGE),
                (fight, component) -> component.updateLeftCellText(fight.getCompetitor().getGhostBarrageStats())
        );

        // ensure all statistics have been initialized, or else plenty of things will break.
        for (TrackedStatistic stat : TrackedStatistic.values())
        {
            if (!stat.initialized)
            {
                throw new InvalidParameterException("TrackedStatistic: An enum value failed to be initialized.");
            }
        }
    }


    private boolean initialized = false;

    @Getter
    private String name;
    @Getter
    private String acronym;
    @Getter
    private String acronymTooltip;

    private BiFunction<FightPerformance, FightPerformance, JPanel> getPanelComponent;
    public JPanel getPanelComponent(FightPerformance f, FightPerformance oppFight) { return this.getPanelComponent.apply(f, oppFight); }

    private Supplier<TableComponent> getOverlayComponent;
    public TableComponent getOverlayComponent() { return this.getOverlayComponent.get(); }

    private BiConsumer<FightPerformance, TableComponent> updateOverlayComponent;
    public void updateOverlayComponent(FightPerformance f, TableComponent t) { this.updateOverlayComponent.accept(f, t); }

    TrackedStatistic(String name, String acronym, String acronymTooltip)
    {
        this.name = name;
        this.acronym = acronym;
        this.acronymTooltip = acronymTooltip;

    }
    private void init(BiFunction<FightPerformance, FightPerformance, JPanel> getPanelComponent,
                      Supplier<TableComponent> getOverlayComponent,
                      BiConsumer<FightPerformance, TableComponent> updateOverlayComponent)
    {
        this.getPanelComponent = getPanelComponent;
        this.getOverlayComponent = getOverlayComponent;
        this.updateOverlayComponent = updateOverlayComponent;
        this.initialized = true;
    }
}
