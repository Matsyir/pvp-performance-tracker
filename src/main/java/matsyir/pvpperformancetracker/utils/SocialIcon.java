package matsyir.pvpperformancetracker.utils;

import java.awt.Color;
import javax.swing.ImageIcon;
import lombok.Getter;
import net.runelite.client.util.ImageUtil;

@Getter
public enum SocialIcon
{
	GITHUB("github24", new Color(203, 210, 218)),
	DISCORD("discord24", new Color(86, 98, 246));

	final Color highlightColor;
	final ImageIcon icon;

	SocialIcon(String fname, Color highlightColor)
	{
		this.highlightColor = highlightColor;
		this.icon = new ImageIcon(ImageUtil.loadImageResource(getClass(), "/socialIcons/" + fname + ".png"));
	}
}
