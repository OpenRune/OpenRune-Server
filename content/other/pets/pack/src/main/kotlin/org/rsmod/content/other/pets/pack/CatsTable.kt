package org.rsmod.content.other.pets.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object CatsTable {
    const val STAGE = 0
    const val COLOUR = 1
    const val OBJ = 2
    const val NPC = 3

    const val KITTEN = 0
    const val CAT = 1
    const val OVERGROWN = 2
    const val WILY = 3
    const val LAZY = 4

    const val GREY = 0
    const val WHITE = 1
    const val BROWN = 2
    const val BLACK = 3
    const val BROWN_GREY = 4
    const val BLUE_GREY = 5
    const val HELL = 6

    fun cats() = dbTable("dbtable.cats", serverOnly = true) {
        column("stage", STAGE, VarType.INT)
        column("colour", COLOUR, VarType.INT)
        column("obj", OBJ, VarType.OBJ)
        column("npc", NPC, VarType.NPC)

        fun cat(stage: Int, colour: Int, obj: String, npc: String) {
            row("dbrow.cat_${obj.removePrefix("obj.")}") {
                column(STAGE, stage)
                column(COLOUR, colour)
                columnRSCM(OBJ, obj)
                columnRSCM(NPC, npc)
            }
        }

        cat(KITTEN, GREY, "obj.kittenobject", "npc.kittenpet1")
        cat(KITTEN, WHITE, "obj.kittenobject_light", "npc.kittenpet_light")
        cat(KITTEN, BROWN, "obj.kittenobject_brown", "npc.kittenpet_brown")
        cat(KITTEN, BLACK, "obj.kittenobject_black", "npc.kittenpet_black")
        cat(KITTEN, BROWN_GREY, "obj.kittenobject_browngrey", "npc.kittenpet_browngrey")
        cat(KITTEN, BLUE_GREY, "obj.kittenobject_bluegrey", "npc.kittenpet_bluegrey")
        cat(KITTEN, HELL, "obj.kittenobject_hell", "npc.kittenpet_hell")

        cat(CAT, GREY, "obj.growncatobject", "npc.growncat")
        cat(CAT, WHITE, "obj.growncatobject_light", "npc.growncat_light")
        cat(CAT, BROWN, "obj.growncatobject_brown", "npc.growncat_brown")
        cat(CAT, BLACK, "obj.growncatobject_black", "npc.growncat_black")
        cat(CAT, BROWN_GREY, "obj.growncatobject_browngrey", "npc.growncat_browngrey")
        cat(CAT, BLUE_GREY, "obj.growncatobject_bluegrey", "npc.growncat_bluegrey")
        cat(CAT, HELL, "obj.growncatobject_hell", "npc.growncat_hell")

        cat(OVERGROWN, GREY, "obj.overgrowncatobject", "npc.overgrowncat")
        cat(OVERGROWN, WHITE, "obj.overgrowncatobject_light", "npc.overgrowncat_light")
        cat(OVERGROWN, BROWN, "obj.overgrowncatobject_brown", "npc.overgrowncat_brown")
        cat(OVERGROWN, BLACK, "obj.overgrowncatobject_black", "npc.overgrowncat_black")
        cat(OVERGROWN, BROWN_GREY, "obj.overgrowncatobject_browngrey", "npc.overgrowncat_browngrey")
        cat(OVERGROWN, BLUE_GREY, "obj.overgrowncatobject_bluegrey", "npc.overgrowncat_bluegrey")
        cat(OVERGROWN, HELL, "obj.overgrowncatobject_hell", "npc.overgrowncat_hell")

        cat(WILY, GREY, "obj.wileycatobject", "npc.wileycat")
        cat(WILY, WHITE, "obj.wileycatobject_light", "npc.wileycat_light")
        cat(WILY, BROWN, "obj.wileycatobject_brown", "npc.wileycat_brown")
        cat(WILY, BLACK, "obj.wileycatobject_black", "npc.wileycat_black")
        cat(WILY, BROWN_GREY, "obj.wileycatobject_browngrey", "npc.wileycat_browngrey")
        cat(WILY, BLUE_GREY, "obj.wileycatobject_bluegrey", "npc.wileycat_bluegrey")
        cat(WILY, HELL, "obj.wileycatobject_hell", "npc.wileycat_hell")

        cat(LAZY, GREY, "obj.lazycatobject", "npc.lazycat")
        cat(LAZY, WHITE, "obj.lazycatobject_light", "npc.lazycat_light")
        cat(LAZY, BROWN, "obj.lazycatobject_brown", "npc.lazycat_brown")
        cat(LAZY, BLACK, "obj.lazycatobject_black", "npc.lazycat_black")
        cat(LAZY, BROWN_GREY, "obj.lazycatobject_browngrey", "npc.lazycat_browngrey")
        cat(LAZY, BLUE_GREY, "obj.lazycatobject_bluegrey", "npc.lazycat_bluegrey")
        cat(LAZY, HELL, "obj.lazycatobject_hell", "npc.lazycat_hell")
    }
}
