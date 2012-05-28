package com.btbb.hoiley;


import org.gcreator.runero.Runner;

public class RunCommand implements Runnable {

    public void runGame() {
        DebugWindow w = DebugWindow.getWindow();
        w.log("Starting Game. Please start LateralGM in terminal to see *potential* errors.");
        try {
            new Runner(ProjectExporter.projectDir);
        } catch (NoClassDefFoundError e) {
            w.log("Error! Cannot find Runero Runner");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        DebugWindow w = DebugWindow.getWindow();
        w.reset();
        try {
            runGame();
        } catch (Exception exc) {
            w.log(exc);
        }
    }
}
