package org.rsmod.content.other.treasuretrails

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import kotlin.random.Random
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/**
 * The hard puzzle box. The tiles live in `inv.trail_puzzleinv`, which the client draws and slides
 * locally; each click is mirrored here so the server knows when the picture is whole.
 */
object ClueSlidePuzzle {
    const val INTERFACE = "interface.trail_slidepuzzle"
    const val PIECES = "component.trail_slidepuzzle:pieces"
    private const val INV = "inv.trail_puzzleinv"
    private const val THEME = "varp.if1"
    private const val SOLVED = "varp.trail_puzzle_solved"
    private const val HARD_BOX_NAME = "Puzzle box (hard)"
    private const val BOX_SUFFIX = "_puzzlebox"
    private const val THEMES = 3
    private val BOX_NAMES = setOf(HARD_BOX_NAME, "Puzzle box (master)")

    private val handOutBoxes =
        listOf(
            "obj.trail_clue_hard_riddle014_puzzlebox",
            "obj.trail_clue_hard_riddle018_puzzlebox",
            "obj.trail_clue_hard_riddle020_puzzlebox",
        )

    private val masterBoxes =
        listOf(
            "obj.trail_master_zulrah_puzzlebox",
            "obj.trail_master_cerberus_puzzlebox",
            "obj.trail_master_gnomechild_puzzlebox",
        )

    private val masterThemes = listOf("zulrah", "cerberus", "gnomechild")

    val boxes: List<ItemServerType> by lazy {
        ServerCacheManager.getItems().values.filter { it.name in BOX_NAMES }
    }

    fun held(player: Player): ItemServerType? =
        player.inv.objs.firstNotNullOfOrNull { obj ->
            obj?.let { TrailTiers.type(it.id) }?.takeIf { it.name in BOX_NAMES }
        }

    fun boxFor(step: ClueStep): ItemServerType? =
        TrailTiers.clueObjs(step.difficulty)
            .filter { TrailTiers.objStep(it) == step.rowId }
            .firstNotNullOfOrNull { clue ->
                RSCM.getRSCMOrNull(clue.symbol() + BOX_SUFFIX, RSCMType.OBJ)
                    ?.let(ServerCacheManager::getItem)
                    ?.takeIf { it.name == HARD_BOX_NAME }
            }

    fun handOut(step: ClueStep, random: GameRandom): ItemServerType {
        boxFor(step)?.let { return it }
        val pool = if (step.difficulty == TrailTiers.MASTER) masterBoxes else handOutBoxes
        val symbol = random.pick(pool)
        return boxes.first { it.symbol() == symbol }
    }

    fun solved(player: Player): Boolean = player.vars[SOLVED] == 1

    fun reset(player: Player) {
        VarPlayerIntMapSetter.set(player, SOLVED, 0)
        val puzzle = player.invMap.getOrPut(INV)
        player.invClear(puzzle)
        player.stopInvTransmit(puzzle)
    }

    fun ProtectedAccess.open(box: ItemServerType) {
        val puzzle = player.invMap.getOrPut(INV)
        val theme = themeOf(box)
        if (puzzle.isEmpty()) {
            VarPlayerIntMapSetter.set(player, SOLVED, 0)
            fill(puzzle, theme, SlidePuzzle.scrambled(Random.Default))
        }
        VarPlayerIntMapSetter.set(player, THEME, theme)
        invTransmit(puzzle)
        ifOpenMainModal(INTERFACE)
        ifSetEvents(PIECES, 0 until SlidePuzzle.CELLS, IfEvent.Op1)
    }

    fun ProtectedAccess.slide(cell: Int) {
        if (solved(player)) return
        val puzzle = player.invMap.getOrPut(INV)
        val theme = player.vars[THEME]
        val board = read(puzzle, theme)
        if (!SlidePuzzle.slide(board, cell)) return
        fill(puzzle, theme, board)
        invTransmit(puzzle)
        if (SlidePuzzle.isSolved(board)) {
            VarPlayerIntMapSetter.set(player, SOLVED, 1)
            mes("You have solved the puzzle!")
        }
    }

    private fun themeOf(box: ItemServerType): Int {
        val canonical = handOutBoxes.indexOf(box.symbol())
        val master = masterBoxes.indexOf(box.symbol())
        return when {
            canonical >= 0 -> canonical + 1
            master >= 0 -> THEMES + master + 1
            else -> box.id % THEMES + 1
        }
    }

    private fun tile(theme: Int, index: Int): String {
        val number = "%02d".format(index + 1)
        return if (theme <= THEMES) {
            "obj.trail_slidingpuzzle${THEME_LETTERS[theme - 1]}$number"
        } else {
            "obj.trail_slidingpuzzle_${masterThemes[theme - THEMES - 1]}_$number"
        }
    }

    private fun fill(puzzle: org.rsmod.game.inv.Inventory, theme: Int, board: IntArray) {
        for (cell in board.indices) {
            puzzle[cell] =
                if (board[cell] == SlidePuzzle.EMPTY) null
                else InvObj(tileType(theme, board[cell]), 1)
        }
    }

    private fun read(puzzle: org.rsmod.game.inv.Inventory, theme: Int): IntArray {
        val ids = (0 until SlidePuzzle.TILES).map { tileType(theme, it).id }
        return IntArray(SlidePuzzle.CELLS) { cell ->
            val obj = puzzle[cell] ?: return@IntArray SlidePuzzle.EMPTY
            ids.indexOf(obj.id)
        }
    }

    private fun tileType(theme: Int, index: Int): ItemServerType =
        requireNotNull(ServerCacheManager.getItem(tile(theme, index).asRSCM(RSCMType.OBJ)))

    private val THEME_LETTERS = listOf("b", "c", "d")
}
