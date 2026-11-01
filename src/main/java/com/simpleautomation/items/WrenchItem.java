package com.simpleautomation.items;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.commands.AutomationCommand;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.machines.Machine;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WrenchItem {

    public static final String WRENCH_NAME = "§6§lAutomation Wrench";

    /**
     * Key under which a machine item's upgrade levels live inside the item's
     * PersistentDataContainer. Stored in the PDC (not editable player-facing lore)
     * so upgrades cannot be duped or hand-edited by rewriting the item text.
     */
    public static NamespacedKey upgradesKey() {
        return new NamespacedKey(SimpleAutomation.getInstance(), "upgrades");
    }

    private static NamespacedKey wrenchKey() {
        return new NamespacedKey(SimpleAutomation.getInstance(), "wrench");
    }

    public static ItemStack createWrench() {
        ItemStack wrench = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = wrench.getItemMeta();

        meta.setDisplayName(WRENCH_NAME);
        meta.getPersistentDataContainer().set(wrenchKey(), PersistentDataType.BYTE, (byte) 1);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7A tool for managing machines");
        lore.add("");
        lore.add("§eRight-click §7machine: Open upgrade GUI");
        lore.add("§eRight-click §7conveyor: Rotate direction");
        lore.add("§eShift+Right-click §7machine: Pick up");
        lore.add("");
        lore.add("§8Preserves upgrades when picking up!");

        meta.setLore(lore);
        wrench.setItemMeta(meta);

        return wrench;
    }

    public static boolean isWrench(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_HOE) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(wrenchKey(), PersistentDataType.BYTE);
    }

    public static void pickupMachine(SimpleAutomation plugin, Player player, Machine machine) {
        if (!machine.isValid()) {
            plugin.getMachineManager().removeMachine(machine);
            return;
        }
        if (machine.getOwner() != null && !machine.getOwner().equals(player.getUniqueId())
                && !player.hasPermission(AutomationCommand.ADMIN_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You don't own this machine!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }

        ItemStack machineItem = createUpgradedMachineItem(machine);

        plugin.getMachineManager().removeMachine(machine);
        machine.getLocation().getBlock().setType(Material.AIR);

        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(machineItem);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), machineItem);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 1.5f);

        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6SimpleAutomation&8] ");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                prefix + "&aPicked up " + machine.getType().getDisplayName() + " with upgrades!"));
    }

    public static ItemStack createUpgradedMachineItem(Machine machine) {
        ItemStack item = PlacementListener.createMachineItem(machine.getType());
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        List<String> lore = meta.getLore();
        if (lore == null)
            lore = new ArrayList<>();

        Map<UpgradeType, Integer> upgrades = machine.getUpgrades();
        boolean preserveUpgrades = SimpleAutomation.getInstance().getConfig()
                .getBoolean("wrench.preserve-upgrades", true);
        if (preserveUpgrades && !upgrades.isEmpty()) {
            // Visible, human-readable summary only. This lore is purely cosmetic; the
            // authoritative upgrade data lives in the PDC below and is what gets read
            // back on placement, so editing these lines does nothing.
            lore.add("");
            lore.add("§d§lUpgrades:");
            for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
                lore.add("§7 • " + entry.getKey().getDisplayName() + " §7Lv." + entry.getValue());
            }

            // Authoritative upgrade data, stored in the PersistentDataContainer where a
            // player cannot see or edit it (no more dupe-by-copying-a-lore-string).
            StringBuilder upgradeData = new StringBuilder();
            for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
                upgradeData.append(entry.getKey().name()).append("=").append(entry.getValue()).append(";");
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(upgradesKey(), PersistentDataType.STRING, upgradeData.toString());
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Restore upgrades from the item's PersistentDataContainer when placing.
    public static Map<UpgradeType, Integer> extractUpgrades(ItemStack item) {
        Map<UpgradeType, Integer> upgrades = new java.util.EnumMap<>(UpgradeType.class);

        if (item == null || !item.hasItemMeta()) {
            return upgrades;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String data = pdc.get(upgradesKey(), PersistentDataType.STRING);
        if (data == null || data.isEmpty()) {
            return upgrades;
        }

        for (String pair : data.split(";")) {
            if (pair.isEmpty())
                continue;
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                try {
                    UpgradeType type = UpgradeType.valueOf(parts[0]);
                    int level = Integer.parseInt(parts[1]);
                    if (level > 0 && level <= type.getMaxLevel()) {
                        upgrades.put(type, level);
                    }
                } catch (Exception ignored) {
                    // Malformed or outdated upgrade entry, skip it.
                }
            }
        }

        return upgrades;
    }
}
