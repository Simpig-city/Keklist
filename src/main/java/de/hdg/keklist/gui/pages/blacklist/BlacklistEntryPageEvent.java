package de.hdg.keklist.gui.pages.blacklist;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.util.LanguageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Handles the events related to the blacklist entry GUI.
 */
public class BlacklistEntryPageEvent implements Listener {

    /**
     * Handles the click on an entry in the main blacklist GUI.
     *
     * @param event The InventoryClickEvent
     */
    @EventHandler
    public void onEntryClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Blacklist Players"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getCurrentItem().getType() != Material.PLAYER_HEAD
                    && event.getCurrentItem().getType() != Material.BOOK
                    && event.getCurrentItem().getType() != Material.WRITABLE_BOOK) return;

            if (!player.hasPermission("keklist.blacklist.info")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }

            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            LanguageUtil translations = Keklist.getTranslations();
            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            ItemStack item = event.getCurrentItem();
            String displayName = serializer.serialize(item.getItemMeta().displayName());
            item.lore(Collections.singletonList(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.info"))
            ));

            int currentPageIndex = event.getClickedInventory().getItem(22).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
            ItemStack backArrow = new ItemStack(Material.ARROW);
            backArrow.editMeta(meta -> {
                        meta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, currentPageIndex);
                        meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.back")));
                        meta.lore(Collections.singletonList(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.back.lore"))));
                    }
            );

            ItemStack removeItem = new ItemStack(Material.BARRIER);
            removeItem.editMeta(meta -> {
                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.removeitem")));
                meta.lore(Collections.singletonList(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.remove"))));
            });

            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted Player"));

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", displayName)
                    ) {
                        if (rs.resultSet().next()) {
                            long unix = rs.resultSet().getLong("unix");
                            String byPlayer = rs.resultSet().getString("byPlayer");
                            UUID uuid = UUID.fromString(rs.resultSet().getString("uuid"));
                            String reason = Objects.equals(rs.resultSet().getString("reason") == null ? "No reason given" : "Some reason", "No reason given") ? translations.get("gui.blacklist.entry.reason.none") : translations.get("gui.blacklist.entry.reason.found");

                            ItemStack infoItem = new ItemStack(Material.BOOK);
                            infoItem.editMeta(meta ->
                                    meta.displayName(
                                            Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.infoitem"))
                                    )
                            );

                            infoItem.lore(List.of(
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.name", displayName)),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.uuid")),
                                    Keklist.getInstance().getMiniMessage().deserialize("<white>" + uuid),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.by", byPlayer)),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.date", sdf.format(new Date(unix)))),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.reason", reason))
                            ));

                            overview.setItem(4, item);
                            overview.setItem(11, infoItem);
                            overview.setItem(15, removeItem);
                            overview.setItem(18, backArrow);

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.notfound")));
                            player.closeInventory();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                case BOOK -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted IP"));

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", displayName)) {
                        if (rs.resultSet().next()) {
                            long unix = rs.resultSet().getLong("unix");
                            String byPlayer = rs.resultSet().getString("byPlayer");
                            String reason = Objects.equals(rs.getResultSet().getString("reason") == null ? "No reason given" : "Some reason", "No reason given") ? translations.get("gui.blacklist.entry.reason.none") : translations.get("gui.blacklist.entry.reason.found");

                            ItemStack infoItem = new ItemStack(Material.PAPER);
                            infoItem.editMeta(meta ->
                                    meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.infoitem")))
                            );

                            infoItem.lore(List.of(
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.name", displayName)),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.by", byPlayer)),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.date", sdf.format(new Date(unix)))),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.reason", reason))
                            ));

                            overview.setItem(4, item);
                            overview.setItem(11, infoItem);
                            overview.setItem(15, removeItem);
                            overview.setItem(18, backArrow);

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.notfound")));
                            player.closeInventory();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                case WRITABLE_BOOK -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted MOTD-IP"));

                    displayName = displayName.replace("(MOTD)", "");

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", displayName)) {
                        if (rs.resultSet().next()) {
                            long unix = rs.resultSet().getLong("unix");
                            String byPlayer = rs.resultSet().getString("byPlayer");

                            ItemStack infoItem = new ItemStack(Material.PRISMARINE_SHARD);
                            infoItem.editMeta(meta ->
                                    meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.infoitem")))
                            );

                            infoItem.lore(List.of(
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.name", displayName)),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.by", byPlayer)),
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.date", sdf.format(new Date(unix))))
                            ));

                            overview.setItem(4, item);
                            overview.setItem(11, infoItem);
                            overview.setItem(15, removeItem);
                            overview.setItem(18, backArrow);

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.notfound"))
                            );
                            player.closeInventory();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Handles the click on the back button in the blacklist entry GUI.
     *
     * @param event The InventoryClickEvent
     * @throws SQLException If there is an error while accessing the database.
     */
    @EventHandler
    public void onBackClick(@NotNull InventoryClickEvent event) throws SQLException {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted Player"))
                || event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted IP"))
                || event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted MOTD-IP"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if (!player.hasPermission("keklist.blacklist.info")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }

            if (event.getCurrentItem().getType() == Material.ARROW) {
                int pageIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(BlacklistPageEvent.getPage(pageIndex));
            }
        }
    }

    /**
     * Handles the click on the remove button in the blacklist entry GUI.
     *
     * @param event The InventoryClickEvent
     * @throws SQLException If there is an error while accessing the database.
     */
    @EventHandler
    public void onRemoveClick(@NotNull InventoryClickEvent event) throws SQLException {
        if (event.getClickedInventory() == null) return;
        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem().getType() != Material.BARRIER) return;

        switch (event.getView().title()) {
            case Component c when c.equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted Player")) -> {
                event.setCancelled(true);

                if (!player.hasPermission("keklist.blacklist.remove")) {
                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return;
                }

                ItemStack item = event.getClickedInventory().getItem(4);
                String username = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

                Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", username);
                player.sendMessage(
                        Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.removed", username))
                );

                int pageIndex = event.getClickedInventory().getItem(18).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(BlacklistPageEvent.getPage(pageIndex));
            }

            case Component c when c.equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted IP")) -> {
                event.setCancelled(true);

                if (!player.hasPermission("keklist.blacklist.remove")) {
                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return;
                }

                ItemStack item = event.getClickedInventory().getItem(4);
                String ip = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

                Keklist.getDatabase().onUpdate("DELETE FROM blacklistIp WHERE ip = ?", ip);
                Keklist.getDatabase().onUpdate("DELETE FROM blacklistMotd WHERE ip = ?", ip);

                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.removed", ip)));

                int pageIndex = event.getClickedInventory().getItem(18).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(BlacklistPageEvent.getPage(pageIndex));
            }

            case Component c when c.equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted MOTD-IP")) -> {
                event.setCancelled(true);

                if (!player.hasPermission("keklist.blacklist.remove")) {
                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return;
                }

                ItemStack item = event.getClickedInventory().getItem(4);
                String ip = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

                Keklist.getDatabase().onUpdate("DELETE FROM blacklistMotd WHERE ip = ?", ip);

                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.removed", ip)));

                int pageIndex = event.getClickedInventory().getItem(18).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(BlacklistPageEvent.getPage(pageIndex));
            }

            default -> {
            }
        }
    }
}
