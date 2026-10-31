package com.simpleautomation.listeners;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.gui.MachineGUI;
import com.simpleautomation.items.WrenchItem;
import com.simpleautomation.machines.ConveyorBelt;
import com.simpleautomation.machines.Machine;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class InteractListener implements Listener {

    private final SimpleAutomation plugin;

    public InteractListener(SimpleAutomation plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (WrenchItem.isWrench(item)) {
            Machine machine = plugin.getMachineManager().getMachineAt(block.getLocation());

            if (machine != null) {
                event.setCancelled(true);

                if (player.isSneaking()) {
                    WrenchItem.pickupMachine(plugin, player, machine);
                } else {
                    // Rotating or opening a machine is a "use" action.
                    if (lacksUsePermission(player)) {
                        return;
                    }
                    if (machine instanceof ConveyorBelt belt) {
                        belt.rotate();
                        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 0.5f, 1.2f);
                    } else {
                        MachineGUI.openGUI(player, machine);
                    }
                }
                return;
            }
        }

        // Right-click machine without wrench opens GUI
        Machine machine = plugin.getMachineManager().getMachineAt(block.getLocation());
        if (machine != null && !(machine instanceof ConveyorBelt)) {
            if (!player.isSneaking()) {
                event.setCancelled(true);
                if (lacksUsePermission(player)) {
                    return;
                }
                MachineGUI.openGUI(player, machine);
            }
        }
    }

    /**
     * Returns true (and messages the player) if they lack the use permission. Opening a
     * machine GUI or rotating a conveyor both count as using a machine.
     */
    private boolean lacksUsePermission(Player player) {
        if (player.hasPermission(PlacementListener.USE_PERMISSION)) {
            return false;
        }
        String prefix = plugin.getConfig().getString("messages.prefix", "&8[&6SimpleAutomation&8] ");
        String msg = plugin.getConfig().getString("messages.no-permission",
                "&cYou don't have permission to do that!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        if (!title.contains("Upgrades") && !title.contains("Farmer") && !title.contains("Quarry")
                && !title.contains("Conveyor")) {
            return;
        }

        // MachineGUI inventories are created with a null holder, so a non-null holder means
        // this inventory belongs to some other plugin/vanilla screen, not ours.
        if (event.getInventory().getHolder() != null) {
            return;
        }

        MachineGUI.handleClick(event);
    }
}
