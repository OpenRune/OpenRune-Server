package dev.openrune.tables.skills

import dev.openrune.tables.skills.runecrafting.Alters
import dev.openrune.tables.skills.runecrafting.CombinationRune
import dev.openrune.tables.skills.runecrafting.RunecraftRune
import dev.openrune.tables.skills.runecrafting.Tiara

object Runecrafting {
    fun altars() = Alters.altars()

    fun runes() = RunecraftRune.runecraftRune()

    fun tiara() = Tiara.tiara()

    fun combo() = CombinationRune.runecraftComboRune()
}
