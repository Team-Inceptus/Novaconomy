val mcVersion = "1.8.8"

dependencies {
    api(project(":novaconomy-abstract"))
    api(project(":novaconomy-api"))

    compileOnly("org.spigotmc:spigot:$mcVersion-R0.1-SNAPSHOT")
}