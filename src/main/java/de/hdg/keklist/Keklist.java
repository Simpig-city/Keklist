package de.hdg.keklist;

import de.hdg.keklist.commandCompletions.BlacklistCompletor;
import de.hdg.keklist.commandCompletions.WhitelistCompletor;
import de.hdg.keklist.commands.Blacklist;
import de.hdg.keklist.commands.Whitelist;
import de.hdg.keklist.database.DB;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public final class Keklist extends JavaPlugin {

    private static @Getter Keklist instance;
    private static final Random random = new Random();
    private final @Getter MiniMessage miniMessage = MiniMessage.builder().tags(
            TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.font())
                    .resolver(StandardTags.reset())
                    .resolver(StandardTags.rainbow())
                    .resolver(StandardTags.translatable())
                    .build()).build();

    @Override
    public void onLoad() {
       instance = this;


       //save config for custom messages
       this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        DB.connect();

        getServer().getPluginManager().registerEvents(new de.hdg.keklist.events.ListPingEvent(), this);

        getCommand("whitelist").setExecutor(new Whitelist());
        getCommand("blacklist").setExecutor(new Blacklist());

        getCommand("whitelist").setTabCompleter(new WhitelistCompletor());
        getCommand("blacklist").setTabCompleter(new BlacklistCompletor());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @NotNull
    public String getRandomizedMessage(@NotNull RandomType type) {
       switch (type) {
           case BLACKLISTED -> {
               return getConfig().getStringList("blacklisted").get(random.nextInt(getConfig().getStringList("blacklisted").size()));
           }
           case WHITELISTED -> {
               return getConfig().getStringList("whitelisted").get(random.nextInt(getConfig().getStringList("whitelisted").size()));
           }
           case NORMAL -> {
               return getConfig().getStringList("default").get(random.nextInt(getConfig().getStringList("default").size()));
           }
           default -> {
               return "null";
           }
       }
    }


    //Enum for the different messages
    public enum RandomType {
        BLACKLISTED, WHITELISTED, NORMAL
    }

}
