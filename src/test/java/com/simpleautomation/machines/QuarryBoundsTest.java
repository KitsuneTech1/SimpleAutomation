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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuarryBoundsTest {

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
    void quarryStopsAtConfiguredMaximumDepth() {
        plugin.getConfig().set("require-build-access", false);
        plugin.getConfig().set("quarry.base-speed", 2);
        plugin.getConfig().set("quarry.max-depth", 1);
        WorldMock world = server.addSimpleWorld("world");
        for (int x = -1; x <= 1; x++) {
            for (int z = -3; z <= -1; z++) {
                world.getBlockAt(x, 63, z).setType(Material.STONE);
                world.getBlockAt(x, 62, z).setType(Material.STONE);
            }
        }
        world.getBlockAt(1, 64, 0).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(1, 64, 0).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.COAL, 64));

        Quarry quarry = new Quarry(new Location(world, 0, 64, 0), BlockFace.NORTH, UUID.randomUUID());
        quarry.setUpgradeLevel(UpgradeType.SPEED, UpgradeType.SPEED.getMaxLevel());
        for (int i = 0; i < 20; i++) {
            quarry.tick();
        }

        assertEquals(Material.STONE, world.getBlockAt(-1, 62, -3).getType(),
                "Quarry mined below its configured maximum depth");
    }

    @Test
    void voidFilterUsesConfiguredBlockList() {
        plugin.getConfig().set("require-build-access", false);
        plugin.getConfig().set("quarry.base-speed", 2);
        plugin.getConfig().set("quarry.void-filter-blocks", List.of("DIRT"));
        WorldMock world = server.addSimpleWorld("world");
        world.getBlockAt(-1, 63, -3).setType(Material.STONE);
        world.getBlockAt(1, 64, 0).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(1, 64, 0).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.COAL, 64));

        Quarry quarry = new Quarry(new Location(world, 0, 64, 0), BlockFace.NORTH, UUID.randomUUID());
        quarry.setUpgradeLevel(UpgradeType.VOID_FILTER, 1);
        quarry.tick();

        org.junit.jupiter.api.Assertions.assertFalse(quarry.isVoidFiltered(Material.STONE),
                "The configured void filter incorrectly includes stone");
        assertEquals(Material.AIR, world.getBlockAt(-1, 63, -3).getType());
    }
}
