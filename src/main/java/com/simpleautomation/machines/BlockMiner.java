package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockMiner extends Machine {

    public BlockMiner(Location location, BlockFace facing, UUID owner) {
        super(location, facing, owner);
    }

    public BlockMiner(UUID id, Location location, BlockFace facing, UUID owner, Map<UpgradeType, Integer> upgrades) {
        super(id, location, facing, owner, upgrades);
    }

    @Override
    public void tick() {
        tickCounter++;

        if (!hasPower()) {
            status = MachineStatus.NO_FUEL;
            if (tickCounter % 4 == 0)
                spawnStatusParticles();
            return;
        }

        if (!hasInventorySpace()) {
            status = MachineStatus.OUTPUT_FULL;
            if (tickCounter % 4 == 0)
                spawnStatusParticles();
            return;
        }

        int baseSpeed = SimpleAutomation.getInstance().getConfig().getInt("block-miner.base-speed", 20);
        int speedLevel = getUpgradeLevel(UpgradeType.SPEED);
        int actualSpeed = Math.max(2, baseSpeed / (1 + speedLevel));

        if (tickCounter % actualSpeed != 0) {
            return;
        }

        status = MachineStatus.RUNNING;

        if (mineBlock()) {
            consumeFuel();
        }

        if (tickCounter % 2 == 0) {
            spawnStatusParticles();
        }
    }

    private boolean mineBlock() {
        World world = location.getWorld();
        Block block = location.getBlock().getRelative(facing);

        if (block.getType().isAir() || block.isLiquid()) {
            status = MachineStatus.IDLE;
            return false;
        }

        if (block.getType() == Material.BEDROCK) {
            status = MachineStatus.IDLE;
            return false;
        }

        // Respect claims/protection: only mine where the owner is allowed to build.
        if (!canAffect(block)) {
            status = MachineStatus.IDLE;
            return false;
        }

        Collection<ItemStack> drops = getBlockDrops(block);

        world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0,
                block.getBlockData());

        block.setType(Material.AIR);

        for (ItemStack drop : drops) {
            if (!depositItems(drop)) {
                world.dropItemNaturally(location.clone().add(0.5, 1.5, 0.5), drop);
            }
        }

        playSound(Sound.BLOCK_STONE_BREAK, 0.3f, 1.2f);
        return true;
    }

    @Override
    public MachineType getType() {
        return MachineType.BLOCK_MINER;
    }

    @Override
    public String getDisplayName() {
        return "§6§lBlock Miner";
    }
}
