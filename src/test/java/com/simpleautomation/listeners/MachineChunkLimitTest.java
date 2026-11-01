package com.simpleautomation.listeners;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.MachineType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineChunkLimitTest {

    private ServerMock server;
    private SimpleAutomation plugin;
    private PlacementListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
        listener = new PlacementListener(plugin);
        plugin.getConfig().set("general.max-machines-per-chunk", 1);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private BlockPlaceEvent placeEvent(WorldMock world, PlayerMock player, int x) {
        Block placed = world.getBlockAt(x, 64, 0);
        BlockState replaced = placed.getState();
        placed.setType(Material.DROPPER);
        return new BlockPlaceEvent(placed, replaced, world.getBlockAt(x, 63, 0),
                PlacementListener.createMachineItem(MachineType.QUARRY), player, true, EquipmentSlot.HAND);
    }

    @Test
    void placementIsCancelledWhenChunkLimitIsReached() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        BlockPlaceEvent first = placeEvent(world, player, 0);
        listener.onBlockPlace(first);
        BlockPlaceEvent second = placeEvent(world, player, 1);
        listener.onBlockPlace(second);

        assertFalse(first.isCancelled());
        assertTrue(second.isCancelled(), "The configured per-chunk machine cap was ignored");
        assertNull(plugin.getMachineManager().getMachineAt(new Location(world, 1, 64, 0)));
    }
}
