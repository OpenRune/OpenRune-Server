package org.rsmod.api.death

import org.rsmod.game.world.WorldType

/**
 * [NpcDeathKillHook] only runs for killers on one of [worldTypes].
 *
 * Hooks are Guice set bindings rather than script handlers, so `PluginScript.worldTypes` cannot
 * scope them.
 */
public abstract class WorldTypeKillHook(private val worldTypes: Collection<WorldType>) :
    NpcDeathKillHook {
    final override fun onKill(context: NpcDeathKillContext) {
        if (context.hero.worldType in worldTypes) {
            onScopedKill(context)
        }
    }

    protected abstract fun onScopedKill(context: NpcDeathKillContext)
}

/** [NpcDeathDropHook] only applies to killers on one of [worldTypes]. */
public abstract class WorldTypeDropHook(private val worldTypes: Collection<WorldType>) :
    NpcDeathDropHook {
    final override fun tryConsume(context: NpcDeathDropContext): Boolean =
        context.hero.worldType in worldTypes && tryConsumeScoped(context)

    final override fun suppressesDropTable(context: NpcDeathKillContext): Boolean =
        context.hero.worldType in worldTypes && suppressesScoped(context)

    protected abstract fun tryConsumeScoped(context: NpcDeathDropContext): Boolean

    protected open fun suppressesScoped(context: NpcDeathKillContext): Boolean = false
}
