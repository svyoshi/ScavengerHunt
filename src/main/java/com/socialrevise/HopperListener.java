package com.socialrevise;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HopperListener implements Listener {
    private final HuntManager hunt;
    public HopperListener(HuntManager hunt) { this.hunt = hunt; }

    // 1) Hopper never inserts ground items. We decide what to do with the entity.
    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent e) {
        Location loc = e.getInventory().getLocation();
        if (loc == null || !hunt.isDesignatedHopper(loc)) return;

        // Stop hopper from sucking anything at all
        e.setCancelled(true);

        Item item = e.getItem();
        ItemStack stack = item.getItemStack();

        // Only single-item drops are ever considered; otherwise leave it on the ground
        if (stack.getAmount() != 1) return;

        // Attribute to a player; if unknown, leave on ground
        UUID who = item.getThrower();
        if (who == null) who = item.getOwner();
        Player p = (who == null) ? null : Bukkit.getPlayer(who);
        if (p == null) return;

        Material mat = stack.getType();

        // If not a target or already submitted → leave it on the ground
        if (!hunt.getTargets().contains(mat) || hunt.hasSubmitted(p.getUniqueId(), mat)) return;

        // Valid submission: count it, then delete ONLY this entity so nothing clogs
        hunt.submitItem(p, mat);
        item.remove();
    }

    // 2) Block any inventory-to-inventory moves involving the designated hopper (funnels, other hoppers, etc.)
    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent e) {
        Location to = e.getDestination() != null ? e.getDestination().getLocation() : null;
        Location from = e.getSource() != null ? e.getSource().getLocation() : null;
        boolean touchesHopper = (to != null && hunt.isDesignatedHopper(to)) || (from != null && hunt.isDesignatedHopper(from));
        if (touchesHopper) e.setCancelled(true);
    }

    // 3) Lock the hopper UI: players can’t insert or steal the display items
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory() == null) return;
        Location loc = e.getInventory().getLocation();
        if (loc == null || !hunt.isDesignatedHopper(loc)) return;
        e.setCancelled(true);
    }
}