/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.vivisect.swing;

import automenta.vivisect.Vis;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import processing.core.PApplet;
import static processing.core.PConstants.DOWN;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.RIGHT;
import static processing.core.PConstants.UP;
import processing.event.MouseEvent;

/**
 *
 * @author me
 */
public class PCanvas extends PApplet implements HierarchyListener {

    int mouseScroll = 0;

    Hnav hnav = new Hnav();

    float zoom = 0.1f;
    float scale = 1f;
    float selection_distance = 10;
    float FrameRate = 25f;

    boolean drawn = false;
    float motionBlur = 0.0f;
    private final Vis vis;

    float camspeed = 20.0f;
    float scrollcammult = 0.92f;
    boolean keyToo = true;
    float savepx = 0;
    float savepy = 0;
    int selID = 0;

    float difx = 0;
    float dify = 0;
    int lastscr = 0;
    boolean EnableZooming = true;
    float scrollcamspeed = 1.1f;

    public PCanvas() {
        this(null);
    }
    
    public PCanvas(Vis vis) {
        this(vis, 26);
    }

    public PCanvas(Vis vis, int frameRate) {
        super();
        this.FrameRate = frameRate;
        init();
        if ((vis == null) && (this instanceof Vis)) {
            //for subclasses:
            vis = (Vis)this;
        }
        this.vis = vis;
        vis.init(this);
    }

    float MouseToWorldCoordX(final int x) {
        return 1 / zoom * (x - difx - width / 2);
    }

    float MouseToWorldCoordY(final int y) {
        return 1 / zoom * (y - dify - height / 2);
    }

    public float getPanX() {
        return difx;
    }
    public float getPanY() {
        return dify;
    }
    
    public void setPanX(float px) { difx = -px; }
    public void setPanY(float py) { dify = -py; }
    
    public float getCursorX() {
        return MouseToWorldCoordX(mouseX);
    }

    public float getCursorY() {
        return MouseToWorldCoordY(mouseY);
    }

    @Override
    protected void resizeRenderer(int newWidth, int newHeight) {
        if ((newWidth > 0) && (newHeight > 0)) {
            super.resizeRenderer(newWidth, newHeight);
            drawn = false;
        }
    }

    public void mouseScrolled() {
        hnav.mouseScrolled();
    }

    @Override
    public void keyPressed() {
        hnav.keyPressed();
    }

    @Override
    public void mouseMoved() {
    }

    @Override
    public void mouseReleased() {
        hnav.mouseReleased();
        super.mouseReleased();
    }

    @Override
    public void mouseDragged() {
        hnav.mouseDragged();
        super.mouseDragged();
    }

    @Override
    public void mousePressed() {
        hnav.mousePressed();
        super.mousePressed();
    }

    @Override
    public void draw() {
        

        if (drawn) {
            return;
        }

        drawn = false;

        if (motionBlur > 0) {
            fill(0, 0, 0, 255f * (1.0f - motionBlur));
            rect(0, 0, getWidth(), getHeight());
        } else {
            background(0, 0, 0);//, 0.001f);
        }

        hnav.applyTransform();
        vis.draw(g);
    }

    public void setMotionBlur(float motionBlur) {
        this.motionBlur = motionBlur;
    }

    public float getMotionBlur() {
        return motionBlur;
    }

    
    
    @Override
    public void mouseWheel(MouseEvent event) {
        super.mouseWheel(event);
        mouseScroll = -event.getCount();
        mouseScrolled();
    }

    @Override
    public void setup() {

        //size(500,500,P3D);
        frameRate(FrameRate);

        if (isGL()) {
            textFont(createDefaultFont(16));
            smooth();
            System.out.println("Processing.org enabled OpenGL");
        }
        
    }
    
   
    @Override
    public void addNotify() {
        super.addNotify();
        addHierarchyListener(this);
    }

