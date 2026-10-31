package com.simpleautomation.upgrades;

import org.bukkit.Material;

public enum UpgradeType {
    // Quarry upgrades
    SPEED("Speed", "§b", 5),
    AREA("Area", "§e", 6),
    FORTUNE("Fortune", "§a", 3),
    SILK_TOUCH("Silk Touch", "§d", 1),
    EFFICIENCY("Efficiency", "§c", 3),
    VOID_FILTER("Void Filter", "§8", 1),

    // Tree/Crop Farmer upgrades
    RANGE("Range", "§6", 3),
    MULTI_TREE("Multi-Tree", "§2", 1),
    LEAF_DECAY("Leaf Decay", "§a", 1);

    private final String displayName;
    private final String colorCode;
    private final int maxLevel;

    UpgradeType(String displayName, String colorCode, int maxLevel) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.maxLevel = maxLevel;
    }

    public String getDisplayName() {
        return colorCode + displayName;
    }

    public String getRawName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Material getGuiIcon() {
        return switch (this) {
            case SPEED -> Material.SUGAR;
            case AREA -> Material.PISTON;
            case FORTUNE -> Material.EMERALD;
            case SILK_TOUCH -> Material.DIAMOND_PICKAXE;
            case EFFICIENCY -> Material.REDSTONE;
            case VOID_FILTER -> Material.LAVA_BUCKET;
            case RANGE -> Material.SPYGLASS;
            case MULTI_TREE -> Material.OAK_SAPLING;
            case LEAF_DECAY -> Material.SHEARS;
        };
    }
}
