package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.enums.GiantsFoundryEnums
import org.rsmod.api.player.dialogue.mesanims
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RewardShopScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc5(KOVAC) { openRewardShop() }
        onIfModalButton(LIST) { listOp(it) }
        onIfModalButton(BUY_BUTTON) { buy(player.shopSelected, 1) }
        onIfModalButton(DROPDOWN) { filter(it) }
        onOpNpcU(KOVAC) { sellBack(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.sellBack(npc: Npc, obj: ItemServerType) {
        when (obj.internalName) {
            GROG -> {
                chatNpc(npc, mesanims.neutral, "No, you keep that or give it to someone else.")
                if (player.isAnyIronman) {
                    chatPlayer(mesanims.neutral, "Can't you make an exception for me? I'm an Ironman by the way.")
                    chatNpc(npc, mesanims.neutral, "You get treated the same anyway.")
                    chatPlayer(mesanims.neutral, "...")
                }
                return
            }
            COLOSSAL_BLADE -> {
                chatNpc(npc, mesanims.neutral, "Kovac does not want that back. Maybe the smith clothes would be good, if you got any.")
                return
            }
        }
        val cost = shopCosts()[obj.id]
        if (cost == null) {
            chatNpc(npc, mesanims.neutral, "You keep that.")
            return
        }
        val value = cost * SELL_PERCENT / 100
        objbox(obj.internalName, "${obj.name}: Kovac will give you ${"%,d".format(value)} Foundry Reputation for this.")
        if (!choice2("Accept", true, "Decline", false)) {
            return
        }
        if (inv.count(obj.internalName) <= 0) {
            return
        }
        invDel(inv, obj.internalName, 1)
        player.foundryReputation += value
        objbox(obj.internalName, "${obj.name}: Kovac takes it back. Foundry Reputation: ${"%,d".format(player.foundryReputation)}")
    }

    private fun shopCosts(): Map<Int, Int> =
        shopItems.values.mapNotNull { structId ->
            val struct = structId?.let(ServerCacheManager::getStruct) ?: return@mapNotNull null
            val obj = struct.params?.get(param("param.giants_foundry_shop_obj")) as? Int ?: return@mapNotNull null
            obj to struct.getInt(param("param.giants_foundry_shop_cost"))
        }.toMap()

    /**
     * Shop rows are built by `giants_foundry_reward_shop_list_item_setup`, 13 components per entry
     * (hidden entries included), in enum order: the entry is `comsub / 13 + 1`.
     */
    private fun ProtectedAccess.listOp(event: IfModalButton) {
        val entry = event.comsub / ROW_CHILDREN + 1
        when (event.op) {
            IfButtonOp.Op1 -> player.shopSelected = entry
            IfButtonOp.Op2 -> buy(entry, 1)
            IfButtonOp.Op3 -> buy(entry, 5)
            IfButtonOp.Op4 -> buy(entry, 10)
            else -> {}
        }
    }

    private fun ProtectedAccess.buy(entry: Int, amount: Int) {
        val struct = entryStruct(entry) ?: return
        val cost = struct.getInt(param("param.giants_foundry_shop_cost"))
        val mouldStruct = struct.params?.get(param("param.giants_foundry_shop_mould")) as? Int
        if (mouldStruct != null) {
            unlockMould(mouldStruct, cost)
            return
        }
        val obj = struct.params?.get(param("param.giants_foundry_shop_obj")) as? Int ?: return
        val name = RSCM.getReverseMapping(RSCMType.OBJ, obj)
        if (name == DOUBLE_AMMO_MOULD && !QuestRequirements.hasCompleted(player, DWARF_CANNON)) {
            mes("You need to complete Dwarf Cannon to buy that.")
            return
        }
        val affordable = minOf(amount, player.foundryReputation / cost)
        if (affordable <= 0) {
            mes("You don't have enough Foundry Reputation for that.")
            return
        }
        val added = invAdd(inv, name, affordable, strict = false).completed()
        if (added <= 0) {
            mes("You don't have enough inventory space.")
            return
        }
        player.foundryReputation -= added * cost
        mes("Kovac hands you your reward.")
        soundSynth(PURCHASE_SYNTH)
    }

    private fun ProtectedAccess.refresh() {
        player.runClientScript(SHOP_SETUP.asRSCM(RSCMType.CLIENTSCRIPT), 1)
    }

    /** The dropdown's options are every other component, starting at 1. */
    private fun ProtectedAccess.filter(event: IfModalButton) {
        player.shopFilter = (event.comsub - 1) / 2
        player.shopSelected = 0
    }

    private fun ProtectedAccess.unlockMould(structId: Int, cost: Int) {
        val mould =
            FoundryData.moulds.values.flatten().firstOrNull { it.struct == structId } ?: return
        val unlock = FoundryData.unlockVarbit(mould) ?: return
        if (player.foundryVar(unlock) != 0) {
            mes("You have already unlocked that mould.")
            return
        }
        if (player.foundryReputation < cost) {
            mes("You don't have enough Foundry Reputation for that.")
            return
        }
        player.setFoundryVar(unlock, 1)
        player.shopSelected = 0
        player.foundryReputation -= cost
        mes(
            "Kovac hands you a mould, which you add to the library of moulds you already have " +
                "stored here."
        )
        soundSynth(PURCHASE_SYNTH)
        refresh()
    }

    private fun entryStruct(entry: Int) = shopItems[entry]?.let(ServerCacheManager::getStruct)

    private fun param(name: String): Int = name.asRSCM(RSCMType.PARAM)

    private companion object {
        const val LIST = "component.giants_foundry_reward_shop:list"
        const val DROPDOWN = "component.giants_foundry_reward_shop:dropdown_content"
        const val PURCHASE_SYNTH = "synth.coins_jingle_1"
        const val BUY_BUTTON = "component.giants_foundry_reward_shop:buy_button"
        const val DOUBLE_AMMO_MOULD = "obj.double_ammo_mould"
        const val DWARF_CANNON = "quest_dwarfcannon"
        const val GROG = "obj.kovacs_grog"
        const val COLOSSAL_BLADE = "obj.giants_foundry_colossal_blade"
        const val SELL_PERCENT = 80
    }
}

private var Player.shopSelected by intVarp("varp.giants_foundry_reward_shop_selected_item")
private var Player.shopFilter by intVarBit("varbit.giants_foundry_reward_shop_filter")

private val shopItems
    get() = GiantsFoundryEnums.giants_foundry_reward_shop_items

private const val SHOP_INTERFACE = "interface.giants_foundry_reward_shop"
private const val SHOP_INIT = "clientscript.[clientscript,reward_shop_init]"
private const val SHOP_SETUP = "clientscript.[clientscript,giants_foundry_reward_shop_setup]"
private const val ROW_CHILDREN = 13
private const val DROPDOWN_END = 12
private const val MODAL_TRANSPARENCY = -3

// TODO(content): [system:bank_pin] the Bank PIN check before the shop opens.
internal fun ProtectedAccess.openRewardShop() {
    ifOpenMainModal(SHOP_INTERFACE, transparency = MODAL_TRANSPARENCY)
    player.runClientScript(SHOP_INIT.asRSCM(RSCMType.CLIENTSCRIPT))
    ifSetEvents(
        "component.giants_foundry_reward_shop:list",
        0..shopItems.backing.size * ROW_CHILDREN,
        IfEvent.Op1,
        IfEvent.Op2,
        IfEvent.Op3,
        IfEvent.Op4,
    )
    ifSetEvents(
        "component.giants_foundry_reward_shop:dropdown_content",
        0..DROPDOWN_END,
        IfEvent.Op1,
    )
}
