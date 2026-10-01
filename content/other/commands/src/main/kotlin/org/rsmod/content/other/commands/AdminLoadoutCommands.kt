package org.rsmod.content.other.commands

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.withBits

class AdminLoadoutCommands @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (loadout in AdminLoadout.entries) {
            onCommand(loadout.command, loadout.description, {
                if (args.isNotEmpty()) {
                    player.mes("Usage: ::${loadout.command}")
                } else {
                    giveLoadout(player, loadout)
                }
            })
        }
    }

    private fun giveLoadout(player: Player, loadout: AdminLoadout) {
        val items = loadout.items()
        val result = player.invTransaction(player.inv) {
            val inventory = select(player.inv)
            for (item in items) {
                add(inventory, item.id, item.count, vars = item.vars, strict = true)
            }
        }
        if (result.success) {
            player.mes("${loadout.label} added to your inventory. Your equipment is unchanged.")
        } else {
            player.mes("Not enough inventory space for ${loadout.label.lowercase()}. Nothing was added or removed.")
        }
    }
}

internal enum class AdminLoadout(
    val command: String,
    val label: String,
    val description: String,
    private val symbols: List<String>,
) {
    RUNES(
        "allrunes", "All runes (5,000 each)", "Spawn every regular rune x 5000 in your inventory",
        listOf(
            "airrune", "waterrune", "earthrune", "firerune", "mindrune", "bodyrune",
            "cosmicrune", "chaosrune", "naturerune", "lawrune", "deathrune", "astralrune",
            "bloodrune", "soulrune", "wrathrune", "mistrune", "dustrune", "mudrune",
            "smokerune", "steamrune", "lavarune", "sunfirerune", "aetherrune", "eoc_rune",
        ),
    ),
    MELEE(
        "maxmelee", "Max melee gear", "Spawn endgame melee gear with Torva and Oathplate alternatives",
        listOf(
            "torva_helm", "torva_chest", "torva_legs", "infernal_cape", "amulet_of_rancour",
            "scythe_of_vitur", "ferocious_gloves", "avernic_treads_max", "ultor_ring",
            "osmumtens_fang", "infernal_defender", "oathplate_helm", "oathplate_chest", "oathplate_legs",
        ),
    ),
    RANGED(
        "maxrange", "Max ranged gear", "Spawn Masori, Twisted bow, quiver and 5000 dragon arrows",
        listOf(
            "masori_mask_fortified", "masori_body_fortified", "masori_chaps_fortified",
            "dizanas_quiver_infinite", "zenyte_necklace_enchanted", "twisted_bow",
            "zaryte_vambraces", "avernic_treads_max", "venator_ring", "dragon_arrow",
        ),
    ),
    MAGIC(
        "maxmage", "Max magic gear", "Spawn Ancestral, charged Shadow and Kodai with fortified ward",
        listOf(
            "ancestral_hat", "ancestral_robe_top", "ancestral_robe_bottom", "ma2_saradomin_cape",
            "occult_necklace", "tumekens_shadow", "zenyte_bracelet_enchanted", "avernic_treads_max",
            "magus_ring", "kodai_wand", "elidinis_ward_fortified",
        ),
    );

    fun items(): List<InvObj> = symbols.map { symbol ->
        val type = checkNotNull(ServerCacheManager.getItem("obj.$symbol".asRSCM(RSCMType.OBJ)))
        val count = if (this == RUNES || symbol == "dragon_arrow") 5_000 else 1
        val vars = if (symbol == "tumekens_shadow") {
            val charges = checkNotNull(ServerCacheManager.getVarObj("varobj.tumeken_charges".asRSCM(RSCMType.VAROBJ)))
            0.withBits(charges.bits, 20_000)
        } else {
            0
        }
        InvObj(type, count, vars)
    }
}
