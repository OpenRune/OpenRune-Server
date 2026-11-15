package org.rsmod.content.bosses.araxxor

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.core.with
import dtx.rs.RSDropTable
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

private const val LOOT_ANIM = "seq.npc_araxxor_01_death_loot_01"
private const val HARVEST_ANIM = "seq.picking_low"
private const val PET_OBJ = "obj.araxxorpet"
private const val MORPH_OBJ = "obj.araxxor_pet_morph"
private const val CLUE_OBJ_MARKER = "trail_"
private const val DESTROY_MESSAGE =
    "You root aggressively through the corpse and the body decays before you can harvest any resources."

private const val CORPSE_SPAWN_DELAY = 1
private const val LOOT_DELAY = 2
private const val CORPSE_SINK_DELAY = 1
private const val CORPSE_REMOVE_DELAY = 3
private const val RESPAWN_DELAY = 15
private const val CORPSE_CENTRE_OFFSET = 2
private const val MORPH_KILL_TICKS = 125
private const val DESTROY_PET_CHANCE = 1500

private val logger = InlineLogger()

private var Player.skipDestroyPrompt by boolVarBit("varbit.araxxor_skip_destroy_prompt")

private enum class DestroyChoice {
    Confirm,
    ConfirmAlways,
    Cancel,
}

