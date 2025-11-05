package me.ali.minecraft;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AreaManager {

    private final File file;
    private FileConfiguration config;
    private final Map<String, Area> areas = new HashMap<>();
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();

    public AreaManager(Main plugin) {
        file = new File(plugin.getDataFolder(), "areas.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        try {
            this.config = YamlConfiguration.loadConfiguration(file);
            loadAreas();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load areas.yml, creating backup and starting fresh");
            // Create backup of corrupted file
            File backupFile = new File(plugin.getDataFolder(), "areas.yml.backup");
            try {
                if (file.exists()) {
                    file.renameTo(backupFile);
                    file.createNewFile();
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            this.config = new YamlConfiguration();
        }
    }

    public void setPos1(Player p, Location loc) {
        pos1.put(p.getUniqueId(), loc);
    }

    public void setPos2(Player p, Location loc) {
        pos2.put(p.getUniqueId(), loc);
    }

    public Location getPos1(Player p) {
        return pos1.get(p.getUniqueId());
    }

    public Location getPos2(Player p) {
        return pos2.get(p.getUniqueId());
    }

    public void addArea(Area area) {
        areas.put(area.getName().toLowerCase(), area);
        saveAreas();
    }

    public void deleteArea(String name) {
        areas.remove(name.toLowerCase());
        saveAreas();
    }
    
    public boolean removeArea(String name) {
        boolean removed = areas.remove(name.toLowerCase()) != null;
        if (removed) {
            saveAreas();
        }
        return removed;
    }

    public Collection<Area> getAllAreas() {
        return areas.values();
    }

    public Area getArea(String name) {
        return areas.get(name.toLowerCase());
    }

    public void saveAreas() {
        for (String key : config.getKeys(false)) config.set(key, null);
        for (Area area : areas.values()) config.set(area.getName(), area);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAreas() {
        for (String key : config.getKeys(false)) {
            try {
                Object obj = config.get(key);
                if (obj instanceof Area area) {
                    areas.put(area.getName().toLowerCase(), area);
                }
            } catch (Exception e) {
                // Skip corrupted area data
                continue;
            }
        }
    }
}
