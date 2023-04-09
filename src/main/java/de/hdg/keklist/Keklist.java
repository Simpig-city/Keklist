package de.hdg.keklist;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.hdg.keklist.api.KeklistAPI;
import de.hdg.keklist.api.KeklistChannelListener;
import de.hdg.keklist.commands.Blacklist;
import de.hdg.keklist.commands.Whitelist;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.events.BlacklistRemoveMotd;
import de.hdg.keklist.events.ListPingEvent;
import de.hdg.keklist.events.PreLoginKickEvent;
import de.hdg.keklist.util.PlanHook;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.UUID;

public final class Keklist extends JavaPlugin {

    private static KeklistAPI api;
    private static @Getter Keklist instance;
    private @Getter @Nullable FloodgateApi floodgateApi = null;
    private static @Getter DB database;
    private static @Getter PlanHook planHook;
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

        //Check for paper
        try {
            Class.forName("io.papermc.paper.plugin.loader.PluginLoader");
        } catch (ClassNotFoundException e) {
            getLogger().severe("This plugin requires Paper to run!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if(Bukkit.getPluginManager().getPlugin("floodgate") != null){
            floodgateApi = FloodgateApi.getInstance();
        }


        //Plugin channel for limbo connections
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "keklist:data");

        //save config for custom messages
        this.saveDefaultConfig();

        //SQL
        if(getConfig().getBoolean("mariadb.enabled")){
            database = new DB(DB.DBType.MARIADB, instance);
        }else
            database = new DB(DB.DBType.SQLITE, instance);

        database.connect();

        //Needs to be called after database
        api = KeklistAPI.makeApi(this);
    }

    @Override
    public void onEnable() {
        registerCommand(new Whitelist());
        registerCommand(new Blacklist());

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ListPingEvent(), this);
        pm.registerEvents(new PreLoginKickEvent(), this);
        pm.registerEvents(new BlacklistRemoveMotd(), this);

        // Register plugin channel for API usage
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "keklist:api", new KeklistChannelListener(api));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "keklist:api");

        //Plan Hook
        if(Bukkit.getPluginManager().getPlugin("Plan") != null){
            if(getConfig().getBoolean("plan-support")) {
                planHook = new PlanHook();
                planHook.hookIntoPlan();
            }
        }
    }

    @Override
    public void onDisable() {
       // Disconnect from database
        database.disconnect();

        // Unregister plugin channel
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
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
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            JsonObject data = new JsonObject();
            data.add("uuid", new JsonPrimitive(uuid.toString()));
            data.add("unix", new JsonPrimitive(System.currentTimeMillis()));
            data.add("from", new JsonPrimitive(getServer().getName()));

            out.writeUTF(data.toString());

            Iterables.getFirst(Bukkit.getOnlinePlayers(), null).sendPluginMessage(this, "keklist:data", out.toByteArray());
        }catch (NullPointerException | IllegalArgumentException ex){
            getLogger().warning("No Player online to limbo player! Waiting for next event...");
        }
    }

    /**
     * Enum for the different messages
     */
    public enum RandomType {
        BLACKLISTED, WHITELISTED, NORMAL
    }

    private void registerCommand(Command command) {
        getServer().getCommandMap().register("keklist", command);
    }

    /**
     * Returns the API of the plugin
     *
     * @return The API object
     * @throws IllegalStateException if the database is not connected, **really unlikely**
     */
    public static KeklistAPI getApi(){
        if(database.isConnected()){
            return api;
        }else
            throw new IllegalStateException("Database is not connected!");
    }
}
