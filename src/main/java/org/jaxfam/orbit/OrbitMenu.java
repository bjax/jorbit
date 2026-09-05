//
// OrbitMenu.java
//
// Part of the orbital mechanics demonstrator program
//
// Written 2011-04-21 by Bruce Jackson, bruce@jaxfam.org
package org.jaxfam.orbit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 *
 * @author Bruce Jackson
 */
public class OrbitMenu extends JMenuBar implements ActionListener {

    String[] fileItems = new String[]{"New", "Open...", "Save", "Save As..."};
    String[] editItems = new String[]{"Undo", "Cut", "Copy", "Paste", "Clear", "Select All"};
    char[] fileShortcuts = {'N', 'O', 'S', 'X'};
    char[] editShortcuts = {'Z', 'X', 'C', 'V', 'L', 'A'};

    public OrbitMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");

        for (int i = 0; i < fileItems.length; i++) {
            JMenuItem item = new JMenuItem(fileItems[i], fileShortcuts[i]);
            item.addActionListener(this);
            fileMenu.add(item);
        }

        for (int i = 0; i < editItems.length; i++) {
            JMenuItem item = new JMenuItem(editItems[i], editShortcuts[i]);
            item.addActionListener(this);
            editMenu.add(item);
        }

        add(fileMenu);
        add(editMenu);
    }

    public void actionPerformed(ActionEvent ae) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
