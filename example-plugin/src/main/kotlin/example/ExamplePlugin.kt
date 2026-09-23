package example

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Starter template for an external plugin (see `docs/external-plugins.md`).
 *
 * 1. Build with `gradlew :example-plugin:jar`, then copy
 *    `example-plugin/build/libs/example-plugin.jar` into `plugins/` and load it in-game with
 *    `::loadplugin example-plugin`. Typing `::example` should reply "Example v1!".
 * 2. To test reload: bump [REVISION], rebuild, copy the jar over the old one in `plugins/`, and
 *    run `::pluginreload example-plugin` without restarting the server. `::example` should now
 *    reply "Example v2!" — the old command handler was unregistered and replaced, not duplicated.
 *
 * Edit freely from here.
 */
class ExamplePlugin @Inject constructor() : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        logger.info { "Example plugin started (revision $REVISION)." }
        onCommand("example") {
            desc = "Example plugin test command."
            cheat { player.mes("Example v$REVISION! (from the external example plugin)") }
        }
    }

    override fun ScriptContext.shutdown() {
        // Called right before this plugin is unregistered during a reload/unload. Only needed if
        // startup() does something beyond registering handlers (spawning entities, mutating
        // shared state, starting coroutines) that you want cleaned up on reload.
    }

    private companion object {
        const val REVISION = 1
    }
}
