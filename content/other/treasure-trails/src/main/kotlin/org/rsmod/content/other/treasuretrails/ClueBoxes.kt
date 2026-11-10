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

    private val MASTER_CODES =
        mapOf(
            ClueLightBox.BOX to MasterVars.LIGHT_BOX,
            "obj.trail_master_zulrah_puzzlebox" to MasterVars.ZULRAH_BOX,
            "obj.trail_master_cerberus_puzzlebox" to MasterVars.CERBERUS_BOX,
            "obj.trail_master_gnomechild_puzzlebox" to MasterVars.GNOME_CHILD_BOX,
            "obj.trail_master_tob_puzzlebox" to MasterVars.TOB_BOX,
        )

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
        MasterVars.setBox(player, 0)
    }

    fun masterCode(box: ItemServerType): Int? = MASTER_CODES[box.symbol()]

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
