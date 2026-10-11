package org.rsmod.content.travel.shortcuts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import java.time.Instant
import kotlin.math.atan2
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.*
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal class ShortcutScript @Inject constructor(private val collision: CollisionFlagMap, private val xp: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        val bindings = ShortcutRoutes.all.filter { it.loc !in ShortcutRoutes.external }.flatMap { r ->
            val base = checkNotNull(ServerCacheManager.getObject(r.loc.asRSCM())) { r.loc }
            val variants = if (base.multiLoc.isNotEmpty()) base.multiLoc.asSequence().mapNotNull { ServerCacheManager.getObject(it) }.toList() else listOf(base)
            val found = variants.flatMap { type -> (1..5).filter { type.id !in setOf(5160, 57710) && type.actions.getOpOrNull(it - 1).equals(r.option, true) }.map { (type.id to it) to r } }
            check(found.isNotEmpty()) { "Missing native shortcut option ${r.loc}: ${r.option}" }
            found
        }.groupBy({ it.first }, { it.second })
        for ((key, routes) in bindings) {
            val symbol = dev.openrune.rscm.RSCM.getReverseMapping(dev.openrune.rscm.RSCMType.LOC, key.first)
            if (routes.any { it.loc == "loc.vampyrium_stepping_stone_shortcut_pillar" }) {
                onApLoc1(symbol) { if (isWithinApRange(it.loc, 4)) cross(it.loc, routes) }
                continue
            }
            when (key.second) {
                1 -> onOpLoc1(symbol) { cross(it.loc, routes) }
                2 -> onOpLoc2(symbol) { cross(it.loc, routes) }
                3 -> onOpLoc3(symbol) { cross(it.loc, routes) }
                4 -> onOpLoc4(symbol) { cross(it.loc, routes) }
                5 -> onOpLoc5(symbol) { cross(it.loc, routes) }
            }
        }
    }

    private suspend fun ProtectedAccess.cross(loc: BoundLocInfo, routes: List<ShortcutRoute>) {
        if (player.hitpoints == 0 || player.pendingLogout || player.loggingOut) return
        if (loc.id == "loc.dagannoth_crevice".asRSCM() && coords.level == 1) { mes("You try to go up the crevice, but it is slippery and you slide back down. This is one way only."); return }
        val from = coords
        val route = routes.filter { it.origin.level == from.level && it.origin.chebyshevDistance(from) <= 6 &&
            it.origin.chebyshevDistance(loc.coords) <= 18 }.minByOrNull { it.origin.chebyshevDistance(from) }
            ?: run { mes("Approach this shortcut from its entrance."); return }
        if (!requirements(route) || collision[route.destination.x, route.destination.z, route.destination.level] and BLOCKED != 0) return
        arriveDelay()
        faceSquare(route.destination)
        if (loc.id in setOf("loc.fenk_bridge_multi_north".asRSCM(), "loc.fenk_bridge_multi_north_mirror".asRSCM(), "loc.fenk_bridge_multi_south".asRSCM(), "loc.fenk_bridge_multi_south_mirror".asRSCM()) &&
            (vars["varbit.fenk_built_bridge_north"] != 2 || vars["varbit.fenk_built_bridge_south"] != 2)) { mes("Both sides of the bridge must be reinforced first."); return }
        if (route.stages.isNotEmpty()) { crossStages(route, from); return }
        val failure = route.failure.split(',')
        val failed = failure.size >= 3 && random.of(256) >= (failure[0].toInt() + (failure[1].toInt() - failure[0].toInt()) * (player.agilityLvl - 1) / 98).coerceIn(0, 256)
        var arrived = false
        try {
            anim(route.animation)
            val jump = route.option.startsWith("Jump", true) || route.option.startsWith("Step", true)
            val samePlane = from.level == route.destination.level && !(route.option.equals("Enter", true) && from.chebyshevDistance(route.destination) > 8)
            if (!failed && samePlane) {
                val direction = ((atan2((route.destination.x - from.x).toDouble(), (route.destination.z - from.z).toDouble()) * 325.949323) + 2048).toInt() and 2047
                exactMove(from, route.destination, 0, if (jump) 30 else route.ticks * 30, direction, TeleportType.Exempt)
            }
            repeat(route.ticks) { tick ->
                delay(1)
                if (player.pendingLogout || player.loggingOut || player.hitpoints == 0) return
                if (samePlane && !failed && coords != route.destination) return
                if (!samePlane && coords != from) return
                if (tick > 0 && !jump && samePlane) anim(route.animation)
            }
            if (failed) {
                mes("You lose your footing and fail to make it across.")
                failure.getOrNull(3)?.takeUnless { it == "null" }?.split("..")?.let { damage ->
                    queueHit(0, HitType.Typeless, random.of(damage[0].toInt(), damage[1].toInt()))
                }
                statAdvance("stat.agility", failure[2].toDouble() * xp.get(player, "stat.agility"))
                return
            }
            if (!samePlane) teleport(route.destination, TeleportType.Exempt)
            if (coords != route.destination) return
            arrived = true
            val award = if (route.loc.startsWith("loc.voros_rockslide02_shortcut")) {
                val now = Instant.now().epochSecond.toInt()
                val previous = player.attr[VAMPYRIUM_LAST_SLIDE]
                val bonus = previous == null || now.toLong() - previous >= 450
                player.attr[VAMPYRIUM_LAST_SLIDE] = now
                if (route.option == "Descend" && bonus) 550.0 else route.xp
            } else route.xp
            statAdvance("stat.agility", award * xp.get(player, "stat.agility"))
        } finally {
            resetAnim()
            if (!arrived && !failed && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && coords == route.destination) {
                teleport(from, TeleportType.Exempt)
            }
        }
    }

    private suspend fun ProtectedAccess.crossStages(route: ShortcutRoute, origin: org.rsmod.map.CoordGrid) {
        val failure = route.failure.split(',')
        val legs = route.stages + route.destination
        var expected = origin
        var finished = false
        try {
            for ((index, destination) in legs.withIndex()) {
                if (coords != expected || player.hitpoints == 0 || player.pendingLogout || player.loggingOut) return
                // The three Karamja rocks have independent success rolls; the final step is the exit.
                val failed = index < route.stages.size && failure.size >= 3 && random.of(256) >=
                    (failure[0].toInt() + (failure[1].toInt() - failure[0].toInt()) * (player.agilityLvl - 1) / 98).coerceIn(0, 256)
                if (failed) {
                    anim("seq.human_walk_logbalance_stumble"); delay(1)
                    if (coords != expected || player.hitpoints == 0 || player.pendingLogout || player.loggingOut) return
                    teleport(origin, TeleportType.Exempt)
                    statAdvance("stat.agility", failure[2].toDouble() * xp.get(player, "stat.agility"))
                    mes("You lose your footing and fail to make it across."); finished = true; return
                }
                anim(route.animation)
                val direction = ((atan2((destination.x - expected.x).toDouble(), (destination.z - expected.z).toDouble()) * 325.949323) + 2048).toInt() and 2047
                exactMove(expected, destination, 0, 30, direction, TeleportType.Exempt)
                expected = destination; delay(1)
                if (coords != expected || player.hitpoints == 0 || player.pendingLogout || player.loggingOut) return
                if (index < route.stages.size) statAdvance("stat.agility", route.xp / route.stages.size * xp.get(player, "stat.agility"))
            }
            finished = true
        } finally {
            resetAnim()
            if (!finished && coords == expected && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut) teleport(origin, TeleportType.Exempt)
        }
    }

    private fun ProtectedAccess.requirements(r: ShortcutRoute): Boolean {
        if (player.agilityLvl < r.level) { mes("You need Agility ${r.level} to use this shortcut."); return false }
        val bare = r.bareLevel > 0 && player.agilityLvl >= r.bareLevel
        if (!bare) {
            if (player.rangedLvl < r.ranged || player.strengthLvl < r.strength) {
                mes("You need Ranged ${r.ranged} and Strength ${r.strength} to use this shortcut."); return false
            }
            if (r.gear == "GEAR_GRAPPLE") {
                val weapon = player.worn[3]?.id?.let { ServerCacheManager.getItem(it) }?.name.orEmpty()
                if (!weapon.contains("crossbow", true) || player.worn[13]?.id != "obj.xbows_grapple_tip_bolt_mithril_rope".asRSCM()) {
                    mes("Wield a crossbow and equip a mith grapple to use this shortcut."); return false
                }
            }
            if (r.gear == "GEAR_CLIMBING_BOOTS" && player.worn[10]?.id != "obj.death_climbingboots".asRSCM()) {
                mes("You need climbing boots to use this shortcut."); return false
            }
        }
        if (r.quest != null && !shortcutQuestCompleted(player, r.quest)) { mes("You must complete the required quest first."); return false }
        if (r.gate != null) {
            val value = player.vars[r.gate]
            if (if (r.gateExact) value != r.gateValue else value < r.gateValue) { mes("You haven't unlocked this shortcut yet."); return false }
        }
        return true
    }

    internal companion object {
        const val BLOCKED = CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK
        val VAMPYRIUM_LAST_SLIDE = AttributeKey<Int>(persistenceKey = "vampyrium_last_slide_seconds")
    }
}

internal fun shortcutQuestCompleted(player: org.rsmod.game.entity.Player, quest: String): Boolean =
    QuestRequirements.hasCompleted(player, quest) || quest == "quest_bloodmoonrises" && player.vars["varbit.myq6"] == 200
