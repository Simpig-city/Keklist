package de.hdg.keklist.gui.events.whitelist;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.LanguageUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class WhitelistEntryEvent implements Listener {

    @EventHandler
    public void onEntryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if(!player.hasPermission("keklist.gui.whitelist"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            LanguageUtil translations = Keklist.getTranslations();

            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Player"));
                    ItemStack item = event.getCurrentItem();

                    String username = ((SkullMeta) item.getItemMeta()).getOwningPlayer().getName();
                    ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = ?", username);

                    try {
                        if (rs.next()) {
                            long unix = rs.getLong("unix");
                            String byPlayer = rs.getString("byPlayer");
                            UUID uuid = UUID.fromString(rs.getString("uuid"));

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

                            item.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.info"))
                            ));
                            overview.setItem(4, item);

                            ItemStack infoItem = new ItemStack(Material.BOOK);
                            infoItem.editMeta(meta -> {
                                meta.displayName(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.infoitem"))
                                );
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.name", username)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.uuid")),
                                        Keklist.getInstance().getMiniMessage().deserialize("<white>" + uuid.toString()),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.date", sdf.format(new Date(unix))))
                                ));
                            });

                            overview.setItem(11, infoItem);

                            ItemStack removeItem = new ItemStack(Material.BARRIER);
                            removeItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.removeitem")));
                                meta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.remove"))));
                            });

                            overview.setItem(15, removeItem);
                            overview.setItem(18, getBackArrow(event.getClickedInventory()));

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.notfound")));
                            player.closeInventory();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                case BOOK -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted IP"));
                    ItemStack item = event.getCurrentItem();

                    String ip = serializer.serialize(item.getItemMeta().displayName());
                    ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp WHERE ip = ?", ip);

                    try {
                        if (rs.next()) {
                            long unix = rs.getLong("unix");
                            String byPlayer = rs.getString("byPlayer");

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

                            item.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.info"))
                            ));
                            overview.setItem(4, item);

                            ItemStack infoItem = new ItemStack(Material.PAPER);
                            infoItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.infoitem")));
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.name", ip)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.date", sdf.format(new Date(unix))))
                                ));
                            });

                            overview.setItem(11, infoItem);

                            ItemStack removeItem = new ItemStack(Material.BARRIER);
                            removeItem.editMeta(meta -> {
                                meta.displayName(
                                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.removeitem"))
                                );
                                meta.lore(
                                        List.of(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.remove"))
                                        ));
                            });

                            overview.setItem(15, removeItem);
                            overview.setItem(18, getBackArrow(event.getClickedInventory()));

                            player.openInventory(overview);
                        } else {
                            player.sendMessage(
                                    Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.notfound"))
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
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Player")) || event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted IP"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if(!player.hasPermission("keklist.gui.whitelist"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            if (event.getCurrentItem().getType() == Material.ARROW) {
                int pageIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                int skipIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER);
                boolean onlyPlayer = false;
                boolean onlyIp = false;
                try {
                    onlyPlayer = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                try {
                    onlyIp = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                player.openInventory(WhitelistEvent.getPage(pageIndex, skipIndex, onlyPlayer, onlyIp));
            }
        }
    }

    @EventHandler
    public void onRemoveClick(InventoryClickEvent event) throws SQLException {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Player"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            if(!player.hasPermission("keklist.gui.whitelist"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            LanguageUtil translations = Keklist.getTranslations();

            if (event.getCurrentItem().getType() == Material.BARRIER) {
                ItemStack item = event.getClickedInventory().getItem(4);
                String username = ((SkullMeta) item.getItemMeta()).getOwningPlayer().getName();

                Keklist.getDatabase().onUpdate("DELETE FROM whitelist WHERE name = ?", username);
                player.sendMessage(
                        Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.player.removed", username))
                );

                player.openInventory(WhitelistEvent.getPage(0, 0, false, false)); // We can't use the back arrow here because the player is not in the inventory anymore and values may change
            }
        } else if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted IP"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if (!(event.getWhoClicked() instanceof Player player)) return;

            LanguageUtil translations = Keklist.getTranslations();

            if (event.getCurrentItem().getType() == Material.BARRIER) {
                ItemStack item = event.getClickedInventory().getItem(4);
                String ip = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());

                Keklist.getDatabase().onUpdate("DELETE FROM whitelistIp WHERE ip = ?", ip);
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(translations.get("gui.whitelist.entry.ip.removed", ip)));

                player.openInventory(WhitelistEvent.getPage(0, 0, false, false)); // We can't use the back arrow here because the player is not in the inventory anymore and values may change
            }
        }
    }

    private ItemStack getBackArrow(Inventory inventory) {
        int pageIndex = 0;
        int skipIndex = 0;
        boolean onlyPlayer = false;
        boolean onlyIp = false;

        ItemStack item = new ItemStack(Material.ARROW);
        item.editMeta(meta -> {
            meta.displayName(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.back"))
            );
            meta.lore(Collections.singletonList(
                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.entry.back.lore"))
            ));
        });

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex);
        container.set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex);
        container.set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, onlyPlayer ? 1 : 0);
        container.set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, onlyIp ? 1 : 0);

        item.setItemMeta(meta);
        return item;
    }
}
