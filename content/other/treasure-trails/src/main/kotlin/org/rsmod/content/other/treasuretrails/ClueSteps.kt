package org.rsmod.content.other.treasuretrails

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.cluehelper.CluehelperClueAnagramRow
import org.rsmod.api.table.cluehelper.CluehelperClueCipherRow
import org.rsmod.api.table.cluehelper.CluehelperClueCoordinateRow
import org.rsmod.api.table.cluehelper.CluehelperClueCrypticRow
import org.rsmod.api.table.cluehelper.CluehelperClueEmoteRow
import org.rsmod.api.table.cluehelper.CluehelperClueFairyringRow
import org.rsmod.api.table.cluehelper.CluehelperClueFalobardRow
import org.rsmod.api.table.cluehelper.CluehelperClueHotcoldRow
import org.rsmod.api.table.cluehelper.CluehelperClueMapRow
import org.rsmod.api.table.cluehelper.CluehelperClueMusicRow
import org.rsmod.api.table.cluehelper.CluehelperClueSkillchallengeRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player

object ClueSteps {
    fun target(player: Player, step: Int): ClueTarget? {
        val targets = targets(step)
        if (targets.isEmpty()) {
            return null
        }
        return ClueTarget.of(targets[targetIndex(player, targets.first())])
    }

    private fun targets(step: Int): List<Int> =
        when (val table = dbTableOf(step)) {
            "dbtable.cluehelper_clue_anagram" ->
                CluehelperClueAnagramRow.getRow(step).target.map { it.id }
            "dbtable.cluehelper_clue_cipher" ->
                CluehelperClueCipherRow.getRow(step).target.map { it.id }
            "dbtable.cluehelper_clue_cryptic" ->
                CluehelperClueCrypticRow.getRow(step).target.map { it.id }
            "dbtable.cluehelper_clue_map" -> listOf(CluehelperClueMapRow.getRow(step).target.id)
            "dbtable.cluehelper_clue_coordinate" ->
                listOf(CluehelperClueCoordinateRow.getRow(step).target.rowId)
            "dbtable.cluehelper_clue_emote" ->
                listOf(CluehelperClueEmoteRow.getRow(step).target.rowId)
            "dbtable.cluehelper_clue_fairyring" ->
                listOf(CluehelperClueFairyringRow.getRow(step).target.rowId)
            "dbtable.cluehelper_clue_hotcold" ->
                listOf(CluehelperClueHotcoldRow.getRow(step).target.rowId)
            "dbtable.cluehelper_clue_falobard" ->
                listOf(CluehelperClueFalobardRow.getRow(step).target.rowId)
            "dbtable.cluehelper_clue_music" ->
                listOf(CluehelperClueMusicRow.getRow(step).target.rowId)
            "dbtable.cluehelper_clue_skillchallenge" ->
                listOfNotNull(CluehelperClueSkillchallengeRow.getRow(step).target?.rowId)
            else -> error("Row $step in $table is not a clue step.")
        }

    /** Mirrors `[proc,script6657]`: some targets move once a quest is far enough along. */
    private fun targetIndex(player: Player, firstTarget: Int): Int =
        when (RSCM.getReverseMapping(RSCMType.DBROW, firstTarget)) {
            "dbrow.cluehelper_target_coord_1_25_83_40_54",
            "dbrow.cluehelper_target_npc_jardric_camp" ->
                completedIndex(player, "quest_dragonslayer2")
            "dbrow.cluehelper_target_npc_eluned_lletya" ->
                completedIndex(player, "quest_songoftheelves")
            "dbrow.cluehelper_target_npc_radimus_erkle_hut" ->
                completedIndex(player, "quest_legends")
            "dbrow.cluehelper_target_npc_viggora_1" ->
                (player.vars["varbit.secret_ghost_randomiser"] - 1).coerceAtLeast(0)
            else -> 0
        }

    private fun completedIndex(player: Player, quest: String): Int =
        if (QuestRequirements.hasCompleted(player, quest)) 1 else 0
}
