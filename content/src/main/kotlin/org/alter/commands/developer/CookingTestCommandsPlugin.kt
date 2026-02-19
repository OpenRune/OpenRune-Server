package org.alter.commands.developer

import org.alter.api.Skills
import org.alter.api.ext.message
import org.alter.game.model.priv.Privilege
import org.alter.game.model.Tile
import org.alter.game.model.move.moveTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.CommandEvent
import org.alter.rscm.RSCM.asRSCM

class CookingTestCommandsPlugin : PluginEvent() {

    private enum class CookTestSet(
        val key: String,
        val level: Int,
        val qty: Int,
    ) {
        LOW("low", level = 1, qty = 5),            // 4 items × 5 = 20 slots
        MID("mid", level = 25, qty = 5),            // 3 items × 5 = 15 slots
        HIGH("high", level = 80, qty = 3),           // 3 items × 3 = 9 slots
        FISH_LOW("fishlow", level = 15, qty = 4),    // 6 items × 4 = 24 slots
        FISH_MID("fishmid", level = 50, qty = 5),    // 5 items × 5 = 25 slots
        FISH_HIGH("fishhigh", level = 99, qty = 3),  // 8 items × 3 = 24 slots
        FISH_SPECIAL("fishspec", level = 70, qty = 5), // 4 items × 5 = 20 slots
        PREP_FISH("prepfish", level = 99, qty = 5),  // 5 items × 5 = 25 slots
        MEAT("meat", level = 1, qty = 3),             // 7 items × 3 = 21 slots
        SPIT("spit", level = 1, qty = 3),             // 6 items × 3 = 18 slots
        PIES_INGR("piesingr", level = 30, qty = 3),  // 7 items × 3 = 21 slots
        PIES_BAKE("piesbake", level = 95, qty = 5),  // 4 items × 5 = 20 slots
        CAKES("cakes", level = 50, qty = 3),          // 7 items × 3 = 21 slots
        PIZZA("pizza", level = 65, qty = 3),          // 8 items × 3 = 24 slots
        STEW("stew", level = 60, qty = 5),            // 5 items × 5 = 25 slots
        BREAD("bread", level = 58, qty = 5),          // 2 items × 5 = 10 slots
        POTATO("potato", level = 68, qty = 3),        // 8 items × 3 = 24 slots
        WINE("wine", level = 35, qty = 5),            // 2 items × 5 = 10 slots
        MISC("misc", level = 67, qty = 3),            // 7 items × 3 = 21 slots
        SNAILS("snails", level = 22, qty = 5),        // 3 items × 5 = 15 slots
        HUNTER("hunter", level = 92, qty = 3);        // 6 items × 3 = 18 slots

        companion object {
            fun fromKey(raw: String): CookTestSet? {
                val token = raw.removePrefix("--").trim().lowercase()
                return entries.firstOrNull { it.key == token }
            }
        }
    }

