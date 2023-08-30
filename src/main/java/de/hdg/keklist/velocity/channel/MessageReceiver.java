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
import net.elytrium.limboapi.api.material.WorldVersion;
import net.elytrium.limboapi.api.player.LimboPlayer;
import net.elytrium.limboapi.api.protocol.packets.data.AbilityFlags;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.UUID;

public class MessageReceiver {

    private final ChannelIdentifier identifier;

    public MessageReceiver(ChannelIdentifier identifier) {
        this.identifier = identifier;
    }

    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent event) {
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
                    if (KeklistVelocity.getInstance().getConfig().getOption(false, "limbo.enabled")
                            && KeklistVelocity.getInstance().getLimboUtil().getLimbo() != null) {
                        Limbo limbo = KeklistVelocity.getInstance().getLimboUtil().getLimbo();

                        limbo.spawnPlayer(player, new KeklistSessionHandler());

                        KeklistVelocity.getInstance().getLogger().info(KeklistVelocity.getTranslations().get("limbo.sent", player.getUsername(), fromServer, (System.currentTimeMillis() - unix) / 1000));
                    } else {
                        player.disconnect(Component.text(KeklistVelocity.getTranslations().get("limbo.disable-kick")));
                    }
                }, () -> KeklistVelocity.getInstance().getLogger().error(KeklistVelocity.getTranslations().get("limbo.user-not-found", uuid)));
            }
        }
    }


    private static class KeklistSessionHandler implements LimboSessionHandler {
        private long joinTime;
        private LimboPlayer player;

        public KeklistSessionHandler() {
        }

        @Override
        public void onSpawn(Limbo server, LimboPlayer player) {
            this.player = player;
            this.joinTime = System.currentTimeMillis();

            player.sendAbilities((byte) (AbilityFlags.FLYING | AbilityFlags.ALLOW_FLYING), 0.05F, 0.1F);

            if (KeklistVelocity.getInstance().getConfig().getOption(false, "limbo.enable-map")) {
                if (KeklistVelocity.getInstance().getDataDirectory().resolve(KeklistVelocity.getInstance().getConfig().getOption("map.jpg", "limbo.map-image")).toFile().exists()) {
                    try {
                        player.sendImage(0, ImageIO.read(KeklistVelocity.getInstance().getDataDirectory().resolve(KeklistVelocity.getInstance().getConfig().getOption("map.jpg", "limbo.map-image")).toFile()));
                        player.setInventory(4, getMap(), 1, 0, CompoundBinaryTag.builder().putInt("map", 0).build());
                    } catch (IOException e) {
                        KeklistVelocity.getInstance().getLogger().error(KeklistVelocity.getTranslations().get("limbo.map.error"));
                    }
                } else
                    KeklistVelocity.getInstance().getLogger().warn(KeklistVelocity.getTranslations().get("limbo.map.not-found"));
            }
        }

        @Override
        public void onDisconnect() {
            KeklistVelocity.getInstance().getLogger().info(KeklistVelocity.getTranslations().get("limbo.user-left", player.getProxyPlayer().getUsername(), (System.currentTimeMillis() - joinTime) / 1000));
        }

        private VirtualItem getMap() {
            return new VirtualItem() {
                @Override
                public short getID(ProtocolVersion version) {
                    return 23504;
                }

                @Override
                public short getID(WorldVersion version) {
                    return 23504;
                }

                @Override
                public boolean isSupportedOn(ProtocolVersion version) {
                    return version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0;
                }

                @Override
                public boolean isSupportedOn(WorldVersion version) {
                    return version.compareTo(WorldVersion.MINECRAFT_1_13) >= 0;
                }

                @Override
                public String getModernID() {
                    return null;
                }
            };
        }
    }
}