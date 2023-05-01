package de.hdg.keklist.api;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.hdg.keklist.Keklist;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * This class is used to listen for plugin messages from other servers and sending responses.
 *
 * @author SageSphinx63920
 * @since 1.0
 */
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

        switch (action){
            case "isBlacklistedUUID" -> {
                String uuid = in.readUTF();
                String server = in.readUTF();

                sendResponse(server, "isBlacklistedUUIDResponse", uuid, api.isBlacklisted(UUID.fromString(uuid)));
            }
            case "isBlacklistedIP" -> {
                String ip = in.readUTF();
                String server = in.readUTF();

                sendResponse(server, "isBlacklistedIPResponse", ip, api.isBlacklisted(ip));
            }
            case "isBlacklistedMOTD" -> {
                String ip = in.readUTF();
                String server = in.readUTF();

                sendResponse(server, "isBlacklistedMOTDResponse", ip, api.isMOTDBlacklisted(ip));
            }
            case "isWhitelistedIP" -> {
                String ip = in.readUTF();
                String server = in.readUTF();

                sendResponse(server, "isWhitelistedIPResponse", ip, api.isWhitelisted(ip));
            }
            case "isWhitelistedUUID" -> {
                String uuid = in.readUTF();
                String server = in.readUTF();

                sendResponse(server, "isWhitelistedUUIDResponse", uuid, api.isWhitelisted(UUID.fromString(uuid)));
            }
        }
    }

    /**
     * Sends a response to the isX request to the proxy
     *
     * @param server Server to relay the response to
     * @param action Request action
     * @param s Request source (IP or UUID)
     * @param b Response
     */
    private void sendResponse(String server, String action, String s, boolean b) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(action);
        out.writeUTF(server);
        out.writeUTF(s);
        out.writeBoolean(b);

        Bukkit.getOnlinePlayers().stream().findFirst().ifPresentOrElse(player -> player.sendPluginMessage(Keklist.getInstance(), "keklist:api", out.toByteArray()), () -> Bukkit.getLogger().warning(Keklist.getTranslations().get("api.proxy-send-fail")));
    }
}
