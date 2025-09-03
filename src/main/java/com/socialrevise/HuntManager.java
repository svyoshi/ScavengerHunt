package com.socialrevise;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class HuntManager {
    private final ScavengerHuntPlugin plugin;
    private final HologramManager holo;

    private final List<Material> todayTargets = new ArrayList<>(5);
    final Map<UUID, Set<Material>> progress = new HashMap<>();
    private org.bukkit.Location hopperLoc; // designated hopper
    private String rewardCmd;              // console command executed on winner
    private UUID winnerOfToday;

    public HuntManager(ScavengerHuntPlugin plugin, HologramManager holo) {
        this.plugin = plugin; this.holo = holo;
        loadConfig();
    }

    public void loadConfig() {
        FileConfiguration c = plugin.cfg();
        this.rewardCmd = c.getString("reward-command", "give %player% diamond 3");
        this.hopperLoc = Util.readLocation(c, "hopper");
        if (hopperLoc != null) holo.spawnAt(hopperLoc.clone().add(0.5, 1.8, 0.5));
        if (todayTargets.isEmpty()) newDay();
    }

    public void saveHopper(org.bukkit.Location l) {
        this.hopperLoc = l;
        Util.writeLocation(plugin.cfg(), "hopper", l);
        plugin.saveConfig();
        holo.spawnAt(l.clone().add(0.5, 1.8, 0.5));
        updateHopperDisplay();
    }

    public boolean isDesignatedHopper(org.bukkit.Location l) {
        return hopperLoc != null && Util.blockEquals(hopperLoc, l);
    }

    public List<Material> getTargets() { return Collections.unmodifiableList(todayTargets); }

    public Set<Material> getProgress(UUID uuid) {
        return progress.getOrDefault(uuid, Collections.emptySet());
    }

    public boolean hasSubmitted(UUID uuid, Material mat) {
        return progress.getOrDefault(uuid, Collections.emptySet()).contains(mat);
    }

    public void submitItem(Player player, Material mat) {
        if (winnerOfToday != null) return;          // already won
        if (!todayTargets.contains(mat)) return;    // not needed

        Set<Material> set = progress.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (!set.add(mat)) return;                  // already submitted by this player

        player.sendMessage(Component.text("Submitted: " + mat, NamedTextColor.GREEN));

        // per-player checkmarks update
        holo.refreshMarksFor(player, todayTargets, set);

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
        long ticksUntilMidnight = Util.ticksUntil(LocalTime.MIDNIGHT, ZoneId.systemDefault());
        new org.bukkit.scheduler.BukkitRunnable(){
            @Override public void run(){ newDay(); }
        }.runTaskTimer(plugin, ticksUntilMidnight, 24L*60L*60L*20L);
    }

    public void newDay() {
        progress.clear();
        winnerOfToday = null;
        todayTargets.clear();

        List<String> pool = plugin.cfg().getStringList("pool");
        List<Material> mats = (pool == null || pool.isEmpty())
                ? defaultPool()
                : pool.stream()
                .map(s -> Material.matchMaterial(s.toUpperCase(Locale.ROOT)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Collections.shuffle(mats, ThreadLocalRandom.current());
        for (int i = 0; i < Math.min(5, mats.size()); i++) todayTargets.add(mats.get(i));

        updateHopperDisplay();         // keep hopper display filled
        holo.resetMarksForAll();       // hide all per-player checks
        hologramUpdate();
        Bukkit.broadcast(Component.text("Scavenger Hunt reset! Check the hologram.", NamedTextColor.AQUA));
    }

    private List<Material> defaultPool() {
        return Arrays.asList(
                Material.OAK_LOG, Material.COBBLESTONE, Material.IRON_INGOT,
                Material.CARROT, Material.STRING, Material.GUNPOWDER, Material.SUGAR,
                Material.BONE, Material.FEATHER, Material.EGG, Material.PAPER, Material.LEATHER
        );
    }

    public void hologramUpdate() { holo.showTargets(todayTargets, winnerOfToday); }

    // --- Hopper display filler (prevents accepting junk; UI is locked by listener) ---
    public void updateHopperDisplay() {
        if (hopperLoc == null) return;
        BlockState st = hopperLoc.getBlock().getState();
        if (!(st instanceof Hopper)) return;
        Inventory inv = ((Hopper) st).getInventory();
        inv.clear();
        for (int i = 0; i < Math.min(5, todayTargets.size()); i++) {
            Material m = todayTargets.get(i);
            ItemStack is = new ItemStack(m);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName("§bTarget " + (i + 1));
            meta.setLore(java.util.Arrays.asList("§7" + m.name().toLowerCase().replace('_', ' ')));
            is.setItemMeta(meta);
            inv.setItem(i, is);
        }
    }
}