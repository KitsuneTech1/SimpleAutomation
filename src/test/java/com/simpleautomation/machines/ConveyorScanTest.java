package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug 3: the conveyor moved items by calling world.getEntitiesByClass(Item.class), a full
 * world entity scan run per belt per tick. This test uses a world that fails hard if that
 * full-world scan is ever called, and asserts the belt still moves an item using the new
 * bounded getNearbyEntities lookup.
 */
class ConveyorScanTest {

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

    /** A world that treats any full-world Item scan as a hard failure. */
    static class ScanTrapWorld extends WorldMock {
        @Override
        public <T extends Entity> Collection<T> getEntitiesByClass(Class<T> cls) {
            throw new AssertionError("Conveyor performed a full-world entity scan: getEntitiesByClass("
                    + cls.getSimpleName() + ")");
        }
    }

    @Test
    void conveyorMovesItemWithoutFullWorldScan() {
        ScanTrapWorld world = new ScanTrapWorld();
        server.addWorld(world);

        int bx = 0, by = 64, bz = 0;
        world.getBlockAt(bx, by, bz).setType(Material.BLUE_GLAZED_TERRACOTTA);

        Location beltLoc = new Location(world, bx, by, bz);
        ConveyorBelt belt = new ConveyorBelt(beltLoc, BlockFace.EAST, UUID.randomUUID());

        // Drop an item sitting on the belt (mid-block so it is not yet at the transfer edge).
        Location itemLoc = new Location(world, bx + 0.5, by + 0.5, bz + 0.5);
        Item item = world.dropItem(itemLoc, new ItemStack(Material.DIRT));
        item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

        // Old code would throw here via the ScanTrapWorld; new bounded code must not.
        belt.moveItems();

        // The belt faces EAST, so a moved item gets a positive-X velocity.
        assertTrue(item.getVelocity().getX() > 0,
                "Conveyor did not push the item along its facing direction");
    }
}
