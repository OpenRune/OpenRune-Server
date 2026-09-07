package org.rsmod.content.quest.area.varrock.gertrudescat

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class GertrudesCatQuest :
    QuestScript(
        "quest_gertrudescat",
        "varp.fluffs",
        rewards {
            xp("stat.cooking", CookingXpReward)
            item(CHOCOLATE_CAKE)
            item(STEW)
            extra("A kitten")
        },
        ItemRewardDisplay(KITTEN_DISPLAY),
        questVarbit = "varbit.gertrudes_cat_progress",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "Gertrude's Cat end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Gertrude</col> in her house <col=800000>west of Varrock</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = stage(player.player)
            val metFluffs = player.player.metFluffs
            val toldAboutSardines = player.player.toldAboutSardines
            description(
                "<red>Gertrude</red> west of Varrock has lost her beloved cat <red>Fluffs</red> " +
                    "and has asked me to find her."
            )
            objective(
                "Her sons <red>Shilop</red> and <red>Wilough</red> saw Fluffs last. They should " +
                    "be in <red>Varrock's market place</red>."
            ) {
                visibleWhen { stage == STAGE_STARTED && !metFluffs }
            }
            objective(
                "For <red>$KidsFee coins</red> the boys told me that Fluffs followed them to " +
                    "their secret play area, the <red>lumber yard</red> north-east of Varrock past " +
                    "the Jolly Boar Inn. I can get in through the broken fence."
            ) {
                visibleWhen { stage == STAGE_PAID_KIDS && !metFluffs }
            }
            objective(
                "I found Fluffs upstairs in the lumber yard's shed, but she hisses when I try to " +
                    "pick her up. Maybe she is thirsty. A <red>bucket of milk</red> might help."
            ) {
                visibleWhen { stage in STAGE_STARTED..STAGE_PAID_KIDS && metFluffs }
            }
            objective(
                if (toldAboutSardines) {
                    "Fluffs drank the milk but still won't leave. Gertrude says she loves " +
                        "<red>doogle sardines</red>: a raw sardine seasoned with doogle leaves " +
                        "from the woods behind her house."
                } else {
                    "Fluffs drank the milk but still won't leave. Maybe she is hungry. " +
                        "<red>Gertrude</red> might know what she likes to eat."
                }
            ) {
                visibleWhen { stage == STAGE_GAVE_MILK }
            }
            objective(
                "Fluffs ate the sardine but still won't leave. I can hear <red>kittens " +
                    "mewing</red> from the crates around the lumber yard. I should search them."
            ) {
                visibleWhen { stage == STAGE_GAVE_SARDINE }
                hasItem(
                    "gertrudekittens",
                    "I found Fluffs' kitten in one of the crates. I should take it up to her.",
                )
            }
            objective("Fluffs ran home with her kitten. I should tell <red>Gertrude</red>.") {
                visibleWhen { stage == STAGE_KITTEN_RETURNED }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "Gertrude had lost her cat Fluffs. Her sons told me, for a price, that Fluffs " +
                    "had followed them to the old lumber yard north-east of Varrock."
            )
            line(
                "Fluffs wouldn't leave until I gave her milk and a doogle sardine, and even then " +
                    "she stayed until I found her kitten in one of the crates and brought it to her."
            )
            line(
                "Gertrude was so grateful that she gave me a kitten to raise, along with a " +
                    "chocolate cake and a stew."
            )
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    /**
     * Gertrude's dialogue from the moment she is first asked until Fluffs is reunited with her
     * kitten. Returns `false` once the quest is ready to finish or already finished; the varrock
     * area's Gertrude handles that because the reward kitten belongs to the pets module, which
     * itself depends on this module.
     */
    suspend fun Dialogue.gertrudeDialogue(): Boolean {
        when (stage(player)) {
            0 -> beforeQuest()
            STAGE_STARTED,
            STAGE_PAID_KIDS -> lookingForFluffs()
            STAGE_GAVE_MILK -> sardineAdvice()
            STAGE_GAVE_SARDINE -> afterSardine()
            else -> return false
        }
        return true
    }

    private suspend fun Dialogue.beforeQuest() {
        chatPlayer(quiz, "Hello, are you okay?")
        chatNpc(angry, "Do I look okay? Those kids drive me crazy.")
        chatNpc(sad, "I'm sorry. It's just that I've lost her.")
        chatPlayer(quiz, "Lost who?")
        chatNpc(sad, "Fluffs. Poor Fluffs. She never hurt anyone.")
        chatPlayer(quiz, "Who's Fluffs?")
        chatNpc(
            sad,
            "My beloved feline friend, Fluffs. She's been purring by my side for almost a decade. " +
                "Please, could you go and search for her while I look after the kids?",
        )
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Sorry, I'm too busy to play pet rescue.")
            chatNpc(sad, "Well, okay then. I'll have to find someone else.")
            return
        }
        chatPlayer(happy, "Well, I suppose I could.")
        chatNpc(happy, "Really? Thank you so much! I really have no idea where she could be!")
        chatNpc(
            neutral,
            "I think my sons, Shilop and Wilough, saw the cat last. They'll be out in the " +
                "market place.",
        )
        chatPlayer(neutral, "Alright then, I'll see what I can do.")
        quest.advanceQuestStage(access)
    }

    private suspend fun Dialogue.lookingForFluffs() {
        if (player.metFluffs) {
            sardineAdvice()
            return
        }
        chatPlayer(happy, "Hello Gertrude.")
        chatNpc(quiz, "Have you seen my poor Fluffs?")
        chatPlayer(sad, "I'm afraid not.")
        chatNpc(quiz, "What about Shilop?")
        chatPlayer(neutral, "No sign of him either.")
        chatNpc(confused, "Hmmm... strange. He should be at the market.")
    }

    private suspend fun Dialogue.sardineAdvice() {
        chatPlayer(happy, "Hello again.")
        chatNpc(quiz, "Hello. How's it going? Any luck?")
        chatPlayer(happy, "Yes, I've found Fluffs!")
        chatNpc(happy, "Well, well, you are clever! Did you bring her back?")
        chatPlayer(worried, "Well, that's the thing. She refuses to leave.")
        chatNpc(
            worried,
            "Oh dear, oh dear! Maybe she's just hungry. She loves doogle sardines, but I'm all out.",
        )
        chatPlayer(quiz, "Doogle sardines?")
        chatNpc(
            neutral,
            "Yes, raw sardines seasoned with doogle leaves. Unfortunately I've used all my doogle " +
                "leaves, but you may find some in the woods out the back.",
        )
        player.toldAboutSardines = true
    }

    private suspend fun Dialogue.afterSardine() {
        chatPlayer(happy, "Hi!")
        chatNpc(quiz, "Hey, traveller. Did Fluffs eat the sardines?")
        chatPlayer(neutral, "Yeah, she loved them, but she still won't leave.")
        chatNpc(confused, "Well, that is strange. There must be a reason.")
    }

    companion object {
        const val STAGE_STARTED = 1
        const val STAGE_PAID_KIDS = 2
        const val STAGE_GAVE_MILK = 3
        const val STAGE_GAVE_SARDINE = 4
        const val STAGE_KITTEN_RETURNED = 5
        const val STAGE_COMPLETE = 6

        const val KITTEN_DISPLAY = "obj.kittenobject"
        const val CHOCOLATE_CAKE = "obj.chocolate_cake"
        const val STEW = "obj.stew"
        const val FLUFFS_KITTEN = "obj.gertrudekittens"
        const val SEASONED_SARDINE = "obj.seasoned_sardine"
        const val RAW_SARDINE = "obj.raw_sardine"
        const val DOOGLE_LEAVES = "obj.doogleleaves"
        const val BUCKET_OF_MILK = "obj.bucket_milk"
        const val BUCKET_EMPTY = "obj.bucket_empty"

        const val KidsFee = 100
        private const val CookingXpReward = 1525.0
    }
}
