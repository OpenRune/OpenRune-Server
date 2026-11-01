package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.ImplingTypesRow
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class ImplingScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val loot: ImplingLootScript,
    private val spawns: ImplingSpawns,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (impling in ImplingTypesRow.all()) {
            onOpNpc1(impling.npc) { catch(it.npc, impling, puroPuro = false) }
            onOpNpc1(impling.npcPuroPuro) { catch(it.npc, impling, puroPuro = true) }
        }
    }

    suspend fun ProtectedAccess.catch(
        npc: Npc,
        impling: ImplingTypesRow,
        puroPuro: Boolean,
    ) {
        val inMaze = puroPuro || inPuroPuro(npc.coords)
        val xp = if (inMaze) impling.xpPuroPuro else impling.xpGielinor
        catch(npc, impling.level, impling.jar.internalName, xp, jarRequired = inMaze) { removeCaught(npc, impling) }
    }

    suspend fun ProtectedAccess.catch(
        npc: Npc,
        level: Int,
        jar: String,
        xp: Int,
        jarRequired: Boolean = false,
        roll: ProtectedAccess.(net: String?) -> Boolean = { net ->
            random.of(100) < catchChance(stat(HUNTER), bonus = net != NET)
        },
        onCaught: () -> Unit,
    ) {
        val net = wieldedNet()
        val required = if (net == null) level + BAREHANDED_EXTRA_LEVELS else level
        if (stat(HUNTER) < required) {
            val barehanded = if (net == null) " barehanded" else ""
            mes("You need a Hunter level of at least $required to catch this impling$barehanded.")
            return
        }
        if (!canCatch(jarRequired)) return
        repeat(MAX_ATTEMPTS) {
            if (!npc.isCatchable()) return
            faceEntitySquare(npc)
            anim(CATCH_SEQ)
            delay(ATTEMPT_TICKS)
            if (!npc.isCatchable()) return
            if (!roll(net)) return@repeat
            if (!canCatch(jarRequired)) return
            val slot = inv.indexOfFirst { it?.id == jarId }
            val jarred = slot >= 0
            if (jarred) {
                mes("You manage to catch the impling and squeeze it into a jar.", ChatType.Spam)
                if (invReplaceSlot(inv, slot, 1, ServerCacheManager.getItem(jar.asRSCM(RSCMType.OBJ))!!).failure) return
            } else {
                mes("You manage to catch the impling and acquire some loot.", ChatType.Spam)
            }
            npc.anim(CAUGHT_SEQ)
            if (!jarred) with(loot) { give(rollJar(jar)) }
            soundSynth(CAUGHT_SYNTH)
            statAdvance(HUNTER, xp.toDouble())
            npc.hideAllOps()
            caught += npc
            spawns.after(CAUGHT_TICKS) {
                caught -= npc
                onCaught()
            }
            return
        }
    }

    private fun ProtectedAccess.canCatch(jarRequired: Boolean): Boolean {
        if (IMPLING_JAR in inv) return true
        if (inv.freeSpace() == 0) {
            mes("You'll need to clear some space in your pack to catch this impling without a jar.")
            return false
        }
        if (jarRequired) {
            mes("You don't have an empty impling jar in which to keep an impling.")
            return false
        }
        return true
    }

    private val caught = HashSet<Npc>()

    private fun Npc.isCatchable(): Boolean = isSlotAssigned && isVisible && this !in caught

    private fun ProtectedAccess.wieldedNet(): String? {
        val held = player.righthand ?: return null
        return listOf(NET, MAGIC_NET).firstOrNull { it.asRSCM(RSCMType.OBJ) == held.id }
    }

    private fun removeCaught(npc: Npc, impling: ImplingTypesRow) {
        npc.showAllOps()
        if (spawns.caught(npc)) return
        if (npc.respawns) {
            npcRepo.despawn(npc, impling.respawnTicks)
        } else {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private val jarId: Int by lazy { IMPLING_JAR.asRSCM(RSCMType.OBJ) }

    companion object {
        const val HUNTER = "stat.hunter"
        const val NET = "obj.hunting_butterfly_net"
        const val MAGIC_NET = "obj.ii_magic_butterfly_net"
        const val IMPLING_JAR = "obj.ii_impling_jar"
        const val CATCH_SEQ = "seq.hunting_impling_catch"
        const val CAUGHT_SEQ = "seq.ii_impling_caught"
        const val CAUGHT_SYNTH = "synth.impling_caught"
        const val BAREHANDED_EXTRA_LEVELS = 10
        const val ATTEMPT_TICKS = 2
        const val CAUGHT_TICKS = 2
        const val MAX_ATTEMPTS = 20

        fun inPuroPuro(coords: CoordGrid): Boolean = coords.x in 2560..2623 && coords.z in 4288..4351

        fun catchChance(level: Int, bonus: Boolean): Int {
            val base = 12 + (98 - 12) * (level.coerceIn(1, 99) - 1) / 98
            return base + if (bonus) 8 else 0
        }
    }
}
