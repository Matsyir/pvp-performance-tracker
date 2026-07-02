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
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import lombok.extern.slf4j.Slf4j;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.views.FightPerformancePanel;
import matsyir.pvpperformancetracker.views.PlaceholderTextField;
import matsyir.pvpperformancetracker.views.TotalStatsPanel;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;

@Slf4j
public class PvpPerformanceTrackerPanel extends PluginPanel
{
	public static final int FULL_PANEL_WIDTH = PANEL_WIDTH + SCROLLBAR_WIDTH;
	public static final int FIGHT_HISTORY_SCROLL_WIDTH = 5;
	public static final int FIGHT_PERFORMANCE_PANEL_WIDTH = FULL_PANEL_WIDTH - FIGHT_HISTORY_SCROLL_WIDTH;

	// put a small delay on the name filtering behavior so it doesn't lag too much if typing quickly
	public static final int BASE_NAME_FILTER_DELAY = 65; // delay in ms
	public static final int NAME_FILTER_DELAY_PER_100_SAVED_FIGHTS = 4;

	// prevent spamming rebuilds too quickly for no reason if the panel isn't visible anyways.
	// For example if people are changing multiple configs which require rebuild, like the colors.
	// Will also call rebuild instantly in this.onActivate, if it's queued.
	// "watching the rebuild" isn't ideal, although it's reasonable with the recent improvements
	public static final int REBUILD_DELAY = 4000; // delay in ms

	// The main fight history container, this will hold all the individual FightPerformancePanels.
	private final JPanel fightHistoryContainer = new JPanel();
	private final TotalStatsPanel totalStatsPanel = new TotalStatsPanel();
	private final JPanel pvpHubHiddenNameLine = new JPanel(new BorderLayout());
	private final JShadowedLabel pvpHubHiddenNameBtn = new JShadowedLabel();
	private boolean hiddenNameIsVisible = false;
	private int filteredFightCount = 0;

	private final PvpPerformanceTrackerPlugin plugin;
	private final PvpPerformanceTrackerConfig config;
	private final WorldService worldService;
	private final Client client;
	private final ClientThread clientThread;
	private final Map<Integer, Integer> worldLocations = new ConcurrentHashMap<>();
	private final Set<Integer> pendingWorldLocationLoads = ConcurrentHashMap.newKeySet();

	private final Timer panelFilterTask = new Timer(BASE_NAME_FILTER_DELAY, e ->
	{
		PvpPerformanceTrackerPanel.this.forceRebuild(true); // rebuild entire panel/fight history using new name filter.
	});

	private final Timer enqueueRebuildTask = new Timer(REBUILD_DELAY, e ->
	{
		PvpPerformanceTrackerPanel.this.forceRebuild();
	});

	@Inject
	private PvpPerformanceTrackerPanel(final PvpPerformanceTrackerPlugin plugin, final PvpPerformanceTrackerConfig config, final WorldService worldService, final Client client, final ClientThread clientThread)
	{
		super(false);
		this.plugin = plugin;
		this.config = config;
		this.worldService = worldService;
		this.client = client;
		this.clientThread = clientThread;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.BORDER_COLOR);
		setBorder(null);
		JPanel mainContent = new JPanel(new BorderLayout());

		fightHistoryContainer.setLayout(new BoxLayout(fightHistoryContainer, BoxLayout.Y_AXIS));

		add(totalStatsPanel);

