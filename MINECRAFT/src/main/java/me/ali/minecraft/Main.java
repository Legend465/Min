package me.ali.minecraft;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {
    private AreaManager areaManager;
    private Map<UUID, PlayerSelectionData> playerSelections = new HashMap<>();

    @Override
    public void onEnable() {
        // Register Area class for serialization
        ConfigurationSerialization.registerClass(Area.class);
        
        // Initialize area manager
        this.areaManager = new AreaManager(this);
        
        // Register events - كود جديد نظيف بدون glitches
        getServer().getPluginManager().registerEvents(new SimpleBuildListener(this), this);
        
        // Register commands
        AreaCommandHandler commandHandler = new AreaCommandHandler(this);
        getCommand("area").setExecutor(commandHandler);
        getLogger().info("Command handler registered: " + commandHandler.getClass().getSimpleName());
        
        getLogger().info("✅ AreaBuilder Plugin Enabled!");
        getLogger().info("Available commands: /area create, /area set, /area list, /area delete");
    }

    @Override
    public void onDisable() {
        if (areaManager != null) {
            areaManager.saveAreas();
        }
        getLogger().info("❌ AreaBuilder Plugin Disabled!");
    }

    public AreaManager getAreaManager() {
        return areaManager;
    }

    public Map<UUID, PlayerSelectionData> getPlayerSelections() {
        return playerSelections;
    }
}
