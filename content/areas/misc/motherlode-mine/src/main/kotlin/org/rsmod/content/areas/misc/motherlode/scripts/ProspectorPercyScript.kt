package org.rsmod.content.areas.misc.motherlode.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InvScope
import dev.openrune.types.InvStackType
import dev.openrune.types.InventoryServerType
import dev.openrune.types.util.UncheckedType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.content.areas.misc.motherlode.MotherlodeMine
import org.rsmod.content.areas.misc.motherlode.hasLargerSack
import org.rsmod.content.areas.misc.motherlode.hasUpperHopper
import org.rsmod.content.areas.misc.motherlode.hasUpperLevel
import org.rsmod.content.areas.misc.motherlode.syncMotherlodeVars
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ProspectorPercyScript
@Inject
constructor(private val shops: Shops, private val shopOps: ShopOperationMap) : PluginScript() {
    private val rewards = createRewardsInventory()

    @OptIn(UncheckedType::class)
    private fun createRewardsInventory(): Inventory {
        val stock =
            checkNotNull(
                ServerCacheManager.getInventory(SHOP_STOCK.asRSCM(RSCMType.INV))
            ) {
                "Missing Motherlode shop stock: $SHOP_STOCK"
            }
        val template =
            checkNotNull(
                ServerCacheManager.getInventory(SHOP_TEMPLATE.asRSCM(RSCMType.INV))
            ) {
                "Missing shop display template: $SHOP_TEMPLATE"
            }
        require(stock.stock.size <= template.size) {
            "$SHOP_TEMPLATE only holds ${template.size} items."
        }
        val type =
            template.copy(
                scope = InvScope.Shared,
                stack = InvStackType.Always,
                stock = stock.stock,
                flags =
                    InventoryServerType.pack(
                        protect = false,
                        allStock = false,
                        restock = true,
                        runWeight = false,
                        dummyInv = false,
                        placeholders = false,
                    ),
            )
        val objs = arrayOfNulls<InvObj>(type.size)
        stock.stock.forEachIndexed { slot, item -> objs[slot] = InvObj(item.obj, item.count) }
        return Inventory(type, objs)
    }

    override fun ScriptContext.startup() {
        shopOps.costOf(CURRENCY) { it.cost.coerceAtLeast(1) }
        onOpNpc1(PERCY) { talk(it.npc) }
        onOpNpc3(PERCY) { player.openNuggetShop() }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { greeting() }

    private suspend fun Dialogue.greeting() {
        chatNpc(neutral, "Git yer pick an' dig! What d'ye want?")
        val choice =
            choice3(
                "I'd like to trade.",
                1,
                "What can I unlock down here?",
                2,
                "Nothing, thanks.",
                3,
            )
        when (choice) {
            1 -> player.openNuggetShop()
            2 -> unlocks()
            3 -> chatPlayer(neutral, "Nothing, thanks.")
        }
    }

    private suspend fun Dialogue.unlocks() {
        chatNpc(neutral, "Pay me in golden nuggets an' I'll open up more o' me mine for ye.")
        val choice =
            choice4(
                "Access to the upper level ($UPPER_LEVEL_COST nuggets)",
                Upgrade.UpperLevel,
                "Use of the upper hopper ($UPPER_HOPPER_COST nuggets)",
                Upgrade.UpperHopper,
                "A larger pay-dirt sack ($LARGER_SACK_COST nuggets)",
                Upgrade.LargerSack,
                "Never mind.",
                null,
            ) ?: return
        purchase(choice)
    }

    private suspend fun Dialogue.purchase(upgrade: Upgrade) {
        if (upgrade.unlocked(player)) {
            chatNpc(neutral, "Ye've already paid fer that one.")
            return
        }
        if (upgrade == Upgrade.UpperLevel && player.baseMiningLvl < MotherlodeMine.UPPER_LEVEL_MINING_LEVEL) {
            chatNpc(
                neutral,
                "Ye need a Mining level of ${MotherlodeMine.UPPER_LEVEL_MINING_LEVEL} before " +
                    "I'll let ye up that ladder.",
            )
            return
        }
        if (player.inv.count(MotherlodeMine.NUGGET) < upgrade.cost) {
            chatNpc(neutral, "Come back when ye've got ${upgrade.cost} golden nuggets.")
            return
        }
        if (access.invDel(player.inv, MotherlodeMine.NUGGET, upgrade.cost).failure) {
            return
        }
        upgrade.unlock(player)
        player.syncMotherlodeVars()
        chatNpc(happy, upgrade.thanks)
    }

    private fun Player.openNuggetShop() {
        shops.open(
            player = this,
            title = "Prospector Percy's Nugget Shop",
            shopInv = rewards,
            sideInv = inv,
            buyPercentage = 80.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
            currency = CURRENCY,
            refundable = SHOP_REFUNDABLE,
            refusalMessages = SHOP_REFUSAL_MESSAGES,
        )
    }

    private enum class Upgrade(
        val cost: Int,
        val thanks: String,
        val unlocked: (Player) -> Boolean,
        val unlock: (Player) -> Unit,
    ) {
        UpperLevel(
            UPPER_LEVEL_COST,
            "Pleasure doin' business. Ye can use the ladder now.",
            { it.hasUpperLevel },
            { it.hasUpperLevel = true },
        ),
        UpperHopper(
            UPPER_HOPPER_COST,
            "The hopper upstairs is all yours.",
            { it.hasUpperHopper },
            { it.hasUpperHopper = true },
        ),
        LargerSack(
            LARGER_SACK_COST,
            "I've stitched a bigger sack fer yer ore.",
            { it.hasLargerSack },
            { it.hasLargerSack = true },
        ),
    }

    private companion object {
        const val PERCY = "npc.motherlode_percy"
        const val SHOP_STOCK = "inv.motherlode_nugget_shop"
        const val SHOP_TEMPLATE = "inv.gemshop"
        const val CURRENCY = "currency.golden_nugget"

        const val UPPER_LEVEL_COST = 100
        const val UPPER_HOPPER_COST = 50
        const val LARGER_SACK_COST = 200

        val SHOP_REFUNDABLE =
            setOf(
                "obj.motherlode_reward_hat",
                "obj.motherlode_reward_top",
                "obj.motherlode_reward_legs",
                "obj.motherlode_reward_boots",
                "obj.coal_bag",
                "obj.gem_bag",
                "obj.pack_softclay",
                "obj.reward_gem_bag",
            )

        val SHOP_REFUSAL_MESSAGES =
            mapOf(
                "obj.fossil_motherlode_reward_hat" to REFUND_REFUSED,
                "obj.fossil_motherlode_reward_top" to REFUND_REFUSED,
                "obj.fossil_motherlode_reward_legs" to REFUND_REFUSED,
                "obj.fossil_motherlode_reward_boots" to REFUND_REFUSED,
            )

        private const val REFUND_REFUSED =
            "You can't sell this item back to Percy as you did not buy it from this shop"
    }
}
