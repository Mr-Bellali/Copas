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

    Thread gameThread;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
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

    }

    //    Methode to draw
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        g2.setColor(Color.white);
        g2.fillRect(100, 100, tileSize , tileSize);
        g2.dispose();
    }
}
