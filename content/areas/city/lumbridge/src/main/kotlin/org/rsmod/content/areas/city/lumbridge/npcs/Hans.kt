package org.rsmod.content.areas.city.lumbridge.npcs

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Hans : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hans") { hansDialogue(it.npc) }
        onOpNpc3("npc.hans") { hansAgeDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.hansDialogue(npc: Npc) =
        startDialogue(npc) { optionsDialogue(npc) }

    private suspend fun Dialogue.optionsDialogue(npc: Npc) {
        chatNpc(neutral, "Hello. What are you doing here?")
        val choice =
            choice5(
                "I'm looking for whoever is in charge of this place.",
                1,
                "I have come to kill everyone in this castle!",
                2,
                "I don't know. I'm lost. Where am I?",
                3,
                "Can you tell me how long I've been here?",
                4,
                "Nothing.",
                5,
            )
        when (choice) {
            1 -> {
                chatPlayer(neutral, "I'm looking for whoever is in charge of this place.")
                chatNpc(neutral, "Who, the Duke? He's in his study, on the first floor.")
            }
            2 -> {
                chatPlayer(angry, "I have come to kill everyone in this castle!")
                npc.playerEscape(player)
                delay(2)
                npc.say("Help! Help!")
            }
            3 -> {
                chatPlayer(confused, "I don't know. I'm lost. Where am I?")
                chatNpc(
                    neutral,
                    "You are in Lumbridge Castle, in the Kingdom of " +
                        "Misthalin. Across the river, the road leads north to " +
                        "Varrock, and to the west lies Draynor Village.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Can you tell me how long I've been here?")
                chatNpc(
                    laugh,
                    "Ahh, I see all the newcomers arriving in Lumbridge, " +
                        "fresh-faced and eager for adventure. I remember you...",
                )
                playtimeDialogue()
            }
            5 -> {
                chatPlayer(shifty, "Nothing.")
            }
        }
    }

    private suspend fun ProtectedAccess.hansAgeDialogue(npc: Npc) =
        startDialogue(npc) { playtimeDialogue() }

    private suspend fun Dialogue.playtimeDialogue() {
        val minutesPlayed = vars["varp.playtime"] / MINUTE_SCALE_FACTOR
        val (days, hours, minutes) = minutesPlayed.toDayHourMinuteParts()
        val accountAgeDays = player.createdAt.accountAgeInDays()

        val text =
            if (accountAgeDays == null) {
                "You've spent $days days, $hours hours, $minutes minutes in the world."
            } else {
                "You've spent $days days, $hours hours, $minutes minutes in the world " +
                    "since you arrived $accountAgeDays days ago."
            }

        chatNpc(happy, text)
    }

    private fun Int.toDayHourMinuteParts(): Triple<Int, Int, Int> {
        val days = this / MINUTES_PER_DAY
        val remainderAfterDays = this % MINUTES_PER_DAY
        val hours = remainderAfterDays / MINUTES_PER_HOUR
        val minutes = remainderAfterDays % MINUTES_PER_HOUR
        return Triple(days, hours, minutes)
    }

    private fun LocalDateTime?.accountAgeInDays(): Long? {
        val createdAt = this ?: return null
        val days = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now())
        return days.coerceAtLeast(0)
    }

    private companion object {
        private const val MINUTE_SCALE_FACTOR = 100
        private const val MINUTES_PER_HOUR = 60
        private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
