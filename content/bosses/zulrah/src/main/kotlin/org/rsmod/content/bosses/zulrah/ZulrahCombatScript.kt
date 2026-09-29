package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.NpcMeleeRangeHook
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ZulrahCombatModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcAttackValidateHook>(ZulrahEncounterManager::class.java)
        addSetBinding<NpcMeleeRangeHook>(ZulrahMeleeReach::class.java)
    }
}

class ZulrahCombatScript
@Inject
constructor(private val encounters: ZulrahEncounterManager, private val death: NpcDeath) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { encounters.tick() }
        onEvent<PlayerHitEvents.Impact> { encounters.onHitImpact(player, hit) }
        onEvent<NpcStateEvents.Delete> { encounters.onNpcDeleted(npc) }
        for (symbol in BOSS_TYPES) {
            val type = ZulrahEncounterManager.type(symbol)
            onAiOpPlayer2(type) { noneMode() }
            onAiApPlayer2(type) { noneMode() }
            onModifyNpcHit(type) { encounters.modifyHit(npc, hit) }
            onNpcQueue(type, "queue.death") {
                val dropCoords = encounters.beginDeath(npc)
                if (dropCoords != null) {
                    death.deathWithDrops(this, dropCoords)
                    encounters.completeDeath(npc)
                } else {
                    death.deathNoDrops(this)
                }
            }
        }
        for (symbol in MINION_TYPES) {
            val type = ZulrahEncounterManager.type(symbol)
            onAiOpPlayer2(type) { encounters.minionAttack(npc, it.target) }
            onAiApPlayer2(type) { encounters.minionAttack(npc, it.target) }
            onModifyNpcHit(type) { encounters.modifyHit(npc, hit) }
            onNpcQueue(type, "queue.death") { death.deathNoDrops(this) }
        }
    }

    companion object {
        val BOSS_TYPES = listOf("npc.snakeboss_boss_ranged", "npc.snakeboss_boss_melee", "npc.snakeboss_boss_magic")
        val MINION_TYPES = listOf("npc.snakeboss_minion_melee", "npc.snakeboss_minion_magic")
    }
}
