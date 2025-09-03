package com.socialrevise;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ProgressListener implements Listener {
    private final HuntManager hunt;
    private final HologramManager holo;

    public ProgressListener(HuntManager hunt, HologramManager holo) {
        this.hunt = hunt; this.holo = holo;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var set = hunt.getProgress(e.getPlayer().getUniqueId());
        holo.refreshMarksFor(e.getPlayer(), hunt.getTargets(), set);
    }
}
