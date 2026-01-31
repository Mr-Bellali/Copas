import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {
//    Screen settings
    final int originalTilesSize = 16; // 16x16 tile
    final int scale = 3;

    final int tileSize = originalTilesSize * scale; // 48x48 tile
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    final int screenWidth = tileSize * maxScreenCol; // 768 pixels
    final int screenHeight = tileSize * maxScreenRow; // 576 pixels

    public GamePanel(){
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
    }
}
