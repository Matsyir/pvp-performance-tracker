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
import java.awt.event.ActionListener;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.CONFIG;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import matsyir.pvpperformancetracker.utils.PvpColorScheme;
import matsyir.pvpperformancetracker.views.FightPerformancePanel;
import matsyir.pvpperformancetracker.views.JShadowedButton;
import static matsyir.pvpperformancetracker.views.JShadowedButton.paddedPanelActionBorder;
import static matsyir.pvpperformancetracker.views.JShadowedButton.paddedPanelActionBorderHovered;
import matsyir.pvpperformancetracker.views.PlaceholderTextField;
import matsyir.pvpperformancetracker.utils.SocialIcon;
import matsyir.pvpperformancetracker.views.TotalStatsPanel;
import static matsyir.pvpperformancetracker.views.TotalStatsPanel.WIKI_HELP_URL;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

@Slf4j
public class PvpPerformanceTrackerPanel extends PluginPanel
{
	public static final int FULL_PANEL_WIDTH = PANEL_WIDTH + SCROLLBAR_WIDTH;
	public static final int FIGHT_HISTORY_SCROLL_WIDTH = 5;
	public static final int FIGHT_PERFORMANCE_PANEL_WIDTH = FULL_PANEL_WIDTH - FIGHT_HISTORY_SCROLL_WIDTH;
	public static final int SOCIAL_BTN_HEIGHT = 34;
	public static final int PVP_HUB_HIDDEN_NAME_BTN_HEIGHT = 26;

	// put a small delay on the name filtering behavior so it doesn't lag too much if typing quickly
	public static final int BASE_NAME_FILTER_DELAY = 65; // base delay in ms
	// additional delay of 4ms per 100 fights saved
	// this would be +400ms if you have the max of 10,000 fights saved. Would be a bit long/appear a bit stuttery, but
	// it likely wouldn't be ideal to try filtering 10,000 fights every 50ms while typing
	private static final double nameFilterDelayPer100fights = 4;
	private static final double nameFilterDelayForFightCount = 100;
	public static final double NAME_FILTER_DELAY_PER_SAVED_FIGHT = nameFilterDelayPer100fights / nameFilterDelayForFightCount; // 4ms per 100 fights

	// prevent spamming rebuilds too quickly for no reason if the panel isn't visible anyways.
	// For example if people are changing multiple configs which require rebuild, like the colors.
	// Will also call rebuild instantly in this.onActivate, if it's queued.
	// "watching the rebuild" isn't ideal, although it's reasonable with the recent improvements
	public static final int REBUILD_DELAY = 4000; // delay in ms

	private static final String DISCORD_INVITE_URL = "https://discord.gg/hg26xeJnY5";

	// The main fight history container, this will hold all the individual FightPerformancePanels.
	private final JPanel fightHistoryContainer = new JPanel();
	private final TotalStatsPanel totalStatsPanel;
	private final JPanel pvpHubHiddenNameLine = new JPanel(new BorderLayout());
	private final JPanel wikiAndDiscordButtonsLine = new JPanel(new BorderLayout());
	private JShadowedButton pvpHubHiddenNameBtn;
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

		totalStatsPanel = new TotalStatsPanel();
		add(totalStatsPanel);

		wikiAndDiscordButtonsLine.setBackground(ColorScheme.SCROLL_TRACK_COLOR);
		JPopupMenu socialButtonsPopupMenu = new JPopupMenu();
		// use different text here, since from this context the action is always "Hide Social Buttons",
		// rather than "Toggle Social Button Visibility"
		JMenuItem toggleSocialButtonsMenuItem = new JMenuItem("<html>&#128065;&nbsp;Hide Social Buttons");
		toggleSocialButtonsMenuItem.setForeground(TotalStatsPanel.toggleSocialButtonsMenuItem.getForeground());
		for (ActionListener actionListener : TotalStatsPanel.toggleSocialButtonsMenuItem.getActionListeners())
		{
			toggleSocialButtonsMenuItem.addActionListener(actionListener);
		}
		socialButtonsPopupMenu.add(toggleSocialButtonsMenuItem);

		JShadowedButton wikiButton = JShadowedButton.getSocialButton("<html>&nbsp;<u>Wiki</u>&nbsp;&#8599",
			FULL_PANEL_WIDTH / 2,
			SOCIAL_BTN_HEIGHT,
			SocialIcon.GITHUB,
			socialButtonsPopupMenu,
			() -> LinkBrowser.browse(WIKI_HELP_URL));

