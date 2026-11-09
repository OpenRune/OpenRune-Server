package org.rsmod.content.events.leaguetasks.pack

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * The Demonic Pacts tutorial's `screenhighlight` pages, one row per page, copied from the
 * `highlight_screen` / `highlight_screen_component` and `highlight_textbox` calls in `rsprox-1098`.
 * [HINT] is the `hnt_selected_hint` the tour runs under. A row frames either a fixed box
 * ([OFF_X]..[OFF_W]) or, when [FOCUS] is set, the box around [FOCUS] (child [FOCUS_SUB]) laid out
 * in [FOCUS_OUTER]. Every column is written on every row; unused component columns hold -1.
 */
object LeagueTutorialHintTable {
    const val HINT = 0
    const val PAGE = 1
    const val HOST = 2
    const val TEXT = 3
    const val FOCUS_OUTER = 4
    const val FOCUS = 5
    const val FOCUS_SUB = 6
    const val OFF_X = 7
    const val OFF_Y = 8
    const val OFF_H = 9
    const val OFF_W = 10
    const val SETPOS_H = 11
    const val SETPOS_V = 12
    const val HIDE_COM7 = 13
    const val DEFAULT_TEXTBOX = 14
    const val LAST_PAGE = 15
    const val PAUSE = 16
    const val PREVIOUS_BUTTON = 17

    const val TASKS = 1
    const val RELICS = 2
    const val AREAS = 3
    const val PACTS = 7

    fun hints() =
        dbTable("dbtable.league_tutorial_hint", serverOnly = true) {
            column("hint", HINT, VarType.INT)
            column("page", PAGE, VarType.INT)
            column("host", HOST, VarType.INT)
            column("text", TEXT, VarType.STRING)
            column("focus_outer", FOCUS_OUTER, VarType.INT)
            column("focus", FOCUS, VarType.INT)
            column("focus_sub", FOCUS_SUB, VarType.INT)
            column("off_x", OFF_X, VarType.INT)
            column("off_y", OFF_Y, VarType.INT)
            column("off_h", OFF_H, VarType.INT)
            column("off_w", OFF_W, VarType.INT)
            column("setpos_h", SETPOS_H, VarType.INT)
            column("setpos_v", SETPOS_V, VarType.INT)
            column("hide_com7", HIDE_COM7, VarType.BOOLEAN)
            column("default_textbox", DEFAULT_TEXTBOX, VarType.BOOLEAN)
            column("last_page", LAST_PAGE, VarType.BOOLEAN)
            column("pause", PAUSE, VarType.BOOLEAN)
            column("previous_button", PREVIOUS_BUTTON, VarType.BOOLEAN)

            val rows = HintRows(this)
            rows.tasksTour()
            rows.areasTour()
            rows.relicsTour()
            rows.pactsTour()
        }

    private fun HintRows.tasksTour() {
        val host = "component.league_tasks:tutorial_highlight"
        page(
            TASKS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "This is the <col=ffffff>task list</col>. Here you can see all of the <col=ffffff>tasks</col> available to you in this <col=ffffff>League</col>.",
        )
        page(
            TASKS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "Within the <col=ffffff>task list</col>, you can view more information about each <col=ffffff>task</col>, including <col=ffffff>difficulty</col> and the amount of <col=ffffff>League Points</col> awarded for completion.",
        )
        page(
            TASKS,
            host,
            focus("component.league_tasks:content_outer", "component.league_tasks:tasks_background", 0),
            "There are <col=ffffff>five</col> different tiers of <col=ffffff>task difficulty</col>. The harder a <col=ffffff>task</col>, the more <col=ffffff>League Points</col> you'll earn.",
            defaultTextbox = true,
        )
        page(
            TASKS,
            host,
            screen(94, 240, 2, 49, 0, 0, hideCom7 = false),
            "Each <col=ffffff>League</col> has hundreds of <col=ffffff>tasks</col> that cover all aspects of the game. You can <col=ffffff>filter</col> the <col=ffffff>task list</col> to make it easier to find the right one for you.",
        )
        page(
            TASKS,
            host,
            focus("component.league_tasks:content_outer", "component.league_tasks:progress_bar", -1),
            "You can easily keep track of your progress within the <col=ffffff>League</col> using the <col=ffffff>progress bar</col>. As you progress, you'll gain access to new <col=ffffff>unlocks</col> within the <col=ffffff>League</col> as well as new <col=ffffff>rewards</col> on your <col=ffffff>main profile</col>.",
            defaultTextbox = true,
        )
        page(
            TASKS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "There are a lot of <col=ffffff>tasks</col> on offer during this <col=ffffff>League</col>, try and aim to complete as many as possible!",
            lastPage = true,
        )
    }

