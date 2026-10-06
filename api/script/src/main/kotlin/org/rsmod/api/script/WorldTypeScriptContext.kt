package org.rsmod.api.script

import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.events.interact.OpEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.events.EventBus
import org.rsmod.events.KeyedEvent
import org.rsmod.events.SuspendEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.cheat.CheatHandler
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.world.WorldTypeGate
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Builds the [ScriptContext] a world-type scoped [PluginScript] registers through.
 *
 * Handlers still live in the shared maps; they are wrapped so they do nothing for a player who is
 * not on one of the script's world types.
 */
public object WorldTypeScriptContext {
    public fun forScript(
        script: PluginScript,
        context: ScriptContext,
    ): ScriptContext {
        if (script.worldTypes.isEmpty()) {
            return context
        }
        val gate = WorldTypeGate(script.worldTypes)
        return ScriptContext(
            eventBus = GatedEventBus(context.eventBus, gate, script.worldTypeDenyMessage),
            cheatCommandMap =
                GatedCheatCommandMap(context.cheatCommandMap, gate, script.worldTypeDenyMessage),
            engineQueueCache = context.engineQueueCache,
        )
    }

    /** Phrased for what was clicked, since `OpEvent` covers npcs, locs and objs alike. */
    private fun denyMessageFor(event: Any, gate: WorldTypeGate, override: String?): String {
        if (override != null) {
            return override
        }
        val modes = gate.worldTypes.joinToString(" or ") { it.label }
        return when (event) {
            is NpcEvents.Op ->
                "${event.npc.name} doesn't seem to want to talk to you outside $modes."
            else -> "That only works in $modes."
        }
    }

    /** Gated when the event identifies a player; world-level events run regardless. */
    private class GatedEventBus(
        parent: EventBus,
        private val gate: WorldTypeGate,
        private val denyMessage: String?,
    ) : EventBus(parent.unbound, parent.keyed, parent.suspend) {
        override fun <T : UnboundEvent> subscribeUnbound(type: Class<T>, action: T.() -> Unit) {
            super.subscribeUnbound(type) {
                val event = this
                if (event !is PlayerEvent || gate.allows(event.player.worldType)) {
                    action.invoke(event)
                }
            }
        }

        override fun <T : KeyedEvent> subscribeKeyed(
            type: Class<T>,
            id: Long,
            action: T.() -> Unit,
        ) {
            super.subscribeKeyed(type, id) {
                val event = this
                if (event !is PlayerEvent || gate.allows(event.player.worldType)) {
                    action.invoke(event)
                }
            }
        }

        override fun <R, T : SuspendEvent<R>> subscribeSuspend(
            type: Class<T>,
            id: Long,
            action: suspend R.(T) -> Unit,
        ) {
            super.subscribeSuspend(type, id) { event ->
                val receiver = this
                when {
                    receiver !is ProtectedAccess -> action.invoke(receiver, event)
                    gate.allows(receiver.player.worldType) -> action.invoke(receiver, event)
                    // Only a deliberate interaction gets an explanation.
                    event is OpEvent -> receiver.mes(denyMessageFor(event, gate, denyMessage))
                }
            }
        }
    }

    private class GatedCheatCommandMap(
        parent: CheatCommandMap,
        private val gate: WorldTypeGate,
        private val denyMessage: String?,
    ) : CheatCommandMap(parent.commands) {
        override fun put(name: String, handler: CheatHandler) {
            val action = handler.action
            // `registrant` attributes the command to the plugin's classloader for unloading.
            super.put(
                name,
                CheatHandler(
                    desc = handler.desc,
                    action = {
                        if (gate.allows(player.worldType)) {
                            action(this)
                        } else {
                            player.mes(denyMessageFor(this, gate, denyMessage))
                        }
                    },
                    registrant = handler.registrant,
                ),
            )
        }
    }
}
