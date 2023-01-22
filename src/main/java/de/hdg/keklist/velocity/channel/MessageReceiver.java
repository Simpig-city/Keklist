package de.hdg.keklist.velocity.channel;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import de.hdg.keklist.velocity.KeklistVelocity;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboSessionHandler;
import net.elytrium.limboapi.api.material.VirtualItem;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

public class MessageReceiver {

    private final ChannelIdentifier identifier;

    public MessageReceiver(ChannelIdentifier identifier) {
        this.identifier = identifier;
    }

    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent event) throws IOException {
        if (event.getIdentifier().equals(identifier)) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());


            if (event.getSource() instanceof ServerConnection) {
                ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
                String message = in.readUTF();


                JsonElement data = JsonParser.parseString(message);

                UUID uuid = UUID.fromString(data.getAsJsonObject().get("uuid").getAsString());
                long unix = data.getAsJsonObject().get("unix").getAsLong();
                String fromServer = data.getAsJsonObject().get("from").getAsString();

                KeklistVelocity.getInstance().getServer().getPlayer(uuid).ifPresentOrElse(player -> {
                    Limbo limbo = KeklistVelocity.getInstance().getLimbo();

                    limbo.spawnPlayer(player, new KeklistSessionHandler());

                    KeklistVelocity.getInstance().getLogger().info("User " + player.getUsername() + " entern the Keklist Limbo from Server " + fromServer + ". Operation took" + (System.currentTimeMillis() - unix) + "ms");
                    }, new Runnable() {
                    @Override
                    public void run() {
                        KeklistVelocity.getInstance().getLogger().error("Failed to send user with UUID: " + uuid.toString() + " to the limbo! User not found.");
                    }
                });

            }
        }
    }


    private class KeklistSessionHandler implements LimboSessionHandler {

        private long joinTime;
        private LimboPlayer player;

        public KeklistSessionHandler() {
        }

        @Override
        public void onSpawn(Limbo server, LimboPlayer player) {
            this.player = player;
            this.joinTime = System.currentTimeMillis();


            player.disableFalling();
            player.flushPackets();
        }

        @Override
        public void onChat(String message) {
            player.sendImage(0, new BufferedImage(1, 1, 1));
            player.setInventory(0, getMap(), 1, 0, CompoundBinaryTag.builder().putInt("map", 0).build());
        }

        @Override
        public void onDisconnect() {
            KeklistVelocity.getInstance().getLogger().info("User " + player.getProxyPlayer().getUsername() + " left the limbo after " + ((System.currentTimeMillis() - joinTime) / 1000) + "secs");
        }

        private VirtualItem getMap() {
            return new VirtualItem() {
                @Override
                public short getID(ProtocolVersion version) {
                    return 23504;
                }
            };
        }
    }
}