    private fun HintRows.areasTour() {
        val host = "component.trailblazer_areas:tutorial_highlight"
        page(
            AREAS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "This is the <col=ffffff>Area Selection Menu</col>. Here you can unlock new <col=ffffff>areas</col> or <col=ffffff>teleport</col> to previously unlocked ones.",
        )
        page(
            AREAS,
            host,
            focus(
                "component.trailblazer_areas:content_outer",
                "component.trailblazer_areas:progress_bar",
                -1,
            ),
            "New <col=ffffff>areas</col> are unlocked using <col=ffffff>tasks</col>. The more <col=ffffff>tasks</col> you have completed, the more <col=ffffff>areas</col> you can unlock. The <col=ffffff>Area Selection Menu</col> can be used to easily track your progress towards the next unlock.",
        )
        page(
            AREAS,
            host,
            screen(135, 120, 0, 5, 0, 2, hideCom7 = false),
            "There are <col=ffffff>ten</col> different <col=ffffff>areas</col> in <col=ffffff>Leagues VI - Demonic Pacts</col>, but you can only unlock <col=ffffff>five</col> of them. Your first area, the <col=ffffff>Kingdom of Varlamore</col>, has been unlocked automatically.",
        )
        page(
            AREAS,
            host,
            screen(55, 84, 255, 32, 0, 2, hideCom7 = false),
            "As a bonus, <col=ffffff>Karamja</col> will be your next region unlock at the first tasks milestone.",
        )
        page(
            AREAS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "To unlock additional <col=ffffff>areas</col>, you can use the <col=ffffff>Area Information Panel</col>. <col=ffffff>Clicking</col> on any <col=ffffff>area</col> will bring up the respective <col=ffffff>Area Information Panel</col>.",
        )
        page(
            AREAS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "As well as letting you unlock new <col=ffffff>areas</col>, the <col=ffffff>Area Information Panel</col> can also be used to <col=ffffff>teleport</col> to already available <col=ffffff>areas</col>.",
            lastPage = true,
        )
    }

    private fun HintRows.relicsTour() {
        val host = "component.league_relics:tutorial_highlight"
        val outer = "component.league_relics:content_outer"
        page(
            RELICS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "This is the <col=ffffff>Relic Selection Menu</col>. Here you can unlock powerful <col=ffffff>Relics</col> to use during this <col=ffffff>League</col>.",
        )
        page(
            RELICS,
            host,
            focus(outer, "component.league_relics:progress_bar", -1),
            "<col=ffffff>Relics</col> are unlocked using <col=ffffff>League Points</col>. The more <col=ffffff>League Points</col> you have, the more <col=ffffff>Relic</col> tiers available to you. There are <col=ffffff>8</col> <col=ffffff>Relic</col> tiers in total and the <col=ffffff>Relic Selection Menu</col> can be used to easily track your progress towards the next one.",
        )
        page(
            RELICS,
            host,
            focus(outer, "component.league_relics:tutorial", 0),
            "Each <col=ffffff>Relic</col> tier contains <col=ffffff>up to three</col> different <col=ffffff>Relics</col>, all with unique powers. Once you've unlocked a <col=ffffff>Relic</col> tier, you can choose any <col=ffffff>Relic</col> from within it. However, you can only pick <col=ffffff>one</col> <col=ffffff>Relic</col> from each tier and you can't change your mind later.",
        )
        page(
            RELICS,
            host,
            focus(outer, "component.league_relics:tutorial", 1),
            "<col=ffffff>Clicking</col> on a <col=ffffff>Relic</col>, even one from a locked tier, will bring up the <col=ffffff>Relic Information Panel</col>. From there, you can view information on that <col=ffffff>Relic</col> and, if it's available, decide if you want to unlock it.",
        )
        page(
            RELICS,
            host,
            focus(outer, "component.league_relics:tutorial", 0),
            "For each <col=ffffff>Relic</col> tier you have available, you'll gain <col=ffffff>passive effects</col>. The <col=ffffff>passive effects</col> for each tier are displayed on the <col=ffffff>Relic Information Panel</col> of any <col=ffffff>Relic</col> in that tier.",
        )
        page(
            RELICS,
            host,
            screen(0, 0, 0, 0, 2, 2, hideCom7 = true),
            "The first <col=ffffff>Relic</col> tier is free. So if you want, you can unlock your first <col=ffffff>Relic</col> now! If you'd rather decide later, just close the <col=ffffff>Relic Selection Menu</col> with the <col=ffffff>cross</col> in the top right corner.",
            lastPage = true,
        )
    }

