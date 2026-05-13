package org.rsmod.api.account.character.inv

import org.rsmod.api.account.character.CharacterDataStage

public class CharacterInventoryData(public val inventories: List<Inventory>) :
    CharacterDataStage.Segment {
    public data class Obj(val objKey: String, val count: Int, val vars: Int)

    public data class Inventory(
        val characterId: Int,
        val invDbKey: String,
        val invKey: String,
        val objs: MutableMap<Int, Obj> = mutableMapOf(),
    )
}
