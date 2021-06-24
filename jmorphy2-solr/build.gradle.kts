description = "Solr stemmer and tagger based on jmorphy2"

repositories {
  mavenCentral()
}

dependencies {
    compile("org.apache.solr:solr-core:${project.getLuceneVersion()}")

    compile(project(":jmorphy2-lucene"))
    testCompile(files(project(":jmorphy2-core")))
}
