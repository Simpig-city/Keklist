package de.hdg.keklist.extentions;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.BooleanProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
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
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", uuid.toString());
            return rs.next();
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
            ResultSet rs = Keklist.getDatabase().onQuery("SELECT * FROM blacklist WHERE uuid = ?", uuid.toString());
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
