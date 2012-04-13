/*
    This program is a plugin for LateralGM

    Copyright (c) 2012 Serge Humphrey<bobtheblueberry@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.btbb.hoiley;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.lateralgm.main.LGM;

public class HoileyDoileyPlugin implements ActionListener {

    public static ProjectExporter exporter;
    protected JMenuItem exportAll;
    protected JMenuItem cleanAll;
    protected JMenuItem runGame;
    protected JMenu runMenu;

    ScheduledExecutorService scheduler;

    public HoileyDoileyPlugin()
        {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            exporter = new ProjectExporter();

            runMenu = new JMenu("Runero");

            runGame = new JMenuItem("Run", getIcon("run"));
            addItem(runGame);

            exportAll = new JMenuItem("Export All", getIcon("build"));
            addItem(exportAll);

            cleanAll = new JMenuItem("Clean All", getIcon("clean"));
            addItem(cleanAll);

            LGM.frame.getJMenuBar().add(runMenu, 2);
        }

    public <K extends AbstractButton> K addItem(K b) {
        runMenu.add(b);
        b.addActionListener(this);
        return b;
    }

    public <K extends AbstractButton> K addButton(Container c, K b) {
        c.add(b);
        b.addActionListener(this);
        return b;
    }

    public static ImageIcon getIcon(String name) {
        String location = "com/btbb/hoiley/icons/" + name + ".png";
        URL url = HoileyDoileyPlugin.class.getClassLoader().getResource(location);
        if (url == null)
            return new ImageIcon(location);
        return new ImageIcon(url);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exportAll) {
            scheduler.schedule(new ExportCommand(), 0, TimeUnit.SECONDS);
        } else if (e.getSource() == cleanAll) {
            exporter.clean();
        } else if (e.getSource() == runGame) {
            new RunCommand().run();
            //scheduler.schedule(new RunCommand(), 0, TimeUnit.SECONDS);
        }
    }
}