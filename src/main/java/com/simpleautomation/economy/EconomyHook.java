package com.simpleautomation.economy;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.MachineType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Optional Vault economy integration. When {@code economy.enabled} is true and a Vault
 * economy provider is registered, placing a machine charges its per-machine place-cost.
 *
 * <p>The economy provider is resolved lazily on first use rather than cached at enable, so
 * it works regardless of plugin load order and so nothing touches the Vault classes at all
 * when the feature is left disabled (the default). If the feature is enabled but no economy
 * is present, charging is skipped and a one-time warning is logged.
 */
public class EconomyHook {

    public enum Result {
        /** Charge succeeded (or the amount was zero). */
        SUCCESS,
        /** The player could not afford the cost; the action must be blocked. */
        INSUFFICIENT,
        /** No charge was applied (feature disabled, or enabled with no economy present). */
        SKIPPED
    }

    private final SimpleAutomation plugin;
    private Economy economy;
    private boolean warnedNoEconomy;

    public EconomyHook(SimpleAutomation plugin) {
        this.plugin = plugin;
    }

    /** True when the economy feature is turned on in config. */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("economy.enabled", false);
    }

    /** True when the feature is on AND a usable economy provider is available. */
    public boolean isActive() {
        return isEnabled() && resolve() != null;
    }

    /** Money charged to place the given machine type, from {@code <machine>.place-cost}. */
    public double getPlaceCost(MachineType type) {
        return plugin.getConfig().getDouble(type.getConfigKey() + ".place-cost", 0.0);
    }

    /**
     * Charges the player the given amount if the economy feature is active. Returns a
     * {@link Result} the caller uses to decide whether to allow the action.
     */
    public Result charge(OfflinePlayer player, double amount) {
        if (!isEnabled()) {
            return Result.SKIPPED;
        }
        Economy eco = resolve();
        if (eco == null) {
            if (!warnedNoEconomy) {
                plugin.getLogger().warning(
                        "economy.enabled is true but no Vault economy is installed; skipping charges.");
                warnedNoEconomy = true;
            }
            return Result.SKIPPED;
        }
        if (amount <= 0) {
            return Result.SUCCESS;
        }
        if (!eco.has(player, amount)) {
            return Result.INSUFFICIENT;
        }
        eco.withdrawPlayer(player, amount);
        return Result.SUCCESS;
    }

    private Economy resolve() {
        if (economy != null) {
            return economy;
        }
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        } catch (Throwable ignored) {
            // Vault classes not present at runtime: treat as no economy.
        }
        return economy;
    }
}
