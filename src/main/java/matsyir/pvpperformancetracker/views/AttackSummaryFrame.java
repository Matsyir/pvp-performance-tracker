package matsyir.pvpperformancetracker.views;

import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.models.AnimationData;
import matsyir.pvpperformancetracker.models.FightLogEntry;
import net.runelite.api.kit.KitType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;

@Slf4j
public class AttackSummaryFrame extends JFrame
{
    private static JFrame attackSummaryFrame; // save frame as static instance so there's only one at a time, to avoid window clutter.

    private final class GroupedWepAnim
    {
        private final AnimationData anim;
        private final int wepId;
        private GroupedWepAnim(AnimationData anim, int wepId)
        {
            this.anim = anim;
            this.wepId = wepId;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupedWepAnim that = (GroupedWepAnim) o;
            return Objects.equals(anim, that.anim) &&
                    Objects.equals(wepId, that.wepId);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(anim, wepId);
        }
    }

    //BorderLayout container;
    Map<GroupedWepAnim, Integer> countsOfAttacksCompetitor = new HashMap<>();
    int competitorMeleeCount = 0;
    int competitorRangeCount = 0;
    int competitorMageCount = 0;
    Map<GroupedWepAnim, Integer> countsOfAttacksOpponent = new HashMap<>();
    int opponentMeleeCount = 0;
    int opponentRangeCount = 0;
    int opponentMageCount = 0;

    public static JFrame createAttackSummaryFrame(FightPerformance fight, JRootPane rootPane)
    {
        // destroy current frame if it exists so we only have one at a time (static field)
        if (attackSummaryFrame != null)
        {
            attackSummaryFrame.dispose();
        }
        // show error modal if the fight has no log entries to display.
        ArrayList<FightLogEntry> fightLogEntries = new ArrayList<>(fight.getAllFightLogEntries());
        fightLogEntries.removeIf(e -> !e.isFullEntry());
        if (fightLogEntries.isEmpty())
        {
            PLUGIN.createConfirmationModal(false, "This fight has no attack summary to display, or the data is outdated.");
            return attackSummaryFrame;
        }

        attackSummaryFrame = new AttackSummaryFrame(fight, fightLogEntries, rootPane);

        return attackSummaryFrame;
    }

    private AttackSummaryFrame(FightPerformance fight, ArrayList<FightLogEntry> fightLogEntries, JRootPane rootPane)
    {
        super("Attack Summary - " + fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName()
                + " on world " + fight.getWorld());

        // if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
        // on top as well so it can be above the client.
        if (isAlwaysOnTopSupported())
        {
            setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
        }

        setIconImage(PLUGIN_ICON);
        setSize(503, 503);
        setLocation(rootPane.getLocationOnScreen());

        // first, populate countsOfAttacks that will be displayed
        for (FightLogEntry entry : fightLogEntries)
        {
            GroupedWepAnim thisAttack = new GroupedWepAnim(entry.getAnimationData(), entry.getAttackerGear()[KitType.WEAPON.getIndex()]);
            boolean isCompetitor = entry.attackerName.equals(fight.getCompetitor().getName());
            if (isCompetitor)
            {
                if (!countsOfAttacksCompetitor.containsKey(thisAttack))
                {
                    countsOfAttacksCompetitor.put(thisAttack, 1);
                }
                else
                {
                    countsOfAttacksCompetitor.put(thisAttack, countsOfAttacksCompetitor.get(thisAttack) + 1);
                }

                if (entry.getAnimationData() != null)
                {
                    competitorMeleeCount += entry.getAnimationData().attackStyle.isMelee() ? 1 : 0;
                    competitorRangeCount += entry.getAnimationData().attackStyle == AnimationData.AttackStyle.RANGED ? 1 : 0;
                    competitorMageCount += entry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC ? 1 : 0;
                }
            }
            else
            {
                if (!countsOfAttacksOpponent.containsKey(thisAttack))
                {
                    countsOfAttacksOpponent.put(thisAttack, 1);
                }
                else
                {
                    countsOfAttacksOpponent.put(thisAttack, countsOfAttacksOpponent.get(thisAttack) + 1);
                }

                if (entry.getAnimationData() != null)
                {
                    opponentMeleeCount += entry.getAnimationData().attackStyle.isMelee() ? 1 : 0;
                    opponentRangeCount += entry.getAnimationData().attackStyle == AnimationData.AttackStyle.RANGED ? 1 : 0;
                    opponentMageCount += entry.getAnimationData().attackStyle == AnimationData.AttackStyle.MAGIC ? 1 : 0;
                }
            }
        }

        // sort attacks by count
        countsOfAttacksCompetitor = countsOfAttacksCompetitor.entrySet().stream()
                .sorted(Entry.<GroupedWepAnim, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
        countsOfAttacksOpponent = countsOfAttacksOpponent.entrySet().stream()
                .sorted(Entry.<GroupedWepAnim, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));


        JLabel competitorNameAndInfo = new JLabel();
        competitorNameAndInfo.setText("<html><strong>" + fight.getCompetitor().getName() + "</strong><br>" +
                competitorMeleeCount + " Melee Attack(s)<br>" +
                competitorRangeCount + " Ranged Attack(s)<br>" +
                competitorMageCount + " Magic Attack(s)");
        competitorNameAndInfo.setForeground(Color.WHITE);

        JLabel opponentNameAndInfo = new JLabel();
        opponentNameAndInfo.setText("<html><strong>" + fight.getOpponent().getName() + "</strong><br>" +
                opponentMeleeCount + " Melee Attack(s)<br>" +
                opponentRangeCount + " Ranged Attack(s)<br>" +
                opponentMageCount + " Magic Attack(s)");
        opponentNameAndInfo.setForeground(Color.WHITE);

        JPanel leftContainer = new JPanel();
        leftContainer.setLayout(new BoxLayout(leftContainer, BoxLayout.Y_AXIS));
        leftContainer.add(competitorNameAndInfo);

        JPanel rightContainer = new JPanel();
        rightContainer.setLayout(new BoxLayout(rightContainer, BoxLayout.Y_AXIS));
        rightContainer.add(opponentNameAndInfo);

        countsOfAttacksCompetitor.forEach((key, val) ->
        {
            JLabel attackLine = new JLabel(val + "x: " + key.anim.toString());
            PLUGIN.addItemToLabelIfValid(attackLine, key.wepId);

            leftContainer.add(attackLine);
        });
        countsOfAttacksOpponent.forEach((key, val) ->
        {
            JLabel attackLine = new JLabel(val + "x: " + key.anim.toString());
            PLUGIN.addItemToLabelIfValid(attackLine, key.wepId);

            rightContainer.add(attackLine);
        });

        setLayout(new BorderLayout());
        this.add(leftContainer, BorderLayout.WEST);
        this.add(rightContainer, BorderLayout.EAST);

        setVisible(true);
    }
}
