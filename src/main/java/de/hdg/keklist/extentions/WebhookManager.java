package de.hdg.keklist.extentions;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import de.hdg.keklist.Keklist;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WebhookManager {

    private final Keklist keklist;
    private WebhookClient client;
    private WebhookEmbedBuilder embedBuilder;
    private static final List<EVENT_TYPE> triggerEvents = new ArrayList<>();

    public WebhookManager(Keklist keklist) {
        this.keklist = keklist;
        init();
    }

    private void init() {
        if(keklist.getConfig().getBoolean("discord.enabled")){
            String url = keklist.getConfig().getString("discord.webhook-url");

            WebhookClientBuilder builder = new WebhookClientBuilder(url);
            builder.setThreadFactory((job) -> {
                Thread thread = new Thread(job);
                thread.setName("keklist-discord-webhook-thread");
                thread.setDaemon(true);
                return thread;
            });
            builder.setWait(true);
            builder.setAllowedMentions(AllowedMentions.all());

            client = builder.build();

            embedBuilder = new WebhookEmbedBuilder();
            embedBuilder.setFooter(new WebhookEmbed.EmbedFooter("Keklist v" + keklist.getPluginMeta().getVersion(), null));

            keklist.getConfig().getStringList("discord.events").stream().map(EVENT_TYPE::getByConfigValue).forEach(triggerEvents::add);
        }
    }

    private void sendWebhookMessage(@NotNull WebhookEmbed message) {
        if(client == null) init();
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.addEmbeds(message);
        builder.setAvatarUrl(keklist.getConfig().getString("discord.avatar-url"));
        builder.setUsername(keklist.getConfig().getString("discord.username"));

        StringBuilder rolesBuilder = new StringBuilder();
        for(String role : keklist.getConfig().getStringList("discord.ping-roles")){
            rolesBuilder.append("<@&").append(role).append("> ");
        }

        builder.setContent(rolesBuilder.toString());
        client.send(builder.build());
    }

    public void fireWhitelistEvent(@NotNull EVENT_TYPE type, @NotNull String entry, @Nullable String from, long unix ) {
        switch (type){
            case WHITELIST_ADD -> {
                if(!triggerEvents.contains(EVENT_TYPE.WHITELIST_ADD)) return;

                embedBuilder.setTimestamp(new Date().toInstant());
                embedBuilder.setColor(1545123);
                embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("Whitelist Add", null));
                embedBuilder.setThumbnailUrl("https://cdn.discordapp.com/attachments/1056727727991959673/1101488230240616500/check-button.png");
                embedBuilder.setDescription(Keklist.getTranslations().get("discord.whitelist.added", entry, from, "<t:" + (unix/1000) + ":f>"));
                sendWebhookMessage(embedBuilder.build());
            }
            case WHITELIST_REMOVE -> {
                if(!triggerEvents.contains(EVENT_TYPE.WHITELIST_REMOVE)) return;

                embedBuilder.setTimestamp(new Date().toInstant());
                embedBuilder.setColor(11553045);
                embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("Whitelist Remove", null));
                embedBuilder.setThumbnailUrl("https://cdn.discordapp.com/attachments/1056727727991959673/1101489868162465802/minus-taste.png");
                embedBuilder.setDescription(Keklist.getTranslations().get("discord.whitelist.removed", entry, from, "<t:" + (unix/1000) + ":f>"));
                sendWebhookMessage(embedBuilder.build());
            }
            case WHITELIST_KICK -> {
                if(!triggerEvents.contains(EVENT_TYPE.WHITELIST_KICK)) return;

                embedBuilder.setTimestamp(new Date().toInstant());
                embedBuilder.setColor(13418516);
                embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("Whitelist Kick", null));
                embedBuilder.setThumbnailUrl("https://cdn.discordapp.com/attachments/1056727727991959673/1102636063761125496/alarm.png");
                embedBuilder.setDescription(Keklist.getTranslations().get("discord.whitelist.kicked", entry, "<t:" + (unix/1000) + ":f>"));
                sendWebhookMessage(embedBuilder.build());
            }
        }
    }

    public void fireBlacklistEvent(@NotNull EVENT_TYPE type, @NotNull String entry, @Nullable String from, @Nullable String reason, long unix ) {
        switch (type){
            case BLACKLIST_ADD -> {
                if(!triggerEvents.contains(EVENT_TYPE.BLACKLIST_ADD)) return;

                embedBuilder.setTimestamp(new Date().toInstant());
                embedBuilder.setColor(2005449);
                embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("Blacklist Add", null));
                embedBuilder.setThumbnailUrl("https://cdn.discordapp.com/attachments/1056727727991959673/1102633373236740198/info-button.png");
                embedBuilder.setDescription(Keklist.getTranslations().get("discord.blacklist.added", entry, from, reason, "<t:" + (unix/1000) + ":f>"));
                sendWebhookMessage(embedBuilder.build());
            }
            case BLACKLIST_REMOVE -> {
                if(!triggerEvents.contains(EVENT_TYPE.BLACKLIST_REMOVE)) return;

                embedBuilder.setTimestamp(new Date().toInstant());
                embedBuilder.setColor(15226380);
                embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("Blacklist Remove", null));
                embedBuilder.setThumbnailUrl("https://cdn.discordapp.com/attachments/1056727727991959673/1102633372867625001/schaltflache-loschen.png");
                embedBuilder.setDescription(Keklist.getTranslations().get("discord.blacklist.removed", entry, from, "<t:" + (unix/1000) + ":f>"));
                sendWebhookMessage(embedBuilder.build());
            }
            case BLACKLIST_KICK -> {
                if(!triggerEvents.contains(EVENT_TYPE.BLACKLIST_KICK)) return;

                embedBuilder.setTimestamp(new Date().toInstant());
                embedBuilder.setColor(13418516);
                embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("Blacklist Kick", null));
                embedBuilder.setThumbnailUrl("https://cdn.discordapp.com/attachments/1056727727991959673/1102636063761125496/alarm.png");
                embedBuilder.setDescription(Keklist.getTranslations().get("discord.blacklist.kicked", entry, "<t:" + (unix/1000) + ":f>"));
                sendWebhookMessage(embedBuilder.build());
            }
        }
    }

    public void fireEvent(@NotNull EVENT_TYPE type, @NotNull String user, long unix){
        switch (type){
            case LIMBO -> {
                if(!triggerEvents.contains(EVENT_TYPE.LIMBO)) return;

                embedBuilder.setTimestamp(new Date().toInstant());
                embedBuilder.setColor(208180);
                embedBuilder.setTitle(new WebhookEmbed.EmbedTitle("Limbo", null));
                embedBuilder.setThumbnailUrl("https://cdn.discordapp.com/attachments/1056727727991959673/1102636063761125496/alarm.png");
                embedBuilder.setDescription(Keklist.getTranslations().get("discord.limbo.kicked", user, "<t:" + (unix/1000) + ":f>"));
                sendWebhookMessage(embedBuilder.build());
            }
        }
    }

    public enum EVENT_TYPE {
        WHITELIST_ADD("whitelist_add"),
        WHITELIST_REMOVE("whitelist_remove"),
        WHITELIST_KICK("whitelist_kick"),
        BLACKLIST_ADD("blacklist_add"),
        BLACKLIST_REMOVE("blacklist_remove"),
        BLACKLIST_KICK("blacklist_kick"),
        LIMBO("limbo");

        private final String configValue;

        private EVENT_TYPE(String configValue) {
            this.configValue = configValue;
        }

        public String getConfigValue() {
            return configValue;
        }

        @Nullable
        public static EVENT_TYPE getByConfigValue(String configValue) {
            for (EVENT_TYPE type : values()) {
                if (type.getConfigValue().equalsIgnoreCase(configValue)) {
                    return type;
                }
            }
            return null;
        }
    }

}
