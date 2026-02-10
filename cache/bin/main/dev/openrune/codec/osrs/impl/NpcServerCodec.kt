package dev.openrune.codec.osrs.impl

import dev.openrune.definition.type.NpcType
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeListActions
import dev.openrune.definition.opcode.impl.DefinitionOpcodeParams
import dev.openrune.definition.opcode.impl.DefinitionOpcodeTransforms
import dev.openrune.types.NpcServerType


class NpcServerCodec(val npcs: Map<Int, NpcType>? = null) : OpcodeDefinitionCodec<NpcServerType>() {

    override val definitionCodec = OpcodeList<NpcServerType>().apply {
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
        add(DefinitionOpcodeListActions(11, OpcodeType.STRING, NpcServerType::actions, 5))
        add(DefinitionOpcodeTransforms(IntRange(12, 13), NpcServerType::transforms, NpcServerType::varbit, NpcServerType::varp))
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
        add(DefinitionOpcodeParams(27, NpcServerType::params))
        add(DefinitionOpcode(28, OpcodeType.USHORT, NpcServerType::height))
        add(DefinitionOpcode(29, OpcodeType.USHORT, NpcServerType::attack))
        add(DefinitionOpcode(30, OpcodeType.USHORT, NpcServerType::defence))
        add(DefinitionOpcode(31, OpcodeType.USHORT, NpcServerType::strength))
        add(DefinitionOpcode(32, OpcodeType.USHORT, NpcServerType::hitpoints))
        add(DefinitionOpcode(33, OpcodeType.USHORT, NpcServerType::ranged))
        add(DefinitionOpcode(34, OpcodeType.USHORT, NpcServerType::magic))
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
        varbit = obj.varbit
        varp = obj.varp
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
        params = obj.params
    }

    override fun createDefinition(): NpcServerType = NpcServerType()
}
