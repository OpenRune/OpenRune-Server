package org.rsmod.content.generic.locs

import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.table.PickableObjectsRow
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Pickables @Inject constructor(
    private val objRepo: ObjRepository,
    private val locRepo: LocRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        PickableObjectsRow.all().forEach { row ->
            row.objects.forEach { type ->
                onOpLoc1(type) { pick(it.loc, it.type, row) }
                onOpLoc2(type) { pick(it.loc, it.type, row) }
            }
        }

        PickableObjectsRow.all().distinctBy { it.replacementloc }.forEach { row ->
            row.replacementloc?.let { replacement ->
                onOpLoc1(replacement) { emptyPickable(it.loc, row) }
                onOpLoc2(replacement) { emptyPickable(it.loc, row) }
            }
        }

        onOpLoc2("loc.fai_varrock_cadavabush_tailored") {
            mes("There are no berries on this bush. Maybe you should try another bush.")
        }

    }

    private suspend fun ProtectedAccess.emptyPickable(loc: BoundLocInfo, row: PickableObjectsRow) {
        arriveDelay()
        mes(emptyPickableMessage(row))
    }

    private suspend fun ProtectedAccess.pick(
        loc: BoundLocInfo,
        type: ObjectServerType,
        row: PickableObjectsRow,
    ) {
        arriveDelay()

        if (handleNettles(row)) {
            return
        }

        val rolledSeed = rollSeed(row)
        val giveSeedOnly = rolledSeed && inv.freeSpace() <= 1

        if (!canPickItem(giveSeedOnly)) {
            mes(invFullMessage(row.itemgiven))
            return
        }

        anim("seq.human_pickupfloor")

        if (row.forceswalk) {
            playerWalkWithMinDelay(loc.coords)
        }

        givePickables(row, rolledSeed, giveSeedOnly)

        soundSynth("synth.pick")

        advanceLoc(loc, type, row)
    }

    private fun ProtectedAccess.handleNettles(row: PickableObjectsRow): Boolean {
        if (!row.isNettles || player.hasPickableGloves()) {
            return false
        }

        anim("seq.human_pickupfloor")
        mes("The nettles sting you.")
        PlayerPoison.incidentalPoisonHit(player, random.of(1, 2))

        return true
    }

    private fun ProtectedAccess.givePickables(
        row: PickableObjectsRow,
        rolledSeed: Boolean,
        giveSeedOnly: Boolean,
    ) {
        if (!giveSeedOnly) {
            mes(pickMessage(row.itemgiven))
            invAddOrDropType(objRepo, row.itemgiven, row.itemamount)
        }

        if (rolledSeed) {
            val seed = requireNotNull(row.seed)
            mes("You pick a ${seed.name.lowercase()}.")
            invAddOrDropType(objRepo, seed)
        }
    }

    private fun ProtectedAccess.canPickItem(giveSeedOnly: Boolean): Boolean =
        inv.freeSpace() > 0 || giveSeedOnly

    private fun ProtectedAccess.rollSeed(row: PickableObjectsRow): Boolean =
        row.seed != null && rollChance(100, 3433)

    private fun ProtectedAccess.advanceLoc(
        loc: BoundLocInfo,
        type: ObjectServerType,
        row: PickableObjectsRow,
    ) {
        val index = row.objects.indexOfFirst { it.id == type.id }
        if (index == -1 || row.respawntime < 0) {
            return
        }

        val respawnDuration = pickableRespawnDuration(row.respawntime)

        if (row.objectcycle) {
            advanceCycle(loc, row, index, respawnDuration)
            return
        }

        if (shouldDespawn(row)) {
            despawnOrReplace(loc, type, row, respawnDuration)
        }
    }

    private fun ProtectedAccess.advanceCycle(
        loc: BoundLocInfo,
        row: PickableObjectsRow,
        index: Int,
        respawnDuration: Int,
    ) {
        val next = row.objects.getOrNull(index + 1)

        if (next != null) {
            locRepo.change(loc, next, Int.MAX_VALUE)
            return
        }

        despawnOrReplace(loc, row.objects.first(), row, respawnDuration)
    }

    private fun ProtectedAccess.despawnOrReplace(
        loc: BoundLocInfo,
        respawnType: ObjectServerType,
        row: PickableObjectsRow,
        respawnDuration: Int,
    ) {
        val replacement = row.replacementloc

        if (replacement == null) {
            locRepo.del(loc, respawnDuration)
            return
        }

        replaceUntilRespawn(loc, replacement, respawnDuration) {
            locRepo.add(
                loc.coords,
                respawnType.internalName,
                Int.MAX_VALUE,
                loc.angle,
                loc.shape,
            )
        }
    }

    private fun ProtectedAccess.shouldDespawn(row: PickableObjectsRow): Boolean {
        val chances = row.despawnchance

        if (chances.isEmpty()) {
            return true
        }

        val numerator = chances.first()
        val denominator = chances.getOrElse(1) { numerator }

        return rollChance(numerator, denominator)
    }

    private fun ProtectedAccess.rollChance(
        numerator: Int,
        denominator: Int,
    ): Boolean = random.of(1, denominator) <= numerator

    private fun ProtectedAccess.replaceUntilRespawn(
        loc: BoundLocInfo,
        replacement: ObjectServerType,
        respawnDuration: Int,
        onRespawn: () -> Unit,
    ) {
        locRepo.del(loc, Int.MAX_VALUE)

        locRepo.add(
            loc.coords,
            replacement.internalName,
            respawnDuration,
            loc.angle,
            loc.shape,
            onDespawn = onRespawn.takeUnless { respawnDuration == Int.MAX_VALUE },
        )
    }

    private fun pickableRespawnDuration(ticks: Int): Int =
        if (ticks == 0) Int.MAX_VALUE else ticks

    private fun Player.hasPickableGloves(): Boolean =
        worn[Wearpos.Hands.slot] != null

    private val PickableObjectsRow.isNettles: Boolean
        get() = itemgiven.internalName == "obj.nettles_picked"

    private fun pickMessage(item: ItemServerType): String =
        "You pick some ${item.name.lowercase()}."

    private fun invFullMessage(item: ItemServerType): String =
        "You can't carry any more ${item.name.lowercase()}."

    private fun emptyPickableMessage(row: PickableObjectsRow): String =
        when (row.itemgiven.internalName) {
            "obj.cadavaberries",
            "obj.redberries",
            -> "There are no berries on this bush at the moment."
            "obj.pineapple" -> "There are no pineapples left on this plant."
            else -> "There is nothing to pick here at the moment."
        }
}
