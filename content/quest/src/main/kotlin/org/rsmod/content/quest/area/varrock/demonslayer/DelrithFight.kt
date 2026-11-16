package org.rsmod.content.quest.area.varrock.demonslayer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.SILVERLIGHT
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.WORDS
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Delrith's summoning at the stone circle, the fight and the banishing incantation. */
class DelrithFight
@Inject
constructor(
    private val quest: DemonSlayerQuest,
    private val scenes: DemonSlayerScenes,
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val launcher: ProtectedAccessLauncher,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val playerList: PlayerList,
) : PluginScript() {

    private class Session(val player: Player, val region: Region) {
        val scene = SceneRegion(region)
        val npcs = mutableListOf<Npc>()
        var delrith: Npc? = null
        var banishing = false
        var arrived = false
    }

    private val sessions = HashMap<Long, Session>()

    override fun ScriptContext.startup() {
        onPlayerCoordsChanged { onMoved(player) }
        onEvent<SessionStateEvent.PrepareLogin> { restoreLogin(player) }
        onPlayerLogin { scenes.releaseLeakedRegion(player) }

        val delrith = npcType(DELRITH)
        onModifyNpcHit(delrith) {
            if (hit.attacker()?.righthand.isType(SILVERLIGHT) == true) {
                hit.damage *= 2
            }
        }
        onNpcHit(delrith) { if (npc.hitpoints <= 0) weaken(npc) }
        onOpNpc1(WEAKENED) { banish(it.npc) }
    }

    private fun restoreLogin(player: Player) {
        player.uuid?.let(sessions::remove)?.let(::dispose)
        val tile = player.demonSlayerReturnTile
        if (tile == 0) return
        player.coords = CoordGrid(tile)
        player.demonSlayerReturnTile = 0
        VarPlayerIntMapSetter.set(player, SEEN_SUMMONING, 0)
    }

    private fun onMoved(player: Player) {
        val uuid = player.uuid ?: return
        val session = sessions[uuid]
        if (session != null) {
            if (session.arrived && !session.banishing) leaveIfOutside(player, session)
            return
        }
        if (!quest.isReadyForDelrith(player) || !inTriggerRing(player.coords)) {
            return
        }
        if (player.inv.count(SILVERLIGHT) == 0 && !player.righthand.isType(SILVERLIGHT)) {
            return
        }
        launcher.launch(player) { summon() }
    }

    private fun inTriggerRing(coords: CoordGrid): Boolean =
        coords.level == 0 &&
            coords.x in StoneCircle.Table.x - TRIGGER_RADIUS..StoneCircle.Table.x + TRIGGER_RADIUS &&
            coords.z in StoneCircle.Table.z - TRIGGER_RADIUS..StoneCircle.Table.z + TRIGGER_RADIUS

    private fun leaveIfOutside(player: Player, session: Session) {
        val coords = player.coords
        val region = session.region
        val inside =
            coords.x in region.southWest.x..region.northEast.x &&
                coords.z in region.southWest.z..region.northEast.z
        if (!inside) {
            end(player)
            return
        }
        val static = StoneCircle.toStatic(region, coords)
        if (static.chebyshev(StoneCircle.Table) <= EXIT_RADIUS) {
            return
        }
        end(player)
        launcher.launch(player) { telejump(static) }
    }

    private fun CoordGrid.chebyshev(other: CoordGrid): Int =
        maxOf(kotlin.math.abs(x - other.x), kotlin.math.abs(z - other.z))

    private suspend fun ProtectedAccess.summon() {
        val uuid = player.uuid ?: return
        if (sessions.containsKey(uuid)) return
        val region = regionRepo.add(StoneCircle.Template) ?: return
        regionRepo.protect(region)
        val session = Session(player, region)
        sessions[uuid] = session
        val r = session.scene
        val start = player.coords
        player.demonSlayerReturnTile = start.packed
        delay(REGION_BUILD_TICKS)
        telejump(r[start])
        session.arrived = true
        minimapHideMap()
        midiSong("midi.delrith_summoning")

        val wizards = WIZARDS.map { (type, tile) -> spawn(session, type, r[tile]) }
        val denath = spawn(session, DENATH, r[DENATH_TILE])
        for (npc in wizards + denath) {
            npc.faceSquare(r[StoneCircle.Table], 2, 2)
            npc.anim("seq.qip_ds_dark_wizard_chanting")
        }
        delay(1)
        startDialogue {
            chatNpcSpecific(DENATH_TITLE, DENATH, angry, "Arise, O mighty Delrith! Bring " +
                "destruction to this soft, weak city!")
            for (npc in wizards + denath) npc.say("Arise, Delrith!")
            chatNpcSpecific("Dark wizards", WIZARDS.first().first, angry, "Arise, Delrith!")
        }
        mesboxNp("The wizards cast an evil spell...")
        val table = r[StoneCircle.Table]
        locRepo.add(table, "loc.qip_ds_stone_table_explosion", SCENE_TICKS, LocAngle.West,
            LocShape.CentrepieceStraight)
        soundSynth("synth.summon_npc")
        soundSynth("synth.ds_table_explosion")
        delay(1)
        locRepo.findAll(table).firstOrNull()?.let {
            worldRepo.locAnim(it, "seq.qip_ds_table_explosion")
        }
        delay(1)
        camShake(CamShakeAxis.LEFT_RIGHT, 15, 0, 0)
        delay(1)
        camShakeReset(CamShakeAxis.LEFT_RIGHT)
        delay(3)
        val delrith = spawn(session, DELRITH, table)
        session.delrith = delrith
        delrith.anim("seq.qip_ds_delrith_summoned")
        locRepo.add(table, "loc.qip_ds_stone_table_summoning", SCENE_TICKS, LocAngle.West,
            LocShape.CentrepieceStraight)
        soundSynth("synth.ds_break_table", delay = 10)
        soundSynth("synth.ds_delrith_appear")
        delay(5)
        locRepo.add(table, "loc.qip_ds_stone_table_destroyed", SCENE_TICKS, LocAngle.West,
            LocShape.CentrepieceStraight)
        delrith.walk(r[DELRITH_STAND])
        delay(3)
        for (npc in wizards + denath) {
            npc.resetAnim()
            npc.faceSquare(delrith.coords, 2, 2)
        }
        startDialogue {
            chatNpcSpecific(DENATH_TITLE, DENATH, laugh, "Ha ha ha! At last you are free, my " +
                "demonic brother! Rest now, and then have your revenge on this pitiful city!")
            for (npc in wizards) npc.facePlayer(player)
            chatNpcSpecific("Dark wizard", WIZARDS.first().first, quiz, "Who's that?")
            chatNpcSpecific(DENATH_TITLE, DENATH, shocked, "Noo! Not Silverlight! Delrith is " +
                "not ready yet!")
            chatNpcSpecific(DENATH_TITLE, DENATH, worried, "I've got to get out of here...")
        }
        VarPlayerIntMapSetter.set(player, SEEN_SUMMONING, 1)
        minimapReset()
        camReset()
        denath.walk(r[DENATH_ESCAPE])
        npcRepo.del(denath, DENATH_ESCAPE_TICKS)
        session.npcs.remove(denath)
        for (npc in wizards + delrith) {
            npc.defaultMode()
            npc.opPlayer2(player, aiPlayerInteractions)
        }
    }

    private fun spawn(session: Session, type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, SCENE_TICKS)
        npc.noneMode()
        session.npcs += npc
        return npc
    }

    private fun weaken(delrith: Npc) {
        delrith.clearQueue(DEATH_QUEUE)
        val session = sessions.values.firstOrNull { it.delrith === delrith } ?: return
        val weakened = Npc(WEAKENED, delrith.coords)
        if (delrith.isSlotAssigned) npcRepo.del(delrith, Int.MAX_VALUE)
        session.npcs.remove(delrith)
        npcRepo.add(weakened, SCENE_TICKS)
        weakened.noneMode()
        session.npcs += weakened
        session.delrith = weakened
        for (wizard in session.wizards()) wizard.resetMode()
        launcher.launch(session.player) {
            stopAction()
            combatClearQueue()
            soundSynth("synth.ds_portal_open")
            banish(weakened)
        }
    }

    private suspend fun ProtectedAccess.banish(weakened: Npc) {
        val session = player.uuid?.let(sessions::get) ?: return
        if (session.delrith !== weakened || session.banishing) return
        session.banishing = true
        try {
            faceSquare(weakened.coords)
            startDialogue { chatPlayer(quiz, "Now what was that incantation again?") }
            val expected = DemonSlayerQuest.INCANTATION_VARBITS.map { player.vars[it] }
            val chosen = mutableListOf<Int>()
            for (slot in expected.indices) {
                var word = -1
                startDialogue { word = menu(WORDS.mapIndexed { i, w -> w to i }, title = WORD_TITLE) }
                chosen += word
                say(WORDS.getOrElse(word) { "" } + if (slot == expected.lastIndex) "!" else "...")
                delay(1)
            }
            if (chosen != expected) {
                mesbox("The vortex collapses. That was the wrong incantation.")
                reviveDelrith(session, weakened)
                return
            }
            mesboxNp("Delrith is sucked into the vortex...")
            weakened.anim("seq.qip_ds_delrith_banished")
            soundSynth("synth.ds_delrith_banished", delay = 10)
            delay(BANISH_TICKS)
            VarPlayerIntMapSetter.set(player, SEEN_SUMMONING, 0)
            if (weakened.isSlotAssigned) npcRepo.del(weakened, Int.MAX_VALUE)
            val table = session.scene[StoneCircle.Table]
            locRepo.add(table, "loc.qip_ds_stone_table", SCENE_TICKS, LocAngle.West,
                LocShape.CentrepieceStraight)
            soundSynth("synth.ds_replace_table")
            mesbox("...back into the dark dimension from which he came.")
            val exit = StoneCircle.toStatic(session.region, player.coords)
            end(player)
            quest.completeBanishing(this)
            telejump(exit)
            anim("seq.qip_ds_recieving_silverlight")
            spotanim("spotanim.qip_ds_sword_glint_spotanim")
            soundSynth("synth.equip_silverlight")
        } finally {
            session.banishing = false
        }
    }

    private fun ProtectedAccess.reviveDelrith(session: Session, weakened: Npc) {
        weakened.anim("seq.qip_ds_delrith_portal_failed")
        soundSynth("synth.ds_portal_close")
        val delrith = Npc(DELRITH, weakened.coords)
        if (weakened.isSlotAssigned) npcRepo.del(weakened, Int.MAX_VALUE)
        session.npcs.remove(weakened)
        npcRepo.add(delrith, SCENE_TICKS)
        session.npcs += delrith
        session.delrith = delrith
        delrith.opPlayer2(player, aiPlayerInteractions)
        for (wizard in session.wizards()) {
            wizard.defaultMode()
            wizard.opPlayer2(player, aiPlayerInteractions)
        }
    }

    private fun Session.wizards(): List<Npc> =
        npcs.filter { npc -> npc.isSlotAssigned && WIZARDS.any { npc.isType(it.first) } }

    private fun end(player: Player) {
        val session = player.uuid?.let(sessions::remove) ?: return
        player.demonSlayerReturnTile = 0
        VarPlayerIntMapSetter.set(player, SEEN_SUMMONING, 0)
        dispose(session)
    }

    private fun dispose(session: Session) {
        for (npc in session.npcs) {
            if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
        }
        session.npcs.clear()
        regionRepo.unprotect(session.region)
    }

    private fun HitBuilder.attacker(): Player? =
        if (isFromPlayer) sourceUid?.let { PlayerUid(it).resolve(playerList) } else null

    private companion object {
        const val DELRITH = "npc.delrith"
        const val WEAKENED = "npc.delrith_weakened"
        const val DENATH = "npc.qip_ds_dark_wizard_denath_sneaky"
        const val DENATH_TITLE = "Denath"
        const val WORD_TITLE = "Select a word"
        const val SEEN_SUMMONING = "varbit.delrith_seen_summoning_cutscene"
        const val DEATH_QUEUE = "queue.death"

        const val TRIGGER_RADIUS = 6
        const val REGION_BUILD_TICKS = 2
        const val EXIT_RADIUS = 14
        const val SCENE_TICKS = 3000
        const val BANISH_TICKS = 16
        const val DENATH_ESCAPE_TICKS = 6

        val WIZARDS =
            listOf(
                "npc.qip_ds_young_dark_wizard1" to CoordGrid(3226, 3371, 0),
                "npc.qip_ds_young_dark_wizard2" to CoordGrid(3229, 3371, 0),
                "npc.qip_ds_young_dark_wizard3" to CoordGrid(3226, 3368, 0),
            )
        val DENATH_TILE = CoordGrid(3229, 3368, 0)
        val DENATH_ESCAPE = CoordGrid(3236, 3368, 0)
        val DELRITH_STAND = CoordGrid(3227, 3367, 0)

        fun npcType(name: String) = checkNotNull(ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)))
    }
}

/** Delrith can only be fought with Silverlight wielded. */
class DelrithAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType("npc.delrith") || player.righthand.isType(SILVERLIGHT)) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny("I'd better wield Silverlight first.")
    }

    override val stopsApproach: Boolean
        get() = true
}
