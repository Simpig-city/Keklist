import java.net.URI
import java.nio.file.Files
import org.ajoberstar.grgit.Grgit
import io.papermc.hangarpublishplugin.model.Platforms

defaultTasks("build")

plugins {
    `java-library`
    `maven-publish`

    id("io.freefair.lombok") version "8.14"
    id("com.gradleup.shadow") version "8.3.8"
    id("com.modrinth.minotaur") version "2.+"
    id("org.ajoberstar.grgit") version "5.3.2"
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
}

group = "de.sage.minecraft"
version = "1.0.0-SNAPSHOT"
description = "Custom made black and whitelist with many different features"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    // mavenLocal()
    mavenCentral()

    // Paper and Velocity API
    maven("https://repo.papermc.io/repository/maven-public/")

    // Adventure API
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")

    // Limbo API
    maven("https://maven.elytrium.net/repo/")

    // Geyser/Floodgate API
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.opencollab.dev/main/")

    // Plan API
    maven("https://jitpack.io")

    // PlaceholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    // Update Checker
    //maven("https://repo.sageee.xyz/snapshots")

    // BkCommonLib
    maven("https://ci.mg-dev.eu/plugin/repository/everything")
}

dependencies {
    // Provided by the server
    compileOnly(libs.io.papermc.paper.paper.api)
    compileOnly(libs.com.velocitypowered.velocity.api)
    annotationProcessor(libs.com.velocitypowered.velocity.api)
    compileOnly(libs.net.kyori.adventure.text.minimessage)
    compileOnly(libs.com.google.code.gson.gson)

    // Provided by plugins
    compileOnly(libs.org.geysermc.floodgate.api)
    compileOnly(libs.org.geysermc.geyser.api)
    compileOnly(libs.net.elytrium.limboapi.api)
    compileOnly(libs.com.github.plan.player.analytics.plan)
    compileOnly(libs.me.clip.placeholderapi)
    compileOnly(libs.net.luckperms.api)
    compileOnly(libs.com.bergerkiller.bukkit.bkcommonlibs)

    // Provided via custom loader
    compileOnly(libs.org.xerial.sqlite.jdbc)
    compileOnly(libs.org.mariadb.jdbc.mariadb.java.client)
    compileOnly(libs.com.squareup.okhttp3.okhttp)
    compileOnly(libs.club.minnced.discord.webhooks)
    //compileOnly(libs.de.sage.util.updatechecker) // Currently disabled due to repository issues
    compileOnly(libs.com.github.tominolp.mfa.api)
    compileOnly(libs.com.zaxxer.hikaricp)
    compileOnly(libs.com.h2database.h2)
    compileOnly(libs.org.reflections.reflections)

    // Other / Shaded
    implementation(libs.org.bstats.bstats.bukkit)
}

java {
    withSourcesJar()
    withJavadocJar()

    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

publishing {
    repositories {
        maven {
            name = "keklist"

            val releasesRepoUrl = uri("https://repo.sageee.xyz/releases")
            val snapshotsRepoUrl = uri("https://repo.sageee.xyz/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = System.getenv("KEKLIST_REPO_USER")
                password = System.getenv("KEKLIST_REPO_PASSWORD")
            }
        }

        publications.create<MavenPublication>("maven") {
            pom {
                name = project.name
                description = project.description
                url = "https://github.com/simpig-city/keklist"

                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        id = "sage"
                        name = "SageSphinx63920"
                        email = "sage@sagesphinx63920.dev"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/Simpig-city/Keklist.git"
                    developerConnection = "scm:git:https://github.com/Simpig-city/Keklist.git"
                    url = "https://github.com/Simpig-city/Keklist"
                }
            }

            artifactId = project.name

            from(components["java"])
        }
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("keklist")
    versionNumber.set("${project.version}")
    versionType.set(if (version.toString().endsWith("SNAPSHOT")) "beta" else "release")
    //uploadFile.set(tasks.jar)
    uploadFile.set(tasks.getByPath("shadowJar"))
    gameVersions.addAll("1.21.3", "1.21.4", "1.12.5", "1.21.6", "1.21.7", "1.21.8")
    loaders.addAll("paper", "purpur", "velocity")
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set("[${getLatestCommitHash()}](https://github.com/Simpig-city/Keklist/commit/${getLatestCommitHash()}) ${getLatestCommitMessage()}")

    dependencies {
        optional.project("geyser")
        optional.project("bkcommonlib")
        optional.project("plan")
        optional.project("limboapi")
        optional.project("luckperms")
    }
}

hangarPublish {
    publications.register("hangar") {
        version.set(project.version as String)
        channel.set(if (!version.toString().endsWith("SNAPSHOT")) "Snapshot" else "Release")
        id.set("keklist")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        changelog.set("[${getLatestCommitHash()}](https://github.com/Simpig-city/Keklist/commit/${getLatestCommitHash()}) ${getLatestCommitMessage()}")
        pages {
            resourcePage(provider { file("README.md").readText() })
        }

        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.jar.flatMap { it.archiveFile })
                platformVersions.set(listOf("1.21.x"))
                dependencies {
                    hangar("Geyser") {
                        required.set(false)
                    }
                    url("Floodgate", "https://geysermc.org/download?project=floodgate") {
                        required.set(false)
                    }
                    hangar("PlaceholderAPI") {
                        required.set(false)
                    }
                    hangar("Plan-Player-Analytics") {
                        required.set(false)
                    }
                    url("BkCommonLib", "https://modrinth.com/plugin/bkcommonlib") {
                        required.set(false)
                    }
                    url("LuckPerms", "https://luckperms.net/download") {
                        required.set(false)
                    }
                }
            }
            register(Platforms.VELOCITY) {
                jar.set(tasks.jar.flatMap { it.archiveFile })
                platformVersions.set(listOf("3.4"))
                dependencies {
                    url("LimboAPI", "https://github.com/Elytrium/LimboAPI") {
                        required.set(false)
                    }
                }
            }
        }
    }
}

