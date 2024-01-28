package de.hdg.keklist.gui.events;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.gui.GuiManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.meta.ItemMeta;

public class MainGUIEvent implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == null) return;
        if(!(event.getWhoClicked() instanceof Player player)) return;

        if(event.getView().title().equals(Keklist.getInstance().getMiniMessage().deserialize("<gold><b>Keklist"))) {
            event.setCancelled(true);

            if(!player.hasPermission("keklist.gui.general"))
                player.closeInventory(InventoryCloseEvent.Reason.CANT_USE);

            ItemMeta meta = event.getCurrentItem().getItemMeta();
            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

            GuiManager.handleMainGUICLick(serializer.serialize(meta.displayName()), player);
        }
    }
}
