import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.*;
import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.PixelAnalyzer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.input.MenuHook;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.trackers.experience.XPTracker;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import java.util.regex.Pattern;
import javax.swing.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Predicate;
import java.util.Random;

@ScriptDefinition(
        name = "KWoodcutter",
        author = "Kelvin",
        version = 1.9,
        description = "Advanced woodcutting script, now with Yews!",
        skillCategory = SkillCategory.WOODCUTTING
)
public class KWoodcutter extends Script {
    private final Random rand = new Random();
    public KWoodcutter(Object param) {
        super(param);
    }

    // Default areas
    private static final RectangleArea NORMAL_AREA = new RectangleArea(3145, 3385, 30, 30, 0);
    private static final WorldPosition NORMAL_CENTER = new WorldPosition(3164, 3403, 0);
    private static final RectangleArea OAK_AREA = new RectangleArea(3149, 3404, 30, 30, 0);
    private static final WorldPosition OAK_CENTER = new WorldPosition(3164, 3419, 0);
    private static final RectangleArea DRAYNOR_WILLOW_AREA = new RectangleArea(3073, 3218, 20, 20, 0);
    private static final WorldPosition DRAYNOR_WILLOW_CENTER = new WorldPosition(3087, 3236, 0);
    private static final RectangleArea BO_WILLOW_AREA = new RectangleArea(2508, 3562, 20, 20, 0);
    private static final WorldPosition BO_WILLOW_CENTER = new WorldPosition(2520, 3578, 0);
    private static final RectangleArea MAPLE_AREA = new RectangleArea(2709, 3474, 20, 20, 0);
    private static final WorldPosition MAPLE_CENTER = new WorldPosition(2731, 3500, 0);
    private static final RectangleArea YEW_AREA = new RectangleArea(2700, 3452, 20, 20, 0); // Centered on 2710,3462
    private static final WorldPosition YEW_CENTER = new WorldPosition(2710, 3462, 0);
    private static final double MAX_DISTANCE_FROM_CENTER = 40.0;

    private static final Set<Integer> AXE_IDS = Set.of(1351, 1349, 1353, 1361, 1355, 1357, 1359, 6739, 13241, 23673, 20011);

    private static final Set<String> NORMAL_NAMES = Set.of("Tree");
    private static final int NORMAL_LOG_ID = 1511;

    private static final Set<String> OAK_NAMES = Set.of("Oak tree");
    private static final int OAK_LOG_ID = 1521;

    private static final Set<String> WILLOW_NAMES = Set.of("Willow tree");
    private static final int WILLOW_LOG_ID = 1519;

    private static final Set<String> MAPLE_NAMES = Set.of("Maple tree");
    private static final int MAPLE_LOG_ID = 1517;

    private static final Set<String> YEW_NAMES = Set.of("Yew tree");
    private static final int YEW_LOG_ID = 1515;

    private static final Set<Integer> CLUE_BOX_IDS = Set.of(19835, 19836, 19837, 19838, 19839, 23164);
    private static final int TINDERBOX_ID = 590;
    private static final int KNIFE_ID = 946;
    private static final Set<Integer> ALL_LOG_IDS = Set.of(1511, 1521, 1519, 1517, 1515);

    // Fletching products
    private static final int ARROW_SHAFT_ID = 52;

    private static final Map<Integer, Integer> SHORTBOW_U_IDS = Map.of(
            NORMAL_LOG_ID, 50,
            OAK_LOG_ID, 54,
            WILLOW_LOG_ID, 60,
            MAPLE_LOG_ID, 64,
            YEW_LOG_ID, 66
    );

    private static final Map<Integer, Integer> LONGBOW_U_IDS = Map.of(
            NORMAL_LOG_ID, 48,
            OAK_LOG_ID, 56,
            WILLOW_LOG_ID, 58,
            MAPLE_LOG_ID, 62,
            YEW_LOG_ID, 68
    );

    // Tree string to log ID
    private static final Map<String, Integer> TREE_TO_LOG_ID = Map.of(
            "Normal trees (Varrock West)", NORMAL_LOG_ID,
            "Oaks (Varrock West)", OAK_LOG_ID,
            "Willows (Draynor Village)", WILLOW_LOG_ID,
            "Willows (Barbarian Outpost)", WILLOW_LOG_ID,
            "Maples (Seers Village)", MAPLE_LOG_ID,
            "Yews (Seers Village)", YEW_LOG_ID
    );

    // Fletching levels (added Yew)
    private static final Map<Integer, Map<String, Integer>> FLETCH_LEVEL_MAP = Map.of(
            NORMAL_LOG_ID, Map.of("Arrow Shafts", 1, "Shortbow (u)", 5, "Longbow (u)", 10),
            OAK_LOG_ID, Map.of("Arrow Shafts", 15, "Shortbow (u)", 20, "Longbow (u)", 25),
            WILLOW_LOG_ID, Map.of("Arrow Shafts", 30, "Shortbow (u)", 35, "Longbow (u)", 40),
            MAPLE_LOG_ID, Map.of("Arrow Shafts", 45, "Shortbow (u)", 50, "Longbow (u)", 55),
            YEW_LOG_ID, Map.of("Arrow Shafts", 60, "Shortbow (u)", 65, "Longbow (u)", 70)
    );

