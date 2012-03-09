/*
 The MIT License

 Copyright (c) 2012 Serge Humphrey<bobtheblueberry@gmail.com>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package com.btbb.hoiley;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.lateralgm.main.LGM;

public class HoileyDoileyPlugin implements ActionListener {

    public static ProjectExporter exporter;
    protected JMenuItem exportAll;
    protected JMenuItem cleanAll;
    ScheduledExecutorService scheduler;
    
    public HoileyDoileyPlugin() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        exporter = new ProjectExporter();
        
        JMenu runMenu = new JMenu("Run Game");
        
        exportAll = new JMenuItem("Export All");
        exportAll.addActionListener(this);
        runMenu.add(exportAll);
        
        cleanAll = new JMenuItem("Clean All");
        cleanAll.addActionListener(this);
        runMenu.add(cleanAll);
        
        LGM.frame.getJMenuBar().add(runMenu, 2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == exportAll) {
            scheduler.schedule(new ExportCommand(), 0, TimeUnit.SECONDS);
        } else if (e.getSource() == cleanAll) {
            exporter.clean();
        }
    }

}