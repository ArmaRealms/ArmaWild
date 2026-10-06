defaultTasks("build")

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = providers.gradleProperty("group")
        .orElse("com.github.ArmaRealms")
        .get()
    version = providers.gradleProperty("version")
        .orElse("1.0.0")
        .get()

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain.languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(25))
        withSourcesJar()
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

    extensions.configure<org.gradle.api.publish.PublishingExtension> {
        publications {
            create<org.gradle.api.publish.maven.MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }

    // Per-module dependencies are declared in each module's build.gradle.kts

    tasks.withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
