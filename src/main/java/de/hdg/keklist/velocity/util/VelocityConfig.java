package de.hdg.keklist.velocity.util;

import de.hdg.keklist.velocity.KeklistVelocity;
import lombok.SneakyThrows;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;

import java.io.*;
import java.nio.file.Path;

public class VelocityConfig {
    private YAMLConfigurationLoader configLoader;
    private final Path configDirectory;

    /**
     * Object for managing the config
     *
     * @param directory Data directory of the plugin
     * @param fileName  Name of the config file
     */
    public VelocityConfig(@NotNull Path directory, @NotNull String fileName) {
        this.configDirectory = directory;

        try {
            if (directory.toFile().exists()) {
                if (directory.resolve(fileName).toFile().createNewFile())
                    generateConfig(fileName);

            } else {
                if (directory.toFile().mkdirs())
                    directory.resolve(fileName).toFile().createNewFile();

                generateConfig(fileName);
            }

            this.configLoader = YAMLConfigurationLoader.builder().setPath(directory.resolve(fileName)).setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();

          /*  if (!firstRun) {
                updateConfig(configLoader, YAMLConfigurationLoader.builder().setSource(() -> new BufferedReader(new InputStreamReader(LobbySystem.class.getResourceAsStream("/config.yml")))).setFlowStyle(DumperOptions.FlowStyle.BLOCK).build());
            }*/
        } catch (IOException exception) {
            exception.printStackTrace();
            KeklistVelocity.getInstance().getLogger().error("Error while creating config file! Please report this to the developer!");
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
    public <T> T getOption(@Nullable T defaultValue, @NotNull String path) {
        return configLoader.load().getNode(path).getValue() != null ? (T) configLoader.load().getNode(path).getValue() : defaultValue;
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
        node.getNode(path).setValue(value);
        configLoader.save(node);
    }

    /**
     * Generates the contents of the config file
     *
     * @param filename The filename to put the values in
     * @throws IOException On any IO error
     */
    private void generateConfig(@NotNull String filename) throws IOException {
        KeklistVelocity.getInstance().getLogger().info("Generating config...");

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

        KeklistVelocity.getInstance().getLogger().info("Config generated!");
    }

}