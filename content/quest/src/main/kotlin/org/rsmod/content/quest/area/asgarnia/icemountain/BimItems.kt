package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class BimItems
@Inject
constructor(
    private val quest: BelowIceMountain,
    private val guardian: BimGuardian,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU("obj.knife", "obj.bread") { makeSteakSandwich() }
        onOpLoc1("loc.bim_willows_bag") { lootWillowBag(quest) { guardian.onBagLooted(this, quest) } }
        onOpLoc1("loc.bim_willows_bag_cutscene") {
            lootWillowBag(quest) { guardian.onBagLooted(this, quest) }
        }
    }

    private suspend fun ProtectedAccess.makeSteakSandwich() {
        if (player.bimMarley < BimMarley.RECIPE_KNOWN) {
            mes("You don't know how to make a steak sandwich.")
            return
        }
        if (inv.count("obj.cooked_meat") < 1) {
            mes("You need some cooked meat to make a steak sandwich.")
            return
        }
        if (!player.craftSteakSandwich()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim("seq.bim_sandwich_make")
        mes("You combine the cooked meat and bread to make a Steak sandwich!")
    }
}

internal fun Player.craftSteakSandwich(): Boolean {
    val transaction =
        invTransaction(inv, autoCommit = false) {
            val inventory = select(inv)
            delete(inventory, "obj.bread".asRSCM(RSCMType.OBJ), 1)
            delete(inventory, "obj.cooked_meat".asRSCM(RSCMType.OBJ), 1)
            add(inventory, "obj.bim_steak_sandwich".asRSCM(RSCMType.OBJ), 1)
        }
    if (transaction.failure) {
        return false
    }
    transaction.commitAll()
    return true
}

internal suspend fun ProtectedAccess.lootWillowBag(
    quest: BelowIceMountain,
    onLooted: suspend ProtectedAccess.() -> Unit = {},
) {
    if (quest.stage(player) < BimStage.GUARDIAN_DEAD) {
        mes("You don't want to touch Willow's bag while she's still around.")
        return
    }
    if (player.bimClaimedReward != 0) {
        mes("There is nothing left in the bag.")
        return
    }

    mesbox("You rummage through Willow's bag...")
    objbox("obj.coins", "You find a pile of coins!")
    if (!player.lootBagCoins()) {
        mes("You don't have enough inventory space.")
        return
    }
    player.bimClaimedReward = 1
    startDialogue { chatPlayer(happy, "Those will do nicely!") }
    onLooted()
}

internal fun Player.lootBagCoins(): Boolean {
    val transaction =
        invTransaction(inv, autoCommit = false) {
            add(select(inv), "obj.coins".asRSCM(RSCMType.OBJ), BAG_COINS)
        }
    if (transaction.failure) {
        return false
    }
    transaction.commitAll()
    return true
}

private const val BAG_COINS = 2_000
