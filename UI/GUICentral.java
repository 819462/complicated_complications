import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class GUICentral implements ActionListener {

    private JFrame frame;
    private JPanel titlePanel;
    private JPanel battlePanel;
    private JTextArea battleLog;
    private JPanel buttonPanel;

    private JLabel[] playerLabels = new JLabel[2];
    private JLabel[] enemyLabels  = new JLabel[2];
    private JProgressBar[] playerBars = new JProgressBar[2];
    private JProgressBar[] enemyBars  = new JProgressBar[2];

    private JButton playButton;
    private JButton tryAgainButton;
    private JButton[] choiceButtons = new JButton[6];

    private WriteOut writeOut;

    public GUICentral() {
        frame = new JFrame("Simple Combat Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 620);
        frame.setLocationRelativeTo(null);

        buildTitlePanel();
        frame.add(titlePanel);
        frame.setVisible(true);
    }

    private void buildTitlePanel() {
        titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(20, 20, 40));

        JLabel heading = new JLabel("Simple Combat Game", SwingConstants.CENTER);
        heading.setFont(new Font("Serif", Font.BOLD, 34));
        heading.setForeground(Color.WHITE);
        titlePanel.add(heading, BorderLayout.CENTER);

        JPanel bottomArea = new JPanel();
        bottomArea.setBackground(new Color(20, 20, 40));

        playButton = new JButton("PLAY");
        playButton.setFont(new Font("Arial", Font.BOLD, 18));
        playButton.addActionListener(this);
        bottomArea.add(playButton);

        titlePanel.add(bottomArea, BorderLayout.SOUTH);
    }

    private void startGame() throws IOException {
        frame.remove(titlePanel);

        battlePanel = new JPanel(new BorderLayout());
        battlePanel.setBackground(new Color(20, 20, 40));

        JPanel hpPanel = new JPanel(new GridLayout(2, 1));
        hpPanel.setBackground(new Color(30, 30, 50));

        JPanel yourRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        yourRow.setBackground(new Color(30, 30, 50));
        JLabel yourTeamTitle = new JLabel("YOUR TEAM:");
        yourTeamTitle.setForeground(Color.WHITE);
        yourRow.add(yourTeamTitle);
        for (int i = 0; i < 2; i++) {
            playerLabels[i] = new JLabel("---");
            playerLabels[i].setForeground(new Color(100, 220, 255));
            yourRow.add(playerLabels[i]);
            playerBars[i] = new JProgressBar(0, 100);
            playerBars[i].setValue(100);
            playerBars[i].setPreferredSize(new Dimension(130, 16));
            playerBars[i].setForeground(Color.GREEN);
            playerBars[i].setBackground(new Color(50, 50, 60));
            yourRow.add(playerBars[i]);
        }

        JPanel enemyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        enemyRow.setBackground(new Color(30, 30, 50));
        JLabel enemyTeamTitle = new JLabel("ENEMY TEAM:");
        enemyTeamTitle.setForeground(Color.WHITE);
        enemyRow.add(enemyTeamTitle);
        for (int i = 0; i < 2; i++) {
            enemyLabels[i] = new JLabel("---");
            enemyLabels[i].setForeground(new Color(255, 160, 80));
            enemyRow.add(enemyLabels[i]);
            enemyBars[i] = new JProgressBar(0, 100);
            enemyBars[i].setValue(100);
            enemyBars[i].setPreferredSize(new Dimension(130, 16));
            enemyBars[i].setForeground(Color.RED);
            enemyBars[i].setBackground(new Color(50, 50, 60));
            enemyRow.add(enemyBars[i]);
        }

        hpPanel.add(yourRow);
        hpPanel.add(enemyRow);
        battlePanel.add(hpPanel, BorderLayout.NORTH);

        battleLog = new JTextArea();
        battleLog.setEditable(false);
        battleLog.setFont(new Font("Monospaced", Font.PLAIN, 13));
        battleLog.setBackground(new Color(15, 15, 25));
        battleLog.setForeground(new Color(210, 210, 210));
        battleLog.setLineWrap(true);
        battleLog.setWrapStyleWord(true);
        battlePanel.add(new JScrollPane(battleLog), BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        buttonPanel.setBackground(new Color(30, 30, 50));
        buttonPanel.setPreferredSize(new Dimension(820, 85));
        battlePanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(battlePanel);
        frame.revalidate();
        frame.repaint();

        PipedOutputStream guiToGame = new PipedOutputStream();
        PipedInputStream  gameIn    = new PipedInputStream(guiToGame);
        PipedOutputStream gameOut   = new PipedOutputStream();
        PipedInputStream  guiIn     = new PipedInputStream(gameOut);

        writeOut = new WriteOut(guiToGame);
        ReadIn readIn = new ReadIn(guiIn, this);

        SimpleCombatGame.init(gameIn, new PrintStream(gameOut, true));

        GameRunner gameThread = new GameRunner();
        gameThread.setDaemon(true);
        gameThread.start();

        Thread readThread = new Thread(readIn);
        readThread.setDaemon(true);
        readThread.start();
    }

    public void actionPerformed(ActionEvent e) throws IOException {
        if (e.getSource() == playButton) {
            startGame();
            return;
        }

        if (tryAgainButton != null && e.getSource() == tryAgainButton) {
            frame.remove(battlePanel);
            battlePanel = null;
            buildTitlePanel();
            frame.add(titlePanel);
            frame.revalidate();
            frame.repaint();
            return;
        }

        for (int i = 0; i < choiceButtons.length; i++) {
            if (choiceButtons[i] != null && e.getSource() == choiceButtons[i]) {
                writeOut.send(String.valueOf(i + 1));
                buttonPanel.removeAll();
                buttonPanel.revalidate();
                buttonPanel.repaint();
                return;
            }
        }
    }

    private void showButtons(String[] labels) {
        buttonPanel.removeAll();
        for (int i = 0; i < labels.length; i++) {
            choiceButtons[i] = new JButton(labels[i]);
            choiceButtons[i].setFont(new Font("Arial", Font.BOLD, 14));
            choiceButtons[i].addActionListener(this);
            buttonPanel.add(choiceButtons[i]);
        }
        for (int i = labels.length; i < choiceButtons.length; i++) {
            choiceButtons[i] = null;
        }
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    public void log(String line) {
        battleLog.append(line + "\n");
        battleLog.setCaretPosition(battleLog.getDocument().getLength());
    }

    public void showCharacterSelect(String[] options) {
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].replaceAll("^[0-9]+\\)\\s*", "");
        }
        showButtons(labels);
    }

    public void showItemSelect(String[] options) {
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].replaceAll("^[0-9]+\\)\\s*", "");
        }
        showButtons(labels);
    }

    public void showActionButtons() {
        String[] actions = {"Attack", "Ultimate", "Item", "Nothing"};
        showButtons(actions);
    }

    public void showTargetButtons() {
        String[] targets = new String[2];
        for (int i = 0; i < 2; i++) {
            if (enemyLabels[i].getText().equals("---")) {
                targets[i] = "Enemy " + (i + 1);
            } else {
                targets[i] = enemyLabels[i].getText();
            }
        }
        showButtons(targets);
    }

    public void showContinueButton() {
        String[] cont = {"Continue"};
        showButtons(cont);
    }

    public void showEndScreen(boolean won) {
        buttonPanel.removeAll();

        JLabel result = new JLabel(won ? "VICTORY!!" : "You Lost :(");
        result.setFont(new Font("Arial", Font.BOLD, 22));
        result.setForeground(won ? Color.GREEN : Color.RED);
        buttonPanel.add(result);

        tryAgainButton = new JButton("Try Again");
        tryAgainButton.setFont(new Font("Arial", Font.BOLD, 14));
        tryAgainButton.addActionListener(this);
        buttonPanel.add(tryAgainButton);

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    public void updatePlayerHP(String line) {
        String data = line.replace("YOUR TEAM:", "").trim();
        parseAndApplyHP(data, playerLabels, playerBars);
    }

    public void updateEnemyHP(String line) {
        String data = line.replace("ENEMY TEAM:", "").trim();
        parseAndApplyHP(data, enemyLabels, enemyBars);
    }

    private void parseAndApplyHP(String text, JLabel[] labels, JProgressBar[] bars) {
        String[] parts = text.split("  +");
        int idx = 0;
        for (int p = 0; p < parts.length && idx < 2; p++) {
            String part = parts[p].trim();
            if (part.isEmpty()) continue;

            int paren = part.indexOf("(");
            int slash = part.indexOf("/", paren);
            int hpEnd = part.indexOf(" HP)", slash);
            if (paren < 0 || slash < 0 || hpEnd < 0) continue;

            String hpStr  = part.substring(paren + 1, slash).trim();
            String maxStr = part.substring(slash + 1, hpEnd).trim();

            if (hpStr.matches("[0-9]+") && maxStr.matches("[0-9]+")) {
                String name  = part.substring(0, paren).trim();
                int hp       = Integer.parseInt(hpStr);
                int maxHp    = Integer.parseInt(maxStr);

                labels[idx].setText(name + "  " + hp + "/" + maxHp);
                bars[idx].setMaximum(maxHp);
                bars[idx].setValue(hp);

                double ratio = (double) hp / maxHp;
                if (ratio > 0.5) {
                    bars[idx].setForeground(Color.GREEN);
                } else if (ratio > 0.25) {
                    bars[idx].setForeground(Color.YELLOW);
                } else {
                    bars[idx].setForeground(Color.RED);
                }
                idx++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new GUICentral();
    }
}
