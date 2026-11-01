package com.simpleautomation.listeners;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.items.WrenchItem;
import com.simpleautomation.machines.Quarry;
import com.simpleautomation.machines.MachineType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemIdentityTest {

    private ServerMock server;
    private SimpleAutomation plugin;
    private PlacementListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
        listener = new PlacementListener(plugin);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void renamedOrdinaryBlockCannotPlaceAMachine() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        ItemStack renamed = new ItemStack(Material.DROPPER);
        ItemMeta meta = renamed.getItemMeta();
        meta.setDisplayName(MachineType.QUARRY.getItemName());
        renamed.setItemMeta(meta);

        Block placed = world.getBlockAt(0, 64, 0);
        BlockState replaced = placed.getState();
        placed.setType(Material.DROPPER);
        BlockPlaceEvent event = new BlockPlaceEvent(placed, replaced,
                world.getBlockAt(0, 63, 0), renamed, player, true, EquipmentSlot.HAND);

        listener.onBlockPlace(event);

        assertNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)),
                "A renamed ordinary block was accepted as a machine item");
    }

    @Test
    void renamedIronHoeCannotActAsWrench() {
        ItemStack renamed = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = renamed.getItemMeta();
        meta.setDisplayName(WrenchItem.WRENCH_NAME);
        renamed.setItemMeta(meta);

        assertFalse(WrenchItem.isWrench(renamed),
                "A renamed ordinary iron hoe was accepted as an automation wrench");
    }

    @Test
    void wrenchCannotPickUpAStaleMachineEntry() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        Location location = new Location(world, 0, 64, 0);
        world.getBlockAt(location).setType(Material.DROPPER);
        Quarry quarry = new Quarry(location, org.bukkit.block.BlockFace.NORTH, player.getUniqueId());
        plugin.getMachineManager().addMachine(quarry);
        world.getBlockAt(location).setType(Material.STONE);

        WrenchItem.pickupMachine(plugin, player, quarry);

        assertNull(plugin.getMachineManager().getMachineAt(location));
        assertFalse(player.getInventory().contains(Material.DROPPER),
                "A stale registry entry produced a machine item after the block changed");
        assertNotNull(world.getBlockAt(location));
    }
}
