package com.simpleautomation.gui;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.MachineType;
import com.simpleautomation.upgrades.UpgradeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Config fix: the conveyor config had no upgrades subsection, so a conveyor Speed upgrade
 * resolved to a null cost and could never be bought. This asserts the conveyor Speed upgrade
 * cost now resolves from the conveyor's own config section.
 */
class ConveyorUpgradeConfigTest {

    private SimpleAutomation plugin;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void conveyorSpeedUpgradeCostResolvesFromConveyorConfig() {
        String path = MachineGUI.getUpgradeConfigPath(MachineType.CONVEYOR_BELT, UpgradeType.SPEED, 1);
        assertEquals("conveyor.upgrades.speed.level-1", path,
                "Conveyor upgrade did not resolve to its own config section");

        String cost = plugin.getConfig().getString(path);
        assertNotNull(cost, "Conveyor Speed upgrade cost is null (missing conveyor.upgrades section)");
        assertEquals("IRON_INGOT:16", cost, "Wrong conveyor Speed level-1 cost");

        // A deeper level must resolve too, proving the whole branch exists.
        String level3 = plugin.getConfig()
                .getString(MachineGUI.getUpgradeConfigPath(MachineType.CONVEYOR_BELT, UpgradeType.SPEED, 3));
        assertEquals("DIAMOND:2", level3, "Wrong conveyor Speed level-3 cost");
    }
}
