import java.net.URI
import java.nio.file.Files
import org.ajoberstar.grgit.Grgit

val serverDir: File = projectDir.resolve("testserver")
val pluginDir: File = serverDir.resolve("plugins")

defaultTasks("licenseFormat", "build")

plugins {
    `java-library`
    `maven-publish`

    id("io.freefair.lombok") version "8.7.1"
    id("org.cadixdev.licenser") version "0.6.1"
    id("com.gradleup.shadow") version "8.3.0"
    id("com.modrinth.minotaur") version "2.+"
    id("org.ajoberstar.grgit") version "5.2.2"
}

repositories {
    // mavenLocal()
    mavenCentral()

    // Plan API
    maven("https://jitpack.io")

    maven("https://maven.pkg.github.com/Simpig-city/Keklist")

    // Paper and Velocity API
    maven("https://repo.papermc.io/repository/maven-public/")

    // Adventure API
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")

    // Limbo API
    maven("https://maven.elytrium.net/repo/")

    // Geyser/Floodgate API
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.opencollab.dev/main/")

    // PlaceholderAPI
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    // Update Checker
    maven("https://repo.sageee.xyz/snapshots")
}

dependencies {
    implementation(libs.org.bstats.bstats.bukkit)

    compileOnly(libs.io.papermc.paper.paper.api)
    compileOnly(libs.net.kyori.adventure.text.minimessage)
    compileOnly(libs.com.google.code.gson.gson)
    compileOnly(libs.com.velocitypowered.velocity.api)
    compileOnly(libs.org.geysermc.floodgate.api)
    compileOnly(libs.net.elytrium.limboapi.api)
    compileOnly(libs.com.github.plan.player.analytics.plan)
    compileOnly(libs.me.clip.placeholderapi)
    compileOnly(libs.net.luckperms.api)
    compileOnly(libs.org.geysermc.geyser.api)
    compileOnly(libs.org.xerial.sqlite.jdbc)
    compileOnly(libs.org.mariadb.jdbc.mariadb.java.client)
    compileOnly(libs.com.squareup.okhttp3.okhttp)
    compileOnly(libs.club.minnced.discord.webhooks)
    compileOnly(libs.de.sage.util.updatechecker)
    compileOnly(libs.org.projectlombok.lombok)
}

group = "de.sage.minecraft"
version = "1.0.0-SNAPSHOT"
description = "Keklist"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

java {
    withSourcesJar()
    withJavadocJar()

    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

license {
    header = resources.text.fromFile(rootProject.file("HEADER.txt"))
    include("**/*.java")
    newLine = true
    ignoreFailures = true
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
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}

modrinth {
    val changelogContent =
        "[${getLatestCommitHash()}](https://github.com/ViaVersion/ViaRewind/commit/${getLatestCommitHash()}) ${getLatestCommitMessage()}"

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

tasks.javadoc.configure() {
    //options.('Xdoclint:-missing', '-quiet')
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
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

    register("downloadExtensions") {
        group = "purpur"
        doFirst {

        }
    }

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