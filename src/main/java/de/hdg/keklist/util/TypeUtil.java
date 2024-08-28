package de.hdg.keklist.util;

import de.hdg.keklist.Keklist;
import org.jetbrains.annotations.NotNull;

public class TypeUtil {

    public static EntryType getEntryType(@NotNull String entry) {
        return switch (entry){
            case String e when e.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") -> EntryType.IPv4;
            case String e when e.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$") -> EntryType.IPv6;
            case String e when e.matches("^[a-zA-Z0-9_]{2,16}$") -> EntryType.JAVA;
            case String e when e.startsWith(Keklist.getInstance().getConfig().getString("floodgate.prefix" , ""))
                    && Keklist.getInstance().getFloodgateApi() == null -> EntryType.BEDROCK;
            case String e when e.matches("^(?:[a-zA-Z0-9]{1,63}\\.){1,126}[a-zA-Z]{2,63}$") -> EntryType.DOMAIN;
            case String e when e.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") -> EntryType.UUID;

            default -> EntryType.UNKNOWN;
        };
    }


    public enum EntryType {
        DOMAIN,
        IPv4,
        IPv6,
        BEDROCK,
        JAVA,
        UUID,
        UNKNOWN
    }
}
