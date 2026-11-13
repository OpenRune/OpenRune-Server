package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

@Singleton
class FarmingEquipment @Inject constructor(private val random: GameRandom) {
    fun seedCost(player: Player, crop: Crop): Int =
        if (crop.kind == PatchKind.ALLOTMENT && crop.seedsPerPlant == 3 &&
            BOUNTY in player.worn && random.of(4) == 0
        ) 1 else crop.seedsPerPlant

    fun planted(player: Player, crop: Crop, seedCount: Int) {
        if (crop.kind != PatchKind.ALLOTMENT || crop.seedsPerPlant != 3 || seedCount != 1) return
        check(BOUNTY in player.worn)
        val remaining = bountyCharges(player) - 1
        if (remaining == 0) {
            check(player.invDel(player.worn, BOUNTY, 1, strict = true).success)
            VarPlayerIntMapSetter.set(player, BOUNTY_USES, 0)
            player.mes("Your amulet of bounty crumbles to dust.")
        } else {
            VarPlayerIntMapSetter.set(player, BOUNTY_USES, MAX_BOUNTY_CHARGES - remaining)
        }
    }

    fun bountyCharges(player: Player): Int =
        MAX_BOUNTY_CHARGES - player.vars[BOUNTY_USES].coerceIn(0, MAX_BOUNTY_CHARGES - 1)

    fun magicSecateurs(player: Player, crop: Crop): Boolean =
        crop.magicSecateurs && (MAGIC_SECATEURS in player.inv || MAGIC_SECATEURS in player.worn)

    fun harvestItemPercent(player: Player, crop: Crop): Int =
        (if (magicSecateurs(player, crop)) 10 else 0) +
            (if (crop.kind == PatchKind.HERB && FARMING_CAPES.any { it in player.worn }) 5 else 0)

    fun barehandedPlanting(player: Player): Boolean = player.vars[BARBARIAN_PLANTING] == 3

    fun needsDibber(player: Player, crop: Crop): Boolean =
        crop.kind == PatchKind.GRAPE_VINE || !barehandedPlanting(player)

    fun smashesPlantPot(player: Player): Boolean =
        player.vars[BARBARIAN_SMASHING] == 1 ||
            (player.vars[BARBARIAN_SMASHING] in 2..3 && player.vars[SMASH_POTS] == 1)

    fun saplingPlanted(player: Player) {
        if (player.vars[BARBARIAN_SMASHING] != 1) return
        VarPlayerIntMapSetter.set(player, BARBARIAN_SMASHING, 2)
        VarPlayerIntMapSetter.set(player, SMASH_POTS, 1)
        player.mes("You feel you have learned more of barbarian ways. Otto might wish to talk to you more.")
    }

    companion object {
        const val BOUNTY = "obj.amulet_of_bounty"
        const val BOUNTY_USES = "varp.farming_bounty_uses"
        private const val BARBARIAN_PLANTING = "varbit.brut_farming_planting"
        private const val BARBARIAN_SMASHING = "varbit.brut_farming_smashing"
        private const val SMASH_POTS = "varbit.brut_smash_pots_automatically"
        private const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
        private val FARMING_CAPES =
            listOf("obj.skillcape_farming", "obj.skillcape_farming_trimmed", "obj.skillcape_max")
        private const val MAX_BOUNTY_CHARGES = 10
    }
}
