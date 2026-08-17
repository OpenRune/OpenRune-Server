package org.rsmod.content.bosses.zilyana

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

class CommanderZilyana @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private val avatarId by lazy { AVATAR.asRSCM(RSCMType.NPC) }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        onEvent<NpcStateEvents.Respawn> { if (npc.id == avatarId) respawnDeadBodyguards(npc) }
    }

    private fun respawnDeadBodyguards(avatar: Npc) {
        deps.npcRepo
            .findAll(ZoneKey.from(avatar.coords), zoneRadius = BODYGUARD_SEARCH_RADIUS)
            .filter { it.visType.isCategoryType(BODYGUARD_CATEGORY) && it.hitpoints == 0 }
            .forEach { bodyguard ->
                bodyguard.lifecycleRespawnCycle = deps.mapClock.cycle + 1
            }
    }

    override val spec =
        boss(AVATAR) {
            stats(attackRate = 5, aggressionRadius = 8)
            val magic =
                ability("magic") {
                    anim("seq.godwars_saradomin_magic_attack")
                    hit {
                        damage(10..20).roll()
                        type(Magic)
                        spotanim("spotanim.godwars_saradomin_light_attk_spot")
                    }
                }

            val melee =
                ability("melee") {
                    anim("seq.godwars_saradomin_attack")
                    hit {
                        damage(0..27).roll()
                        type(Melee)
                    }
                }

            phase("combat") {
                weightedSelectorRandom {
                    +random(melee, weight = 3, requires = WithinMeleeRange)
                    +random(magic, weight = 2)
                }
            }
        }

    private companion object {
        private const val AVATAR = "npc.godwars_saradomin_avatar"
        private const val BODYGUARD_CATEGORY = "category.godwars_saradomin_bodyguard"
        private const val BODYGUARD_SEARCH_RADIUS = 10
    }
}
