package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeFarmerSafetyTest {

    private ServerMock server;
    private SimpleAutomation plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
        plugin.getConfig().set("tree-farmer.check-interval", 1);
        plugin.getConfig().set("tree-farmer.harvest-speed", 1);
        plugin.getConfig().set("particles.enabled", false);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private TreeFarmer prepareFarmer(PlayerMock owner, WorldMock world) {
        Location machineLocation = new Location(world, 0, 64, 0);
        world.getBlockAt(machineLocation).setType(Material.DISPENSER);
        world.getBlockAt(1, 64, 0).setType(Material.OAK_LOG);
        world.getBlockAt(1, 65, 0).setType(Material.OAK_LOG);
        world.getBlockAt(1, 63, 0).setType(Material.DIRT);
        world.getBlockAt(0, 65, 0).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(0, 65, 0).getState();
        chest.getInventory().addItem(new ItemStack(Material.COAL, 1));
        chest.getInventory().addItem(new ItemStack(Material.OAK_SAPLING, 1));
        return new TreeFarmer(machineLocation, BlockFace.EAST, owner.getUniqueId());
    }

    @Test
    void completedHarvestReplantsASapling() {
        plugin.getConfig().set("require-build-access", false);
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();
        TreeFarmer farmer = prepareFarmer(owner, world);

        for (int i = 0; i < 4; i++) {
            farmer.tick();
        }

        assertEquals(Material.OAK_SAPLING, world.getBlockAt(1, 64, 0).getType(),
                "A completed tree harvest did not replant a sapling");
    }

    @Test
    void deniedPlacementPreventsAutomatedReplanting() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock owner = server.addPlayer();
        server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void deny(BlockPlaceEvent event) {
                event.setCancelled(true);
            }
        }, plugin);
        TreeFarmer farmer = prepareFarmer(owner, world);

        for (int i = 0; i < 4; i++) {
            farmer.tick();
        }

        assertEquals(Material.AIR, world.getBlockAt(1, 64, 0).getType(),
                "A cancelled placement probe did not block automated replanting");
    }
}
