description = "Stemmer and tagger based on jmorphy2 for Lucene"

dependencies {
    implementation("org.apache.lucene:lucene-core:${project.getLuceneVersion()}")
    implementation("org.apache.lucene:lucene-analyzers-common:${project.getLuceneVersion()}")
    testImplementation("org.apache.lucene:lucene-test-framework:${project.getLuceneVersion()}")

    api(project(":jmorphy2-core"))
    api(project(":jmorphy2-nlp"))
    implementation(project(":jmorphy2-dicts-ru"))
    implementation(project(":jmorphy2-dicts-uk"))

    testImplementation(project(":jmorphy2-core").dependencyProject.sourceSets["test"].output)
}