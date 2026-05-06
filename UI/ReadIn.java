// ReadIn.java
import java.io.*;
import java.util.*;

public class ReadIn implements Runnable {
    private BufferedReader reader;
    private GUICentral gui;

    private String collectMode = ""; // "chars" or "items"
    private List<String> collectedOptions = new ArrayList<>();

    public ReadIn(InputStream in, GUICentral gui) {
        reader = new BufferedReader(new InputStreamReader(in));
        this.gui = gui;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                process(line);
            }
        } catch (IOException e) {
            gui.log("Connection to game lost.");
        }
    }

    private void process(String line) {

        // === HP STATUS LINES ===
        if (line.contains("YOUR TEAM:")) {
            gui.updatePlayerHP(line);
            return;
        }
        if (line.contains("ENEMY TEAM:")) {
            gui.updateEnemyHP(line);
            return;
        }

        // === CHARACTER SELECT ===
        if (line.contains("Choose character")) {
            collectMode = "chars";
            collectedOptions.clear();
            gui.log(line);
            return;
        }

        // === ITEM SELECT ===
        if (line.contains("choose item:")) {
            collectMode = "items";
            collectedOptions.clear();
            gui.log(line);
            return;
        }

        // === COLLECT NUMBERED OPTIONS (1), 2), 3)...) ===
        if (!collectMode.isEmpty() && line.trim().matches("^[1-9][0-9]*\\).*")) {
            collectedOptions.add(line.trim());
            gui.log(line);
            return;
        }

        // === "0) Description" = all options collected, show buttons ===
        if (line.contains("0) Description") && !collectMode.isEmpty()) {
            gui.log(line);
            if (collectMode.equals("chars")) {
                gui.showCharacterSelect(new ArrayList<>(collectedOptions));
            } else {
                gui.showItemSelect(new ArrayList<>(collectedOptions));
            }
            collectMode = "";
            return;
        }

        // === PLANNING PHASE ===
        if (line.contains("Planning for")) {
            gui.log(line);
            gui.showActionButtons();
            return;
        }
        // Suppress the "1) Attack 2) Ultimate..." line — buttons already shown
        if (line.contains("1) Attack")) {
            return;
        }

        // === TARGET SELECT ===
        if (line.contains("Target (1 or 2):") || line.contains("Blow Dart target (1 or 2):")) {
            gui.showTargetButtons();
            return;
        }

        // === CONTINUE BUTTON ===
        if (line.contains("Press Enter to continue")) {
            gui.showContinueButton();
            return;
        }

        // === END SCREENS ===
        if (line.contains("VICTORY")) {
            gui.log(line);
            gui.showEndScreen(true);
            return;
        }
        if (line.contains("GAME OVER")) {
            gui.log(line);
            gui.showEndScreen(false);
            return;
        }

        // === SKIP DECORATIVE LINES & BLANKS ===
        if (line.isBlank() || line.matches(".*[╔╚║╗╝═]+.*")) {
            return;
        }

        // === DEFAULT: LOG IT ===
        gui.log(line);
    }
}
