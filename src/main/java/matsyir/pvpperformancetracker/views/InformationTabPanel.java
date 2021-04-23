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
		contentContainer.add(new JLabel("<html>wowowowow hmmmm<br><br>TEST</html>"));

		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(new JLabel("wow this is a hidden label very cool wow wow hmmmm wow test hmm aksjdasd ajksdjkahsd kjasdhjka"));
		contentContainer.add(new HiddenContentPanel("Testing?", "stuff", true, contentPanel));

		add(contentContainer, BorderLayout.CENTER);
	}

	// TODO maybe this isnt required for TabContentPanel...?
	public void rebuild()
	{

	}
}
