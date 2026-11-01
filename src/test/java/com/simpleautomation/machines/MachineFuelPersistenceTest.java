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

import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineFuelPersistenceTest {

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

    private Chest addFuelChest(WorldMock world) {
        world.getBlockAt(1, 64, 0).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(1, 64, 0).getState();
        chest.getBlockInventory().addItem(new ItemStack(Material.COAL, 64));
        return chest;
    }

    @Test
    void blockMinerFuelSurvivesReload() {
        WorldMock world = server.addSimpleWorld("world");
        addFuelChest(world);
        world.getBlockAt(0, 64, -1).setType(Material.STONE);
        BlockMiner miner = new BlockMiner(new Location(world, 0, 64, 0), BlockFace.NORTH,
                UUID.randomUUID());
        MachineManager manager = plugin.getMachineManager();
        manager.addMachine(miner);
        miner.tick();
        int storedBefore = miner.fuelStored;
        manager.flush();

        MachineManager reloaded = new MachineManager(plugin);
        reloaded.loadMachines();
        Machine restored = reloaded.getMachine(miner.getId());

        assertTrue(storedBefore > 0 && restored instanceof BlockMiner
                        && restored.fuelStored == storedBefore,
                "Block Miner fuel was not restored from saved state");
    }

    @Test
    void cropFarmerFuelSurvivesReload() {
        WorldMock world = server.addSimpleWorld("world");
        addFuelChest(world);
        CropFarmer farmer = new CropFarmer(new Location(world, 0, 64, 0), BlockFace.NORTH,
                UUID.randomUUID());
        MachineManager manager = plugin.getMachineManager();
        manager.addMachine(farmer);
        farmer.tick();
        int storedBefore = farmer.fuelStored;
        manager.flush();

        MachineManager reloaded = new MachineManager(plugin);
        reloaded.loadMachines();
        Machine restored = reloaded.getMachine(farmer.getId());

        assertTrue(storedBefore > 0 && restored instanceof CropFarmer
                        && restored.fuelStored == storedBefore,
                "Crop Farmer fuel was not restored from saved state");
    }
}
