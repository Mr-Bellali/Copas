package main;

import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainMenuPanel extends JPanel {
    private static final Color BUTTON_BACKGROUND = new Color(244, 222, 133);
    private static final Color BUTTON_HOVER_BACKGROUND = new Color(252, 233, 154);
    private static final Color BUTTON_TEXT_COLOR = new Color(33, 33, 33);
    private static final Color BUTTON_BORDER_COLOR = new Color(120, 88, 27);
    private static final Color BUTTON_HOVER_BORDER_COLOR = new Color(255, 248, 214);
    private static final int ORIGINAL_TILE_SIZE = 64;
    private static final int SCALE = 2;
    private static final int TILE_SIZE = ORIGINAL_TILE_SIZE * SCALE;
    private static final int SCREEN_WIDTH = (TILE_SIZE * 16) / 2;
    private static final int SCREEN_HEIGHT = (TILE_SIZE * 12) / 2;

    public MainMenuPanel(Runnable onPlayAgainstCpu) {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(new Color(16, 94, 58));
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 0, 12, 0);

        JLabel titleLabel = new JLabel("Copas", SwingConstants.CENTER);
        titleLabel.setFont(UiFonts.bold(40f));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Choose a mode", SwingConstants.CENTER);
        subtitleLabel.setFont(UiFonts.plain(20f));
        subtitleLabel.setForeground(new Color(230, 230, 230));

        JButton cpuButton = buildMenuButton("Play against CPU");
        cpuButton.addActionListener(_ -> onPlayAgainstCpu.run());

        JButton localButton = buildMenuButton("Other players (local)");
        localButton.addActionListener(_ -> JOptionPane.showMessageDialog(
                this,
                "Local multiplayer is coming later.",
                "Coming soon",
                JOptionPane.INFORMATION_MESSAGE
        ));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 48, 24, 48));

        GridBagConstraints inner = new GridBagConstraints();
        inner.gridx = 0;
        inner.weightx = 1.0;
        inner.fill = GridBagConstraints.HORIZONTAL;
        inner.insets = new Insets(8, 0, 8, 0);

        inner.gridy = 0;
        content.add(titleLabel, inner);
        inner.gridy = 1;
        content.add(subtitleLabel, inner);
        inner.gridy = 2;
        inner.insets = new Insets(28, 0, 8, 0);
        content.add(cpuButton, inner);
        inner.gridy = 3;
        inner.insets = new Insets(8, 0, 8, 0);
        content.add(localButton, inner);

        add(content, gbc);
    }

    private JButton buildMenuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UiFonts.bold(18f));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBackground(BUTTON_BACKGROUND);
        button.setForeground(BUTTON_TEXT_COLOR);
        Border defaultBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_BORDER_COLOR, 3, true),
                new EmptyBorder(14, 22, 14, 22)
        );
        Border hoverBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_HOVER_BORDER_COLOR, 4, true),
                new EmptyBorder(13, 21, 13, 21)
        );
        button.setBorder(defaultBorder);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_HOVER_BACKGROUND);
                button.setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_BACKGROUND);
                button.setBorder(defaultBorder);
            }
        });
        return button;
    }
}


