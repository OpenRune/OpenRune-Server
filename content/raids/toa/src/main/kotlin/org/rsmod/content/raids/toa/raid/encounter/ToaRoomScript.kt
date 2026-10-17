package org.rsmod.content.raids.toa.raid.encounter

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionContext
import org.rsmod.api.instances.InstanceManager
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRoomScript
@Inject
constructor(private val deps: BossDeps, private val instances: InstanceManager) : PluginScript() {
    override fun ScriptContext.startup() {
        ToaRooms.bind(instances)
        deps.extensionRegistry.register(ADOPT_SUMMON) { context: BossExtensionContext ->
            ToaRooms.roomOf(context.target)?.adopt(context.npc)
            val summon = context.params as RoomSummon
            val next = summon.onSummon ?: return@register
            deps.extensionRegistry.invoke(
                next,
                context.access,
                context.npc,
                context.target,
                summon.params,
                context.tile,
            )
        }
    }
}
