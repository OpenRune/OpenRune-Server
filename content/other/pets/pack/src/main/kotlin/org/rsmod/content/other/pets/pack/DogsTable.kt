package org.rsmod.content.other.pets.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object DogsTable {
    const val BREED = 0
    const val COLOUR = 1
    const val PUPPY_OBJ = 2
    const val PUPPY_NPC = 3
    const val DOG_OBJ = 4
    const val DOG_NPC = 5

    fun dogs() = dbTable("dbtable.dogs", serverOnly = true) {
        column("breed", BREED, VarType.DBROW)
        column("colour", COLOUR, VarType.STRING)
        column("puppy_obj", PUPPY_OBJ, VarType.OBJ)
        column("puppy_npc", PUPPY_NPC, VarType.NPC)
        column("dog_obj", DOG_OBJ, VarType.OBJ)
        column("dog_npc", DOG_NPC, VarType.NPC)

        fun dog(row: String, breed: String, colour: String, puppyObj: String, puppyNpc: String, dogObj: String, dogNpc: String) {
            row(row) {
                columnRSCM(BREED, breed)
                column(COLOUR, colour)
                columnRSCM(PUPPY_OBJ, puppyObj)
                columnRSCM(PUPPY_NPC, puppyNpc)
                columnRSCM(DOG_OBJ, dogObj)
                columnRSCM(DOG_NPC, dogNpc)
            }
        }

        dog(
            "dbrow.dog_labrador_yellow",
            "dbrow.labrador",
            "Golden",
            "obj.labrador_yellow_puppy_object",
            "npc.labrador_yellow_puppy",
            "obj.labrador_yellow_object",
            "npc.labrador_yellow",
        )
        dog(
            "dbrow.dog_labrador_brown",
            "dbrow.labrador",
            "Chocolate",
            "obj.labrador_choco_puppy_object",
            "npc.labrador_choco_puppy",
            "obj.labrador_brown_object",
            "npc.labrador_brown",
        )
        dog(
            "dbrow.dog_labrador_black",
            "dbrow.labrador",
            "Black",
            "obj.labrador_black_puppy_object",
            "npc.labrador_black_puppy",
            "obj.labrador_black_object",
            "npc.labrador_black",
        )

        dog(
            "dbrow.dog_pug_fawn",
            "dbrow.pug",
            "Fawn",
            "obj.pug_fawn_puppy_object",
            "npc.pug_fawn_puppy",
            "obj.pug_fawn_object",
            "npc.pug_fawn",
        )
        dog(
            "dbrow.dog_pug_black",
            "dbrow.pug",
            "Black",
            "obj.pug_black_puppy_object",
            "npc.pug_black_puppy",
            "obj.pug_black_object",
            "npc.pug_black",
        )
        dog(
            "dbrow.dog_pug_brown",
            "dbrow.pug",
            "Brown",
            "obj.pug_brown_puppy_object",
            "npc.pug_brown_puppy",
            "obj.pug_brown_object",
            "npc.pug_brown",
        )

        dog(
            "dbrow.dog_spaniel_red",
            "dbrow.spaniel",
            "Red",
            "obj.spaniel_red_puppy_object",
            "npc.spaniel_red_puppy",
            "obj.spaniel_red_object",
            "npc.spaniel_red",
        )
        dog(
            "dbrow.dog_spaniel_white",
            "dbrow.spaniel",
            "White",
            "obj.spaniel_white_puppy_object",
            "npc.spaniel_white_puppy",
            "obj.spaniel_white_object",
            "npc.spaniel_white",
        )
        dog(
            "dbrow.dog_spaniel_black",
            "dbrow.spaniel",
            "Black",
            "obj.spaniel_black_puppy_object",
            "npc.spaniel_black_puppy",
            "obj.spaniel_black_object",
            "npc.spaniel_black",
        )

        dog(
            "dbrow.dog_shepard_toasted",
            "dbrow.shepard",
            "Toasted",
            "obj.shepard_toasted_puppy_object",
            "npc.shepard_toasted_puppy",
            "obj.shepard_toasted_object",
            "npc.shepard_toasted",
        )
        dog(
            "dbrow.dog_shepard_merle",
            "dbrow.shepard",
            "Merle",
            "obj.shepard_merle_puppy_object",
            "npc.shepard_merle_puppy",
            "obj.shepard_merle_object",
            "npc.shepard_merle",
        )
        dog(
            "dbrow.dog_shepard_choco",
            "dbrow.shepard",
            "Chocolate",
            "obj.shepard_choco_puppy_object",
            "npc.shepard_choco_puppy",
            "obj.shepard_choco_object",
            "npc.shepard_choco",
        )

        dog(
            "dbrow.dog_collie_bw",
            "dbrow.collie",
            "Black & White",
            "obj.collie_bw_puppy_object",
            "npc.collie_bw_puppy",
            "obj.collie_bw_object",
            "npc.collie_bw",
        )
        dog(
            "dbrow.dog_collie_choco",
            "dbrow.collie",
            "Chocolate",
            "obj.collie_choco_puppy_object",
            "npc.collie_choco_puppy",
            "obj.collie_choco_object",
            "npc.collie_choco",
        )
        dog(
            "dbrow.dog_collie_merle",
            "dbrow.collie",
            "Merle",
            "obj.collie_merle_puppy_object",
            "npc.collie_merle_puppy",
            "obj.collie_merle_object",
            "npc.collie_merle",
        )

        dog(
            "dbrow.dog_chihuahua_tan",
            "dbrow.chihuahua",
            "Tan",
            "obj.chihuahua_tan_puppy_object",
            "npc.chihuahua_tan_puppy",
            "obj.chihuahua_tan_object",
            "npc.chihuahua_tan",
        )
        dog(
            "dbrow.dog_chihuahua_toasted",
            "dbrow.chihuahua",
            "Toasted",
            "obj.chihuahua_toasted_puppy_object",
            "npc.chihuahua_toasted_puppy",
            "obj.chihuahua_toasted_object",
            "npc.chihuahua_toasted",
        )
        dog(
            "dbrow.dog_chihuahua_white",
            "dbrow.chihuahua",
            "White",
            "obj.chihuahua_white_puppy_object",
            "npc.chihuahua_white_puppy",
            "obj.chihuahua_white_object",
            "npc.chihuahua_white",
        )

        dog(
            "dbrow.dog_corgi_tan",
            "dbrow.corgi",
            "Tan",
            "obj.corgi_tan_puppy_object",
            "npc.corgi_tan_puppy",
            "obj.corgi_tan_object",
            "npc.corgi_tan",
        )
        dog(
            "dbrow.dog_corgi_toasted",
            "dbrow.corgi",
            "Toasted",
            "obj.corgi_toasted_puppy_object",
            "npc.corgi_toasted_puppy",
            "obj.corgi_toasted_object",
            "npc.corgi_toasted",
        )
        dog(
            "dbrow.dog_corgi_yellow",
            "dbrow.corgi",
            "Fawn",
            "obj.corgi_yellow_puppy_object",
            "npc.corgi_yellow_puppy",
            "obj.corgi_yellow_object",
            "npc.corgi_yellow",
        )

        dog(
            "dbrow.dog_greyhound_grey",
            "dbrow.greyhound",
            "Grey",
            "obj.greyhound_grey_puppy_object",
            "npc.greyhound_grey_puppy",
            "obj.greyhound_grey_object",
            "npc.greyhound_grey",
        )
        dog(
            "dbrow.dog_greyhound_cream",
            "dbrow.greyhound",
            "Cream",
            "obj.greyhound_cream_puppy_object",
            "npc.greyhound_cream_puppy",
            "obj.greyhound_cream_object",
            "npc.greyhound_cream",
        )
        dog(
            "dbrow.dog_greyhound_tan",
            "dbrow.greyhound",
            "Tan",
            "obj.greyhound_tan_puppy_object",
            "npc.greyhound_tan_puppy",
            "obj.greyhound_tan_object",
            "npc.greyhound_tan",
        )

        dog(
            "dbrow.dog_husky_bw",
            "dbrow.husky",
            "Black & White",
            "obj.husky_bw_puppy_object",
            "npc.husky_bw_puppy",
            "obj.husky_bw_object",
            "npc.husky_bw",
        )
        dog(
            "dbrow.dog_husky_choco",
            "dbrow.husky",
            "Chocolate",
            "obj.husky_choco_puppy_object",
            "npc.husky_choco_puppy",
            "obj.husky_choco_object",
            "npc.husky_choco",
        )
        dog(
            "dbrow.dog_husky_grey",
            "dbrow.husky",
            "Grey",
            "obj.husky_grey_puppy_object",
            "npc.husky_grey_puppy",
            "obj.husky_grey_object",
            "npc.husky_grey",
        )

        dog(
            "dbrow.dog_samoyed_white",
            "dbrow.samoyed",
            "White",
            "obj.samoyed_white_puppy_object",
            "npc.samoyed_white_puppy",
            "obj.samoyed_white_object",
            "npc.samoyed_white",
        )
        dog(
            "dbrow.dog_samoyed_yellow",
            "dbrow.samoyed",
            "Golden",
            "obj.samoyed_yellow_puppy_object",
            "npc.samoyed_yellow_puppy",
            "obj.samoyed_yellow_object",
            "npc.samoyed_yellow",
        )
        dog(
            "dbrow.dog_samoyed_black",
            "dbrow.samoyed",
            "Black",
            "obj.samoyed_black_puppy_object",
            "npc.samoyed_black_puppy",
            "obj.samoyed_black_object",
            "npc.samoyed_black",
        )

        dog(
            "dbrow.dog_shiba_tan",
            "dbrow.shiba",
            "Tan",
            "obj.shiba_tan_puppy_object",
            "npc.shiba_tan_puppy",
            "obj.shiba_tan_object",
            "npc.shiba_tan",
        )
        dog(
            "dbrow.dog_shiba_toasted",
            "dbrow.shiba",
            "Toasted",
            "obj.shiba_toasted_puppy_object",
            "npc.shiba_toasted_puppy",
            "obj.shiba_toasted_object",
            "npc.shiba_toasted",
        )
        dog(
            "dbrow.dog_shiba_white",
            "dbrow.shiba",
            "White",
            "obj.shiba_white_puppy_object",
            "npc.shiba_white_puppy",
            "obj.shiba_white_object",
            "npc.shiba_white",
        )

        dog(
            "dbrow.dog_yorkie_brown",
            "dbrow.yorkie",
            "Brown",
            "obj.yorkie_brown_puppy_object",
            "npc.yorkie_brown_puppy",
            "obj.yorkie_brown_object",
            "npc.yorkie_brown",
        )
        dog(
            "dbrow.dog_yorkie_white",
            "dbrow.yorkie",
            "White",
            "obj.yorkie_white_puppy_object",
            "npc.yorkie_white_puppy",
            "obj.yorkie_white_object",
            "npc.yorkie_white",
        )
        dog(
            "dbrow.dog_yorkie_yellow",
            "dbrow.yorkie",
            "Golden",
            "obj.yorkie_yellow_puppy_object",
            "npc.yorkie_yellow_puppy",
            "obj.yorkie_yellow_object",
            "npc.yorkie_yellow",
        )
    }
}
