package org.rsmod.content.other.pets.dogs

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.DogsRow
import org.rsmod.api.table.PuppyColoursRow
import org.rsmod.content.other.pets.PetForm
import org.rsmod.game.entity.Player

class DogBreed(row: PuppyColoursRow) {
    val id: Int = row.rowId
    val key: String = RSCM.getReverseMapping(RSCMType.DBROW, row.rowId).removePrefix("dbrow.")
    val name: String = row.name
    val unlock: String? = row.unlockBit?.let { "varbit.dog_unlock_$it" }

    fun unlocked(player: Player): Boolean {
        val varbit = unlock ?: return true
        return player.vars[varbit] != 0
    }
}

class DogForm(val breed: DogBreed, val colour: String, val puppy: Boolean, val form: PetForm) {
    val obj: String
        get() = form.obj

    val npc: String
        get() = form.npc
}

object Dogs {
    val breeds: List<DogBreed> = PuppyColoursRow.all().map(::DogBreed).sortedBy { it.name }

    private val breedById: Map<Int, DogBreed> = breeds.associateBy { it.id }

    val all: List<DogForm> =
        DogsRow.all().flatMap { row ->
            val breed = breedById.getValue(row.breed.rowId)
            listOf(
                DogForm(breed, row.colour, puppy = true, PetForm(row.puppyObj.id, row.puppyNpc.id, runs = false)),
                DogForm(breed, row.colour, puppy = false, PetForm(row.dogObj.id, row.dogNpc.id, runs = true)),
            )
        }

    private val byObj: Map<Int, DogForm> = all.associateBy { it.form.objId }

    fun colours(breed: DogBreed): List<String> = all.filter { it.breed === breed && it.puppy }.map { it.colour }

    fun of(breed: DogBreed, colour: String, puppy: Boolean): DogForm =
        all.first { it.breed === breed && it.colour == colour && it.puppy == puppy }

    fun adult(form: DogForm): DogForm = of(form.breed, form.colour, puppy = false)

    fun forObj(objId: Int): DogForm? = byObj[objId]

    fun formForObj(objId: Int): PetForm? = byObj[objId]?.form

    fun objsOf(puppy: Boolean): List<String> = all.filter { it.puppy == puppy }.map { it.obj }
}
