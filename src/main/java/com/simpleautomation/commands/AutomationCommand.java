package com.simpleautomation.commands;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.items.WrenchItem;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.machines.Machine;
import com.simpleautomation.machines.MachineType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AutomationCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "simpleautomation.admin";

    private final SimpleAutomation plugin;

    public AutomationCommand(SimpleAutomation plugin) {
        this.plugin = plugin;
    }

    private boolean checkAdminPermission(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return false;
        }
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "give" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }

                if (!checkAdminPermission(player)) {
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /" + label + " give <machine|wrench>");
                    return true;
                }

                String itemName = args[1].toLowerCase();
                ItemStack item = null;

                if (itemName.equals("wrench")) {
                    item = WrenchItem.createWrench();
                } else {
                    for (MachineType type : MachineType.values()) {
                        if (type.name().toLowerCase().replace("_", "").contains(itemName.replace("_", ""))) {
                            item = PlacementListener.createMachineItem(type);
                            break;
                        }
                    }
                }

                if (item == null) {
                    player.sendMessage(ChatColor.RED + "Unknown item: " + itemName);
                    player.sendMessage(ChatColor.GRAY + "Available: quarry, conveyor, treefarm, cropfarm, wrench");
                    return true;
                }

                player.getInventory().addItem(item);
                player.sendMessage(
                        ChatColor.GREEN + "Given " + item.getItemMeta().getDisplayName() + ChatColor.GREEN + "!");
            }

            case "reload" -> {
                if (!checkAdminPermission(sender)) {
                    return true;
                }

                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
            }

            case "list" -> {
                if (!checkAdminPermission(sender)) {
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "=== SimpleAutomation Machines ===");
                int count = 0;
                for (Machine machine : plugin.getMachineManager().getAllMachines()) {
                    count++;
                    sender.sendMessage(ChatColor.YELLOW + "• " + machine.getDisplayName() + ChatColor.GRAY +
                            " at " + machine.getLocation().getBlockX() + ", " +
                            machine.getLocation().getBlockY() + ", " +
                            machine.getLocation().getBlockZ() +
                            " (" + machine.getStatus().name() + ")");

                    if (count >= 20) {
                        sender.sendMessage(ChatColor.GRAY + "... and "
                                + (plugin.getMachineManager().getMachineCount() - 20) + " more");
                        break;
                    }
                }

                if (count == 0) {
                    sender.sendMessage(ChatColor.GRAY + "No machines placed.");
                }
            }

            case "save" -> {
                if (!checkAdminPermission(sender)) {
                    return true;
                }

                plugin.getMachineManager().saveMachines();
                sender.sendMessage(ChatColor.GREEN + "Machines saved!");
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SimpleAutomation v" + plugin.getDescription().getVersion() + " ===");
        sender.sendMessage(
                ChatColor.YELLOW + "/automation give <item>" + ChatColor.GRAY + " - Get a machine or wrench");
        sender.sendMessage(ChatColor.YELLOW + "/automation list" + ChatColor.GRAY + " - List all machines");
        sender.sendMessage(ChatColor.YELLOW + "/automation reload" + ChatColor.GRAY + " - Reload config");
        sender.sendMessage(ChatColor.YELLOW + "/automation save" + ChatColor.GRAY + " - Force save machines");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("give", "list", "reload", "save"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.add("wrench");
            for (MachineType type : MachineType.values()) {
                completions.add(type.name().toLowerCase());
            }
        }

        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }
}
