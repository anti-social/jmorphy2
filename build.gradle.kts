subprojects {
    apply(plugin="java")
    apply(plugin="maven")
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
    }
    tasks.withType<Test> {
        exclude("**/*Benchmark*")
    }
}
