package de.hdg.keklist;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.hdg.keklist.commandCompletions.BlacklistCompletor;
import de.hdg.keklist.commandCompletions.WhitelistCompletor;
import de.hdg.keklist.commands.Blacklist;
import de.hdg.keklist.commands.Whitelist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.events.BlacklistRemoveMotd;
import de.hdg.keklist.events.ListPingEvent;
import de.hdg.keklist.events.PreLoginKickEvent;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;

public final class Keklist extends JavaPlugin  {

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

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "keklist");

        //save config for custom messages
        this.saveDefaultConfig();
        DB.connect();
    }

    @Override
    public void onEnable() {
        DB.connect();

        getServer().getPluginManager().registerEvents(new de.hdg.keklist.events.ListPingEvent(), this);

        getCommand("whitelist").setExecutor(new Whitelist());
        getCommand("blacklist").setExecutor(new Blacklist());

        getCommand("whitelist").setTabCompleter(new WhitelistCompletor());
        getCommand("blacklist").setTabCompleter(new BlacklistCompletor());

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ListPingEvent(), this);
        pm.registerEvents(new PreLoginKickEvent(), this);
        pm.registerEvents(new BlacklistRemoveMotd(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DB.disconnect();
    }

    @NotNull
    public String getRandomizedMotd(@NotNull RandomType type) {
        switch (type) {
            case BLACKLISTED -> {
                return getConfig().getStringList("messages.motd.blacklisted").get(random.nextInt(getConfig().getStringList("messages.motd.blacklisted").size()));
            }
            case WHITELISTED -> {
                return getConfig().getStringList("messages.motd.whitelisted").get(random.nextInt(getConfig().getStringList("messages.motd.whitelisted").size()));
            }
            case NORMAL -> {
                return getConfig().getStringList("messages.motd.default").get(random.nextInt(getConfig().getStringList("messages.motd.default").size()));
            }
            default -> {
                return "null";
            }
        }
    }

    public String getRandomizedKickMessage(@NotNull RandomType type) {
        switch (type) {
            case BLACKLISTED -> {
                System.out.println(getConfig().getStringList("messages.kick.blacklisted"));
                System.out.println(getConfig().getStringList("messages.kick.blacklisted").get(random.nextInt(getConfig().getStringList("messages.kick.blacklisted").size())));
                return getConfig().getStringList("messages.kick.blacklisted").get(random.nextInt(getConfig().getStringList("messages.kick.blacklisted").size()));
            }
            case WHITELISTED -> {
                return getConfig().getStringList("messages.kick.whitelisted").get(random.nextInt(getConfig().getStringList("messages.kick.whitelisted").size()));
            }
            default -> {
                return "null";
            }
        }
    }

    public void sendUserToLimbo(Player player){
       sendUserToLimbo(player.getUniqueId());
    }

    public void sendUserToLimbo(UUID uuid){
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("data");

        JsonObject data = new JsonObject();
        data.add("uuid", new JsonPrimitive(uuid.toString()));
        data.add("unix", new JsonPrimitive(System.currentTimeMillis()));
        data.add("from", new JsonPrimitive("Keklist"));

        out.writeUTF(data.getAsString());

        Iterables.getFirst(Bukkit.getOnlinePlayers(), null).sendPluginMessage(this, "keklist", out.toByteArray());
    }

    //Enum for the different messages
    public enum RandomType {
        BLACKLISTED, WHITELISTED, NORMAL
    }

}
