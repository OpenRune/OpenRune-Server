package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.droptable.DropRateModifiers
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.table.ToaVaultDisplaysRow
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.loot.ToaChestTables
import org.rsmod.content.raids.toa.raid.loot.ToaLootRoller
import org.rsmod.content.raids.toa.raid.personalContribution
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class ToaRewardEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    private var rolled = false
    private var sarcophagusWinner: Player? = null
    private var sarcophagusObj: String? = null
    private var revealStarted = false
    internal var forceUnique = false
    var sarcophagusReady = false
        private set
    private val logged = HashSet<Player>()

    override fun onEnter(player: Player) {
        if (!rolled) {
            rolled = true
            rollRewards()
            spawnOsmumten()
        }
        showVault(player)
        player.midiSong(MUSIC)
    }

    private fun rollRewards() {
        val points = raid.players.associateWith { raid.points.lootPoints(it) }
        val level = raid.settings.raidLevel
        val roller =
            ToaLootRoller<Player>(
                random = deps.random,
                pickCommon = ToaChestTables::rollCommon,
                pickUnique = ToaChestTables::rollUnique,
                uniqueBoost = {
                    if (forceUnique) FORCED_BOOST else DropRateModifiers.multiplierFor(it) * UNIQUE_BOOST
                },
            )
        val roll = roller.rollRaid(points, level) { true }
        val winners = roll.uniques.filterValues { !it.consolation }
        sarcophagusWinner = winners.keys.toList().takeIf { it.isNotEmpty() }?.let(deps.random::pick)
        sarcophagusObj = sarcophagusWinner?.let { winners.getValue(it).obj }

        for (player in raid.players) {
            val chest = player.rewardChest
            chest.fillNulls()
            for (slot in chest.indices) chest[slot] = null
            val unique = roll.uniques[player]?.obj
            if (unique != null) store(player, unique, 1)
            if (player in roll.dung) store(player, DUNG, 1)
            for (item in roll.commons[player].orEmpty()) store(player, item.obj, item.count)
        }
    }

    private fun store(player: Player, obj: String, count: Int) {
        player.invAdd(player.rewardChest, obj, count, strict = false)
    }

    fun onFirstOpen(player: Player) {
        if (!logged.add(player)) return
        player.personalContribution = 0
        for (obj in player.rewardChest.filterNotNull { true }) {
            CollectionLog.grant(player, RSCM.getReverseMapping(RSCMType.OBJ, obj.id), obj.count)
        }
    }

    private fun showVault(player: Player) {
        val index = raid.players.indexOf(player)
        for (slot in CHEST_SETTERS.indices) {
            val state =
                when {
                    slot != index || player === sarcophagusWinner -> CHEST_NOT_MINE
                    player.rewardChest.isEmpty() -> CHEST_EMPTY
                    else -> CHEST_MINE
                }
            CHEST_SETTERS[slot](player, state)
        }
        player.sarcophagus =
            if (player === sarcophagusWinner) SARCOPHAGUS_PURPLE else SARCOPHAGUS_WHITE
        player.shouldHaveLoot = if (player.rewardChest.isEmpty()) 0 else 1
    }

    fun sarcophagusFront(): CoordGrid = coords(SARCOPHAGUS_FRONT_TILE)

    fun isSarcophagusWinner(player: Player): Boolean = player === sarcophagusWinner

    fun revealSarcophagus(): Boolean {
        if (revealStarted) return false
        revealStarted = true
        val tile = coords(SARCOPHAGUS_TILE)
        sound(OPEN_SOUNDS)
        val closed = SARCOPHAGUS_CLOSED.firstNotNullOfOrNull { deps.locRepo.findExact(tile, locType(it)) }
        if (closed != null) deps.locRepo.change(closed, locType(SARCOPHAGUS_OPEN), Int.MAX_VALUE)
        schedule(REVEAL_DELAY) { animateLoc(tile, SARCOPHAGUS_OPEN, REVEAL_SEQ) }
        schedule(OPEN_DELAY) {
            animateLoc(tile, SARCOPHAGUS_OPEN, OPEN_SEQ)
            sound(listOf(LOOT_SOUND to 0))
            deps.locRepo.add(
                coords(LOOT_TILE),
                LOOT_LOC,
                Int.MAX_VALUE,
                LocAngle.East,
                LocShape.CentrepieceStraight,
            )
        }
        schedule(ANNOUNCE_DELAY) {
            announceRare()
            sound(listOf(FOUND_SOUND to 0))
            animateLoc(coords(LOOT_TILE), LOOT_LOC, displaySeq())
            sarcophagusReady = true
        }
        return true
    }

    private fun sound(sounds: List<Pair<String, Int>>) {
        val at = coords(SARCOPHAGUS_FRONT_TILE)
        for ((synth, delay) in sounds) {
            deps.worldRepo.soundArea(at, synth, delay = delay, radius = SOUND_RADIUS)
        }
    }

    private fun displaySeq(): String {
        val obj = sarcophagusObj ?: return GLOW_SEQ
        val row = ToaVaultDisplaysRow.all().firstOrNull { it.obj.internalName == obj }
        return row?.let { RSCM.getReverseMapping(RSCMType.SEQ, it.seq.id) } ?: GLOW_SEQ
    }

    private fun announceRare() {
        val winner = sarcophagusWinner ?: return
        val obj = sarcophagusObj ?: return
        val name = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: return
        for (player in players) {
            player.mes(
                "<col=ef1020>${winner.displayName}</col> found something special: " +
                    "<col=ef1020>$name</col>"
            )
        }
    }

    private fun animateLoc(tile: CoordGrid, loc: String, seq: String) {
        val found = deps.locRepo.findExact(tile, locType(loc)) ?: return
        deps.worldRepo.locAnim(found, seq)
    }

    private fun locType(loc: String) =
        checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))

    private fun spawnOsmumten() {
        val tile = room.osmumtenTile ?: return
        spawn(OSMUMTEN, coords(tile), Direction.North).noneMode()
        for (player in players) player.midiJingle(JINGLE)
    }

    companion object {
        const val CHEST_INV = "inv.toa_chests"
        const val CHEST_MINE = 2
        const val CHEST_EMPTY = 4
        const val CHEST_NOT_MINE = 4
        const val SARCOPHAGUS_WHITE = 0
        const val SARCOPHAGUS_PURPLE = 3
        const val SARCOPHAGUS_OPEN = "loc.toa_vault_sarcophagus_open"

        private val SARCOPHAGUS_CLOSED =
            listOf(
                "loc.toa_vault_sarcophagus_parent",
                "loc.toa_vault_sarcophagus_closed",
                "loc.toa_vault_sarcophagus_closed_rare",
            )
        private const val LOOT_LOC = "loc.toa_vault_loot01"
        private const val REVEAL_SEQ = "seq.toa_osmumten_chest_reveal"
        private const val OPEN_SEQ = "seq.toa_osmumten_chest_open"
        private const val GLOW_SEQ = "seq.fx_cycle_ring"
        private const val SOUND_RADIUS = 7
        private const val LOOT_SOUND = "synth.toa_vault_loot_appear"
        private const val FOUND_SOUND = "synth.toa_vault_rare_found"
        private val OPEN_SOUNDS =
            listOf(
                "synth.toa_vault_sarcophagus_open_a" to 20,
                "synth.toa_vault_sarcophagus_open_b" to 37,
                "synth.toa_vault_sarcophagus_open_c" to 132,
            )
        private const val REVEAL_DELAY = 1
        private const val OPEN_DELAY = 8
        private const val ANNOUNCE_DELAY = 15
        private val SARCOPHAGUS_TILE = CoordGrid(3679, 5140, 0)
        private val SARCOPHAGUS_FRONT_TILE = CoordGrid(3680, 5142, 0)
        private val LOOT_TILE = CoordGrid(3679, 5141, 0)

        private var Player.chest0 by intVarBit("varbit.toa_vault_chest_0")
        private var Player.chest1 by intVarBit("varbit.toa_vault_chest_1")
        private var Player.chest2 by intVarBit("varbit.toa_vault_chest_2")
        private var Player.chest3 by intVarBit("varbit.toa_vault_chest_3")
        private var Player.chest4 by intVarBit("varbit.toa_vault_chest_4")
        private var Player.chest5 by intVarBit("varbit.toa_vault_chest_5")
        private var Player.chest6 by intVarBit("varbit.toa_vault_chest_6")
        private var Player.chest7 by intVarBit("varbit.toa_vault_chest_7")

        private val CHEST_SETTERS: List<(Player, Int) -> Unit> =
            listOf(
                { p, v -> p.chest0 = v },
                { p, v -> p.chest1 = v },
                { p, v -> p.chest2 = v },
                { p, v -> p.chest3 = v },
                { p, v -> p.chest4 = v },
                { p, v -> p.chest5 = v },
                { p, v -> p.chest6 = v },
                { p, v -> p.chest7 = v },
            )

        fun markEmpty(player: Player, slot: Int) {
            CHEST_SETTERS[slot](player, CHEST_EMPTY)
            player.shouldHaveLoot = 0
        }

        private var Player.shouldHaveLoot by intVarBit("varbit.toa_should_have_loot")

        private const val UNIQUE_BOOST = 2.0
        private const val FORCED_BOOST = 1_000_000.0
        private const val DUNG = "obj.cert_toa_loot_poo"
        private const val OSMUMTEN = "npc.toa_osmumten_vis"
        private const val JINGLE = "jingle.in_the_pits"
        private const val MUSIC = "midi.toa_laid_to_rest"

        val Player.rewardChest
            get() = invMap.getOrPut(CHEST_INV)

        var Player.sarcophagus by intVarBit("varbit.toa_vault_sarcophagus")
    }
}
