description = "Russian pymorphy2 dictionaries"

val dictsVersion = "2.4.404381.4453942"
version = "${getLibraryVersion()}-$dictsVersion"

val fetchDicts = tasks.register<Pymorphy2Dicts>("fetchPymorphy2Dicts") {
    lang.set("ru")
    version.set(dictsVersion)
    md5sum.set("bdd5d23660f2ad5e8ec2721743a8b419")
}

tasks.named("processResources") {
    dependsOn(fetchDicts)
}
