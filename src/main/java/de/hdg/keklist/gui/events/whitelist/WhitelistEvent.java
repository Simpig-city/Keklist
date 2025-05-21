package de.hdg.keklist.gui.events.whitelist;

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
import org.bukkit.event.inventory.*;
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

public class WhitelistEvent implements Listener {

    private final HashMap<Location, BlockData> signMap = new HashMap<>();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    @EventHandler
    public void onWhitelistClick(@NotNull InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Whitelist"))) {
            event.setCancelled(true);

            if (!player.hasPermission("keklist.gui.use")) {
                player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("no-permission")));
                return;
            }

            switch (event.getCurrentItem().getType()) {
                case SPRUCE_SIGN -> {
                    Location location = player.getLocation();
                    location.setY(location.y() + 2);
                    location.setPitch(0);
                    location.setYaw(0);

                    signMap.put(location.toBlockLocation(), location.getBlock().getBlockData());
                    player.getWorld().setBlockData(location, Material.SPRUCE_SIGN.createBlockData());

                    Sign sign = (Sign) location.getBlock().getState();
                    sign.setWaxed(false);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING, "add");
                    sign.getSide(Side.FRONT).line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.sign.line")));
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
                case PRISMARINE_SHARD -> {
                    Location location = player.getLocation();
                    location.setY(location.y() + 2);
                    location.setPitch(0);
                    location.setYaw(0);

                    signMap.put(location.toBlockLocation(), location.getBlock().getBlockData());
                    player.getWorld().setBlockData(location, Material.SPRUCE_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(location);
                    sign.setWaxed(false);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING, "remove");
                    sign.getSide(Side.FRONT).line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.sign.line")));
                    sign.getSide(Side.FRONT).line(1, Component.empty());
                    sign.update();

                    // Used as backup if the player never exists the sign
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
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.sign.destroy")));
            }
        }
    }

    @EventHandler
    public void onSign(@NotNull SignChangeEvent event) {
        if (event.getBlock().getType().equals(Material.SPRUCE_SIGN)) {
            Sign sign = (Sign) event.getBlock().getWorld().getBlockState(event.getBlock().getLocation());

            if (sign.getPersistentDataContainer().has(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING)) {
                String entry = PlainTextComponentSerializer.plainText().serialize(event.lines().get(1));

                if (!entry.isBlank())
                    switch (sign.getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "whitelistMode"), PersistentDataType.STRING)) {
                        case "add" -> Bukkit.dispatchCommand(event.getPlayer(), "whitelist add " + entry);
                        case "remove" -> Bukkit.dispatchCommand(event.getPlayer(), "whitelist remove " + entry);
                    }
                else
                    event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.whitelist.sign.empty")));

                sign.getWorld().setBlockData(sign.getLocation(), signMap.get(sign.getLocation()));
                signMap.remove(sign.getLocation());

                GuiManager.handleMainGUICLick(GuiManager.GuiPage.WHITELIST, event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onPageChange(@NotNull InventoryClickEvent event) throws SQLException {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"))) {
            switch (event.getCurrentItem().getType()) {
                case BARRIER -> GuiManager.handleMainGUICLick(GuiManager.GuiPage.WHITELIST, player);
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
        Inventory whitelistInv = Bukkit.createInventory(null, 9 * 3, Keklist.getInstance().getMiniMessage().deserialize("<white><b>Whitelist Players"));

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

        try (DB.QueryResult entries = Keklist.getDatabase().onQuery("SELECT entries.entry, entries.name FROM (SELECT uuid AS entry, name AS name FROM whitelist UNION ALL SELECT ip AS entry, 'nul' AS name FROM whitelistIp UNION ALL SELECT domain AS entry, 'nul' AS entry FROM whitelistDomain) AS entries LIMIT ?,19", pageIndex * 18)) {
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
                                        playerProfile -> replaceHead(whitelistInv, playerName, playerProfile),
                                        runnable -> Bukkit.getScheduler().runTask(Keklist.getInstance(), runnable)
                                );
                            }
                        } else
                            meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid));

                        skull.setItemMeta(meta);
                        pageEntryItems.add(skull);
                    }

                    case IPv4, IPv6 -> {
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

                    case DOMAIN -> {
                        ItemStack domain = new ItemStack(Material.PAPER);
                        ItemMeta domainMeta = domain.getItemMeta();
                        domainMeta.displayName(
                                Keklist.getInstance().getMiniMessage().deserialize(entries.resultSet().getString("entry"))
                        );
                        domainMeta.lore(Collections.singletonList(
                                Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("gui.list.entry"))
                        ));
                        domain.setItemMeta(domainMeta);
                        pageEntryItems.add(domain);
                    }
                }
            }
        }

        if (pageEntryItems.isEmpty() && pageIndex != 0)
            return getPage(pageIndex - 1);

        if (pageEntryItems.size() > 18) {
            /* We limit to 19, so we know if there are more entries; one query more is no significant hit and still adds QoL */
            whitelistInv.setItem(26, nextPage);
            pageEntryItems.removeLast(); // Remove the last entry, which is the one we used to check if there are more entries
        }

        if (pageIndex > 0)
            whitelistInv.setItem(18, previousPage);
        else
            whitelistInv.setItem(18, mainMenu);

        whitelistInv.setItem(22, pageInfo);

        pageEntryItems.forEach(whitelistInv::addItem);

        return whitelistInv;
    }

    /**
     * Replaces the head of the player in the inventory with the given profile
     *
     * @param inventory  The whitelist page inventory
     * @param playerName The name of the player
     * @param profile    The profile of the player provided by the future
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
