package org.rsmod.content.other.pets.cats

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.owner.clearSpawnOwner
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpHeld5
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpIfPresent
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CatScript
@Inject
constructor(
    private val followers: PetFollowers,
    private val care: CatCare,
    private val chase: CatChase,
    private val dialogue: CatDialogue,
    private val death: NpcDeath,
    private val players: PlayerList,
) : PluginScript(), PlayerPostTickHook {
    override fun ScriptContext.startup() {
        for (cat in Cats.all) {
            val type = ServerCacheManager.getNpc(cat.npc.asRSCM(RSCMType.NPC)) ?: error("Missing npc: ${cat.npc}")
            onNpcQueue(type, "queue.death") { catDeath() }
            onOpHeld5(cat.obj) { drop(it.slot, cat) }
            onPetOp(cat.npc, PICK_UP_OP) { pickUp(it, cat) }
            onPetOp(cat.npc, TALK_OP) { talk(it, cat) }
            onPetOpIfPresent(cat.npc, CHASE_OP) { chaseVermin(it, cat) }
            onPetOpIfPresent(cat.npc, INTERACT_OP) { interact(it, cat) }
            onPetOpU(cat.npc) { useItem(it.npc, cat, it.objType) }
        }
    }

    override fun onPostTick(player: Player) {
        if (!player.loggingOut) {
            care.tick(player)
        }
    }

    private suspend fun StandardNpcAccess.catDeath() {
        val owner = npc.spawnOwner.resolve(players)
        death.deathNoDrops(this)
        npc.clearSpawnOwner()
        if (owner != null) {
            followers.dismiss(owner)
            owner.mes("Your cat has passed away.")
        }
    }

    private fun ProtectedAccess.drop(slot: Int, cat: CatForm) {
        if (followers.hasFollower(player)) {
            mes("You already have a follower.")
            return
        }
        if (!invDel(inv, cat.obj, count = 1, slot = slot).success) {
            return
        }
        followers.spawn(player, cat.form)
        followers.follower(player)?.say(if (cat.isKitten) "Miaow!" else "Miaooow!")
    }

    private fun ProtectedAccess.pickUp(npc: Npc, cat: CatForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space to pick up your follower.")
            return
        }
        followers.dismiss(player)
        invAdd(inv, cat.obj, 1)
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, cat: CatForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        dialogue.talk(this, npc, cat)
    }

    private suspend fun ProtectedAccess.interact(npc: Npc, cat: CatForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        val title = if (cat.isKitten) "Interact with kitten" else "Interact with cat"
        var option = 0
        startDialogue(npc) {
            option =
                if (cat.isKitten) {
                    choice4("Stroke", 1, "Chase Vermin", 2, "Guess age", 3, "Shoo away", 4, title = title)
                } else {
                    choice3("Stroke", 1, "Chase Vermin", 2, "Shoo away", 4, title = title)
                }
        }
        when (option) {
            1 -> {
                care.stroke(player)
                dialogue.stroke(this, npc, cat)
            }
            2 -> chaseVermin(npc, cat)
            3 -> dialogue.guessAge(this, npc)
            4 -> shoo(npc, cat)
        }
    }

    private suspend fun ProtectedAccess.shoo(npc: Npc, cat: CatForm) {
        if (!dialogue.shoo(this, npc, cat)) {
            mes("You choose not to shoo away the cat.")
            return
        }
        player.say("Shoo cat!")
        npc.say("Meeeooow!")
        mes("The cat has run away.")
        followers.dismiss(player)
    }

    private suspend fun ProtectedAccess.chaseVermin(npc: Npc, cat: CatForm) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        if (chase.cryptRatNearby(npc)) {
            dialogue.cryptRat(this, npc, cat)
            return
        }
        if (cat.stage === CatStage.Overgrown) {
            mes("The cat is just too big to catch vermin.")
            return
        }
        val rat = chase.nearestRat(npc)
        if (rat == null) {
            mes("There aren't any vermin around.")
            return
        }
        chase.chase(this, npc, cat, rat)
    }

    private suspend fun ProtectedAccess.useItem(npc: Npc, cat: CatForm, obj: ItemServerType) {
        if (!followers.requireOwned(this, npc)) {
            return
        }
        val name = if (cat.isKitten) "kitten" else "cat"
        when {
            obj.internalName == BALL_OF_WOOL -> {
                care.play(player)
                npc.say("Purr!")
                startDialogue(npc) {
                    chatPlayer(happy, "That $name loves to play with that ball of wool. I think it is its favourite.")
                }
            }
            obj.internalName in MILK -> {
                if (obj.internalName == BUCKET_OF_MILK) {
                    invReplace(inv, BUCKET_OF_MILK, 1, EMPTY_BUCKET)
                }
                mes("The $name laps up the milk.")
                npc.say("Purpurr")
                care.feed(player)
                if (cat.isHell) {
                    care.fromHell(player)
                    mes("Your hell-cat transforms into an ordinary cat.")
                }
            }
            care.isFood(obj) -> {
                if (!cat.isKitten) {
                    startDialogue(npc) { chatPlayer(neutral, "I don't need to feed the cat anymore.") }
                    return
                }
                invDel(inv, obj.internalName, 1)
                mes("The kitten gobbles up the fish.")
                npc.say("Purr!")
                care.feed(player)
            }
            care.isInedibleFish(obj) -> mes("Nothing interesting happens.")
        }
    }

    private companion object {
        const val PICK_UP_OP = "Pick-up"
        const val TALK_OP = "Talk-to"
        const val CHASE_OP = "Chase"
        const val INTERACT_OP = "Interact"
        const val BALL_OF_WOOL = "obj.ball_of_wool"
        const val BUCKET_OF_MILK = "obj.bucket_milk"
        const val EMPTY_BUCKET = "obj.bucket_empty"
        val MILK = setOf(BUCKET_OF_MILK, "obj.bottomless_milk_bucket_filled")
    }
}
