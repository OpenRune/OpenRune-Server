package org.rsmod.content.skills.agility

import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.api.table.agility.AgilityTransformsRow
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey

internal object AgilityForms {
    private val rows by lazy { AgilityTransformsRow.all() }

    private val byObj by lazy { rows.filter { it.obj != null }.associateBy { it.obj!!.id } }

    private val wornForms by lazy { byObj.values.map { it.npc.id }.toSet() }

    val penguinSuit: AgilityTransformsRow by lazy {
        AgilityTransformsRow.getRow("dbrow.agility_transform_penguin_suit")
    }

    fun requiredFor(course: AgilityCoursesRow): AgilityTransformsRow? =
        rows.firstOrNull { it.course?.rowId == course.rowId }

    fun wornBy(obj: Int): AgilityTransformsRow? = byObj[obj]

    fun isWornForm(npc: Int): Boolean = npc in wornForms

    fun AgilityTransformsRow.holdsAt(coords: CoordGrid): Boolean =
        MapSquareKey.from(coords).id in mapsquares
}