		// panelActionBorders: To be used with any future actions which don't require the same padding as panelActionPaddingBorder
		Border panelActionBorder = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1),
			BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR, 2));
		Border panelActionBorderHovered = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
			BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker().darker(), 2));
		Border panelActionBorderWarning = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(255, 0, 0, 160), 1),
			BorderFactory.createLineBorder(new Color(255, 0, 0, 128), 1));
		Border panelActionPaddingBorder = BorderFactory.createEmptyBorder(2, 0, 3, 0);

		// paddedPanelActionBorder: Padding included via panelActionPaddingBorder, this is currently used for
		// both pvpHubHiddenNameBtn and the name filter.
		Border paddedPanelActionBorder = BorderFactory.createCompoundBorder(
			panelActionBorder,
			panelActionPaddingBorder);
		Border paddedPanelActionBorderHovered = BorderFactory.createCompoundBorder(
			panelActionBorderHovered,
			panelActionPaddingBorder);
		Border paddedPanelActionBorderNameVisible = BorderFactory.createCompoundBorder(
			BorderFactory.createCompoundBorder(
				panelActionBorderWarning,
				BorderFactory.createLineBorder(new Color(255, 0, 0, 80))),
			panelActionPaddingBorder);
		Border paddedPanelActionBorderWarningHovered = BorderFactory.createCompoundBorder(
				BorderFactory.createCompoundBorder(
					BorderFactory.createLineBorder(new Color(255, 0, 0, 160), 1),
					BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 2)),
			panelActionPaddingBorder);

		pvpHubHiddenNameBtn.setBorder(paddedPanelActionBorder);
		pvpHubHiddenNameBtn.setForeground(ColorScheme.TEXT_COLOR);
		pvpHubHiddenNameBtn.setShadow(Color.BLACK);
		pvpHubHiddenNameBtn.setBackground(ColorScheme.BORDER_COLOR);
		pvpHubHiddenNameBtn.setHorizontalAlignment(SwingConstants.CENTER);
		pvpHubHiddenNameBtn.setToolTipText("<html>Your private, read-only \"hidden name\" used when \"Hide RSN on PvP-Hub\" is enabled. " +
			"Click to toggle visibility.<br><br>" +
			"You can reset this hidden name by right-clicking this button or the Total Stats just above.");
		pvpHubHiddenNameBtn.setComponentPopupMenu(new JPopupMenu());
		pvpHubHiddenNameBtn.getComponentPopupMenu().add(TotalStatsPanel.resetPvpHubHiddenNameMenuItem);
		pvpHubHiddenNameBtn.addMouseListener(new MouseListener()
		{
			private boolean hovered;
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1) // left click: toggle hidden name visibility
				{
					hiddenNameIsVisible = !hiddenNameIsVisible;
					updatePvpHubHiddenName();
					if (hovered)
					{
						pvpHubHiddenNameBtn.setBorder(hiddenNameIsVisible ? paddedPanelActionBorderWarningHovered : paddedPanelActionBorderHovered);
					}
					else
					{
						pvpHubHiddenNameBtn.setBorder(hiddenNameIsVisible ? paddedPanelActionBorderNameVisible : paddedPanelActionBorder);
					}
				}
				else if (e.getButton() == MouseEvent.BUTTON3) // right click: popup menu to reset hidden name
				{
					pvpHubHiddenNameBtn.getComponentPopupMenu().show(e.getComponent(), e.getX(), e.getY());
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				hovered = true;
				pvpHubHiddenNameBtn.setBorder(hiddenNameIsVisible ? paddedPanelActionBorderWarningHovered : paddedPanelActionBorderHovered);
				setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hovered = false;
				pvpHubHiddenNameBtn.setBorder(hiddenNameIsVisible ? paddedPanelActionBorderNameVisible : paddedPanelActionBorder);
				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseReleased(MouseEvent e) {}
		});

		pvpHubHiddenNameLine.add(pvpHubHiddenNameBtn, BorderLayout.CENTER);
		pvpHubHiddenNameLine.setMaximumSize(new Dimension(FULL_PANEL_WIDTH, (int) pvpHubHiddenNameLine.getPreferredSize().getHeight()));
		pvpHubHiddenNameLine.setBackground(ColorScheme.BORDER_COLOR);
		pvpHubHiddenNameLine.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorScheme.BORDER_COLOR));
		updatePvpHubHiddenName();

		add(pvpHubHiddenNameLine);

		// add filter line with text field.
		JPanel filterLine = new JPanel(new BorderLayout());
		String filterTooltip = "<html>" +
			"You can filter the displayed fights as well as the total stats by typing in this textbox.<br><br>" +
			"There are currently 3 different ways you can filter fights:<br>" +
			"<b>1)</b> Searching for RSN, either yours or the opponent's<br>" +
			"<b>2)</b> Searching for the Border style, for example you can search for \"max hit ko\" or \"spec ko\"<br>" +
			"<b>3)</b> Searching for <i>&gt;X</i>, <i>&gt;=X</i>, <i>&lt;X</i>, or <i>&lt;=X</i> &nbsp;total attacks by the client player<br>" +
			"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;For example, <i>\"&gt;60\"</i> will only display fights with over 60 total attacks made by yourself (the client player)";
		filterLine.setForeground(ColorScheme.TEXT_COLOR);
		filterLine.setBackground(ColorScheme.BORDER_COLOR);
		// filter textfield
		PlaceholderTextField nameFilter = new PlaceholderTextField("Filter Usernames:", config.nameFilter());
		nameFilter.setHorizontalAlignment(SwingConstants.CENTER);
		nameFilter.setForeground(ColorScheme.TEXT_COLOR);
		nameFilter.setBackground(ColorScheme.BORDER_COLOR);
		nameFilter.setBorder(paddedPanelActionBorder);
		filterLine.setMaximumSize(new Dimension(FULL_PANEL_WIDTH, (int) filterLine.getPreferredSize().getHeight()));

		panelFilterTask.setRepeats(false);
		((AbstractDocument) nameFilter.getDocument()).setDocumentFilter(new DocumentFilter()
		{
			@Override
			public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
				throws BadLocationException
			{
				update(fb, offset, 0, string, attr);
			}

			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException
			{
				update(fb, offset, length, text, attrs);
			}

			@Override
			public void remove(FilterBypass fb, int offset, int length)
				throws BadLocationException
			{
				update(fb, offset, length, "", null);
			}

			private void update(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
				throws BadLocationException
			{
				String sanitized = fb.getDocument().getText(0, fb.getDocument().getLength());

				sanitized = sanitized.substring(0, offset)
					+ text
					+ sanitized.substring(offset + length);

				sanitized = plugin.updateNameFilterConfig(sanitized);

				fb.replace(0, fb.getDocument().getLength(), sanitized, attrs);

				panelFilterTask.restart();
			}
		});
		nameFilter.addFocusListener(new FocusListener()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				nameFilter.setBorder(paddedPanelActionBorderHovered);
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				nameFilter.setBorder(paddedPanelActionBorder);
			}
		});
		nameFilter.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				nameFilter.setBorder(paddedPanelActionBorderHovered);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				nameFilter.setBorder(paddedPanelActionBorder);
			}

			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}
			@Override public void mouseReleased(MouseEvent e) {}
		});

		filterLine.add(nameFilter, BorderLayout.CENTER);
		filterLine.setToolTipText(filterTooltip);
		nameFilter.setToolTipText(filterTooltip);
		add(filterLine);

		enqueueRebuildTask.setRepeats(false);

		// wrap mainContent with scrollpane so it's scrollable
		JScrollPane scrollableContainer = new JScrollPane(mainContent);
		scrollableContainer.setBackground(ColorScheme.SCROLL_TRACK_COLOR);
		scrollableContainer.getVerticalScrollBar().setPreferredSize(new Dimension(FIGHT_HISTORY_SCROLL_WIDTH, 0));

		scrollableContainer.getViewport().addChangeListener(e -> scrollableContainer.getViewport().repaint());

		mainContent.add(fightHistoryContainer, BorderLayout.NORTH);
		add(scrollableContainer);

		setPreferredSize(new Dimension(FULL_PANEL_WIDTH, getPreferredSize().height));
	}

	public void addFight(FightPerformance fight)
	{
		// skip adding the fight to panels if it doesn't respect the name filter
		if (!fight.isRelevantForFilter(config.nameFilter(), fight.getBgStyle()))
		{
			return;
		}
		filteredFightCount++;

		totalStatsPanel.addFight(fight.getPvpHubDisplayFight());
		// run all of this on UI thread, since we're adding and removing containers in it.
		SwingUtilities.invokeLater(() ->
		{
			fightHistoryContainer.add(new FightPerformancePanel(fight, worldService, getWorldLocationSupplier(fight.getPvpHubDisplayFight().getWorld())), 0);

			// if we now have more fights than we want to render, then remove fights from the container in order to only render our max.
			int c;
			while ((c = fightHistoryContainer.getComponentCount()) > config.fightHistoryRenderLimit())
			{
				fightHistoryContainer.remove(c - 1);
			}
		});
	}

	public void addFights(ArrayDeque<FightPerformance> fights, boolean skipUpdatesIfFightCountUnchanged)
	{
		// skip adding any fights to panels if they don't respect the name filter
		// see FightPerformance.isRelevantForFilter for filter behavior details
		if (!config.nameFilter().isEmpty())
		{
			fights.removeIf((FightPerformance f) ->
				!f.isRelevantForFilter(config.nameFilter(), f.getBgStyle()));
		}

		//log.info("Panel.addFights: skipUpdatesIfFightCountUnchanged=" + skipUpdatesIfFightCountUnchanged + ", fights.size=" + fights.size() + ", filteredFightCount=" + filteredFightCount);
		if (skipUpdatesIfFightCountUnchanged && fights.size() == filteredFightCount)
		{
			return;
		}

		filteredFightCount = fights.size();

		totalStatsPanel.reset();
		ArrayList<FightPerformance> displayFights = new ArrayList<>();
		for (FightPerformance fight : fights)
		{
			displayFights.add(fight.getPvpHubDisplayFight());
		}
		totalStatsPanel.addFights(displayFights);

		fights.clear();
		fights.addAll(displayFights);
		displayFights.clear();

		// if we're adding more fights than we want to render at all, then reduce the number of fights we're adding
		while(fights.size() > config.fightHistoryRenderLimit())
		{
			fights.removeFirst();
		}

		// initializing all the panels on the client thread before the UI thread call helps a lot,
		// especially when we're doing 200 or potentially more.
		ArrayList<FightPerformancePanel> panelsToAdd = new ArrayList<>();
		fights.forEach((f) -> panelsToAdd.add(new FightPerformancePanel(
			f, worldService, getWorldLocationSupplier(f.getPvpHubDisplayFight().getWorld()))));

		// for bulk addFights, don't just add them to 0, since this causes visual flickering while they all get added.
		// instead, start at 0 and go upwards, so the first few panels simply get replaced, and then while the rest
		// of the fights get initialized, the only visual change would be the scrollbar length changing, rather than
		// entire panels flickering.
		// run all of this on UI thread, since we're removing and adding containers in it.
		SwingUtilities.invokeLater(() ->
		{
			fightHistoryContainer.removeAll();
			for (int i = 0; i < panelsToAdd.size(); i++)
			{
				int endOffset = i+1; // end-based index
				// get fight panels starting from the end of the fights, so we add the newest ones first
				// (newest ones get added to the end of the fight array)
				fightHistoryContainer.add(panelsToAdd.get(panelsToAdd.size() - endOffset), i);
			}
		});
	}

	private IntSupplier getWorldLocationSupplier(int world)
	{
		requestWorldLocation(world);
		return () -> worldLocations.getOrDefault(world, -1);
	}

	private void requestWorldLocation(int world)
	{
		if (world <= 0 || worldLocations.containsKey(world) || !pendingWorldLocationLoads.add(world))
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			int location = -1;
			try
			{
				EnumComposition worldLocationsEnum = client.getEnum(EnumID.WORLD_LOCATIONS);
				if (worldLocationsEnum != null)
				{
					location = worldLocationsEnum.getIntValue(world);
				}
			}
			catch (RuntimeException ignored)
			{
				// Keep fight cards renderable even when the world-location enum is unavailable.
			}

			final int resolvedLocation = location;
			SwingUtilities.invokeLater(() ->
			{
				worldLocations.put(world, resolvedLocation);
				pendingWorldLocationLoads.remove(world);
				fightHistoryContainer.repaint();
			});
		});
	}
	public void enqueueRebuild()
	{
		enqueueRebuild(false);
	}
	public void enqueueRebuild(boolean now)
	{
		if (this.isVisible() || now)
		{
			forceRebuild();
			return;
		}

		enqueueRebuildTask.restart();
	}

	private void forceRebuild()
	{
		forceRebuild(false);
	}
	private void forceRebuild(boolean skipUpdatesIfFightCountUnchanged)
	{
		// if the main enqueueRebuildTask is running, then we need a full update, not just a filter check
		if (enqueueRebuildTask.isRunning())
		{
			skipUpdatesIfFightCountUnchanged = false;
		}
		int newFilterDelay = BASE_NAME_FILTER_DELAY +
			(NAME_FILTER_DELAY_PER_100_SAVED_FIGHTS * (PvpPerformanceTrackerPlugin.PLUGIN.fightHistory.size() / 100));
		panelFilterTask.setInitialDelay(newFilterDelay);
		panelFilterTask.setDelay(newFilterDelay);

		enqueueRebuildTask.stop();
		panelFilterTask.stop();

		FightPerformancePanel.updateCurrentBgColor();

		if (!plugin.fightHistory.isEmpty())
		{

			// create new arrayDeque here from the main one so we can't/don't modify the fight history
			// addFights handles UI updates on its own.
			addFights(new ArrayDeque<>(plugin.fightHistory), skipUpdatesIfFightCountUnchanged);
		}
		else
		{
			totalStatsPanel.reset();
			fightHistoryContainer.removeAll();
			SwingUtilities.invokeLater(this::updateUI);
		}
	}

	public void setConfigWarning(boolean enable)
	{
		totalStatsPanel.setConfigWarning(enable);
	}

	public void updatePvpHubHiddenName()
	{
		boolean showHiddenName = config.uploadFightsToPvpHub() && config.hideRsnOnPvpHub();
		pvpHubHiddenNameLine.setVisible(showHiddenName);
		pvpHubHiddenNameBtn.setText("PvP-Hub Name: " + (showHiddenName && hiddenNameIsVisible ? plugin.getPvpHubHiddenName() : "(click to view)"));

		revalidate();
		repaint();
	}

	public void updatePopupMenuForPvpHubConfig()
	{
		totalStatsPanel.updatePopupMenuForPvpHubConfig();
	}

	@Override
	public void onActivate()
	{
		super.onActivate();
		if (enqueueRebuildTask.isRunning())
		{
			forceRebuild();
		}
		else if (panelFilterTask.isRunning())
		{
			forceRebuild(true);
		}
	}
}
