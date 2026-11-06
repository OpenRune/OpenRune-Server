package org.rsmod.content.areas.city.falador.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.obj.Obj

data class MoleParts(val claws: Int, val skins: Int, val immaculate: Int) {
    val rolls: Int
        get() = claws + skins + immaculate * 5

    val isEmpty: Boolean
        get() = rolls == 0
}

enum class WysonNest(val nest: String, val box: String) {
    SEEDS("obj.bird_nest_decentseeds_jan2019", "obj.nest_box_decentseeds_jan2019"),
    RING("obj.bird_nest_ring", "obj.nest_box_ring"),
    EMPTY("obj.bird_nest_empty", "obj.nest_box_empty"),
}

val WysonNest.nestId: Int
    get() = nest.asRSCM(RSCMType.OBJ)

val WysonNest.boxId: Int
    get() = box.asRSCM(RSCMType.OBJ)

fun InvObj.wysonNestCount(): Int = if (vars > 0) vars else count.coerceAtLeast(1)

@OptIn(UncheckedType::class)
fun WysonNest.createBox(count: Int): InvObj = InvObj(boxId, 1, count)

object WysonMoleParts {
    private val PART_ITEMS =
        listOf(
            "obj.mole_claw",
            "obj.cert_mole_claw",
            "obj.mole_skin",
            "obj.cert_mole_skin",
            "obj.immaculate_mole_skin",
        )

    private val PART_IDS: Set<Int> by lazy {
        PART_ITEMS.map { it.asRSCM(RSCMType.OBJ) }.toSet()
    }

    fun parts(player: Player): MoleParts =
        MoleParts(
            claws =
                player.inv.count("obj.mole_claw") + player.inv.count("obj.cert_mole_claw"),
            skins =
                player.inv.count("obj.mole_skin") + player.inv.count("obj.cert_mole_skin"),
            immaculate = player.inv.count("obj.immaculate_mole_skin"),
        )

    fun hasParts(player: Player): Boolean = !parts(player).isEmpty

    fun nestForRoll(roll: Int): WysonNest =
        when {
            roll < 72 -> WysonNest.SEEDS
            roll < 92 -> WysonNest.RING
            else -> WysonNest.EMPTY
        }

    fun rollNests(random: GameRandom, count: Int): Map<WysonNest, Int> {
        val rewards = mutableMapOf<WysonNest, Int>()
        repeat(count) {
            val nest = nestForRoll(random.of(100))
            rewards[nest] = (rewards[nest] ?: 0) + 1
        }
        return rewards
    }

    @OptIn(UncheckedType::class)
    fun exchange(player: Player, random: GameRandom, objRepo: ObjRepository) {
        val parts = parts(player)
        if (parts.isEmpty) {
            return
        }
        val rewards = rollNests(random, parts.rolls)
        val partSlots =
            player.inv.indices.filter { index ->
                val obj = player.inv[index] ?: return@filter false
                obj.id in PART_IDS
            }
        val freeAfter = player.inv.freeSpace() + partSlots.size
        val boxed = parts.rolls > freeAfter
        for (slot in partSlots) {
            player.inv[slot] = null
        }
        for ((nest, count) in rewards) {
            if (boxed) {
                addOrDrop(player, objRepo, nest, nest.createBox(count), count)
            } else {
                repeat(count) { addOrDrop(player, objRepo, nest, InvObj(nest.nestId, 1), 1) }
            }
        }
    }

    private fun addOrDrop(
        player: Player,
        objRepo: ObjRepository,
        nest: WysonNest,
        obj: InvObj,
        count: Int,
    ) {
        val slot = player.inv.indices.firstOrNull { player.inv[it] == null }
        if (slot != null) {
            player.inv[slot] = obj
            return
        }
        val nestType = ServerCacheManager.getItem(nest.nestId) ?: return
        repeat(count) {
            val dropped = Obj.fromOwner(player, player.coords, nestType, 1)
            objRepo.add(dropped, constants.lootdrop_duration)
        }
    }

    fun extract(player: Player, slot: Int, nest: WysonNest): Boolean {
        val box = player.inv[slot] ?: return false
        if (box.id != nest.boxId) {
            return false
        }
        val remaining = box.wysonNestCount() - 1
        if (remaining > 0 && player.inv.freeSpace() <= 0) {
            return false
        }
        player.inv[slot] = if (remaining > 0) nest.createBox(remaining) else null
        val result = player.invAdd(player.inv, nest.nest, 1)
        if (!result.success) {
            player.inv[slot] = box
            return false
        }
        return true
    }
}
