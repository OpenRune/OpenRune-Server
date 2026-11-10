package org.rsmod.content.other.treasuretrails

import dev.openrune.definition.type.DBRowType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.dbcol.DbHelper
import java.util.concurrent.ConcurrentHashMap
import org.rsmod.api.table.MusicRow
import org.rsmod.api.table.Tuple2
import org.rsmod.api.table.cluehelper.CluehelperChallengeQuestionRow
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
import org.rsmod.api.table.cluehelper.CluehelperCombatEncounterRow
import org.rsmod.api.table.cluehelper.CluehelperOutfitRow
import org.rsmod.api.table.cluehelper.CluehelperRequirementObjParamTrailItemRow
import org.rsmod.api.table.cluehelper.CluehelperRequirementObjRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player

enum class ClueKind {
    Anagram,
    Cipher,
    Coordinate,
    Cryptic,
    Emote,
    FairyRing,
    FaloBard,
    HotCold,
    Map,
    Music,
    SkillChallenge,
}

class ClueStep(
    val rowId: Int,
    val kind: ClueKind,
    val difficulty: Int,
    val text: String?,
    val targets: List<Int>,
    val emotes: List<Int> = emptyList(),
    val outfit: CluehelperOutfitRow? = null,
    val outfits: List<CluehelperOutfitRow> = listOfNotNull(outfit),
    val challenge: Int? = null,
    val hideyHole: ObjectServerType? = null,
    val music: MusicRow? = null,
    val restricted: Boolean = false,
    val encounters: List<CluehelperCombatEncounterRow> = emptyList(),
    val requirement: Int? = null,
    val itemRequirements: List<CluehelperRequirementObjRow> = emptyList(),
)

object ClueSteps {
    private val steps = ConcurrentHashMap<Int, ClueStep>()

    fun step(rowId: Int): ClueStep = steps.getOrPut(rowId) { load(rowId) }

    fun isStep(rowId: Int): Boolean = dbTableOf(rowId) in STEP_TABLES

    private fun load(rowId: Int): ClueStep =
        when (val table = dbTableOf(rowId)) {
            "dbtable.cluehelper_clue_anagram" ->
                CluehelperClueAnagramRow.getRow(rowId).let {
                    ClueStep(
                        rowId,
                        ClueKind.Anagram,
                        it.difficulty,
                        it.clueText,
                        it.target.ids(),
                        challenge = it.challenge?.id,
                        restricted = it.restrictedContent != null,
                    )
                }
            "dbtable.cluehelper_clue_cipher" ->
                CluehelperClueCipherRow.getRow(rowId).let {
                    ClueStep(
                        rowId,
                        ClueKind.Cipher,
                        it.difficulty,
                        it.clueText,
                        it.target.ids(),
                        challenge = it.challenge?.id,
                        restricted = it.restrictedContent != null,
                    )
                }
            "dbtable.cluehelper_clue_coordinate" ->
                CluehelperClueCoordinateRow.getRow(rowId).let {
                    ClueStep(
                        rowId,
                        ClueKind.Coordinate,
                        it.difficulty,
                        it.clueText,
                        listOf(it.target.rowId),
                        restricted = it.restrictedContent != null,
                        encounters = it.combatEncounter,
                    )
                }
            "dbtable.cluehelper_clue_cryptic" ->
                CluehelperClueCrypticRow.getRow(rowId).let {
                    ClueStep(
                        rowId,
                        ClueKind.Cryptic,
                        it.difficulty,
                        it.clueText,
                        it.target.ids(),
                        outfit = it.outfit,
                        challenge = it.challenge?.rowId,
                        hideyHole = it.hideyHoleLoc,
                        itemRequirements = it.requirements,
                        restricted = it.restrictedContent != null,
                    )
                }
            "dbtable.cluehelper_clue_emote" ->
                CluehelperClueEmoteRow.getRow(rowId).let {
                    ClueStep(
                        rowId,
                        ClueKind.Emote,
                        it.difficulty,
                        it.clueText,
                        listOf(it.target.rowId),
                        emotes = it.emote,
                        outfit = it.outfit.firstOrNull(),
                        outfits = it.outfit,
                        hideyHole = it.hideyHoleLoc,
                        restricted = it.restrictedContent != null,
                        encounters = listOfNotNull(it.combatEncounter),
                    )
                }
            "dbtable.cluehelper_clue_fairyring" ->
                CluehelperClueFairyringRow.getRow(rowId).let {
                    val targets = listOf(it.target.rowId)
                    ClueStep(rowId, ClueKind.FairyRing, it.difficulty, it.clueText, targets)
                }
            "dbtable.cluehelper_clue_falobard" ->
                CluehelperClueFalobardRow.getRow(rowId).let {
                    val targets = listOf(it.target.rowId)
                    ClueStep(
                        rowId,
                        ClueKind.FaloBard,
                        it.difficulty,
                        it.clueText,
                        targets,
                        restricted = it.restrictedContent != null,
                        requirement = it.requirements.id,
                    )
                }
            "dbtable.cluehelper_clue_hotcold" ->
                CluehelperClueHotcoldRow.getRow(rowId).let {
                    ClueStep(
                        rowId,
                        ClueKind.HotCold,
                        it.difficulty,
                        null,
                        listOf(it.target.rowId),
                        encounters = it.combatEncounter,
                    )
                }
            "dbtable.cluehelper_clue_map" ->
                CluehelperClueMapRow.getRow(rowId).let {
                    ClueStep(rowId, ClueKind.Map, it.difficulty, null, listOf(it.target.id))
                }
            "dbtable.cluehelper_clue_music" ->
                CluehelperClueMusicRow.getRow(rowId).let {
                    ClueStep(
                        rowId,
                        ClueKind.Music,
                        it.difficulty,
                        it.unlockText,
                        listOf(it.target.rowId),
                        music = it.music,
                        restricted = it.restrictedContent != null,
                    )
                }
            "dbtable.cluehelper_clue_skillchallenge" ->
                CluehelperClueSkillchallengeRow.getRow(rowId).let {
                    val targets = listOfNotNull(it.target?.rowId)
                    ClueStep(rowId, ClueKind.SkillChallenge, it.difficulty, it.clueText, targets)
                }
            else -> error("Row $rowId in $table is not a clue step.")
        }