@Singleton
class AraxxorCorpse
@Inject
constructor(
    private val deps: BossDeps,
    private val manager: InstanceManager,
    private val dropTables: DropTableRegistry,
    private val areaChecker: AreaChecker,
    private val objRepo: ObjRepository,
    private val lair: AraxxorLair,
) {
    private class Pending(
        val table: RSDropTable<Player, DropRollItem>?,
        val killTicks: Int,
        val session: InstanceSession,
        val boss: Npc,
    ) {
        var claimed = false
    }

    private val pending = IdentityHashMap<Npc, Pending>()

    fun onBossKilled(hero: Player, boss: Npc) {
        val session = manager.sessionForPlayer(hero) ?: return
        val killTicks = deps.mapClock.cycle - boss.vars[SPAWN_TICK_VARN]
        val table = dropTables.forNpc(boss, areaChecker)
        val coords = boss.coords.translate(CORPSE_CENTRE_OFFSET, CORPSE_CENTRE_OFFSET)
        deps.worldQueues.add(CORPSE_SPAWN_DELAY) {
            spawnCorpse(hero, session, coords, table, killTicks, boss)
        }
    }

    fun onCorpseDeleted(corpse: Npc) {
        pending.remove(corpse)
    }

    suspend fun harvest(access: ProtectedAccess, corpse: Npc) {
        val state = claim(corpse)
        if (state == null) {
            access.mes("There is nothing left to harvest.")
            return
        }
        logger.info { "Araxxor harvest started by ${access.player}." }
        access.pickThrough()
        pending.remove(corpse)
        var given = 0
        state.table?.let { table ->
            for (drop in rollTable(access.player, table, state.boss)) {
                if (drop.obj == MORPH_OBJ && !morphEligible(access.player, state.killTicks)) continue
                given += grant(access, drop, state.boss)
            }
        }
        if (given == 0) access.mes("You harvest the corpse, but find nothing of value.")
        logger.info { "Araxxor harvest finished for ${access.player}, $given items granted." }
        finish(access.player, corpse, state.session)
    }

    suspend fun destroy(access: ProtectedAccess, corpse: Npc) {
        if (pending[corpse]?.claimed != false) {
            access.mes("There is nothing left to destroy.")
            return
        }
        if (!access.player.skipDestroyPrompt) {
            val pick =
                access.choice3(
                    "Yes, I'm sure.",
                    DestroyChoice.Confirm,
                    "Yes, and don't ask again.",
                    DestroyChoice.ConfirmAlways,
                    "Nevermind.",
                    DestroyChoice.Cancel,
                    title = "Destroying forfeits all loot except clue scrolls!",
                )
            if (pick == DestroyChoice.Cancel) return
            if (pick == DestroyChoice.ConfirmAlways) access.player.skipDestroyPrompt = true
        }
        val state = claim(corpse) ?: return
        access.pickThrough()
        pending.remove(corpse)
        state.table?.let { table ->
            for (drop in rollTable(access.player, table, state.boss)) {
                val obj = drop.transformObj(access.player) ?: drop.obj
                if (obj.contains(CLUE_OBJ_MARKER)) grant(access, drop, state.boss)
            }
        }
        if (deps.random.of(DESTROY_PET_CHANCE) == 0) give(access, PET_OBJ, 1)
        access.mes(DESTROY_MESSAGE)
        finish(access.player, corpse, state.session)
    }

    /** A sequence sent on the tick the player takes a step is overridden by the walk animation. */
    private suspend fun ProtectedAccess.pickThrough() {
        if (player.hasMovedThisCycle) delay(1)
        anim(HARVEST_ANIM)
        delay(LOOT_DELAY)
    }

    private fun spawnCorpse(
        killer: Player,
        session: InstanceSession,
        coords: CoordGrid,
        table: RSDropTable<Player, DropRollItem>?,
        killTicks: Int,
        boss: Npc,
    ) {
        if (manager.sessionsForKey(INSTANCE_KEY).none { it === session }) return
        val type = ServerCacheManager.getNpc(CORPSE_NPC.asRSCM(RSCMType.NPC)) ?: return
        val corpse = Npc(type, coords)
        corpse.mode = NpcMode.None
        deps.npcRepo.add(corpse, Int.MAX_VALUE)
        if (!manager.registerSessionNpc(killer, corpse)) manager.attachNpc(session.id, corpse)
        pending[corpse] = Pending(table, killTicks, session, boss)
    }

    private fun rollTable(
        player: Player,
        table: RSDropTable<Player, DropRollItem>,
        boss: Npc,
    ): List<DropRollItem> {
        val args =
            ArgMap(KillRollContext.npc with boss, KillRollContext.areaChecker with areaChecker)
        val rolled =
            runCatching { table.roll(player, args).flatten() }
                .onFailure { logger.error(it) { "Araxxor harvest roll failed for $player." } }
                .getOrNull() ?: return emptyList()
        return when (rolled) {
            is RollResult.Nothing -> emptyList()
            is RollResult.Single -> listOf(rolled.result)
            is RollResult.ListOf -> rolled.results
        }
    }

    private fun grant(access: ProtectedAccess, drop: DropRollItem, boss: Npc): Int {
        if (drop.isNothing || !drop.condition(access.player)) return 0
        drop.killCondition?.let { if (!it(access.player, boss, areaChecker)) return 0 }
        val obj = drop.transformObj(access.player) ?: drop.obj
        var given = if (give(access, obj, drop.rollCount(deps.random))) 1 else 0
        for (bonus in drop.bonusDrops) given += grant(access, bonus, boss)
        return given
    }

    private fun give(access: ProtectedAccess, obj: String, count: Int): Boolean {
        if (count <= 0) return false
        val objId = runCatching { obj.asRSCM(RSCMType.OBJ) }.getOrNull()
        if (objId == null) {
            logger.warn { "Araxxor harvest skipped unresolvable obj '$obj'." }
            return false
        }
        runCatching { CollectionLog.grant(access.player, objId, count) }
            .onFailure { logger.error(it) { "Araxxor harvest collection-log failed for '$obj'." } }
        return runCatching { access.invAddOrDrop(objRepo, obj, count) }
            .onFailure { logger.error(it) { "Araxxor harvest failed to give '$obj' x$count." } }
            .isSuccess
    }

    private fun morphEligible(player: Player, killTicks: Int): Boolean =
        killTicks <= MORPH_KILL_TICKS && !ownsMorph(player)

    private fun ownsMorph(player: Player): Boolean {
        val morphId = MORPH_OBJ.asRSCM(RSCMType.OBJ)
        if (player.inv.any { it != null && it.id == morphId }) return true
        val bank = player.invMap.getOrPut("inv.bank")
        return bank.any { it != null && it.id == morphId }
    }

    private fun claim(corpse: Npc): Pending? {
        val state = pending[corpse] ?: return null
        if (state.claimed) return null
        state.claimed = true
        return state
    }

    private fun finish(player: Player, corpse: Npc, session: InstanceSession) {
        deps.worldQueues.add(CORPSE_SINK_DELAY) { corpse.anim(LOOT_ANIM) }
        deps.worldQueues.add(CORPSE_REMOVE_DELAY) { deps.npcRepo.del(corpse, Int.MAX_VALUE) }
        deps.worldQueues.add(RESPAWN_DELAY) { lair.spawnBoss(player, session) }
    }
}
