package de.hdg.keklist.velocity.util;

import de.hdg.keklist.velocity.KeklistVelocity;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class VelocityConfig {
    private YamlConfigurationLoader configLoader;
    private final Path configDirectory;
    private final String fileName;

    /**
     * Object for managing the config
     *
     * @param directory Data directory of the plugin
     * @param fileName  Name of the config file
     */
    public VelocityConfig(@NotNull Path directory, @NotNull String fileName) {
        this.configDirectory = directory;
        this.fileName = fileName;

        try {
            if (directory.toFile().exists()) {
                if (directory.resolve(fileName).toFile().createNewFile())
                    generateConfig(fileName);

            } else {
                if (directory.toFile().mkdirs())
                    directory.resolve(fileName).toFile().createNewFile();

                generateConfig(fileName);
            }

            this.configLoader = YamlConfigurationLoader.builder().path(directory.resolve(fileName)).nodeStyle(NodeStyle.BLOCK).build();

        } catch (IOException exception) {
            exception.printStackTrace();
            //KeklistVelocity.getInstance().getLogger().error(KeklistVelocity.getTranslations().get("velocity.config.error"));
        }
    }

    /**
     * Return the value of the given path or the default value
     *
     * @param defaultValue Default if no value was found on the path
     * @param path         Path to find the value
     * @return The value to the path or default
     */
    @SneakyThrows(IOException.class)
    public <T> T getOption(@NotNull T defaultValue, @NotNull String path) {
        return configLoader.load().node(path).raw() != null ? (T) configLoader.load().node(path).raw() : defaultValue;
    }

    /**
     * Sets the value to the given path
     *
     * @param value Value to set
     * @param path  Path of the value to set
     * @throws IOException On any IO error
     */
    public void setValue(@NotNull Object value, @NotNull String path) throws IOException {
        ConfigurationNode node = configLoader.load();
        node.node(path).set(value);
        configLoader.save(node);
    }

    /**
     *
     */
    public void updateToNewVersion() {
        try {
            updateConfig(configLoader, YamlConfigurationLoader.builder().url(this.getClass().getResource("/velocity-config.yml")).nodeStyle(NodeStyle.BLOCK).build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates the contents of the config file
     *
     * @param filename The filename to put the values in
     * @throws IOException On any IO error
     */
    private void generateConfig(@NotNull String filename) throws IOException {
        //KeklistVelocity.getInstance().getLogger().info(KeklistVelocity.getTranslations().get("velocity.config.creating"));

        InputStream configStream = KeklistVelocity.class.getResourceAsStream("/velocity-config.yml");
        File config = configDirectory.resolve(filename).toFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(config));
        BufferedReader reader = new BufferedReader(new InputStreamReader(configStream));

        int charactar;
        while (!((charactar = reader.read()) == -1)) {
            writer.write(charactar);
        }

        writer.close();
        reader.close();

        //KeklistVelocity.getInstance().getLogger().info(KeklistVelocity.getTranslations().get("velocity.config.created"));
    }

    /**
     * Updates the config file
     *
     * @param oldConfigLoader Old config loader
     * @param newConfigLoader New config loader
     * @throws IOException On any IO error
     */
    private void updateConfig(@NotNull YamlConfigurationLoader oldConfigLoader, @NotNull YamlConfigurationLoader newConfigLoader) throws IOException {
        ConfigurationNode oldConfig = oldConfigLoader.load();
        ConfigurationNode newConfig = newConfigLoader.load();

        if (oldConfig.node("config_version").getInt() < newConfig.node("config_version").getInt()) {
            //KeklistVelocity.getInstance().getLogger().info(KeklistVelocity.getTranslations().get("velocity.config.updating"));


            for (ConfigurationNode node : newConfig.childrenMap().values()) {
                if (!oldConfig.childrenMap().containsKey(node.key())) {
                    oldConfig.node(node.key()).set(node.raw());
                }
            }

            oldConfigLoader.save(oldConfig);

            addComments();


            // KeklistVelocity.getInstance().getLogger().info(KeklistVelocity.getTranslations().get("velocity.config.updated"));
        }
    }

    private void addComments() throws IOException {
        System.out.println("Adding comments");
        InputStream configStream = KeklistVelocity.class.getResourceAsStream("/velocity-config.yml");
        BufferedReader reader = new BufferedReader(new InputStreamReader(configStream));

        BufferedReader oldReader = new BufferedReader(new FileReader(configDirectory.resolve(fileName).toFile()));
        List<String> oldConfig = new java.util.ArrayList<>(oldReader.lines().toList());

        AtomicInteger lineInt = new AtomicInteger(0);
        AtomicInteger lineIndex = new AtomicInteger(0);

        boolean comment = false;
        StringBuilder commentString = new StringBuilder();

        int charactar;
        while (!((charactar = reader.read()) == -1)) {
            if ((char) charactar == '#' && !comment) {
                comment = true;
                commentString.append((char) charactar);
                System.out.println("Comment started");
            } else
                lineIndex.incrementAndGet();

            if (comment) {
                commentString.append((char) charactar);
            }

            if (String.valueOf((char) charactar).equals(System.lineSeparator())) {
                if (oldConfig.size() > lineInt.get())
                    oldConfig.set(lineInt.get(), oldConfig.get(lineInt.get()) + System.lineSeparator());


                System.out.println("New line");
                if (comment) {
                    comment = false;

                    if (oldConfig.size() > lineInt.get()) {
                        oldConfig.set(lineInt.get(), insertString(oldConfig.get(lineInt.get()), commentString.toString(), lineIndex.get()) + System.lineSeparator());
                    } else
                        oldConfig.add(commentString.toString() + System.lineSeparator());
                    System.out.println("Comment ended");
                }

                lineIndex.set(0);
                lineInt.incrementAndGet();
            }
        }


        /*reader.lines().forEach(line -> {
            if (line.startsWith("#") && !oldConfig.get(lineInt.get()).startsWith("#")) {
                oldConfig.add(lineInt.get(), line);
            }

            lineInt.incrementAndGet();
        });*/


        BufferedWriter writer = new BufferedWriter(new FileWriter(configDirectory.resolve(fileName).toFile()));
        oldConfig.forEach(line -> {
            try {
                System.out.println("Written: " + line);
                writer.write(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        writer.close();
        reader.close();
        oldReader.close();
    }

    private String insertString(String originalString, String stringToBeInserted, int index) {
        StringBuilder newString = new StringBuilder();

        for (int i = 0; i < originalString.length(); i++) {
            newString.append(originalString.charAt(i));

            if (i == index) {
                newString.append(stringToBeInserted);
            }
        }

        return newString.toString();
    }
}