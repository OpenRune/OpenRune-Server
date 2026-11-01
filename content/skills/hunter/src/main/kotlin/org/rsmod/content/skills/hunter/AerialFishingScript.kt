package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpWorn1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.table.hunter.HunterAerialFishRow
import org.rsmod.api.table.hunter.HunterPearlShopRow
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class AerialFishingScript
@Inject
constructor(
    private val shops: Shops,
    private val shopOps: ShopOperationMap,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val clock: MapClock,
    private val pets: PetRewards,
) : PluginScript() {
    private val fishes: List<HunterAerialFishRow> by lazy { HunterAerialFishRow.all().sortedByDescending { it.roll } }
    private val prices: Map<Int, Int> by lazy { HunterPearlShopRow.all().associate { it.obj.id to it.pearls } }
    private val flights = HashMap<Player, Flight>()
    private val spotTiles = LinkedHashSet<CoordGrid>()

    override fun ScriptContext.startup() {
        shopOps.costOf(CURRENCY) { type -> prices[type.id] ?: type.cost.coerceAtLeast(1) }
        onOpNpc1(ALRY) { startDialogue(it.npc) { alry() } }
        onOpNpc3(ALRY) { openShop() }
        onOpNpc4(ALRY) { startDialogue(it.npc) { getBird() } }
        for (spot in SPOTS) {
            onOpNpc1(spot) { send(it.npc) }
            onApNpc1(spot) {
                if (isWithinDistance(it.npc, RANGE) && lineOfSight(player.coords, it.npc.coords)) send(it.npc) else apRange(RANGE)
            }
            onNpcTimer(spot.toNpcType(), MOVE_TIMER) { relocate(npc) }
        }
        onEvent<NpcStateEvents.Create> { if (npc.type.internalName == SPOT) startSpot(npc) }
        for (glove in GLOVES) onOpWorn1(glove) { mes("You should speak to Alry to get this removed safely.") }
        for (fish in fishes) onOpHeldU(KNIFE, fish.fish.internalName) { cut(fish) }
        onPlayerSoftTimer(TIMER) { player.flightTick() }
        onPlayerLogout { flights.remove(player) }
    }

    private fun String.toNpcType() = dev.openrune.ServerCacheManager.getNpc(asRSCM(RSCMType.NPC))!!

    private fun startSpot(npc: Npc) {
        spotTiles += npc.spawnCoords
        npc.timer(MOVE_TIMER, random.of(MOVE_MIN, MOVE_MAX))
    }

    private fun relocate(npc: Npc) {
        val free = spotTiles.filter { it != npc.coords }
        if (free.isNotEmpty()) npc.telejump(collision, free[random.of(free.size)])
        npc.timer(MOVE_TIMER, random.of(MOVE_MIN, MOVE_MAX))
    }

    private fun ProtectedAccess.openShop() {
        shops.open(
            player = player,
            title = SHOP_TITLE,
            shopInv = SHOP_INV,
            buyPercentage = 0.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
            currency = CURRENCY,
        )
    }

    private fun wearsGlove(player: Player): Boolean = GLOVES.any { it in player.worn }

    private suspend fun Dialogue.alry() {
        if (wearsGlove(player)) {
            chatPlayer(neutral, "Hello again.")
            chatNpc(neutral, "Ah, it's you. How are you finding things?")
            chatPlayer(neutral, "It certainly is a challenge...")
            chatNpc(neutral, "Yes, your type do often seem to underestimate it! Are you finished?")
            val tench = GOLDEN_TENCH in player.inv
            val choice =
                if (tench) {
                    choice3("Not yet. I think I'll keep trying.", 1, "Yeah, I'll try again another time.", 2, "I found this big fish...", 3)
                } else {
                    choice2("Not yet. I think I'll keep trying.", 1, "Yeah, I'll try again another time.", 2)
                }
            when (choice) {
                1 -> {
                    chatPlayer(neutral, "Not yet. I think I'll keep trying.")
                    chatNpc(neutral, "Just come back and let me know when you get tired!")
                }
                2 -> {
                    chatPlayer(neutral, "I'll try again another time.")
                    access.returnGlove()
                    mesbox("Alry takes back the Cormorant's glove.")
                }
                else -> goldenTench()
            }
            return
        }
        chatPlayer(neutral, "Hello there.")
        chatNpc(neutral, "What brings you to these parts, stranger?")
        val tench = GOLDEN_TENCH in player.inv
        val choice =
            if (tench) {
                choice5(
                    "What is this place?", 1,
                    "Could I have a go with your bird?", 2,
                    "What's in the sack?", 3,
                    "I found this big fish...", 4,
                    "About feeding the bird...", 5,
                )
            } else {
                choice4(
                    "What is this place?", 1,
                    "Could I have a go with your bird?", 2,
                    "What's in the sack?", 3,
                    "About feeding the bird...", 5,
                )
            }
        when (choice) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(neutral, "I suppose it is a little different to the places your type are used to...")
                chatNpc(
                    neutral,
                    "This is my humble little hideaway! Plenty of fish around to sustain a man like me, with the " +
                        "help of my trusty cormorant.",
                )
                chatPlayer(quiz, "A bird helps you to fish?")
                chatNpc(
                    happy,
                    "Sure she does! I haven't used a rod in years, just send her out to the waters and she takes " +
                        "care of it for me!",
                )
                if (choice2("Could I have a go with your bird?", true, "I don't think that's for me.", false)) {
                    getBird()
                } else {
                    chatPlayer(neutral, "I don't think that's for me... I'll leave that to you.")
                    chatNpc(neutral, "No surprises there! Your type never have been cut out for it.")
                }
            }
            2 -> getBird()
            3 -> {
                chatPlayer(quiz, "What's in the sack?")
                chatNpc(happy, "Oh, this? This is my trusty old fish sack!")
                chatPlayer(quiz, "Fish sack?")
                chatNpc(neutral, "Yeah, fish sack! I might sell you one if you've found any of those pearls around here.")
                if (choice2("Let's take a look.", true, "No thanks.", false)) {
                    access.openShop()
                } else {
                    chatPlayer(neutral, "No thanks.")
                }
            }
            4 -> goldenTench()
            else -> feeding()
        }
    }

    private suspend fun Dialogue.getBird() {
        chatPlayer(quiz, "Could I have a go with your bird?")
        if (player.stat(FISHING) < FISHING_LEVEL || player.stat(HUNTER) < HUNTER_LEVEL) {
            chatNpc(
                neutral,
                "These birds can be a lot of work... I'm not sure you could handle them, let alone to catch a fish! " +
                    "You need 43 Fishing and 35 Hunter to go aerial fishing.",
            )
            return
        }
        chatNpc(neutral, "I don't know, I doubt that your type's up to the task...")
        chatNpc(happy, "But it would be quite the amusing sight. Go on!")
        if (HAND_SLOTS.any { player.worn[it] != null }) {
            chatNpc(neutral, "You're going to need to free up your hands first, though. Nothing in or on your hands.")
            return
        }
        chatNpc(neutral, "I'll keep an eye on you and make sure you don't have too much trouble with her!")
        access.invAdd(player.worn, BIRD_GLOVE, slot = Wearpos.RightHand.slot)
        access.rebuildAppearance()
    }

    private suspend fun Dialogue.goldenTench() {
        chatPlayer(neutral, "I found this big fish...")
        chatNpc(happy, "Now that is a biggun! How she managed to wrangle one like that I'll never know.")
        chatNpc(neutral, "If you don't want it, I could take it off your hands for a few of those pearls!")
        chatPlayer(quiz, "I'm interested... How many pearls are we talking?")
        chatNpc(neutral, "I'll give you a hundred pearls for it. What do you say?")
        val title = "Trade in your golden tench for 100 molch pearls?"
        if (!choice2("It's a deal.", true, "No thanks.", false, title = title)) {
            chatPlayer(neutral, "No thanks.")
            chatNpc(neutral, "Well, let me know if you change your mind.")
            return
        }
        if (!choice2("Yes, exchange the golden tench.", true, "No, keep the tench.", false, title = "Are you REALLY sure?")) {
            chatPlayer(neutral, "On seconds thoughts, I'd rather keep it.")
            chatNpc(neutral, "As you wish.")
            return
        }
        chatPlayer(happy, "It's a deal.")
        if (!access.sellTench()) return
        chatNpc(happy, "Pleasure doing business with you!")
    }

    private fun ProtectedAccess.sellTench(): Boolean =
        player
            .invTransaction(inv) {
                val target = select(inv)
                delete(target, GOLDEN_TENCH.asRSCM(RSCMType.OBJ), 1)
                add(target, PEARL.asRSCM(RSCMType.OBJ), TENCH_PEARLS)
            }
            .success

    private suspend fun Dialogue.feeding() {
        chatPlayer(quiz, "About feeding the bird... Do I have to cut up the fish for it?")
        chatNpc(neutral, "Yes, she likes her food chopped up.")
        chatPlayer(quiz, "But she could eat them whole in a pinch?")
        chatNpc(neutral, "I suppose, but you wouldn't want me to catch you feeding her that way.")
        if (vars[WHOLE_FISH_PAID] == 0) {
            if (player.inv.count(PEARL) < WHOLE_FISH_COST) {
                chatPlayer(neutral, "Of course, I'd never do such a thing.")
                mesbox("You could bargain better if you had some pearls.")
                return
            }
            chatPlayer(neutral, "What if I were to... Sweeten the deal.")
            mesbox("You flash a handful of pearls.")
            chatNpc(happy, "I do like pearls...")
            chatNpc(neutral, "I'll do it for 20 of them.")
            if (!choice2("Yes", true, "No", false, title = "Pay 20 pearls?")) {
                chatPlayer(neutral, "I'm afraid that's a little too steep for me.")
                chatNpc(angry, "Then get out of my sight!")
                return
            }
            chatPlayer(neutral, "Here you go.")
            if (access.invDel(player.inv, PEARL, WHOLE_FISH_COST).failure) return
            VarPlayerIntMapSetter.set(player, WHOLE_FISH_PAID, 1)
        }
        val on = choice2("Yes", true, "No", false, title = "Feed the cormorant whole fish?")
        VarPlayerIntMapSetter.set(player, WHOLE_FISH_TOGGLE, if (on) 1 else 0)
    }

    private fun ProtectedAccess.returnGlove() {
        flights.remove(player)
        for (glove in GLOVES) if (glove in player.worn) invDel(player.worn, glove)
        rebuildAppearance()
    }

    private fun ProtectedAccess.send(spot: Npc) {
        if (BIRD_GLOVE !in player.worn || flights.containsKey(player)) return
        if (!spot.isSlotAssigned || !spot.isVisible) return
        if (BAITS.none { it in inv } && !(vars[WHOLE_FISH_TOGGLE] == 1 && fishes.any { it.fish.internalName in inv })) {
            mes("You need some fish offcuts or king worms to reward your cormorant.")
            return
        }
        faceEntitySquare(spot)
        anim(SEND_SEQ)
        spotanim(LAUNCH_SPOTANIM)
        invReplace(player.worn, BIRD_GLOVE, 1, EMPTY_GLOVE)
        rebuildAppearance()
        val d = distance(player.coords, spot.coords)
        val ticks = returnTicks(d)
        worldRepo.projAnim(
            ProjAnim(TRAVEL_SPOTANIM.asRSCM(RSCMType.SPOTANIM), 150, 0, 0, ticks * 15, 10, 0,
                -(player.slotId + 1), 0, player.coords, spot.coords)
        )
        flights[player] = Flight(spot.coords, clock.cycle + ticks)
        player.softTimer(TIMER, 1)
    }

    private fun returnTicks(distance: Int): Int =
        when {
            distance <= 2 -> 1
            distance <= 4 -> 2
            distance <= 5 -> 3
            else -> (distance + 1) / 2
        }

    private fun Player.flightTick() {
        val flight = flights[this]
        if (flight == null) {
            clearSoftTimer(TIMER)
            return
        }
        if (clock.cycle < flight.returnCycle) return
        flights.remove(this)
        if (EMPTY_GLOVE in worn) {
            worn[Wearpos.RightHand.slot] = org.rsmod.game.inv.InvObj(BIRD_GLOVE)
            rebuildAppearance()
        }
        catchFish(flight.spot)
    }

    private fun Player.catchFish(spot: CoordGrid) {
        worldRepo.spotanimMap(SPLASH_TYPE, spot)
        val fishing = stat(FISHING)
        val hunter = stat(HUNTER)
        val x = (fishing * 2 + hunter) / 3
        val roll = random.of(x.coerceAtLeast(1))
        val fish =
            fishes.firstOrNull { roll >= it.roll && fishing >= it.fishingLevel && hunter >= it.hunterLevel }
                ?: fishes.last()
        consumeBait(fish.fish.internalName)
        statAdvance(FISHING, fish.fishingXpTenths / 10.0)
        statAdvance(HUNTER, fish.hunterXpTenths / 10.0)
        if (random.of(GOLDEN_TENCH_RATE) == 0) {
            addOrGround(GOLDEN_TENCH, 1)
        } else {
            val extra = rollExtra()
            if (inv.freeSpace() > 0) invAddPlayer(fish.fish.internalName, minOf(1 + extra, inv.freeSpace()))
        }
        val pearlDenominator = (PEARL_BASE - (x - PEARL_X_OFFSET) * PEARL_SLOPE / PEARL_SLOPE_DIVISOR) / PEARL_BONUS
        if (random.of(max(1.0, pearlDenominator).toInt()) == 0) addOrGround(PEARL, 1)
        pets.rollSkillingPet(this, HERON, FISHING, HERON_BASE)
    }

    private fun Player.rollExtra(): Int {
        val blessing =
            when {
                RADAS_BLESSING_4 in worn -> 8
                RADAS_BLESSING_3 in worn -> 6
                RADAS_BLESSING_2 in worn -> 4
                RADAS_BLESSING_1 in worn -> 2
                else -> 0
            }
        val flakes = inv.count(SPIRIT_FLAKES) > 0 && inv.freeSpace() >= 2
        val chance = (if (flakes) SPIRIT_FLAKES_CHANCE else 0) + blessing
        if (flakes) invDel(inv, SPIRIT_FLAKES.asRSCM(RSCMType.OBJ), 1)
        if (chance <= 0 || random.of(100) >= chance) return 0
        return 1
    }

    private fun Player.invAddPlayer(obj: String, count: Int) {
        invAdd(inv, obj.asRSCM(RSCMType.OBJ), count)
    }

    private fun Player.addOrGround(obj: String, count: Int) {
        invAddOrDrop(objRepo, obj, count, GROUND_DURATION)
    }

    private fun Player.consumeBait(caught: String) {
        if (KING_WORM in inv) {
            invDel(inv, KING_WORM.asRSCM(RSCMType.OBJ), 1)
            return
        }
        val offcut = OFFCUTS.firstOrNull { it in inv }
        if (offcut != null) {
            if (random.of(OFFCUT_EVERY_MIN, OFFCUT_EVERY_MAX) == OFFCUT_EVERY_MIN) {
                invDel(inv, offcut.asRSCM(RSCMType.OBJ), 1)
            }
            return
        }
        if (vars[WHOLE_FISH_TOGGLE] == 1) {
            val whole = fishes.map { it.fish.internalName }.firstOrNull { it in inv } ?: caught
            if (whole in inv) invDel(inv, whole.asRSCM(RSCMType.OBJ), 1)
        }
    }

    private fun ProtectedAccess.cut(fish: HunterAerialFishRow) {
        val name = fish.fish.internalName
        if (invReplace(inv, name, 1, FISH_CHUNKS).failure) return
        statAdvance(COOKING, fish.cookingXpTenths / 10.0)
        mes("You cut the fish into offcuts.", ChatType.Spam)
    }

    private fun distance(a: CoordGrid, b: CoordGrid): Int = max(abs(a.x - b.x), abs(a.z - b.z))

    class Flight(val spot: CoordGrid, val returnCycle: Int)

    companion object {
        const val HUNTER = "stat.hunter"
        const val FISHING = "stat.fishing"
        const val COOKING = "stat.cooking"
        const val ALRY = "npc.fishing_npc_angler"
        const val SPOT = "npc.fishing_spot_aerial"
        val SPOTS = listOf(SPOT)
        const val SHOP_INV = "inv.aerial_fishing_pearl_shop"
        const val SHOP_TITLE = "Alry the Angler's Angling Accessories"
        const val CURRENCY = "currency.molch_pearl"
        const val BIRD_GLOVE = "obj.aerial_fishing_gloves_bird"
        const val EMPTY_GLOVE = "obj.aerial_fishing_gloves_no_bird"
        val GLOVES = listOf(BIRD_GLOVE, EMPTY_GLOVE)
        val HAND_SLOTS = listOf(Wearpos.RightHand.slot, Wearpos.LeftHand.slot, Wearpos.Hands.slot)
        const val KNIFE = "obj.knife"
        const val KING_WORM = "obj.king_worm"
        const val FISH_CHUNKS = "obj.fish_chunks"
        val OFFCUTS = listOf(FISH_CHUNKS, "obj.sailing_fine_fish_offcuts")
        val BAITS = listOf(KING_WORM) + OFFCUTS
        const val PEARL = "obj.aerial_fishing_pearl"
        const val GOLDEN_TENCH = "obj.golden_tench"
        const val HERON = "obj.skillpetfish"
        const val SPIRIT_FLAKES = "obj.spirit_flakes"
        const val RADAS_BLESSING_1 = "obj.zeah_blessing_easy"
        const val RADAS_BLESSING_2 = "obj.zeah_blessing_medium"
        const val RADAS_BLESSING_3 = "obj.zeah_blessing_hard"
        const val RADAS_BLESSING_4 = "obj.zeah_blessing_elite"
        const val WHOLE_FISH_TOGGLE = "varbit.aerial_fishing_whole_fish_toggle"
        const val WHOLE_FISH_PAID = "varbit.aerial_fishing_whole_fish_toggle_paid"
        const val SEND_SEQ = "seq.human_falconer_attack"
        const val LAUNCH_SPOTANIM = "spotanim.aerial_fishing_launch"
        const val TRAVEL_SPOTANIM = "spotanim.aerial_fishing_travel"
        val SPLASH_TYPE: SpotanimType get() = SpotanimType("spotanim.aerial_fishing_splash".asRSCM(RSCMType.SPOTANIM))
        const val TIMER = "timer.hunter_aerial_fishing"
        const val MOVE_TIMER = "timer.hunter_aerial_spot_move"
        const val MOVE_MIN = 12
        const val MOVE_MAX = 20
        const val RANGE = 8
        const val FISHING_LEVEL = 43
        const val HUNTER_LEVEL = 35
        const val TENCH_PEARLS = 100
        const val WHOLE_FISH_COST = 20
        const val GOLDEN_TENCH_RATE = 20_000
        const val HERON_BASE = 636_833
        const val SPIRIT_FLAKES_CHANCE = 50
        const val GROUND_DURATION = 200
        const val OFFCUT_EVERY_MIN = 3
        const val OFFCUT_EVERY_MAX = 4
        const val PEARL_BASE = 67.0
        const val PEARL_X_OFFSET = 40
        const val PEARL_SLOPE = 25.0
        const val PEARL_SLOPE_DIVISOR = 59.0
        const val PEARL_BONUS = 1.5
    }
}
