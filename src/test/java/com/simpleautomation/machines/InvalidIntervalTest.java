package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InvalidIntervalTest {

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

    private void addFuel(WorldMock world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(x, y, z).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.COAL, 1));
    }

    @Test
    void zeroCropCheckIntervalDoesNotCrash() {
        plugin.getConfig().set("crop-farmer.check-interval", 0);
        WorldMock world = server.addSimpleWorld("world");
        addFuel(world, 1, 64, 0);

        CropFarmer farmer = new CropFarmer(new Location(world, 0, 64, 0), BlockFace.NORTH,
                UUID.randomUUID());
        assertDoesNotThrow(farmer::tick);
    }

    @Test
    void zeroTreeCheckIntervalDoesNotCrash() {
        plugin.getConfig().set("tree-farmer.check-interval", 0);
        WorldMock world = server.addSimpleWorld("world");
        addFuel(world, 1, 64, 0);

        TreeFarmer farmer = new TreeFarmer(new Location(world, 0, 64, 0), BlockFace.NORTH,
                UUID.randomUUID());
        assertDoesNotThrow(farmer::tick);
    }
}
