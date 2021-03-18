description = "Java port of pymorphy2"

dependencies {
    compile("commons-io:commons-io:2.4")
    compile("org.noggit:noggit:0.8")
    compile("com.google.guava:guava:23.0")

    compile(project(":dawg"))

    testCompile("junit:junit:4.11")
    testCompile(project(":jmorphy2-dicts-ru"))
    testCompile(project(":jmorphy2-dicts-uk"))
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
    extendsFrom(configurations.testCompile.get())
}

artifacts {
    add("tests", tasks.getByName("jarTest"))
}