    // WC levels (added Yew)
    private static final Map<String, Integer> TREE_WC_LEVEL_MAP = Map.of(
            "Normal trees (Varrock West)", 1,
            "Oaks (Varrock West)", 15,
            "Willows (Draynor Village)", 30,
            "Willows (Barbarian Outpost)", 30,
            "Maples (Seers Village)", 45,
            "Yews (Seers Village)", 60
    );

    private Set<String> currentTreeNames;
    private int currentLogID;
    private RectangleArea currentTreeArea;
    private WorldPosition currentTreeCenter;
    private String currentTreeType;

    private String mode = "Power chop";
    private String fletchOption = "Arrow Shafts";

    private long scriptStartTime;
    private long lastLogCount = 0;
    private long lastLogIncreaseTime = 0;
    private double wcXpPerHour = 0;
    private double secondaryXpPerHour = 0;
    private String currentState = "Initializing";

    private RSObject currentChoppingTree = null;
    private boolean isProcessing = false;
    private volatile boolean guiReady = false;
    private boolean loaded = false;

    private Map<SkillType, XPTracker> xpTrackers;

    private static final String SWING_MESSAGE = "You swing your axe at the tree";
    private static final String FULL_INVENTORY_MESSAGE = "Your inventory is too full to hold any more logs.";
    private static final String LIGHT_FIRE_MESSAGE = "The fire catches and the logs begin to burn.";
    private static final String CANNOT_LIGHT_MESSAGE = "You can't light a fire here.";

    private WorldPosition bonfirePosition = null;
    private long outOfAreaStart = 0;

    // Bonfire state machine
    private enum BonfireState {
        IDLE,
        FIND_FIRE,
        WALK_TO_FIRE,
        LIGHT_FIRE,
        TRIGGER_ADD,
        WAIT_DIALOGUE,
        BURNING,
        STUCK
    }

    private BonfireState bonfireState = BonfireState.IDLE;
    private int previousBurnLogs = 0;
    private long lastLogDecreaseTime = 0;

    // Fletching trigger lock
    private boolean fletchTriggeredThisInv = false;
    private int previousFletchLogs = 0;
    private long lastFletchDecreaseTime = 0;

    private static final Pattern USE_LOG_TO_FIRE = Pattern.compile("^use\\s+(.+?)\\s*->\\s*(fire|forester's campfire)$", Pattern.CASE_INSENSITIVE);

    public void onStart() {
        scriptStartTime = System.currentTimeMillis();
        currentChoppingTree = null;
        isProcessing = false;
        loaded = false;
        currentState = "Initializing";
        bonfirePosition = null;
        outOfAreaStart = 0;
        lastLogIncreaseTime = scriptStartTime;
        lastLogCount = 0;
        bonfireState = BonfireState.IDLE;
        previousBurnLogs = 0;
        lastLogDecreaseTime = 0;
        fletchTriggeredThisInv = false;
        previousFletchLogs = 0;
        lastFletchDecreaseTime = 0;

        xpTrackers = getXPTrackers();

        SwingUtilities.invokeLater(this::createModernGUI);
        while (!guiReady) {
            sleep(500);
        }

        WorldPosition startPos = getWorldPosition();
        if (startPos == null) {
            log("Could not get starting position");
        } else if (!currentTreeArea.contains(startPos)) {
            log("Started outside selected tree area (" + startPos + ") - pathing to center on startup");
            WalkConfig startupConfig = new WalkConfig.Builder()
                    .breakDistance(1)
                    .breakCondition(() -> {
                        WorldPosition current = getWorldPosition();
                        return current != null && currentTreeArea.contains(current);
                    })
                    .build();
            getWalker().walkTo(currentTreeCenter, startupConfig);
            sleep(myRandom(2000, 4000));
        } else {
            log("Started inside selected area - proceeding");
        }

        log("KWoodcutter started - " + mode + " mode, chopping " + currentTreeType);
    }

