package com.simpleautomation.managers;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.Machine;
import com.simpleautomation.machines.Quarry;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug 4: machines were only written to disk onDisable, so a crash lost everything. The
 * manager now tracks a dirty flag and exposes flush() (also driven by a periodic async
 * autosave). This test creates a machine, flushes, reloads a fresh manager and asserts it
 * persisted with its upgrades intact.
 */
class AutosaveTest {

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
    void machinePersistsAcrossFlushAndReload() {
        WorldMock world = server.addSimpleWorld("world");

        MachineManager manager = plugin.getMachineManager();
        assertFalse(manager.isDirty(), "A freshly loaded manager should not be dirty");

        Location loc = new Location(world, 5, 64, 5);
        Quarry quarry = new Quarry(loc, BlockFace.NORTH, UUID.randomUUID());
        quarry.setUpgradeLevel(UpgradeType.SPEED, 3);

        manager.addMachine(quarry);
        assertTrue(manager.isDirty(), "Adding a machine should mark the manager dirty");

        // Force the autosave write.
        manager.flush();
        assertFalse(manager.isDirty(), "Flushing should clear the dirty flag");

        // Reload into a brand new manager reading the same data file, simulating a restart.
        MachineManager reloaded = new MachineManager(plugin);
        reloaded.loadMachines();

        assertEquals(1, reloaded.getMachineCount(), "Machine did not survive the reload");
        Machine restored = reloaded.getMachine(quarry.getId());
        assertNotNull(restored, "Reloaded machine missing by id");
        assertEquals(3, restored.getUpgradeLevel(UpgradeType.SPEED),
                "Upgrade level was not persisted");
    }
}
