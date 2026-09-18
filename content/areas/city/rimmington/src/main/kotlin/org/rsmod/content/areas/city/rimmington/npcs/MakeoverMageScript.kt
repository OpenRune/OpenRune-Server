package org.rsmod.content.areas.city.rimmington.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.table.FacialHairStylesRow
import org.rsmod.api.table.HairStylesRow
import org.rsmod.api.table.HandStylesRow
import org.rsmod.api.table.LeggingStylesRow
import org.rsmod.api.table.ShoeStylesRow
import org.rsmod.api.table.SleeveStylesRow
import org.rsmod.api.table.TorsoStylesRow
import org.rsmod.game.entity.player.Appearance
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Makeover Mage south-west of Falador. The interface only moves its highlights client-side, so
 * the server keeps its own copy of each pick and applies them on Apply or Confirm.
 */
class MakeoverMageScript @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((index, mage) in MAGES.withIndex()) {
            onOpNpc1(mage) {
                vars[VAR_MAGE] = index
                startDialogue(it.npc) { makeoverMage() }
            }
            onOpNpc4(mage) {
                vars[VAR_MAGE] = index
                openMakeover()
            }
            onAiTimer(mage) { npc.queue(SWITCH_QUEUE, 1) }
            onNpcQueue(npcType(mage), SWITCH_QUEUE) { switchForm() }
        }

        onIfModalButton("component.makeover_mage:bodytype_a") {
            selectBodyType(Appearance.BODY_TYPE_A)
        }
        onIfModalButton("component.makeover_mage:bodytype_b") {
            selectBodyType(Appearance.BODY_TYPE_B)
        }
        onIfModalButton("component.makeover_mage:colour") {
            SKIN_COLOUR_ORDER.getOrNull(it.comsub)?.let { colour ->
                vars[VAR_COLOUR] = colour
                vars["varp.if2"] = colour
            }
        }
        onIfModalButton("component.makeover_mage:he_him") {
            selectPronoun(Appearance.PRONOUN_HE)
        }
        onIfModalButton("component.makeover_mage:she_her") {
            selectPronoun(Appearance.PRONOUN_SHE)
        }
        onIfModalButton("component.makeover_mage:they_them") {
            selectPronoun(Appearance.PRONOUN_THEY)
        }
        onIfModalButton("component.makeover_mage:apply_button") { applyMakeover() }
        onIfModalButton("component.makeover_mage:confirm_button") {
            applyMakeover()
            ifClose()
            val mage = MAGES[vars[VAR_MAGE]]
            startDialogue { afterMakeover(mage) }
        }
    }

    private suspend fun StandardNpcAccess.switchForm() {
        anim("seq.human_castentangle_staff")
        spotanim("spotanim.curse_impact", delay = 15, height = 124)
        delay(2)
        val female = npc.visType.id == "npc.makeover_mage_female".asRSCM(RSCMType.NPC)
        npc.transmog(npcType(if (female) MAGES[0] else MAGES[1]), Int.MAX_VALUE)
        say(if (random.randomBoolean()) "Ahah!" else "Ooh!")
        aiTimer(random.of(SWITCH_MIN_TICKS, SWITCH_MAX_TICKS))
    }

    private fun npcType(internal: String): NpcServerType =
        ServerCacheManager.getNpc(internal.asRSCM(RSCMType.NPC)) ?: error("Missing npc: $internal")

    private suspend fun Dialogue.makeoverMage() {
        chatNpc(
            happy,
            "Hello there! I am known as the Makeover Mage! I have spent many years researching " +
                "magics that can change your physical appearance!",
        )
        chatNpc(
            quiz,
            "I can alter your physical form with my magic, there's no fee. Would you like me to " +
                "perform my magics upon you?",
        )
        makeoverOptions()
    }

    private suspend fun Dialogue.makeoverOptions() {
        val option =
            choice4(
                "Tell me more about this 'makeover'.",
                Option.TellMore,
                "Sure, I'll have a makeover.",
                Option.Makeover,
                "Cool amulet! Can I have one?",
                Option.Amulet,
                "No thanks.",
                Option.Decline,
            )
        when (option) {
            Option.TellMore -> tellMore()
            Option.Makeover -> haveMakeover()
            Option.Amulet -> buyAmulet()
            Option.Decline -> {
                chatPlayer(angry, "No thanks.")
                chatNpc(sad, "Ehhh... suit yourself.")
            }
        }
    }

    private suspend fun Dialogue.tellMore() {
        chatPlayer(quiz, "Tell me more about this 'make-over'.")
        chatNpc(
            happy,
            "Why, of course! Basically, and I will try and explain this so that you will " +
                "understand it correctly,",
        )
        chatNpc(
            happy,
            "I use my secret magical technique to melt your body down into a puddle of its elements.",
        )
        chatNpc(
            happy,
            "When I have broken down all trace of your body, I then rebuild it into the form I am " +
                "thinking of!",
        )
        chatNpc(neutral, "Or, you know, somewhere vaguely close enough anyway.")
        chatPlayer(worried, "Uh... that doesn't sound particularly safe to me...")
        chatNpc(
            happy,
            "It's as safe as houses! Why, I have only had thirty-six major accidents this month!",
        )
        chatNpc(quiz, "So what do you say? Feel like a change? There's no fee.")
        makeoverOptions()
    }

    private suspend fun Dialogue.haveMakeover() {
        chatPlayer(happy, "Sure, I'll have a makeover.")
        chatNpc(
            happy,
            "Good choice, good choice. You wouldn't want to carry on looking like that, I'm sure!",
        )
        access.openMakeover()
    }

    private suspend fun Dialogue.buyAmulet() {
        chatPlayer(quiz, "Cool amulet! Can I have one?")
        chatNpc(
            neutral,
            "No problem, but please remember that the amulet I will sell you is only a copy of my " +
                "own. It contains no magical powers, and as such will only cost you 100 coins.",
        )
        when {
            access.inv.count("obj.coins") < AMULET_PRICE -> {
                chatPlayer(sad, "Oh, I don't have enough money for that.")
            }
            access.inv.isFull() && access.inv.count("obj.coins") != AMULET_PRICE -> {
                chatPlayer(neutral, "No way! That's far too expensive.")
                chatNpc(neutral, "That's fair enough, my jewellery is not to everyone's taste.")
            }
            choice2("Sure, here you go.", true, "No way! That's far too expensive.", false) -> {
                chatPlayer(happy, "Sure, here you go.")
                access.invDel(access.inv, "obj.coins", AMULET_PRICE)
                access.invAdd(access.inv, AMULET, 1)
                objbox(AMULET, "You receive an amulet in exchange for 100 coins.")
            }
            else -> chatPlayer(neutral, "I don't have room to hold it. Maybe another time.")
        }
        chatNpc(
            quiz,
            "Anyway, would you like me to alter your physical form? For you, I'll do it for free!",
        )
        makeoverOptions()
    }

    private enum class Option {
        TellMore,
        Makeover,
        Amulet,
        Decline,
    }

    private fun ProtectedAccess.openMakeover() {
        val appearance = player.appearance
        vars[VAR_BODY_TYPE] = appearance.bodyType
        vars[VAR_COLOUR] = appearance.coloursSnapshot()[SKIN_COLOUR_SLOT].toInt()
        vars[VAR_PRONOUN] = appearance.pronoun
        vars["varp.if1"] = appearance.bodyType
        vars["varp.if2"] = vars[VAR_COLOUR]
        vars["varp.if3"] = appearance.pronoun

        ifOpenMainModal(INTERFACE)
        ifSetEvents("component.makeover_mage:colour", SKIN_COLOUR_ORDER.indices, IfEvent.Op1)
    }

    private fun ProtectedAccess.selectBodyType(bodyType: Int) {
        vars[VAR_BODY_TYPE] = bodyType
        vars["varp.if1"] = bodyType
    }

    private fun ProtectedAccess.selectPronoun(pronoun: Int) {
        vars[VAR_PRONOUN] = pronoun
        vars["varp.if3"] = pronoun
    }

    private fun ProtectedAccess.applyMakeover() {
        val appearance = player.appearance
        val bodyType = vars[VAR_BODY_TYPE]
        if (bodyType != appearance.bodyType) {
            convertIdentKits(appearance, toBodyTypeB = bodyType == Appearance.BODY_TYPE_B)
            appearance.bodyType = bodyType
        }
        appearance.setColour(SKIN_COLOUR_SLOT, vars[VAR_COLOUR])
        appearance.pronoun = vars[VAR_PRONOUN]
    }

    private fun convertIdentKits(appearance: Appearance, toBodyTypeB: Boolean) {
        val kits = appearance.identKitSnapshot()
        for ((slot, styles) in KIT_STYLES.withIndex()) {
            val current = kits[slot].toInt()
            val style =
                styles.firstOrNull { (a, b) -> current == if (toBodyTypeB) a else b }
                    ?: styles.firstOrNull()
                    ?: continue
            appearance.setIdentKit(slot, if (toBodyTypeB) style.second else style.first)
        }
    }

    private suspend fun Dialogue.afterMakeover(mage: String) {
        suspend fun mage(mesanim: MesAnimType, text: String) =
            chatNpcSpecific(MAGE_TITLE, mage, mesanim, text)

        when (access.random.of(0, 3)) {
            0 -> {
                mage(shocked, "Woah!")
                chatPlayer(confused, "What?")
                mage(happy, "You still look human!")
            }
            1 -> {
                mage(
                    quiz,
                    "Hmm... you didn't feel any unexpected growths anywhere around your head " +
                        "just then did you?",
                )
                chatPlayer(confused, "Uh... no...?")
                mage(happy, "Good, good! I was worried for a second there!")
            }
            2 -> {
                mage(happy, "Whew! That was lucky!")
                chatPlayer(confused, "What was?")
                mage(happy, "Nothing! It's all fine! You seem alive anyway!")
            }
            else ->
                mage(
                    happy,
                    "Two arms... two legs... one head... it seems that spell finally worked okay!",
                )
        }
        chatPlayer(confused, "Uh... Thanks, I guess.")
    }

    private companion object {
        const val INTERFACE = "interface.makeover_mage"
        const val VAR_BODY_TYPE = "varbit.rimmington_makeover_bodytype"
        const val VAR_COLOUR = "varbit.rimmington_makeover_colour"
        const val VAR_MAGE = "varbit.rimmington_makeover_mage"
        const val VAR_PRONOUN = "varbit.rimmington_makeover_pronoun"
        const val SKIN_COLOUR_SLOT = 4
        const val AMULET = "obj.postie_pete_yin_yang"
        const val AMULET_PRICE = 100

        const val MAGE_TITLE = "Makeover Mage"
        const val SWITCH_QUEUE = "queue.generic_queue1"
        const val SWITCH_MIN_TICKS = 14
        const val SWITCH_MAX_TICKS = 20

        val MAGES = listOf("npc.makeover_mage", "npc.makeover_mage_female")

        /** The skin colour behind each swatch of the colour column, in the client's enum order. */
        val SKIN_COLOUR_ORDER = listOf(7, 0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13)

        /** Body type A and B kits of every style, indexed by identity kit slot. */
        val KIT_STYLES: List<List<Pair<Int, Int>>> by lazy {
            listOf(
                HairStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
                FacialHairStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
                TorsoStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
                SleeveStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
                HandStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
                LeggingStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
                ShoeStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            )
        }
    }
}
