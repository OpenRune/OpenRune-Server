package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class ZebakAutos(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private var usingMage = false

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

    fun splat(tile: CoordGrid) {
        if (deps.locRepo.findExact(tile, LocShape.GroundDecor) != null) return
        val type = ZebakLocs.BLOOD_SPLATS[deps.random.of(0, ZebakLocs.BLOOD_SPLATS.lastIndex)]
        deps.locRepo.add(tile, type, SPLAT_TICKS, LocAngle.West, LocShape.GroundDecor)
    }

    fun clear() {
        meleeTargets = emptyList()
        style = Style.RANGED
        usingMage = deps.random.of(0, 1) == 0
    }

    private companion object {
        const val SPLAT_TICKS = 10
    }
}
