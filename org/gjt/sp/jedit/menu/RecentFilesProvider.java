/*
 * RecentFilesProvider.java - Recent file list menu
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.menu;

import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.browser.FileCellRenderer;
import org.gjt.sp.util.Log;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RecentFilesProvider implements DynamicMenuProvider {

    private static final int MAX_ITEMS = jEdit.getIntegerProperty("menu.spillover", 20);

    @Override
    public boolean updateEveryTime() {
        return false;
    }

    @Override
    public void update(JMenu menu) {
        final View view = GUIUtilities.getView(menu);

        List<BufferHistory.Entry> recentVector = BufferHistory.getHistory();

        if (recentVector.isEmpty()) {
            JMenuItem menuItem = new JMenuItem(jEdit.getProperty("no-recent-files.label"));
            menuItem.setEnabled(false);
            menu.add(menuItem);
            return;
        }

        final List<JMenuItem> menuItems = new ArrayList<>();
        final JTextField text = new JTextField();
        text.setToolTipText(jEdit.getProperty("recent-files.textfield.tooltip") +
                ": " + jEdit.getProperty("glob.tooltip"));
        menu.add(text);
        text.addKeyListener(new RecentFilesKeyAdapter(text, menuItems));

        // Initialize the iterator here
        Iterator<BufferHistory.Entry> iter = recentVector.iterator();

        while (iter.hasNext()) {
            String path = iter.next().path;
            if (jEdit.getBooleanProperty("hideOpen") && jEdit.getBuffer(path) != null) {
                continue;
            }
            JMenuItem menuItem = createMenuItem(
                    MiscUtilities.getFileName(path),
                    path,
                    path,
                    createActionListener(view),
                    createChangeListener(view)
            );
            menuItems.add(menuItem);
            if (menu.getMenuComponentCount() >= MAX_ITEMS && iter.hasNext()) {
                JMenu newMenu = new JMenu(jEdit.getProperty("common.more"));
                menu.add(newMenu);
                menu = newMenu;
            }
            menu.add(menuItem);
        }

        JMenuItem clearMenuItem = new JMenuItem(jEdit.getProperty("clear-recent-files.label"));
        clearMenuItem.addActionListener(e -> BufferHistory.clear());
        menu.addSeparator();
        menu.add(clearMenuItem);
    }


    private JMenuItem createMenuItem(String text, String tooltip, String actionCommand, ActionListener actionListener, ChangeListener changeListener) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.setToolTipText(tooltip);
        menuItem.setActionCommand(actionCommand);
        menuItem.addActionListener(actionListener);
        menuItem.addChangeListener(changeListener);
        menuItem.setIcon(FileCellRenderer.fileIcon);
        return menuItem;
    }

    private ActionListener createActionListener(View view) {
        return evt -> {
            jEdit.openFile(view, evt.getActionCommand());
            view.getStatus().setMessage(null);
        };
    }

    private ChangeListener createChangeListener(View view) {
        return e -> {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            view.getStatus().setMessage(menuItem.isArmed() ? menuItem.getActionCommand() : null);
        };
    }

    private class RecentFilesKeyAdapter extends KeyAdapter {
        private final JTextField text;
        private final List<JMenuItem> menuItems;

        public RecentFilesKeyAdapter(JTextField text, List<JMenuItem> menuItems) {
            this.text = text;
            this.menuItems = menuItems;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            String typedText = text.getText();
            boolean filter = (typedText.length() > 0);
            Pattern pattern = null;
            if (filter) {
                String regex = ".*" + Pattern.quote(typedText) + ".*";
                pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            }
            try {
                for (JMenuItem recent : menuItems) {
                    recent.setEnabled(!filter || pattern.matcher(recent.getText()).find());
                }
            } catch (PatternSyntaxException ex) {
                Log.log(Log.ERROR, this, ex.getMessage());
            }
        }
    }
}


