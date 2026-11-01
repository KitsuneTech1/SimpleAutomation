package com.simpleautomation.gui;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.machines.Machine;
import com.simpleautomation.machines.MachineType;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MachineGUI {

    private static final Map<UUID, Machine> openGUIs = new HashMap<>();

    public static void openGUI(Player player, Machine machine) {
        String title = machine.getDisplayName() + " §8- Upgrades";
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        List<UpgradeType> availableUpgrades = getAvailableUpgrades(machine.getType());

        int slot = 10;
        for (UpgradeType upgrade : availableUpgrades) {
            int currentLevel = machine.getUpgradeLevel(upgrade);
            int maxLevel = upgrade.getMaxLevel();

            ItemStack upgradeItem = createUpgradeItem(upgrade, currentLevel, maxLevel, machine.getType());
            inv.setItem(slot, upgradeItem);
            slot++;

            if (slot == 13)
                slot = 14; // Skip middle slot for info
        }

        inv.setItem(13, createInfoItem(machine));
        inv.setItem(22, createItem(Material.BARRIER, "§c§lClose", List.of("§7Click to close")));

        openGUIs.put(player.getUniqueId(), machine);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public static void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Machine machine = openGUIs.get(player.getUniqueId());

        if (machine == null) {
            return;
        }

        event.setCancelled(true);

        SimpleAutomation plugin = SimpleAutomation.getInstance();
        Machine current = plugin.getMachineManager().getMachine(machine.getId());
        if (current != machine || !machine.isValid()
                || !player.hasPermission(PlacementListener.USE_PERMISSION)) {
            openGUIs.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }

        // The bottom inventory is the player's inventory. It is locked while this GUI is
        // open, but it must never be interpreted as an upgrade button.
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (name.equals("Close")) {
            player.closeInventory();
            openGUIs.remove(player.getUniqueId());
            return;
        }

        for (UpgradeType upgrade : UpgradeType.values()) {
            if (name.contains(upgrade.getRawName())) {
                attemptUpgrade(player, machine, upgrade);
                // Refresh GUI
                openGUI(player, machine);
                return;
            }
        }
    }

    private static void attemptUpgrade(Player player, Machine machine, UpgradeType upgrade) {
        int currentLevel = machine.getUpgradeLevel(upgrade);
        int maxLevel = upgrade.getMaxLevel();

        if (currentLevel >= maxLevel) {
            player.sendMessage(ChatColor.RED + "This upgrade is already at max level!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }

        String configPath = getUpgradeConfigPath(machine.getType(), upgrade, currentLevel + 1);
        String costString = SimpleAutomation.getInstance().getConfig().getString(configPath);

        if (costString == null) {
            player.sendMessage(ChatColor.RED + "Upgrade not available!");
            return;
        }

        UpgradeCost cost = parseUpgradeCost(costString);
        if (cost == null) {
            player.sendMessage(ChatColor.RED + "Upgrade unavailable due to invalid configuration.");
            return;
        }
        Material costMaterial = cost.material();
        int costAmount = cost.amount();

        if (!hasItems(player, costMaterial, costAmount)) {
            String prefix = SimpleAutomation.getInstance().getConfig().getString("messages.prefix",
                    "&8[&6SimpleAutomation&8] ");
            String msg = SimpleAutomation.getInstance().getConfig().getString("messages.upgrade-fail",
                    "&cNot enough materials for this upgrade!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }

        removeItems(player, costMaterial, costAmount);
        machine.setUpgradeLevel(upgrade, currentLevel + 1);

        String prefix = SimpleAutomation.getInstance().getConfig().getString("messages.prefix",
                "&8[&6SimpleAutomation&8] ");
        String msg = SimpleAutomation.getInstance().getConfig().getString("messages.upgrade-success",
                "&aUpgrade applied! %upgrade% is now level %level%");
        msg = msg.replace("%upgrade%", upgrade.getRawName()).replace("%level%", String.valueOf(currentLevel + 1));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    static String getUpgradeConfigPath(MachineType type, UpgradeType upgrade, int level) {
        String machineKey = switch (type) {
            case QUARRY -> "quarry";
            case TREE_FARMER -> "tree-farmer";
            case CROP_FARMER -> "crop-farmer";
            case BLOCK_MINER -> "block-miner";
            case CONVEYOR_BELT -> "conveyor";
        };

        String upgradeKey = upgrade.name().toLowerCase().replace("_", "-");
        return machineKey + ".upgrades." + upgradeKey + ".level-" + level;
    }

    static UpgradeCost parseUpgradeCost(String costString) {
        if (costString == null) {
            return null;
        }

        String[] parts = costString.split(":", -1);
        if (parts.length != 2) {
            return null;
        }

        Material material = Material.matchMaterial(parts[0].trim());
        if (material == null) {
            return null;
        }

        try {
            int amount = Integer.parseInt(parts[1].trim());
            return amount > 0 ? new UpgradeCost(material, amount) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    record UpgradeCost(Material material, int amount) {
    }

    private static boolean hasItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private static void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    item.setAmount(0);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0)
                    break;
            }
        }
    }

    private static List<UpgradeType> getAvailableUpgrades(MachineType type) {
        return switch (type) {
            case QUARRY -> List.of(
                    UpgradeType.SPEED, UpgradeType.AREA, UpgradeType.FORTUNE,
                    UpgradeType.SILK_TOUCH, UpgradeType.EFFICIENCY, UpgradeType.VOID_FILTER);
            case TREE_FARMER -> List.of(
                    UpgradeType.SPEED, UpgradeType.RANGE, UpgradeType.MULTI_TREE, UpgradeType.LEAF_DECAY);
            case CROP_FARMER -> List.of(
                    UpgradeType.SPEED, UpgradeType.RANGE);
            case BLOCK_MINER -> List.of(
                    UpgradeType.SPEED, UpgradeType.FORTUNE, UpgradeType.SILK_TOUCH, UpgradeType.EFFICIENCY);
            case CONVEYOR_BELT -> List.of(UpgradeType.SPEED);
        };
    }

    private static ItemStack createUpgradeItem(UpgradeType upgrade, int currentLevel, int maxLevel,
            MachineType machineType) {
        Material icon = upgrade.getGuiIcon();
        String name = upgrade.getDisplayName() + " §7[" + currentLevel + "/" + maxLevel + "]";

        List<String> lore = new ArrayList<>();
        lore.add("");

        switch (upgrade) {
            case SPEED -> lore.add("§7Increases operation speed");
            case AREA -> lore.add("§7Increases mining area (up to 15x15)");
            case FORTUNE -> lore.add("§7Increases drops from ores");
            case SILK_TOUCH -> lore.add("§7Preserves mined blocks");
            case EFFICIENCY -> lore.add("§7Reduces fuel consumption");
            case VOID_FILTER -> lore.add("§7Destroys cobble, dirt, gravel");
            case RANGE -> lore.add("§7Increases detection range");
            case MULTI_TREE -> lore.add("§7Handles 2x2 large trees");
            case LEAF_DECAY -> lore.add("§7Instantly breaks leaves");
        }

        lore.add("");

        if (currentLevel >= maxLevel) {
            lore.add("§a§lMAX LEVEL");
        } else {
            String configPath = getUpgradeConfigPath(machineType, upgrade, currentLevel + 1);
            String costString = SimpleAutomation.getInstance().getConfig().getString(configPath, "IRON_INGOT:8");
            UpgradeCost cost = parseUpgradeCost(costString);
            if (cost == null) {
                lore.add("§cUpgrade unavailable");
            } else {
                String materialName = cost.material().name().replace("_", " ").toLowerCase();
                materialName = Character.toUpperCase(materialName.charAt(0)) + materialName.substring(1);

                lore.add("§eClick to upgrade!");
                lore.add("§7Cost: §f" + cost.amount() + "x " + materialName);
            }
        }

        ItemStack item = new ItemStack(icon);
        if (currentLevel > 0) {
            item.setAmount(currentLevel);
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createInfoItem(Machine machine) {
        Material icon = machine.getType().getBlockMaterial();
        String name = machine.getDisplayName() + " §7Info";

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Status: " + getStatusColor(machine.getStatus()) + machine.getStatus().name());
        String ownerName = machine.getOwner() == null ? "Unknown"
                : Bukkit.getOfflinePlayer(machine.getOwner()).getName();
        lore.add("§7Owner: §f" + ownerName);
        lore.add("");
        lore.add("§7Location:");
        lore.add("§f  " + machine.getLocation().getBlockX() + ", " +
                machine.getLocation().getBlockY() + ", " +
                machine.getLocation().getBlockZ());
        lore.add("");
        lore.add("§7Facing: §f" + machine.getFacing().name());

        return createItem(icon, name, lore);
    }

    private static String getStatusColor(Machine.MachineStatus status) {
        return switch (status) {
            case RUNNING -> "§a";
            case NO_FUEL -> "§c";
            case OUTPUT_FULL -> "§e";
            case IDLE -> "§7";
            case DISABLED -> "§8";
        };
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) {
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static void closeGUI(Player player) {
        openGUIs.remove(player.getUniqueId());
    }
}
