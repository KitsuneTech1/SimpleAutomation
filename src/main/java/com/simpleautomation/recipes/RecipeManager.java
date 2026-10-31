package com.simpleautomation.recipes;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.items.WrenchItem;
import com.simpleautomation.listeners.PlacementListener;
import com.simpleautomation.machines.MachineType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeManager {

    private final SimpleAutomation plugin;

    public RecipeManager(SimpleAutomation plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        registerQuarryRecipe();
        registerConveyorRecipe();
        registerTreeFarmerRecipe();
        registerCropFarmerRecipe();
        registerBlockMinerRecipe();
        registerWrenchRecipe();

        plugin.getLogger().info("Registered all crafting recipes.");
    }

    private void registerQuarryRecipe() {
        ItemStack quarry = PlacementListener.createMachineItem(MachineType.QUARRY);
        NamespacedKey key = new NamespacedKey(plugin, "quarry");

        ShapedRecipe recipe = new ShapedRecipe(key, quarry);
        recipe.shape("IPI", "RDR", "IPI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('P', Material.DIAMOND_PICKAXE);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('D', Material.DROPPER);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerConveyorRecipe() {
        ItemStack conveyor = PlacementListener.createMachineItem(MachineType.CONVEYOR_BELT);
        conveyor.setAmount(8);
        NamespacedKey key = new NamespacedKey(plugin, "conveyor_belt");

        ShapedRecipe recipe = new ShapedRecipe(key, conveyor);
        recipe.shape("III", "RRR", "III");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.REDSTONE);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerTreeFarmerRecipe() {
        ItemStack farmer = PlacementListener.createMachineItem(MachineType.TREE_FARMER);
        NamespacedKey key = new NamespacedKey(plugin, "tree_farmer");

        ShapedRecipe recipe = new ShapedRecipe(key, farmer);
        recipe.shape("IAI", "RDR", "ISI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('A', Material.DIAMOND_AXE);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('D', Material.DISPENSER);
        recipe.setIngredient('S', Material.OAK_SAPLING);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerCropFarmerRecipe() {
        ItemStack farmer = PlacementListener.createMachineItem(MachineType.CROP_FARMER);
        NamespacedKey key = new NamespacedKey(plugin, "crop_farmer");

        ShapedRecipe recipe = new ShapedRecipe(key, farmer);
        recipe.shape("IHI", "RDR", "IWI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('H', Material.DIAMOND_HOE);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('D', Material.DISPENSER);
        recipe.setIngredient('W', Material.WHEAT_SEEDS);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerWrenchRecipe() {
        ItemStack wrench = WrenchItem.createWrench();
        NamespacedKey key = new NamespacedKey(plugin, "wrench");

        ShapedRecipe recipe = new ShapedRecipe(key, wrench);
        recipe.shape(" I ", " R ", "   ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.REDSTONE);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerBlockMinerRecipe() {
        ItemStack miner = PlacementListener.createMachineItem(MachineType.BLOCK_MINER);
        NamespacedKey key = new NamespacedKey(plugin, "block_miner");

        ShapedRecipe recipe = new ShapedRecipe(key, miner);
        recipe.shape("IGI", "ROR", "IPI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('G', Material.GOLDEN_PICKAXE);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('O', Material.OBSERVER);
        recipe.setIngredient('P', Material.IRON_PICKAXE);

        plugin.getServer().addRecipe(recipe);
    }
}
