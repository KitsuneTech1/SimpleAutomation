package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConveyorSafetyTest {

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
    void rotatingConveyorUpdatesItsRuntimeDirection() {
        WorldMock world = server.addSimpleWorld("world");
        Location location = new Location(world, 0, 64, 0);
        world.getBlockAt(location).setType(Material.BLUE_GLAZED_TERRACOTTA);
        ConveyorBelt belt = new ConveyorBelt(location, BlockFace.NORTH, UUID.randomUUID());

        belt.rotate();

        assertEquals(BlockFace.EAST, belt.getFacing(),
                "Rotating a conveyor changed only the block data, not its runtime direction");
    }

    @Test
    void itemAtNonConveyorNeighborDoesNotCrashTransfer() {
        WorldMock world = server.addSimpleWorld("world");
        Location beltLocation = new Location(world, 0, 64, 0);
        world.getBlockAt(beltLocation).setType(Material.BLUE_GLAZED_TERRACOTTA);
        world.getBlockAt(1, 64, 0).setType(Material.DROPPER);

        ConveyorBelt belt = new ConveyorBelt(beltLocation, BlockFace.EAST, UUID.randomUUID());
        plugin.getMachineManager().addMachine(belt);
        plugin.getMachineManager().addMachine(new Quarry(
                new Location(world, 1, 64, 0), BlockFace.NORTH, UUID.randomUUID()));
        world.dropItem(new Location(world, 0.9, 64.5, 0.5), new ItemStack(Material.DIRT));

        assertDoesNotThrow(belt::moveItems,
                "A non-conveyor machine at the next block crashed conveyor transfer");
    }
}
