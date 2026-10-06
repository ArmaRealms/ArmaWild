defaultTasks("build")

subprojects {
    apply(plugin = "java-library")

    group = "biz.donvi"
    version = "0.15.0"

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain.languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    repositories {
        mavenLocal()
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
