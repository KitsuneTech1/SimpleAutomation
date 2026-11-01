package com.simpleautomation.gui;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.machines.Quarry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MachineGUISafetyTest {

    private ServerMock server;
    private SimpleAutomation plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void malformedUpgradeCostDoesNotCrashGuiCreation() {
        plugin.getConfig().set("quarry.upgrades.speed.level-1", "NOT_A_MATERIAL:0");
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        Quarry quarry = new Quarry(new Location(world, 0, 64, 0), BlockFace.NORTH, UUID.randomUUID());

        assertDoesNotThrow(() -> MachineGUI.openGUI(player, quarry));
    }

    @Test
    void malformedUpgradeCostIsRejected() {
        assertNull(MachineGUI.parseUpgradeCost("NOT_A_MATERIAL:0"));
        assertNull(MachineGUI.parseUpgradeCost("IRON_INGOT"));
        assertNull(MachineGUI.parseUpgradeCost("IRON_INGOT:-1"));
    }

    @Test
    void removedMachineCannotBeUpgradedThroughAnOpenGui() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, PlacementListener.USE_PERMISSION, true);
        Location location = new Location(world, 0, 64, 0);
        world.getBlockAt(location).setType(Material.DROPPER);
        Quarry quarry = new Quarry(location, BlockFace.NORTH, player.getUniqueId());
        plugin.getMachineManager().addMachine(quarry);

        MachineGUI.openGUI(player, quarry);
        plugin.getMachineManager().removeMachine(quarry);

        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, 10,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
        MachineGUI.handleClick(event);

        assertEquals(0, quarry.getUpgradeLevel(com.simpleautomation.upgrades.UpgradeType.SPEED));
        org.junit.jupiter.api.Assertions.assertTrue(event.isCancelled());
    }

    @Test
    void playerInventoryItemsAreNotTreatedAsUpgradeButtons() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, PlacementListener.USE_PERMISSION, true);
        Location location = new Location(world, 0, 64, 0);
        world.getBlockAt(location).setType(Material.DROPPER);
        Quarry quarry = new Quarry(location, BlockFace.NORTH, player.getUniqueId());
        plugin.getMachineManager().addMachine(quarry);
        MachineGUI.openGUI(player, quarry);

        org.bukkit.inventory.ItemStack renamed = new org.bukkit.inventory.ItemStack(Material.STONE);
        org.bukkit.inventory.meta.ItemMeta meta = renamed.getItemMeta();
        meta.setDisplayName("§bSpeed");
        renamed.setItemMeta(meta);
        player.getInventory().setItem(0, renamed);

        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER, 27,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
        MachineGUI.handleClick(event);

        assertEquals(0, quarry.getUpgradeLevel(com.simpleautomation.upgrades.UpgradeType.SPEED));
    }
}
