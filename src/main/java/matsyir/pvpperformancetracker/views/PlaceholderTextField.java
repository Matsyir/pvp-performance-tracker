package matsyir.pvpperformancetracker.views;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import javax.swing.JTextField;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;

@Getter
@Setter
public class PlaceholderTextField extends JTextField
{
	private static final Color DEFAULT_PLACEHOLDER_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color DEFAULT_PLACEHOLDER_SHADOW_COLOR = Color.BLACK;

	private String placeholder;
	private Color placeholderColor = DEFAULT_PLACEHOLDER_COLOR;
	private Color placeholderShadowColor = DEFAULT_PLACEHOLDER_SHADOW_COLOR;

	public PlaceholderTextField(String placeholder)
	{
		super();
		this.placeholder = placeholder;
	}

	public PlaceholderTextField(String placeholder, String text)
	{
		super(text);
		this.placeholder = placeholder;
	}

	public PlaceholderTextField(String placeholder, String text, Color placeholderColor)
	{
		super(text);
		this.placeholder = placeholder;
		this.placeholderColor = placeholderColor;
	}


	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		if (getText().isEmpty() && !isFocusOwner())
		{
			for (int i = 0; i <= 1; i++)
			{
				boolean drawingShadow = i == 0;
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setColor(drawingShadow ? placeholderShadowColor : placeholderColor);
				g2.setFont(getFont().deriveFont(Font.ITALIC));

				Insets insets = getInsets();
				FontMetrics fm = g2.getFontMetrics();

				int textWidth = fm.stringWidth(placeholder);
				int availableWidth = getWidth() - insets.left - insets.right;

				int x;
				switch (getHorizontalAlignment())
				{
					case CENTER:
						x = insets.left + (availableWidth - textWidth) / 2;
						break;

					case RIGHT:
					case TRAILING:
						x = getWidth() - insets.right - textWidth;
						break;

					case LEFT:
					case LEADING:
					default:
						x = insets.left;
						break;
				}

				int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

				if (drawingShadow)
				{
					x += 1;
					y += 1;
				}

				g2.drawString(placeholder, x, y);
				g2.dispose();
			}
		}
	}
}
