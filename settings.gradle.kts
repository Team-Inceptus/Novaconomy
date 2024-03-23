rootProject.name = "Novaconomy"

include(":novaconomy")
project(":novaconomy").projectDir = rootDir.resolve("plugin")

include(":novaconomy-abstract")
project(":novaconomy-abstract").projectDir = rootDir.resolve("abstraction")

listOf("api", "adventure").forEach {
    include(":novaconomy-$it")
    project(":novaconomy-$it").projectDir = rootDir.resolve(it)
}

if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
    include(":novaconomy-folia")
    project(":novaconomy-folia").projectDir = rootDir.resolve("folia")
}

mapOf(
    "1_8_R1" to 8,
    "1_8_R2" to 8,
    "1_8_R3" to 8,
    "1_9_R1" to 8,
    "1_9_R2" to 8,
    "1_10_R1" to 8,
    "1_11_R1" to 8,
    "1_12_R1" to 8,
    "1_13_R1" to 8,
    "1_13_R2" to 8,
    "1_14_R1" to 8,
    "1_15_R1" to 8,
    "1_16_R1" to 8,
    "1_16_R2" to 8,
    "1_16_R3" to 8,
    "1_17_R1" to 16,
    "1_18_R1" to 17,
    "1_18_R2" to 17,
    "1_19_R1" to 17,
    "1_19_R2" to 17,
    "1_19_R3" to 17,
    "1_20_R1" to 17,
    "1_20_R2" to 17,
    "1_20_R3" to 17
).forEach {
    val id = it.key
    val minJava = it.value

    if (JavaVersion.current().isCompatibleWith(JavaVersion.toVersion(minJava))) {
        include(":novaconomy-$id")
        project(":novaconomy-$id").projectDir = rootDir.resolve("nms/$id")
    }
}