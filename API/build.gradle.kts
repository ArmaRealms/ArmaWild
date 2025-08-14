plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get()))
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${libs.versions.paper}")
}
