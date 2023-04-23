package de.hdg.keklist.gui.events.whitelist;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.LanguageUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EntryEvent implements Listener {

    @EventHandler
    public void onEntryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"))) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;
            if(!(event.getWhoClicked() instanceof Player player)) return;

            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
            LanguageUtil lang = Keklist.getLanguage();

            switch (event.getCurrentItem().getType()) {
                case PLAYER_HEAD -> {
                    Inventory overview = Bukkit.createInventory(player, 27, Keklist.getInstance().getMiniMessage().deserialize("<blue><b>Whitelisted Player"));
                    ItemStack item = event.getCurrentItem();

                    String username = ((SkullMeta) item.getItemMeta()).getOwningPlayer().getName();
                    ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE name = '" + username + "'");

                    try {
                        if (rs.next()) {
                            long unix = rs.getLong("unix");
                            String byPlayer = rs.getString("byPlayer");
                            UUID uuid = UUID.fromString(rs.getString("uuid"));

                            SimpleDateFormat sdf = new SimpleDateFormat(Keklist.getInstance().getConfig().getString("date-format"));

                            item.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.entry.info"))));
                            overview.setItem(4, item);

                            ItemStack infoItem = new ItemStack(Material.BOOK);
                            infoItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.player.infoitem")));
                                meta.lore(List.of(
                                        Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.player.name", username)),
                                        Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.player.uuid", uuid.toString())),
                                        Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.player.by", byPlayer)),
                                        Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.player.date", sdf.format(new Date(unix))))
                                ));
                            });

                            overview.setItem(11, infoItem);

                            ItemStack removeItem = new ItemStack(Material.BARRIER);
                            removeItem.editMeta(meta -> {
                                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.player.removeitem")));
                                meta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.remove"))));
                            });

                            overview.setItem(15, removeItem);

                        } else {
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(lang.get("gui.whitelist.entry.player.notfound")));
                            player.closeInventory();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                   }

                case PAPER -> {

                }
            }

            int pageIndex = event.getInventory().getItem(26).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
            int skipIndex = event.getInventory().getItem(26).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER);
            boolean onlyPlayer = false;
            boolean onlyIp = false;
            try {onlyPlayer = 0 != event.getInventory().getItem(26).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER);} catch (Exception ignored) {}
            try {onlyIp = 0 != event.getInventory().getItem(26).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER);} catch (Exception ignored) {}

        }
    }
}
