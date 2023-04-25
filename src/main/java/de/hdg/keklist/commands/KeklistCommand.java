package de.hdg.keklist.commands;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.gui.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class KeklistCommand extends Command {
    public KeklistCommand() {
        super("keklist");
        setAliases(List.of("kek"));
        setPermission("keklist.manage");
        setUsage(Keklist.getTranslations().get("keklist.usage"));
        setDescription(Keklist.getTranslations().get("keklist.description"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length >= 2) {
            switch (args[0]) {
                case "blacklist" -> {
                    switch (args[1]) {
                        case "enable" -> {
                            if (!Keklist.getInstance().getConfig().getBoolean("blacklist.enabled")) {
                                Keklist.getInstance().getConfig().set("blacklist.enabled", true);
                                try {
                                    Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (player.hasPermission("keklist.manage")) {
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.enabled")));
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-enabled")));
                            }
                        }

                        case "disable" -> {
                            if (Keklist.getInstance().getConfig().getBoolean("blacklist.enabled")) {
                                Keklist.getInstance().getConfig().set("blacklist.enabled", false);
                                try {
                                    Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (player.hasPermission("keklist.manage")) {
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.disabled")));
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-disabled")));
                            }
                        }

                        case "allow-blacklisted" -> {
                            if (!Keklist.getInstance().getConfig().getBoolean("blacklist.allow-join-with-admin")) {
                                Keklist.getInstance().getConfig().set("blacklist.allow-join-with-admin", true);
                                try {
                                    Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (player.hasPermission("keklist.manage")) {
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.allow-blacklisted")));
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-allow-blacklisted")));
                            }
                        }

                        case "disallow-blacklisted" -> {
                            if (Keklist.getInstance().getConfig().getBoolean("blacklist.allow-join-with-admin")) {
                                Keklist.getInstance().getConfig().set("blacklist.allow-join-with-admin", false);
                                try {
                                    Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (player.hasPermission("keklist.manage")) {
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.disallow-blacklisted")));
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-disallow-blacklisted")));
                            }
                        }

                        default -> {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));
                        }
                    }
                }

                case "whitelist" -> {
                    switch (args[1]) {
                        case "enable" -> {
                            if (!Keklist.getInstance().getConfig().getBoolean("whitelist.enabled")) {
                                Keklist.getInstance().getConfig().set("whitelist.enabled", true);
                                try {
                                    Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (player.hasPermission("keklist.manage")) {
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.enabled")));
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-enabled")));
                            }
                        }

                        case "disable" -> {
                            if (Keklist.getInstance().getConfig().getBoolean("whitelist.enabled")) {
                                Keklist.getInstance().getConfig().set("whitelist.enabled", false);
                                try {
                                    Keklist.getInstance().getConfig().save(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        if (player.hasPermission("keklist.manage")) {
                                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.disabled")));
                                        }
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-disabled")));
                            }
                        }

                        default -> {
                            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));
                        }
                    }
                }

                default -> {
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));
                }
            }

        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                try {
                    Keklist.getInstance().getConfig().load(new File(Keklist.getInstance().getDataFolder(), "config.yml"));
                    sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("config-reloaded")));
                } catch (IOException | InvalidConfigurationException e) {
                    throw new RuntimeException(e);
                }
            } else if (args[0].equalsIgnoreCase("gui")) {
                GuiManager.openMainGUI((Player) sender);
            }else
                sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));
        } else
            sender.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.usage.command")));

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length < 2) {
            return List.of("whitelist", "blacklist", "reload", "gui");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("whitelist")) {
                return List.of("enable", "disable");
            } else if (args[0].equalsIgnoreCase("blacklist")) {
                return List.of("enable", "disable", "allow-blacklisted", "disallow-blacklisted");
            }
        }
        return Collections.emptyList();
    }
}
