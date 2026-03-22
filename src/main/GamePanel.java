package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements Runnable {

    // screen settings
    final int originalTilesSize = 64;
    final int scale             = 2;
    public int tileSize         = originalTilesSize * scale;
    final int maxScreenCol      = 16;
    final int maxScreenRow      = 12;
    final int screenWidth       = (tileSize * maxScreenCol) / 2;
    final int screenHeight      = (tileSize * maxScreenRow) / 2;

    int FPS = 60;
    Thread gameThread;

    private static final long ANIMATION_DURATION_MS = 450;
    private static final int  AI_THINK_DELAY_MS     = 1600;

    private static final int    TABLE_MARGIN            = 24;
    private static final int    STACK_OFFSET            = 4;
    private static final int    MAX_VISIBLE_STACK       = 6;
    private static final double HAND_FAN_SPREAD_DEGREES = 36.0;
    private static final String[] SUIT_OPTIONS = {"Basto", "Copa", "Espada", "Oro"};

    private final int  cardWidth  = tileSize - 24;
    private final int  cardHeight = (int) Math.round(cardWidth * 1.45);
    private final Font hudFont      = new Font("SansSerif", Font.BOLD,  16);
    private final Font smallHudFont = new Font("SansSerif", Font.PLAIN, 14);

    private final CopasGameState gameState;
    private volatile CardAnimation activeAnimation;
    private volatile boolean       aiTurnPending;
    private          Timer         aiTurnTimer;

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

    @Override
    public void run() {
        double drawInterval = 1_000_000_000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - lastTime) / drawInterval;
            lastTime = now;
            if (delta >= 1) { update(); repaint(); delta--; }
        }
    }

    public void update() {
        if (activeAnimation != null && activeAnimation.isDone()) {
            activeAnimation = null;
            if (gameState.isAiTurn() && !aiTurnPending)
                SwingUtilities.invokeLater(this::scheduleAiTurn);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        drawCenterCard(g2);
        drawPlayerHand(g2);
        drawDrawPile(g2);
        drawOpponentHandTop(g2);
        drawOpponentHandLeft(g2);
        drawOpponentHandRight(g2);
        if (activeAnimation != null) drawCardAnimation(g2);
        drawHud(g2);
        g2.dispose();
    }

    private void drawCenterCard(Graphics2D g2) {
        int x = (screenWidth  - cardWidth)  / 2;
        int y = (screenHeight - cardHeight) / 2 - 36;
        gameState.getTopCard().drawFront(g2, x, y, cardWidth, cardHeight);
    }

    private void drawPlayerHand(Graphics2D g2) {
        for (CardPlacement p : getPlayerHandPlacements()) {
            Graphics2D cg = (Graphics2D) g2.create();
            cg.rotate(p.angleRadians(), p.rotationPivotX(), p.rotationPivotY());
            p.card().drawFront(cg, p.x(), p.y(), cardWidth, cardHeight);
            if (gameState.isHumanTurn() && gameState.isHumanCardPlayable(p.handIndex())) {
                cg.setColor(new Color(255, 255, 255, 170));
                cg.setStroke(new BasicStroke(3f));
                cg.drawRoundRect(p.x(), p.y(), cardWidth, cardHeight, 16, 16);
            }
            cg.dispose();
        }
    }

    private void drawDrawPile(Graphics2D g2) {
        if (!gameState.hasAvailableDrawPile()) return;
        int vis   = Math.max(1, Math.min(gameState.getDrawPileSize(), MAX_VISIBLE_STACK));
        int baseX = screenWidth  - cardWidth  - TABLE_MARGIN;
        int baseY = screenHeight - cardHeight - TABLE_MARGIN;
        for (int i = vis - 1; i >= 0; i--)
            Card.drawBackImage(g2, baseX - i * STACK_OFFSET, baseY - i * STACK_OFFSET, cardWidth, cardHeight);
    }

    private void drawOpponentHandTop(Graphics2D g2) {
        drawOpponentHand(g2, 1, screenWidth / 2, TABLE_MARGIN + 16, true, false);
    }
    private void drawOpponentHandLeft(Graphics2D g2) {
        drawOpponentHand(g2, 2, TABLE_MARGIN + 16, screenHeight / 2, false, true);
    }
    private void drawOpponentHandRight(Graphics2D g2) {
        drawOpponentHand(g2, 3, screenWidth - TABLE_MARGIN - 16, screenHeight / 2, false, false);
    }

    private void drawOpponentHand(Graphics2D g2, int playerIndex, int cx, int cy,
                                   boolean horizontal, boolean isLeft) {
        int count = gameState.getPlayerCardCount(playerIndex);
        if (count <= 0) return;
        int vis = Math.min(count, 4);
        int cw  = cardWidth - 8;
        int ch  = (int) Math.round(cw * 1.45);
        if (horizontal)  drawFanH (g2, cx, cy, vis, cw, ch, count);
        else if (isLeft) drawFanVL(g2, cx, cy, vis, cw, ch, count);
        else             drawFanVR(g2, cx, cy, vis, cw, ch, count);
    }

    private void drawFanH(Graphics2D g2, int cx, int cy, int vis, int cw, int ch, int total) {
        double spread = vis == 1 ? 0 : 20.0, step = vis == 1 ? 0 : spread / (vis - 1);
        for (int i = 0; i < vis; i++) {
            double a = Math.toRadians(-spread / 2.0 + step * i);
            int x = clamp((int) Math.round(cx + Math.sin(a) * 30 - cw / 2.0), TABLE_MARGIN, screenWidth  - TABLE_MARGIN - cw);
            int y = (int) Math.round(Math.max(TABLE_MARGIN + 32, cy - Math.cos(a) * 30 - ch / 2.0));
            Graphics2D cg = (Graphics2D) g2.create();
            cg.rotate(a, x + cw / 2.0, y + ch * 0.5);
            Card.drawBackImage(cg, x, y, cw, ch);
            cg.dispose();
        }
        if (vis < total) { g2.setFont(smallHudFont); g2.setColor(Color.WHITE); g2.drawString("+" + (total - vis), cx - 12, cy + 40); }
    }

    private void drawFanVL(Graphics2D g2, int cx, int cy, int vis, int cw, int ch, int total) {
        double spread = vis == 1 ? 0 : 20.0, step = vis == 1 ? 0 : spread / (vis - 1);
        for (int i = 0; i < vis; i++) {
            double a = Math.toRadians(180.0 - spread / 2.0 + step * i);
            int x = (int) Math.round(Math.max(TABLE_MARGIN, cx + Math.cos(a) * 30 - cw / 2.0));
            int y = clamp((int) Math.round(cy + Math.sin(a) * 30 - ch / 2.0), TABLE_MARGIN, screenHeight - TABLE_MARGIN - ch);
            Graphics2D cg = (Graphics2D) g2.create();
            cg.rotate(a, x + cw / 2.0, y + ch / 2.0);
            Card.drawBackImage(cg, x, y, cw, ch);
            cg.dispose();
        }
        if (vis < total) { g2.setFont(smallHudFont); g2.setColor(Color.WHITE); g2.drawString("+" + (total - vis), cx - 50, cy + 24); }
    }

    private void drawFanVR(Graphics2D g2, int cx, int cy, int vis, int cw, int ch, int total) {
        double spread = vis == 1 ? 0 : 20.0, step = vis == 1 ? 0 : spread / (vis - 1);
        for (int i = 0; i < vis; i++) {
            double a = Math.toRadians(-spread / 2.0 + step * i);
            int x = clamp((int) Math.round(cx + Math.cos(a) * 30 - cw / 2.0), TABLE_MARGIN, screenWidth  - TABLE_MARGIN - cw);
            int y = clamp((int) Math.round(cy + Math.sin(a) * 30 - ch / 2.0), TABLE_MARGIN, screenHeight - TABLE_MARGIN - ch);
            Graphics2D cg = (Graphics2D) g2.create();
            cg.rotate(a, x + cw / 2.0, y + ch / 2.0);
            Card.drawBackImage(cg, x, y, cw, ch);
            cg.dispose();
        }
        if (vis < total) { g2.setFont(smallHudFont); g2.setColor(Color.WHITE); g2.drawString("+" + (total - vis), cx + 32, cy + 24); }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private void drawHud(Graphics2D g2) {
        g2.setFont(hudFont);
        g2.setColor(Color.WHITE);
        g2.drawString("Current turn: " + gameState.getPlayerName(gameState.getCurrentPlayerIndex()),
                TABLE_MARGIN, 28);
    }

    private void drawCardAnimation(Graphics2D g2) {
        double x = activeAnimation.currentX(), y = activeAnimation.currentY();
        double a = activeAnimation.currentAngle();
        Graphics2D cg = (Graphics2D) g2.create();
        cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cg.rotate(a, x + cardWidth / 2.0, y + cardHeight / 2.0);
        activeAnimation.draw(cg, (int) x, (int) y, cardWidth, cardHeight);
        cg.dispose();
    }

    private void scheduleAiTurn() {
        if (aiTurnPending) return;
        aiTurnPending = true;
        if (aiTurnTimer != null) aiTurnTimer.stop();
        aiTurnTimer = new Timer(AI_THINK_DELAY_MS, e -> {
            ((Timer) e.getSource()).stop();
            aiTurnPending = false;
            executeAiTurn();
        });
        aiTurnTimer.setRepeats(false);
        aiTurnTimer.start();
    }

    private void executeAiTurn() {
        if (!gameState.isAiTurn()) { repaint(); return; }
        CopasGameState.AiTurnResult result = gameState.stepAiTurn();
        if (result != null && result.cardPlayed() != null) {
            double[] from = playerHandCenter(result.playerIndex());
            int cx = (screenWidth  - cardWidth)  / 2;
            int cy = (screenHeight - cardHeight) / 2 - 36;
            activeAnimation = new CardAnimation(result.cardPlayed(), false,
                    from[0], from[1], cx, cy, 0.0, 0.0);
            activeAnimation.start();
        } else {
            if (gameState.isAiTurn() && !aiTurnPending) scheduleAiTurn();
        }
        repaint();
    }

    private double[] playerHandCenter(int playerIndex) {
        return switch (playerIndex) {
            case 1  -> new double[]{ screenWidth / 2.0 - cardWidth / 2.0, TABLE_MARGIN + 16 };
            case 2  -> new double[]{ TABLE_MARGIN + 16, screenHeight / 2.0 - cardHeight / 2.0 };
            case 3  -> new double[]{ screenWidth - TABLE_MARGIN - cardWidth - 16, screenHeight / 2.0 - cardHeight / 2.0 };
            default -> new double[]{ screenWidth / 2.0 - cardWidth / 2.0, screenHeight - cardHeight - TABLE_MARGIN };
        };
    }

    private List<CardPlacement> getPlayerHandPlacements() {
        List<Card> hand = gameState.getHumanHand();
        List<CardPlacement> list = new ArrayList<>();
        if (hand.isEmpty()) return list;
        double fanRadius   = cardHeight + 95.0;
        double pivotX      = screenWidth  / 2.0;
        double pivotY      = screenHeight + 220.0;
        double totalSpread = hand.size() == 1 ? 0 : HAND_FAN_SPREAD_DEGREES;
        double angleStep   = hand.size() == 1 ? 0 : totalSpread / (hand.size() - 1);
        double startAngle  = -totalSpread / 2.0;
        for (int i = 0; i < hand.size(); i++) {
            double ar  = Math.toRadians(startAngle + angleStep * i);
            int x = (int) Math.round(pivotX + Math.sin(ar) * fanRadius - cardWidth  / 2.0);
            int y = (int) Math.round(pivotY - Math.cos(ar) * fanRadius - cardHeight);
            double prx = x + cardWidth  / 2.0;
            double pry = y + cardHeight * 0.92;
            Shape hit  = AffineTransform.getRotateInstance(ar, prx, pry)
                    .createTransformedShape(new Rectangle(x, y, cardWidth, cardHeight));
            list.add(new CardPlacement(i, hand.get(i), x, y, ar, prx, pry, hit));
        }
        return list;
    }

    private Rectangle getDrawPileBounds() {
        if (!gameState.hasAvailableDrawPile()) return new Rectangle();
        int vis   = Math.max(1, Math.min(gameState.getDrawPileSize(), MAX_VISIBLE_STACK));
        int baseX = screenWidth  - cardWidth  - TABLE_MARGIN;
        int baseY = screenHeight - cardHeight - TABLE_MARGIN;
        return new Rectangle(baseX - (vis-1)*STACK_OFFSET, baseY - (vis-1)*STACK_OFFSET,
                cardWidth + (vis-1)*STACK_OFFSET, cardHeight + (vis-1)*STACK_OFFSET);
    }

    private void handleMouseClick(Point point) {
        if (gameState.isRoundOver() || activeAnimation != null || aiTurnPending) return;
        if (!gameState.isHumanTurn()) return;
        List<CardPlacement> ps = getPlayerHandPlacements();
        for (int i = ps.size() - 1; i >= 0; i--) {
            CardPlacement p = ps.get(i);
            if (!p.hitShape().contains(point)) continue;
            String suit = p.card().isSuitChange() ? requestSuitChoice() : null;
            if (p.card().isSuitChange() && suit == null) return;
            boolean played = gameState.playHumanCard(p.handIndex(), suit);
            if (played) {
                int dx = (screenWidth  - cardWidth)  / 2;
                int dy = (screenHeight - cardHeight) / 2 - 36;
                activeAnimation = new CardAnimation(p.card(), true,
                        p.x(), p.y(), dx, dy, p.angleRadians(), 0.0);
                activeAnimation.start();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
            repaint();
            return;
        }
        if (getDrawPileBounds().contains(point)) {
            gameState.drawForHuman();
            if (gameState.isAiTurn() && !aiTurnPending) scheduleAiTurn();
            repaint();
        }
    }

    private String requestSuitChoice() {
        Object sel = JOptionPane.showInputDialog(this, "Choose the next suit:", "Change Suit",
                JOptionPane.PLAIN_MESSAGE, null, SUIT_OPTIONS, gameState.getActiveSuit());
        return sel instanceof String s ? s : null;
    }

    private static class CardAnimation {
        private final Card card; private final boolean faceUp;
        private final double startX, startY, endX, endY, startAngle, endAngle;
        private long startTimeMs = -1;

        CardAnimation(Card card, boolean faceUp,
                      double sx, double sy, double ex, double ey, double sa, double ea) {
            this.card = card; this.faceUp = faceUp;
            startX = sx; startY = sy; endX = ex; endY = ey;
            startAngle = sa; endAngle = ea;
        }

        void    start()        { startTimeMs = System.currentTimeMillis(); }
        Card    card()         { return card; }
        boolean isDone()       { return startTimeMs >= 0 && progress() >= 1.0; }
        double  currentX()     { return lerp(startX, endX, ease()); }
        double  currentY()     { return lerp(startY, endY, ease()); }
        double  currentAngle() { return lerp(startAngle, endAngle, ease()); }

        void draw(Graphics2D g2, int x, int y, int w, int h) {
            if (faceUp) card.drawFront(g2, x, y, w, h);
            else        Card.drawBackImage(g2, x, y, w, h);
        }

        private double progress() {
            if (startTimeMs < 0) return 0;
            return Math.min(1.0, (double)(System.currentTimeMillis() - startTimeMs) / ANIMATION_DURATION_MS);
        }
        private double ease() {
            double t = progress();
            return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2.0;
        }
        private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    }

    private record CardPlacement(int handIndex, Card card, int x, int y,
                                  double angleRadians, double rotationPivotX,
                                  double rotationPivotY, Shape hitShape) {}

    private class TableMouseHandler extends MouseAdapter {
        @Override public void mouseClicked(MouseEvent e) { handleMouseClick(e.getPoint()); }
    }
}
