/* ImageEnhancer.java  Version 1.2
 *   S. Tanimoto,  September 29, 2014.
 *   (The actionPerformed method has been made more straightforward.)
 *   
 * This program is
 * based on the "SaveImage.java" tutorial demo from Oracle.com.
 * Their attribution message is below.
 * A few changes have been made to their code here.
 * Primarily, there are some new "filters", and the operations
 * are activated by a popup menu rather than a JComboBox.
 * Other changes:
 *  (a) When an operation is applied, its result becomes the
 *  new current image, so that the effects of operations are combined.
 *  (b) When one operation is applied again, the second application
 *  is no longer ignored.
 *  (c) Lookup tables are computed when first needed, then saved.
 * 
 * 
 * 
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ByteLookupTable;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

public class ImageEnhancer extends Component implements ActionListener {

    String startingImage = "Aeroplane-view-of-UW.png";
    int opIndex;
    BufferedImage biTemp, biOriginal, biWorking, biFiltered;
    Graphics gOrig, gWorking, gFiltered;
    int w, h;
    byte[] lut0, lut3, lut4;
    LookupOp op0, op3, op4;

    static JPopupMenu popup;

    public static final float[] SHARPEN3x3 = { // sharpening filter kernel
        0.f, -1.f,  0.f,
       -1.f,  5.f, -1.f,
        0.f, -1.f,  0.f
    };

    public static final float[] BLUR3x3 = {
        0.1f, 0.1f, 0.1f,    // low-pass filter kernel
        0.1f, 0.2f, 0.1f,
        0.1f, 0.1f, 0.1f
    };

    public ImageEnhancer() {
        try {
            biTemp = ImageIO.read(new File(startingImage));
            w = biTemp.getWidth(null);
            h = biTemp.getHeight(null);
            biOriginal = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gOrig = biOriginal.getGraphics();
            gOrig.drawImage(biTemp, 0, 0, null);
            biWorking = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gWorking = biWorking.getGraphics();
            gWorking.drawImage(biOriginal, 0, 0, null);
            biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            gFiltered = biFiltered.getGraphics();
            
        } catch (IOException e) {
            System.out.println("Image could not be read: "+startingImage);
            System.exit(1);
        }
        //Add listener to this component so that it can bring up popup menus.
        MouseListener popupListener = new PopupListener();
        addMouseListener(popupListener);

    }

    public Dimension getPreferredSize() {
        return new Dimension(w, h);
    }

    void setOpIndex(int i) {
        opIndex = i;
    }

    public void paint(Graphics g) {
        g.drawImage(biWorking, 0, 0, null);
    }

    int lastOp;
    public void filterImage() {
        BufferedImageOp op = null;
        lastOp = opIndex;
        switch (opIndex) {
        case 0 : /* darken. */
        	if (lut0==null) {
                lut0 = new byte[256];
                for (int j=0; j<256; j++) {
                    lut0[j] = (byte)(j*9.0 / 10.0); 
                }
                ByteLookupTable blut0 = new ByteLookupTable(0, lut0); 
                op0 = new LookupOp(blut0, null);
        	}
        	op = op0;
            break;
        case 1:  /* low pass filter */
        case 2:  /* sharpen */
            float[] data = (opIndex == 1) ? BLUR3x3 : SHARPEN3x3;
            op = new ConvolveOp(new Kernel(3, 3, data),
                                ConvolveOp.EDGE_NO_OP,
                                null);
  
            break;

        case 3 : /* photonegative */
        	if (lut3==null) {
                lut3 = new byte[256];
                for (int j=0; j<256; j++) {
                    lut3[j] = (byte)(256-j); 
                }
                ByteLookupTable blut3 = new ByteLookupTable(0, lut3); 
                op3 = new LookupOp(blut3, null);
            }
        	op = op3;
            break;
 
        case 4 : /* threshold RGB values. */
        	if (lut4==null) {
                lut4 = new byte[256];
                for (int j=0; j<256; j++) {
                    lut4[j] = (byte)(j < 128 ? 0: 200); 
                }
                ByteLookupTable blut4 = new ByteLookupTable(0, lut4); 
                op4 = new LookupOp(blut4, null);
        	}
        	op = op4;
            break;
        }

        /* Rather than directly drawing the filtered image to the
         * destination, filter it into a new image first, then that
         * filtered image is ready for writing out or painting. 
         */
        biFiltered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        op.filter(biWorking, biFiltered);
        gWorking.drawImage(biFiltered,  0, 0, null);
        
    }

    /* Return the formats sorted alphabetically and in lower case */
    public String[] getFormats() {
        String[] formats = ImageIO.getWriterFormatNames();
        TreeSet<String> formatSet = new TreeSet<String>();
        for (String s : formats) {
            formatSet.add(s.toLowerCase());
        }
        return formatSet.toArray(new String[0]);
    }

    class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }
    }
    public void actionPerformed(ActionEvent e) {
    	try {
    		Object obj = e.getSource();
    		if (obj instanceof JComboBox) {
    			JComboBox cb = (JComboBox)obj;
			if (cb.getActionCommand().equals("Formats")) {
			    /* Save the filtered image in the selected format.
			     * The selected item will be the name of the format to use
			     */
			    String format = (String)cb.getSelectedItem();
			    /* Use the format name to initialize the file suffix.
			     * Format names typically correspond to suffixes
			     */
			    File saveFile = new File("savedimage."+format);
			    JFileChooser chooser = new JFileChooser();
			    chooser.setSelectedFile(saveFile);
			    int rval = chooser.showSaveDialog(cb);
			    if (rval == JFileChooser.APPROVE_OPTION) {
                		saveFile = chooser.getSelectedFile();
                		/* Write the filtered image in the selected format,
                		 * to the file chosen by the user.
                		 */
                		try {
                			ImageIO.write(biFiltered, format, saveFile);
                		} catch (IOException ex) {}
			    }
			}
    		}
    		else if (obj instanceof JMenuItem) {
    			JMenuItem mi = (JMenuItem)obj;
    			String menuCommand = mi.getText();
    			char firstChar = menuCommand.charAt(0);
    			if (firstChar >= '0' && firstChar <= '4') {
    				int index = Character.getNumericValue(firstChar);
    				System.out.println(menuCommand);
    				setOpIndex(index);
    				filterImage(); 
    				repaint();
    			}
    			else {
    				System.out.println("Some non-filter menu command was selected.");
    			}
    		}
    	}
    	catch (Exception ee) {
    		System.out.println("Unknown action request.");
    	}
    }

    public static void main(String s[]) {
        JFrame f = new JFrame("ImageEnhancer without Undo or Redo");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
        ImageEnhancer si = new ImageEnhancer();
        f.add("Center", si);
        JComboBox formats = new JComboBox(si.getFormats());
        formats.setActionCommand("Formats");
        formats.addActionListener(si);
        JPanel panel = new JPanel();
        panel.add(new JLabel("Save As"));
        panel.add(formats);
        f.add("South", panel);
        f.pack();
        f.setVisible(true);
        
        //Create the popup menu.
        popup = new JPopupMenu();
        JMenuItem menuItem = new JMenuItem("0: Darken by 10%");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("1: Convolve: Low-Pass");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("2: Convolve: High-Pass");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("3: Photonegative");
        menuItem.addActionListener(si);
        popup.add(menuItem);
        menuItem = new JMenuItem("4: RGB Thresholds at 128");
        menuItem.addActionListener(si);
        popup.add(menuItem);
  
    }
}
