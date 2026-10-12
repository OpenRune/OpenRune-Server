package org.rsmod.content.areas.city.portsarim.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Security_Guard
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:bank_pin] the line for a player with a Bank PIN
class SecurityGuard @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sos_guard") { startDialogue(it.npc) { securityGuard() } }
    }

    private suspend fun Dialogue.securityGuard() {
        chatNpc(quiz, "What brings you here?")
        while (true) {
            val options = buildList {
                add("Who are you?" to Topic.Who)
                if (!hasBook()) {
                    add("If you're a security guard, let's talk about security." to Topic.Security)
                }
                add("I'll be off now." to Topic.Leave)
            }
            when (menu(options)) {
                Topic.Who -> whoAreYou()
                Topic.Security -> accountCheck()
                Topic.Leave -> {
                    chatPlayer(neutral, "I'll be off now.")
                    chatNpc(happy, "Stay safe!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "I'm a security guard. Long ago I used to wander the world, teaching people how to " +
                "protect themselves from security threats, and giving out books of advice.",
        )
        chatNpc(
            neutral,
            "I've retired now. They tell me there's some flashy poser in a vampyre outfit doing " +
                "my old job now, and even giving out prizes to people he thinks are worthy!",
        )
        chatNpc(
            angry,
            "In my day, people didn't need prizes for taking security seriously! Honestly, I " +
                "don't know what's the matter with young people these days...",
        )
        chatNpc(
            neutral,
            "... but if fear of XP waste is what people need to make them protect themselves " +
                "properly, I suppose that's how it'll have to be.",
        )
    }

    private suspend fun Dialogue.accountCheck() {
        chatNpc(
            happy,
            "It's nice to meet someone who takes this stuff seriously! Alright, I'll have a look " +
                "over your account...",
        )
        chatNpc(
            happy,
            "Well, you're off to a good start - I see you have a Jagex Account, so you should " +
                "have the latest protection.",
        )
        chatNpc(
            neutral,
            "Jagex Accounts protect you via two-factor authentication - anyone trying to steal " +
                "your Jagex Account would need get past that second check too, rather than just " +
                "learning your password.",
        )
        chatNpc(
            neutral,
            "For additional protection, you can use a mobile authenticator and backup codes too.",
        )
        chatNpc(
            neutral,
            "Make sure you keep your email account properly secure too, though - good email " +
                "providers will offer two- factor authentication options.",
        )
        chatNpc(
            neutral,
            "Aside from that, I see you don't have a Bank PIN. Next time you visit a bank, talk " +
                "to a banker and ask to check your PIN settings.",
        )
        player.invAddOrDrop(objRepo, SECURITY_BOOK)
        chatNpc(happy, "Here's a security book for you.")
    }

    private fun Dialogue.hasBook(): Boolean =
        SECURITY_BOOK in player.inv || SECURITY_BOOK in access.bank

    private enum class Topic {
        Who,
        Security,
        Leave,
    }

    private companion object {
        const val SECURITY_BOOK = "obj.sos_security_book"
    }
}
