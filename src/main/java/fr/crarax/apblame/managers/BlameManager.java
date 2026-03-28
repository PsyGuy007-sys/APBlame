package fr.crarax.apblame.managers;

import fr.crarax.apblame.APBlame;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Manages all core blame logic for APBlame:
 * <ul>
 *     <li>Trigger chance verification</li>
 *     <li>Random nearby player selection</li>
 *     <li>Per-player cooldown management</li>
 *     <li>Per-player toggle on/off</li>
 *     <li>Message resolution and delivery</li>
 * </ul>
 *
 * <p>Thread-safety: all maps use ConcurrentHashMap. While Bukkit events run on the
 * main thread, the toggle set could theoretically be accessed from an async reload.
 * We remain thread-safe as a precaution.</p>
 *
 * @author Crarax
 */
public class BlameManager {

    private final APBlame plugin;

    /** Cooldowns by UUID: timestamp (ms) of the last blame message sent. */
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /** Players who have disabled blame messages via /apblame toggle. */
    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

    /** Last blamed player name for each player (the scapegoat). */
    private final Map<UUID, String> lastBlamed = new ConcurrentHashMap<>();

    /** Last blame message received by each player. */
    private final Map<UUID, String> lastMessage = new ConcurrentHashMap<>();

    /** Total number of times a player has been blamed (as scapegoat). */
    private final Map<UUID, AtomicInteger> totalBlamed =
        new ConcurrentHashMap<>();

    /** Total number of blame messages received by each player. */
    private final Map<UUID, AtomicInteger> totalReceived =
        new ConcurrentHashMap<>();

    public BlameManager(APBlame plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to trigger a blame for a given event.
     * Checks in order: toggle, cooldown, chance, nearby player availability.
     * If all conditions are met, sends a blame message to the triggering player.
     *
     * <p>Must be called from the main thread (accesses Player, Location, World).</p>
     *
     * @param trigger  the player who triggered the event
     * @param category the event category (block-break, block-place, mob-kill, death, craft)
     */
    public void tryBlame(Player trigger, String category) {
        // Check if the player has disabled blame messages
        if (disabledPlayers.contains(trigger.getUniqueId())) {
            return;
        }

        // Check cooldown
        if (isOnCooldown(trigger.getUniqueId())) {
            return;
        }

        // Check trigger chance
        int chance = plugin.getConfig().getInt("chances." + category, 10);
        // Clamp between 5 and 20 as specified
        chance = Math.max(5, Math.min(20, chance));

        if (ThreadLocalRandom.current().nextInt(100) >= chance) {
            return;
        }

        // Find a random nearby player (excluding the trigger and immune players)
        Player blamed = findNearbyPlayer(trigger);
        if (blamed == null) {
            return; // No one to blame, silently abort
        }

        // Pick a random message from the category
        String message = getRandomMessage(
            category,
            blamed.getName(),
            trigger.getName()
        );
        if (message == null) {
            return; // No messages configured for this category
        }

        // Parse PlaceholderAPI placeholders if available (main thread, safe)
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(trigger, message);
        }

        // Update stats for PAPI placeholders
        UUID triggerUuid = trigger.getUniqueId();
        UUID blamedUuid = blamed.getUniqueId();

        lastBlamed.put(triggerUuid, blamed.getName());
        lastMessage.put(triggerUuid, message);
        totalBlamed
            .computeIfAbsent(blamedUuid, k -> new AtomicInteger(0))
            .incrementAndGet();
        totalReceived
            .computeIfAbsent(triggerUuid, k -> new AtomicInteger(0))
            .incrementAndGet();

        // Send the message only to the trigger player
        trigger.sendMessage(message);

        // Update cooldown
        cooldowns.put(triggerUuid, System.currentTimeMillis());
    }

    /**
     * Checks whether a player is still on cooldown.
     *
     * @param uuid the player's UUID
     * @return true if the player is on cooldown
     */
    private boolean isOnCooldown(UUID uuid) {
        Long lastBlame = cooldowns.get(uuid);
        if (lastBlame == null) {
            return false;
        }
        long cooldownMs = plugin.getConfig().getInt("cooldown", 5) * 1000L;
        return (System.currentTimeMillis() - lastBlame) < cooldownMs;
    }

