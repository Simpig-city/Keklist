import java.net.URI
import java.nio.file.Files
import org.ajoberstar.grgit.Grgit

defaultTasks("build")

plugins {
    `java-library`
    `maven-publish`

    id("io.freefair.lombok") version "8.7.1"
    id("com.gradleup.shadow") version "8.3.0"
    id("com.modrinth.minotaur") version "2.+"
    id("org.ajoberstar.grgit") version "5.2.2"
}

group = "de.sage.minecraft"
version = "1.0.0-SNAPSHOT"
description = "Keklist"
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
    maven("https://repo.sageee.xyz/snapshots")
}

dependencies {
    // Provided by the server
    compileOnly(libs.io.papermc.paper.paper.api)
    compileOnly(libs.net.kyori.adventure.text.minimessage)
    compileOnly(libs.com.google.code.gson.gson)
    compileOnly(libs.com.velocitypowered.velocity.api)

    // Provided by plugins
    compileOnly(libs.org.geysermc.floodgate.api)
    compileOnly(libs.net.elytrium.limboapi.api)
    compileOnly(libs.com.github.plan.player.analytics.plan)
    compileOnly(libs.me.clip.placeholderapi)
    compileOnly(libs.net.luckperms.api)
    compileOnly(libs.org.geysermc.geyser.api)

    // Provided via custom loader
    compileOnly(libs.org.xerial.sqlite.jdbc)
    compileOnly(libs.org.mariadb.jdbc.mariadb.java.client)
    compileOnly(libs.com.squareup.okhttp3.okhttp)
    compileOnly(libs.club.minnced.discord.webhooks)
    compileOnly(libs.de.sage.util.updatechecker)

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

            val releasesRepoUrl = "https://repo.sageee.xyz/releases"
            val snapshotsRepoUrl = "https://repo.sageee.xyz/snapshots"
            url = uri(if (!(version as String).contains("SNAPSHOT")) releasesRepoUrl else snapshotsRepoUrl)

            credentials {
                username = System.getenv("KEKLIST_USER")
                password = System.getenv("KEKLIST_PASSWORD")

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

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}

modrinth {
    val changelogContent =
        "[${getLatestCommitHash()}](https://github.com/Simpig-city/Keklist/commit/${getLatestCommitHash()}) ${getLatestCommitMessage()}"

    token.set(System.getenv("MODRINTH_TOKEN")) // Remember to have the MODRINTH_TOKEN environment variable set or else this will fail - just make sure it stays private!
    projectId.set("keklist")
    versionNumber.set("${project.version}")
    versionType.set(if ((version as String).contains("SNAPSHOT")) "release" else "beta")
    //uploadFile.set(tasks.jar)
    uploadFile.set(tasks.getByPath("shadowJar"))
    gameVersions.addAll("1.21.1")
    loaders.addAll("paper", "purpur")
    syncBodyFrom.set(rootProject.file("README.md").readText())
    changelog.set(changelogContent)

    dependencies {
        optional.project("geyser") // Sadly this is the only project on modrinth
    }
}

tasks.modrinth.get().dependsOn(tasks.modrinthSyncBody)

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
        archiveFileName.set("keklist-${version}.jar")
        relocate("org.bstats", "de.hdg.keklist.bstats")
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
            URI.create("https://api.purpurmc.org/v2/purpur/1.21.1/latest/download").toURL().openStream().use {
                Files.copy(it, serverDir.resolve("server.jar").toPath())
            }
        }
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
        if (!serverDir.resolve("server.jar").exists()) {
            dependsOn("downloadServer")
        }
        doFirst {
            pluginDir.resolve("keklist.jar").delete()
            Files.copy(
                layout.buildDirectory.file("libs/Keklist-${version}.jar").get().asFile.toPath(),
                pluginDir.resolve("keklist.jar").toPath()
            )
        }
        classpath = files(serverDir.resolve("server.jar"))
        workingDir = serverDir
        jvmArgs = listOf("-Dcom.mojang.eula.agree=true")
        args = listOf("--nogui")
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