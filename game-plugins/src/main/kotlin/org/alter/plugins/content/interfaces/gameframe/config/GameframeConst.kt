package org.alter.plugins.content.interfaces.gameframe.config

import org.alter.rscm.RSCM.asRSCM

object Orbs {
    val ORBS_UNIVERSE = "interfaces.orbs".asRSCM() //interface 160
    val XP_ORB = "components.orbs:cr_button".asRSCM() //Component 6
    val XPDROPSENABLED = "varbits.xpdrops_enabled".asRSCM() //XP_DROPS_VISIBLE_VARBIT = 4702
    val XP_DROPS = "interfaces.xp_drops".asRSCM() //Interface_id = 12
    val RUNVARP = "varp.option_run".asRSCM() //VARP 173
    val RUN_ORB = "components.orbs:runenergy_backing".asRSCM() //160:28
    val POISON = "varp.poison".asRSCM() //VARP = 102 / HEALTHY = 0 POISON = 1 VENOM = 1000000
    val DISEASED = "varp.disease".asRSCM() //VARP 456 / HEALTHY = 0 DISEASED = 1
    val SPECBUTTON = "components.orbs:runbutton".asRSCM() //160:35
    val SPECATTVARP = "varp.sa_attack".asRSCM() //VARP 301
}

//TO-DO
// XP Settings stuff
// HP Cure
// Quik Prayer