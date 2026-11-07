package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.ImplingTypesRow
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ImplingScript @Inject constructor(private val npcRepo: NpcRepository) : PluginScript() {
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
        val xp = if (puroPuro) impling.xpPuroPuro else impling.xpGielinor
        catch(npc, impling.level, impling.jar.internalName, xp) { removeCaught(npc, impling) }
    }

    suspend fun ProtectedAccess.catch(
        npc: Npc,
        level: Int,
        jar: String,
        xp: Int,
        roll: ProtectedAccess.(net: String?) -> Boolean = { net ->
            random.of(100) < catchChance(stat(HUNTER), bonus = net != NET)
        },
        onCaught: () -> Unit,
    ) {
        val net = wieldedNet()
        val required = if (net == null) level + BAREHANDED_EXTRA_LEVELS else level
        if (stat(HUNTER) < required) {
            val barehanded = if (net == null) " barehanded" else ""
            mes("You need a Hunter level of $required to catch this impling$barehanded.")
            return
        }
        if (IMPLING_JAR !in inv) {
            mes("You need an empty impling jar to keep the impling in.")
            return
        }
        repeat(MAX_ATTEMPTS) {
            if (!npc.isCatchable()) return
            faceEntitySquare(npc)
            anim(CATCH_SEQ)
            delay(ATTEMPT_TICKS)
            if (!npc.isCatchable()) return
            if (!roll(net)) return@repeat
            val slot = (0 until inv.size).firstOrNull { inv[it]?.id == jarId } ?: return
            if (invDel(inv, IMPLING_JAR, slot = slot).failure) return
            invAdd(inv, jar, slot = slot)
            statAdvance(HUNTER, xp.toDouble())
            onCaught()
            mes("You manage to catch the impling and squeeze it into a jar.", ChatType.Spam)
            return
        }
    }

    private fun Npc.isCatchable(): Boolean = isSlotAssigned && isVisible

    private fun ProtectedAccess.wieldedNet(): String? {
        val held = player.righthand ?: return null
        return listOf(NET, MAGIC_NET).firstOrNull { it.asRSCM(RSCMType.OBJ) == held.id }
    }

    private fun removeCaught(npc: Npc, impling: ImplingTypesRow) {
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
        const val BAREHANDED_EXTRA_LEVELS = 10
        const val ATTEMPT_TICKS = 2
        const val MAX_ATTEMPTS = 20

        fun catchChance(level: Int, bonus: Boolean): Int {
            val base = 12 + (98 - 12) * (level.coerceIn(1, 99) - 1) / 98
            return base + if (bonus) 8 else 0
        }
    }
}
