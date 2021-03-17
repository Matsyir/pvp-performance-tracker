package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.Image;
import javax.swing.JPanel;
import lombok.Getter;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPanel;
import matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin;
import net.runelite.client.ui.PluginPanel;

public abstract class TabContentPanel extends JPanel
{
	@Getter
	private String name;
	@Getter
	private Image tabIcon;
	@Getter
	private PluginPanel rootPluginPanel;

	public TabContentPanel(PluginPanel rootPluginPanel, String name, Image tabIcon)
	{
		super(new BorderLayout());

		// this should never happen "in production", only as a warning during development, if something is incomplete.
		if (rootPluginPanel == null || name == null || name.length() < 1)
		{
			throw new RuntimeException("Invalid TabContentPanel initialization");
		}

		this.rootPluginPanel = rootPluginPanel;
		this.name = name;
		this.tabIcon = tabIcon;
	}

	public void update()
	{
		this.validate();
		this.repaint();
		rootPluginPanel.updateUI();
	}

	public abstract void rebuild();
}
