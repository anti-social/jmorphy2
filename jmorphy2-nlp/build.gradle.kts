description = "NLP based on pymorphy2 dictionaries"

dependencies {
    api(project(":jmorphy2-core"))
    implementation("com.google.guava:guava:${Versions.guava}")

    testImplementation("junit:junit:${Versions.junit}")
    testImplementation(project(":jmorphy2-dicts-ru"))
    testImplementation(project(":jmorphy2-core").dependencyProject.sourceSets["test"].output)
}

tasks.withType<Test> {
    exclude("**/*Benchmark*")
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
    }
}