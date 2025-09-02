package com.socialrevise;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;


import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class HuntManager {
    private final ScavengerHuntPlugin plugin;
    private final HologramManager holo;


    private final List<Material> todayTargets = new ArrayList<>(5);
    private final Map<UUID, Set<Material>> progress = new HashMap<>();
    private Location hopperLoc; // designated hopper
    private String rewardCmd; // console command executed on winner
    private UUID winnerOfToday;


    public HuntManager(ScavengerHuntPlugin plugin, HologramManager holo) {
        this.plugin = plugin; this.holo = holo;
        loadConfig();
    }


    public void loadConfig() {
        FileConfiguration c = plugin.cfg();
        this.rewardCmd = c.getString("reward-command", "give %player% diamond 3");
        this.hopperLoc = Util.readLocation(c, "hopper");
        if (hopperLoc != null) {
            holo.spawnAt(hopperLoc.clone().add(0.5, 1.8, 0.5));
            if (!todayTargets.isEmpty()) updateHopperDisplay(); // <<< add this
        }
        if (todayTargets.isEmpty()) newDay();
    }


    public void saveHopper(Location l) {
        this.hopperLoc = l;
        Util.writeLocation(plugin.cfg(), "hopper", l);
        plugin.saveConfig();
        holo.spawnAt(l.clone().add(0.5, 1.8, 0.5));
        updateHopperDisplay();
    }


    public boolean isDesignatedHopper(Location l) {
        return hopperLoc != null && Util.blockEquals(hopperLoc, l);
    }


    public List<Material> getTargets() { return Collections.unmodifiableList(todayTargets); }


    public void submitItem(Player player, Material mat) {
        if (winnerOfToday != null) return; // already won
        if (!todayTargets.contains(mat)) return; // not needed
        progress.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        Set<Material> set = progress.get(player.getUniqueId());
        if (set.contains(mat)) return; // already submitted by this player
        set.add(mat);
        player.sendMessage(Component.text("Submitted: " + mat, NamedTextColor.GREEN));
        hologramUpdate();
        if (set.size() >= todayTargets.size()) {
            declareWinner(player);
        }
    }


    private void declareWinner(Player p) {
        winnerOfToday = p.getUniqueId();
        String cmd = rewardCmd.replace("%player%", p.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        Bukkit.broadcast(Component.text("Scavenger Hunt winner: " + p.getName() + "!", NamedTextColor.GOLD));
    }


    public void initDailyRotation() {
// schedule to reset at next local midnight
        long ticksUntilMidnight = Util.ticksUntil(LocalTime.MIDNIGHT, ZoneId.systemDefault());
        new BukkitRunnable(){
            @Override public void run(){ newDay(); }
        }.runTaskTimer(plugin, ticksUntilMidnight, 24L*60L*60L*20L);
    }

    public boolean hasSubmitted(UUID uuid, Material mat) {
        return progress.getOrDefault(uuid, Collections.emptySet()).contains(mat);
    }

    public void updateHopperDisplay() {
        if (hopperLoc == null) return;
        BlockState st = hopperLoc.getBlock().getState();
        if (!(st instanceof Hopper)) return;
        Inventory inv = ((Hopper) st).getInventory();
        inv.clear();
        // Fill first 5 slots with the targets as labeled items
        for (int i = 0; i < Math.min(5, todayTargets.size()); i++) {
            Material m = todayTargets.get(i);
            ItemStack is = new ItemStack(m);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName("§bTarget " + (i+1));
            meta.setLore(Arrays.asList("§7" + m.name().toLowerCase().replace('_',' ')));
            is.setItemMeta(meta);
            inv.setItem(i, is);
        }
    }


    public void newDay() {
        progress.clear();
        winnerOfToday = null;
        todayTargets.clear();
// build pool from config or default commons
        List<String> pool = plugin.cfg().getStringList("pool");
        List<Material> mats = pool.isEmpty()
                ? defaultPool()
                : pool.stream().map(s -> Material.matchMaterial(s.toUpperCase(Locale.ROOT)))
                .filter(Objects::nonNull).collect(Collectors.toList());
        Collections.shuffle(mats, ThreadLocalRandom.current());
        for (int i=0;i<Math.min(5, mats.size());i++) todayTargets.add(mats.get(i));
        hologramUpdate();
        Bukkit.broadcast(Component.text("Scavenger Hunt reset! Check the hologram.", NamedTextColor.AQUA));
        updateHopperDisplay();
    }


    private List<Material> defaultPool() {
        return Arrays.asList(Material.OAK_LOG, Material.COBBLESTONE, Material.IRON_INGOT,
                Material.CARROT, Material.STRING, Material.GUNPOWDER, Material.SUGAR,
                Material.BONE, Material.FEATHER, Material.EGG, Material.PAPER, Material.LEATHER);
    }


    public void hologramUpdate() { holo.showTargets(todayTargets, winnerOfToday); }
}