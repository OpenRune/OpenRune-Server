package org.rsmod.content.raids.toa.raid

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.hasProtectItemPrayer
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.party.ToaLobbyParty
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.supplies.ToaSupply
import org.rsmod.game.entity.Player

object ToaRaidManager {

    private val CURRENT_RAID = AttributeKey<ToaRaid>()

    var Player.currentRaid: ToaRaid?
        get() = attr[CURRENT_RAID]
        private set(value) {
            if (value != null) attr[CURRENT_RAID] = value else attr.remove(CURRENT_RAID)
        }

    private val raids = HashMap<ToaLobbyParty, ToaRaid>()

    private var nextControllerId = 1

    internal fun nextControllerId(): Int = nextControllerId++

    var Player.toaController by intVarp("varp.toa_mycontroller")
    private var Player.hudPartySlot by intVarBit("varbit.toa_client_partyslot")
    private var Player.hudRaidLevel by intVarBit("varbit.toa_client_raid_level")
    private var Player.hudCurrentPath by intVarBit("varbit.toa_client_current_path")

    private var Player.kickedFromRaid by intVarBit("varbit.toa_kicked_from_raid")

    const val CONTROLLER_NONE = -1

    private const val HUD_STATE_EMPTY = 0
    private const val HUD_STATE_ZERO_HEALTH = 1
    private const val HUD_STATE_FULL_HEALTH = 27
    private const val HUD_STATE_DEAD = 30
    private const val HUD_STATE_ELSEWHERE = 31

    private const val HUD_REFRESH_TICKS = 3

    private const val SCRIPT_HUD_STATUS_NAMES = 6585
    private const val SCRIPT_SPEEDRUN_TIME_UPDATE = 6580

    fun raidFor(party: ToaLobbyParty): ToaRaid? = raids[party]

    fun isInRaid(player: Player): Boolean = player.currentRaid != null

    fun start(party: ToaLobbyParty, deps: ToaRaidDeps): ToaRaid? {
        val raid = ToaRaid(party, party.settings.copy(), deps)
        raid.advanceTo(ToaRoom.MAIN_HALL) ?: return null
        raids[party] = raid
        scheduleHudRefresh(raid)
        return raid
    }

    private fun scheduleHudRefresh(raid: ToaRaid) {
        raid.deps.worldQueues.add(HUD_REFRESH_TICKS) {
            if (raid.ended) return@add
            refreshHudStates(raid)
            scheduleHudRefresh(raid)
        }
    }

    fun moveTo(player: Player, encounter: ToaEncounter) {
        val raid = encounter.raid
        val room = encounter.room
        val firstEntry = !raid.isInside(player)

        player.currentRaid = raid
        raid.place(player, encounter)
        raid.deps.network.extendNpcView(player)
        player.toaController = encounter.controllerId
        player.kickedFromRaid = if (room.kind == ToaRoom.Kind.MAIN_HALL) 0 else 1
        player.hudCurrentPath = room.hudPath
        if (firstEntry) {
            ToaPartyManager.setPartyStatus(player, ToaPartyManager.PARTY_STATUS_IN_PARTY)
            ToaStats.recordAttempt(player, raid.settings.mode)
        }

        if (room.kind != ToaRoom.Kind.MAIN_HALL && !raid.timerStarted) {
            raid.startTimer(raid.deps.mapClock.cycle)
            raid.timeLimitMinutes?.let { limit ->
                for (member in raid.players) {
                    member.mes("Overall time to beat: <col=ef1020>$limit:00</col>. The timer has started!")
                }
            }
            refreshTimers(raid)
        }

        refreshHudStates(raid)
    }

    fun leave(player: Player, logout: Boolean) {
        val raid = player.currentRaid ?: return
        val room = raid.encounterOf(player)

        var unsafeLogout = false
        if (logout && room != null && room.stage == ToaStage.STARTED && room.inChallengeArea(player)) {
            unsafeLogout = true
            raid.totalDeaths++
            ToaStats.recordDeath(player, raid.settings.mode)
            for (other in room.players) {
                if (other !== player) {
                    other.mes(
                        "<col=ff0000>${player.displayName}</col> has logged out. " +
                            "Total deaths: <col=ff0000>${raid.totalDeaths}</col>."
                    )
                }
            }
        }

        raid.revive(player)
        raid.stopDying(player)
        removeRaidItems(player)
        raid.deps.supplyEffects.clearSessionEffects(player)

        if (unsafeLogout && raid.permittedTeamDeaths != null) {
            val deps = raid.deps
            val protectItem = player.hasProtectItemPrayer()
            ToaRetrieval.store(player, deps.deathDrops, deps.marketPrices, protectItem)
        }
        raid.deps.network.resetNpcView(player)
        player.currentRaid = null
        raid.remove(player)
        raid.removePlayer(player)

        if (!logout) {
            resetClientVars(player)
        }

        val party = raid.lobbyParty
        if (ToaPartyManager.leaveParty(player)) {
            ToaPartyManager.refreshViewers(party, exclude = player)
        }

        refreshHud(raid)
        room?.checkRoomReset()
        if (raid.players.isEmpty()) end(raid)
    }

