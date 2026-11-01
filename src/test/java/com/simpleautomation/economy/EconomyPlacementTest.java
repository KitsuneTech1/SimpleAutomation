package com.simpleautomation.economy;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.machines.MachineType;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional Vault economy: with economy.enabled and a registered economy provider, placing a
 * machine charges its place-cost, and insufficient funds block the placement. With the
 * feature disabled (the default), placement is free and the balance is untouched.
 */
class EconomyPlacementTest {

    private ServerMock server;
    private SimpleAutomation plugin;
    private PlacementListener listener;
    private FakeEconomy economy;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SimpleAutomation.class);
        listener = new PlacementListener(plugin);
        economy = new FakeEconomy();
        server.getServicesManager().register(Economy.class, economy, plugin, ServicePriority.Normal);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private BlockPlaceEvent quarryPlaceEvent(WorldMock world, PlayerMock player) {
        Block placed = world.getBlockAt(0, 64, 0);
        BlockState replaced = placed.getState();
        placed.setType(Material.DROPPER);

        Block against = world.getBlockAt(0, 63, 0);
        against.setType(Material.STONE);

        ItemStack inHand = PlacementListener.createMachineItem(MachineType.QUARRY);
        return new BlockPlaceEvent(placed, replaced, against, inHand, player, true, EquipmentSlot.HAND);
    }

    @Test
    void placingAMachineDeductsTheConfiguredPlaceCost() {
        plugin.getConfig().set("economy.enabled", true);
        double cost = plugin.getConfig().getDouble("quarry.place-cost");
        assertTrue(cost > 0, "quarry place-cost should be configured");

        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        economy.set(player, cost + 200.0);

        BlockPlaceEvent event = quarryPlaceEvent(world, player);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "Placement was cancelled despite sufficient funds");
        assertNotNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)),
                "Machine was not placed despite sufficient funds");
        assertEquals(200.0, economy.getBalance(player), 0.0001,
                "Placing the machine did not deduct the configured place-cost");
    }

    @Test
    void insufficientFundsBlocksPlacement() {
        plugin.getConfig().set("economy.enabled", true);
        double cost = plugin.getConfig().getDouble("quarry.place-cost");

        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        economy.set(player, cost - 1.0); // one short

        BlockPlaceEvent event = quarryPlaceEvent(world, player);
        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "Placement was not cancelled for a player who cannot afford it");
        assertNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)),
                "A machine was placed despite insufficient funds");
        assertEquals(cost - 1.0, economy.getBalance(player), 0.0001,
                "Balance changed even though placement was blocked");
    }

    @Test
    void economyDisabledLeavesBalanceUntouchedAndStillPlaces() {
        // economy.enabled defaults to false; do not turn it on.
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        economy.set(player, 1000.0);

        BlockPlaceEvent event = quarryPlaceEvent(world, player);
        listener.onBlockPlace(event);

        assertFalse(event.isCancelled(), "Placement was cancelled with economy disabled");
        assertNotNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)),
                "Machine was not placed with economy disabled");
        assertEquals(1000.0, economy.getBalance(player), 0.0001,
                "Balance was charged even though economy is disabled");
    }

    @Test
    void failedWithdrawalBlocksPlacement() {
        plugin.getConfig().set("economy.enabled", true);
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();
        double cost = plugin.getConfig().getDouble("quarry.place-cost");
        economy.set(player, cost + 200.0);
        economy.setWithdrawFailure(true);

        BlockPlaceEvent event = quarryPlaceEvent(world, player);
        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "Placement proceeded after the economy rejected withdrawal");
        assertNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)),
                "A machine was registered after the economy rejected withdrawal");
        assertEquals(cost + 200.0, economy.getBalance(player), 0.0001,
                "A failed withdrawal changed the player's balance");
    }

    @Test
    void negativeConfiguredCostIsRejected() {
        plugin.getConfig().set("economy.enabled", true);
        PlayerMock player = server.addPlayer();

        assertEquals(EconomyHook.Result.FAILED,
                plugin.getEconomyHook().charge(player, -1.0),
                "A negative configured cost must not become a free purchase");
    }

    @Test
    void enabledEconomyWithoutAProviderBlocksPlacement() {
        plugin.getConfig().set("economy.enabled", true);
        server.getServicesManager().unregister(economy);
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer();

        BlockPlaceEvent event = quarryPlaceEvent(world, player);
        listener.onBlockPlace(event);

        assertTrue(event.isCancelled(), "Placement was free when the enabled economy provider was missing");
        assertNull(plugin.getMachineManager().getMachineAt(new Location(world, 0, 64, 0)));
    }

    /** Minimal in-memory Vault economy keyed by player name; only the used methods do real work. */
    static class FakeEconomy implements Economy {
        private final Map<String, Double> balances = new HashMap<>();
        private boolean withdrawFailure;

        void set(OfflinePlayer player, double amount) {
            balances.put(player.getName(), amount);
        }

        void setWithdrawFailure(boolean withdrawFailure) {
            this.withdrawFailure = withdrawFailure;
        }

        private double bal(OfflinePlayer player) {
            return balances.getOrDefault(player.getName(), 0.0);
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            return bal(player);
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return bal(player) >= amount;
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
            if (withdrawFailure) {
                return new EconomyResponse(0, bal(player), EconomyResponse.ResponseType.FAILURE,
                        "withdrawal rejected");
            }
            double newBal = bal(player) - amount;
            balances.put(player.getName(), newBal);
            return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
            double newBal = bal(player) + amount;
            balances.put(player.getName(), newBal);
            return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
        }

        // ---- everything below is unused boilerplate to satisfy the interface ----

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public String getName() {
            return "FakeEconomy";
        }

        @Override
        public boolean hasBankSupport() {
            return false;
        }

        @Override
        public int fractionalDigits() {
            return 2;
        }

        @Override
        public String format(double amount) {
            return String.valueOf(amount);
        }

        @Override
        public String currencyNamePlural() {
            return "coins";
        }

        @Override
        public String currencyNameSingular() {
            return "coin";
        }

        @Override
        public boolean hasAccount(String playerName) {
            return true;
        }

        @Override
        public boolean hasAccount(OfflinePlayer player) {
            return true;
        }

        @Override
        public boolean hasAccount(String playerName, String worldName) {
            return true;
        }

        @Override
        public boolean hasAccount(OfflinePlayer player, String worldName) {
            return true;
        }

        @Override
        public double getBalance(String playerName) {
            return balances.getOrDefault(playerName, 0.0);
        }

        @Override
        public double getBalance(String playerName, String world) {
            return getBalance(playerName);
        }

        @Override
        public double getBalance(OfflinePlayer player, String world) {
            return bal(player);
        }

        @Override
        public boolean has(String playerName, double amount) {
            return getBalance(playerName) >= amount;
        }

        @Override
        public boolean has(String playerName, String worldName, double amount) {
            return getBalance(playerName) >= amount;
        }

        @Override
        public boolean has(OfflinePlayer player, String worldName, double amount) {
            return bal(player) >= amount;
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, double amount) {
            double newBal = getBalance(playerName) - amount;
            balances.put(playerName, newBal);
            return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
            return withdrawPlayer(playerName, amount);
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
            return withdrawPlayer(player, amount);
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, double amount) {
            double newBal = getBalance(playerName) + amount;
            balances.put(playerName, newBal);
            return new EconomyResponse(amount, newBal, EconomyResponse.ResponseType.SUCCESS, null);
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
            return depositPlayer(playerName, amount);
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
            return depositPlayer(player, amount);
        }

        @Override
        public EconomyResponse createBank(String name, String player) {
            return unsupported();
        }

        @Override
        public EconomyResponse createBank(String name, OfflinePlayer player) {
            return unsupported();
        }

        @Override
        public EconomyResponse deleteBank(String name) {
            return unsupported();
        }

        @Override
        public EconomyResponse bankBalance(String name) {
            return unsupported();
        }

        @Override
        public EconomyResponse bankHas(String name, double amount) {
            return unsupported();
        }

        @Override
        public EconomyResponse bankWithdraw(String name, double amount) {
            return unsupported();
        }

        @Override
        public EconomyResponse bankDeposit(String name, double amount) {
            return unsupported();
        }

        @Override
        public EconomyResponse isBankOwner(String name, String playerName) {
            return unsupported();
        }

        @Override
        public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
            return unsupported();
        }

        @Override
        public EconomyResponse isBankMember(String name, String playerName) {
            return unsupported();
        }

        @Override
        public EconomyResponse isBankMember(String name, OfflinePlayer player) {
            return unsupported();
        }

        @Override
        public List<String> getBanks() {
            return List.of();
        }

        @Override
        public boolean createPlayerAccount(String playerName) {
            return true;
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player) {
            return true;
        }

        @Override
        public boolean createPlayerAccount(String playerName, String worldName) {
            return true;
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
            return true;
        }

        private EconomyResponse unsupported() {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "banks not supported");
        }
    }
}
