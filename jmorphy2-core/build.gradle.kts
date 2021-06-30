description = "Java port of pymorphy2"

dependencies {
    implementation("commons-io:commons-io:${Versions.commonsIo}")
    implementation("org.noggit:noggit:${Versions.noggit}")

    implementation(project(":dawg"))

    testImplementation("junit:junit:${Versions.junit}")
    testImplementation(project(":jmorphy2-dicts-ru"))
    testImplementation(project(":jmorphy2-dicts-uk"))
}
tasks.withType<Test> {
    exclude("**/*Benchmark*")
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
    }
}

tasks.register<Jar>(
    name = "jarTest"
) {
    dependsOn("testClasses")
    classifier = "tests"
    from(sourceSets.test)
}

configurations.register("tests") {
    extendsFrom(configurations.testImplementation.get())
}

artifacts {
    add("tests", tasks.getByName("jarTest"))
}
