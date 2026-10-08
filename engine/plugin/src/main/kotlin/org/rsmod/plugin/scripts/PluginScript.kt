package org.rsmod.plugin.scripts

import org.rsmod.game.world.WorldType

public abstract class PluginScript {
    /**
     * The world types this script belongs to; empty - the default - means all of them.
     *
     * ```
     * override val worldTypes = listOf(WorldType.RAGING_ECHOES_LEAGUE)
     * ```
     *
     * A world serving none of them never registers the script at all; one that serves any registers
     * it, but its player-facing handlers only fire for players on a listed mode.
     *
     * See `docs/world-type-plugins.md`.
     */
    public open val worldTypes: Collection<WorldType>
        get() = emptyList()

    /**
     * What to tell a player who interacts with something this script owns while not on one of its
     * [worldTypes]. `null` uses a default phrased for whatever was clicked.
     *
     * Sent only for a deliberate interaction; login, movement and timers stay silent.
     */
    public open val worldTypeDenyMessage: String?
        get() = null

    public abstract fun ScriptContext.startup()

    /**
     * Called before this script's handlers are unregistered, when it's being unloaded/reloaded as
     * an external plugin (see `ExternalPluginLoader`). Never called for built-in plugins, which
     * never unload.
     *
     * Unregistering event/command handlers is handled automatically; this hook exists only for
     * side effects `startup()` caused that the engine has no way to know about or undo on its own
     * — entities this script spawned, shared state it mutated, background coroutines it started.
     * There's no default way to reverse any of that, so if a script does something like that and
     * wants clean reloads, override this to clean it up. The default does nothing.
     */
    public open fun ScriptContext.shutdown() {}
}
