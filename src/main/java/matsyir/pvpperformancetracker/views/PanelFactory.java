package matsyir.pvpperformancetracker.views;

import matsyir.pvpperformancetracker.controllers.AnalyzedFightPerformance;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.models.FightLogEntry;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;

public final class PanelFactory
{
    // constants/final values
    public static final float LINE_INDEX_LABEL_FONT_SCALE = 0.65f;

    private static final Color STATS_LINE_INDEX_COLOR = new Color(255, 255, 255, 99);

    // static fields
    private static JFrame fightLogFrame; // save frame as static instance so there's only one at a time, to avoid window clutter.
    private static JFrame attackSummaryFrame; // save frame as static instance so there's only one at a time, to avoid window clutter.


    public static JPanel createStatsLine(String miniCenterLabelText, String miniCenterLabelTooltip,
                                         String leftText, String leftTooltip, Color leftColor,
                                         String rightText, String rightTooltip, Color rightColor)
    {
        JPanel statsLine = new JPanel(new GridBagLayout());
        statsLine.setBackground(null);

        String tooltipSuffix = "<br><br>" + (miniCenterLabelText + ": " + miniCenterLabelTooltip);

        ForwardingLabel leftLabel = new ForwardingLabel(leftText);
        leftLabel.setToolTipText("<html>" + leftTooltip + tooltipSuffix);
        leftLabel.setForeground(leftColor);
        leftLabel.setHorizontalAlignment(SwingConstants.LEFT);

        ForwardingLabel rightLabel = new ForwardingLabel(rightText);
        rightLabel.setToolTipText("<html>" + rightTooltip + tooltipSuffix);
        rightLabel.setForeground(rightColor);
        rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel indexLabel = new ForwardingLabel(miniCenterLabelText);
        indexLabel.setForeground(STATS_LINE_INDEX_COLOR);
        indexLabel.setFont(new Font("Monospace", Font.PLAIN,
                Math.round(indexLabel.getFont().getSize() * LINE_INDEX_LABEL_FONT_SCALE)));
        indexLabel.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // left
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        statsLine.add(leftLabel, gbc.clone());

        // right
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.EAST;
        statsLine.add(rightLabel, gbc.clone());

        // index/miniCenterLabel centered across full row
        GridBagConstraints gbcIndex = new GridBagConstraints();
        gbcIndex.gridx = 0;
        gbcIndex.gridy = 0;
        gbcIndex.gridwidth = 3;
        gbcIndex.anchor = GridBagConstraints.CENTER;
        gbcIndex.fill = GridBagConstraints.NONE;
        statsLine.add(indexLabel, gbcIndex);

        return statsLine;
    }


    // A JLabel that still receives mouse events (so its tooltip works) but forwards them to the parent
    // using SwingUtilities.convertMouseEvent(...) so the parent sees clicks/popup/hover as if the label
    // wasn't there.
    private static class ForwardingLabel extends JLabel
    {
        public ForwardingLabel(String text) {
            super(text);
            setOpaque(false);
            setFocusable(false);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            // allow label's normal handling (tooltips etc)
            super.processMouseEvent(e);
            forwardToParent(e);
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            // allow label's normal handling (tooltips)
            super.processMouseMotionEvent(e);
            forwardToParent(e); // forward so parent can show hover effects / track mouse
        }

        private void forwardToParent(MouseEvent e) {
            Container parent = getParent();
            if (parent == null) return;

            // convert to parent's coordinate system and dispatch there
            MouseEvent parentEvent = SwingUtilities.convertMouseEvent(this, e, parent);
            parent.dispatchEvent(parentEvent);
        }
    }

    public static JFrame createFightLogFrame(FightPerformance fight, AnalyzedFightPerformance analyzedFight, JRootPane rootPane)
    {
        // destroy current frame if it exists so we only have one at a time (static field)
        if (fightLogFrame != null)
        {
            fightLogFrame.dispose();
        }

        // show error modal if the fight has no log entries to display.
        ArrayList<FightLogEntry> fightLogEntries = new ArrayList<>(fight.getAllFightLogEntries());
        fightLogEntries.removeIf(e -> !e.isFullEntry());
        if (fightLogEntries.isEmpty())
        {
            PLUGIN.createConfirmationModal(false, "This fight has no attack logs to display, or the data is outdated.");
        }
        else if (analyzedFight != null) // if analyzed fight is set, then show an analyzed fight's fightLogFrame.
        {
            fightLogFrame = new FightLogFrame(analyzedFight, rootPane);

        }
        else
        {
            fightLogFrame = new FightLogFrame(fight,
                    fightLogEntries,
                    rootPane);
        }

        return fightLogFrame;
    }

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
}
