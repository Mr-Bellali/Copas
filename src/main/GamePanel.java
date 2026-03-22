package main;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class GamePanel extends JPanel implements Runnable {
    //    Screen settings
    final int originalTilesSize = 64; // 16x16 tile
    final int scale = 2;

    public int tileSize = originalTilesSize * scale; // 48x48 tile
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    final int screenWidth = (tileSize * maxScreenCol) / 2; // 768 pixels
    final int screenHeight = (tileSize * maxScreenRow) / 2; // 576 pixels

    //    Game's frame rate
    int FPS = 60;

    Thread gameThread;
    private static final int PLAYER_HAND_SIZE = 4;
    private static final int TABLE_MARGIN = 24;
    private static final int STACK_OFFSET = 4;
    private static final int MAX_VISIBLE_STACK = 6;
    private static final double HAND_FAN_SPREAD_DEGREES = 36.0;
    private final int cardWidth = tileSize - 24;
    private final int cardHeight = (int) Math.round(cardWidth * 1.45);

    private Deck drawPile;
    private Card centerCard;
    private List<Card> playerHand;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(16, 94, 58));
        this.setDoubleBuffered(true);
        this.initializeTable();
    }

    private void initializeTable() {
        drawPile = Deck.createShuffledSpanishDeck();
        centerCard = drawPile.drawCard();
        playerHand = drawPile.dealCards(PLAYER_HAND_SIZE);
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }

        }
    }

    //    Methode to update the frame
    public void update() {
        // Table is static for now.
    }

    //    Methode to draw
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawCenterCard(g2);
        drawPlayerHand(g2);
        drawDrawPile(g2);

        g2.dispose();
    }

    private void drawCenterCard(Graphics2D g2) {
        if (centerCard == null) {
            return;
        }

        int x = (screenWidth - cardWidth) / 2;
        int y = (screenHeight - cardHeight) / 2 - 36;
        centerCard.drawFront(g2, x, y, cardWidth, cardHeight);
    }

    private void drawPlayerHand(Graphics2D g2) {
        if (playerHand == null || playerHand.isEmpty()) {
            return;
        }

        double fanRadius = cardHeight + 95.0;
        double pivotX = screenWidth / 2.0;
        double pivotY = screenHeight + 220.0;
        double totalSpread = playerHand.size() == 1 ? 0 : HAND_FAN_SPREAD_DEGREES;
        double angleStep = playerHand.size() == 1 ? 0 : totalSpread / (playerHand.size() - 1);
        double startAngle = -totalSpread / 2.0;

        for (int i = 0; i < playerHand.size(); i++) {
            double angleDegrees = startAngle + (angleStep * i);
            double angleRadians = Math.toRadians(angleDegrees);

            double cardCenterX = pivotX + (Math.sin(angleRadians) * fanRadius);
            double cardTopY = pivotY - (Math.cos(angleRadians) * fanRadius) - cardHeight;
            int x = (int) Math.round(cardCenterX - (cardWidth / 2.0));
            int y = (int) Math.round(cardTopY);

            Graphics2D cardGraphics = (Graphics2D) g2.create();
            cardGraphics.rotate(angleRadians, x + (cardWidth / 2.0), y + (cardHeight * 0.92));
            playerHand.get(i).drawFront(cardGraphics, x, y, cardWidth, cardHeight);
            cardGraphics.dispose();
        }
    }

    private void drawDrawPile(Graphics2D g2) {
        if (drawPile == null || drawPile.isEmpty()) {
            return;
        }

        int visibleCards = Math.min(drawPile.size(), MAX_VISIBLE_STACK);
        int baseX = screenWidth - cardWidth - TABLE_MARGIN;
        int baseY = screenHeight - cardHeight - TABLE_MARGIN;

        for (int i = visibleCards - 1; i >= 0; i--) {
            int x = baseX - (i * STACK_OFFSET);
            int y = baseY - (i * STACK_OFFSET);
            Card.drawBackImage(g2, x, y, cardWidth, cardHeight);
        }
    }
}