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

    public HologramManager(ScavengerHuntPlugin plugin) { this.plugin = plugin; }

    public void spawnAt(Location base) {
        despawn();
        this.base = base.clone();
        World w = base.getWorld();

        // Title centered above row
        title = w.spawn(base.clone().add(0, 0.6, 0), TextDisplay.class, td -> {
            td.setBillboard(Billboard.CENTER);
            td.setViewRange(32f);
            td.setShadowed(false);
            td.setSeeThrough(true);
            td.setPersistent(true);
            td.setText("§eScavenger Hunt");
        });

        // Center 5 item slots around hopper
        double spacing = 0.6;
        double total = spacing * 4;     // 5 items -> 4 gaps
        double start = -total / 2.0;    // center around base

        for (int i = 0; i < 5; i++) {
            Location pos = base.clone().add(start + (i * spacing), 0, 0);

            ItemDisplay id = w.spawn(pos, ItemDisplay.class, d -> {
                d.setBillboard(Billboard.CENTER);
                d.setViewRange(32f);
                d.setPersistent(true);
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
                td.setPersistent(true);
                td.setText("§a✔"); // or "§a☑"
            });
            marks.add(mark);
            for (Player pl : Bukkit.getOnlinePlayers()) pl.hideEntity(plugin, mark);
        }
    }

    public void despawn() {
        for (ItemDisplay id : slots) try { id.remove(); } catch (Exception ignore) {}
        for (TextDisplay td : marks) try { td.remove(); } catch (Exception ignore) {}
        slots.clear();
        marks.clear();
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