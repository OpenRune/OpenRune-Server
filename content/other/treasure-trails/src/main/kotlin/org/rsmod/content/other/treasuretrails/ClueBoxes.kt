package org.rsmod.content.other.treasuretrails

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

object ClueBoxes {
    private const val LIGHT_ROW = "dbrow.cluehelper_challenge_box_light"
    private const val EITHER_ROW = "dbrow.cluehelper_challenge_box_puzzle_or_light"

    fun held(player: Player): ItemServerType? =
        ClueSlidePuzzle.held(player) ?: ClueLightBox.held(player)

    fun solved(player: Player, box: ItemServerType): Boolean =
        if (box.symbol() == ClueLightBox.BOX) {
            ClueLightBox.solved(player)
        } else {
            ClueSlidePuzzle.solved(player)
        }

    fun reset(player: Player) {
        ClueSlidePuzzle.reset(player)
        ClueLightBox.reset(player)
    }

    fun handOut(step: ClueStep, random: GameRandom): ItemServerType {
        val light = lightBox()
        return when (challengeRow(step)) {
            LIGHT_ROW -> light
            EITHER_ROW -> random.pick(listOf(light, ClueSlidePuzzle.handOut(step, random)))
            else -> ClueSlidePuzzle.handOut(step, random)
        }
    }

    fun isBoxChallenge(step: ClueStep): Boolean =
        challengeRow(step) in setOf(LIGHT_ROW, EITHER_ROW)

    private fun lightBox(): ItemServerType =
        TrailTiers.type(ClueLightBox.BOX.asRSCM(RSCMType.OBJ))

    private fun challengeRow(step: ClueStep): String? =
        step.challenge?.let { RSCM.getReverseMapping(RSCMType.DBROW, it) }
}
