package org.rsmod.plugin.scripts

public abstract class PluginScript {
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
