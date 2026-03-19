package dev.openrune

import java.nio.file.Path
import java.nio.file.Paths

object DirectoryConstants {
    val DATA_PATH: Path = Paths.get(".data")
    val CACHE_PATH: Path = DATA_PATH.resolve("cache")
}
