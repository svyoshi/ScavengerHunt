package com.socialrevise;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;


import java.util.*;


public class HologramManager {
    private final ScavengerHuntPlugin plugin;
    private final List<TextDisplay> lines = new ArrayList<>();
    private Location base;


    public HologramManager(ScavengerHuntPlugin plugin) { this.plugin = plugin; }


    public void spawnAt(Location base) {
        despawn();
        this.base = base.clone();
        World w = base.getWorld();
        for (int i=0;i<6;i++) {
            TextDisplay td = w.spawn(base.clone().add(i*0.6, 0, 0), TextDisplay.class, ent -> {
                ent.setBillboard(Billboard.CENTER);
                ent.setViewRange(32f);
                ent.setShadowed(false);
                ent.setLineWidth(200);
                ent.setSeeThrough(true);
                ent.setPersistent(true);
            });
            lines.add(td);
        }
        showText(0, "§eScavenger Hunt");
    }


    public void despawn() {
        for (TextDisplay td : lines) try { td.remove(); } catch (Exception ignore) {}
        lines.clear();
    }


    private void showText(int idx, String s) {
        if (idx >= lines.size()) return;
        lines.get(idx).setText(s);
    }


    public void showTargets(List<Material> targets, java.util.UUID winner) {
        if (lines.isEmpty() || base == null) return;
        showText(0, winner == null ? "§eScavenger Hunt" : "§6Winner: §e" + plugin.getServer().getOfflinePlayer(winner).getName());
        for (int i=0;i<5;i++) {
            String text = i < targets.size() ? "§b" + niceName(targets.get(i)) : "-";
            showText(i+1, text);
        }
    }


    private String niceName(Material m) {
        String n = m.name().toLowerCase(Locale.ROOT).replace('_',' ');
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}