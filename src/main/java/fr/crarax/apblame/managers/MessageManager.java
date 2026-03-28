package fr.crarax.apblame.managers;

import fr.crarax.apblame.APBlame;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player-facing messages loaded from messages.yml.
 * Supports {@code &} color codes (translated to section signs) and PlaceholderAPI placeholders.
 *
 * <p>Thread-safe: uses a ConcurrentHashMap cache that is atomically replaced on reload.</p>
 *
 * @author Crarax
 */
public class MessageManager {

    private final APBlame plugin;
    private final File messagesFile;

    /** Cached messages keyed by their YAML path (e.g. "reload.success"). */
    private volatile ConcurrentHashMap<String, String> cache;

    public MessageManager(APBlame plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        saveDefault();
        loadMessages();
    }

    /**
     * Copies the default messages.yml from the JAR resources if it does not already exist
     * on disk. Uses the standard saveResource pattern.
     */
    private void saveDefault() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    /**
     * Loads all messages from messages.yml into the cache.
     * Traverses the YAML tree recursively so nested keys are stored as dot-separated paths.
     */
    private void loadMessages() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        ConcurrentHashMap<String, String> newCache = new ConcurrentHashMap<>();
        loadSection(config, "", newCache);
        this.cache = newCache;
    }

    /**
     * Recursively loads a YAML configuration section into the flat cache map.
     *
     * @param config  the configuration section to read
     * @param prefix  the current key prefix (empty string for root)
     * @param target  the map to populate
     */
    private void loadSection(org.bukkit.configuration.ConfigurationSection config,
                             String prefix, ConcurrentHashMap<String, String> target) {
        for (String key : config.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (config.isConfigurationSection(key)) {
                loadSection(config.getConfigurationSection(key), fullKey, target);
            } else {
                target.put(fullKey, config.getString(key, ""));
            }
        }
    }

    /**
     * Retrieves a message by its key as an Adventure Component.
     * The prefix is automatically prepended and {@code &} color codes are parsed.
     *
     * @param key the message key (e.g. "reload.success")
     * @return the formatted Component, or the raw key as Component if not found
     */
    public Component getMessage(String key) {
        String prefix = cache.getOrDefault("prefix", "");
        String message = cache.getOrDefault(key, key);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
    }

    /**
     * Retrieves a message by its key as an Adventure Component, replacing placeholders.
     * The prefix is automatically prepended and {@code &} color codes are parsed.
     *
     * @param key          the message key (e.g. "reload.success")
     * @param placeholders a map of placeholder names to their replacement values
     *                     (e.g. {@code {player}} mapped to a player name)
     * @return the formatted Component with placeholders replaced
     */
    public Component getMessage(String key, Map<String, String> placeholders) {
        String prefix = cache.getOrDefault("prefix", "");
        String message = cache.getOrDefault(key, key);
        String result = prefix + message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return LegacyComponentSerializer.legacyAmpersand().deserialize(result);
    }

    /**
     * Parses PlaceholderAPI placeholders in the given message for the specified player.
     * If PlaceholderAPI is not installed, the message is returned unchanged.
     *
     * <p>Must be called from the main thread (PlaceholderAPI resolves on main thread).</p>
     *
     * @param player  the player context for placeholder resolution
     * @param message the message to parse
     * @return the message with PAPI placeholders resolved
     */
    public String parsePlaceholders(Player player, String message) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }

    /**
     * Reloads messages from messages.yml.
     * The cache is atomically replaced so in-flight reads see a consistent snapshot.
     */
    public void reload() {
        loadMessages();
    }
}
