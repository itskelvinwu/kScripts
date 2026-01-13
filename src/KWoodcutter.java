import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.*;
import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.PixelAnalyzer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.utils.UIResultList;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@ScriptDefinition(
        name = "KWoodcutter",
        author = "Kelvin",
        version = 1.0,
        description = "Advanced woodcutting script - GUI for tree type (normal/oak/willow/maple), powerchop only, drops logs/clue boxes, modern neon HUD paint.",
        skillCategory = SkillCategory.WOODCUTTING
)
public class KWoodcutter extends Script {

    public KWoodcutter(Object param) {
        super(param);
    }

    private static final int VARROCK_WEST_REGION = 12850;
    private static final int DRAYNOR_REGION = 12337;
    private static final int BA_REGION = 10039;
    private static final int SEERS_REGION = 10806;

    private static final RectangleArea NORMAL_AREA = new RectangleArea(3155, 3395, 20, 20, 0);
    private static final WorldPosition NORMAL_CENTER = new WorldPosition(3164, 3403, 0);
    private static final Set<String> NORMAL_NAMES = Set.of("Tree");
    private static final int NORMAL_LOG_ID = 1511;

    private static final RectangleArea OAK_AREA = new RectangleArea(3155, 3395, 20, 20, 0);
    private static final WorldPosition OAK_CENTER = new WorldPosition(3165, 3403, 0);
    private static final Set<String> OAK_NAMES = Set.of("Oak tree");
    private static final int OAK_LOG_ID = 1521;

    private static final RectangleArea DRAYNOR_WILLOW_AREA = new RectangleArea(3083, 3228, 12, 12, 0);
    private static final WorldPosition DRAYNOR_WILLOW_CENTER = new WorldPosition(3087, 3236, 0);
    private static final Set<String> DRAYNOR_WILLOW_NAMES = Set.of("Willow tree");
    private static final int DRAYNOR_WILLOW_LOG_ID = 1519;

    private static final RectangleArea BA_WILLOW_AREA = new RectangleArea(2518, 3572, 12, 12, 0);
    private static final WorldPosition BA_WILLOW_CENTER = new WorldPosition(2520, 3578, 0);
    private static final Set<String> BA_WILLOW_NAMES = Set.of("Willow tree");
    private static final int BA_WILLOW_LOG_ID = 1519;

    private static final RectangleArea MAPLE_AREA = new RectangleArea(2719, 3484, 12, 12, 0);
    private static final WorldPosition MAPLE_CENTER = new WorldPosition(2731, 3500, 0);
    private static final Set<String> MAPLE_NAMES = Set.of("Maple tree");
    private static final int MAPLE_LOG_ID = 1517;

    private static final Set<Integer> CLUE_BOX_IDS = Set.of(19835, 19836, 19837, 19838, 19839, 23164);

    private static final int FULL_THRESHOLD = 26;

    private Set<String> currentTreeNames;
    private int currentLogID;
    private RectangleArea currentTreeArea;
    private WorldPosition currentTreeCenter;
    private String currentTreeType;
    private int expectedRegion;

    private String currentState = "Initializing";

    private RSObject currentChoppingTree = null;

    private boolean isDropping = false;

    private volatile boolean guiReady = false;

    private static final String SWING_MESSAGE = "You swing your axe at the tree";
    private static final String FULL_INVENTORY_MESSAGE = "Your inventory is too full to hold any more logs.";

    public void onStart() {
        currentChoppingTree = null;
        isDropping = false;
        currentState = "Initializing";

        SwingUtilities.invokeLater(this::createModernGUI);

        while (!guiReady) {
            sleep(500);
        }

        currentState = "Chopping";
        log("KWoodcutter started - powerchop mode, chopping " + currentTreeType);
    }

