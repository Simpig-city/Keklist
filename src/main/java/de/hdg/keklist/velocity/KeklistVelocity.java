package de.hdg.keklist.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.hdg.keklist.velocity.util.VeloConfigUtil;
import lombok.Getter;
import net.elytrium.limboapi.api.LimboFactory;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "de.hdg.keklist.Keklist", name = "Keklist", version = "1.0.0",
        url = "https://simp.sasp.gq", description = "Block all the Keks and accept the nice people",
        authors = {
                "hdgamer1404Jonas",
                "SageSphinx63920"},
        dependencies = {
                @Dependency(id = "limboapi")
        })
public class KeklistVelocity {

    private final Path dataDirectory;
    private final ProxyServer server;
    private final @Getter Logger logger;
    private @Getter VeloConfigUtil config;
    private static KeklistVelocity instance;
    private @Getter LimboFactory limboAPI;
    private final ChannelIdentifier limboChannel =
            MinecraftChannelIdentifier.from("keklist:data");

    @Inject
    public KeklistVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.instance = this;

        limboAPI = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        //Register channel
        server.getChannelRegistrar().register(limboChannel);

    }

    public static KeklistVelocity getInstance(){
        return instance;
    }

    public void sendExampleMessage(ChannelMessageSink receiver, String message) {
        // Write whatever you want to send to a buffer
        ByteArrayDataOutput buf = ByteStreams.newDataOutput();
        buf.writeUTF(message);
        // Send it
        receiver.sendPluginMessage(limboChannel, buf.toByteArray());
    }
}