    @Override
    public void removeNotify() {
        removeHierarchyListener(this);
        super.removeNotify();
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            boolean showing = isShowing();
            onShowing(showing);
                        
        }
    }    

    
    protected void onShowing(boolean showing) {
        vis.onVisible(showing);
        
        if (showing) {
            //restart loop? can this even happen
            //throw new RuntimeException("if this happens, looping state should be restored here");
        }
        else {
            noLoop();
        }
    }
    

    public PCanvas setFrameRate(float frameRate) {
        this.FrameRate = frameRate;
        frameRate(FrameRate);
        return this;
    }

    public float getFrameRate() {
        return frameRate;
    }
    
    public PCanvas setZoom(float x, float y, float z) {
        setPanX(x);
        setPanY(y);
        setZoom(z);
        return this;
    }

    public PCanvas setZoom(float f) {
        zoom = f;
        return this;
    }

    public float getZoom() {
        return zoom;
    }

    /** zoom to a rectangular region */
    public void setZoom(float cx, float cy, float width, float height) {
        //TODO add margin, right-click zoom out
        
        //System.out.println("auto-zoom: " + cx + " " + cy + " " + width + " " + height);
        
        //https://github.com/automenta/automenta.spacegraphj/blob/master/spacegraph/automenta/spacegraph/control/FractalControl.java#L128
        
       // float targetZ = getTargetDepth(width, height); //TODO calculate correct height
        float targetZ = this.width / Math.max(width,height);
        
        //System.out.print(getPanX() + " " + getPanY() + " " + getZoom() + " --> ");        
        float tx = (cx) * targetZ;
        float ty = (cy) * targetZ;
        //System.out.println(tx + " " + ty + " " + targetZ);
        setZoom(tx, ty, targetZ);

    }
    /*static float getTargetDepth(float width, float height) {
        float zoomDilation = 1.0f;
        float r = Math.max(width, height) / 2.0f * zoomDilation;
        final float focus = (float)Math.PI/4f;
        return (float) (r * Math.sin(Math.PI / 2.0 - focus / 2.0) / Math.sin(focus / 2.0));
    }*/

    class Hnav {

        private boolean md = false;

        void mousePressed() {
            md = true;
            if (mouseButton == RIGHT) {
                savepx = mouseX;
                savepy = mouseY;
                redraw();
            }
        }

        void mouseReleased() {
            md = false;
        }

        void mouseDragged() {
            if (mouseButton == RIGHT) {
                difx += (mouseX - savepx);
                dify += (mouseY - savepy);
                savepx = mouseX;
                savepy = mouseY;
                redraw();                
            }
        }

        void keyPressed() {
            if ((keyToo && key == 'w') || keyCode == UP) {
                dify += (camspeed);
            }
            if ((keyToo && key == 's') || keyCode == DOWN) {
                dify += (-camspeed);
            }
            if ((keyToo && key == 'a') || keyCode == LEFT) {
                difx += (camspeed);
            }
            if ((keyToo && key == 'd') || keyCode == RIGHT) {
                difx += (-camspeed);
            }
            if (!EnableZooming) {
                return;
            }
            if (key == '-' || key == '#') {
                float zoomBefore = zoom;
                zoom *= scrollcammult;
                difx = (difx) * (zoom / zoomBefore);
                dify = (dify) * (zoom / zoomBefore);
            }
            if (key == '+') {
                float zoomBefore = zoom;
                zoom /= scrollcammult;
                difx = (difx) * (zoom / zoomBefore);
                dify = (dify) * (zoom / zoomBefore);
            }
            redraw();            
            drawn = false;
        }

        void Init() {
            difx = -width / 2;
            dify = -height / 2;
        }

        void mouseScrolled() {
            if (!EnableZooming) {
                return;
            }
            float zoomBefore = zoom;
            if (mouseScroll > 0) {
                zoom *= scrollcamspeed;
            } else {
                zoom /= scrollcamspeed;
            }
            difx = (difx) * (zoom / zoomBefore);
            dify = (dify) * (zoom / zoomBefore);
            redraw();
            drawn = false;
        }

        void applyTransform() {
            translate(difx + 0.5f * width, dify + 0.5f * height);
            scale(zoom, zoom);
        }
    }

}
