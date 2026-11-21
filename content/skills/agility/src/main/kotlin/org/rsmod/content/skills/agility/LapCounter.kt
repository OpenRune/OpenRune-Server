package org.rsmod.content.skills.agility

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.game.entity.Player

/** The lap count message every course sends in its own colour, which Grace turns off and on. */
object LapCounter {
    fun announce(access: ProtectedAccess, course: AgilityCoursesRow, laps: Int) {
        if (access.player.lapCounterOff) {
            return
        }
        val colour = course.lapColour.toString(16).padStart(6, '0')
        access.mes("Your ${course.name} lap count is: <col=$colour>$laps</col>.")
    }

    var Player.lapCounterOff by boolVarBit("varbit.agility_lap_counter_off")
}