    fun target(player: Player, step: ClueStep): ClueTarget? {
        val targets = step.targets
        if (targets.isEmpty()) {
            return null
        }
        return ClueTarget.of(targets[targetIndex(player, targets.first())])
    }

    fun target(player: Player, step: Int): ClueTarget? = target(player, step(step))

    fun question(step: ClueStep): Tuple2<String, Int>? {
        val row = step.challenge ?: return null
        if (dbTableOf(row) != "dbtable.cluehelper_challenge_question") return null
        return CluehelperChallengeQuestionRow.getRow(row).question.firstOrNull()
    }

    fun holdsRequiredItem(player: Player, step: ClueStep): Boolean {
        val row = step.requirement ?: return true
        val held = (0 until player.inv.size).mapNotNull { player.inv[it]?.id }
        return when (dbTableOf(row)) {
            "dbtable.cluehelper_requirement_obj" ->
                CluehelperRequirementObjRow.getRow(row).item.any { it.id in held }
            "dbtable.cluehelper_requirement_obj_param_trail_item" -> {
                val group = CluehelperRequirementObjParamTrailItemRow.getRow(row).itemGroup
                held.any { TrailTiers.itemGroup(TrailTiers.type(it)) == group }
            }
            else -> error("Row $row is not a known item requirement.")
        }
    }

    fun isPuzzleBox(step: ClueStep): Boolean {
        val row = step.challenge
        if (row != null && RSCM.getReverseMapping(RSCMType.DBROW, row) == PUZZLE_BOX_ROW) {
            return true
        }
        return ClueBoxes.isBoxChallenge(step) || ClueSlidePuzzle.boxFor(step) != null
    }

    fun all(difficulty: Int): List<ClueStep> =
        STEP_TABLES.flatMap { table -> stepRowIds(table) }
            .map(::step)
            .filter { it.difficulty == difficulty }

    private fun stepRowIds(table: String): List<Int> = DbHelper.table(table).map { it.id }

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

    private fun List<DBRowType>.ids(): List<Int> = map { it.id }

    private const val PUZZLE_BOX_ROW = "dbrow.cluehelper_challenge_box_puzzle"

    private val STEP_TABLES =
        listOf(
            "dbtable.cluehelper_clue_anagram",
            "dbtable.cluehelper_clue_cipher",
            "dbtable.cluehelper_clue_coordinate",
            "dbtable.cluehelper_clue_cryptic",
            "dbtable.cluehelper_clue_emote",
            "dbtable.cluehelper_clue_fairyring",
            "dbtable.cluehelper_clue_falobard",
            "dbtable.cluehelper_clue_hotcold",
            "dbtable.cluehelper_clue_map",
            "dbtable.cluehelper_clue_music",
            "dbtable.cluehelper_clue_skillchallenge",
        )
}
