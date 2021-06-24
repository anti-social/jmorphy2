group = "company.evo.jmorphy2"

plugins {
    java
}

subprojects {
    apply(plugin="java-library")

    group = rootProject.group

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        exclude("**/*Benchmark*")
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(Versions.java.majorVersion))
        }
    }
}
