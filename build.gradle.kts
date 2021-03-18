subprojects {
    apply(plugin="java")
    apply(plugin="maven")

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
