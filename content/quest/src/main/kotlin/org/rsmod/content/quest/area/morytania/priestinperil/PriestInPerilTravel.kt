package org.rsmod.content.quest.area.morytania.priestinperil

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.BLESSED_WATER
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.CELL_UNLOCKED
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.COFFIN_SEALED
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.GOLD_KEY
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.GUARDIAN_DEAD
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.GUARDIAN_REQUESTED
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.IRON_KEY
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.MET_DREZEL
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.MURKY_WATER
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.RETURNED_TO_ROALD
import org.rsmod.content.quest.area.morytania.priestinperil.PriestInPeril.Companion.STARTED
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Translation
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Native trapdoor IDs are already owned by EdgevilleLocs, which delegates only this location. */
@Singleton
class PriestInPerilTravel
@Inject
constructor(
    private val quest: PriestInPeril,
    private val instances: InstanceManager,
    private val locs: LocRepository,
    private val clock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // The old members' gates are still present on the only western approach.
        for (gate in listOf("loc.fai_varrock_member_gatel", "loc.fai_varrock_member_gater")) {
            onOpLoc1(gate) {
                arriveDelay()
                crossWall(it.loc)
            }
        }
        for (stairs in listOf("loc.paterdomus_spiralstairs", "loc.paterdomus_spiralstairs_m")) {
            onOpLoc1(stairs) {
                arriveDelay()
                anim("seq.human_climbing")
                delay(1)
                telejump(it.loc.coords.translate(Translation(x = -1, z = 1, level = 1)))
                resetAnim()
            }
        }
        onOpLoc1("loc.spiralstairstop_m") {
            arriveDelay()
            anim("seq.human_climbing_down")
            delay(1)
            telejump(it.loc.coords.translate(Translation(x = -1, z = 1, level = -1)))
            resetAnim()
        }
        for (door in listOf("loc.priestperiltempledoorl", "loc.priestperiltempledoorr")) {
            onOpLoc1(door) {
                arriveDelay()
                if (coords.x <= 3408 && quest.stage(player) < RETURNED_TO_ROALD)
                    startDialogue { templeDoor() }
                else crossWall(it.loc)
            }
        }
        onOpLoc1("loc.pip_underground_door1") {
            arriveDelay()
            if (isGuardianInstance(player)) {
                mes("I should report the guardian's death to King Roald first.")
                return@onOpLoc1
            }
            if (coords.z >= 9895 && !quest.canEnterMausoleum(player))
                mes("The gate is locked. A golden key might fit.")
            else if (
                crossWall(it.loc) && GOLD_KEY in inv && quest.stage(player) >= RETURNED_TO_ROALD
            ) {
                VarPlayerIntMapSetter.set(player, PriestInPeril.MAUSOLEUM_UNLOCKED, 1)
            }
        }
        onOpLoc1("loc.pip_underground_door2") {
            arriveDelay()
            if (quest.stage(player) < PriestInPeril.MOVED_UNDERGROUND)
                mes("I should help Drezel leave the temple before going further.")
            else crossWall(it.loc)
        }
        onOpLoc1("loc.pip_prisondoor") { cellDoor(it.loc) }
        onOpLocU("loc.pip_prisondoor", IRON_KEY) { cellDoor(it.loc) }
        onOpLocU("loc.pip_prisondoor", GOLD_KEY) {
            arriveDelay()
            mes("The golden key doesn't fit this lock.")
        }
        for (monument in 1..7) {
            val loc = "loc.priestperil_grave_base$monument"
            onOpLoc1(loc) {
                arriveDelay()
                if (monument == 4)
                    objbox(
                        IRON_KEY,
                        "The inscription honours a priest whose faith was the key to the Salve's protection. An iron key rests in the monument.",
                    )
                else
                    mesbox(
                        "This monument honours one of the seven priests who blessed the River Salve."
                    )
            }
            onOpLoc2(loc) {
                arriveDelay()
                mes("I should leave something of the same shape in its place.")
            }
        }
        onOpLocU("loc.priestperil_grave_base4", GOLD_KEY) {
            arriveDelay()
            if (quest.stage(player) !in MET_DREZEL..COFFIN_SEALED) {
                mes("I have no reason to exchange this key yet.")
                return@onOpLocU
            }
            if (IRON_KEY in inv || IRON_KEY in player.invMap.getOrPut("inv.bank")) {
                mes("You have already exchanged the golden key for the iron key.")
                return@onOpLocU
            }
            if (quest.exchange(this, GOLD_KEY, IRON_KEY))
                objbox(IRON_KEY, "You exchange the golden replica for the iron key.")
        }
        onOpLoc1("loc.priestperil_well") {
            arriveDelay()
            mesbox("The water below comes from the River Salve. I could collect some in a bucket.")
        }
        onOpLocU("loc.priestperil_well", "obj.bucket_empty") {
            arriveDelay()
            if (quest.stage(player) !in MET_DREZEL..COFFIN_SEALED) {
                mes("I don't need this water just now.")
                return@onOpLocU
            }
            if (quest.exchange(this, "obj.bucket_empty", MURKY_WATER))
                objbox(MURKY_WATER, "You draw murky water from the well.")
        }
        onOpLoc1("loc.priestperil_coffin_noanim") {
            arriveDelay()
            mesbox("A vampyre lies within. Opening the coffin would be unwise.")
        }
        onOpLocU("loc.priestperil_coffin_noanim", BLESSED_WATER) {
            arriveDelay()
            if (quest.stage(player) != CELL_UNLOCKED) {
                mes("The coffin does not need more blessed water.")
                return@onOpLocU
            }
            anim("seq.human_pickupfloor")
            delay(1)
            if (
                quest.stage(player) == CELL_UNLOCKED &&
                    quest.exchange(this, BLESSED_WATER, "obj.bucket_empty")
            ) {
                quest.quest.setQuestStage(this, COFFIN_SEALED)
                mesbox("You pour the blessed water over the coffin, sealing the vampyre inside.")
            }
        }
        onOpLoc1("loc.pip_underground_wall_side_withportal") {
            arriveDelay()
            if (!player.members) {
                mes("You need to be on a members' world to enter Morytania.")
                return@onOpLoc1
            }
            if (!quest.canPass(player)) {
                mesbox(
                    "The holy barrier stops you. I should speak to Drezel before passing into Morytania."
                )
                return@onOpLoc1
            }
            // Native passage exits at the eastern surface trapdoor, not an adjacent dungeon tile.
            anim("seq.human_climbing")
            delay(1)
            if (!player.members) {
                resetAnim()
                return@onOpLoc1
            }
            telejump(MORYTANIA_EXIT)
            resetAnim()
        }
        onOpLoc1("loc.pipeastsidetrapdoor") {
            arriveDelay()
            locs.change(it.loc, "loc.pipeastsidetrapdoor_open", 500)
        }
        onOpLoc2("loc.pipeastsidetrapdoor_open") {
            arriveDelay()
            locs.change(it.loc, "loc.pipeastsidetrapdoor", 500)
        }
        onOpLoc1("loc.pipeastsidetrapdoor_open") {
            arriveDelay()
            if (!quest.canPass(player)) {
                mes("The holy protection prevents me entering. I need Drezel's blessing.")
                return@onOpLoc1
            }
            anim("seq.human_climbing_down")
            delay(1)
            telejump(MORYTANIA_RETURN)
            resetAnim()
        }
        // Immunity applies before damage/XP is calculated; the private guardian retains native
        // stats.
        for (npcName in listOf("npc.priestperilguarddog", "npc.priestperil_guardian_model")) {
            val type = checkNotNull(ServerCacheManager.getNpc(npcName.asRSCM()))
            onEvent<NpcStateEvents.Spawn>(type.id.toLong()) {
                npc.vars["varn.immune_magic"] = 1
                // The raw map also contains public placeholder spawns. Only the private fight is
                // playable.
                if (npc.coords == GUARDIAN) {
                    npc.noneMode()
                    npc.lifecycleDelCycle = clock.cycle + 1
                }
            }
            onModifyNpcHit(type) { if (hit.type == HitType.Magic) hit.damage = 0 }
        }
    }

    internal fun isGuardianInstance(player: Player) =
        instances.sessionForPlayer(player)?.key == GUARDIAN_INSTANCE

    /** Returns true only when this quest owns the otherwise shared trapdoor interaction. */
    suspend fun enterTrapdoor(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        if (loc.coords != TRAPDOOR) return false
        with(access) {
            arriveDelay()
            if (quest.stage(player) < GUARDIAN_REQUESTED) {
                mes("I should investigate the temple door before entering this mausoleum.")
                return true
            }
            anim("seq.human_climbing_down")
            delay(1)
            if (quest.stage(player) != GUARDIAN_REQUESTED) {
                telejump(DUNGEON_ENTRY)
                resetAnim()
                return true
            }
            val area =
                InstanceArea.copyRegions(
                    regionIds = listOf((53 shl 8) or 154),
                    enterCoord =
                        org.rsmod.api.instances.RegionLocal(
                            0,
                            DUNGEON_ENTRY.mx,
                            DUNGEON_ENTRY.mz,
                            DUNGEON_ENTRY.lx,
                            DUNGEON_ENTRY.lz,
                        ),
                    exitCoord = SURFACE_ENTRY,
                    npcSpawns = listOf(InstanceNpc("npc.priestperilguarddog", GUARDIAN)),
                )
            val spec =
                InstanceSpec(
                    fee = 0,
                    maxPlayers = 1,
                    reclaimTicks = 100,
                    graceTicks = 100,
                    destroyWhenEmpty = true,
                    area = area,
                    settingsRowId = -1,
                )
            when (
                val result =
                    instances.create(
                        player,
                        GUARDIAN_INSTANCE,
                        spec,
                        InstanceAccess.Private,
                        mapClock,
                    )
            ) {
                is InstanceManager.Result.Created -> {
                    telejump(result.enter)
                    instances.finalizeEntry(player, result.session, mapClock)
                }
                is InstanceManager.Result.Failed -> mes(result.reason)
                else -> mes("The guardian's chamber is not available right now.")
            }
            resetAnim()
        }
        return true
    }

    suspend fun leaveDungeon(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val session = instances.sessionForPlayer(access.player)
        if (session?.key != GUARDIAN_INSTANCE && loc.coords != DUNGEON_LADDER) return false
        with(access) {
            arriveDelay()
            anim("seq.human_climbing")
            delay(1)
            if (session?.key == GUARDIAN_INSTANCE) instances.leave(player, session, mapClock)
            telejump(SURFACE_ENTRY)
            resetAnim()
        }
        return true
    }

    private suspend fun Dialogue.templeDoor() {
        when (quest.stage(player)) {
            STARTED -> {
                mesbox("A voice behind the door asks what you want.")
                if (choice2("King Roald sent me to check on Drezel.", 1, "Nothing, sorry.", 2) != 1)
                    return
                mesbox(
                    "The men inside ask you to kill a dangerous dog beneath the northern trapdoor."
                )
                quest.quest.setQuestStage(access, GUARDIAN_REQUESTED)
            }
            GUARDIAN_REQUESTED ->
                mesbox("The voice asks whether you have killed the dog in the mausoleum yet.")
            GUARDIAN_DEAD -> mesbox("Nobody answers. I should tell King Roald what happened.")
            else -> mesbox("The temple door is shut. King Roald may know more about this place.")
        }
    }

    private suspend fun ProtectedAccess.cellDoor(loc: BoundLocInfo) {
        arriveDelay()
        if (
            quest.stage(player) < MET_DREZEL ||
                (quest.stage(player) == MET_DREZEL && IRON_KEY !in inv)
        ) {
            mes("The cell gate is locked.")
            return
        }
        if (quest.stage(player) == MET_DREZEL) quest.quest.setQuestStage(this, CELL_UNLOCKED)
        crossWall(loc)
    }

    private suspend fun ProtectedAccess.crossWall(loc: BoundLocInfo): Boolean {
        val across =
            when (loc.angle.id) {
                0 -> loc.coords.translateX(-1)
                1 -> loc.coords.translateZ(1)
                2 -> loc.coords.translateX(1)
                else -> loc.coords.translateZ(-1)
            }
        val start =
            if (coords.chebyshevDistance(across) < coords.chebyshevDistance(loc.coords)) across
            else loc.coords
        val destination = if (start == across) loc.coords else across
        if (coords != start) {
            playerWalkWithMinDelay(start)
            if (coords != start) return false
        }
        soundSynth("synth.door_open")
        locs.del(loc, 3)
        val facing =
            when {
                destination.x > start.x -> constants.em_face_east
                destination.x < start.x -> constants.em_face_west
                destination.z > start.z -> constants.em_face_north
                else -> constants.em_face_south
            }
        exactMove(start, destination, 0, 30, facing, TeleportType.Exempt)
        delay(1)
        return coords == destination
    }

    companion object {
        internal const val GUARDIAN_INSTANCE = "quest_priestinperil_guardian"
        internal val TRAPDOOR = CoordGrid(3405, 3507, 0)
        internal val SURFACE_ENTRY = CoordGrid(3405, 3506, 0)
        internal val DUNGEON_LADDER = CoordGrid(3405, 9907, 0)
        internal val DUNGEON_ENTRY = CoordGrid(3405, 9906, 0)
        internal val MORYTANIA_EXIT = CoordGrid(3423, 3485, 0)
        internal val MORYTANIA_RETURN = CoordGrid(3440, 9887, 0)
        internal val GUARDIAN = CoordGrid(3405, 9902, 0)
    }
}
