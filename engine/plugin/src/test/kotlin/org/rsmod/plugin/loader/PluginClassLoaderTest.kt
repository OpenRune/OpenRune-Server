package org.rsmod.plugin.loader

import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.tools.ToolProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PluginClassLoaderTest {
    @TempDir lateinit var temporary: Path

    @Test
    fun `directory callbacks and resources retain their loaded revision until reload`() {
        checkReplacement(jar = false)
    }

    @Test
    fun `jar callbacks and resources retain their loaded revision until reload`() {
        checkReplacement(jar = true)
    }

    private fun checkReplacement(jar: Boolean) {
        val source = temporary.resolve(if (jar) "plugin.jar" else "plugin")
        compileSource(source, "old", jar)
        val first = PluginClassLoader.create(source.toFile(), javaClass.classLoader)
        val snapshot = first.source.parentFile
        try {
            val callback = first.loadClass("fixture.Callback").getConstructor().newInstance()
            compileSource(source, "new", jar)
            assertEquals("old", (callback as Supplier<*>).get())
            assertEquals("old", first.getResourceAsStream("revision.txt")!!.bufferedReader().use { it.readText() })
            PluginClassLoader.create(source.toFile(), javaClass.classLoader).use { replacement ->
                val reloaded = replacement.loadClass("fixture.Callback").getConstructor().newInstance()
                assertEquals("new", (reloaded as Supplier<*>).get())
                assertEquals("new", replacement.getResourceAsStream("revision.txt")!!.bufferedReader().use { it.readText() })
            }
            assertEquals("old", callback.get())
        } finally {
            first.close()
        }
        assertFalse(snapshot.exists())
    }

    private fun compileSource(destination: Path, revision: String, jar: Boolean) {
        val build = Files.createTempDirectory(temporary, "compile-")
        val source = build.resolve("Callback.java")
        Files.writeString(
            source,
            """
            package fixture;
            public class Callback implements java.util.function.Supplier<String> {
                public String get() { return new LazyHelper().value(); }
            }
            class LazyHelper {
                String value() { return "$revision"; }
            }
            """.trimIndent(),
        )
        val classes = build.resolve("classes")
        Files.createDirectories(classes)
        val compiler = ToolProvider.getSystemJavaCompiler()
        assertEquals(0, compiler.run(null, null, null, "-d", classes.toString(), source.toString()))
        Files.writeString(classes.resolve("revision.txt"), revision)
        if (jar) {
            JarOutputStream(Files.newOutputStream(destination)).use { output ->
                Files.walk(classes).use { paths ->
                    paths.filter(Files::isRegularFile).forEach { file ->
                        output.putNextEntry(JarEntry(classes.relativize(file).toString().replace('\\', '/')))
                        Files.copy(file, output)
                        output.closeEntry()
                    }
                }
            }
        } else {
            classes.toFile().copyRecursively(destination.toFile(), overwrite = true)
        }
    }
}
