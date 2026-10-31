package com.simpleautomation.managers;

import com.simpleautomation.SimpleAutomation;
import com.simpleautomation.machines.*;
import com.simpleautomation.upgrades.UpgradeType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MachineManager {

    private final SimpleAutomation plugin;
    private final Map<UUID, Machine> machines;
    private final Map<Location, Machine> locationIndex;
    private final File dataFile;
    private volatile boolean dirty;
    private org.bukkit.scheduler.BukkitTask autosaveTask;

    public MachineManager(SimpleAutomation plugin) {
        this.plugin = plugin;
        this.machines = new ConcurrentHashMap<>();
        this.locationIndex = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "machines.yml");
        this.dirty = false;
    }

    public void addMachine(Machine machine) {
        machines.put(machine.getId(), machine);
        locationIndex.put(machine.getLocation().getBlock().getLocation(), machine);
        markDirty();
    }

    public void removeMachine(Machine machine) {
        machines.remove(machine.getId());
        locationIndex.remove(machine.getLocation().getBlock().getLocation());
        markDirty();
    }

    /**
     * Flags that machine state has changed and needs to be written to disk. The
     * periodic autosave only touches the file when this is set, so idle servers do
     * no needless I/O.
     */
    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    /**
     * Starts the background autosave loop. Runs asynchronously on the configured
     * interval and only writes when something actually changed, so a crash loses at
     * most one interval of work instead of everything since the last shutdown.
     */
    public void startAutosave() {
        if (autosaveTask != null) {
            return;
        }
        int seconds = plugin.getConfig().getInt("general.autosave-interval", 60);
        if (seconds <= 0) {
            return;
        }
        long ticks = seconds * 20L;
        autosaveTask = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::flush, ticks, ticks);
    }

    public void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    /**
     * Writes machines to disk only if there are unsaved changes. Safe to call from
     * any thread; the machine maps are concurrent.
     */
    public void flush() {
        if (dirty) {
            saveMachines();
        }
    }

    public Machine getMachine(UUID id) {
        return machines.get(id);
    }

    public Machine getMachineAt(Location location) {
        return locationIndex.get(location.getBlock().getLocation());
    }

    public Collection<Machine> getAllMachines() {
        return Collections.unmodifiableCollection(machines.values());
    }

    public int getMachineCount() {
        return machines.size();
    }

    public void tickAllMachines() {
        Iterator<Machine> iterator = machines.values().iterator();
        while (iterator.hasNext()) {
            Machine machine = iterator.next();

            if (!machine.isValid()) {
                iterator.remove();
                locationIndex.remove(machine.getLocation().getBlock().getLocation());
                continue;
            }

            if (machine.getLocation().getChunk().isLoaded()) {
                try {
                    machine.tick();
                } catch (Exception e) {
                    plugin.getLogger().warning("Error ticking machine " + machine.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    public void saveMachines() {
        YamlConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Machine machine : machines.values()) {
            ConfigurationSection section = config.createSection("machines." + index);
            machine.serialize(section);
            index++;
        }

        try {
            config.save(dataFile);
            dirty = false;
            plugin.getLogger().info("Saved " + index + " machines.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save machines: " + e.getMessage());
        }
    }

    public void loadMachines() {
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection machinesSection = config.getConfigurationSection("machines");

        if (machinesSection == null) {
            return;
        }

        int loaded = 0;
        int failed = 0;

        for (String key : machinesSection.getKeys(false)) {
            ConfigurationSection section = machinesSection.getConfigurationSection(key);
            if (section == null)
                continue;

            try {
                Machine machine = deserializeMachine(section);
                if (machine != null) {
                    addMachine(machine);
                    loaded++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load machine " + key + ": " + e.getMessage());
                failed++;
            }
        }

        plugin.getLogger().info("Loaded " + loaded + " machines (" + failed + " failed).");
        // Freshly loaded state already matches disk; do not force a redundant write.
        dirty = false;
    }

    private Machine deserializeMachine(ConfigurationSection section) {
        String typeName = section.getString("type");
        if (typeName == null)
            return null;

        MachineType type;
        try {
            type = MachineType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }

        UUID id = UUID.fromString(section.getString("id"));
        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null)
            return null;

        int x = section.getInt("x");
        int y = section.getInt("y");
        int z = section.getInt("z");
        Location location = new Location(world, x, y, z);

        BlockFace facing = BlockFace.valueOf(section.getString("facing", "NORTH"));
        // Machines saved before ownership tracking have no owner field. Rather than dropping
        // them, load them ownerless; an ownerless machine is grandfathered as always allowed
        // by the build-access gate (see Machine.canAffect).
        String ownerStr = section.getString("owner");
        UUID owner = ownerStr != null ? UUID.fromString(ownerStr) : null;

        Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);
        ConfigurationSection upgradeSection = section.getConfigurationSection("upgrades");
        if (upgradeSection != null) {
            for (String upgradeName : upgradeSection.getKeys(false)) {
                try {
                    UpgradeType upgradeType = UpgradeType.valueOf(upgradeName);
                    int level = upgradeSection.getInt(upgradeName);
                    upgrades.put(upgradeType, level);
                } catch (IllegalArgumentException ignored) {
                    // Unknown upgrade name in saved data (e.g. from a removed upgrade type), skip it
                }
            }
        }

        return switch (type) {
            case QUARRY -> {
                Quarry q = new Quarry(id, location, facing, owner, upgrades);
                q.loadState(section);
                yield q;
            }
            case CONVEYOR_BELT -> new ConveyorBelt(id, location, facing, owner, upgrades);
            case TREE_FARMER -> {
                TreeFarmer tf = new TreeFarmer(id, location, facing, owner, upgrades);
                tf.loadState(section);
                yield tf;
            }
            case CROP_FARMER -> new CropFarmer(id, location, facing, owner, upgrades);
            case BLOCK_MINER -> new BlockMiner(id, location, facing, owner, upgrades);
        };
    }
}
