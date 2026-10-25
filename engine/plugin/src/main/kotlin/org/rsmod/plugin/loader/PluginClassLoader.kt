package org.rsmod.plugin.loader

import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.Files

internal class PluginClassLoader private constructor(
    val source: File,
    private val snapshotDirectory: File,
    parent: ClassLoader,
) : URLClassLoader(arrayOf(source.toURI().toURL()), parent) {
    override fun close() {
        try {
            super.close()
        } finally {
            if (!snapshotDirectory.deleteRecursively()) {
                throw IOException("Could not remove plugin snapshot: $snapshotDirectory")
            }
        }
    }

    companion object {
        fun create(source: File, parent: ClassLoader): PluginClassLoader {
            val directory = Files.createTempDirectory("openrune-plugin-").toFile()
            try {
                val snapshot = File(directory, source.name)
                source.copyRecursively(snapshot)
                return PluginClassLoader(snapshot, directory, parent)
            } catch (failure: Throwable) {
                if (!directory.deleteRecursively()) {
                    failure.addSuppressed(IOException("Could not remove plugin snapshot: $directory"))
                }
                throw failure
            }
        }
    }
}
