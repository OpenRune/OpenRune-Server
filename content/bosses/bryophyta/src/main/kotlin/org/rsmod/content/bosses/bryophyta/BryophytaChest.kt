package org.rsmod.content.bosses.bryophyta

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.slayer.dialogue.SlayerMasters
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.uncert
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BryophytaChest
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {

    private var Player.chestsOpened by intVarp("varp.bryophyta_chests_opened")

    private val pendingRespawns = mutableSetOf<InstanceId>()
    private val openingChests = mutableSetOf<Long>()

    override fun ScriptContext.startup() {
        onOpLoc1(CHEST_CLOSED) { openChest(it.loc) }
    }

    private suspend fun ProtectedAccess.openChest(chest: BoundLocInfo) {
        val uuid = player.uuid ?: return
        if (!openingChests.add(uuid)) return
        try {
            openChestInternal(chest)
        } finally {
            openingChests.remove(uuid)
        }
    }

    private suspend fun ProtectedAccess.openChestInternal(chest: BoundLocInfo) {
        val session = instances.sessionForPlayer(player) ?: return
        if (session.key != INSTANCE_KEY) return
        if (session.id in pendingRespawns) return
        if (instances.npcsForInstance(session.id).any { it.isType(BOSS_NPC) && it.hitpoints > 0 }) {
            mes("You can't loot the chest whilst Bryophyta is still attacking you!")
            return
        }

        mes("You hear the leaves stir...")
        anim("seq.human_openchest")
        delay(1)

        val hasKey = invTotal(player.inv, MOSSY_KEY) > 0
        if (!hasKey) {
            mes("You need a Mossy key to open this chest.")
            scheduleBossRespawn(session)
            return
        }
        if (invDel(player.inv, MOSSY_KEY, 1).failure) {
            return
        }

        player.chestsOpened += 1
        mes("The loot spills out as you open the chest.")
        mes("Your Bryophyta chests opened count is: <col=ff0000>${player.chestsOpened}</col>.")
        awardLoot()
        locRepo.change(chest, CHEST_OPEN, CHEST_OPEN_TICKS)
        scheduleBossRespawn(session)
    }

    private fun ProtectedAccess.awardLoot() {
        val onTask = isMossGiantTask(player)
        val drops = BryophytaChestLoot.roll(player.members, onTask, deps.random)
        for (drop in drops) {
            val spawnObj = if (drop.obj == CLUE) player.clueScrollTransformObj(CLUE) ?: CLUE else drop.obj
            val logObj = if (drop.obj == CLUE) CLUE else drop.obj
            ServerCacheManager.getItem(logObj.asRSCM(RSCMType.OBJ))?.let { type ->
                CollectionLog.grant(player, uncert(type).id, drop.count)
            }
            val duration = if (drop.obj == CLUE) CLUE_DESPAWN_TICKS else LOOT_DESPAWN_TICKS
            objRepo.add(spawnObj, player.coords, duration, receiver = player, count = drop.count)
        }
    }

    private fun isMossGiantTask(player: Player): Boolean {
        if (player.vars["varbit.slayer_master"] == SlayerMasters.TASK_WILDERNESS) {
            return false
        }
        val taskKey = SlayerTaskCredit.taskKey(player)
        if (taskKey <= 0) return false
        val bossType = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC)) ?: return false
        return SlayerTaskCredit.counts(bossType, taskKey)
    }

    private fun scheduleBossRespawn(session: InstanceSession) {
        if (!pendingRespawns.add(session.id)) return
        deps.worldQueues.add(BOSS_RESPAWN_TICKS) {
            try {
                if (instances.sessionForId(session.id) == session) {
                    spawnBoss(session)
                }
            } finally {
                pendingRespawns.remove(session.id)
            }
        }
    }

    private fun spawnBoss(session: InstanceSession) {
        val coords = instances.resolveCoord(session, BOSS_SPAWN) ?: return
        val type = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC)) ?: return
        val boss = Npc(type, coords)
        boss.mode = type.defaultMode
        deps.npcRepo.add(boss, Int.MAX_VALUE)
        boss.respawns = false
        val occupant =
            session.occupants.firstOrNull()?.let { uuid ->
                deps.playerList.firstOrNull { it.uuid == uuid }
            }
        if (occupant == null) {
            instances.attachNpc(session.id, boss)
            return
        }
        instances.registerSessionNpc(occupant, boss)
        boss.movementLocked = true
        boss.apRangeOverride = MAGIC_RANGE
        boss.apRequiresLineOfSight = false
        boss.apPlayer2(occupant, aiPlayerInteractions)
    }

    private companion object {
        private const val INSTANCE_KEY = "bryophyta"
        private const val BOSS_NPC = "npc.gb_mossgiant"
        private const val CHEST_CLOSED = "loc.mossykey_chest_closed"
        private const val CHEST_OPEN = "loc.mossykey_chest_open"
        private const val MOSSY_KEY = "obj.mossy_key"
        private const val CLUE = "obj.trail_clue_beginner"
        private const val MAGIC_RANGE = 10

        private const val BOSS_RESPAWN_TICKS = 15
        private const val CHEST_OPEN_TICKS = 20
        private const val LOOT_DESPAWN_TICKS = 200
        private const val CLUE_DESPAWN_TICKS = 18_000

        private val BOSS_SPAWN = CoordGrid(3220, 9933)
    }
}
