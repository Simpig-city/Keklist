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
    private HashMap<Player, Block> signMap = new HashMap<>();

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

                    Sign sign = (Sign) player.getWorld().getBlockAt(new Location(player.getWorld(), 0, 0, Bukkit.getMaxWorldSize() - 1)).getBlockData();
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "signTypeDataMode"), PersistentDataType.STRING, "whitelist");
                    sign.line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.sign")));
                    player.openSign(sign);

                    signMap.put(player, block);
                }

                case PLAYER_HEAD -> {
                    Inventory whitelist = Bukkit.createInventory(player, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"));

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

                    if(playerHeads.size() > 18) {
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
                    }else {
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

                    player.openInventory(whitelist);
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
                if(caseType == null) return;

                switch (caseType){
                    case "whitelist" -> Bukkit.dispatchCommand(event.getPlayer(), "whitelist add " +  serializer.serialize(sign.line(1)));
                    case "removeWhitelist" -> Bukkit.dispatchCommand(event.getPlayer(), "whitelist remove " +  serializer.serialize(sign.line(1)));
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

        if(event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"))){
            if(event.getCurrentItem().getType().equals(Material.ARROW)) {
                int pageIndex = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);
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
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(players.getString("uuid")));
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

                    if(playerHeads.size() > 18) {
                        for (int i = 0; i < 18; i++) {
                            whitelist.setItem(i, playerHeads.get(i));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, playerEntries+18);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex+1);

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    } else if ((playerHeads.size() + ipItems.size()) > 18) {
                        int sharedI = 0;
                        for (int i = sharedI; sharedI < playerHeads.size(); sharedI++) {
                            whitelist.setItem(sharedI, playerHeads.get(sharedI));
                        }
                        for (int i = sharedI; sharedI < ipItems.size(); sharedI++) {
                            if(sharedI == 18) break;
                            whitelist.setItem(sharedI, ipItems.get(sharedI));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, sharedI);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex+1);

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    }else {
                        int sharedI = 0;
                        for (int i = sharedI; sharedI < playerHeads.size(); sharedI++) {
                            whitelist.setItem(sharedI, playerHeads.get(sharedI));
                        }
                        for (int i = sharedI; sharedI < ipItems.size(); sharedI++) {
                            whitelist.setItem(sharedI, ipItems.get(sharedI));
                        }
                    }

                    ItemStack previousPage = new ItemStack(Material.ARROW);
                    ItemMeta previousPageMeta = previousPage.getItemMeta();
                    previousPageMeta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.whitelist.list.previous")));
                    previousPage.setItemMeta(previousPageMeta);

                    if(playerEntries > 18) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, playerEntries-18);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex-1);

                        previousPage.setItemMeta(previousPageMeta);
                        whitelist.setItem(18, previousPage);
                    } else if (playerEntries > 0) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, 0);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex-1);

                        previousPage.setItemMeta(previousPageMeta);
                        whitelist.setItem(18, previousPage);
                    }


                }catch (NullPointerException onlyIpEntries) {
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

                    if(ipItems.size() > 18 + pageIndex*18) {
                        for (int i = 0; i < ipEntries + 18; i++) {
                            whitelist.setItem(i, ipItems.get(i+ipEntries));
                        }

                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, ipEntries+ 18);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "playerEntries"), PersistentDataType.INTEGER, pageIndex*18);
                        nextPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex+1);

                        nextPage.setItemMeta(nextPageMeta);
                        whitelist.setItem(26, nextPage);
                    }else {
                        for (int i = 0; i < 18; i++) {
                            whitelist.setItem(i, ipItems.get(i+ipEntries));
                        }
                    }



                    if(ipEntries > 18) {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, ipEntries - 18);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex-1);
                    }else {
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "ipEntries"), PersistentDataType.INTEGER, 0);
                        previousPageMeta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex-1);
                    }

                    previousPage.setItemMeta(previousPageMeta);
                    whitelist.setItem(18, previousPage);
                }

                player.openInventory(whitelist);
            }
        }
    }
}
