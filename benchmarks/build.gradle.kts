plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version Versions.jmhGradlePlugin
}

dependencies {
    jmhImplementation(project(":jmorphy2-core"))
    jmhImplementation(project(":jmorphy2-core").dependencyProject.sourceSets["test"].output)
    jmhImplementation(project(":jmorphy2-dicts-ru"))
}

jmh {
    fork.set(2)
    warmup.set("1s")
    warmupIterations.set(2)
    timeOnIteration.set("2s")
    iterations.set(4)
}
