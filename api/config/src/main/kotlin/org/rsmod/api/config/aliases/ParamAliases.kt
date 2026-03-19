package org.rsmod.api.config.aliases

import dev.openrune.TypedParamType
import dev.openrune.definition.type.HitSplatType
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.HealthBarServerType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.ProjAnimType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.CategoryType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.SynthType
import org.rsmod.map.CoordGrid

typealias ParamInt = TypedParamType<Int>

typealias ParamStr = TypedParamType<String>

typealias ParamBool = TypedParamType<Boolean>

typealias ParamCategory = TypedParamType<CategoryType>

typealias ParamComponent = TypedParamType<ComponentType>

typealias ParamCoord = TypedParamType<CoordGrid>

typealias ParamHeadbar = TypedParamType<HealthBarServerType>

typealias ParamHitmark = TypedParamType<HitSplatType>

typealias ParamObj = TypedParamType<ItemServerType>

typealias ParamNamedObj = TypedParamType<ItemServerType>

typealias ParamLoc = TypedParamType<ObjectServerType>

typealias ParamNpc = TypedParamType<NpcServerType>

typealias ParamProj = TypedParamType<ProjAnimType>

typealias ParamSeq = TypedParamType<SequenceServerType>

typealias ParamSpot = TypedParamType<SpotanimType>

typealias ParamStat = TypedParamType<StatType>

typealias ParamSynth = TypedParamType<SynthType>

typealias ParamVarBit = TypedParamType<VarBitType>
