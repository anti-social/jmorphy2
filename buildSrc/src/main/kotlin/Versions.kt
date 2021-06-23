import org.gradle.api.JavaVersion
import org.gradle.api.Project

data class EsVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0
) : Comparable<EsVersion> {
    companion object {
        fun parse(version: String): EsVersion {
            val versionParts = version.split('.')
            return EsVersion(
                versionParts[0].toInt(),
                versionParts[1].toInt(),
                versionParts[2].toInt()
            )
        }
    }

    override fun toString(): String {
        return "$major.$minor.$patch"
    }

    override fun compareTo(other: EsVersion): Int {
        if (major != other.major) {
            return major.compareTo(other.major)
        }
        if (minor != other.minor) {
            return minor.compareTo(other.minor)
        }
        return patch.compareTo(other.patch)
    }
}

object Versions {
    val java = JavaVersion.VERSION_1_8

    val esLuceneVersions = mapOf(
        EsVersion(6, 6) to "7.6.0",
        EsVersion(6, 7) to "7.7.0",
        EsVersion(6, 8) to "7.7.3",
        EsVersion(7, 0) to "8.0.0",
        EsVersion(7, 1) to "8.0.0",
        EsVersion(7, 2) to "8.0.0",
        EsVersion(7, 3) to "8.1.0",
        EsVersion(7, 4) to "8.2.0",
        EsVersion(7, 5) to "7.3.0",
        EsVersion(7, 6) to "7.4.0",
        EsVersion(7, 7) to "7.5.1",
        EsVersion(7, 8) to "7.5.1",
        EsVersion(7, 9) to "8.6.2",
        EsVersion(7, 10) to "8.7.0",
        EsVersion(7, 11) to "8.7.0",
        EsVersion(7, 12) to "8.8.0"
    )
}

fun Project.getLibraryVersion(): String {
    return rootProject.file("project.version").readLines().first().toUpperCase().removeSuffix("-SNAPSHOT")
}

fun Project.getElasticsearchDefaultVersion(): String {
    return rootProject.file("es.version").readLines().first()
}

fun Project.getElasticsearchVersion(): String {
    return properties["esVersion"]?.toString() ?: getElasticsearchDefaultVersion()
}

fun Project.getLuceneVersion(): String {
    val curEsVersion = EsVersion.parse(getElasticsearchVersion())
    var lastLuceneVersion: String? = null
    for ((esVersion, luceneVersion) in Versions.esLuceneVersions) {
        if (curEsVersion < esVersion) {
            break
        }
        lastLuceneVersion = luceneVersion
    }
    return lastLuceneVersion
        ?: throw IllegalStateException("Invalid Elasticsearch version: $curEsVersion")
}