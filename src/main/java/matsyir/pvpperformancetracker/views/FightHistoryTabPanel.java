package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

public class FightHistoryTabPanel extends TabContentPanel
{
	// core interface fields
	private static String NAME = "Fight History";
	private static Image TAB_ICON = ImageUtil.loadImageResource(FightHistoryTabPanel.class, "/listing.png");

	// The main fight history container, this will hold all the individual FightPerformancePanels.
	private final JPanel fightHistoryContainer = new JPanel();
	private final TotalStatsPanel totalStatsPanel = new TotalStatsPanel();

	public FightHistoryTabPanel(PluginPanel rootPluginPanel)
	{
		super(rootPluginPanel, NAME, TAB_ICON);

		fightHistoryContainer.setLayout(new BoxLayout(fightHistoryContainer, BoxLayout.Y_AXIS));
		fightHistoryContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// TODO hmmmmmmmmmmmmmmm this var name aint it but it is what it is
		JPanel fightHistoryContainerContainer = new JPanel(new BorderLayout());
		// wrap mainContent with scrollpane so it has a scrollbar
		JScrollPane scrollableContainer = new JScrollPane(fightHistoryContainerContainer);
		scrollableContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollableContainer.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
		fightHistoryContainerContainer.add(fightHistoryContainer, BorderLayout.NORTH);

		add(totalStatsPanel, BorderLayout.NORTH);
		add(fightHistoryContainerContainer, BorderLayout.CENTER);
	}

	public void addFight(FightPerformance fight)
	{
		totalStatsPanel.addFight(fight);

		SwingUtilities.invokeLater(() ->
		{
			fightHistoryContainer.add(new FightPerformancePanel(fight), 0);
			update();
		});
	}

	public void addFights(ArrayList<FightPerformance> fights)
	{
		totalStatsPanel.addFights(fights);
		SwingUtilities.invokeLater(() ->
		{
			fights.forEach((FightPerformance f) -> fightHistoryContainer.add(new FightPerformancePanel(f), 0));
			update();
		});
	}

	public void rebuild()
	{
		totalStatsPanel.reset();
		fightHistoryContainer.removeAll();

		if (PLUGIN.fightHistory.size() > 0)
		{
			addFights(PLUGIN.fightHistory);
		}
		else
		{
			SwingUtilities.invokeLater(() ->
			{
				update();
			});
		}
	}

	public void setConfigWarning(boolean enable)
	{
		totalStatsPanel.setConfigWarning(enable);
	}

}
