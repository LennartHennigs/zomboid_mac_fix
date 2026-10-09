package pzmousefix;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Runtime helpers called from the patched game classes.
 * onButton: injected at the start of org.lwjglx.input.Mouse.addButtonEvent (GLFW callback).
 * isDown:   replaces Mouse.isButtonDown inside zombie.input.MouseState.poll (render thread).
 */
public final class MouseFix {
    private static final boolean[] latch = new boolean[16];
    private static final long[] pressNanos = new long[16];
    private static final boolean[] lastSample = new boolean[16];
    private static final boolean[] sawDownSincePress = new boolean[16];
    static boolean fix = true;
    static boolean log = false;
    private static PrintStream out;
    private static int presses, missed;

    static void init(boolean fixOn, boolean logOn) {
        fix = fixOn;
        log = logOn;
        {
            try {
                Path p = Path.of(System.getProperty("user.home"), "pz-mouse-fix.log");
                out = new PrintStream(Files.newOutputStream(p, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING), true);
                out.println("# pz-mouse-fix loaded, fix=" + fixOn + " log=" + logOn);
            } catch (Exception e) {
                log = false;
                out = null;
            }
        }
    }

    static void note(String m) { if (out != null) out.println("# " + m); }

    public static synchronized void onButton(int button, boolean down) {
        if (button < 0 || button >= latch.length) return;
        long now = System.nanoTime();
        if (down) {
            latch[button] = true;
            pressNanos[button] = now;
            sawDownSincePress[button] = false;
            presses++;
        } else if (log && !sawDownSincePress[button]) {
            missed++;
        }
        if (log) {
            String extra = down ? "" : String.format(" held=%.1fms sampledDuringHold=%s",
                    (now - pressNanos[button]) / 1e6, sawDownSincePress[button]);
            out.printf("%d btn=%d %s%s | presses=%d withoutSampledDown=%d%n",
                    now / 1_000_000, button, down ? "PRESS" : "RELEASE", extra, presses, missed);
        }
    }

    public static synchronized boolean isDown(int button) {
        boolean raw = org.lwjglx.input.Mouse.isButtonDown(button);
        boolean r = raw;
        if (button >= 0 && button < latch.length) {
            if (fix && latch[button]) r = true;
            latch[button] = false;
            if (raw) sawDownSincePress[button] = true;
            if (log && r != lastSample[button]) {
                out.printf("%d btn=%d sample=%s (raw=%s)%n", System.nanoTime() / 1_000_000, button, r, raw);
            }
            if (r) sawDownSincePress[button] = true;
            lastSample[button] = r;
        }
        return r;
    }
}
