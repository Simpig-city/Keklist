package de.hdg.keklist.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import de.hdg.keklist.velocity.api.APIMessageReceiver;
import de.hdg.keklist.velocity.channel.MessageReceiver;
import de.hdg.keklist.velocity.command.WhereAmICommand;
import de.hdg.keklist.velocity.util.VelocityConfig;
import lombok.Getter;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboapi.api.player.GameMode;
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

    private @Getter LimboFactory limboAPI;
    private @Getter Limbo limbo;

    private final ChannelIdentifier limboChannel = MinecraftChannelIdentifier.from("keklist:data");
    private final ChannelIdentifier apiChannel = MinecraftChannelIdentifier.from("keklist:api");

    private static KeklistVelocity instance;

    @Inject
    public KeklistVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;
        this.config = new VelocityConfig(dataDirectory, "config.yml");

        if (config.getOption(false, "limbo.enabled")) {
            if(server.getPluginManager().isLoaded("limboapi")) {
                limboAPI = (LimboFactory) this.server.getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
                if (!KeklistVelocity.getInstance().dataDirectory.resolve(config.getOption("limbo.nbt", "limbo.file")).toFile().exists()) {
                    logger.error("The schematic file for the Limbo doesn't exist! Could not load " + config.getOption("limbo.nbt", "limbo.file"));
                }
            }else{
                logger.error("LimboAPI not found! Please install LimboAPI to use the Limbo!");
            }
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        if(config.getOption(true, "limbo.enabled")){
            this.limbo = createLimbo();
        }else
            logger.warn("Velocity Limbo not enabled! Kicking player instead if message from Spigot Plugin...");

        //Register Limbo channel
        server.getChannelRegistrar().register(limboChannel);
        server.getEventManager().register(this, new MessageReceiver(limboChannel));

        //Register API channel
        server.getChannelRegistrar().register(apiChannel);
        server.getEventManager().register(this, new APIMessageReceiver(apiChannel));
    }

    public static KeklistVelocity getInstance() {
        return instance;
    }

    private Limbo createLimbo() throws IOException {
        LimboFactory factory = KeklistVelocity.getInstance().getLimboAPI();

        VirtualWorld world = factory.createVirtualWorld(Dimension.OVERWORLD, 0, 0, 0, 0, 0);
        WorldFile file = factory.openWorldFile(BuiltInWorldFileType.STRUCTURE, dataDirectory.resolve(config.getOption("limbo.nbt", "nbt-file")));

        int[] offset = config.getOption(new int[]{0, 0, 0}, "limbo.offset");

        file.toWorld(factory, world, offset[0], Math.max(offset[1], 0), offset[2], 7);

        Limbo setupLimbo = factory.createLimbo(world);

        setupLimbo.setName("Keklist Limbo");
        setupLimbo.setGameMode(GameMode.CREATIVE);
        setupLimbo.setShouldRespawn(true);
        setupLimbo.setWorldTime(1000);
        setupLimbo.setViewDistance(8);
        setupLimbo.setSimulationDistance(1);

        if(config.getOption(true, "limbo.enable-command")) {
            setupLimbo.registerCommand(server.getCommandManager().metaBuilder("whereami").build(), new WhereAmICommand());
        }

        return setupLimbo;
    }
}
