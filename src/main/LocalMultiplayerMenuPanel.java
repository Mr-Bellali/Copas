package main;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
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

public class LocalMultiplayerMenuPanel extends JPanel {
    public interface Listener {
        void onHostRequested(String playerName, String portText);

        void onJoinRequested(String playerName, String hostAddress, String portText);

        void onBackRequested();
    }

    private static final Color BUTTON_BACKGROUND = new Color(244, 222, 133);
    private static final Color BUTTON_HOVER_BACKGROUND = new Color(252, 233, 154);
    private static final Color BUTTON_TEXT_COLOR = new Color(33, 33, 33);
    private static final Color BUTTON_BORDER_COLOR = new Color(120, 88, 27);
    private static final Color BUTTON_HOVER_BORDER_COLOR = new Color(255, 248, 214);
    private static final Color PANEL_BACKGROUND = new Color(16, 94, 58);
    private static final Color FIELD_BACKGROUND = new Color(247, 244, 222);
    private static final int ORIGINAL_TILE_SIZE = 64;
    private static final int SCALE = 2;
    private static final int TILE_SIZE = ORIGINAL_TILE_SIZE * SCALE;
    private static final int SCREEN_WIDTH = (TILE_SIZE * 16) / 2;
    private static final int SCREEN_HEIGHT = (TILE_SIZE * 12) / 2;

    public LocalMultiplayerMenuPanel(Listener listener) {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(PANEL_BACKGROUND);
        setLayout(new GridBagLayout());

        JLabel titleLabel = new JLabel("Local Multiplayer", SwingConstants.CENTER);
        titleLabel.setFont(UiFonts.bold(28f));
        titleLabel.setForeground(Color.WHITE);

        JLabel playerNameLabel = new JLabel("Your name");
        playerNameLabel.setFont(UiFonts.plain(16f));
        playerNameLabel.setForeground(Color.WHITE);

        JTextField playerNameField = buildTextField("Player 1");

        JLabel hostAddressLabel = new JLabel("Host address");
        hostAddressLabel.setFont(UiFonts.plain(16f));
        hostAddressLabel.setForeground(Color.WHITE);

        JTextField hostAddressField = buildTextField("127.0.0.1");

        JLabel portLabel = new JLabel("Port");
        portLabel.setFont(UiFonts.plain(16f));
        portLabel.setForeground(Color.WHITE);

        JTextField portField = buildTextField(String.valueOf(LocalMultiplayerHost.DEFAULT_PORT));

        JButton hostButton = buildMenuButton("Host game");
        hostButton.addActionListener(_ -> listener.onHostRequested(playerNameField.getText(), portField.getText()));

        JButton joinButton = buildMenuButton("Join game");
        joinButton.addActionListener(_ -> listener.onJoinRequested(playerNameField.getText(), hostAddressField.getText(), portField.getText()));

        JButton backButton = buildMenuButton("Back");
        backButton.addActionListener(_ -> listener.onBackRequested());

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 44, 24, 44));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        gbc.gridy = 0;
        gbc.insets = new Insets(8, 0, 24, 0);
        content.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(8, 0, 4, 0);
        content.add(playerNameLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(4, 0, 12, 0);
        content.add(playerNameField, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(8, 0, 4, 0);
        content.add(hostAddressLabel, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(4, 0, 22, 0);
        content.add(hostAddressField, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(8, 0, 4, 0);
        content.add(portLabel, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(4, 0, 14, 0);
        content.add(portField, gbc);

        gbc.gridy = 7;
        gbc.insets = new Insets(8, 0, 8, 0);
        content.add(hostButton, gbc);

        gbc.gridy = 8;
        content.add(joinButton, gbc);

        gbc.gridy = 9;
        gbc.insets = new Insets(18, 0, 0, 0);
        content.add(backButton, gbc);

        add(content);
    }

    private JTextField buildTextField(String defaultValue) {
        JTextField field = new JTextField(defaultValue);
        field.setFont(UiFonts.plain(16f));
        field.setBackground(FIELD_BACKGROUND);
        field.setForeground(new Color(33, 33, 33));
        field.setCaretColor(new Color(33, 33, 33));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_BORDER_COLOR, 2, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        return field;
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

