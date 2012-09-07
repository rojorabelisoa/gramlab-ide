/*
 * Unitex
 *
 * Copyright (C) 2001-2012 Université Paris-Est Marne-la-Vallée <unitex@univ-mlv.fr>
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import fr.umlv.unitex.DropTargetManager;
import fr.umlv.unitex.concord.BigConcordance;
import fr.umlv.unitex.config.Config;
import fr.umlv.unitex.config.ConfigManager;
import fr.umlv.unitex.console.Console;
import fr.umlv.unitex.exceptions.InvalidConcordanceOrderException;
import fr.umlv.unitex.files.FileUtil;
import fr.umlv.unitex.graphrendering.GenericGraphBox;
import fr.umlv.unitex.graphrendering.TfstGraphBox;
import fr.umlv.unitex.graphrendering.TfstGraphicalZone;
import fr.umlv.unitex.graphrendering.TfstTextField;
import fr.umlv.unitex.io.Encoding;
import fr.umlv.unitex.io.GraphIO;
import fr.umlv.unitex.io.UnicodeIO;
import fr.umlv.unitex.listeners.GraphListener;
import fr.umlv.unitex.process.EatStreamThread;
import fr.umlv.unitex.process.Launcher;
import fr.umlv.unitex.process.Log;
import fr.umlv.unitex.process.ToDo;
import fr.umlv.unitex.process.commands.ConcordCommand;
import fr.umlv.unitex.process.commands.LocateTfstCommand;
import fr.umlv.unitex.process.commands.MultiCommands;
import fr.umlv.unitex.process.commands.RebuildTfstCommand;
import fr.umlv.unitex.process.commands.Tfst2GrfCommand;
import fr.umlv.unitex.tfst.TokensInfo;

/**
 * This class describes a frame used to lemmatize sentence automata.
 * 
 * @author Sébastien Paumier
 */
public class LemmatizeFrame extends TfstFrame {
	
	JPanel concordancePanel=new JPanel(new BorderLayout());
	BigConcordance list;
	JComboBox lemmaCombo=new JComboBox();
	
	final JLabel sentence_count_label = new JLabel(" 0 sentence");
	JSpinner spinner;
	SpinnerNumberModel spinnerModel;
	JScrollBar tfstScrollbar;
	TfstGraphicalZone graphicalZone;
	public JScrollPane scrollPane;
	private final GraphListener listener = new GraphListener() {
		@Override
		public void graphChanged(boolean m) {
			if (m)
				setModified(true);
			repaint();
		}
	};

	@Override
	public JScrollPane getTfstScrollPane() {
		return scrollPane;
	}

	@Override
	public TfstGraphicalZone getTfstGraphicalZone() {
		return graphicalZone;
	}

	private final TfstTextField textfield = new TfstTextField(25, this);
	boolean modified = false;
	int sentence_count = 0;
	File sentence_text;
	File sentence_grf;
	File sentence_tok;
	File sentence_modified;
	File text_tfst;
	boolean isAcurrentLoadingThread = false;
	JSplitPane superpanel;
	JButton resetSentenceGraph;

