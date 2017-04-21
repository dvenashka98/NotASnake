import org.lwjgl.LWJGLException;

import org.lwjgl.input.Mouse;
import sun.audio.*;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.TrueTypeFont;
import org.newdawn.slick.Color;

import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;

public class Core {
    /** "moving direction" flag*/
    int flag = 0;
    /** "who won" flag*/
    int flag1 = 0;
    /** "music" flag*/
    public boolean mflag = false;
    int delta1Increaser = 0;
    int rand;

    int displayWidth = Display.getDisplayMode().getWidth();
    int displayHeight = Display.getDisplayMode().getHeight();

    /** position of rectangle1 */
    float x1 = 40, y1 = displayHeight / 2;
    /** position of rectangle2 */
    float x2 = displayWidth - 40, y2 = displayHeight / 2;
    /** position of ball */
    float x3 = displayWidth / 2, y3 = displayHeight / 2;
    /** angle of rectangles rotation */
    float rotation = 0;

    /** The font to draw to the screen */
    private TrueTypeFont font;
    /** Boolean flag on whether AntiAliasing is enabled or not */
    private boolean antiAlias = true;
    /** time at last frame */
    long lastFrame;

    /** frames per second */
    int fps;
    /** last fps time */
    long lastFPS;

    /** is VSync Enabled */
    boolean vsync;

