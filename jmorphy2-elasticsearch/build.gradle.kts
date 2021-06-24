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

configure<org.elasticsearch.gradle.plugin.PluginPropertiesExtension> {
    name = "analysis-jmorphy2"
    description = "Jmorphy2 plugin for ElasticSearch"
    classname = "company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin"
    version = project.version.toString()
    licenseFile = project.file("LICENSE.txt")
    noticeFile = project.file("NOTICE.txt")
}


val libVersion: String = project.getLibraryVersion()

version = "${libVersion}-es${project.getElasticsearchVersion()}"

dependencies {
    implementation(project(":jmorphy2-lucene"))
    implementation(project(":jmorphy2-dicts-ru"))
    implementation(project(":jmorphy2-dicts-uk"))
}

// prior 7.13
tasks.findByName("validateNebulaPom")?.enabled = false
// 7.13 and after
tasks.findByName("validateElasticPom")?.enabled = false
