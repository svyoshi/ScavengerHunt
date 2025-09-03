package com.socialrevise;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HologramManager {
    private final ScavengerHuntPlugin plugin;
    private final List<ItemDisplay> slots = new ArrayList<>(5);   // 5 item models
    private final List<TextDisplay> marks = new ArrayList<>(5);   // 5 per-slot checkmarks
    private TextDisplay title;
    private Location base;
    private HuntManager hunt;
    private TextDisplay timer;
    private TextDisplay winnerLine;
    private org.bukkit.scheduler.BukkitTask timerTask;

    public HologramManager(ScavengerHuntPlugin plugin) { this.plugin = plugin; }

    public void bind(HuntManager hunt) { this.hunt = hunt; }
    public void spawnAt(Location base) {
        despawn();
        this.base = base.clone();
        World w = base.getWorld();
        this.base.getChunk().load(true);

        Bukkit.getScheduler().runTask(ScavengerHuntPlugin.get(), () -> {
        // Title centered above row
        title = w.spawn(base.clone().add(0, 0.6, 0), TextDisplay.class, td -> {
            td.setBillboard(Billboard.CENTER);
            td.setViewRange(32f);
            td.setShadowed(false);
            td.setSeeThrough(true);
            td.setPersistent(false);
            td.setText("§eScavenger Hunt");
        });

        timer = w.spawn(base.clone().add(0, 0.35, 0), TextDisplay.class, td -> {
            td.setBillboard(Billboard.CENTER);
            td.setViewRange(32f);
            td.setSeeThrough(true);
            td.setPersistent(false);
            td.setText("§7Time remaining: §f--:--");
        });

        winnerLine = w.spawn(base.clone().add(0, 0.12, 0), TextDisplay.class, td -> {
            td.setBillboard(Billboard.CENTER);
            td.setViewRange(32f);
            td.setSeeThrough(true);
            td.setPersistent(false);
            td.setText(""); // hidden by empty text
        });

        startTimerTask();

        // Center 5 item slots around hopper
        double spacing = 0.6;
        double total = spacing * 4;     // 5 items -> 4 gaps
        double start = -total / 2.0;    // center around base

        for (int i = 0; i < 5; i++) {
            Location pos = base.clone().add(start + (i * spacing), 0, 0);

            ItemDisplay id = w.spawn(pos, ItemDisplay.class, d -> {
                d.setBillboard(Billboard.CENTER);
                d.setViewRange(32f);
                d.setPersistent(false);
                d.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new Quaternionf(),
                        new Vector3f(0.3f, 0.3f, 0.3f),  // scale here if you want bigger/smaller
                        new Quaternionf()
                ));
                d.setItemStack(new ItemStack(Material.AIR));
            });
            slots.add(id);

            // per-slot green checkmark (hidden for players by default)
            TextDisplay mark = w.spawn(pos.clone().add(0, -0.25, 0), TextDisplay.class, td -> {
                td.setBillboard(Billboard.CENTER);
                td.setViewRange(32f);
                td.setShadowed(false);
                td.setSeeThrough(true);
                td.setPersistent(false);
                td.setText("§a✔"); // or "§a☑"
            });
            marks.add(mark);
            for (Player pl : Bukkit.getOnlinePlayers()) pl.hideEntity(plugin, mark);
        }
        });
    }

    public void despawn() {
        for (ItemDisplay id : slots) try { id.remove(); } catch (Exception ignore) {}
        for (TextDisplay td : marks) try { td.remove(); } catch (Exception ignore) {}

        slots.clear();
        marks.clear();
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (title != null) { try { title.remove(); } catch (Exception ignored) {} title = null; }
    }

    // Set item models; title shows winner if any
    public void showTargets(List<Material> targets, UUID winner) {
        if (slots.isEmpty() || base == null) return;
        if (title != null) {
            title.setText(winner == null ? "§eScavenger Hunt"
                    : "§6Winner: §e" + plugin.getServer().getOfflinePlayer(winner).getName());
        }
        for (int i = 0; i < slots.size(); i++) {
            Material mat = (i < targets.size()) ? targets.get(i) : Material.AIR;
            slots.get(i).setItemStack(new ItemStack(mat));
        }
    }

    // Hide all checkmarks for everyone (called on new day)
    public void resetMarksForAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (TextDisplay td : marks) p.hideEntity(plugin, td);
        }
    }

    private String fmt(long ms) {
        long s = ms / 1000;
        long h = s / 3600; s %= 3600;
        long m = s / 60;   s %= 60;
        return (h > 0) ? String.format("%d:%02d:%02d", h, m, s)
                : String.format("%02d:%02d", m, s);
    }

    private void startTimerTask() {
        if (timerTask != null) timerTask.cancel();
        if (hunt == null) return;

        timerTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(
                ScavengerHuntPlugin.get(),
                () -> {
                    long ms = hunt.getMillisUntilReset();
                    boolean won = hunt.hasWinner();
                    String label = won ? "§7Next Round: §f" : "§7Time remaining: §f";
                    if (timer != null) timer.setText(label + fmt(ms));

                    if (winnerLine != null) {
                        if (won) {
                            String name = hunt.getWinnerName();
                            winnerLine.setText("§6Winner: §e" + (name == null ? "—" : name));
                        } else {
                            winnerLine.setText(""); // hide
                        }
                    }
                },
                0L, 20L
        );
    }

    // Per-player: show checkmarks for submitted items; hide others
    public void refreshMarksFor(Player p, List<Material> targets, java.util.Set<Material> submitted) {
        int n = Math.min(targets.size(), marks.size());
        for (int i = 0; i < n; i++) {
            TextDisplay td = marks.get(i);
            if (submitted.contains(targets.get(i))) p.showEntity(plugin, td);
            else p.hideEntity(plugin, td);
        }
    }
}