	LemmatizeFrame() {
		super("Lemmatization", true, true, true, true);
		DropTargetManager.getDropTarget().newDropTarget(this);
		setContentPane(constructPanel());
		pack();
		setBounds(10, 10, 900, 600);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addInternalFrameListener(new InternalFrameAdapter() {
			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				try {
					setIcon(true);
				} catch (final java.beans.PropertyVetoException e2) {
					e2.printStackTrace();
				}
			}
		});
		textfield.setEditable(false);
	}

	private JPanel constructPanel() {
		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(constructUpPanel(), BorderLayout.NORTH);
		final JScrollPane scroll = new JScrollPane(concordancePanel);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		superpanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				scroll,constructTextPanel());
		superpanel.setOneTouchExpandable(true);
		superpanel.setResizeWeight(0.5);
		panel.add(superpanel, BorderLayout.CENTER);
		return panel;
	}

	private JPanel constructTextPanel() {
		final JPanel textframe = new JPanel(new BorderLayout());
		final JPanel downPanel = new JPanel(new BorderLayout());
		graphicalZone = new TfstGraphicalZone(null, textfield, this, true);
		graphicalZone.addGraphListener(listener);
		graphicalZone.setPreferredSize(new Dimension(1188, 840));
		scrollPane = new JScrollPane(graphicalZone);
		tfstScrollbar = scrollPane.getHorizontalScrollBar();
		tfstScrollbar.setUnitIncrement(20);
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.setPreferredSize(new Dimension(1188, 840));
		textfield.setFont(ConfigManager.getManager().getInputFont(null));
		downPanel.add(textfield, BorderLayout.NORTH);
		downPanel.add(scrollPane, BorderLayout.CENTER);
		textframe.add(downPanel, BorderLayout.CENTER);
		return textframe;
	}


	private JPanel constructUpPanel() {
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints gbc=new GridBagConstraints();
		gbc.fill=GridBagConstraints.BOTH;
		p.add(constructSearchPanel(),gbc);
		p.add(constructLemmaPanel(),gbc);
		p.add(constructSentenceNavigationPanel(),gbc);
		gbc.weightx=1;
		p.add(new JPanel(),gbc);
		return p;
	}


	private JPanel constructLemmaPanel() {
		JPanel p = new JPanel(new GridLayout(3,1));
		p.setBorder(BorderFactory.createTitledBorder("Lemma selection"));
		lemmaCombo.setMinimumSize(new Dimension(200,lemmaCombo.getPreferredSize().height));
		p.add(lemmaCombo);
		JButton validateOne=new JButton("Validate for selected item");
		validateOne.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String s=(String) list.getSelectedValue();
				if (s==null) return;
				String lemma=(String)lemmaCombo.getSelectedItem();
				if (lemma==null) {
					JOptionPane.showMessageDialog(null, "You must select a lemma!",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				int[] res=getHrefInfos(s);
				int offsetInTokens=getOffsetInTokens();
				int matchStartInTokens=res[3]-offsetInTokens;
				/* Now that we have the lemma information and the start position
				 * of the match, we can locate the graph box to select as if with a
				 * right click
				 */
				int index=getBoxToSelectIndex(lemma,matchStartInTokens);
				
			}

			private int getBoxToSelectIndex(String lemma,int matchStartInTokens) {
				ArrayList<GenericGraphBox> boxes=graphicalZone.getBoxes();
				for (int i=0;i<boxes.size();i++) {
					TfstGraphBox b=(TfstGraphBox)boxes.get(i);
					if (b.getBounds()==null) continue;
					if (b.getBounds().getStart_in_tokens()!=matchStartInTokens) continue;
					String boxLemma;
					if (b.lines.size()==2) {
						boxLemma=b.lines.get(1)+"."+b.transduction;
					} else {
						boxLemma=b.lines.get(0)+"."+b.transduction;
					}
				if (!boxLemma.equals(lemma)) continue;
					return i;
				}
				return -1;
			}
		});
		JButton validateAll=new JButton("Global update");
		p.add(validateOne);
		p.add(validateAll);
		return p;
	}

	
	protected int getOffsetInTokens() {
		File start=new File(Config.getCurrentSntDir(),"cursentence.start");
		String content=Encoding.getContent(start);
		Scanner s=new Scanner(content);
		return s.nextInt();
	}

	private JPanel constructSearchPanel() {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder("Select elements to lemmatize"));
		JPanel p2=new JPanel(new BorderLayout());
		p2.add(new JLabel("Pattern: "),BorderLayout.WEST);
		final JTextField pattern=new JTextField();
		p2.add(pattern,BorderLayout.CENTER);
		JButton GO=new JButton("GO");
		p2.add(GO,BorderLayout.EAST);
		p.add(p2,BorderLayout.NORTH);

		JPanel p3=new JPanel(new GridBagLayout());
		GridBagConstraints gbc=new GridBagConstraints();
		p3.setBorder(BorderFactory.createTitledBorder("Show matching sequences in context"));
		gbc.fill=GridBagConstraints.BOTH;
		gbc.gridwidth=GridBagConstraints.RELATIVE;
		p3.add(new JLabel("Context lengths:"),gbc);
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		p3.add(new JLabel("Sort according to:"),gbc);
		gbc.gridwidth=1;
		final JTextField left=new JTextField("40");
		final JTextField right=new JTextField("55");
		final JTextField limit=new JTextField("200");
		left.setPreferredSize(new Dimension(50,15));
		right.setPreferredSize(new Dimension(50,15));
		limit.setPreferredSize(new Dimension(50,15));
		p3.add(new JLabel("Left: "),gbc);
		p3.add(left,gbc);
		p3.add(new JLabel(" chars  "),gbc);
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		final String[] items = new String[7];
		items[0] = "Text Order";
		items[1] = "Left, Center";
		items[2] = "Left, Right";
		items[3] = "Center, Left";
		items[4] = "Center, Right";
		items[5] = "Right, Left";
		items[6] = "Right, Center";
		final JComboBox sortBox = new JComboBox(items);
		sortBox.setSelectedIndex(3);
		p3.add(sortBox,gbc);
		gbc.gridwidth=1;
		p3.add(new JLabel("Right: "),gbc);
		p3.add(right,gbc);
		p3.add(new JLabel(" chars  "),gbc);
		final JRadioButton all=new JRadioButton("Show all elements",false);
		final JRadioButton unresolved=new JRadioButton("Show unresolved only",true);
		ButtonGroup bg=new ButtonGroup();
		bg.add(all);
		bg.add(unresolved);
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		p3.add(all,gbc);
		gbc.gridwidth=1;
		p3.add(new JLabel("Limit: "),gbc);
		p3.add(limit,gbc);
		limit.setToolTipText("Leave empty to get all matches");
		p3.add(new JLabel(" matches "),gbc);
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		p3.add(unresolved,gbc);
		p.add(p3,BorderLayout.CENTER);
		
		GO.addActionListener(new ActionListener() {
			
			/**
			 * Returns -2 if s is empty, -1 if not empty and not a positive integer,
			 * or n if s represents the value n.
			 */
			private int getInt(String s) {
				if (s.equals("")) return -2;
				try {
					int y=Integer.parseInt(s);
					if (y<0) return -1;
					return y;
				} catch (NumberFormatException e) {
					return -1;
				}
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String s=pattern.getText();
				if (s.equals("")) {
					JOptionPane.showMessageDialog(null, "You must specify a pattern!",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				int leftCtx=getInt(left.getText());
				if (leftCtx<0) {
					JOptionPane.showMessageDialog(null, "The left context must be a valid integer >=0",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				int rightCtx=getInt(right.getText());
				if (rightCtx<0) {
					JOptionPane.showMessageDialog(null, "The right context must be a valid integer >=0",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				int limitCtx=getInt(limit.getText());
				if (limitCtx==-1 || limitCtx==0) {
					JOptionPane.showMessageDialog(null, "The limit must either empty (all matches) or valid integer >0",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				final File fst2=new File(ConfigManager.getManager().getCurrentLanguageDir(),"lemmatize.fst2");
				createLemmatizeFst2(fst2,s);
				MultiCommands commands=new MultiCommands();
				LocateTfstCommand cmd1=new LocateTfstCommand().allowAmbiguousOutputs()
						.alphabet(ConfigManager.getManager().getAlphabet(null))
						.mergeOutputs()
						.tfst(new File(Config.getCurrentSntDir(),"text.tfst"))
						.fst2(fst2)
						.allMatches()
						.backtrackOnVariableErrors()
						.singleTagsOnly();
				if (limitCtx==-2) cmd1=cmd1.allMatches();
				else cmd1=cmd1.limit(limitCtx);
				if (ConfigManager.getManager().isKorean(null)) {
					cmd1=cmd1.korean();
				}
				commands.addCommand(cmd1);
				ConcordCommand cmd2;
				File indFile=new File(Config.getCurrentSntDir(),"concord.ind");
				try {
					cmd2 = new ConcordCommand()
							.indFile(indFile)
							.font(ConfigManager.getManager().getConcordanceFontName(
									null))
							.fontSize(
									ConfigManager.getManager().getConcordanceFontSize(
											null))
							.left(leftCtx, false)
							.right(rightCtx, false)
							.lemmatize()
							.sortAlphabet()
							.thai(ConfigManager.getManager().isThai(null));
					if (unresolved.isSelected()) {
						cmd2 = cmd2.onlyAmbiguous();
					} else {
						cmd2 = cmd2.order(sortBox.getSelectedIndex());
					}
					if (ConfigManager.getManager().isPRLGLanguage(null)) {
						final File prlgIndex = new File(Config.getCurrentSntDir(),
								"prlg.idx");
						final File offsets = new File(Config.getCurrentSntDir(),
								"tokenize.out.offsets");
						if (prlgIndex.exists() && offsets.exists()) {
							cmd2 = cmd2.PRLG(prlgIndex, offsets);
						}
					}
				} catch (final InvalidConcordanceOrderException e2) {
					e2.printStackTrace();
					return;
				}
				commands.addCommand(cmd2);
				final File html=new File(Config.getCurrentSntDir(),"concord.html");
				ToDo after=new ToDo() {
					
					@Override
					public void toDo(boolean success) {
						fst2.delete();
						loadTfst();
						loadConcordance(html);
					}

				};
				concordancePanel.removeAll();
				Launcher.exec(commands,true,after);
			}

			private void createLemmatizeFst2(File fst2,String s) {
				OutputStreamWriter writer=Encoding.UTF8.getOutputStreamWriter(fst2);
				try {
					writer.write("0000000001\n");
					writer.write("-1 biniou\n");
					writer.write(": 1 1 \n");
					writer.write(": 2 2 \n");
					writer.write("t \n");
					writer.write("f \n");
					writer.write("%<E>\n");
					writer.write("%"+s+"/$:x$\n");
					writer.write("%<E>//$x.LEMMA$.$x.CODE$\n");
					writer.write("f\n");
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
		return p;
	}
	
	void loadConcordance(File html) {
		if (list!=null) {
			list.reset();
		}
		concordancePanel.removeAll();
		list=new BigConcordance();
		concordancePanel.add(list,BorderLayout.CENTER);
		concordancePanel.revalidate();
		concordancePanel.repaint();
		list.setFont(ConfigManager.getManager().getConcordanceFont(null));
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final String s = (String) list.getSelectedValue();
				if (s == null || e.getValueIsAdjusting())
					return;
				int[] res=getHrefInfos(s);
				spinner.setValue(res[2]);
				loadSentence(res[2]);
				lemmaCombo.setModel(getLemmaModel(s));
				lemmaCombo.revalidate();
				lemmaCombo.repaint();
			}

			private ComboBoxModel getLemmaModel(String s) {
				Vector<String> vector=new Vector<String>();
				int start=s.indexOf("<!--")+4;
				int end=s.indexOf("-->",start);
				int n=Integer.parseInt(""+s.subSequence(start,end));
				for (int i=0;i<n;i++) {
					start=s.indexOf("<!--",end)+4;
					end=s.indexOf("-->",start);
					vector.add(""+s.subSequence(start,end));
				}
				return new DefaultComboBoxModel(vector);
			}
		});
		list.load(html);
	}

	
	private JPanel constructSentenceNavigationPanel() {
		final JPanel p = new JPanel(new GridLayout(5, 1));
		p.setBorder(BorderFactory.createTitledBorder("Sentence navigation"));
		p.add(sentence_count_label);
		final JPanel middle = new JPanel(new BorderLayout());
		middle.add(new JLabel(" Sentence # "), BorderLayout.WEST);
		spinnerModel = new SpinnerNumberModel(0,0,0,1);
		spinnerModel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent arg0) {
				loadSentence(spinnerModel.getNumber().intValue());
			}
		});
		spinner = new JSpinner(spinnerModel);
		middle.add(spinner, BorderLayout.CENTER);
		middle.setPreferredSize(new Dimension(150,20));
		p.add(middle);
		final Action resetSentenceAction = new AbstractAction(
				"Reset Sentence Graph") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				final int n = spinnerModel.getNumber().intValue();
				final File f2 = new File(sentence_modified.getAbsolutePath()
						+ n + ".grf");
				if (f2.exists())
					f2.delete();
				loadSentence(n);
			}
		};
		resetSentenceGraph = new JButton(resetSentenceAction);
		p.add(resetSentenceGraph);
		final Action rebuildAction = new AbstractAction("Rebuild FST-Text") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				InternalFrameManager.getManager(null).closeTextAutomatonFrame();
				InternalFrameManager.getManager(null).closeTfstTagsFrame();
				Config.cleanTfstFiles(false);
				final RebuildTfstCommand command = new RebuildTfstCommand()
						.automaton(new File(Config.getCurrentSntDir(),
								"text.tfst"));
				Launcher.exec(command, true,
						new RebuildTextAutomatonDo(Config.getCurrentSntDir()));
			}
		};
		final JButton rebuildTfstButton = new JButton(rebuildAction);
		p.add(rebuildTfstButton);
		final JButton deleteStates = new JButton("Remove greyed states");
		deleteStates.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final ArrayList<GenericGraphBox> boxes = new ArrayList<GenericGraphBox>();
				for (final GenericGraphBox gb : graphicalZone.graphBoxes) {
					if (graphicalZone.isBoxToBeRemoved((TfstGraphBox) gb)) {
						boxes.add(gb);
					}
				}
				graphicalZone.removeBoxes(boxes);
			}
		});
		p.add(deleteStates);
		return p;
	}

	/**
	 * Shows the frame
	 */
	boolean loadTfst() {
		text_tfst = new File(Config.getCurrentSntDir(), "text.tfst");
		if (!text_tfst.exists()) {
			return false;
		}
		sentence_text = new File(Config.getCurrentSntDir(), "cursentence.txt");
		sentence_grf = new File(Config.getCurrentSntDir(), "cursentence.grf");
		sentence_tok = new File(Config.getCurrentSntDir(), "cursentence.tok");
		sentence_modified = new File(Config.getCurrentSntDir(), "sentence");
		sentence_count = readSentenceCount(text_tfst);
		String s = " " + sentence_count;
		s = s + " sentence";
		if (sentence_count > 1)
			s = s + "s";
		sentence_count_label.setText(s);
		spinnerModel.setMaximum(sentence_count);
		spinnerModel.setMinimum(1);
		return true;
	}

	/**
	 * Indicates if the graph has been modified
	 * 
	 * @param b
	 *            <code>true</code> if the graph has been modified,
	 *            <code>false</code> otherwise
	 */
	void setModified(boolean b) {
		repaint();
		resetSentenceGraph.setVisible(b);
		final int n = spinnerModel.getNumber().intValue();
		if (b && !isAcurrentLoadingThread && n != 0) {
			/*
			 * We save each modification, but only if the sentence graph loading
			 * is terminated
			 */
			final GraphIO g = new GraphIO(graphicalZone);
			g.saveSentenceGraph(new File(sentence_modified.getAbsolutePath()
					+ n + ".grf"), graphicalZone.getGraphPresentationInfo());
		}
	}

	private int readSentenceCount(File f) {
		String s = "0";
		try {
			final InputStreamReader reader = Encoding.getInputStreamReader(f);
			if (reader == null) {
				return 0;
			}
			s = UnicodeIO.readLine(reader);
			if (s == null || s.equals("")) {
				return 0;
			}
			reader.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return Integer.parseInt(s);
	}

	public void loadSentenceFromConcordance(int n) {
		if (!isVisible() || isIcon()) {
			return;
		}
		if (n < 1 || n > sentence_count)
			return;
		if (loadSentence(n))
			spinnerModel.setValue(new Integer(n));
	}

	public boolean loadCurrSentence() {
		return loadSentence(spinnerModel.getNumber().intValue());
	}

	/**
	 * Loads a sentence automaton
	 * 
	 * @param n
	 *            sentence number
	 * @return <code>false</code> if a sentence is already being loaded,
	 *         <code>true</code> otherwise
	 */
	boolean loadSentence(int n) {
		if (n < 1 || n > sentence_count)
			return false;
		final int z = n;
		if (isAcurrentLoadingThread)
			return false;
		isAcurrentLoadingThread = true;
		graphicalZone.empty();
		Tfst2GrfCommand cmd = new Tfst2GrfCommand().automaton(text_tfst)
				.sentence(z);
		cmd = cmd.font(ConfigManager.getManager().getInputFont(null).getName())
				.fontSize(ConfigManager.getManager().getInputFontSize(null));
		Console.addCommand(cmd.getCommandLine(), false, Log.getCurrentLogID());
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd.getCommandArguments(true));
			final BufferedInputStream in = new BufferedInputStream(
					p.getInputStream());
			final BufferedInputStream err = new BufferedInputStream(
					p.getErrorStream());
			new EatStreamThread(in).start();
			new EatStreamThread(err).start();
			p.waitFor();
		} catch (final IOException e1) {
			e1.printStackTrace();
		} catch (final InterruptedException e1) {
			e1.printStackTrace();
		}
		final String text = readSentenceText();
		TokensInfo.loadTokensInfo(sentence_tok, text);
		final File f = new File(sentence_modified + String.valueOf(z) + ".grf");
		final boolean isSentenceModified = f.exists();
		if (isSentenceModified) {
			loadSentenceGraph(new File(sentence_modified.getAbsolutePath()
					+ String.valueOf(z) + ".grf"),n);
			setModified(isSentenceModified);
		} else {
			loadSentenceGraph(sentence_grf,n);
		}
		isAcurrentLoadingThread = false;
		return true;
	}

	String readSentenceText() {
		String s = "";
		try {
			final InputStreamReader br = Encoding
					.getInputStreamReader(sentence_text);
			if (br == null) {
				return "";
			}
			s = UnicodeIO.readLine(br);
			if (s == null || s.equals("")) {
				return "";
			}
			br.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return s;
	}

	public void changeAntialiasingValue() {
		final boolean a = graphicalZone.getAntialiasing();
		graphicalZone.setAntialiasing(!a);
	}

	boolean loadSentenceGraph(File file,int sentence) {
		setModified(false);
		final GraphIO g = GraphIO.loadGraph(file, true, true);
		if (g == null) {
			return false;
		}
		textfield.setFont(g.getInfo().getInput().getFont());
		graphicalZone.setup(g,sentence);
		final Timer t = new Timer(300, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (ConfigManager.getManager().isRightToLeftForGraphs(null)) {
					tfstScrollbar.setValue(tfstScrollbar.getMaximum());
				} else {
					tfstScrollbar.setValue(0);
				}
			}
		});
		t.setRepeats(false);
		t.start();
		return true;
	}

	
	int[] getHrefInfos(String s) {
		int[] res=new int[4];
		int start = s.indexOf("<a href=\"") + 9;
		int end = s.indexOf(' ', start);
		int selectionStart = Integer.valueOf((String) s
				.subSequence(start, end));
		start = end + 1;
		end = s.indexOf(' ', start);
		int selectionEnd = Integer.valueOf((String) s
				.subSequence(start, end));
		start = end + 1;
		end = s.indexOf(' ', start);
		int sentenceNumber = Integer.valueOf((String) s
				.subSequence(start, end));
		start = end + 1;
		end = s.indexOf('\"', start);
		int matchNumber = Integer.valueOf((String) s.subSequence(
				start, end));
		res[0]=selectionStart;
		res[1]=selectionEnd;
		res[2]=sentenceNumber;
		res[3]=matchNumber;
		return res;
	}
	

	class RebuildTextAutomatonDo implements ToDo {
		File sntDir;

		public RebuildTextAutomatonDo(File sntDir) {
			this.sntDir = sntDir;
		}

		@Override
		public void toDo(boolean success) {
			FileUtil.deleteFileByName(new File(sntDir, "sentence*.grf"));
			/* Todo: reload de la phrase courante */
		}
	}

	public int getSentenceCount() {
		return (Integer) spinnerModel.getMaximum();
	}
}

class LoadSentenceDo2 implements ToDo {
	private final LemmatizeFrame frame;

	LoadSentenceDo2(LemmatizeFrame f) {
		frame = f;
	}

	@Override
	public void toDo(boolean success) {
		frame.loadCurrSentence();
	}
}
