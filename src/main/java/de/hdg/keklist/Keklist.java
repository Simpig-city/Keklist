package de.hdg.keklist;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.hdg.keklist.api.KeklistAPI;
import de.hdg.keklist.api.KeklistChannelListener;
import de.hdg.keklist.database.DB;
import de.hdg.keklist.events.PreLoginKickEvent;
import de.hdg.keklist.events.command.ListCommandPageEvent;
import de.hdg.keklist.events.feats.ListPingEvent;
import de.hdg.keklist.events.feats.NotifyJoinEvent;
import de.hdg.keklist.events.qol.ServerWhitelistChangeEvent;
import de.hdg.keklist.events.command.NameChangeCommandEvent;
import de.hdg.keklist.events.mfa.MFAEvent;
import de.hdg.keklist.events.mfa.CommandEvent;
import de.hdg.keklist.extentions.GeyserEventRegistrar;
import de.hdg.keklist.extentions.PlaceholderAPIExtension;
import de.hdg.keklist.extentions.context.BlacklistedCalculator;
import de.hdg.keklist.extentions.context.WhitelistedCalculator;
import de.hdg.keklist.gui.pages.MainPageEvent;
import de.hdg.keklist.gui.pages.SettingsPageEvent;
import de.hdg.keklist.gui.pages.blacklist.BlacklistEntryPageEvent;
import de.hdg.keklist.gui.pages.blacklist.BlacklistPageEvent;
import de.hdg.keklist.gui.pages.whitelist.WhitelistEntryPageEvent;
import de.hdg.keklist.gui.pages.whitelist.WhitelistPageEvent;
import de.hdg.keklist.util.KeklistConfigUtil;
import de.hdg.keklist.util.LanguageUtil;
import de.hdg.keklist.extentions.PlanHook;
import de.hdg.keklist.extentions.WebhookManager;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.model.user.User;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Keklist extends JavaPlugin {

    /* Intern */
    private final int bstatsID = 18279;
    private KeklistMetrics metrics;
    private static final Random random = new Random();
    private KeklistConfigUtil configUtil;
    //private UpdateChecker updateChecker;
    //private final ScheduledThreadPoolExecutor updateExecutor = new ScheduledThreadPoolExecutor(1); // TODO : Uncomment this when the plugin is *publicly* released

    /* Extensions */
    private @Getter
    @Nullable FloodgateApi floodgateApi = null;
    private static @Getter PlanHook planHook;
    private PlaceholderAPIExtension placeholders;
    private @Getter LuckPerms luckPermsAPI;
    private final List<ContextCalculator<Player>> registeredCalculators = new ArrayList<>();
    private GeyserApi geyserApi;

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
        translations = new LanguageUtil(getConfig().getString("language", "en-us"), this.getDataFolder(), this.getSLF4JLogger());

        //Check for paper
        try {
            Class.forName("io.papermc.paper.plugin.loader.PluginLoader");
        } catch (ClassNotFoundException e) {
            getLogger().severe(translations.get("paper.required"));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getServer().getMinecraftVersion().contains("1.20")) {
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

        if (Bukkit.getPluginManager().getPlugin("BKCommonLib") == null && getConfig().getBoolean("2fa.enabled")) {
            getLogger().warning(translations.get("2fa.bkcommonlib"));
            getConfig().set("2fa.enabled", false);
        }

        //updateChecker = new UpdateChecker("simpig-city", "Keklist", getPluginMeta().getVersion(), true, getLogger());

        //SQL
        switch (DB.DBType.valueOf(getConfig().getString("database.type", "H2"))) {
            case H2 -> database = new DB(DB.DBType.H2, instance);
            case SQLITE -> database = new DB(DB.DBType.SQLITE, instance);
            case MARIADB -> database = new DB(DB.DBType.MARIADB, instance);
            default -> {
                getLogger().severe(translations.get("database.error", getConfig().getString("database.type")));
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }

        database.connect();

        //Needs to be called after the connection to the database
        api = KeklistAPI.makeApi(this);
    }

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();

        // command
        pm.registerEvents(new NameChangeCommandEvent(), this);
        pm.registerEvents(new ListCommandPageEvent(), this);

        // features
        pm.registerEvents(new ListPingEvent(), this);
        pm.registerEvents(new NotifyJoinEvent(), this);
        pm.registerEvents(new PreLoginKickEvent(), this);

        // MFA
        pm.registerEvents(new CommandEvent(), this);
        pm.registerEvents(new MFAEvent(), this);

        // QoL
        pm.registerEvents(new ServerWhitelistChangeEvent(), this);

        // GUI Listener
        pm.registerEvents(new MainPageEvent(), this);
        pm.registerEvents(new SettingsPageEvent(), this);

        pm.registerEvents(new WhitelistPageEvent(), this);
        pm.registerEvents(new WhitelistEntryPageEvent(), this);

        pm.registerEvents(new BlacklistPageEvent(), this);
        pm.registerEvents(new BlacklistEntryPageEvent(), this);


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

        // LuckPerms contexts (no config option, always enabled)
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                luckPermsAPI = provider.getProvider();
                registeredCalculators.addAll(List.of(new WhitelistedCalculator(), new BlacklistedCalculator()));
                registeredCalculators.forEach(luckPermsAPI.getContextManager()::registerCalculator);

                luckPermsAPI.getEventBus().subscribe(getInstance(), NodeAddEvent.class, event -> {
                    if (event.isUser())
                        if (Bukkit.getPlayer(((User) event.getTarget()).getUniqueId()) != null)
                            Bukkit.getPlayer(((User) event.getTarget()).getUniqueId()).updateCommands();
                });
            }
        }

        // Geyser hook
        if (Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null) {
            geyserApi = GeyserApi.api();

            // We will not set the prefix if geyser is not enabled as the proxy could provide a different prefix than the floodgate api on sub server does
            if (geyserApi.usernamePrefix() != null) {
                getConfig().set("floodgate.prefix", geyserApi.usernamePrefix());
                getLogger().info(translations.get("geyser.prefix", geyserApi.usernamePrefix()));
            }

            GeyserEventRegistrar eventRegistrar = new GeyserEventRegistrar(geyserApi, this);
            eventRegistrar.registerEvents();
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

        //updateChecker.setUpdateMessage(translations.get("update.message", getPluginMeta().getVersion()));

    }

    @Override
    public void onDisable() {
        // Shutdown metrics
        if (metrics != null)
            metrics.shutdown();

        // Disable context calculators
        if (luckPermsAPI != null) {
            registeredCalculators.forEach(luckPermsAPI.getContextManager()::unregisterCalculator);
            registeredCalculators.clear();
        }

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
        return switch (type) {
            case BLACKLISTED ->
                    getConfig().getStringList("messages.kick.blacklisted").get(random.nextInt(getConfig().getStringList("messages.kick.blacklisted").size()));

            case WHITELISTED ->
                    getConfig().getStringList("messages.kick.whitelisted").get(random.nextInt(getConfig().getStringList("messages.kick.whitelisted").size()));

            case CONTINENT ->
                    getConfig().getStringList("messages.kick.blacklist.continent").get(random.nextInt(getConfig().getStringList("messages.kick.blacklist.continent").size()));

            case COUNTRY ->
                    getConfig().getStringList("messages.kick.blacklist.country").get(random.nextInt(getConfig().getStringList("messages.kick.blacklist.country").size()));

            case PROXY ->
                    getConfig().getStringList("messages.kick.proxy").get(random.nextInt(getConfig().getStringList("messages.kick.proxy").size()));

            default -> "null";

        };
    }

    public void sendUserToLimbo(@NotNull Player player) {
        sendUserToLimbo(player.getUniqueId());
    }

    public void sendUserToLimbo(@NotNull UUID uuid) {
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

    /**
     * Enum for the different messages
     */
    public enum RandomType {
        BLACKLISTED, WHITELISTED, CONTINENT, COUNTRY, PROXY, NORMAL
    }

    /**
     * Returns the API of the plugin
     *
     * @return The API object
     * @throws IllegalStateException if the database is not connected, **really unlikely**
     */
    @NotNull
    public static KeklistAPI getApi() {
        if (database.isConnected()) {
            return api;
        } else
            throw new IllegalStateException(translations.get("api.database-not-connected"));
    }
}
