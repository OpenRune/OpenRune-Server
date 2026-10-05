package org.rsmod.api.testing.scope

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provider
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.StatType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import kotlin.contracts.contract
import kotlin.reflect.KClass
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.buttons.IfButtonD
import net.rsprot.protocol.game.incoming.locs.OpLocV2
import net.rsprot.protocol.game.incoming.misc.user.MoveGameClick
import net.rsprot.protocol.game.incoming.npcs.OpNpcV2
import net.rsprot.protocol.game.incoming.resumed.ResumePCountDialog
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions
import org.rsmod.annotations.InternalApi
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.db.Database
import org.rsmod.api.db.DatabaseConfig
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.game.process.GameCycle
import org.rsmod.api.inv.map.InvMapInit
import org.rsmod.api.market.DefaultMarketPrices
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.net.rsprot.handlers.IfButtonDHandler
import org.rsmod.api.net.rsprot.handlers.MoveGameClickHandler
import org.rsmod.api.net.rsprot.handlers.OpLocHandler
import org.rsmod.api.net.rsprot.handlers.OpNpcHandler
import org.rsmod.api.net.rsprot.handlers.ResumePCountDialogHandler
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.hit.NpcDamageContributor
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.processor.NpcHitProcessor
import org.rsmod.api.npc.hit.processor.StandardNpcHitProcessor
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.processor.DamageOnlyPlayerHitProcessor
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hook.PlayerGroundItemDropHook
import org.rsmod.api.player.hook.PlayerInvUpdateHook
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifOpenMain
import org.rsmod.api.player.ui.ifOpenSub
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.varMoveSpeed
import org.rsmod.api.random.CoreRandom
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.realm.Realm
import org.rsmod.api.registry.account.AccountRegistry
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.controller.isSuccess
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.npc.isSuccess
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.route.RayCastFactory
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.api.server.config.ServerConfigModule
import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.capture.CaptureClient
import org.rsmod.api.testing.factory.TestCacheTypes
import org.rsmod.api.testing.factory.collisionFactory
import org.rsmod.api.testing.random.FixedRandom
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.api.testing.util.TestRealmConfig
import org.rsmod.api.utils.logging.GameExceptionHandler
import org.rsmod.events.EventBus
import org.rsmod.game.GameUpdate
import org.rsmod.game.MapClock
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.entity.util.ShuffledPlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.map.collision.addLoc
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.Region
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.loc.LocLayerConstants
import org.rsmod.server.app.modules.ParserModule

