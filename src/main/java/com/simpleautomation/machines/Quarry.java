package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Quarry extends Machine {

    private int currentX;
    private int currentY;
    private int currentZ;
    private int minX, maxX, minZ, maxZ;
    private int startY;
    private boolean initialized;

    private static final Set<Material> VOID_FILTER_MATERIALS = new HashSet<>();

    static {
        VOID_FILTER_MATERIALS.addAll(Arrays.asList(
                Material.COBBLESTONE, Material.DIRT, Material.GRAVEL, Material.STONE,
                Material.ANDESITE, Material.DIORITE, Material.GRANITE,
                Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.TUFF, Material.NETHERRACK));
    }

    public Quarry(Location location, BlockFace facing, UUID owner) {
        super(location, facing, owner);
        this.initialized = false;
    }

    public Quarry(UUID id, Location location, BlockFace facing, UUID owner, Map<UpgradeType, Integer> upgrades) {
        super(id, location, facing, owner, upgrades);
        this.initialized = false;
    }

    private void initialize() {
        if (initialized)
            return;

        calculateBounds();

        currentX = minX;
        currentY = startY;
        currentZ = minZ;
        initialized = true;
    }

    private void calculateBounds() {
        int area = getArea();
        int halfArea = area / 2;

        Location front = location.clone().add(facing.getModX(), 0, facing.getModZ());

        switch (facing) {
            case NORTH -> {
                minX = front.getBlockX() - halfArea;
                maxX = front.getBlockX() + halfArea;
                minZ = front.getBlockZ() - (area - 1);
                maxZ = front.getBlockZ();
            }
            case SOUTH -> {
                minX = front.getBlockX() - halfArea;
                maxX = front.getBlockX() + halfArea;
                minZ = front.getBlockZ();
                maxZ = front.getBlockZ() + (area - 1);
            }
            case EAST -> {
                minX = front.getBlockX();
                maxX = front.getBlockX() + (area - 1);
                minZ = front.getBlockZ() - halfArea;
                maxZ = front.getBlockZ() + halfArea;
            }
            case WEST -> {
                minX = front.getBlockX() - (area - 1);
                maxX = front.getBlockX();
                minZ = front.getBlockZ() - halfArea;
                maxZ = front.getBlockZ() + halfArea;
            }
            default -> {
                minX = front.getBlockX() - halfArea;
                maxX = front.getBlockX() + halfArea;
                minZ = front.getBlockZ() - halfArea;
                maxZ = front.getBlockZ() + halfArea;
            }
        }

        startY = location.getBlockY() - 1;
    }

    @Override
    public void tick() {
        tickCounter++;

        if (!initialized) {
            initialize();
        }

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

        int baseSpeed = SimpleAutomation.getInstance().getConfig().getInt("quarry.base-speed", 40);
        int speedLevel = getUpgradeLevel(UpgradeType.SPEED);
        int actualSpeed = Math.max(2, baseSpeed / (1 + speedLevel));

        // Mine once every "interval" machine ticks. Clamp to at least 1 so upgraded
        // quarries (small actualSpeed) never produce a zero divisor, which used to
        // throw ArithmeticException every tick and silently stall the machine.
        int interval = Math.max(1, actualSpeed / 10);
        if (tickCounter % interval != 0) {
            return;
        }

        status = MachineStatus.RUNNING;

        if (mineCurrentBlock()) {
            consumeFuel();
        }

        advancePosition();

        if (tickCounter % 2 == 0) {
            spawnStatusParticles();
            spawnMiningParticles();
        }
    }

    private boolean mineCurrentBlock() {
        World world = location.getWorld();
        if (currentY < world.getMinHeight()) {
            status = MachineStatus.IDLE;
            return false;
        }

        Block block = world.getBlockAt(currentX, currentY, currentZ);

        if (block.getType().isAir() || block.isLiquid()) {
            return false;
        }

        if (block.getType() == Material.BEDROCK) {
            return true;
        }

        // Respect claims/protection: skip blocks the owner is not allowed to break. The
        // quarry simply advances past them instead of grinding through a claim.
        if (!canAffect(block)) {
            return false;
        }

        boolean hasVoidFilter = getUpgradeLevel(UpgradeType.VOID_FILTER) > 0;
        if (hasVoidFilter && VOID_FILTER_MATERIALS.contains(block.getType())) {
            block.setType(Material.AIR);
            spawnVoidParticles(block.getLocation());
            return true;
        }

        Collection<ItemStack> drops = getBlockDrops(block);
        block.setType(Material.AIR);

        for (ItemStack drop : drops) {
            if (!depositItems(drop)) {
                world.dropItemNaturally(location.clone().add(0.5, 1.5, 0.5), drop);
            }
        }

        playSound(Sound.BLOCK_STONE_BREAK, 0.3f, 1.0f);
        return true;
    }

    private void advancePosition() {
        currentX++;
        if (currentX > maxX) {
            currentX = minX;
            currentZ++;
            if (currentZ > maxZ) {
                currentZ = minZ;
                currentY--;
            }
        }
    }

    private int getArea() {
        int base = SimpleAutomation.getInstance().getConfig().getInt("quarry.default-area", 3);
        int areaLevel = getUpgradeLevel(UpgradeType.AREA);
        return base + (areaLevel * 2);
    }

    private void spawnMiningParticles() {
        Location particleLoc = new Location(location.getWorld(), currentX + 0.5, currentY + 0.5, currentZ + 0.5);
        location.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 5, 0.3, 0.3, 0.3, 0,
                Material.STONE.createBlockData());
    }

    private void spawnVoidParticles(Location loc) {
        loc.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
    }

    @Override
    public MachineType getType() {
        return MachineType.QUARRY;
    }

    @Override
    public String getDisplayName() {
        return "§6§lQuarry";
    }

    @Override
    public void serialize(ConfigurationSection section) {
        super.serialize(section);
        section.set("currentX", currentX);
        section.set("currentY", currentY);
        section.set("currentZ", currentZ);
        section.set("initialized", initialized);
    }

    public void loadState(ConfigurationSection section) {
        super.loadBaseState(section);
        boolean savedInitialized = section.getBoolean("initialized", false);
        if (savedInitialized) {
            calculateBounds();
            this.currentX = section.getInt("currentX", minX);
            this.currentY = section.getInt("currentY", startY);
            this.currentZ = section.getInt("currentZ", minZ);
            this.initialized = true;
        } else {
            this.initialized = false;
        }
    }
}
