package com.simpleautomation.machines;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

public class ConveyorBelt extends Machine {

    public ConveyorBelt(Location location, BlockFace facing, UUID owner) {
        super(location, facing, owner);
    }

    public ConveyorBelt(UUID id, Location location, BlockFace facing, UUID owner, Map<UpgradeType, Integer> upgrades) {
        super(id, location, facing, owner, upgrades);
    }

    @Override
    public void tick() {
        tickCounter++;
        status = MachineStatus.RUNNING;

        if (tickCounter % 20 == 0) { // Extract once per second
            extractItems();
        }

        // Conveyor tick is handled by ConveyorManager for efficiency
        // This just spawns ambient particles
        if (tickCounter % 10 == 0) {
            spawnAmbientParticles();
        }
    }

    private void extractItems() {
        for (BlockFace face : BlockFace.values()) {
            if (face == facing || face == BlockFace.SELF || !face.isCartesian())
                continue;

            Block adjacent = location.getBlock().getRelative(face);
            if (adjacent.getState() instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();

                for (int i = 0; i < inv.getSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (stack != null && !stack.getType().isAir()) {
                        ItemStack toExtract = stack.clone();
                        toExtract.setAmount(1);
                        stack.setAmount(stack.getAmount() - 1);

                        Location spawnLoc = location.clone().add(0.5, 0.6, 0.5);
                        Item itemEntity = location.getWorld().dropItem(spawnLoc, toExtract);
                        itemEntity.setVelocity(new Vector(0, 0, 0));
                        itemEntity.setPickupDelay(40);

                        return; // Only extract one per tick cycle
                    }
                }
            }
        }
    }

    public void moveItems() {
        double speed = SimpleAutomation.getInstance().getConfig().getDouble("conveyor.speed", 2.0);
        int maxItems = SimpleAutomation.getInstance().getConfig().getInt("conveyor.max-items-per-belt", 8);
        int pickupDelay = SimpleAutomation.getInstance().getConfig().getInt("conveyor.pickup-delay", 40);

        // Only look at entities inside this belt's own block instead of scanning every
        // Item in the whole world every tick (the old getEntitiesByClass call was a
        // per-belt, per-tick lag bomb). getNearbyEntities with a half-extent of 0.5
        // around the block centre bounds the search to exactly this belt's cube.
        Location center = location.clone().add(0.5, 0.5, 0.5);
        int itemCount = 0;
        for (Entity entity : location.getWorld().getNearbyEntities(center, 0.5, 0.5, 0.5)) {
            if (!(entity instanceof Item item))
                continue;
            if (itemCount >= maxItems)
                break;

            Location itemLoc = item.getLocation();
            if (isOnBelt(itemLoc)) {
                itemCount++;

                if (shouldTransfer(item)) {
                    continue;
                }

                Vector direction = new Vector(facing.getModX(), 0, facing.getModZ());
                direction.multiply(speed * 0.05); // Scale for tick rate

                double bob = Math.sin(System.currentTimeMillis() / 200.0) * 0.02;

                item.setVelocity(direction.add(new Vector(0, bob, 0)));
                item.setPickupDelay(pickupDelay);

                if (tickCounter % 5 == 0) {
                    location.getWorld().spawnParticle(Particle.DUST, item.getLocation(), 1,
                            new Particle.DustOptions(Color.ORANGE, 0.5f));
                }
            }
        }
    }

    private boolean isOnBelt(Location itemLoc) {
        double x = itemLoc.getX() - location.getBlockX();
        double y = itemLoc.getY() - location.getBlockY();
        double z = itemLoc.getZ() - location.getBlockZ();

        return x >= 0 && x < 1 && y >= 0 && y < 1 && z >= 0 && z < 1;
    }

    private boolean shouldTransfer(Item item) {
        // Check if item is at the edge of the belt in the facing direction
        Location itemLoc = item.getLocation();
        double progress = 0;

        switch (facing) {
            case NORTH -> progress = 1 - (itemLoc.getZ() - location.getBlockZ());
            case SOUTH -> progress = itemLoc.getZ() - location.getBlockZ();
            case EAST -> progress = itemLoc.getX() - location.getBlockX();
            case WEST -> progress = 1 - (itemLoc.getX() - location.getBlockX());
            default -> {
            }
        }

        if (progress > 0.8) {
            Block nextBlock = location.getBlock().getRelative(facing);

            if (nextBlock.getState() instanceof InventoryHolder holder) {
                Inventory inv = holder.getInventory();
                ItemStack stack = item.getItemStack();
                Map<Integer, ItemStack> leftover = inv.addItem(stack.clone());
                if (leftover.isEmpty()) {
                    Location soundLoc = item.getLocation().clone();
                    item.remove();
                    location.getWorld().playSound(soundLoc, Sound.BLOCK_BARREL_CLOSE, 0.3f, 1.5f);
                    return true;
                }
            }

            // Check for next conveyor (handled by manager, just let item continue)
            ConveyorBelt nextBelt = (ConveyorBelt) SimpleAutomation.getInstance()
                    .getMachineManager().getMachineAt(nextBlock.getLocation());
            if (nextBelt != null) {
                return false;
            }
        }

        return false;
    }

    private void spawnAmbientParticles() {
        if (!SimpleAutomation.getInstance().getConfig().getBoolean("particles.enabled", true)) {
            return;
        }

        Location center = location.clone().add(0.5, 0.1, 0.5);

        double offsetX = facing.getModX() * 0.3;
        double offsetZ = facing.getModZ() * 0.3;

        location.getWorld().spawnParticle(Particle.DUST, center.clone().add(offsetX, 0.1, offsetZ),
                1, new Particle.DustOptions(Color.fromRGB(255, 140, 0), 0.4f));
    }

    public void rotate() {
        // Rotate to next direction: NORTH -> EAST -> SOUTH -> WEST -> NORTH
        BlockFace[] directions = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };
        int currentIndex = 0;
        for (int i = 0; i < directions.length; i++) {
            if (directions[i] == facing) {
                currentIndex = i;
                break;
            }
        }

        Block block = location.getBlock();
        if (block.getBlockData() instanceof Directional directional) {
            BlockFace newFacing = directions[(currentIndex + 1) % 4];
            directional.setFacing(newFacing);
            block.setBlockData(directional);
        }
    }

    // Check if this is a vertical belt (placed on wall)
    public boolean isVertical() {
        return facing == BlockFace.UP || facing == BlockFace.DOWN;
    }

    @Override
    public MachineType getType() {
        return MachineType.CONVEYOR_BELT;
    }

    @Override
    public String getDisplayName() {
        return "§6§lConveyor Belt";
    }

    @Override
    public boolean isValid() {
        Material mat = location.getBlock().getType();
        return mat == Material.BLUE_GLAZED_TERRACOTTA;
    }
}
