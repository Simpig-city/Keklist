package de.hdg.keklist.gui.events.blacklist;

import com.destroystokyo.paper.profile.PlayerProfile;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.gui.GuiManager;
import de.hdg.keklist.util.TypeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BlacklistEvent implements Listener {

    private final HashMap<Location, BlockData> signMap = new HashMap<>();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    @EventHandler
    public void onBlacklistClick(@NotNull InventoryClickEvent event) throws SQLException {
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
                        location.getWorld().setBlockData(location, signMap.get(location));
                        signMap.remove(location);
                    }, 2, TimeUnit.MINUTES);

                    Bukkit.getOnlinePlayers().forEach(p -> p.sendBlockChange(location, Material.AIR.createBlockData()));
                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> player.openSign(sign, Side.FRONT), 5);
                }
                case PLAYER_HEAD -> player.openInventory(getPage(0));
                case ARROW -> GuiManager.openMainGUI(player);
            }
        }
    }

    @EventHandler
    public void onSignDestroy(@NotNull BlockBreakEvent event) {
        if (event.getBlock().getType().equals(Material.SPRUCE_SIGN)) {
            Sign sign = (Sign) event.getBlock().getState();

            if (signMap.containsKey(sign.getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.blacklist.sign.destroy")));
            }
        }
    }

    @EventHandler
    public void onSign(@NotNull SignChangeEvent event) {
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

                GuiManager.handleMainGUICLick(GuiManager.GuiPage.BLACKLIST, event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onPageChange(@NotNull InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Blacklist Players"))) {
            switch (event.getCurrentItem().getType()) {
                case BARRIER -> GuiManager.handleMainGUICLick(GuiManager.GuiPage.BLACKLIST, player);
                case ARROW -> {
                    if (event.getClickedInventory().getItem(22) != null) {
                        if (event.getClickedInventory().getItem(22).getItemMeta() != null && event.getClickedInventory().getItem(22).getType().equals(Material.GRAY_DYE)) {
                            int currentPageIndex = event.getClickedInventory().getItem(22).getItemMeta().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER);

                            switch (event.getSlot()) {
                                case 18 -> player.openInventory(getPage(Math.max(0, currentPageIndex - 1)));
                                case 26 -> player.openInventory(getPage(currentPageIndex + 1));
                                default -> player.openInventory(getPage(0)); /* Failsafe */
                            }
                        }
                    }
                }
            }
        }
    }

    public static @NotNull Inventory getPage(@Range(from = 0, to = Integer.MAX_VALUE) int pageIndex) throws SQLException {
        Inventory blacklistInv = Bukkit.createInventory(null, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Blacklist Players"));

        List<ItemStack> pageEntryItems = new ArrayList<>();

        ItemStack nextPage = new ItemStack(Material.ARROW);
        nextPage.editMeta(meta ->
                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.list.next")))
        );

        ItemStack previousPage = new ItemStack(Material.ARROW);
        previousPage.editMeta(meta ->
                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.list.previous")))
        );

        ItemStack mainMenu = new ItemStack(Material.BARRIER);
        mainMenu.editMeta(meta ->
                meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.back")))
        );

        ItemStack pageInfo = new ItemStack(Material.GRAY_DYE);
        pageInfo.editMeta(meta -> {
                    meta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "pageIndex"), PersistentDataType.INTEGER, pageIndex);
                    meta.displayName(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.list.index", pageIndex + 1)));
                }
        );

        try (DB.QueryResult entries = Keklist.getDatabase().onQuery("SELECT entries.entry, entries.name FROM (SELECT uuid AS entry, name AS name FROM blacklist UNION ALL SELECT ip AS entry, 'nul' AS name FROM blacklistIp UNION ALL SELECT ip AS entry, '-MOTD#' AS entry FROM blacklistMotd) AS entries LIMIT ?,19", pageIndex * 18)) {
            while (entries.resultSet().next()) {
                switch (TypeUtil.getEntryType(entries.resultSet().getString("entry"))) {
                    case UUID -> {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        meta.displayName(
                                Keklist.getInstance().getMiniMessage().deserialize(entries.resultSet().getString("name"))
                        );
                        meta.lore(Collections.singletonList(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.list.entry"))
                        ));

                        UUID playerUuid = UUID.fromString(entries.resultSet().getString("entry"));

                        // Its intended behavior that only steve is displayed as head until the future finishes. They will get replaced as soon as their textures are loaded. Without the load option enabled, the different variants of default skins will be used, as they get randomly selected when using an offline player
                        if (Keklist.getInstance().getConfig().getBoolean("general.load-heads-in-gui", true)) {
                            boolean isFloodgatePlayer = false;
                            if (Keklist.getInstance().getFloodgateApi() != null)
                                isFloodgatePlayer = Keklist.getInstance().getFloodgateApi().isFloodgateId(playerUuid);

                            if (isFloodgatePlayer) {
                                meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid));
                            } else {
                                String playerName = entries.resultSet().getString("name");

                                Bukkit.createProfile(playerUuid, playerName).update().thenAcceptAsync(
                                        playerProfile -> replaceHead(blacklistInv, playerName, playerProfile),
                                        runnable -> Bukkit.getScheduler().runTask(Keklist.getInstance(), runnable)
                                );
                            }
                        } else
                            meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid));

                        skull.setItemMeta(meta);
                        pageEntryItems.add(skull);
                    }

                    case IPv4, IPv6 -> {
                        if (entries.resultSet().getString("name").equals("-MOTD#")) {
                            ItemStack motd = new ItemStack(Material.WRITABLE_BOOK);
                            ItemMeta motdItemMeta = motd.getItemMeta();
                            motdItemMeta.displayName(
                                    Keklist.getInstance().getMiniMessage().deserialize(entries.resultSet().getString("entry") + "(MOTD)")
                            );
                            motdItemMeta.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.list.entry"))
                            ));
                            motd.setItemMeta(motdItemMeta);
                            pageEntryItems.add(motd);
                        } else {
                            ItemStack ip = new ItemStack(Material.BOOK);
                            ItemMeta ipMeta = ip.getItemMeta();
                            ipMeta.displayName(
                                    Keklist.getInstance().getMiniMessage().deserialize(entries.resultSet().getString("entry"))
                            );
                            ipMeta.lore(Collections.singletonList(
                                    Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.list.entry"))
                            ));
                            ip.setItemMeta(ipMeta);
                            pageEntryItems.add(ip);
                        }
                    }
                }
            }
        }

        if (pageEntryItems.isEmpty() && pageIndex != 0)
            return getPage(pageIndex - 1);

        if (pageEntryItems.size() > 18) {
            /* We limit to 19, so we know if there are more entries; one query more is no significant hit and still adds QoL */
            blacklistInv.setItem(26, nextPage);
            pageEntryItems.removeLast(); // Remove the last entry, which is the one we used to check if there are more entries
        }

        if (pageIndex > 0)
            blacklistInv.setItem(18, previousPage);
        else
            blacklistInv.setItem(18, mainMenu);

        blacklistInv.setItem(22, pageInfo);

        pageEntryItems.forEach(blacklistInv::addItem);

        return blacklistInv;
    }

     /**
     * Replaces the head of the player in the inventory with the given profile
     *
     * @param inventory The blacklist page inventory
     * @param playerName The name of the player
     * @param profile The profile of the player provided by the future
     */
    private static void replaceHead(@NotNull Inventory inventory, @NotNull String playerName, @NotNull PlayerProfile profile) {
        Arrays.stream(inventory.getContents())
                .filter(Objects::nonNull)
                .filter(itemStack -> itemStack.getType().equals(Material.PLAYER_HEAD))
                .filter(ItemStack::hasItemMeta)
                .filter(itemStack -> PlainTextComponentSerializer.plainText().serialize(itemStack.getItemMeta().displayName()).equals(playerName))
                .findFirst().ifPresent(itemStack ->
                        itemStack.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(profile))
                );
    }
}
