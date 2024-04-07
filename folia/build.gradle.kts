dependencies {
    api(project(":novaconomy-abstract"))

    compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

description = "Folia API Implementation for Novaconomy"