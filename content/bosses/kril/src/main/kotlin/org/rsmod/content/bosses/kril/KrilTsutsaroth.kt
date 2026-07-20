package org.rsmod.content.bosses.kril

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

class KrilTsutsaroth @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private val avatarId by lazy { AVATAR.asRSCM(RSCMType.NPC) }
    private val bodyguardIds by lazy { BODYGUARDS.map { it.asRSCM(RSCMType.NPC) }.toHashSet() }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        onEvent<NpcStateEvents.Respawn> { if (npc.id == avatarId) respawnDeadBodyguards(npc) }

        deps.extensionRegistry.register("kril.prayer_smash_drain") { _, _, target, _ ->
            target.statSub("stat.prayer", constant = 0, percent = 50)
        }
    }

    private fun respawnDeadBodyguards(avatar: Npc) {
        deps.npcRepo
            .findAll(ZoneKey.from(avatar.coords), zoneRadius = BODYGUARD_SEARCH_RADIUS)
            .filter { it.id in bodyguardIds && it.hitpoints == 0 }
            .forEach { bodyguard ->
                bodyguard.lifecycleRespawnCycle = deps.mapClock.cycle + 1
            }
    }

    override val spec =
        boss(AVATAR) {
            stats(attackRate = 5, aggressionRadius = 8)

            val melee =
                ability("melee") {
                    anim("seq.godwars_zamorak_attack")
                    hit {
                        damage(0..46).roll()
                        type(Melee)
                    }
                }

            val magic =
                ability("magic") {
                    anim("seq.godwars_zamorak_magic_attack")
                    spotanim("spotanim.godwars_zamorak_magic_attack_spot")
                    hit {
                        damage(10..30).roll()
                        type(Magic)
                    }
                }

            val magicDrain =
                ability("magic_drain") {
                    anim("seq.godwars_zamorak_attack")
                    say("YARRRRRRR!")
                    hit {
                        damage(35..49).roll()
                        type(Melee)
                    }
                    include(external("kril.prayer_smash_drain"))
                }

            phase("combat") {
                weightedSelectorRandom {
                    +random(melee, weight = 16, requires = WithinMeleeRange)
                    +random(magic, weight = 9)
                    +random(magicDrain, weight = 2, requires = WithinMeleeRange)
                }
            }
        }

    private companion object {
        private const val AVATAR = "npc.godwars_zamorak_avatar"
        private val BODYGUARDS =
            listOf(
                "npc.godwars_ancient_black_demon",
                "npc.godwars_ancient_greater_demon",
                "npc.godwars_ancient_lesser_demon",
            )
        private const val BODYGUARD_SEARCH_RADIUS = 10
    }
}
