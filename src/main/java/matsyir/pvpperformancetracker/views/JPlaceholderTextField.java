package matsyir.pvpperformancetracker.views;

import java.awt.Graphics;
import javax.swing.JTextField;
import lombok.Getter;
import lombok.Setter;

public class JPlaceholderTextField extends JTextField
{
	@Getter
	@Setter
	private String placeholderText;
	private String originalText;

	public JPlaceholderTextField(String text, String placeholderText)
	{
		setText(text);
		this.originalText = text;
		this.placeholderText = placeholderText;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
//		originalText = getText();
//		if (originalText.isEmpty())
//		{
//			setText(placeholderText);
//		}
//		super.paintComponent(g);
//
//		setText(originalText);
	}
}
