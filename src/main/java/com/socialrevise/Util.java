package com.socialrevise;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.*;

public class Util {
    public static boolean blockEquals(Location a, Location b){
        return a.getWorld().equals(b.getWorld()) && a.getBlockX()==b.getBlockX() && a.getBlockY()==b.getBlockY() && a.getBlockZ()==b.getBlockZ();
    }


    public static long ticksUntil(LocalTime target, java.time.ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.with(target);
        if (!next.isAfter(now)) next = next.plusDays(1);
        long seconds = Duration.between(now, next).getSeconds();
        return seconds * 20L;
    }


    public static void writeLocation(FileConfiguration c, String path, Location l){
        c.set(path+".world", l.getWorld().getName());
        c.set(path+".x", l.getBlockX());
        c.set(path+".y", l.getBlockY());
        c.set(path+".z", l.getBlockZ());
    }


    public static Location readLocation(FileConfiguration c, String path){
        String w = c.getString(path+".world");
        if (w == null) return null;
        World world = Bukkit.getWorld(w);
        if (world == null) return null;
        return new Location(world, c.getInt(path+".x"), c.getInt(path+".y"), c.getInt(path+".z"));
    }
}