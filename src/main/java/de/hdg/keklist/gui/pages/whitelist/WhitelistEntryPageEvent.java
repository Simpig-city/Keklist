package de.hdg.keklist.gui.pages.whitelist;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Handles the events for the whitelist entry GUI.
 */
public class WhitelistEntryPageEvent implements Listener {

    /**
     * Handles the click event for the main whitelist entry GUI.
     *
     * @param event The InventoryClickEvent
     */
    @EventHandler
    public void onEntryClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;
            if (event.getCurrentItem().getType() != Material.PLAYER_HEAD
                    && event.getCurrentItem().getType() != Material.BOOK
                    && event.getCurrentItem().getType() != Material.PAPER) return;

            if (!player.hasPermission("keklist.whitelist.info")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }

            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            LanguageUtil translations = Keklist.getTranslations();
            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

            ItemStack item = event.getCurrentItem();
            String displayName = serializer.serialize(item.getItemMeta().displayName());
            item.lore(Collections.singletonList(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.info"))
            ));

            int currentPageIndex = event.getClickedInventory().getItem(22).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
            ItemStack backArrow = new ItemStack(Material.ARROW);
            backArrow.editMeta(meta -> {
                        meta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, currentPageIndex);
                        meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.back")));
                        meta.lore(Collections.singletonList(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.back.lore"))));
                    }
            );

            ItemStack removeItem = new ItemStack(Material.BARRIER);
            removeItem.editMeta(meta -> {
                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.removeitem")));
                meta.lore(Collections.singletonList(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.remove"))));
            });

            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Player"));

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT w.*, COALESCE(l.whitelistLevel, 0) AS level FROM whitelist w LEFT JOIN whitelistLevel l ON w.uuid = l.entry WHERE w.name = ?", displayName)) {
                        if (rs.resultSet().next()) {
                            long unix = rs.resultSet().getLong("unix");
                            String byPlayer = rs.resultSet().getString("byPlayer");
                            UUID uuid = UUID.fromString(rs.resultSet().getString("uuid"));
                            int level = rs.resultSet().getInt("level");

                            ItemStack infoItem = new ItemStack(Material.BOOK);
                            infoItem.editMeta(meta -> {
                                meta.displayName(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.infoitem"))
                                );
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.name", displayName)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.uuid")),
                                        Keklist.getInstance().getMiniMessage().deserialize("<white>" + uuid),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.level", level)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.date", sdf.format(new Date(unix))))
                                ));
                            });

                            overview.setItem(4, item);
                            overview.setItem(11, infoItem);
                            overview.setItem(15, removeItem);
                            overview.setItem(18, backArrow);

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.notfound")));
                            player.closeInventory();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                case BOOK -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted IP"));

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT w.*, COALESCE(l.whitelistLevel, 0) AS level FROM whitelistIp w LEFT JOIN whitelistLevel l ON w.ip = l.entry WHERE w.ip = ?", displayName)) {
                        if (rs.getResultSet().next()) {
                            long unix = rs.getResultSet().getLong("unix");
                            String byPlayer = rs.getResultSet().getString("byPlayer");
                            int level = rs.resultSet().getInt("level");

                            ItemStack infoItem = new ItemStack(Material.PAPER);
                            infoItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.infoitem")));
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.name", displayName)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.level", level)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.date", sdf.format(new Date(unix))))
                                ));
                            });

                            overview.setItem(4, item);
                            overview.setItem(11, infoItem);
                            overview.setItem(15, removeItem);
                            overview.setItem(18, backArrow);

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.notfound")));
                            player.closeInventory();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                case PAPER -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Domain"));

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT w.*, COALESCE(l.whitelistLevel, 0) AS level FROM whitelistDomain w LEFT JOIN whitelistLevel l ON w.domain = l.entry WHERE w.domain = ?", displayName)) {
                        if (rs.getResultSet().next()) {
                            long unix = rs.getResultSet().getLong("unix");
                            String byPlayer = rs.getResultSet().getString("byPlayer");
                            int level = rs.resultSet().getInt("level");

                            ItemStack infoItem = new ItemStack(Material.PRISMARINE_SHARD);
                            infoItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.infoitem")));
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.domain.name", displayName)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.level", level)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.date", sdf.format(new Date(unix))))
                                ));
                            });

                            overview.setItem(4, item);
                            overview.setItem(11, infoItem);
                            overview.setItem(15, removeItem);
                            overview.setItem(18, backArrow);

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.notfound")));
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
     * Handles the click event for the back button in the whitelist entry GUI.
     *
     * @param event The InventoryClickEvent
     * @throws SQLException If a database error occurs
     */
    @EventHandler
    public void onBackClick(@NotNull InventoryClickEvent event) throws SQLException {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Player"))
                || event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted IP"))
                || event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Domain"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if (!player.hasPermission("keklist.whitelist.info")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }

            if (event.getCurrentItem().getType() == Material.ARROW) {
                int pageIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(WhitelistPageEvent.getPage(pageIndex));
            }
        }
    }

    /**
     * Handles the click event for the remove button in the whitelist entry GUI.
     *
     * @param event The InventoryClickEvent
     * @throws SQLException If a database error occurs
     */
    @EventHandler
    public void onRemoveClick(@NotNull InventoryClickEvent event) throws SQLException {
        if (event.getClickedInventory() == null) return;
        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem().getType() != Material.BARRIER) return;

        switch (event.getView().title()) {
            case Component c when c.equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Player")) -> {
                event.setCancelled(true);

                if (!player.hasPermission("keklist.whitelist.remove")) {
                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return;
                }

                ItemStack item = event.getClickedInventory().getItem(4);
                String username = PlainTextComponentSerializer.plainText().serialize(item.displayName()).replace("[", "").replace("]", "");

                Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE name = ?", username);
                Keklist.getDatabase().onUpdate("DELETE FROM whitelistLevel WHERE entry = ?", username);

                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.removed", username)));

                int pageIndex = event.getClickedInventory().getItem(18).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(WhitelistPageEvent.getPage(pageIndex));
            }

            case Component c when c.equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted IP")) -> {
                event.setCancelled(true);

                if (!player.hasPermission("keklist.whitelist.remove")) {
                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return;
                }

                ItemStack item = event.getClickedInventory().getItem(4);
                String ip = PlainTextComponentSerializer.plainText().serialize(item.displayName()).replace("[", "").replace("]", "");

                Keklist.getDatabase().onUpdate("DELETE FROM whitelistIp WHERE ip = ?", ip);
                Keklist.getDatabase().onUpdate("DELETE FROM whitelistLevel WHERE entry = ?", ip);

                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.removed", ip)));

                int pageIndex = event.getClickedInventory().getItem(18).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(WhitelistPageEvent.getPage(pageIndex));
            }

            case Component c when c.equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Domain")) -> {
                event.setCancelled(true);

                if (!player.hasPermission("keklist.whitelist.remove")) {
                    player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                    return;
                }

                ItemStack item = event.getClickedInventory().getItem(4);
                String domain = PlainTextComponentSerializer.plainText().serialize(item.displayName()).replace("[", "").replace("]", "");

                Keklist.getDatabase().onUpdate("DELETE FROM whitelistDomain WHERE domain = ?", domain);
                Keklist.getDatabase().onUpdate("DELETE FROM whitelistLevel WHERE entry = ?", domain);

                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.removed", domain)));

                int pageIndex = event.getClickedInventory().getItem(18).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                player.openInventory(WhitelistPageEvent.getPage(pageIndex));
            }

            default -> {
            }
        }
    }
}
