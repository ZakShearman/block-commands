import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "pink.zak.minecraft.blockcommands"
version = "1.0.0"

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
    implementation("com.github.Revxrsal.Lamp:common:3.2.1")
    implementation("com.github.Revxrsal.Lamp:bukkit:3.2.1")

    compileOnly("me.clip:placeholderapi:2.11.6")

    implementation("com.jeff-media:MorePersistentDataTypes:2.4.0")
    implementation("com.jeff-media:custom-block-data:2.2.3")
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
    isEnableRelocation = true
    relocationPrefix = "pink.zak.minecraft.blockcommands.shaded"
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
    minecraftVersion("1.21.1")
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition", "-XX:+AllowRedefinitionToAddDeleteMethods")
}