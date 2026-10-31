package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public abstract class Machine {

    protected final UUID id;
    protected final Location location;
    protected final BlockFace facing;
    protected final UUID owner;
    protected final Map<UpgradeType, Integer> upgrades;
    protected MachineStatus status;
    protected int tickCounter;
    protected int fuelStored;

    /**
     * Set for the duration of a build-access probe so our own block listeners can tell a
     * dummy probe event apart from a real player break/place and ignore it. Static because
     * a probe is fully synchronous on the main thread.
     */
    private static boolean probing = false;

    public static boolean isProbing() {
        return probing;
    }

    public enum MachineStatus {
        RUNNING,
        NO_FUEL,
        OUTPUT_FULL,
        IDLE,
        DISABLED
    }

    public Machine(Location location, BlockFace facing, UUID owner) {
        this.id = UUID.randomUUID();
        this.location = location.clone();
        this.facing = facing;
        this.owner = owner;
        this.upgrades = new EnumMap<>(UpgradeType.class);
        this.status = MachineStatus.IDLE;
        this.tickCounter = 0;
        this.fuelStored = 0;
    }

    public Machine(UUID id, Location location, BlockFace facing, UUID owner, Map<UpgradeType, Integer> upgrades) {
        this.id = id;
        this.location = location.clone();
        this.facing = facing;
        this.owner = owner;
        this.upgrades = new EnumMap<>(upgrades);
        this.status = MachineStatus.IDLE;
        this.tickCounter = 0;
        this.fuelStored = 0;
    }

    public abstract void tick();

    public abstract MachineType getType();

    public abstract String getDisplayName();

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location.clone();
    }

    public BlockFace getFacing() {
        return facing;
    }

    public UUID getOwner() {
        return owner;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public int getUpgradeLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeLevel(UpgradeType type, int level) {
        if (level <= 0) {
            upgrades.remove(type);
        } else {
            upgrades.put(type, level);
        }
    }

    public Map<UpgradeType, Integer> getUpgrades() {
        return new EnumMap<>(upgrades);
    }

    public boolean isValid() {
        Block block = location.getBlock();
        return block.getType() == getType().getBlockMaterial();
    }

    protected Inventory findAdjacentInventory() {
        BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP,
                BlockFace.DOWN };
        for (BlockFace face : faces) {
            Block adjacent = location.getBlock().getRelative(face);
            if (adjacent.getState() instanceof InventoryHolder holder) {
                return holder.getInventory();
            }
        }
        return null;
    }

    protected boolean depositItems(ItemStack... items) {
        Inventory inv = findAdjacentInventory();
        if (inv == null)
            return false;

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir())
                continue;
            Map<Integer, ItemStack> leftover = inv.addItem(item.clone());
            if (!leftover.isEmpty()) {
                status = MachineStatus.OUTPUT_FULL;
                return false;
            }
        }
        return true;
    }

    protected boolean hasInventorySpace() {
        Inventory inv = findAdjacentInventory();
        if (inv == null)
            return false;
        return inv.firstEmpty() != -1;
    }

    protected void spawnStatusParticles() {
        if (!SimpleAutomation.getInstance().getConfig().getBoolean("particles.enabled", true)) {
            return;
        }

        Location particleLoc = location.clone().add(0.5, 1.2, 0.5);
        double density = SimpleAutomation.getInstance().getConfig().getDouble("particles.density", 1.0);
        int count = (int) (3 * density);

        switch (status) {
            case RUNNING -> {
                location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, count, 0.2, 0.1, 0.2, 0);
            }
            case NO_FUEL -> {
                location.getWorld().spawnParticle(Particle.SMOKE, particleLoc, count, 0.2, 0.1, 0.2, 0.01);
            }
            case OUTPUT_FULL -> {
                location.getWorld().spawnParticle(Particle.DUST, particleLoc, count, 0.2, 0.1, 0.2, 0,
                        new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f));
            }
            case IDLE -> {
                if (tickCounter % 4 == 0) {
                    location.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            default -> {
            }
        }
    }

    protected void playSound(Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    public void serialize(ConfigurationSection section) {
        section.set("type", getType().name());
        section.set("id", id.toString());
        section.set("world", location.getWorld().getName());
        section.set("x", location.getBlockX());
        section.set("y", location.getBlockY());
        section.set("z", location.getBlockZ());
        section.set("facing", facing.name());
        // owner may be null for machines migrated from data saved before ownership existed.
        if (owner != null) {
            section.set("owner", owner.toString());
        }
        section.set("fuelStored", fuelStored);

        ConfigurationSection upgradeSection = section.createSection("upgrades");
        for (Map.Entry<UpgradeType, Integer> entry : upgrades.entrySet()) {
            upgradeSection.set(entry.getKey().name(), entry.getValue());
        }
    }

    public void loadBaseState(ConfigurationSection section) {
        this.fuelStored = section.getInt("fuelStored", 0);
    }

    protected boolean hasPower() {
        if (fuelStored > 0) {
            return true;
        }

        Inventory inv = findAdjacentInventory();
        if (inv != null) {
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && isFuel(item)) {
                    int fuelValue = getFuelValue(item);
                    if (fuelValue > 0) {
                        fuelStored += fuelValue;
                        item.setAmount(item.getAmount() - 1);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean isFuel(ItemStack item) {
        return getFuelValue(item) > 0;
    }

    protected int getFuelValue(ItemStack item) {
        Material mat = item.getType();
        double value = switch (mat) {
            case COAL, CHARCOAL -> 8;
            case COAL_BLOCK -> 80;
            case LAVA_BUCKET -> 100;
            case BLAZE_ROD -> 12;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG -> 1.5;
            case OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD, JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD -> 1.5;
            case OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS -> 1.5;
            case STICK -> 0.5;
            default -> {
                if (mat.name().contains("PLANKS") || mat.name().contains("LOG") || mat.name().contains("WOOD"))
                    yield 1.5;
                yield 0.0;
            }
        };
        return (int) value;
    }

    protected void consumeFuel() {
        int efficiencyLevel = getUpgradeLevel(UpgradeType.EFFICIENCY);
        if (Math.random() < (efficiencyLevel * 0.2)) {
            return;
        }
        if (fuelStored > 0) {
            fuelStored--;
        }
    }

    /**
     * Build-access gate. Returns true only if this machine is allowed to break or place
     * the given block. When {@code require-build-access} is on we fire a dummy
     * {@link BlockBreakEvent} as the machine's owner and treat a cancellation by any other
     * plugin (WorldGuard, GriefPrevention, Towny, ...) as "denied", the standard canBuild
     * pattern. This is what stops a quarry from chewing through another player's claim or
     * chest.
     *
     * <p>The owner is resolved to an online {@link Player}; if the owner is offline (or the
     * machine was migrated with no owner) we cannot run a live probe, so we allow the
     * action rather than freezing every machine whenever its owner logs off.
     */
    protected boolean canAffect(Block block) {
        if (!SimpleAutomation.getInstance().getConfig().getBoolean("require-build-access", true)) {
            return true;
        }
        if (owner == null) {
            return true;
        }
        Player ownerPlayer = Bukkit.getPlayer(owner);
        if (ownerPlayer == null) {
            return true;
        }

        probing = true;
        try {
            BlockBreakEvent probe = new BlockBreakEvent(block, ownerPlayer);
            Bukkit.getPluginManager().callEvent(probe);
            return !probe.isCancelled();
        } finally {
            probing = false;
        }
    }

    /**
     * Drops for a mined block, respecting silk touch and fortune upgrade levels.
     * Shared by the machines that mine terrain (Quarry, Block Miner).
     */
    protected Collection<ItemStack> getBlockDrops(Block block) {
        boolean silkTouch = getUpgradeLevel(UpgradeType.SILK_TOUCH) > 0;
        int fortuneLevel = getUpgradeLevel(UpgradeType.FORTUNE);

        if (silkTouch) {
            return Collections.singletonList(new ItemStack(block.getType()));
        }

        ItemStack tool = new ItemStack(Material.DIAMOND_PICKAXE);
        if (fortuneLevel > 0) {
            tool.addEnchantment(Enchantment.FORTUNE, fortuneLevel);
        }

        return block.getDrops(tool);
    }
}
