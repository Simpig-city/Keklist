package de.hdg.keklist.velocity.api;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.hdg.keklist.velocity.KeklistVelocity;

/**
 * This class is responsible for receiving messages from the backend and forwarding them to the correct server.
 *
 * @author SageSphinx63920
 * @since 1.0
 */
public class APIMessageReceiver {

    private final ChannelIdentifier identifier;

    public APIMessageReceiver(ChannelIdentifier identifier) {
        this.identifier = identifier;
    }

    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent event) {
        if (event.getIdentifier().equals(identifier)) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());

            if (event.getSource() instanceof ServerConnection) {
                if(KeklistVelocity.getInstance().getConfig().getOption(true, "enable-api")) {
                    KeklistVelocity.getInstance().getLogger().error(KeklistVelocity.getTranslations().get("velocity.api.disabled"));
                    return;
                }

                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                ByteArrayDataOutput out = ByteStreams.newDataOutput();

                String server = in.readUTF();
                String action = in.readUTF();

                out.writeUTF(action);

                //Only 2 actions that need 2 parameters
                if(action.equals("blacklistUUID") || action.equals("blacklistIP")){
                    String blacklisted = in.readUTF();
                    String reason = in.readUTF();

                    out.writeUTF(blacklisted);
                    out.writeUTF(reason);
                }else{
                    if(action.endsWith("Response")){
                        String requestedAction = in.readUTF().replace("Response", "");
                        String relayServer = in.readUTF();
                        String requested = in.readUTF();
                        boolean result = in.readBoolean();

                        out.writeUTF(requestedAction);
                        out.writeUTF(requested);
                        out.writeBoolean(result);

                        KeklistVelocity.getInstance().getServer().getServer(relayServer).ifPresentOrElse(serverConnection -> {
                            serverConnection.sendPluginMessage(identifier, out.toByteArray());
                        }, () -> {
                            KeklistVelocity.getInstance().getLogger().error(KeklistVelocity.getTranslations().get("velocity.api.response-not-found"), relayServer);
                        });
                        return;
                    }
                    String parameter = in.readUTF();

                    out.writeUTF(parameter);
                }

                KeklistVelocity.getInstance().getServer().getServer(server).ifPresentOrElse(serverConnection -> {
                    serverConnection.sendPluginMessage(identifier, out.toByteArray());
                }, () -> {
                    KeklistVelocity.getInstance().getLogger().error(KeklistVelocity.getTranslations().get("velocity.api.action-server-not-found"), server);
                });
            }
        }
    }
}
