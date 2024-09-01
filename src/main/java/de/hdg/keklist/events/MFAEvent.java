package de.hdg.keklist.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.util.mfa.MFAUtil;
import de.tomino.AuthSys;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class MFAEvent implements Listener {

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

                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.codes", builder.toString())));

            } else {
                event.getPlayer().sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.setup-fail")));
            }
        }
    }


    /* Player is not allow to do anything during setup */

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onOffhand(@NotNull PlayerDropItemEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler()
    public void onInvMove(InventoryClickEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(((Player) event.getWhoClicked()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(((Player) event.getWhoClicked()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapItem(PlayerSwapHandItemsEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (MFAUtil.getPendingApproval().containsKey((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (MFAUtil.getPendingApproval().containsKey(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        MFAUtil.MFAPendingData data = MFAUtil.getPendingApproval().remove(event.getPlayer());

        if (data != null)
            event.getPlayer().getInventory().setItemInOffHand(data.offhand());
    }
}
