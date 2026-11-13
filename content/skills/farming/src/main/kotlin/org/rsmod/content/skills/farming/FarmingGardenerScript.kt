package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val NO_CONFIRM = "varbit.settings_warning_confirmation_farming_protection"

class FarmingGardenerScript
@Inject
constructor(private val gardeners: FarmingGardeners, private val farming: FarmingService, private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((npc, patches) in gardeners.patches) {
            onOpNpc1(npc) { startDialogue(it.npc) { menu(it.npc, patches) } }
            onOpNpc3(npc) { startDialogue(it.npc) { pay(it.npc, patches.filter { patch -> patch.payOp == 3 }, talk = false) } }
            if (patches.any { it.payOp == 4 }) {
                onOpNpc4(npc) { startDialogue(it.npc) { pay(it.npc, patches.filter { patch -> patch.payOp == 4 }, talk = false) } }
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
            1 -> {
                chatPlayer(quiz, "Would you look after my crops for me?")
                pay(npc, patches, talk = true)
            }
            2 -> {
                val tip = choice4("Tree seeds", 0, "Vegetable sacks", 1, "Fruit baskets", 2, "Plant pots", 3)
                chatNpc(neutral, FarmingServicesData.row.advice[tip])
            }
            3 -> access.openShop(npc)
        }
    }

    private suspend fun Dialogue.pay(npc: Npc, patches: List<GardenerPatch>, talk: Boolean) {
        if (patches.isEmpty()) return
        val patch = access.choosePatch(npc, patches)
        val expected = farming.state(player, patch)
        val crop = expected.crop
        if (crop == null && patch.kind == PatchKind.TREE) {
            chatNpc(neutral, "You don't have a sapling planted in that patch. Plant one and I might agree to look after it for you.")
            return
        }
        if (crop != null && expected.health == Health.HEALTHY && expected.grown(crop) && gardeners.removalPrice(patch) != null) {
            access.removeTree(npc, patch, expected)
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
            chatNpcNoAnim(refusal)
            return
        }
        val payment = gardeners.payment(checkNotNull(crop))
        if (payment.isEmpty()) {
            access.mes("This crop has no gardener protection service.")
            return
        }
        // UNVERIFIED: wording for crops without payment_text (guess)
        val cost = crop.paymentText ?: payment.joinToString(" and ") { (obj, count) ->
            "$count ${checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).name}"
        }
        if (talk) {
            chatNpc(neutral, "If you like, but I want $cost for that.")
            if (!gardeners.hasPayment(player, payment)) {
                cannotPay()
                return
            }
        }
        if (talk || vars[NO_CONFIRM] == 0) {
            // TODO(content): [verify:in_game] whether Talk still offers these once the setting is on
            val answer = if (talk) {
                choice3("Okay, it's a deal.", 1, "No, that's too much.", 2, "Okay, it's a deal and don't ask again.", 3)
            } else choice3("Yes.", 1, "No.", 2, "Yes, and don't ask again.", 3, title = "Pay $cost?")
            if (answer == 2) return
            if (answer == 3) vars[NO_CONFIRM] = 1
            // TODO(content): [verify:in_game] the player line after "Okay, it's a deal and don't ask again."
            if (talk) chatPlayer(neutral, if (answer == 3) "Okay, it's a deal and don't ask again." else "Okay, it's a deal.")
        }
        when (gardeners.protect(player, patch, expected)) {
            ProtectResult.PROTECTED -> {
                player.spam("You pay the gardener $cost to protect the patch.")
                val address = when {
                    npc.type.id == FarmingServicesData.row.iknamiGardener.id -> "iknami"
                    access.isBodyTypeA() -> "sir"
                    else -> "madam"
                }
                // TODO: Ehecatl and Tziuhtla are uncaptured; they may also say "iknami".
                chatNpc(neutral, "That'll do nicely, $address. Leave it with me - I'll make sure that patch grows for you.")
            }
            ProtectResult.UNPAID -> {
                // TODO(content): [verify:in_game] the Pay op's reply without the items
                cannotPay()
            }
            ProtectResult.ALREADY -> chatNpcNoAnim("I'm already looking after that patch for you.")
            ProtectResult.STALE -> access.mes("The patch has changed. Please ask again.")
            else -> access.mes("The patch can no longer be protected.")
        }
    }

    // TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
    private suspend fun Dialogue.cannotPay() {
        chatPlayer(neutral, "I'm afraid I don't have any of those at the moment.")
        chatNpc(neutral, "Well, I'm not wasting my time for free.")
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
