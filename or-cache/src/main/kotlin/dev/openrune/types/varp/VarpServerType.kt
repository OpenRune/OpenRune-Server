package dev.openrune.types.varp

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("varp")
public data class VarpServerType(
    override var id: Int = -1,
    var bitProtect: Boolean = false,
    var configType: Int = -1,
    var scope: VarpLifetime = VarpLifetime.Perm,
    var transmit: VarpTransmitLevel = VarpTransmitLevel.OnSetAlways,
) : Definition
