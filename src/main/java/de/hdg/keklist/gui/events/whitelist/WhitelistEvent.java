package de.hdg.keklist.gui.events.whitelist;

import com.typesafe.config.ConfigException;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.commands.Whitelist;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhitelistEvent implements Listener {

    private static final OkHttpClient client = new OkHttpClient();
    private final HashMap<Player, Block> signMap = new HashMap<>();

    @EventHandler
    public void onWhitelistClick(InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Whitelist"))) {
            event.setCancelled(true);

            switch (event.getCurrentItem().getType()) {
                case SPRUCE_SIGN -> {
                    Block block = player.getWorld().getBlockAt(new Location(player.getWorld(), 0, 0, Bukkit.getMaxWorldSize() - 1));
                    player.getWorld().setBlockData(new Location(player.getWorld(), 0, 0, Bukkit.getMaxWorldSize() - 1), Material.SPRUCE_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(new Location(player.getWorld(), 0, 0, Bukkit.getMaxWorldSize() - 1));
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "signTypeDataMode"), PersistentDataType.STRING, "whitelist");
                    sign.line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.sign")));
                    player.openSign(sign);

                    signMap.put(player, block);
                }

                case PLAYER_HEAD -> {
                    /*Inventory whitelist = Bukkit.createInventory(player, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"));

                    ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM whitelist");
                    ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");

                    List<ItemStack> playerHeads = new ArrayList<>();
                    List<ItemStack> ipItems = new ArrayList<>();

                    while (players.next()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        skullMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(players.getString("name")));
                        skullMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(players.getString("name")));
                        skull.setItemMeta(skullMeta);
                        playerHeads.add(skull);
                    }

                    while (ips.next()) {
                        ItemStack ip = new ItemStack(Material.BOOK);
                        ItemMeta ipMeta = ip.getItemMeta();
                        ipMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip")));
                        ipMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        ip.setItemMeta(ipMeta);
                        ipItems.add(ip);
                    }

                    ItemStack nextPage = new ItemStack(Material.ARROW);
                    ItemMeta nextPageMeta = nextPage.getItemMeta();
                    nextPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.next")));
                    nextPage.setItemMeta(nextPageMeta);

                    if (playerHeads.size() > 18) {
                        System.out.println("CASE 1");
                        for (int i = 0; i < 18; i++) {
                            whitelist.setItem(i, playerHeads.get(i));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, 18);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 0);

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    } else if ((playerHeads.size() + ipItems.size()) > 18) {
                        System.out.println("CASE 2");
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

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, ipIndex);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, 0);

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    } else {
                        System.out.println("CASE 3");
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

                    player.openInventory(whitelist);*/
                    player.openInventory(getPage(0, 0, false, false , null));
                }

                case PRISMARINE_SHARD -> {
                    Block block = player.getWorld().getBlockAt(new Location(player.getWorld(), 0, 0, Bukkit.getMaxWorldSize() - 1));
                    player.getWorld().setBlockData(new Location(player.getWorld(), 0, 0, Bukkit.getMaxWorldSize() - 1), Material.SPRUCE_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockAt(new Location(player.getWorld(), 0, 0, Bukkit.getMaxWorldSize() - 1)).getBlockData();
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "signTypeDataMode"), PersistentDataType.STRING, "removeWhitelist");
                    sign.line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.sign")));
                    player.openSign(sign);

                    signMap.put(player, block);
                }
            }
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent event) {
        if (event.getBlock().getLocation().getZ() == Bukkit.getMaxWorldSize() - 1) {
            if (event.getBlock() instanceof Sign sign) {
                PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();
                String caseType = sign.getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "signTypeDataMode"), PersistentDataType.STRING);
                if (caseType == null) return;

                switch (caseType) {
                    case "whitelist" ->
                            Bukkit.dispatchCommand(event.getPlayer(), "whitelist add " + serializer.serialize(sign.line(1)));
                    case "removeWhitelist" ->
                            Bukkit.dispatchCommand(event.getPlayer(), "whitelist remove " + serializer.serialize(sign.line(1)));
                }

                event.getPlayer().closeInventory();
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
                /*int pageIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                Inventory whitelist = Bukkit.createInventory(player, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"));
                event.setCancelled(true);

                try {
                    int playerEntries = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER);

                    ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM whitelist");
                    ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");

                    List<ItemStack> playerHeads = new ArrayList<>();
                    List<ItemStack> ipItems = new ArrayList<>();

                    while (players.next()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        skullMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(players.getString("name")));
                        skullMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(players.getString("name")));
                        skull.setItemMeta(skullMeta);
                        playerHeads.add(skull);
                    }

                    while (ips.next()) {
                        ItemStack ip = new ItemStack(Material.BOOK);
                        ItemMeta ipMeta = ip.getItemMeta();
                        ipMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip")));
                        ipMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        ip.setItemMeta(ipMeta);
                        ipItems.add(ip);
                    }

                    ItemStack nextPage = new ItemStack(Material.ARROW);
                    ItemMeta nextPageMeta = nextPage.getItemMeta();
                    nextPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.next")));
                    nextPage.setItemMeta(nextPageMeta);

                    if (playerHeads.size() > 18) {
                        for (int i = 0; i < 18; i++) {
                            whitelist.setItem(i, playerHeads.get(i));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, playerEntries + 18);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);

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

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, ipIndex);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);

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

                    ItemStack previousPage = new ItemStack(Material.ARROW);
                    ItemMeta previousPageMeta = previousPage.getItemMeta();
                    previousPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.previous")));
                    previousPage.setItemMeta(previousPageMeta);

                    if (playerEntries > 18) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, playerEntries - 18);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex - 1);

                        previousPage.setItemMeta(previousPageMeta);
                        whitelist.setItem(18, previousPage);
                    } else if (playerEntries > 0) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, 0);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex - 1);

                        previousPage.setItemMeta(previousPageMeta);
                        whitelist.setItem(18, previousPage);
                    }


                } catch (NullPointerException onlyIpEntries) {
                    int ipEntries = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER);

                    ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");
                    List<ItemStack> ipItems = new ArrayList<>();

                    while (ips.next()) {
                        ItemStack ip = new ItemStack(Material.BOOK);
                        ItemMeta ipMeta = ip.getItemMeta();
                        ipMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip")));
                        ipMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        ip.setItemMeta(ipMeta);
                        ipItems.add(ip);
                    }

                    ItemStack nextPage = new ItemStack(Material.ARROW);
                    ItemMeta nextPageMeta = nextPage.getItemMeta();
                    nextPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.next")));
                    nextPage.setItemMeta(nextPageMeta);

                    ItemStack previousPage = new ItemStack(Material.ARROW);
                    ItemMeta previousPageMeta = previousPage.getItemMeta();
                    previousPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.previous")));
                    previousPage.setItemMeta(previousPageMeta);

                    if (ipItems.size() - ipEntries > 18 + pageIndex * 18) {
                        for (int i = ipEntries; i < ipItems.size(); i++) {
                            System.out.println(i);
                            System.out.println(ipEntries);
                            System.out.println(ipItems.size());
                            whitelist.setItem(i, ipItems.get(i - 1));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, ipEntries + 18);
                        if (ipEntries < 18)
                            nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, pageIndex * 18);

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex + 1);

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    } else {
                        for (int i = 0; i < ipItems.size() - ipEntries; i++) {
                            whitelist.setItem(i, ipItems.get(ipEntries + i));
                        }
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, 0);
                    }


                    if (ipEntries > 18) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, ipEntries - 18);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex - 1);
                    } else {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, 0);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex - 1);
                    }

                    previousPage.setItemMeta(previousPageMeta);
                    whitelist.setItem(18, previousPage);
                }

                player.openInventory(whitelist);*/

                int pageIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
                int skipIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER);
                boolean onlyPlayer = false;
                boolean onlyIp = false;
                try {
                   onlyPlayer = 0 == event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER)?false:true;
                } catch (Exception ignored){}
                try {
                    onlyIp = 0 == event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER)?false:true;
                } catch (Exception ignored){}

                player.openInventory(getPage(pageIndex, skipIndex, onlyPlayer, onlyIp, null));
            }
        }
    }

    private Inventory getPage(int pageIndex, int skipIndex, boolean onlyPlayer, boolean onlyIP, ItemStack previousItem) throws SQLException {
        Inventory whitelist = Bukkit.createInventory(null, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"));

        List<ItemStack> playerHeads = new ArrayList<>();
        List<ItemStack> ipItems = new ArrayList<>();

        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextPageMeta = nextPage.getItemMeta();
        nextPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.next")));

        ItemStack previousPage = new ItemStack(Material.ARROW);
        ItemMeta previousPageMeta = previousPage.getItemMeta();
        previousPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.previous")));


        if (pageIndex == 0) {
            ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM whitelist");
            ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");

            while (players.next()) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                skullMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(players.getString("name")));
                skullMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(players.getString("name")));
                skull.setItemMeta(skullMeta);
                playerHeads.add(skull);
            }

            while (ips.next()) {
                ItemStack ip = new ItemStack(Material.BOOK);
                ItemMeta ipMeta = ip.getItemMeta();
                ipMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip")));
                ipMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                ip.setItemMeta(ipMeta);
                ipItems.add(ip);
            }


            if (playerHeads.size() > 18) {
                for (int i = 0; i < 18; i++) {
                    whitelist.setItem(i, playerHeads.get(i));
                }


                // TODO : Add metadata
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

                // TODO : Add metadata
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
        }

        if(pageIndex > 0) {
            // Next Page
            if(previousItem == null) {
                if(onlyIP){
                    ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");
                    List<ItemStack> skippedIPs = new ArrayList<>();

                    while (ips.next()) {
                        ItemStack ip = new ItemStack(Material.BOOK);
                        ItemMeta ipMeta = ip.getItemMeta();
                        ipMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip")));
                        ipMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        ip.setItemMeta(ipMeta);
                        ipItems.add(ip);
                    }

                    skippedIPs = ipItems.stream().skip(skipIndex).toList();


                    // There is a next page
                    if(skippedIPs.size() > 18) {
                        for(int i = 0; i < 18; i++){
                            whitelist.setItem(i, skippedIPs.get(i));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex+1);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    }else {
                        int i = 0;
                        for(ItemStack ipItem : skippedIPs){
                            whitelist.setItem(i, ipItem);
                            i++;
                        }
                    }


                    // Last page must be only IPs
                    if(skipIndex > 18) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex - 18); // We must start at skipIndex - 18 for the page before
                    }else
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, -1); // We must start at 0 for the page before

                    // NOTE : pageIndex*18 = player heads to skip if not onlyIP mode is active
                }else {
                    ResultSet players = Keklist.getDatabase().onQuery("SELECT * FROM whitelist");
                    ResultSet isIPThere = Keklist.getDatabase().onQuery("SELECT ip FROM whitelistIp LIMIT 1");
                    List<ItemStack> skippedHeads = new ArrayList<>();

                    while (players.next()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        skullMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(players.getString("name")));
                        skullMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(players.getString("name")));
                        skull.setItemMeta(skullMeta);
                        playerHeads.add(skull);
                    }

                    skippedHeads = playerHeads.stream().skip(skipIndex).toList();

                    if(skippedHeads.size() > 18) {
                        for(int i = 0; i < 18; i++){
                            whitelist.setItem(i, skippedHeads.get(i));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex+1);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    }else {
                        int invI = 0;
                        for(ItemStack ipItem : skippedHeads){
                            whitelist.setItem(invI, ipItem);
                            invI++;
                        }


                        if(isIPThere.next()){
                            ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");

                            while (ips.next()) {
                                ItemStack ip = new ItemStack(Material.BOOK);
                                ItemMeta ipMeta = ip.getItemMeta();
                                ipMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip")));
                                ipMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                                ip.setItemMeta(ipMeta);
                                ipItems.add(ip);
                            }


                            // There is a next page
                            if(ipItems.size() > 18 - invI) {
                                for(int i = invI; i < 18; i++){
                                    whitelist.setItem(i, ipItems.get(i));
                                }

                                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex+1);
                                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                                nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, 18-invI ); // We used 18 - invI ips already

                                nextPage.setItemMeta(nextPageMeta);
                                whitelist.setItem(26, nextPage);
                            }else {
                                int i = 0;
                                for(ItemStack ipItem : ipItems){
                                    whitelist.setItem(i, ipItem);
                                    i++;
                                }
                            }
                        }
                    }

                    // Last page must be only Players
                    if(skipIndex > 18 && onlyPlayer) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyPlayer"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex - 18); // We must start at skipIndex - 18 for the page before
                    }else
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, -1); // We must start at 0 for the page before

                    // NOTE : pageIndex shows if it's the first page
                }/*else {
                    // This is not only player and not only ip





                    ResultSet ips = Keklist.getDatabase().onQuery("SELECT * FROM whitelistIp");

                    while (ips.next()) {
                        ItemStack ip = new ItemStack(Material.BOOK);
                        ItemMeta ipMeta = ip.getItemMeta();
                        ipMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(ips.getString("ip")));
                        ipMeta.lore(List.of(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.entry"))));
                        ip.setItemMeta(ipMeta);
                        ipItems.add(ip);
                    }


                    // There is a next page
                    if(ipItems.size() > 18) {
                        for(int i = 0; i < 18; i++){
                            whitelist.setItem(i, ipItems.get(i));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex+1);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "onlyIP"), PersistentDataType.INTEGER, 1); // Workaround for missing boolean
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "skipIndex"), PersistentDataType.INTEGER, skipIndex + 18); // We used skipIndex + 18 ips already

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    }else {
                        int i = 0;
                        for(ItemStack ipItem : ipItems){
                            whitelist.setItem(i, ipItem);
                            i++;
                        }
                    }

                }*/
            }


           // TODO : Add previous page item
            previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex-1);
            previousItem.setItemMeta(previousPageMeta);
            whitelist.setItem(18, previousPage);
        }

        return whitelist;
    }
}
