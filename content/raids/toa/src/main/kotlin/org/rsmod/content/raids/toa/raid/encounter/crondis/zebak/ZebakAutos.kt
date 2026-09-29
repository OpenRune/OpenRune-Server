package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class ZebakAutos(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private var usingMage = false
    private var bleedRolls: List<Player> = emptyList()
    private val bleeding = HashMap<Player, Int>()
    private val lastCoords = HashMap<Player, CoordGrid>()

    enum class Style {
        MELEE,
        MAGIC,
        RANGED,
    }

    var style = Style.RANGED
        private set

    var meleeTargets: List<Player> = emptyList()
        private set

    fun rollStyle(boss: Npc) {
        if (deps.random.of(0, 2) == 0) usingMage = !usingMage
        meleeTargets = room.targets().filter { boss.isBeside(it) }
        style =
            when {
                meleeTargets.isNotEmpty() && deps.random.of(0, 2) == 0 -> Style.MELEE
                usingMage -> Style.MAGIC
                else -> Style.RANGED
            }
    }

    fun queueBleedRolls() {
        bleedRolls = meleeTargets
    }

    fun rollBleeds(targets: List<Player>) {
        if (bleedRolls.isEmpty()) return
        val rolls = bleedRolls
        bleedRolls = emptyList()
        for (player in rolls) {
            if (player !in targets || deps.random.of(0, 3) != 0) continue
            player.mes(BLEED_MESSAGE)
            bleeding[player] = deps.mapClock.cycle + BLEED_TICKS
        }
    }

    fun tickBleeding(targets: List<Player>) {
        val now = deps.mapClock.cycle
        bleeding.entries.removeIf { it.value <= now }
        for (player in targets) {
            val last = lastCoords.put(player, player.coords)
            if (last == null || last == player.coords || player !in bleeding) continue
            val base = room.maxHit(BLEED_BASE_DAMAGE)
            player.hitTypeless(deps.random.of(base, base + BLEED_DAMAGE_SPREAD))
            splat(player.coords)
        }
    }

    private fun splat(tile: CoordGrid) {
        if (deps.locRepo.findExact(tile, LocShape.GroundDecor) != null) return
        val type = ZebakLocs.BLOOD_SPLATS[deps.random.of(0, ZebakLocs.BLOOD_SPLATS.lastIndex)]
        deps.locRepo.add(tile, type, SPLAT_TICKS, LocAngle.West, LocShape.GroundDecor)
    }

    fun forget(player: Player) {
        bleeding.remove(player)
        lastCoords.remove(player)
    }

    fun clear() {
        bleeding.clear()
        lastCoords.clear()
        bleedRolls = emptyList()
        meleeTargets = emptyList()
        style = Style.RANGED
        usingMage = deps.random.of(0, 1) == 0
    }

    private companion object {
        const val BLEED_TICKS = 10
        const val BLEED_BASE_DAMAGE = 5
        const val BLEED_DAMAGE_SPREAD = 7
        const val SPLAT_TICKS = 10
        const val BLEED_MESSAGE =
            "<col=ff3045>Zebak's fangs tear into your flesh, causing you to bleed.</col>"
    }
}
