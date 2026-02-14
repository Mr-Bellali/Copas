package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Card {
    public BufferedImage frontView, backView;
    static int id = 0;
    private String type;
    private int number;
    public int x, y;

    GamePanel gp;
    KeyHandler keyHandler;

    public Card(String type, int number, GamePanel gp, KeyHandler keyHandler) {
        this.type = type;
        this.number = number;

        this.gp = gp;
        this.keyHandler = keyHandler;
        setDefaultValues();
        getCardImage();
        id++;
    }

    public void displayInfo() {
        IO.println("type: " + this.type);
        IO.println("number: " + this.number);
    }

    public void setDefaultValues() {
        x = 100;
        y = 100;
    }

    // Function to get the card image
    public void getCardImage() {
        try {
            frontView = ImageIO.read(getClass().getClassLoader().getResourceAsStream("cards/comodines1.png"));
            backView = ImageIO.read(getClass().getClassLoader().getResourceAsStream("cards/cardBack.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2){
        BufferedImage frontViewImage = null;
        BufferedImage backViewImage = null;

        frontViewImage = frontView;
        backViewImage = backView;

        g2.drawImage(frontViewImage, x, y, gp.tileSize, gp.tileSize, null);
        g2.drawImage(backViewImage, x+x, y+y, gp.tileSize, gp.tileSize, null);
    }

}