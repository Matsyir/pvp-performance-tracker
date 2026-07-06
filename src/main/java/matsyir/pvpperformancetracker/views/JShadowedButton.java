/*
 * Copyright (c) 2026, Matsyir <https://github.com/Matsyir>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package matsyir.pvpperformancetracker.views;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import lombok.Getter;
import lombok.Setter;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import matsyir.pvpperformancetracker.utils.SocialIcon;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.ColorUtil;

// if we add mouse listeners onto a button, it breaks some default hover/right click events,
// so we have to re-add a bunch of stuff anyways, use this to fix that while being able to use mouse listeners
// without spamming so much boilerplate everywhere
@Getter
@Setter
public class JShadowedButton extends JShadowedLabel
{
	// panelActionBorders: To be used with any future actions which don't require the same padding as panelActionPaddingBorder
	public static final Border panelActionBorder = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1),
		BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR, 2));
	public static final Border panelActionBorderHovered = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
		BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker().darker(), 2));

	public static final Border panelActionBorderWarning = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(PvpColorScheme.BLOOD_RED_ORANGE, 1),
		BorderFactory.createLineBorder(PvpColorScheme.BLOOD_RED_ORANGE_DARKER, 2)); //ColorUtil.colorWithAlpha(PvpColorScheme.BLOOD_RED_ORANGE, (int)(255.0 * 0.65))
	public static final Border panelActionBorderWarningHovered = BorderFactory.createCompoundBorder(
		BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
		BorderFactory.createLineBorder(PvpColorScheme.BLOOD_RED_ORANGE, 2));

	public static final Border panelActionPaddingBorder = BorderFactory.createEmptyBorder(2, 0, 2, 0);

	// paddedPanelActionBorder: Padding included via panelActionPaddingBorder, this is currently used for
	// both pvpHubHiddenNameBtn and the name filter.
	public static final Border paddedPanelActionBorder = BorderFactory.createCompoundBorder(
		panelActionBorder,
		panelActionPaddingBorder);
	public static final Border paddedPanelActionBorderHovered = BorderFactory.createCompoundBorder(
		panelActionBorderHovered,
		panelActionPaddingBorder);

	public static final Border paddedPanelActionBorderWarning = BorderFactory.createCompoundBorder(
		panelActionBorderWarning,
		panelActionPaddingBorder);

	public static final Border paddedPanelActionBorderWarningHovered = BorderFactory.createCompoundBorder(
		panelActionBorderWarningHovered,
		panelActionPaddingBorder);

	private boolean hovered = false;
	private boolean warning = false;

	public JShadowedButton()
	{
		super();
	}
	public JShadowedButton(String text)
	{
		super(text);
	}
	private JShadowedButton(String text, int desiredWidth, int desiredHeight, JPopupMenu popupMenu, Runnable onLeftClick)
	{
		super(text);

		setText(text);
		setOpaque(false);
		setBorder(paddedPanelActionBorder);
		setHorizontalAlignment(SwingConstants.CENTER);

		if (desiredWidth > 0 && desiredHeight > 0)
		{
			setMaximumSize(new Dimension(desiredWidth, desiredHeight));
			setPreferredSize(getMaximumSize());
		}

		addMouseListener(new MouseListener()
		{
			private void updateBorder()
			{
				if (hovered)
				{
					JShadowedButton.this.setBorder(warning ? paddedPanelActionBorderWarningHovered : paddedPanelActionBorderHovered);
				}
				else
				{
					JShadowedButton.this.setBorder(warning ? paddedPanelActionBorderWarning : paddedPanelActionBorder);
				}
			}
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1 && onLeftClick != null) // left click: run onLeftClick
				{
					onLeftClick.run();
					updateBorder(); // update border in case onLeftClick updated this.warning
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				hovered = true;
				updateBorder();
				JShadowedButton.this.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
			@Override
			public void mouseExited(MouseEvent e)
			{
				hovered = false;
				updateBorder();
				JShadowedButton.this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
			@Override public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON3) // right click: show popup menu
				{
					if (popupMenu != null && e.getComponent() != null)
					{
						popupMenu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			}
			@Override public void mouseReleased(MouseEvent e){}
		});
	}
	public static JShadowedButton getSocialButton(String text, int desiredWidth, int desiredHeight, SocialIcon sIcon, JPopupMenu popupMenu, Runnable onLeftClick)
	{
		return new JShadowedButton(text, desiredWidth, desiredHeight, sIcon.getIcon(), sIcon.getHighlightColor(), popupMenu, onLeftClick);
	}
	public JShadowedButton(String text, int desiredWidth, int desiredHeight, ImageIcon icon, Color fg, JPopupMenu popupMenu, Runnable onLeftClick)
	{
		this(text, desiredWidth, desiredHeight, popupMenu, onLeftClick);
		setForeground(fg);

		if (icon != null)
		{
			setIcon(icon);
		}
	}
	private JShadowedButton(String text, int desiredWidth, int desiredHeight, Color fg, Runnable onLeftClick, JPopupMenu popupMenu)
	{
		this(text, desiredWidth, desiredHeight, null, fg, popupMenu, onLeftClick);
	}
}
