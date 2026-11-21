package org.rsmod.content.quest.area.varrock.dragonslayer

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.CRANDOR_MAP
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.LOZAR_PIECE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.ownsAny
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.wormbrainInstructions
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Wormbrain, the goblin thief in Port Sarim jail who holds Lozar's map piece. */
class DragonSlayerWormbrain @Inject constructor(private val quest: DragonSlayerQuest) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(WORMBRAIN) { startDialogue(it.npc) { wormbrain() } }
        onApNpc1(WORMBRAIN) {
            if (isWithinDistance(it.npc, ACROSS_BARS)) {
                startDialogue(it.npc) { wormbrain() }
            } else {
                apRange(ACROSS_BARS)
            }
        }
    }

    private suspend fun Dialogue.wormbrain() {
        chatNpc(angry, "Whut you want?")
        val options = buildList {
            if (quest.wantsLozarPiece(player)) {
                add("I believe you've got a piece of a map that I need." to Topic.Map)
            }
            add("What are you in for?" to Topic.Crime)
            add("Sorry, thought this was a zoo." to Topic.Zoo)
        }
        when (menu(options)) {
            Topic.Map -> mapPiece()
            Topic.Crime -> {
                chatPlayer(quiz, "What are you in for?")
                chatNpc(confused, "Me not sure. Me pick some stuff up and take it away.")
                chatPlayer(quiz, "Well, did the stuff belong to you?")
                chatNpc(confused, "Umm...no.")
                chatPlayer(neutral, "Well, that would be why then.")
                chatNpc(neutral, "Oh, right.")
            }
            Topic.Zoo -> chatPlayer(laugh, "Sorry, thought this was a zoo.")
        }
    }

    private suspend fun Dialogue.mapPiece() {
        chatPlayer(neutral, "I believe you've got piece of a map that I need.")
        chatNpc(neutral, "So? Why should I be giving it to you? What you do for Wormbrain?")
        while (true) {
            val topic =
                menu(
                    "I'm not going to do anything for you. Forget it." to Bargain.Refuse,
                    "I'll let you live. I could just kill you." to Bargain.Threaten,
                    "I suppose I could pay you for the map piece..." to Bargain.Pay,
                    "Where did you get the map piece from?" to Bargain.Where,
                )
            when (topic) {
                Bargain.Refuse -> {
                    chatPlayer(angry, "I'm not going to do anything for you. Forget it.")
                    chatNpc(angry, "Be dat way then.")
                    return
                }
                Bargain.Threaten -> {
                    chatPlayer(angry, "I'll let you live. I could just kill you.")
                    chatNpc(laugh, "Ha! Me in here and you out dere. You not get map piece.")
                }
                Bargain.Pay -> {
                    pay()
                    return
                }
                Bargain.Where -> whereFrom()
            }
        }
    }

    private suspend fun Dialogue.pay() {
        chatPlayer(neutral, "I suppose I could pay you for the map piece. Say, 500 coins?")
        chatNpc(angry, "Me not stoopid, it worth at least 10,000 coins!")
        val accept =
            menu("You must be joking! Forget it." to false, "Alright then, 10,000 it is." to true)
        if (!accept) {
            chatPlayer(angry, "You must be joking! Forget it.")
            chatNpc(angry, "Fine, you not get map piece.")
            return
        }
        chatPlayer(neutral, "Alright then, 10,000 coins it is.")
        if (player.inv.count(COINS) < PRICE) {
            chatPlayer(sad, "Oops, I don't have enough on me.")
            chatNpc(angry, "Comes back when you has enough.")
            return
        }
        if (!quest.wantsLozarPiece(player)) {
            return
        }
        if (!player.exchange(take = listOf(COINS to PRICE), give = listOf(LOZAR_PIECE to 1))) {
            mesbox("You don't have enough inventory space.")
            return
        }
        objbox(LOZAR_PIECE, "You buy the map piece from Wormbrain.")
        chatNpc(happy, "Fank you very much! Now me can bribe da guards, hehehe.")
    }

    private suspend fun Dialogue.whereFrom() {
        chatPlayer(quiz, "Where did you get the map piece from?")
        chatNpc(laugh, "We rob house of stupid wizard. She very old, not put up much fight at all. Hahaha!")
        chatPlayer(laugh, "Uh... Hahaha.")
        chatNpc(
            neutral,
            "Her house full of pictures of a city on island and old pictures of people. Me not " +
                "recognise island.",
        )
        chatNpc(
            neutral,
            "Me find map piece. Me not know what it is, but it in locked box so me figure it " +
                "important.",
        )
        chatNpc(
            sad,
            "But, by the time me get box open, other goblins gone. Then me not run fast enough " +
                "and guards catch me.",
        )
        chatNpc(quiz, "But now you want map piece so must be special! What you do for me to get it?")
    }

    private enum class Topic {
        Map,
        Crime,
        Zoo,
    }

    private enum class Bargain {
        Refuse,
        Threaten,
        Pay,
        Where,
    }

    companion object {
        const val WORMBRAIN = "npc.wormbrain"
        private const val COINS = "obj.coins"
        private const val PRICE = 10_000
        private const val ACROSS_BARS = 2
    }
}

class WormbrainAttackHook @Inject constructor(private val objRepo: ObjRepository) :
    NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(DragonSlayerWormbrain.WORMBRAIN)) {
            return NpcAttackValidateResult.Pass
        }
        if (player.ownsAny(LOZAR_PIECE, CRANDOR_MAP)) {
            return NpcAttackValidateResult.Deny(ALREADY_TAKEN)
        }
        if (hasDroppedPieceFor(objRepo, player, npc.coords)) {
            return NpcAttackValidateResult.Deny(JUST_BEATEN)
        }
        return NpcAttackValidateResult.Pass
    }

    private companion object {
        const val ALREADY_TAKEN =
            "You have already taken Wormbrain's map piece. There is no use in beating him up further."
        const val JUST_BEATEN = "You have just beaten Wormbrain up. Give the poor goblin a break."
    }
}

/** Beating Wormbrain makes him drop Lozar's piece in his cell, for the attacker only. */
class WormbrainKillHook
@Inject
constructor(private val quest: DragonSlayerQuest, private val objRepo: ObjRepository) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        val hero = context.hero
        if (!npc.isType(DragonSlayerWormbrain.WORMBRAIN) || !quest.wantsLozarPiece(hero)) {
            return
        }
        if (hasDroppedPieceFor(objRepo, hero, context.dropCoords)) {
            return
        }
        npc.say("Ow!")
        hero.mes("Wormbrain drops a map piece on the floor.")
        objRepo.add(LOZAR_PIECE, context.dropCoords, PIECE_DURATION, receiver = hero)
    }

    private companion object {
        const val PIECE_DURATION = 200
    }
}

private fun hasDroppedPieceFor(objRepo: ObjRepository, player: Player, around: CoordGrid): Boolean {
    val piece = LOZAR_PIECE.asRSCM(RSCMType.OBJ)
    for (dx in -CELL_RADIUS..CELL_RADIUS) {
        for (dz in -CELL_RADIUS..CELL_RADIUS) {
            val tile = around.translate(dx, dz, 0)
            if (objRepo.findAll(tile).any { it.type == piece && it.isVisibleTo(player) && it.isPrivate }) {
                return true
            }
        }
    }
    return false
}

private const val CELL_RADIUS = 2

internal fun DragonSlayerQuest.wantsLozarPiece(player: Player): Boolean =
    isCollectingMap(player) &&
        player.wormbrainInstructions == 0 &&
        !player.ownsAny(LOZAR_PIECE, CRANDOR_MAP)
