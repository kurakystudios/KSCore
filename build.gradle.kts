plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    // Se exponen como `api` para que los consumidores (por ejemplo KsEconomia
    // sombreando KsCore) reciban estas dependencias como transitivas y las
    // puedan relocalizar bajo su propio paquete `.libs.*`.
    api("com.github.ben-manes.caffeine:caffeine:3.1.8")
    api("com.zaxxer:HikariCP:6.2.1")
    api("org.xerial:sqlite-jdbc:3.47.1.0")
    api("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    api("com.electronwill.night-config:toml:3.8.1")
    api("com.electronwill.night-config:json:3.8.1")

    // Driver de MongoDB es opcional: se declara compileOnly y el sistema de datos lo
    // detectará por reflexión en Fase 6. Sombreado sólo si se activa en config.
    compileOnly("org.mongodb:mongodb-driver-sync:5.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // paper-api es compileOnly en producción pero se necesita en los tests para
    // acceder a los tipos de Adventure y la configuración YAML de Bukkit.
    testImplementation("io.papermc.paper:paper-api:26.2.build.+")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

val paqueteRelocado = "studio.kuraky.kSCore.libs"

tasks {
    // El jar "normal" (sin sombrear) es el que se publica como artefacto
    // Maven para que los consumidores puedan relocar el core dentro de su
    // propio jar. El plugin propio KsCore sigue publicando el shadowJar.
    jar {
        archiveClassifier.set("")
    }

    shadowJar {
        archiveClassifier.set("all")
        mergeServiceFiles()
        relocate("com.github.benmanes.caffeine", "$paqueteRelocado.caffeine")
        relocate("com.zaxxer.hikari", "$paqueteRelocado.hikari")
        relocate("org.sqlite", "$paqueteRelocado.sqlite")
        relocate("org.mariadb.jdbc", "$paqueteRelocado.mariadb")
        relocate("com.electronwill.nightconfig", "$paqueteRelocado.nightconfig")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = "ks-core"
            version = project.version.toString()
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
