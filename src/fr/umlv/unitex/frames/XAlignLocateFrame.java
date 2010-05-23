/*
 * Unitex
 *
 * Copyright (C) 2001-2010 Université Paris-Est Marne-la-Vallée <unitex@univ-mlv.fr>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA.
 *
 */

package fr.umlv.unitex.frames;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import fr.umlv.unitex.Config;
import fr.umlv.unitex.PersonalFileFilter;
import fr.umlv.unitex.ToDo;
import fr.umlv.unitex.Util;
import fr.umlv.unitex.exceptions.InvalidConcordanceOrderException;
import fr.umlv.unitex.exceptions.NotAUnicodeLittleEndianFileException;
import fr.umlv.unitex.io.UnicodeIO;
import fr.umlv.unitex.process.Launcher;
import fr.umlv.unitex.process.commands.ConcordCommand;
import fr.umlv.unitex.process.commands.Grf2Fst2Command;
import fr.umlv.unitex.process.commands.LocateCommand;
import fr.umlv.unitex.process.commands.MultiCommands;
import fr.umlv.unitex.process.commands.Reg2GrfCommand;
import fr.umlv.unitex.xalign.ConcordanceLoader;
import fr.umlv.unitex.xalign.ConcordanceModel;


/**
 * This class describes the XAlign locate pattern frame.
 * 
 * @author Sébastien Paumier
 *  
 */
public class XAlignLocateFrame extends JInternalFrame {

	JRadioButton regularExpression = new JRadioButton("Regular expression:",false);
	JRadioButton graph = new JRadioButton("Graph:", true);
	JTextField regExp = new JTextField();
	JTextField graphName = new JTextField();
	JRadioButton shortestMatches = new JRadioButton("Shortest matches", false);
	JRadioButton longuestMatches = new JRadioButton("Longest matches", true);
	JRadioButton allMatches = new JRadioButton("All matches", false);
	JRadioButton stopAfterNmatches = new JRadioButton("Stop after ", true);
	JRadioButton indexAllMatches = new JRadioButton("Index all utterances in text", false);
	JTextField nMatches = new JTextField("200");

	String language;
	File snt;
	ConcordanceModel concordModel;
	
	
	XAlignLocateFrame() {
		super("XAlign Locate Pattern", false, true);
		setContentPane(constructPanel());
		pack();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}

	
	void configure(String language1,File snt1,ConcordanceModel concordModel1) {
		this.language=language1;
		this.snt=snt1;
		this.concordModel=concordModel1;		
	}

