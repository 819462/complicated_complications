import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles character and item selection through dialog windows BEFORE the main game screen.
 * Returns all selections so they can be auto-fed to the game, keeping the main screen clean.
 */
public class GameSetupDialogs {
    
    private static final String[] ALL_CHARACTERS = {"Knight", "Robot", "Witch"};
    private static final String[] ALL_ITEMS = {"Shield", "Potion", "Knife", "Boots", "Blow Dart"};
    
    private static final String KNIGHT_DESC = "Knight - 250 HP, Speed 2, Base Damage: 30\nUlt (2 charges): Counter (2.5x reflect)";
    private static final String ROBOT_DESC = "Robot - 300 HP, Speed 1, Base Damage: 35\nUlt (3 charges): Rocket (50 damage to all enemies)";
    private static final String WITCH_DESC = "Witch - 200 HP, Speed 3, Base Damage: 20\nUlt (5 charges): Revive ally or heal 20 HP";
    
    private static final String SHIELD_DESC = "Shield - Blocks all damage this turn";
    private static final String POTION_DESC = "Potion - Heals 40 HP (one-time use)";
    private static final String KNIFE_DESC = "Knife - +50% damage for 2 turns";
    private static final String BOOTS_DESC = "Boots - +2 Speed permanently";
    private static final String DART_DESC = "Blow Dart - Target enemy (3 uses)\n50% chance their action fails for 3 turns";
    
    /**
     * Container for all pre-game selections
     */
    public static class GameSelections {
        public int char1Index;  // 1-based index for SimpleCombatGame
        public int char2Index;
        public int item1Index;
        public int item2Index;
        
        public String char1Name;
        public String char2Name;
        public String item1Name;
        public String item2Name;
    }
    
    /**
     * Run the full setup sequence and return all selections
     */
    public static GameSelections runSetup(JFrame parentFrame) {
        GameSelections selections = new GameSelections();
        
        List<String> availableChars = new ArrayList<>(Arrays.asList(ALL_CHARACTERS));
        List<String> availableItems = new ArrayList<>(Arrays.asList(ALL_ITEMS));
        
        // Character 1
        int char1Result = showCharacterDialog(parentFrame, availableChars, 1);
        if (char1Result == -1) return null; // User cancelled
        selections.char1Index = char1Result + 1;
        selections.char1Name = availableChars.get(char1Result);
        availableChars.remove(char1Result);
        
        // Character 2
        int char2Result = showCharacterDialog(parentFrame, availableChars, 2);
        if (char2Result == -1) return null;
        selections.char2Index = char2Result + 1;
        selections.char2Name = availableChars.get(char2Result);
        availableChars.remove(char2Result);
        
        // Item 1
        int item1Result = showItemDialog(parentFrame, availableItems, selections.char1Name);
        if (item1Result == -1) return null;
        selections.item1Index = item1Result + 1;
        selections.item1Name = availableItems.get(item1Result);
        availableItems.remove(item1Result);
        
        // Item 2
        int item2Result = showItemDialog(parentFrame, availableItems, selections.char2Name);
        if (item2Result == -1) return null;
        selections.item2Index = item2Result + 1;
        selections.item2Name = availableItems.get(item2Result);
        availableItems.remove(item2Result);
        
        return selections;
    }
    
