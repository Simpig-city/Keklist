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

    static Keklist plugin = Keklist.getInstance();
    static LanguageUtil language = Keklist.getLanguage();

    public static void openMainGUI(Player player) {
        Inventory mainMenu = Bukkit.createInventory(player, 9*3, plugin.getMiniMessage().deserialize("<gold><b>Keklist"));

        ItemStack whitelist = new ItemStack(Material.PAPER);
        ItemMeta whitelistMeta = whitelist.getItemMeta();
        whitelistMeta.displayName(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.title")));
        whitelistMeta.lore(List.of(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.description"))));
        whitelist.setItemMeta(whitelistMeta);

        ItemStack blacklist = new ItemStack(Material.ANVIL);
        ItemMeta blacklistMeta = blacklist.getItemMeta();
        blacklistMeta.displayName(plugin.getMiniMessage().deserialize(language.get("gui.blacklist.title")));
        blacklistMeta.lore(List.of(plugin.getMiniMessage().deserialize(language.get("gui.blacklist.description"))));
        blacklist.setItemMeta(blacklistMeta);

        ItemStack settings = new ItemStack(Material.REDSTONE);
        ItemMeta settingsMeta = settings.getItemMeta();
        settingsMeta.displayName(plugin.getMiniMessage().deserialize(language.get("gui.settings.title")));
        settingsMeta.lore(List.of(plugin.getMiniMessage().deserialize(language.get("gui.settings.description"))));
        settings.setItemMeta(settingsMeta);

        mainMenu.setItem(11, whitelist);
        mainMenu.setItem(13, blacklist);
        mainMenu.setItem(15, settings);

        player.openInventory(mainMenu);
    }


    public static void handleMainGUICLick(String menu, Player player) {
        switch (menu.toLowerCase()) {
            case "whitelist" -> {
                Inventory whitelist = Bukkit.createInventory(null, 9*3, plugin.getMiniMessage().deserialize("<gold><b>Whitelist"));

                ItemStack add = new ItemStack(Material.SPRUCE_SIGN);
                ItemMeta addMeta = add.getItemMeta();
                addMeta.displayName(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.add.title")));
                addMeta.lore(List.of(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.add.description"))));
                add.setItemMeta(addMeta);

                ItemStack remove = new ItemStack(Material.PRISMARINE_SHARD);
                ItemMeta removeMeta = remove.getItemMeta();
                removeMeta.displayName(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.remove.title")));
                removeMeta.lore(List.of(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.remove.description"))));
                remove.setItemMeta(removeMeta);

                ItemStack listPlayer = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta listPlayerMeta = (SkullMeta) listPlayer.getItemMeta();
                listPlayerMeta.displayName(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.list.title")));
                listPlayerMeta.lore(List.of(plugin.getMiniMessage().deserialize(language.get("gui.whitelist.list.description"))));
                listPlayerMeta.setOwningPlayer(player);
                listPlayer.setItemMeta(listPlayerMeta);

                ItemStack back = new ItemStack(Material.ARROW);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.displayName(plugin.getMiniMessage().deserialize(language.get("gui.back")));
                back.setItemMeta(backMeta);

                whitelist.setItem(11, add);
                whitelist.setItem(13, listPlayer);
                whitelist.setItem(15, remove);

                whitelist.setItem(18, back);
                player.openInventory(whitelist);
            }

            case "blacklist" -> {

            }

            case "settings" -> {

            }

        }
    }
}
