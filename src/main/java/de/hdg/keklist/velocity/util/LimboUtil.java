package de.hdg.keklist.velocity.util;

import com.velocitypowered.api.plugin.PluginContainer;
import de.hdg.keklist.velocity.KeklistVelocity;
import de.hdg.keklist.velocity.command.WhereAmICommand;
import lombok.Getter;
import net.elytrium.limboapi.api.Limbo;
import net.elytrium.limboapi.api.LimboFactory;
import net.elytrium.limboapi.api.chunk.Dimension;
import net.elytrium.limboapi.api.chunk.VirtualWorld;
import net.elytrium.limboapi.api.file.BuiltInWorldFileType;
import net.elytrium.limboapi.api.file.WorldFile;
import net.elytrium.limboapi.api.player.GameMode;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;

public class LimboUtil {

    private final KeklistVelocity plugin;
    private final VelocityConfig config;
    private final Logger logger;

    private LimboFactory limboAPI;
    private @Getter @Nullable Limbo limbo;

    public LimboUtil(KeklistVelocity plugin, VelocityConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    public void onLoad() {
        limboAPI = (LimboFactory) plugin.getServer().getPluginManager().getPlugin("limboapi").flatMap(PluginContainer::getInstance).orElseThrow();
        if (!KeklistVelocity.getInstance().getDataDirectory().resolve(config.getOption("limbo.nbt", "limbo.file")).toFile().exists()) {
            logger.error(KeklistVelocity.getTranslations().get("limbo.no-schematic", config.getOption("limbo.nbt", "limbo.file")));
        }
    }

    public void onEnable() throws IOException {
        this.limbo = createLimbo();
    }

    private Limbo createLimbo() throws IOException {
        LimboFactory factory = limboAPI;

        VirtualWorld world = factory.createVirtualWorld(Dimension.OVERWORLD, 0, 0, 0, 0, 0);
        WorldFile file = factory.openWorldFile(BuiltInWorldFileType.STRUCTURE, plugin.getDataDirectory().resolve(config.getOption("limbo.nbt", "nbt-file")));

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
            setupLimbo.registerCommand(plugin.getServer().getCommandManager().metaBuilder("whereami").build(), new WhereAmICommand());
        }

        return setupLimbo;
    }

}
