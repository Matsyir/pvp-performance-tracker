package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

public class HiddenContentPanel extends JPanel
{
	private static final ImageIcon SECTION_EXPAND_ICON;
	private static final ImageIcon SECTION_EXPAND_ICON_HOVER;
	private static final ImageIcon SECTION_RETRACT_ICON;
	private static final ImageIcon SECTION_RETRACT_ICON_HOVER;
	static
	{
		BufferedImage sectionRetractIcon = ImageUtil.loadImageResource(HiddenContentPanel.class, "/util/arrow_right.png");
		sectionRetractIcon = ImageUtil.luminanceOffset(sectionRetractIcon, -121);
		SECTION_EXPAND_ICON = new ImageIcon(sectionRetractIcon);
		SECTION_EXPAND_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionRetractIcon, -100));
		final BufferedImage sectionExpandIcon = ImageUtil.rotateImage(sectionRetractIcon, Math.PI / 2);
		SECTION_RETRACT_ICON = new ImageIcon(sectionExpandIcon);
		SECTION_RETRACT_ICON_HOVER = new ImageIcon(ImageUtil.alphaOffset(sectionExpandIcon, -100));
	}

	private boolean isVisible;

	public HiddenContentPanel(String title, String titleTooltip, boolean initiallyVisible, JPanel content)
	{
		this.isVisible = initiallyVisible;
		content.setVisible(isVisible);

		setLayout(new BorderLayout());

		JPanel titlePanel = new JPanel(new BorderLayout());
		JLabel titleLabel = new JLabel(title);
		titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
		if (titleTooltip != null && titleTooltip.length() > 0)
		{
			titleLabel.setToolTipText(titleTooltip);
		}

		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		titlePanel.add(titleLabel, BorderLayout.NORTH);

		HiddenContentPanel that = this;
		titlePanel.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() != MouseEvent.BUTTON1) { return; }

				isVisible = !isVisible;
				content.setVisible(isVisible);
				titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON_HOVER : SECTION_EXPAND_ICON_HOVER);
				that.repaint();
			}


			@Override public void mouseEntered(MouseEvent e)
			{
				titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
				that.repaint();
			}
			@Override public void mouseExited(MouseEvent e)
			{
				titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
				that.repaint();
			}

			@Override public void mousePressed(MouseEvent e) { }
			@Override public void mouseReleased(MouseEvent e) { }
		});



		add(titlePanel, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}
}
