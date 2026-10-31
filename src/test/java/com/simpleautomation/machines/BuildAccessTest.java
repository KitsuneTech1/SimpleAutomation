package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Grief / ownership protection: a machine may only break a block where its OWNER has build
 * rights. Access is checked by firing a dummy BlockBreakEvent as the owner and treating a
 * cancellation from another plugin (a claim/protection plugin) as denied. These tests stand
 * in for WorldGuard/GriefPrevention with a listener that cancels the owner's break event.
 */
class BuildAccessTest {

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

    /** A stand-in protection plugin: cancels every block break (i.e. the owner has no rights). */
    static class DenyAllBreaks implements Listener {
        @EventHandler
        public void onBreak(BlockBreakEvent event) {
            event.setCancelled(true);
        }
    }

    private BlockMiner buildMinerFacingStone(WorldMock world, Player owner) {
        int mx = 0, my = 64, mz = 0;
        // Target block the miner will try to mine, directly in front (NORTH = -Z).
        world.getBlockAt(mx, my, mz - 1).setType(Material.STONE);

        // Adjacent chest supplies fuel and receives output so fuel/inventory checks pass.
        world.getBlockAt(mx + 1, my, mz).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(mx + 1, my, mz).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.COAL, 64));

        Location loc = new Location(world, mx, my, mz);
        return new BlockMiner(loc, BlockFace.NORTH, owner.getUniqueId());
    }

    @Test
    void machineDoesNotMineWhenOwnerBuildAccessIsDenied() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();

        // The claim plugin denies the owner's break at this location.
        server.getPluginManager().registerEvents(new DenyAllBreaks(), plugin);

        BlockMiner miner = buildMinerFacingStone(world, owner);

        for (int i = 0; i < 200; i++) {
            miner.tick();
        }

        assertEquals(Material.STONE, world.getBlockAt(0, 64, -1).getType(),
                "Machine mined a block its owner has no build rights to");
    }

    @Test
    void machineMinesNormallyWhenNoProtectionDeniesTheOwner() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();

        // No protection plugin registered: the probe is not cancelled, so mining proceeds.
        BlockMiner miner = buildMinerFacingStone(world, owner);

        for (int i = 0; i < 200; i++) {
            miner.tick();
        }

        assertEquals(Material.AIR, world.getBlockAt(0, 64, -1).getType(),
                "Machine failed to mine a block its owner is allowed to break");
    }
}
