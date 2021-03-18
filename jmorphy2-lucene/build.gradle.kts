description = "Stemmer and tagger based on jmorphy2 for Lucene"

dependencies {
    compile("org.apache.lucene:lucene-core:${project.getLuceneVersion()}")
    compile("org.apache.lucene:lucene-analyzers-common:${project.getLuceneVersion()}")
    testCompile("org.apache.lucene:lucene-test-framework:${project.getLuceneVersion()}")

    compile(project(":jmorphy2-core"))
    compile(project(":jmorphy2-nlp"))
    compile(project(":jmorphy2-dicts-ru"))

    testCompile(project(":jmorphy2-core").dependencyProject.sourceSets["test"].output)
}