package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.Image;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

public class InformationTabPanel extends TabContentPanel
{
	// core interface fields
	private static String NAME = "Info / Help";
	private static Image TAB_ICON = ImageUtil.loadImageResource(InformationTabPanel.class, "/help.png");

	// The main fight history container, this will hold all the individual FightPerformancePanels.
	private final JPanel contentContainer = new JPanel();

	public InformationTabPanel(PluginPanel rootPluginPanel)
	{
		super(rootPluginPanel, NAME, TAB_ICON);

		contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
		contentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentContainer.add(new JLabel(String.format("<html><body style=\"text-align: justify; text-justify: inter-word;\">%s</body></html>", "Thanks for using the PvP Performance tracker. <strong>It's not perfect</strong>, but it is good enough to determine if you lost due to bad RNG, and find ways you can improve. Here are some details on how it works and how you can interpret the stats.")));


		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(new JLabel("hey its me a test"));
		contentContainer.add(new HiddenContentPanel("Testing?", "stuff", false, contentPanel));

		JPanel secondContentPanel = new JPanel(new BorderLayout());
		secondContentPanel.add(new JLabel("hey its me another test"));
		contentContainer.add(new HiddenContentPanel("Testing2?", "stuff2", false, secondContentPanel));

		add(contentContainer, BorderLayout.CENTER);
	}

	// TODO maybe this isnt required for TabContentPanel...?
	public void rebuild()
	{

	}
}
