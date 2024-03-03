import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.sonarqube") version "4.0.0.2929"
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false

    java
    `maven-publish`
    `java-library`
    jacoco
}

val pGroup = "us.teaminceptus.novaconomy"
val pVersion = "1.9.2-SNAPSHOT"
val pAuthor = "Team-Inceptus"

sonarqube {
    properties {
        property("sonar.projectKey", "${pAuthor}_Novaconomy")
        property("sonar.organization", "team-inceptus")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

allprojects {
    group = pGroup
    version = pVersion
    description = "Multi-Economy and Business Plugin made for Spigot 1.8+"

    apply(plugin = "maven-publish")
    apply<JavaPlugin>()
    apply<JavaLibraryPlugin>()

    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://jitpack.io")
        maven("https://plugins.gradle.org/m2/")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://oss.sonatype.org/content/repositories/central")

        maven("https://repo.codemc.org/repository/nms/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
        maven("https://libraries.minecraft.net/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.essentialsx.net/releases/")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = pGroup
                artifactId = project.name
                version = pVersion

                pom {
                    description.set(project.description)
                    licenses {
                        license {
                            name.set("GPL-3.0")
                            url.set("https://github.com/Team-Inceptus/Novaconomy/blob/master/LICENSE")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://Team-Inceptus/Novaconomy.git")
                        developerConnection.set("scm:git:ssh://Team-Inceptus/Novaconomy.git")
                        url.set("https://github.com/Team-Inceptus/Novaconomy")
                    }
                }

                from(components["java"])
            }
        }

        repositories {
            maven {
                credentials {
                    username = System.getenv("JENKINS_USERNAME")
                    password = System.getenv("JENKINS_PASSWORD")
                }

                val releases = "https://repo.codemc.io/repository/maven-releases/"
                val snapshots = "https://repo.codemc.io/repository/maven-snapshots/"
                url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshots else releases)
            }
        }
    }
}

val jvmVersion = JavaVersion.VERSION_1_8

subprojects {
    apply<JacocoPlugin>()
    apply(plugin = "org.sonarqube")
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        compileOnly(kotlin("stdlib"))
        compileOnly("org.jetbrains:annotations:24.1.0")

        testImplementation("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")
        testImplementation("net.md-5:bungeecord-chat:1.20-R0.2")
        testImplementation("org.mockito:mockito-core:5.10.0")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    }

    java {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }

    tasks {
        assemble {
            dependsOn(compileKotlin)
        }

        compileJava {
            options.encoding = "UTF-8"
            options.isWarnings = false
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        }

        compileKotlin {
            kotlinOptions {
                jvmTarget = jvmVersion.toString()
                freeCompilerArgs = listOf("-Xjvm-default=all")
            }
        }

        jacocoTestReport {
            dependsOn(test)

            reports {
                xml.required.set(false)
                csv.required.set(false)

                html.required.set(true)
                html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
            }
        }

        test {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
            finalizedBy(jacocoTestReport)

            sourceSets["test"].allSource.srcDir("src/main/resources")
        }

        javadoc {
            enabled = false
            options.encoding = "UTF-8"
            options.memberLevel = JavadocMemberLevel.PROTECTED
        }

        jar.configure {
            dependsOn("shadowJar")
            archiveClassifier.set("dev")
        }
        withType<ShadowJar> {
            manifest {
                attributes(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to pAuthor
                )
            }

            relocate("revxrsal.commands", "us.teaminceptus.novaconomy.shaded.lamp")
            relocate("org.bstats", "us.teaminceptus.novaconomy.shaded.bstats")
            relocate("com.jeff_media.updatechecker", "us.teaminceptus.novaconomy.shaded.updatechecker")

            archiveFileName.set("${project.name}-${project.version}.jar")
            archiveClassifier.set("")
        }
    }

    artifacts {
        add("default", tasks.getByName<ShadowJar>("shadowJar"))
    }
}