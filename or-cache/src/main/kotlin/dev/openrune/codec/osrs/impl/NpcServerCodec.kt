package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.OpcodeType.BOOLEAN.enumType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeEntityOps
import dev.openrune.definition.opcode.impl.DefinitionOpcodeParams
import dev.openrune.definition.opcode.impl.DefinitionOpcodeTransforms
import dev.openrune.definition.type.NpcType
import dev.openrune.revision
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import dev.openrune.util.BlockWalk
import dev.openrune.util.Coord
import dev.openrune.util.DefinitionOpcodeParamMap
import dev.openrune.util.NpcPatrol
import dev.openrune.util.NpcPatrolWaypoint
import org.rsmod.game.map.Direction

class NpcServerCodec(
    val rev : Int,
    val npcs: Map<Int, NpcType>? = null,
    val custom: Map<Int, NpcServerType>? = emptyMap(),
    val examines: Map<Int, String> = emptyMap(),
) : OpcodeDefinitionCodec<NpcServerType>() {

    override val definitionCodec =
        OpcodeList<NpcServerType>().apply {
            add(DefinitionOpcode(1, OpcodeType.STRING, NpcServerType::name))
            add(DefinitionOpcode(2, OpcodeType.INT, NpcServerType::size))
            add(DefinitionOpcode(3, OpcodeType.INT, NpcServerType::category))
            add(DefinitionOpcode(4, OpcodeType.INT, NpcServerType::standAnim))
            add(DefinitionOpcode(5, OpcodeType.INT, NpcServerType::rotateLeftAnim))
            add(DefinitionOpcode(6, OpcodeType.INT, NpcServerType::rotateRightAnim))
            add(DefinitionOpcode(7, OpcodeType.INT, NpcServerType::walkAnim))
            add(DefinitionOpcode(8, OpcodeType.INT, NpcServerType::rotateBackAnim))
            add(DefinitionOpcode(9, OpcodeType.INT, NpcServerType::walkLeftAnim))
            add(DefinitionOpcode(10, OpcodeType.INT, NpcServerType::walkRightAnim))
            add(DefinitionOpcodeEntityOps(11, NpcServerType::actions, rev))
            add(
                DefinitionOpcodeTransforms(
                    IntRange(12, 13),
                    NpcServerType::transforms,
                    NpcServerType::multiVarBit,
                    NpcServerType::multiVarp,
                    NpcServerType::multiDefault,
                )
            )
            add(DefinitionOpcode(14, OpcodeType.INT, NpcServerType::combatLevel))
            add(DefinitionOpcode(15, OpcodeType.INT, NpcServerType::renderPriority))
            add(DefinitionOpcode(16, OpcodeType.BOOLEAN, NpcServerType::lowPriorityFollowerOps))
            add(DefinitionOpcode(17, OpcodeType.BOOLEAN, NpcServerType::isFollower))
            add(DefinitionOpcode(18, OpcodeType.INT, NpcServerType::runSequence))
            add(DefinitionOpcode(19, OpcodeType.BOOLEAN, NpcServerType::isInteractable))
            add(DefinitionOpcode(20, OpcodeType.INT, NpcServerType::runBackSequence))
            add(DefinitionOpcode(21, OpcodeType.INT, NpcServerType::runRightSequence))
            add(DefinitionOpcode(22, OpcodeType.INT, NpcServerType::runLeftSequence))
            add(DefinitionOpcode(23, OpcodeType.INT, NpcServerType::crawlSequence))
            add(DefinitionOpcode(24, OpcodeType.INT, NpcServerType::crawlBackSequence))
            add(DefinitionOpcode(25, OpcodeType.INT, NpcServerType::crawlRightSequence))
            add(DefinitionOpcode(26, OpcodeType.INT, NpcServerType::crawlLeftSequence))
            add(DefinitionOpcodeParamMap(27, NpcServerType::paramsRaw, NpcServerType::paramMap))
            add(DefinitionOpcode(28, OpcodeType.USHORT, NpcServerType::height))
            add(DefinitionOpcode(29, OpcodeType.USHORT, NpcServerType::attack))
            add(DefinitionOpcode(30, OpcodeType.USHORT, NpcServerType::defence))
            add(DefinitionOpcode(31, OpcodeType.USHORT, NpcServerType::strength))
            add(DefinitionOpcode(32, OpcodeType.USHORT, NpcServerType::hitpoints))
            add(DefinitionOpcode(33, OpcodeType.USHORT, NpcServerType::ranged))
            add(DefinitionOpcode(34, OpcodeType.USHORT, NpcServerType::magic))

            add(DefinitionOpcode(35, OpcodeType.USHORT, NpcServerType::timer))
            add(DefinitionOpcode(36, enumType<Direction>(), NpcServerType::respawnDir))
            add(DefinitionOpcode(37, OpcodeType.USHORT, NpcServerType::contentGroup))
            add(DefinitionOpcode(38, OpcodeType.USHORT, NpcServerType::heroCount))
            add(DefinitionOpcode(39, OpcodeType.USHORT, NpcServerType::regenRate))
            add(DefinitionOpcode(40, enumType<MoveRestrict>(), NpcServerType::moveRestrict))
            add(DefinitionOpcode(41, enumType<NpcMode>(), NpcServerType::defaultMode))
            add(DefinitionOpcode(42, enumType<BlockWalk>(), NpcServerType::blockWalk))
            add(DefinitionOpcode(43, OpcodeType.INT, NpcServerType::respawnRate))
            add(DefinitionOpcode(44, OpcodeType.STRING, NpcServerType::examine))
            add(DefinitionOpcode(45, OpcodeType.INT, NpcServerType::maxRange))
            add(DefinitionOpcode(46, OpcodeType.INT, NpcServerType::wanderRange))
            add(DefinitionOpcode(47, OpcodeType.INT, NpcServerType::attackRange))
            add(DefinitionOpcode(48, OpcodeType.INT, NpcServerType::huntRange))
            add(DefinitionOpcode(49, OpcodeType.INT, NpcServerType::huntMode))
            add(DefinitionOpcode(50, OpcodeType.BOOLEAN, NpcServerType::giveChase))
            add(
                DefinitionOpcode(
                    51,
                    decode = { buf, def, _ ->
                        val count = buf.readUnsignedByte().toInt() + 1
                        val waypoints = mutableListOf<NpcPatrolWaypoint>()
                        repeat(count) {
                            val coords = Coord.unpack(buf.readInt())
                            val pauseDelay = buf.readUnsignedByte().toInt()
                            waypoints += NpcPatrolWaypoint(coords, pauseDelay)
                        }
                        def.patrol = NpcPatrol(waypoints)
                    },
                    encode = { buf, def ->
                        buf.writeByte(def.waypoints.size - 1)
                        for (i in def.waypoints.indices) {
                            buf.writeInt(def.waypoints[i].destination.pack())
                            buf.writeByte(def.waypoints[i].pauseDelay)
                        }
                    },
                    shouldEncode = { def -> def.waypoints.isNotEmpty() },
                )
            )
        }

    override fun NpcServerType.createData() {
        if (npcs == null) return
        val obj = npcs[id] ?: return

        name = obj.name
        size = obj.size
        category = obj.category
        standAnim = obj.standAnim
        rotateLeftAnim = obj.rotateLeftAnim
        rotateRightAnim = obj.rotateRightAnim
        walkAnim = obj.walkAnim
        rotateBackAnim = obj.rotateBackAnim
        walkLeftAnim = obj.walkLeftAnim
        walkRightAnim = obj.walkRightAnim
        actions = obj.actions
        multiVarBit = obj.multiVarBit
        multiDefault = obj.multiDefault
        multiVarp = obj.multiVarp
        transforms = obj.transforms
        combatLevel = obj.combatLevel
        renderPriority = obj.renderPriority
        lowPriorityFollowerOps = obj.lowPriorityFollowerOps
        isFollower = obj.isFollower
        runSequence = obj.runSequence
        isInteractable = obj.isInteractable
        runBackSequence = obj.runBackSequence
        runRightSequence = obj.runRightSequence
        runLeftSequence = obj.runLeftSequence
        crawlSequence = obj.crawlSequence
        crawlBackSequence = obj.crawlBackSequence
        crawlRightSequence = obj.crawlRightSequence
        crawlLeftSequence = obj.crawlLeftSequence
        height = obj.height
        attack = obj.attack
        defence = obj.defence
        strength = obj.strength
        hitpoints = obj.hitpoints
        ranged = obj.ranged
        magic = obj.magic
        paramsRaw = obj.params

        val customData = custom?.get(id)

        if (customData != null) {

            timer = customData.timer
            respawnDir = customData.respawnDir
            patrol = customData.patrol
            contentGroup = customData.contentGroup
            heroCount = customData.heroCount
            regenRate = customData.regenRate
            moveRestrict = customData.moveRestrict
            defaultMode = customData.defaultMode
            blockWalk = customData.blockWalk
            respawnRate = customData.respawnRate
            examine = customData.examine.takeIf { it.isNotEmpty() } ?: examines[obj.id] ?: ""
            maxRange = customData.maxRange
            wanderRange = customData.wanderRange
            attackRange = customData.attackRange
            huntRange = customData.huntRange
            huntMode = customData.huntMode
            giveChase = customData.giveChase
            waypoints = customData.waypoints
        }
    }

    override fun createDefinition(): NpcServerType = NpcServerType()
}
