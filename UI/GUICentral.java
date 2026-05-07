// GUICentral.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class GUICentral {

    // === WINDOW ===
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel battlePanel; // tracked so we can replace it on Play Again

    // === COMMUNICATION ===
    private WriteOut writeOut;

    // === BATTLE SCREEN COMPONENTS ===
    private JTextArea battleLog;
    private JPanel buttonPanel;

    // === HP DISPLAY ===
    private JLabel[] playerLabels = new JLabel[2];
    private JLabel[] enemyLabels  = new JLabel[2];
    private JProgressBar[] playerBars = new JProgressBar[2];
    private JProgressBar[] enemyBars  = new JProgressBar[2];

    // ==================== SETUP ====================

    public GUICentral() {
        frame = new JFrame("Turn-Based Combat Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 620);
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        frame.add(mainPanel);

        buildTitleScreen();
        frame.setVisible(true);
    }

    private void buildTitleScreen() {
        JPanel title = new JPanel(new GridBagLayout());
        title.setBackground(new Color(20, 20, 40));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.insets = new Insets(25, 0, 25, 0);

        JLabel heading = new JLabel("⚔  TURN-BASED COMBAT  ⚔");
        heading.setFont(new Font("Serif", Font.BOLD, 34));
        heading.setForeground(Color.WHITE);
        g.gridy = 0;
        title.add(heading, g);

        JButton play = styledButton("PLAY", 18);
        play.setPreferredSize(new Dimension(150, 50));
        play.addActionListener(e -> startGame());
        g.gridy = 1;
        title.add(play, g);

        mainPanel.add(title, "title");
        cardLayout.show(mainPanel, "title");
    }

    private void buildBattleScreen() {
        // Remove old battle panel if exists (Play Again)
        if (battlePanel != null) mainPanel.remove(battlePanel);

        battlePanel = new JPanel(new BorderLayout());
        battlePanel.setBackground(new Color(20, 20, 40));

        // --- HP BARS (North) ---
        JPanel hpPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        hpPanel.setBackground(new Color(30, 30, 50));
        hpPanel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JPanel yourRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        yourRow.setBackground(new Color(30, 30, 50));
        yourRow.add(colorLabel("YOUR TEAM:", Color.WHITE));
        for (int i = 0; i < 2; i++) {
            playerLabels[i] = colorLabel("---", new Color(100, 220, 255));
            playerBars[i]   = makeBar(Color.GREEN);
            yourRow.add(playerLabels[i]);
            yourRow.add(playerBars[i]);
        }

        JPanel enemyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        enemyRow.setBackground(new Color(30, 30, 50));
        enemyRow.add(colorLabel("ENEMY TEAM:", Color.WHITE));
        for (int i = 0; i < 2; i++) {
            enemyLabels[i] = colorLabel("---", new Color(255, 160, 80));
            enemyBars[i]   = makeBar(Color.RED);
            enemyRow.add(enemyLabels[i]);
            enemyRow.add(enemyBars[i]);
        }

        hpPanel.add(yourRow);
        hpPanel.add(enemyRow);
        battlePanel.add(hpPanel, BorderLayout.NORTH);

        // --- BATTLE LOG (Center) ---
        battleLog = new JTextArea();
        battleLog.setEditable(false);
        battleLog.setFont(new Font("Monospaced", Font.PLAIN, 13));
        battleLog.setBackground(new Color(15, 15, 25));
        battleLog.setForeground(new Color(210, 210, 210));
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(battleLog);
        battlePanel.add(scroll, BorderLayout.CENTER);

        // --- BUTTON PANEL (South) ---
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        buttonPanel.setBackground(new Color(30, 30, 50));
        buttonPanel.setPreferredSize(new Dimension(820, 85));
        battlePanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(battlePanel, "battle");
    }

    // ==================== START GAME ====================

    private void startGame() {
        buildBattleScreen();
        cardLayout.show(mainPanel, "battle");

        try {
            PipedOutputStream guiToGame = new PipedOutputStream();
            PipedInputStream  gameIn    = new PipedInputStream(guiToGame);

            PipedOutputStream gameOut = new PipedOutputStream();
            PipedInputStream  guiIn   = new PipedInputStream(gameOut);

            writeOut = new WriteOut(guiToGame);
            ReadIn readIn = new ReadIn(guiIn, this);

            SimpleCombatGame.init(gameIn, new PrintStream(gameOut, true));

            Thread gameThread = new Thread(() -> SimpleCombatGame.main(new String[]{}));
            gameThread.setDaemon(true);
            gameThread.start();

            Thread readThread = new Thread(readIn);
            readThread.setDaemon(true);
            readThread.start();

        } catch (IOException e) {
            log("Failed to start game: " + e.getMessage());
        }
    }

    // ==================== CALLED BY READIN ====================

    public void log(String line) {
        SwingUtilities.invokeLater(() -> {
            battleLog.append(line + "\n");
            // Auto-scroll to bottom
            battleLog.setCaretPosition(battleLog.getDocument().getLength());
        });
    }

    public void showCharacterSelect(List<String> options) {
        SwingUtilities.invokeLater(() -> {
            clearButtons();
            for (int i = 0; i < options.size(); i++) {
                final int choice = i + 1;
                // Strip "1) " prefix to get just the name
                String label = options.get(i).replaceAll("^[0-9]+\\)\\s*", "");
                buttonPanel.add(styledButton(label, 14, () -> sendAndClear(String.valueOf(choice))));
            }
            refreshButtons();
        });
    }

    public void showItemSelect(List<String> options) {
        SwingUtilities.invokeLater(() -> {
            clearButtons();
            for (int i = 0; i < options.size(); i++) {
                final int choice = i + 1;
                String label = options.get(i).replaceAll("^[0-9]+\\)\\s*", "");
                buttonPanel.add(styledButton(label, 14, () -> sendAndClear(String.valueOf(choice))));
            }
            refreshButtons();
        });
    }

    public void showActionButtons() {
        SwingUtilities.invokeLater(() -> {
            clearButtons();
            String[] actions = {"Attack", "Ultimate", "Item", "Nothing"};
            for (int i = 0; i < actions.length; i++) {
                final int choice = i + 1;
                buttonPanel.add(styledButton(actions[i], 14, () -> sendAndClear(String.valueOf(choice))));
            }
            refreshButtons();
        });
    }

    public void showTargetButtons() {
        SwingUtilities.invokeLater(() -> {
            clearButtons();
            for (int i = 0; i < 2; i++) {
                final int choice = i + 1;
                // Show actual enemy name from HP bar labels if available
                String name = enemyLabels[i].getText().equals("---")
                    ? "Enemy " + choice
                    : enemyLabels[i].getText();
                buttonPanel.add(styledButton(name, 14, () -> sendAndClear(String.valueOf(choice))));
            }
            refreshButtons();
        });
    }

    public void showContinueButton() {
        SwingUtilities.invokeLater(() -> {
            clearButtons();
            buttonPanel.add(styledButton("Continue →", 14, () -> sendAndClear("")));
            refreshButtons();
        });
    }

    public void showEndScreen(boolean won) {
        SwingUtilities.invokeLater(() -> {
            clearButtons();

            JLabel result = new JLabel(won ? "🏆  VICTORY!" : "💀  GAME OVER");
            result.setFont(new Font("Arial", Font.BOLD, 22));
            result.setForeground(won ? Color.GREEN : Color.RED);
            buttonPanel.add(result);

            buttonPanel.add(styledButton("Play Again", 14, () -> {
                cardLayout.show(mainPanel, "title");
            }));

            refreshButtons();
        });
    }

    public void updatePlayerHP(String line) {
        String data = line.replace("YOUR TEAM:", "").trim();
        SwingUtilities.invokeLater(() -> parseAndApplyHP(data, playerLabels, playerBars));
    }

    public void updateEnemyHP(String line) {
        String data = line.replace("ENEMY TEAM:", "").trim();
        SwingUtilities.invokeLater(() -> parseAndApplyHP(data, enemyLabels, enemyBars));
    }

    // ==================== HP PARSING ====================

    private void parseAndApplyHP(String text, JLabel[] labels, JProgressBar[] bars) {
        // Input: "O's Knight (180/250 HP) [Poison:2]  O's Robot (300/300 HP)"
        // Split on 2+ spaces to separate characters
        String[] parts = text.split("  +");
        int idx = 0;
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty() || idx >= 2) continue;

            int paren = part.indexOf("(");
            int slash = part.indexOf("/", paren);
            // " HP)" might have status tags after, find just the digits
            int hpEnd = part.indexOf(" HP)", slash);
            if (paren < 0 || slash < 0 || hpEnd < 0) continue;

            try {
                String name  = part.substring(0, paren).trim();
                int    hp    = Integer.parseInt(part.substring(paren + 1, slash).trim());
                int    maxHp = Integer.parseInt(part.substring(slash + 1, hpEnd).trim());

                labels[idx].setText(name + "  " + hp + "/" + maxHp);
                bars[idx].setMaximum(maxHp);
                bars[idx].setValue(hp);

                double ratio = (double) hp / maxHp;
                bars[idx].setForeground(
                    ratio > 0.5 ? Color.GREEN :
                    ratio > 0.25 ? Color.YELLOW : Color.RED
                );
                idx++;
            } catch (NumberFormatException ignored) {}
        }
    }

    // ==================== HELPERS ====================

    private void clearButtons() {
        buttonPanel.removeAll();
    }

    private void refreshButtons() {
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    private void sendAndClear(String val) {
        writeOut.send(val);
        clearButtons();
        refreshButtons();
    }

    private JLabel colorLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(new Font("Arial", Font.PLAIN, 13));
        return l;
    }

    private JProgressBar makeBar(Color color) {
        JProgressBar b = new JProgressBar(0, 100);
        b.setValue(100);
        b.setPreferredSize(new Dimension(130, 16));
        b.setForeground(color);
        b.setBackground(new Color(50, 50, 60));
        return b;
    }

    // Button with no action (for layout)
    private JButton styledButton(String label, int fontSize) {
        JButton b = new JButton(label);
        b.setFont(new Font("Arial", Font.BOLD, fontSize));
        return b;
    }

    // Button with action
    private JButton styledButton(String label, int fontSize, Runnable action) {
        JButton b = styledButton(label, fontSize);
        b.addActionListener(e -> action.run());
        return b;
    }

    // ==================== ENTRY POINT ====================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUICentral());
    }
}