    /**
     * Show character selection dialog
     * @return index in availableChars list, or -1 if cancelled
     */
    private static int showCharacterDialog(JFrame parent, List<String> availableChars, int playerNum) {
        JDialog dialog = new JDialog(parent, "Select Character " + playerNum, true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        JLabel titleLabel = new JLabel("Choose Character " + playerNum + " for Your Team", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.YELLOW);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Character buttons panel
        JPanel charPanel = new JPanel(new GridLayout(availableChars.size(), 1, 10, 10));
        charPanel.setBackground(new Color(30, 30, 30));
        
        final int[] selectedIndex = {-1};
        
        for (int i = 0; i < availableChars.size(); i++) {
            String charName = availableChars.get(i);
            final int index = i;
            
            JPanel charRow = new JPanel(new BorderLayout(10, 0));
            charRow.setBackground(new Color(50, 50, 50));
            charRow.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JButton selectBtn = new JButton(charName);
            selectBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
            selectBtn.setBackground(new Color(70, 130, 180));
            selectBtn.setForeground(Color.WHITE);
            selectBtn.setFocusPainted(false);
            selectBtn.setPreferredSize(new Dimension(120, 40));
            selectBtn.addActionListener(e -> {
                selectedIndex[0] = index;
                dialog.dispose();
            });
            
            JTextArea descArea = new JTextArea(getCharacterDescription(charName));
            descArea.setEditable(false);
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            descArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
            descArea.setBackground(new Color(50, 50, 50));
            descArea.setForeground(Color.LIGHT_GRAY);
            descArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            charRow.add(selectBtn, BorderLayout.WEST);
            charRow.add(descArea, BorderLayout.CENTER);
            
            charPanel.add(charRow);
        }
        
        JScrollPane scrollPane = new JScrollPane(charPanel);
        scrollPane.setBackground(new Color(30, 30, 30));
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
        
        return selectedIndex[0];
    }
    
    /**
     * Show item selection dialog
     * @return index in availableItems list, or -1 if cancelled
     */
    private static int showItemDialog(JFrame parent, List<String> availableItems, String characterName) {
        JDialog dialog = new JDialog(parent, "Select Item for " + characterName, true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        JLabel titleLabel = new JLabel(characterName + " - Choose Equipment", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(Color.CYAN);
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Item buttons panel
        JPanel itemPanel = new JPanel(new GridLayout(availableItems.size(), 1, 10, 10));
        itemPanel.setBackground(new Color(30, 30, 30));
        
        final int[] selectedIndex = {-1};
        
        for (int i = 0; i < availableItems.size(); i++) {
            String itemName = availableItems.get(i);
            final int index = i;
            
            JPanel itemRow = new JPanel(new BorderLayout(10, 0));
            itemRow.setBackground(new Color(50, 50, 50));
            itemRow.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            JButton selectBtn = new JButton(itemName);
            selectBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
            selectBtn.setBackground(new Color(100, 180, 100));
            selectBtn.setForeground(Color.WHITE);
            selectBtn.setFocusPainted(false);
            selectBtn.setPreferredSize(new Dimension(120, 40));
            selectBtn.addActionListener(e -> {
                selectedIndex[0] = index;
                dialog.dispose();
            });
            
            JTextArea descArea = new JTextArea(getItemDescription(itemName));
            descArea.setEditable(false);
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            descArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
            descArea.setBackground(new Color(50, 50, 50));
            descArea.setForeground(Color.LIGHT_GRAY);
            descArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            itemRow.add(selectBtn, BorderLayout.WEST);
            itemRow.add(descArea, BorderLayout.CENTER);
            
            itemPanel.add(itemRow);
        }
        
        JScrollPane scrollPane = new JScrollPane(itemPanel);
        scrollPane.setBackground(new Color(30, 30, 30));
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        dialog.setContentPane(mainPanel);
        dialog.setVisible(true);
        
        return selectedIndex[0];
    }
    
    private static String getCharacterDescription(String charName) {
        switch (charName) {
            case "Knight": return KNIGHT_DESC;
            case "Robot": return ROBOT_DESC;
            case "Witch": return WITCH_DESC;
            default: return "";
        }
    }
    
    private static String getItemDescription(String itemName) {
        switch (itemName) {
            case "Shield": return SHIELD_DESC;
            case "Potion": return POTION_DESC;
            case "Knife": return KNIFE_DESC;
            case "Boots": return BOOTS_DESC;
            case "Blow Dart": return DART_DESC;
            default: return "";
        }
    }
}
