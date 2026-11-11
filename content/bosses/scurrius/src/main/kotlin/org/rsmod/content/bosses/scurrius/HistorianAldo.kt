package org.rsmod.content.bosses.scurrius

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.other.special.weapons.scripts.RatBoneWeapons
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HistorianAldo @Inject constructor(private val weapons: RatBoneWeapons) : PluginScript() {
    private var Player.toldOfLamps by boolVarBit("varbit.rat_boss_biologist_npc")

    override fun ScriptContext.startup() {
        for (aldo in ALDO_VARIANTS) {
            onOpNpc1(aldo) { startDialogue(it.npc) { talk() } }
            onOpNpcU(aldo) { useItem(it.npc, it.objType.internalName) }
        }
        onOpNpc3(ALDO_TRADER) { startDialogue(it.npc) { trade() } }
        onOpHeld1(LAMP) { rubLamp(it.slot) }
    }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: String) {
        if (obj != RatBoneWeapons.SPINE) {
            mes("Nothing interesting happens.")
            return
        }
        startDialogue(npc) {
            chatNpc(
                happy,
                "Ah, a Scurrius spine! A captivating subject indeed. I'll eagerly take them off " +
                    "your hands, and rest assured, you will be well rewarded.",
            )
            trade()
        }
    }

    private suspend fun Dialogue.talk() {
        chatNpc(happy, "Hello there.")
        while (true) {
            val option =
                choice4(
                    "What's through those broken bars?",
                    1,
                    "Can you tell me about Scurrius' spine?",
                    2,
                    "Can you make a bone weapon for me please?",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> brokenBars()
                2 -> aboutSpine()
                3 -> if (!requestWeapon()) return
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.brokenBars() {
        chatPlayer(quiz, "What's through those broken bars?")
        chatNpc(
            happy,
            "Well, my intrepid friend, through those broken bars lies a most remarkable " +
                "discovery! I named it Scurrius, The Rat King!",
        )
        chatNpc(
            happy,
            "This is no ordinary rat. It is a magnificent creature of evolution, forged by the " +
                "unwitting contributions of adventurers who entered its cave.",
        )
        chatNpc(
            happy,
            "It absorbed their combat abilities, transforming itself into an obscene monster. " +
                "Nature's response to intrusion, you might say! How utterly fascinating!",
        )
        chatNpc(
            neutral,
            "Ahem, anyway you'll be absolutely fine, of course! Your victory awaits, I'm sure. I " +
                "shall observe your progress with interest... for science, you know.",
        )
        chatNpc(
            neutral,
            "Oh, and if you happen to come across Scurrius' spine, please bring it to me.",
        )
    }

    private suspend fun Dialogue.aboutSpine() {
        chatPlayer(quiz, "Excuse me, can you tell me about Scurrius' spine?")
        chatNpc(happy, "Of course! Scurrius spines are invaluable for my research.")
        chatNpc(
            neutral,
            "I can use them to craft a potent bone weapon which will aid you when battling the " +
                "Rat King.",
        )
        chatNpc(
            neutral,
            "Although you can create the weapons yourself, provided you have the required " +
                "expertise and appropriate materials.",
        )
        chatNpc(
            neutral,
            "If you don't want a bone weapon, I can also trade you a lamp I found during my " +
                "research expedition - one for every Scurrius spine you bring to me.",
        )
        player.toldOfLamps = true
    }

    /** Returns false once a weapon has been handed over, which ends the conversation. */
    private suspend fun Dialogue.requestWeapon(): Boolean {
        chatPlayer(quiz, "Can you make a bone weapon for me please?")
        chatNpc(
            neutral,
            "You'll need to bring a Scurrius spine and either a Rune mace, a Yew shortbow, or a " +
                "Battlestaff. An additional 1000 chaos runes are needed if you want to create a " +
                "Bone staff.",
        )
        chatNpc(neutral, "I'll also charge 50,000 coins to craft the weapon.")
        if (!canAffordAny()) {
            chatNpc(neutral, "Come back when you have all the necessary materials.")
            return true
        }
        chooseWeapon()
        return false
    }

    private suspend fun Dialogue.chooseWeapon() {
        val weapon =
            choice3(
                "Bone mace.",
                RatBoneWeapons.MACE,
                "Bone shortbow.",
                RatBoneWeapons.BOW,
                "Bone staff.",
                RatBoneWeapons.STAFF,
                title = "Which bone weapon do you want?",
            )
        chatNpc(happy, "Okay, I'll make you the ${weapon.productName}.")
        val created = with(weapons) { access.create(weapon, FEE) }
        if (!created) {
            chatNpc(neutral, "Come back when you have all the necessary materials.")
            return
        }
        objbox(weapon.product, "Aldo handed the ${weapon.productName} to you.")
    }

    private suspend fun Dialogue.trade() {
        val spines = invTotal(player.inv, RatBoneWeapons.SPINE)
        if (spines <= 0) {
            chatNpc(
                neutral,
                "Ah, I'll need one of Scurrius' spines to do that. Perhaps you could fetch me one?",
            )
            return
        }
        if (canAffordAny()) {
            val weapon =
                choice2(
                    "Weapon.",
                    true,
                    "Combat lamp.",
                    false,
                    title = "Trade your Scurrius spines for a bone weapon or combat lamp?",
                )
            if (weapon) {
                chooseWeapon()
                return
            }
        }
        offerLamps()
    }

    private suspend fun Dialogue.offerLamps() {
        val spines = invTotal(player.inv, RatBoneWeapons.SPINE)
        val title =
            if (spines == 1) {
                "Trade Scurrius' spine for a combat lamp?"
            } else {
                "Trade ALL your Scurrius spines for combat lamps?"
            }
        if (!choice2("Yes.", true, "No.", false, title = title)) {
            return
        }
        val count = invTotal(player.inv, RatBoneWeapons.SPINE)
        if (count <= 0 || access.invReplace(player.inv, RatBoneWeapons.SPINE, count, LAMP).failure) {
            return
        }
        objbox(
            LAMP,
            if (count == 1) {
                "Aldo gives you an Antique lamp for Scurrius' spine."
            } else {
                "Aldo gives you some Antique lamps for Scurrius' spines."
            },
        )
    }

    private fun Dialogue.canAffordAny(): Boolean =
        with(weapons) { RatBoneWeapons.ALL.any { access.hasMaterials(it, FEE) } }

    private suspend fun ProtectedAccess.rubLamp(slot: Int) {
        val stat = chooseLampStat() ?: return
        if (!inv[slot].isType(LAMP) || invDel(inv, LAMP, 1, slot = slot).failure) {
            return
        }
        val xp = if (stat.internal == "stat.prayer") PRAYER_LAMP_XP else LAMP_XP
        statAdvance(stat.internal, xp.toDouble())
        soundSynth("synth.found_gem")
        objbox(
            LAMP,
            "<col=0000ff>Your wish has been granted!</col><br>You have been awarded " +
                "${"%,d".format(xp)} ${stat.name} XP!",
        )
    }

    private suspend fun ProtectedAccess.chooseLampStat(): LampStat? {
        var page = 0
        while (true) {
            val picked =
                if (page == 0) {
                    choice5(
                        "Attack.", LampStat("stat.attack", "Attack"),
                        "Strength.", LampStat("stat.strength", "Strength"),
                        "Defence.", LampStat("stat.defence", "Defence"),
                        "Prayer.", LampStat("stat.prayer", "Prayer"),
                        "More options...", null,
                        title = LAMP_TITLE,
                    )
                } else {
                    choice4(
                        "Ranged.", LampStat("stat.ranged", "Ranged"),
                        "Magic.", LampStat("stat.magic", "Magic"),
                        "Hitpoints.", LampStat("stat.hitpoints", "Hitpoints"),
                        "Previous options...", null,
                        title = LAMP_TITLE,
                    )
                }
            if (picked != null) {
                return picked
            }
            page = 1 - page
        }
    }

    private class LampStat(val internal: String, val name: String)

    private companion object {
        val ALDO_VARIANTS = listOf("npc.rat_boss_biologist_op1", "npc.rat_boss_biologist_op2")
        const val ALDO_TRADER = "npc.rat_boss_biologist_op2"
        const val LAMP = "obj.rat_boss_lamp"
        const val LAMP_TITLE = "Choose the skill you wish to gain XP in."
        const val LAMP_XP = 5_000
        const val PRAYER_LAMP_XP = 3_500
        const val FEE = 50_000
    }
}
