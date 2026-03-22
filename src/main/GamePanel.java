package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
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
    private static final int TABLE_MARGIN = 24;
    private static final int STACK_OFFSET = 4;
    private static final int MAX_VISIBLE_STACK = 6;
    private static final double HAND_FAN_SPREAD_DEGREES = 36.0;
    private static final String[] SUIT_OPTIONS = {"Basto", "Copa", "Espada", "Oro"};
    private final int cardWidth = tileSize - 24;
    private final int cardHeight = (int) Math.round(cardWidth * 1.45);
    private final Font hudFont = new Font("SansSerif", Font.BOLD, 16);
    private final Font smallHudFont = new Font("SansSerif", Font.PLAIN, 14);

    private final CopasGameState gameState;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(new Color(16, 94, 58));
        this.setDoubleBuffered(true);
        this.gameState = new CopasGameState();
        this.addMouseListener(new TableMouseHandler());
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
        drawHud(g2);

        g2.dispose();
    }

    private void drawCenterCard(Graphics2D g2) {
        int x = (screenWidth - cardWidth) / 2;
        int y = (screenHeight - cardHeight) / 2 - 36;
        gameState.getTopCard().drawFront(g2, x, y, cardWidth, cardHeight);

        g2.setFont(smallHudFont);
        g2.setColor(Color.WHITE);
        g2.drawString("Active suit: " + gameState.getActiveSuit(), x - 8, y - 12);
        g2.drawString("Top card: " + gameState.getTopCard().getDisplayName(), x - 8, y + cardHeight + 18);
    }

    private void drawPlayerHand(Graphics2D g2) {
        List<CardPlacement> placements = getPlayerHandPlacements();
        if (placements.isEmpty()) {
            return;
        }

        for (CardPlacement placement : placements) {
            Graphics2D cardGraphics = (Graphics2D) g2.create();
            cardGraphics.rotate(placement.angleRadians(), placement.rotationPivotX(), placement.rotationPivotY());
            placement.card().drawFront(cardGraphics, placement.x(), placement.y(), cardWidth, cardHeight);

            if (gameState.isHumanTurn() && gameState.isHumanCardPlayable(placement.handIndex())) {
                cardGraphics.setColor(new Color(255, 255, 255, 170));
                cardGraphics.setStroke(new BasicStroke(3f));
                cardGraphics.drawRoundRect(placement.x(), placement.y(), cardWidth, cardHeight, 16, 16);
            }

            cardGraphics.dispose();
        }
    }

    private void drawDrawPile(Graphics2D g2) {
        if (gameState.hasAvailableDrawPile()) {
            int visibleCards = Math.max(1, Math.min(gameState.getDrawPileSize(), MAX_VISIBLE_STACK));
            int baseX = screenWidth - cardWidth - TABLE_MARGIN;
            int baseY = screenHeight - cardHeight - TABLE_MARGIN;

            for (int i = visibleCards - 1; i >= 0; i--) {
                int x = baseX - (i * STACK_OFFSET);
                int y = baseY - (i * STACK_OFFSET);
                Card.drawBackImage(g2, x, y, cardWidth, cardHeight);
            }

            g2.setFont(smallHudFont);
            g2.setColor(Color.WHITE);
            g2.drawString("Draw pile: " + gameState.getDrawPileSize(), baseX - 18, baseY - 10);
            if (gameState.canHumanDraw()) {
                g2.drawString("Click to draw", baseX - 14, baseY + cardHeight + 18);
            }
        }
    }

    private void drawHud(Graphics2D g2) {
        g2.setFont(hudFont);
        g2.setColor(Color.WHITE);
        g2.drawString("Current turn: " + gameState.getPlayerName(gameState.getCurrentPlayerIndex()), TABLE_MARGIN, 28);

        g2.setFont(smallHudFont);
        drawWrappedStatus(g2, gameState.getStatusMessage(), TABLE_MARGIN, 54, screenWidth - (TABLE_MARGIN * 2), 18);

        int opponentsX = TABLE_MARGIN;
        int opponentsY = 120;
        for (int playerIndex = 1; playerIndex < 4; playerIndex++) {
            String label = gameState.getPlayerName(playerIndex) + ": "
                    + gameState.getPlayerCardCount(playerIndex)
                    + (gameState.isPlayerFinished(playerIndex) ? " (finished)" : " cards");
            g2.drawString(label, opponentsX, opponentsY + ((playerIndex - 1) * 20));
        }

        String humanLabel = "You: " + gameState.getPlayerCardCount(0)
                + (gameState.isPlayerFinished(0) ? " (finished)" : " cards");
        g2.drawString(humanLabel, TABLE_MARGIN, screenHeight - 16);
    }

    private void drawWrappedStatus(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        FontMetrics metrics = g2.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = y;

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && !line.isEmpty()) {
                g2.drawString(line.toString(), x, lineY);
                line = new StringBuilder(word);
                lineY += lineHeight;
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (!line.isEmpty()) {
            g2.drawString(line.toString(), x, lineY);
        }
    }

    private List<CardPlacement> getPlayerHandPlacements() {
        List<Card> playerHand = gameState.getHumanHand();
        List<CardPlacement> placements = new ArrayList<>();
        if (playerHand.isEmpty()) {
            return placements;
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
            double pivotRotationX = x + (cardWidth / 2.0);
            double pivotRotationY = y + (cardHeight * 0.92);
            Shape hitShape = AffineTransform.getRotateInstance(angleRadians, pivotRotationX, pivotRotationY)
                    .createTransformedShape(new Rectangle(x, y, cardWidth, cardHeight));

            placements.add(new CardPlacement(i, playerHand.get(i), x, y, angleRadians, pivotRotationX, pivotRotationY, hitShape));
        }

        return placements;
    }

    private Rectangle getDrawPileBounds() {
        if (gameState.hasAvailableDrawPile()) {
            int visibleCards = Math.max(1, Math.min(gameState.getDrawPileSize(), MAX_VISIBLE_STACK));

            int baseX = screenWidth - cardWidth - TABLE_MARGIN;
            int baseY = screenHeight - cardHeight - TABLE_MARGIN;
            int x = baseX - ((visibleCards - 1) * STACK_OFFSET);
            int y = baseY - ((visibleCards - 1) * STACK_OFFSET);
            int width = cardWidth + ((visibleCards - 1) * STACK_OFFSET);
            int height = cardHeight + ((visibleCards - 1) * STACK_OFFSET);
            return new Rectangle(x, y, width, height);
        }

        return new Rectangle();
    }

    private void handleMouseClick(Point point) {
        if (gameState.isRoundOver()) {
            return;
        }

        List<CardPlacement> placements = getPlayerHandPlacements();
        for (int placementIndex = placements.size() - 1; placementIndex >= 0; placementIndex--) {
            CardPlacement placement = placements.get(placementIndex);
            if (placement.hitShape().contains(point)) {
                String chosenSuit = placement.card().isSuitChange() ? requestSuitChoice() : null;
                if (placement.card().isSuitChange() && chosenSuit == null) {
                    return;
                }

                boolean cardPlayed = gameState.playHumanCard(placement.handIndex(), chosenSuit);
                if (!cardPlayed) {
                    Toolkit.getDefaultToolkit().beep();
                }
                repaint();
                return;
            }
        }

        if (getDrawPileBounds().contains(point)) {
            gameState.drawForHuman();
            repaint();
        }
    }

    private String requestSuitChoice() {
        Object selection = JOptionPane.showInputDialog(
                this,
                "Choose the next suit:",
                "Change Suit",
                JOptionPane.PLAIN_MESSAGE,
                null,
                SUIT_OPTIONS,
                gameState.getActiveSuit()
        );

        return selection instanceof String ? (String) selection : null;
    }

    private record CardPlacement(
            int handIndex,
            Card card,
            int x,
            int y,
            double angleRadians,
            double rotationPivotX,
            double rotationPivotY,
            Shape hitShape
    ) {
    }

    private class TableMouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            handleMouseClick(e.getPoint());
        }
    }
}