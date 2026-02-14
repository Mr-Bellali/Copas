package main;

import entity.Player;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {
    //    Screen settings
    final int originalTilesSize = 64; // 16x16 tile
    final int scale = 2;

    public int tileSize = originalTilesSize * scale; // 48x48 tile
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    final int screenWidth = (tileSize * maxScreenCol)/2; // 768 pixels
    final int screenHeight = (tileSize * maxScreenRow)/2; // 576 pixels

    //    Game's frame rate
    int FPS = 60;

    KeyHandler keyHandler = new KeyHandler();
    Thread gameThread;
    Player player = new Player(this, keyHandler);
    Card card = new Card("joker", 0, this, keyHandler);

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
            if (timer >= 1000000000) {
                IO.println("FPS: " + drawCount);
                drawCount = 0;
                timer = 0;
            }

        }
    }

    //    Methode to update the frame
    public void update() {
        player.update();
    }

    //    Methode to draw
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        player.draw(g2);
        card.draw(g2);
        g2.dispose();
    }
}
