import java.io.File
import java.net.URI;
import org.gradle.api.Project
import org.gradle.api.file.RelativePath


fun Project.downloadAndUnpackDicts(packageName: String, version: String, md5: String, destPath: Unit) {
    val fullName = "${packageName}-${version}"
    val filename = "${fullName}.tar.gz"
    val url = "https://pypi.python.org/packages/source/p/${packageName}/${filename}?md5=${md5}"
    val baseDictsPath: String = destPath.toString()

    val versionFile: File = File("${baseDictsPath}/version.txt")
    if (versionFile.exists() && versionFile.readText(Charsets.UTF_8) == version) {
        return
    }
    val dictsDir: File = File("${baseDictsPath}/pymorphy2_dicts")
    if (dictsDir.exists()) {
        dictsDir.delete()
    }
    val dictsFile: File = File("${baseDictsPath}/${filename}")
    if (dictsFile.exists()) {
        dictsFile.delete()
    }
    println("Downloading ${packageName} dicts...")
    dictsDir.mkdirs()
    dictsFile.writeText(URI(url).toURL().readText())

    println("Unpacking ${packageName} dicts...")
    val zipRootDirName: String = packageName.replace("-", "_")

    copy {
        from(tarTree(resources.gzip(dictsFile)))
        into(dictsDir)
        includeEmptyDirs = false
        eachFile {
            val zipRootPath: String = "${fullName}/${zipRootDirName}/data/"
            val strip_segments: Int = zipRootPath.length - zipRootPath.replace("/", "").length
            if (relativePath.pathString.startsWith(zipRootPath)) {
                relativePath = RelativePath(
                    !file.isDirectory(),
                    relativePath.segments.slice(strip_segments..-1).toTypedArray() as String
                )
            } else {
                exclude()
            }
        }
    }
    // Store pymorhpy2 dictionary version
    File("${baseDictsPath}/version.txt").writeText(version)

    dictsFile.delete()
}
