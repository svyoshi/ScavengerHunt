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
        huntManager.initDailyRotation();
        getLogger().info("ScavengerHunt enabled");
    }


    @Override public void onDisable() {
        if (hologramManager != null) hologramManager.despawn();
    }


    public FileConfiguration cfg() { return getConfig(); }
}