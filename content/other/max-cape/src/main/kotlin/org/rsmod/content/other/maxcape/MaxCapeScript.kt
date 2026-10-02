package org.rsmod.content.other.maxcape

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.params
import org.rsmod.api.enums.EquipmentEnums.equipment_stats_to_slots_map
import org.rsmod.api.enums.EquipmentEnums.equipment_tab_to_slots_map
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.content.other.consumables.potion.PotionEffectService
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.game.type.getOrNull
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MaxCapeScript @Inject constructor(
    private val spellbooks: MagicSpellbookManager,
    private val potions: PotionEffectService,
    private val teleportValidator: PlayerTeleportValidator,
    private val areas: AreaChecker,
    private val launcher: ProtectedAccessLauncher,
    private val random: GameRandom,
) : PluginScript(), PlayerPostTickHook {
    override fun ScriptContext.startup() {
        for (cape in MaxCapeOptions.capes) {
            onOpHeld1(cape) {
                val result = invEquip(it.slot, it.inventory)
                if (result is HeldEquipResult.Fail) result.messages.forEach(::mes)
            }
            onOpHeld2(cape) { choose(MaxCapeOptions.heldTeleports) }
            onOpHeld3(cape) { choose(if (cape == MaxCapeOptions.WORN) MaxCapeOptions.portals else MaxCapeOptions.spells) }
            onOpHeld4(cape) { choose(MaxCapeOptions.features) }
            onOpHeldSubOp(cape) {
                if (!it.inventory[it.slot].isType(it.type)) return@onOpHeldSubOp
                MaxCapeOptions.held(it.op.ordinal + 1, it.subop, cape == MaxCapeOptions.WORN)?.let { action -> perform(action) }
            }
        }
        onOpWorn2(MaxCapeOptions.WORN) { perform(CapeAction.Teleport(CapeDestination.Home)) }
        onOpWorn3(MaxCapeOptions.WORN) { perform(CapeAction.Teleport(CapeDestination.Crafting)) }
        onOpWorn4(MaxCapeOptions.WORN) { choose(MaxCapeOptions.guilds) }
        onOpWorn5(MaxCapeOptions.WORN) { choose(MaxCapeOptions.skilling) }
        onOpWorn6(MaxCapeOptions.WORN) { choose(MaxCapeOptions.portals) }
        onOpWorn7(MaxCapeOptions.WORN) { choose(MaxCapeOptions.spells) }
        onOpWorn8(checkNotNull(ServerCacheManager.getItem(MaxCapeOptions.WORN.asRSCM(RSCMType.OBJ)))) { choose(MaxCapeOptions.features) }
        val side = checkNotNull(equipment_tab_to_slots_map[Wearpos.Back.slot])
        onIfOverlaySubOpMenu(RSCM.getReverseMapping(RSCMType.COMPONENT, side.packed)) {
            wornSubOp(it.obj?.id, it.op, it.subop)
        }
        val main = checkNotNull(equipment_stats_to_slots_map[Wearpos.Back.slot])
        onIfModalSubOpMenu(RSCM.getReverseMapping(RSCMType.COMPONENT, main.packed)) {
            wornSubOp(it.obj?.id, it.op, it.subop, modal = true)
        }
        onPlayerHit {
            if (hit.damage > 0 && player.back.isType(MaxCapeOptions.WORN) && shouldEscape(player)) {
                player.capeEscapePending = 1
            }
        }
    }

    private suspend fun ProtectedAccess.wornSubOp(packetItem: Int?, op: IfButtonOp, subop: Int, modal: Boolean = false) {
        val protected = if (modal) player.isModalButtonProtected else player.isAccessProtected
        if (protected || !player.back.isType(MaxCapeOptions.WORN)) return
        if (packetItem != null && packetItem != player.back?.id) return
        val action = MaxCapeOptions.worn(op.ordinal + 1, subop) ?: return
        clearPendingAction()
        perform(action)
    }

    private suspend fun ProtectedAccess.choose(options: List<CapeAction>) {
        val pages = options.filter { it != CapeAction.Sailing }.chunked(3)
        var page = 0
        while (hasCape(player)) {
            val entries = pages[page]
            val labels = entries.map(::label) + (if (pages.size > 1) listOf("More...") else emptyList()) + "Cancel"
            val choice = menu("Max cape", false, labels) - 1
            if (choice in entries.indices) {
                perform(entries[choice])
                return
            }
            if (pages.size > 1 && choice == entries.size) page = (page + 1) % pages.size else return
        }
    }

    internal suspend fun ProtectedAccess.perform(action: CapeAction) {
        if (!hasCape(player) || player.hitpoints <= 0) return
        player.resetCapeDailyUses()
        when (action) {
            is CapeAction.Teleport -> travel(action.destination)
            is CapeAction.Spellbook -> changeBook(action.index)
            CapeAction.Check -> mes("You have ${5 - player.capeSpellbookUses} spellbook changes remaining today (UTC).")
            CapeAction.Search -> search()
            CapeAction.RingOfLife -> {
                player.capeLifeDisabled = !player.capeLifeDisabled
                mes("Your cape's Ring of Life effect is now ${if (player.capeLifeDisabled) "disabled" else "enabled"} while worn.")
            }
            CapeAction.Commune -> {
                player.capeJunkDisabled = !player.capeJunkDisabled
                player.capeNextCollection = mapClock + COLLECTION_TICKS
                mes("Your cape will ${if (player.capeJunkDisabled) "no longer" else "now"} collect miscellaneous metal items while worn.")
            }
            CapeAction.Stamina -> {
                if (player.capeStaminaUsed) {
                    mes("You have already used your cape's stamina boost today (UTC).")
                    return
                }
                potions.boostStamina(this, 100)
                player.runEnergy = 10_000
                player.capeStaminaUsed = true
                mes("Your run energy is restored. Your stamina boost lasts at least one minute.")
            }
            CapeAction.Sailing -> mes("Boat and Sailing options are not available on this server yet.")
        }
    }

    private fun ProtectedAccess.changeBook(index: Int) {
        val books = listOf(Spellbook.Standard, Spellbook.Ancients, Spellbook.Lunars, Spellbook.Arceuus)
        val book = books.getOrNull(index) ?: return
        if (spellbooks.activeSpellbook(player) == book) {
            mes("You are already using the ${label(CapeAction.Spellbook(index))} spellbook.")
            return
        }
        if (player.capeSpellbookUses >= 5) {
            mes("You have used all five spellbook changes today (UTC).")
            return
        }
        spellbooks.setSpellbook(player, book)
        player.capeSpellbookUses++
        mes("Spellbook changed to ${label(CapeAction.Spellbook(index))}. ${5 - player.capeSpellbookUses} changes remaining today.")
    }

    private fun ProtectedAccess.search() {
        if (player.capeSearchUses >= 3) {
            mes("You have already searched your cape three times today (UTC).")
            return
        }
        val result = player.invTransaction(inv) {
            val target = select(inv)
            for (symbol in listOf("obj.xbows_crossbow_bronze", "obj.xbows_grapple_tip_bolt_mithril_rope")) {
                add(target, symbol.asRSCM(RSCMType.OBJ), 1)
            }
        }
        if (!result.success) {
            mes("You need room for a bronze crossbow and mithril grapple.")
            return
        }
        player.capeSearchUses++
        mes("You find a bronze crossbow and mithril grapple. ${3 - player.capeSearchUses} searches remaining today.")
    }

    private suspend fun ProtectedAccess.travel(destination: CapeDestination) {
        if (destination.hunterCharge && player.capeHunterUses >= 5) {
            mes("You have used all five chinchompa teleports today (UTC).")
            return
        }
        if (destination == CapeDestination.BlackChins && menu("Teleport into the Wilderness?", "Yes, teleport.", "Cancel") != 1) return
        if (!hasCape(player)) return
        val denial = teleportValidator.validate(player, TeleportType.Standard, areas)
        if (denial != null) { mes(denial); return }
        val landing = mapFindSquareLineOfWalk(destination.coords, minRadius = 0, maxRadius = 3)
        if (landing == null) { mes("That destination is temporarily unavailable."); return }
        val before = coords
        clearPendingAction()
        telejump(landing)
        if (coords != landing || before == coords) return
        anim("seq.human_castteleport_reverse")
        if (destination.hunterCharge) player.capeHunterUses++
        if (destination == CapeDestination.Home) mes("Welcome home to Lumbridge. Player-owned houses are not available yet.")
    }

    override fun onPostTick(player: Player) {
        if (!player.back.isType(MaxCapeOptions.WORN) || player.hitpoints <= 0) {
            player.capeEscapePending = 0
            return
        }
        collect(player)
        if (player.capeEscapePending == 0) return
        if (!shouldEscape(player) || teleportValidator.validate(player, TeleportType.MemberLevel30, areas) != null) {
            player.capeEscapePending = 0
            return
        }
        val launched = launcher.launch(player) {
            if (!shouldEscape(player) || !player.back.isType(MaxCapeOptions.WORN)) return@launch
            clearPendingAction()
            telejump(CapeDestination.Home.coords, TeleportType.MemberLevel30)
            if (coords == CapeDestination.Home.coords) mes("Your max cape's Ring of Life effect brings you to safety.")
        }
        if (launched) player.capeEscapePending = 0
    }

    private fun collect(player: Player) {
        if (player.capeNextCollection == 0) player.capeNextCollection = player.currentMapClock + COLLECTION_TICKS
        if (player.currentMapClock < player.capeNextCollection) return
        player.capeNextCollection = player.currentMapClock + COLLECTION_TICKS
        if (player.capeJunkDisabled || player.isAccessProtected) return
        if (getOrNull(player.torso)?.param(params.metallic_interference) == true) return
        val roll = random.of(2000)
        val item = when (roll) {
            in 0..1974 -> "obj.steel_arrow"
            in 1975..1979 -> "obj.steel_dart"
            in 1980..1984 -> "obj.steel_knife"
            in 1985..1989 -> "obj.iron_ore"
            in 1990..1994 -> "obj.nails"
            else -> "obj.steel_bar"
        }
        player.invAdd(player.inv, item)
    }

    internal fun shouldEscape(player: Player): Boolean = !player.capeLifeDisabled &&
        player.hitpoints > 0 && player.hitpoints * 10 <= player.baseHitpointsLvl

    private fun hasCape(player: Player): Boolean = MaxCapeOptions.capes.any { it in player.inv || it in player.worn }

    private fun label(action: CapeAction): String = when (action) {
        is CapeAction.Teleport -> action.destination.label
        is CapeAction.Spellbook -> listOf("Standard", "Ancient", "Lunar", "Arceuus")[action.index]
        CapeAction.Check -> "Check remaining spellbook changes"
        CapeAction.Search -> "Search"
        CapeAction.RingOfLife -> "Ring of Life"
        CapeAction.Commune -> "Commune"
        CapeAction.Stamina -> "Stamina Boost"
        CapeAction.Sailing -> "Sailing (unavailable)"
    }

    private companion object {
        const val COLLECTION_TICKS = 100
    }
}
