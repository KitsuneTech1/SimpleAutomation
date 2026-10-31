package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Bug 1: a max-speed quarry used to compute tickCounter % (actualSpeed / 10), and at
 * upgraded speeds actualSpeed / 10 rounds down to 0, throwing ArithmeticException every
 * tick. This test drives a fully upgraded quarry many times and asserts it never throws
 * and actually mines blocks.
 */
class QuarrySpeedTest {

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
    void upgradedQuarryTicksWithoutThrowingAndMines() {
        WorldMock world = server.addSimpleWorld("world");

        int qx = 0, qy = 64, qz = 0;

        // Fill a solid stone region the quarry will mine: front is NORTH (z negative),
        // area 3 -> x in [-1,1], z in [-3,-1], starting one block below the quarry.
        for (int x = -1; x <= 1; x++) {
            for (int z = -3; z <= -1; z++) {
                for (int y = 60; y <= 63; y++) {
                    world.getBlockAt(x, y, z).setType(Material.STONE);
                }
            }
        }

        // Adjacent chest supplies fuel and receives output.
        world.getBlockAt(qx + 1, qy, qz).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(qx + 1, qy, qz).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.COAL, 64));

        Location loc = new Location(world, qx, qy, qz);
        Quarry quarry = new Quarry(loc, BlockFace.NORTH, UUID.randomUUID());

        // Max out the speed upgrade: this is exactly the condition that used to crash.
        quarry.setUpgradeLevel(UpgradeType.SPEED, UpgradeType.SPEED.getMaxLevel());

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 300; i++) {
                quarry.tick();
            }
        }, "Upgraded quarry threw while ticking (divide-by-zero in speed formula)");

        // The first target block in the mining region must have actually been mined.
        assertEquals(Material.AIR, world.getBlockAt(-1, 63, -3).getType(),
                "Upgraded quarry did not mine the first target block");
    }
}
