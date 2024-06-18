plugins {
    `java-library`
    `maven-publish`
     id("io.freefair.lombok") version "8.6"
     id("org.cadixdev.licenser") version "0.6.1"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven ("https://jitpack.io") // Used for Plan API

    maven ("https://maven.pkg.github.com/Simpig-city/Keklist")

    // Paper and Velocity API
    maven ("https://repo.papermc.io/repository/maven-public/")

    // Adventure API
    maven ("https://s01.oss.sonatype.org/content/repositories/snapshots/")

    // Limbo API
    maven ("https://maven.elytrium.net/repo/")

    // Geyser/Floodgate API
    maven ("https://repo.opencollab.dev/maven-snapshots/")
    maven ("https://repo.opencollab.dev/main/")

    // PlaceholderAPI
    maven ("https://repo.extendedclip.com/content/repositories/placeholderapi/")

    // Update Checker
    maven ("https://repo.sageee.xyz/snapshots")
}

dependencies {
    api(libs.org.bstats.bstats.bukkit)
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
java.sourceCompatibility = JavaVersion.VERSION_17

java {
    withSourcesJar()
    withJavadocJar()

    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

license {
    header = resources.text.fromFile(rootProject.file("HEADER.txt"))
    include("**/*.java")
    newLine = true
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
    options.release = 21
}

tasks.withType<Javadoc>() {
    options.encoding = Charsets.UTF_8.name()
}