    private fun HintRows.pactsTour() {
        val host = "component.talent_tree:tutorial_highlight"
        val frame = screen(0, 0, 0, 0, 0, 2, hideCom7 = true)
        page(
            PACTS,
            host,
            frame,
            "This is the <col=ffffff>Demonic Pacts menu</col>. When you gain <col=ffffff>Demonic Pact points</col> you can spend them here to unlock new abilities and increase your power.",
        )
        page(
            PACTS,
            host,
            frame,
            "You can see which tasks will reward you with <col=ffffff>Demonic Pacts points</col> by using the filters in the <col=ffffff>League Task menu</col>.",
        )
        page(
            PACTS,
            host,
            frame,
            "To get you started, you have <col=ffffff>1 free pact point</col>. Spend this point to <col=ffffff>unlock the central pact</col> now.",
            pause = true,
        )
        page(
            PACTS,
            host,
            frame,
            "After this tutorial you will receive another <col=ffffff>1 free pact point</col> to spend.<br><col=ffffff>We strongly recommend spending it in a style specific pact, linked to the central pact.",
            previousButton = true,
        )
        page(
            PACTS,
            host,
            frame,
            "You can spend pact points whenever you like, so take some time to explore the options available - and choose wisely!",
        )
    }

    private fun screen(
        offX: Int,
        offY: Int,
        offH: Int,
        offW: Int,
        setPosH: Int,
        setPosV: Int,
        hideCom7: Boolean,
    ): Frame = Frame(null, null, -1, offX, offY, offH, offW, setPosH, setPosV, hideCom7)

    private fun focus(outer: String, focus: String, sub: Int): Frame =
        Frame(outer, focus, sub, 0, 0, 0, 0, 0, 0, false)

    private class Frame(
        val outer: String?,
        val focus: String?,
        val focusSub: Int,
        val offX: Int,
        val offY: Int,
        val offH: Int,
        val offW: Int,
        val setPosH: Int,
        val setPosV: Int,
        val hideCom7: Boolean,
    )

    /** Rows take ids from a reserved block; nothing refers to a hint row by id. */
    private class HintRows(private val table: DBTableBuilder) {
        private var next = FIRST_ROW
        private val pages = HashMap<Int, Int>()

        fun page(
            hint: Int,
            host: String,
            frame: Frame,
            text: String,
            defaultTextbox: Boolean = false,
            lastPage: Boolean = false,
            pause: Boolean = false,
            previousButton: Boolean = false,
        ) {
            check(next <= LAST_ROW) { "dbtable.league_tutorial_hint ran out of row ids at $LAST_ROW." }
            val page = pages.merge(hint, 1, Int::plus)!! - 1
            table.row(next++) {
                column(HINT, hint)
                column(PAGE, page)
                columnRSCM(HOST, host)
                column(TEXT, text)
                if (frame.outer != null && frame.focus != null) {
                    columnRSCM(FOCUS_OUTER, frame.outer)
                    columnRSCM(FOCUS, frame.focus)
                } else {
                    column(FOCUS_OUTER, NO_COMPONENT)
                    column(FOCUS, NO_COMPONENT)
                }
                column(FOCUS_SUB, frame.focusSub)
                column(OFF_X, frame.offX)
                column(OFF_Y, frame.offY)
                column(OFF_H, frame.offH)
                column(OFF_W, frame.offW)
                column(SETPOS_H, frame.setPosH)
                column(SETPOS_V, frame.setPosV)
                column(HIDE_COM7, frame.hideCom7)
                column(DEFAULT_TEXTBOX, defaultTextbox)
                column(LAST_PAGE, lastPage)
                column(PAUSE, pause)
                column(PREVIOUS_BUTTON, previousButton)
            }
        }

        private companion object {
            const val FIRST_ROW = 43000
            const val LAST_ROW = 43099
            const val NO_COMPONENT = -1
        }
    }
}
