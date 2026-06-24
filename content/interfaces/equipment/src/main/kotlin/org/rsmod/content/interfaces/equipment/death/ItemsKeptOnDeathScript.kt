package org.rsmod.content.interfaces.equipment.death

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import java.util.Objects
import kotlin.math.abs
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.death.PlayerDeathHandlingResolver
import org.rsmod.api.death.PlayerDeathPreviewContext
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.isInCombat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ItemsKeptOnDeathScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val marketPrices: MarketPrices,
    private val handlingResolver: PlayerDeathHandlingResolver,
    private val deathDrops: PlayerDeathDrops,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    private var Player.inInstance by boolVarBit("varbit.player_in_instance")

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.wornitems:deathkeep") { player.selectKeptOnDeath() }
        onIfClose("interface.deathkeep") { player.closeKeptOnDeath() }
    }

    private fun Player.selectKeptOnDeath() {
        if (isInCombat()) {
            mes("You cannot view items kept on death while in combat.")
            return
        }
        if (isAccessProtected) {
            mes("Please finish what you're doing before opening this menu.")
            soundSynth("synth.pillory_wrong")
            return
        }
        protectedAccess.launch(this) {
            stopAction()
            showKeptOnDeath()
        }
    }

    private suspend fun ProtectedAccess.showKeptOnDeath() {
        val deathSettings = DeathSettings()
        val deathInventory = createDeathInventory(deathSettings)
        updateDeathInventory(deathInventory)
        openDeathInventory(deathInventory, deathSettings)
        initChangeListener(deathSettings)
    }

    private fun ProtectedAccess.updateDeathInventory(deathInventory: DeathInventory) {
        invTransmit(deathInventory.lost)
        invTransmit(deathInventory.data)
    }

    private fun ProtectedAccess.openDeathInventory(
        deathInventory: DeathInventory,
        deathSettings: DeathSettings,
    ) {
        ifOpenMainModal("interface.deathkeep")
        deathKeepInit(deathInventory, deathSettings)
        ifSetEvents("component.deathkeep:right", 0..3, IfEvent.PauseButton)
        updateDeathRisk(deathInventory)
    }

    private fun ProtectedAccess.updateDeathRisk(deathInventory: DeathInventory) {
        ifSetText(
            "component.deathkeep:value",
            "Guide risk value:<br><col=ffffff>" +
                "${deathInventory.calculateRisk().formatAmount}</col>",
        )
    }

    private suspend fun ProtectedAccess.initChangeListener(settings: DeathSettings) {
        val update = pauseButton()

        ifClose()

        if (update.component != "component.deathkeep:right") {
            return
        }

        val updatedSettings =
            when (val sub = update.subcomponent) {
                0 -> settings.copy(protectItemPrayer = !settings.protectItemPrayer)
                1 -> settings.copy(skullActive = !settings.skullActive)
                2 -> settings.copy(playerKill = !settings.playerKill)
                3 -> settings.copy(wildernessLvl = abs(settings.wildernessLvl - 21))
                else -> throw IllegalStateException("Invalid sub component: $sub")
            }

        val updatedInventory = createDeathInventory(updatedSettings)
        updateDeathInventory(updatedInventory)
        openDeathInventory(updatedInventory, updatedSettings)

        initChangeListener(updatedSettings)
    }

    private fun Player.closeKeptOnDeath() {
        stopInvTransmit(itemsKeptOnDeath)
        stopInvTransmit(itemsKeptOnDeathData)
    }

    private fun ProtectedAccess.createDeathInventory(settings: DeathSettings): DeathInventory {
        val keptInventory = Inventory.create("inv.skill_guide_hunting_tracking")
        val lostInventory = Inventory.create("inv.deathkeep_items")
        val dataInventory = Inventory.create("inv.diango_hols_sack")

        check(keptInventory.size == 4) {
            "Size for `keptInventory` expected to be `4`. (size=${keptInventory.size})"
        }

        check(lostInventory.size == dataInventory.size) {
            "Unexpected size mismatch for inventories: lost=$lostInventory, data=$dataInventory"
        }

        check(inv.size + worn.size <= lostInventory.size) {
            "Death inventory can only fit `${lostInventory.size}` objs."
        }

        val carried = inv.filterNotNull() + worn.filterNotNull()
        val wildernessLevel = if (settings.wildernessLvl > 0) settings.wildernessLvl else 0
        val context =
            PlayerDeathPreviewContext.create(
                player = player,
                protectItem = settings.protectItemPrayer,
                skulled = settings.skullActive,
                playerKill = settings.playerKill,
                wildernessLevel = wildernessLevel,
                inInstance = player.inInstance,
                inRevenantCaves = areaChecker.inArea("area.revenant_caves", player.coords),
                gamemode = player.gamemode,
            )
        val handling = handlingResolver.resolve(context)

        val rules =
            PlayerDeathDrops.DeathDropRules(
                isUIM = context.isUIM,
                isPvpDeath = context.isPvpDeath,
            )
        val result = deathDrops.selectDrops(carried, rules, handling)

        val neverKept = carried.filter { deathDrops.isNeverKept(it, rules) }
        val lost =
            buildList {
                addAll(result.lostTradeable)
                if (handling.untradeableHandling != UntradeableHandling.KEEP) {
                    addAll(result.lostUntradeable)
                }
                addAll(neverKept)
            }

        val keptAddResult = invMoveAll(keptInventory, result.kept)
        val lostAddResult = invMoveAll(lostInventory, lost)

        check(result.kept.isEmpty() || keptAddResult.success) {
            "Could not add `inv` and `worn` into kept inventory. (result=$keptAddResult)"
        }

        check(lost.isEmpty() || lostAddResult.success) {
            "Could not add `inv` and `worn` into lost inventory. (result=$lostAddResult)"
        }

        for (i in dataInventory.indices) {
            dataInventory[i] = convertToDataObj(lostInventory[i])
        }

        return DeathInventory(keptInventory, lostInventory, dataInventory)
    }

    private fun convertToDataObj(obj: InvObj?): InvObj {
        if (obj == null) {
            return InvObj("obj.burntfish1")
        }
        return InvObj("obj.burntfish4", 1)
    }

    private fun marketPriceSingle(obj: InvObj?): Long {
        if (obj == null) {
            return 0
        }
        val type = getInvObj(obj)
        return (marketPrices[type] ?: type.cost).toLong()
    }

    private fun marketPriceTotal(obj: InvObj?): Long = marketPriceSingle(obj) * (obj?.count ?: 0)

    private fun DeathInventory.calculateRisk(): Long = lost.sumOf(::marketPriceTotal)

    private data class DeathInventory(
        val kept: Inventory,
        val lost: Inventory,
        val data: Inventory,
    )

    private data class DeathSettings(
        val header: String = "",
        val skullActive: Boolean = false,
        val protectItemPrayer: Boolean = false,
        val wildernessLvl: Int = 0,
        val playerKill: Boolean = false,
    )

    private fun ProtectedAccess.deathKeepInit(inventory: DeathInventory, settings: DeathSettings) {
        val keepCount = inventory.kept.count(Objects::nonNull)
        val keepObjs = inventory.kept.map { it?.id ?: -1 }
        runClientScript(
            972,
            if (settings.skullActive) 1 else 0,
            if (settings.protectItemPrayer) 1 else 0,
            settings.wildernessLvl,
            if (settings.playerKill) 1 else 0,
            settings.header,
            keepCount,
            keepObjs[0],
            keepObjs[1],
            keepObjs[2],
            keepObjs[3],
        )
    }
}

private val Player.itemsKeptOnDeath: Inventory
    get() = invMap.getValue("inv.deathkeep_items")

private val Player.itemsKeptOnDeathData: Inventory
    get() = invMap.getValue("inv.diango_hols_sack")
