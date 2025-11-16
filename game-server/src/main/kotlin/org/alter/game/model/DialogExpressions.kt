package org.alter.game.model

import org.alter.rscm.RSCM.asRSCM

enum class DialogueExpression(val id: String) {
    NEUTRAL_DIALOGEXPR("sequences.chatquiz1"),
    HAPPY("sequences.chatneu1"),
    CALM("sequences.chatneu2"),
    CALM_CONTINUED("sequences.chatneu3"),
    DEFAULT("sequences.chatneu4"),
    EVIL("sequences.chatshifty1"),
    EVIL_CONTINUED("sequences.chatshifty2"),
    DELIGHTED_EVIL("sequences.chatshifty3"),
    ANNOYED("sequences.chatshifty4"),
    DISTRESSED("sequences.chatscared1"),
    DISTRESSED_CONTINUED("sequences.chatscared2"),
    DISORIENTED_LEFT("sequences.chatdrunk1"),
    DISORIENTED_RIGHT("sequences.chatdrunk2"),
    UNINTERESTED("sequences.chatdrunk3"),
    SLEEPY("sequences.chatdrunk4"),
    PLAIN_EVIL("sequences.evilidle1"),
    LAUGHING("sequences.chatlaugh1"),
    LONGER_LAUGHING("sequences.chatlaugh2"),
    LONGER_LAUGHING_2("sequences.chatlaugh3"),
    LAUGHING_2("sequences.chatlaugh4"),
    EVIL_LAUGH_SHORT("sequences.evillaugh1"),
    SLIGHTLY_SAD("sequences.chatsad1"),
    SAD("sequences.chatscared4"),
    VERY_SAD("sequences.chatsad2"),
    OTHER("sequences.chatsad3"),
    NEAR_TEARS("sequences.chatscared3"),
    NEAR_TEARS_2("sequences.chatsad4"),
    ANGRY_1("sequences.chatang1"),
    ANGRY_2("sequences.chatang2"),
    ANGRY_3("sequences.chatang3"),
    ANGRY_4("sequences.chatang4");
}