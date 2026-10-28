plugins {
    java
}

group = "dev.eflite"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Check https://papermc.io/downloads/paper for the exact 26.2 build string and adjust if needed.
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25)) // 26.x requires Java 25
}

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
