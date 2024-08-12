package de.hdg.keklist.extentions;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.BooleanProvider;
import com.djrapitops.plan.extension.annotation.Conditional;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.StringProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import de.hdg.keklist.Keklist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@PluginInfo(
        name = "Keklist",
        iconName = "user-lock",
        iconFamily = Family.SOLID,
        color = Color.BLUE
)
public class KekDataExtension implements DataExtension {

    public KekDataExtension() {
        Keklist.getInstance().getLogger().info(Keklist.getTranslations().get("plan.enabled"));
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_JOIN,
                CallEvents.PLAYER_LEAVE,
                CallEvents.MANUAL
        };
    }

    @BooleanProvider(
            text = "Whitelisted",
            description = "Is the player whitelisted?",
            iconName = "lock-open",
            iconColor = Color.LIGHT_GREEN,
            priority = 100,
            showInPlayerTable = true,
            conditionName = "isWhitelisted"
    )
    public boolean isWhitelisted(UUID uuid) {
        try {
           return Keklist.getDatabase().onQuery("SELECT 1 FROM whitelist WHERE uuid = ?", uuid.toString()).next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @StringProvider(
            text = "Whitelisted By",
            description = "Who whitelisted the player?",
            iconName = "user-lock",
            iconColor = Color.LIGHT_GREEN,
            priority = 95,
            showInPlayerTable = false
    )
    @Conditional("isWhitelisted")
    public String whitelistedBy(UUID uuid) {
        try {
            ResultSet result = Keklist.getDatabase().onQuery("SELECT byPlayer FROM whitelist WHERE uuid = ?", uuid.toString());
            if (result.next()) {
                return result.getString("byPlayer");
            }
            return "Unknown";
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @BooleanProvider(
            text = "Blacklisted",
            description = "Is the player blacklisted?",
            iconName = "lock",
            iconColor = Color.BLUE,
            priority = 90,
            showInPlayerTable = true,
            conditionName = "isBlacklisted"
    )
    public boolean isBlacklisted(UUID uuid) {
        try {
           return Keklist.getDatabase().onQuery("SELECT 1 FROM blacklist WHERE uuid = ?", uuid.toString()).next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @StringProvider(
            text = "Blacklisted By",
            description = "Who blacklisted the player?",
            iconName = "user-lock",
            iconColor = Color.BLUE,
            priority = 85,
            showInPlayerTable = false
    )
    @Conditional("isBlacklisted")
    public String blacklistedBy(UUID uuid) {
        try {
            ResultSet result = Keklist.getDatabase().onQuery("SELECT byPlayer FROM blacklist WHERE uuid = ?", uuid.toString());
            if (result.next()) {
                return result.getString("byPlayer");
            }
            return "Unknown";
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @StringProvider(
            text = "Last Seen IP",
            description = "The last IP the player was seen with.",
            iconName = "ethernet",
            iconColor = Color.LIGHT_BLUE,
            priority = 80,
            showInPlayerTable = true
    )
    public String lastSeenIp(UUID uuid) {
        try {
            ResultSet result = Keklist.getDatabase().onQuery("SELECT ip FROM lastSeen WHERE uuid = ?", uuid.toString());
            if (result.next()) {
                return result.getString("ip");
            }
            return "Unknown";
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
