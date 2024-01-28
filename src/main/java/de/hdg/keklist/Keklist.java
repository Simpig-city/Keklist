package de.hdg.keklist;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.hdg.keklist.api.KeklistAPI;
import de.hdg.keklist.api.KeklistChannelListener;
import de.hdg.keklist.commands.BlacklistCommand;
import de.hdg.keklist.commands.KeklistCommand;
import de.hdg.keklist.commands.WhitelistCommand;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.events.BlacklistRemoveMotd;
import de.hdg.keklist.events.ListPingEvent;
import de.hdg.keklist.events.PreLoginKickEvent;
import de.hdg.keklist.events.ServerWhitelistChangeEvent;
import de.hdg.keklist.extentions.PlaceholderAPIExtension;
import de.hdg.keklist.gui.events.MainGUIEvent;
import de.hdg.keklist.gui.events.SettingsEvent;
import de.hdg.keklist.gui.events.blacklist.BlacklistEntryEvent;
import de.hdg.keklist.gui.events.blacklist.BlacklistEvent;
import de.hdg.keklist.gui.events.whitelist.WhitelistEntryEvent;
import de.hdg.keklist.gui.events.whitelist.WhitelistEvent;
import de.hdg.keklist.util.KeklistConfigUtil;
import de.hdg.keklist.util.LanguageUtil;
import de.hdg.keklist.extentions.PlanHook;
import de.hdg.keklist.extentions.WebhookManager;
import de.sage.util.UpdateChecker;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public final class Keklist extends JavaPlugin {

    /* Intern */
    private final int bstatsID = 18279;
    private KeklistMetrics metrics;
    private static final Random random = new Random();
    private KeklistConfigUtil configUtil;
    private UpdateChecker updateChecker;
    private final ScheduledThreadPoolExecutor updateExecutor = new ScheduledThreadPoolExecutor(1);

    /* Extensions */
    private @Getter
    @Nullable FloodgateApi floodgateApi = null;
    private static @Getter PlanHook planHook;
    private PlaceholderAPIExtension placeholders;

    /* Global */
    private static @Getter DB database;
    private static @Getter LanguageUtil translations;
    private static @Getter WebhookManager webhookManager;
    private static @Getter Keklist instance;
    private static KeklistAPI api;
    private final @Getter MiniMessage miniMessage = MiniMessage.builder().tags(
                    TagResolver.builder().resolver(StandardTags.defaults()).build())
            .build();
    private static @Getter boolean debug = false;

    @Override
    public void onLoad() {
        instance = this;
        translations = new LanguageUtil(Objects.requireNonNull(getConfig().getString("language")), this.getDataFolder(), this.getSLF4JLogger());

        //Check for paper
        try {
            Class.forName("io.papermc.paper.plugin.loader.PluginLoader");
        } catch (ClassNotFoundException e) {
            getLogger().severe(translations.get("paper.required"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if(Bukkit.getServer().getMinecraftVersion().equals("1.20")) {
            getLogger().severe(translations.get("paper.version.unsupported"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        //Init utilities after paper checkÏ
        configUtil = new KeklistConfigUtil(this);

        //Check for bedrock support
        if (Bukkit.getPluginManager().getPlugin("floodgate") != null)
            floodgateApi = FloodgateApi.getInstance();

        //Plugin channel for limbo connections
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "keklist:data");

        //save config for custom messages
        this.saveDefaultConfig();

        //update config
        configUtil.updateConfig();

        //set debug mode
        debug = getConfig().getBoolean("debug");

        updateChecker = new UpdateChecker("simpig-city", "Keklist", getPluginMeta().getVersion(), true, getLogger());

        //SQL
        if (getConfig().getBoolean("mariadb.enabled")) {
            database = new DB(DB.DBType.MARIADB, instance);
        } else
            database = new DB(DB.DBType.SQLITE, instance);

        database.connect();

        //Needs to be called after the connection to the database
        api = KeklistAPI.makeApi(this);
    }

    @Override
    public void onEnable() {
        registerCommand(new WhitelistCommand());
        registerCommand(new BlacklistCommand());

        // Manage commands are handled in the command itself
        registerCommand(new KeklistCommand());

        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new ListPingEvent(), this);
        pm.registerEvents(new PreLoginKickEvent(), this);
        pm.registerEvents(new BlacklistRemoveMotd(), this);
        pm.registerEvents(new ServerWhitelistChangeEvent(), this);

        // GUI Listener
        pm.registerEvents(new MainGUIEvent(), this);
        pm.registerEvents(new SettingsEvent(), this);

        pm.registerEvents(new WhitelistEvent(), this);
        pm.registerEvents(new WhitelistEntryEvent(), this);

        pm.registerEvents(new BlacklistEvent(), this);
        pm.registerEvents(new BlacklistEntryEvent(), this);


        // Register plugin channel for API usage
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "keklist:api", new KeklistChannelListener(api));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "keklist:api");

        //Plan Hook
        if (Bukkit.getPluginManager().getPlugin("Plan") != null) {
            if (getConfig().getBoolean("plan-support")) {
                planHook = new PlanHook();
                planHook.hookIntoPlan();
            }
        }

        //Webhook Manager
        if (getConfig().getBoolean("discord.enabled"))
            webhookManager = new WebhookManager(this);

        // BStats Metrics
        if (getConfig().getBoolean("bstats"))
            metrics = new KeklistMetrics(new Metrics(this, bstatsID), this);

        // Placeholder updates
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && getConfig().getBoolean("placeholderapi")) {
            placeholders = new PlaceholderAPIExtension(this);
            placeholders.register();
        }

        // Update checker
        // TODO : Uncomment this when the plugin is *publicly* released
        /*if (getConfig().getBoolean("update.check"))
            updateExecutor.scheduleAtFixedRate(() -> {
                try {
                    updateChecker.check();
                } catch (IOException e) {
                    getLogger().severe("There was an error while checking for updates! Please report this to the developer!");
                    getLogger().severe(e.getMessage());
                }
            }, 0, getConfig().getInt("update.interval"), java.util.concurrent.TimeUnit.HOURS);*/

        updateChecker.setUpdateMessage(translations.get("update.message", getPluginMeta().getVersion()));

    }

    @Override
    public void onDisable() {
        // Shutdown metrics
        if (metrics != null)
            metrics.shutdown();

        // Shutdown placeholders
        if (placeholders != null)
            placeholders.unregister();

        // Disconnect from the database
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

    @NotNull
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

    public void sendUserToLimbo(Player player) {
        sendUserToLimbo(player.getUniqueId());
    }

    public void sendUserToLimbo(UUID uuid) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();

            JsonObject data = new JsonObject();
            data.add("uuid", new JsonPrimitive(uuid.toString()));
            data.add("unix", new JsonPrimitive(System.currentTimeMillis()));
            data.add("from", new JsonPrimitive(getServer().getName()));

            out.writeUTF(data.toString());

            Iterables.getFirst(Bukkit.getOnlinePlayers(), null).sendPluginMessage(this, "keklist:data", out.toByteArray());

            if (Keklist.getWebhookManager() != null)
                Keklist.getWebhookManager().fireEvent(WebhookManager.EVENT_TYPE.LIMBO, uuid.toString(), System.currentTimeMillis());
        } catch (NullPointerException | IllegalArgumentException ex) {
            getLogger().warning(translations.get("limbo.error"));
        }
    }

    private void registerCommand(Command command) {
        getServer().getCommandMap().register("keklist", command);
    }

    /**
     * Enum for the different messages
     */
    public enum RandomType {
        BLACKLISTED, WHITELISTED, NORMAL
    }

    /**
     * Returns the API of the plugin
     *
     * @return The API object
     * @throws IllegalStateException if the database is not connected, **really unlikely**
     */
    public static KeklistAPI getApi() {
        if (database.isConnected()) {
            return api;
        } else
            throw new IllegalStateException(translations.get("api.database-not-connected"));
    }
}
