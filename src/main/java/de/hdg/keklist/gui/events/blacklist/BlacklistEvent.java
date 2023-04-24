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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class BlacklistEvent implements Listener {

    private final HashMap<Player, Block> signMap = new HashMap<>();

    @EventHandler
    public void onBlacklistClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Blacklist"))) {
            event.setCancelled(true);

            switch (event.getCurrentItem().getType()) {
                case DARK_OAK_SIGN -> {
                    Location location = player.getLocation();
                    location.setY(location.getWorld().getMaxHeight() - 1);

                    signMap.put(player, location.getBlock());
                    player.getWorld().setBlockData(location, Material.DARK_OAK_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(location);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "blacklistMode"), PersistentDataType.STRING, "add");
                    sign.line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.blacklist.sign.line")));
                    sign.line(1, Component.empty());
                    sign.update();

                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> player.openSign(sign), 5);
                }
                case DARK_PRISMARINE -> {
                    Location location = player.getLocation();
                    location.setY(location.getWorld().getMaxHeight() - 1);

                    signMap.put(player, location.getBlock());
                    player.getWorld().setBlockData(location, Material.DARK_OAK_SIGN.createBlockData());

                    Sign sign = (Sign) player.getWorld().getBlockState(location);
                    sign.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "blacklistMode"), PersistentDataType.STRING, "remove");
                    sign.line(0, Keklist.getInstance().getMiniMessage().deserialize(Keklist.getLanguage().get("gui.blacklist.sign.line")));
                    sign.line(1, Component.empty());
                    sign.update();

                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> player.openSign(sign), 5);

                }
                case PLAYER_HEAD -> {}
                case ARROW -> GuiManager.openMainGUI(player);
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
                        if(event.lines().get(2) == null){
                            Bukkit.dispatchCommand(event.getPlayer(), "blacklist add " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                        }else if(event.lines().get(2).equals(Component.text("motd"))){
                            Bukkit.dispatchCommand(event.getPlayer(), "blacklist motd " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                        }
                    }
                    case "remove" ->
                            Bukkit.dispatchCommand(event.getPlayer(), "blacklist remove " + PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)));
                }

                Block block = signMap.get(event.getPlayer());
                block.getWorld().setBlockData(block.getLocation(), block.getBlockData());
                signMap.remove(event.getPlayer());

                GuiManager.handleMainGUICLick("blacklist", event.getPlayer());
            }
        }
    }
}
