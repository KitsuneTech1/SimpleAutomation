package com.simpleautomation.commands;

import com.simpleautomation.SimpleAutomation;
import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomationCommandTest {

    private ServerMock server;
    private SimpleAutomation plugin;
    private PluginCommand command;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
        command = plugin.getCommand("automation");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void documentedAdminCommandsExecuteForConsole() {
        assertNotNull(command);

        for (String subcommand : new String[]{"list", "reload", "save"}) {
            assertTrue(command.execute(server.getConsoleSender(), "automation", new String[]{subcommand}),
                    "Documented subcommand did not execute: " + subcommand);
        }
    }

    @Test
    void documentedGiveCommandCreatesMachineAndWrenchItems() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, AutomationCommand.ADMIN_PERMISSION, true);

        assertTrue(command.execute(player, "automation", new String[]{"give", "quarry"}));
        assertEquals(1, player.getInventory().getItem(0).getAmount());

        assertTrue(command.execute(player, "automation", new String[]{"give", "wrench"}));
        assertEquals(1, player.getInventory().getItem(1).getAmount());
    }
}
