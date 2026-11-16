package org.rsmod.content.raids.toa.raid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository

/**
 * Keris partisan of the sun, Tumeken's Light: costs 75% special energy (cache table) and 50 Prayer
 * points; heals to full plus an overheal of 20% of base Hitpoints, cures poison and venom, restores
 * run energy and drained stats. Only usable inside the Tombs of Amascut.
 */
class ToaKerisSpecial : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerInstant("obj.keris_partisan_sun", ::tumekensLight)
    }

    private fun tumekensLight(access: ProtectedAccess): Boolean {
        with(access) {
            if (!ToaRaidManager.isInRaid(player)) {
                mes("This special attack can only be used within the Tombs of Amascut.")
                return false
            }
            if (player.stat("stat.prayer") < PRAYER_COST) {
                mes("You need at least $PRAYER_COST Prayer points to use this special attack.")
                return false
            }

            statSub("stat.prayer", PRAYER_COST, 0)
            anim("seq.toa_keris_partisan_special01")
            spotanim(
                "spotanim.keris_partisan_special01",
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val base = statBase("stat.hitpoints")
            val target = base + base * 20 / 100
            val missing = target - player.stat("stat.hitpoints")
            if (missing > 0) {
                statAdd("stat.hitpoints", missing, 0)
            }

            PlayerVenom.reduceToPoison(player)
            PlayerPoison.clear(player)

            for (stat in ServerCacheManager.getStats().values) {
                val name = RSCM.getReverseMapping(RSCMType.STAT, stat.id)
                if (name != "stat.hitpoints" && name != "stat.prayer") {
                    statRestore(name)
                }
            }
            player.runEnergy = MAX_RUN_ENERGY
        }
        return true
    }

    private companion object {
        const val PRAYER_COST = 50
        const val MAX_RUN_ENERGY = 10_000
    }
}