    private void createModernGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("KWoodcutter");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(500, 380);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new Color(15, 15, 20));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(15, 15, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel title = new JLabel("KWoodcutter");
        title.setFont(new Font("Segoe UI", Font.BOLD, 40));
        title.setForeground(new Color(0, 255, 140));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        JLabel subtitle = new JLabel("Advanced Powerchop Script");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        subtitle.setForeground(new Color(180, 180, 180));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(40));

        JLabel treeLabel = new JLabel("Select Tree Type");
        treeLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        treeLabel.setForeground(Color.WHITE);
        treeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(treeLabel);
        panel.add(Box.createVerticalStrut(10));

        String[] trees = {
                "Normal trees (Varrock West)",
                "Oaks (Varrock West)",
                "Willows (Draynor Village)",
                "Willows (Barbarian Assault)",
                "Maples (Seers Village)"
        };
        JComboBox<String> treeCombo = new JComboBox<>(trees);
        treeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        treeCombo.setMaximumSize(new Dimension(400, 50));
        treeCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        treeCombo.setBackground(Color.WHITE);
        treeCombo.setForeground(Color.BLACK);

        panel.add(treeCombo);
        panel.add(Box.createVerticalStrut(40));

        JButton startBtn = new JButton("START SCRIPT");
        startBtn.setFont(new Font("Segoe UI", Font.BOLD, 24));
        startBtn.setForeground(Color.BLACK);
        startBtn.setBackground(new Color(0, 255, 140));
        startBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        startBtn.setMaximumSize(new Dimension(300, 70));
        startBtn.setBorder(BorderFactory.createEmptyBorder());
        startBtn.setFocusPainted(false);
        startBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        startBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startBtn.setBackground(new Color(0, 220, 120));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startBtn.setBackground(new Color(0, 255, 140));
            }
        });
        startBtn.addActionListener(e -> {
            String selectedTree = (String) treeCombo.getSelectedItem();
            if (selectedTree.contains("Normal trees")) {
                currentTreeNames = NORMAL_NAMES;
                currentLogID = NORMAL_LOG_ID;
                currentTreeArea = NORMAL_AREA;
                currentTreeCenter = NORMAL_CENTER;
                currentTreeType = "Normal Trees";
                expectedRegion = VARROCK_WEST_REGION;
            } else if (selectedTree.contains("Oaks")) {
                currentTreeNames = OAK_NAMES;
                currentLogID = OAK_LOG_ID;
                currentTreeArea = OAK_AREA;
                currentTreeCenter = OAK_CENTER;
                currentTreeType = "Oaks";
                expectedRegion = VARROCK_WEST_REGION;
            } else if (selectedTree.contains("Willows (Draynor Village)")) {
                currentTreeNames = DRAYNOR_WILLOW_NAMES;
                currentLogID = DRAYNOR_WILLOW_LOG_ID;
                currentTreeArea = DRAYNOR_WILLOW_AREA;
                currentTreeCenter = DRAYNOR_WILLOW_CENTER;
                currentTreeType = "Willows (Draynor)";
                expectedRegion = DRAYNOR_REGION;
            } else if (selectedTree.contains("Willows (Barbarian Assault)")) {
                currentTreeNames = BA_WILLOW_NAMES;
                currentLogID = BA_WILLOW_LOG_ID;
                currentTreeArea = BA_WILLOW_AREA;
                currentTreeCenter = BA_WILLOW_CENTER;
                currentTreeType = "Willows (BA)";
                expectedRegion = BA_REGION;
            } else {
                currentTreeNames = MAPLE_NAMES;
                currentLogID = MAPLE_LOG_ID;
                currentTreeArea = MAPLE_AREA;
                currentTreeCenter = MAPLE_CENTER;
                currentTreeType = "Maples";
                expectedRegion = SEERS_REGION;
            }

            guiReady = true;
            frame.dispose();
        });
        panel.add(startBtn);

        frame.add(panel);
        frame.setVisible(true);
    }

    @Override
    public int poll() {
        if (hasChatMessage(FULL_INVENTORY_MESSAGE)) {
            currentState = "Dropping";
            isDropping = true;
            currentChoppingTree = null;
        }

        if (isDropping) {
            currentState = "Dropping";
            if (getCurrentLogCount() == 0 && getClueBoxCount() == 0) {
                isDropping = false;
                currentChoppingTree = null;
                currentState = "Chopping";
                return random(600, 1200);
            }

            getWidgetManager().getInventory().dropItems(Set.of(currentLogID));
            getWidgetManager().getInventory().dropItems(CLUE_BOX_IDS);
            return random(600, 1000);
        }

        currentState = "Chopping";

        if (getCurrentLogCount() >= FULL_THRESHOLD || getClueBoxCount() > 0) {
            currentState = "Dropping";
            isDropping = true;
            currentChoppingTree = null;
            getWidgetManager().getInventory().dropItems(Set.of(currentLogID));
            getWidgetManager().getInventory().dropItems(CLUE_BOX_IDS);
            return random(1200, 2000);
        }

        List<RSObject> uncutTrees = getUncutTrees();

        if (uncutTrees.isEmpty()) {
            currentState = "Chopping";
            currentChoppingTree = null;
            WalkConfig walkConfig = new WalkConfig.Builder()
                    .breakCondition(() -> !getUncutTrees().isEmpty())
                    .breakDistance(1)
                    .build();

            getWalker().walkTo(currentTreeCenter, walkConfig);
            return random(800, 1400);
        }

        if (currentChoppingTree != null && uncutTrees.contains(currentChoppingTree)) {
            return random(600, 1200);
        }

        RSObject tree = uncutTrees.get(0);
        currentChoppingTree = tree;

        Polygon hull = tree.getConvexHull();
        if (hull == null || !getFinger().tapGameScreen(hull)) {
            currentChoppingTree = null;
            return random(400, 800);
        }

        double dist = tree.getTileDistance(getWorldPosition());
        if (!submitHumanTask(() -> tree.getTileDistance(getWorldPosition()) <= 1, (int)(dist * 1000) + 2000)) {
            log("Failed to reach tree");
            currentChoppingTree = null;
            return random(600, 1200);
        }

        if (!submitHumanTask(() -> hasChatMessage(SWING_MESSAGE), 4000)) {
            log("No 'swing' message - interaction failed, moving to next tree");
            currentChoppingTree = null;
            return random(600, 1200);
        }

        if (!submitHumanTask(() -> !getUncutTrees().contains(tree), 40000)) {
            log("Stump wait timed out after 40s - moving to next tree");
            currentChoppingTree = null;
            return random(600, 1200);
        }

        currentChoppingTree = null;

        return random(600, 1200);
    }

    private int getCurrentLogCount() {
        var result = getWidgetManager().getInventory().search(Set.of(currentLogID));
        if (result == null) return 0;
        return result.getAmount(currentLogID);
    }

    private int getClueBoxCount() {
        var result = getWidgetManager().getInventory().search(CLUE_BOX_IDS);
        if (result == null) return 0;
        int total = 0;
        for (int id : CLUE_BOX_IDS) {
            total += result.getAmount(id);
        }
        return total;
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
                .getRespawnCircleObjects(trees, PixelAnalyzer.RespawnCircleDrawType.CENTER, 0, 7);

        Set<RSObject> cutTrees = respawnMap.keySet();

        return trees.stream()
                .filter(t -> !cutTrees.contains(t))
                .filter(RSObject::isInteractableOnScreen)
                .sorted(Comparator.comparingDouble(t -> t.distance(getWorldPosition())))
                .toList();
    }

    @Override
    public void onPaint(Canvas c) {
        int bgX = 10;
        int bgY = 20;
        int bgWidth = 280;
        int bgHeight = 140;

        c.fillRect(bgX - 5, bgY - 5, bgWidth + 10, bgHeight + 10, 0x9900FF00);
        c.fillRect(bgX - 3, bgY - 3, bgWidth + 6, bgHeight + 6, 0x7700CC00);
        c.fillRect(bgX - 2, bgY - 2, bgWidth + 4, bgHeight + 4, 0x5500AA00);

        c.fillRect(bgX, bgY, bgWidth, bgHeight, 0xFF0F0F1F);
        c.fillRect(bgX + 5, bgY + 5, bgWidth - 10, bgHeight - 10, 0xBB000000);

        c.drawRect(bgX, bgY, bgWidth, bgHeight, 0xFF00FF00);
        c.drawRect(bgX + 1, bgY + 1, bgWidth - 2, bgHeight - 2, 0xFF00DD00);
        c.drawRect(bgX + 2, bgY + 2, bgWidth - 4, bgHeight - 4, 0xFF00BB00);
        c.drawRect(bgX + 3, bgY + 3, bgWidth - 6, bgHeight - 6, 0xFF009900);

        int textX = bgX + 25;
        int textY = bgY + 40;

        java.awt.Font titleFont = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28);
        c.drawText("KWoodcutter", textX + 1, textY + 1, 0x8800FF00, titleFont);
        c.drawText("KWoodcutter", textX, textY, 0xFF00FF00, titleFont);

        textY += 50;
        java.awt.Font statFont = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18);

        c.drawText("Status: " + currentState, textX + 1, textY + 1, 0x66000000, statFont);
        c.drawText("Status: " + currentState, textX, textY, 0xFFFFFFFF, statFont);

        textY += 35;
        c.drawText("Tree: " + currentTreeType, textX + 1, textY + 1, 0x66000000, statFont);
        c.drawText("Tree: " + currentTreeType, textX, textY, 0xFFCCFFCC, statFont);
    }

    public void onExit() {
        log("KWoodcutter stopped");
    }
}