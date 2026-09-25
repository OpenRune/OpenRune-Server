package org.rsmod.content.other.pets.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * Boss pets keyed by the npc that drops them, so a pet can be wired up without editing that npc's
 * drop table. Npcs whose drop table already lists the pet item must not appear here, or the pet
 * would be rolled twice.
 */
object PetDropsTable {
    const val NPC = 0
    const val PET = 1
    const val RATE = 2

    fun drops() = dbTable("dbtable.pet_drops", serverOnly = true) {
        column("npc", NPC, VarType.NPC)
        column("pet", PET, VarType.OBJ)
        column("rate", RATE, VarType.INT)

        fun drop(npc: String, pet: String, rate: Int) {
            row("dbrow.pet_drop_${npc.removePrefix("npc.")}") {
                columnRSCM(NPC, npc)
                columnRSCM(PET, pet)
                column(RATE, rate)
            }
        }

        drop("npc.mad_angel", "obj.madangelpet", 2000)
        drop("npc.mad_angel_quest", "obj.madangelpet", 2000)
        drop("npc.mole_giant", "obj.molepet", 3000)
        drop("npc.cowboss", "obj.cowbosspet", 1000)
        drop("npc.callisto", "obj.callisto_pet", 1500)
        drop("npc.callisto_singles", "obj.callisto_pet", 2800)
        drop("npc.inferno_tzkalzuk_placeholder", "obj.infernopet", 100)
        drop("npc.kalphite_flyingqueen", "obj.kqpet_walking", 3000)
        drop("npc.maggot_king", "obj.maggotkingpet", 3500)
        drop("npc.nex", "obj.nexpet", 500)
        drop("npc.gargboss_dusk_phase4", "obj.dawnpet", 3000)
        drop("npc.dagcave_magic_boss", "obj.primepet", 5000)
        drop("npc.dagcave_melee_boss", "obj.rexpet", 5000)
        drop("npc.dagcave_ranged_boss", "obj.supremepet", 5000)
        drop("npc.corp_beast", "obj.corepet", 5000)
        drop("npc.slayer_kraken_boss", "obj.krakenpet", 3000)
        drop("npc.smoke_devil_boss", "obj.smokepet", 3000)
        drop("npc.snakeboss_boss_ranged", "obj.snakepet", 4000)
        drop("npc.snakeboss_boss_melee", "obj.snakepet", 4000)
        drop("npc.snakeboss_boss_magic", "obj.snakepet", 4000)
        drop("npc.cata_boss", "obj.skotizopet", 65)
        drop("npc.zalcano", "obj.zalcanopet", 2250)
        drop("npc.tzhaar_fightcave_swarm_boss", "obj.jad_pet", 200)
        drop("npc.venenatis", "obj.venenatis_pet", 1500)
        drop("npc.venenatis_singles", "obj.venenatis_pet", 2800)
        drop("npc.vetion_2", "obj.vetion_pet", 1500)
        drop("npc.vetion_2_single", "obj.vetion_pet", 2800)
    }
}
