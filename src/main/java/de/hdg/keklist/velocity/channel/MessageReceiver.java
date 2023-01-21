package de.hdg.keklist.velocity.channel;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.hdg.keklist.velocity.KeklistVelocity;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.SchematicFile;
import net.elytrium.limboapi.api.file.WorldFile;

public class MessageReceiver {

    private final ChannelIdentifier identifier;

    public MessageReceiver(ChannelIdentifier identifier) {
        this.identifier = identifier;
    }

    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent event) {
        if (event.getIdentifier().equals(identifier)) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());


            if (event.getSource() instanceof Player) {
                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                String message = in.readUTF();

                VirtualWorld world = KeklistVelocity.getInstance().getLimboAPI().createVirtualWorld(Dimension.OVERWORLD, 0, 0,0, 0, 0);

            }
        }
    }
}