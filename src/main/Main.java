import main.GamePanel;
import main.MainMenuPanel;
import main.UiFonts;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.Enumeration;

void main() {
    applyGlobalUiFont(UiFonts.plain(14f));

    JFrame window = new JFrame();
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.setResizable(false);
    window.setTitle("Carta");

    MainMenuPanel mainMenuPanel = new MainMenuPanel(() -> launchCpuGame(window));
    window.setContentPane(mainMenuPanel);

    window.pack();

    window.setLocationRelativeTo(null);
    window.setVisible(true);
}

void launchCpuGame(JFrame window) {
    GamePanel gamePanel = new GamePanel();
    window.setContentPane(gamePanel);
    window.revalidate();
    window.repaint();
    gamePanel.startGameThread();
}

void applyGlobalUiFont(Font font) {
    FontUIResource fontResource = new FontUIResource(font);
    Enumeration<Object> keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
        Object key = keys.nextElement();
        Object value = UIManager.get(key);
        if (value instanceof FontUIResource) {
            UIManager.put(key, fontResource);
        }
    }
}

