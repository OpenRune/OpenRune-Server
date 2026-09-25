package org.rsmod.content.other.pets

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.enums.enum

class PetCategory(val id: Int, val name: String) {
    companion object {
        val all: List<PetCategory> =
            enum<Int, String>("pet_categories").backing.map { (id, name) -> PetCategory(id, name ?: "") }

        fun of(id: Int): PetCategory = all.first { it.id == id }
    }
}

class PetForm(
    val objId: Int,
    val npcId: Int,
    val runs: Boolean,
    val unlockId: Int? = null,
    val itemUse: Boolean = false,
    emoteTypes: List<SequenceServerType> = emptyList(),
) {
    val obj: String = RSCM.getReverseMapping(RSCMType.OBJ, objId)
    val npc: String = RSCM.getReverseMapping(RSCMType.NPC, npcId)
    val unlock: String? = unlockId?.let { RSCM.getReverseMapping(RSCMType.VARBIT, it) }
    val emotes: List<String> = emoteTypes.map { it.internalName }
}

class Pet(
    val key: String,
    val name: String,
    val category: PetCategory,
    val forms: List<PetForm>,
    val mainDrop: Boolean,
) {
    val base: PetForm
        get() = forms.first()
}
