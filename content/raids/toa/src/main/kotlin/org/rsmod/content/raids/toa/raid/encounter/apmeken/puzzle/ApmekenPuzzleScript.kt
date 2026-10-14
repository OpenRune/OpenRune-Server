package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ApmekenPuzzleScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ApmekenLocs.HAMMERS) { takeHammer() }
        onOpLoc1(ApmekenLocs.POTIONS) { takePotions() }
        onOpLoc1(ApmekenLocs.PILLAR) { repair(it.loc) }
        onOpHeld1(ApmekenObjs.POTION) { pour() }
        onEvent<PlayerHitEvents.Modify> { scaleBaboonHit() }
        onEvent<PlayerHitEvents.Impact> {
            ToaRooms.of<ApmekenPuzzleEncounter>(player)?.baboons?.onImpact(player, hit)
        }
    }

    private fun PlayerHitEvents.Modify.scaleBaboonHit() {
        val slot = hit.sourceSlot ?: return
        if (!hit.isFromNpc) return
        val room = ToaRooms.roomOf(player) as? ApmekenPuzzleEncounter ?: return
        val factor = room.baboons.damageFactor(slot) ?: return
        hit.damage = (hit.damage * factor).toInt()
    }

    private val ProtectedAccess.room: ApmekenPuzzleEncounter?
        get() = ToaRooms.of<ApmekenPuzzleEncounter>(player)

    private suspend fun ProtectedAccess.takeHammer() {
        arriveDelay()
        val room = room ?: return
        if (inv.freeSpace() == 0) return mes(ApmekenMessages.NO_SPACE)
        anim(ApmekenSeqs.PICKUP)
        soundSynth(ApmekenSynths.CRATE)
        delay(1)
        if (invAdd(inv, ApmekenObjs.HAMMER, 1).failure) return mes(ApmekenMessages.NO_SPACE)
        room.grantHammer(player)
        mes(ApmekenMessages.TAKE_HAMMER)
    }

    private suspend fun ProtectedAccess.takePotions() {
        arriveDelay()
        room ?: return
        anim(ApmekenSeqs.PICKUP)
        soundSynth(ApmekenSynths.CRATE)
        delay(1)
        if (invAdd(inv, ApmekenObjs.POTION, POTION_STACK).failure) {
            return mes(ApmekenMessages.NO_SPACE)
        }
        mes(ApmekenMessages.TAKE_POTIONS)
    }

    private suspend fun ProtectedAccess.repair(loc: BoundLocInfo) {
        arriveDelay()
        val room = room ?: return
        if (room.stage != ToaStage.STARTED) return
        val hasHammer =
            inv.contains(ApmekenObjs.HAMMER) || inv.contains(ApmekenObjs.DRAGON_WARHAMMER)
        if (!hasHammer) return mes(ApmekenMessages.NEED_HAMMER)
        val index = room.supportIndexAt(loc.coords)
        if (index < 0) return
        anim(ApmekenSeqs.REPAIR)
        room.sight.repair(player, index)
        delay(1)
    }

    private suspend fun ProtectedAccess.pour() {
        val room = room ?: return
        if (room.stage != ToaStage.STARTED || !room.inChallengeArea(player)) return
        if (invDel(inv, ApmekenObjs.POTION, 1, strict = false).failure) return
        anim(ApmekenSeqs.POUR)
        soundSynth(ApmekenSynths.POUR)
        soundSynth(ApmekenSynths.POUR_TAIL, delay = POUR_TAIL_DELAY)
        val vent = room.ventIndexAt(player)
        if (vent < 0) return mes(ApmekenMessages.NOTHING_HAPPENS)
        room.sight.neutralise(player, vent)
        delay(1)
    }

    private companion object {
        const val POTION_STACK = 100
        const val POUR_TAIL_DELAY = 30
    }
}
