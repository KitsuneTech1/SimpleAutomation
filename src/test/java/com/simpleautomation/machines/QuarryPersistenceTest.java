package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.managers.MachineManager;
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

class QuarryPersistenceTest {

    private ServerMock server;
    private SimpleAutomation plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
        plugin.getConfig().set("require-build-access", false);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void reloadedQuarryKeepsItsConfiguredMiningBounds() {
        WorldMock world = server.addSimpleWorld("world");
        Location location = new Location(world, 0, 64, 0);
        world.getBlockAt(location).setType(Material.DROPPER);
        for (int x = -1; x <= 1; x++) {
            for (int z = -3; z <= -1; z++) {
                world.getBlockAt(x, 63, z).setType(Material.STONE);
            }
        }

        world.getBlockAt(1, 64, 0).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(1, 64, 0).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.COAL, 64));

        Quarry quarry = new Quarry(location, BlockFace.NORTH, UUID.randomUUID());
        MachineManager manager = plugin.getMachineManager();
        manager.addMachine(quarry);
        for (int i = 0; i < 4; i++) {
            quarry.tick();
        }
        manager.flush();

        MachineManager reloaded = new MachineManager(plugin);
        reloaded.loadMachines();
        for (int i = 0; i < 8; i++) {
            reloaded.tickAllMachines();
        }

        assertEquals(Material.AIR, world.getBlockAt(1, 63, -3).getType(),
                "Reloaded quarry did not continue across its saved area bounds");
    }
}
