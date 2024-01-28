package de.hdg.keklist.gui.events.whitelist;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.gui.GuiManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class WhitelistEvent implements Listener {

    private final HashMap<Player, Block> signMap = new HashMap<>();

    @EventHandler
    public void onWhitelistClick(InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Whitelist"))) {
            event.setCancelled(true);

            if(!player.hasPermission("keklist.gui.whitelist"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            switch (event.getCurrentItem().getType()) {
                case SPRUCE_SIGN -> {
                    Location location = player.getLocation();
                    location.setY(location.getWorld().getMaxHeight() - 1);

                    signMap.put(player, location.getBlock());
                    player.getWorld().setBlockData(location, Material.SPRUCE_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(location);
                    sign.setWaxed(false);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING, "add");
                    sign.getSide(Side.FRONT).line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.sign.line")));
                    sign.getSide(Side.FRONT).line(1, Component.empty());
                    sign.update();

                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> player.openSign(sign, Side.FRONT), 5);
                }
                case PRISMARINE_SHARD -> {
                    Location location = player.getLocation();
                    location.setY(location.getWorld().getMaxHeight() - 1);

                    signMap.put(player, location.getBlock());
                    player.getWorld().setBlockData(location, Material.SPRUCE_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(location);
                    sign.setWaxed(false);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING, "remove");
                    sign.getSide(Side.FRONT).line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.sign.line")));
                    sign.getSide(Side.FRONT).line(1, Component.empty());
                    sign.update();

                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> player.openSign(sign, Side.FRONT), 5);
                }
                case PLAYER_HEAD -> player.openInventory(getPage(0, 0, false, false));
                case ARROW -> GuiManager.openMainGUI(player);
            }
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent event) {
        if (event.getBlock().getType().equals(Material.SPRUCE_SIGN)) {
            Sign sign = (Sign) event.getBlock().getWorld().getBlockState(event.getBlock().getLocation());

            if (sign.getPersistentDataContainer().has(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING)) {
                switch (sign.getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING)) {
                    case "add" ->
                            Bukkit.dispatchCommand(event.getPlayer(), "whitelist add " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                    case "remove" ->
                            Bukkit.dispatchCommand(event.getPlayer(), "whitelist remove " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                }

                Block block = signMap.get(event.getPlayer());
                block.getWorld().setBlockData(block.getLocation(), block.getBlockData());
                signMap.remove(event.getPlayer());

                GuiManager.handleMainGUICLick("whitelist", event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onPageChange(InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"))) {
            if (event.getCurrentItem().getType().equals(Material.ARROW)) {
                event.setCancelled(true);

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

                player.openInventory(getPage(pageIndex, skipIndex, onlyPlayer, onlyIp));
            }

            if (event.getCurrentItem().getType().equals(Material.BARRIER)) {
                event.setCancelled(true);
                GuiManager.handleMainGUICLick("whitelist", player);
            }
        }
    }

    public static Inventory getPage(int pageIndex, int skipIndex, boolean onlyPlayer, boolean onlyIP) throws SQLException {
        Inventory whitelist = Bukkit.createInventory(null, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"));

        List<ItemStack> playerHeads = new ArrayList<>();
        List<ItemStack> ipItems = new ArrayList<>();

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextPageMeta = nextPage.getItemMeta();
        nextPageMeta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.list.next"))
        );

        ItemStack previousPage = new ItemStack(Material.ARROW);
        ItemMeta previousPageMeta = previousPage.getItemMeta();
        previousPageMeta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.list.previous"))
        );


        if (pageIndex == 0) {
            ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM whitelist");
            ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");

            while (players.next()) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                skullMeta.displayName(
                        Keklist.getInstance().getMiniMessage().deserialize(players.getString("name"))
                );
                skullMeta.lore(Collections.singletonList(
                        Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.list.entry"))
                ));
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(players.getString("name")));
                skull.setItemMeta(skullMeta);
                playerHeads.add(skull);
            }

            while (ips.next()) {
                ItemStack ip = new ItemStack(Material.BOOK);
                ItemMeta ipMeta = ip.getItemMeta();
                ipMeta.displayName(
                        Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip"))
                );
                ipMeta.lore(Collections.singletonList(
                        Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.list.entry"))
                ));
                ip.setItemMeta(ipMeta);
                ipItems.add(ip);
            }


            if (playerHeads.size() > 18) {
                for (int i = 0; i < 18; i++) {
                    whitelist.setItem(i, playerHeads.get(i));
                }


                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 1);
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, 18); // We have 18 heads

                nextPage.setItemMeta(nextPageMeta);
                whitelist.setItem(26, nextPage);

            } else if ((playerHeads.size() + ipItems.size()) > 18) {
                int sharedI = 0;
                for (ItemStack playerHead : playerHeads) {
                    whitelist.setItem(sharedI, playerHead);
                    sharedI++;
                }

                int ipIndex = 0;
                for (ItemStack ipItem : ipItems) {
                    if (sharedI == 18) break;
                    whitelist.setItem(sharedI, ipItem);
                    sharedI++;
                    ipIndex++;
                }

                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 1);
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, ipIndex); // We used X ips already

                nextPage.setItemMeta(nextPageMeta);
                whitelist.setItem(26, nextPage);
            } else {
                int sharedI = 0;
                for (ItemStack playerHead : playerHeads) {
                    whitelist.setItem(sharedI, playerHead);
                    sharedI++;
                }

                for (ItemStack ipItem : ipItems) {
                    whitelist.setItem(sharedI, ipItem);
                    sharedI++;
                }
            }

            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.back")));
            back.setItemMeta(backMeta);
            whitelist.setItem(22, back);
        }

        if (pageIndex > 0) {
            // Next Page
            if (onlyIP) {
                ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");
                List<ItemStack> skippedIPs;

                while (ips.next()) {
                    ItemStack ip = new ItemStack(Material.BOOK);
                    ItemMeta ipMeta = ip.getItemMeta();
                    ipMeta.displayName(
                            Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip"))
                    );
                    ipMeta.lore(Collections.singletonList(
                            Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.list.entry"))
                    ));
                    ip.setItemMeta(ipMeta);
                    ipItems.add(ip);
                }

                skippedIPs = ipItems.stream().skip(skipIndex).toList();


                // There is a next page
                if (skippedIPs.size() > 18) {
                    for (int i = 0; i < 18; i++) {
                        whitelist.setItem(i, skippedIPs.get(i));
                    }

                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                    nextPage.setItemMeta(nextPageMeta);
                    whitelist.setItem(26, nextPage);
                } else {
                    int i = 0;
                    for (ItemStack ipItem : skippedIPs) {
                        whitelist.setItem(i, ipItem);
                        i++;
                    }
                }


                // The Last page must be only IPs
                if (skipIndex > 18) {
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex - 18); // We must start at skipIndex - 18 for the page before
                } else
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, -1); // We must start at 0 for the page before

                // NOTE: pageIndex*18 = player heads to skip if not onlyIP mode is active
            } else {
                ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM whitelist");
                ResultSet isIPThere = Keklist.getDatabase().onQuery("SELECT ip FROM whitelistIp LIMIT 1");
                List<ItemStack> skippedHeads;

                while (players.next()) {
                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    skullMeta.displayName(
                            Keklist.getInstance().getMiniMessage().deserialize(players.getString("name"))
                    );
                    skullMeta.lore(Collections.singletonList(
                            Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.list.entry"))
                    ));
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(players.getString("name")));
                    skull.setItemMeta(skullMeta);
                    playerHeads.add(skull);
                }

                if (skipIndex == -1) {
                    skippedHeads = playerHeads.stream().skip(18L * pageIndex).toList();
                } else
                    skippedHeads = playerHeads.stream().skip(skipIndex).toList();


                if (skippedHeads.size() > 18) {
                    for (int i = 0; i < 18; i++) {
                        whitelist.setItem(i, skippedHeads.get(i));
                    }

                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                    nextPage.setItemMeta(nextPageMeta);
                    whitelist.setItem(26, nextPage);
                } else {
                    int invI = 0;
                    for (ItemStack ipItem : skippedHeads) {
                        whitelist.setItem(invI, ipItem);
                        invI++;
                    }


                    if (isIPThere.next()) {
                        ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");

                        while (ips.next()) {
                            ItemStack ip = new ItemStack(Material.BOOK);
                            ItemMeta ipMeta = ip.getItemMeta();
                            ipMeta.displayName(
                                    Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip"))
                            );
                            ipMeta.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.list.entry"))
                            ));
                            ip.setItemMeta(ipMeta);
                            ipItems.add(ip);
                        }


                        // There is a next page
                        if (ipItems.size() > 18 - invI) {
                            for (int i = invI; i < 18; i++) {
                                whitelist.setItem(i, ipItems.get(i));
                            }

                            nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);
                            nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                            nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, 18 - invI); // We used 18 - invI ips already

                            nextPage.setItemMeta(nextPageMeta);
                            whitelist.setItem(26, nextPage);
                        } else {
                            int i = 0;
                            for (ItemStack ipItem : ipItems) {
                                whitelist.setItem(i, ipItem);
                                i++;
                            }
                        }
                    }
                }

                // The Last page must be only Players
                if (skipIndex > 18 && onlyPlayer) {
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex - 18); // We must start at skipIndex - 18 for the page before
                } else
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, -1); // We must start at 0 for the page before

                // NOTE: pageIndex shows if it's the first page
            }


            previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex - 1);
            previousPage.setItemMeta(previousPageMeta);
            whitelist.setItem(18, previousPage);
        }

        return whitelist;
    }
}
