description = "NLP based on pymorphy2 dictionaries"

dependencies {
    compile(project(":jmorphy2-core"))

    testCompile("junit:junit:4.11")
    testCompile(project(":jmorphy2-dicts-ru"))
    testCompile(project(":jmorphy2-core").dependencyProject.sourceSets["test"].output)
}

tasks.withType<Test> {
    exclude("**/*Benchmark*")
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
    }
}