package org.rsmod.content.bosses.araxxor

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.plugin.module.PluginModule

class AraxxorModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathDropHook>(AraxxorDropHook::class.java)
        addSetBinding<NpcDeathKillHook>(AraxxorKillHook::class.java)
    }
}

class AraxxorDropHook @Inject constructor() : NpcDeathDropHook {
    override fun tryConsume(context: NpcDeathDropContext): Boolean = false

    override fun suppressesDropTable(context: NpcDeathKillContext): Boolean =
        context.npc.isType(ARAXXOR)
}

class AraxxorKillHook
@Inject
constructor(private val corpse: AraxxorCorpse, private val eggs: AraxxorEggs) : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        if (!context.npc.isType(ARAXXOR)) return
        eggs.crumbleEggs(context.npc)
        corpse.onBossKilled(context.hero, context.npc)
    }
}
