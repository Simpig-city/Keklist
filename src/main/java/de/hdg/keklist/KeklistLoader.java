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

        resolver.addRepository(new RemoteRepository.Builder("maven", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
        resolver.addRepository(new RemoteRepository.Builder("jitpack", "default", "https://jitpack.io").build());
        //resolver.addRepository(new RemoteRepository.Builder("sage-snap", "default", "https://repo.sageee.xyz/snapshots").build());
        //resolver.addRepository(new RemoteRepository.Builder("sage-release", "default", "https://repo.sageee.xyz/releases").build());

        resolver.addDependency(new Dependency(new DefaultArtifact("com.squareup.okhttp3:okhttp:5.1.0"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.xerial:sqlite-jdbc:3.50.3.0"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.mariadb.jdbc:mariadb-java-client:3.5.4"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("club.minnced:discord-webhooks:0.8.4"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.TominoLP:2FA-API:1.3.2"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:6.3.1"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.h2database:h2:2.3.232"), "compile"));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.reflections:reflections:0.10.2"), "compile"));
        //resolver.addDependency(new Dependency(new DefaultArtifact("de.sage.util:updatechecker:1.0.3-SNAPSHOT"), "compile"));

        pluginClasspathBuilder.addLibrary(resolver);
    }
}
