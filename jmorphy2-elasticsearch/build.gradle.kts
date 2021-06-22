import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.tasks.RecordingCopyTask
import java.util.Date


buildscript {
    repositories {
        mavenCentral()
        maven(url="http://maven.restlet.org")
        jcenter()
    }
    dependencies {
        classpath("org.elasticsearch.gradle:build-tools:${project.getElasticsearchVersion()}")
    }
}

plugins {
    id("com.jfrog.bintray") version "1.8.4"
}
apply(plugin="idea")
apply(plugin="elasticsearch.esplugin")

configure<org.elasticsearch.gradle.plugin.PluginPropertiesExtension> {
    name = "analysis-jmorphy2"
    description = "Jmorphy2 plugin for ElasticSearch"
    classname = "company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin"
    version = project.version.toString()
    licenseFile = project.file("LICENSE.txt")
    noticeFile = project.file("NOTICE.txt")
}


val libVersion: String = project.getVersion().toString()

version = "${libVersion}-es${project.getElasticsearchVersion()}"

ext {
    val dependenciesDir = "${buildDir}/dependencies"
}

dependencies {
    compile(project(":jmorphy2-lucene"))
    compile(project(":jmorphy2-dicts-ru"))
    compile(project(":jmorphy2-dicts-uk"))
}

tasks.findByName("validateNebulaPom")?.enabled = false

bintray {
    val user = project.properties["bintrayUser"]?.toString() ?: System.getenv("BINTRAY_USER")
    val key = project.properties["bintrayApiKey"]?.toString() ?: System.getenv("BINTRAY_API_KEY")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "elasticsearch"
        name = project.name
        userOrg = "evo"
        setLicenses("Apache-2.0")
        setLabels("elasticsearch-plugin", "analysis-plugin", "jmorphy2", "pymorphy2")
        vcsUrl = "https://github.com/anti-social/jmorphy2.git"
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = libVersion
            released = Date().toString()
            vcsTag = "v$libVersion"
        })
    })
    filesSpec(delegateClosureOf<RecordingCopyTask> {
        val distributionsDir = buildDir.resolve("distributions")
        from(distributionsDir)
        include("*-${version}.zip")
        into(".")
    })

    val publish = true
    val dryRun = project.hasProperty("bintrayDryRun")
}
