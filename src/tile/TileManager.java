package tile;

import main.GamePanel;

public class TileManager {

    GamePanel pg;
    Tile[] tile;

    public TileManager(GamePanel gp) {
        this.pg = gp;
        tile = new Tile[10];
        getTileImage();
    }

    public void getTileImage(){
        try {
            tile[0] = new Tile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
