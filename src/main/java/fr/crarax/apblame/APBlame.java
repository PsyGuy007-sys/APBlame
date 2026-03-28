package fr.crarax.apblame;

import fr.crarax.apblame.commands.BlameCommand;
import fr.crarax.apblame.hooks.BlameExpansion;
import fr.crarax.apblame.listeners.BlameListener;
import fr.crarax.apblame.managers.BlameManager;
import fr.crarax.apblame.managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * APBlame - A humorous plugin that randomly blames a nearby player
 * when certain events occur (breaking/placing blocks, killing mobs, dying, crafting).
 *
 * @author Crarax
 */
public final class APBlame extends JavaPlugin {

    private MessageManager messageManager;
    private BlameManager blameManager;

    @Override
    public void onEnable() {
        // Save default configs if they don't exist
        saveDefaultConfig();

        // Initialize managers (MessageManager first, as other components depend on it)
        this.messageManager = new MessageManager(this);
        this.blameManager = new BlameManager(this);

        // Register event listener
        getServer().getPluginManager().registerEvents(new BlameListener(blameManager), this);

        // Register command executor and tab completer
        final BlameCommand blameCommand = new BlameCommand(blameManager, messageManager);
        getCommand("apblame").setExecutor(blameCommand);
        getCommand("apblame").setTabCompleter(blameCommand);

        // Hook into PlaceholderAPI if available (soft dependency)
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new BlameExpansion(this).register();
            getLogger().info("PlaceholderAPI hook enabled!");
        }

        getLogger().info("APBlame enabled! The accusations shall rain down.");
    }

    @Override
    public void onDisable() {
        // Clean up cooldowns and toggles (GC-friendly)
        if (blameManager != null) {
            blameManager.cleanup();
        }
        getLogger().info("APBlame disabled. Players are temporarily innocent.");
    }

    /**
     * Returns the BlameManager instance for access from other components.
     *
     * @return the BlameManager instance
     */
    public BlameManager getBlameManager() {
        return blameManager;
    }

    /**
     * Returns the MessageManager instance for access from other components.
     *
     * @return the MessageManager instance
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }
}
