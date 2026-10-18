package org.rsmod.content.other.treasuretrails

import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.hook.PlayerLocOpHook
import org.rsmod.api.player.hook.PlayerNpcOpHook
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.plugin.module.PluginModule

class TreasureTrailsModule : PluginModule() {
    override fun bind() {
        addSetBinding<PlayerNpcOpHook>(ClueNpcHook::class.java)
        addSetBinding<PlayerLocOpHook>(ClueLocHook::class.java)
        addSetBinding<SpadeDigHook>(ClueDigHook::class.java)
        addSetBinding<PlayerObjTakeValidateHook>(ClueTakeHook::class.java)
        addSetBinding<NpcDeathKillHook>(ClueKeyDropHook::class.java)
        addSetBinding<NpcDeathKillHook>(ClueEncounterKillHook::class.java)
    }
}
