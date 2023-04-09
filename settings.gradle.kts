rootProject.name = "Novaconomy"

include(":novaconomy")
project(":novaconomy").projectDir = rootDir.resolve("plugin")

include(":novaconomy-api")
project(":novaconomy-api").projectDir = rootDir.resolve("api")

include(":novaconomy-abstract")
project(":novaconomy-abstract").projectDir = rootDir.resolve("abstraction")

listOf(
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
).forEach {
    include(":novaconomy-$it")
    project(":novaconomy-$it").projectDir = rootDir.resolve("nms/$it")
}