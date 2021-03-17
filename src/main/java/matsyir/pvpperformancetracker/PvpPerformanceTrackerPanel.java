/*
 * Copyright (c)  2020, Matsyir <https://github.com/Matsyir>
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
package matsyir.pvpperformancetracker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import matsyir.pvpperformancetracker.views.ConfigurationTabPanel;
import matsyir.pvpperformancetracker.views.FightHistoryTabPanel;
import matsyir.pvpperformancetracker.views.InformationTabPanel;
import matsyir.pvpperformancetracker.views.TabContentPanel;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

public class PvpPerformanceTrackerPanel extends PluginPanel
{
	private final JPanel mainContainer = new JPanel();
	public static final int MAX_TAB_COUNT = 3;
	public final Map<String, MaterialTab> contentTabs = new HashMap<>();
	private final MaterialTabGroup tabGroup = new MaterialTabGroup(mainContainer);


	public FightHistoryTabPanel fightHistoryTabPanel = new FightHistoryTabPanel(this);
	private ConfigurationTabPanel configurationTabPanel = new ConfigurationTabPanel(this);
	private InformationTabPanel informationTabPanel = new InformationTabPanel(this);

	private TabContentPanel activeTabPanel = fightHistoryTabPanel;

	private final PvpPerformanceTrackerPlugin plugin;
	private final PvpPerformanceTrackerConfig config;

	@Inject
	public PvpPerformanceTrackerPanel(final PvpPerformanceTrackerPlugin plugin, final PvpPerformanceTrackerConfig config)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));

		tabGroup.setAlignmentX(LEFT_ALIGNMENT);

		// First tab: Fight history tab
		addTab(fightHistoryTabPanel);
		addTab(configurationTabPanel);
		addTab(informationTabPanel);

		add(tabGroup, BorderLayout.NORTH);
		add(mainContainer, BorderLayout.CENTER);

		JPanel mainContent = new JPanel(new BorderLayout());



		// wrap mainContent with scrollpane so it has a scrollbar
//		JScrollPane scrollableContainer = new JScrollPane(mainContent);
//		scrollableContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
//		scrollableContainer.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

		//mainContent.add(fightHistoryContainer, BorderLayout.NORTH);
		//add(scrollableContainer, BorderLayout.CENTER);
	}

	private void addTab(TabContentPanel tabContentPanel)
	{
		JPanel wrapped = new JPanel(new BorderLayout());
		wrapped.add(tabContentPanel, BorderLayout.NORTH);
		wrapped.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JScrollPane scroller = new JScrollPane(wrapped);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scroller.getVerticalScrollBar().setBorder(new EmptyBorder(0, 0, 0, 0));
		scroller.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Use a placeholder icon until the async image gets loaded
		MaterialTab materialTab = new MaterialTab(new ImageIcon(), tabGroup, scroller);
		materialTab.setPreferredSize(new Dimension(64, 28)); // Math.round(((float)this.getWidth() / MAX_TAB_COUNT) - 8) ?? TODO why no work
		materialTab.setName(tabContentPanel.getName());
		materialTab.setToolTipText(tabContentPanel.getName());

		// TODO ICON
		// 16x16?
		//BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/configure.png");
		materialTab.setIcon(new ImageIcon(tabContentPanel.getTabIcon().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
//		Runnable resize = () ->
//		{
//			BufferedImage subIcon = icon.getSubimage(0, 0, 32, 32);
//			materialTab.setIcon(new ImageIcon(subIcon.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
//		};
//		icon.onLoaded(resize);
//		resize.run();

		materialTab.setOnSelectEvent(() ->
		{
			activeTabPanel = tabContentPanel;
			tabContentPanel.update();
			return true;
		});

		contentTabs.put(tabContentPanel.getName(), materialTab);
		tabGroup.addTab(materialTab);

		if (activeTabPanel.getName().equals(tabContentPanel.getName()))
		{
			tabGroup.select(materialTab);
		}
	}

	void switchTab(String tabName)
	{
		tabGroup.select(contentTabs.get(tabName));
	}

	void switchTab(TabContentPanel tab)
	{
		tabGroup.select(contentTabs.get(tab.getName()));
	}
}