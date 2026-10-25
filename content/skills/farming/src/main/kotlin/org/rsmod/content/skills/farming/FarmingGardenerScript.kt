package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingGardenerScript
@Inject
constructor(private val gardeners: FarmingGardeners, private val farming: FarmingService, private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((npc, patches) in gardeners.patches) {
            onOpNpc1(npc) { startDialogue(it.npc) { menu(it.npc, patches) } }
            onOpNpc3(npc) { pay(it.npc, patches.filter { patch -> patch.payOp == 3 }, confirm = false) }
            if (patches.any { it.payOp == 4 }) {
                onOpNpc4(npc) { pay(it.npc, patches.filter { patch -> patch.payOp == 4 }, confirm = false) }
            }
            val type = FarmingServicesData.row.gardeners.first { it.t0.internalName == npc }.t0
            if (type.actions.getOpOrNull(4) == "Trade") onOpNpc5(npc) { openShop(it.npc) }
        }
    }

    private suspend fun Dialogue.menu(npc: Npc, patches: List<GardenerPatch>) {
        val option = if (npc.type.actions.getOpOrNull(4) == "Trade") {
            choice4(
                "Would you look after my crops for me?", 1,
                "Can you give me any farming advice?", 2,
                "Can you sell me something?", 3,
                "I'll come back another time.", 4,
            )
        } else choice3(
            "Would you look after my crops for me?", 1,
            "Can you give me any farming advice?", 2,
            "I'll come back another time.", 4,
        )
        when (option) {
            1 -> access.pay(npc, patches, confirm = true)
            2 -> {
                val tip = choice4("Tree seeds", 0, "Vegetable sacks", 1, "Fruit baskets", 2, "Plant pots", 3)
                chatNpc(neutral, FarmingServicesData.row.advice[tip])
            }
            3 -> access.openShop(npc)
        }
    }

    private suspend fun ProtectedAccess.pay(npc: Npc, patches: List<GardenerPatch>, confirm: Boolean) {
        if (patches.isEmpty()) return
        val patch = choosePatch(npc, patches)
        val expected = farming.state(player, patch)
        val crop = expected.crop
        if (crop != null && expected.health == Health.HEALTHY && expected.grown(crop) && gardeners.removalPrice(patch) != null) {
            removeTree(npc, patch, expected)
            return
        }
        val refusal = when {
            crop == null -> "You don't have any seeds planted in that patch. Plant some and I might agree to look after it for you."
            expected.health == Health.DEAD -> "That patch is dead - it's too late for me to do anything about it now."
            expected.health == Health.DISEASED -> "That patch is diseased - you should cure it before asking me to be responsible for it!"
            expected.grown(crop) -> "That patch is already fully grown! I don't know what you want me to do with it!"
            expected.paid -> "I'm already looking after that patch for you."
            else -> null
        }
        if (refusal != null) {
            chatNpcNoAnim(npc, refusal)
            return
        }
        val payment = gardeners.payment(checkNotNull(crop))
        if (payment.isEmpty()) {
            mes("This crop has no gardener protection service.")
            return
        }
        if (confirm) {
            val cost = payment.joinToString(" and ") { (obj, count) ->
                "$count ${checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).name}"
            }
            chatNpcNoAnim(npc, "If you like, but I want $cost for that.")
            if (!choice2("Okay, it's a deal.", true, "No, that's too much.", false)) return
        }
        when (gardeners.protect(player, patch, expected)) {
            ProtectResult.PROTECTED -> chatNpcNoAnim(npc, "Leave it with me - I'll make sure that patch grows for you.")
            ProtectResult.UNPAID -> {
                val cost = payment.joinToString(" and ") { (obj, count) ->
                    "$count ${checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).name}"
                }
                chatNpcNoAnim(npc, "I want $cost for that.")
            }
            ProtectResult.ALREADY -> chatNpcNoAnim(npc, "I'm already looking after that patch for you.")
            ProtectResult.STALE -> mes("The patch has changed. Please ask again.")
            else -> mes("The patch can no longer be protected.")
        }
    }

    private suspend fun ProtectedAccess.choosePatch(npc: Npc, patches: List<GardenerPatch>): PatchDef {
        fun label(index: Int): String {
            val entry = patches[index]
            val native = npc.type.actions.getOpOrNull(entry.payOp - 1).orEmpty()
            return if (native.startsWith("Pay (") && patches.count { it.payOp == entry.payOp } == 1) {
                native.removePrefix("Pay ")
            } else {
                val kind = entry.patch.kind
                val label = if (kind == PatchKind.ALLOTMENT) "allotment"
                else kind.name.lowercase().replace('_', ' ') + " patch"
                "$label ${index + 1}"
            }
        }
        val selected = when (patches.size) {
            1 -> 0
            2 -> choice2(label(0), 0, label(1), 1)
            3 -> choice3(label(0), 0, label(1), 1, label(2), 2)
            4 -> choice4(label(0), 0, label(1), 1, label(2), 2, label(3), 3)
            else -> error("Too many patches for gardener: ${npc.name}")
        }
        return patches[selected].patch
    }

    private suspend fun ProtectedAccess.removeTree(npc: Npc, patch: PatchDef, expected: PatchState) {
        if (expected.phase == Phase.GROWING) {
            mes("You need to check the health of your tree first.")
            return
        }
        val price = gardeners.removalPrice(patch) ?: return
        if (!choice2("Yes.", true, "No.", false, "Pay $price coins to have your tree chopped down?")) return
        if (gardeners.removeTree(player, patch, expected)) {
            mes("${npc.name} sets to work...")
        } else if (farming.state(player, patch) != expected) {
            mes("The patch has changed. Please ask again.")
        } else mes("You don't have enough coins.")
    }

    private fun ProtectedAccess.openShop(npc: Npc) {
        if (npc.type.actions.getOpOrNull(4) != "Trade") return
        shops.open(
            player = player,
            title = "Farming Supplies",
            shopInv = "inv.farmer_supplies",
            buyPercentage = 0.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
        )
    }
}
