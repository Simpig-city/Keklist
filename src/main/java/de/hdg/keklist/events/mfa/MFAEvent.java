package de.hdg.keklist.events.mfa;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.mfa.MFAUtil;
import de.tomino.AuthSys;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MFAEvent implements Listener {

    private final static List<Player> lockedPlayers = new ArrayList<>();

    @EventHandler
    public void onCode(@NotNull AsyncChatEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(event.getPlayer())) {
            event.setCancelled(true);

            MFAUtil.MFAPendingData data = MFAUtil.getPendingApproval().remove(event.getPlayer());
            event.getPlayer().getInventory().setItemInOffHand(data.offhand());

            if (AuthSys.validateCode(data.secret(), PlainTextComponentSerializer.plainText().serialize(event.message()))) {
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.setup-success")));

                String[] recoveryCodes = MFAUtil.generateRecoveryCodes();
                StringBuilder builder = new StringBuilder();

                for (String code : recoveryCodes) {
                    if ((builder.length() / 2) != 1) {
                        builder.append(code).append(" | ");
                    } else
                        builder.append(code).append("\n");
                }

                Keklist.getDatabase().onUpdate("INSERT INTO mfa(uuid, secret, recoveryCodes) VALUES (?, ?, ?)", event.getPlayer().getUniqueId().toString(), data.secret(), Arrays.toString(recoveryCodes));
                MFAUtil.setVerified(event.getPlayer(), true);

                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.codes", builder.toString())));

            } else {
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.setup-fail")));
            }
        } else if (lockedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        if(!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED))
           return;

        if (Keklist.getInstance().getConfig().getBoolean("2fa.enabled")) {
            if (Keklist.getInstance().getConfig().getBoolean("2fa.2fa-on-join")
                    && (Keklist.getInstance().getConfig().getBoolean("2fa.enforce-settings") || MFAUtil.hasMFAEnabled(event.getPlayer()))) {
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.join")));
                lockPlayer(event.getPlayer()); // Gets unlocked by MFAUtil#setVerified which gets called in the Keklist command
            }
        }
    }


    /**
     * Locks the player from doing anything
     *
     * @param player the player to lock
     */
    public static void lockPlayer(@NotNull Player player) {
        if (!lockedPlayers.contains(player))
            lockedPlayers.add(player);
    }

    /**
     * Unlocks the player
     *
     * @param player the player to unlock
     */
    public static void unlockPlayer(@NotNull Player player) {
        lockedPlayers.remove(player);
    }

    /* Player is not allow to do anything during setup */

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (lockedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOffhand(@NotNull PlayerDropItemEvent event) {
        if (lockedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void onInvMove(@NotNull InventoryClickEvent event) {
        if (lockedPlayers.contains((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInvDrag(@NotNull InventoryDragEvent event) {
        if (lockedPlayers.contains((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapItem(@NotNull PlayerSwapHandItemsEvent event) {
        if (lockedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCreativeInv(@NotNull InventoryCreativeEvent event) {
        if (lockedPlayers.contains((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (lockedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (lockedPlayers.contains(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (lockedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (lockedPlayers.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        MFAUtil.MFAPendingData data = MFAUtil.getPendingApproval().remove(event.getPlayer());

        if (data != null)
            event.getPlayer().getInventory().setItemInOffHand(data.offhand());

        MFAUtil.clearPlayerFromLists(event.getPlayer());
        unlockPlayer(event.getPlayer());
    }
}
