package matsyir.pvpperformancetracker.views;

import java.awt.BorderLayout;
import java.awt.TextArea;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;
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

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel titleLabel = new JLabel(title);
		titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
		if (titleTooltip != null && titleTooltip.length() > 0)
		{
			titleLabel.setToolTipText(titleTooltip);
		}

		JPanel contentContainer = new JPanel(new BorderLayout());
		contentContainer.setBorder(new LineBorder(ColorScheme.DARKER_GRAY_HOVER_COLOR));
		contentContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		content.setBackground(null);
		contentContainer.add(content, BorderLayout.CENTER);
		contentContainer.setVisible(isVisible);


		HiddenContentPanel that = this;
		MouseListener toggleContentOnClick = new MouseListener()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() != MouseEvent.BUTTON1) { return; }

				isVisible = !isVisible;
				contentContainer.setVisible(isVisible);
				titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON_HOVER : SECTION_EXPAND_ICON_HOVER);
				that.validate();
			}


			@Override public void mouseEntered(MouseEvent e)
			{
				titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON_HOVER : SECTION_EXPAND_ICON_HOVER);
				that.validate();
			}
			@Override public void mouseExited(MouseEvent e)
			{
				titleLabel.setIcon(isVisible ? SECTION_RETRACT_ICON : SECTION_EXPAND_ICON);
				that.validate();
			}

			@Override public void mousePressed(MouseEvent e) { }
			@Override public void mouseReleased(MouseEvent e) { }
		};

		titlePanel.addMouseListener(toggleContentOnClick);
		titleLabel.addMouseListener(toggleContentOnClick);



		titlePanel.add(titleLabel, BorderLayout.WEST);

//		add(titlePanel, BorderLayout.NORTH);
//		add(content, BorderLayout.CENTER);
		add(titlePanel);
		add(contentContainer);
	}
}