		JShadowedButton discordButton = JShadowedButton.getSocialButton("<html>&nbsp;<u>Discord</u>&nbsp;&#8599",
			FULL_PANEL_WIDTH / 2,
			SOCIAL_BTN_HEIGHT,
			SocialIcon.DISCORD,
			socialButtonsPopupMenu,
			() -> LinkBrowser.browse(DISCORD_INVITE_URL));

		wikiAndDiscordButtonsLine.add(wikiButton, BorderLayout.WEST);
		wikiAndDiscordButtonsLine.add(discordButton, BorderLayout.EAST);

		add(wikiAndDiscordButtonsLine);

		updateSocialButtons();

		JPopupMenu pvpHubHiddenNameBtnPopupMenu = new JPopupMenu();
		// seems to break if we just popupMenu.add(totalStatsPanel.resetPvpHubHiddenNameMenuItem),
		// so copy fields into a new JMenuItem
		JMenuItem resetPvpHubHiddenNameMenuItem = new JMenuItem(TotalStatsPanel.resetPvpHubHiddenNameMenuItem.getText());
		resetPvpHubHiddenNameMenuItem.setForeground(TotalStatsPanel.resetPvpHubHiddenNameMenuItem.getForeground());
		for (ActionListener actionListener : TotalStatsPanel.resetPvpHubHiddenNameMenuItem.getActionListeners())
		{
			resetPvpHubHiddenNameMenuItem.addActionListener(actionListener);
		}
		pvpHubHiddenNameBtnPopupMenu.add(resetPvpHubHiddenNameMenuItem);
		pvpHubHiddenNameBtn = null;
		pvpHubHiddenNameBtn = new JShadowedButton("",
			FULL_PANEL_WIDTH,
			PVP_HUB_HIDDEN_NAME_BTN_HEIGHT,
			null,
			ColorScheme.TEXT_COLOR,
			pvpHubHiddenNameBtnPopupMenu,
			() -> {
				hiddenNameIsVisible = !hiddenNameIsVisible;
				if (pvpHubHiddenNameBtn != null)
				{
					pvpHubHiddenNameBtn.setWarning(hiddenNameIsVisible);
				}
				updatePvpHubHiddenName();
		});
		pvpHubHiddenNameBtn.setToolTipText("<html>Your private, read-only \"hidden name\" used when \"Hide RSN on PvP-Hub\" is enabled. " +
			"Click to toggle visibility.<br><br>" +
			"You can reset this hidden name by right-clicking this button or the Total Stats just above.");

		pvpHubHiddenNameLine.add(pvpHubHiddenNameBtn, BorderLayout.CENTER);
		pvpHubHiddenNameLine.setBackground(ColorScheme.SCROLL_TRACK_COLOR);
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
		PlaceholderTextField nameFilter = new PlaceholderTextField("Filter Fights:", config.nameFilter());
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
		JScrollPane scrollableContainer = new JScrollPane(mainContent,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

		updateUI();
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

		// if we're adding more fights than we want to render at all, then reduce the number of fights we're adding
		while(fights.size() > config.fightHistoryRenderLimit())
		{
			fights.removeFirst();
		}

		// initializing all the panels on the client thread before the UI thread call helps a lot,
		// especially when we're doing 200 or potentially more.
		ArrayList<FightPerformancePanel> panelsToAdd = new ArrayList<>();
		fights.forEach((f) -> panelsToAdd.add(new FightPerformancePanel(
			f, worldService, getWorldLocationSupplier(f.getWorld()))));

		// for bulk addFights, don't just add them to 0, since this causes visual flickering while they all get added.
		// instead, start at 0 and go upwards, so the first few panels simply get replaced, and then while the rest
		// of the fights get initialized, the only visual change would be the scrollbar length changing, rather than
		// entire panels flickering.
		// run all of this on UI thread, since we're removing and adding containers.
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
		int newFilterDelay = BASE_NAME_FILTER_DELAY + (int)(NAME_FILTER_DELAY_PER_SAVED_FIGHT * PLUGIN.fightHistory.size());
		panelFilterTask.setInitialDelay(newFilterDelay);
		panelFilterTask.setDelay(newFilterDelay);

		enqueueRebuildTask.stop();
		panelFilterTask.stop();

		FightPerformancePanel.updateCurrentBgColor();
		totalStatsPanel.updateBgColor();
		updateSocialButtons();

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
			SwingUtilities.invokeLater(() -> {
				totalStatsPanel.setLabels();
				this.updateUI();
			});
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

	public void updateSocialButtons()
	{
		wikiAndDiscordButtonsLine.setVisible(CONFIG.displayPanelSocialButtons());
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
