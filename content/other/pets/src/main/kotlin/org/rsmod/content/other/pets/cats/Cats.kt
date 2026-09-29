package org.rsmod.content.other.pets.cats

import dev.openrune.types.enums.enum
import org.rsmod.api.table.CatsRow
import org.rsmod.content.other.pets.PetForm

class CatStage(val id: Int, val name: String, val catchChance: Int) {
    companion object {
        val all: List<CatStage> = run {
            val chances = enum<Int, Int>("cat_catch_chances")
            enum<Int, String>("cat_stages").backing.map { (id, name) -> CatStage(id, name ?: "", chances[id] ?: 0) }
        }

        val Kitten: CatStage = of(0)
        val Cat: CatStage = of(1)
        val Overgrown: CatStage = of(2)
        val Wily: CatStage = of(3)
        val Lazy: CatStage = of(4)

        fun of(id: Int): CatStage = all.first { it.id == id }
    }
}

class CatColour(val id: Int, val name: String) {
    companion object {
        val all: List<CatColour> =
            enum<Int, String>("cat_colours").backing.map { (id, name) -> CatColour(id, name ?: "") }

        val Hell: CatColour = of(6)

        val natural: List<CatColour> = all.filter { it !== Hell }

        fun of(id: Int): CatColour = all.first { it.id == id }
    }
}

class CatForm(val stage: CatStage, val colour: CatColour, val form: PetForm) {
    val obj: String
        get() = form.obj

    val npc: String
        get() = form.npc

    val isHell: Boolean
        get() = colour === CatColour.Hell

    val isKitten: Boolean
        get() = stage === CatStage.Kitten
}

object Cats {
    val all: List<CatForm> =
        CatsRow.all().map {
            CatForm(CatStage.of(it.stage), CatColour.of(it.colour), PetForm(it.obj.id, it.npc.id, runs = false))
        }

    private val byObj: Map<Int, CatForm> = all.associateBy { it.form.objId }

    fun of(stage: CatStage, colour: CatColour): CatForm = all.first { it.stage === stage && it.colour === colour }

    fun forObj(objId: Int): CatForm? = byObj[objId]

    fun formForObj(objId: Int): PetForm? = byObj[objId]?.form

    fun objsOf(stage: CatStage): List<String> = all.filter { it.stage === stage }.map { it.obj }

    fun adultObjs(): List<String> = all.filter { !it.isKitten }.map { it.obj }
}