    override fun init() {
        on<CommandEvent> {
            where {
                command.equals("cooktest", ignoreCase = true) &&
                    player.world.privileges.isEligible(player.privilege, Privilege.DEV_POWER)
            }
            then {
                val args = args.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }

                val set = parseCookTestSet(args)
                if (set == null) {
                    sendCookTestHelp(player)
                    return@then
                }

                player.getSkills().setBaseLevel(Skills.COOKING, set.level)

                val keys = keysFor(set)
                if (keys.isEmpty()) {
                    player.message("No items configured for preset '${set.key}'.")
                    return@then
                }

                var addedAny = false
                var failed = 0
                for (key in keys) {
                    val id = runCatching { key.asRSCM() }.getOrNull() ?: continue
                    val result = player.inventory.add(id, set.qty)
                    if (result.hasSucceeded()) {
                        addedAny = true
                    } else {
                        failed++
                    }
                }

                if (!addedAny) {
                    player.message("Couldn't add any items (inventory full?).")
                } else if (failed > 0) {
                    player.message("Added cooking test items (some didn't fit: $failed).")
                } else {
                    player.message("Applied cooktest preset '${set.key}' (x${set.qty} each, Cooking=${set.level}).")
                }
            }
        }
    }

    private fun parseCookTestSet(args: List<String>): CookTestSet? {
        if (args.isEmpty() || args.any { it.equals("help", ignoreCase = true) || it == "-h" || it == "--help" }) {
            return null
        }

        return CookTestSet.fromKey(args.first())
    }

    private fun keysFor(set: CookTestSet): List<String> =
        when (set) {
            CookTestSet.LOW -> listOf(
                "items.raw_shrimp",
                "items.raw_anchovies",
                "items.raw_sardine",
                "items.raw_herring"
            )

            CookTestSet.MID -> listOf(
                "items.raw_mackerel",
                "items.raw_trout",
                "items.raw_salmon"
            )

            CookTestSet.HIGH -> listOf(
                "items.raw_lobster",
                "items.raw_swordfish",
                "items.raw_shark"
            )

            CookTestSet.FISH_LOW -> listOf(
                "items.raw_shrimp",
                "items.raw_anchovies",
                "items.raw_sardine",
                "items.raw_herring",
                "items.raw_mackerel",
                "items.raw_trout"
            )

            CookTestSet.FISH_MID -> listOf(
                "items.raw_cod",
                "items.raw_pike",
                "items.raw_salmon",
                "items.raw_tuna",
                "items.raw_lobster"
            )

            CookTestSet.FISH_HIGH -> listOf(
                "items.raw_bass",
                "items.raw_swordfish",
                "items.raw_monkfish",
                "items.raw_shark",
                "items.raw_seaturtle",
                "items.raw_anglerfish",
                "items.raw_dark_crab",
                "items.raw_mantaray"
            )

            CookTestSet.FISH_SPECIAL -> listOf(
                "items.hunting_raw_fish_special",
                "items.mort_slimey_eel",
                "items.raw_cave_eel",
                "items.tbwt_raw_karambwan"
            )

            CookTestSet.PREP_FISH -> listOf(
                "items.knife",
                "items.raw_guppy",
                "items.raw_cavefish",
                "items.raw_tetra",
                "items.raw_catfish"
            )

            CookTestSet.MEAT -> listOf(
                "items.raw_beef",
                "items.raw_chicken",
                "items.raw_rat_meat",
                "items.raw_bear_meat",
                "items.raw_rabbit",
                "items.raw_ugthanki_meat",
                "items.raw_chompy"
            )

            CookTestSet.SPIT -> listOf(
                "items.spit_iron",
                "items.spit_raw_bird_meat",
                "items.spit_raw_beast_meat",
                "items.spit_skewered_bird_meat",
                "items.spit_skewered_beast_meat",
                "items.spit_skewered_rabbit_meat"
            )

            CookTestSet.PIES_INGR -> listOf(
                "items.pie_shell",
                "items.tomato",
                "items.onion",
                "items.cabbage",
                "items.redberries",
                "items.cooked_meat",
                "items.cooking_apple"
            )

            CookTestSet.PIES_BAKE -> listOf(
                "items.uncooked_garden_pie",
                "items.uncooked_redberry_pie",
                "items.uncooked_meat_pie",
                "items.uncooked_apple_pie"
            )

            CookTestSet.CAKES -> listOf(
                "items.cake_tin",
                "items.egg",
                "items.bucket_milk",
                "items.pot_flour",
                "items.uncooked_cake",
                "items.cake",
                "items.chocolate_bar"
            )

            CookTestSet.PIZZA -> listOf(
                "items.pizza_base",
                "items.tomato",
                "items.cheese",
                "items.cooked_meat",
                "items.anchovies",
                "items.pineapple_ring",
                "items.uncooked_pizza",
                "items.plain_pizza"
            )

            CookTestSet.STEW -> listOf(
                "items.bowl_water",
                "items.potato",
                "items.cooked_meat",
                "items.spice",
                "items.uncooked_stew"
            )

            CookTestSet.BREAD -> listOf(
                "items.bread_dough",
                "items.pitta_dough"
            )

            CookTestSet.POTATO -> listOf(
                "items.potato",
                "items.potato_baked",
                "items.pat_of_butter",
                "items.potato_butter",
                "items.cheese",
                "items.bowl_egg_scrambled",
                "items.bowl_mushroom_onion",
                "items.bowl_tuna_corn"
            )

            CookTestSet.WINE -> listOf(
                "items.grapes",
                "items.jug_water"
            )

            CookTestSet.MISC -> listOf(
                "items.sweetcorn_raw",
                "items.egg",
                "items.bowl_empty",
                "items.bowl_onion_chopped",
                "items.bowl_mushroom_sliced",
                "items.tuna",
                "items.sweetcorn_cooked"
            )

            CookTestSet.SNAILS -> listOf(
                "items.snail_corpse1",
                "items.snail_corpse2",
                "items.snail_corpse3"
            )

            CookTestSet.HUNTER -> listOf(
                "items.hunting_larupia_meat",
                "items.hunting_graahk_meat",
                "items.hunting_kyatt_meat",
                "items.hunting_fennecfox_meat",
                "items.hunting_antelopesun_meat",
                "items.hunting_antelopemoon_meat"
            )
        }

    private fun sendCookTestHelp(player: org.alter.game.model.entity.Player) {
        player.message("Usage: ::cooktest <preset>")
        player.message("Presets: ${CookTestSet.entries.joinToString(", ") { it.key }}")
        val examples = CookTestSet.entries.map { "::cooktest ${it.key}" }
        val lines = mutableListOf<String>()
        var current = "Examples: "
        for ((i, ex) in examples.withIndex()) {
            val separator = if (i == 0) "" else " | "
            if (current.length + separator.length + ex.length > 250) {
                lines.add(current)
                current = ex
            } else {
                current += separator + ex
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        lines.forEach { player.message(it) }
    }
}
