package com.simpleautomation.items;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.machines.Quarry;
import com.simpleautomation.machines.MachineType;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bug 5: picked-up machine items stored their upgrades as an editable hidden lore string,
 * which was trivially dupe-able / editable. Upgrades now live in the item's
 * PersistentDataContainer. This test round-trips upgrades via the PDC and proves that
 * rewriting the visible lore does not change the real upgrade levels.
 */
class UpgradePdcTest {

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
    void upgradesRoundTripViaPdcAndIgnoreLoreEdits() {
        WorldMock world = server.addSimpleWorld("world");
        Location loc = new Location(world, 0, 64, 0);

        Quarry machine = new Quarry(loc, BlockFace.NORTH, UUID.randomUUID());
        machine.setUpgradeLevel(UpgradeType.SPEED, 4);
        machine.setUpgradeLevel(UpgradeType.AREA, 2);

        // "break" the machine into an item.
        ItemStack item = WrenchItem.createUpgradedMachineItem(machine);

        // The authoritative data must live in the PDC, not the lore.
        String pdcData = item.getItemMeta().getPersistentDataContainer()
                .get(WrenchItem.upgradesKey(), PersistentDataType.STRING);
        assertNotNull(pdcData, "Upgrades were not stored in the PersistentDataContainer");

        // "place" it: upgrades restored from the item carry the correct levels.
        Map<UpgradeType, Integer> restored = WrenchItem.extractUpgrades(item);
        assertEquals(4, restored.getOrDefault(UpgradeType.SPEED, 0));
        assertEquals(2, restored.getOrDefault(UpgradeType.AREA, 0));

        // Now maliciously rewrite the visible lore, including a fake upgrade line that the
        // old lore-based system would have trusted. The real upgrades must not change.
        ItemMeta meta = item.getItemMeta();
        meta.setLore(List.of(
                "§7Hacked display text",
                "§0§oUPGRADES:SPEED=99;AREA=99;"));
        item.setItemMeta(meta);

        Map<UpgradeType, Integer> afterEdit = WrenchItem.extractUpgrades(item);
        assertEquals(4, afterEdit.getOrDefault(UpgradeType.SPEED, 0),
                "Editing the display lore changed the real SPEED upgrade");
        assertEquals(2, afterEdit.getOrDefault(UpgradeType.AREA, 0),
                "Editing the display lore changed the real AREA upgrade");
    }

    @Test
    void upgradeLevelsAboveTheirDeclaredMaximumAreIgnored() {
        ItemStack item = PlacementListener.createMachineItem(MachineType.QUARRY);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(WrenchItem.upgradesKey(), PersistentDataType.STRING,
                "SPEED=999;");
        item.setItemMeta(meta);

        assertTrue(WrenchItem.extractUpgrades(item).isEmpty(),
                "An out-of-range PDC upgrade level was accepted");
    }

    @Test
    void preservationSettingCanDisableUpgradeTransfer() {
        plugin.getConfig().set("wrench.preserve-upgrades", false);
        WorldMock world = server.addSimpleWorld("world");
        Quarry machine = new Quarry(new Location(world, 0, 64, 0), BlockFace.NORTH, UUID.randomUUID());
        machine.setUpgradeLevel(UpgradeType.SPEED, 4);

        ItemStack item = WrenchItem.createUpgradedMachineItem(machine);

        assertTrue(WrenchItem.extractUpgrades(item).isEmpty(),
                "Wrench preservation setting was ignored");
    }
}