	private JPanel constructPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(constructPatternPanel(), BorderLayout.CENTER);
		panel.add(constructDownPanel(), BorderLayout.SOUTH);
		return panel;
	}

	private JFileChooser grfAndFst2DialogBox;
	
	JFileChooser getGrfAndFst2DialogBox(File dir) {
		if (grfAndFst2DialogBox != null) {
			grfAndFst2DialogBox.setCurrentDirectory(dir);
			return grfAndFst2DialogBox;
		}
		grfAndFst2DialogBox = new JFileChooser();
		grfAndFst2DialogBox.addChoosableFileFilter(new PersonalFileFilter(
				"fst2", "Unicode Compiled Graphs"));
		grfAndFst2DialogBox.addChoosableFileFilter(new PersonalFileFilter(
				"grf", "Unicode Graphs"));
		grfAndFst2DialogBox.setDialogType(JFileChooser.OPEN_DIALOG);
		grfAndFst2DialogBox.setCurrentDirectory(dir);
		grfAndFst2DialogBox.setMultiSelectionEnabled(false);
		return grfAndFst2DialogBox;
	}
	
	private JPanel constructPatternPanel() {
		JPanel patternPanel = new JPanel(new BorderLayout());
		patternPanel.setBorder(new TitledBorder(
				"Locate pattern in the form of:"));
		final File graphDir=new File(new File(Config.getUserDir(),language),"Graphs");
		Action setGraphAction = new AbstractAction("Set") {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser grfAndFst2 = getGrfAndFst2DialogBox(graphDir);
				int returnVal = grfAndFst2.showOpenDialog(null);
				if (returnVal != JFileChooser.APPROVE_OPTION) {
					// we return if the user has clicked on CANCEL
					return;
				}
				graphName.setText(grfAndFst2.getSelectedFile()
						.getAbsolutePath());
				graph.setSelected(true);
			}
		};
		JButton setGraphButton = new JButton(setGraphAction);
		ButtonGroup bg = new ButtonGroup();
		graphName.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent arg0) {
				graph.setSelected(true);
			}
		});
		regExp.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent arg0) {
				regularExpression.setSelected(true);
			}
		});
		bg.add(regularExpression);
		bg.add(graph);
		patternPanel.add(regularExpression, BorderLayout.NORTH);
		regExp.setPreferredSize(new Dimension(300, 30));
		patternPanel.add(regExp, BorderLayout.CENTER);
		JPanel p = new JPanel(new BorderLayout());
		p.add(graph, BorderLayout.WEST);
		p.add(graphName, BorderLayout.CENTER);
		p.add(setGraphButton, BorderLayout.EAST);
		patternPanel.add(p, BorderLayout.SOUTH);
		return patternPanel;
	}

	private JPanel constructDownPanel() {
		JPanel downPanel = new JPanel(new BorderLayout());
		JPanel b = new JPanel(new BorderLayout());
		b.add(constructSearchLimitationPanel(), BorderLayout.WEST);
		final XAlignLocateFrame f=this;
		Action searchAction = new AbstractAction("SEARCH") {
			public void actionPerformed(ActionEvent arg0) {
				f.launchLocate();
			}
		};
		JButton searchButton = new JButton(searchAction);
		b.add(searchButton, BorderLayout.CENTER);
		downPanel.add(constructIndexPanel(), BorderLayout.CENTER);
		downPanel.add(b, BorderLayout.SOUTH);
		return downPanel;
	}

	private JPanel constructIndexPanel() {
		JPanel indexPanel = new JPanel(new GridLayout(3, 1));
		indexPanel.setBorder(new TitledBorder("Index"));
		ButtonGroup bg = new ButtonGroup();
		bg.add(shortestMatches);
		bg.add(longuestMatches);
		bg.add(allMatches);
		indexPanel.add(shortestMatches);
		indexPanel.add(longuestMatches);
		indexPanel.add(allMatches);
		return indexPanel;
	}

	private JPanel constructSearchLimitationPanel() {
		JPanel searchLimitationPanel = new JPanel(new GridLayout(2, 1));
		searchLimitationPanel.setBorder(new TitledBorder("Search limitation"));
		JPanel p = new JPanel(new BorderLayout());
		p.add(stopAfterNmatches, BorderLayout.WEST);
		p.add(nMatches, BorderLayout.CENTER);
		p.add(new JLabel(" matches"), BorderLayout.EAST);
		ButtonGroup bg = new ButtonGroup();
		bg.add(stopAfterNmatches);
		bg.add(indexAllMatches);
		searchLimitationPanel.add(p);
		searchLimitationPanel.add(indexAllMatches);
		return searchLimitationPanel;
	}

	void launchLocate() {
		MultiCommands commands = new MultiCommands();
		File fst2;
		int n=-1;
		if (stopAfterNmatches.isSelected()) {
			try {
				n=Integer.parseInt(nMatches.getText());
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(null,
						"Invalid empty search limitation value !", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		if (regularExpression.isSelected()) {
			// we need to process a regular expression
			if (regExp.getText().equals("")) {
				JOptionPane.showMessageDialog(null,
						"Empty regular expression !", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			File regexpFile = new File(new File(Config.getUserDir(),language),
					"regexp.txt");
			createRegExpFile(regExp.getText(), regexpFile);
			Reg2GrfCommand reg2GrfCmd = new Reg2GrfCommand().file(regexpFile);
			commands.addCommand(reg2GrfCmd);
			Grf2Fst2Command grfCmd = new Grf2Fst2Command().grf(new File(new File(Config.getUserDir(),language), "regexp.grf"))
					.enableLoopAndRecursionDetection(true)
					.tokenizationMode().library();
			commands.addCommand(grfCmd);
			fst2 = new File(new File(Config.getUserDir(),language), "regexp.fst2");
		} else {
			// we need to process a graph
			if (graphName.getText().equals("")) {
				JOptionPane.showMessageDialog(null,
						"You must specify a graph name", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			String grfName = graphName.getText();
			if (grfName.substring(grfName.length() - 3, grfName.length())
					.equalsIgnoreCase("grf")) {
				// we must compile the grf
				Grf2Fst2Command grfCmd = new Grf2Fst2Command().grf(new File(
						grfName)).enableLoopAndRecursionDetection(true)
						.tokenizationMode().library();
				commands.addCommand(grfCmd);
				String fst2Name = grfName.substring(0, grfName.length() - 3);
				fst2Name = fst2Name + "fst2";
				fst2 = new File(fst2Name);
			} else {
				if (!(grfName.substring(grfName.length() - 4, grfName.length())
						.equalsIgnoreCase("fst2"))) {
					// if the extension is nor GRF neither FST2
					JOptionPane.showMessageDialog(null,
							"Invalid graph name extension !", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				fst2 = new File(grfName);
			}
		}
		File alphabet=new File(new File(Config.getUserDir(),language),"Alphabet.txt");
		LocateCommand locateCmd = new LocateCommand().snt(snt).fst2(fst2).alphabet(alphabet);
		if (shortestMatches.isSelected())
			locateCmd = locateCmd.shortestMatches();
		else if (longuestMatches.isSelected())
			locateCmd = locateCmd.longestMatches();
		else
			locateCmd = locateCmd.allMatches();
		locateCmd = locateCmd.ignoreOutputs();
		if (Config.isKorean() || Config.isKoreanJeeSun()) {
			locateCmd=locateCmd.korean();
		}
		if (stopAfterNmatches.isSelected()) {
			locateCmd = locateCmd.limit(n);
		} else {
			locateCmd = locateCmd.noLimit();
		}
		if (Config.isCharByCharLanguage(language)) {
			locateCmd = locateCmd.charByChar();
		}
		if (Config.morphologicalUseOfSpaceAllowed(language)) {
			locateCmd = locateCmd.enableMorphologicalUseOfSpace();
		}
		locateCmd=locateCmd.morphologicalDic(Config.morphologicalDic(language));
		commands.addCommand(locateCmd);
		String foo=Util.getFileNameWithoutExtension(snt)+"_snt";
		File indFile=new File(foo,"concord.ind");
		ConcordCommand concord=null;
		try {
			concord = new ConcordCommand().indFile(indFile).font("NULL")
					.fontSize(0).left(0,false).right(0,false).order(0)
					.xalign();
			commands.addCommand(concord);
		} catch (InvalidConcordanceOrderException e) {
			e.printStackTrace();
		}
		setVisible(false);
		foo=Util.getFileNameWithoutExtension(indFile)+".txt";
		Launcher.exec(commands,true,new XAlignLocateDo(new File(foo),concordModel),false);
	}


	private void createRegExpFile(String regExp2, File f) {
		try {
			if (!f.exists()) {
				f.createNewFile();
			}
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f), "UTF-16LE"));
			bw.write('\ufeff');
			bw.write(regExp2, 0, regExp2.length());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static String readInfo(File file) {
		if (!file.exists()) {
			return null;
		}
		if (!file.canRead()) {
			return null;
		}
		String res;
		FileInputStream source;
		try {
			source = UnicodeIO.openUnicodeLittleEndianFileInputStream(file);
			res = UnicodeIO.readLine(source) + "\n";
			res = res + UnicodeIO.readLine(source) + "\n";
			res = res + UnicodeIO.readLine(source);
			source.close();
		} catch (NotAUnicodeLittleEndianFileException e) {
			return null;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		return res;
	}

	class XAlignLocateDo implements ToDo {
		
		File file;
		ConcordanceModel concordModel1;
		
		public XAlignLocateDo(File file,ConcordanceModel concordModel) {
			this.file=file;
			this.concordModel1=concordModel;
		}

		public void toDo() {
			ConcordanceLoader.load(file,concordModel1);
		}
	}

}