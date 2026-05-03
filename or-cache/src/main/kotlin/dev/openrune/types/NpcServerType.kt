package dev.openrune.types

import dev.openrune.ParamMap
import dev.openrune.TypedParamType
import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.type.ParamType
import dev.openrune.resolve
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.seralizer.ParamSerializer
import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.util.BlockWalk
import dev.openrune.util.NpcPatrol
import dev.openrune.util.NpcPatrolWaypoint
import org.rsmod.game.map.Direction

@RsTableHeaders("npc")
data class NpcServerType(
    override var id: Int = -1,
    var name: String = "",
    var size: Int = 1,
    var category: Int = -1,
    var standAnim: Int = -1,
    var rotateLeftAnim: Int = -1,
    var rotateRightAnim: Int = -1,
    var walkAnim: Int = -1,
    var rotateBackAnim: Int = -1,
    var walkLeftAnim: Int = -1,
    var walkRightAnim: Int = -1,
    var actions: EntityOpsDefinition = EntityOpsDefinition(),
    var multiVarBit: Int = -1,
    var multiDefault: Int = -1,
    var multiVarp: Int = -1,
    var transforms: MutableList<Int>? = null,
    var combatLevel: Int = -1,
    var renderPriority: Int = 0,
    var lowPriorityFollowerOps: Boolean = false,
    var isFollower: Boolean = false,
    var runSequence: Int = -1,
    var isInteractable: Boolean = true,
    var runBackSequence: Int = -1,
    var runRightSequence: Int = -1,
    var runLeftSequence: Int = -1,
    var crawlSequence: Int = -1,
    var crawlBackSequence: Int = -1,
    var crawlRightSequence: Int = -1,
    var crawlLeftSequence: Int = -1,
    var height: Int = -1,
    var attack: Int = 1,
    var defence: Int = 1,
    var strength: Int = 1,
    var hitpoints: Int = 1,
    var ranged: Int = 1,
    var magic: Int = 1,
    var timer: Int = -1,
    var respawnDir: Direction = Direction.South,
    var waypoints: List<NpcPatrolWaypoint> = emptyList(),
    var contentGroup: Int = -1,
    var heroCount: Int = 16,
    var regenRate: Int = 100,
    var moveRestrict: MoveRestrict = MoveRestrict.Normal,
    var defaultMode: NpcMode = NpcMode.Wander,
    var blockWalk: BlockWalk = BlockWalk.Npc,
    var respawnRate: Int = 100,
    var examine: String = "",
    var maxRange: Int = 7,
    var wanderRange: Int = 5,
    var attackRange: Int = 1,
    var huntRange: Int = 5,
    var huntMode: Int? = null,
    var giveChase: Boolean = true,
    @param:TomlField(["params"],serializer = ParamSerializer::class)
    var paramsRaw: MutableMap<Int, Any>? = null,
) : Definition {

    var paramMap: ParamMap? = null

    var patrol: NpcPatrol? = null

    val multiNpc: IntArray
        get() = transforms?.toIntArray() ?: intArrayOf()

    public val isMultiNpc: Boolean
        get() = transforms?.isNotEmpty() == true || multiDefault > 0

    fun hasOp(slot: Int): Boolean {
        val text = actions.getOpOrNull(slot - 1) ?: return false
        return text.isNotBlank()
    }

    fun isType(other: String): Boolean = id == other.asRSCM(RSCMType.NPC)

    fun isContentType(content: String): Boolean = contentGroup == content.asRSCM(RSCMType.CONTENT)

    fun isCategoryType(cat: String): Boolean = category == cat.asRSCM(RSCMType.CATEGORY)

    public fun <T : Any> param(type: ParamType): T = paramMap.resolve(type)

    public fun <T : Any> param(type: TypedParamType<T>): T = paramMap.resolve(type)

    public fun <T : Any> paramOrNull(type: ParamType): T? = paramMap?.getOrNull(type)

    public fun <T : Any> paramOrNull(type: TypedParamType<T>): T? = paramMap?.getOrNull(type)

    public fun hasParam(type: ParamType): Boolean = paramMap?.contains(type) == true

    public fun hasParam(type: TypedParamType<*>): Boolean = paramOrNull(type) != null
}
