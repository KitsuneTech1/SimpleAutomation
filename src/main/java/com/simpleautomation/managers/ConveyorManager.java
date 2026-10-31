package com.simpleautomation.managers;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.ConveyorBelt;
import com.simpleautomation.machines.Machine;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ConveyorManager {

    private final SimpleAutomation plugin;
    private final Set<Location> beltLocations;
    private BukkitTask tickTask;

    public ConveyorManager(SimpleAutomation plugin) {
        this.plugin = plugin;
        this.beltLocations = Collections.synchronizedSet(new HashSet<>());
    }

    public void registerBelt(Location location) {
        beltLocations.add(location.getBlock().getLocation());
    }

    public void unregisterBelt(Location location) {
        beltLocations.remove(location.getBlock().getLocation());
    }

    public void tick() {
        // Process all conveyor belts
        MachineManager machineManager = plugin.getMachineManager();

        for (Machine machine : machineManager.getAllMachines()) {
            if (machine instanceof ConveyorBelt belt) {
                if (machine.getLocation().getChunk().isLoaded()) {
                    belt.moveItems();
                }
            }
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        beltLocations.clear();
    }

    // Get connected belt chain starting from a location
    public List<ConveyorBelt> getConnectedBelts(Location start) {
        List<ConveyorBelt> chain = new ArrayList<>();
        Set<Location> visited = new HashSet<>();

        Location current = start.getBlock().getLocation();
        MachineManager manager = plugin.getMachineManager();

        while (current != null && !visited.contains(current)) {
            visited.add(current);
            Machine machine = manager.getMachineAt(current);

            if (machine instanceof ConveyorBelt belt) {
                chain.add(belt);
                // Move to next belt in direction
                current = current.clone().add(
                        belt.getFacing().getModX(),
                        belt.getFacing().getModY(),
                        belt.getFacing().getModZ());
            } else {
                current = null;
            }
        }

        return chain;
    }
}
