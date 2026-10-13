package org.rsmod.content.other.nerocurator

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import java.awt.Color
import org.rsmod.api.player.dialogue.mesanims
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.ui.setColour
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CuratorPlugin @Inject constructor(private val npcs: NpcRepository, private val launcher: org.rsmod.api.player.protect.ProtectedAccessLauncher, private val players: PlayerList, private val appearance: CompletionistAppearanceFeed) : PluginScript() {
    private var curator: Npc? = null
    private val colours = intArrayOf(0xffffff, 0xb9c6cc, 0x50565c, 0x15191f, 0x9d2834, 0xdf354d, 0xfa8e9b, 0x731927, 0xeeb630, 0xffdf81, 0xc98636, 0xf0782a, 0x2859c4, 0x659eff, 0x22324f, 0x64dce5, 0x188775, 0x22c85a, 0x95dc70, 0x193d29, 0x8849c9, 0xc277e8, 0xed59bd, 0xf5a0d5)
    private val original = intArrayOf(65214, 65200, 65186, 62995, 0xffffff, 100)
    private val labels = arrayOf("Detail (top)", "Background (top)", "Detail (bottom)", "Background (bottom)", "Particle colour")
    private val items = arrayOf("obj.nero_completionist_2011", "obj.nero_completionist_hood")
    private val prices = intArrayOf(10_000_000, 5_000_000)

    override fun ScriptContext.startup() {
        onGameStartup { spawn(); appearance.start() }
        onOpNpc1("npc.nero_curator") {
            chatNpc(it.npc, mesanims.happy, "Welcome! I preserve the Completionist cape and hood.<br>I can sell you a set or help you customise its colours and particles.")
            when (choice3("Buy cape or hood", 1, "How do I make a variant?", 2, "Never mind", 3, title = "The Curator")) {
                1 -> trade()
                2 -> chatNpc(it.npc, mesanims.happy, "Use your regular Completionist cape on the matching cape item.<br>Keep your regular Completionist hood in your inventory.<br>All three become a fixed-colour cape and matching hood.")
            }
        }
        onOpNpc2("npc.nero_curator") { trade() }
        onOpNpc3("npc.nero_curator") { openEditor() }
        for (recipe in CompletionistRecipes.all) {
            onOpHeldU("obj.nero_completionist_2011", recipe.input) {
                val origin = coords
                val hoodSlot = inv.indexOfFirst { it?.id == "obj.nero_completionist_hood".asRSCM() }
                val inputs = listOf(it.firstSlot, it.secondSlot, hoodSlot).map { slot -> slot to inv[slot] }
                if (inputs.any { it.second == null }) { mes("You need the regular cape, hood and matching item in your inventory."); return@onOpHeldU }
                if (choice2("Combine cape, hood and item (consumes all three)", 1, "Cancel", 2, title = "Create ${recipe.name}?") == 1) {
                    if (coords != origin || !alive(player) || inputs.any { (slot, item) -> inv[slot] !== item }) return@onOpHeldU
                    val success = CompletionistFusion.combine(player, inv, recipe.input, recipe.cape, recipe.hood)
                    mes(if (success) "Your fixed-colour Completionist cape and matching hood are ready." else "You need the regular Completionist cape, regular hood and matching item in your inventory.")
                }
            }
        }
        onPlayerLogin {
            initialise(player)
            set(player, 34, 0)
            publish(player)
        }
        onPlayerLogout { appearance.remove(key(player)) }
        for (player in players) { initialise(player); set(player, 34, 0); publish(player) }
        onIfClose(INTERFACE) {
            set(player, 34, 0)
            publish(player)
        }
        for (n in 0..3) {
            onIfModalButton(component("channel$n")) {
                if (canEdit(player)) { set(player, 35, n); update(player) }
            }
        }
        for (name in arrayOf("particle", "particle_swatch")) {
            onIfModalButton(component(name)) {
                if (canEdit(player)) { set(player, 35, 4); update(player) }
            }
        }
        for (n in colours.indices) {
            onIfModalButton(component("colour$n")) {
                if (canEdit(player)) {
                    val channel = get(player, 35).coerceIn(0, 4)
                    set(player, 28 + channel, if (channel == 4) colours[n] else toHsl(colours[n]))
                    update(player)
                }
            }
        }
        onIfModalButton(component("hex")) {
            if (canEdit(player)) {
                val channel = get(player, 35).coerceIn(0, 4)
                val editor = get(player, 38)
                val input = stringDialog("Enter a colour as #RRGGBB:").trim().removePrefix("#")
                if (canEdit(player) && get(player, 38) == editor && input.matches(Regex("[0-9a-fA-F]{6}"))) {
                    val rgb = input.toInt(16)
                    set(player, 28 + channel, if (channel == 4) rgb else toHsl(rgb))
                    update(player)
                } else player.mes("Use six hexadecimal digits, for example #C277E8.")
            }
        }
        for ((name, delta) in arrayOf("size_down" to -25, "size_up" to 25)) {
            onIfModalButton(component(name)) {
                if (canEdit(player)) { set(player, 33, (get(player, 33) + delta).coerceIn(25, 300)); update(player) }
            }
        }
        for (n in 0..2) {
            onIfModalButton(component("save$n")) {
                if (canEdit(player)) {
                    for (c in 0..5) set(player, 7 + n * 7 + c, get(player, 28 + c))
                    set(player, 13 + n * 7, 1)
                    update(player, "Preset ${n + 1} saved.")
                }
            }
            onIfModalButton(component("load$n")) {
                if (canEdit(player)) {
                    if (get(player, 13 + n * 7) == 0) update(player, "Save a look in this preset first.")
                    else {
                        for (c in 0..5) set(player, 28 + c, get(player, 7 + n * 7 + c))
                        update(player, "Preset ${n + 1} loaded.")
                    }
                }
            }
        }
        onIfModalButton(component("reset")) {
            if (canEdit(player)) { for (c in 0..5) set(player, 28 + c, original[c]); update(player, "Original colours restored.") }
        }
        onIfModalButton(component("preview_mode")) {
            if (canEdit(player)) {
                set(player, 36, 1 - get(player, 36).coerceIn(0, 1))
                update(player)
            }
        }
        onIfModalButton(component("preview_particles")) {
            if (canEdit(player)) { set(player, 37, 1 - get(player, 37).coerceIn(0, 1)); update(player) }
        }
        onIfModalButton(component("done")) {
            if (canEdit(player)) {
                for (c in 0..5) set(player, c, get(player, 28 + c))
                set(player, 6, CuratorStyle.VERSION); set(player, 34, 0); publish(player)
                player.mes("Your Completionist colours and particles have been saved.")
                ifClose()
            }
        }
        onIfModalButton(component("cancel")) { set(player, 34, 0); publish(player); ifClose() }
        onCommand("curator") {
            completionistTestAccess()
            desc = "Visit The Curator in Edgeville."
            cheat {
                launcher.launch(player) { teleport(CoordGrid(x = 3087, z = 3493)) }
            }
        }
        onCommand("compcapes") {
            completionistTestAccess()
            desc = "Test Completionist capes: regular, trimmed, all, hoods, <variant>, recipe <variant>, list."
            cheat {
                val key = args.getOrNull(0)?.lowercase(java.util.Locale.ROOT) ?: "regular"
                val requested = if (key == "recipe") args.getOrNull(1)?.lowercase(java.util.Locale.ROOT) else key
                val recipe = CompletionistRecipes.all.firstOrNull { it.cape.removePrefix("obj.nero_completionist_") == requested }
                val regular = listOf("obj.nero_completionist_2011", "obj.nero_completionist_hood")
                val items = when {
                    key == "regular" -> regular
                    key == "trimmed" -> listOf("obj.nero_completionist_2011_trimmed", "obj.nero_completionist_hood_trimmed")
                    key == "all" -> regular + CompletionistRecipes.all.map { it.cape }
                    key == "hoods" -> listOf("obj.nero_completionist_hood", "obj.nero_completionist_hood_trimmed") + CompletionistRecipes.all.map { it.hood }
                    key == "recipe" && recipe != null -> regular + recipe.input
                    recipe != null -> listOf(recipe.cape, recipe.hood)
                    else -> emptyList()
                }
                if (items.isEmpty()) {
                    player.mes("Use ::compcapes, ::compcapes all, ::compcapes hoods, ::compcapes <variant> or ::compcapes recipe <variant>.")
                    for (group in CompletionistRecipes.all.map { it.cape.removePrefix("obj.nero_completionist_") }.chunked(4))player.mes(group.joinToString(", "))
                } else {
                    val success = CompletionistTestItems.grant(player, player.inv, items)
                    player.mes(if (success) "Completionist test items added: ${items.size}." else "Make ${items.size} free inventory slots. No test items were added.")
                }
            }
        }
    }

    override fun ScriptContext.shutdown() {
        appearance.close()
        curator?.let { npcs.del(it, Int.MAX_VALUE) }; curator = null
    }

    private fun spawn() {
        if (curator != null) return
        curator = Npc("npc.nero_curator", CoordGrid(x = 3088, z = 3493)).also {
            it.movementLocked = true
            npcs.add(it, Int.MAX_VALUE)
        }
        println("NERO_CURATOR_READY Edgeville 3088,3493; cape=10000000 hood=5000000")
    }

    private suspend fun ProtectedAccess.trade() {
        if (!nearCurator(player)) return
        when (choice3("Completionist cape - 10,000,000 coins", 1, "Completionist hood - 5,000,000 coins", 2, "Leave", 3, title = "The Curator's collection")) {
            1 -> purchase(0)
            2 -> purchase(1)
        }
    }

    private suspend fun ProtectedAccess.purchase(n: Int) {
        val origin = coords
        if (choice2("Buy for ${"%,d".format(java.util.Locale.US,prices[n])} coins", 1, "Cancel", 2, title = "Confirm purchase") != 1) return
        if (coords != origin || !nearCurator(player)) return
        val bought = CuratorPurchase.buy(player, player.inv, items[n], prices[n])
        player.mes(if (bought) "Thank you. Your Completionist ${if (n == 0) "cape" else "hood"} is ready." else "You need ${"%,d".format(java.util.Locale.US,prices[n])} coins and room in your inventory.")
    }

    private fun ProtectedAccess.openEditor() {
        if (!nearCurator(player)) return
        initialise(player)
        for (c in 0..5) set(player, 28 + c, get(player, c))
        ifOpenMainModal(INTERFACE)
        set(player, 34, 1); set(player, 35, 0); set(player, 36, 0); set(player, 37, 1); set(player, 38, (get(player, 38) + 1).coerceAtLeast(1))
        update(player)
    }

    private fun initialise(player: Player) {
        CuratorStyle.initialise(original, { get(player, it) }, { n, value -> set(player, n, value) })
    }

    private fun alive(player: Player) = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    private fun nearCurator(player: Player) = alive(player) && player.coords.level == 0 && player.coords.chebyshevDistance(CoordGrid(3088, 3493)) <= 6
    private fun canEdit(player: Player): Boolean = get(player, 34) == 1 && nearCurator(player) && player.ui.containsModal(INTERFACE)

    private fun update(player: Player, message: String = "Changes are previewed. Done saves them.") {
        for (c in 0..3) player.setColour(component("channel$c"), Color(fromHsl(get(player, 28 + c))))
        player.setColour(component("particle_swatch"), Color(get(player, 32)))
        player.ifSetText(component("size"), "${get(player,33)}%")
        val selected = get(player, 35).coerceIn(0, 4)
        player.ifSetText(component("selected"), labels[selected])
        player.ifSetText(component("status"), message)
        player.ifSetText(component("preview_particles"), "Particles: ${if (get(player, 37) == 1) "On" else "Off"}")
        for (n in 0..2) player.ifSetText(component("preset_label$n"), "Preset ${n + 1}${if (get(player,13 + n * 7) == 1) " *" else ""}")
        publish(player)
    }

    private fun publish(player: Player) {
        val base = if (get(player, 34) == 1) 28 else 0
        appearance.publish(key(player), mapOf("colours" to (0..3).map { get(player, base + it).coerceIn(0, 65535) }, "particleColour" to get(player, base + 4).coerceIn(0, 0xffffff), "size" to get(player, base + 5).coerceIn(25, 300), "editing" to (base == 28), "previewHood" to (get(player, 36) == 1), "previewParticles" to (get(player, 37) == 1)))
    }
    private fun key(player: Player) = player.displayName.lowercase(java.util.Locale.ROOT).replace(' ', '_').replace('\u00a0', '_')
    private fun get(player: Player, n: Int) = player.vars["varp.nero_style_$n"]
    private fun set(player: Player, n: Int, value: Int) = VarPlayerIntMapSetter.set(player, "varp.nero_style_$n", value)
    private fun component(name: String) = "component.nero_cape_customiser:$name"

    private fun toHsl(rgb: Int): Int {
        val r = (rgb shr 16 and 255) / 255.0; val g = (rgb shr 8 and 255) / 255.0; val b = (rgb and 255) / 255.0
        val min = minOf(r, g, b); val max = maxOf(r, g, b); val l = (min + max) / 2; val d = max - min
        val s = if (d == 0.0) 0.0 else d / (1 - kotlin.math.abs(2 * l - 1))
        var h = if (d == 0.0) 0.0 else when (max) { r -> ((g - b) / d) % 6; g -> (b - r) / d + 2; else -> (r - g) / d + 4 } / 6
        if (h < 0) h += 1
        return ((h * 64).toInt().coerceIn(0, 63) shl 10) or ((s * 8).toInt().coerceIn(0, 7) shl 7) or (l * 128).toInt().coerceIn(2, 126)
    }
    private fun fromHsl(hsl: Int): Int {
        val h = (hsl shr 10 and 63) / 64.0; val s = (hsl shr 7 and 7) / 7.0; val l = (hsl and 127) / 127.0
        val c = (1 - kotlin.math.abs(2 * l - 1)) * s; val x = c * (1 - kotlin.math.abs((h * 6) % 2 - 1)); val m = l - c / 2
        val channels = when ((h * 6).toInt()) { 0 -> doubleArrayOf(c, x, 0.0); 1 -> doubleArrayOf(x, c, 0.0); 2 -> doubleArrayOf(0.0, c, x); 3 -> doubleArrayOf(0.0, x, c); 4 -> doubleArrayOf(x, 0.0, c); else -> doubleArrayOf(c, 0.0, x) }
        return (((channels[0] + m) * 255).toInt() shl 16) or (((channels[1] + m) * 255).toInt() shl 8) or ((channels[2] + m) * 255).toInt()
    }

    private companion object {
        const val INTERFACE = "interface.nero_cape_customiser"
    }
}
