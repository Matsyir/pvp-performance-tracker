/*
 * Copyright (c) 2021, Matsyir <https://github.com/Matsyir>
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN_ICON;
import matsyir.pvpperformancetracker.controllers.FightPerformance;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import matsyir.pvpperformancetracker.controllers.AnalyzedFightPerformance;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

@Slf4j
public class FightAnalysisFrame extends JFrame
{
	public static String WIKI_HELP_URL = "https://github.com/Matsyir/pvp-performance-tracker/wiki#fight-analysisfight-merge";
	private static String WINDOW_TITLE = "PvP Performance Tracker: Fight Analysis";
	private static final NumberFormat nf = NumberFormat.getInstance();

	private JPanel mainPanel;
	private JTextField mainFightJsonInput;
	private JTextField opponentFightJsonInput;

	private FightPerformance mainFight;
	private FightPerformance opponentFight;
	private AnalyzedFightPerformance analyzedFight;

	static
	{
		// initialize number format
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

	FightAnalysisFrame(FightPerformance fight, JRootPane rootPane)
	{
		this(rootPane);
		mainFightJsonInput.setText(PLUGIN.GSON.toJson(fight, FightPerformance.class));
		validate();
		repaint();
	}

	FightAnalysisFrame(JRootPane rootPane)
	{
		super("PvP Performance Tracker: Fight Analysis");
		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported())
		{
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		setIconImage(PLUGIN_ICON);
		Dimension size = new Dimension(700, 448);
		setSize(size);
		setMinimumSize(size);
		setLocation(rootPane.getLocationOnScreen());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setPreferredSize(getSize());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
		mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR); //must set all backgrounds to null after for this to work... hmm maybe
		mainPanel.setVisible(true);

		add(mainPanel);

		setVisible(true);
		initializeFrame();
	}

	// setup the initial frame contents, which should be two text inputs used to input
	// two single related fightPerformance json data.
	private void initializeFrame()
	{
		setTitle(WINDOW_TITLE);
		mainPanel.removeAll();

		// init containers
		GridLayout textAreaLayout = new GridLayout(1, 2);
		textAreaLayout.setHgap(4);
		textAreaLayout.setVgap(4);
		JPanel textLabelLine = new JPanel(textAreaLayout);
		textLabelLine.setBackground(null);
		JPanel textAreaLine = new JPanel(textAreaLayout);
		textAreaLine.setBackground(null);

		// wiki link label

		JButton wikiLinkLabel = new JButton("<html><u>Wiki/Example</u>&nbsp;&#8599;</html>");
		wikiLinkLabel.setToolTipText("Open URL to Github wiki with an example & more details");
		wikiLinkLabel.setSize(256, 32);
		wikiLinkLabel.setMaximumSize(new Dimension(256, 32));
		wikiLinkLabel.setHorizontalAlignment(SwingConstants.CENTER);
		wikiLinkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		wikiLinkLabel.setForeground(ColorScheme.GRAND_EXCHANGE_LIMIT);
		wikiLinkLabel.addActionListener(e -> LinkBrowser.browse(WIKI_HELP_URL));

		// instruction label
		JLabel instructionLabel = new JLabel();
		instructionLabel.setText("<html>This window is used to merge two opposing fighters' fight data in order to " +
			"get more accurate stats about the fight, since some data is only available client-side. Both data " +
			"entries should come from the same fight, but from two different clients. Fighter 2 is Fighter 1's " +
			"opponent. Right click a fight in order to copy its data.<br/><br/>" +
			"When using this, the following stats are applied to deserved damage & deserved magic hits:<br/>" +
			"&nbsp;&nbsp;&mdash; Offensive prayers, instead of always being correct<br/>" +
			"&nbsp;&nbsp;&mdash; Boosted or drained levels (e.g from brewing down), instead of using config stats or fixed LMS stats<br/>" +
			"&nbsp;&nbsp;&mdash; The magic defence buff from Augury, instead of assuming Piety/Rigour while getting maged (if it's used)" +
			"<br><br><strong>Note: </strong>For now, ghost barrages are not integrated into this, the above improvements " +
			"do not apply for its deserved damage, and its deserved damage is not included in the main deserved damage " +
			"stat. It merely displays what each client had saved.</html>");
		instructionLabel.setForeground(Color.WHITE);
		instructionLabel.setSize(mainPanel.getWidth(), instructionLabel.getHeight());
		instructionLabel.setHorizontalAlignment(SwingConstants.LEFT);
		instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		// fight data input labels
		JLabel firstFightLabel = new JLabel();
		firstFightLabel.setText("<html><strong>Enter fight data for Fighter 1:</strong><br/>You can drag & drop a text file onto the text box.</html>");
		firstFightLabel.setForeground(Color.WHITE);
		textLabelLine.add(firstFightLabel);

		JLabel secondFightLabel = new JLabel();
		secondFightLabel.setText("<html><strong>Enter fight data for Fighter 2:</strong><br/>You can drag & drop a text file onto the text box.</html>");
		secondFightLabel.setForeground(Color.WHITE);
		textLabelLine.add(secondFightLabel);
		textLabelLine.setSize(mainPanel.getWidth(), 48);
		textLabelLine.setMaximumSize(new Dimension(mainPanel.getWidth(), 48));

		// fight data input fields
		mainFightJsonInput = new JTextField(32);
		mainFightJsonInput.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		mainFightJsonInput.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		// setup drag & drop file upload support directly into textbox
		// https://stackoverflow.com/a/9111327/7982774
		mainFightJsonInput.setDropTarget(new DropTarget() {
			public synchronized void drop(DropTargetDropEvent evt) {
				try
				{
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					List<File> droppedFiles = (List<File>)
						evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

					for (File file : droppedFiles)
					{
						String fileData = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
						mainFightJsonInput.setText(fileData);
						FightAnalysisFrame.this.validate();
						break;
					}

					evt.dropComplete(true);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});

		textAreaLine.add(mainFightJsonInput);
		opponentFightJsonInput = new JTextField(32);
		opponentFightJsonInput.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		opponentFightJsonInput.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		// setup drag & drop file upload support directly into textbox
		opponentFightJsonInput.setDropTarget(new DropTarget() {
			public synchronized void drop(DropTargetDropEvent evt) {
				try
				{
					evt.acceptDrop(DnDConstants.ACTION_COPY);
					List<File> droppedFiles = (List<File>)
						evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

					for (File file : droppedFiles)
					{
						String fileData = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
						opponentFightJsonInput.setText(fileData);
						FightAnalysisFrame.this.validate();
						break;
					}

					evt.dropComplete(true);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});

		textAreaLine.add(opponentFightJsonInput);
		textAreaLine.setSize(mainPanel.getWidth(), 32);
		textAreaLine.setMaximumSize(new Dimension(mainPanel.getWidth(), 32));

		// confirm button
		JButton confirmButton = new JButton("<html><strong>Merge Fight Data</strong></html>");
		confirmButton.setSize(256, 32);
		confirmButton.setMaximumSize(new Dimension(256, 32));
		confirmButton.addActionListener(e -> performAnalysis());
		confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		// add all components
		mainPanel.add(wikiLinkLabel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
		mainPanel.add(instructionLabel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 16)));
		mainPanel.add(textLabelLine);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 4)));
		mainPanel.add(textAreaLine);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 16)));
		mainPanel.add(confirmButton);

		validate();
		repaint();
	}

	// parse fights into mainFight / opponentFight. Returns false if either are invalid.
	private boolean parseFights()
	{
		try
		{
			// if either fight or their log entries are null, error
			mainFight = PLUGIN.GSON.fromJson(mainFightJsonInput.getText().trim(), FightPerformance.class);
			if (mainFight == null || mainFight.getAllFightLogEntries() == null || mainFight.getAllFightLogEntries().size() < 1)
			{
				PLUGIN.createConfirmationModal(false, "Error parsing Fighter 1's fight data.");
				mainFight = null;

				return false;
			}

			opponentFight = PLUGIN.GSON.fromJson(opponentFightJsonInput.getText().trim(), FightPerformance.class);
			if (opponentFight == null || opponentFight.getAllFightLogEntries() == null || opponentFight.getAllFightLogEntries().size() < 1)
			{
				PLUGIN.createConfirmationModal(false, "Error parsing Fighter 2's fight data.");
				opponentFight = null;

				return false;
			}


			PLUGIN.initializeImportedFight(mainFight);
			PLUGIN.initializeImportedFight(opponentFight);
		}
		catch(Exception e)
		{
			PLUGIN.createConfirmationModal(false, "Error while parsing fight data.");
			return false;
		}

		return true;
	}

	// start fight parsing, and if fights are valid then move onto the next "state" of the frame
	// where we will display stats about the fight.
	private void performAnalysis()
	{
		boolean fightsValid = parseFights();
		// parseFights includes error messages if the parse fails
		if (!fightsValid) { return; }

		try
		{

			analyzedFight = new AnalyzedFightPerformance(mainFight, opponentFight, this::displayAnalysis);
			// now that we've got the merged fight, display results, this is done with the displayAnalysis callback
		}
		catch(Exception e)
		{
			log.info("Error during fight analysis - could not merge fights. Exception tack trace: ", e);
			PLUGIN.createConfirmationModal(false, "<html>Error while merging fights. Unable to analyze.<br/>If you think this should have been valid, feel free to submit<br/>an issue on the github repo, and include client logs.</html>");
		}
	}

	private void displayAnalysis()
	{
		mainPanel.removeAll();
		setTitle(WINDOW_TITLE + " - " + analyzedFight.competitor.getName() + " vs " + analyzedFight.opponent.getName());

		// intro label
		JLabel mergedFightLabel = new JLabel("<html><strong>Merged Fight &mdash; Click the panel for more details.</strong></html>");
		mergedFightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		mergedFightLabel.setHorizontalAlignment(SwingConstants.CENTER);

		// analyzed fight
		FightPerformancePanel analyzedFightPanel = new FightPerformancePanel(analyzedFight);
		analyzedFightPanel.setSize(220, 134);
		analyzedFightPanel.setMaximumSize(new Dimension(220, 134));
		analyzedFightPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		// back to setup/config button
		JButton backButton = new JButton("<html><strong>Back</strong></html>");
		backButton.setSize(256, 32);
		backButton.setMaximumSize(new Dimension(256, 32));
		backButton.addActionListener(e -> initializeFrame());
		backButton.setAlignmentX(Component.CENTER_ALIGNMENT);


		// checkbox to show initial fights
		JCheckBox initialFightCheckbox = new JCheckBox();
		initialFightCheckbox.setText("Show initial fights");
		initialFightCheckbox.setSelected(false);
		initialFightCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);

		JPanel initialFightsPanel = new JPanel();
		initialFightsPanel.setLayout(new BoxLayout(initialFightsPanel, BoxLayout.X_AXIS));
		initialFightsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		// initial fights
		FightPerformancePanel mainFightPanel = new FightPerformancePanel(mainFight, false, true, false, null);
		mainFightPanel.setSize(220, 134);
		mainFightPanel.setMaximumSize(new Dimension(220, 134));
		initialFightsPanel.add(mainFightPanel);
		initialFightsPanel.add(Box.createRigidArea(new Dimension(8, 0)));

		FightPerformancePanel oppFightPanel = new FightPerformancePanel(opponentFight, false, true, false, null);
		oppFightPanel.setSize(220, 134);
		oppFightPanel.setMaximumSize(new Dimension(220, 134));
		initialFightsPanel.add(oppFightPanel);
		initialFightsPanel.setVisible(false);


		initialFightCheckbox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				initialFightsPanel.setVisible(true);
				validate();
				repaint();
			}
			else
			{
				initialFightsPanel.setVisible(false);
				validate();
				repaint();
			}
		});

		// add all components
		mainPanel.add(mergedFightLabel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		mainPanel.add(analyzedFightPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		mainPanel.add(backButton);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		mainPanel.add(initialFightCheckbox);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
		mainPanel.add(initialFightsPanel);

		validate();
		repaint();
	}
}
