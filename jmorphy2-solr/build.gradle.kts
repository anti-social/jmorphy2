description = "Solr stemmer and tagger based on jmorphy2"

repositories {
  mavenCentral()
  maven(url = "http://maven.restlet.org")
}

dependencies {
    compile("org.apache.solr:solr-core:${project.getLuceneVersion()}")

    compile(project(":jmorphy2-lucene"))
    testCompile(files(project(":jmorphy2-core")))
}
