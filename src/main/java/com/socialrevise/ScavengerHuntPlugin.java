package com.socialrevise;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;


public class ScavengerHuntPlugin extends JavaPlugin {
    private static ScavengerHuntPlugin instance;
    private HuntManager huntManager;
    private HologramManager hologramManager;


    public static ScavengerHuntPlugin get() { return instance; }


    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.hologramManager = new HologramManager(this);
        this.huntManager = new HuntManager(this, hologramManager);
        getCommand("hunt").setExecutor(new Commands(huntManager, hologramManager));
        Bukkit.getPluginManager().registerEvents(new HopperListener(huntManager), this);
        Bukkit.getPluginManager().registerEvents(new ProgressListener(huntManager, hologramManager), this);
        this.hologramManager.bind(huntManager);
        Bukkit.getScheduler().runTask(this, () -> {
            var l = huntManager.getHopperLocation(); // add getter if missing
            if (l != null) hologramManager.spawnAt(l.clone().add(0.5, 1.8, 0.5));

            // THEN start-now hourly rotation one tick later
            Bukkit.getScheduler().runTask(this, huntManager::initHourlyRotationStartNow);
        });
        getLogger().info("ScavengerHunt enabled");
    }


    @Override public void onDisable() {
        if (hologramManager != null) hologramManager.despawn();
    }


    public FileConfiguration cfg() { return getConfig(); }
}