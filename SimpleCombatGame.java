import java.util.*;

public class SimpleCombatGame {
    static Scanner sc = new Scanner(System.in);
    static Random rand = new Random();

    static class Character {
        String name;
        int maxHp, hp, speed, ultCharge, ultMax;
        String ultName, item;
        boolean isAlive, protectedThisTurn, counterMode;
        int poisonTurns;
        int knifeTurns;
        int dartTurns;
        int blowDartUsesLeft;
        int plannedAction;          // 1=Attack, 2=Ultimate, 3=Item, 4=Nothing
        Character plannedTarget;
        Character plannedBlowDartTarget;

        Character(String n, int h, int s, int uMax, String u) {
            name = n; 
            maxHp = hp = h; 
            speed = s; 
            ultMax = uMax; 
            ultName = u;
            ultCharge = 0; 
            isAlive = true; 
            protectedThisTurn = false;
            counterMode = false;
            poisonTurns = knifeTurns = dartTurns = 0;
            blowDartUsesLeft = 0;
            item = "";
            plannedAction = 0;
            plannedTarget = null;
            plannedBlowDartTarget = null;
        }
    }

    static void clearScreen() {
        for (int i = 0; i < 50; i++) System.out.println();
    }

    static void pause() {
        System.out.println("\nPress Enter to continue...");
        sc.nextLine();
    }

