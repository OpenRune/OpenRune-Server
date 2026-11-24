package org.rsmod.content.quest.mudfoot

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.other.pets.ExternalFollowerForms
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.PetForm
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

private var Player.followerObjId: Int by intVarp("varp.follower_obj")

/** Mudfoot as a follower: he uses the same follower varps and movement as pets. */
@Singleton
class MudfootFollower
@Inject
constructor(private val followers: PetFollowers, private val forms: ExternalFollowerForms) {
    private val hammerForm =
        PetForm(
            objId = HAMMER_OBJ.asRSCM(RSCMType.OBJ),
            npcId = HAMMER_NPC.asRSCM(RSCMType.NPC),
            runs = true,
        )

    private val spearForm =
        PetForm(
            objId = SPEAR_OBJ.asRSCM(RSCMType.OBJ),
            npcId = SPEAR_NPC.asRSCM(RSCMType.NPC),
            runs = true,
        )

    fun register() {
        forms.register(hammerForm)
        forms.register(spearForm)
    }

    fun isFollowing(player: Player): Boolean =
        player.followerObjId == hammerForm.objId || player.followerObjId == spearForm.objId

    /** True when another follower (a pet) is already out, which stops Mudfoot joining. */
    fun hasOtherFollower(player: Player): Boolean =
        followers.hasFollower(player) && !isFollowing(player)

    fun follower(player: Player): Npc? = if (isFollowing(player)) followers.follower(player) else null

    fun isFollowerOf(npc: Npc, player: Player): Boolean =
        isFollowing(player) && followers.isFollowerOf(npc, player)

    fun start(player: Player, spear: Boolean) {
        player.mudfootVis = MudfootVis.FOLLOWING
        followers.spawn(player, if (spear) spearForm else hammerForm)
    }

    /** Marks Mudfoot as following without spawning him; the pets tick spawns him on login. */
    fun resumeOnLogin(player: Player, spear: Boolean) {
        player.mudfootVis = MudfootVis.FOLLOWING
        player.followerObjId = if (spear) spearForm.objId else hammerForm.objId
    }

    fun stop(player: Player) {
        if (isFollowing(player)) {
            followers.dismiss(player)
        }
    }

    fun spear(stage: Int): Boolean = stage >= MudfootStage.WINDMILL

    /**
     * Sends Mudfoot back to his waiting spot when the player wanders out of Lumbridge with him.
     */
    fun leash(player: Player, stage: Int) {
        if (!isFollowing(player) || inLumbridge(player.coords)) {
            return
        }
        val npc = follower(player)
        val line = "Me no want go this way. Me meet you back ${waitingSpotName(stage)}."
        npc?.say(line)
        player.mes("Mudfoot|$line", ChatType.NpcSay)
        stop(player)
        player.mudfootVis = waitingSpot(stage)
    }

    private fun inLumbridge(coords: CoordGrid): Boolean =
        coords.level == 0 && coords.x in LUMBRIDGE_MIN_X..LUMBRIDGE_MAX_X &&
            coords.z in LUMBRIDGE_MIN_Z..LUMBRIDGE_MAX_Z

    companion object {
        private const val HAMMER_OBJ = "obj.mba_mudfoot_follower_obj"
        private const val SPEAR_OBJ = "obj.mba_mudfoot_follower_spear_obj"
        private const val HAMMER_NPC = "npc.mba_mudfoot_follower"
        private const val SPEAR_NPC = "npc.mba_mudfoot_follower_spear"

        private const val LUMBRIDGE_MIN_X = 3136
        private const val LUMBRIDGE_MAX_X = 3271
        private const val LUMBRIDGE_MIN_Z = 3168
        private const val LUMBRIDGE_MAX_Z = 3327

        fun waitingSpot(stage: Int): Int =
            when {
                stage < MudfootStage.WINDMILL -> MudfootVis.GOBLIN_HOUSE
                stage < MudfootStage.MAGE_DEAD -> MudfootVis.WINDMILL
                stage < MudfootStage.WARRIOR_DEAD -> MudfootVis.WOODS
                else -> MudfootVis.GRAVEYARD
            }

        fun waitingSpotName(stage: Int): String =
            when (waitingSpot(stage)) {
                MudfootVis.GOBLIN_HOUSE -> "outside goblin house"
                MudfootVis.WINDMILL -> "by windmill"
                MudfootVis.WOODS -> "by ruined building behind castle"
                else -> "in graveyard"
            }
    }
}

class MudfootLeash
@Inject
constructor(
    private val follower: MudfootFollower,
    private val encounters: MudfootEncounters,
) : PlayerPostTickHook {
    override fun onPostTick(player: Player) {
        if (player.loggingOut) {
            return
        }
        encounters.checkPlayer(player)
        follower.leash(player, player.mudfootStage)
    }
}