    private void createModernGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("KWoodcutter");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 650);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new Color(30, 35, 40));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(30, 35, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel titleLabel = new JLabel("KWoodcutter");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Advanced Woodcutting & Firemaking Script");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        subtitleLabel.setForeground(new Color(0, 180, 140));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 20, 40, 20);
        mainPanel.add(subtitleLabel, gbc);

        JLabel treeLabel = new JLabel("Select Tree Type:");
        treeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        treeLabel.setForeground(Color.WHITE);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 20, 10, 20);
        mainPanel.add(treeLabel, gbc);

        String[] trees = {
                "Normal trees (Varrock West)",
                "Oaks (Varrock West)",
                "Willows (Draynor Village)",
                "Willows (Barbarian Outpost)",
                "Maples (Seers Village)",
                "Yews (Seers Village)"
        };
        JComboBox<String> treeCombo = new JComboBox<>(trees);
        treeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        treeCombo.setPreferredSize(new Dimension(400, 50));
        gbc.gridy = 3;
        mainPanel.add(treeCombo, gbc);

        JLabel levelLabel = new JLabel("Required Levels: Woodcutting - ? | Firemaking - ?");
        levelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        levelLabel.setForeground(new Color(180, 180, 180));
        gbc.gridy = 4;
        gbc.insets = new Insets(10, 20, 30, 20);
        mainPanel.add(levelLabel, gbc);

        JLabel modeLabel = new JLabel("Select Mode:");
        modeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        modeLabel.setForeground(Color.WHITE);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 20, 10, 20);
        mainPanel.add(modeLabel, gbc);

        String[] modes = {"Power chop", "Bonfire", "Fletching"};
        JComboBox<String> modeCombo = new JComboBox<>(modes);
        modeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        modeCombo.setPreferredSize(new Dimension(400, 50));
        gbc.gridy = 6;
        mainPanel.add(modeCombo, gbc);

        JLabel fletchLabel = new JLabel("Fletch Option:");
        fletchLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        fletchLabel.setForeground(Color.WHITE);
        gbc.gridy = 7;
        gbc.insets = new Insets(20, 20, 10, 20);
        mainPanel.add(fletchLabel, gbc);

        String[] fletchOptions = {"Arrow Shafts", "Shortbow (u)", "Longbow (u)"};
        JComboBox<String> fletchCombo = new JComboBox<>(fletchOptions);
        fletchCombo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        fletchCombo.setPreferredSize(new Dimension(400, 50));
        gbc.gridy = 8;
        mainPanel.add(fletchCombo, gbc);

        fletchLabel.setVisible(false);
        fletchCombo.setVisible(false);

        JButton startBtn = new JButton("START SCRIPT");
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 32));
        startBtn.setForeground(Color.BLACK);
        startBtn.setBackground(new Color(0, 180, 140));
        startBtn.setPreferredSize(new Dimension(400, 80));
        startBtn.setBorder(BorderFactory.createLineBorder(new Color(0, 140, 110), 4));
        startBtn.setFocusPainted(false);
        startBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                startBtn.setBackground(new Color(0, 220, 160));
            }
            public void mouseExited(MouseEvent evt) {
                startBtn.setBackground(new Color(0, 180, 140));
            }
        });

        startBtn.addActionListener(e -> {
            String selectedTree = (String) treeCombo.getSelectedItem();
            mode = (String) modeCombo.getSelectedItem();
            if (mode.equals("Fletching")) {
                fletchOption = (String) fletchCombo.getSelectedItem();
            }

            currentLogID = TREE_TO_LOG_ID.get(selectedTree);

            if (selectedTree.contains("Normal trees")) {
                currentTreeNames = NORMAL_NAMES;
                currentTreeArea = NORMAL_AREA;
                currentTreeCenter = NORMAL_CENTER;
                currentTreeType = "Normal Trees";
            } else if (selectedTree.contains("Oaks")) {
                currentTreeNames = OAK_NAMES;
                currentTreeArea = OAK_AREA;
                currentTreeCenter = OAK_CENTER;
                currentTreeType = "Oaks";
            } else if (selectedTree.contains("Willows")) {
                currentTreeNames = WILLOW_NAMES;
                currentTreeArea = selectedTree.contains("Draynor") ? DRAYNOR_WILLOW_AREA : BO_WILLOW_AREA;
                currentTreeCenter = selectedTree.contains("Draynor") ? DRAYNOR_WILLOW_CENTER : BO_WILLOW_CENTER;
                currentTreeType = selectedTree.contains("Draynor") ? "Willows (Draynor)" : "Willows (BO)";
            } else if (selectedTree.contains("Maples")) {
                currentTreeNames = MAPLE_NAMES;
                currentTreeArea = MAPLE_AREA;
                currentTreeCenter = MAPLE_CENTER;
                currentTreeType = "Maples";
            } else if (selectedTree.contains("Yews")) {
                currentTreeNames = YEW_NAMES;
                currentTreeArea = YEW_AREA;
                currentTreeCenter = YEW_CENTER;
                currentTreeType = "Yews";
            }

            guiReady = true;
            frame.dispose();
        });

        gbc.gridy = 9;
        gbc.insets = new Insets(30, 20, 20, 20);
        mainPanel.add(startBtn, gbc);

        // Update level label live
        Runnable updateLevels = () -> {
            String selectedTree = (String) treeCombo.getSelectedItem();
            String selectedMode = (String) modeCombo.getSelectedItem();
            boolean isFletch = selectedMode.equals("Fletching");
            fletchLabel.setVisible(isFletch);
            fletchCombo.setVisible(isFletch);

            int wcLevel = TREE_WC_LEVEL_MAP.getOrDefault(selectedTree, 1);

            if (isFletch) {
                String option = (String) fletchCombo.getSelectedItem();
                int logID = TREE_TO_LOG_ID.get(selectedTree);
                int fletchLevel = FLETCH_LEVEL_MAP.getOrDefault(logID, Map.of()).getOrDefault(option, 1);
                levelLabel.setText("Required Levels: Woodcutting " + wcLevel + " | Fletching " + fletchLevel);
            } else if (selectedMode.equals("Bonfire")) {
                levelLabel.setText("Required Levels: Woodcutting " + wcLevel + " | Firemaking " + wcLevel);
            } else {
                levelLabel.setText("Required Level: Woodcutting " + wcLevel);
            }
        };

        treeCombo.addActionListener(e -> updateLevels.run());
        modeCombo.addActionListener(e -> updateLevels.run());
        fletchCombo.addActionListener(e -> updateLevels.run());

        updateLevels.run();

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private int myRandom(int min, int max) {
        return min + rand.nextInt(max - min + 1);
    }

    private Polygon getBonfireTilePoly() {
        if (bonfirePosition == null) return null;
        Polygon poly = getSceneProjector().getTilePoly(bonfirePosition, true);
        if (poly == null) return null;
        poly = poly.getResized(0.8);
        return poly;
    }

    private RSObject findNearbyFire(int maxDistance) {
        WorldPosition myPos = getWorldPosition();
        if (myPos == null) return null;

        List<RSObject> fires = getObjectManager().getObjects(obj -> {
            String name = obj.getName();
            if (name == null) return false;
            String lower = name.toLowerCase();
            return lower.contains("fire") && obj.distance(myPos) <= maxDistance;
        });

        if (fires.isEmpty()) return null;

        Optional<RSObject> forester = fires.stream()
                .filter(f -> f.getName().toLowerCase().contains("forester"))
                .min(Comparator.comparingDouble(f -> f.distance(myPos)));

        return forester.orElse(fires.stream()
                .min(Comparator.comparingDouble(f -> f.distance(myPos)))
                .orElse(null));
    }

    private int getFletchProductID() {
        if (fletchOption.equals("Arrow Shafts")) {
            return ARROW_SHAFT_ID;
        }
        if (fletchOption.equals("Shortbow (u)")) {
            return SHORTBOW_U_IDS.getOrDefault(currentLogID, 50);
        }
        return LONGBOW_U_IDS.getOrDefault(currentLogID, 48);
    }

    private int getCurrentLogCount() {
        ItemGroupResult result = getWidgetManager().getInventory().search(Set.of(currentLogID));
        return result != null ? result.getAmount(currentLogID) : 0;
    }

    @Override
    public int poll() {
        try {
            if (!loaded) {
                currentState = "Initializing";
                WorldPosition pos = getWorldPosition();
                if (pos == null) return myRandom(1000, 2000);
                loaded = true;
                lastLogCount = getCurrentLogCount();
                lastLogIncreaseTime = System.currentTimeMillis();
                currentState = "Chopping";
                return myRandom(600, 1200);
            }

            int currentLogs = getCurrentLogCount();
            if (currentLogs > lastLogCount) {
                lastLogIncreaseTime = System.currentTimeMillis();
            }
            lastLogCount = currentLogs;

            long elapsed = System.currentTimeMillis() - scriptStartTime;
            long elapsedSeconds = elapsed / 1000;

            // Real-time XP from XPTracker map (SkillType enum keys)
            XPTracker wcTracker = xpTrackers.get(SkillType.WOODCUTTING);
            wcXpPerHour = wcTracker != null ? wcTracker.getXpPerHour() : 0;

            if (mode.equals("Bonfire")) {
                XPTracker fmTracker = xpTrackers.get(SkillType.FIREMAKING);
                secondaryXpPerHour = fmTracker != null ? fmTracker.getXpPerHour() : 0;
            } else if (mode.equals("Fletching")) {
                XPTracker fletchTracker = xpTrackers.get(SkillType.FLETCHING);
                secondaryXpPerHour = fletchTracker != null ? fletchTracker.getXpPerHour() : 0;
            } else {
                secondaryXpPerHour = 0;
            }

            ItemGroupResult fullInv = getWidgetManager().getInventory().search(Collections.emptySet());
            ItemGroupResult axeResult = getWidgetManager().getInventory().search(AXE_IDS);
            ItemGroupResult tinderResult = getWidgetManager().getInventory().search(Set.of(TINDERBOX_ID));
            ItemGroupResult knifeResult = getWidgetManager().getInventory().search(Set.of(KNIFE_ID));
            ItemGroupResult logResult = getWidgetManager().getInventory().search(Set.of(currentLogID));
            ItemGroupResult clueResult = getWidgetManager().getInventory().search(CLUE_BOX_IDS);

            int axeCount = 0;
            if (axeResult != null) {
                for (int id : AXE_IDS) {
                    axeCount += axeResult.getAmount(id);
                }
            }

            boolean hasAxe = axeCount > 0;
            boolean hasTinderbox = tinderResult != null && tinderResult.getAmount(TINDERBOX_ID) > 0;
            boolean hasKnife = knifeResult != null && knifeResult.getAmount(KNIFE_ID) > 0;

            int clueCount = 0;
            if (clueResult != null) {
                for (int id : CLUE_BOX_IDS) {
                    clueCount += clueResult.getAmount(id);
                }
            }

            int reservedSlots = hasAxe ? 1 : 0;
            if (mode.equals("Bonfire") && hasTinderbox) reservedSlots += 1;
            if (mode.equals("Fletching") && hasKnife) reservedSlots += 1;

            if (!isProcessing && (hasChatMessage(FULL_INVENTORY_MESSAGE) ||
                    (fullInv != null && fullInv.getFreeSlots() <= reservedSlots + 1) ||
                    clueCount > 0)) {
                currentState = mode.equals("Bonfire") ? "Bonfire" : mode.equals("Fletching") ? "Fletching" : "Dropping";
                isProcessing = true;
                currentChoppingTree = null;
                bonfireState = BonfireState.FIND_FIRE;
                fletchTriggeredThisInv = false;
                lastLogDecreaseTime = System.currentTimeMillis();
                lastFletchDecreaseTime = System.currentTimeMillis();
            }

            if (isProcessing) {
                if (clueCount > 0) {
                    getWidgetManager().getInventory().dropItems(CLUE_BOX_IDS);
                    return myRandom(600, 1000);
                }

                if (mode.equals("Power chop")) {
                    getWidgetManager().getInventory().dropItems(ALL_LOG_IDS);
                    if (currentLogs == 0) {
                        isProcessing = false;
                        currentState = "Chopping";
                    }
                    return myRandom(600, 1000);
                }

                if (mode.equals("Bonfire")) {
                    currentState = "Bonfire";

                    int logsNow = logResult != null ? logResult.getAmount(currentLogID) : 0;

                    switch (bonfireState) {
                        case FIND_FIRE:
                            if (bonfirePosition != null) {
                                log("Using persistent bonfire at " + bonfirePosition + " - moving to WALK_TO_FIRE");
                                bonfireState = BonfireState.WALK_TO_FIRE;
                                break;
                            }

                            RSObject nearbyFire = findNearbyFire(15);
                            if (nearbyFire != null) {
                                bonfirePosition = nearbyFire.getWorldPosition();
                                log("Found existing fire at " + bonfirePosition + " - moving to WALK_TO_FIRE");
                                bonfireState = BonfireState.WALK_TO_FIRE;
                            } else {
                                log("No existing fire - moving to LIGHT_FIRE");
                                bonfireState = BonfireState.LIGHT_FIRE;
                            }
                            break;

                        case WALK_TO_FIRE:
                            WorldPosition myPos = getWorldPosition();
                            if (myPos == null || myPos.distanceTo(bonfirePosition) > 1) {
                                log("Walking to bonfire at " + bonfirePosition + " (current dist: " + (myPos == null ? "unknown" : myPos.distanceTo(bonfirePosition)) + ")");
                                WalkConfig walkConfig = new WalkConfig.Builder()
                                        .breakDistance(1)
                                        .breakCondition(() -> getWorldPosition().distanceTo(bonfirePosition) <= 1)
                                        .build();
                                getWalker().walkTo(bonfirePosition, walkConfig);
                                sleep(myRandom(1000, 2000));
                                return myRandom(600, 1200);
                            } else {
                                log("Adjacent to bonfire - moving to TRIGGER_ADD");
                                bonfireState = BonfireState.TRIGGER_ADD;
                            }
                            break;

                        case LIGHT_FIRE:
                            if (!hasTinderbox) {
                                log("No tinderbox - dropping logs");
                                getWidgetManager().getInventory().dropItems(ALL_LOG_IDS);
                                isProcessing = false;
                                currentState = "Chopping";
                                bonfireState = BonfireState.IDLE;
                                return myRandom(600, 1200);
                            }

                            WorldPosition lightPos = getWorldPosition();
                            ItemSearchResult tinderbox = tinderResult.getRandomItem(Set.of(TINDERBOX_ID));
                            tinderbox.interact("Use");
                            sleep(myRandom(300, 500));
                            ItemSearchResult logItemLight = logResult.getRandomItem(Set.of(currentLogID));
                            logItemLight.interact();

                            if (pollFramesHuman(() -> hasChatMessage(LIGHT_FIRE_MESSAGE), 12000)) {
                                bonfirePosition = lightPos;
                                log("New bonfire lit at " + bonfirePosition);
                                lastLogDecreaseTime = System.currentTimeMillis();
                                bonfireState = BonfireState.WALK_TO_FIRE;
                            } else if (hasChatMessage(CANNOT_LIGHT_MESSAGE)) {
                                log("Can't light here - moving to center");
                                getWalker().walkTo(currentTreeCenter, new WalkConfig.Builder().breakDistance(1).build());
                                sleep(myRandom(3000, 5000));
                            }
                            return myRandom(5000, 8000);

                        case TRIGGER_ADD:
                            ItemSearchResult logItem = logResult.getRandomItem(Set.of(currentLogID));
                            logItem.interact("Use");
                            sleep(myRandom(400, 700));

                            Polygon tilePoly = getBonfireTilePoly();
                            if (tilePoly != null) {
                                MenuHook fireHook = e -> e.stream()
                                        .filter(entry -> entry.getRawText() != null && USE_LOG_TO_FIRE.matcher(entry.getRawText()).matches())
                                        .findFirst()
                                        .orElse(null);

                                if (getFinger().tapGameScreen(tilePoly, fireHook)) {
                                    log("Triggered add-to-bonfire - moving to WAIT_DIALOGUE");
                                    bonfireState = BonfireState.WAIT_DIALOGUE;
                                } else {
                                    log("Tap failed - retrying");
                                }
                            }
                            return myRandom(1200, 2000);

                        case WAIT_DIALOGUE:
                            if (getWidgetManager().getDialogue().getDialogueType() == null) {
                                log("No dialogue - assuming burn started, moving to BURNING");
                                bonfireState = BonfireState.BURNING;
                            } else if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION) {
                                if (getWidgetManager().getDialogue().selectItem(currentLogID)) {
                                    log("Auto-selected log in dialogue - moving to BURNING");
                                    bonfireState = BonfireState.BURNING;
                                }
                                return myRandom(600, 1000);
                            }
                            return myRandom(800, 1400);

                        case BURNING:
                            if (logsNow < previousBurnLogs) {
                                lastLogDecreaseTime = System.currentTimeMillis();
                                previousBurnLogs = logsNow;
                            }

                            if (logsNow == 0) {
                                isProcessing = false;
                                currentState = "Chopping";
                                bonfireState = BonfireState.IDLE;
                                log("All logs burned - resuming chopping (persistent fire kept at " + bonfirePosition + ")");
                                return myRandom(600, 1200);
                            }

                            if (System.currentTimeMillis() - lastLogDecreaseTime > 90000) {
                                log("Stuck burning - moving to STUCK");
                                bonfireState = BonfireState.STUCK;
                            }

                            return myRandom(3000, 6000);

                        case STUCK:
                            log("Stuck recovery - clearing bonfire position and relighting fresh");
                            bonfirePosition = null;
                            bonfireState = BonfireState.FIND_FIRE;
                            lastLogDecreaseTime = System.currentTimeMillis();
                            break;

                        case IDLE:
                            break;
                    }

                    return myRandom(600, 1200);
                }

                if (mode.equals("Fletching")) {
                    currentState = "Fletching";

                    int logsNow = logResult != null ? logResult.getAmount(currentLogID) : 0;

                    if (clueCount > 0) {
                        getWidgetManager().getInventory().dropItems(CLUE_BOX_IDS);
                        return myRandom(600, 1000);
                    }

                    if (!hasKnife) {
                        log("No knife - dropping logs");
                        getWidgetManager().getInventory().dropItems(Set.of(currentLogID));
                        isProcessing = false;
                        currentState = "Chopping";
                        return myRandom(600, 1200);
                    }

                    if (logsNow == 0) {
                        if (!fletchOption.equals("Arrow Shafts")) {
                            int bowID = fletchOption.equals("Shortbow (u)") ? SHORTBOW_U_IDS.getOrDefault(currentLogID, 50)
                                    : LONGBOW_U_IDS.getOrDefault(currentLogID, 48);
                            ItemGroupResult bows = getWidgetManager().getInventory().search(Set.of(bowID));
                            if (bows != null && bows.getAmount(bowID) > 0) {
                                log("Dropping all " + bows.getAmount(bowID) + " " + fletchOption);
                                getWidgetManager().getInventory().dropItems(Set.of(bowID));
                                sleep(myRandom(800, 1200));
                            }
                        }

                        isProcessing = false;
                        currentState = "Chopping";
                        fletchTriggeredThisInv = false;
                        log("All logs fletched - resuming chopping");
                        return myRandom(600, 1200);
                    }

                    if (logsNow < previousFletchLogs) {
                        lastFletchDecreaseTime = System.currentTimeMillis();
                        previousFletchLogs = logsNow;
                    }

                    if (fletchTriggeredThisInv && System.currentTimeMillis() - lastFletchDecreaseTime > 60000) {
                        log("No fletching progress 60s - retrying trigger");
                        fletchTriggeredThisInv = false;
                    }

                    if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION) {
                        int productID = getFletchProductID();
                        if (getWidgetManager().getDialogue().selectItem(productID)) {
                            log("Selected " + fletchOption + " in fletching dialogue - making all");
                            fletchTriggeredThisInv = true;
                        } else {
                            log("Failed to select " + fletchOption + " - fallback to first option");
                            getWidgetManager().getDialogue().selectItem(currentLogID);
                        }
                        return myRandom(600, 1000);
                    }

                    if (!fletchTriggeredThisInv) {
                        ItemSearchResult knife = knifeResult.getRandomItem(Set.of(KNIFE_ID));
                        ItemSearchResult logItem = logResult.getRandomItem(Set.of(currentLogID));
                        knife.interact("Use");
                        sleep(myRandom(200, 400));
                        logItem.interact();
                        sleep(myRandom(600, 800));
                        log("Triggered fletching - waiting for dialogue to select " + fletchOption);
                    }

                    return myRandom(3000, 6000);
                }
            }

            currentState = "Chopping";

            WorldPosition myPos = getWorldPosition();
            if (myPos == null) return myRandom(600, 1200);

            if (myPos.distanceTo(currentTreeCenter) > MAX_DISTANCE_FROM_CENTER) {
                log(String.format("Too far from center (%.0f tiles) - stopping", myPos.distanceTo(currentTreeCenter)));
                stop();
                return 0;
            }

            if (!currentTreeArea.contains(myPos)) {
                if (outOfAreaStart == 0) outOfAreaStart = System.currentTimeMillis();
                else if (System.currentTimeMillis() - outOfAreaStart > 20000) {
                    log("Out of area 20s - walking to center");
                    getWalker().walkTo(currentTreeCenter, new WalkConfig.Builder().breakDistance(1).build());
                    outOfAreaStart = 0;
                    sleep(myRandom(1000, 2000));
                    return myRandom(800, 1400);
                }
                return myRandom(600, 1200);
            }
            outOfAreaStart = 0;

            if (System.currentTimeMillis() - lastLogIncreaseTime > 20000) {
                log("No progress 20s - resetting tree");
                currentChoppingTree = null;
                lastLogIncreaseTime = System.currentTimeMillis();
            }

            List<RSObject> uncutTrees = getUncutTrees();
            if (uncutTrees.isEmpty()) {
                if (currentTreeArea.contains(myPos)) {
                    log("No trees - waiting load");
                    sleep(myRandom(1000, 2000));
                } else {
                    currentState = "Walking to trees";
                    currentChoppingTree = null;
                    getWalker().walkTo(currentTreeCenter, new WalkConfig.Builder()
                            .breakCondition(() -> !getUncutTrees().isEmpty())
                            .breakDistance(1)
                            .build());
                    sleep(myRandom(1000, 2000));
                }
                return myRandom(800, 1400);
            }

            // If current tree still exists, idle (no tap)
            if (currentChoppingTree != null && uncutTrees.contains(currentChoppingTree)) {
                return myRandom(600, 1200);
            }

            // Tree gone - select new
            currentChoppingTree = null;

            // Select and chop new tree
            RSObject tree = uncutTrees.get(0);
            currentChoppingTree = tree;
            log("Selected tree at " + tree.getWorldPosition() + " dist " + tree.getTileDistance(myPos));

            Polygon hull = tree.getConvexHull();
            if (hull == null) {
                log("No hull - skipping");
                currentChoppingTree = null;
                return myRandom(400, 800);
            }
            hull = hull.getResized(0.8);

            if (!getFinger().tapGameScreen(hull)) {
                log("Tap failed - retrying");
                currentChoppingTree = null;
                return myRandom(400, 800);
            }

            double dist = tree.getTileDistance(myPos);
            if (!pollFramesHuman(() -> tree.getTileDistance(getWorldPosition()) <= 1, (int)(dist * 1000) + 3000)) {
                log("Failed reach - retrying");
                currentChoppingTree = null;
                return myRandom(600, 1200);
            }

            if (!pollFramesHuman(() -> hasChatMessage(SWING_MESSAGE), 5000)) {
                log("No swing - retrying");
                currentChoppingTree = null;
                return myRandom(600, 1200);
            }

            return myRandom(600, 1200);

        } catch (Exception e) {
            log("Poll error: " + e.getMessage());
            return myRandom(1000, 2000);
        }
    }

    private boolean hasChatMessage(String message) {
        UIResultList<String> result = getWidgetManager().getChatbox().getText();
        if (result == null || !result.isFound() || result.size() == 0) return false;
        int checkCount = Math.min(10, result.size());
        for (int i = result.size() - checkCount; i < result.size(); i++) {
            String m = result.get(i);
            if (m != null && m.contains(message)) {
                return true;
            }
        }
        return false;
    }

    private List<RSObject> getUncutTrees() {
        Predicate<RSObject> treePredicate = obj -> {
            if (obj.getName() == null) return false;
            return currentTreeNames.contains(obj.getName()) &&
                    currentTreeArea.contains(obj.getWorldPosition()) &&
                    obj.canReach();
        };

        List<RSObject> trees = getObjectManager().getObjects(treePredicate);

        Map<RSObject, PixelAnalyzer.RespawnCircle> respawnMap = getPixelAnalyzer()
                .getRespawnCircleObjects(trees, PixelAnalyzer.RespawnCircleDrawType.CENTER, 0, 5);

        Set<RSObject> cutTrees = respawnMap.keySet();

        List<RSObject> uncut = trees.stream()
                .filter(t -> !cutTrees.contains(t))
                .filter(RSObject::isInteractableOnScreen)
                .sorted((t1, t2) -> Double.compare(t1.getTileDistance(getWorldPosition()), t2.getTileDistance(getWorldPosition())))
                .toList();

        if (uncut.isEmpty() && !trees.isEmpty()) {
            log("Trees detected but all filtered (stumps or off-screen) - count: " + trees.size());
        }

        return uncut;
    }

    @Override
    public void onPaint(Canvas c) {
        long elapsed = System.currentTimeMillis() - scriptStartTime;
        if (elapsed <= 0) return;

        long elapsedSeconds = elapsed / 1000;
        String runtime = String.format("%02d:%02d:%02d",
                elapsedSeconds / 3600, (elapsedSeconds % 3600) / 60, elapsedSeconds % 60);

        int bgX = 10;
        int bgY = 20;
        int bgWidth = 220;
        int bgHeight = mode.equals("Bonfire") || mode.equals("Fletching") ? 220 : 180;

        c.fillRect(bgX, bgY, bgWidth, bgHeight, 0xFF1E2328);
        c.drawRect(bgX, bgY, bgWidth, bgHeight, 0xFF00B48C);
        c.drawRect(bgX + 2, bgY + 2, bgWidth - 4, bgHeight - 4, 0xFF008B6B);

        int textX = bgX + 15;
        int textY = bgY + 25;

        Font titleFont = new Font("Segoe UI", Font.BOLD, 24);
        c.drawText("KWoodcutter", textX + 2, textY + 2, 0xFF008B6B, titleFont);
        c.drawText("KWoodcutter", textX, textY, 0xFFFFFFFF, titleFont);

        textY += 30;

        Font statFont = new Font("Segoe UI", Font.BOLD, 14);

        String status = currentState;
        if ((mode.equals("Bonfire") || mode.equals("Fletching")) && isProcessing) {
            status = mode;
        }

        c.drawText("Status: " + status, textX + 1, textY + 1, 0xFF008B6B, statFont);
        c.drawText("Status: " + status, textX, textY, 0xFFFFFFFF, statFont);

        textY += 20;
        c.drawText("Tree: " + currentTreeType, textX + 1, textY + 1, 0xFF008B6B, statFont);
        c.drawText("Tree: " + currentTreeType, textX, textY, 0xFF00B48C, statFont);

        textY += 20;

        String displayMode = mode.equals("Power chop") ? "Powerchop" : mode;
        c.drawText("Mode: " + displayMode, textX + 1, textY + 1, 0xFF008B6B, statFont);
        c.drawText("Mode: " + displayMode, textX, textY, 0xFF00B48C, statFont);

        textY += 20;
        c.drawText("Runtime: " + runtime, textX + 1, textY + 1, 0xFF008B6B, statFont);
        c.drawText("Runtime: " + runtime, textX, textY, 0xFFFFFFFF, statFont);

        textY += 20;

        c.drawText(String.format("Woodcutting XP/hr: %.0f", wcXpPerHour), textX + 1, textY + 1, 0xFF008B6B, statFont);
        c.drawText(String.format("Woodcutting XP/hr: %.0f", wcXpPerHour), textX, textY, 0xFF00B48C, statFont);

        textY += 20;

        if (mode.equals("Bonfire") || mode.equals("Fletching")) {
            String secondaryLabel = mode.equals("Fletching") ? "Fletching" : "Firemaking";
            int secondaryColor = mode.equals("Fletching") ? 0xFFFF4500 : 0xFF00B48C;
            c.drawText(String.format("%s XP/hr: %.0f", secondaryLabel, secondaryXpPerHour), textX + 1, textY + 1, 0xFF008B6B, statFont);
            c.drawText(String.format("%s XP/hr: %.0f", secondaryLabel, secondaryXpPerHour), textX, textY, secondaryColor, statFont);
        }

        if (bonfirePosition != null) {
            Polygon poly = getBonfireTilePoly();
            if (poly != null) {
                c.drawPolygon(poly, 0xFF00FF00, 1.0);
                c.fillPolygon(poly, 0x00FF00, 0.25);
            }
        }
    }

    public void onExit() {
        log("KWoodcutter stopped");
    }
}