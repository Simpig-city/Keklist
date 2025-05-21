package de.hdg.keklist.gui.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.gui.GuiManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class SettingsEvent implements Listener {

    @EventHandler
    public void onSettingsClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Settings"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if (!player.hasPermission("keklist.gui.settings"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            switch (event.getCurrentItem().getType()) {
                case PAPER -> openWhitelistGUI(player);
                case ANVIL -> openBlacklistGUI(player);
                case ARROW -> GuiManager.openMainGUI(player);
            }
        }
    }

    @EventHandler
    public void onWhitelistClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Whitelist Settings"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if (!player.hasPermission("keklist.manage.whitelist"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            switch (event.getCurrentItem().getType()) {
                case LIME_DYE -> {
                    if (!Keklist.getInstance().getConfig().getBoolean("whitelist.enabled")) {
                        Keklist.getInstance().getConfig().set("whitelist.enabled", true);
                        Keklist.getInstance().saveConfig();

                        player.sendMessage(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.enabled"))
                        );

                        openWhitelistGUI(player);
                    } else
                        player.sendMessage(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-enabled"))
                        );
                }

                case RED_DYE -> {
                    if (Keklist.getInstance().getConfig().getBoolean("whitelist.enabled")) {
                        Keklist.getInstance().getConfig().set("whitelist.enabled", false);
                        Keklist.getInstance().saveConfig();

                        player.sendMessage(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.disabled"))
                        );

                        openWhitelistGUI(player);
                    } else
                        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("whitelist.already-disabled")));
                }

                case ARROW -> GuiManager.handleMainGUICLick(GuiManager.GuiPage.SETTINGS, player);
            }
        }
    }

    private void openWhitelistGUI(@NotNull Player player) {
        Inventory whitelist = Keklist.getInstance().getServer().createInventory(player, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Whitelist Settings"));

        int whitelistedPlayers = 0;
        int whitelistedIPs = 0;

        try (DB.QueryResult rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelist");
             DB.QueryResult rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM whitelistIp")
        ) {
            if (rsPlayers.resultSet().next()) whitelistedPlayers = rsPlayers.resultSet().getInt(1);
            if (rsIPs.resultSet().next()) whitelistedIPs = rsIPs.resultSet().getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        ItemStack enable = new ItemStack(Material.LIME_DYE);
        enable.editMeta(meta -> {
            meta.displayName(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.enable.title"))
            );
            meta.lore(
                    Collections.singletonList(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.enable.description"))
                    ));
        });

        ItemStack disable = new ItemStack(Material.RED_DYE);
        disable.editMeta(meta -> {
            meta.displayName(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.disable.title"))
            );
            meta.lore(Collections.singletonList(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.disable.description"))
            ));
        });

        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.editMeta(meta -> meta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.back"))
        ));

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.info.title"))
        );
        infoMeta.lore(List.of(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.info.status", Keklist.getInstance().getConfig().getBoolean("whitelist.enabled"))),
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.info.players", whitelistedPlayers)),
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.info.ips", whitelistedIPs)),
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.whitelist.info.hide-online", Keklist.getInstance().getConfig().getBoolean("whitelist.hide-online-players")))
        ));
        info.setItemMeta(infoMeta);

        whitelist.setItem(4, info);
        whitelist.setItem(11, enable);
        whitelist.setItem(15, disable);
        whitelist.setItem(18, arrow);

        player.openInventory(whitelist);
    }

    @EventHandler
    public void onBlacklistClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Blacklist Settings"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if (!player.hasPermission("keklist.manage.blacklist"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            switch (event.getCurrentItem().getType()) {
                case LIME_DYE -> {
                    if (!Keklist.getInstance().getConfig().getBoolean("blacklist.enabled")) {
                        Keklist.getInstance().getConfig().set("blacklist.enabled", true);
                        Keklist.getInstance().saveConfig();

                        player.sendMessage(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.enabled"))
                        );

                        openBlacklistGUI(player);
                    } else
                        player.sendMessage(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-enabled"))
                        );
                }

                case RED_DYE -> {
                    if (Keklist.getInstance().getConfig().getBoolean("blacklist.enabled")) {
                        Keklist.getInstance().getConfig().set("blacklist.enabled", false);
                        Keklist.getInstance().saveConfig();

                        player.sendMessage(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.disabled"))
                        );

                        openBlacklistGUI(player);
                    } else
                        player.sendMessage(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("blacklist.already-disabled"))
                        );
                }

                case ARROW -> GuiManager.handleMainGUICLick(GuiManager.GuiPage.SETTINGS, player);
            }
        }
    }

    private void openBlacklistGUI(@NotNull Player player) {
        Inventory blacklist = Keklist.getInstance().getServer().createInventory(player, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Blacklist Settings"));

        int blacklistedPlayers = 0;
        int blacklistedIPS = 0;
        int blacklistedMotd = 0;

        try (DB.QueryResult rsPlayers = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklist");
             DB.QueryResult rsIPs = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklistIp");
             DB.QueryResult rsMOTD = Keklist.getDatabase().onQuery("SELECT COUNT(*) FROM blacklistMotd")
        ) {
            if (rsPlayers.getResultSet().next()) blacklistedPlayers = rsPlayers.getResultSet().getInt(1);
            if (rsIPs.getResultSet().next()) blacklistedIPS = rsIPs.getResultSet().getInt(1);
            if (rsMOTD.getResultSet().next()) blacklistedMotd = rsMOTD.getResultSet().getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        ItemStack enable = new ItemStack(Material.LIME_DYE);
        enable.editMeta(meta -> {
            meta.displayName(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.enable.title"))
            );
            meta.lore(Collections.singletonList(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.enable.description"))
            ));
        });

        ItemStack disable = new ItemStack(Material.RED_DYE);
        disable.editMeta(meta -> {
            meta.displayName(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.disable.title"))
            );
            meta.lore(Collections.singletonList(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.disable.description"))
            ));
        });

        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.editMeta(meta -> meta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.back"))
        ));

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.info.title"))
        );
        infoMeta.lore(List.of(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.info.status", Keklist.getInstance().getConfig().getBoolean("blacklist.enabled"))),
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.info.players", blacklistedPlayers)),
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.info.ips", blacklistedIPS)),
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.info.motd", blacklistedMotd)),
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.settings.blacklist.info.allow-admin", Keklist.getInstance().getConfig().getBoolean("blacklist.allow-join-with-admin")))
        ));
        info.setItemMeta(infoMeta);

        blacklist.setItem(4, info);
        blacklist.setItem(11, enable);
        blacklist.setItem(15, disable);
        blacklist.setItem(18, arrow);

        player.openInventory(blacklist);
    }
}
