package org.rsmod.content.skills.hunter

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterCapeScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (cape in CAPES) {
            onOpHeld3(cape) { teleportMenu() }
            onOpWorn3(cape) { teleportMenu() }
            onOpWorn2(cape) { boost() }
        }
        onOpNpc1(APATURA) { startDialogue(it.npc) { apatura() } }
    }

    private fun ProtectedAccess.boost() {
        if (!isMaster()) return
        statBoost(HUNTER, constant = 1, percent = 0)
    }

    private fun ProtectedAccess.isMaster(): Boolean {
        if (player.statBase(HUNTER) >= 99) return true
        mes("You need a Hunter level of 99 to do that.")
        return false
    }

    private fun utcDay(): Int = epochMinute() / MINUTES_PER_DAY

    private suspend fun ProtectedAccess.teleportMenu() {
        if (!isMaster()) return
        if (vars[TELEPORT_DAY] != utcDay()) {
            VarPlayerIntMapSetter.set(player, TELEPORT_DAY, utcDay())
            VarPlayerIntMapSetter.set(player, TELEPORTS_USED, 0)
        }
        val left = DAILY_TELEPORTS - vars[TELEPORTS_USED]
        val guild = vars[GUILD_UNLOCKED] != 0
        val dest =
            if (guild) {
                choice3(
                    "Feldip Hunter area ($left left today)",
                    FELDIP,
                    "Wilderness Hunter area ($left left today)",
                    WILDERNESS,
                    "Hunter Guild",
                    GUILD,
                    "Hunter Areas",
                )
            } else {
                choice2(
                    "Feldip Hunter area ($left left today)",
                    FELDIP,
                    "Wilderness Hunter area ($left left today)",
                    WILDERNESS,
                    "Hunter Areas",
                )
            }
        if (dest != GUILD) {
            if (left <= 0) {
                mes("You have used all of your Hunter cape teleports for today.")
                return
            }
            VarPlayerIntMapSetter.set(player, TELEPORTS_USED, vars[TELEPORTS_USED] + 1)
        }
        teleport(dest)
    }

    private suspend fun ProtectedAccess.teleport(dest: CoordGrid) {
        anim(TELEPORT_SEQ)
        spotanim(TELEPORT_SPOTANIM, height = TELEPORT_SPOTANIM_HEIGHT)
        soundSynth(TELEPORT_SYNTH)
        delay(TELEPORT_DELAY)
        telejump(dest, TeleportType.Standard)
        resetAnim()
    }

    private suspend fun Dialogue.apatura() {
        if (player.statBase(HUNTER) < GUILD_LEVEL) {
            chatNpc(
                neutral,
                "Sorry, but you need to be a better hunter before we can help you out.",
            )
            mesbox("You need $GUILD_LEVEL Hunter to access Hunter Guild amenities.")
            return
        }
        if (vars[APATURA_MET] == 0) {
            chatNpc(quiz, "Oh, you're a new face around here, aren't you?")
            chatPlayer(neutral, "I am indeed. Just exploring the savannah.")
            chatNpc(happy, "Well, I'm Apatura. Let me welcome you to our small corner of Varlamore.")
            VarPlayerIntMapSetter.set(player, APATURA_MET, 1)
        } else {
            chatNpc(neutral, "Nilsal, hunter.")
            chatPlayer(neutral, "Nilsal, Apatura.")
        }
        if (vars[GUILD_UNLOCKED] == 0 && CAPES.any { it in player.inv || it in player.worn }) {
            chatNpc(quiz, "Is that a skillcape you have with you?")
            chatPlayer(neutral, "It is.")
            chatNpc(quiz, "Would you like me to enhance it with the ability to teleport directly to the guild?")
            if (choice2("Yes please.", true, "No thanks.", false)) {
                chatPlayer(happy, "Yes please.")
                VarPlayerIntMapSetter.set(player, GUILD_UNLOCKED, 1)
                chatNpc(happy, "Kuani! That should be done for you now.")
            } else {
                chatPlayer(neutral, "No thanks.")
            }
        }
        if (choice2("What do you do here?", true, "I'd better be off.", false)) {
            chatPlayer(quiz, "What do you do here?")
            chatNpc(
                neutral,
                "We're mostly here to keep an eye on the local animal populations, and provide animal parts to the " +
                    "city.",
            )
            chatNpc(neutral, "As for me personally, I'm the current master of this guild.")
            chatPlayer(quiz, "Current master?")
            chatNpc(
                neutral,
                "A hunter's work is dangerous. We tend to go through members faster than other guilds. That said, " +
                    "I'm usually too busy with politics and paperwork to go out hunting these days.",
            )
        } else {
            chatPlayer(neutral, "I'd better be off.")
        }
    }

    internal companion object {
        const val HUNTER = "stat.hunter"
        const val APATURA = "npc.hg_apatura"
        const val GUILD_LEVEL = 46
        const val TELEPORTS_USED = "varbit.chinchompa_teleports"
        const val TELEPORT_DAY = "varp.hunter_cape_teleport_day"
        const val GUILD_UNLOCKED = "varp.hunter_cape_guild_teleport"
        const val APATURA_MET = "varp.hunter_guild_apatura_met"
        const val DAILY_TELEPORTS = 5
        const val MINUTES_PER_DAY = 1440
        const val TELEPORT_SEQ = "seq.human_castteleport"
        const val TELEPORT_SPOTANIM = "spotanim.teleport_casting"
        const val TELEPORT_SPOTANIM_HEIGHT = 92
        const val TELEPORT_SYNTH = "synth.teleport_all"
        const val TELEPORT_DELAY = 4
        val CAPES = listOf("obj.skillcape_hunting", "obj.skillcape_hunting_trimmed")
        val FELDIP = CoordGrid(2504, 2898, 0)
        val WILDERNESS = CoordGrid(3150, 3770, 0)
        val GUILD = CoordGrid(1557, 3046, 0)
    }
}
