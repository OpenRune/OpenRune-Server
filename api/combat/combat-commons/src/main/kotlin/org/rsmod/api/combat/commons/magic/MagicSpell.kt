package org.rsmod.api.combat.commons.magic

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType

public data class MagicSpell(
    public val obj: ItemServerType,
    public val name: String,
    public val component: ComponentType,
    public val spellbook: Spellbook?,
    public val type: MagicSpellType,
    public val maxHit: Int,
    public val levelReq: Int,
    public val castXp: Double,
    public val objReqs: List<ObjRequirement>,
) {
    public data class ObjRequirement(val obj: ItemServerType, val count: Int, val wornSlot: Int?)
}
