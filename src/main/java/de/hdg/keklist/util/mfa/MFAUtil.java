package de.hdg.keklist.util.mfa;

import com.bergerkiller.bukkit.common.map.MapDisplay;
import com.bergerkiller.bukkit.common.map.MapTexture;
import de.hdg.keklist.Keklist;
import de.hdg.keklist.events.mfa.MFAEvent;
import de.tomino.AuthSys;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MFAUtil {

    private final static String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final @Getter HashMap<Player, MFAPendingData> pendingApproval = new HashMap<>();
    private static final List<Player> isCurrentlyVerified = new ArrayList<>();
    private static final List<Player> hasEnabledMfaCache = new ArrayList<>(); // Cache data to prevent unnecessary database queries

    /**
     * Sends the QR code to the player and registers the player in the database
     *
     * @param player the player to set up
     */
    public static void setupPlayer(@NotNull Player player) {
        String secret = AuthSys.generateSecretKey();
        BufferedImage qrCode = AuthSys.generateQrCodeData(secret, "Keklist " + Bukkit.getServer().getName(), player.getName());

        ItemStack item = MapDisplay.createMapItem(QrCodeDisplay.class);
        item.editMeta(meta -> {
            meta.displayName(Keklist.getInstance().getMiniMessage().deserialize("<gold><bold>2FA"));
            meta.getPersistentDataContainer().set(new NamespacedKey(Keklist.getInstance(), "keklist.2fa.base64"), PersistentDataType.STRING, pngImageToBase64(qrCode));
        });

        pendingApproval.put(player, new MFAPendingData(secret, player.getInventory().getItemInOffHand()));
        MFAEvent.lockPlayer(player);

        player.getInventory().setItemInOffHand(item);  // send qr code to player
        player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.setup")));
    }

    /**
     * Validates the 2fa code for a given player
     *
     * @param player the player to validate the code for
     * @param code   the code to validate
     * @return true if the code is valid, false otherwise
     */
    public static boolean validateCode(@NotNull Player player, @NotNull String code) {
        try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT secret FROM mfa WHERE uuid = ?", player.getUniqueId().toString())) {
            if (rs.next()) {
                try {
                    return AuthSys.validateCode(rs.getString("secret"), code);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * Validates a recovery code for a given player
     *
     * @param player the player to validate the code for
     * @param code   the code to validate
     * @return true if the code is valid, false otherwise
     */
    public static boolean validateRecoveryCode(@NotNull Player player, @NotNull String code) {
        try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT recoveryCodes FROM mfa WHERE uuid = ?", player.getUniqueId().toString())) {
            if (rs.next()) {
                String[] recoveryCodes = rs.getString("recoveryCodes").split(",");
                for (String recoveryCode : recoveryCodes) {
                    if (recoveryCode.contains(code)) {
                        Keklist.getDatabase().onUpdate("UPDATE mfa SET recoveryCodes = ? WHERE uuid = ?", Arrays.toString(Arrays.stream(recoveryCodes).filter(s -> !s.contains(code)).toArray()), player.getUniqueId().toString());
                        return true;
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * Checks if the player has 2fa enabled
     *
     * @param player the player to check
     * @return true if the player has 2fa enabled, false otherwise
     */
    public static boolean hasMFAEnabled(@NotNull Player player) {
        if (hasEnabledMfaCache.contains(player))
            return true;

        try (ResultSet rs = Keklist.getDatabase().onQuery("SELECT secret FROM mfa WHERE uuid = ?", player.getUniqueId().toString())) {
            if (rs.next()) {
                hasEnabledMfaCache.add(player);
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * Disables 2fa for a given player <p>Please check the 2fa/recovery code before disabling 2fa</p>
     * The player should be notified about this!
     *
     * @param player the player to disable 2fa for
     */
    public static void disableMFA(@NotNull Player player) {
        Keklist.getDatabase().onUpdate("DELETE FROM mfa WHERE uuid = ?", player.getUniqueId().toString());
        hasEnabledMfaCache.remove(player);
        setVerified(player, false);
        MFAEvent.unlockPlayer(player); // Unlock the player as they had to enter a valid code to disable 2fa
    }

    /**
     * Generates 10 recovery codes
     *
     * @return the generated recovery codes
     */
    public static String @NotNull [] generateRecoveryCodes() {
        SecureRandom random = new SecureRandom();

        String[] codes = new String[10];
        for (int i = 0; i < 10; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 8; j++) {
                code.append(alphabet.charAt(random.nextInt(62)));
            }
            codes[i] = code.toString();
        }

        return codes;
    }

    /**
     * Checks if the player has already verified their 2fa
     *
     * @param player the player to check
     * @return true if the player has verified their 2fa, false otherwise
     */
    public static boolean hasVerified(@NotNull Player player) {
        return isCurrentlyVerified.contains(player);
    }

    /**
     * Sets the player as verified or not verified
     *
     * @param player   the player to set
     * @param verified true if the player is verified, false otherwise
     */
    public static void setVerified(@NotNull Player player, boolean verified) {
        if (verified) {
            isCurrentlyVerified.add(player);
            MFAEvent.unlockPlayer(player);

            int expireTime = Keklist.getInstance().getConfig().getInt("2fa.expire-time", 180);

            switch (expireTime) {
                case -1 -> {} // Do nothing
                case 0 -> isCurrentlyVerified.remove(player);
                default -> {
                    Bukkit.getScheduler().runTaskLater(Keklist.getInstance(), () -> {
                        if (!player.isOnline())
                            return;

                        setVerified(player, false);
                        if (Keklist.getInstance().getConfig().getBoolean("2fa.notify-expiration-chat", false)) // Not present in the default config
                            player.sendMessage(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.expired")));

                        player.sendActionBar(Keklist.getInstance().getMiniMessage().deserialize(Keklist.getTranslations().get("keklist.2fa.expired")));
                    }, 20L * Math.max(expireTime, 1));
                }
            }
        } else
            isCurrentlyVerified.remove(player);
    }

    /**
     * Clears a player from every list including the pending approval list
     *
     * @param player the player to clear from the lists
     */
    public static void clearPlayerFromLists(@NotNull Player player) {
        pendingApproval.remove(player);
        isCurrentlyVerified.remove(player);
        hasEnabledMfaCache.remove(player);
    }

    /**
     * A record that holds the secret and the offhand item of a player
     */
    public record MFAPendingData(@NotNull String secret, @NotNull ItemStack offhand) {
    }


    /**
     * A map display that displays a qr code
     */
    public static class QrCodeDisplay extends MapDisplay {

        @Override
        public void onAttached() {
            try {
                String base64Code = getMapItem().getPersistentDataContainer().get(new NamespacedKey(Keklist.getInstance(), "keklist.2fa.base64"), PersistentDataType.STRING);
                BufferedImage qrCode = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64Code)));

                getLayer().draw(MapTexture.fromImage(qrCode), 0, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onDetached() {
            if (!getOwners().isEmpty()) {
                MFAPendingData data = pendingApproval.remove(getOwners().getFirst());
                if (data != null && getOwners().getFirst().isOnline()) {
                    getOwners().getFirst().getInventory().setItemInOffHand(data.offhand());
                    getOwners().getFirst().kick(Component.text("2FA Setup"), PlayerKickEvent.Cause.WHITELIST);
                }
            }
        }
    }

    /**
     * Converts a BufferedImage to a base64 encoded string
     *
     * @param image the image to convert
     * @return the base64 encoded string
     */
    private static String pngImageToBase64(@NotNull BufferedImage image) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", os);
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