    public static void main(String[] args) {
        System.out.println("=== TURN-BASED COMBAT GAME ===\n");

        Character knight = new Character("Knight", 250, 2, 2, "Counter");
        Character robot  = new Character("Robot",  300, 1, 3, "Rocket");
        Character witch  = new Character("Witch",  200, 3, 5, "Revive");

        Character[] originals = {knight, robot, witch};

        // PLAYER CHARACTER SELECTION
        Character[] playerTeam = new Character[2];
        List<Character> availableChars = new ArrayList<>(Arrays.asList(originals));

        for (int i = 0; i < 2; i++) {
            while (true) {
                System.out.println("\nChoose character " + (i+1) + " for your team:");
                for (int j = 0; j < availableChars.size(); j++) {
                    System.out.println((j+1) + ") " + availableChars.get(j).name);
                }
                System.out.println("0) Description");
                int choice = sc.nextInt();
                if (choice == 0) {
                    showCharacterDescriptions();
                    continue;
                } else if (choice >= 1 && choice <= availableChars.size()) {
                    playerTeam[i] = availableChars.remove(choice - 1);
                    break;
                } else {
                    System.out.print("Invalid choice. Try again: ");
                }
            }
        }

        // AI picks 2 different random
        Character[] aiTeam = new Character[2];
        int a1 = rand.nextInt(3), a2 = rand.nextInt(3);
        while (a2 == a1) a2 = rand.nextInt(3);
        aiTeam[0] = copy(originals[a1]);
        aiTeam[1] = copy(originals[a2]);

        // Apply O's and X's naming
        for (Character c : playerTeam) c.name = "O's " + c.name;
        for (Character c : aiTeam)   c.name = "X's " + c.name;

        // === DISPLAY TEAMS ===
        System.out.println("\n=== TEAMS ===");
        System.out.println("YOUR TEAM:");
        for (int i = 0; i < 2; i++) {
            System.out.println("  " + (i+1) + ". " + playerTeam[i].name + 
                             " (HP: " + playerTeam[i].hp + ", Speed: " + playerTeam[i].speed + ")");
        }
        System.out.println("ENEMY TEAM:");
        for (int i = 0; i < 2; i++) {
            System.out.println("  " + (i+1) + ". " + aiTeam[i].name + 
                             " (HP: " + aiTeam[i].hp + ", Speed: " + aiTeam[i].speed + ")");
        }
        System.out.println("==============================\n");

        // ITEM SELECTION
        String[] allItems = {"Shield", "Potion", "Knife", "Boots", "Blow Dart"};
        List<String> availableItems = new ArrayList<>(Arrays.asList(allItems));

        for (Character p : playerTeam) {
            while (true) {
                System.out.println(p.name + " - choose item:");
                for (int i = 0; i < availableItems.size(); i++) {
                    System.out.println((i+1) + ") " + availableItems.get(i));
                }
                System.out.println("0) Description");
                
                int choice = sc.nextInt();
                if (choice == 0) {
                    showItemDescriptions();
                    continue;
                } else if (choice >= 1 && choice <= availableItems.size()) {
                    p.item = availableItems.remove(choice - 1);
                    if (p.item.equals("Blow Dart")) p.blowDartUsesLeft = 3;
                    System.out.println(p.name + " equipped " + p.item + "\n");
                    break;
                } else {
                    System.out.print("Invalid choice. Try again: ");
                }
            }
        }

        // ====================== MAIN BATTLE LOOP ======================
        while (true) {
            if (bothDead(playerTeam)) { 
                clearScreen();
                printStatus(playerTeam, aiTeam);
                System.out.println("\n⚔️ GAME OVER - You lost... ⚔️"); 
                break; 
            }
            if (bothDead(aiTeam)) { 
                clearScreen();
                printStatus(playerTeam, aiTeam);
                System.out.println("\n⚔️ VICTORY - You won! ⚔️"); 
                break; 
            }

            // === PLANNING PHASE ===
            clearScreen();
            printStatus(playerTeam, aiTeam);
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║     PLANNING PHASE - Choose Actions      ║");
            System.out.println("╚══════════════════════════════════════════╝");
            planPlayerActions(playerTeam, aiTeam);
            planAIActions(aiTeam, playerTeam);

            sc.nextLine(); // consume newline
            pause();

            // === BATTLE PHASE ===
            clearScreen();
            printStatus(playerTeam, aiTeam);
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║            BATTLE PHASE                  ║");
            System.out.println("╚══════════════════════════════════════════╝\n");
            Character[] order = getTurnOrder(playerTeam, aiTeam);

            for (Character c : order) {
                if (!c.isAlive) continue;
                c.protectedThisTurn = false;
                c.counterMode = false;

                // Blow Dart 50% fail chance
                if (c.dartTurns > 0) {
                    if (rand.nextBoolean()) {
                        System.out.println(c.name + "'s action failed due to Blow Dart!");
                        c.dartTurns--;
                        continue;
                    } else {
                        c.dartTurns--;
                    }
                }

                executePlannedAction(c, playerTeam, aiTeam);
            }

            sc.nextLine(); // consume newline
            pause();

            System.out.println("\n--- End of Turn ---");
            tickStatuses(playerTeam);
            tickStatuses(aiTeam);
            chargeUltimates(playerTeam);
            chargeUltimates(aiTeam);

            sc.nextLine();
            pause();
        }
        sc.close();
    }

    // ==================== PLANNING METHODS ====================
    static void planPlayerActions(Character[] playerTeam, Character[] enemyTeam) {
        for (Character p : playerTeam) {
            if (p.isAlive) {
                planSinglePlayerAction(p, enemyTeam, playerTeam);
            }
        }
    }

