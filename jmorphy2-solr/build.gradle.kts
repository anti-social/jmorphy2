description = "Solr stemmer and tagger based on jmorphy2"

version = getLibraryVersion()

repositories {
  mavenCentral()
}

dependencies {
    implementation("org.apache.solr:solr-core:${project.getLuceneVersion()}")

    implementation(project(":jmorphy2-lucene"))
    testImplementation(files(project(":jmorphy2-core")))
}
