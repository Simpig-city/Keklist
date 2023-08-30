package de.hdg.keklist.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.hdg.keklist.util.LanguageUtil;
import de.hdg.keklist.velocity.api.APIMessageReceiver;
import de.hdg.keklist.velocity.channel.MessageReceiver;
import de.hdg.keklist.velocity.util.LimboUtil;
import de.hdg.keklist.velocity.util.VelocityConfig;
import lombok.Getter;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(id = "keklist", name = "Keklist", version = "1.0.0",
        url = "https://simp.sasp.gq", description = "Block all the Keks and accept the nice people",
        authors = {
                "hdgamer1404Jonas",
                "SageSphinx63920"},
        dependencies = {
                @Dependency(id = "limboapi", optional = true)
        })
public class KeklistVelocity {

    private final @Getter Path dataDirectory;
    private final @Getter ProxyServer server;
    private final @Getter Logger logger;
    private final @Getter VelocityConfig config;
    private static @Getter LanguageUtil translations;
    private @Getter LimboUtil limboUtil;

    private final ChannelIdentifier limboChannel = MinecraftChannelIdentifier.from("keklist:data");
    private final ChannelIdentifier apiChannel = MinecraftChannelIdentifier.from("keklist:api");

    private static @Getter KeklistVelocity instance;

    @Inject
    public KeklistVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        instance = this;

        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = new VelocityConfig(dataDirectory, "config.yml");
        translations = new LanguageUtil(config.getOption("en-us", "language"), dataDirectory.toFile(), logger);

        //config.updateToNewVersion(); // may be used in future

       if (config.getOption(false, "limbo.enabled")) {
            if(server.getPluginManager().isLoaded("limboapi")) {
                limboUtil = new LimboUtil(this, config);

                limboUtil.onLoad();
            }else{
                logger.error(translations.get("limbo.no-api"));
            }
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        if(config.getOption(true, "limbo.enabled") && server.getPluginManager().isLoaded("limboapi")) {
            limboUtil.onEnable();

            //Register Limbo channel
            server.getChannelRegistrar().register(limboChannel);
            server.getEventManager().register(this, new MessageReceiver(limboChannel));
        }else
            logger.warn(translations.get("limbo.proxy-disabled"));

        //Register API channel
        server.getChannelRegistrar().register(apiChannel);
        server.getEventManager().register(this, new APIMessageReceiver(apiChannel));
    }
}