    /**
     * Finds a random nearby player within the configured radius.
     * Excludes the triggering player and players with the apblame.immune permission.
     *
     * <p>Ref Paper Javadoc 1.21.11: {@code World#getPlayers()}
     * <a href="https://jd.papermc.io/paper/1.21.11/">https://jd.papermc.io/paper/1.21.11/</a></p>
     *
     * @param trigger the triggering player to exclude
     * @return a random nearby player, or null if none are available
     */
    private Player findNearbyPlayer(Player trigger) {
        double radius = plugin.getConfig().getDouble("radius", 30.0);
        Location loc = trigger.getLocation();

        // Gather nearby players in the same world, filtering by distance
        List<Player> candidates = new ArrayList<>();

        for (Player nearby : trigger.getWorld().getPlayers()) {
            if (nearby.equals(trigger)) {
                continue; // Exclude the trigger
            }
            if (nearby.hasPermission("apblame.immune")) {
                continue; // Exclude immune players
            }
            if (nearby.getLocation().distanceSquared(loc) <= radius * radius) {
                candidates.add(nearby);
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Random selection from candidates
        return candidates.get(
            ThreadLocalRandom.current().nextInt(candidates.size())
        );
    }

    /**
     * Retrieves a random message from the config for the given category,
     * and replaces the {player} and {victim} placeholders.
     *
     * @param category   the message category
     * @param playerName the name of the blamed player (the scapegoat)
     * @param victimName the name of the triggering player
     * @return the formatted message, or null if no messages are configured
     */
    private String getRandomMessage(
        String category,
        String playerName,
        String victimName
    ) {
        List<String> messages = plugin
            .getConfig()
            .getStringList("messages." + category);
        if (messages.isEmpty()) {
            return null;
        }

        String message = messages.get(
            ThreadLocalRandom.current().nextInt(messages.size())
        );
        return message
            .replace("{player}", playerName)
            .replace("{victim}", victimName);
    }

    /**
     * Toggles blame messages on or off for a player.
     *
     * @param uuid the player's UUID
     * @return true if messages are now enabled, false if disabled
     */
    public boolean togglePlayer(UUID uuid) {
        if (disabledPlayers.contains(uuid)) {
            disabledPlayers.remove(uuid);
            return true; // Re-enabled
        } else {
            disabledPlayers.add(uuid);
            return false; // Disabled
        }
    }

    /**
     * Reloads the plugin configuration from config.yml.
     * Cooldowns and toggles are preserved (no reason to reset them on reload).
     */
    public void reloadConfiguration() {
        plugin.reloadConfig();
    }

    // ==================== Getters for PlaceholderAPI ====================

    /**
     * Returns the name of the last player blamed by the given player.
     *
     * @param uuid the player's UUID
     * @return the name of the last scapegoat, or null if no blame has occurred
     */
    public String getLastBlamed(UUID uuid) {
        return lastBlamed.get(uuid);
    }

    /**
     * Returns the last blame message received by the given player.
     *
     * @param uuid the player's UUID
     * @return the last message (with color codes), or null if none
     */
    public String getLastMessage(UUID uuid) {
        return lastMessage.get(uuid);
    }

    /**
     * Returns the total number of times this player has been blamed as scapegoat.
     *
     * @param uuid the player's UUID
     * @return the total blame count
     */
    public int getTotalBlamed(UUID uuid) {
        AtomicInteger count = totalBlamed.get(uuid);
        return count != null ? count.get() : 0;
    }

    /**
     * Returns the total number of blame messages received by this player.
     *
     * @param uuid the player's UUID
     * @return the total received count
     */
    public int getTotalReceived(UUID uuid) {
        AtomicInteger count = totalReceived.get(uuid);
        return count != null ? count.get() : 0;
    }

    /**
     * Checks whether a player has blame messages enabled.
     *
     * @param uuid the player's UUID
     * @return true if messages are enabled (player is NOT in disabledPlayers)
     */
    public boolean isEnabled(UUID uuid) {
        return !disabledPlayers.contains(uuid);
    }

    /**
     * Clears all in-memory data.
     * Called in onDisable() for a clean shutdown.
     */
    public void cleanup() {
        cooldowns.clear();
        disabledPlayers.clear();
        lastBlamed.clear();
        lastMessage.clear();
        totalBlamed.clear();
        totalReceived.clear();
    }
}
