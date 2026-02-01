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

    //    Game's frame rate
    int FPS = 60;

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
//    public void run() {
//        double drawInterval = 1000000000 / FPS; // 0.0166666 seconds
//        double nextDrawTime = System.nanoTime() + drawInterval;
//
//        while (gameThread != null) {
////          1 Update
//            update();
//
////          2 Draw the screen with updated information
//            repaint();
//
//            try {
//                double remainingTime = (nextDrawTime - System.nanoTime()) / 1000000;
//
//                if(remainingTime < 0){
//                    remainingTime = 0;
//                }
//
//                Thread.sleep((long) remainingTime);
//
//                nextDrawTime += drawInterval;
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }

    public void run() {
        double drawInterval = 1000000000 / FPS; // 0.0166666 seconds
        double delta = 0;
        long timer = 0;
        int drawCount = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            timer += (currentTime - lastTime);

            lastTime = currentTime;

            if (delta >= 1) {
                // 1 Update
                update();

                // 2 Draw the screen with updated information
                repaint();
                delta--;
                drawCount++;
            }
            if(timer >= 1000000000){
                IO.println("FPS: " + drawCount);
                drawCount = 0;
                timer = 0;
            }

        }
    }

    //    Methode to update the frame
    public void update() {
        if (keyHandler.upPressed) {
            playerY -= playerSpeed;
        }
        if (keyHandler.downPressed) {
            playerY += playerSpeed;

        }
        if (keyHandler.leftPressed) {
            playerX -= playerSpeed;

        }
        if (keyHandler.rightPressed) {
            playerX += playerSpeed;

        }
    }

    //    Methode to draw
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.white);
        g2.fillRect(playerX, playerY, tileSize, tileSize);
        g2.dispose();
    }
}
