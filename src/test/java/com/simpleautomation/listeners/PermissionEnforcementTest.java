package com.simpleautomation.listeners;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.MachineType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Permission enforcement: the declared simpleautomation.use permission was never checked, so
 * anyone could place and use machines. Placing a machine is a "use" action; without the
 * permission the placement is cancelled and no machine is registered, with it the machine is
 * registered.
 */
class PermissionEnforcementTest {

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

    private BlockPlaceEvent quarryPlaceEvent(WorldMock world, PlayerMock player) {
        Block placed = world.getBlockAt(0, 64, 0);
        BlockState replaced = placed.getState();
        placed.setType(Material.DROPPER); // the quarry's block material

        Block against = world.getBlockAt(0, 63, 0);
        against.setType(Material.STONE);

        ItemStack inHand = PlacementListener.createMachineItem(MachineType.QUARRY);

        return new BlockPlaceEvent(placed, replaced, against, inHand, player, true, EquipmentSlot.HAND);
    }

    @Test
    void playerWithoutUsePermissionCannotPlaceMachine() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, PlacementListener.USE_PERMISSION, false);

        BlockPlaceEvent event = quarryPlaceEvent(world, player);
        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "Placement was not cancelled for a player lacking the use permission");
        assertNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)),
                "A machine was registered despite the player lacking the use permission");
    }

    @Test
    void playerWithUsePermissionCanPlaceMachine() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, PlacementListener.USE_PERMISSION, true);

        BlockPlaceEvent event = quarryPlaceEvent(world, player);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "Placement was cancelled for a player holding the use permission");
        assertNotNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)),
                "Machine was not registered for a player holding the use permission");
    }

    @Test
    void anotherPlayerCannotBreakOrStealAnOwnedMachine() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();
        PlayerMock thief = server.addPlayer();
        thief.addAttachment(plugin, PlacementListener.USE_PERMISSION, true);

        Location location = new Location(world, 0, 64, 0);
        world.getBlockAt(location).setType(Material.DROPPER);
        com.simpleautomation.machines.Quarry quarry = new com.simpleautomation.machines.Quarry(
                location, org.bukkit.block.BlockFace.NORTH, owner.getUniqueId());
        plugin.getMachineManager().addMachine(quarry);

        BlockBreakEvent event = new BlockBreakEvent(world.getBlockAt(location), thief);
        listener.onBlockBreak(event);

        assertTrue(event.isCancelled(), "A non-owner was allowed to break an owned machine");
        assertNotNull(plugin.getMachineManager().getMachineAt(location),
                "A non-owner removed the machine from the manager");
    }

    @Test
    void protectionCancellationPreventsMachineRemoval() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();
        owner.addAttachment(plugin, PlacementListener.USE_PERMISSION, true);

        Location location = new Location(world, 0, 64, 0);
        world.getBlockAt(location).setType(Material.DROPPER);
        com.simpleautomation.machines.Quarry quarry = new com.simpleautomation.machines.Quarry(
                location, org.bukkit.block.BlockFace.NORTH, owner.getUniqueId());
        plugin.getMachineManager().addMachine(quarry);

        server.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void deny(org.bukkit.event.block.BlockBreakEvent event) {
                event.setCancelled(true);
            }
        }, plugin);

        BlockBreakEvent event = new BlockBreakEvent(world.getBlockAt(location), owner);
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled(), "Protection listener did not cancel the break");
        assertNotNull(plugin.getMachineManager().getMachineAt(location),
                "A cancelled break removed the machine");
    }
}