public class GameTestScope
@Inject
constructor(
    public val random: VariableGameRandom,
    public val mapClock: MapClock,
    public val eventBus: EventBus,
    public val players: PlayerList,
    public val conRepo: ControllerRepository,
    public val npcRepo: NpcRepository,
    private val gameCycle: GameCycle,
    private val collision: CollisionFlagMap,
    private val locZoneStorage: LocZoneStorage,
    private val locRegistry: LocRegistry,
    private val conRegistry: ControllerRegistry,
    private val npcRegistry: NpcRegistry,
    private val invMapInit: InvMapInit,
    private val protectedAccess: ProtectedAccessLauncher,
    private val ifButtonHandler: If3ButtonHandler,
    private val ifButtonDHandler: IfButtonDHandler,
    private val gameClickHandler: MoveGameClickHandler,
    private val resumePCountDialog: ResumePCountDialogHandler,
    private val opLocHandler: OpLocHandler,
    private val opNpcHandler: OpNpcHandler,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val npcHitModifier: NpcHitModifier,
    private val regionRegistry: RegionRegistry,
    private val regionRepo: RegionRepository,
) {
    init {
        registerPlayer()
    }

    public val player: Player by lazy { players.first() }
    public val client: CaptureClient by lazy { player.captureClient }

    private val Player.captureClient
        get() = client as CaptureClient

    public val Player.stats: StatsDelegate
        get() = StatsDelegate(statMap)

    private var playerUuidCounter = 0L

    public fun advance(ticks: Int = 1) {
        repeat(ticks) {
            clearCaptureClients()
            gameCycle.tick()
            flushCaptureClients()
        }
    }

    public fun advanceUntil(
        predicate: () -> Boolean,
        timeoutTicks: Int = 1000,
        timeoutMessage: () -> String? = { null },
    ) {
        for (i in 0 until timeoutTicks) {
            if (predicate()) {
                return
            }
            advance()
        }
        val message = timeoutMessage() ?: "Could not advance after $timeoutTicks ticks!"
        throw IllegalStateException(message)
    }

    @OptIn(InternalApi::class)
    public fun registerPlayer(
        coords: CoordGrid = CoordGrid(0, 50, 50, 0, 0),
        player: Player = Player(),
        slot: Int = players.nextFreeSlot() ?: error("No available slot."),
        client: Client<Any, Any> = CaptureClient(),
        uuid: Long? = null,
    ): Player {
        val resolvedUuid = uuid ?: playerUuidCounter++
        player.coords = coords
        player.slotId = slot
        player.client = client
        player.uuid = resolvedUuid
        player.accountId = resolvedUuid.toInt()
        player.characterId = resolvedUuid.toInt()
        player.accountHash = resolvedUuid
        player.userId = resolvedUuid
        player.runEnergy = Int.MAX_VALUE
        player.assignUid()
        players[slot] = player
        eventBus.publish(SessionStateEvent.Initialize(player))
        if (player.invMap.isEmpty()) {
            invMapInit.init(player)
        }
        eventBus.publish(SessionStateEvent.Login(player))
        eventBus.publish(SessionStateEvent.EngineLogin(player))
        return player
    }

    @OptIn(InternalApi::class)
    public fun unregisterPlayer(player: Player) {
        require(player.slotId != PathingEntity.INVALID_SLOT) {
            "Invalid `slotId` for player: $player"
        }
        val slot = player.slotId
        player.slotId = -1
        player.clearUid()
        player.destroy()
        players.remove(slot)
    }

    public fun Player.setVarp(varp: String, value: Int) {
        VarPlayerIntMapSetter.set(this, varp, value)
    }

    public fun Player.setVarBit(varbit: VarBitType, value: Int) {
        VarPlayerIntMapSetter.set(this, varbit, value)
    }

    public fun Player.setVarBit(varbit: String, value: Int) {
        VarPlayerIntMapSetter.set(this, varbit, value)
    }

    public fun Player.setCurrentLevel(stat: String, level: Int) {
        statMap.setCurrentLevel(stat, level.toByte())
    }

    public fun Player.setBaseLevel(stat: String, level: Int) {
        statMap.setBaseLevel(stat, level.toByte())
    }

    public fun Player.setCurrentLevel(stat: StatType, level: Int) {
        stats.setCurrentLevel(stat, level)
    }

    public fun Player.setBaseLevel(stat: StatType, level: Int) {
        stats.setBaseLevel(stat, level)
    }

    public fun Player.setMoveSpeed(speed: MoveSpeed) {
        this.varMoveSpeed = speed
    }

    public fun Player.enableRun() {
        setMoveSpeed(MoveSpeed.Run)
    }

    public fun Player.enableWalk() {
        setMoveSpeed(MoveSpeed.Walk)
    }

    /**
     * Instantly sets the [Player]'s coordinates to [dest] without performing any of the updates or
     * side effects that normal teleport functions handle (such as updating the collision flag map).
     *
     * This function is intended for test setup only. It exists because using [teleport] or
     * [telejump] in tests would require calling [advance] to fully process the teleport logic and
     * any side effects, which can be unnecessary or cumbersome during test initialization.
     *
     * To simulate actual gameplay behavior during tests, use [teleport] or [telejump], as they
     * accurately reflect in-game mechanics and processing.
     */
    public fun Player.placeAt(dest: CoordGrid) {
        allocZoneCollision(dest)
        coords = dest
    }

    public fun Player.teleport(dest: CoordGrid) {
        allocZoneCollision(dest)
        PathingEntityCommon.teleport(this, collision, dest)
    }

    public fun Player.telejump(dest: CoordGrid) {
        allocZoneCollision(dest)
        PathingEntityCommon.telejump(this, collision, dest)
    }

    public fun Player.clearPendingAction() {
        clearPendingAction(eventBus)
    }

    public fun Player.clearInv(inv: Inventory = this.inv) {
        inv.fillNulls()
    }

    public fun Player.fillInv(with: InvObj = InvObj(testItem("obj.beer")), inv: Inventory = this.inv) {
        repeat(inv.size) { inv[it] = InvObj(with) }
    }

    public fun Player.count(obj: ItemServerType, inv: Inventory = this.inv): Int =
        inv.count(obj.internalName)

    public fun Player.clearAllInvs() {
        invMap.values.forEach(Inventory::fillNulls)
    }

    public fun Player.opLoc1(loc: BoundLocInfo, controlKey: Boolean = false) {
        val message = OpLocV2(loc.id, loc.x, loc.z, controlKey, op = 1, subop = 0)
        captureClient.queue(opLocHandler, message)
    }

    public fun Player.opLoc2(loc: BoundLocInfo, controlKey: Boolean = false) {
        val message = OpLocV2(loc.id, loc.x, loc.z, controlKey, op = 2, subop = 0)
        captureClient.queue(opLocHandler, message)
    }

    public fun Player.opLoc3(loc: BoundLocInfo, controlKey: Boolean = false) {
        val message = OpLocV2(loc.id, loc.x, loc.z, controlKey, op = 3, subop = 0)
        captureClient.queue(opLocHandler, message)
    }

    public fun Player.opLoc4(loc: BoundLocInfo, controlKey: Boolean = false) {
        val message = OpLocV2(loc.id, loc.x, loc.z, controlKey, op = 4, subop = 0)
        captureClient.queue(opLocHandler, message)
    }

    public fun Player.opLoc5(loc: BoundLocInfo, controlKey: Boolean = false) {
        val message = OpLocV2(loc.id, loc.x, loc.z, controlKey, op = 5, subop = 0)
        captureClient.queue(opLocHandler, message)
    }

    public fun Player.opNpc1(npc: Npc, controlKey: Boolean = false) {
        val message = OpNpcV2(npc.slotId, controlKey, op = 1, subop = 0)
        captureClient.queue(opNpcHandler, message)
    }

    public fun Player.opNpc2(npc: Npc, controlKey: Boolean = false) {
        val message = OpNpcV2(npc.slotId, controlKey, op = 2, subop = 0)
        captureClient.queue(opNpcHandler, message)
    }

    public fun Player.opNpc3(npc: Npc, controlKey: Boolean = false) {
        val message = OpNpcV2(npc.slotId, controlKey, op = 3, subop = 0)
        captureClient.queue(opNpcHandler, message)
    }

    public fun Player.opNpc4(npc: Npc, controlKey: Boolean = false) {
        val message = OpNpcV2(npc.slotId, controlKey, op = 4, subop = 0)
        captureClient.queue(opNpcHandler, message)
    }

    public fun Player.opNpc5(npc: Npc, controlKey: Boolean = false) {
        val message = OpNpcV2(npc.slotId, controlKey, op = 5, subop = 0)
        captureClient.queue(opNpcHandler, message)
    }

    public fun Player.ifOpenMain(interf: String) {
        ifOpenMain(interf, eventBus)
    }

    public fun Player.ifOpenOverlay(interf: String, target: String) {
        ifOpenSub(interf, target, IfSubType.Overlay, eventBus)
    }

    public fun Player.ifClose() {
        ifClose(eventBus)
    }

    public fun Player.ifButton(
        type: ComponentType,
        comsub: Int? = null,
        op: IfButtonOp = IfButtonOp.Op1,
        obj: Int? = null,
    ) {
        val combinedId = CombinedId(type.interfaceId, type.component)
        val message = If3Button(combinedId, comsub ?: -1, obj = obj ?: -1, op = op.slot)
        captureClient.queue(ifButtonHandler, message)
    }

    public fun Player.ifButtonD(
        fromComponent: ComponentType,
        fromComsub: Int,
        fromObj: ItemServerType?,
        intoComponent: ComponentType,
        intoComsub: Int,
        intoObj: ItemServerType?,
    ) {
        val fromCombinedId = CombinedId(fromComponent.interfaceId, fromComponent.component)
        val intoCombinedId = CombinedId(intoComponent.interfaceId, intoComponent.component)
        val message =
            IfButtonD(
                fromCombinedId,
                fromComsub,
                fromObj?.id ?: -1,
                intoCombinedId,
                intoComsub,
                intoObj?.id ?: -1,
            )
        captureClient.queue(ifButtonDHandler, message)
    }

    public fun Player.ifButtonD(
        fromComponent: ComponentType,
        fromComsub: Int,
        intoComsub: Int,
        fromObj: ItemServerType? = null,
        intoObj: ItemServerType? = null,
    ) {
        ifButtonD(
            fromComponent = fromComponent,
            fromComsub = fromComsub,
            fromObj = fromObj,
            intoComponent = fromComponent,
            intoComsub = intoComsub,
            intoObj = intoObj,
        )
    }

    public fun Player.resumeCountDialog(count: Int) {
        val message = ResumePCountDialog(count)
        captureClient.queue(resumePCountDialog, message)
    }

    public fun Player.moveGameClick(dest: CoordGrid, keyCombination: Int = 0) {
        allocZoneCollision(dest)
        val message = MoveGameClick(dest.x, dest.z, keyCombination)
        captureClient.queue(gameClickHandler, message)
    }

    public fun Player.withProtectedAccess(action: suspend ProtectedAccess.() -> Unit) {
        protectedAccess.launch(this) { action() }
    }

    public fun Npc.telejump(dest: CoordGrid) {
        allocZoneCollision(dest)
        PathingEntityCommon.telejump(this, collision, dest)
    }

    public fun Npc.opPlayer2(target: Player) {
        opPlayer2(target, aiPlayerInteractions)
    }

    public fun Npc.apPlayer2(target: Player) {
        apPlayer2(target, aiPlayerInteractions)
    }

    public fun Npc.queueHit(source: Player, delay: Int, type: HitType, damage: Int) {
        queueHit(source, delay, type, damage, npcHitModifier)
    }

    public fun Inventory.count(obj: ItemServerType): Int = count(obj.internalName)

    public fun allocZoneCollision(coord: CoordGrid) {
        collision.allocateIfAbsent(coord.x, coord.z, coord.level)
    }

    public fun spawnNpc(coords: CoordGrid, type: NpcServerType, init: Npc.() -> Unit = {}): Npc {
        val npc = Npc(type, coords).apply(init)
        val add = npcRegistry.add(npc)
        check(add.isSuccess()) { "Could not add npc: result=$add, npc=$npc" }
        return npc
    }

    public fun spawnController(
        coords: CoordGrid,
        type: String,
        duration: Int = Int.MAX_VALUE,
        init: Controller.() -> Unit = {},
    ): Controller {
        val controller = Controller(type, coords).apply { duration(duration) }.apply(init)
        val add = conRegistry.add(controller)
        check(add.isSuccess()) { "Could not add controller: result=$add, controller=$controller" }
        return controller
    }

    public fun placeMapLoc(
        coords: CoordGrid,
        type: ObjectServerType,
        shape: LocShape = LocShape.CentrepieceStraight,
        angle: LocAngle = LocAngle.West,
    ): BoundLocInfo {
        val entity = LocEntity(type.id, shape.id, angle.id)
        val locInfo = LocInfo(LocLayerConstants.of(shape.id), coords, entity)
        val boundLoc = BoundLocInfo(locInfo, type)
        collision.addLoc(locInfo, type)

        val zoneKey = ZoneKey.from(coords)
        val zoneGrid = ZoneGrid.from(coords)
        val locZoneKey = LocZoneKey(zoneGrid, locInfo.layer)
        locZoneStorage.mapLocs[zoneKey, locZoneKey] = entity

        TestCacheTypes.objects.registerIfSynthetic(type.id, type)

        return boundLoc
    }

    public fun placeMapLoc(
        coords: CoordGrid,
        type: String,
        shape: LocShape = LocShape.CentrepieceStraight,
        angle: LocAngle = LocAngle.West,
    ): BoundLocInfo = placeMapLoc(coords, testLoc(type), shape, angle)

    public fun locDel(bound: BoundLocInfo) {
        val locInfo = LocInfo(bound.layer, bound.coords, bound.entity)
        locDel(locInfo)
    }

    public fun locDel(loc: LocInfo) {
        locRegistry.del(loc)
    }

    public fun findLocs(coords: CoordGrid): Sequence<LocInfo> =
        locRegistry.findAll(ZoneKey.from(coords))

    public fun findLoc(coords: CoordGrid, type: ObjectServerType): LocInfo? =
        findLocs(coords).firstOrNull { it.id == type.id }

    public fun locExists(loc: BoundLocInfo): Boolean = locRegistry.isValid(loc.coords, loc.id)

    public fun locExists(coords: CoordGrid, type: ObjectServerType): Boolean =
        locRegistry.isValid(coords, type.id)

    public fun findLocTypes(predicate: (ObjectServerType) -> Boolean): Sequence<ObjectServerType> {
        return sequence {
            for (type in ServerCacheManager.getObjects().values) {
                if (predicate(type)) {
                    yield(type)
                }
            }
        }
    }

    public fun findLocType(
        content: String,
        predicate: (ObjectServerType) -> Boolean = { true },
    ): ObjectServerType = findLocTypes { it.isContentType(content) && predicate(it) }.first()

    public fun findObjType(predicate: (ItemServerType) -> Boolean): Sequence<ItemServerType> {
        return sequence {
            for (type in ServerCacheManager.getItems().values) {
                if (predicate(type)) {
                    yield(type)
                }
            }
        }
    }

    public fun firstObjType(predicate: (ItemServerType) -> Boolean): ItemServerType {
        val filtered = findObjType(predicate)
        return filtered.firstOrNull()
            ?: throw NoSuchElementException("No ItemServerType found with given predicate.")
    }

    public fun createRegionOrNull(template: RegionTemplate): Region? {
        return regionRepo.add(template)
    }

    public fun createRegion(template: RegionTemplate): Region {
        return createRegionOrNull(template) ?: error("Could not create region.")
    }

    public fun removeInactiveRegions() {
        regionRegistry.removeInactiveSmallRegions()
        regionRegistry.removeInactiveLargeRegions()
    }

    public fun CaptureClient.clear() {
        clearOutgoing()
    }

    private fun clearCaptureClients() {
        for (player in players) {
            val client = player.captureClient
            client.clearOutgoing()
        }
    }

    private fun flushCaptureClients() {
        for (player in players) {
            val client = player.captureClient
            client.clearIncoming()
        }
    }

    public fun assertTrue(condition: Boolean) {
        contract { returns() implies condition }
        Assertions.assertTrue(condition)
    }

    public fun assertTrue(condition: Boolean, message: () -> String) {
        contract { returns() implies condition }
        Assertions.assertTrue(condition, message)
    }

    public fun assertFalse(condition: Boolean) {
        contract { returns() implies !condition }
        Assertions.assertFalse(condition)
    }

    public fun assertNull(actual: Any?) {
        contract { returns() implies (actual == null) }
        Assertions.assertNull(actual)
    }

    public fun assertNotNull(actual: Any?) {
        contract { returns() implies (actual != null) }
        Assertions.assertNotNull(actual)
    }

    public fun assertEquals(expected: Any?, actual: Any?) {
        Assertions.assertEquals(expected, actual)
    }

    public fun assertNotEquals(expected: Any?, actual: Any?) {
        Assertions.assertNotEquals(expected, actual)
    }

    public fun assertContains(inv: Inventory, obj: ItemServerType) {
        Assertions.assertTrue(obj in inv) { "Obj not found. (obj=$obj) | (inv=$inv)" }
    }

    public fun assertDoesNotContain(inv: Inventory, obj: ItemServerType) {
        Assertions.assertFalse(obj in inv) { "Obj found. (obj=$obj) | (inv=$inv)" }
    }

    public fun assertExists(loc: BoundLocInfo) {
        Assertions.assertTrue(locExists(loc)) {
            val found = locRegistry.findAll(ZoneKey.from(loc.coords)).toList()
            "Loc not found. (loc=$loc) | (found=$found)"
        }
    }

    public fun assertExists(coords: CoordGrid, type: ObjectServerType) {
        Assertions.assertTrue(locExists(coords, type)) {
            val found = locRegistry.findAll(ZoneKey.from(coords)).toList()
            "Loc not found. (coords=$coords, type=$type) | (found=$found)"
        }
    }

    public fun assertDoesNotExist(loc: BoundLocInfo) {
        Assertions.assertFalse(locExists(loc)) { "Loc found. (loc=$loc)" }
    }

    public fun assertDoesNotExist(coords: CoordGrid, type: ObjectServerType) {
        Assertions.assertFalse(locExists(coords, type)) {
            "Loc found. (coords=$coords) | (type=$type)"
        }
    }

    public fun assertMessageSent(text: String, player: Player = this.player) {
        val messages = player.captureClient.mapOf(MessageGame::message)
        val sent = messages.contains(text)
        Assertions.assertTrue(sent) { "Message not found. (search=$text) | (messages=$messages)" }
    }

    public fun assertMessageNotSent(text: String, player: Player = this.player) {
        val messages = player.captureClient.mapOf(MessageGame::message)
        val sent = messages.contains(text)
        Assertions.assertFalse(sent) { "Message found. (search=$text) | (messages=$messages)" }
    }

    public fun assertMessagesSent(vararg text: String, player: Player = this.player) {
        require(text.isNotEmpty()) { "Must provide at least one `text` argument." }
        val messages = player.captureClient.mapOf(MessageGame::message)
        val notFound = text.filter { search -> messages.none { search == it } }
        Assertions.assertTrue(notFound.isEmpty()) {
            "Messages not found. (notFound=$notFound) | (messages=$messages)"
        }
    }

    public fun assertNoMessageSent(player: Player = this.player) {
        val messages = player.captureClient.mapOf(MessageGame::message)
        Assertions.assertEquals(emptyList<String>(), messages) { "Messages found:" }
    }

    public fun assertModalOpen(interf: String, player: Player = this.player) {
        Assertions.assertTrue(player.ui.containsModal(interf)) {
            val openedModals = player.ui.modals.values.map(::UserInterface)
            "Modal not opened. (expected=$interf) | (found=$openedModals) | (player=$player)"
        }
    }

    public fun assertModalNotOpen(interf: String, player: Player = this.player) {
        Assertions.assertFalse(player.ui.containsModal(interf)) {
            val openedModals = player.ui.modals.values.map(::UserInterface)
            "Modal is opened. (modal=$interf) | (opened=$openedModals) | (player=$player)"
        }
    }

    /**
     * This requires a context (`Player` in this case) so that after catching the expected
     * [Throwable] the entity can be registered into the game world once again if they were
     * disconnected.
     */
    public inline fun <reified T : Throwable> Player.assertThrows(block: () -> Unit): T {
        val startClient = client
        val startSlot = slotId
        val startUuid = uuid
        val caught: Throwable? =
            try {
                client = CaptureClient()
                block()
                null
            } catch (t: Throwable) {
                t
            }

        Assertions.assertNotNull(caught) {
            "Expected ${T::class.java.simpleName} to be thrown, but nothing was thrown."
        }

        Assertions.assertInstanceOf(T::class.java, caught) {
            "Expected ${T::class.java.simpleName} to be thrown, " +
                "but ${caught?.javaClass?.simpleName} was thrown."
        }

        // We want to restore the player's capture client before any errors occurred.
        this.client = startClient

        // If player was disconnected due to the error, we should re-register them.
        if (slotId == PathingEntity.INVALID_SLOT && startSlot != PathingEntity.INVALID_SLOT) {
            registerPlayer(coords, this, startSlot, startClient, startUuid)
        }
        return caught as T
    }

    public fun assertDoesNotThrow(msg: String, block: () -> Unit) {
        Assertions.assertDoesNotThrow({ block() }, msg)
    }

    public fun assertDoesNotThrow(block: () -> Unit) {
        Assertions.assertDoesNotThrow { block() }
    }

    private fun testItem(rscm: String): ItemServerType =
        requireNotNull(ServerCacheManager.getItem(rscm.asRSCM(RSCMType.OBJ))) {
            "Unknown item: $rscm"
        }

    private fun testLoc(rscm: String): ObjectServerType =
        requireNotNull(ServerCacheManager.getObject(rscm.asRSCM(RSCMType.LOC))) {
            "Unknown loc: $rscm"
        }

    public class Builder(state: GameTestState, private val scripts: Set<KClass<out PluginScript>>) {
        private val collisionMap: CollisionFlagMap = state.collision

        internal fun build(): GameTestScope {
            val module = TestModule(collisionMap)
            val injector = Guice.createInjector(module)
            bindScriptEvents(injector)
            return injector.getInstance(GameTestScope::class.java)
        }

        internal fun buildInjector(optionalChildModule: AbstractModule?): Injector {
            val module = TestModule(collisionMap)
            val parentInjector = Guice.createInjector(module)
            val injector =
                if (optionalChildModule != null) {
                    parentInjector.createChildInjector(optionalChildModule)
                } else {
                    parentInjector
                }
            bindScriptEvents(injector)
            return injector
        }

        private fun bindScriptEvents(injector: Injector) {
            val context = injector.getInstance(ScriptContext::class.java)
            for (clazz in scripts) {
                val script = injector.getInstance(clazz.java)
                with(script) { context.startup() }
            }
        }
    }

    private class TestModule(private val gameCollisionMap: CollisionFlagMap) : AbstractModule() {
        override fun configure() {
            bindInstances()
            installModules()
        }

        private fun bindInstances() {
            collisionFactory.borrowSharedMap().let { collision ->
                // Copy the original game's collision flag map into the test.
                // Important Note: This does _not_ add locs into the loc registry.
                gameCollisionMap.flags.copyInto(collision.flags)
                bind(CollisionFlagMap::class.java).toInstance(collision)
            }

            bind(AreaIndex::class.java).`in`(Scopes.SINGLETON)

            bind(GameExceptionHandler::class.java)
                .to(TestExceptionHandler::class.java)
                .`in`(Scopes.SINGLETON)

            bind(GameRandom::class.java)
                .annotatedWith(CoreRandom::class.java)
                .toInstance(FixedRandom(start = 0))

            VariableGameRandom().let { random ->
                bind(GameRandom::class.java).toInstance(random.impl)
                bind(VariableGameRandom::class.java).toInstance(random)
            }

            bind(Realm::class.java).toInstance(createTestRealm())
            bind(Database::class.java).to(ThrowDatabase::class.java).`in`(Scopes.SINGLETON)
            bind(DatabaseConfig::class.java)
                .toInstance(DatabaseConfig("jdbc:test", "test", "test", usesEmbeddedPostgres = false))

            bind(EventBus::class.java).`in`(Scopes.SINGLETON)
            bind(GameUpdate::class.java).`in`(Scopes.SINGLETON)
            bind(MapClock::class.java).`in`(Scopes.SINGLETON)
            bind(LocZoneStorage::class.java).`in`(Scopes.SINGLETON)

            bind(BoundValidator::class.java).`in`(Scopes.SINGLETON)
            bind(RayCastValidator::class.java).`in`(Scopes.SINGLETON)
            bind(RayCastFactory::class.java).`in`(Scopes.SINGLETON)
            bind(RouteFactory::class.java).`in`(Scopes.SINGLETON)
            bind(StepFactory::class.java).`in`(Scopes.SINGLETON)

            bind(AccountRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(ControllerList::class.java).`in`(Scopes.SINGLETON)
            bind(ControllerRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(ControllerRepository::class.java).`in`(Scopes.SINGLETON)
            bind(PlayerList::class.java).`in`(Scopes.SINGLETON)
            bind(PlayerRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(PlayerRepository::class.java).`in`(Scopes.SINGLETON)
            bind(ZonePlayerActivityBitSet::class.java).`in`(Scopes.SINGLETON)
            bind(org.rsmod.game.entity.NpcList::class.java).`in`(Scopes.SINGLETON)
            bind(NpcRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(NpcRepository::class.java).`in`(Scopes.SINGLETON)
            bind(LocRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(LocRegistryNormal::class.java).`in`(Scopes.SINGLETON)
            bind(LocRegistryRegion::class.java).`in`(Scopes.SINGLETON)
            bind(LocRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RegionListSmall::class.java).`in`(Scopes.SINGLETON)
            bind(RegionListLarge::class.java).`in`(Scopes.SINGLETON)
            bind(RegionRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(RegionRepository::class.java).`in`(Scopes.SINGLETON)
            bind(ObjRegistry::class.java).`in`(Scopes.SINGLETON)
            bind(ObjRepository::class.java).`in`(Scopes.SINGLETON)
            bind(WorldRepository::class.java).`in`(Scopes.SINGLETON)

            bind(CheatCommandMap::class.java).`in`(Scopes.SINGLETON)
            bind(EngineQueueCache::class.java).`in`(Scopes.SINGLETON)

            Multibinder.newSetBinder(binder(), InvisibleLevelMod::class.java)
            bind(InvisibleLevels::class.java).`in`(Scopes.SINGLETON)

            Multibinder.newSetBinder(binder(), XpMod::class.java)
            bind(XpModifiers::class.java).`in`(Scopes.SINGLETON)

            bind(MarketPrices::class.java)
                .to(DefaultMarketPrices::class.java)
                .`in`(Scopes.SINGLETON)

            bind(ShuffledPlayerList::class.java)
                .toProvider(ShuffledPlayerListProvider::class.java)
                .`in`(Scopes.SINGLETON)

            Multibinder.newSetBinder(binder(), NpcDamageContributor::class.java)
            Multibinder.newSetBinder(binder(), PlayerInvUpdateHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerPostTickHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerGroundItemDropHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerObjTakeValidateHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerTeleportValidateHook::class.java)
            bind(NpcHitModifier::class.java).to(StandardNpcHitModifier::class.java)
            bind(PlayerHitModifier::class.java).to(StandardPlayerHitModifier::class.java)
            bind(NpcHitProcessor::class.java).to(StandardNpcHitProcessor::class.java)
            bind(InstantPlayerHitProcessor::class.java).to(DamageOnlyPlayerHitProcessor::class.java)

            Multibinder.newSetBinder(binder(), CharacterDataStage.Pipeline::class.java)
        }

        private fun installModules() {
            install(ParserModule)
            install(ServerConfigModule)
        }

        private fun createTestRealm(): Realm {
            val realm = Realm(name = "test-suite")
            realm.updateConfig(TestRealmConfig.create())
            return realm
        }

        private class ShuffledPlayerListProvider
        @Inject
        constructor(private val playerList: PlayerList) : Provider<ShuffledPlayerList> {
            override fun get(): ShuffledPlayerList = ShuffledPlayerList(playerList)
        }

        private class ThrowDatabase : Database {
            override suspend fun <T> withTransaction(block: (DatabaseConnection) -> T): T =
                error(
                    "ThrowDatabase was used: no real Database is available. " +
                        "If your test needs database access, bind a test-specific Database."
                )
        }
    }

    @Suppress("konsist.avoid usage of stdlib Random in properties")
    public class VariableGameRandom {
        internal val impl: InternalRandomImpl = InternalRandomImpl()
        private val sequence = SequenceRandom(size = 128)
        private val default = DefaultGameRandom(seed = 123456)

        /** @see [SequenceRandom.next] */
        public var next: Int by sequence::next

        /** @see [SequenceRandom.then] */
        public var then: Int by sequence::then

        public var nextBoolean: Boolean
            get() = if (sequence.hasNext) sequence.next == 0 else default.randomBoolean()
            set(value) {
                next = if (value) 0 else 1
            }

        internal inner class InternalRandomImpl : GameRandom {
            override fun of(maxExclusive: Int): Int {
                return if (sequence.hasNext) {
                    sequence.of(maxExclusive)
                } else {
                    default.of(maxExclusive)
                }
            }

            override fun of(minInclusive: Int, maxInclusive: Int): Int {
                return if (sequence.hasNext) {
                    sequence.of(minInclusive, maxInclusive)
                } else {
                    default.of(minInclusive, maxInclusive)
                }
            }

            override fun randomDouble(): Double {
                return if (sequence.hasNext) {
                    sequence.randomDouble()
                } else {
                    default.randomDouble()
                }
            }
        }
    }

    public class StatsDelegate(private val backing: PlayerStatMap) {
        @OptIn(InternalApi::class)
        public operator fun get(stat: StatType): Int =
            backing.getCurrentLevel(stat.internalName).toInt() and 0xFF

        public operator fun set(stat: StatType, value: Int) {
            backing.setBaseLevel(stat.internalName, value.toByte())
            backing.setCurrentLevel(stat.internalName, value.toByte())
            if (value <= 0) {
                backing.setFineXP(stat.internalName, 0)
            } else {
                val xp = PlayerSkillXPTable.getFineXPFromLevel(value)
                backing.setFineXP(stat.internalName, xp)
            }
        }

        public fun setBaseLevel(stat: StatType, value: Int) {
            backing.setBaseLevel(stat.internalName, value.toByte())
            if (value <= 0) {
                backing.setFineXP(stat.internalName, 0)
            } else {
                val xp = PlayerSkillXPTable.getFineXPFromLevel(value)
                backing.setFineXP(stat.internalName, xp)
            }
        }

        public fun setCurrentLevel(stat: StatType, value: Int) {
            backing.setCurrentLevel(stat.internalName, value.toByte())
        }
    }

    private class TestExceptionHandler : GameExceptionHandler {
        override fun handle(t: Throwable, msg: () -> Any?) {
            throw t
        }
    }
}
