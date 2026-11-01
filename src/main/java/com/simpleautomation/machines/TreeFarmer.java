package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TreeFarmer extends Machine {

    private static final Set<Material> LOG_MATERIALS = EnumSet.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.CRIMSON_STEM, Material.WARPED_STEM);

    private static final Set<Material> SAPLING_MATERIALS = EnumSet.of(
            Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING,
            Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.MANGROVE_PROPAGULE, Material.CHERRY_SAPLING,
            Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS);

    private static final Set<Material> LEAF_MATERIALS = EnumSet.of(
            Material.OAK_LEAVES, Material.SPRUCE_LEAVES, Material.BIRCH_LEAVES, Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.MANGROVE_LEAVES, Material.CHERRY_LEAVES,
            Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES,
            Material.NETHER_WART_BLOCK, Material.WARPED_WART_BLOCK);

    private boolean harvesting;
    private Queue<Block> blocksToHarvest;
    private Location treeBaseLocation;

    public TreeFarmer(Location location, BlockFace facing, UUID owner) {
        super(location, facing, owner);
        this.harvesting = false;
        this.blocksToHarvest = new LinkedList<>();
    }

    public TreeFarmer(UUID id, Location location, BlockFace facing, UUID owner, Map<UpgradeType, Integer> upgrades) {
        super(id, location, facing, owner, upgrades);
        this.harvesting = false;
        this.blocksToHarvest = new LinkedList<>();
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

        int checkInterval = SimpleAutomation.getInstance().getConfig().getInt("tree-farmer.check-interval", 100);
        int harvestSpeed = SimpleAutomation.getInstance().getConfig().getInt("tree-farmer.harvest-speed", 5);
        int speedLevel = getUpgradeLevel(UpgradeType.SPEED);
        harvestSpeed = Math.max(1, harvestSpeed - speedLevel);

        if (harvesting && !blocksToHarvest.isEmpty()) {
            status = MachineStatus.RUNNING;

            if (tickCounter % harvestSpeed == 0 && harvestNextBlock()) {
                consumeFuel();
            }

            if (tickCounter % 4 == 0)
                spawnStatusParticles();
            return;
        }

        if (tickCounter % checkInterval == 0) {
            Block treeBase = findTreeBase();
            if (treeBase != null) {
                startHarvest(treeBase);
            } else {
                status = MachineStatus.IDLE;
            }
        }

        if (tickCounter % 10 == 0)
            spawnStatusParticles();
    }

    private Block findTreeBase() {
        int range = 1 + getUpgradeLevel(UpgradeType.RANGE);
        boolean multiTree = getUpgradeLevel(UpgradeType.MULTI_TREE) > 0;

        for (int dist = 1; dist <= range; dist++) {
            Block check = location.getBlock().getRelative(facing, dist);

            if (LOG_MATERIALS.contains(check.getType())) {
                // Verify it's a full tree (has logs above), not just a single placed log
                Block above = check.getRelative(BlockFace.UP);
                if (LOG_MATERIALS.contains(above.getType())) {
                    return check;
                }
            }

            if (multiTree) {
                for (BlockFace side : new BlockFace[] { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH,
                        BlockFace.SOUTH }) {
                    Block adjacent = check.getRelative(side);
                    if (LOG_MATERIALS.contains(adjacent.getType())) {
                        Block above = adjacent.getRelative(BlockFace.UP);
                        if (LOG_MATERIALS.contains(above.getType())) {
                            return adjacent;
                        }
                    }
                }
            }
        }

        return null;
    }

    private void startHarvest(Block treeBase) {
        harvesting = true;
        blocksToHarvest.clear();
        this.treeBaseLocation = treeBase.getLocation();

        // Flood fill to find all connected logs and leaves
        Set<Block> visited = new HashSet<>();
        Queue<Block> toVisit = new LinkedList<>();
        toVisit.add(treeBase);

        int maxHeight = SimpleAutomation.getInstance().getConfig().getInt("tree-farmer.max-tree-height", 32);
        boolean instantLeafDecay = getUpgradeLevel(UpgradeType.LEAF_DECAY) > 0;

        while (!toVisit.isEmpty() && visited.size() < 1000) {
            Block current = toVisit.poll();
            if (visited.contains(current))
                continue;

            if (current.getY() > treeBase.getY() + maxHeight)
                continue;

            Material type = current.getType();
            if (LOG_MATERIALS.contains(type)) {
                visited.add(current);
                blocksToHarvest.add(current);

                for (BlockFace face : BlockFace.values()) {
                    if (face == BlockFace.SELF)
                        continue;
                    Block adjacent = current.getRelative(face);
                    if (!visited.contains(adjacent)) {
                        toVisit.add(adjacent);
                    }
                }
            } else if (LEAF_MATERIALS.contains(type) && instantLeafDecay) {
                visited.add(current);
                blocksToHarvest.add(current);

                // Only add adjacent leaves, not logs (to avoid spreading too far)
                for (BlockFace face : BlockFace.values()) {
                    if (face == BlockFace.SELF)
                        continue;
                    Block adjacent = current.getRelative(face);
                    if (!visited.contains(adjacent) &&
                            (LOG_MATERIALS.contains(adjacent.getType())
                                    || LEAF_MATERIALS.contains(adjacent.getType()))) {
                        toVisit.add(adjacent);
                    }
                }
            }
        }

        // Sort blocks top to bottom for natural-looking harvest
        List<Block> sorted = new ArrayList<>(blocksToHarvest);
        sorted.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        blocksToHarvest = new LinkedList<>(sorted);

        playSound(Sound.BLOCK_WOOD_HIT, 0.5f, 0.8f);
    }

    private boolean harvestNextBlock() {
        if (blocksToHarvest.isEmpty()) {
            harvesting = false;
            replantSapling();
            return false;
        }

        Block block = blocksToHarvest.poll();
        Material type = block.getType();

        if (!LOG_MATERIALS.contains(type) && !LEAF_MATERIALS.contains(type)) {
            return false; // Block was already broken or changed
        }

        // Respect claims/protection: only harvest where the owner is allowed to build.
        if (!canAffect(block)) {
            return false;
        }

        Collection<ItemStack> drops = block.getDrops();
        block.setType(Material.AIR);

        for (ItemStack drop : drops) {
            if (!depositItems(drop)) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
            }
        }

        playSound(Sound.BLOCK_WOOD_BREAK, 0.3f, 1.0f);
        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5),
                10, 0.3, 0.3, 0.3, 0, type.createBlockData());
        return true;
    }

    private void replantSapling() {
        if (treeBaseLocation == null) {
            return;
        }

        Block plantBlock = treeBaseLocation.getBlock();

        if (!plantBlock.getType().isAir() && plantBlock.getType() != Material.SHORT_GRASS) {
            return;
        }

        Block soilBlock = plantBlock.getRelative(BlockFace.DOWN);
        if (!isSoil(soilBlock.getType())) {
            return;
        }

        // Respect claims/protection: only replant where the owner is allowed to build.
        if (!canAffect(plantBlock)) {
            return;
        }

        var inv = findAdjacentInventory();
        if (inv == null) {
            return;
        }

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && SAPLING_MATERIALS.contains(item.getType())) {
                Material saplingType = item.getType();
                plantBlock.setType(saplingType);

                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    inv.setItem(i, null);
                }

                playSound(Sound.BLOCK_GRASS_PLACE, 0.5f, 1.2f);
                plantBlock.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        plantBlock.getLocation().add(0.5, 0.5, 0.5), 5, 0.2, 0.2, 0.2, 0);
                return;
            }
        }
    }

    private boolean isSoil(Material type) {
        return type == Material.DIRT || type == Material.GRASS_BLOCK ||
                type == Material.PODZOL || type == Material.MYCELIUM ||
                type == Material.ROOTED_DIRT || type == Material.COARSE_DIRT ||
                type == Material.SOUL_SOIL || type == Material.CRIMSON_NYLIUM ||
                type == Material.WARPED_NYLIUM || type == Material.MUD ||
                type == Material.FARMLAND || type == Material.MOSS_BLOCK;
    }

    @Override
    public MachineType getType() {
        return MachineType.TREE_FARMER;
    }

    @Override
    public void serialize(ConfigurationSection section) {
        super.serialize(section);
        if (treeBaseLocation != null) {
            section.set("treeBase.world", treeBaseLocation.getWorld().getName());
            section.set("treeBase.x", treeBaseLocation.getX());
            section.set("treeBase.y", treeBaseLocation.getY());
            section.set("treeBase.z", treeBaseLocation.getZ());
        }
    }

    public void loadState(ConfigurationSection section) {
        super.loadBaseState(section);
        if (section.contains("treeBase")) {
            String worldName = section.getString("treeBase.world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = section.getDouble("treeBase.x");
                double y = section.getDouble("treeBase.y");
                double z = section.getDouble("treeBase.z");
                this.treeBaseLocation = new Location(world, x, y, z);
            }
        }
    }

    @Override
    public String getDisplayName() {
        return "§2§lTree Farmer";
    }
}