    static void planSinglePlayerAction(Character p, Character[] enemies, Character[] ownTeam) {
        p.plannedAction = 0;
        p.plannedTarget = null;
        p.plannedBlowDartTarget = null;

        while (true) {
            System.out.println("\n→ Planning for " + p.name + " (" + p.hp + " HP) | Ult: " + p.ultCharge + "/" + p.ultMax);
            if (p.blowDartUsesLeft > 0) System.out.println("Blow Dart uses left: " + p.blowDartUsesLeft);

            System.out.println("1) Attack  2) Ultimate  3) Item  4) Nothing  5) Description");
            int choice = sc.nextInt();

            if (choice == 5) {
                showHelpMenu();
                continue;
            }
            if (choice == 1) {
                System.out.print("Target (1 or 2): ");
                int t = sc.nextInt() - 1;
                if (t >= 0 && t < 2 && enemies[t].isAlive) {
                    p.plannedAction = 1;
                    p.plannedTarget = enemies[t];
                    break;
                }
            } else if (choice == 2 && p.ultCharge >= p.ultMax) {
                p.plannedAction = 2;
                break;
            } else if (choice == 3 && !p.item.isEmpty()) {
                // For Blow Dart, select target now
                if (p.item.equals("Blow Dart") && p.blowDartUsesLeft > 0) {
                    System.out.print("Blow Dart target (1 or 2): ");
                    int t = sc.nextInt() - 1;
                    if (t >= 0 && t < 2 && enemies[t].isAlive) {
                        p.plannedBlowDartTarget = enemies[t];
                        p.plannedAction = 3;
                        break;
                    } else {
                        System.out.println("Invalid target.");
                        continue;
                    }
                }
                p.plannedAction = 3;
                break;
            } else if (choice == 4) {
                p.plannedAction = 4;
                break;
            } else {
                System.out.println("Invalid choice.");
            }
        }
    }

    static void planAIActions(Character[] aiTeam, Character[] playerTeam) {
        for (Character e : aiTeam) {
            if (e.isAlive) {
                planSingleAIAction(e, playerTeam, aiTeam);
            }
        }
    }

    static void planSingleAIAction(Character e, Character[] players, Character[] ownTeam) {
        e.plannedAction = 0;
        e.plannedTarget = null;

        Character target = players[rand.nextInt(2)];
        while (!target.isAlive) target = players[rand.nextInt(2)];

        if (e.ultCharge >= e.ultMax && rand.nextBoolean()) {
            e.plannedAction = 2;
        } else {
            e.plannedAction = 1;
            e.plannedTarget = target;
        }
    }

    static void executePlannedAction(Character c, Character[] players, Character[] enemies) {
        Character[] enemySide = isPlayer(c, players) ? enemies : players;
        Character[] ownSide   = isPlayer(c, players) ? players : enemies;

        switch (c.plannedAction) {
            case 1: // Attack
                if (c.plannedTarget != null && c.plannedTarget.isAlive) {
                    attack(c, c.plannedTarget);
                }
                break;
            case 2: // Ultimate
                useUltimate(c, enemySide, ownSide);
                c.ultCharge = 0;
                break;
            case 3: // Item
                useItem(c, enemySide);
                break;
            case 4:
                System.out.println(c.name + " does nothing.");
                break;
        }
    }

    // ==================== DESCRIPTION MENUS ====================
    static void showCharacterDescriptions() {
        System.out.println("\n=== CHARACTER DESCRIPTIONS ===");
        System.out.println("Knight - 250 HP, Speed 2, Base Damage: 30, Ult (2 charges): Counter (2.5x reflect on attack)");
        System.out.println("Robot  - 300 HP, Speed 1, Base Damage: 35, Ult (3 charges): Rocket (50 damage to both enemies)");
        System.out.println("Witch  - 200 HP, Speed 3, Base Damage: 20, Ult (5 charges): Revive (dead ally returns with 50 HP, or heal living ally 20 HP)");
        System.out.println("\nPress Enter to return...");
        sc.nextLine(); sc.nextLine();
    }

    static void showItemDescriptions() {
        System.out.println("\n=== ITEM DESCRIPTIONS ===");
        System.out.println("Shield     - Blocks all damage this turn (no counter)");
        System.out.println("Potion     - Heals 40 HP");
        System.out.println("Knife      - +50% damage for 2 turns");
        System.out.println("Boots      - +2 Speed permanently");
        System.out.println("Blow Dart  - Target enemy (3 uses total) | 50% chance their action fails for 3 turns");
        System.out.println("\nPress Enter to return...");
        sc.nextLine(); sc.nextLine();
    }

