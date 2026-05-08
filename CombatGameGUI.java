import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Same design as the previous one, just simplified, combined into one, and yeah!
 * WHY DID NOBODY TELL ME ALL THE ERRORS WERE DUE TO THE SAME METHODS IN DIFFERENT LIBRARIES.
 * I'm joking. But anyway, enjoy it! Eesh this took a LOT of time and help to do :|
 */

public class CombatGameGUI
{
    static JFrame frame;
    static JTextArea outputArea;
    static JPanel buttonPanel;
    static JPanel hpPanel;
    static JProgressBar[] playerBars = new JProgressBar[2];
    static JProgressBar[] enemyBars = new JProgressBar[2];
    static PipedOutputStream toGame;
    static StringBuilder output = new StringBuilder();
    static Timer debounce;

    static List<String> availChars = new ArrayList<>(Arrays.asList("Knight", "Robot", "Witch"));
    static List<String> availItems = new ArrayList<>(Arrays.asList("Shield", "Potion", "Knife", "Boots", "Blow Dart"));
    static int charCount = 0;
    static int itemCount = 0;

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new StartUp());
    }

    static void showStartScreen()
    {
        frame = new JFrame("Combat Game - Without MSG Guaranteed");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(940, 700);
        frame.setLocationRelativeTo(null);

        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(Color.BLACK);

        JLabel title = new JLabel("Combat Game - Without MSG Guaranteed");
        title.setFont(new Font("SansSerif", Font.BOLD, 44));
        title.setForeground(Color.YELLOW);

        JLabel sub = new JLabel("Oliver - Sophia - Ryan");
        sub.setFont(new Font("SansSerif", Font.ITALIC, 23));
        sub.setForeground(Color.CYAN);

        JButton play = btn("PLAY", 26);
        play.setPreferredSize(new Dimension(220, 60));
        play.addActionListener(new PlayAction());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0; c.insets = new Insets(0, 0, 14, 0); bg.add(title, c);
        c.gridy = 1; c.insets = new Insets(0, 0, 22, 0); bg.add(sub, c);
        c.gridy = 3; c.insets = new Insets(0, 0, 0, 0); bg.add(play, c);

        frame.setContentPane(bg);
        frame.setVisible(true);
    }

    static void setupGameScreen()
    {
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.GREEN);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setMargin(new Insets(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.setPreferredSize(new Dimension(940, 120));

        playerBars[0] = makeBar();
        playerBars[1] = makeBar();
        enemyBars[0] = makeBar();
        enemyBars[1] = makeBar();

        JLabel yourLabel = new JLabel("YOUR TEAM:");
        yourLabel.setForeground(Color.WHITE);
        yourLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel enemyLabel = new JLabel("ENEMIES:");
        enemyLabel.setForeground(Color.WHITE);
        enemyLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        hpPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        hpPanel.setBackground(Color.BLACK);
        hpPanel.setPreferredSize(new Dimension(940, 48));
        hpPanel.add(yourLabel);
        hpPanel.add(playerBars[0]);
        hpPanel.add(playerBars[1]);
        hpPanel.add(enemyLabel);
        hpPanel.add(enemyBars[0]);
        hpPanel.add(enemyBars[1]);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(Color.BLACK);
        bottom.add(hpPanel, BorderLayout.NORTH);
        bottom.add(buttonPanel, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.revalidate();

        launchGame();
    }

    static JProgressBar makeBar()
    {
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(100);
        bar.setStringPainted(true);
        bar.setString("---");
        bar.setPreferredSize(new Dimension(160, 26));
        bar.setForeground(Color.GREEN);
        bar.setBackground(Color.DARK_GRAY);
        return bar;
    }

    static void updateBars(String line)
    {
        if (line.startsWith("YOUR TEAM:"))
        {
            parseHPLine(line, playerBars);
        }
        else if (line.startsWith("ENEMY TEAM:"))
        {
            parseHPLine(line, enemyBars);
        }
    }

    static void parseHPLine(String line, JProgressBar[] bars)
    {
        int barIdx = 0;
        int pos = 0;
        while (pos < line.length() && barIdx < 2)
        {
            int open = line.indexOf('(', pos);
            if (open == -1)
            {
                break;
            }
            int slash = line.indexOf('/', open);
            int close = line.indexOf(')', open);
            if (slash == -1 || close == -1 || slash > close)
            {
                pos = open + 1;
                continue;
            }
            String afterSlash = line.substring(slash + 1, close);
            if (!afterSlash.contains("HP"))
            {
                pos = open + 1;
                continue;
            }
            String maxStr = afterSlash.replace(" HP", "").trim();
            String curStr = line.substring(open + 1, slash).trim();
            try
            {
                int cur = Integer.parseInt(curStr);
                int max = Integer.parseInt(maxStr);
                bars[barIdx].setMaximum(max);
                bars[barIdx].setValue(cur);
                bars[barIdx].setString(cur + " / " + max + " HP");
                barIdx++;
            }
            catch (NumberFormatException ignored) { }
            pos = close + 1;
        }
    }

    static void launchGame()
    {
        try
        {
            PipedInputStream gameIn = new PipedInputStream(8192);
            toGame = new PipedOutputStream(gameIn);

            PipedOutputStream gameOut = new PipedOutputStream();
            PipedInputStream guiIn = new PipedInputStream(gameOut, 8192);
            PrintStream ps = new PrintStream(gameOut, true);

            SimpleCombatGame.init(gameIn, ps);

            Thread reader = new Thread(new GameReader(guiIn), "game-reader");
            reader.setDaemon(true);
            reader.start();

            Thread game = new Thread(new GameRunner(), "game");
            game.setDaemon(true);
            game.start();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static void send(String s)
    {
        try
        {
            toGame.write(s.getBytes());
            toGame.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static void scheduleDetect()
    {
        if (debounce != null)
        {
            debounce.stop();
        }
        debounce = new Timer(150, new TimerTick());
        debounce.setRepeats(false);
        debounce.start();
    }

    static void detectState()
    {
        String[] allLines = output.toString().split("\n");

        List<String> recent = new ArrayList<>();
        for (int i = allLines.length - 1; i >= 0 && recent.size() < 20; i--)
        {
            String t = allLines[i].trim();
            if (!t.isEmpty())
            {
                recent.add(0, t);
            }
        }
        if (recent.isEmpty())
        {
            return;
        }

        String last = recent.get(recent.size() - 1);

        clearBtns();

        if (last.contains("GAME OVER") || last.contains("VICTORY") || last.contains("You won") || last.contains("You lost"))
        {
            addGameOverBtns();
        }
        else if (last.contains("Press Enter to return"))
        {
            JButton ret = btn("Return", 14);
            ret.addActionListener(new DisableSend(ret));
            buttonPanel.add(ret);
        }
        else if (last.contains("Press Enter to continue"))
        {
            JButton cont = btn("Continue", 15);
            cont.addActionListener(new DisableSend(cont));
            buttonPanel.add(cont);
        }
        else if (last.equals("2) Items"))
        {
            JButton chars = btn("Characters", 14);
            chars.addActionListener(new SendAction("1\n"));
            JButton items = btn("Items", 14);
            items.addActionListener(new SendAction("2\n"));
            buttonPanel.add(chars);
            buttonPanel.add(items);
        }
        else if (last.contains("1) Attack") && last.contains("2) Ultimate"))
        {
            addActionBtns();
        }
        else if (last.contains("Blow Dart target"))
        {
            addTargetBtns("Blow Dart ->");
        }
        else if (last.contains("Target (1 or 2)"))
        {
            addTargetBtns("Attack ->");
        }
        else if (last.equals("0) Description"))
        {
            if (charCount < 2)
            {
                addCharBtns();
            }
            else
            {
                addItemBtns();
            }
        }

        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    static void addCharBtns()
    {
        List<String> list = new ArrayList<>(availChars);
        for (int i = 0; i < list.size(); i++)
        {
            String name = list.get(i);
            int idx = i + 1;
            JButton b = btn(name, 15);
            b.addActionListener(new PickChar(b, idx, name));
            buttonPanel.add(b);
        }
        JButton d = btn("Descriptions", 13);
        d.addActionListener(new SendAction("0\n"));
        buttonPanel.add(d);
    }

    static void addItemBtns()
    {
        List<String> list = new ArrayList<>(availItems);
        for (int i = 0; i < list.size(); i++)
        {
            String name = list.get(i);
            int idx = i + 1;
            JButton b = btn(name, 14);
            b.addActionListener(new PickItem(b, idx, name));
            buttonPanel.add(b);
        }
        JButton d = btn("Descriptions", 13);
        d.addActionListener(new SendAction("0\n"));
        buttonPanel.add(d);
    }

    static void addActionBtns()
    {
        JButton atk = btn("[ Attack ]", 14);
        JButton ult = btn("[ Ultimate ]", 14);
        JButton item = btn("[ Use Item ]", 14);
        JButton wait = btn("[ Wait ]", 14);
        JButton help = btn("[ Help ]", 13);

        atk.addActionListener(new SendAction("1\n"));
        ult.addActionListener(new SendAction("2\n"));
        item.addActionListener(new SendAction("3\n"));
        wait.addActionListener(new SendAction("4\n"));
        help.addActionListener(new SendAction("5\n"));

        buttonPanel.add(atk);
        buttonPanel.add(ult);
        buttonPanel.add(item);
        buttonPanel.add(wait);
        buttonPanel.add(help);
    }

    static void addTargetBtns(String label)
    {
        JLabel lbl = new JLabel(label);
        lbl.setForeground(Color.LIGHT_GRAY);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton t1 = btn("Enemy 1", 14);
        JButton t2 = btn("Enemy 2", 14);

        t1.addActionListener(new SendAction("1\n"));
        t2.addActionListener(new SendAction("2\n"));

        buttonPanel.add(lbl);
        buttonPanel.add(t1);
        buttonPanel.add(t2);
    }

    static void addGameOverBtns()
    {
        JButton quit = btn("Quit", 16);
        quit.addActionListener(new QuitAction());
        buttonPanel.add(quit);
    }

    static void clearBtns()
    {
        buttonPanel.removeAll();
    }

    static JButton btn(String text, int fontSize)
    {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        b.setBackground(Color.GRAY);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        return b;
    }

    static class StartUp implements Runnable
    {
        public void run()
        {
            showStartScreen();
        }
    }

    static class GameReader implements Runnable
    {
        PipedInputStream src;

        GameReader(PipedInputStream src)
        {
            this.src = src;
        }

        public void run()
        {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(src)))
            {
                String line;
                while ((line = br.readLine()) != null)
                {
                    SwingUtilities.invokeLater(new UIUpdate(line));
                }
            }
            catch (IOException ignored) { }
        }
    }

    static class UIUpdate implements Runnable
    {
        String line;

        UIUpdate(String line)
        {
            this.line = line;
        }

        public void run()
        {
            output.append(line).append('\n');
            outputArea.append(line + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
            updateBars(line);
            scheduleDetect();
        }
    }

    static class GameRunner implements Runnable
    {
        public void run()
        {
            SimpleCombatGame.main(new String[]{});
        }
    }

    static class TimerTick implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            ((Timer) e.getSource()).stop();
            detectState();
        }
    }

    static class PlayAction implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            setupGameScreen();
        }
    }

    static class SendAction implements ActionListener
    {
        String msg;

        SendAction(String msg)
        {
            this.msg = msg;
        }

        public void actionPerformed(ActionEvent e)
        {
            send(msg);
        }
    }

    static class DisableSend implements ActionListener
    {
        JButton b;

        DisableSend(JButton b)
        {
            this.b = b;
        }

        public void actionPerformed(ActionEvent e)
        {
            b.setEnabled(false);
            send("\n\n");
        }
    }

    static class PickChar implements ActionListener
    {
        JButton b;
        int idx;
        String name;

        PickChar(JButton b, int idx, String name)
        {
            this.b = b;
            this.idx = idx;
            this.name = name;
        }

        public void actionPerformed(ActionEvent e)
        {
            b.setEnabled(false);
            send(idx + "\n");
            availChars.remove(name);
            charCount++;
        }
    }

    static class PickItem implements ActionListener
    {
        JButton b;
        int idx;
        String name;

        PickItem(JButton b, int idx, String name)
        {
            this.b = b;
            this.idx = idx;
            this.name = name;
        }

        public void actionPerformed(ActionEvent e)
        {
            b.setEnabled(false);
            send(idx + "\n");
            availItems.remove(name);
            itemCount++;
        }
    }

    static class QuitAction implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            frame.dispose();
        }
    }
}
