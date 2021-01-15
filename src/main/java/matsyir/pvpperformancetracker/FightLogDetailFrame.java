package matsyir.pvpperformancetracker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.api.HeadIcon;
import net.runelite.api.SpriteID;
import net.runelite.http.api.item.ItemEquipmentStats;

class FightLogDetailFrame extends JFrame
{
	public static final int DEFAULT_WIDTH = 400;
	public int rowIdx;
	FightLogDetailFrame(FightPerformance fight, FightLogEntry log, int rowIdx, Point location)
	{
		super(fight.getCompetitor().getName() + " vs " + fight.getOpponent().getName() + " - Log Details");

		this.rowIdx = rowIdx;

		Dimension size = new Dimension(DEFAULT_WIDTH, 503);
		setSize(size);
		setMinimumSize(size);
		setLayout(new BorderLayout());
		//setLayout(new GridLayout(2, 2));
		setLocation(location);

		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported())
		{
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		JPanel mainPanel = new JPanel(); //new GridLayout(0, 2)
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setVisible(true);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

		ItemEquipmentStats attackerStats = PvpDamageCalc.calculateBonusesToStats(log.getAttackerGear());
		ItemEquipmentStats defenderStats = PvpDamageCalc.calculateBonusesToStats(log.getDefenderGear());

		boolean isCompetitorLog = log.attackerName.equals(fight.getCompetitor().getName());
		Fighter attacker = isCompetitorLog ? fight.getCompetitor() : fight.getOpponent();
		Fighter defender = isCompetitorLog ? fight.getOpponent() : fight.getCompetitor();

		JPanel namesLine = new JPanel(new BorderLayout());
		JLabel attackerName = new JLabel();
		attackerName.setText("<html>Attacker:<br/>" + log.attackerName + "</html>");
		attackerName.setHorizontalAlignment(SwingConstants.CENTER);
		namesLine.add(attackerName, BorderLayout.WEST);

		JLabel defenderName = new JLabel();
		defenderName.setText("Defender: " + defender.getName());
		defenderName.setHorizontalAlignment(SwingConstants.CENTER);
		namesLine.add(defenderName, BorderLayout.EAST);


		JPanel equipmentStatsLine = new JPanel(new BorderLayout());
		JLabel attackerStatsLabel = new JLabel();
		attackerStatsLabel.setText(getItemEquipmentStatsString(attackerStats));
		equipmentStatsLine.add(attackerStatsLabel, BorderLayout.WEST);

		JLabel defenderStatsLabel = new JLabel();
		defenderStatsLabel.setText(getItemEquipmentStatsString(defenderStats));
		equipmentStatsLine.add(defenderStatsLabel, BorderLayout.EAST);


		JPanel overheadPrayLine = new JPanel(new BorderLayout());
		int attackerOverheadSpriteId = PLUGIN.getSpriteForHeadIcon(log.getAttackerOverhead());
		if (attackerOverheadSpriteId > 0)
		{
			ImageIcon icon = new ImageIcon(PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(attackerOverheadSpriteId, 0));
			JLabel prayIconLabel = new JLabel(icon);
			overheadPrayLine.add(prayIconLabel, BorderLayout.WEST);
		}

		int defenderOverheadSpriteId = PLUGIN.getSpriteForHeadIcon(log.getDefenderOverhead());
		if (defenderOverheadSpriteId > 0)
		{
			ImageIcon icon = new ImageIcon(PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(defenderOverheadSpriteId, 0));
			JLabel prayIconLabel = new JLabel(icon);
			overheadPrayLine.add(prayIconLabel, BorderLayout.EAST);
		}

		JPanel offensivePrayLine = new JPanel(new BorderLayout());
		if (log.getAttackerOffensivePray() > 0)
		{
			ImageIcon icon = new ImageIcon(PvpPerformanceTrackerPlugin.SPRITE_MANAGER.getSprite(log.getAttackerOffensivePray(), 0));
			JLabel prayIconLabel = new JLabel(icon);
			offensivePrayLine.add(prayIconLabel, BorderLayout.WEST);
		}


		JPanel animationDetectedLine = new JPanel(new BorderLayout());
		JLabel attackerAnimationDetected = new JLabel();
		attackerAnimationDetected.setText("Animation Detected: " + log.getAnimationData().toString());
		attackerAnimationDetected.setToolTipText("<html>Note that the animation can seem misleading, as many animations are re-used, but this is normal.<br/>For example, Zammy Hasta and Staff of Fire use the same crush animation.</html>");
		attackerAnimationDetected.setHorizontalAlignment(SwingConstants.CENTER);
		animationDetectedLine.add(attackerAnimationDetected, BorderLayout.CENTER);


		mainPanel.add(namesLine);
		mainPanel.add(equipmentStatsLine);
		mainPanel.add(overheadPrayLine);
		mainPanel.add(offensivePrayLine);

		mainPanel.add(attackerAnimationDetected);

		this.add(mainPanel, BorderLayout.CENTER);
		this.setVisible(true);
	}

	String getItemEquipmentStatsString(ItemEquipmentStats stats)
	{
		String sep = "<br/>&nbsp;&nbsp;";
		return "<html><strong>Attack bonus</strong>" + sep +
			"Stab: " + prependPlusIfPositive(stats.getAstab()) + sep +
			"Slash: " + prependPlusIfPositive(stats.getAslash()) + sep +
			"Crush: " + prependPlusIfPositive(stats.getAcrush()) + sep +
			"Magic: " + prependPlusIfPositive(stats.getAmagic()) + sep +
			"Range: " + prependPlusIfPositive(stats.getArange()) +
			"<br/><strong>Defence bonus</strong>" + sep +
			"Stab: " + prependPlusIfPositive(stats.getDstab()) + sep +
			"Slash: " + prependPlusIfPositive(stats.getDslash()) + sep +
			"Crush: " + prependPlusIfPositive(stats.getDcrush()) + sep +
			"Magic: " + prependPlusIfPositive(stats.getDmagic()) + sep +
			"Range: " + prependPlusIfPositive(stats.getDrange()) +
			"<br/><strong>Other bonuses</strong>" + sep +
			"Melee strength: " + prependPlusIfPositive(stats.getStr()) + sep +
			"Ranged strength: " + prependPlusIfPositive(stats.getRstr()) + sep +
			"Magic damage: " + prependPlusIfPositive(stats.getMdmg()) + sep +
			"</html>";
	}

	String prependPlusIfPositive(int number)
	{
		if (number >= 0)
		{
			return "+" + number;
		}

		return String.valueOf(number);
	}
}
