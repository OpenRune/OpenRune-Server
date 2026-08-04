package org.rsmod.content.other.ironman

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class IronmanTutorScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(NPC) { startDialogue(it.npc) { talk() } }
        onOpNpc3(NPC) { startDialogue(it.npc) { giveArmour() } }
        onOpNpc4(NPC) { ifOpenMainModal(SETUP) }
    }

    private suspend fun Dialogue.talk() {
        when {
            player.attr[MET] != true -> {
                chatNpc(happy, "Hello, ${player.displayName}. I'm Adam, the Iron$iw tutor. What can I do for you?")
                player.attr[MET] = true
                menu(firstMeet = true)
            }
            player.isAnyIronman -> {
                chatPlayer(happy, "Hey there Adam.")
                chatNpc(happy, "Hail, Iron$iw!")
                menu(iron = true)
            }
            else -> {
                chatNpc(happy, "Hello, ${player.displayName}. I'm the Iron$iw tutor. What can I do for you?")
                menu(iron = false)
            }
        }
    }

    private suspend fun Dialogue.menu(firstMeet: Boolean = false, iron: Boolean = false) {
        when {
            firstMeet ->
                when (
                    choice3(
                        "Tell me about Iron$iws.",
                        1,
                        "I'd like to change my Iron$iw mode.",
                        2,
                        "I'm fine, thanks.",
                        3,
                    )
                ) {
                    1 -> about(groupModes = true).also { adviceForNormal(); afterAbout(iron = false) }
                    2 -> changeMode(warnTutorial = true)
                    3 -> fine()
                }
            iron ->
                when (
                    choice4(
                        "Tell me about Iron$iws.",
                        1,
                        "I'd like to review my Iron$iw mode.",
                        2,
                        "Have you any armour for me, please?",
                        3,
                        "I'm fine, thanks.",
                        4,
                    )
                ) {
                    1 -> about(groupModes = false).also { adviceForIron(); afterAbout(iron = true) }
                    2 -> reviewMode()
                    3 -> giveArmour()
                    4 -> fine()
                }
            else ->
                when (
                    choice4(
                        "Tell me about Iron$iws.",
                        1,
                        "I'd like to review my Iron$iw mode.",
                        2,
                        "Have you any armour for me, please?",
                        3,
                        "I'm fine, thanks.",
                        4,
                    )
                ) {
                    1 -> about(groupModes = true).also { adviceForNormal(); afterAbout(iron = false) }
                    2 -> reviewMode()
                    3 -> giveArmour()
                    4 -> fine()
                }
        }
    }

    private suspend fun Dialogue.about(groupModes: Boolean) {
        chatPlayer(quiz, "Tell me about Iron$iws.")
        chatNpc(
            happy,
            "When you play as an ${std("Iron$iw")}, you do everything for yourself. " +
                "You don't trade with other players, or take their items, or accept their help.",
        )
        chatNpc(
            happy,
            "As an ${std("Iron$iw")}, you choose to have these restrictions imposed on you, " +
                "so everyone knows you're doing it properly.",
        )
        chatNpc(happy, "If you think you have what it takes, you can choose to become a ${hc("Hardcore Iron$iw")}.")
        chatNpc(
            happy,
            "In addition to the standard restrictions, ${hc("Hardcore Iron$iws")} only have ${hc("one life")}. " +
                "In the event of a dangerous death, your ${hc("Hardcore Iron$iw")} status will be downgraded " +
                "to that of a ${std("standard Iron$iw")}, and your",
        )
        chatNpc(
            happy,
            "stats will be frozen on the ${hc("Hardcore Iron ${iw.replaceFirstChar(Char::uppercaseChar)}")} hiscores.",
        )
        if (groupModes) {
            chatNpc(
                happy,
                "If you have some friends, you could stand alone... Together, as ${std("Group Ironmen")}. " +
                    "Group Ironmen have the same restrictions as regular Iron$iws except you can trade " +
                    "your items with each other!",
            )
            chatNpc(
                happy,
                "If you are a daring bunch, you could even try ${hc("Hardcore Group Ironmen")}. With Hardcore " +
                    "Group Ironmen you have a set number of lives, when they reach zero, you will become " +
                    "regular ${std("Group Ironmen")}.",
            )
            chatNpc(
                happy,
                "If you ever decided to invite experienced players to your group, you would become " +
                    "${std("Unranked Group Ironmen")}, which are no longer shown on the Group Ironman hiscores.",
            )
        }
        chatNpc(
            happy,
            "For the ultimate challenge, you can choose to become an ${uim("Ultimate Iron$iw")}, " +
                "a game mode inspired by the player ${uim("IronNoBank")}.",
        )
        chatNpc(
            happy,
            "In addition to the standard restrictions, ${uim("Ultimate Iron$iws")} are blocked from using " +
                "the bank, and they drop all their items when they die.",
        )
    }

    private suspend fun Dialogue.adviceForIron() {
        when (player.gamemode) {
            PlayerGamemode.HARDCORE_IRONMAN ->
                chatNpc(
                    happy,
                    "You're a ${hc("Hardcore Iron$iw")}. You can downgrade yourself to a " +
                        "${std("standard Iron$iw")} or a normal player if you like.",
                )
            PlayerGamemode.ULTIMATE_IRONMAN ->
                chatNpc(
                    happy,
                    "You're an ${uim("Ultimate Iron$iw")}. You can downgrade yourself to a " +
                        "${std("standard Iron$iw")} or a normal player if you like.",
                )
            else ->
                chatNpc(
                    happy,
                    "You're a ${std("standard Iron$iw")}. You can downgrade yourself to a normal player if you like.",
                )
        }
    }

    private suspend fun Dialogue.adviceForNormal() {
        chatNpc(
            happy,
            "You can choose to become an ${std("Iron$iw")} while you're on Tutorial Island. " +
                "Although you can't do that on this account, you might like to bear it in mind for the future.",
        )
    }

    private suspend fun Dialogue.afterAbout(iron: Boolean) {
        when (
                choice3(
                "I'd like to change my Iron$iw mode.",
                1,
                "Have you any armour for me, please?",
                2,
                "I'm fine, thanks.",
                3,
            )
        ) {
            1 -> if (iron) reviewMode() else changeMode(warnTutorial = false)
            2 -> giveArmour()
            3 -> fine()
        }
    }

    private suspend fun Dialogue.changeMode(warnTutorial: Boolean) {
        chatPlayer(quiz, "I'd like to change my Iron$iw mode.")
        access.ifOpenMainModal(SETUP)
        if (warnTutorial) {
            chatNpc(
                happy,
                "Make sure you've got your Iron$iw settings all sorted before you leave Tutorial Island. " +
                    "Some things can't be changed later.",
            )
        }
    }

    private suspend fun Dialogue.reviewMode() {
        chatPlayer(quiz, "I'd like to review my Iron$iw mode.")
        access.ifOpenMainModal(SETUP)
    }

    private suspend fun Dialogue.giveArmour() {
        chatPlayer(quiz, "Have you any armour for me, please?")
        if (!player.isAnyIronman) {
            chatNpc(neutral, "You're not an Iron$iw. This armour is only for them.")
            return
        }
        val pieces = armourFor(player.gamemode)
        if (pieces.any { it in player.inv || it in player.worn }) {
            chatNpc(happy, "I think you've already got some. Wear it with pride.")
            return
        }
        for (piece in pieces) {
            if (!player.invAdd(player.inv, piece).success) {
                chatNpc(sad, "I'd give you some armour but you don't have enough room in your inventory.")
                return
            }
        }
        chatNpc(happy, "There you go. Wear it with pride.")
    }

    private suspend fun Dialogue.fine() {
        chatPlayer(neutral, "I'm fine, thanks.")
    }

    private val Dialogue.iw: String
        get() = if (access.isBodyTypeA()) "man" else "woman"

    private val Dialogue.iws: String
        get() = if (access.isBodyTypeA()) "men" else "women"

    private fun std(text: String) = "<col=7f0000>$text</col>"

    private fun hc(text: String) = "<col=b10000>$text</col>"

    private fun uim(text: String) = "<col=00007f>$text</col>"

    private fun armourFor(mode: Int): List<String> =
        when (mode) {
            PlayerGamemode.ULTIMATE_IRONMAN ->
                listOf(
                    "obj.ultimate_ironman_helm",
                    "obj.ultimate_ironman_platebody",
                    "obj.ultimate_ironman_platelegs",
                )
            PlayerGamemode.HARDCORE_IRONMAN ->
                listOf(
                    "obj.hardcore_ironman_helm",
                    "obj.hardcore_ironman_platebody",
                    "obj.hardcore_ironman_platelegs",
                )
            else -> listOf("obj.ironman_helm", "obj.ironman_platebody", "obj.ironman_platelegs")
        }

    private companion object {
        private const val NPC = "npc.ironman_tutor_1"
        private const val SETUP = "interface.ironman_setup"
        private val MET = AttributeKey<Boolean>(persistenceKey = "ironman_tutor_adam_met")
    }
}
