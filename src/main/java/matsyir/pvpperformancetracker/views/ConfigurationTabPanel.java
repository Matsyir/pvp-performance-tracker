package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.Image;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

public class ConfigurationTabPanel extends TabContentPanel
{
	// core interface fields
	private static String NAME = "Configuration";
	private static Image TAB_ICON = ImageUtil.loadImageResource(ConfigurationTabPanel.class, "/configure.png");

	// The main fight history container, this will hold all the individual FightPerformancePanels.
	private final JPanel contentContainer = new JPanel();

	public ConfigurationTabPanel(PluginPanel rootPluginPanel)
	{
		super(rootPluginPanel, NAME, TAB_ICON);

		contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
		contentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentContainer.add(new JLabel("wow"));

		add(contentContainer, BorderLayout.CENTER);
	}

	// TODO maybe this isnt required for TabContentPanel...?
	public void rebuild()
	{

	}
}
