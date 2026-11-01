package com.simpleautomation;

import com.simpleautomation.commands.AutomationCommand;
import com.simpleautomation.economy.EconomyHook;
import com.simpleautomation.listeners.InteractListener;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.managers.ConveyorManager;
import com.simpleautomation.managers.MachineManager;
import com.simpleautomation.recipes.RecipeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleAutomation extends JavaPlugin {

    private static SimpleAutomation instance;
    private MachineManager machineManager;
    private ConveyorManager conveyorManager;
    private RecipeManager recipeManager;
    private EconomyHook economyHook;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        economyHook = new EconomyHook(this);
        machineManager = new MachineManager(this);
        conveyorManager = new ConveyorManager(this);
        recipeManager = new RecipeManager(this);

        getServer().getPluginManager().registerEvents(new PlacementListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);

        getCommand("automation").setExecutor(new AutomationCommand(this));

        recipeManager.registerRecipes();

        machineManager.loadMachines();

        startMachineTick();
        machineManager.startAutosave();

        getLogger().info("SimpleAutomation v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Loaded " + machineManager.getMachineCount() + " machines.");
    }

    @Override
    public void onDisable() {
        if (machineManager != null) {
            machineManager.stopAutosave();
            machineManager.saveMachines();
        }

        if (conveyorManager != null) {
            conveyorManager.shutdown();
        }

        getLogger().info("SimpleAutomation disabled!");
    }
    
    private void startMachineTick() {
        int interval = Math.max(1, getConfig().getInt("general.tick-interval", 10));
        getServer().getScheduler().runTaskTimer(this, () -> {
            machineManager.tickAllMachines();
            conveyorManager.tick();
        }, interval, interval);
    }
    
    public static SimpleAutomation getInstance() {
        return instance;
    }
    
    public MachineManager getMachineManager() {
        return machineManager;
    }
    
    public ConveyorManager getConveyorManager() {
        return conveyorManager;
    }
    
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }
}
