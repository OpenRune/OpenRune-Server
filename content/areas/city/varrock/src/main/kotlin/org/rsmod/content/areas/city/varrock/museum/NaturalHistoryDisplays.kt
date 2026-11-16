package org.rsmod.content.areas.city.varrock.museum

internal class NaturalHistoryModel(val component: String, val model: String)

internal class NaturalHistoryDisplay(
    val number: Int,
    val plaque: String,
    val varbit: String,
    val title: String,
    val models: List<NaturalHistoryModel>,
    val rotates: Boolean,
)

private fun model(name: String) =
    NaturalHistoryModel("component.vm_natural_history:vm_nathis_$name", "models.vm_nathis_$name")

internal val NaturalHistoryDisplays: List<NaturalHistoryDisplay> =
    listOf(
        NaturalHistoryDisplay(
            number = 1,
            plaque = "loc.vm_plaque_dragon",
            varbit = "varbit.vm_nathis_dragon_display",
            title = "The Dragon (Draconis Rex)",
            models = listOf(model("dragon_01"), model("dragon_02"), model("dragon_03")),
            rotates = false,
        ),
        NaturalHistoryDisplay(
            number = 2,
            plaque = "loc.vm_plaque_wyvern",
            varbit = "varbit.vm_nathis_wyvern_display",
            title = "The Wyvern (Draconis Ossis)",
            models = listOf(model("wyvern")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 3,
            plaque = "loc.vm_plaque_battle_tortoise",
            varbit = "varbit.vm_nathis_tortoise_display",
            title = "The Giant Tortoise (Testudines Giganteus)",
            models = listOf(model("battle_tortoise")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 4,
            plaque = "loc.vm_plaque_lizard",
            varbit = "varbit.vm_nathis_lizard_display",
            title = "The Giant Lizard (Lacertilia Giganteus)",
            models = listOf(model("lizard")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 5,
            plaque = "loc.vm_plaque_leech",
            varbit = "varbit.vm_nathis_leech_display",
            title = "The Giant Leech (Hirudinea Acidia)",
            models = listOf(model("leech")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 6,
            plaque = "loc.vm_plaque_camel",
            varbit = "varbit.vm_nathis_camel_display",
            title = "The Camel (Camelus Bactrian)",
            models = listOf(model("camel")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 7,
            plaque = "loc.vm_plaque_mole",
            varbit = "varbit.vm_nathis_mole_display",
            title = "The Giant Mole (Talpidae Wysonian)",
            models = listOf(model("mole")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 8,
            plaque = "loc.vm_plaque_penguine",
            varbit = "varbit.vm_nathis_penguin_display",
            title = "The Penguin (Spheniscidae Callidus)",
            models = listOf(model("penguine")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 9,
            plaque = "loc.vm_plaque_terrorbird",
            varbit = "varbit.vm_nathis_terrorbird_display",
            title = "The Terrorbird (Aves Terror)",
            models = listOf(model("terrorbird")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 10,
            plaque = "loc.vm_plaque_kalphite_queen",
            varbit = "varbit.vm_nathis_kalphite_display",
            title = "The Kalphite Queen (Kalphiscarabeinae Pasha)",
            models = listOf(model("kalphite_queen")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 11,
            plaque = "loc.vm_plaque_monkey",
            varbit = "varbit.vm_nathis_monkey_display",
            title = "The Monkey (Simiiformes Karamjan)",
            models = listOf(model("monkey")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 12,
            plaque = "loc.vm_plaque_seaslug",
            varbit = "varbit.vm_nathis_seaslug_display",
            title = "The Sea Slug (Opisthobranchia Alucinor)",
            models =
                listOf(
                    model("seaslugs_1"),
                    model("seaslugs_2"),
                    model("seaslugs_3"),
                    model("seaslugs_4"),
                ),
            rotates = false,
        ),
        NaturalHistoryDisplay(
            number = 13,
            plaque = "loc.vm_plaque_snake",
            varbit = "varbit.vm_nathis_snake_display",
            title = "The Snake (Serpentes Fellis)",
            models = listOf(model("snake")),
            rotates = true,
        ),
        NaturalHistoryDisplay(
            number = 14,
            plaque = "loc.vm_plaque_snail",
            varbit = "varbit.vm_nathis_snail_display",
            title = "The Giant Snail (Achatina Acidia Giganteus)",
            models = listOf(model("snail")),
            rotates = true,
        ),
    )
