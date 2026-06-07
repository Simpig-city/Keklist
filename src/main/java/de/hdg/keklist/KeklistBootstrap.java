package de.hdg.keklist;

import de.hdg.keklist.commands.type.BrigadierCommand;
import de.hdg.keklist.commands.type.CommandData;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class KeklistBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        // Initialize Reflections with an explicit classloader
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("de.hdg.keklist", Keklist.class.getClassLoader()))  // Manually set the plugin's location
                .setScanners(Scanners.SubTypes)  // Scan for subclasses
                .addClassLoaders(Keklist.class.getClassLoader()));  // Use the correct classloader


        Set<Class<? extends BrigadierCommand>> subTypes = reflections.getSubTypesOf(BrigadierCommand.class);

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            subTypes.forEach(clazz -> {
                try {
                    BrigadierCommand command = clazz.getDeclaredConstructor().newInstance();
                    Method getCommandMethod = clazz.getDeclaredMethod("getCommand");

                    if (getCommandMethod.isAnnotationPresent(CommandData.class)) {
                        CommandData data = getCommandMethod.getAnnotation(CommandData.class);
                        commands.registrar().register(command.getCommand(), data.descriptionKey(), List.of(data.aliases()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
}