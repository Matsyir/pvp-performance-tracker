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
import java.util.ArrayList;
import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.views.FightPerformancePanel;
import matsyir.pvpperformancetracker.views.TotalStatsPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

class PvpPerformanceTrackerPanel extends PluginPanel
{
	// The main fight history container, this will hold all the individual FightPerformancePanels.
	private final JPanel fightHistoryContainer = new JPanel();
	private final TotalStatsPanel totalStatsPanel = new TotalStatsPanel();

	private final PvpPerformanceTrackerPlugin plugin;
	private final PvpPerformanceTrackerConfig config;

	@Inject
	private PvpPerformanceTrackerPanel(final PvpPerformanceTrackerPlugin plugin, final PvpPerformanceTrackerConfig config)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));
		JPanel mainContent = new JPanel(new BorderLayout());

		fightHistoryContainer.setLayout(new BoxLayout(fightHistoryContainer, BoxLayout.Y_AXIS));

		add(totalStatsPanel);

		// add filter line with label & text field.
		JPanel filterLine = new JPanel(new BorderLayout());
		// filter label
		JLabel filterLabel = new JLabel("Filter Usernames:");
		filterLabel.setHorizontalAlignment(SwingConstants.CENTER);
		// filter textfield
		JTextField nameFilter = new JTextField(config.nameFilter());
		filterLine.setMaximumSize(new Dimension(PANEL_WIDTH, (int)filterLine.getPreferredSize().getHeight()));

		nameFilter.getDocument().addDocumentListener(new DocumentListener() {
			private void updateNameFilterValue()
			{
				plugin.updateNameFilterConfig(nameFilter.getText());

				// do not rebuild the panel if the filter starts with a space. This is because you could spam rebuild()
				// by holding down space and causing massive lag. This isn't really an issue with real filters as the
				// results quickly get filtered down, resulting in less UI elements.
				if (!nameFilter.getText().startsWith(" "))
				{
					PvpPerformanceTrackerPanel.this.rebuild();
				}
			}

			public void changedUpdate(DocumentEvent e) { updateNameFilterValue(); }
			public void removeUpdate(DocumentEvent e) { updateNameFilterValue(); }
			public void insertUpdate(DocumentEvent e) { updateNameFilterValue(); }
		});

		filterLine.add(filterLabel, BorderLayout.NORTH);
		filterLine.add(nameFilter, BorderLayout.CENTER);

		add(Box.createRigidArea(new Dimension(0, 4)));
		add(filterLine);

		// wrap mainContent with scrollpane so it has a scrollbar
		JScrollPane scrollableContainer = new JScrollPane(mainContent);
		scrollableContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scrollableContainer.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));

		mainContent.add(fightHistoryContainer, BorderLayout.NORTH);

		add(Box.createRigidArea(new Dimension(0, 4)));
		add(scrollableContainer);
	}

	public void addFight(FightPerformance fight)
	{
		// if the nameFilter isn't blank, skip adding the fight to panels if it doesn't respect the name filter
		if (!config.nameFilter().equals("")
			&& (config.exactNameFilter() ?
				!fight.getCompetitor().getName().toLowerCase().equals(config.nameFilter())
				&& !fight.getOpponent().getName().toLowerCase().equals(config.nameFilter())
				: !fight.getCompetitor().getName().toLowerCase().startsWith(config.nameFilter())
				&& !fight.getOpponent().getName().toLowerCase().startsWith(config.nameFilter())))
		{
			return;
		}

		totalStatsPanel.addFight(fight);

		SwingUtilities.invokeLater(() ->
		{
			fightHistoryContainer.add(new FightPerformancePanel(fight), 0);

			// if we now have more fights than we want to render, then remove fights from the container in order to only render our max.
			if (fightHistoryContainer.getComponentCount() > config.fightHistoryRenderLimit())
			{
				// this will probably only remove 1 fight in most cases, but just in case we need to remove more than that.
				int numFightsToRemove = fightHistoryContainer.getComponentCount() - config.fightHistoryRenderLimit();
				for (int i = 0; i < numFightsToRemove && fightHistoryContainer.getComponentCount() > 0; i++)
				{
					// remove from the last components since we add to 0
					fightHistoryContainer.remove(fightHistoryContainer.getComponentCount() - 1);
				}
			}

			updateUI();
		});
	}

	public void addFights(ArrayList<FightPerformance> fights)
	{
		// if the nameFilter isn't blank, skip adding any fights to panels if they don't respect the name filter
		if (!config.nameFilter().equals(""))
		{
			fights.removeIf((FightPerformance f) ->
				// remove if the names aren't EQUAL when using "exactNameFilter",
				// if not then remove names that don't start with the name filter.
				config.exactNameFilter() ?
					!f.getCompetitor().getName().toLowerCase().equals(config.nameFilter())
						&& !f.getOpponent().getName().toLowerCase().equals(config.nameFilter())
					: !f.getCompetitor().getName().toLowerCase().startsWith(config.nameFilter())
					&& !f.getOpponent().getName().toLowerCase().startsWith(config.nameFilter()));
		}

		totalStatsPanel.addFights(fights);
		SwingUtilities.invokeLater(() ->
		{
			// if we're adding more fights than we want to render at all, then reduce the number of fights and clear all the existing ones
			if (fights.size() > config.fightHistoryRenderLimit())
			{
				int numFightsToRemove = fights.size() - config.fightHistoryRenderLimit();
				fights.removeIf((FightPerformance f) -> fights.indexOf(f) < numFightsToRemove);
				fightHistoryContainer.removeAll();
			}
			// if we're adding a normal number of fights, then check if we actually need to remove existing fights to make room for it.
			else
			{
				int fightsToAdd = fights.size();
				int fightsToRemove = fightHistoryContainer.getComponentCount() - config.fightHistoryRenderLimit() + fightsToAdd;

				if (fightsToRemove > 0)
				{
					// Remove oldest fightHistory until the size is equal to the limit.
					for (int i = 0; i < fightsToRemove && fightHistoryContainer.getComponentCount() > 0; i++)
					{
						// remove from the last components since we add to 0
						fightHistoryContainer.remove(fightHistoryContainer.getComponentCount() - 1);
					}
				}

			}

			fights.forEach((FightPerformance f) -> fightHistoryContainer.add(new FightPerformancePanel(f), 0));
			updateUI();
		});
	}

	public void rebuild()
	{
		totalStatsPanel.reset();
		fightHistoryContainer.removeAll();
		if (plugin.fightHistory.size() > 0)
		{
			// create new arraylist from the main one so we can't modify the fight history
			ArrayList<FightPerformance> fightsToAdd = new ArrayList<>(plugin.fightHistory);

			addFights(fightsToAdd);
		}
		SwingUtilities.invokeLater(this::updateUI);
	}

	public void setConfigWarning(boolean enable)
	{
		totalStatsPanel.setConfigWarning(enable);
	}
}