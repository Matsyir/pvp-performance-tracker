/*
 * Copyright (c) 2025, Matsyir <https://github.com/matsyir>
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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;

public final class PanelFactory
{
	// constants/final values
	public static final float LINE_INDEX_LABEL_FONT_SCALE = 0.65f;
	public static final Color STATS_LINE_INDEX_COLOR = new Color(1f, 1f, 1f, 0.4f);
	public static final Color OVERLAY_STATS_LINE_INDEX_COLOR = new Color(1f, 1f, 1f, 0.2f);

	public static final Font INDEX_FONT = new Font("Monospace", Font.PLAIN, 12);

	public static Font getIndexFontFrom(float initialFontSize)
	{
		return INDEX_FONT.deriveFont(initialFontSize * LINE_INDEX_LABEL_FONT_SCALE);
	}

	public static Font getIndexFontFrom(Font initialFont)
	{
		return getIndexFontFrom(initialFont.getSize());
	}

	public static JPanel createStatsLine(String miniCenterLabelText, String miniCenterLabelTooltip,
										 String leftText, String leftTooltip, Color leftColor,
										 String rightText, String rightTooltip, Color rightColor)
	{
		JPanel statsLine = new JPanel(new GridBagLayout());
		statsLine.setBackground(leftColor.equals(Color.GREEN) ? new Color(40, 52, 78, 92).brighter() : (rightColor.equals(Color.GREEN) ? new Color(89, 52, 59, 92).brighter() : null));

		String tooltipSuffix = "<br><br><b><i>" + miniCenterLabelText + "</i></b>: " + miniCenterLabelTooltip;

		ForwardingLabel leftLabel = new ForwardingLabel(leftText);
		leftLabel.setToolTipText("<html>" + leftTooltip + tooltipSuffix);
		leftLabel.setForeground(leftColor);
		leftLabel.setHorizontalAlignment(SwingConstants.LEFT);

		ForwardingLabel rightLabel = new ForwardingLabel(rightText);
		rightLabel.setToolTipText("<html>" + rightTooltip + tooltipSuffix);
		rightLabel.setForeground(rightColor);
		rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		JLabel indexLabel = new ForwardingLabel(miniCenterLabelText);
		indexLabel.setForeground(STATS_LINE_INDEX_COLOR);
		indexLabel.setFont(PanelFactory.getIndexFontFrom(indexLabel.getFont()));
		indexLabel.setHorizontalAlignment(SwingConstants.CENTER);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// left
		gbc.gridx = 0;
		gbc.anchor = GridBagConstraints.WEST;
		statsLine.add(leftLabel, gbc.clone());

		// right
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		statsLine.add(rightLabel, gbc.clone());

		// index/miniCenterLabel centered across full row
		GridBagConstraints gbcIndex = new GridBagConstraints();
		gbcIndex.gridx = 0;
		gbcIndex.gridy = 0;
		gbcIndex.gridwidth = 3;
		gbcIndex.anchor = GridBagConstraints.CENTER;
		gbcIndex.fill = GridBagConstraints.NONE;
		statsLine.add(indexLabel, gbcIndex);

		return statsLine;
	}

	public static TableComponent createOverlayStatsLine(String miniCenterLabelText, int pLeft, int pRight,
														String leftText, Color leftColor,
														String rightText, Color rightColor)
	{
		TableComponent overlayStatsLine = new TableComponent(TableComponent.TableRowStyle.PERCENTAGE_BASED, pLeft, pRight);
		overlayStatsLine.addRow(leftText, miniCenterLabelText, rightText);
		overlayStatsLine.setColumnColors(leftColor, OVERLAY_STATS_LINE_INDEX_COLOR, rightColor);
		overlayStatsLine.setColumnAlignments(TableComponent.TableAlignment.LEFT, TableComponent.TableAlignment.CENTER, TableComponent.TableAlignment.RIGHT);
		overlayStatsLine.setGutter(new Dimension(2, 0));

		return overlayStatsLine;
	}


	// A JShadowedLabel that still receives mouse events (so its tooltip works) but forwards them to the parent
	// using SwingUtilities.convertMouseEvent(...) so the parent sees clicks/popup/hover as if the label wasn't there.
	private static class ForwardingLabel extends JShadowedLabel
	{
		public ForwardingLabel(String text)
		{
			super(text);
			setOpaque(false);
			setFocusable(false);
			setShadow(Color.BLACK);
		}

		@Override
		protected void processMouseEvent(MouseEvent e)
		{
			// allow label's normal handling (tooltips etc)
			super.processMouseEvent(e);
			forwardToParent(e);
		}

		@Override
		protected void processMouseMotionEvent(MouseEvent e)
		{
			// allow label's normal handling (tooltips)
			super.processMouseMotionEvent(e);
			forwardToParent(e); // forward so parent can show hover effects / track mouse
		}

		private void forwardToParent(MouseEvent e)
		{
			Container parent = getParent();
			if (parent == null)
			{
				return;
			}

			// convert to parent's coordinate system and dispatch there
			MouseEvent parentEvent = SwingUtilities.convertMouseEvent(this, e, parent);
			parent.dispatchEvent(parentEvent);
		}
	}
}
