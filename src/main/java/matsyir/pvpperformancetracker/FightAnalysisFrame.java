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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.RoundingMode;
import java.text.NumberFormat;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
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
		setSize(765, 256);
		setLocation(rootPane.getLocationOnScreen());

		//mainPanel = new JPanel(new BorderLayout(4, 4));
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setPreferredSize(getSize());
		mainPanel.setVisible(true);
		add(mainPanel);
//		ArrayList<FightLogEntry> fightLogEntries = fight.getAllFightLogEntries();
//		if (fightLogEntries == null || fightLogEntries.size() < 1)
//		{
//			PLUGIN.createConfirmationModal("Info", "There are no fight log entries available for this fight.");
//			return;
//		}

		//mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);
		setVisible(true);
		initializeFrame();
	}

	// setup the initial frame contents, which should be two textareas used to input
	// two single related fightPerformance json data.
	private void initializeFrame()
	{

		mainPanel.removeAll();
		JPanel instructionLabelLine = new JPanel(new BorderLayout(4, 4));
		JPanel textLabelLine = new JPanel(new BorderLayout(4, 4));
		textLabelLine.setPreferredSize(new Dimension(getWidth(), 18));
		JPanel textAreaLine = new JPanel(new BorderLayout(4, 4));
		JPanel actionLine = new JPanel(new BorderLayout(4, 4));

		JLabel instructionLabel = new JLabel();
		instructionLabel.setText("This panel is used to merge two fighter's fights in order to get more accurate stats about the fight, as some data is only client-side.");
		instructionLabel.setForeground(Color.WHITE);
		instructionLabelLine.add(instructionLabel, BorderLayout.NORTH);

		JLabel firstFightLabel = new JLabel();
		firstFightLabel.setText("Enter fight JSON data for Fighter 1:");
		firstFightLabel.setForeground(Color.WHITE);
		textLabelLine.add(firstFightLabel, BorderLayout.WEST);

		JLabel secondFightLabel = new JLabel();
		secondFightLabel.setText("Enter fight JSON data for Fighter 2:");
		secondFightLabel.setForeground(Color.WHITE);
		textLabelLine.add(secondFightLabel, BorderLayout.EAST);

		mainFightJsonInput = new JTextField(32);
		textAreaLine.add(mainFightJsonInput, BorderLayout.WEST);

		opponentFightJsonInput = new JTextField(32);
		textAreaLine.add(opponentFightJsonInput, BorderLayout.EAST);

		JButton confirmButton = new JButton("Merge & Analyze");
		confirmButton.addActionListener(e -> {
			// do stuff on click
			performAnalysis();
		});
		actionLine.add(confirmButton, BorderLayout.CENTER);

		mainPanel.add(instructionLabelLine);
		mainPanel.add(textLabelLine);
		mainPanel.add(textAreaLine);
		mainPanel.add(actionLine);
	}

	// start fight parsing, and if fights are valid then move onto the next "state" of the frame
	// where we will display stats about the fight.
	private void performAnalysis()
	{
		parseFights();

		// do stuff
	}

	private void parseFights()
	{
		mainFight = PLUGIN.gson.fromJson(mainFightJsonInput.getText().trim(), FightPerformance.class);
		if (mainFight == null || mainFight.getAllFightLogEntries() == null || mainFight.getAllFightLogEntries().size() < 1)
		{
			// error
			PLUGIN.createConfirmationModal("Error", "Error parsing Fighter 1's fight data.");
			mainFight = null;
		}

		opponentFight = PLUGIN.gson.fromJson(opponentFightJsonInput.getText().trim(), FightPerformance.class);
		if (opponentFight == null || opponentFight.getAllFightLogEntries() == null || opponentFight.getAllFightLogEntries().size() < 1)
		{
			// error
			PLUGIN.createConfirmationModal("Error", "Error parsing Fighter 2's fight data.");
			opponentFight = null;
		}
	}
}
