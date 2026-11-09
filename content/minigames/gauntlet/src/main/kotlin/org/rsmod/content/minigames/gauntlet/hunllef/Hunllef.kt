package org.rsmod.content.minigames.gauntlet.hunllef

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.clearOwnedNpcs
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onNpcQueue
import org.rsmod.content.minigames.gauntlet.GauntletRewards
import org.rsmod.content.minigames.gauntlet.GauntletRuns
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.ScriptContext

class Hunllef
@Inject
internal constructor(
    deps: BossDeps,
    private val specs: HunllefSpecs,
    private val floor: HunllefFloor,
    private val tornadoes: HunllefTornadoes,
    private val runs: GauntletRuns,
    private val rewards: GauntletRewards,
    private val protectedAccess: ProtectedAccessLauncher,
) : BossPluginScript(deps) {
    override val spec: BossSpec = specs.crystalline

    override fun ScriptContext.startup() {
        tornadoes.registerExtensions()
        BossCombat.register(
            this,
            listOf(specs.crystalline, specs.corrupted),
            deps,
            onModifyHit = { countOffPrayerAttack() },
        )
        for (name in HunllefSpecs.TYPES) {
            val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(type, "queue.death") { death() }
        }
    }

    private fun NpcHitEvents.Modify.countOffPrayerAttack() {
        if (!hit.isFromPlayer) return
        val style = protectValue(hit.type) ?: return
        if (npc.vars[HunllefVarns.PROTECT] == style) return
        val count = npc.vars[HunllefVarns.OFF_PRAYER] + 1
        if (count < HunllefSpecs.OFF_PRAYER_SWITCH) {
            npc.vars[HunllefVarns.OFF_PRAYER] = count
            return
        }
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) } ?: return
        deps.runAbility(npc, attacker, PROTECT_ABILITIES.getValue(style))
    }

    private fun protectValue(type: HitType): Int? =
        when (type) {
            HitType.Melee -> HunllefVarns.PROTECT_MELEE
            HitType.Ranged -> HunllefVarns.PROTECT_RANGED
            HitType.Magic -> HunllefVarns.PROTECT_MAGIC
            else -> null
        }

    private suspend fun StandardNpcAccess.death() {
        val run = runs.runFor(npc)
        val player = run?.fighter
        floor.stop(npc)
        if (run != null && player != null) rewards.complete(player, run)
        deps.clearOwnedNpcs(npc)
        noneMode()
        hideAllOps()
        arriveDelay()
        anim(DEATH_PART_A)
        delay(DEATH_PART_B_DELAY)
        anim(DEATH_PART_B)
        val corrupted = run?.mode?.corrupted == true
        val deathType = if (corrupted) DEATH_NPC_HM else DEATH_NPC
        ServerCacheManager.getNpc(deathType.asRSCM(RSCMType.NPC))?.let { changeType(it, Int.MAX_VALUE) }
        delay(EXIT_DELAY)
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        if (player != null) {
            protectedAccess.launch(player) { with(runs) { leave() } }
        }
    }

    private companion object {
        const val DEATH_PART_A = "seq.hunllef_death_part_a"
        const val DEATH_PART_B = "seq.hunllef_death_part_b"
        const val DEATH_NPC = "npc.crystal_hunllef_death"
        const val DEATH_NPC_HM = "npc.crystal_hunllef_death_hm"
        const val DEATH_PART_B_DELAY = 3
        const val EXIT_DELAY = 3

        val PROTECT_ABILITIES =
            mapOf(
                HunllefVarns.PROTECT_MELEE to "protect_melee",
                HunllefVarns.PROTECT_RANGED to "protect_ranged",
                HunllefVarns.PROTECT_MAGIC to "protect_magic",
            )
    }
}
