package de.hdg.keklist.gui;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.LanguageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GuiManager {

    private static final Keklist plugin = Keklist.getInstance();
    private static final LanguageUtil translations = Keklist.getTranslations();

    public static void openMainGUI(@NotNull Player player) {
        Inventory mainMenu = Bukkit.createInventory(player, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Keklist"));

        ItemStack whitelist = new ItemStack(Material.PAPER);
        whitelist.editMeta(meta -> {
            meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.title")));
            meta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.description"))));
        });

        ItemStack blacklist = new ItemStack(Material.ANVIL);
        blacklist.editMeta(meta -> {
            meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.title")));
            meta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.description"))));
        });

        ItemStack settings = new ItemStack(Material.REDSTONE);
        settings.editMeta(meta -> {
            meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.title")));
            meta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.settings.description"))));
        });

        mainMenu.setItem(11, whitelist);
        mainMenu.setItem(13, settings);
        mainMenu.setItem(15, blacklist);

        player.openInventory(mainMenu);
    }

    public static void handleMainGUICLick(@NotNull GuiPage menu, @NotNull Player player) {
        switch (menu) {
            case GuiPage.WHITELIST -> {
                Inventory whitelist = Bukkit.createInventory(null, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Whitelist"));

                ItemStack add = new ItemStack(Material.SPRUCE_SIGN);
                add.editMeta(meta -> {
                    meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.add.title")));
                    meta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.add.description"))));
                });

                ItemStack remove = new ItemStack(Material.PRISMARINE_SHARD);
                remove.editMeta(meta -> {
                    meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.remove.title")));
                    meta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.remove.description"))));
                });

                ItemStack listPlayer = new ItemStack(Material.PLAYER_HEAD);
                listPlayer.editMeta(SkullMeta.class, meta -> {
                    meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.list.title")));
                    meta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.list.description"))));
                    meta.setOwningPlayer(player);
                });

                ItemStack back = new ItemStack(Material.ARROW);
                back.editMeta(meta -> meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.back"))));

                whitelist.setItem(11, add);
                whitelist.setItem(13, listPlayer);
                whitelist.setItem(15, remove);
                whitelist.setItem(18, back);

                player.openInventory(whitelist);
            }

            case GuiPage.BLACKLIST -> {
                Inventory blacklist = Bukkit.createInventory(null, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Blacklist"));

                ItemStack add = new ItemStack(Material.DARK_OAK_SIGN);
                add.editMeta(meta -> {
                    meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.add.title")));
                    meta.lore(List.of(
                            plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.add.description")),
                            plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.add.description.motd"))
                    ));
                });

                ItemStack remove = new ItemStack(Material.DARK_PRISMARINE);
                remove.editMeta(meta -> {
                    meta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.remove.title")));
                    meta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.remove.description"))));
                });

                ItemStack listPlayer = new ItemStack(Material.PLAYER_HEAD);
                listPlayer.editMeta(SkullMeta.class, listPlayerMeta -> {
                    listPlayerMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.list.title")));
                    listPlayerMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.list.description"))));
                    listPlayerMeta.setOwningPlayer(player);
                });

                ItemStack back = new ItemStack(Material.ARROW);
                back.editMeta(backMeta -> plugin.getMiniMessage().deserialize(translations.get("gui.back")));

                blacklist.setItem(11, add);
                blacklist.setItem(13, listPlayer);
                blacklist.setItem(15, remove);
                blacklist.setItem(18, back);

                player.openInventory(blacklist);
            }

            case GuiPage.SETTINGS -> {
                Inventory settings = Bukkit.createInventory(null, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Settings"));

                ItemStack back = new ItemStack(Material.ARROW);
                back.editMeta(backMeta ->
                        backMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.back")))
                );

                ItemStack whitelist = new ItemStack(Material.PAPER);
                whitelist.editMeta(whitelistMeta -> {
                    whitelistMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.whitelist.title")));
                    whitelistMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.settings.whitelist.description"))));

                });

                ItemStack blacklist = new ItemStack(Material.ANVIL);
                blacklist.editMeta(blacklistMeta -> {
                    blacklistMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.blacklist.title")));
                    blacklistMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.settings.blacklist.description"))));

                });

                ItemStack info = new ItemStack(Material.BOOK);
                info.editMeta(infoMeta -> {
                    infoMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.title")));
                    infoMeta.lore(List.of(
                            plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.language", translations.getLanguageCode())),
                            plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.version", plugin.getPluginMeta().getVersion())),
                            plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.authors")),
                            plugin.getMiniMessage().deserialize("<white><b>" + String.join(", ", plugin.getPluginMeta().getAuthors())),
                            plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.database", Keklist.getDatabase().getType().toString()))
                    ));
                });

                settings.setItem(4, info);
                settings.setItem(11, whitelist);
                settings.setItem(15, blacklist);
                settings.setItem(18, back);

                player.openInventory(settings);
            }
        }
    }

    public enum GuiPage {
        WHITELIST,
        BLACKLIST,
        SETTINGS
    }
}
