import main.GamePanel;
import main.LocalMultiplayerClient;
import main.LocalMultiplayerHost;
import main.LocalMultiplayerMenuPanel;
import main.LocalMultiplayerProtocol.LobbyStateMessage;
import main.LocalMultiplayerProtocol.MultiplayerGameSnapshot;
import main.MainMenuPanel;
import main.MultiplayerGameController;
import main.MultiplayerGamePanel;
import main.MultiplayerLobbyPanel;
import main.UiFonts;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Enumeration;

/** Entry point: initializes frame/theme and routes to main menu. */
void main() {
    applyGlobalUiFont(UiFonts.plain(14f));

    JFrame window = new JFrame();
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.setResizable(false);
    window.setTitle("Carta");

    showMainMenu(window);

    window.pack();
    window.setLocationRelativeTo(null);
    window.setVisible(true);
}

/** Shows initial mode selection screen. */
void showMainMenu(JFrame window) {
    MainMenuPanel mainMenuPanel = new MainMenuPanel(
            () -> launchCpuGame(window),
            () -> showLocalMultiplayerMenu(window)
    );
    setWindowContent(window, mainMenuPanel);
}

/** Shows multiplayer setup form (name, host, port). */
void showLocalMultiplayerMenu(JFrame window) {
    LocalMultiplayerMenuPanel multiplayerMenuPanel = new LocalMultiplayerMenuPanel(new LocalMultiplayerMenuPanel.Listener() {
        @Override
        public void onHostRequested(String playerName, String portText) {
            Integer port = parsePortOrNull(portText);
            if (port == null) {
                JOptionPane.showMessageDialog(window, "Invalid port. Use a number between 1 and 65535.",
                        "Local multiplayer", JOptionPane.WARNING_MESSAGE);
                return;
            }
            showHostLobby(window, playerName, port);
        }

        @Override
        public void onJoinRequested(String playerName, String hostAddress, String portText) {
            InetSocketAddress target = parseJoinTarget(hostAddress, portText);
            if (target == null) {
                JOptionPane.showMessageDialog(window,
                        "Invalid host/port. Use host + port field, or host:port in host address.",
                        "Local multiplayer",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            showJoinLobby(window, playerName, target.getHostString(), target.getPort());
        }

        @Override
        public void onBackRequested() {
            showMainMenu(window);
        }
    });
    setWindowContent(window, multiplayerMenuPanel);
}

/** Creates host lobby, starts server, and waits for remote player handshake. */
void showHostLobby(JFrame window, String playerName, int port) {
    final LocalMultiplayerHost[] hostHolder = new LocalMultiplayerHost[1]; // array wrapper allows updates inside anonymous callbacks
    MultiplayerLobbyPanel lobbyPanel = new MultiplayerLobbyPanel("Host lobby", true, new MultiplayerLobbyPanel.Listener() {
        @Override
        public void onStartRequested() {
            if (hostHolder[0] != null) {
                hostHolder[0].startGame();
            }
        }

        @Override
        public void onBackRequested() {
            if (hostHolder[0] != null) {
                hostHolder[0].close();
            }
            showLocalMultiplayerMenu(window);
        }
    });

    LocalMultiplayerHost host = new LocalMultiplayerHost(playerName, port, new LocalMultiplayerHost.LobbyListener() {
        @Override
        public void onLobbyUpdated(LobbyStateMessage lobbyState) {
            lobbyPanel.updateLobby(lobbyState);
        }

        @Override
        public void onGameStarted(LocalMultiplayerHost controller, MultiplayerGameSnapshot snapshot) {
            launchMultiplayerGame(window, controller, snapshot);
        }

        @Override
        public void onError(String message) {
            JOptionPane.showMessageDialog(window, message, "Host lobby", JOptionPane.WARNING_MESSAGE);
        }

        @Override
        public void onDisconnected(String reason) {
            lobbyPanel.updateLobby(new LobbyStateMessage(lobbyPanelStateFallback(playerName), false, reason));
        }
    });
    hostHolder[0] = host;

    try {
        host.start();
        List<String> addressHints = host.getReachableAddressHints();
        String hostLabel = addressHints.isEmpty()
                ? "<html>Join from another PC with this host IP and port: " + host.getConnectionAddress() + "</html>"
                : "<html>Join from another PC using:<br/>" + String.join("<br/>", addressHints) + "</html>";
        lobbyPanel.setConnectionLabel(hostLabel);
        setWindowContent(window, lobbyPanel);
    } catch (IOException exception) {
        JOptionPane.showMessageDialog(window,
                "Could not start the host: " + exception.getMessage(),
                "Host lobby",
                JOptionPane.ERROR_MESSAGE);
        showLocalMultiplayerMenu(window);
    }
}

/** Builds a safe fallback lobby list when host loses remote player before game start. */
java.util.List<main.LocalMultiplayerProtocol.LobbyPlayerInfo> lobbyPanelStateFallback(String playerName) {
    return java.util.List.of(
            new main.LocalMultiplayerProtocol.LobbyPlayerInfo(playerName == null || playerName.isBlank() ? "Host" : playerName.trim(), true, true),
            new main.LocalMultiplayerProtocol.LobbyPlayerInfo("Waiting for player 2...", false, false)
    );
}

/** Connects to host and displays joiner waiting room until host starts game. */
void showJoinLobby(JFrame window, String playerName, String hostAddress, int port) {
    final LocalMultiplayerClient[] clientHolder = new LocalMultiplayerClient[1];
    MultiplayerLobbyPanel lobbyPanel = new MultiplayerLobbyPanel("Join lobby", false, new MultiplayerLobbyPanel.Listener() {
        @Override
        public void onStartRequested() {
        }

        @Override
        public void onBackRequested() {
            if (clientHolder[0] != null) {
                clientHolder[0].close();
            }
            showLocalMultiplayerMenu(window);
        }
    });
    lobbyPanel.setConnectionLabel("Connecting to " + (hostAddress == null || hostAddress.isBlank() ? "127.0.0.1" : hostAddress.trim())
            + ":" + port);
    setWindowContent(window, lobbyPanel);

    LocalMultiplayerClient client = new LocalMultiplayerClient(playerName, hostAddress, port,
            new LocalMultiplayerClient.LobbyListener() {
                @Override
                public void onLobbyUpdated(LobbyStateMessage lobbyState) {
                    lobbyPanel.updateLobby(lobbyState);
                }

                @Override
                public void onGameStarted(LocalMultiplayerClient controller, MultiplayerGameSnapshot snapshot) {
                    launchMultiplayerGame(window, controller, snapshot);
                }

                @Override
                public void onError(String message) {
                    JOptionPane.showMessageDialog(window, message, "Join lobby", JOptionPane.WARNING_MESSAGE);
                }

                @Override
                public void onDisconnected(String reason) {
                    JOptionPane.showMessageDialog(window, reason, "Join lobby", JOptionPane.INFORMATION_MESSAGE);
                    showLocalMultiplayerMenu(window);
                }
            });
    clientHolder[0] = client;

    try {
        client.connect();
    } catch (IOException exception) {
        JOptionPane.showMessageDialog(window,
                "Could not connect to the host: " + exception.getMessage(),
                "Join lobby",
                JOptionPane.ERROR_MESSAGE);
        client.close();
        showLocalMultiplayerMenu(window);
    }
}

/** Parses and validates port text. Returns null for invalid input. */
Integer parsePortOrNull(String portText) {
    try {
        int port = Integer.parseInt(portText == null ? "" : portText.trim());
        return port >= 1 && port <= 65535 ? port : null;
    } catch (NumberFormatException ignored) {
        return null;
    }
}

/**
 * Parses join target from either:
 * - host + explicit port field, or
 * - host:port in host field.
 */
InetSocketAddress parseJoinTarget(String hostAddressText, String portText) {
    String host = hostAddressText == null || hostAddressText.isBlank() ? "127.0.0.1" : hostAddressText.trim();
    Integer explicitPort = parsePortOrNull(portText);

    int colonIndex = host.lastIndexOf(':');
    if (colonIndex > 0 && colonIndex < host.length() - 1 && host.indexOf(']') < 0) {
        String maybePort = host.substring(colonIndex + 1);
        Integer portFromHost = parsePortOrNull(maybePort);
        if (portFromHost != null) {
            return new InetSocketAddress(host.substring(0, colonIndex), portFromHost);
        }
    }

    if (explicitPort == null) {
        return null;
    }
    return new InetSocketAddress(host, explicitPort);
}

/** Launches existing single-player CPU panel. */
void launchCpuGame(JFrame window) {
    GamePanel gamePanel = new GamePanel();
    setWindowContent(window, gamePanel);
    gamePanel.startGameThread();
}

/** Launches multiplayer panel with a host/client controller backend. */
void launchMultiplayerGame(JFrame window, MultiplayerGameController controller, MultiplayerGameSnapshot snapshot) {
    MultiplayerGamePanel gamePanel = new MultiplayerGamePanel(controller, snapshot);
    setWindowContent(window, gamePanel);
}

/** Replaces active frame panel and refreshes layout. */
void setWindowContent(JFrame window, JPanel panel) {
    window.setContentPane(panel);
    window.revalidate();
    window.repaint();
}

/** Applies a global Swing UI font so dialogs and controls match in-game typography. */
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
