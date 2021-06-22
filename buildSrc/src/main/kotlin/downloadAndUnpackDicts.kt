import java.io.File
import java.net.URI;

import org.gradle.api.DefaultTask
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType

abstract class Pymorphy2Dicts : DefaultTask() {
    @get:Input
    abstract val lang: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val md5sum: Property<String>

    @OutputDirectory
    val outputDir = project.extensions
        .getByType<SourceSetContainer>()["main"]
        .resources
        .srcDirs.iterator().next()

    @TaskAction
    fun apply() {
        val packageName = "pymorphy2-dicts-${lang.get()}"
        val fullName = "${packageName}-${version.get()}"
        val filename = "${fullName}.tar.gz"
        val url = "https://pypi.python.org/packages/source/p/${packageName}/${filename}?md5=${md5sum.get()}"

        println("Downloading $url")
        val dictsArchive = temporaryDir.resolve(filename)
        if (dictsArchive.exists()) {
            dictsArchive.delete()
        }
        URI(url).toURL().openStream().copyTo(dictsArchive.outputStream())
        println("Saved dicts archive into $dictsArchive")

        println("Unpacking $dictsArchive")
        try {
            val baseDictsDir = project.group.toString().split('.')
                .fold(outputDir, File::resolve)
                .resolve(lang.get())
            val dictsDataDir = baseDictsDir.resolve("pymorphy2_dicts")
            if (dictsDataDir.exists()) {
                dictsDataDir.delete()
            }
            val zipRootDirName: String = packageName.replace("-", "_")

            project.run {
                copy {
                    from(tarTree(resources.gzip(dictsArchive)))
                    into(dictsDataDir)
                    includeEmptyDirs = false
                    eachFile {
                        val zipRootPath = "${fullName}/${zipRootDirName}/data/"
                        val stripSegments = zipRootPath.length - zipRootPath.replace("/", "").length
                        if (relativePath.pathString.startsWith(zipRootPath)) {
                            relativePath = RelativePath(
                                file.isFile,
                                *relativePath.segments.slice(stripSegments until relativePath.segments.size)
                                    .toTypedArray()
                            )
                        } else {
                            exclude()
                        }
                    }
                }
            }
            println("Unpacked dicts archive into $dictsDataDir")

            // Store pymorhpy2 dictionary version
            baseDictsDir.resolve("version.txt").writeText(version.get())
        } finally {
            dictsArchive.delete()
        }
    }
}
