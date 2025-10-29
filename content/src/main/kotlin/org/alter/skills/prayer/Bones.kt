package org.alter.skills.prayer

import org.alter.game.util.enum
import org.alter.game.util.DbHelper.Companion.row
import org.alter.game.util.column
import org.alter.game.util.vars.BooleanType
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType
import org.alter.game.util.vars.RowType

object Bones {

    data class BoneData(
        val id: Int,
        val xp: Int,
        val isAshes: Boolean
    )

    val boneEnum = enum("enums.bone_data", ObjType, RowType)

    val bones: List<BoneData> = boneEnum.map { (boneId, rowId) ->
        val row = row(rowId)
        val xp = row.column("columns.skill_prayer:exp", IntType)
        val isAshes = row.column("columns.skill_prayer:ashes", BooleanType)
        BoneData(boneId, xp, isAshes)
    }
    
}