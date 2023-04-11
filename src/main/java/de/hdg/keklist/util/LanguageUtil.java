package de.hdg.keklist.util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.hdg.keklist.velocity.KeklistVelocity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;

public class LanguageUtil {

    private final String language;
    private final TypeToken<Map<String, String>> translationTypes = new TypeToken<>() {};
    private final Gson gson = new Gson();
    private final Map<String, String> translations;
    private static Map<String, String> defaultTranslations;


    public LanguageUtil(@NotNull String language) {
        this.language = language;
        InputStream langStream = KeklistVelocity.class.getResourceAsStream("/assets/lang/" + language + ".json");
        InputStream defaultStream = KeklistVelocity.class.getResourceAsStream("/assets/lang/en-us.json");

        if (langStream == null) {
            KeklistVelocity.getInstance().getLogger().warn("Language " + language + " not found! Using default language en-us");
            langStream = KeklistVelocity.class.getResourceAsStream("/assets/lang/en-us.json");
        }else
            KeklistVelocity.getInstance().getLogger().info("Language " + language + " loaded!");

        translations = gson.fromJson(new InputStreamReader(langStream), translationTypes.getType());
        defaultTranslations = gson.fromJson(new InputStreamReader(defaultStream), translationTypes.getType());
    }

    @NotNull
    public String get(String key) {
        if(translations.get(key) == null)
            return Objects.requireNonNull(getDefault(key), "Translation for key " + key + " not found!");
        else
            return translations.get(key);
    }

    @NotNull
    public String get(String key, Object... args) {
        if(translations.get(key) == null)
            return String.format(Objects.requireNonNull(getDefault(key), "Translation for key " + key + " not found!"), args);
        else
            return String.format(get(key), args);
    }

    @Nullable
    public String getDefault(String key) {
        return defaultTranslations.get(key);
    }

    @Nullable
    public String getDefault(String key, Object... args) {
        return String.format(Objects.requireNonNull(getDefault(key)), args);
    }

    public String getLanguage() {
        return language;
    }

    public ImmutableMap<String, String> getRawTranslations() {
        return ImmutableMap.copyOf(translations);
    }

    public ImmutableMap<String, String> getRawDefaultTranslations() {
        return ImmutableMap.copyOf(defaultTranslations);
    }
}
