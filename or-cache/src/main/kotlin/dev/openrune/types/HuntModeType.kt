package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.types.hunt.HuntCheckNotTooStrong
import dev.openrune.types.hunt.HuntCondition
import dev.openrune.types.hunt.HuntNobodyNear
import dev.openrune.types.hunt.HuntType
import dev.openrune.types.hunt.HuntVis

@RsTableHeaders("hunt")
data class HuntModeType(
    override var id: Int = -1,
    var type: HuntType = HuntType.Off,
    var checkVis: HuntVis = HuntVis.Off,
    var checkNotTooStrong: HuntCheckNotTooStrong = HuntCheckNotTooStrong.Off,
    var checkNotCombat: Int = -1,
    var checkNotCombatSelf: Int = -1,
    var checkAfk: Boolean = true,
    var checkNotBusy: Boolean = false,
    var findKeepHunting: Boolean = false,
    var findNewMode: NpcMode = NpcMode.None,
    var nobodyNear: HuntNobodyNear = HuntNobodyNear.KeepHunting,
    var rate: Int = 1,
    var checkInvObj: HuntCondition.InvCondition? = null,
    var checkInvParam: HuntCondition.InvCondition? = null,
    var checkLoc: HuntCondition.LocCondition? = null,
    var checkNpc: HuntCondition.NpcCondition? = null,
    var checkObj: HuntCondition.ObjCondition? = null,
    var checkVar1: HuntCondition.VarCondition? = null,
    var checkVar2: HuntCondition.VarCondition? = null,
    var checkVar3: HuntCondition.VarCondition? = null,
) : Definition
