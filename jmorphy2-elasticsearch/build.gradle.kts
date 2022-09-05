import java.nio.file.Paths

buildscript {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("org.elasticsearch.gradle:build-tools:${project.getElasticsearchVersion()}")
        classpath("com.netflix.nebula:gradle-ospackage-plugin:8.5.6")
    }
}

apply(plugin = "idea")
apply(plugin = "elasticsearch.esplugin")
apply(plugin = "nebula.ospackage")

val pluginName = "analysis-jmorphy2"
configure<org.elasticsearch.gradle.plugin.PluginPropertiesExtension> {
    name = pluginName
    description = "Jmorphy2 plugin for ElasticSearch"
    classname = "company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin"
    version = project.version.toString()
    licenseFile = project.file("LICENSE.txt")
    noticeFile = project.file("NOTICE.txt")
}

java {
    sourceCompatibility = Versions.java
    targetCompatibility = Versions.java
}

val libVersion: String = project.getLibraryVersion()

version = "${libVersion}-es${project.getElasticsearchVersion()}"

val versions = org.elasticsearch.gradle.VersionProperties.getVersions() as Map<String, String>

configurations {
    create("shadow")
    val shadowClasses = create("shadowClasses")
    // We should prevent from getting generated shadow classes into testRuntimeClasspath
    // as in this case jar hell error will be raised
    compileClasspath {
        extendsFrom(shadowClasses)
    }
}

val shadowClassesDir = layout.buildDirectory.dir("generated/shadow-classes/java/main").get()

dependencies {
    implementation(project(":jmorphy2-lucene"))
    implementation(project(":jmorphy2-dicts-ru"))
    implementation(project(":jmorphy2-dicts-uk"))

    testImplementation("org.apache.logging.log4j:log4j-core:${versions["log4j"]}")

    add("shadow", "com.github.ben-manes.caffeine:caffeine:${Versions.caffeine}")
    add("shadowClasses", files(shadowClassesDir) {
        builtBy("generateShadowClasses")
    })
}

tasks.register<Copy>("generateShadowClasses") {
    configurations.named("shadow").get()
        .forEach { artifact ->
            val depName = artifact.nameWithoutExtension.substringBeforeLast('-')
            from(zipTree(artifact)) {
                exclude("module-info.class")
                this.eachFile {
                    val segments = relativePath.segments
                    if (segments.isEmpty()) {
                        return@eachFile
                    }
                    if (segments[0] != "META-INF") {
                        return@eachFile
                    }
                    val newSegments = arrayOf("META-INF-$depName") + segments.slice(1 until segments.size)
                    relativePath = RelativePath(
                        relativePath.isFile,
                        *newSegments
                    )
                }
            }
        }
    into(shadowClassesDir)
}

tasks.register<Copy>("copyShadowClasses") {
    from(shadowClassesDir)
    into(sourceSets.main.get().java.classesDirectory)

    dependsOn("generateShadowClasses")
}

tasks.named("classes") {
    dependsOn("copyShadowClasses")
}

// prior 7.13
tasks.findByName("validateNebulaPom")?.enabled = false
// 7.13 and after
tasks.findByName("validateElasticPom")?.enabled = false

tasks.register("deb", com.netflix.gradle.plugins.deb.Deb::class) {
    dependsOn("bundlePlugin")

    packageName = "elasticsearch-$pluginName-plugin"

    requires("elasticsearch", versions["elasticsearch"])
        .or("elasticsearch-oss", versions["elasticsearch"])

    from(zipTree(tasks["bundlePlugin"].outputs.files.singleFile))

    val esHome = project.properties["esHome"] ?: "/usr/share/elasticsearch"
    into("$esHome/plugins/${pluginName}")

    doLast {
        if (properties.containsKey("assembledInfo")) {
            val distDir = Paths.get(buildDir.path, "distributions")
            distDir.resolve("assembled-deb.filename").toFile()
                .writeText(assembleArchiveName())
        }
    }
}
