package de.hdg.keklist.gui;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.LanguageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class GuiManager {

    private static final Keklist plugin = Keklist.getInstance();
    private static final LanguageUtil translations = Keklist.getTranslations();

    public static void openMainGUI(Player player) {
        Inventory mainMenu = Bukkit.createInventory(player, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Keklist"));

        ItemStack whitelist = new ItemStack(Material.PAPER);
        ItemMeta whitelistMeta = whitelist.getItemMeta();
        whitelistMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.title")));
        whitelistMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.description"))));
        whitelist.setItemMeta(whitelistMeta);

        ItemStack blacklist = new ItemStack(Material.ANVIL);
        ItemMeta blacklistMeta = blacklist.getItemMeta();
        blacklistMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.title")));
        blacklistMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.description"))));
        blacklist.setItemMeta(blacklistMeta);

        ItemStack settings = new ItemStack(Material.REDSTONE);
        ItemMeta settingsMeta = settings.getItemMeta();
        settingsMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.title")));
        settingsMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.settings.description"))));
        settings.setItemMeta(settingsMeta);

        mainMenu.setItem(11, whitelist);
        mainMenu.setItem(13, settings);
        mainMenu.setItem(15, blacklist);

        player.openInventory(mainMenu);
    }


    public static void handleMainGUICLick(String menu, Player player) {
        switch (menu.toLowerCase()) {
            case "whitelist" -> {
                Inventory whitelist = Bukkit.createInventory(null, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Whitelist"));

                ItemStack add = new ItemStack(Material.SPRUCE_SIGN);
                ItemMeta addMeta = add.getItemMeta();
                addMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.add.title")));
                addMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.add.description"))));
                add.setItemMeta(addMeta);

                ItemStack remove = new ItemStack(Material.PRISMARINE_SHARD);
                ItemMeta removeMeta = remove.getItemMeta();
                removeMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.remove.title")));
                removeMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.remove.description"))));
                remove.setItemMeta(removeMeta);

                ItemStack listPlayer = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta listPlayerMeta = (SkullMeta) listPlayer.getItemMeta();
                listPlayerMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.list.title")));
                listPlayerMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.whitelist.list.description"))));
                listPlayerMeta.setOwningPlayer(player);
                listPlayer.setItemMeta(listPlayerMeta);

                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.back")));
                back.setItemMeta(backMeta);

                whitelist.setItem(11, add);
                whitelist.setItem(13, listPlayer);
                whitelist.setItem(15, remove);

                whitelist.setItem(18, back);
                player.openInventory(whitelist);
            }

            case "blacklist" -> {
                Inventory blacklist = Bukkit.createInventory(null, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Blacklist"));

                ItemStack add = new ItemStack(Material.DARK_OAK_SIGN);
                ItemMeta addMeta = add.getItemMeta();
                addMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.add.title")));
                addMeta.lore(List.of(
                        plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.add.description")),
                        plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.add.description.motd"))
                ));
                add.setItemMeta(addMeta);

                ItemStack remove = new ItemStack(Material.DARK_PRISMARINE);
                ItemMeta removeMeta = remove.getItemMeta();
                removeMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.remove.title")));
                removeMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.remove.description"))));
                remove.setItemMeta(removeMeta);

                ItemStack listPlayer = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta listPlayerMeta = (SkullMeta) listPlayer.getItemMeta();
                listPlayerMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.list.title")));
                listPlayerMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.blacklist.list.description"))));
                listPlayerMeta.setOwningPlayer(player);
                listPlayer.setItemMeta(listPlayerMeta);

                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.back")));
                back.setItemMeta(backMeta);

                blacklist.setItem(11, add);
                blacklist.setItem(13, listPlayer);
                blacklist.setItem(15, remove);

                blacklist.setItem(18, back);
                player.openInventory(blacklist);
            }

            case "settings" -> {
                Inventory settings = Bukkit.createInventory(null, 9 * 3, plugin.getMiniMessage().deserialize("<gold><b>Settings"));

                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.back")));
                back.setItemMeta(backMeta);

                ItemStack whitelist = new ItemStack(Material.PAPER);
                ItemMeta whitelistMeta = whitelist.getItemMeta();
                whitelistMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.whitelist.title")));
                whitelistMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.settings.whitelist.description"))));
                whitelist.setItemMeta(whitelistMeta);

                ItemStack blacklist = new ItemStack(Material.ANVIL);
                ItemMeta blacklistMeta = blacklist.getItemMeta();
                blacklistMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.blacklist.title")));
                blacklistMeta.lore(List.of(plugin.getMiniMessage().deserialize(translations.get("gui.settings.blacklist.description"))));
                blacklist.setItemMeta(blacklistMeta);

                ItemStack info = new ItemStack(Material.BOOK);
                ItemMeta infoMeta = info.getItemMeta();
                infoMeta.displayName(plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.title")));
                infoMeta.lore(List.of(
                        plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.language", translations.getLanguageCode())),
                        plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.version", plugin.getPluginMeta().getVersion())),
                        plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.authors")),
                        plugin.getMiniMessage().deserialize("<white><b>" + String.join(", ", plugin.getPluginMeta().getAuthors())),
                        plugin.getMiniMessage().deserialize(translations.get("gui.settings.info.database", Keklist.getInstance().getConfig().getBoolean("mariadb.enabled") ? "MariaDB" : "SQLite"))
                ));
                info.setItemMeta(infoMeta);

                settings.setItem(4, info);
                settings.setItem(11, whitelist);
                settings.setItem(15, blacklist);
                settings.setItem(18, back);

                player.openInventory(settings);
            }
        }
    }
}
