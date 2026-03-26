package main;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable card model + rendering helpers.
 *
 * Each card knows its logical value (type/number) and preloaded front image.
 * Images are loaded from resources and cached to avoid repeated disk/classpath reads.
 */
public class Card {
    private static final Map<String, BufferedImage> FRONT_IMAGE_CACHE = new HashMap<>(); // key: resource path -> front image
    private static final BufferedImage BACK_IMAGE = loadImage("cards/cardBack.png");

    private final String type;
    private final int number;
    private final BufferedImage frontView;

    /** Creates a card and resolves its front image from cache/resources. */
    public Card(String type, int number) {
        this.type = type;
        this.number = number;
        this.frontView = FRONT_IMAGE_CACHE.computeIfAbsent(buildFrontImagePath(type, number), Card::loadImage);
    }

    /** Returns suit/type name (Basto, Copa, Espada, Oro). */
    public String getType() {
        return type;
    }

    /** Returns numeric value of the card (1..10). */
    public int getNumber() {
        return number;
    }

    /** Rule check: card matches if same number or same active suit/type. */
    public boolean matches(Card topCard, String activeSuit) {
        return number == topCard.number || type.equals(activeSuit);
    }

    /** True when this card applies +2 effect. */
    public boolean isDrawTwo() {
        return number == 2;
    }

    /** True for regular skip card (number 1 except golden-1). */
    public boolean isSkip() {
        return number == 1 && !isGoldenOne();
    }

    /** True when this card lets player choose next suit. */
    public boolean isSuitChange() {
        return number == 7;
    }

    /** True for golden-1 special penalty card (Oro 1). */
    public boolean isGoldenOne() {
        return number == 1 && "Oro".equals(type);
    }

    /** Groups all special cards used by the game rules. */
    public boolean isSpecialCard() {
        return isDrawTwo() || isSkip() || isSuitChange() || isGoldenOne();
    }

    /** Friendly label used in HUD/status text. */
    public String getDisplayName() {
        return type + " " + number;
    }

    /** Draws card front image in target rectangle. */
    public void drawFront(Graphics2D g2, int x, int y, int width, int height) {
        g2.drawImage(frontView, x, y, width, height, null);
    }

    /** Draws the shared card back image in target rectangle. */
    public void drawBack(Graphics2D g2, int x, int y, int width, int height) {
        drawBackImage(g2, x, y, width, height);
    }

    /** Static helper used by panels/animations when only card back is visible. */
    public static void drawBackImage(Graphics2D g2, int x, int y, int width, int height) {
        g2.drawImage(BACK_IMAGE, x, y, width, height, null);
    }

    /** Maps logical card values to image resource paths. */
    private static String buildFrontImagePath(String type, int number) {
        String normalizedType = Objects.requireNonNull(type, "Card type cannot be null").trim();

        return switch (normalizedType) {
            case "Basto" -> "cards/Basto/basto" + number + ".png";
            case "Copa" -> "cards/Copa/copa" + number + ".png";
            case "Espada" -> "cards/Espada/espada" + number + ".png";
            case "Oro" -> "cards/Oro/oro" + number + ".png";
            default -> throw new IllegalArgumentException("Unknown card type: " + type);
        };
    }

    /** Loads image bytes from classpath and decodes a BufferedImage. */
    private static BufferedImage loadImage(String resourcePath) {
        try (InputStream inputStream = Card.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }

            return ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource: " + resourcePath, e);
        }
    }

}