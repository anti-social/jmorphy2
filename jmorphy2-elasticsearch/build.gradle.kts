import java.nio.file.Paths

buildscript {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("org.elasticsearch.gradle:build-tools:${project.getElasticsearchVersion()}")
    }
}

apply(plugin="idea")
apply(plugin="elasticsearch.esplugin")

plugins {
    id("nebula.ospackage") version Versions.nebula
}

val pluginName = "analysis-jmorphy2"

configure<org.elasticsearch.gradle.plugin.PluginPropertiesExtension> {
    name = pluginName
    description = "Jmorphy2 plugin for ElasticSearch"
    classname = "company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin"
    version = project.version.toString()
    licenseFile = project.file("LICENSE.txt")
    noticeFile = project.file("NOTICE.txt")
}

val libVersion: String = project.getLibraryVersion()

version = "${libVersion}-es${project.getElasticsearchVersion()}"
val esVersions = org.elasticsearch.gradle.VersionProperties.getVersions() as Map<String, String>

val distDir = Paths.get(buildDir.path, "distributions")

dependencies {
    implementation(project(":jmorphy2-lucene"))
    implementation(project(":jmorphy2-dicts-ru"))
    implementation(project(":jmorphy2-dicts-uk"))
}

// prior 7.13
tasks.findByName("validateNebulaPom")?.enabled = false
// 7.13 and after
tasks.findByName("validateElasticPom")?.enabled = false


tasks.named("assemble") {
    dependsOn("deb")
}

tasks.register("deb", com.netflix.gradle.plugins.deb.Deb::class) {
    dependsOn("bundlePlugin")

    packageName = "elasticsearch-${pluginName}-plugin"
    requires("elasticsearch", esVersions["elasticsearch"])
        .or("elasticsearch-oss", esVersions["elasticsearch"])

    from(zipTree(tasks["bundlePlugin"].outputs.files.singleFile))

    val esHome = project.properties["esHome"] ?: "/usr/share/elasticsearch"
    into("$esHome/plugins/${pluginName}")

    doLast {
        if (properties.containsKey("assembledInfo")) {
            distDir.resolve("assembled-deb.filename").toFile()
                .writeText(assembleArchiveName())
        }
    }
}
