package fr.crarax.apblame.hooks;

import fr.crarax.apblame.APBlame;
import fr.crarax.apblame.managers.BlameManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for APBlame.
 * Provides custom placeholders usable by other plugins.
 *
 * <p>Available placeholders:</p>
 * <ul>
 *     <li>{@code %apblame_last_blamed%} - name of the last blamed player</li>
 *     <li>{@code %apblame_last_message%} - last blame message received (without color codes)</li>
 *     <li>{@code %apblame_total_blamed%} - total times blamed as scapegoat</li>
 *     <li>{@code %apblame_total_received%} - total blame messages received</li>
 *     <li>{@code %apblame_enabled%} - true/false whether the player has APBlame enabled</li>
 * </ul>
 *
 * <p>{@code persist()} returns true: the expansion survives PlaceholderAPI reloads.</p>
 *
 * @author Crarax
 */
public class BlameExpansion extends PlaceholderExpansion {

    private final APBlame plugin;

    public BlameExpansion(APBlame plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "apblame";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Crarax";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    /**
     * Persists across PlaceholderAPI reloads.
     * No need to manually re-register the expansion.
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Handles placeholder requests.
     * Called from the main thread by PAPI when a %apblame_xxx% placeholder is resolved.
     *
     * @param player the player for whom the placeholder is resolved
     * @param params the suffix after "apblame_" (e.g. "last_blamed", "total_received")
     * @return the placeholder value, or null if the placeholder does not exist
     */
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        BlameManager manager = plugin.getBlameManager();

        return switch (params.toLowerCase()) {
            case "last_blamed" -> {
                String name = manager.getLastBlamed(player.getUniqueId());
                yield name != null ? name : "Nobody";
            }
            case "last_message" -> {
                String msg = manager.getLastMessage(player.getUniqueId());
                // Strip color codes for clean display in scoreboards/placeholders
                yield msg != null ? PlainTextComponentSerializer.plainText().serialize(
                        LegacyComponentSerializer.legacySection().deserialize(msg)) : "None";
            }
            case "total_blamed" -> String.valueOf(manager.getTotalBlamed(player.getUniqueId()));
            case "total_received" -> String.valueOf(manager.getTotalReceived(player.getUniqueId()));
            case "enabled" -> String.valueOf(manager.isEnabled(player.getUniqueId()));
            default -> null; // Unknown placeholder, PAPI will display the raw placeholder
        };
    }
}
