package de.hdg.keklist.api;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class KeklistChannelListener implements PluginMessageListener {

    private final KeklistAPI api;

    public KeklistChannelListener(@NotNull KeklistAPI api) {
        this.api = api;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if(!channel.equals("keklist")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();

        if(!subchannel.equals("api")) return;

        String action = in.readUTF();
        switch (action) {
            case "blacklistUUID" -> {
                String uuid = in.readUTF();
                String reason = in.readUTF();

                api.blacklist(UUID.fromString(uuid), null, reason);
            }
            case "blacklistIP" -> {
                String ip = in.readUTF();
                String reason = in.readUTF();

                api.blacklist(ip, reason);
            }
            case "blacklistMOTD" -> {
                String ip = in.readUTF();

                api.blacklistMOTD(ip);
            }
        }

        switch (action) {
            case "whilistIP" -> {
                String ip = in.readUTF();

                api.whitelist(ip);
            }
            case "whitelistUUID" -> {
                String uuid = in.readUTF();

                api.whitelist(UUID.fromString(uuid), null);
            }
        }
    }
}
