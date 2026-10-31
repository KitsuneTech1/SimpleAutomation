package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CropFarmer extends Machine {

    private static final Map<Material, Material> CROP_TO_SEED = new HashMap<>();

    static {
        CROP_TO_SEED.put(Material.WHEAT, Material.WHEAT_SEEDS);
        CROP_TO_SEED.put(Material.CARROTS, Material.CARROT);
        CROP_TO_SEED.put(Material.POTATOES, Material.POTATO);
        CROP_TO_SEED.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        CROP_TO_SEED.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    private List<Block> cropsToHarvest;
    private int harvestIndex;

    public CropFarmer(Location location, BlockFace facing, UUID owner) {
        super(location, facing, owner);
        this.cropsToHarvest = new ArrayList<>();
        this.harvestIndex = 0;
    }

    public CropFarmer(UUID id, Location location, BlockFace facing, UUID owner, Map<UpgradeType, Integer> upgrades) {
        super(id, location, facing, owner, upgrades);
        this.cropsToHarvest = new ArrayList<>();
        this.harvestIndex = 0;
    }

    @Override
    public void tick() {
        tickCounter++;

        int checkInterval = SimpleAutomation.getInstance().getConfig().getInt("crop-farmer.check-interval", 60);
        int speedLevel = getUpgradeLevel(UpgradeType.SPEED);
        int harvestDelay = Math.max(1, 3 - speedLevel);

        if (!cropsToHarvest.isEmpty()) {
            status = MachineStatus.RUNNING;

            if (tickCounter % harvestDelay == 0) {
                harvestNextCrop();
            }

            if (tickCounter % 4 == 0)
                spawnStatusParticles();
            return;
        }

        if (tickCounter % checkInterval == 0) {
            scanForCrops();
        }

        if (cropsToHarvest.isEmpty()) {
            status = MachineStatus.IDLE;
        }

        if (tickCounter % 10 == 0)
            spawnStatusParticles();
    }

    private void scanForCrops() {
        cropsToHarvest.clear();
        harvestIndex = 0;

        int baseRadius = SimpleAutomation.getInstance().getConfig().getInt("crop-farmer.default-radius", 4);
        int rangeLevel = getUpgradeLevel(UpgradeType.RANGE);
        int radius = baseRadius + (rangeLevel * 2);
        int maxRadius = SimpleAutomation.getInstance().getConfig().getInt("crop-farmer.max-radius", 9);
        radius = Math.min(radius, maxRadius);

        World world = location.getWorld();
        int centerX = location.getBlockX();
        int centerY = location.getBlockY();
        int centerZ = location.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Check a few Y levels (crops might be above or below)
                for (int y = -1; y <= 1; y++) {
                    Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);

                    if (isMatureCrop(block)) {
                        cropsToHarvest.add(block);
                    }
                }
            }
        }

        if (!cropsToHarvest.isEmpty()) {
            playSound(Sound.BLOCK_CROP_BREAK, 0.3f, 1.2f);
        }
    }

    private boolean isMatureCrop(Block block) {
        Material type = block.getType();

        if (!CROP_TO_SEED.containsKey(type)) {
            return false;
        }

        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }

        return false;
    }

    private void harvestNextCrop() {
        if (harvestIndex >= cropsToHarvest.size()) {
            cropsToHarvest.clear();
            harvestIndex = 0;
            return;
        }

        Block crop = cropsToHarvest.get(harvestIndex);
        harvestIndex++;

        Material type = crop.getType();

        if (!isMatureCrop(crop)) {
            return;
        }

        // Respect claims/protection: only harvest where the owner is allowed to build.
        if (!canAffect(crop)) {
            return;
        }

        Collection<ItemStack> drops = crop.getDrops();
        Material seedType = CROP_TO_SEED.get(type);
        boolean replanted = false;

        List<ItemStack> toDeposit = new ArrayList<>();
        for (ItemStack drop : drops) {
            // Keep one seed for replanting
            if (!replanted && drop.getType() == seedType) {
                replanted = true;
                if (drop.getAmount() > 1) {
                    ItemStack remaining = drop.clone();
                    remaining.setAmount(drop.getAmount() - 1);
                    toDeposit.add(remaining);
                }
            } else {
                toDeposit.add(drop);
            }
        }

        for (ItemStack item : toDeposit) {
            if (!depositItems(item)) {
                crop.getWorld().dropItemNaturally(crop.getLocation().add(0.5, 0.5, 0.5), item);
            }
        }

        if (replanted) {
            if (crop.getBlockData() instanceof Ageable ageable) {
                ageable.setAge(0);
                crop.setBlockData(ageable);
            }
        } else {
            crop.setType(Material.AIR);
        }

        playSound(Sound.BLOCK_CROP_BREAK, 0.2f, 1.0f);
        crop.getWorld().spawnParticle(Particle.BLOCK, crop.getLocation().add(0.5, 0.3, 0.5),
                5, 0.2, 0.1, 0.2, 0, type.createBlockData());
        crop.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                crop.getLocation().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0);
    }

    @Override
    public MachineType getType() {
        return MachineType.CROP_FARMER;
    }

    @Override
    public String getDisplayName() {
        return "§a§lCrop Farmer";
    }
}
