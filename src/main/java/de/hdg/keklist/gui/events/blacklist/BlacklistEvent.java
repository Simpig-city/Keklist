package de.hdg.keklist.gui.events.blacklist;

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
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BlacklistEvent implements Listener {

    private final HashMap<Location, BlockData> signMap = new HashMap<>();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    @EventHandler
    public void onBlacklistClick(InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Blacklist"))) {
            event.setCancelled(true);

            if (!player.hasPermission("keklist.gui.blacklist")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }

            switch (event.getCurrentItem().getType()) {
                case DARK_OAK_SIGN -> {
                    Location location = player.getLocation();
                    location.setY(location.y() + 2);
                    location.setPitch(0);
                    location.setYaw(0);

                    signMap.put(location.toBlockLocation(), location.getBlock().getBlockData());
                    player.getWorld().setBlockData(location, Material.DARK_OAK_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(location);
                    sign.setWaxed(false);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "blacklistMode"), PersistentDataType.STRING, "add");
                    sign.getSide(Side.FRONT).line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.sign.line")));
                    sign.getSide(Side.FRONT).line(1, Component.empty());
                    sign.update();

                    player.getWorld().setBlockData(location, sign.getBlockData());

                    executor.schedule(() -> {
                        if (!signMap.containsKey(location)) return;
                        player.getWorld().setBlockData(location, signMap.get(location));
                        signMap.remove(location);
                    }, 2, TimeUnit.MINUTES);

                    Bukkit.getOnlinePlayers().forEach(p -> p.sendBlockChange(location, Material.AIR.createBlockData()));
                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> player.openSign(sign, Side.FRONT), 5);
                }
                case DARK_PRISMARINE -> {
                    Location location = player.getLocation();
                    location.setY(location.y() + 2);
                    location.setPitch(0);
                    location.setYaw(0);

                    signMap.put(location.toBlockLocation(), location.getBlock().getBlockData());
                    player.getWorld().setBlockData(location, Material.DARK_OAK_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(location);
                    sign.setWaxed(false);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "blacklistMode"), PersistentDataType.STRING, "remove");
                    sign.getSide(Side.FRONT).line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.sign.line")));
                    sign.getSide(Side.FRONT).line(1, Component.empty());
                    sign.update();

                    executor.schedule(() -> {
                        if (!signMap.containsKey(location)) return;
                        player.getWorld().setBlockData(location, signMap.get(location));
                        signMap.remove(location);
                    }, 2, TimeUnit.MINUTES);

                    Bukkit.getOnlinePlayers().forEach(p -> p.sendBlockChange(location, Material.AIR.createBlockData()));
                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> player.openSign(sign, Side.FRONT), 5);
                }
                case PLAYER_HEAD -> player.openInventory(getPage(0, 0, false, false, false));
                case ARROW -> GuiManager.openMainGUI(player);
            }
        }
    }

    @EventHandler
    public void onPageChange(InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Blacklist Players"))) {
            if (event.getCurrentItem().getType().equals(Material.ARROW)) {
                event.setCancelled(true);

                int pageIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                int skipIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER);
                boolean onlyPlayer = false;
                boolean onlyIp = false;
                boolean onlyMOTD = false;
                try {
                    onlyPlayer = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                try {
                    onlyIp = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                try {
                    onlyMOTD = 0 != event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER);
                } catch (Exception ignored) {
                }

                player.openInventory(getPage(pageIndex, skipIndex, onlyPlayer, onlyIp, onlyMOTD));
            }

            if (event.getCurrentItem().getType().equals(Material.BARRIER)) {
                event.setCancelled(true);
                GuiManager.handleMainGUICLick("blacklist", player);
            }
        }
    }

    @EventHandler
    public void onSignDestroy(BlockBreakEvent event) {
        if (event.getBlock().getType().equals(Material.SPRUCE_SIGN)) {
            Sign sign = (Sign) event.getBlock().getState();

            if (sign.getPersistentDataContainer().has(new NamespacedKey(Keklist.getInstance(), "blacklistMode"), PersistentDataType.STRING)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.sign.destroy")));
            }
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent event) {
        if (event.getBlock().getType().equals(Material.DARK_OAK_SIGN)) {
            Sign sign = (Sign) event.getBlock().getWorld().getBlockState(event.getBlock().getLocation());

            if (sign.getPersistentDataContainer().has(new NamespacedKey(Keklist.getInstance(), "blacklistMode"), PersistentDataType.STRING)) {
                switch (sign.getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "blacklistMode"), PersistentDataType.STRING)) {
                    case "add" -> {
                        if (event.lines().get(2).equals(Component.text("motd"))) {
                            Bukkit.dispatchCommand(event.getPlayer(), "blacklist motd " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                        } else {
                            Bukkit.dispatchCommand(event.getPlayer(), "blacklist add " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                        }
                    }
                    case "remove" ->
                            Bukkit.dispatchCommand(event.getPlayer(), "blacklist remove " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                }

                sign.getWorld().setBlockData(sign.getLocation(), signMap.get(sign.getLocation()));
                signMap.remove(sign.getLocation());

                GuiManager.handleMainGUICLick("blacklist", event.getPlayer());
            }
        }
    }


    public static Inventory getPage(int pageIndex, int skipIndex, boolean onlyPlayer, boolean onlyIP, boolean onlyMOTD) throws SQLException {
        Inventory blacklist = Bukkit.createInventory(null, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Blacklist Players"));

        List<ItemStack> playerHeads = new ArrayList<>();
        List<ItemStack> ipItems = new ArrayList<>();
        List<ItemStack> motdItems = new ArrayList<>();

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextPageMeta = nextPage.getItemMeta();
        nextPageMeta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.next"))
        );

        ItemStack previousPage = new ItemStack(Material.ARROW);
        ItemMeta previousPageMeta = previousPage.getItemMeta();
        previousPageMeta.displayName(
                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.previous"))
        );


        if (pageIndex == 0) {
            ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM blacklist");
            ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp");
            ResultSet motds = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd");

            while (players.next()) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                skullMeta.displayName(
                        Keklist.getInstance().getMiniMessage().deserialize(players.getString("name"))
                );
                skullMeta.lore(Collections.singletonList(
                        Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                ));

                if(Bukkit.getOfflinePlayerIfCached(players.getString("name")) != null)
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayerIfCached(players.getString("name")));

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
                        Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                ));
                ip.setItemMeta(ipMeta);
                ipItems.add(ip);
            }

            while (motds.next()) {
                ItemStack motd = new ItemStack(Material.WRITABLE_BOOK);
                ItemMeta motdItemMeta = motd.getItemMeta();
                motdItemMeta.displayName(
                        Keklist.getInstance().getMiniMessage().deserialize(motds.getString("ip") + "(MOTD)")
                );
                motdItemMeta.lore(Collections.singletonList(
                        Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                ));
                motd.setItemMeta(motdItemMeta);
                motdItems.add(motd);
            }


            if (playerHeads.size() > 18) {
                for (int i = 0; i < 18; i++) {
                    blacklist.setItem(i, playerHeads.get(i));
                }


                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 1);
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, 18); // We have 18 heads

                nextPage.setItemMeta(nextPageMeta);
                blacklist.setItem(26, nextPage);

            } else if ((playerHeads.size() + ipItems.size()) > 18) {
                int sharedI = 0;
                for (ItemStack playerHead : playerHeads) {
                    blacklist.setItem(sharedI, playerHead);
                    sharedI++;
                }

                int ipIndex = 0;
                for (ItemStack ipItem : ipItems) {
                    if (sharedI == 18) break;
                    blacklist.setItem(sharedI, ipItem);
                    sharedI++;
                    ipIndex++;
                }

                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 1);
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, ipIndex); // We used X ips already

                nextPage.setItemMeta(nextPageMeta);
                blacklist.setItem(26, nextPage);
            } else if ((playerHeads.size() + ipItems.size() + motdItems.size()) > 18) {
                int sharedI = 0;
                for (ItemStack playerHead : playerHeads) {
                    blacklist.setItem(sharedI, playerHead);
                    sharedI++;
                }

                for (ItemStack ipItem : ipItems) {
                    blacklist.setItem(sharedI, ipItem);
                    sharedI++;
                }

                int motdIndex = 0;
                for (ItemStack motdItem : motdItems) {
                    if (sharedI == 18) break;
                    blacklist.setItem(sharedI, motdItem);
                    sharedI++;
                    motdIndex++;
                }

                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 1);
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, motdIndex); // We used X ips already

                nextPage.setItemMeta(nextPageMeta);
                blacklist.setItem(26, nextPage);
            } else {
                int sharedI = 0;
                for (ItemStack playerHead : playerHeads) {
                    blacklist.setItem(sharedI, playerHead);
                    sharedI++;
                }

                for (ItemStack ipItem : ipItems) {
                    blacklist.setItem(sharedI, ipItem);
                    sharedI++;
                }

                for (ItemStack motdItem : motdItems) {
                    blacklist.setItem(sharedI, motdItem);
                    sharedI++;
                }
            }

            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.back")));
            back.setItemMeta(backMeta);
            blacklist.setItem(22, back);
        }

        if (pageIndex > 0) {
            // Next Page
            if (onlyIP) {
                ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp");
                ResultSet isMOTDthere = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistMotd LIMIT 1");
                List<ItemStack> skippedIPs;

                while (ips.next()) {
                    ItemStack ip = new ItemStack(Material.BOOK);
                    ItemMeta ipMeta = ip.getItemMeta();
                    ipMeta.displayName(
                            Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip"))
                    );
                    ipMeta.lore(Collections.singletonList(
                            Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                    ));
                    ip.setItemMeta(ipMeta);
                    ipItems.add(ip);
                }

                skippedIPs = ipItems.stream().skip(skipIndex).toList();


                // There is a next page
                if (skippedIPs.size() > 18) {
                    for (int i = 0; i < 18; i++) {
                        blacklist.setItem(i, skippedIPs.get(i));
                    }

                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                    nextPage.setItemMeta(nextPageMeta);
                    blacklist.setItem(26, nextPage);
                } else {
                    int i = 0;
                    for (ItemStack ipItem : skippedIPs) {
                        blacklist.setItem(i, ipItem);
                        i++;
                    }

                    if (isMOTDthere.next()) {
                        ResultSet motds = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd");

                        while (motds.next()) {
                            ItemStack motd = new ItemStack(Material.WRITABLE_BOOK);
                            ItemMeta motdItemMeta = motd.getItemMeta();
                            motdItemMeta.displayName(
                                    Keklist.getInstance().getMiniMessage().deserialize(motds.getString("ip") + "(MOTD)")
                            );
                            motdItemMeta.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                            ));
                            motd.setItemMeta(motdItemMeta);
                            motdItems.add(motd);
                        }

                        int motdIndex = 0;
                        for (ItemStack motdItem : motdItems) {
                            if (i == 18) {
                                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 1);
                                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, motdIndex); // We used motdIndex motds already

                                nextPage.setItemMeta(nextPageMeta);
                                blacklist.setItem(26, nextPage);

                                break;
                            }
                            blacklist.setItem(i, motdItem);
                            i++;
                            motdIndex++;
                        }
                    }
                }


                // Last page must be only IPs
                if (skipIndex > 18) {
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex - 18); // We must start at skipIndex - 18 for the page before
                } else
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, -1); // We must start at 0 for the page before

                // NOTE : pageIndex*18 = player heads to skip if not onlyIP mode is active
            } else if (onlyMOTD) {
                ResultSet motds = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd");
                List<ItemStack> skippedMOTDs;

                while (motds.next()) {
                    ItemStack motd = new ItemStack(Material.WRITABLE_BOOK);
                    ItemMeta motdItemMeta = motd.getItemMeta();
                    motdItemMeta.displayName(
                            Keklist.getInstance().getMiniMessage().deserialize(motds.getString("ip") + "(MOTD)")
                    );
                    motdItemMeta.lore(Collections.singletonList(
                            Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                    ));
                    motd.setItemMeta(motdItemMeta);
                    motdItems.add(motd);
                }

                skippedMOTDs = motdItems.stream().skip(skipIndex).toList();


                // There is a next page
                if (skippedMOTDs.size() > 18) {
                    for (int i = 0; i < 18; i++) {
                        blacklist.setItem(i, skippedMOTDs.get(i));
                    }

                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                    nextPage.setItemMeta(nextPageMeta);
                    blacklist.setItem(26, nextPage);
                } else {
                    int i = 0;
                    for (ItemStack motdItem : skippedMOTDs) {
                        blacklist.setItem(i, motdItem);
                        i++;
                    }
                }


                // Last page must be only IPs
                if (skipIndex > 18) {
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex - 18); // We must start at skipIndex - 18 for the page before
                } else
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, -1); // We must start at 0 for the page before


            } else {
                ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM blacklist");
                ResultSet isIPThere = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistIp LIMIT 1");
                ResultSet isMOTDthere = Keklist.getDatabase().onQuery("SELECT ip FROM blacklistMotd LIMIT 1");

                List<ItemStack> skippedHeads;

                while (players.next()) {
                    ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    skullMeta.displayName(
                            Keklist.getInstance().getMiniMessage().deserialize(players.getString("name"))
                    );
                    skullMeta.lore(Collections.singletonList(
                            Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                    ));

                    if(Bukkit.getOfflinePlayerIfCached(players.getString("name")) != null)
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayerIfCached(players.getString("name")));

                    skull.setItemMeta(skullMeta);
                    playerHeads.add(skull);
                }

                if (skipIndex == -1) {
                    skippedHeads = playerHeads.stream().skip(18L * pageIndex).toList();
                } else
                    skippedHeads = playerHeads.stream().skip(skipIndex).toList();


                if (skippedHeads.size() > 18) {
                    for (int i = 0; i < 18; i++) {
                        blacklist.setItem(i, skippedHeads.get(i));
                    }

                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                    nextPage.setItemMeta(nextPageMeta);
                    blacklist.setItem(26, nextPage);
                } else {
                    int invI = 0;
                    for (ItemStack ipItem : skippedHeads) {
                        blacklist.setItem(invI, ipItem);
                        invI++;
                    }


                    if (isIPThere.next()) {
                        ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM blacklistIp");

                        while (ips.next()) {
                            ItemStack ip = new ItemStack(Material.BOOK);
                            ItemMeta ipMeta = ip.getItemMeta();
                            ipMeta.displayName(
                                    Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip"))
                            );
                            ipMeta.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                            ));
                            ip.setItemMeta(ipMeta);
                            ipItems.add(ip);
                        }


                        // There is a next page
                        if (ipItems.size() > 18 - invI) {
                            for (int i = invI; i < 18; i++) {
                                blacklist.setItem(i, ipItems.get(i));
                            }

                            nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);
                            nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                            nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, 18 - invI); // We used 18 - invI ips already

                            nextPage.setItemMeta(nextPageMeta);
                            blacklist.setItem(26, nextPage);
                        } else {
                            int i = 0;
                            for (ItemStack ipItem : ipItems) {
                                blacklist.setItem(i, ipItem);
                                i++;
                            }

                            if (isMOTDthere.next()) {
                                ResultSet motds = Keklist.getDatabase().onQuery("SELECT * FROM blacklistMotd");

                                while (motds.next()) {
                                    ItemStack motd = new ItemStack(Material.WRITABLE_BOOK);
                                    ItemMeta motdItemMeta = motd.getItemMeta();
                                    motdItemMeta.displayName(
                                            Keklist.getInstance().getMiniMessage().deserialize(motds.getString("ip") + "(MOTD)")
                                    );
                                    motdItemMeta.lore(Collections.singletonList(
                                            Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.list.entry"))
                                    ));
                                    motd.setItemMeta(motdItemMeta);
                                    motdItems.add(motd);
                                }

                                int motdIndex = 0;
                                for (ItemStack motdItem : motdItems) {
                                    if (i == 18) {
                                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 1);
                                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyMOTD"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, motdIndex); // We used motdIndex motds already

                                        nextPage.setItemMeta(nextPageMeta);
                                        blacklist.setItem(26, nextPage);

                                        break;
                                    }
                                    blacklist.setItem(i, motdItem);
                                    i++;
                                    motdIndex++;
                                }
                            }
                        }
                    }
                }

                // Last page must be only Players
                if (skipIndex > 18 && onlyPlayer) {
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex - 18); // We must start at skipIndex - 18 for the page before
                } else
                    previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, -1); // We must start at 0 for the page before

                // NOTE : pageIndex shows if it's the first page
            }


            previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex - 1);
            previousPage.setItemMeta(previousPageMeta);
            blacklist.setItem(18, previousPage);
        }

        return blacklist;
    }
}
