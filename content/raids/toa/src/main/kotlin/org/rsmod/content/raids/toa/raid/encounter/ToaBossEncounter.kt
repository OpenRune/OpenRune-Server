package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionPlayer
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

open class ToaBossEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    internal open val fight: ToaFight? = null

    override val hpBar: ToaHpBar? by lazy { fight?.let { ToaHpBar(this, subject = it.bar) } }

    override val combatantSpecs: List<ToaCombatant.Spec>
        get() = fight?.combatants.orEmpty()

    protected open val osmumtenDelay: Int
        get() = fight?.death?.modelDelay ?: 0

    protected open val loot: ToaBossLoot? = null

    protected open val lootSource: Npc?
        get() = fight?.boss?.invoke()

    protected open fun onFightStart(boss: Npc) {}

    protected open fun onFightTick(boss: Npc, targets: List<Player>) {}

    protected open fun onFightComplete() {}

    internal fun bossAlive(npc: Npc): Boolean =
        stage == ToaStage.STARTED && npc === fight?.boss?.invoke() && npc.hitpoints > 0

    internal fun fighting(npc: Npc): Boolean = bossAlive(npc) && targets().isNotEmpty()

    final override fun onStart() {
        val fight = fight ?: return
        val boss = fight.boss() ?: return
        scaleCombatants()
        onFightStart(boss)
        fight.music?.let { music -> for (player in players) player.midiSong(music) }
        boss.ignoreCombatInteractions = false
        deps.bossDeps.encounterRegistry.remove(boss)
        fight.attackRate?.let { deps.bossDeps.encounter(boss).attackRateOverride = it() }
        if (fight.firstAttackDelay > 0) deps.bossDeps.suppressAttacks(boss, fight.firstAttackDelay)
        engage(boss, targets())
    }

    final override fun onTick(targets: List<Player>) {
        val boss = fight?.boss?.invoke() ?: return
        holdDefenceFloors()
        onFightTick(boss, targets)
        engage(boss, targets)
    }

    override fun onEnter(player: Player) {
        if (stage == ToaStage.STARTED) fight?.music?.let { player.midiSong(it) }
    }

    final override fun onComplete() {
        onFightComplete()
        fight?.let(::playDeath)
        val path = room.path ?: return
        if (path !in raid.pathsCompleted) {
            raid.pathsCompleted += path
        }
        if (osmumtenDelay > 0) schedule(osmumtenDelay) { reward() } else reward()
    }

    private fun engage(boss: Npc, targets: List<Player>) {
        if (boss.hitpoints <= 0) return
        if (targets.isEmpty()) {
            if (boss.interaction != null) boss.noneMode()
            return
        }
        val current = (boss.interaction as? InteractionPlayer)?.target
        if (current != null && current in targets) return
        boss.apPlayer2(targets[deps.random.of(0, targets.lastIndex)], deps.aiInteractions)
    }

    private fun playDeath(fight: ToaFight) {
        val death = fight.death ?: return
        val boss = fight.boss() ?: return
        if (!boss.isSlotAssigned) return
        release(boss)
        boss.ignoreCombatInteractions = true
        boss.noneMode()
        boss.hideAllOps()
        boss.anim(death.anim)
        val parts = death.parts.map { it to it.npc() }
        for ((part, npc) in parts) npc?.anim(part.anim)
        death.shake?.let(::shakeCameras)
        schedule(death.modelDelay) {
            if (boss.isSlotAssigned) boss.transmog(npcType(death.dead), Int.MAX_VALUE)
            for ((part, npc) in parts) {
                if (npc == null || !npc.isSlotAssigned) continue
                npc.transmog(npcType(part.dead), Int.MAX_VALUE)
            }
        }
    }

    private fun shakeCameras(shake: ToaDeath.Shake) {
        schedule(shake.delay) {
            for (player in players) {
                Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, shake.leftRight, 0, 0)
                Camera.camShake(player, CamShakeAxis.UP_DOWN, shake.upDown, 0, 0)
                Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, shake.forwards, 0, 0)
            }
        }
        schedule(shake.delay + 1) { for (player in players) Camera.camReset(player) }
    }

    private fun reward() {
        spawnOsmumten()
        dropLoot()
    }

    private fun spawnOsmumten() {
        val tile = room.osmumtenTile ?: return
        val challengeSpawn = room.challengeSpawn ?: return
        val npc = Npc(OSMUMTEN, coords(tile))
        npc.respawnDir = if (challengeSpawn.x > tile.x) Direction.East else Direction.West
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        npc.anim(OSMUMTEN_SPAWN_ANIM)
        for (player in players) {
            player.midiJingle(OSMUMTEN_JINGLE)
            player.midiSong(STOP_MUSIC)
        }
    }

    private fun dropLoot() {
        val loot = loot ?: return
        val tile = coords(room.dropTile ?: return)
        val npcId = loot.trackerNpc.asRSCM(RSCMType.NPC)
        val heroObj = loot.heroObj
        val hero = lootSource?.findHero(deps.playerList)
        if (heroObj != null && hero != null && hero in players) {
            dropFor(hero, heroObj, tile, npcId)
        }
        val book = loot.book ?: return
        for (player in players) {
            if (!owns(player, book)) dropFor(player, book.obj, tile, npcId)
        }
    }

    private fun dropFor(player: Player, obj: String, tile: CoordGrid, npcId: Int) {
        val drop = deps.objRepo.add(obj, tile, DROP_DESPAWN, player, reveal = DROP_DESPAWN + 1)
        ClientScripts.lootTrackerAddLoot(player, npcId, deps.mapClock.cycle, drop.type, drop.count)
    }

    private fun owns(player: Player, book: ToaBook): Boolean {
        val varbit = ServerCacheManager.getVarbit(book.ownedVarbit.asRSCM(RSCMType.VARBIT))
        if (varbit != null && player.vars[varbit] == 1) return true
        return book.obj in player.inv || player.invMap[BANK]?.contains(book.obj) == true
    }

    companion object {
        const val OSMUMTEN = "npc.toa_osmumten_vis"

        private const val OSMUMTEN_SPAWN_ANIM = "seq.ghost_summon2_priority"
        private const val OSMUMTEN_JINGLE = 296
        private const val STOP_MUSIC = "midi.stop_music"
        private const val DROP_DESPAWN = 18_000
        private const val BANK = "inv.bank"
    }
}
