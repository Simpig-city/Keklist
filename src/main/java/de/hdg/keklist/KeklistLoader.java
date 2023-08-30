package de.hdg.keklist;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

public class KeklistLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder pluginClasspathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("maven", "default", "https://repo.maven.apache.org/maven2").build());

        resolver.addDependency(new Dependency(new DefaultArtifact("com.squareup.okhttp3:okhttp:5.0.0-alpha.11"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.xerial:sqlite-jdbc:3.42.0.1"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.mariadb.jdbc:mariadb-java-client:3.2.0"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("club.minnced:discord-webhooks:0.8.4"), "compile"));

        pluginClasspathBuilder.addLibrary(resolver);
    }
}