    fun onLeftParty(player: Player, party: ToaLobbyParty) {
        val raid = raids[party] ?: return
        if (raid.isInside(player)) return
        if (!raid.removePlayer(player)) return
        refreshHud(raid)
        if (raid.players.isEmpty()) end(raid)
    }

    private fun end(raid: ToaRaid) {
        raid.ended = true
        raids.remove(raid.lobbyParty)
        raid.destroyAll()
    }

    fun removeRaidItems(player: Player) {
        for (slot in player.inv.indices) {
            val obj = player.inv[slot] ?: continue
            if (obj.id in RAID_ITEM_IDS) player.inv[slot] = null
        }
        val bag = player.invMap[SUPPLY_BAG_INV] ?: return
        for (slot in bag.indices) bag[slot] = null
    }

    private const val SUPPLY_BAG_INV = "inv.toa_midraidloot_bag"

    private val RAID_ITEM_IDS: Set<Int> by lazy {
        val names =
            listOf("obj.toa_crondis_water_container", "obj.toa_honey_locust", SUPPLY_BAG) +
                ToaSupply.ALL_OBJS
        names.mapTo(HashSet()) { it.asRSCM(RSCMType.OBJ) }
    }

    private const val SUPPLY_BAG = "obj.toa_midraidloot_bag"

    fun resetClientVars(player: Player) {
        player.toaController = CONTROLLER_NONE
        player.personalContribution = 0
        player.hudPartySlot = 0
        player.hudRaidLevel = 0
        player.hudCurrentPath = 0
        player.kickedFromRaid = 0
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            VarPlayerIntMapSetter.set(player, "varbit.toa_client_p$i", HUD_STATE_EMPTY)
        }
        for (path in ToaPath.entries) {
            VarPlayerIntMapSetter.set(player, path.levelVarbit, 0)
        }
        ToaDamage.resetAll(player)
    }

    fun sendHud(viewer: Player, raid: ToaRaid) {
        viewer.hudPartySlot = raid.players.indexOf(viewer) + 1
        viewer.hudRaidLevel = raid.settings.raidLevel
        viewer.hudCurrentPath = raid.encounterOf(viewer)?.room?.hudPath ?: 0
        sendHudStates(viewer, raid)
        for (path in ToaPath.entries) {
            VarPlayerIntMapSetter.set(viewer, path.levelVarbit, raid.pathLevels[path.ordinal])
        }

        val names = Array(ToaLobbyParty.MAX_PARTY_MEMBERS) { i ->
            raid.players.getOrNull(i)?.displayName ?: ""
        }
        viewer.runClientScript(SCRIPT_HUD_STATUS_NAMES, *names)
        sendTimer(viewer, raid)
    }

    fun refreshHud(raid: ToaRaid) {
        for (player in raid.players) {
            if (raid.isInside(player)) sendHud(player, raid)
        }
    }

    fun refreshHudStates(raid: ToaRaid) {
        for (player in raid.players) {
            if (raid.isInside(player)) sendHudStates(player, raid)
        }
    }

    fun refreshTimers(raid: ToaRaid) {
        for (player in raid.players) {
            if (raid.isInside(player)) sendTimer(player, raid)
        }
    }

    private fun sendHudStates(viewer: Player, raid: ToaRaid) {
        val viewerRoom = raid.encounterOf(viewer)
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            val member = raid.players.getOrNull(i)
            val state =
                when {
                    member == null -> HUD_STATE_EMPTY
                    raid.encounterOf(member) !== viewerRoom -> HUD_STATE_ELSEWHERE
                    raid.isGhost(member) -> HUD_STATE_DEAD
                    else -> healthState(member)
                }
            VarPlayerIntMapSetter.set(viewer, "varbit.toa_client_p$i", state)
        }
    }

    private fun sendTimer(viewer: Player, raid: ToaRaid) {
        val ticks = raid.elapsedTicks(raid.deps.mapClock.cycle)
        val frozen = !raid.timerStarted || raid.finished
        viewer.runClientScript(SCRIPT_SPEEDRUN_TIME_UPDATE, ticks, if (frozen) 1 else 0)
    }

    private fun healthState(player: Player): Int {
        if (player.hitpoints <= 0) return HUD_STATE_ZERO_HEALTH
        val max = player.baseHitpointsLvl.coerceAtLeast(1)
        val steps = HUD_STATE_FULL_HEALTH - HUD_STATE_ZERO_HEALTH
        val scaled = (player.hitpoints * steps / max).coerceIn(1, steps)
        return HUD_STATE_ZERO_HEALTH + scaled
    }
}
