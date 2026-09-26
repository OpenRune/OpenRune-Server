package org.rsmod.content.other.pets.cats

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey

@Singleton
class CatChase
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val random: GameRandom,
    private val care: CatCare,
    private val followers: PetFollowers,
) {
    fun nearestRat(cat: Npc): Npc? =
        npcRepo
            .findAll(ZoneKey.from(cat.coords), 1)
            .filter { it.type.internalName in RATS && it.coords.level == cat.coords.level }
            .filter { it.coords.chebyshevDistance(cat.coords) <= RANGE }
            .minByOrNull { it.coords.chebyshevDistance(cat.coords) }

    fun cryptRatNearby(cat: Npc): Boolean =
        npcRepo.findAll(ZoneKey.from(cat.coords), 1).any {
            it.type.internalName == CRYPT_RAT && it.coords.chebyshevDistance(cat.coords) <= RANGE
        }

    fun chase(access: ProtectedAccess, cat: Npc, form: CatForm, rat: Npc) {
        val player = access.player
        player.say("Go on puss...kill that rat!")
        followers.markBusy(player)
        cat.faceNpc(rat)
        cat.walk(rat.coords) { pounce(player, cat, form, rat) }
    }

    /**
     * Runs on arrival. The pet stays marked busy for this cycle so it keeps facing the rat through
     * the pounce; the follower upkeep turns it back towards its owner on the next one.
     */
    private fun pounce(player: Player, cat: Npc, form: CatForm, rat: Npc) {
        cat.faceNpc(rat)
        cat.say("Meeeeeoooowww!")
        rat.say("Eek!")
        cat.anim(POUNCE)
        if (random.of(100) >= form.stage.catchChance) {
            player.mes("The rat manages to get away!")
            followers.clearBusy(player)
            return
        }
        val hellRat = rat.type.internalName == HELL_RAT
        rat.queueDeath()
        player.say("Hey well done puss, you got it!")
        val caught = minOf(player.catRatsCaught + 1, MAX_RATS)
        player.catRatsCaught = caught
        when (caught) {
            10 -> player.mes("Well done puss! 10 horrible rodents caught!")
            50 -> player.mes("Great puss, that's 50 rodents caught!")
            100 -> player.mes("Wow, incredible puss! That's 100 rodents caught!")
        }
        if (hellRat && !form.isHell) {
            player.mes("Your cat suddenly transforms!")
            care.toHell(player)?.say("MeeeoooooW!")
        }
        followers.clearBusy(player)
    }

    private companion object {
        const val RANGE = 8
        const val MAX_RATS = 255
        const val POUNCE = "seq.cat_pounce"
        const val HELL_RAT = "npc.hundred_dave_hellrat"
        const val CRYPT_RAT = "npc.barrows_rat"
        val RATS = setOf("npc.rat", "npc.rat_indoors", "npc.dungeon_rat", HELL_RAT)
    }
}
