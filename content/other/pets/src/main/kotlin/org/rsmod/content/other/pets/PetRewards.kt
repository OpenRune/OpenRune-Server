package org.rsmod.content.other.pets

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

@Singleton
class PetRewards
@Inject
constructor(
    private val followers: PetFollowers,
    private val insurance: PetInsurance,
    private val random: GameRandom,
) {
    fun give(player: Player, obj: String): Boolean {
        val (pet, form) = Pets.forObj(obj) ?: return false
        if (!pet.mainDrop && player.ownsPet(pet)) {
            player.mes("<col=ff0000>You have a funny feeling like you would have been followed...</col>")
            return true
        }
        val insured = insurance.insure(player, pet)
        deliver(player, form)
        if (insured) {
            player.mes(
                "<col=ff00ff>Your new pet has been automatically insured. If lost, it can be reclaimed " +
                    "from Probita in Ardougne.</col>"
            )
        }
        return true
    }

    fun rollSkillingPet(
        player: Player,
        obj: String,
        stat: String,
        baseChance: Int,
        rarityMultiplier: Int = 1,
    ): Boolean {
        val scaled = (baseChance - player.statBase(stat) * 25).coerceAtLeast(1)
        if (random.of(scaled * rarityMultiplier) != 0) {
            return false
        }
        return give(player, obj)
    }

    fun reclaim(player: Player, pet: Pet): Boolean {
        if (followers.hasFollower(player) && player.inv.isFull()) {
            return false
        }
        deliver(player, pet.base)
        return true
    }

    private fun deliver(player: Player, form: PetForm) {
        when {
            !followers.hasFollower(player) -> {
                followers.spawn(player, form)
                player.mes("<col=ff0000>You have a funny feeling like you're being followed.</col>")
            }
            !player.inv.isFull() -> {
                player.invAdd(player.inv, form.obj, 1)
                player.mes("<col=ff0000>You feel something weird sneaking into your backpack.</col>")
            }
            else -> {
                player.mes(
                    "<col=ff0000>You have a funny feeling like you would have been followed, but your " +
                        "backpack is full. Probita in East Ardougne is looking after your pet.</col>"
                )
            }
        }
    }
}