    static void showHelpMenu() {
        System.out.println("\n=== DESCRIPTION MENU ===");
        System.out.println("1) Characters");
        System.out.println("2) Items");
        int ch = sc.nextInt();
        if (ch == 1) showCharacterDescriptions();
        else if (ch == 2) showItemDescriptions();
    }

    // ==================== TURN ORDER ====================
    static Character[] getTurnOrder(Character[] t1, Character[] t2) {
        List<Character> all = new ArrayList<>();
        for (Character c : t1) if (c.isAlive) all.add(c);
        for (Character c : t2) if (c.isAlive) all.add(c);
        all.sort((a, b) -> {
            if (a.speed != b.speed) return Integer.compare(b.speed, a.speed);
            return Integer.compare(a.hp, b.hp);
        });
        return all.toArray(new Character[0]);
    }

    // ==================== UTILITIES ====================
    static boolean bothDead(Character[] team) {
        return !team[0].isAlive && !team[1].isAlive;
    }

    static boolean isPlayer(Character c, Character[] playerTeam) {
        return c == playerTeam[0] || c == playerTeam[1];
    }

    static void chargeUltimates(Character[] team) {
        for (Character c : team) {
            if (c.isAlive) {
                c.ultCharge++;
                if (c.ultCharge > c.ultMax) c.ultCharge = c.ultMax;
            }
        }
    }

    static Character copy(Character original) {
        Character c = new Character(original.name, original.maxHp, original.speed, original.ultMax, original.ultName);
        c.item = original.item;
        c.blowDartUsesLeft = original.blowDartUsesLeft;
        return c;
    }

    static void tickStatuses(Character[] team) {
        for (Character c : team) {
            if (c.poisonTurns > 0) {
                c.hp -= 10;
                c.poisonTurns--;
                System.out.println(c.name + " takes 10 poison damage!");
            }
            if (c.knifeTurns > 0) c.knifeTurns--;
            if (c.hp <= 0) { 
                c.hp = 0; 
                c.isAlive = false; 
            }
        }
    }

    static void printStatus(Character[] pTeam, Character[] eTeam) {
        System.out.println("\n=== STATUS ===");
        System.out.print("YOUR TEAM: ");
        for (Character c : pTeam) {
            System.out.print(c.name + " (" + c.hp + "/" + c.maxHp + " HP)");
            if (c.poisonTurns > 0) System.out.print(" [Poison:" + c.poisonTurns + "]");
            if (c.knifeTurns > 0) System.out.print(" [Knife:" + c.knifeTurns + "]");
            if (c.dartTurns > 0) System.out.print(" [BlowDart:" + c.dartTurns + "]");
            if (c.protectedThisTurn) System.out.print(" [Shield:1]");
            System.out.print("  ");
        }
        System.out.print("\nENEMY TEAM: ");
        for (Character c : eTeam) {
            System.out.print(c.name + " (" + c.hp + "/" + c.maxHp + " HP)");
            if (c.poisonTurns > 0) System.out.print(" [Poison:" + c.poisonTurns + "]");
            if (c.knifeTurns > 0) System.out.print(" [Knife:" + c.knifeTurns + "]");
            if (c.dartTurns > 0) System.out.print(" [BlowDart:" + c.dartTurns + "]");
            if (c.protectedThisTurn) System.out.print(" [Shield:1]");
            System.out.print("  ");
        }
        System.out.println("\n");
    }

    // ==================== ATTACK ====================
    static void attack(Character attacker, Character target) {
        if (!target.isAlive) return;
        int dmg = getAttackDamage(attacker);

        if (target.protectedThisTurn) {
            if (target.counterMode) {
                int counterDmg = (int)(dmg * 2.5);
                attacker.hp -= counterDmg;
                System.out.println(target.name + " COUNTERS! " + attacker.name + " takes " + counterDmg + " damage!");
                checkDeath(attacker);
            } else {
                System.out.println(target.name + "'s Shield blocks the attack! No damage taken.");
            }
        } else {
            target.hp -= dmg;
            System.out.println(attacker.name + " hits " + target.name + " for " + dmg + " damage!");
            if (attacker.name.contains("Witch")) target.poisonTurns = 3;
            checkDeath(target);
        }
    }

