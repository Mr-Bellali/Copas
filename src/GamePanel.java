import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {
    //    Screen settings
    final int originalTilesSize = 16; // 16x16 tile
    final int scale = 3;

    final int tileSize = originalTilesSize * scale; // 48x48 tile
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    final int screenWidth = tileSize * maxScreenCol; // 768 pixels
    final int screenHeight = tileSize * maxScreenRow; // 576 pixels

    KeyHandler keyHandler = new KeyHandler();
    Thread gameThread;

//    Set player's default position
    int playerX = 100;
    int playerY = 100;
    int playerSpeed = 4;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyHandler);
        this.setFocusable(true);
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (gameThread != null) {
//        1 Update
            update();

//        2 Draw the screen with updated information
            repaint();
        }

    }

    //    Methode to update the frame
    public void update() {
        if(keyHandler.upPressed){
            playerY -= playerSpeed;
            try {
                Thread.sleep(playerSpeed);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if(keyHandler.downPressed){
            playerY += playerSpeed;
            try {
                Thread.sleep(playerSpeed);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
        if(keyHandler.leftPressed){
            playerX -= playerSpeed;
            try {
                Thread.sleep(playerSpeed);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
        if(keyHandler.rightPressed){
            playerX += playerSpeed;
            try {
                Thread.sleep(playerSpeed);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }

    //    Methode to draw
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        g2.setColor(Color.white);
        g2.fillRect(playerX, playerY, tileSize , tileSize);
        g2.dispose();
    }
}
