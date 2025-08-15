defaultTasks("build")

subprojects {
    apply(plugin = "java-library")

    group = "biz.donvi"
    version = "0.14.9"
    // Configure Java toolchain for all subprojects (avoid using version catalog here)
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenLocal()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://maven.enginehub.org/repo/")
        maven("https://repo.mikeprimm.com/")
    }

    // Per-module dependencies are declared in each module's build.gradle.kts

    tasks.withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
