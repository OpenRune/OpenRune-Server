package org.alter.game.util.vars

import dev.openrune.definition.util.VarType

data object IntType : NumericIntegerVarType(VarType.INT)
data object BooleanType : BooleanVarType(VarType.BOOLEAN)
data object StringType : GenericStringVarType(VarType.STRING)
data object LongType : NumericLongVarType(VarType.LONG)
data object ComponentType : NumericIntegerVarType(VarType.COMPONENT)
data object ObjType : NumericIntegerVarType(VarType.OBJ)
data object NpcType : NumericIntegerVarType(VarType.NPC)
data object LocType : NumericIntegerVarType(VarType.LOC)
data object EnumType : NumericIntegerVarType(VarType.ENUM)
data object RowType : NumericIntegerVarType(VarType.DBROW)
data object TableType : NumericIntegerVarType(VarType.DBTABLE)
data object SeqType : NumericIntegerVarType(VarType.SEQ)
data object GraphicType : NumericIntegerVarType(VarType.GRAPHIC)
data object HeadBarType : NumericIntegerVarType(VarType.HEADBAR)
data object HitMarkType : NumericIntegerVarType(VarType.HITMARK)
data object MesType : NumericIntegerVarType(VarType.MESANIM)
data object CoordType : NumericIntegerVarType(VarType.COORDGRID)
data object SkillType : NumericIntegerVarType(VarType.STAT)