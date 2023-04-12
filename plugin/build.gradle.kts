@file:Suppress("UnstableApiUsage")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    // Spigot
    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")

    // Implementation Dependencies
    implementation("org.bstats:bstats-bukkit:3.0.1")
    implementation("com.jeff_media:SpigotUpdateChecker:3.0.1")

    // Soft Dependencies
    compileOnly("me.clip:placeholderapi:2.11.3")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.github.lokka30.treasury:treasury-api:1.2.1")
    compileOnly("com.github.EssentialsX.Essentials:EssentialsX:2.19.7")

    // API

    listOf(
        "api",
        "abstract",
        "1_8_R1",
        "1_8_R2",
        "1_8_R3",
        "1_9_R1",
        "1_9_R2",
        "1_10_R1",
        "1_11_R1",
        "1_12_R1",
        "1_13_R1",
        "1_13_R2",
        "1_14_R1",
        "1_15_R1",
        "1_16_R1",
        "1_16_R2",
        "1_16_R3",
        "1_17_R1",
        "1_18_R1",
        "1_18_R2",
        "1_19_R1",
        "1_19_R2",
        "1_19_R3"
    ).forEach { api(project(":novaconomy-$it")) }
}

tasks {
    register("sourcesJar", Jar::class.java) {
        dependsOn("classes")

        archiveFileName.set("Novaconomy-${project.version}-sources.jar")
        from(sourceSets["main"].allSource)
    }

    compileJava {
        listOf(
            "1_18_R1",
            "1_18_R2",
            "1_19_R1",
            "1_19_R2",
            "1_19_R3"
        ).forEach { dependsOn(":novaconomy-$it:remap") }
    }

    withType<ProcessResources> {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }

    withType<ShadowJar> {
        archiveFileName.set("Novaconomy-${project.version}.jar")
    }
}

artifacts {
    add("archives", tasks["sourcesJar"])
}