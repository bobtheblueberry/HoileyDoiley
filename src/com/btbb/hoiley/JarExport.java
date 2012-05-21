package com.btbb.hoiley;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.lateralgm.components.ErrorDialog;
import org.lateralgm.main.LGM;

public class JarExport implements Runnable {

    @Override
    public void run() {
        File f = getFile();
        if (f == null)
            return;
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "org.gcreator.runero.JarMain");
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, "LWJGL/lwjgl.jar LWJGL/lwjgl_util.jar LWJGL/slick-util.jar");
        

        JarOutputStream out = copyRunero(f, manifest);
        if (out == null)
            return;
        try {
            new ProjectExporter(true, out).export();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JarOutputStream copyRunero(File f, Manifest m) {
        File runero = new File("plugins/shared/Runero.jar");
        if (!runero.exists()) {
            JOptionPane.showMessageDialog(LGM.frame, "Missing Runero.jar");
            return null;
        }
        try {
            JarInputStream in = new JarInputStream(new FileInputStream(runero));
            JarOutputStream out = new JarOutputStream(new FileOutputStream(f), m);
            ZipEntry en;
            while ((en = in.getNextEntry()) != null) {
                out.putNextEntry(en);
                int i;
                while ((i = in.read()) != -1)
                    out.write(i);
                in.closeEntry();
            }
            in.close();
            return out;
        } catch (IOException exc) {
            new ErrorDialog(LGM.frame, "Error copying Runero.jar", exc.getMessage(), exc);
        }
        return null;
    }

    private File getFile() {
        JFileChooser fc = new JFileChooser();
        fc.showSaveDialog(LGM.frame);
        File f = fc.getSelectedFile();
        if (f == null)
            return null;
        if (!f.getName().toLowerCase().endsWith(".jar"))
            f = new File(f.getParentFile(), f.getName() + ".jar");
        if (f.exists()) {
            int o = JOptionPane.showConfirmDialog(LGM.frame, "File " + f.getName() + " exists. Replace?");
            if (o != JOptionPane.OK_OPTION)
                return null;
        }
        return f;
    }
}
