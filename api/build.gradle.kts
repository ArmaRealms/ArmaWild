plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.get().toInt()))
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    add("compileOnly", libs.paper)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("JakesRTP-API")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(rootProject.layout.projectDirectory.dir("out"))
}
