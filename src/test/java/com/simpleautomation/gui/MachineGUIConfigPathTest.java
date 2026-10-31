package com.simpleautomation.gui;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.MachineType;
import com.simpleautomation.upgrades.UpgradeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Bug 2: the upgrade-cost config lookup defaulted every non-quarry machine (block miner,
 * conveyor) to the "quarry" config section, so their own upgrade config was dead and they
 * billed from quarry costs. This test asserts the block miner reads its own section.
 */
class MachineGUIConfigPathTest {

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
    void blockMinerUpgradeCostComesFromBlockMinerConfigNotQuarry() {
        // block-miner speed level-3 and quarry speed level-3 have deliberately different
        // costs in config.yml (DIAMOND:2 vs GOLD_INGOT:16), so if the path is wrong the
        // resolved cost is the quarry one.
        String path = MachineGUI.getUpgradeConfigPath(MachineType.BLOCK_MINER, UpgradeType.SPEED, 3);
        assertEquals("block-miner.upgrades.speed.level-3", path,
                "Block miner did not resolve to its own config section");

        String blockMinerCost = plugin.getConfig().getString(path);
        String quarryCost = plugin.getConfig().getString("quarry.upgrades.speed.level-3");

        assertEquals("DIAMOND:2", blockMinerCost, "Wrong block miner upgrade cost");
        assertEquals("GOLD_INGOT:16", quarryCost, "Config sanity check failed");
        assertNotEquals(quarryCost, blockMinerCost,
                "Block miner is billing from the quarry cost table");
    }
}
