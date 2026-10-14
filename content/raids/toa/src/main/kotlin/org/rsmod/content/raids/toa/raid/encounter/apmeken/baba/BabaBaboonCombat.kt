package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.roomCombatGate
import org.rsmod.plugin.scripts.ScriptContext

class BabaBaboonCombat @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onCombatTick =
                deps.roomCombatGate<BabaEncounter> { room, baboon, _ ->
                    !room.baboons.isRaiding(baboon)
                },
        )
        deps.onRoomExternal<BabaEncounter>(SHOOT) { room, ext ->
            room.baboons.shoot(ext.target)
        }
    }

    override val spec: BossSpec =
        boss(BabaNpcs.BABOON) {
            stats(attackRate = BabaApes.BABOON_ATTACK_RATE)
            ability(SHOT, sequence(anim(BabaSeqs.BABOON_RANGED), external(SHOOT)))
            phase(HUNT) { rotationSelector { +then(SHOT) } }
        }

    private companion object {
        const val SHOT = "shot"
        const val HUNT = "hunt"
        const val SHOOT = "baba.baboon_shoot"
    }
}
