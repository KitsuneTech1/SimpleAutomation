package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FarmerFuelTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(SimpleAutomation.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void cropFarmerDoesNotHarvestWithoutFuel() {
        WorldMock world = server.addSimpleWorld("world");
        CropFarmer farmer = new CropFarmer(new Location(world, 0, 64, 0), BlockFace.NORTH,
                UUID.randomUUID());
        farmer.tick();

        assertEquals(Machine.MachineStatus.NO_FUEL, farmer.getStatus(),
                "Crop farmer did not stop when it had no fuel");
    }

    @Test
    void treeFarmerDoesNotHarvestWithoutFuel() {
        WorldMock world = server.addSimpleWorld("world");
        Location base = new Location(world, 0, 64, -1);
        world.getBlockAt(0, 63, -1).setType(Material.DIRT);
        world.getBlockAt(base).setType(Material.OAK_LOG);
        world.getBlockAt(0, 65, -1).setType(Material.OAK_LOG);

        TreeFarmer farmer = new TreeFarmer(new Location(world, 0, 64, 0), BlockFace.NORTH,
                UUID.randomUUID());
        farmer.tick();

        assertEquals(Machine.MachineStatus.NO_FUEL, farmer.getStatus(),
                "Tree farmer did not stop when it had no fuel");
    }
}
