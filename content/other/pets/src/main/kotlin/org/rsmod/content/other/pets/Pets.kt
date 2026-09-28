package org.rsmod.content.other.pets

import org.rsmod.api.table.PetsRow

object Pets {
    val all: List<Pet> =
        PetsRow.all().groupBy { it.pet }.values.map { rows ->
            val base = rows.first { it.base }
            val forms = listOf(base) + rows.filter { !it.base }
            Pet(
                key = base.pet,
                name = base.name,
                category = PetCategory.of(base.category),
                forms = forms.map { PetForm(it.obj.id, it.npc.id, it.runs, it.unlock, it.itemUse, it.emotes) },
                mainDrop = base.mainDrop,
            )
        }

    private val byKey: Map<String, Pet> = all.associateBy { it.key }

    private val byObj: Map<Int, Pair<Pet, PetForm>> =
        buildMap { for (pet in all) for (form in pet.forms) put(form.objId, pet to form) }

    private val byNpc: Map<Int, Pair<Pet, PetForm>> =
        buildMap { for (pet in all) for (form in pet.forms) put(form.npcId, pet to form) }

    private val byObjName: Map<String, Pair<Pet, PetForm>> =
        buildMap { for (pet in all) for (form in pet.forms) put(form.obj, pet to form) }

    operator fun get(key: String): Pet = byKey[key] ?: error("Unknown pet: $key")

    fun forObj(objId: Int): Pair<Pet, PetForm>? = byObj[objId]

    fun forNpc(npcId: Int): Pair<Pet, PetForm>? = byNpc[npcId]

    fun forObj(obj: String): Pair<Pet, PetForm>? = byObjName[obj]

    fun formForObj(objId: Int): PetForm? = byObj[objId]?.second
}
