package de.hdg.keklist.extentions.context;

import de.hdg.keklist.Keklist;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;

public class WhitelistedCalculator implements ContextCalculator<Player> {

    private static final String KEY = "keklist:whitelisted";

    @Override
    public void calculate(@NonNull Player player, @NonNull ContextConsumer contextConsumer) {
        ResultSet rsUser = Keklist.getDatabase().onQuery("SELECT * FROM whitelist WHERE uuid = ?", player.getUniqueId().toString());

        try {
            if (rsUser.next())
                contextConsumer.accept(KEY, "true");
            else
                contextConsumer.accept(KEY, "false");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull ContextSet estimatePotentialContexts() {
        return ImmutableContextSet.builder()
                .add(KEY, "true")
                .add(KEY, "false")
                .build();
    }
}
