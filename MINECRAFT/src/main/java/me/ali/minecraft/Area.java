package me.ali.minecraft;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

public class Area implements ConfigurationSerializable {
    private final String name;
    private Location pos1;
    private Location pos2;
    private Material material;

    public Area(String name, Location pos1, Location pos2, Material material) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.material = material;
    }

    public String getName() {
        return name;
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;

        double xMin = Math.min(pos1.getX(), pos2.getX());
        double xMax = Math.max(pos1.getX(), pos2.getX());
        double yMin = Math.min(pos1.getY(), pos2.getY());
        double yMax = Math.max(pos1.getY(), pos2.getY());
        double zMin = Math.min(pos1.getZ(), pos2.getZ());
        double zMax = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= xMin && loc.getX() <= xMax
                && loc.getY() >= yMin && loc.getY() <= yMax
                && loc.getZ() >= zMin && loc.getZ() <= zMax;
    }
    
    public Location getMinimumPoint() {
        double xMin = Math.min(pos1.getX(), pos2.getX());
        double yMin = Math.min(pos1.getY(), pos2.getY());
        double zMin = Math.min(pos1.getZ(), pos2.getZ());
        
        return new Location(pos1.getWorld(), xMin, yMin, zMin);
    }
    
    public Location getMaximumPoint() {
        double xMax = Math.max(pos1.getX(), pos2.getX());
        double yMax = Math.max(pos1.getY(), pos2.getY());
        double zMax = Math.max(pos1.getZ(), pos2.getZ());
        
        return new Location(pos1.getWorld(), xMax, yMax, zMax);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("pos1", pos1);
        map.put("pos2", pos2);
        map.put("material", material.name());
        return map;
    }

    public static Area deserialize(Map<String, Object> map) {
        return new Area(
                (String) map.get("name"),
                (Location) map.get("pos1"),
                (Location) map.get("pos2"),
                Material.valueOf((String) map.get("material"))
        );
    }
}
