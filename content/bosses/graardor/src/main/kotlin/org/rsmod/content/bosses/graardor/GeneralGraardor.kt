package org.rsmod.content.bosses.graardor

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

class GeneralGraardor @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private val generalId by lazy { GENERAL.asRSCM(RSCMType.NPC) }
    private val bodyguardIds by lazy { BODYGUARDS.map { it.asRSCM(RSCMType.NPC) }.toHashSet() }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        onEvent<NpcStateEvents.Respawn> { if (npc.id == generalId) respawnDeadBodyguards(npc) }
    }

    private fun respawnDeadBodyguards(general: Npc) {
        deps.npcRepo
            .findAll(ZoneKey.from(general.coords), zoneRadius = BODYGUARD_SEARCH_RADIUS)
            .filter { it.id in bodyguardIds && it.hitpoints == 0 }
            .forEach { bodyguard ->
                bodyguard.lifecycleRespawnCycle = deps.mapClock.cycle + 1
            }
    }

    override val spec =
        boss(GENERAL) {
            stats(attackRate = 5, aggressionRadius = 8)

            val melee =
                ability("melee") {
                    anim("seq.godwars_bandos_attack")
                    hit {
                        damage(0..60).roll()
                        type(Melee)
                    }
                }

            val ranged =
                ability("ranged") {
                    anim("seq.godwars_bandos_ranged")
                    projectile(
                        spotanim = "spotanim.godwars_bandos_proj",
                        travel = "projanim.godwars_bandos_ranged",
                        hit = Effect.Hit(damage = Roll(15..35), type = Ranged)
                    )
                }

            phase("combat") {
                weightedSelectorRandom {
                    +random(melee, weight = 2, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = WithinMeleeRange)
                }
            }
        }

    private companion object {
        private const val GENERAL = "npc.godwars_bandos_avatar"
        private val BODYGUARDS =
            listOf(
                "npc.godwars_sergeant_goblin1",
                "npc.godwars_sergeant_goblin2",
                "npc.godwars_sergeant_goblin3",
            )
        private const val BODYGUARD_SEARCH_RADIUS = 10
    }
}
