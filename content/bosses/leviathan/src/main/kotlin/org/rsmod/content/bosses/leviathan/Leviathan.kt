package org.rsmod.content.bosses.leviathan

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.plugin.scripts.ScriptContext

class Leviathan
@Inject
internal constructor(
    deps: BossDeps,
    private val encounters: LeviathanEncounters,
    private val npcDeath: NpcDeath,
) : BossPluginScript(deps) {

    /**
     * `bite` and `volley` are declared here and selected by the DSL's weighted-random phase
     * selector. `bite` is fully DSL-native (a cosmetic anim pick via nested `choose`, then a
     * plain `hit`); `volley` (the escalating shot ladder, then rockfall) is bespoke enough to
     * stay in [LeviathanEncounters] and run through [external]/[LeviathanEncounters.VOLLEY_EXT],
     * the same way `Muspah`'s shockwave and `Vardorvis`'s axe-set are delegated. The shadow-spell
     * stun/weak-spot break and the lightning/smoke specials it triggers are reactive events, not
     * selector-chosen abilities, so they're wired through [onNpcHit]/[BossCombat.register]'s
     * `onModifyHit` straight into [LeviathanEncounters] rather than declared here.
     *
     * The `enraged` phase's `entryHp` is the one piece of this fight the DSL drives natively -
     * `BossCombat`'s own hp-threshold auto-transition replaces the old hand-rolled poll in
     * `onDamaged`. Its entry/rockfall/orb abilities are still bespoke externals for the same
     * reason `volley` is.
     */
    private val biteAnim: Effect =
        choose(
            selector = weightedRandom { +random(BITE_ANIM_1, weight = 1); +random(BITE_ANIM_2, weight = 1) },
            branches = mapOf(BITE_ANIM_1 to anim(BITE_ANIM_1), BITE_ANIM_2 to anim(BITE_ANIM_2)),
        )

    override val spec: BossSpec =
        boss(LeviathanEncounters.BOSS_NPC) {
            stats(retaliateOnHit = false)

            val bite =
                ability("bite") {
                    include(biteAnim)
                    hit {
                        damage(DamageExpr.Custom { npc, _ -> encounters.biteDamage(npc) })
                        type(Melee)
                    }
                }

            val volley = ability("volley") { include(external(LeviathanEncounters.VOLLEY_EXT)) }

            phase(FIGHT_PHASE) {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    +random(bite, weight = 1, requires = Condition.Custom { npc -> encounters.shouldBite(npc) })
                    +random(volley, weight = 1)
                }
            }

            val enrageEntry = ability("enrage_entry") { include(external(LeviathanEncounters.ENRAGE_ENTRY_EXT)) }
            val enragedRockfall =
                ability("enraged_rockfall") { include(external(LeviathanEncounters.ENRAGED_ROCKFALL_EXT)) }
            val enragedOrb = ability("enraged_orb") { include(external(LeviathanEncounters.ENRAGED_ORB_EXT)) }

            phase(ENRAGED_PHASE, entryHp = LeviathanEncounters.ENRAGE_HP_FRACTION) {
                entry = enrageEntry.name
                weightedSelectorRandom(noRepeatBias = 0.0) { +random(enragedOrb, weight = 1) }
                forceEvery(LeviathanEncounters.ENRAGED_ROCKFALL_INTERVAL, enragedRockfall)
            }
        }

    override fun ScriptContext.startup() {
        val type = ServerCacheManager.getNpc(LeviathanEncounters.BOSS_NPC.asRSCM(RSCMType.NPC))!!
        val shadowSpells = SHADOW_SPELLS.mapNotNull { ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ)) }

        encounters.registerExtensions()
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                if (hit.isFromPlayer) {
                    val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) }
                    hit.damage = encounters.modifyIncoming(npc, attacker, hit.damage, hit.type)
                }
            },
        )

        onNpcHit(type) {
            if (hit.isFromPlayer && shadowSpells.any(hit::isSecondaryObj)) {
                hit.resolvePlayerSource(deps.playerList)?.let { encounters.onShadowSpellImpact(npc, it) }
            }
            encounters.onDamaged(npc)
            deps.worldRepo.soundArea(npc, HIT_SYNTH, radius = HIT_SYNTH_RADIUS)
        }

        onNpcQueue(type, "queue.death") {
            val session = encounters.sessionOf(npc)
            val dropCoords = encounters.dropCoords(npc)
            encounters.onDeath(npc)
            npcDeath.deathWithDrops(this, dropCoords)
            session?.let(encounters::afterKill)
        }

        onEvent<NpcStateEvents.Delete> { encounters.onDeleted(npc) }
    }

    private companion object {
        private const val FIGHT_PHASE = "fight"
        private const val ENRAGED_PHASE = "enraged"
        private const val HIT_SYNTH = "synth.leviathan_hit"
        private const val HIT_SYNTH_RADIUS = 15
        private const val BITE_ANIM_1 = "seq.npc_leviathan_01_melee_01"
        private const val BITE_ANIM_2 = "seq.npc_leviathan_01_melee_02"

        private val SHADOW_SPELLS =
            listOf(
                "obj.52_shadow_rush",
                "obj.64_shadow_burst",
                "obj.76_shadow_blitz",
                "obj.88_shadow_barrage",
            )
    }
}
