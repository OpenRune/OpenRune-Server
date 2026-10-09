package org.rsmod.content.minigames.gauntlet.layout

import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.content.minigames.gauntlet.GauntletMode
import org.rsmod.content.minigames.gauntlet.GauntletZones

internal object LayoutTemplateBuilder {
    fun build(layout: GauntletLayout, mode: GauntletMode): RegionStaticTemplate =
        RegionTemplate.create {
            for (room in layout.all()) {
                if (room.kind != RoomKind.START && room.kind != RoomKind.BOSS) continue
                val zoneX = RoomTemplates.zoneX(mode, room.kind)
                val zoneZ = RoomTemplates.zoneZ(room)
                for (level in 0..3) {
                    copy(zoneX, zoneZ, level) {
                        zoneWidth = GauntletZones.ROOM_ZONES
                        zoneLength = GauntletZones.ROOM_ZONES
                        regionZoneX = room.x * GauntletZones.ROOM_ZONES
                        regionZoneZ = room.z * GauntletZones.ROOM_ZONES
                        rotation = room.rotation
                    }
                }
            }
        }
}
