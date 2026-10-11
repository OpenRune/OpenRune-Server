package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.other.pets.Pets
import org.rsmod.content.other.pets.storesAnyObj
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class QuiverExchangeScript @Inject constructor(
    private val rewards: PetRewards,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcList: NpcList,
) : PluginScript() {
    private var minimus: Npc? = null
    override fun ScriptContext.startup() {
        onGameStartup {
            if (npcList.none { it.id == NPC.asRSCM() }) {
                minimus = Npc(NPC, LOCATION).also { npcs.add(it, Int.MAX_VALUE) }
            }
        }
        onOpNpc1(NPC) {
            val slot = inv.indexOfFirst { it?.id in UNCHARGED_IDS }
            if (slot >= 0) exchange(slot) else mes("Bring a spare uncharged Dizana's quiver to exchange it.")
        }
        for (symbol in org.rsmod.api.player.worn.DizanasQuiver.usable) {
            onOpNpcU(ServerCacheManager.getNpc(NPC.asRSCM())!!, ServerCacheManager.getItem(symbol.asRSCM())!!) {
                if (it.objType.id in UNCHARGED_IDS) exchange(it.invSlot)
                else mes("Minimus only accepts uncharged quivers. Uncharge it first; blessed quivers cannot be exchanged.")
            }
        }
    }
    override fun ScriptContext.shutdown() {
        minimus?.takeIf { it.isSlotAssigned }?.let { npcs.del(it, Int.MAX_VALUE) }
        minimus = null
    }
    private suspend fun ProtectedAccess.exchange(slot: Int) {
        if (!AmmoContainers.active(player)) return
        val original = inv[slot]?.takeIf { it.id in UNCHARGED_IDS } ?: return
        val origin = coords
        val owned = ownsPet(player)
        val choices = listOf("Trade for 4,000 sunfire splinters") + (if (owned) emptyList() else listOf("Gamble for Smol Heredit (1/200)")) + "Keep my quiver"
        val selected = menu("Exchange Dizana's quiver", hotkeys = true, choices = choices)
        val gamble = !owned && selected == 1
        if (selected != 0 && !gamble) return
        if (!choice2("Exchange my quiver", true, "Keep my quiver", false, title = if (gamble) "Lose this quiver for a 1/200 pet chance?" else "Exchange this quiver for 4,000 sunfire splinters?")) return
        if (!AmmoContainers.active(player) || coords != origin || inv[slot] !== original || gamble && ownsPet(player)) return
        val result = player.invTransaction(inv) {
            val inventory = select(inv)
            delete(inventory, original.id, 1, slot = slot)
            if (!gamble) add(inventory, DizanasQuiverScript.SPLINTER.asRSCM(), 4000)
        }
        if (result.failure) { mes("You need more inventory space to exchange your quiver."); return }
        if (!gamble) mes("You exchange your quiver for 4,000 sunfire splinters.")
        else if (this@QuiverExchangeScript.random.of(200) == 0) rewards.give(player, PET, source = "Fortis Colosseum")
        else mes("Smol Heredit doesn't want to follow you this time.")
    }
    private fun ownsPet(player: Player): Boolean {
        val pet = checkNotNull(Pets.forObj(PET)).first
        return player.storesAnyObj(pet.forms.map { it.obj }) || pet.forms.any { it.objId == player.vars["varp.follower_obj"] }
    }
    companion object {
        const val NPC = "npc.colosseum_master_outside"
        const val PET = "obj.solhereditpet"
        val LOCATION = CoordGrid(1805, 9508)
        private val UNCHARGED_IDS get() = listOf("obj.dizanas_quiver_uncharged", "obj.dizanas_quiver_uncharged_trouver").map { it.asRSCM() }
    }
}
