package dev.openrune

fun main() {
    val dir = DirectoryConstants.CS2_PATH.toFile()
    if (!dir.exists()) {
        println("No CS2 directory at ${dir.absolutePath}")
        return
    }
    if (!dir.deleteRecursively()) {
        error("Failed to delete ${dir.absolutePath}")
    }
    println("Deleted CS2 directory: ${dir.absolutePath}")
}
