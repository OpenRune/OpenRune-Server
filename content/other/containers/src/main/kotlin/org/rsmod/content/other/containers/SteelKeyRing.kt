package org.rsmod.content.other.containers

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.inv.storage.*
import org.rsmod.game.entity.Player

internal object SteelKeyRing {
    const val ITEM = "obj.favour_key_ring"
    val CONTENTS = AttributeKey<Int>(persistenceKey = "steel_keyring_keys")
    val SELECTED = AttributeKey<Int>()
    val keys = listOf(
        "obj.elid_key" to "Access to the Spirits of the Elid cave.",
        "obj.elemental_workshop_key" to "Access to the Elemental Workshop.",
        "obj.zqbonekey" to "Access to Rashiliyia's tomb.",
        "obj.edgevilledungeonkey" to "Access to the Edgeville Dungeon entrance.",
        "obj.hauntedmine_reward_key" to "Access to the crystal mine.",
        "obj.dusty_key" to "Access to the deeper Taverley Dungeon.",
        "obj.makinghistory_key" to "A mysterious key that reacts to hidden treasure.",
        "obj.melzarkey" to "Access to Melzar's Maze.",
        "obj.metal_key" to "Access to the Desert Mining Camp.",
        "obj.mourning_excavation_key" to "Access to the Mourner Tunnels.",
        "obj.ikov_shinykey" to "Access to the shed behind McGrubor's Wood.",
        "obj.phoenixkey2" to "Access to the Phoenix Gang weapon store.",
        "obj.thgoodminekey" to "Access to the Desert Mining Camp mine.",
    )
    fun owned(p: Player) = p.inv.physicalCount(ITEM) > 0
    fun backed(p: Player) = owned(p) || p.invMap["inv.bank"]?.physicalCount(ITEM)?.let { it > 0 } == true
    fun mask(p: Player) = (p.attr[CONTENTS] ?: 0) and 8191
    fun has(p: Player, index: Int) = mask(p) and (1 shl index) != 0
    fun set(p: Player, index: Int, present: Boolean) { val bit = 1 shl index; p.attr[CONTENTS] = if (present) mask(p) or bit else mask(p) and bit.inv() }
    fun stored(p: Player) = keys.indices.filter { has(p, it) }
}

/** Existing reusable-key checks see carried key-ring contents without materialising inventory items. */
internal class SteelKeyRingStorageHook @Inject constructor() : PlayerItemStorageHook {
    override fun shouldProcess(ctx: PlayerItemStorageContext) = ctx.inventory === ctx.player.inv && SteelKeyRing.owned(ctx.player) && SteelKeyRing.keys.any { it.first == ctx.itemInternal }
    override fun contains(ctx: PlayerItemStorageContext) = if (SteelKeyRing.has(ctx.player, SteelKeyRing.keys.indexOfFirst { it.first == ctx.itemInternal })) 1 else 0
    override fun add(ctx: PlayerItemStorageContext, amount: Int) = 0
    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int {
        val index = SteelKeyRing.keys.indexOfFirst { it.first == ctx.itemInternal }
        if (amount <= 0 || !SteelKeyRing.has(ctx.player, index)) return 0
        SteelKeyRing.set(ctx.player, index, false); return 1
    }
}
