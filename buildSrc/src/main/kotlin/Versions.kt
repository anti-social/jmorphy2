import org.gradle.api.Project


object Versions {
    val group = "company.evo.jmorphy2"
    val sourceCompatibility = "1.8"
    val targetCompatibility = "1.8"
    val defaultLuceneVersion = "8.7.0"
    val esLuceneVersions = mapOf(
            "6.6" to "7.6.0",
            "6.7" to "7.7.0",
            "6.8" to "7.7.3",
            "7.0" to "8.0.0",
            "7.1" to "8.0.0",
            "7.2" to "8.0.0",
            "7.3" to "8.1.0",
            "7.4" to "8.2.0",
            "7.5" to "7.3.0",
            "7.6" to "7.4.0",
            "7.7" to "7.5.1",
            "7.8" to "7.5.1",
            "7.9" to "8.6.2",
            "7.10" to "8.7.0",
            "7.11" to defaultLuceneVersion
    )
}

fun Project.getVersion(): String {
    return rootProject.file("project.version").readLines().first().toUpperCase().removeSuffix("-SNAPSHOT")
}

fun Project.getElasticsearchDefaultVersion(): String {
    return rootProject.file("es.version").readLines().first()
}

fun Project.getElasticsearchVersion(): String {
    return properties["esVersion"]?.toString() ?: getElasticsearchDefaultVersion()
}


fun Project.getLuceneVersion(): String {
    return Versions.esLuceneVersions.getOrDefault(
            getElasticsearchVersion(),
            Versions.esLuceneVersions.getOrDefault(
                    getElasticsearchVersion().substring(0, 2),
                    Versions.defaultLuceneVersion
            )
    )
}