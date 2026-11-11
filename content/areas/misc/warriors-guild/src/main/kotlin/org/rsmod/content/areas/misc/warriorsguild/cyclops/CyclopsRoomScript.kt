package org.rsmod.content.areas.misc.warriorsguild.cyclops

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.content.areas.misc.warriorsguild.Defenders
import org.rsmod.content.areas.misc.warriorsguild.GuildDoors
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger
import org.rsmod.content.areas.misc.warriorsguild.basementUnlocked
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.BASEMENT
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.ENTRY_TOKENS
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.MINUTE_TICKS
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.NONE
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.TIMER
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.TOKENS_PER_MINUTE
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.TOP
import org.rsmod.content.areas.misc.warriorsguild.cyclops.CyclopsRooms.wearsAttackCape
import org.rsmod.content.areas.misc.warriorsguild.cyclopsFree
import org.rsmod.content.areas.misc.warriorsguild.cyclopsGrace
import org.rsmod.content.areas.misc.warriorsguild.cyclopsRoom
import org.rsmod.content.areas.misc.warriorsguild.cyclopsTier
import org.rsmod.content.areas.misc.warriorsguild.defenderEarned
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CyclopsRoomScript
@Inject
constructor(private val doors: GuildDoors, private val areas: AreaChecker) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.warguild_door_armour_l") {
            armourDoor(it.loc, "loc.warguild_door_inactive_armour_l", "loc.warguild_door_armour_r")
        }
        onOpLoc1("loc.warguild_door_armour_r") {
            armourDoor(it.loc, "loc.warguild_door_inactive_armour_r", "loc.warguild_door_armour_l")
        }
        onOpLoc1("loc.warguild_basement_door") { basementDoor(it.loc) }
        onOpLoc1("loc.warguild_ladder_down") { climb(LADDER_DOWN_DEST, "seq.human_pickupfloor") }
        onOpLoc1("loc.warguild_ladder_up") { climb(LADDER_UP_DEST, "seq.human_reachforladder") }

        onPlayerTimer(TIMER) { drain() }
        onAreaExit(CyclopsRooms.TOP_AREA) { leaveRoom(TOP) }
        onAreaExit(CyclopsRooms.BASEMENT_AREA) { leaveRoom(BASEMENT) }
        onPlayerLogin { resumeRoom(player) }
    }

    private suspend fun ProtectedAccess.armourDoor(
        door: BoundLocInfo,
        openLoc: String,
        partner: String,
    ) {
        val entering = doors.destination(this, door).x >= door.coords.x
        if (door.coords.level != TOP_LEVEL || !entering) {
            doors.pass(this, door, openLoc, partner = partner)
            return
        }
        if (!canAfford(TOP)) return
        val owned = Defenders.bestOwned(player)
        startDialogue { kamfreenaWelcome(owned) }
        if (!charge()) return
        Defenders.remember(player, owned)
        startRoom(TOP, minOf(owned + 1, Defenders.RUNE))
        doors.pass(this, door, openLoc, partner = partner)
    }

    private suspend fun ProtectedAccess.basementDoor(door: BoundLocInfo) {
        val entering = doors.destination(this, door).x > door.coords.x
        if (!entering) {
            doors.pass(this, door, BASEMENT_OPEN_DOOR)
            return
        }
        if (!player.basementUnlocked) {
            if (Defenders.bestCarried(player) < Defenders.RUNE) {
                startDialogue {
                    chatNpcSpecific(
                        "Lorelai",
                        CyclopsRooms.LORELAI,
                        neutral,
                        "You'll need to display your skills as a Warrior before I can allow you " +
                            "to enter the room.",
                    )
                }
                return
            }
            player.basementUnlocked = true
            startDialogue {
                chatNpcSpecific(
                    "Lorelai",
                    CyclopsRooms.LORELAI,
                    happy,
                    "I see you have a worthy defender! I'll release some cyclops to help you get " +
                        "an upgrade. They are quite mean and strong though...",
                )
            }
        }
        if (!canAfford(BASEMENT)) return
        if (!charge()) return
        startRoom(BASEMENT, Defenders.DRAGON)
        doors.pass(this, door, BASEMENT_OPEN_DOOR)
    }

    private suspend fun ProtectedAccess.canAfford(room: Int): Boolean {
        if (player.wearsAttackCape() || inv.count(TokenLedger.TOKEN) >= ENTRY_TOKENS) return true
        startDialogue {
            chatNpcSpecific(
                CyclopsRooms.keeperName(room),
                CyclopsRooms.keeper(room),
                neutral,
                "You don't have enough Warrior Guild Tokens to enter the cyclopes enclosure yet, " +
                    "collect at least $ENTRY_TOKENS then come back.",
            )
        }
        return false
    }

    private fun ProtectedAccess.charge(): Boolean {
        if (player.wearsAttackCape()) return true
        if (inv.count(TokenLedger.TOKEN) < ENTRY_TOKENS) return false
        return invDel(inv, TokenLedger.TOKEN, TOKENS_PER_MINUTE).success
    }

    private fun ProtectedAccess.startRoom(room: Int, tier: Int) {
        player.cyclopsRoom = room
        player.cyclopsTier = tier
        player.cyclopsGrace = false
        player.cyclopsFree = player.wearsAttackCape()
        player.clearTimer(TIMER)
        timer(TIMER, MINUTE_TICKS)
    }

    private suspend fun ProtectedAccess.drain() {
        val room = player.cyclopsRoom
        if (room == NONE) return
        if (!areas.inArea(CyclopsRooms.area(room), coords)) {
            clearRoom(player)
            return
        }
        timer(TIMER, MINUTE_TICKS)
        if (player.cyclopsFree && player.wearsAttackCape()) return
        player.cyclopsFree = false
        if (player.cyclopsGrace) {
            evict(room)
            return
        }
        if (inv.count(TokenLedger.TOKEN) >= TOKENS_PER_MINUTE) {
            invDel(inv, TokenLedger.TOKEN, TOKENS_PER_MINUTE)
            mes("$TOKENS_PER_MINUTE of your tokens crumble away.")
        }
        if (inv.count(TokenLedger.TOKEN) < TOKENS_PER_MINUTE) {
            player.cyclopsGrace = true
            keeperSays(room, "Your time is up, please make your way to the exit.")
        }
    }

    private suspend fun ProtectedAccess.evict(room: Int) {
        clearRoom(player)
        telejump(CyclopsRooms.exit(room))
        keeperSays(room, "Next time, please leave as soon as your time is up.")
    }

    private suspend fun ProtectedAccess.keeperSays(room: Int, text: String) {
        startDialogue {
            chatNpcSpecific(CyclopsRooms.keeperName(room), CyclopsRooms.keeper(room), neutral, text)
        }
    }

    private fun ProtectedAccess.leaveRoom(room: Int) {
        if (player.cyclopsRoom == room) clearRoom(player)
    }

    private fun resumeRoom(player: Player) {
        val room = player.cyclopsRoom
        if (room == NONE) return
        if (!areas.inArea(CyclopsRooms.area(room), player.coords)) {
            clearRoom(player)
            return
        }
        player.timer(TIMER, MINUTE_TICKS)
    }

    private fun clearRoom(player: Player) {
        player.cyclopsRoom = NONE
        player.cyclopsGrace = false
        player.cyclopsFree = false
        player.clearTimer(TIMER)
    }

    private suspend fun ProtectedAccess.climb(dest: CoordGrid, seq: String) {
        arriveDelay()
        anim(seq)
        delay(1)
        telejump(dest)
    }

    private suspend fun Dialogue.kamfreenaWelcome(owned: Int) {
        val kamfreena = CyclopsRooms.KAMFREENA
        when {
            owned == Defenders.NONE && player.defenderEarned == 0 -> {
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "Well, since you haven't shown me a defender to prove your prowess as a " +
                        "warrior,",
                )
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "I'll release some cyclopes which might drop bronze defenders for you to " +
                        "start off with, unless you show me another. Have fun in there.",
                )
            }
            owned == Defenders.NONE -> {
                chatNpcSpecific("Kamfreena", kamfreena, neutral, "I remember you....")
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "You don't have the defender you earned last time.",
                )
                chatPlayer(sad, "Err... no, don't have it on me. I kind of... lost it.")
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "Sorry, but as you've lost any that you earned before I can only let you in " +
                        "to kill the cyclopes that drop the bronze defenders. Go on in when " +
                        "you're ready.",
                )
            }
            owned == Defenders.DRAGON -> {
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    happy,
                    "I see you have one of the dragon defenders already! Well done, but you can " +
                        "only get more dragon defenders from the basement.",
                )
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "If you'd rather stay up here, I've released the cyclopes that can drop " +
                        "rune defenders. Enter when you're ready.",
                )
            }
            owned == Defenders.RUNE -> {
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    happy,
                    "Ahh I see that you have one of the rune defenders already! Well done.",
                )
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "You should speak to my apprentice Lorelai in the basement and test yourself " +
                        "against even stronger Cyclopes!",
                )
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "The Cyclopes up here will continue to have a chance of dropping rune " +
                        "defenders. Have fun in there.",
                )
            }
            else -> {
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    happy,
                    "Ahh I see that you have one of the defenders already! Well done.",
                )
                chatNpcSpecific(
                    "Kamfreena",
                    kamfreena,
                    neutral,
                    "I'll release some cyclopes which might drop the next defender for you. Have " +
                        "fun in there.",
                )
            }
        }
    }

    private companion object {
        const val TOP_LEVEL = 2
        const val BASEMENT_OPEN_DOOR = "loc.warguild_basement_door_inactive"
        val LADDER_DOWN_DEST = CoordGrid(2907, 9968, 0)
        val LADDER_UP_DEST = CoordGrid(2834, 3542, 0)
    }
}
