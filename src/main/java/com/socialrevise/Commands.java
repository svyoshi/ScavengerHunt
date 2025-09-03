package com.socialrevise;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;


import java.util.Locale;


public class Commands implements CommandExecutor {
    private final HuntManager hunt;
    private final HologramManager holo;
    public Commands(HuntManager hunt, HologramManager holo){ this.hunt = hunt; this.holo = holo; }


    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) return help(sender);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sethopper":
                if (!(sender instanceof Player) || !sender.hasPermission("scavenger.admin")) return true;
                Player p = (Player) sender;
                if (p.getTargetBlockExact(5) == null || p.getTargetBlockExact(5).getType() != Material.HOPPER) {
                    sender.sendMessage("Look at a hopper within 5 blocks.");
                    return true;
                }
                hunt.saveHopper(p.getTargetBlockExact(5).getLocation());
                sender.sendMessage("Hopper set.");
                return true;
            case "force":
                if (!sender.hasPermission("scavenger.admin")) return true;
                hunt.newDay(); sender.sendMessage("Reset."); return true;
            case "show":
                sender.sendMessage("Targets: " + hunt.getTargets()); return true;
            default: return help(sender);
        }
    }


    private boolean help(CommandSender s){
        s.sendMessage("/hunt sethopper – set designated hopper (admin)\n/hunt force – new day now (admin)\n/hunt show – print today targets");
        return true;
    }
}