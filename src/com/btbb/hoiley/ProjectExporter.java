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

import static org.lateralgm.main.Util.deRef;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import org.lateralgm.components.impl.ResNode;
import org.lateralgm.file.GmFile;
import org.lateralgm.file.GmStreamEncoder;
import org.lateralgm.file.ResourceList;
import org.lateralgm.file.StreamEncoder;
import org.lateralgm.main.LGM;
import org.lateralgm.resources.Background;
import org.lateralgm.resources.Background.PBackground;
import org.lateralgm.resources.Font;
import org.lateralgm.resources.Font.PFont;
import org.lateralgm.resources.GameInformation;
import org.lateralgm.resources.GameInformation.PGameInformation;
import org.lateralgm.resources.GameSettings;
import org.lateralgm.resources.GameSettings.PGameSettings;
import org.lateralgm.resources.GmObject;
import org.lateralgm.resources.GmObject.PGmObject;
import org.lateralgm.resources.InstantiableResource;
import org.lateralgm.resources.Path;
import org.lateralgm.resources.Path.PPath;
import org.lateralgm.resources.Resource;
import org.lateralgm.resources.ResourceReference;
import org.lateralgm.resources.Room;
import org.lateralgm.resources.Room.PRoom;
import org.lateralgm.resources.Script;
import org.lateralgm.resources.Sound;
import org.lateralgm.resources.Sound.PSound;
import org.lateralgm.resources.Sound.SoundKind;
import org.lateralgm.resources.Sprite;
import org.lateralgm.resources.Sprite.MaskShape;
import org.lateralgm.resources.Sprite.PSprite;
import org.lateralgm.resources.Timeline;
import org.lateralgm.resources.library.LibAction;
import org.lateralgm.resources.sub.Action;
import org.lateralgm.resources.sub.Argument;
import org.lateralgm.resources.sub.BackgroundDef;
import org.lateralgm.resources.sub.BackgroundDef.PBackgroundDef;
import org.lateralgm.resources.sub.Event;
import org.lateralgm.resources.sub.Instance;
import org.lateralgm.resources.sub.Instance.PInstance;
import org.lateralgm.resources.sub.MainEvent;
import org.lateralgm.resources.sub.Moment;
import org.lateralgm.resources.sub.PathPoint;
import org.lateralgm.resources.sub.Tile;
import org.lateralgm.resources.sub.Tile.PTile;
import org.lateralgm.resources.sub.View;
import org.lateralgm.resources.sub.View.PView;

public class ProjectExporter {

    private File actionDir;
    public File projectDir;
    private StreamEncoder resData;
    private static final byte SPRITE = 1;
    private static final byte BACKGROUND = 2;
    private static final byte SOUND = 3;
    private static final byte PATH = 4;
    private static final byte SCRIPT = 5;
    private static final byte FONT = 6;
    private static final byte TIMELINE = 7;
    private static final byte OBJECT = 8;
    private static final byte ROOM = 9;
    private static final byte GAME_INFO = 10;
    private static final byte SETTINGS = 11;

    public ProjectExporter()
        {
            projectDir = new File("RuneroGame/");
        }

    public void clean() {
        deleteDir(projectDir);
    }

