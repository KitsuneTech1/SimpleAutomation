package com.simpleautomation.machines;

import org.bukkit.Material;

public enum MachineType {
    QUARRY("Quarry", Material.DROPPER, "§6§lQuarry"),
    CONVEYOR_BELT("§9§lConveyor Belt", Material.BLUE_GLAZED_TERRACOTTA, "§9§lConveyor Belt"),
    TREE_FARMER("Tree Farmer", Material.DISPENSER, "§2§lTree Farmer"),
    CROP_FARMER("Crop Farmer", Material.DISPENSER, "§a§lCrop Farmer"),
    BLOCK_MINER("Block Miner", Material.OBSERVER, "§6§lBlock Miner");

    private final String displayName;
    private final Material blockMaterial;
    private final String itemName;

    MachineType(String displayName, Material blockMaterial, String itemName) {
        this.displayName = displayName;
        this.blockMaterial = blockMaterial;
        this.itemName = itemName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public String getItemName() {
        return itemName;
    }

    /**
     * Root config-section key for this machine (e.g. "quarry", "tree-farmer"). Used for
     * per-machine settings like place-cost and upgrade costs.
     */
    public String getConfigKey() {
        return switch (this) {
            case QUARRY -> "quarry";
            case CONVEYOR_BELT -> "conveyor";
            case TREE_FARMER -> "tree-farmer";
            case CROP_FARMER -> "crop-farmer";
            case BLOCK_MINER -> "block-miner";
        };
    }

    public static MachineType fromItemName(String name) {
        if (name == null)
            return null;
        String stripped = name.replace("§", "").toLowerCase();
        for (MachineType type : values()) {
            if (type.itemName.replace("§", "").toLowerCase()
                    .contains(stripped.replace("l", "").replace("6", "").replace("2", "").replace("a", ""))) {
                return type;
            }
        }
        // Direct match
        for (MachineType type : values()) {
            if (name.equals(type.itemName)) {
                return type;
            }
        }
        return null;
    }
}
