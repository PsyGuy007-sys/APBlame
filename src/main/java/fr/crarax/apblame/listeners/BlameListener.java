package fr.crarax.apblame.listeners;

import fr.crarax.apblame.managers.BlameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;

/**
 * Listens to game events and delegates to BlameManager
 * to potentially blame a nearby player.
 *
 * <p>All events are listened to at MONITOR priority to avoid interfering
 * with other plugins that might cancel them.
 * {@code isCancelled()} is checked where applicable.</p>
 *
 * <p>Ref Paper Javadoc 1.21.11:
 * <a href="https://jd.papermc.io/paper/1.21.11/">https://jd.papermc.io/paper/1.21.11/</a></p>
 */
public class BlameListener implements Listener {

    private final BlameManager blameManager;

    public BlameListener(BlameManager blameManager) {
        this.blameManager = blameManager;
    }

    /**
     * A player breaks a block.
     * MONITOR priority + ignoreCancelled to avoid reacting to cancelled events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        blameManager.tryBlame(event.getPlayer(), "block-break");
    }

    /**
     * A player places a block.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        blameManager.tryBlame(event.getPlayer(), "block-place");
    }

    /**
     * An entity dies. Only reacts if the killer is a player.
     * This event is not Cancellable, no need to check.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        // Ignore player deaths (handled by PlayerDeathEvent)
        if (event.getEntity() instanceof Player) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            blameManager.tryBlame(killer, "mob-kill");
        }
    }

    /**
     * A player dies.
     * PlayerDeathEvent is not Cancellable.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        blameManager.tryBlame(event.getEntity(), "death");
    }

    /**
     * A player crafts an item.
     * Verifies the clicker is indeed a Player (should always be the case).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            blameManager.tryBlame(player, "craft");
        }
    }
}
