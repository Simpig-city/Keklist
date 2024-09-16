package de.hdg.keklist.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Matches;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public final class PluginConfig {

    @Comment("The language of the plugin")
    private String language = "en-us";

    @Comment("Enable the /keklist command to enable/disable the blacklist/whitelist")
    private boolean enableManageCommand = true;

    @Comment("Date format for the blacklist/whitelist GUI")
    private String dateFormat = "dd-MM-yyyy HH:mm";

    @Comment("Notify the player if someone joins the server which is blacklisted/whitelisted")
    private boolean chatNotify = true;


    private GeneralConfig general = new GeneralConfig();

    private IpConfig ip = new IpConfig();

    @Comment("Blacklist settings")
    private BlacklistSettings blacklist = new BlacklistSettings();

    @Comment("Whitelist settings")
    private WhitelistSettings whitelist = new WhitelistSettings();


    @Comment("Enable PlaceholderAPI support")
    private boolean placeholderapi = false;

    @Comment("Enable support for the Plan plugin")
    private boolean planSupport = false;


    @Comment("Note: If you use a proxy you need to enable 'send-floodgate-data' on the proxy's config \n More: https://wiki.geysermc.org/floodgate/setup/")
    private FloodgateConfig floodgate = new FloodgateConfig();

    private DiscordSettings discord = new DiscordSettings();

    @Comment("Settings for the database \nThis is useful to share the blacklist and whitelist between multiple servers")
    private MariaDBSettings mariaDB = new MariaDBSettings();


    @ConfigSerializable
    private static class GeneralConfig {
        @Comment("Keklist will now display the default motd. NOTE: This will override other motd plugins so it's false by default ; Same as for blacklisted/whitelisted motd")
        private boolean enableDefaultMotd = false;

        @Comment("Require the player to add this server to the server list before joining")
        private boolean requireServerListBeforeJoin = false;
    }

    @ConfigSerializable
    private static class IpConfig {
        @Comment("Send a message about a player's ip on join")
        private boolean sendMessageOnJoin = false;

        @Comment("Option to block users using proxies, vpns and tor exit relays")
        private boolean proxyAllowed = true;
    }

    @ConfigSerializable
    private static class BlacklistSettings {
        @Comment("Enable the blacklist feature")
        private boolean enabled = true;

        @Comment("Allows blacklisted player to join if a player with the admin-permission is online")
        private boolean allowJoinWithAdmin = false;

        private String adminPermission = "blacklist.admin";

        @Comment("Change the motd for players (ips) on the (modt-) blacklist if the server is in blacklist mode. NOTE: Change this to false if you want to use the default motd or any other motd plugin")
        private boolean changeMotd = true;

        @Comment("May let the player join if nobody is online but fallback kicked; Needs velocity plugin")
        private boolean limbo = false;

        @Comment("The icon for the server if player is blacklisted. Put the file in the same folder as the config. Use default for the default icon")
        private String iconFile = "default";

        @Comment("List of countries in ISO 3166-1 A2 that are blacklisted. NOTE: Can be empty ; There won't be an option for blocking cities as this data is not inaccurate enough")
        private List<String> countries = new ArrayList<>();

        @Comment("List of continents in 2-digit code that are blacklisted. NOTE: Can be empty")
        private List<String> continents = new ArrayList<>();
    }

    @ConfigSerializable
    private static class WhitelistSettings {
        @Comment("Enable the whitelist feature")
        private boolean enabled = true;

        @Comment("Change the motd for players (ips) on the whitelist if the server is in whitelist mode")
        private boolean changeMotd = false;

        @Comment("This sends fake player's AND fake player counts to the client")
        private boolean hideOnlinePlayers = false;

        @Comment("List of fake players that are shown to the client NOTE: Can be empty")
        private List<String> fakePlayers = List.of("SageSphinx63920", "hdgaymer1404Jonas", "LPTP1");

        @Comment("Please use the format: INTEGER-INTEGER")
        @Matches("^[0-9]+-[0-9]+$")
        private String fakeMaxRange = "20-40";

        @Comment("Please use the format: INTEGER-INTEGER")
        @Matches("^[0-9]+-[0-9]+$")
        private String fakeOnlineRange = "0-10";
    }

    @ConfigSerializable
    private static class FloodgateConfig {
        @Comment("Prefix for the floodgate player. NOTE: Gets automatically set if geyser is installed")
        private String prefix = ".";

        @Comment("https://mcprofile.io api key for getting the floodgate uuid for a bedrock player not being in the server's cache. Please read the wiki for more information")
        private String apiKey = "your-api";
    }

    @ConfigSerializable
    private static class DiscordSettings {
        private boolean enabled = false;

        @Comment("Webhook url for the discord webhook")
        private String webhookUrl = "https://discord.com";

        @Comment("Username for the webhook")
        private String username = "Keklist";

        @Comment("Avatar url for the webhook")
        private String avatarUrl = "https://cdn.discordapp.com/attachments/1056727727991959673/1102655035290157176/keklist.png";

        @Comment("List of events which trigger a message. See wiki for events")
        private List<String> events = List.of("blacklist_add", "blacklist_remove", "blacklist_kick", "whitelist_add", "whitelist_remove", "whitelist_kick", "limbo");

        @Comment("Pings this role on any event. NOTE: Can be empty")
        private List<String> pingRoles = List.of("214809157574983681");
    }

    @ConfigSerializable
    private static class MariaDBSettings {
        private boolean enabled = false;

        private String host = "localhost";

        private String port = "3306";

        private String database = "keklist";

        private String username = "root";

        private String password = "root";

        private String options = "?useSSL=false&serverTimezone=UTC";
    }

    @ConfigSerializable
    private static class MessagesSettings {



        @ConfigSerializable
        private static class MotdMessages {
            private List<String> blacklisted = List.of("<red><bold>Your IP is blacklisted on this server!", "<red><bold>You are not allowed to join this server!");
            private List<String> whitelisted = List.of("<gold><bold>The server is in whitelist mode but you can join.", "<gold><bold>Glad you can join us!");
            private List<String> defaultMotd = List.of("This is a normal motd", "By default these are visible for everyone");
        }
    }
}
