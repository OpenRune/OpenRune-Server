package org.rsmod.plugin.loader

import com.google.inject.Guice
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("external-plugin-loader")
class ExternalPluginBootLifecycleTest {
    @Test
    fun `unloading a boot loaded plugin runs its shutdown hook exactly once`() {
        val plugins = File("plugins").apply { mkdirs() }
        val source = Files.createTempDirectory(plugins.toPath(), "boot-lifecycle-").toFile()
        val context = ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache())
        try {
            File(source, "plugin.properties").writeText(
                "name=Lifecycle fixture\ndescription=Boot shutdown regression\nrevision=1\nauthor=Tests\n",
            )
            val fixture = BootLifecycleFixture::class.java
            val classPath = fixture.name.replace('.', '/') + ".class"
            val classFile = File(source, classPath)
            classFile.parentFile.mkdirs()
            fixture.getResourceAsStream("/$classPath")!!.use { input ->
                classFile.outputStream().use(input::copyTo)
            }

            val script =
                ExternalPluginLoader.loadScriptsAtBoot(Guice.createInjector())
                    .filterIsInstance<BootLifecycleFixture>()
                    .single()
            with(script) { context.startup() }
            assertTrue(script.running)

            assertTrue(ExternalPluginLoader.unload(source.name, context))
            assertFalse(script.running)
            assertEquals(1, script.shutdowns)
            assertFalse(ExternalPluginLoader.unload(source.name, context))
            assertEquals(1, script.shutdowns)
        } finally {
            ExternalPluginLoader.unload(source.name, context)
            source.deleteRecursively()
        }
    }
}

class BootLifecycleFixture : PluginScript() {
    var running = false
    var shutdowns = 0

    override fun ScriptContext.startup() {
        running = true
    }

    override fun ScriptContext.shutdown() {
        running = false
        shutdowns++
    }
}