tasks.modrinth.get().dependsOn(tasks.modrinthSyncBody)
tasks.jar.get().dependsOn(tasks.shadowJar)

val serverDir: File = projectDir.resolve("testserver")
val pluginDir: File = serverDir.resolve("plugins")

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
        options.showFromProtected()
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("org.bstats", "libs.bstats")

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }

    jar {
        archiveClassifier.set("unshaded")
    }

    processResources {
        filesMatching("*.yml") {
            expand(
                mapOf(
                    "name" to project.name,
                    "version" to project.version,
                    "description" to project.description
                )
            )
        }
    }

    register("downloadServer") {
        group = "purpur"
        doFirst {
            serverDir.mkdirs()
            pluginDir.mkdirs()
            URI.create("https://api.purpurmc.org/v2/purpur/1.21.8/latest/download").toURL().openStream().use {
                if (serverDir.resolve("server.jar").exists()) {
                    Files.delete(serverDir.resolve("server.jar").toPath())
                        .also { _ -> Files.copy(it, serverDir.resolve("server.jar").toPath()) }
                } else
                    Files.copy(it, serverDir.resolve("server.jar").toPath())

            }
        }
    }

    register("publishNewRelease") {
        group = "util"
        dependsOn(
            "publish",
            "modrinth",
            "publishHangarPublicationToHangar",
            "syncHangarPublicationMainResourcePagePageToHangar"
        )
    }

    // Is this any useful?
    /*register("downloadExtensions") {
        group = "purpur"
        dependsOn("downloadServer")
        doFirst {
            pluginDir.listFiles { _, name -> name.endsWith(".jar") && !name.equals("keklist.jar") }
                ?.forEach { it.delete() }

            URI.create("https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot")
                .toURL().openStream().use {
                Files.copy(it, pluginDir.resolve("geyser.jar").toPath())
            }

            URI.create("https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot")
                .toURL().openStream().use {
                Files.copy(it, pluginDir.resolve("floodgate.jar").toPath())
            }

            URI.create("https://download.luckperms.net/1554/bukkit/loader/LuckPerms-Bukkit-5.4.139.jar").toURL()
                .openStream().use {
                Files.copy(it, pluginDir.resolve("luckperms.jar").toPath())
            }

            URI.create("https://hangarcdn.papermc.io/plugins/HelpChat/PlaceholderAPI/versions/2.11.6/PAPER/PlaceholderAPI-2.11.6.jar")
                .toURL().openStream().use {
                Files.copy(it, pluginDir.resolve("placeholderAPI.jar").toPath())
            }

            URI.create("https://github.com/plan-player-analytics/Plan/releases/download/5.6.2883/Plan-5.6-build-2883.jar")
                .toURL().openStream().use {
                Files.copy(it, pluginDir.resolve("plan.jar").toPath())
            }
        }
    }*/

    register("runServer", JavaExec::class) {
        group = "purpur"
        dependsOn("shadowJar")

        if (!serverDir.resolve("server.jar").exists())
            dependsOn("downloadServer")
        else if (serverDir.resolve("server.jar")
                .lastModified() < System.currentTimeMillis() - (((1000 * 60) * 60) * 24) * 7
        )
            dependsOn("downloadServer")

        doFirst {
            pluginDir.resolve("keklist.jar").delete()
            Files.copy(
                layout.buildDirectory.file("libs/Keklist-${version}.jar").get().asFile.toPath(),
                pluginDir.resolve("keklist.jar").toPath()
            )
        }
        classpath = files(serverDir.resolve("server.jar"))
        workingDir = serverDir
        jvmArgs = listOf(
            "-Dcom.mojang.eula.agree=true",
            "--add-modules=jdk.incubator.vector",
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
        )
        // args = listOf("--nogui")
        standardInput = System.`in`
    }
}


fun getBranch(): String {
    Grgit.open(mapOf("currentDir" to project.rootDir)).use { git ->
        return git.branch.current().name
    }
}

fun getLatestCommitHash(): String {
    Grgit.open(mapOf("currentDir" to project.rootDir)).use { git ->
        return git.head().id
    }
}

fun getLatestCommitMessage(): String {
    Grgit.open(mapOf("currentDir" to project.rootDir)).use { git ->
        return git.log().first().fullMessage
    }
}