    static int getAttackDamage(Character c) {
        int base = switch (c.name) {
            case "O's Knight", "X's Knight" -> 30;
            case "O's Robot",  "X's Robot"  -> 35;
            case "O's Witch",  "X's Witch"  -> 20;
            default -> 25;
        };
        return (c.knifeTurns > 0) ? (int)(base * 1.5) : base;
    }

    // ==================== ULTIMATES ====================
    static void useUltimate(Character c, Character[] enemies, Character[] ownTeam) {
        System.out.println(c.name + " uses " + c.ultName + "!");
        
        if (c.name.contains("Knight")) {
            c.protectedThisTurn = true;
            c.counterMode = true;
            System.out.println(c.name + " prepares to counter! Next attack will be reflected 2.5x!");
        } else if (c.name.contains("Robot")) {
            for (Character t : enemies) if (t.isAlive) { 
                t.hp -= 50; 
                System.out.println("Rocket hits " + t.name + " for 50 damage!");
                checkDeath(t); 
            }
        } else if (c.name.contains("Witch")) {
            // First check for dead allies
            for (Character t : ownTeam) {
                if (!t.isAlive) {
                    t.hp = 50;
                    t.isAlive = true;
                    t.poisonTurns = t.knifeTurns = t.dartTurns = 0;
                    System.out.println(t.name + " has been revived with 50 HP!");
                    return;
                }
            }
            // If no dead allies, heal a living one for 20 HP
            for (Character t : ownTeam) {
                if (t.isAlive && t != c) {
                    t.hp += 20;
                    if (t.hp > t.maxHp) t.hp = t.maxHp;
                    System.out.println(t.name + " is healed for 20 HP!");
                    return;
                }
            }
            // If only the witch is alive, heal self
            c.hp += 20;
            if (c.hp > c.maxHp) c.hp = c.maxHp;
            System.out.println(c.name + " heals self for 20 HP!");
        }
    }

    // ==================== ITEMS ====================
    static void useItem(Character user, Character[] enemies) {
        System.out.println(user.name + " uses " + user.item + "!");
        switch (user.item) {
            case "Potion" -> { 
                user.hp += 40; 
                if (user.hp > user.maxHp) user.hp = user.maxHp;
                System.out.println(user.name + " heals for 40 HP!");
            }
            case "Shield" -> {
                user.protectedThisTurn = true;
                System.out.println(user.name + " raises their shield!");
            }
            case "Knife"  -> {
                user.knifeTurns = 2;
                System.out.println(user.name + " sharpens their knife! +50% damage for 2 turns!");
            }
            case "Boots"  -> {
                user.speed += 2;
                System.out.println(user.name + " puts on boots! Speed increased by 2 permanently!");
            }
            case "Blow Dart" -> {
                if (user.blowDartUsesLeft > 0 && user.plannedBlowDartTarget != null && user.plannedBlowDartTarget.isAlive) {
                    user.plannedBlowDartTarget.dartTurns = 3;
                    System.out.println("Blow Dart hits " + user.plannedBlowDartTarget.name + "! 50% action failure for 3 turns!");
                    user.blowDartUsesLeft--;
                    if (user.blowDartUsesLeft == 0) {
                        System.out.println("Blow Dart has broken!");
                        user.item = "";
                    }
                }
            }
        }
        if (!user.item.equals("Boots") && !user.item.equals("Blow Dart")) user.item = "";
    }

    static void checkDeath(Character c) {
        if (c.hp <= 0) { 
            c.hp = 0; 
            c.isAlive = false; 
            System.out.println(c.name + " has been defeated!"); 
        }
    }
}