    private void deleteDir(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory())
                deleteDir(f);
            f.delete();
        }
    }

    public void export() {
        projectDir.mkdir();
        // Export stuff

        actionDir = new File(projectDir, "actions");
        actionDir.mkdir();

        initResFile();
        // Write Sprites
        exportSprites(projectDir);
        // Write Backgrounds
        exportBackgrounds(projectDir);
        // Write Sounds
        exportSounds(projectDir);
        // Write Paths
        exportPaths(projectDir);
        // Write Scripts
        exportScripts(projectDir);
        // Write Fonts
        exportFonts(projectDir);
        // Write Timelines
        exportTimelines(projectDir);
        // Write Objects
        exportGmObjects(projectDir);
        // Write Rooms
        exportRooms(projectDir);
        // Write Game Information
        exportGameInfo(projectDir);
        // Write Global settings
        exportSettings(projectDir);
        closeRes();
    }

    private void initResFile() {
        File res = new File(projectDir, "resources.dat");
        try {
            resData = new StreamEncoder(res);
            resData.write4(LGM.currentFile.resMap.getList(Sprite.class).size());
            resData.write4(LGM.currentFile.resMap.getList(Background.class).size());
            resData.write4(LGM.currentFile.resMap.getList(Sound.class).size());
            resData.write4(LGM.currentFile.resMap.getList(Path.class).size());
            resData.write4(LGM.currentFile.resMap.getList(Script.class).size());
            resData.write4(LGM.currentFile.resMap.getList(Font.class).size());
            resData.write4(LGM.currentFile.resMap.getList(Timeline.class).size());
            resData.write4(LGM.currentFile.resMap.getList(GmObject.class).size());
            resData.write4(LGM.currentFile.resMap.getList(Room.class).size());
        } catch (IOException exc) {
            System.err.println("Cannot open resources file");
        }
    }

    private void addRes(String name, byte type) throws IOException {
        resData.write(type);
        writeStr(name, resData);
    }

    private void closeRes() {
        try {
            resData.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportSprites(File parentdir) {
        File sprDir = new File(parentdir, "sprites");
        sprDir.mkdir();
        ResourceList<Sprite> sprites = LGM.currentFile.resMap.getList(Sprite.class);
        Iterator<Sprite> sprI = sprites.iterator();

        while (sprI.hasNext()) {
            ArrayList<String> subimgs = new ArrayList<String>();
            Sprite s = sprI.next();
            if (s.subImages.isEmpty()) {
                System.out.println(s + " has no subimgs");
            } else {
                int count = 0;
                boolean trans = s.get(PSprite.TRANSPARENT);
                for (int i = 0; i < s.subImages.size(); i++) {
                    File imf = new File(sprDir, s.getName() + "_sub" + count++ + ".img");

                    BufferedImage image = s.subImages.get(i);
                    writeImage(image, imf, trans);

                    subimgs.add(imf.getName());
                }
            }
            // TRANSPARENT,SHAPE,ALPHA_TOLERANCE,SEPARATE_MASK,SMOOTH_EDGES,PRELOAD,ORIGIN_X,ORIGIN_Y,
            // BB_MODE*,BB_LEFT,BB_RIGHT,BB_TOP,BB_BOTTOM
            // * ignored
            boolean transparent = s.properties.get(PSprite.TRANSPARENT);
            MaskShape shape = s.properties.get(PSprite.SHAPE);
            int alpha = s.properties.get(PSprite.ALPHA_TOLERANCE);
            boolean mask = s.properties.get(PSprite.SEPARATE_MASK);
            boolean smooth = s.properties.get(PSprite.SMOOTH_EDGES);
            boolean preload = s.properties.get(PSprite.PRELOAD);
            int x = s.properties.get(PSprite.ORIGIN_X);
            int y = s.properties.get(PSprite.ORIGIN_Y);
            int left = s.properties.get(PSprite.BB_LEFT);
            int right = s.properties.get(PSprite.BB_RIGHT);
            int top = s.properties.get(PSprite.BB_TOP);
            int bottom = s.properties.get(PSprite.BB_BOTTOM);

            File f = new File(sprDir, s.getName() + ".spr");

            try {
                addRes(f.getName(), SPRITE);
                StreamEncoder out = new StreamEncoder(f);
                writeStr(s.getName(), out);
                out.write4(s.getId());
                if (s.subImages != null) {
                    out.write4(s.subImages.getWidth());
                    out.write4(s.subImages.getHeight());
                } else {
                    out.write4(0);
                    out.write4(0);
                }
                // subimages
                out.write4(subimgs.size());
                for (String ss : subimgs)
                    writeStr(ss, out);

                writeBool(transparent, out);
                // PRECISE,RECTANGLE,DISK,DIAMOND,POLYGON
                int box;
                if (shape == MaskShape.RECTANGLE)
                    box = 0;
                else if (shape == MaskShape.PRECISE)
                    box = 1;
                else if (shape == MaskShape.DISK)
                    box = 2;
                else if (shape == MaskShape.DIAMOND)
                    box = 3;
                else if (shape == MaskShape.POLYGON)
                    box = 4;
                else
                    box = -1;
                out.write(box);
                out.write4(alpha);
                writeBool(mask, out);
                writeBool(smooth, out);
                writeBool(preload, out);
                out.write4(x);
                out.write4(y);
                out.write4(left);
                out.write4(right);
                out.write4(top);
                out.write4(bottom);
                // write mask
                writeSpriteMask(s, shape, out);
                out.close();
                System.out.println("Wrote " + f);

            } catch (IOException e) {
                System.err.println("Error, can't write sprite " + f);
            }
        }
    }

    private void writeSpriteMask(Sprite s, MaskShape shape, StreamEncoder out) throws IOException {
        if (shape != MaskShape.PRECISE) {
            if (shape != MaskShape.RECTANGLE)
                System.out.println("Cannot create mask shape of type " + shape);
            out.write4(0);
            return;
        }
        if (s.subImages == null) {
            out.write4(0);
            return;
        }
        // precise

        // determine length of data for each subimage
        int len = s.subImages.getWidth() * s.subImages.getHeight();
        int wlen = (int) Math.ceil(len / 8.0);
        out.write4(wlen);

        int threshold = 0x7F; // TODO: Add threshold thing to LGM - use alpha var
        for (BufferedImage img : s.subImages) {
            boolean hasAlpha = img.getColorModel().hasAlpha();
            int pixels[] = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
            int trans = img.getRGB(0, img.getHeight() - 1) & 0x00FFFFFF;

            for (int p = 0; p < pixels.length; p += 8) {
                char b = 0;
                for (int j = 0; j < 8; j++) {
                    if (p + j >= pixels.length) {
                        b <<= (7 - j);
                        break;
                    }
                    int pixel = pixels[p + j];
                    if (hasAlpha) {
                        if ((pixel >>> 24) > threshold)
                            b++;// solid
                    } else {
                        if ((pixel & 0x00FFFFFF) != trans)
                            b++;// solid
                    }

                    if (j < 7)
                        b <<= 1;
                }
                out.write(b);
            }
        }
    }

    private void exportSounds(File parentdir) {

        File sndDir = new File(parentdir, "sounds");
        sndDir.mkdir();
        ResourceList<Sound> sounds = LGM.currentFile.resMap.getList(Sound.class);
        Iterator<Sound> sndI = sounds.iterator();
        while (sndI.hasNext()) {
            Sound s = sndI.next();
            String name = s.getName() + s.properties.get(PSound.FILE_TYPE);
            File sf = new File(sndDir, name);
            try {
                BufferedOutputStream w = new BufferedOutputStream(new FileOutputStream(sf));
                w.write(s.data);
                w.close();
            } catch (IOException exc) {
                System.err.println("Error writing sound " + sf.getName());
                exc.printStackTrace();
            }
            // KIND,FILE_TYPE,FILE_NAME,CHORUS,ECHO,FLANGER,GARGLE,REVERB,VOLUME,PAN,PRELOAD
            SoundKind kind = s.properties.get(PSound.KIND);
            String file_type = s.properties.get(PSound.FILE_TYPE);
            String file_name = s.properties.get(PSound.FILE_NAME);
            boolean chorus = s.properties.get(PSound.CHORUS);
            boolean echo = s.properties.get(PSound.ECHO);
            boolean flanger = s.properties.get(PSound.FLANGER);
            boolean gargle = s.properties.get(PSound.GARGLE);
            boolean reverb = s.properties.get(PSound.REVERB);
            double volume = s.properties.get(PSound.VOLUME);
            double pan = s.properties.get(PSound.PAN);
            boolean preload = s.properties.get(PSound.PRELOAD);

            File data = new File(sndDir, s.getName() + ".snd");
            try {
                addRes(data.getName(), SOUND);
                StreamEncoder out = new StreamEncoder(data);
                writeStr(s.getName(), out);
                out.write4(s.getId());
                if (kind == SoundKind.NORMAL)
                    out.write(0);
                else if (kind == SoundKind.MULTIMEDIA)
                    out.write(1);
                else if (kind == SoundKind.BACKGROUND)
                    out.write(2);
                else if (kind == SoundKind.SPATIAL)
                    out.write(3);
                writeStr(file_type, out);
                writeStr(file_name, out);
                writeBool(chorus, out);
                writeBool(echo, out);
                writeBool(flanger, out);
                writeBool(gargle, out);
                writeBool(reverb, out);
                out.writeD(volume);
                out.writeD(pan);
                writeBool(preload, out);
                writeStr(sf.getName(), out);
                out.close();
            } catch (IOException e) {
                System.err.println("Error writing sound data " + data);
            }

        }
    }

    private void exportBackgrounds(File parentdir) {
        File bgDir = new File(parentdir, "backgrounds");
        bgDir.mkdir();
        ResourceList<Background> backgrounds = LGM.currentFile.resMap.getList(Background.class);
        Iterator<Background> bgI = backgrounds.iterator();
        while (bgI.hasNext()) {
            Background b = bgI.next();
            File bf = new File(bgDir, b.getName() + ".img");
            writeImage(b.getBackgroundImage(), bf, false);
            // TRANSPARENT,SMOOTH_EDGES,PRELOAD,USE_AS_TILESET,TILE_WIDTH,
            // TILE_HEIGHT,H_OFFSET,V_OFFSET,H_SEP, V_SEP
            boolean transparent = b.properties.get(PBackground.TRANSPARENT);
            boolean smooth = b.properties.get(PBackground.SMOOTH_EDGES);
            boolean preload = b.properties.get(PBackground.PRELOAD);
            boolean tileset = b.properties.get(PBackground.USE_AS_TILESET);
            int tile_width = b.properties.get(PBackground.TILE_WIDTH);
            int tile_height = b.properties.get(PBackground.TILE_HEIGHT);
            int h_offset = b.properties.get(PBackground.H_OFFSET);
            int v_offset = b.properties.get(PBackground.V_OFFSET);
            int h_sep = b.properties.get(PBackground.H_SEP);
            int v_sep = b.properties.get(PBackground.V_SEP);

            File f = new File(bgDir, b.getName() + ".bg");
            try {
                addRes(f.getName(), BACKGROUND);
                StreamEncoder out = new StreamEncoder(f);
                writeStr(b.getName(), out);
                out.write4(b.getId());
                writeBool(transparent, out);
                writeBool(smooth, out);
                writeBool(preload, out);
                writeBool(tileset, out);
                out.write4(tile_width);
                out.write4(tile_height);
                out.write4(h_offset);
                out.write4(v_offset);
                out.write4(h_sep);
                out.write4(v_sep);
                writeStr(bf.getName(), out);

                out.close();
                System.out.println("Wrote background " + f);
            } catch (IOException exc) {
                System.err.println("Error writing background data for file " + f);
            }
        }
    }

    private void exportPaths(File parentdir) {
        File pathDir = new File(parentdir, "paths");
        pathDir.mkdir();
        ResourceList<Path> paths = LGM.currentFile.resMap.getList(Path.class);
        Iterator<Path> pathI = paths.iterator();
        while (pathI.hasNext()) {
            Path p = pathI.next();
            File f = new File(pathDir, p.getName() + ".path");
            // SMOOTH,CLOSED,PRECISION,BACKGROUND_ROOM,SNAP_X,SNAP_Y
            boolean smooth = p.properties.get(PPath.SMOOTH);
            boolean closed = p.properties.get(PPath.CLOSED);
            int precision = p.properties.get(PPath.PRECISION);

            int points = p.points.size();

            try {
                addRes(f.getName(), PATH);
                StreamEncoder out = new StreamEncoder(f);
                writeStr(p.getName(), out);
                out.write4(p.getId());
                writeBool(smooth, out);
                writeBool(closed, out);
                out.write4(precision);
                out.write4(points);

                for (PathPoint pp : p.points) {
                    out.write4(pp.getX());
                    out.write4(pp.getY());
                    out.write4(pp.getSpeed());
                }
                out.close();
                System.out.println("Exported path " + f);

            } catch (IOException e) {
                System.err.println("Cannot export path " + f);
            }

        }
    }

    private void exportScripts(File parentdir) {
        File scrDir = new File(parentdir, "scripts");
        scrDir.mkdir();
        ResourceList<Script> scrs = LGM.currentFile.resMap.getList(Script.class);
        Iterator<Script> scrI = scrs.iterator();
        while (scrI.hasNext()) {
            Script s = scrI.next();
            File f = new File(scrDir, s.getName() + ".scr");
            try {
                addRes(f.getName(), SCRIPT);
                StreamEncoder out = new StreamEncoder(f);
                writeStr(s.getName(), out);
                out.write4(s.getId());
                writeStr(s.getCode(), out);
                out.close();
                System.out.println("Wrote Script " + f);
            } catch (IOException e) {
                System.err.println("Couldn't write script " + f);
                e.printStackTrace();
            }

        }
    }

    private void exportFonts(File parentdir) {
        File fntDir = new File(parentdir, "fonts");
        fntDir.mkdir();
        ResourceList<Font> fonts = LGM.currentFile.resMap.getList(Font.class);
        Iterator<Font> fntI = fonts.iterator();
        while (fntI.hasNext()) {
            Font font = fntI.next();
            File ff = new File(fntDir, font.getName() + ".fnt");
            try {
                addRes(ff.getName(), FONT);
                // resource name,
                // FONT_NAME,SIZE,BOLD,ITALIC,ANTIALIAS,CHARSET,RANGE_MIN,RANGE_MAX
                String font_name = font.properties.get(PFont.FONT_NAME);
                int size = font.properties.get(PFont.SIZE);
                boolean bold = font.properties.get(PFont.BOLD);
                boolean italic = font.properties.get(PFont.ITALIC);
                int antialias = font.properties.get(PFont.ANTIALIAS);
                int charset = font.properties.get(PFont.CHARSET);
                int range_min = font.properties.get(PFont.RANGE_MIN);
                int range_max = font.properties.get(PFont.RANGE_MAX);

                StreamEncoder out = new StreamEncoder(ff);
                writeStr(font.getName(), out);
                out.write4(font.getId());
                writeStr(font_name, out);

                out.write4(size);
                writeBool(bold, out);
                writeBool(italic, out);
                out.write4(antialias);
                out.write4(charset);
                out.write4(range_min);
                out.write4(range_max);

                out.close();

                System.out.println("Wrote font data " + ff);
            } catch (IOException exc) {
                System.err.println("Couldn't open font file for writing: " + ff);
            }
        }

    }

    private void exportTimelines(File parentdir) {
        File tlDir = new File(parentdir, "timelines");
        tlDir.mkdir();
        ResourceList<Timeline> tls = LGM.currentFile.resMap.getList(Timeline.class);
        Iterator<Timeline> tlI = tls.iterator();
        while (tlI.hasNext()) {
            Timeline t = tlI.next();
            File tlf = new File(tlDir, t.getName() + ".tl");
            try {
                addRes(tlf.getName(), TIMELINE);
                StreamEncoder out = new StreamEncoder(tlf);
                writeStr(t.getName(), out);
                out.write4(t.getId());
                out.write4(t.moments.size());
                for (Moment m : t.moments) {
                    out.write4(m.stepNo);
                    for (Action a : m.actions)
                        writeAction(a, out);
                }
                out.close();
                System.out.println("Wrote timeline " + tlf);
            } catch (IOException e) {
                System.err.println("Can not write timeline data " + tlf);
            }

        }
    }

    private void exportGmObjects(File parentdir) {
        File objDir = new File(parentdir, "objects");
        objDir.mkdir();
        ResourceList<GmObject> objs = LGM.currentFile.resMap.getList(GmObject.class);
        Iterator<GmObject> objI = objs.iterator();
        while (objI.hasNext()) {
            GmObject obj = objI.next();
            File dat = new File(objDir, obj.getName() + ".obj");

            try {
                addRes(dat.getName(), OBJECT);
                // SPRITE,SOLID,VISIBLE,DEPTH,PERSISTENT,PARENT,MASK
                int sprite = getId((ResourceReference<?>) obj.get(PGmObject.SPRITE));
                boolean solid = obj.properties.get(PGmObject.SOLID);
                boolean visible = obj.properties.get(PGmObject.VISIBLE);
                int depth = obj.properties.get(PGmObject.DEPTH);
                boolean persistent = obj.properties.get(PGmObject.PERSISTENT);
                StreamEncoder out = new StreamEncoder(dat);
                writeStr(obj.getName(), out);
                out.write4(obj.getId());
                out.write4(sprite);
                writeBool(solid, out);
                writeBool(visible, out);
                writeBool(persistent, out);
                out.write4(depth);
                out.write4(getId((ResourceReference<?>) obj.get(PGmObject.PARENT), -100));
                out.write4(getId((ResourceReference<?>) obj.get(PGmObject.MASK)));

                int numMainEvents = 11;// ver == 800 ? 12 : 11;
                // Does not support custom trigger events
                out.write4(numMainEvents);
                for (int j = 0; j < numMainEvents; j++) {
                    MainEvent me = obj.mainEvents.get(j);
                    out.write4(me.events.size());
                    for (Event ev : me.events) {
                        out.write(ev.mainId);
                        if (j == MainEvent.EV_COLLISION)
                            out.write4(getId(ev.other));
                        else
                            out.write4(ev.id);
                        out.write4(ev.actions.size());
                        for (int i = 0; i < ev.actions.size(); i++) {
                            Action a = ev.actions.get(i);
                            writeAction(a, out);
                        }
                    }
                }

                out.close();
                System.out.println("Wrote object " + dat);
            } catch (IOException e) {
                System.err.println("Can not write object data " + dat);
                e.printStackTrace();
            }

        }
    }

    private void exportRooms(File parentdir) {
        File rmDir = new File(parentdir, "rooms");
        rmDir.mkdir();

        ArrayList<String> names = new ArrayList<String>();

        Enumeration<?> e = LGM.root.preorderEnumeration();
        while (e.hasMoreElements()) {
            ResNode node = (ResNode) e.nextElement();
            if (node.kind == org.lateralgm.resources.Room.class) {
                org.lateralgm.resources.Room r = (org.lateralgm.resources.Room) deRef((ResourceReference<?>) node
                        .getRes());
                if (r == null) {
                    // Probably the root Room folder
                    continue;
                }
                names.add(r.getName());
            }
        }
        for (String s : names) {
            Room r = LGM.currentFile.resMap.getList(Room.class).get(s);
            File dat = new File(rmDir, r.getName() + ".room");
            try {
                addRes(dat.getName(), ROOM);
                StreamEncoder out = new StreamEncoder(dat);
                // CAPTION,WIDTH,HEIGHT,SNAP_X,SNAP_Y,ISOMETRIC,SPEED,PERSISTENT,BACKGROUND_COLOR,
                // DRAW_BACKGROUND_COLOR,CREATION_CODE,REMEMBER_WINDOW_SIZE,EDITOR_WIDTH,EDITOR_HEIGHT,SHOW_GRID,
                // SHOW_OBJECTS,SHOW_TILES,SHOW_BACKGROUNDS,SHOW_FOREGROUNDS,SHOW_VIEWS,DELETE_UNDERLYING_OBJECTS,
                // DELETE_UNDERLYING_TILES,CURRENT_TAB,SCROLL_BAR_X,SCROLL_BAR_Y,ENABLE_VIEWS

                writeStr(r.getName(), out);
                out.write4(r.getId());
                String caption = r.properties.get(PRoom.CAPTION);
                int width = r.properties.get(PRoom.WIDTH);
                int height = r.properties.get(PRoom.HEIGHT);
                int speed = r.properties.get(PRoom.SPEED);
                boolean persistent = r.properties.get(PRoom.PERSISTENT);
                Color c = r.properties.get(PRoom.BACKGROUND_COLOR);
                boolean draw_background_color = r.properties.get(PRoom.DRAW_BACKGROUND_COLOR);
                String code = r.properties.get(PRoom.CREATION_CODE);

                writeStr(caption, out);
                out.write4(width);
                out.write4(height);
                // USELESS: SNAP_X, SNAP_Y, ISOMETRIC
                out.write4(speed);
                writeBool(persistent, out);
                out.write4(c.getRGB());
                writeBool(draw_background_color, out);
                writeStr(code, out);
                out.write4(r.backgroundDefs.size());
                for (BackgroundDef b : r.backgroundDefs) {
                    // VISIBLE,FOREGROUND,BACKGROUND,X,Y,TILE_HORIZ,TILE_VERT,H_SPEED,V_SPEED,STRETCH
                    boolean visible = b.properties.get(PBackgroundDef.VISIBLE);
                    boolean foreground = b.properties.get(PBackgroundDef.FOREGROUND);
                    // bg id
                    int x = b.properties.get(PBackgroundDef.X);
                    int y = b.properties.get(PBackgroundDef.Y);
                    boolean tile_horiz = b.properties.get(PBackgroundDef.TILE_HORIZ);
                    boolean tile_vert = b.properties.get(PBackgroundDef.TILE_VERT);
                    int hspeed = b.properties.get(PBackgroundDef.H_SPEED);
                    int vspeed = b.properties.get(PBackgroundDef.V_SPEED);
                    boolean stretch = b.properties.get(PBackgroundDef.STRETCH);
                    writeBool(visible, out);
                    writeBool(foreground, out);
                    out.write4(getId((ResourceReference<?>) b.properties.get(PBackgroundDef.BACKGROUND)));
                    out.write4(x);
                    out.write4(y);
                    writeBool(tile_horiz, out);
                    writeBool(tile_vert, out);
                    out.write4(hspeed);
                    out.write4(vspeed);
                    writeBool(stretch, out);
                }
                boolean enableViews = r.properties.get(PRoom.ENABLE_VIEWS);
                writeBool(enableViews, out);
                out.write4(r.views.size());
                for (View view : r.views) {
                    // VISIBLE,VIEW_X,VIEW_Y,VIEW_W,VIEW_H,PORT_X,PORT_Y,PORT_W,PORT_H,
                    // BORDER_H,BORDER_V, SPEED_H, SPEED_V, OBJECT
                    boolean visible = view.properties.get(PView.VISIBLE);
                    int view_x = view.properties.get(PView.VIEW_X);
                    int view_y = view.properties.get(PView.VIEW_Y);
                    int view_w = view.properties.get(PView.VIEW_W);
                    int view_h = view.properties.get(PView.VIEW_H);
                    int port_x = view.properties.get(PView.PORT_X);
                    int port_y = view.properties.get(PView.PORT_Y);
                    int port_w = view.properties.get(PView.PORT_W);
                    int port_h = view.properties.get(PView.PORT_H);
                    int border_h = view.properties.get(PView.BORDER_H);
                    int border_v = view.properties.get(PView.BORDER_V);
                    int speed_h = view.properties.get(PView.SPEED_H);
                    int speed_v = view.properties.get(PView.SPEED_V);
                    writeBool(visible, out);
                    out.write4(view_x);
                    out.write4(view_y);
                    out.write4(view_w);
                    out.write4(view_h);
                    out.write4(port_x);
                    out.write4(port_y);
                    out.write4(port_w);
                    out.write4(port_h);
                    out.write4(border_h);
                    out.write4(border_v);
                    out.write4(speed_h);
                    out.write4(speed_v);
                    out.write4(getId((ResourceReference<?>) view.properties.get(PView.OBJECT)));
                }
                out.write4(r.instances.size());

                for (Instance in : r.instances) {
                    ResourceReference<GmObject> or = in.properties.get(PInstance.OBJECT);
                    int id = in.properties.get(PInstance.ID);
                    out.write4(in.getPosition().x);
                    out.write4(in.getPosition().y);
                    out.write4(getId(or));
                    out.write4(id);
                    writeStr(in.getCreationCode(), out);
                }
                out.write4(r.tiles.size());

                for (Tile tile : r.tiles) {
                    ResourceReference<Background> rb = tile.properties.get(PTile.BACKGROUND);
                    int id = tile.properties.get(PTile.ID);
                    out.write4(tile.getRoomPosition().x);
                    out.write4(tile.getRoomPosition().y);
                    out.write4(tile.getBackgroundPosition().x);
                    out.write4(tile.getBackgroundPosition().y);
                    out.write4(tile.getSize().width);
                    out.write4(tile.getSize().height);
                    out.write4(tile.getDepth());
                    out.write4(getId(rb));
                    out.write4(id);
                }
                // Rest of the stuff is useless

                out.close();
                System.out.println("Wrote room " + dat);
            } catch (IOException ex) {
                System.err.println("Cannot write room " + dat);
            }
        }
    }

    private void exportGameInfo(File parentdir) {
        GameInformation info = LGM.currentFile.gameInfo;
        File file = new File(parentdir, "Game Information.info");
        String text = info.get(PGameInformation.TEXT);
        try {
            addRes(file.getName(), GAME_INFO);
            // BACKGROUND_COLOR,MIMIC_GAME_WINDOW,FORM_CAPTION,LEFT,TOP,WIDTH,HEIGHT,SHOW_BORDER,
            // ALLOW_RESIZE, STAY_ON_TOP,PAUSE_GAME,TEXT
            Color bg_color = info.get(PGameInformation.BACKGROUND_COLOR);
            String caption = info.get(PGameInformation.FORM_CAPTION);
            int left = info.get(PGameInformation.LEFT);
            int top = info.get(PGameInformation.TOP);
            int width = info.get(PGameInformation.WIDTH);
            int height = info.get(PGameInformation.HEIGHT);
            boolean mimic = info.get(PGameInformation.MIMIC_GAME_WINDOW);
            boolean show_border = info.get(PGameInformation.SHOW_BORDER);
            boolean allow_resize = info.get(PGameInformation.ALLOW_RESIZE);
            boolean stay_on_top = info.get(PGameInformation.STAY_ON_TOP);
            boolean pause_game = info.get(PGameInformation.PAUSE_GAME);

            StreamEncoder out = new StreamEncoder(file);
            writeStr(text, out);
            out.write4(bg_color.getRGB());
            writeStr(caption, out);
            out.write4(left);
            out.write4(top);
            out.write4(width);
            out.write4(height);
            writeBool(mimic, out);
            writeBool(show_border, out);
            writeBool(allow_resize, out);
            writeBool(stay_on_top, out);
            writeBool(pause_game, out);
            out.close();

            System.out.println("Wrote Game Information.");

        } catch (IOException e) {
            System.err.println("Couldn't write Game information");
            e.printStackTrace();
        }
    }

    private void exportSettings(File parentdir) {
        GameSettings s = LGM.currentFile.gameSettings;
        /*
        GAME_ID,DPLAY_GUID,START_FULLSCREEN,INTERPOLATE,DONT_DRAW_BORDER,DISPLAY_CURSOR,SCALING,
        ALLOW_WINDOW_RESIZE,ALWAYS_ON_TOP,COLOR_OUTSIDE_ROOM,
        SET_RESOLUTION,COLOR_DEPTH,RESOLUTION,FREQUENCY,
        DONT_SHOW_BUTTONS,USE_SYNCHRONIZATION,DISABLE_SCREENSAVERS,LET_F4_SWITCH_FULLSCREEN,
        LET_F1_SHOW_GAME_INFO,LET_ESC_END_GAME,LET_F5_SAVE_F6_LOAD,LET_F9_SCREENSHOT,
        TREAT_CLOSE_AS_ESCAPE,GAME_PRIORITY,
        FREEZE_ON_LOSE_FOCUS,LOAD_BAR_MODE,FRONT_LOAD_BAR,BACK_LOAD_BAR,SHOW_CUSTOM_LOAD_IMAGE,
        LOADING_IMAGE,IMAGE_PARTIALLY_TRANSPARENTY,LOAD_IMAGE_ALPHA,SCALE_PROGRESS_BAR,
        DISPLAY_ERRORS,WRITE_TO_LOG,ABORT_ON_ERROR,TREAT_UNINIT_AS_0,ERROR_ON_ARGS,AUTHOR,VERSION,
        LAST_CHANGED,INFORMATION,
        INCLUDE_FOLDER,OVERWRITE_EXISTING,REMOVE_AT_GAME_END,VERSION_MAJOR,VERSION_MINOR,
        VERSION_RELEASE,VERSION_BUILD,COMPANY,PRODUCT,COPYRIGHT,DESCRIPTION,GAME_ICON
        */
        int gameId = s.get(PGameSettings.GAME_ID);
        boolean startFullscreen = s.get(PGameSettings.START_FULLSCREEN);
        boolean interpolate = s.get(PGameSettings.INTERPOLATE);
        boolean dontDrawBorder = s.get(PGameSettings.DONT_DRAW_BORDER);
        boolean displayCursor = s.get(PGameSettings.DISPLAY_CURSOR);
        int scaling = s.get(PGameSettings.SCALING);
        boolean allowWindowResize = s.get(PGameSettings.ALLOW_WINDOW_RESIZE);
        boolean alwaysOnTop = s.get(PGameSettings.ALWAYS_ON_TOP);
        Color colorOutsideRoom = s.get(PGameSettings.COLOR_OUTSIDE_ROOM);
        boolean setResolution = s.get(PGameSettings.SET_RESOLUTION);
        byte colorDepth = GmFile.GS_DEPTH_CODE.get(s.get(PGameSettings.COLOR_DEPTH)).byteValue();
        byte resolution = GmFile.GS_RESOL_CODE.get(s.get(PGameSettings.RESOLUTION)).byteValue();
        byte frequency = GmFile.GS_FREQ_CODE.get(s.get(PGameSettings.FREQUENCY)).byteValue();
        boolean dontShowButtons = s.get(PGameSettings.DONT_SHOW_BUTTONS);
        boolean useSynchronization = s.get(PGameSettings.USE_SYNCHRONIZATION);
        boolean disableScreensavers = s.get(PGameSettings.DISABLE_SCREENSAVERS);
        boolean letF4SwitchFullscreen = s.get(PGameSettings.LET_F4_SWITCH_FULLSCREEN);
        boolean letF1ShowGameInfo = s.get(PGameSettings.LET_F1_SHOW_GAME_INFO);
        boolean letEscEndGame = s.get(PGameSettings.LET_ESC_END_GAME);
        boolean letF5SaveF6Load = s.get(PGameSettings.LET_F5_SAVE_F6_LOAD);
        boolean letF9Screenshot = s.get(PGameSettings.LET_F9_SCREENSHOT);
        boolean treatCloseAsEscape = s.get(PGameSettings.TREAT_CLOSE_AS_ESCAPE);
        byte gamePriority = GmFile.GS_PRIORITY_CODE.get(s.get(PGameSettings.GAME_PRIORITY)).byteValue();
        boolean freezeOnLoseFocus = s.get(PGameSettings.FREEZE_ON_LOSE_FOCUS);
        byte loadBarMode = GmFile.GS_PROGBAR_CODE.get(s.get(PGameSettings.LOAD_BAR_MODE)).byteValue();
        // Image frontLoadBar
        // Image backLoadBar
        boolean showCustomLoadImage = s.get(PGameSettings.SHOW_CUSTOM_LOAD_IMAGE);
        // Image loadingImage
        boolean imagePartiallyTransparent = s.get(PGameSettings.IMAGE_PARTIALLY_TRANSPARENTY);
        int loadImageAlpha = s.get(PGameSettings.LOAD_IMAGE_ALPHA);
        boolean scaleProgressBar = s.get(PGameSettings.SCALE_PROGRESS_BAR);
        boolean displayErrors = s.get(PGameSettings.DISPLAY_ERRORS);
        boolean writeToLog = s.get(PGameSettings.WRITE_TO_LOG);
        boolean abortOnError = s.get(PGameSettings.ABORT_ON_ERROR);
        boolean treatUninitializedAs0 = s.get(PGameSettings.TREAT_UNINIT_AS_0);
        String author = s.get(PGameSettings.AUTHOR);
        String version = s.get(PGameSettings.VERSION);
        double lastChanged = s.get(PGameSettings.LAST_CHANGED);
        String information = s.get(PGameSettings.INFORMATION);

        int includeFolder = GmFile.GS_INCFOLDER_CODE.get(s.get(PGameSettings.INCLUDE_FOLDER));

        boolean overwriteExisting = s.get(PGameSettings.OVERWRITE_EXISTING);
        boolean removeAtGameEnd = s.get(PGameSettings.REMOVE_AT_GAME_END);

        int versionMajor = s.get(PGameSettings.VERSION_MAJOR);
        int versionMinor = s.get(PGameSettings.VERSION_MINOR);
        int versionRelease = s.get(PGameSettings.VERSION_RELEASE);
        int versionBuild = s.get(PGameSettings.VERSION_BUILD);
        String company = s.get(PGameSettings.COMPANY);
        String product = s.get(PGameSettings.PRODUCT);
        String copyright = s.get(PGameSettings.COPYRIGHT);
        String description = s.get(PGameSettings.DESCRIPTION);
        //ICOFile ico = s.get(PGameSettings.GAME_ICON);

        File f = new File(parentdir, "settings.dat");
        try {
            addRes(f.getName(), SETTINGS);
            StreamEncoder out = new StreamEncoder(f);
            out.write4(gameId);
            writeBool(startFullscreen, out);
            writeBool(interpolate, out);
            writeBool(dontDrawBorder, out);
            writeBool(displayCursor, out);
            out.write4(scaling);
            writeBool(allowWindowResize, out);
            writeBool(alwaysOnTop, out);
            out.write4(colorOutsideRoom.getRGB());
            writeBool(setResolution, out);
            out.write(colorDepth);
            out.write(resolution);
            out.write(frequency);
            writeBool(dontShowButtons, out);
            writeBool(useSynchronization, out);
            writeBool(disableScreensavers, out);
            writeBool(letF4SwitchFullscreen, out);
            writeBool(letF1ShowGameInfo, out);
            writeBool(letEscEndGame, out);
            writeBool(letF5SaveF6Load, out);
            writeBool(letF9Screenshot, out);
            writeBool(treatCloseAsEscape, out);
            out.write(gamePriority);
            writeBool(freezeOnLoseFocus, out);
            out.write(loadBarMode);
            writeBool(showCustomLoadImage, out);
            writeBool(imagePartiallyTransparent, out);
            out.write4(loadImageAlpha);
            writeBool(scaleProgressBar, out);
            writeBool(displayErrors, out);
            writeBool(writeToLog, out);
            writeBool(abortOnError, out);
            writeBool(treatUninitializedAs0, out);
            writeStr(author, out);
            writeStr(version, out);
            out.writeD(lastChanged);
            writeStr(information, out);
            out.write4(includeFolder);
            writeBool(overwriteExisting, out);
            writeBool(removeAtGameEnd, out);
            out.write4(versionMajor);
            out.write4(versionMinor);
            out.write4(versionRelease);
            out.write4(versionBuild);
            writeStr(company, out);
            writeStr(product, out);
            writeStr(copyright, out);
            writeStr(description, out);

            out.close();
        } catch (IOException e) {
            System.err.println("Error writing settings");
            e.printStackTrace();
        }
    }

    private void writeAction(Action act, StreamEncoder out) throws IOException {
        LibAction la = act.getLibAction();

        out.write4(la.parent != null ? la.parent.id : la.parentId);
        out.write4(la.id);

        List<Argument> args = act.getArguments();
        out.write4(args.size());
        for (Argument arg : args) {
            out.write(arg.kind);
            Class<? extends Resource<?, ?>> kind = Argument.getResourceKind(arg.kind);

            if (kind != null && InstantiableResource.class.isAssignableFrom(kind)) {
                out.write(0);
                Resource<?, ?> r = deRef((ResourceReference<?>) arg.getRes());
                if (r != null && r instanceof InstantiableResource<?, ?>)
                    out.write4(((InstantiableResource<?, ?>) r).getId());
                else
                    out.write4(0);
            } else {
                if (la.actionKind == Action.ACT_CODE) {
                    out.write(2);
                    // There should only be 1 argument for Code type action
                    writeStr(arg.getVal(), out);
                } else {
                    out.write(1);
                    writeStr(arg.getVal(), out);
                }
            }
        }
        ResourceReference<GmObject> at = act.getAppliesTo();
        if (at != null) {
            if (at == GmObject.OBJECT_OTHER)
                out.write4(-2);
            else if (at == GmObject.OBJECT_SELF)
                out.write4(-1);
            else
                out.write4(getId(at, -100));
        } else {
            out.write4(-100);
        }
        writeBool(act.isRelative(), out);
        writeBool(act.isNot(), out);

    }

    private void writeImage(BufferedImage i, File f, boolean useTransp) {

        int width = i.getWidth();
        int height = i.getHeight();

        int trans = i.getRGB(0, height - 1) & 0x00FFFFFF;
        // gotta do 2 factor
        int texWidth = 2;
        int texHeight = 2;

        // find the closest power of 2 for the width and height
        // of the produced texture
        while (texWidth < width) {
            texWidth *= 2;
        }
        while (texHeight < height) {
            texHeight *= 2;
        }

        int pixels[] = i.getRGB(0, 0, width, height, null, 0, width);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(pixels.length * 4);
            DeflaterOutputStream dos = new DeflaterOutputStream(baos);

            // RGBA
            write4(width, dos);
            write4(height, dos);
            write4(texWidth, dos);
            write4(texHeight, dos);
            for (int p = 0; p < pixels.length; p++) {
                dos.write(pixels[p] >>> 16 & 0xFF);
                dos.write(pixels[p] >>> 8 & 0xFF);
                dos.write(pixels[p] & 0xFF);
                if (useTransp && ((pixels[p] & 0x00FFFFFF) == trans))
                    dos.write(0);
                else
                    dos.write(pixels[p] >>> 24);
            }

            dos.finish();

            StreamEncoder out = new StreamEncoder(f);
            out.write(baos.toByteArray());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Borrowed from {@link GmStreamEncoder#writeId}
     * 
     * @param id
     * @param noneval
     * @throws IOException
     */
    private <R extends Resource<R, ?>> int getId(ResourceReference<R> id, int noneval) {
        R r = deRef(id);
        if (r != null && r instanceof InstantiableResource<?, ?>)
            return ((InstantiableResource<?, ?>) r).getId();
        else
            return noneval;
    }

    private <R extends Resource<R, ?>> int getId(ResourceReference<R> id) {
        return getId(id, -1);
    }

    private void writeStr(String str, StreamEncoder out) throws IOException {
        byte[] encoded = str.getBytes();
        out.write4(encoded.length);
        out.write(encoded);
    }

    private void writeBool(boolean bool, StreamEncoder out) throws IOException {
        out.write(bool ? 1 : 0);
    }

    private void write4(int val, OutputStream s) throws IOException {
        s.write(val & 255);
        s.write((val >>> 8) & 255);
        s.write((val >>> 16) & 255);
        s.write((val >>> 24) & 255);
    }
}