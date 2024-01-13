val mcVersion = "1.13.2"
val lampVersion = "3.1.8"

dependencies {
    api(project(":novaconomy-abstract"))
    api(project(":novaconomy-api"))

    implementation("com.github.Revxrsal.Lamp:bukkit:$lampVersion")
    implementation("com.github.Revxrsal.Lamp:common:$lampVersion")

    compileOnly("org.spigotmc:spigot:$mcVersion-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.compilerArgs.add("-parameters")
    }
}