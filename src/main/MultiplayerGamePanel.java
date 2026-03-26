package main;

import main.LocalMultiplayerProtocol.CardPayload;
import main.LocalMultiplayerProtocol.MultiplayerGameEvent;
import main.LocalMultiplayerProtocol.MultiplayerGameSnapshot;
import main.LocalMultiplayerProtocol.MultiplayerEventType;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultiplayerGamePanel extends JPanel {
    private static final long ANIMATION_DURATION_MS = 420;
    private static final int TABLE_MARGIN = 24;
    private static final int STACK_OFFSET = 4;
    private static final int MAX_VISIBLE_STACK = 6;
    private static final double HAND_FAN_SPREAD_DEGREES = 36.0;
    private static final String[] SUIT_OPTIONS = {"Basto", "Copa", "Espada", "Oro"};

    final int originalTilesSize = 64;
    final int scale = 2;
    public int tileSize = originalTilesSize * scale;
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    final int screenWidth = (tileSize * maxScreenCol) / 2;
    final int screenHeight = (tileSize * maxScreenRow) / 2;

    private final int cardWidth = tileSize - 24;
    private final int cardHeight = (int) Math.round(cardWidth * 1.45);
    private final MultiplayerGameController controller;
    private final Timer animationTimer;
    private final List<CardAnimation> activeAnimations = new ArrayList<>();

    private final java.awt.Font hudFont = UiFonts.bold(16f);
    private volatile MultiplayerGameSnapshot snapshot;

    public MultiplayerGamePanel(MultiplayerGameController controller, MultiplayerGameSnapshot initialSnapshot) {
        this.controller = controller;
        this.snapshot = initialSnapshot;

        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(new Color(16, 94, 58));
        setDoubleBuffered(true);
        addMouseListener(new TableMouseHandler());
        addMouseMotionListener(new TableMouseMotionHandler());

        controller.setListener(new MultiplayerGameController.Listener() {
            @Override
            public void onSnapshotUpdated(MultiplayerGameSnapshot updatedSnapshot) {
                handleSnapshotUpdated(updatedSnapshot);
            }

            @Override
            public void onError(String message) {
                JOptionPane.showMessageDialog(MultiplayerGamePanel.this, message, "Local multiplayer", JOptionPane.WARNING_MESSAGE);
            }

            @Override
            public void onDisconnected(String reason) {
                animationTimer.stop();
                JOptionPane.showMessageDialog(MultiplayerGamePanel.this, reason, "Connection closed", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        animationTimer = new Timer(1000 / 60, _ -> {
            updateAnimations();
            repaint();
        });
        animationTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawCenterCard(g2);
        drawOpponentHand(g2);
        drawPlayerHand(g2);
        drawDrawPile(g2);
        drawAnimations(g2);
        drawHud(g2);
        g2.dispose();
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }

    private void handleSnapshotUpdated(MultiplayerGameSnapshot updatedSnapshot) {
        MultiplayerGameSnapshot previousSnapshot = snapshot;
        snapshot = updatedSnapshot;
        buildAnimations(previousSnapshot, updatedSnapshot);
        if (updatedSnapshot.roundOver() && (previousSnapshot == null || !previousSnapshot.roundOver())) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this,
                    updatedSnapshot.statusMessage(),
                    "Round over",
                    JOptionPane.INFORMATION_MESSAGE
            ));
        }
        repaint();
    }

    private void buildAnimations(MultiplayerGameSnapshot previousSnapshot, MultiplayerGameSnapshot updatedSnapshot) {
        activeAnimations.clear();
        if (previousSnapshot == null || updatedSnapshot.recentEvent() == null) {
            return;
        }

        MultiplayerGameEvent event = updatedSnapshot.recentEvent();
        switch (event.type()) {
            case ROUND_START -> {
            }
            case PLAY_CARD -> buildPlayAnimation(previousSnapshot, event);
            case DRAW_CARD -> buildDrawAnimation(event);
        }
    }

    private void buildPlayAnimation(MultiplayerGameSnapshot previousSnapshot, MultiplayerGameEvent event) {
        double fromX;
        double fromY;
        double fromAngle;

        if (event.actorIsLocal() && event.card() != null) {
            CardPlacement placement = findPlacement(previousSnapshot.localHand(), event.card());
            if (placement != null) {
                fromX = placement.x();
                fromY = placement.y();
                fromAngle = placement.angleRadians();
            } else {
                fromX = localHandAnchorX();
                fromY = localHandAnchorY();
                fromAngle = 0.0;
            }
        } else {
            fromX = opponentHandAnchorX();
            fromY = opponentHandAnchorY();
            fromAngle = 0.0;
        }

        activeAnimations.add(new CardAnimation(event.card(), true,
                fromX, fromY, centerCardX(), centerCardY(), fromAngle, 0.0));
        activeAnimations.getLast().start();

        if (event.affectedCardCount() > 0) {
            activeAnimations.add(new CardAnimation(null, false,
                    drawPileX(), drawPileY(),
                    event.targetIsLocal() ? localHandAnchorX() : opponentHandAnchorX(),
                    event.targetIsLocal() ? localHandAnchorY() : opponentHandAnchorY(),
                    0.0, 0.0));
            activeAnimations.getLast().start();
        }
    }

    private void buildDrawAnimation(MultiplayerGameEvent event) {
        boolean faceUp = event.actorIsLocal() && event.card() != null;
        activeAnimations.add(new CardAnimation(event.card(), faceUp,
                drawPileX(), drawPileY(),
                event.actorIsLocal() ? localHandAnchorX() : opponentHandAnchorX(),
                event.actorIsLocal() ? localHandAnchorY() : opponentHandAnchorY(),
                0.0, 0.0));
        activeAnimations.getLast().start();
    }

    private void updateAnimations() {
        Iterator<CardAnimation> iterator = activeAnimations.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isDone()) {
                iterator.remove();
            }
        }
    }

    private void drawCenterCard(Graphics2D g2) {
        if (snapshot == null || snapshot.topCard() == null) {
            return;
        }
        snapshot.topCard().toCard().drawFront(g2, centerCardX(), centerCardY(), cardWidth, cardHeight);
    }

    private void drawPlayerHand(Graphics2D g2) {
        if (snapshot == null) {
            return;
        }
        for (CardPlacement placement : getPlayerHandPlacements(snapshot.localHand())) {
            Graphics2D cg = (Graphics2D) g2.create();
            cg.rotate(placement.angleRadians(), placement.rotationPivotX(), placement.rotationPivotY());
            placement.card().toCard().drawFront(cg, placement.x(), placement.y(), cardWidth, cardHeight);
            if (snapshot.localTurn() && snapshot.playableCardIndexes().contains(placement.handIndex())) {
                cg.setColor(new Color(255, 255, 255, 170));
                cg.setStroke(new BasicStroke(3f));
                cg.drawRoundRect(placement.x(), placement.y(), cardWidth, cardHeight, 16, 16);
            }
            cg.dispose();
        }
    }

    private void drawOpponentHand(Graphics2D g2) {
        if (snapshot == null || snapshot.opponentCardCount() <= 0) {
            return;
        }

        int visibleCards = Math.min(snapshot.opponentCardCount(), 6);
        int opponentCardWidth = cardWidth - 8;
        int opponentCardHeight = (int) Math.round(opponentCardWidth * 1.45);
        double spread = visibleCards == 1 ? 0.0 : 22.0;
        double step = visibleCards == 1 ? 0.0 : spread / (visibleCards - 1);

        for (int index = 0; index < visibleCards; index++) {
            double angle = Math.toRadians(-spread / 2.0 + step * index);
            int x = clamp((int) Math.round(screenWidth / 2.0 + Math.sin(angle) * 36 - opponentCardWidth / 2.0),
                    TABLE_MARGIN, screenWidth - TABLE_MARGIN - opponentCardWidth);
            int y = (int) Math.round(Math.max(TABLE_MARGIN + 24,
                    TABLE_MARGIN + 20 - Math.cos(angle) * 22));
            Graphics2D cg = (Graphics2D) g2.create();
            cg.rotate(angle, x + opponentCardWidth / 2.0, y + opponentCardHeight * 0.52);
            Card.drawBackImage(cg, x, y, opponentCardWidth, opponentCardHeight);
            cg.dispose();
        }
    }

    private void drawDrawPile(Graphics2D g2) {
        if (snapshot == null || snapshot.drawPileSize() <= 0) {
            return;
        }
        int visibleCards = Math.max(1, Math.min(snapshot.drawPileSize(), MAX_VISIBLE_STACK));
        for (int index = visibleCards - 1; index >= 0; index--) {
            Card.drawBackImage(g2,
                    drawPileX() - index * STACK_OFFSET,
                    drawPileY() - index * STACK_OFFSET,
                    cardWidth,
                    cardHeight);
        }
    }

    private void drawAnimations(Graphics2D g2) {
        for (CardAnimation animation : activeAnimations) {
            Graphics2D cg = (Graphics2D) g2.create();
            cg.rotate(animation.currentAngle(), animation.currentX() + cardWidth / 2.0, animation.currentY() + cardHeight / 2.0);
            animation.draw(cg, (int) animation.currentX(), (int) animation.currentY(), cardWidth, cardHeight);
            cg.dispose();
        }
    }

    private void drawHud(Graphics2D g2) {
        if (snapshot == null) {
            return;
        }
        g2.setFont(hudFont);
        g2.setColor(Color.WHITE);
        g2.drawString("Current turn: " + snapshot.currentTurnName(), TABLE_MARGIN, 28);
    }

    private List<CardPlacement> getPlayerHandPlacements(List<CardPayload> hand) {
        List<CardPlacement> placements = new ArrayList<>();
        if (hand == null || hand.isEmpty()) {
            return placements;
        }
        double fanRadius = cardHeight + 95.0;
        double pivotX = screenWidth / 2.0;
        double pivotY = screenHeight + 220.0;
        double totalSpread = hand.size() == 1 ? 0 : HAND_FAN_SPREAD_DEGREES;
        double angleStep = hand.size() == 1 ? 0 : totalSpread / (hand.size() - 1);
        double startAngle = -totalSpread / 2.0;
        for (int index = 0; index < hand.size(); index++) {
            double angleRadians = Math.toRadians(startAngle + angleStep * index);
            int x = (int) Math.round(pivotX + Math.sin(angleRadians) * fanRadius - cardWidth / 2.0);
            int y = (int) Math.round(pivotY - Math.cos(angleRadians) * fanRadius - cardHeight);
            double pivotRotationX = x + cardWidth / 2.0;
            double pivotRotationY = y + cardHeight * 0.92;
            Shape hitShape = AffineTransform.getRotateInstance(angleRadians, pivotRotationX, pivotRotationY)
                    .createTransformedShape(new Rectangle(x, y, cardWidth, cardHeight));
            placements.add(new CardPlacement(index, hand.get(index), x, y, angleRadians, pivotRotationX, pivotRotationY, hitShape));
        }
        return placements;
    }

    private CardPlacement findPlacement(List<CardPayload> hand, CardPayload card) {
        if (card == null) {
            return null;
        }
        for (CardPlacement placement : getPlayerHandPlacements(hand)) {
            if (placement.card().equals(card)) {
                return placement;
            }
        }
        return null;
    }

    private Rectangle getDrawPileBounds() {
        if (snapshot == null || snapshot.drawPileSize() <= 0) {
            return new Rectangle();
        }
        int visibleCards = Math.max(1, Math.min(snapshot.drawPileSize(), MAX_VISIBLE_STACK));
        return new Rectangle(
                drawPileX() - (visibleCards - 1) * STACK_OFFSET,
                drawPileY() - (visibleCards - 1) * STACK_OFFSET,
                cardWidth + (visibleCards - 1) * STACK_OFFSET,
                cardHeight + (visibleCards - 1) * STACK_OFFSET
        );
    }

    private void handleMouseClick(Point point) {
        if (snapshot == null || snapshot.roundOver() || !activeAnimations.isEmpty()) {
            return;
        }
        if (!snapshot.localTurn()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        List<CardPlacement> placements = getPlayerHandPlacements(snapshot.localHand());
        for (int index = placements.size() - 1; index >= 0; index--) {
            CardPlacement placement = placements.get(index);
            if (!placement.hitShape().contains(point)) {
                continue;
            }
            if (!snapshot.playableCardIndexes().contains(placement.handIndex())) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            String chosenSuit = placement.card().number() == 7 ? requestSuitChoice() : null;
            if (placement.card().number() == 7 && chosenSuit == null) {
                return;
            }
            controller.playCard(placement.handIndex(), chosenSuit);
            return;
        }

        if (getDrawPileBounds().contains(point) && snapshot.canDraw()) {
            controller.drawCard();
        }
    }

    private void updateCursor(Point point) {
        if (snapshot == null || snapshot.roundOver() || !snapshot.localTurn() || !activeAnimations.isEmpty()) {
            setCursor(java.awt.Cursor.getDefaultCursor());
            return;
        }

        boolean onPlayableCard = false;
        List<CardPlacement> placements = getPlayerHandPlacements(snapshot.localHand());
        for (CardPlacement placement : placements) {
            if (!placement.hitShape().contains(point)) {
                continue;
            }
            onPlayableCard = snapshot.playableCardIndexes().contains(placement.handIndex());
            break;
        }

        boolean onDrawPile = getDrawPileBounds().contains(point) && snapshot.canDraw();
        setCursor((onPlayableCard || onDrawPile)
                ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                : java.awt.Cursor.getDefaultCursor());
    }

    private String requestSuitChoice() {
        Object selection = JOptionPane.showInputDialog(
                this,
                "Choose the next suit:",
                "Change Suit",
                JOptionPane.PLAIN_MESSAGE,
                null,
                SUIT_OPTIONS,
                snapshot.activeSuit()
        );
        return selection instanceof String suit ? suit : null;
    }

    private int centerCardX() {
        return (screenWidth - cardWidth) / 2;
    }

    private int centerCardY() {
        return (screenHeight - cardHeight) / 2 - 36;
    }

    private int drawPileX() {
        return screenWidth - cardWidth - TABLE_MARGIN;
    }

    private int drawPileY() {
        return screenHeight - cardHeight - TABLE_MARGIN;
    }

    private int localHandAnchorX() {
        return screenWidth / 2 - cardWidth / 2;
    }

    private int localHandAnchorY() {
        return screenHeight - cardHeight - TABLE_MARGIN;
    }

    private int opponentHandAnchorX() {
        return screenWidth / 2 - cardWidth / 2;
    }

    private int opponentHandAnchorY() {
        return TABLE_MARGIN + 16;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class CardAnimation {
        private final CardPayload card;
        private final boolean faceUp;
        private final double startX;
        private final double startY;
        private final double endX;
        private final double endY;
        private final double startAngle;
        private final double endAngle;
        private long startTimeMs = -1;

        CardAnimation(CardPayload card, boolean faceUp, double startX, double startY,
                      double endX, double endY, double startAngle, double endAngle) {
            this.card = card;
            this.faceUp = faceUp;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }

        void start() {
            startTimeMs = System.currentTimeMillis();
        }

        boolean isDone() {
            return startTimeMs >= 0 && progress() >= 1.0;
        }

        double currentX() {
            return lerp(startX, endX, ease());
        }

        double currentY() {
            return lerp(startY, endY, ease());
        }

        double currentAngle() {
            return lerp(startAngle, endAngle, ease());
        }

        void draw(Graphics2D g2, int x, int y, int width, int height) {
            if (faceUp && card != null) {
                card.toCard().drawFront(g2, x, y, width, height);
            } else {
                Card.drawBackImage(g2, x, y, width, height);
            }
        }

        private double progress() {
            if (startTimeMs < 0) {
                return 0.0;
            }
            return Math.min(1.0, (double) (System.currentTimeMillis() - startTimeMs) / ANIMATION_DURATION_MS);
        }

        private double ease() {
            double t = progress();
            return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2.0;
        }

        private static double lerp(double start, double end, double amount) {
            return start + (end - start) * amount;
        }
    }

    private record CardPlacement(int handIndex, CardPayload card, int x, int y,
                                 double angleRadians, double rotationPivotX,
                                 double rotationPivotY, Shape hitShape) {
    }

    private class TableMouseHandler extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            handleMouseClick(e.getPoint());
        }
    }

    private class TableMouseMotionHandler extends MouseMotionAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            updateCursor(e.getPoint());
        }
    }
}

