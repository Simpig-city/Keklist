package de.hdg.keklist.util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
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


    public LanguageUtil(@NotNull String language, @NotNull File dataFolder, Logger logger) {
        this.language = language;
        InputStream langStream = this.getClass().getResourceAsStream("/assets/lang/" + language + ".json");
        InputStream defaultStream = this.getClass().getResourceAsStream("/assets/lang/en-us.json");

        if (new File(dataFolder, "lang/" + language + ".json").exists()) {
            try {
                langStream = new File(dataFolder, "lang/" + language + ".json").toURI().toURL().openStream();
            } catch (IOException e) {
                logger.warn("Custom language " + language + " not found! Using default language en-us");
                langStream = defaultStream;
            }
        } else {
            if (langStream == null) {
                logger.warn("Language " + language + " not found! Using default language en-us");
                langStream = defaultStream;
            } else
                logger.info("Language " + language + " loaded!");
        }

        translations = gson.fromJson(new InputStreamReader(langStream), translationTypes.getType());
        defaultTranslations = gson.fromJson(new InputStreamReader(defaultStream), translationTypes.getType());
    }

    @NotNull
    public String get(@NotNull String key) {
        if (translations.get(key) == null)
            return Objects.requireNonNull(getDefault(key), "Translation for key " + key + " not found!");
        else
            return translations.get(key);
    }

    @NotNull
    public String get(@NotNull String key, @Nullable Object... args) {
        if (translations.get(key) == null)
            return String.format(Objects.requireNonNull(getDefault(key), "Translation for key " + key + " not found!"), args);
        else
            return String.format(get(key), args);
    }

    @Nullable
    public String getDefault(@NotNull String key) {
        return defaultTranslations.get(key);
    }

    @Nullable
    public String getDefault(@NotNull String key, @Nullable Object... args) {
        return String.format(Objects.requireNonNull(getDefault(key)), args);
    }

    @NotNull
    public String getLanguageCode() {
        return language;
    }

    @NotNull
    public ImmutableMap<String, String> getRawTranslations() {
        return ImmutableMap.copyOf(translations);
    }

    @NotNull
    public ImmutableMap<String, String> getRawDefaultTranslations() {
        return ImmutableMap.copyOf(defaultTranslations);
    }
}
