import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pink.zak.minecraft.blockcommands"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // SpigotMC
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI

    maven("https://repo.jeff-media.com/public") // MorePersistentDataTypes, custom-block-data

    maven("https://jitpack.io") // Lamp
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.17.1-R0.1-SNAPSHOT")

    compileOnly("org.jetbrains:annotations:24.1.0")

    // Lamp - Command Framework
    implementation("io.github.revxrsal:lamp.common:4.0.0-beta.19")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-beta.19")

    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("org.flywaydb:flyway-core:11.8.2")
}

val targetJavaVersion = 17
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<ShadowJar>().configureEach {
    isEnableRelocation = false
    val relocationBase = "pink.zak.minecraft.blockcommands.shaded"

    // todo these don't actually relocate to these locations, IDK
    relocate("org.xerial.sqlite", "$relocationBase.sqlite")
    relocate("io.github.revxrsal", "$relocationBase.revxrsal")
    relocate("com.jeff_media", "$relocationBase.jeffmedia")
//    relocate("org.flywaydb", "$relocationBase.flywaydb") // if Flyway is relocated, it causes an error location the sqlite driver. IDK why
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(targetJavaVersion)
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
            "version" to project.version,
            "group" to project.group,
            "name" to providers.gradleProperty("name").get()
    )
    inputs.properties(props)

    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.withType<RunServer> {
    minecraftVersion("1.21.5")
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(21)
    }
}