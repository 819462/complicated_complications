import java.io.*;

public class ReadIn implements Runnable {
    private BufferedReader reader;
    private GUICentral gui;

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
        if (line.contains("Choose character")) {
            gui.showCharacterSelect();
        } else if (line.contains("choose item:")) {
            gui.showItemSelect();
        } else if (line.contains("Planning for")) {
            gui.showActionButtons(line);
        } else if (line.contains("Target (1 or 2):")) {
            gui.showTargetButtons();
        } else if (line.contains("Blow Dart target (1 or 2):")) {
            gui.showTargetButtons();
        } else if (line.contains("Press Enter to continue")) {
            gui.showContinueButton();
        } else if (line.contains("VICTORY")) {
            gui.showEndScreen(true);
        } else if (line.contains("GAME OVER")) {
            gui.showEndScreen(false);
        } else if (line.contains("=== STATUS ===")) {
            gui.updateStatus();
        } else {
            gui.log(line);
        }
    }
}
