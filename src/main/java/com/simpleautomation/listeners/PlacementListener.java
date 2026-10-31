package com.simpleautomation.listeners;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.economy.EconomyHook;
import com.simpleautomation.items.WrenchItem;
import com.simpleautomation.machines.*;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlacementListener implements Listener {

    public static final String USE_PERMISSION = "simpleautomation.use";
    public static final String CRAFT_PERMISSION = "simpleautomation.craft";

    private final SimpleAutomation plugin;

    public PlacementListener(SimpleAutomation plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String name = item.getItemMeta().getDisplayName();
        MachineType type = getMachineType(name);

        if (type == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        // Permission: placing a machine is a "use" action.
        if (!player.hasPermission(USE_PERMISSION)) {
            event.setCancelled(true);
            sendConfigMessage(player, "messages.no-permission", "&cYou don't have permission to do that!", type);
            return;
        }

        // Economy: optionally charge money to place. Blocks placement if the owner cannot
        // afford it; a disabled economy or a missing provider is a no-op (SKIPPED).
        EconomyHook eco = plugin.getEconomyHook();
        EconomyHook.Result charge = eco.charge(player, eco.getPlaceCost(type));
        if (charge == EconomyHook.Result.INSUFFICIENT) {
            event.setCancelled(true);
            String costText = String.valueOf(eco.getPlaceCost(type));
            sendConfigMessage(player,
                    "messages.insufficient-funds", "&cYou need &f%cost%&c to place a %machine%!", type,
                    "%cost%", costText);
            return;
        }

        BlockFace facing = getPlayerFacing(player);

        if (block.getBlockData() instanceof Directional directional) {
            facing = directional.getFacing();
        }

        Machine machine = createMachine(type, block, facing, player);
        if (machine != null) {
            // Restore upgrades from item lore
            Map<UpgradeType, Integer> upgrades = WrenchItem.extractUpgrades(item);
            for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
                machine.setUpgradeLevel(entry.getKey(), entry.getValue());
            }

            plugin.getMachineManager().addMachine(machine);

            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6SimpleAutomation&8] ");
            String msg = plugin.getConfig().getString("messages.machine-placed", "&aPlaced %machine%!");
            msg = msg.replace("%machine%", type.getDisplayName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Use LOWEST to catch it before other things might drop it
    public void onBlockBreak(BlockBreakEvent event) {
        // Ignore our own build-access probe events (fired by Machine.canAffect) so a probe
        // never trips the machine pickup/removal logic below.
        if (Machine.isProbing()) {
            return;
        }

        Block block = event.getBlock();
        Machine machine = plugin.getMachineManager().getMachineAt(block.getLocation());

        if (machine != null) {
            // Cancel vanilla drops entirely to prevent duplication
            event.setDropItems(false);

            Player player = event.getPlayer();

            // Drop custom machine item with ALL upgrades preserved
            ItemStack machineItem = WrenchItem.createUpgradedMachineItem(machine);
            block.getWorld().dropItemNaturally(block.getLocation(), machineItem);

            plugin.getMachineManager().removeMachine(machine);

            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6SimpleAutomation&8] ");
            String msg = plugin.getConfig().getString("messages.machine-removed", "&cRemoved %machine%!");
            msg = msg.replace("%machine%", machine.getType().getDisplayName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
        }
    }

    /**
     * Blocks crafting our machines/wrench when the crafter lacks the craft permission.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (!isPluginItem(result)) {
            return;
        }

        HumanEntity whoClicked = event.getWhoClicked();
        if (!whoClicked.hasPermission(CRAFT_PERMISSION)) {
            event.setCancelled(true);
            if (whoClicked instanceof Player player) {
                sendConfigMessage(player, "messages.no-permission",
                        "&cYou don't have permission to do that!", null);
            }
        }
    }

    /** True if the stack is one of our custom machine items or the automation wrench. */
    private boolean isPluginItem(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) {
            return false;
        }
        String name = stack.getItemMeta().getDisplayName();
        if (name.equals(WrenchItem.WRENCH_NAME)) {
            return true;
        }
        return getMachineType(name) != null;
    }

    /**
     * Sends a config message with the standard prefix, translating colour codes and the
     * common placeholders. Extra placeholder/value pairs may be appended.
     */
    private void sendConfigMessage(Player player, String path, String def, MachineType type, String... replacements) {
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6SimpleAutomation&8] ");
        String msg = plugin.getConfig().getString(path, def);
        if (type != null) {
            msg = msg.replace("%machine%", type.getDisplayName());
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    private MachineType getMachineType(String displayName) {
        for (MachineType type : MachineType.values()) {
            if (displayName.equals(type.getItemName())) {
                return type;
            }
        }
        return null;
    }

    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0)
            yaw += 360;

        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }

    private Machine createMachine(MachineType type, Block block, BlockFace facing, Player player) {
        return switch (type) {
            case QUARRY -> new Quarry(block.getLocation(), facing, player.getUniqueId());
            case CONVEYOR_BELT -> new ConveyorBelt(block.getLocation(), facing, player.getUniqueId());
            case TREE_FARMER -> new TreeFarmer(block.getLocation(), facing, player.getUniqueId());
            case CROP_FARMER -> new CropFarmer(block.getLocation(), facing, player.getUniqueId());
            case BLOCK_MINER -> new BlockMiner(block.getLocation(), facing, player.getUniqueId());
        };
    }

    public static ItemStack createMachineItem(MachineType type) {
        ItemStack item = new ItemStack(type.getBlockMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getItemName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to place");
        lore.add(ChatColor.GRAY + "Shift+Right-click with wrench to pick up");

        switch (type) {
            case QUARRY -> {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Mines blocks in front of it");
                lore.add(ChatColor.YELLOW + "Deposits items to adjacent chest");
            }
            case CONVEYOR_BELT -> {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Moves items along the belt");
                lore.add(ChatColor.YELLOW + "Connects to chests and hoppers");
            }
            case TREE_FARMER -> {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Harvests trees automatically");
                lore.add(ChatColor.YELLOW + "Replants saplings");
            }
            case CROP_FARMER -> {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Harvests mature crops");
                lore.add(ChatColor.YELLOW + "Auto-replants seeds");
            }
            case BLOCK_MINER -> {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Mines the block directly in front");
                lore.add(ChatColor.YELLOW + "Deposits items to adjacent chest");
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