    public void start() {
        try {
            Display.setDisplayMode(new DisplayMode(800, 600));
            if (flag1 == 0) {
                Display.create();
                flag1 = 1;
            }
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        initGL(); // init OpenGL
        getDelta(); // call once before loop to initialise lastFrame
        lastFPS = getTime(); // call before loop to initialise fps timer

        while (!Display.isCloseRequested()) {
            int delta = getDelta();
            int delta1 = delta + (delta1Increaser * delta) / 4000;
            update(delta, delta1);
            renderGL();

            Display.update();
            Display.sync(70); // cap fps to 70fps
        }

        Display.destroy();
    }

    public void update(int delta, int delta1) {
        // rotate rectangle
        rotation += 0.0f * delta;

        if (Keyboard.isKeyDown(Keyboard.KEY_W)) y1 += 0.35f * delta;
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) y1 -= 0.35f * delta;
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) x1 += 0.35f * delta;
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) x1 -= 0.35f * delta;

        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) y2 += 0.35f * delta;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) y2 -= 0.35f * delta;

        //right won
        if (x3 < -14) {
            flag1 = 0;
            startText();
        }

        //left won
        if (x3 > displayWidth + 14) {
            flag1 = 1;
            startText();
        }

        //flags for up and down borders for ball and ball tangency with rectangles
        if (flag == 0) {
            delta1Increaser++;
            y3 += 0.15f * (delta1 + rand);
            x3 += 0.3f * (delta1);
        }
        if (flag == 1) {
            delta1Increaser++;
            y3 -= 0.15f * (delta1);
            x3 += 0.3f * (delta1 + rand);
        }
        if (flag == 2) {
            delta1Increaser++;
            y3 += 0.15f * (delta1 + rand);
            x3 -= 0.3f * (delta1);
        }
        if (flag == 3) {
            delta1Increaser++;
            y3 -= 0.15f * (delta1);
            x3 -= 0.3f * (delta1 + rand);
        }

        //up and down borders for ball
        if (y3 < 15) {
            if (flag == 1) {
                flag = 0;
            }
            else {
                flag = 2;
            }
        }
        if (y3 > 785) {
            if (flag == 0) {
                flag = 1;
            }
            else {
                flag = 3;
            }
        }

        //ball tangency with rectangle1
        if (((Math.abs(x1 - x3)) < 35) && ((Math.abs(y1 - y3)) < 75) && (x3 > 55)) {
            if (flag == 2) {
                flag = 0;
                rand = getRand();
            }
            else {
                flag = 1;
                rand = getRand();
            }
        }

        //ball tangency with rectangle2
        if (((Math.abs(x2 - x3)) < 35) && ((Math.abs(y2 - y3)) < 75) && (x3 < (displayWidth - 55))) {
            if (flag == 0) {
                flag = 2;
                rand = getRand();
            }
            else {
                flag = 3;
                rand = getRand();
            }
        }


        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                //fullscreen
                if (Keyboard.getEventKey() == Keyboard.KEY_F) {
                    setDisplayMode(800, 600, !Display.isFullscreen());
                }
                //vsync
                if (Keyboard.getEventKey() == Keyboard.KEY_V) {
                    vsync = !vsync;
                    Display.setVSyncEnabled(vsync);
                }

                if (Keyboard.getEventKey() == Keyboard.KEY_M) {
                    music("intro.wav", mflag);
                }
            }
        }

        // keep rectangles on the screen
        if (x1 < 0) x1 = 0;
        if (x1 > displayWidth - 20) x1 = displayWidth - 20;
        if (y1 < 60) y1 = 60;
        if (y1 > displayHeight - 60) y1 = displayHeight - 60;
        if (x2 < 0) x2 = 0;
        if (x2 > displayWidth - 20) x2 = displayWidth - 20;
        if (y2 < 60) y2 = 60;
        if (y2 > displayHeight - 60) y2 = displayHeight - 60;

        updateFPS(); // update FPS Counter
    }

    /**
     * Set the display mode to be used
     *
     * param width - The width of the display required
     * param height - The height of the display required
     * param fullscreen - True if we want fullscreen mode
     */
    public void setDisplayMode(int width, int height, boolean fullscreen) {

        // return if requested DisplayMode is already set
        if ((displayWidth == width) &&
                (displayHeight == height) &&
                (Display.isFullscreen() == fullscreen)) {
            return;
        }

        try {
            DisplayMode targetDisplayMode = null;
            Mouse.create();

            if (fullscreen) {
                Mouse.setGrabbed(true);
                DisplayMode[] modes = Display.getAvailableDisplayModes();
                int freq = 0;

                for (int i=0;i<modes.length;i++) {
                    DisplayMode current = modes[i];

                    if ((current.getWidth() == width) && (current.getHeight() == height)) {
                        if ((targetDisplayMode == null) || (current.getFrequency() >= freq)) {
                            if ((targetDisplayMode == null) || (current.getBitsPerPixel() > targetDisplayMode.getBitsPerPixel())) {
                                targetDisplayMode = current;
                                freq = targetDisplayMode.getFrequency();
                            }
                        }

                        /*
                        if we've found a match for bpp and frequency against the
                        original display mode then it's probably best to go for this one
                        since it's most likely compatible with the monitor
                        */
                        if ((current.getBitsPerPixel() == Display.getDesktopDisplayMode().getBitsPerPixel()) &&
                                (current.getFrequency() == Display.getDesktopDisplayMode().getFrequency())) {
                            targetDisplayMode = current;
                            break;
                        }
                    }
                }
            } else {
                Mouse.setGrabbed(false);
                Mouse.destroy();
                targetDisplayMode = new DisplayMode(width,height);
            }

            if (targetDisplayMode == null) {
                System.out.println("Failed to find value mode: "+width+"x"+height+" fs="+fullscreen);
                return;
            }

            Display.setDisplayMode(targetDisplayMode);
            Display.setFullscreen(fullscreen);

        } catch (LWJGLException e) {
            System.out.println("Unable to setup mode "+width+"x"+height+" fullscreen="+fullscreen + e);
        }
    }

    public void startText() {
        initGLText(800,600);
        init();

        while (true) {
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            render();

            Display.update();
            Display.sync(70);


            if (Display.isCloseRequested()) {
                Display.destroy();
                System.exit(0);
            }
        }
    }

    /**
     * Initialise the GL display
     *
     * @param width The width of the display
     * @param height The height of the display
     */
    private void initGLText(int width, int height) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClearDepth(1);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glViewport(0,0,width,height);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    /**
     * Initialise resources
     */
    public void init() {
        // load a default java font
        Font awtFont = new Font("Arial Black", Font.BOLD, 24);
        font = new TrueTypeFont(awtFont, antiAlias);
    }

    /**
     * Game loop render
     */
    public void render() {
        Color.white.bind();
        if (flag1 == 0) {
            font.drawString(297, 270, "RIGHT WON->", Color.cyan);
            while (Keyboard.next())
                if (Keyboard.getEventKeyState())
                    //fullscreen
                    if (Keyboard.getEventKey() == Keyboard.KEY_F)
                        setDisplayMode(800, 600, !Display.isFullscreen());
        }
        else {
            font.drawString(310, 270, "<-LEFT WON", Color.pink);
            while (Keyboard.next())
                if (Keyboard.getEventKeyState())
                    //fullscreen
                    if (Keyboard.getEventKey() == Keyboard.KEY_F)
                        setDisplayMode(800, 600, !Display.isFullscreen());
        }
    }

    //ms since last frame
    /**
     * Calculate how many milliseconds have passed
     * since last frame.
     *
     * return milliseconds passed since last frame
     */
    public int getDelta() {
        long time = getTime();
        int delta = (int) (time - lastFrame);
        lastFrame = time;
        return delta;
    }
    //sys time
    /**
     * Get the accurate system time
     *
     * return The system time in milliseconds
     */
    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    public int getRand() {
        return (new Random(System.currentTimeMillis()).nextInt(7) - 3);
    }
    //current fps
    /**
     * Calculate the FPS and set it in the title bar
     */
    public void updateFPS() {
        if (getTime() - lastFPS > 1000) {
            Display.setTitle("FPS: " + fps);
            fps = 0;
            lastFPS += 1000;
        }
        fps++;
    }

    public void initGL() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, displayWidth, 0, displayHeight, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    public void renderGL() {
        // Clear The Screen And The Depth Buffer
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // draw rectangle1
        GL11.glPushMatrix();
        GL11.glColor3f(1.0f, 0.5f, 1.0f);
        GL11.glTranslatef(x1, y1, 0);
        GL11.glRotatef(rotation, 0f, 0f, 1f);
        GL11.glTranslatef(-x1, -y1, 0);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1 - 20, y1 - 60);
        GL11.glVertex2f(x1 + 20, y1 - 60);
        GL11.glVertex2f(x1 + 20, y1 + 60);
        GL11.glVertex2f(x1 - 20, y1 + 60);
        GL11.glEnd();
        GL11.glPopMatrix();

        //draw rectangle2
        GL11.glPushMatrix();
        GL11.glColor3f(0.5f, 1.0f, 1.0f);
        GL11.glTranslatef(x2, y2, 0);
        GL11.glRotatef(rotation, 0f, 0f, 1f);
        GL11.glTranslatef(-x2, -y2, 0);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x2 - 20, y2 - 60);
        GL11.glVertex2f(x2 + 20, y2 - 60);
        GL11.glVertex2f(x2 + 20, y2 + 60);
        GL11.glVertex2f(x2 - 20, y2 + 60);
        GL11.glEnd();
        GL11.glPopMatrix();

        //draw ball
        GL11.glPushMatrix();
        GL11.glColor3f(1.0f, 1.0f, 0.5f);
        GL11.glTranslatef(x3, y3, 0);
        GL11.glRotatef(rotation, 0f, 0f, 1f);
        GL11.glTranslatef(-x3, -y3, 0);

        float theta;
        float pi = 22/7;
        float radius = 15.0f; //radius
        float step = 0.5f; //the smaller the step, the better the circle

        //draw a circle clockwise
        GL11.glBegin(GL_TRIANGLE_FAN);
        for(float a = 0.0f; a < 360.0f; a += step) {
            theta = 2.0f * pi * a / 180.0f;
            GL11.glColor3f(1.0f, 1.0f,0.5f);
            GL11.glVertex2d(x3 + radius * Math.cos(theta),y3 + radius * Math.sin(theta));
        }
        GL11.glEnd();
        GL11.glPopMatrix();

    }

    public void music(String s, boolean mflag) {
        if (mflag == true) return;
        AudioPlayer MGP = AudioPlayer.player;
        AudioStream BGM;
        AudioData MD;
        ContinuousAudioDataStream loop = null;
        try {
            BGM = new AudioStream(new FileInputStream(s));
            MD = BGM.getData();
            loop = new ContinuousAudioDataStream(MD);
        } catch (IOException error) {
            System.out.println("Music file not found");
        }
        MGP.start(loop);
        this.mflag = true;
    }


    public static void main(String[] argv) {
        System.setProperty("org.lwjgl.librarypath", new File("libs/natives").getAbsolutePath());
        Core core = new Core();
        core.start();
    }
}