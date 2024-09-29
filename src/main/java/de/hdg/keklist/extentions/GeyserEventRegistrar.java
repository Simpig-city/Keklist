package de.hdg.keklist.extentions;

import de.hdg.keklist.Keklist;
import de.hdg.keklist.events.feats.GeyserConnectionEvent;
import lombok.SneakyThrows;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.connection.ConnectionRequestEvent;

public class GeyserEventRegistrar implements EventRegistrar {

    private final GeyserApi geyserApi;
    private final Keklist keklist;

    public GeyserEventRegistrar(GeyserApi geyserApi, Keklist keklist) {
        this.geyserApi = geyserApi;
        this.keklist = keklist;
    }

    @SneakyThrows
    public void registerEvents() {
        geyserApi.eventBus().subscribe(this, ConnectionRequestEvent.class, GeyserConnectionEvent::onConnectionRequestEvent/*, PostOrder.FIRST*/); // Commented out due to a bug in Geyser
        keklist.getLogger().info(Keklist.getTranslations().get("geyser.events.registered"));
    }
}
