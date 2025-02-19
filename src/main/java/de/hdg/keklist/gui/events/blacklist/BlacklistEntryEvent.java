package de.hdg.keklist.gui.events.blacklist;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.util.LanguageUtil;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BlacklistEntryEvent implements Listener {

    @EventHandler
    public void onEntryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Blacklist Players"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if (!player.hasPermission("keklist.blacklist.info")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }


            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            LanguageUtil translations = Keklist.getTranslations();

            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted Player"));
                    ItemStack item = event.getCurrentItem();

                    String username = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE name = ?", username)
                    ) {
                        if (rs.getResultSet().next()) {
                            long unix = rs.getResultSet().getLong("unix");
                            String byPlayer = rs.getResultSet().getString("byPlayer");
                            UUID uuid = UUID.fromString(rs.getResultSet().getString("uuid"));
                            String reason = Objects.equals(rs.getResultSet().getString("reason") == null ? "No reason given" : "Some reason", "No reason given") ? translations.get("gui.blacklist.entry.reason.none") : translations.get("gui.blacklist.entry.reason.found");

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

                            item.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.info"))
                            ));
                            overview.setItem(4, item);

                            ItemStack infoItem = new ItemStack(Material.BOOK);
                            infoItem.editMeta(meta -> {
                                meta.displayName(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.infoitem"))
                                );
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.name", username)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.uuid")),
                                        Keklist.getInstance().getMiniMessage().deserialize("<white>" + uuid),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.date", sdf.format(new Date(unix)))),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.reason", reason))
                                ));
                            });

                            overview.setItem(11, infoItem);

                            ItemStack removeItem = new ItemStack(Material.BARRIER);
                            removeItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.removeitem")));
                                meta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.remove"))));
                            });

                            overview.setItem(15, removeItem);
                            overview.setItem(18, getBackArrow(event.getClickedInventory()));

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.notfound")));
                            player.closeInventory();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                case BOOK -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted IP"));
                    ItemStack item = event.getCurrentItem();

                    String ip = serializer.serialize(item.getItemMeta().displayName());

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp WHERE ip = ?", ip)) {
                        if (rs.getResultSet().next()) {
                            long unix = rs.getResultSet().getLong("unix");
                            String byPlayer = rs.getResultSet().getString("byPlayer");
                            String reason = Objects.equals(rs.getResultSet().getString("reason") == null ? "No reason given" : "Some reason", "No reason given") ? translations.get("gui.blacklist.entry.reason.none") : translations.get("gui.blacklist.entry.reason.found");

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

                            item.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.info"))
                            ));
                            overview.setItem(4, item);

                            ItemStack infoItem = new ItemStack(Material.PAPER);
                            infoItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.infoitem")));
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.name", ip)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.date", sdf.format(new Date(unix)))),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.reason", reason))
                                ));
                            });

                            overview.setItem(11, infoItem);

                            ItemStack removeItem = new ItemStack(Material.BARRIER);
                            removeItem.editMeta(meta -> {
                                meta.displayName(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.removeitem"))
                                );
                                meta.lore(Collections.singletonList(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.remove"))
                                ));
                            });

                            overview.setItem(15, removeItem);
                            overview.setItem(18, getBackArrow(event.getClickedInventory()));

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.notfound"))
                            );
                            player.closeInventory();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                case WRITABLE_BOOK -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted MOTD-IP"));
                    ItemStack item = event.getCurrentItem();

                    String ip = serializer.serialize(item.getItemMeta().displayName()).replace("(MOTD)", "");

                    try (DB.QueryResult rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd WHERE ip = ?", ip)) {
                        if (rs.getResultSet().next()) {
                            long unix = rs.getResultSet().getLong("unix");
                            String byPlayer = rs.getResultSet().getString("byPlayer");

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

                            item.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.info"))
                            ));
                            overview.setItem(4, item);

                            ItemStack infoItem = new ItemStack(Material.PRISMARINE_SHARD);
                            infoItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.infoitem")));
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.motd.name", ip)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.motd.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.motd.date", sdf.format(new Date(unix))))
                                ));
                            });

                            overview.setItem(11, infoItem);

                            ItemStack removeItem = new ItemStack(Material.BARRIER);
                            removeItem.editMeta(meta -> {
                                meta.displayName(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.motd.removeitem"))
                                );
                                meta.lore(Collections.singletonList(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.motd.remove"))
                                ));
                            });

                            overview.setItem(15, removeItem);
                            overview.setItem(18, getBackArrow(event.getClickedInventory()));

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.motd.notfound"))
                            );
                            player.closeInventory();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBackClick(InventoryClickEvent event) throws SQLException {
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
                int skipIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER);
                boolean onlyPlayer = false;
                boolean onlyIp = false;
                boolean onlyMotd = false;
                try {
                    onlyPlayer = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                try {
                    onlyIp = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                try {
                    onlyMotd = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                player.openInventory(BlacklistEvent.getPage(pageIndex, skipIndex, onlyPlayer, onlyIp, onlyMotd));
            }
        }
    }

    @EventHandler
    public void onRemoveClick(InventoryClickEvent event) throws SQLException {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted Player"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if (!player.hasPermission("keklist.blacklist.remove")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }

            LanguageUtil translations = Keklist.getTranslations();

            if (event.getCurrentItem().getType() == Material.BARRIER) {
                ItemStack item = event.getClickedInventory().getItem(4);
                String username = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

                Keklist.getDatabase().onUpdate("DELETE FROM blacklist WHERE name = ?", username);
                player.sendMessage(
                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.player.removed", username))
                );

                player.openInventory(BlacklistEvent.getPage(0, 0, false, false, false)); // We can't use the back arrow here because the player is not in the inventory anymore and values may change
            }
        } else if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted IP"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            LanguageUtil translations = Keklist.getTranslations();

            if (event.getCurrentItem().getType() == Material.BARRIER) {
                ItemStack item = event.getClickedInventory().getItem(4);
                String ip = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

                Keklist.getDatabase().onUpdate("DELETE FROM blacklistIp WHERE ip = ?", ip);
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.ip.removed", ip)));

                player.openInventory(BlacklistEvent.getPage(0, 0, false, false, false)); // We can't use the back arrow here because the player is not in the inventory anymore and values may change
            }
        } else if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Blacklisted MOTD-IP"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            LanguageUtil translations = Keklist.getTranslations();

            if (event.getCurrentItem().getType() == Material.BARRIER) {
                ItemStack item = event.getClickedInventory().getItem(4);
                String ip = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()).replace("(MOTD)", "");

                Keklist.getDatabase().onUpdate("DELETE FROM blacklistMotd WHERE ip = ?", ip);
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.blacklist.entry.motd.removed", ip)));

                player.openInventory(BlacklistEvent.getPage(0, 0, false, false, false)); // We can't use the back arrow here because the player is not in the inventory anymore and values may change
            }
        }
    }

    private ItemStack getBackArrow(Inventory inventory) {
        ItemStack item = new ItemStack(Material.ARROW);
        item.editMeta(meta -> {
            meta.displayName(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.back"))
            );
            meta.lore(Collections.singletonList(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.entry.back.lore"))
            ));
        });

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 0);
        container.set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, 0);
        container.set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, 0); // boolean workaround
        container.set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 0); // boolean workaround
        container.set(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER, 0); // boolean workaround


        item.setItemMeta(meta);
        return item;
    }
}
