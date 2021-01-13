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
package matsyir.pvpperformancetracker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import static matsyir.pvpperformancetracker.PvpPerformanceTrackerPlugin.PLUGIN;
import net.runelite.client.util.ImageUtil;

public class FightAnalysisFrame extends JFrame
{
	static Image frameIcon;
	private static final NumberFormat nf = NumberFormat.getInstance();

	private JPanel mainPanel;
	private JTextField mainFightJsonInput;
	private JTextField opponentFightJsonInput;

	private FightPerformance mainFight;
	private FightPerformance opponentFight;
	private FightPerformance analyzedFight;

	static
	{
		frameIcon = new ImageIcon(ImageUtil.getResourceStreamFromClass(FightLogFrame.class, "/skull_red.png")).getImage();

		// initialize number format
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
	}

	FightAnalysisFrame(JRootPane rootPane)
	{
		super("Fight Analysis (Advanced)");
		// if always on top is supported, and the core RL plugin has "always on top" set, make the frame always
		// on top as well so it can be above the client.
		if (isAlwaysOnTopSupported())
		{
			setAlwaysOnTop(PLUGIN.getRuneliteConfig().gameAlwaysOnTop());
		}

		setIconImage(frameIcon);
		Dimension size = new Dimension(640, 256);
		setSize(size);
		setMinimumSize(size);
		setLocation(rootPane.getLocationOnScreen());

		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setPreferredSize(getSize());
		mainPanel.setVisible(true);
		mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

		add(mainPanel);

		setVisible(true);
		initializeFrame();
	}

	// setup the initial frame contents, which should be two text inputs used to input
	// two single related fightPerformance json data.
	private void initializeFrame()
	{
		mainPanel.removeAll();

		JPanel instructionLabelLine = new JPanel(new BorderLayout());
		JPanel textLabelLine = new JPanel(new BorderLayout());
		GridLayout textAreaLayout = new GridLayout(1, 2);
		textAreaLayout.setHgap(4);
		textAreaLayout.setVgap(4);
		JPanel textAreaLine = new JPanel(textAreaLayout);
		JPanel actionLine = new JPanel(new BorderLayout());
		actionLine.setBorder(BorderFactory.createEmptyBorder(8, 64, 0, 64));

		JLabel instructionLabel = new JLabel();
		instructionLabel.setText("<html>This panel is used to merge two opposing fighters' fight data in order to get more accurate stats about the fight, since some data is only available client-side. Both data entries should come from the same fight, but from two different clients. Fighter 2 is Fighter 1's opponent.</html>");
		instructionLabel.setForeground(Color.WHITE);
		instructionLabelLine.add(instructionLabel, BorderLayout.CENTER);

		JLabel firstFightLabel = new JLabel();
		firstFightLabel.setText("<html>Enter fight data for Fighter 1:</html>");
		firstFightLabel.setForeground(Color.WHITE);
		textLabelLine.add(firstFightLabel, BorderLayout.WEST);

		JLabel secondFightLabel = new JLabel();
		secondFightLabel.setText("<html>Enter fight data for Fighter 2:</html>");
		secondFightLabel.setForeground(Color.WHITE);
		textLabelLine.add(secondFightLabel, BorderLayout.EAST);

		mainFightJsonInput = new JTextField(32);
		mainFightJsonInput.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		textAreaLine.add(mainFightJsonInput);
		opponentFightJsonInput = new JTextField(32);
		opponentFightJsonInput.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		textAreaLine.add(opponentFightJsonInput);

		JButton confirmButton = new JButton("✔ Merge Fight Data");
//		confirmButton.setSize(256, 32);
//		confirmButton.setPreferredSize(new Dimension(256, 32));
		confirmButton.addActionListener(e -> performAnalysis());
		actionLine.add(confirmButton, BorderLayout.CENTER);

		mainPanel.add(instructionLabelLine);
		mainPanel.add(textLabelLine);
		mainPanel.add(textAreaLine);
		mainPanel.add(actionLine);

		validate();
	}

	// parse fights into mainFight / opponentFight. Returns false if either are invalid.
	private boolean parseFights()
	{
		try
		{
			// if either fight or their log entries are null, error
			mainFight = PLUGIN.gson.fromJson(mainFightJsonInput.getText().trim(), FightPerformance.class);
			if (mainFight == null || mainFight.getAllFightLogEntries() == null || mainFight.getAllFightLogEntries().size() < 1)
			{
				PLUGIN.createConfirmationModal(false, "Error parsing Fighter 1's fight data.");
				mainFight = null;

				return false;
			}

			opponentFight = PLUGIN.gson.fromJson(opponentFightJsonInput.getText().trim(), FightPerformance.class);
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

		if (!fightsValid) { return; }

		// now that we know fights are valid, merge them to get more detailed data:
		// only save attacks sent by the client/"main" fighter, so ones with the competitor's RSN
//		ArrayList<FightLogEntry> mainFightDetailedEntries = new ArrayList<>(mainFight.getAllFightLogEntries());
//		mainFightDetailedEntries.removeIf(e -> !e.attackerName.equals(mainFight.getCompetitor().getName()));
//
//		ArrayList<FightLogEntry> oppFightDetailedEntries = new ArrayList<>(opponentFight.getAllFightLogEntries());
//		oppFightDetailedEntries.removeIf(e -> !e.attackerName.equals(opponentFight.getCompetitor().getName()));


		try
		{
			analyzedFight = new FightPerformance(mainFight, opponentFight/*, mainFightDetailedEntries, oppFightDetailedEntries*/);
		}
		catch(Exception e)
		{
			PLUGIN.createConfirmationModal(false, "Error while merging fights. Unable to analyze.");
		}

		// now that we've got the merged fight, display results;
		displayAnalysis();
	}

	private void displayAnalysis()
	{
		mainPanel.removeAll();

		JPanel backButtonLine = new JPanel(new BorderLayout(4, 4));
		JButton backButton = new JButton("Return to setup");
		backButton.addActionListener(e -> initializeFrame());
		backButtonLine.add(backButton);

		JPanel fightPanelLine = new JPanel(new BorderLayout(12, 12));
		FightPerformancePanel mainFightPanel = new FightPerformancePanel(mainFight, false, false, false, null);
		FightPerformancePanel opponentFightPanel = new FightPerformancePanel(opponentFight, false, false, false, null);
		FightPerformancePanel analyzedFightPanel = new FightPerformancePanel(analyzedFight, false, false, true, opponentFight);

		fightPanelLine.add(mainFightPanel, BorderLayout.WEST);
		fightPanelLine.add(opponentFightPanel, BorderLayout.EAST);
		fightPanelLine.add(analyzedFightPanel, BorderLayout.CENTER);

		mainPanel.add(backButtonLine);
		mainPanel.add(fightPanelLine);
		validate();
	}
}
