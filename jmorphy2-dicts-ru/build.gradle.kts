description = "Russian pymorphy2 dictionaries"

tasks.register("fetchPymorphy2Dicts") {
    project(":jmorphy2-core").downloadAndUnpackDicts(
        "pymorphy2-dicts-ru",
        "2.4.404381.4453942",
        "bdd5d23660f2ad5e8ec2721743a8b419",
        sourceSets.main.configure {
            resources.srcDirs().includes.addAll(
                arrayOf("company", "evo", "jmorphy2", "ru")
            ).toString()
        }
    )
}
