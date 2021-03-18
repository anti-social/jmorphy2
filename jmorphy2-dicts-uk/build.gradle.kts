description = "Ukrainian pymorphy2 dictionaries"

tasks.register("fetchPymorphy2Dicts") {
    project(":jmorphy2-core").downloadAndUnpackDicts(
        "pymorphy2-dicts-uk",
        "2.4.1.1.1460299261",
        "f193a4ac7a8e6124e6fd8846f06ccca0",
        sourceSets.main.configure {
            resources.srcDirs().includes.addAll(
                arrayOf("company", "evo", "jmorphy2", "uk")
            ).toString()
        }
    )
}
