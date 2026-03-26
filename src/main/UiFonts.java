package main;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Centralized UI font provider.
 *
 * Loads a custom font once from resources and exposes style/size helpers.
 * If loading fails, returns a safe SansSerif fallback.
 */
public final class UiFonts {
    private static final String FONT_RESOURCE_PATH = "fonts/LowresPixel-Regular.otf";
    private static final Font BASE_FONT = loadBaseFont();

    private UiFonts() {
    }

    /** Returns bold variant at requested size. */
    public static Font bold(float size) {
        return BASE_FONT.deriveFont(Font.BOLD, size);
    }

    /** Returns regular variant at requested size. */
    public static Font plain(float size) {
        return BASE_FONT.deriveFont(Font.PLAIN, size);
    }

    /** Loads custom OTF from classpath and converts it into a usable base font. */
    private static Font loadBaseFont() {
        try (InputStream inputStream = UiFonts.class.getClassLoader().getResourceAsStream(FONT_RESOURCE_PATH)) {
            if (inputStream == null) {
                return fallbackFont();
            }

            return Font.createFont(Font.TRUETYPE_FONT, inputStream);
        } catch (IOException | FontFormatException exception) {
            return fallbackFont();
        }
    }

    /** Final fallback when custom font is unavailable or invalid. */
    private static Font fallbackFont() {
        return new Font("SansSerif", Font.PLAIN, 16);
    }
}
