group = "company.evo.jmorphy2"

subprojects {
    apply(plugin="java")
    apply(plugin="maven")

    group = rootProject.group

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        exclude("**/*Benchmark*")
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = Versions.java
        targetCompatibility = Versions.java
    }
}
