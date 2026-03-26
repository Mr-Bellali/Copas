package main;

import main.LocalMultiplayerProtocol.LobbyPlayerInfo;
import main.LocalMultiplayerProtocol.LobbyStateMessage;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

/**
 * Waiting-room screen shown before multiplayer game start.
 */
public class MultiplayerLobbyPanel extends JPanel {
    /** Host/client callbacks for start and back actions. */
    public interface Listener {
        void onStartRequested();

        void onBackRequested();
    }

    private static final Color BUTTON_BACKGROUND = new Color(244, 222, 133);
    private static final Color BUTTON_HOVER_BACKGROUND = new Color(252, 233, 154);
    private static final Color BUTTON_TEXT_COLOR = new Color(33, 33, 33);
    private static final Color BUTTON_BORDER_COLOR = new Color(120, 88, 27);
    private static final Color BUTTON_HOVER_BORDER_COLOR = new Color(255, 248, 214);
    private static final Color PANEL_BACKGROUND = new Color(16, 94, 58);
    private static final Color LIST_BACKGROUND = new Color(247, 244, 222);
    private static final int ORIGINAL_TILE_SIZE = 64;
    private static final int SCALE = 2;
    private static final int TILE_SIZE = ORIGINAL_TILE_SIZE * SCALE;
    private static final int SCREEN_WIDTH = (TILE_SIZE * 16) / 2;
    private static final int SCREEN_HEIGHT = (TILE_SIZE * 12) / 2;

    private final DefaultListModel<String> playersModel = new DefaultListModel<>(); // rendered rows in lobby player list
    private final JLabel subtitleLabel = new JLabel("Waiting for players...", SwingConstants.CENTER);
    private final JLabel connectionLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton startButton;

    /** Creates a lobby panel for either host controls or read-only joiner view. */
    public MultiplayerLobbyPanel(String title, boolean hostControls, Listener listener) {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(PANEL_BACKGROUND);
        setLayout(new GridBagLayout());

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(UiFonts.bold(28f));
        titleLabel.setForeground(Color.WHITE);

        subtitleLabel.setFont(UiFonts.plain(16f));
        subtitleLabel.setForeground(new Color(236, 236, 236));

        connectionLabel.setFont(UiFonts.plain(15f));
        connectionLabel.setForeground(new Color(232, 225, 171));

        JList<String> playersList = new JList<>(playersModel);
        playersList.setFont(UiFonts.plain(16f));
        playersList.setBackground(LIST_BACKGROUND);
        playersList.setForeground(new Color(33, 33, 33));
        playersList.setBorder(new EmptyBorder(12, 12, 12, 12));
        playersList.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(playersList);
        scrollPane.setPreferredSize(new Dimension(360, 170));
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BUTTON_BORDER_COLOR, 3, true),
                new EmptyBorder(6, 6, 6, 6)
        ));

        startButton = buildMenuButton(hostControls ? "Start" : "Waiting for host...");
        startButton.setEnabled(hostControls);
        startButton.setVisible(hostControls);
        startButton.addActionListener(_ -> listener.onStartRequested());

        JButton backButton = buildMenuButton("Back");
        backButton.addActionListener(_ -> listener.onBackRequested());

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(24, 40, 24, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        gbc.gridy = 0;
        gbc.insets = new Insets(8, 0, 14, 0);
        content.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(4, 0, 8, 0);
        content.add(subtitleLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 14, 0);
        content.add(connectionLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(6, 0, 18, 0);
        content.add(scrollPane, gbc);

        if (hostControls) {
            gbc.gridy = 4;
            gbc.insets = new Insets(8, 0, 8, 0);
            content.add(startButton, gbc);
        }

        gbc.gridy = hostControls ? 5 : 4;
        gbc.insets = new Insets(14, 0, 0, 0);
        content.add(backButton, gbc);

        add(content);
    }

    /** Sets the connection helper label (LAN IP/port hints). */
    public void setConnectionLabel(String text) {
        connectionLabel.setText(text == null ? "" : text);
    }

    /** Applies latest lobby state into labels/list/buttons. */
    public void updateLobby(LobbyStateMessage lobbyState) {
        subtitleLabel.setText(lobbyState.statusText());
        startButton.setEnabled(lobbyState.canStart());
        playersModel.clear();
        for (LobbyPlayerInfo player : lobbyState.players()) {
            String prefix = player.host() ? "Host" : "Guest";
            String suffix = player.connected() ? "connected" : "waiting";
            playersModel.addElement(prefix + " - " + player.name() + " (" + suffix + ")");
        }
    }

    /** Creates a styled button used for start/back actions. */
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
                if (!button.isEnabled()) {
                    return;
                }
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

