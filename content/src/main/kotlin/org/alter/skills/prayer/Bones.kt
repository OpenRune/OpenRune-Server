package org.alter.skills.prayer

import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.vars.BooleanType
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType

object Bones {

    data class BoneData(
        val id: Int,
        val xp: Int,
        val isAshes: Boolean
    )

    val bones: List<BoneData> = table("tables.skill_prayer").map { row ->
        val boneId = row.column("columns.skill_prayer:item", ObjType)
        val xp = row.column("columns.skill_prayer:exp", IntType)
        val isAshes = row.column("columns.skill_prayer:ashes", BooleanType)
        BoneData(boneId, xp, isAshes)
    }
    
}