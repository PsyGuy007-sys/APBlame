package fr.crarax.apblame.commands;

import fr.crarax.apblame.managers.BlameManager;
import fr.crarax.apblame.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the /apblame command with subcommands: reload and toggle.
 */
public class BlameCommand implements CommandExecutor, TabCompleter {

    private final BlameManager blameManager;
    private final MessageManager messageManager;

    public BlameCommand(BlameManager blameManager, MessageManager messageManager) {
        this.blameManager = blameManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage(messageManager.getMessage("command.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "toggle" -> handleToggle(sender);
            default -> sender.sendMessage(messageManager.getMessage("command.unknown"));
        }

        return true;
    }

    /**
     * Reloads the plugin configuration and messages.
     * Requires the apblame.reload permission.
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("apblame.reload")) {
            sender.sendMessage(messageManager.getMessage("reload.no-permission"));
            return;
        }

        // Reload both config.yml and messages.yml
        blameManager.reloadConfiguration();
        messageManager.reload();
        sender.sendMessage(messageManager.getMessage("reload.success"));
    }

    /**
     * Toggles blame messages on or off for the sending player.
     * Requires the apblame.toggle permission and must be a player (not console).
     */
    private void handleToggle(CommandSender sender) {
        if (!sender.hasPermission("apblame.toggle")) {
            sender.sendMessage(messageManager.getMessage("toggle.no-permission"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageManager.getMessage("command.player-only"));
            return;
        }

        boolean enabled = blameManager.togglePlayer(player.getUniqueId());
        if (enabled) {
            player.sendMessage(messageManager.getMessage("toggle.enabled"));
        } else {
            player.sendMessage(messageManager.getMessage("toggle.disabled"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            if ("reload".startsWith(input) && sender.hasPermission("apblame.reload")) {
                completions.add("reload");
            }
            if ("toggle".startsWith(input) && sender.hasPermission("apblame.toggle")) {
                completions.add("toggle");
            }
            return completions;
        }
        return List.of();
    }
}
