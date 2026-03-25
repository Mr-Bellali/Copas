package main;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

public final class UiFonts {
    private static final String FONT_RESOURCE_PATH = "fonts/LowresPixel-Regular.otf";
    private static final Font BASE_FONT = loadBaseFont();

    private UiFonts() {
    }

    public static Font bold(float size) {
        return BASE_FONT.deriveFont(Font.BOLD, size);
    }

    public static Font plain(float size) {
        return BASE_FONT.deriveFont(Font.PLAIN, size);
    }

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

    private static Font fallbackFont() {
        return new Font("SansSerif", Font.PLAIN, 16);
    }
}

