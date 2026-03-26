package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Basic WASD key state tracker.
 *
 * This class currently is not used by the card flow, but it keeps directional
 * pressed/released states that can be reused for movement-based screens.
 */
public class KeyHandler implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed; // live key states read by update loops

    /** Not used; included to satisfy KeyListener. */
    @Override
    public void keyTyped(KeyEvent e) {

    }

    /** Marks WASD flags as pressed when key down events arrive. */
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) {
            upPressed = true;
        }
        if (code == KeyEvent.VK_S) {
            downPressed = true;
        }
        if (code == KeyEvent.VK_A) {
            leftPressed = true;
        }
        if (code == KeyEvent.VK_D) {
            rightPressed = true;
        }
    }

    /** Clears WASD flags on key release events. */
    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) {
            upPressed = false;
        }
        if (code == KeyEvent.VK_S) {
            downPressed = false;
        }
        if (code == KeyEvent.VK_A) {
            leftPressed = false;
        }
        if (code == KeyEvent.VK_D) {
            rightPressed = false;
        }
    }
}
