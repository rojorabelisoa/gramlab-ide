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

import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import fr.umlv.unitex.BigTextArea;
import fr.umlv.unitex.Config;
import fr.umlv.unitex.FontListener;
import fr.umlv.unitex.MyDropTarget;
import fr.umlv.unitex.exceptions.*;
import fr.umlv.unitex.io.*;


/**
 * This class describes a frame used to display the current corpus.  
 * @author Sébastien Paumier
 *
 */
public class TextFrame extends JInternalFrame {

   protected BigTextArea text= new BigTextArea();
   protected JLabel ligne1= new JLabel("");
   protected JLabel ligne2= new JLabel("");

   TextFrame() {
      super("", true, true, true, true);
      MyDropTarget.newDropTarget(this);
      JPanel top= new JPanel(new BorderLayout());
      top.setOpaque(true);
      top.setBorder(new EmptyBorder(2, 2, 2, 2));
      JPanel middle= new JPanel(new BorderLayout());
      middle.setOpaque(true);
      middle.setBorder(BorderFactory.createLoweredBevelBorder());
      middle.add(text);
		final FontListener fontListener = new FontListener() {
			public void fontChanged(Font font) {
				text.setFont(font);
			}
		};
		UnitexFrame.getFrameManager().getGlobalPreferencesFrame().addTextFontListener(fontListener);
      JPanel up= new JPanel(new GridLayout(2, 1));
      up.setOpaque(true);
      up.setBorder(new EmptyBorder(2, 2, 2, 2));
      up.add(ligne1);
      up.add(ligne2);
      top.add(up, BorderLayout.NORTH);
      top.add(middle, BorderLayout.CENTER);
      setContentPane(top);
      pack();
      setBounds(100, 100, 800, 600);
      addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameClosed(InternalFrameEvent e) {
				UnitexFrame.getFrameManager().getGlobalPreferencesFrame().removeTextFontListener(fontListener);
				text.reset();
				System.gc();
			}
		});
   }


   private void loadStatistics() {
      ligne1.setText("");
      ligne2.setText("");
      FileInputStream source;
      String s;
      s= UnicodeIO.readFirstLine(new File(Config.getCurrentSntDir(),"stats.n"));
      if (s != null)
         ligne1.setText(" " + s);
      s= UnicodeIO.readFirstLine(new File(Config.getCurrentSntDir(),"dlf.n"));
      if (s == null)
         return;
      int dlf_entries= new Integer(s).intValue();
      s= UnicodeIO.readFirstLine(new File(Config.getCurrentSntDir(),"dlc.n"));
      if (s == null)
         return;
      int dlc_entries= new Integer(s).intValue();
      s= UnicodeIO.readFirstLine(new File(Config.getCurrentSntDir(),"err.n"));
      if (s == null)
         return;
      int err_entries= new Integer(s).intValue();
      File f= new File(Config.getCurrentSntDir(),"stat_dic.n");
      if (!f.exists()) {
         return;
      }
      if (!f.canRead()) {
         return;
      }
      int simple_total;
      int compound_total;
      int err_total;
      try {
         source= UnicodeIO.openUnicodeLittleEndianFileInputStream(f);
         simple_total= new Integer(UnicodeIO.readLine(source)).intValue();
         compound_total= new Integer(UnicodeIO.readLine(source)).intValue();
         err_total= new Integer(UnicodeIO.readLine(source)).intValue();
         source.close();
      } catch (NotAUnicodeLittleEndianFileException e) {
         return;
      } catch (FileNotFoundException e) {
         return;
      } catch (IOException e) {
         return;
      }
      s= " " + String.valueOf(simple_total) + " occurrence"+(simple_total>1?"s":"")+" (";
      s= s + String.valueOf(dlf_entries) + " DLF entries) simple word";
      if (dlf_entries > 1)
         s= s + "s";
      s= s + ", ";
      s= s + String.valueOf(compound_total) + " occurrence"+(compound_total>1?"s":"")+" (";
      s= s + String.valueOf(dlc_entries) + " DLC entries) compound word";
      if (dlc_entries > 1)
         s= s + "s";
      s= s + ", ";
      s= s + String.valueOf(err_total) + " occurrence"+(err_total>1?"s":"")+" (";
      s= s + String.valueOf(err_entries) + " ERR lines) unknown word";
      if (err_entries > 1)
         s= s + "s";
      ligne2.setText(s);
   }

   
   /**
    * Loads a corpus
    * @param sntFile name of the corpus file
    */
   void loadText(File sntFile) {
      loadStatistics();
      text.setFont(Config.getCurrentTextFont());
      text.setComponentOrientation(Config.isRightToLeftLanguage()?ComponentOrientation.RIGHT_TO_LEFT:ComponentOrientation.LEFT_TO_RIGHT);
      if (sntFile.length() <= 2) {
         text.setText(Config.EMPTY_FILE_MESSAGE);
      } else {
    	  text.load(sntFile);
      }
      setTitle(sntFile.getAbsolutePath());
   }


	public BigTextArea getText() {
		return text;
	}

}