description = "Java library to read dawg files"

version = getLibraryVersion()

dependencies {
    implementation("commons-io:commons-io:${Versions.commonsIo}")
    implementation("commons-codec:commons-codec:${Versions.commonsCodec}")
}
