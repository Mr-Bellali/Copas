package main;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Card {
    private static final Map<String, BufferedImage> FRONT_IMAGE_CACHE = new HashMap<>();
    private static final BufferedImage BACK_IMAGE = loadImage("cards/cardBack.png");

    private final String type;
    private final int number;
    private final BufferedImage frontView;

    public Card(String type, int number) {
        this.type = type;
        this.number = number;
        this.frontView = FRONT_IMAGE_CACHE.computeIfAbsent(buildFrontImagePath(type, number), Card::loadImage);
    }

    public String getType() {
        return type;
    }

    public int getNumber() {
        return number;
    }

    public boolean matches(Card topCard, String activeSuit) {
        return number == topCard.number || type.equals(activeSuit);
    }

    public boolean isDrawTwo() {
        return number == 2;
    }

    public boolean isSkip() {
        return number == 1 && !isGoldenOne();
    }

    public boolean isSuitChange() {
        return number == 7;
    }

    public boolean isGoldenOne() {
        return number == 1 && "Oro".equals(type);
    }

    public boolean isSpecialCard() {
        return isDrawTwo() || isSkip() || isSuitChange() || isGoldenOne();
    }

    public String getDisplayName() {
        return type + " " + number;
    }

    public void drawFront(Graphics2D g2, int x, int y, int width, int height) {
        g2.drawImage(frontView, x, y, width, height, null);
    }

    public void drawBack(Graphics2D g2, int x, int y, int width, int height) {
        drawBackImage(g2, x, y, width, height);
    }

    public static void drawBackImage(Graphics2D g2, int x, int y, int width, int height) {
        g2.drawImage(BACK_IMAGE, x, y, width, height, null);
    }

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